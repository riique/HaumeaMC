package com.haumea.kitpvp.managers;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.models.EloLeague;
import com.haumea.kitpvp.models.PlayerRank;
import com.haumea.kitpvp.profile.PlayerProfile;
import com.haumea.kitpvp.utils.ChatStorage;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gerenciador central do sistema de Ligas/Elo do HaumeaMC.
 * 
 * Responsável por:
 * - Configurar e gerenciar todas as ligas e divisões
 * - Calcular ganhos/perdas de Elo baseado em combate
 * - Promover/rebaixar jogadores automaticamente
 * - Dar recompensas por promoção de divisão
 * - Exibir títulos e mensagens de rank up
 * - Atualizar nametags com símbolos de liga
 * 
 * @author HaumeaMC
 */
public class LeagueManager {

    private final HaumeaMC plugin;
    private final List<EloLeague> leagues;
    private final Map<UUID, Set<String>> claimedRewards; // Ranks já recompensados

    // Configurações de Elo
    private static final int BASE_ELO_GAIN = 5;
    private static final int BASE_ELO_LOSS = 5;
    private static final int MIN_ELO_GAIN = 1;
    private static final int MAX_ELO_GAIN = 30;
    private static final int STARTING_ELO = 100;

    // Multiplicadores para cálculo de Elo
    private static final double LEAGUE_DIFF_MULTIPLIER = 1.5;
    private static final double KILLSTREAK_BONUS = 0.2;
    private static final double LOW_KD_PENALTY = 0.5;

    /**
     * Construtor do LeagueManager
     */
    public LeagueManager(HaumeaMC plugin) {
        this.plugin = plugin;
        this.leagues = new ArrayList<>();
        this.claimedRewards = new ConcurrentHashMap<>();
        initLeagues();
        plugin.getLogger().info("Sistema de Ligas iniciado com " + getTotalRanks() + " ranks configurados.");
    }

    /**
     * Inicializa todas as ligas do sistema conforme especificação
     */
    private void initLeagues() {
        // Ordem 0-7 (menor para maior)
        // Liga: id, nome, cor, símbolo, eloBase, eloPorDivisao, coins, ordem,
        // temDivisões

        // 1. Primary (Verde Claro - §a)
        leagues.add(new EloLeague("primary", "Primary", "&a", "✥", 100, 100, 20, 0));

        // 2. Bronze (Cinza Escuro - §8)
        leagues.add(new EloLeague("bronze", "Bronze", "&8", "✱", 600, 100, 40, 1));

        // 3. Silver (Cinza - §7)
        leagues.add(new EloLeague("silver", "Silver", "&7", "✶", 1100, 100, 50, 2));

        // 4. Gold (Dourado - §6)
        leagues.add(new EloLeague("gold", "Gold", "&6", "✹", 1600, 100, 80, 3));

        // 5. Diamond (Azul Claro - §b)
        leagues.add(new EloLeague("diamond", "Diamond", "&b", "✦", 2100, 100, 100, 4));

        // 6. Emerald (Verde Escuro - §2)
        leagues.add(new EloLeague("emerald", "Emerald", "&2", "✥", 2600, 100, 120, 5));

        // 7. Master (Vermelho Claro - §c)
        leagues.add(new EloLeague("master", "Master", "&c", "✠", 3100, 100, 140, 6));

        // 8. Legendary (Vermelho Escuro - §4) - SEM DIVISÕES
        leagues.add(new EloLeague("legendary", "Legendary", "&4", "✪", 4000, 0, 500, 7, false));

        // Ordenar por ordem hierárquica
        leagues.sort(Comparator.comparingInt(EloLeague::getOrder));
    }

    // ==================== MÉTODOS DE ELO ====================

    /**
     * Obtém o Elo atual de um jogador
     */
    public int getElo(Player player) {
        PlayerProfile profile = plugin.getProfileManager().getProfile(player);
        if (profile == null)
            return STARTING_ELO;
        return profile.getData().getCustomData("elo", STARTING_ELO);
    }

    /**
     * Obtém o Elo de um jogador por UUID
     */
    public int getElo(UUID uuid) {
        PlayerProfile profile = plugin.getProfileManager().getProfile(uuid);
        if (profile == null)
            return STARTING_ELO;
        return profile.getData().getCustomData("elo", STARTING_ELO);
    }

    /**
     * Define o Elo de um jogador
     */
    public void setElo(Player player, int elo) {
        PlayerProfile profile = plugin.getProfileManager().getProfile(player);
        if (profile != null) {
            profile.getData().setCustomData("elo", Math.max(0, elo));
        }
    }

    /**
     * Adiciona Elo a um jogador com verificação de promoção
     * 
     * @param player Jogador
     * @param amount Quantidade de Elo a adicionar
     * @return true se houve promoção
     */
    public boolean addElo(Player player, int amount) {
        PlayerRank rankBefore = getRank(player);
        int oldElo = getElo(player);
        int newElo = oldElo + amount;
        setElo(player, newElo);

        PlayerRank rankAfter = getRank(player);

        // Verificar promoção
        if (rankAfter.compareTo(rankBefore) > 0) {
            onPromotion(player, rankBefore, rankAfter);
            return true;
        }

        return false;
    }

    /**
     * Remove Elo de um jogador com verificação de rebaixamento
     * 
     * @param player Jogador
     * @param amount Quantidade de Elo a remover
     * @return true se houve rebaixamento
     */
    public boolean removeElo(Player player, int amount) {
        PlayerRank rankBefore = getRank(player);
        int oldElo = getElo(player);
        int newElo = Math.max(0, oldElo - amount);
        setElo(player, newElo);

        PlayerRank rankAfter = getRank(player);

        // Verificar rebaixamento
        if (rankAfter.compareTo(rankBefore) < 0) {
            onDemotion(player, rankBefore, rankAfter);
            return true;
        }

        return false;
    }

    // ==================== MÉTODOS DE RANK ====================

    /**
     * Obtém o rank atual de um jogador (liga + divisão)
     */
    public PlayerRank getRank(Player player) {
        return getRankByElo(getElo(player));
    }

    /**
     * Obtém o rank atual de um jogador por UUID
     */
    public PlayerRank getRank(UUID uuid) {
        return getRankByElo(getElo(uuid));
    }

    /**
     * Converte Elo para PlayerRank
     */
    public PlayerRank getRankByElo(int elo) {
        // Percorre da liga mais alta para a mais baixa
        for (int i = leagues.size() - 1; i >= 0; i--) {
            EloLeague league = leagues.get(i);
            int division = league.getDivisionForElo(elo);
            if (division > 0) {
                return new PlayerRank(league, division, elo);
            }
        }
        // Fallback para Primary I
        return new PlayerRank(leagues.get(0), 1, elo);
    }

    /**
     * Obtém o próximo rank do jogador
     * 
     * @return Próximo PlayerRank ou null se já está no máximo
     */
    public PlayerRank getNextRank(Player player) {
        PlayerRank current = getRank(player);
        return getNextRank(current);
    }

    /**
     * Obtém o próximo rank a partir de um rank atual
     */
    public PlayerRank getNextRank(PlayerRank current) {
        EloLeague league = current.getLeague();
        int division = current.getDivision();

        // Se é Legendary, não há próximo
        if (!league.hasDivisions()) {
            return null;
        }

        // Se pode subir de divisão na mesma liga
        if (division < 5) {
            int nextElo = league.getEloForDivision(division + 1);
            return new PlayerRank(league, division + 1, nextElo);
        }

        // Precisa subir de liga
        int nextLeagueOrder = league.getOrder() + 1;
        for (EloLeague nextLeague : leagues) {
            if (nextLeague.getOrder() == nextLeagueOrder) {
                int nextElo = nextLeague.getEloForDivision(1);
                return new PlayerRank(nextLeague, 1, nextElo);
            }
        }

        return null; // Não há próxima liga
    }

    /**
     * Obtém o Elo necessário para o próximo rank
     */
    public int getEloToNextRank(Player player) {
        PlayerRank next = getNextRank(player);
        if (next == null)
            return 0;

        int currentElo = getElo(player);
        return Math.max(0, next.getRequiredElo() - currentElo);
    }

    /**
     * Obtém o progresso percentual para o próximo rank (0-100)
     */
    public int getProgressPercent(Player player) {
        PlayerRank current = getRank(player);
        PlayerRank next = getNextRank(current);

        if (next == null)
            return 100; // Legendary

        int currentElo = getElo(player);
        int currentReq = current.getRequiredElo();
        int nextReq = next.getRequiredElo();
        int range = nextReq - currentReq;

        if (range <= 0)
            return 100;

        int progress = currentElo - currentReq;
        return Math.min(100, Math.max(0, (progress * 100) / range));
    }

    // ==================== CÁLCULO DE ELO EM COMBATE ====================

    /**
     * Calcula o Elo a ganhar quando um jogador mata outro
     * 
     * Considera:
     * - Diferença de liga entre os jogadores
     * - Killstreak da vítima
     * - KD da vítima
     * 
     * @param killer Jogador que matou
     * @param victim Jogador que morreu
     * @return Elo a ser ganho (limitado entre MIN e MAX)
     */
    public int calculateEloGain(Player killer, Player victim) {
        PlayerRank killerRank = getRank(killer);
        PlayerRank victimRank = getRank(victim);

        // Base de ganho
        double eloGain = BASE_ELO_GAIN;

        // Bônus/penalidade por diferença de liga
        int leagueDiff = victimRank.getTotalOrder() - killerRank.getTotalOrder();
        if (leagueDiff > 0) {
            // Vítima é de liga superior: bônus
            eloGain += leagueDiff * LEAGUE_DIFF_MULTIPLIER;
        } else if (leagueDiff < 0) {
            // Vítima é de liga inferior: redução
            eloGain += leagueDiff * 0.5; // Reduz menos que aumenta
        }

        // Bônus por killstreak da vítima
        StatsManager statsManager = plugin.getStatsManager();
        int victimStreak = statsManager.getKillstreak(victim);
        if (victimStreak > 0) {
            eloGain += victimStreak * KILLSTREAK_BONUS;
        }

        // Penalidade se vítima tem KD muito baixo (< 0.5)
        double victimKd = statsManager.getKdr(victim);
        if (victimKd < 0.5 && victimKd > 0) {
            eloGain *= LOW_KD_PENALTY;
        }

        // Aplicar limites
        int finalElo = (int) Math.round(eloGain);
        return Math.min(MAX_ELO_GAIN, Math.max(MIN_ELO_GAIN, finalElo));
    }

    /**
     * Calcula o Elo a perder quando um jogador morre
     * 
     * @param victim Jogador que morreu
     * @return Elo a ser perdido
     */
    public int calculateEloLoss(Player victim) {
        int currentElo = getElo(victim);

        // Não perde se Elo atual <= perda base
        if (currentElo <= BASE_ELO_LOSS) {
            return Math.max(0, currentElo - 1);
        }

        return BASE_ELO_LOSS;
    }

    // ==================== EVENTOS DE PROMOÇÃO/REBAIXAMENTO ====================

    /**
     * Chamado quando um jogador é promovido de rank
     */
    private void onPromotion(Player player, PlayerRank oldRank, PlayerRank newRank) {
        String rankKey = newRank.getLeague().getId() + "_" + newRank.getDivision();

        // Verificar se já recebeu recompensa deste rank
        Set<String> claimed = claimedRewards.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>());

        if (!claimed.contains(rankKey)) {
            // Dar recompensa
            int coins = newRank.getCoinsReward();
            plugin.getStatsManager().addMoney(player, coins);
            claimed.add(rankKey);

            // Mensagem de recompensa
            ChatStorage.send(player, "league.reward",
                    "coins", String.valueOf(coins));
        }

        // Título na tela
        sendPromotionTitle(player, newRank);

        // Mensagem no chat
        ChatStorage.send(player, "league.promoted",
                "league", newRank.getFormattedName());

        // Broadcast global
        String broadcast = ChatStorage.getMessage("league.promoted-broadcast",
                "player", player.getName(),
                "league", newRank.getFormattedName());
        Bukkit.broadcastMessage(broadcast);

        // Som de level up
        player.playSound(player.getLocation(), Sound.LEVEL_UP, 1.0f, 1.0f);

        // Atualizar nametag
        updatePlayerNametag(player);
    }

    /**
     * Chamado quando um jogador é rebaixado de rank
     */
    private void onDemotion(Player player, PlayerRank oldRank, PlayerRank newRank) {
        // Mensagem no chat
        ChatStorage.send(player, "league.demoted",
                "league", newRank.getFormattedName());

        // Som de falha
        player.playSound(player.getLocation(), Sound.ANVIL_BREAK, 0.5f, 1.0f);

        // Atualizar nametag
        updatePlayerNametag(player);
    }

    /**
     * Envia título de promoção na tela do jogador
     */
    private void sendPromotionTitle(Player player, PlayerRank newRank) {
        String title = "§a§lPROMOVIDO!";
        String subtitle = "§fVocê alcançou " + newRank.getFormattedName();

        // Usar reflection ou API nativa do 1.8 para Title
        try {
            player.sendTitle(title, subtitle);
        } catch (NoSuchMethodError e) {
            // Fallback para servers sem o método (1.8 antigo)
            player.sendMessage(title);
            player.sendMessage(subtitle);
        }
    }

    /**
     * Atualiza a nametag do jogador com o símbolo da liga
     */
    public void updatePlayerNametag(Player player) {
        // O NametagManager é agora o responsável central por nametags
        if (plugin.getNametagManager() != null) {
            plugin.getNametagManager().updateNametag(player);
        } else if (plugin.getTabManager() != null) {
            // Fallback para TabManager
            plugin.getTabManager().updatePlayerTeam(player);
        }
    }

    // ==================== MÉTODOS DE LIGA ====================

    /**
     * Obtém todas as ligas configuradas
     */
    public List<EloLeague> getAllLeagues() {
        return new ArrayList<>(leagues);
    }

    /**
     * Obtém uma liga pelo ID
     */
    public EloLeague getLeagueById(String id) {
        for (EloLeague league : leagues) {
            if (league.getId().equalsIgnoreCase(id)) {
                return league;
            }
        }
        return null;
    }

    /**
     * Obtém o total de ranks (ligas * divisões)
     */
    public int getTotalRanks() {
        int total = 0;
        for (EloLeague league : leagues) {
            total += league.hasDivisions() ? 5 : 1;
        }
        return total;
    }

    /**
     * Obtém todos os ranks possíveis em ordem
     */
    public List<PlayerRank> getAllRanks() {
        List<PlayerRank> allRanks = new ArrayList<>();
        for (EloLeague league : leagues) {
            if (league.hasDivisions()) {
                for (int div = 1; div <= 5; div++) {
                    int elo = league.getEloForDivision(div);
                    allRanks.add(new PlayerRank(league, div, elo));
                }
            } else {
                allRanks.add(new PlayerRank(league, 1, league.getBaseElo()));
            }
        }
        return allRanks;
    }

    // ==================== GETTERS ====================

    public int getStartingElo() {
        return STARTING_ELO;
    }

    public int getBaseEloGain() {
        return BASE_ELO_GAIN;
    }

    public int getBaseEloLoss() {
        return BASE_ELO_LOSS;
    }

    // ==================== PERSISTÊNCIA DE RECOMPENSAS ====================

    private static final String DATA_KEY_CLAIMED_REWARDS = "league_claimed_rewards";

    /**
     * Carrega as recompensas já coletadas de um jogador do customData.
     * Chamado quando o jogador entra no servidor.
     * 
     * @param player Jogador
     */
    public void loadClaimedRewards(Player player) {
        PlayerProfile profile = plugin.getProfileManager().getProfile(player);
        if (profile == null)
            return;

        Set<String> claimed = new HashSet<>();

        // Carregar do customData
        Object data = profile.getData().getCustomData(DATA_KEY_CLAIMED_REWARDS);
        if (data instanceof List) {
            List<?> list = (List<?>) data;
            for (Object item : list) {
                if (item instanceof String) {
                    claimed.add((String) item);
                }
            }
        }

        claimedRewards.put(player.getUniqueId(), claimed);
    }

    /**
     * Salva as recompensas já coletadas de um jogador no customData.
     * 
     * @param player Jogador
     */
    public void saveClaimedRewards(Player player) {
        PlayerProfile profile = plugin.getProfileManager().getProfile(player);
        if (profile == null)
            return;

        Set<String> claimed = claimedRewards.get(player.getUniqueId());
        if (claimed == null) {
            claimed = new HashSet<>();
        }

        // Converter para lista para salvar
        List<String> claimedList = new ArrayList<>(claimed);
        profile.getData().setCustomData(DATA_KEY_CLAIMED_REWARDS, claimedList);
    }

    /**
     * Remove o cache do jogador quando ele sai.
     * 
     * @param player Jogador
     */
    public void onPlayerQuit(Player player) {
        saveClaimedRewards(player);
        claimedRewards.remove(player.getUniqueId());
    }
}
