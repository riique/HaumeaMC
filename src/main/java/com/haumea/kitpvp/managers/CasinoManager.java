package com.haumea.kitpvp.managers;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.models.PlayerData;
import com.haumea.kitpvp.models.casino.*;
import com.haumea.kitpvp.profile.PlayerProfile;
import com.haumea.kitpvp.utils.ChatStorage;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gerenciador central do sistema de Cassino.
 * 
 * Controla todas as operações do cassino incluindo:
 * - Sessões de jogos ativos
 * - Coinflips pendentes
 * - Jogo de Crash em tempo real
 * - Estatísticas e validações
 * 
 * @author HaumeaMC
 */
public class CasinoManager {

    private final HaumeaMC plugin;

    // Sessões ativas de jogadores
    private final Map<UUID, CasinoSession> activeSessions;

    // Coinflips pendentes
    private final Map<UUID, CoinflipRequest> pendingCoinflips;

    // Jogo de Crash atual
    private CrashGame currentCrashGame;
    private int crashCountdown;
    private BukkitRunnable crashTask;

    // Pool do Jackpot
    private long jackpotPool;

    // Configurações
    private boolean enabled;
    private int hotbarSlot;
    private boolean onlyInSpawn;
    private long minBet;
    private long maxBet;
    private long dailyLossLimit;
    private List<Long> presetBets;
    private boolean allowCustomBet;
    private int houseEdgeCoinflip;

    // Formatador de números
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#,##0");
    private static final Random RANDOM = new Random();

    // Constantes para chaves de customData
    public static final String KEY_TOTAL_WAGERED = "casino_total_wagered";
    public static final String KEY_TOTAL_WON = "casino_total_won";
    public static final String KEY_TOTAL_LOST = "casino_total_lost";
    public static final String KEY_GAMES_PLAYED = "casino_games_played";
    public static final String KEY_BIGGEST_WIN = "casino_biggest_win";
    public static final String KEY_DAILY_LOSS = "casino_daily_loss";
    public static final String KEY_DAILY_LOSS_DATE = "casino_daily_loss_date";

    // Prefixos por jogo
    public static final String KEY_SLOTS_PREFIX = "slots_";
    public static final String KEY_ROULETTE_PREFIX = "roulette_";
    public static final String KEY_BLACKJACK_PREFIX = "blackjack_";
    public static final String KEY_COINFLIP_PREFIX = "coinflip_";
    public static final String KEY_CRASH_PREFIX = "crash_";

    public CasinoManager(HaumeaMC plugin) {
        this.plugin = plugin;
        this.activeSessions = new ConcurrentHashMap<>();
        this.pendingCoinflips = new ConcurrentHashMap<>();
        this.jackpotPool = 0;

        loadConfig();
        startCrashGameLoop();
        startCleanupTask();
    }

    /**
     * Carrega as configurações do cassino.
     */
    private void loadConfig() {
        this.enabled = plugin.getConfig().getBoolean("casino.enabled", true);
        this.hotbarSlot = plugin.getConfig().getInt("casino.hotbar-slot", 2);
        this.onlyInSpawn = plugin.getConfig().getBoolean("casino.only-in-spawn", true);
        this.minBet = plugin.getConfig().getLong("casino.limits.min-bet", 100);
        this.maxBet = plugin.getConfig().getLong("casino.limits.max-bet", 100000);
        this.dailyLossLimit = plugin.getConfig().getLong("casino.limits.daily-loss-limit", 500000);
        this.allowCustomBet = plugin.getConfig().getBoolean("casino.allow-custom-bet", true);
        this.houseEdgeCoinflip = plugin.getConfig().getInt("casino.house-edge.coinflip", 5);

        // Carregar apostas pré-definidas
        this.presetBets = new ArrayList<>();
        List<Integer> configBets = plugin.getConfig().getIntegerList("casino.preset-bets");
        if (configBets.isEmpty()) {
            // Valores padrão
            presetBets.add(100L);
            presetBets.add(500L);
            presetBets.add(1000L);
            presetBets.add(5000L);
            presetBets.add(10000L);
        } else {
            for (Integer bet : configBets) {
                presetBets.add(bet.longValue());
            }
        }
    }

    // ==================== VALIDAÇÕES ====================

    /**
     * Verifica se o jogador pode apostar.
     */
    public boolean canBet(Player player, long amount) {
        // Verificar se está habilitado
        if (!enabled)
            return false;

        // Verificar se está no spawn (se configurado)
        if (onlyInSpawn && !isInSpawn(player)) {
            return false;
        }

        // Verificar limites de aposta
        if (amount < minBet || amount > maxBet) {
            return false;
        }

        // Verificar saldo
        long balance = plugin.getStatsManager().getMoney(player);
        if (balance < amount) {
            return false;
        }

        // Verificar limite diário de perdas
        if (dailyLossLimit > 0 && getDailyLoss(player) >= dailyLossLimit) {
            return false;
        }

        // Verificar se não está em outro jogo
        if (hasActiveSession(player)) {
            return false;
        }

        return true;
    }

    /**
     * Valida a aposta e envia mensagem de erro se inválida.
     */
    public boolean validateBet(Player player, long amount) {
        if (!enabled) {
            ChatStorage.sendRaw(player, "&c&lCASSINO &fO cassino está desativado no momento!");
            return false;
        }

        if (onlyInSpawn && !isInSpawn(player)) {
            ChatStorage.sendRaw(player, "&c&lCASSINO &fVocê precisa estar no spawn para usar o cassino!");
            return false;
        }

        if (amount < minBet) {
            ChatStorage.sendRaw(player, "&c&lCASSINO &fAposta mínima é &e" + formatCoins(minBet) + " coins&f!");
            return false;
        }

        if (amount > maxBet) {
            ChatStorage.sendRaw(player, "&c&lCASSINO &fAposta máxima é &e" + formatCoins(maxBet) + " coins&f!");
            return false;
        }

        long balance = plugin.getStatsManager().getMoney(player);
        if (balance < amount) {
            ChatStorage.sendRaw(player, "&c&lCASSINO &fVocê não tem coins suficientes!");
            return false;
        }

        if (dailyLossLimit > 0 && getDailyLoss(player) >= dailyLossLimit) {
            ChatStorage.sendRaw(player, "&c&lCASSINO &fVocê atingiu o limite diário de perdas! Volte amanhã.");
            return false;
        }

        if (hasActiveSession(player)) {
            ChatStorage.sendRaw(player, "&c&lCASSINO &fVocê já está em um jogo! Termine primeiro.");
            return false;
        }

        return true;
    }

    // ==================== SLOTS ====================

    /**
     * Joga no caça-níqueis.
     */
    public SlotsResult playSlots(Player player, long bet) {
        // Deduzir aposta
        plugin.getStatsManager().removeMoney(player, bet);

        // Gerar resultado
        SlotsResult result = generateSlotsResult(bet);

        // Processar resultado
        if (result.isWin()) {
            recordWin(player, CasinoGame.SLOTS, bet, result.getPayout());
            plugin.getStatsManager().addMoney(player, result.getPayout());

            // Incrementar estatísticas de vitória
            incrementStat(player, KEY_SLOTS_PREFIX + "wins");

            if (result.isJackpot()) {
                incrementStat(player, KEY_SLOTS_PREFIX + "jackpots");
                broadcastJackpot(player, result.getPayout());
            }
        } else {
            recordLoss(player, CasinoGame.SLOTS, bet);
        }

        // Incrementar jogos
        incrementStat(player, KEY_SLOTS_PREFIX + "played");

        return result;
    }

    /**
     * Gera um resultado aleatório para os slots.
     */
    private SlotsResult generateSlotsResult(long bet) {
        // Probabilidades (configuráveis)
        int loseChance = plugin.getConfig().getInt("casino.slots.lose", 55);
        int twoMatchChance = plugin.getConfig().getInt("casino.slots.two-match", 30);
        int threeCommonChance = plugin.getConfig().getInt("casino.slots.three-common", 10);
        int threeRareChance = plugin.getConfig().getInt("casino.slots.three-rare", 4);
        // jackpot = 100 - todos os outros = 1%

        int roll = RANDOM.nextInt(100);
        int cumulative = 0;

        SlotsResult.SlotSymbol[] symbols = new SlotsResult.SlotSymbol[3];
        SlotsResult.ResultType resultType;
        double multiplier;

        cumulative += loseChance;
        if (roll < cumulative) {
            // Perda - 3 símbolos diferentes
            symbols = generateDifferentSymbols();
            resultType = SlotsResult.ResultType.LOSE;
            multiplier = 0;
        } else {
            cumulative += twoMatchChance;
            if (roll < cumulative) {
                // 2 iguais
                symbols = generateTwoMatchSymbols();
                resultType = SlotsResult.ResultType.TWO_MATCH;
                multiplier = plugin.getConfig().getDouble("casino.multipliers.slots.two-match", 1.5);
            } else {
                cumulative += threeCommonChance;
                if (roll < cumulative) {
                    // 3 comuns iguais
                    SlotsResult.SlotSymbol symbol = randomCommonSymbol();
                    symbols[0] = symbol;
                    symbols[1] = symbol;
                    symbols[2] = symbol;
                    resultType = SlotsResult.ResultType.THREE_COMMON;
                    multiplier = plugin.getConfig().getDouble("casino.multipliers.slots.three-common", 3.0);
                } else {
                    cumulative += threeRareChance;
                    if (roll < cumulative) {
                        // 3 raros iguais
                        SlotsResult.SlotSymbol symbol = randomRareSymbol();
                        symbols[0] = symbol;
                        symbols[1] = symbol;
                        symbols[2] = symbol;
                        resultType = SlotsResult.ResultType.THREE_RARE;
                        multiplier = plugin.getConfig().getDouble("casino.multipliers.slots.three-rare", 10.0);
                    } else {
                        // JACKPOT (Sete)
                        symbols[0] = SlotsResult.SlotSymbol.SEVEN;
                        symbols[1] = SlotsResult.SlotSymbol.SEVEN;
                        symbols[2] = SlotsResult.SlotSymbol.SEVEN;
                        resultType = SlotsResult.ResultType.JACKPOT;
                        multiplier = plugin.getConfig().getDouble("casino.multipliers.slots.jackpot", 25.0);
                    }
                }
            }
        }

        long payout = (long) (bet * multiplier);
        return new SlotsResult(symbols, resultType, multiplier, payout);
    }

    private SlotsResult.SlotSymbol[] generateDifferentSymbols() {
        SlotsResult.SlotSymbol[] all = SlotsResult.SlotSymbol.values();
        List<SlotsResult.SlotSymbol> list = new ArrayList<>(Arrays.asList(all));
        Collections.shuffle(list);
        return new SlotsResult.SlotSymbol[] { list.get(0), list.get(1), list.get(2) };
    }

    private SlotsResult.SlotSymbol[] generateTwoMatchSymbols() {
        SlotsResult.SlotSymbol match = randomSymbol();
        SlotsResult.SlotSymbol different;
        do {
            different = randomSymbol();
        } while (different == match);

        // Posição aleatória para o diferente
        int diffPos = RANDOM.nextInt(3);
        SlotsResult.SlotSymbol[] symbols = new SlotsResult.SlotSymbol[3];
        for (int i = 0; i < 3; i++) {
            symbols[i] = (i == diffPos) ? different : match;
        }
        return symbols;
    }

    private SlotsResult.SlotSymbol randomSymbol() {
        SlotsResult.SlotSymbol[] all = SlotsResult.SlotSymbol.values();
        return all[RANDOM.nextInt(all.length)];
    }

    private SlotsResult.SlotSymbol randomCommonSymbol() {
        SlotsResult.SlotSymbol[] common = SlotsResult.SlotSymbol.getCommon();
        return common[RANDOM.nextInt(common.length)];
    }

    private SlotsResult.SlotSymbol randomRareSymbol() {
        SlotsResult.SlotSymbol[] rare = SlotsResult.SlotSymbol.getRare();
        return rare[RANDOM.nextInt(rare.length)];
    }

    // ==================== ROLETA ====================

    /**
     * Joga na roleta.
     */
    public int playRoulette(Player player, long bet, RouletteBet betType, int betNumber) {
        // Deduzir aposta
        plugin.getStatsManager().removeMoney(player, bet);

        // Girar a roleta (0-36)
        int result = RANDOM.nextInt(37);

        // Verificar vitória
        boolean won = betType.isWinner(result, betNumber);

        if (won) {
            long payout = (long) (bet * betType.getMultiplier());
            recordWin(player, CasinoGame.ROULETTE, bet, payout);
            plugin.getStatsManager().addMoney(player, payout);
            incrementStat(player, KEY_ROULETTE_PREFIX + "wins");
        } else {
            recordLoss(player, CasinoGame.ROULETTE, bet);
        }

        incrementStat(player, KEY_ROULETTE_PREFIX + "played");

        return result;
    }

    // ==================== BLACKJACK ====================

    /**
     * Inicia um jogo de Blackjack.
     */
    public BlackjackGame startBlackjack(Player player, long bet) {
        // Deduzir aposta
        plugin.getStatsManager().removeMoney(player, bet);

        // Criar jogo
        BlackjackGame game = new BlackjackGame(player.getUniqueId(), bet);
        game.start();

        // Criar sessão
        CasinoSession session = new CasinoSession(player.getUniqueId(), CasinoGame.BLACKJACK, bet);
        session.setGameState(game);
        activeSessions.put(player.getUniqueId(), session);

        // Verificar blackjack natural
        if (game.getState() == BlackjackGame.GameState.PLAYER_BLACKJACK) {
            finishBlackjack(player, game);
        } else if (game.getState() == BlackjackGame.GameState.DEALER_WINS ||
                game.getState() == BlackjackGame.GameState.PUSH) {
            finishBlackjack(player, game);
        }

        return game;
    }

    /**
     * Jogador pede carta no Blackjack.
     */
    public Card blackjackHit(Player player) {
        CasinoSession session = activeSessions.get(player.getUniqueId());
        if (session == null || session.getGame() != CasinoGame.BLACKJACK) {
            return null;
        }

        BlackjackGame game = session.getGameState(BlackjackGame.class);
        if (game == null)
            return null;

        Card card = game.hit();

        if (game.isFinished()) {
            finishBlackjack(player, game);
        }

        return card;
    }

    /**
     * Jogador para no Blackjack.
     */
    public void blackjackStand(Player player) {
        CasinoSession session = activeSessions.get(player.getUniqueId());
        if (session == null || session.getGame() != CasinoGame.BLACKJACK) {
            return;
        }

        BlackjackGame game = session.getGameState(BlackjackGame.class);
        if (game == null)
            return;

        game.stand();
        finishBlackjack(player, game);
    }

    /**
     * Jogador dobra no Blackjack.
     */
    public Card blackjackDouble(Player player) {
        CasinoSession session = activeSessions.get(player.getUniqueId());
        if (session == null || session.getGame() != CasinoGame.BLACKJACK) {
            return null;
        }

        BlackjackGame game = session.getGameState(BlackjackGame.class);
        if (game == null || !game.getPlayerHand().canDouble())
            return null;

        // Deduzir aposta adicional
        long additionalBet = game.getBet();
        if (plugin.getStatsManager().getMoney(player) < additionalBet) {
            ChatStorage.sendRaw(player, "&c&lCASSINO &fVocê não tem coins suficientes!");
            return null;
        }

        plugin.getStatsManager().removeMoney(player, additionalBet);

        Card card = game.doubleDown();
        finishBlackjack(player, game);

        return card;
    }

    /**
     * Finaliza um jogo de Blackjack.
     */
    private void finishBlackjack(Player player, BlackjackGame game) {
        // Remover sessão
        activeSessions.remove(player.getUniqueId());

        long payout = game.calculatePayout();
        long bet = game.getActualBet();

        if (payout > 0) {
            plugin.getStatsManager().addMoney(player, payout);

            if (payout > bet) {
                // Vitória
                recordWin(player, CasinoGame.BLACKJACK, bet, payout);
                incrementStat(player, KEY_BLACKJACK_PREFIX + "wins");

                if (game.getState() == BlackjackGame.GameState.PLAYER_BLACKJACK) {
                    incrementStat(player, KEY_BLACKJACK_PREFIX + "naturals");
                }
            }
            // Se payout == bet é empate, não conta como win nem loss
        } else {
            recordLoss(player, CasinoGame.BLACKJACK, bet);
        }

        incrementStat(player, KEY_BLACKJACK_PREFIX + "played");
    }

    // ==================== COINFLIP ====================

    /**
     * Cria um coinflip.
     */
    public CoinflipRequest createCoinflip(Player player, long amount) {
        if (!validateBet(player, amount)) {
            return null;
        }

        // Verificar se já tem um coinflip pendente
        if (pendingCoinflips.containsKey(player.getUniqueId())) {
            ChatStorage.sendRaw(player, "&c&lCASSINO &fVocê já tem um coinflip pendente!");
            return null;
        }

        // Deduzir aposta
        plugin.getStatsManager().removeMoney(player, amount);

        // Criar request
        CoinflipRequest request = new CoinflipRequest(
                player.getUniqueId(),
                player.getName(),
                amount);

        pendingCoinflips.put(player.getUniqueId(), request);

        ChatStorage.sendRaw(player,
                "&6&lCASSINO &fCoinflip de &e" + formatCoins(amount) + " coins &fcriado! Aguardando oponente...");

        return request;
    }

    /**
     * Aceita um coinflip.
     */
    public boolean acceptCoinflip(Player acceptor, CoinflipRequest request) {
        // Verificar se ainda está disponível
        if (!request.isAvailable()) {
            ChatStorage.sendRaw(acceptor, "&c&lCASSINO &fEste coinflip não está mais disponível!");
            return false;
        }

        // Verificar se não é o próprio criador
        if (request.getCreatorId().equals(acceptor.getUniqueId())) {
            ChatStorage.sendRaw(acceptor, "&c&lCASSINO &fVocê não pode aceitar seu próprio coinflip!");
            return false;
        }

        // Validar aposta do aceitante
        if (!validateBet(acceptor, request.getAmount())) {
            return false;
        }

        // Deduzir aposta do aceitante
        plugin.getStatsManager().removeMoney(acceptor, request.getAmount());

        // Marcar como aceito
        request.accept(acceptor.getUniqueId());
        pendingCoinflips.remove(request.getCreatorId());

        // Executar o coinflip
        executeCoinflip(request, acceptor);

        return true;
    }

    /**
     * Cancela um coinflip.
     */
    public boolean cancelCoinflip(Player player) {
        CoinflipRequest request = pendingCoinflips.remove(player.getUniqueId());
        if (request == null) {
            return false;
        }

        // Devolver aposta
        plugin.getStatsManager().addMoney(player, request.getAmount());
        ChatStorage.sendRaw(player, "&6&lCASSINO &fCoinflip cancelado! Coins devolvidos.");

        return true;
    }

    /**
     * Executa o coinflip e determina vencedor.
     */
    private void executeCoinflip(CoinflipRequest request, Player acceptor) {
        Player creator = Bukkit.getPlayer(request.getCreatorId());

        // Calcular prêmio (com taxa da casa)
        long totalPot = request.getAmount() * 2;
        long houseTax = (long) (totalPot * (houseEdgeCoinflip / 100.0));
        long prize = totalPot - houseTax;

        // Sortear vencedor (50/50)
        boolean creatorWins = RANDOM.nextBoolean();

        Player winner;
        Player loser;

        if (creatorWins) {
            winner = creator;
            loser = acceptor;
        } else {
            winner = acceptor;
            loser = creator;
        }

        // Dar prêmio ao vencedor
        if (winner != null && winner.isOnline()) {
            plugin.getStatsManager().addMoney(winner, prize);
            recordWin(winner, CasinoGame.COINFLIP, request.getAmount(), prize);
            incrementStat(winner, KEY_COINFLIP_PREFIX + "wins");
            incrementStat(winner, KEY_COINFLIP_PREFIX + "played");

            ChatStorage.sendRaw(winner, "&a&lVITÓRIA! &fVocê ganhou o coinflip! +&e" + formatCoins(prize) + " coins");
            winner.playSound(winner.getLocation(), Sound.LEVEL_UP, 1f, 1f);
        }

        if (loser != null && loser.isOnline()) {
            recordLoss(loser, CasinoGame.COINFLIP, request.getAmount());
            incrementStat(loser, KEY_COINFLIP_PREFIX + "played");

            String winnerName = winner != null ? winner.getName() : request.getCreatorName();
            ChatStorage.sendRaw(loser,
                    "&c&lDerrota! &f" + winnerName + " venceu o coinflip de &e" + formatCoins(prize) + " coins");
            loser.playSound(loser.getLocation(), Sound.VILLAGER_NO, 1f, 1f);
        }

        // Broadcast
        String winnerName = winner != null ? winner.getName()
                : (creatorWins ? request.getCreatorName() : acceptor.getName());
        String loserName = loser != null ? loser.getName()
                : (!creatorWins ? request.getCreatorName() : acceptor.getName());

        if (prize >= plugin.getConfig().getLong("casino.broadcasts.big-win-threshold", 50000)) {
            broadcastCoinflip(winnerName, loserName, prize);
        }
    }

    /**
     * Obtém todos os coinflips disponíveis.
     */
    public List<CoinflipRequest> getAvailableCoinflips() {
        List<CoinflipRequest> available = new ArrayList<>();
        for (CoinflipRequest request : pendingCoinflips.values()) {
            if (request.isAvailable()) {
                available.add(request);
            }
        }
        return available;
    }

    // ==================== CRASH ====================

    /**
     * Inicia o loop do jogo Crash.
     */
    private void startCrashGameLoop() {
        int roundInterval = plugin.getConfig().getInt("casino.crash.round-interval", 30);
        crashCountdown = roundInterval;
        currentCrashGame = new CrashGame();

        crashTask = new BukkitRunnable() {
            @Override
            public void run() {
                tickCrash();
            }
        };
        crashTask.runTaskTimer(plugin, 10L, 10L); // 0.5 segundos
    }

    /**
     * Tick do jogo Crash.
     */
    private void tickCrash() {
        if (currentCrashGame == null)
            return;

        switch (currentCrashGame.getState()) {
            case WAITING:
                crashCountdown--;
                if (crashCountdown <= 0) {
                    if (currentCrashGame.getParticipantCount() > 0) {
                        currentCrashGame.start();
                        notifyCrashPlayers("&6&lCRASH &aO jogo começou! Faça cashout antes de crashar!");
                    } else {
                        // Resetar countdown se não há jogadores
                        crashCountdown = plugin.getConfig().getInt("casino.crash.round-interval", 30);
                    }
                }
                break;

            case RUNNING:
                if (!currentCrashGame.tick()) {
                    // Crashou!
                    handleCrashEnd();
                }
                break;

            case CRASHED:
                // Iniciar nova rodada
                currentCrashGame = new CrashGame();
                crashCountdown = plugin.getConfig().getInt("casino.crash.round-interval", 30);
                break;
        }
    }

    /**
     * Jogador entra no Crash.
     */
    public boolean joinCrash(Player player, long bet) {
        if (currentCrashGame == null ||
                currentCrashGame.getState() != CrashGame.GameState.WAITING) {
            ChatStorage.sendRaw(player, "&c&lCRASH &fO jogo já está em andamento! Aguarde o próximo.");
            return false;
        }

        if (!validateBet(player, bet)) {
            return false;
        }

        if (currentCrashGame.isParticipant(player.getUniqueId())) {
            ChatStorage.sendRaw(player, "&c&lCRASH &fVocê já está participando!");
            return false;
        }

        plugin.getStatsManager().removeMoney(player, bet);
        currentCrashGame.join(player.getUniqueId(), bet);

        ChatStorage.sendRaw(player, "&6&lCRASH &fVocê entrou com &e" + formatCoins(bet) + " coins&f! Boa sorte!");

        return true;
    }

    /**
     * Jogador sai do Crash (antes de começar).
     */
    public boolean leaveCrash(Player player) {
        if (currentCrashGame == null)
            return false;

        if (currentCrashGame.getState() != CrashGame.GameState.WAITING) {
            ChatStorage.sendRaw(player, "&c&lCRASH &fVocê não pode sair durante o jogo!");
            return false;
        }

        long bet = currentCrashGame.getBet(player.getUniqueId());
        if (currentCrashGame.leave(player.getUniqueId())) {
            plugin.getStatsManager().addMoney(player, bet);
            ChatStorage.sendRaw(player, "&6&lCRASH &fVocê saiu do jogo. Coins devolvidos.");
            return true;
        }

        return false;
    }

    /**
     * Jogador faz cash out no Crash.
     */
    public boolean cashoutCrash(Player player) {
        if (currentCrashGame == null ||
                currentCrashGame.getState() != CrashGame.GameState.RUNNING) {
            return false;
        }

        if (!currentCrashGame.isParticipant(player.getUniqueId())) {
            return false;
        }

        if (currentCrashGame.hasCashedOut(player.getUniqueId())) {
            return false;
        }

        if (currentCrashGame.cashout(player.getUniqueId())) {
            long payout = currentCrashGame.calculatePayout(player.getUniqueId());
            double mult = currentCrashGame.getCashoutMultiplier(player.getUniqueId());
            long bet = currentCrashGame.getBet(player.getUniqueId());

            plugin.getStatsManager().addMoney(player, payout);
            recordWin(player, CasinoGame.CRASH, bet, payout);

            // Atualizar maior multiplicador
            PlayerProfile profile = plugin.getProfileManager().getProfile(player);
            if (profile != null) {
                PlayerData data = profile.getData();
                double current = 0;
                Object obj = data.getCustomData(KEY_CRASH_PREFIX + "biggest_mult");
                if (obj instanceof Number) {
                    current = ((Number) obj).doubleValue();
                }
                if (mult > current) {
                    data.setCustomData(KEY_CRASH_PREFIX + "biggest_mult", mult);
                }
            }

            ChatStorage.sendRaw(player, "&a&lCASHOUT! &fVocê saiu em " + String.format("%.2f", mult) + "x e ganhou &e"
                    + formatCoins(payout) + " coins&f!");

            player.playSound(player.getLocation(), Sound.ORB_PICKUP, 1f, 1.5f);

            return true;
        }

        return false;
    }

    /**
     * Processa o fim do jogo Crash.
     */
    private void handleCrashEnd() {
        String crashMult = String.format("%.2f", currentCrashGame.getCrashPoint());

        // Processar perdedores (quem não fez cashout)
        for (Map.Entry<UUID, Long> entry : currentCrashGame.getParticipants().entrySet()) {
            UUID playerId = entry.getKey();
            long bet = entry.getValue();

            if (!currentCrashGame.hasCashedOut(playerId)) {
                Player player = Bukkit.getPlayer(playerId);
                if (player != null && player.isOnline()) {
                    recordLoss(player, CasinoGame.CRASH, bet);
                    ChatStorage.sendRaw(player, "&c&lCRASH! &fVocê perdeu &e" + formatCoins(bet) + " coins");
                    player.playSound(player.getLocation(), Sound.EXPLODE, 1f, 1f);
                }
            }

            incrementStat(playerId, KEY_CRASH_PREFIX + "played");
        }

        notifyCrashPlayers("&c&l💥 CRASHOU EM " + crashMult + "x! &fQuem não saiu, perdeu!");
    }

    /**
     * Notifica todos os participantes do Crash.
     */
    private void notifyCrashPlayers(String message) {
        if (currentCrashGame == null)
            return;

        for (UUID playerId : currentCrashGame.getParticipants().keySet()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                ChatStorage.sendRaw(player, message);
            }
        }
    }

    // ==================== ESTATÍSTICAS ====================

    /**
     * Registra uma vitória.
     */
    public void recordWin(Player player, CasinoGame game, long bet, long payout) {
        PlayerProfile profile = plugin.getProfileManager().getProfile(player);
        if (profile == null)
            return;

        PlayerData data = profile.getData();

        // Atualizar totais
        addToStat(data, KEY_TOTAL_WON, payout - bet);
        addToStat(data, KEY_TOTAL_WAGERED, bet);
        addToStat(data, KEY_GAMES_PLAYED, 1);

        // Verificar maior vitória
        long biggestWin = getLongStat(data, KEY_BIGGEST_WIN);
        if (payout - bet > biggestWin) {
            data.setCustomData(KEY_BIGGEST_WIN, payout - bet);
        }
    }

    /**
     * Registra uma derrota.
     */
    public void recordLoss(Player player, CasinoGame game, long bet) {
        PlayerProfile profile = plugin.getProfileManager().getProfile(player);
        if (profile == null)
            return;

        PlayerData data = profile.getData();

        // Atualizar totais
        addToStat(data, KEY_TOTAL_LOST, bet);
        addToStat(data, KEY_TOTAL_WAGERED, bet);
        addToStat(data, KEY_GAMES_PLAYED, 1);

        // Atualizar perda diária
        updateDailyLoss(player, bet);
    }

    /**
     * Obtém as estatísticas de um jogador.
     */
    public CasinoStats getPlayerStats(Player player) {
        PlayerProfile profile = plugin.getProfileManager().getProfile(player);
        if (profile == null)
            return new CasinoStats();

        return getPlayerStats(profile.getData());
    }

    /**
     * Obtém as estatísticas de um PlayerData.
     */
    public CasinoStats getPlayerStats(PlayerData data) {
        CasinoStats stats = new CasinoStats();

        stats.setTotalWagered(getLongStat(data, KEY_TOTAL_WAGERED));
        stats.setTotalWon(getLongStat(data, KEY_TOTAL_WON));
        stats.setTotalLost(getLongStat(data, KEY_TOTAL_LOST));
        stats.setGamesPlayed(getIntStat(data, KEY_GAMES_PLAYED));
        stats.setBiggestWin(getLongStat(data, KEY_BIGGEST_WIN));

        stats.setSlotsPlayed(getIntStat(data, KEY_SLOTS_PREFIX + "played"));
        stats.setSlotsWins(getIntStat(data, KEY_SLOTS_PREFIX + "wins"));
        stats.setSlotsJackpots(getIntStat(data, KEY_SLOTS_PREFIX + "jackpots"));

        stats.setRoulettePlayed(getIntStat(data, KEY_ROULETTE_PREFIX + "played"));
        stats.setRouletteWins(getIntStat(data, KEY_ROULETTE_PREFIX + "wins"));

        stats.setBlackjackPlayed(getIntStat(data, KEY_BLACKJACK_PREFIX + "played"));
        stats.setBlackjackWins(getIntStat(data, KEY_BLACKJACK_PREFIX + "wins"));
        stats.setBlackjackNaturals(getIntStat(data, KEY_BLACKJACK_PREFIX + "naturals"));

        stats.setCoinflipPlayed(getIntStat(data, KEY_COINFLIP_PREFIX + "played"));
        stats.setCoinflipWins(getIntStat(data, KEY_COINFLIP_PREFIX + "wins"));

        stats.setCrashPlayed(getIntStat(data, KEY_CRASH_PREFIX + "played"));

        Object biggestMult = data.getCustomData(KEY_CRASH_PREFIX + "biggest_mult");
        if (biggestMult instanceof Number) {
            stats.setCrashBiggestMult(((Number) biggestMult).doubleValue());
        }

        return stats;
    }

    // ==================== MÉTODOS AUXILIARES ====================

    /**
     * Incrementa uma estatística.
     */
    private void incrementStat(Player player, String key) {
        incrementStat(player.getUniqueId(), key);
    }

    private void incrementStat(UUID playerId, String key) {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null)
            return;

        PlayerProfile profile = plugin.getProfileManager().getProfile(player);
        if (profile == null)
            return;

        addToStat(profile.getData(), key, 1);
    }

    private void addToStat(PlayerData data, String key, long amount) {
        long current = getLongStat(data, key);
        data.setCustomData(key, current + amount);
    }

    private long getLongStat(PlayerData data, String key) {
        Object value = data.getCustomData(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return 0;
    }

    private int getIntStat(PlayerData data, String key) {
        Object value = data.getCustomData(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return 0;
    }

    /**
     * Obtém a perda diária do jogador.
     */
    private long getDailyLoss(Player player) {
        PlayerProfile profile = plugin.getProfileManager().getProfile(player);
        if (profile == null)
            return 0;

        PlayerData data = profile.getData();

        // Verificar se é o mesmo dia
        String today = java.time.LocalDate.now().toString();
        String lastDate = (String) data.getCustomData(KEY_DAILY_LOSS_DATE);

        if (!today.equals(lastDate)) {
            // Novo dia, resetar
            data.setCustomData(KEY_DAILY_LOSS, 0L);
            data.setCustomData(KEY_DAILY_LOSS_DATE, today);
            return 0;
        }

        return getLongStat(data, KEY_DAILY_LOSS);
    }

    /**
     * Atualiza a perda diária.
     */
    private void updateDailyLoss(Player player, long amount) {
        PlayerProfile profile = plugin.getProfileManager().getProfile(player);
        if (profile == null)
            return;

        PlayerData data = profile.getData();
        String today = java.time.LocalDate.now().toString();

        data.setCustomData(KEY_DAILY_LOSS_DATE, today);
        addToStat(data, KEY_DAILY_LOSS, amount);
    }

    /**
     * Verifica se o jogador está no spawn.
     */
    private boolean isInSpawn(Player player) {
        return plugin.getArenaItemsHandler().isInLobby(player);
    }

    /**
     * Verifica se o jogador tem uma sessão ativa.
     */
    public boolean hasActiveSession(Player player) {
        return activeSessions.containsKey(player.getUniqueId());
    }

    /**
     * Obtém a sessão ativa do jogador.
     */
    public CasinoSession getActiveSession(Player player) {
        return activeSessions.get(player.getUniqueId());
    }

    /**
     * Remove a sessão de um jogador.
     */
    public void cleanupSession(Player player) {
        CasinoSession session = activeSessions.remove(player.getUniqueId());

        // Se estava em um blackjack, considerar como perda
        if (session != null && session.getGame() == CasinoGame.BLACKJACK) {
            // Perda da aposta (já foi deduzida)
            recordLoss(player, CasinoGame.BLACKJACK, session.getCurrentBet());
        }

        // Cancelar coinflip pendente
        CoinflipRequest coinflip = pendingCoinflips.remove(player.getUniqueId());
        if (coinflip != null) {
            // Devolver aposta
            plugin.getStatsManager().addMoney(player, coinflip.getAmount());
        }
    }

    /**
     * Broadcast de jackpot.
     */
    private void broadcastJackpot(Player player, long amount) {
        if (!plugin.getConfig().getBoolean("casino.broadcasts.jackpot", true))
            return;

        for (Player p : Bukkit.getOnlinePlayers()) {
            ChatStorage.sendRaw(p, "&6&l⭐ JACKPOT! &e&l" + player.getName() + " &fganhou o jackpot de &a&l"
                    + formatCoins(amount) + " coins &fno Slots!");
            p.playSound(p.getLocation(), Sound.FIREWORK_LAUNCH, 0.5f, 1f);
        }
    }

    /**
     * Broadcast público de jackpot (chamado pelo SlotsMenu).
     */
    public void broadcastJackpotPublic(Player player, long amount) {
        broadcastJackpot(player, amount);
    }

    /**
     * Broadcast de coinflip.
     */
    private void broadcastCoinflip(String winner, String loser, long amount) {
        if (!plugin.getConfig().getBoolean("casino.broadcasts.coinflip", true))
            return;

        List<String> lines = ChatStorage.getMessageList("casino.coinflip.broadcast");
        for (String line : lines) {
            line = line.replace("{winner}", winner)
                    .replace("{loser}", loser)
                    .replace("{amount}", formatCoins(amount));
            Bukkit.broadcastMessage(ChatStorage.colorize(line));
        }
    }

    /**
     * Task para limpar coinflips expirados.
     */
    private void startCleanupTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                List<UUID> expired = new ArrayList<>();

                for (Map.Entry<UUID, CoinflipRequest> entry : pendingCoinflips.entrySet()) {
                    if (entry.getValue().isExpired()) {
                        expired.add(entry.getKey());
                    }
                }

                for (UUID playerId : expired) {
                    CoinflipRequest request = pendingCoinflips.remove(playerId);
                    if (request != null) {
                        Player player = Bukkit.getPlayer(playerId);
                        if (player != null && player.isOnline()) {
                            plugin.getStatsManager().addMoney(player, request.getAmount());
                            ChatStorage.sendRaw(player, "&6&lCASSINO &fSeu coinflip expirou. Coins devolvidos.");
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 20L * 30, 20L * 30); // A cada 30 segundos
    }

    /**
     * Formata o valor de coins.
     */
    public String formatCoins(long amount) {
        return DECIMAL_FORMAT.format(amount);
    }

    // ==================== GETTERS ====================

    public boolean isEnabled() {
        return enabled;
    }

    public int getHotbarSlot() {
        return hotbarSlot;
    }

    public long getMinBet() {
        return minBet;
    }

    public long getMaxBet() {
        return maxBet;
    }

    public List<Long> getPresetBets() {
        return presetBets;
    }

    public boolean isAllowCustomBet() {
        return allowCustomBet;
    }

    public CrashGame getCurrentCrashGame() {
        return currentCrashGame;
    }

    public int getCrashCountdown() {
        return crashCountdown;
    }

    public long getJackpotPool() {
        return jackpotPool;
    }

    /**
     * Desliga o manager.
     */
    public void shutdown() {
        if (crashTask != null) {
            crashTask.cancel();
        }

        // Devolver apostas de coinflips pendentes
        for (Map.Entry<UUID, CoinflipRequest> entry : pendingCoinflips.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null && player.isOnline()) {
                plugin.getStatsManager().addMoney(player, entry.getValue().getAmount());
            }
        }
        pendingCoinflips.clear();

        // Devolver apostas do crash
        if (currentCrashGame != null && currentCrashGame.getState() == CrashGame.GameState.WAITING) {
            for (Map.Entry<UUID, Long> entry : currentCrashGame.getParticipants().entrySet()) {
                Player player = Bukkit.getPlayer(entry.getKey());
                if (player != null && player.isOnline()) {
                    plugin.getStatsManager().addMoney(player, entry.getValue());
                }
            }
        }
    }
}
