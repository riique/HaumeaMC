package com.haumea.kitpvp.managers;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.models.DuelArena;
import com.haumea.kitpvp.models.DuelMatch;
import com.haumea.kitpvp.models.DuelSettings;
import com.haumea.kitpvp.models.Warp;
import com.haumea.kitpvp.profile.PlayerProfile;
import com.haumea.kitpvp.utils.ChatStorage;
import com.haumea.kitpvp.utils.ItemBuilder;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gerenciador Central do Sistema de Duelos 1v1 do HaumeaMC.
 * 
 * Este manager é responsável por TODA a orquestração do sistema de duelos:
 * - Fila de matchmaking
 * - Duelos ativos
 * - Desafios entre jogadores
 * - Arenas de duelo
 * - Configurações de duelo
 * - Eventos de combate dentro do duelo
 * - Estatísticas de 1v1
 * 
 * ARQUITETURA: Manager único centralizado conforme padrão do projeto.
 * 
 * @author HaumeaMC
 */
public class DuelManager {

    private final HaumeaMC plugin;

    // ==================== ARENAS ====================

    /** Cache de arenas carregadas */
    private final Map<String, DuelArena> arenas;

    // ==================== FILA DE MATCHMAKING ====================

    /** Jogadores na fila de matchmaking (ordem de entrada) */
    private final LinkedList<UUID> queue;

    /** Configurações escolhidas pelos jogadores na fila */
    private final Map<UUID, DuelSettings> queueSettings;

    /** Timestamp de entrada na fila */
    private final Map<UUID, Long> queueJoinTime;

    // ==================== DUELOS ATIVOS ====================

    /** Duelos ativos por ID */
    private final Map<UUID, DuelMatch> activeMatches;

    /** Mapeamento jogador -> duelo ativo */
    private final Map<UUID, UUID> playerToMatch;

    // ==================== DESAFIOS PENDENTES ====================

    /** Desafios pendentes: destinatário -> remetente */
    private final Map<UUID, UUID> pendingChallenges;

    /** Configurações dos desafios pendentes */
    private final Map<UUID, DuelSettings> challengeSettings;

    /** Timestamp de quando o desafio foi enviado */
    private final Map<UUID, Long> challengeTimestamps;

    // ==================== CONFIGURAÇÕES ====================

    /** Tempo de expiração de desafios em segundos */
    private static final int CHALLENGE_EXPIRE_SECONDS = 30;

    /** Tempo de countdown antes do duelo em segundos */
    private static final int COUNTDOWN_SECONDS = 5;

    /** Task de matchmaking */
    private BukkitTask matchmakingTask;

    /** Task de limpeza de desafios expirados */
    private BukkitTask cleanupTask;

    // ==================== ARQUIVOS ====================

    private final File arenasFile;
    private FileConfiguration arenasConfig;

    // ==================== CONSTRUTOR ====================

    public DuelManager(HaumeaMC plugin) {
        this.plugin = plugin;

        // Inicializar coleções
        this.arenas = new ConcurrentHashMap<>();
        this.queue = new LinkedList<>();
        this.queueSettings = new ConcurrentHashMap<>();
        this.queueJoinTime = new ConcurrentHashMap<>();
        this.activeMatches = new ConcurrentHashMap<>();
        this.playerToMatch = new ConcurrentHashMap<>();
        this.pendingChallenges = new ConcurrentHashMap<>();
        this.challengeSettings = new ConcurrentHashMap<>();
        this.challengeTimestamps = new ConcurrentHashMap<>();

        // Carregar arenas
        this.arenasFile = new File(plugin.getDataFolder(), "duel_arenas.yml");
        loadArenas();

        // Iniciar tasks
        startMatchmakingTask();
        startCleanupTask();

        plugin.getLogger().info("DuelManager inicializado com " + arenas.size() + " arena(s).");
    }

    // ==================== FILA DE MATCHMAKING ====================

    /**
     * Adiciona um jogador à fila de matchmaking.
     * 
     * @param player   Jogador a adicionar
     * @param settings Configurações desejadas
     * @return true se entrou na fila com sucesso
     */
    public boolean joinQueue(Player player, DuelSettings settings) {
        UUID uuid = player.getUniqueId();

        // Verificar se já está na fila
        if (isInQueue(player)) {
            ChatStorage.sendCustom(player, "§cVocê já está na fila!");
            return false;
        }

        // Verificar se está em duelo
        if (isInDuel(player)) {
            ChatStorage.sendCustom(player, "§cVocê já está em um duelo!");
            return false;
        }

        // Verificar se tem desafio pendente
        if (hasPendingChallenge(player)) {
            ChatStorage.sendCustom(player, "§cVocê tem um desafio pendente! Use §e/duel deny §cpara cancelar.");
            return false;
        }

        // Verificar se está em combate na arena principal
        if (plugin.getStateManager().isInCombat(player)) {
            ChatStorage.sendCustom(player, "§cVocê está em combate!");
            return false;
        }

        // Adicionar à fila
        queue.add(uuid);
        queueSettings.put(uuid, settings != null ? settings : new DuelSettings());
        queueJoinTime.put(uuid, System.currentTimeMillis());

        // Feedback
        ChatStorage.sendCustom(player, "§aVocê entrou na fila de duelos!");
        ChatStorage.sendCustom(player, "§7Jogadores na fila: §e" + queue.size());
        ChatStorage.sendCustom(player, "§7Use §e/duel sair §7para sair da fila.");

        player.playSound(player.getLocation(), Sound.NOTE_PLING, 1.0f, 1.5f);

        return true;
    }

    /**
     * Remove um jogador da fila de matchmaking.
     * 
     * @param player Jogador a remover
     * @return true se foi removido com sucesso
     */
    public boolean leaveQueue(Player player) {
        UUID uuid = player.getUniqueId();

        if (!isInQueue(player)) {
            ChatStorage.sendCustom(player, "§cVocê não está na fila!");
            return false;
        }

        queue.remove(uuid);
        queueSettings.remove(uuid);
        queueJoinTime.remove(uuid);

        ChatStorage.sendCustom(player, "§cVocê saiu da fila de duelos.");
        player.playSound(player.getLocation(), Sound.NOTE_BASS, 1.0f, 0.5f);

        return true;
    }

    /**
     * Verifica se um jogador está na fila.
     */
    public boolean isInQueue(Player player) {
        return player != null && queue.contains(player.getUniqueId());
    }

    /**
     * Verifica se um jogador está na fila por UUID.
     */
    public boolean isInQueue(UUID uuid) {
        return uuid != null && queue.contains(uuid);
    }

    /**
     * Obtém o tamanho atual da fila.
     */
    public int getQueueSize() {
        return queue.size();
    }

    /**
     * Obtém a posição de um jogador na fila (1-indexed).
     */
    public int getQueuePosition(Player player) {
        if (player == null)
            return -1;
        int index = queue.indexOf(player.getUniqueId());
        return index >= 0 ? index + 1 : -1;
    }

    /**
     * Obtém o tempo de espera de um jogador na fila em segundos.
     */
    public int getQueueWaitTime(Player player) {
        if (player == null)
            return 0;
        Long joinTime = queueJoinTime.get(player.getUniqueId());
        if (joinTime == null)
            return 0;
        return (int) ((System.currentTimeMillis() - joinTime) / 1000);
    }

    // ==================== SISTEMA DE DESAFIOS ====================

    /**
     * Envia um desafio de duelo para outro jogador.
     * 
     * @param challenger Jogador que está desafiando
     * @param target     Jogador alvo
     * @param settings   Configurações do duelo
     * @return true se o desafio foi enviado com sucesso
     */
    public boolean sendChallenge(Player challenger, Player target, DuelSettings settings) {
        if (challenger == null || target == null)
            return false;

        // Verificar se está desafiando a si mesmo
        if (challenger.equals(target)) {
            ChatStorage.sendCustom(challenger, "§cVocê não pode desafiar a si mesmo!");
            return false;
        }

        // Verificar se o desafiante está ocupado
        if (isInQueue(challenger) || isInDuel(challenger)) {
            ChatStorage.sendCustom(challenger, "§cVocê não pode desafiar enquanto está na fila ou em duelo!");
            return false;
        }

        // Verificar se o alvo está ocupado
        if (isInQueue(target) || isInDuel(target)) {
            ChatStorage.sendCustom(challenger, "§c" + target.getName() + " está ocupado(a)!");
            return false;
        }

        // Verificar se já existe um desafio pendente
        UUID targetUuid = target.getUniqueId();
        if (pendingChallenges.containsKey(targetUuid)) {
            ChatStorage.sendCustom(challenger, "§c" + target.getName() + " já tem um desafio pendente!");
            return false;
        }

        // Verificar se o desafiante já tem desafio pendente para alguém
        for (UUID targetId : pendingChallenges.keySet()) {
            if (pendingChallenges.get(targetId).equals(challenger.getUniqueId())) {
                ChatStorage.sendCustom(challenger, "§cVocê já enviou um desafio! Aguarde a resposta.");
                return false;
            }
        }

        // Verificar combate
        if (plugin.getStateManager().isInCombat(challenger) || plugin.getStateManager().isInCombat(target)) {
            ChatStorage.sendCustom(challenger, "§cUm dos jogadores está em combate!");
            return false;
        }

        // Registrar desafio
        pendingChallenges.put(targetUuid, challenger.getUniqueId());
        challengeSettings.put(targetUuid, settings != null ? settings : new DuelSettings());
        challengeTimestamps.put(targetUuid, System.currentTimeMillis());

        // Notificar desafiante
        ChatStorage.sendCustom(challenger, "§aDesafio enviado para §e" + target.getName() + "§a!");
        ChatStorage.sendCustom(challenger, "§7Aguardando resposta... (expira em " + CHALLENGE_EXPIRE_SECONDS + "s)");

        // Notificar alvo com botões clicáveis
        sendChallengeNotification(target, challenger, settings);

        challenger.playSound(challenger.getLocation(), Sound.NOTE_PLING, 1.0f, 1.5f);
        target.playSound(target.getLocation(), Sound.NOTE_PLING, 1.0f, 2.0f);

        return true;
    }

    /**
     * Envia a notificação de desafio com botões clicáveis.
     */
    private void sendChallengeNotification(Player target, Player challenger, DuelSettings settings) {
        target.sendMessage("");
        target.sendMessage("§8§m--------------------------------------------");
        target.sendMessage("");
        target.sendMessage("  §e§l⚔ DESAFIO DE DUELO!");
        target.sendMessage("");
        target.sendMessage("  §f" + challenger.getName() + " §7te desafiou para um 1v1!");
        if (settings != null) {
            target.sendMessage("  §7Configurações: " + settings.getSummary());
        }
        target.sendMessage("");

        // Criar botões clicáveis
        TextComponent acceptBtn = new TextComponent("  §a§l[ACEITAR]");
        acceptBtn.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/duel accept"));
        acceptBtn.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder("§aClique para aceitar o desafio!").create()));

        TextComponent space = new TextComponent("  ");

        TextComponent denyBtn = new TextComponent("§c§l[RECUSAR]");
        denyBtn.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/duel deny"));
        denyBtn.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder("§cClique para recusar o desafio!").create()));

        target.spigot().sendMessage(acceptBtn, space, denyBtn);

        target.sendMessage("");
        target.sendMessage("  §7Expira em §e" + CHALLENGE_EXPIRE_SECONDS + " segundos§7!");
        target.sendMessage("");
        target.sendMessage("§8§m--------------------------------------------");
        target.sendMessage("");
    }

    /**
     * Aceita um desafio de duelo.
     * 
     * @param player Jogador que está aceitando
     * @return true se o duelo foi iniciado com sucesso
     */
    public boolean acceptChallenge(Player player) {
        UUID playerUuid = player.getUniqueId();

        if (!pendingChallenges.containsKey(playerUuid)) {
            ChatStorage.sendCustom(player, "§cVocê não tem nenhum desafio pendente!");
            return false;
        }

        UUID challengerUuid = pendingChallenges.get(playerUuid);
        Player challenger = Bukkit.getPlayer(challengerUuid);

        if (challenger == null || !challenger.isOnline()) {
            ChatStorage.sendCustom(player, "§cO desafiante não está mais online!");
            clearChallenge(playerUuid);
            return false;
        }

        // Verificar se o desafiante ficou ocupado
        if (isInQueue(challenger) || isInDuel(challenger)) {
            ChatStorage.sendCustom(player, "§cO desafiante agora está ocupado!");
            clearChallenge(playerUuid);
            return false;
        }

        DuelSettings settings = challengeSettings.get(playerUuid);
        clearChallenge(playerUuid);

        // Iniciar duelo
        return startDuel(challenger, player, settings);
    }

    /**
     * Recusa um desafio de duelo.
     * 
     * @param player Jogador que está recusando
     * @return true se o desafio foi recusado
     */
    public boolean denyChallenge(Player player) {
        UUID playerUuid = player.getUniqueId();

        if (!pendingChallenges.containsKey(playerUuid)) {
            ChatStorage.sendCustom(player, "§cVocê não tem nenhum desafio pendente!");
            return false;
        }

        UUID challengerUuid = pendingChallenges.get(playerUuid);
        Player challenger = Bukkit.getPlayer(challengerUuid);

        clearChallenge(playerUuid);

        ChatStorage.sendCustom(player, "§cVocê recusou o desafio.");

        if (challenger != null && challenger.isOnline()) {
            ChatStorage.sendCustom(challenger, "§c" + player.getName() + " recusou seu desafio.");
            challenger.playSound(challenger.getLocation(), Sound.VILLAGER_NO, 1.0f, 1.0f);
        }

        player.playSound(player.getLocation(), Sound.NOTE_BASS, 1.0f, 0.5f);

        return true;
    }

    /**
     * Verifica se um jogador tem desafio pendente.
     */
    public boolean hasPendingChallenge(Player player) {
        return player != null && pendingChallenges.containsKey(player.getUniqueId());
    }

    /**
     * Limpa um desafio pendente.
     */
    private void clearChallenge(UUID targetUuid) {
        pendingChallenges.remove(targetUuid);
        challengeSettings.remove(targetUuid);
        challengeTimestamps.remove(targetUuid);
    }

    // ==================== GERENCIAMENTO DE DUELOS ====================

    /**
     * Inicia um duelo entre dois jogadores.
     * 
     * @param player1  Primeiro jogador
     * @param player2  Segundo jogador
     * @param settings Configurações do duelo
     * @return true se o duelo foi iniciado com sucesso
     */
    public boolean startDuel(Player player1, Player player2, DuelSettings settings) {
        // Encontrar arena disponível
        DuelArena arena = findAvailableArena();
        if (arena == null) {
            ChatStorage.sendCustom(player1, "§cNenhuma arena disponível no momento!");
            ChatStorage.sendCustom(player2, "§cNenhuma arena disponível no momento!");
            return false;
        }

        // Remover da fila se estiverem
        queue.remove(player1.getUniqueId());
        queue.remove(player2.getUniqueId());
        queueSettings.remove(player1.getUniqueId());
        queueSettings.remove(player2.getUniqueId());
        queueJoinTime.remove(player1.getUniqueId());
        queueJoinTime.remove(player2.getUniqueId());

        // Marcar arena como em uso
        arena.setInUse(true);

        // Criar match
        DuelMatch match = new DuelMatch(player1, player2, arena, settings != null ? settings : new DuelSettings());
        activeMatches.put(match.getMatchId(), match);
        playerToMatch.put(player1.getUniqueId(), match.getMatchId());
        playerToMatch.put(player2.getUniqueId(), match.getMatchId());

        // Teleportar jogadores
        Location spawn1 = arena.getSpawn1();
        Location spawn2 = arena.getSpawn2();

        if (spawn1 != null)
            player1.teleport(spawn1);
        if (spawn2 != null)
            player2.teleport(spawn2);

        // Preparar inventários
        prepareDuelInventory(player1, match.getSettings());
        prepareDuelInventory(player2, match.getSettings());

        // Aplicar isolamento visual - jogadores em duelo só veem um ao outro
        applyVisualIsolation(player1, player2);

        // Congelar jogadores
        player1.setWalkSpeed(0);
        player2.setWalkSpeed(0);

        // Iniciar countdown
        startCountdown(match);

        return true;
    }

    /**
     * Inicia o countdown antes do duelo.
     */
    private void startCountdown(DuelMatch match) {
        Player player1 = match.getPlayer1();
        Player player2 = match.getPlayer2();

        new BukkitRunnable() {
            int countdown = COUNTDOWN_SECONDS;

            @Override
            public void run() {
                if (match.isEnded()) {
                    cancel();
                    return;
                }

                // Verificar se os jogadores ainda estão online
                Player p1 = match.getPlayer1();
                Player p2 = match.getPlayer2();

                if (p1 == null || !p1.isOnline() || p2 == null || !p2.isOnline()) {
                    handlePlayerLeave(match,
                            p1 == null || !p1.isOnline() ? match.getPlayer1Id() : match.getPlayer2Id());
                    cancel();
                    return;
                }

                if (countdown > 0) {
                    // Mostrar countdown
                    String color = countdown <= 3 ? "§c" : (countdown <= 5 ? "§e" : "§a");

                    p1.sendTitle(color + countdown, "§7Prepare-se!");
                    p2.sendTitle(color + countdown, "§7Prepare-se!");

                    p1.playSound(p1.getLocation(), Sound.NOTE_PLING, 1.0f, 1.0f);
                    p2.playSound(p2.getLocation(), Sound.NOTE_PLING, 1.0f, 1.0f);

                    countdown--;
                } else {
                    // Começar duelo
                    match.setState(DuelMatch.MatchState.FIGHTING);

                    // Descongelar jogadores
                    p1.setWalkSpeed(0.2f);
                    p2.setWalkSpeed(0.2f);

                    // Título de início
                    p1.sendTitle("§a§lLUTE!", "§e" + p2.getName());
                    p2.sendTitle("§a§lLUTE!", "§e" + p1.getName());

                    p1.playSound(p1.getLocation(), Sound.LEVEL_UP, 1.0f, 1.0f);
                    p2.playSound(p2.getLocation(), Sound.LEVEL_UP, 1.0f, 1.0f);

                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    /**
     * Prepara o inventário do jogador para o duelo.
     */
    private void prepareDuelInventory(Player player, DuelSettings settings) {
        PlayerInventory inv = player.getInventory();
        inv.clear();
        inv.setArmorContents(null);

        // Curar jogador
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setSaturation(20f);

        // Remover efeitos
        player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));

        // Espada
        ItemBuilder swordBuilder = new ItemBuilder(settings.getSwordType().getMaterial())
                .name("§fEspada de " + settings.getSwordType().getDisplayName())
                .unbreakable();

        if (settings.hasEnchantments()) {
            swordBuilder.enchant(Enchantment.DAMAGE_ALL, 1);
        }
        inv.setItem(0, swordBuilder.build());

        // Armadura
        DuelSettings.ArmorType armor = settings.getArmorType();
        ItemStack helmet = new ItemBuilder(armor.getHelmet()).unbreakable().build();
        ItemStack chestplate = new ItemBuilder(armor.getChestplate()).unbreakable().build();
        ItemStack leggings = new ItemBuilder(armor.getLeggings()).unbreakable().build();
        ItemStack boots = new ItemBuilder(armor.getBoots()).unbreakable().build();

        if (settings.hasEnchantments()) {
            helmet = new ItemBuilder(armor.getHelmet()).unbreakable()
                    .enchant(Enchantment.PROTECTION_ENVIRONMENTAL, 1).build();
            chestplate = new ItemBuilder(armor.getChestplate()).unbreakable()
                    .enchant(Enchantment.PROTECTION_ENVIRONMENTAL, 1).build();
            leggings = new ItemBuilder(armor.getLeggings()).unbreakable()
                    .enchant(Enchantment.PROTECTION_ENVIRONMENTAL, 1).build();
            boots = new ItemBuilder(armor.getBoots()).unbreakable()
                    .enchant(Enchantment.PROTECTION_ENVIRONMENTAL, 1).build();
        }

        inv.setHelmet(helmet);
        inv.setChestplate(chestplate);
        inv.setLeggings(leggings);
        inv.setBoots(boots);

        // Recraft (materiais para craft de sopas)
        if (settings.hasRecraft()) {
            inv.setItem(13, new ItemStack(Material.BOWL, 24));
            inv.setItem(14, new ItemStack(Material.RED_MUSHROOM, 24));
            inv.setItem(15, new ItemStack(Material.BROWN_MUSHROOM, 24));
        }

        // Sopas
        ItemStack soup = ItemBuilder.mushroomSoup().build();
        int soupAmount = settings.getSoupMode().isUnlimited() ? 32 : settings.getSoupMode().getAmount();
        int addedSoups = 0;

        for (int slot = 0; slot < 36 && addedSoups < soupAmount; slot++) {
            if (inv.getItem(slot) == null) {
                inv.setItem(slot, soup.clone());
                addedSoups++;
            }
        }

        player.updateInventory();
    }

    /**
     * Finaliza um duelo com um vencedor.
     * 
     * @param winner Vencedor do duelo
     * @param loser  Perdedor do duelo
     */
    public void endDuel(Player winner, Player loser) {
        UUID winnerUuid = winner != null ? winner.getUniqueId() : null;
        UUID loserUuid = loser != null ? loser.getUniqueId() : null;

        // Encontrar o match
        UUID matchId = winnerUuid != null ? playerToMatch.get(winnerUuid)
                : (loserUuid != null ? playerToMatch.get(loserUuid) : null);

        if (matchId == null)
            return;

        DuelMatch match = activeMatches.get(matchId);
        if (match == null || match.isEnded())
            return;

        // Finalizar match
        match.end(winner);

        // Limpar mapeamentos
        playerToMatch.remove(match.getPlayer1Id());
        playerToMatch.remove(match.getPlayer2Id());

        // Atualizar estatísticas
        if (winner != null) {
            updateDuelStats(winner.getUniqueId(), true);
        }
        if (loser != null) {
            updateDuelStats(loser.getUniqueId(), false);
        }

        // Mensagens
        String duration = match.getFormattedDuration();

        if (winner != null && winner.isOnline()) {
            winner.sendTitle("§a§lVITÓRIA!", "§7Duração: " + duration);
            winner.playSound(winner.getLocation(), Sound.LEVEL_UP, 1.0f, 1.5f);
            ChatStorage.sendCustom(winner, "§aVocê venceu o duelo contra §e" +
                    (loser != null ? loser.getName() : "???") + "§a!");
        }

        if (loser != null && loser.isOnline()) {
            loser.sendTitle("§c§lDERROTA!", "§7Duração: " + duration);
            loser.playSound(loser.getLocation(), Sound.VILLAGER_DEATH, 1.0f, 1.0f);
            ChatStorage.sendCustom(loser, "§cVocê perdeu o duelo contra §e" +
                    (winner != null ? winner.getName() : "???") + "§c.");
        }

        // Teleportar de volta ao lobby após 3 segundos
        new BukkitRunnable() {
            @Override
            public void run() {
                teleportToLobby(match.getPlayer1());
                teleportToLobby(match.getPlayer2());

                // Remover match depois do teleporte
                activeMatches.remove(matchId);
            }
        }.runTaskLater(plugin, 60L);
    }

    /**
     * Trata a saída de um jogador do duelo (disconnect/morte).
     */
    public void handlePlayerLeave(DuelMatch match, UUID leaverId) {
        if (match == null || match.isEnded())
            return;

        UUID winnerId = match.getOpponent(leaverId);
        Player winner = winnerId != null ? Bukkit.getPlayer(winnerId) : null;
        Player leaver = Bukkit.getPlayer(leaverId);

        endDuel(winner, leaver);
    }

    /**
     * Teleporta o jogador de volta ao lobby/spawn.
     */
    private void teleportToLobby(Player player) {
        if (player == null || !player.isOnline())
            return;

        // Restaurar velocidade
        player.setWalkSpeed(0.2f);

        // Restaurar visibilidade normal
        restoreVisibility(player);

        // Curar
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);

        // Verificar warp do lobby de 1v1
        Warp lobbyWarp = plugin.getWarpsManager().getWarp("1v1");
        if (lobbyWarp == null) {
            lobbyWarp = plugin.getWarpsManager().getWarp("spawn");
        }

        if (lobbyWarp != null && lobbyWarp.isValid()) {
            player.teleport(lobbyWarp.toLocation());
        }

        // Dar itens de lobby
        giveDuelLobbyItems(player);
    }

    /**
     * Verifica se um jogador está em um duelo ativo.
     */
    public boolean isInDuel(Player player) {
        return player != null && playerToMatch.containsKey(player.getUniqueId());
    }

    /**
     * Verifica se um jogador está em duelo por UUID.
     */
    public boolean isInDuel(UUID uuid) {
        return uuid != null && playerToMatch.containsKey(uuid);
    }

    /**
     * Obtém o match de um jogador.
     */
    public DuelMatch getPlayerMatch(Player player) {
        if (player == null)
            return null;
        UUID matchId = playerToMatch.get(player.getUniqueId());
        return matchId != null ? activeMatches.get(matchId) : null;
    }

    /**
     * Força o fim de um duelo (admin).
     */
    public boolean forceEndDuel(UUID matchId) {
        DuelMatch match = activeMatches.get(matchId);
        if (match == null)
            return false;

        match.cancel();

        Player p1 = match.getPlayer1();
        Player p2 = match.getPlayer2();

        playerToMatch.remove(match.getPlayer1Id());
        playerToMatch.remove(match.getPlayer2Id());

        if (p1 != null && p1.isOnline()) {
            ChatStorage.sendCustom(p1, "§cO duelo foi encerrado por um administrador.");
            teleportToLobby(p1);
        }
        if (p2 != null && p2.isOnline()) {
            ChatStorage.sendCustom(p2, "§cO duelo foi encerrado por um administrador.");
            teleportToLobby(p2);
        }

        activeMatches.remove(matchId);
        return true;
    }

    // ==================== ISOLAMENTO VISUAL ====================

    /**
     * Aplica isolamento visual para jogadores em duelo.
     * Jogadores em duelo só veem um ao outro.
     * 
     * @param player1 Primeiro duelista
     * @param player2 Segundo duelista
     */
    private void applyVisualIsolation(Player player1, Player player2) {
        // Para cada jogador online
        for (Player online : Bukkit.getOnlinePlayers()) {
            // Se é o oponente, mostrar
            if (online.equals(player2)) {
                player1.showPlayer(player2);
            } else if (online.equals(player1)) {
                player2.showPlayer(player1);
            } else {
                // Esconder outros jogadores dos duelistas
                player1.hidePlayer(online);
                player2.hidePlayer(online);
                // Esconder duelistas dos outros jogadores
                online.hidePlayer(player1);
                online.hidePlayer(player2);
            }
        }

        plugin.getLogger().info("Isolamento visual aplicado para duelo: " +
                player1.getName() + " vs " + player2.getName());
    }

    /**
     * Restaura a visibilidade normal de um jogador após o duelo.
     * 
     * @param player Jogador a restaurar visibilidade
     */
    private void restoreVisibility(Player player) {
        if (player == null || !player.isOnline())
            return;

        // Mostrar todos os jogadores para este jogador
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.equals(player)) {
                // Mostrar este jogador para os outros
                online.showPlayer(player);
                // Mostrar outros para este jogador
                player.showPlayer(online);
            }
        }

        plugin.getLogger().info("Visibilidade restaurada para: " + player.getName());
    }

    /**
     * Esconde os duelistas de um jogador que acabou de entrar.
     * Chamado pelo DuelListener no PlayerJoinEvent.
     * 
     * @param joiningPlayer Jogador que está entrando
     */
    public void hideActiveDuelistsFrom(Player joiningPlayer) {
        if (joiningPlayer == null)
            return;

        for (DuelMatch match : activeMatches.values()) {
            if (match.isEnded())
                continue;

            Player p1 = match.getPlayer1();
            Player p2 = match.getPlayer2();

            if (p1 != null && p1.isOnline()) {
                joiningPlayer.hidePlayer(p1);
                p1.hidePlayer(joiningPlayer);
            }
            if (p2 != null && p2.isOnline()) {
                joiningPlayer.hidePlayer(p2);
                p2.hidePlayer(joiningPlayer);
            }
        }
    }

    // ==================== ARENAS ====================

    /**
     * Encontra uma arena disponível.
     */
    public DuelArena findAvailableArena() {
        for (DuelArena arena : arenas.values()) {
            if (arena.isAvailable()) {
                return arena;
            }
        }
        return null;
    }

    /**
     * Obtém uma arena pelo nome.
     */
    public DuelArena getArena(String name) {
        return arenas.get(name.toLowerCase());
    }

    /**
     * Cria ou atualiza uma arena.
     */
    public DuelArena createArena(String name) {
        String key = name.toLowerCase();
        DuelArena arena = arenas.get(key);
        if (arena == null) {
            arena = new DuelArena(name);
            arenas.put(key, arena);
        }
        saveArenas();
        return arena;
    }

    /**
     * Remove uma arena.
     */
    public boolean deleteArena(String name) {
        DuelArena removed = arenas.remove(name.toLowerCase());
        if (removed != null) {
            saveArenas();
            return true;
        }
        return false;
    }

    /**
     * Obtém todas as arenas.
     */
    public Collection<DuelArena> getAllArenas() {
        return arenas.values();
    }

    /**
     * Obtém o número de arenas disponíveis.
     */
    public int getAvailableArenasCount() {
        int count = 0;
        for (DuelArena arena : arenas.values()) {
            if (arena.isAvailable())
                count++;
        }
        return count;
    }

    // ==================== ESTATÍSTICAS ====================

    /**
     * Atualiza as estatísticas de duelo de um jogador.
     */
    private void updateDuelStats(UUID playerId, boolean won) {
        PlayerProfile profile = plugin.getProfileManager().getProfile(playerId);
        if (profile == null)
            return;

        // Usar customData do PlayerData para armazenar stats de 1v1
        int wins = profile.getData().getCustomData("duel_wins", 0);
        int losses = profile.getData().getCustomData("duel_losses", 0);
        int streak = profile.getData().getCustomData("duel_streak", 0);
        int bestStreak = profile.getData().getCustomData("duel_best_streak", 0);

        if (won) {
            wins++;
            streak++;
            if (streak > bestStreak) {
                bestStreak = streak;
            }

            // Verificar conquistas de duel streak
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && plugin.getAchievementManager() != null) {
                // Verificar conquistas de streak de duelos
                if (streak == 3) {
                    plugin.getAchievementManager().incrementSpecialAchievement(player, "duel_streak_3");
                } else if (streak == 5) {
                    plugin.getAchievementManager().incrementSpecialAchievement(player, "duel_streak_5");
                } else if (streak == 10) {
                    plugin.getAchievementManager().incrementSpecialAchievement(player, "duel_streak_10");
                } else if (streak == 20) {
                    plugin.getAchievementManager().incrementSpecialAchievement(player, "duel_streak_20");
                }
            }
        } else {
            losses++;
            streak = 0;
        }

        profile.getData().setCustomData("duel_wins", wins);
        profile.getData().setCustomData("duel_losses", losses);
        profile.getData().setCustomData("duel_streak", streak);
        profile.getData().setCustomData("duel_best_streak", bestStreak);

        // Verificar conquistas de duelos
        Player player = Bukkit.getPlayer(playerId);
        if (player != null && plugin.getAchievementManager() != null) {
            plugin.getAchievementManager().checkAchievements(player);
        }
    }

    /**
     * Obtém as vitórias de duelo de um jogador.
     */
    public int getDuelWins(Player player) {
        if (player == null)
            return 0;
        PlayerProfile profile = plugin.getProfileManager().getProfile(player);
        if (profile == null)
            return 0;
        Integer wins = profile.getData().getCustomData("duel_wins", Integer.class);
        return wins != null ? wins : 0;
    }

    /**
     * Obtém as derrotas de duelo de um jogador.
     */
    public int getDuelLosses(Player player) {
        if (player == null)
            return 0;
        PlayerProfile profile = plugin.getProfileManager().getProfile(player);
        if (profile == null)
            return 0;
        Integer losses = profile.getData().getCustomData("duel_losses", Integer.class);
        return losses != null ? losses : 0;
    }

    /**
     * Obtém a winstreak atual de duelo de um jogador.
     */
    public int getDuelStreak(Player player) {
        if (player == null)
            return 0;
        PlayerProfile profile = plugin.getProfileManager().getProfile(player);
        if (profile == null)
            return 0;
        Integer streak = profile.getData().getCustomData("duel_streak", Integer.class);
        return streak != null ? streak : 0;
    }

    // ==================== ITENS DO LOBBY 1v1 ====================

    /**
     * Dá os itens do lobby de 1v1 para o jogador.
     */
    public void giveDuelLobbyItems(Player player) {
        PlayerInventory inv = player.getInventory();
        inv.clear();
        inv.setArmorContents(null);

        // Slot 0: Espada (para desafiar jogadores)
        ItemStack sword = new ItemBuilder(Material.DIAMOND_SWORD)
                .name("§a§lDesafiar Jogador")
                .lore(
                        "§7Clique em um jogador para",
                        "§7desafiá-lo para um 1v1!",
                        "",
                        "§eClique direito em um jogador!")
                .build();
        inv.setItem(0, sword);

        // Slot 4: Olho de Ender (entrar/sair da fila)
        boolean inQueue = isInQueue(player);
        ItemStack queueItem = new ItemBuilder(Material.EYE_OF_ENDER)
                .name(inQueue ? "§c§lSair da Fila" : "§a§lEntrar na Fila")
                .lore(
                        inQueue ? "§7Clique para sair da fila" : "§7Clique para procurar um oponente",
                        "",
                        "§7Jogadores na fila: §e" + getQueueSize(),
                        "",
                        inQueue ? "§cClique para sair!" : "§aClique para entrar!")
                .glow()
                .build();
        inv.setItem(4, queueItem);

        // Slot 8: Cama (voltar ao spawn principal)
        ItemStack bed = new ItemBuilder(Material.BED)
                .name("§c§lVoltar ao Spawn")
                .lore(
                        "§7Retorne ao spawn principal",
                        "§7do servidor.",
                        "",
                        "§eClique para voltar!")
                .build();
        inv.setItem(8, bed);

        player.updateInventory();
    }

    // ==================== PERSISTÊNCIA ====================

    /**
     * Carrega as arenas do arquivo.
     */
    private void loadArenas() {
        arenas.clear();

        if (!arenasFile.exists()) {
            try {
                arenasFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Erro ao criar duel_arenas.yml: " + e.getMessage());
                return;
            }
        }

        arenasConfig = YamlConfiguration.loadConfiguration(arenasFile);

        ConfigurationSection section = arenasConfig.getConfigurationSection("arenas");
        if (section == null)
            return;

        for (String key : section.getKeys(false)) {
            String path = "arenas." + key;

            String worldName = arenasConfig.getString(path + ".world");
            double s1x = arenasConfig.getDouble(path + ".spawn1.x");
            double s1y = arenasConfig.getDouble(path + ".spawn1.y");
            double s1z = arenasConfig.getDouble(path + ".spawn1.z");
            float s1yaw = (float) arenasConfig.getDouble(path + ".spawn1.yaw");
            float s1pitch = (float) arenasConfig.getDouble(path + ".spawn1.pitch");
            double s2x = arenasConfig.getDouble(path + ".spawn2.x");
            double s2y = arenasConfig.getDouble(path + ".spawn2.y");
            double s2z = arenasConfig.getDouble(path + ".spawn2.z");
            float s2yaw = (float) arenasConfig.getDouble(path + ".spawn2.yaw");
            float s2pitch = (float) arenasConfig.getDouble(path + ".spawn2.pitch");
            boolean enabled = arenasConfig.getBoolean(path + ".enabled", true);

            DuelArena arena = new DuelArena(key, worldName,
                    s1x, s1y, s1z, s1yaw, s1pitch,
                    s2x, s2y, s2z, s2yaw, s2pitch);
            arena.setEnabled(enabled);

            arenas.put(key.toLowerCase(), arena);
        }
    }

    /**
     * Salva as arenas no arquivo.
     */
    public void saveArenas() {
        arenasConfig = new YamlConfiguration();

        for (DuelArena arena : arenas.values()) {
            String path = "arenas." + arena.getName();

            arenasConfig.set(path + ".world", arena.getWorldName());
            arenasConfig.set(path + ".spawn1.x", arena.getSpawn1X());
            arenasConfig.set(path + ".spawn1.y", arena.getSpawn1Y());
            arenasConfig.set(path + ".spawn1.z", arena.getSpawn1Z());
            arenasConfig.set(path + ".spawn1.yaw", arena.getSpawn1Yaw());
            arenasConfig.set(path + ".spawn1.pitch", arena.getSpawn1Pitch());
            arenasConfig.set(path + ".spawn2.x", arena.getSpawn2X());
            arenasConfig.set(path + ".spawn2.y", arena.getSpawn2Y());
            arenasConfig.set(path + ".spawn2.z", arena.getSpawn2Z());
            arenasConfig.set(path + ".spawn2.yaw", arena.getSpawn2Yaw());
            arenasConfig.set(path + ".spawn2.pitch", arena.getSpawn2Pitch());
            arenasConfig.set(path + ".enabled", arena.isEnabled());
        }

        try {
            arenasConfig.save(arenasFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Erro ao salvar duel_arenas.yml: " + e.getMessage());
        }
    }

    // ==================== TASKS ====================

    /**
     * Inicia a task de matchmaking.
     */
    private void startMatchmakingTask() {
        matchmakingTask = new BukkitRunnable() {
            @Override
            public void run() {
                processQueue();
            }
        }.runTaskTimer(plugin, 40L, 40L); // A cada 2 segundos
    }

    /**
     * Processa a fila de matchmaking.
     */
    private void processQueue() {
        while (queue.size() >= 2) {
            UUID uuid1 = queue.pollFirst();
            UUID uuid2 = queue.pollFirst();

            if (uuid1 == null || uuid2 == null)
                break;

            Player player1 = Bukkit.getPlayer(uuid1);
            Player player2 = Bukkit.getPlayer(uuid2);

            // Verificar se ambos ainda estão online
            if (player1 == null || !player1.isOnline()) {
                queueSettings.remove(uuid1);
                queueJoinTime.remove(uuid1);
                if (player2 != null && player2.isOnline()) {
                    queue.addFirst(uuid2); // Devolver à fila
                }
                continue;
            }

            if (player2 == null || !player2.isOnline()) {
                queueSettings.remove(uuid2);
                queueJoinTime.remove(uuid2);
                queue.addFirst(uuid1); // Devolver à fila
                continue;
            }

            // Usar configurações do primeiro jogador
            DuelSettings settings = queueSettings.remove(uuid1);
            queueSettings.remove(uuid2);
            queueJoinTime.remove(uuid1);
            queueJoinTime.remove(uuid2);

            // Iniciar duelo
            if (!startDuel(player1, player2, settings)) {
                // Se falhou, devolver à fila
                queue.addFirst(uuid2);
                queue.addFirst(uuid1);
                queueSettings.put(uuid1, settings);
                queueSettings.put(uuid2, new DuelSettings());
                queueJoinTime.put(uuid1, System.currentTimeMillis());
                queueJoinTime.put(uuid2, System.currentTimeMillis());
                break; // Para evitar loop infinito se não há arenas
            }
        }
    }

    /**
     * Inicia a task de limpeza de desafios expirados.
     */
    private void startCleanupTask() {
        cleanupTask = new BukkitRunnable() {
            @Override
            public void run() {
                cleanupExpiredChallenges();
            }
        }.runTaskTimer(plugin, 200L, 200L); // A cada 10 segundos
    }

    /**
     * Limpa desafios expirados.
     */
    private void cleanupExpiredChallenges() {
        long now = System.currentTimeMillis();
        long expireTime = CHALLENGE_EXPIRE_SECONDS * 1000L;

        Iterator<Map.Entry<UUID, Long>> it = challengeTimestamps.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Long> entry = it.next();
            if (now - entry.getValue() > expireTime) {
                UUID targetUuid = entry.getKey();
                UUID challengerUuid = pendingChallenges.get(targetUuid);

                // Notificar
                Player target = Bukkit.getPlayer(targetUuid);
                if (target != null && target.isOnline()) {
                    ChatStorage.sendCustom(target, "§cO desafio expirou.");
                }

                Player challenger = challengerUuid != null ? Bukkit.getPlayer(challengerUuid) : null;
                if (challenger != null && challenger.isOnline()) {
                    ChatStorage.sendCustom(challenger, "§cSeu desafio expirou.");
                }

                // Limpar
                it.remove();
                pendingChallenges.remove(targetUuid);
                challengeSettings.remove(targetUuid);
            }
        }
    }

    // ==================== SHUTDOWN ====================

    /**
     * Chamado quando o plugin desliga.
     */
    public void shutdown() {
        // Cancelar tasks
        if (matchmakingTask != null) {
            matchmakingTask.cancel();
        }
        if (cleanupTask != null) {
            cleanupTask.cancel();
        }

        // Cancelar todos os duelos ativos
        for (DuelMatch match : activeMatches.values()) {
            if (!match.isEnded()) {
                match.cancel();
                teleportToLobby(match.getPlayer1());
                teleportToLobby(match.getPlayer2());
            }
        }

        // Salvar arenas
        saveArenas();

        // Limpar coleções
        activeMatches.clear();
        playerToMatch.clear();
        queue.clear();
        queueSettings.clear();
        pendingChallenges.clear();
        challengeSettings.clear();
        challengeTimestamps.clear();

        plugin.getLogger().info("DuelManager desligado.");
    }

    // ==================== GETTERS ====================

    public Map<UUID, DuelMatch> getActiveMatches() {
        return new HashMap<>(activeMatches);
    }

    public int getActiveMatchesCount() {
        return activeMatches.size();
    }
}
