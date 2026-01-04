package com.haumea.kitpvp.managers;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.models.EventState;
import com.haumea.kitpvp.models.Warp;
import com.haumea.kitpvp.scoreboard.PlayerBoard;
import com.haumea.kitpvp.utils.ChatStorage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gerenciador Central de Eventos do HaumeaMC.
 * 
 * Este manager é responsável por todo o ciclo de vida de eventos especiais,
 * incluindo gerenciamento de jogadores, configurações dinâmicas, timer,
 * scoreboard específica e integração com sistemas existentes.
 * 
 * Design Pattern: Singleton via instância no plugin principal
 * Thread-Safety: ConcurrentHashMap para lista de participantes
 * 
 * @author HaumeaMC
 */
public class EventManager {

    private final HaumeaMC plugin;

    // ==================== ESTADO DO EVENTO ====================
    private EventState state;
    private String eventName;
    private String creatorName;

    // ==================== JOGADORES ====================
    private final Set<UUID> participants;
    private final Map<UUID, Location> previousLocations;
    private String winnerName;

    // ==================== CONFIGURAÇÕES DINÂMICAS ====================
    private boolean pvpEnabled;
    private boolean damageEnabled;
    private boolean buildEnabled;
    private boolean hungerEnabled;

    // ==================== TIMER ====================
    private long startTime;
    private BukkitTask timerTask;
    private BukkitTask scoreboardUpdateTask;

    // ==================== WARP DO EVENTO ====================
    private static final String EVENT_WARP_NAME = "evento";

    // ==================== CONFIGURAÇÕES ====================
    private static final int SCOREBOARD_UPDATE_INTERVAL = 20; // 1 segundo
    private static final int FEW_PLAYERS_THRESHOLD = 5; // Aviso quando restam poucos jogadores
    private static final int ELIMINATION_BROADCAST_INTERVAL = 5; // Broadcast a cada X eliminações

    // ==================== ÍCONES E SÍMBOLOS ====================
    private static final String ICON_SWORD = "⚔";
    private static final String ICON_CHECK = "✔";
    private static final String ICON_CROSS = "✘";
    private static final String ICON_TIMER = "⏱";
    private static final String SEPARATOR = "§7§m-----------------";
    private static final String SERVER_URL = "§e www.haumeamc.com.br";

    // Contador de eliminações para broadcast
    private int eliminationCounter;

    public EventManager(HaumeaMC plugin) {
        this.plugin = plugin;
        this.participants = ConcurrentHashMap.newKeySet();
        this.previousLocations = new ConcurrentHashMap<>();
        this.state = EventState.NONE;
        this.pvpEnabled = false;
        this.damageEnabled = false;
        this.buildEnabled = false;
        this.hungerEnabled = false;
        this.startTime = 0;
        this.eliminationCounter = 0;
    }

    // ==================== CICLO DE VIDA DO EVENTO ====================

    /**
     * Cria um novo evento
     * 
     * @param creator Jogador que criou o evento
     * @param name    Nome do evento (opcional)
     * @return true se criado com sucesso
     */
    public boolean createEvent(Player creator, String name) {
        if (state != EventState.NONE) {
            ChatStorage.send(creator, "event.already-exists");
            return false;
        }

        // Verificar se a warp do evento existe
        if (!hasEventWarp()) {
            ChatStorage.send(creator, "event.no-warp");
            return false;
        }

        state = EventState.CREATED;
        eventName = (name != null && !name.isEmpty()) ? name : "Evento";
        creatorName = creator.getName();
        winnerName = null;
        eliminationCounter = 0;

        // Configurações padrão
        pvpEnabled = false;
        damageEnabled = false;
        buildEnabled = false;
        hungerEnabled = false;

        // Broadcast
        broadcast("event.created", "name", eventName);

        plugin.getLogger().info("Evento '" + eventName + "' criado por " + creatorName);
        return true;
    }

    /**
     * Abre as inscrições do evento
     * 
     * @param staff Staff que abriu
     * @return true se aberto com sucesso
     */
    public boolean openEvent(Player staff) {
        if (state != EventState.CREATED && state != EventState.CLOSED) {
            ChatStorage.send(staff, "event.invalid-state", "expected", "Criado ou Fechado");
            return false;
        }

        state = EventState.OPEN;
        broadcast("event.open");

        plugin.getLogger().info("Inscrições abertas para o evento por " + staff.getName());
        return true;
    }

    /**
     * Fecha as inscrições do evento
     * 
     * @param staff Staff que fechou
     * @return true se fechado com sucesso
     */
    public boolean closeEvent(Player staff) {
        if (state != EventState.OPEN) {
            ChatStorage.send(staff, "event.invalid-state", "expected", "Aberto");
            return false;
        }

        state = EventState.CLOSED;
        broadcast("event.closed");

        plugin.getLogger().info("Inscrições fechadas por " + staff.getName());
        return true;
    }

    /**
     * Inicia o evento
     * 
     * @param staff Staff que iniciou
     * @return true se iniciado com sucesso
     */
    public boolean startEvent(Player staff) {
        if (state != EventState.CLOSED && state != EventState.PAUSED) {
            ChatStorage.send(staff, "event.invalid-state", "expected", "Fechado ou Pausado");
            return false;
        }

        if (participants.size() < 2) {
            ChatStorage.send(staff, "event.not-enough-players");
            return false;
        }

        // Se estava pausado, apenas retomar
        if (state == EventState.PAUSED) {
            state = EventState.STARTED;
            broadcast("event.resumed");
            startTimerTask();
            startScoreboardTask();
            return true;
        }

        state = EventState.STARTED;
        startTime = System.currentTimeMillis();

        // Habilitar dano e PvP por padrão ao iniciar
        damageEnabled = true;
        pvpEnabled = true;

        // Iniciar tasks
        startTimerTask();
        startScoreboardTask();

        // Atualizar scoreboards de todos os participantes
        updateAllScoreboards();

        broadcast("event.started");

        plugin.getLogger()
                .info("Evento iniciado por " + staff.getName() + " com " + participants.size() + " jogadores");
        return true;
    }

    /**
     * Pausa o evento
     * 
     * @param staff Staff que pausou
     * @return true se pausado com sucesso
     */
    public boolean pauseEvent(Player staff) {
        if (state != EventState.STARTED) {
            ChatStorage.send(staff, "event.invalid-state", "expected", "Iniciado");
            return false;
        }

        state = EventState.PAUSED;

        // Parar tasks
        stopTimerTask();
        stopScoreboardTask();

        broadcast("event.paused");

        plugin.getLogger().info("Evento pausado por " + staff.getName());
        return true;
    }

    /**
     * Finaliza o evento declarando um vencedor
     * 
     * @param staff  Staff que finalizou (pode ser null para finalização automática)
     * @param winner Vencedor (pode ser null)
     * @return true se finalizado com sucesso
     */
    public boolean finishEvent(Player staff, Player winner) {
        if (state == EventState.NONE || state == EventState.FINISHED) {
            if (staff != null) {
                ChatStorage.send(staff, "event.no-active-event");
            }
            return false;
        }

        state = EventState.FINISHED;
        winnerName = winner != null ? winner.getName() : null;

        // Parar tasks
        stopTimerTask();
        stopScoreboardTask();

        // Broadcast do vencedor
        if (winnerName != null) {
            broadcast("event.winner", "player", winnerName);
        } else {
            broadcast("event.finished");
        }

        // Teleportar todos ao spawn e restaurar scoreboards
        for (UUID uuid : new HashSet<>(participants)) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                removePlayerFromEvent(player, false);
            }
        }

        // Limpar estado
        cleanupEvent();

        String finishedBy = staff != null ? staff.getName() : "Sistema";
        plugin.getLogger().info(
                "Evento finalizado por " + finishedBy + ". Vencedor: " + (winnerName != null ? winnerName : "Nenhum"));
        return true;
    }

    /**
     * Cancela o evento
     * 
     * @param staff Staff que cancelou
     * @return true se cancelado com sucesso
     */
    public boolean cancelEvent(Player staff) {
        if (state == EventState.NONE) {
            ChatStorage.send(staff, "event.no-active-event");
            return false;
        }

        // Parar tasks
        stopTimerTask();
        stopScoreboardTask();

        broadcast("event.cancelled");

        // Expulsar todos os jogadores
        for (UUID uuid : new HashSet<>(participants)) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                removePlayerFromEvent(player, true);
            }
        }

        // Limpar estado
        cleanupEvent();

        plugin.getLogger().info("Evento cancelado por " + staff.getName());
        return true;
    }

    /**
     * Limpa todo o estado do evento
     */
    private void cleanupEvent() {
        state = EventState.NONE;
        eventName = null;
        creatorName = null;
        winnerName = null;
        participants.clear();
        previousLocations.clear();
        pvpEnabled = false;
        damageEnabled = false;
        buildEnabled = false;
        hungerEnabled = false;
        startTime = 0;
        eliminationCounter = 0;
    }

    // ==================== GERENCIAMENTO DE JOGADORES ====================

    /**
     * Adiciona um jogador ao evento
     * 
     * @param player Jogador a adicionar
     * @return true se adicionado com sucesso
     */
    public boolean addPlayerToEvent(Player player) {
        if (!state.canJoin()) {
            ChatStorage.send(player, "event.not-open");
            return false;
        }

        if (isParticipant(player)) {
            ChatStorage.send(player, "event.already-in");
            return false;
        }

        // Salvar localização anterior
        previousLocations.put(player.getUniqueId(), player.getLocation().clone());

        // Adicionar à lista de participantes
        participants.add(player.getUniqueId());

        // Teleportar para a warp do evento
        Warp eventWarp = getEventWarp();
        if (eventWarp != null) {
            Location warpLoc = eventWarp.toLocation();
            if (warpLoc != null) {
                player.teleport(warpLoc);
            }
        }

        // Dar item de voltar
        giveLeaveItem(player);

        // Trocar scoreboard para a do evento
        createEventScoreboard(player);

        // Mensagem
        ChatStorage.send(player, "event.joined", "name", eventName);

        // Atualizar scoreboards de todos (contagem de jogadores)
        updateAllScoreboards();

        plugin.getLogger().info(player.getName() + " entrou no evento. Total: " + participants.size());
        return true;
    }

    /**
     * Remove um jogador do evento
     * 
     * @param player    Jogador a remover
     * @param voluntary true se saiu voluntariamente
     */
    public void removePlayerFromEvent(Player player, boolean voluntary) {
        if (!isParticipant(player)) {
            return;
        }

        participants.remove(player.getUniqueId());

        // Restaurar scoreboard padrão
        restoreDefaultScoreboard(player);

        // Teleportar ao spawn ou localização anterior
        Location previousLoc = previousLocations.remove(player.getUniqueId());
        Warp spawnWarp = plugin.getWarpsManager().getWarp("spawn");

        if (spawnWarp != null && spawnWarp.toLocation() != null) {
            player.teleport(spawnWarp.toLocation());
        } else if (previousLoc != null) {
            player.teleport(previousLoc);
        }

        // Remover item de voltar
        removeLeaveItem(player);

        if (voluntary) {
            ChatStorage.send(player, "event.left");
        }

        // Verificar se há vencedor (se o evento estava em andamento)
        if (state.isRunning()) {
            eliminationCounter++;

            // Broadcast de eliminação se necessário
            if (eliminationCounter % ELIMINATION_BROADCAST_INTERVAL == 0) {
                broadcast("event.players-remaining", "count", String.valueOf(participants.size()));
            }

            // Aviso de poucos jogadores
            if (participants.size() <= FEW_PLAYERS_THRESHOLD && participants.size() > 1) {
                broadcast("event.few-players", "count", String.valueOf(participants.size()));
            }

            // Verificar vencedor
            checkForWinner();
        }

        // Atualizar scoreboards
        updateAllScoreboards();
    }

    /**
     * Expulsa um jogador do evento (kick administrativo)
     * 
     * @param player Jogador a expulsar
     * @param staff  Staff que expulsou
     */
    public void kickPlayerFromEvent(Player player, Player staff) {
        if (!isParticipant(player)) {
            ChatStorage.send(staff, "event.player-not-in-event", "player", player.getName());
            return;
        }

        removePlayerFromEvent(player, false);
        ChatStorage.send(player, "event.kicked");
        ChatStorage.send(staff, "event.kicked-confirm", "player", player.getName());
    }

    /**
     * Teleporta todos os participantes para a warp do evento
     * 
     * @param staff Staff que executou
     */
    public void teleportAllToEvent(Player staff) {
        Warp eventWarp = getEventWarp();
        if (eventWarp == null || eventWarp.toLocation() == null) {
            ChatStorage.send(staff, "event.no-warp");
            return;
        }

        Location loc = eventWarp.toLocation();
        int count = 0;

        for (UUID uuid : participants) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.teleport(loc);
                count++;
            }
        }

        ChatStorage.send(staff, "event.tpall-success", "count", String.valueOf(count));
    }

    /**
     * Verifica se há um vencedor
     */
    private void checkForWinner() {
        if (participants.size() == 1) {
            UUID winnerUUID = participants.iterator().next();
            Player winner = Bukkit.getPlayer(winnerUUID);
            finishEvent(null, winner);
        } else if (participants.isEmpty()) {
            finishEvent(null, null);
        }
    }

    // ==================== CONFIGURAÇÕES DINÂMICAS ====================

    /**
     * Alterna PvP no evento
     * 
     * @param staff Staff que alterou
     * @return novo estado do PvP
     */
    public boolean togglePvP(Player staff) {
        pvpEnabled = !pvpEnabled;
        broadcastParticipants("event.pvp-toggle", "status", pvpEnabled ? "§a§lATIVADO" : "§c§lDESATIVADO");
        updateAllScoreboards();
        return pvpEnabled;
    }

    /**
     * Alterna Dano no evento
     * 
     * @param staff Staff que alterou
     * @return novo estado do Dano
     */
    public boolean toggleDamage(Player staff) {
        damageEnabled = !damageEnabled;
        broadcastParticipants("event.damage-toggle", "status", damageEnabled ? "§a§lATIVADO" : "§c§lDESATIVADO");
        updateAllScoreboards();
        return damageEnabled;
    }

    /**
     * Alterna Build no evento
     * 
     * @param staff Staff que alterou
     * @return novo estado do Build
     */
    public boolean toggleBuild(Player staff) {
        buildEnabled = !buildEnabled;
        broadcastParticipants("event.build-toggle", "status", buildEnabled ? "§a§lATIVADO" : "§c§lDESATIVADO");
        updateAllScoreboards();
        return buildEnabled;
    }

    // ==================== TIMER ====================

    /**
     * Inicia a task do timer
     */
    private void startTimerTask() {
        stopTimerTask();

        timerTask = new BukkitRunnable() {
            @Override
            public void run() {
                // O timer é calculado dinamicamente, não precisa fazer nada aqui
                // A atualização é feita pela scoreboardUpdateTask
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    /**
     * Para a task do timer
     */
    private void stopTimerTask() {
        if (timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }
    }

    /**
     * Obtém o tempo decorrido em segundos
     * 
     * @return segundos desde o início
     */
    public long getElapsedSeconds() {
        if (startTime == 0)
            return 0;
        return (System.currentTimeMillis() - startTime) / 1000;
    }

    /**
     * Formata o tempo para exibição (MM:SS)
     * 
     * @return tempo formatado
     */
    public String getFormattedTime() {
        long seconds = getElapsedSeconds();
        long minutes = seconds / 60;
        long secs = seconds % 60;
        return String.format("%02d:%02d", minutes, secs);
    }

    // ==================== SCOREBOARD DO EVENTO ====================

    /**
     * Inicia a task de atualização de scoreboard
     */
    private void startScoreboardTask() {
        stopScoreboardTask();

        scoreboardUpdateTask = new BukkitRunnable() {
            @Override
            public void run() {
                updateAllScoreboards();
            }
        }.runTaskTimer(plugin, SCOREBOARD_UPDATE_INTERVAL, SCOREBOARD_UPDATE_INTERVAL);
    }

    /**
     * Para a task de atualização de scoreboard
     */
    private void stopScoreboardTask() {
        if (scoreboardUpdateTask != null) {
            scoreboardUpdateTask.cancel();
            scoreboardUpdateTask = null;
        }
    }

    /**
     * Cria a scoreboard do evento para um jogador
     * 
     * @param player Jogador
     */
    private void createEventScoreboard(Player player) {
        // Remover scoreboard atual se existir
        plugin.getScoreboardManager().removeBoard(player);

        // Criar nova scoreboard do evento
        PlayerBoard board = new PlayerBoard(player);
        updateEventScoreboard(player, board);
    }

    /**
     * Atualiza a scoreboard do evento de um jogador
     * 
     * @param player Jogador
     * @param board  PlayerBoard
     */
    private void updateEventScoreboard(Player player, PlayerBoard board) {
        if (board == null)
            return;

        // Título
        board.setTitle("§6§l" + ICON_SWORD + " EVENTO " + ICON_SWORD);

        // Construir linhas
        String[] lines = buildEventScoreboardLines();
        board.setLines(lines);
    }

    /**
     * Constrói as linhas da scoreboard do evento
     * 
     * @return Array de linhas
     */
    private String[] buildEventScoreboardLines() {
        String pvpDisplay = pvpEnabled ? "§a" + ICON_CHECK : "§c" + ICON_CROSS;
        String damageDisplay = damageEnabled ? "§a" + ICON_CHECK : "§c" + ICON_CROSS;
        String buildDisplay = buildEnabled ? "§a" + ICON_CHECK : "§c" + ICON_CROSS;

        String timeDisplay = state.isRunning() ? getFormattedTime() : "--:--";
        String stateDisplay = state.getDisplayName();

        return new String[] {
                SEPARATOR,
                "",
                " §fJogadores: §b" + participants.size(),
                " §fTempo: §e" + ICON_TIMER + " " + timeDisplay,
                " ",
                " §fPvP: " + pvpDisplay,
                " §fDano: " + damageDisplay,
                " §fBuild: " + buildDisplay,
                "  ",
                " §fStatus: " + stateDisplay,
                "   ",
                SERVER_URL
        };
    }

    /**
     * Restaura a scoreboard padrão de um jogador
     * 
     * @param player Jogador
     */
    private void restoreDefaultScoreboard(Player player) {
        // Remover scoreboard do evento
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());

        // Criar scoreboard padrão via ScoreboardManager
        plugin.getScoreboardManager().createBoard(player);
    }

    /**
     * Atualiza as scoreboards de todos os participantes
     */
    private void updateAllScoreboards() {
        for (UUID uuid : participants) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                PlayerBoard board = getPlayerEventBoard(player);
                if (board != null) {
                    updateEventScoreboard(player, board);
                }
            }
        }
    }

    /**
     * Obtém o PlayerBoard do evento de um jogador
     * 
     * @param player Jogador
     * @return PlayerBoard ou null
     */
    private PlayerBoard getPlayerEventBoard(Player player) {
        // Verificar se o jogador tem uma scoreboard
        if (player.getScoreboard() == Bukkit.getScoreboardManager().getMainScoreboard()) {
            // Criar nova scoreboard do evento
            createEventScoreboard(player);
        }

        // Retornar board (precision limitada nesta abordagem por simpliciação)
        // Em produção, manter cache de boards do evento separado
        return null; // Será atualizado diretamente via createEventScoreboard
    }

    // ==================== ITENS DO EVENTO ====================

    /**
     * Dá o item de voltar ao spawn para o jogador
     * 
     * @param player Jogador
     */
    private void giveLeaveItem(Player player) {
        ItemStack bed = new ItemStack(Material.BED, 1);
        ItemMeta meta = bed.getItemMeta();
        meta.setDisplayName("§c§lVoltar ao Spawn");
        meta.setLore(Arrays.asList("§7Clique para sair do evento", "§7e voltar ao spawn"));
        bed.setItemMeta(meta);

        player.getInventory().setItem(8, bed);
        player.updateInventory();
    }

    /**
     * Remove o item de voltar do jogador
     * 
     * @param player Jogador
     */
    private void removeLeaveItem(Player player) {
        ItemStack item = player.getInventory().getItem(8);
        if (item != null && item.getType() == Material.BED) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.hasDisplayName() && meta.getDisplayName().contains("Voltar")) {
                player.getInventory().setItem(8, null);
                player.updateInventory();
            }
        }
    }

    // ==================== WARP DO EVENTO ====================

    /**
     * Verifica se a warp do evento existe
     * 
     * @return true se existe
     */
    public boolean hasEventWarp() {
        return plugin.getWarpsManager().warpExists(EVENT_WARP_NAME);
    }

    /**
     * Obtém a warp do evento
     * 
     * @return Warp ou null
     */
    public Warp getEventWarp() {
        return plugin.getWarpsManager().getWarp(EVENT_WARP_NAME);
    }

    /**
     * Define a warp do evento
     * 
     * @param location Localização
     * @return Warp criada
     */
    public Warp setEventWarp(Location location) {
        return plugin.getWarpsManager().setWarp(EVENT_WARP_NAME, location);
    }

    // ==================== BROADCASTS ====================

    /**
     * Envia broadcast global
     * 
     * @param key          Chave da mensagem
     * @param replacements Placeholders
     */
    private void broadcast(String key, String... replacements) {
        String message = ChatStorage.getMessage(key, replacements);
        Bukkit.broadcastMessage(message);
    }

    /**
     * Envia broadcast apenas para participantes
     * 
     * @param key          Chave da mensagem
     * @param replacements Placeholders
     */
    private void broadcastParticipants(String key, String... replacements) {
        String message = ChatStorage.getMessage(key, replacements);
        for (UUID uuid : participants) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.sendMessage(message);
            }
        }
    }

    // ==================== GETTERS ====================

    public EventState getState() {
        return state;
    }

    public String getEventName() {
        return eventName;
    }

    public String getCreatorName() {
        return creatorName;
    }

    public String getWinnerName() {
        return winnerName;
    }

    public int getParticipantCount() {
        return participants.size();
    }

    public Set<UUID> getParticipants() {
        return Collections.unmodifiableSet(participants);
    }

    public boolean isParticipant(Player player) {
        return participants.contains(player.getUniqueId());
    }

    public boolean isParticipant(UUID uuid) {
        return participants.contains(uuid);
    }

    public boolean isPvPEnabled() {
        return pvpEnabled;
    }

    public boolean isDamageEnabled() {
        return damageEnabled;
    }

    public boolean isBuildEnabled() {
        return buildEnabled;
    }

    public boolean isHungerEnabled() {
        return hungerEnabled;
    }

    /**
     * Obtém lista de nomes dos participantes
     * 
     * @return Lista de nomes
     */
    public List<String> getParticipantNames() {
        List<String> names = new ArrayList<>();
        for (UUID uuid : participants) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                names.add(player.getName());
            }
        }
        return names;
    }

    /**
     * Verifica se há um evento ativo
     * 
     * @return true se há evento ativo
     */
    public boolean hasActiveEvent() {
        return state.isActive();
    }

    /**
     * Desliga o EventManager (cleanup no onDisable)
     */
    public void shutdown() {
        if (hasActiveEvent()) {
            cancelEvent(null);
        }
        stopTimerTask();
        stopScoreboardTask();
    }
}
