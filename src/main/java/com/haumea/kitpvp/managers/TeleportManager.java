package com.haumea.kitpvp.managers;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.utils.ChatStorage;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Gerenciador centralizado de teleportes do HaumeaMC.
 * 
 * Funcionalidades:
 * - Teleporte instantâneo ou com delay
 * - Cancelamento automático em movimento ou combate
 * - Efeitos visuais (partículas) e sonoros
 * - Warmup configurável por tipo de teleporte
 * - Bypass para staff
 * - Callbacks de sucesso/falha
 * 
 * @author HaumeaMC
 */
public class TeleportManager {

    private final HaumeaMC plugin;

    // Teleportes pendentes (UUID -> TeleportTask)
    private final Map<UUID, TeleportTask> pendingTeleports;

    // Configurações de delay por tipo (em segundos)
    private final Map<String, Integer> teleportDelays;

    // Tipos de teleporte
    public static final String WARP = "warp";
    public static final String SPAWN = "spawn";
    public static final String TP = "tp";
    public static final String TPA = "tpa";
    public static final String HOME = "home";
    public static final String ARENA = "arena";
    public static final String ADMIN = "admin"; // Sem delay

    // Configurações padrão
    private static final double MOVE_CANCEL_DISTANCE = 0.5; // Blocos
    private static final boolean DEFAULT_EFFECTS = true;
    private static final boolean DEFAULT_SOUNDS = true;

    public TeleportManager(HaumeaMC plugin) {
        this.plugin = plugin;
        this.pendingTeleports = new ConcurrentHashMap<>();
        this.teleportDelays = new ConcurrentHashMap<>();

        setupDefaultDelays();
    }

    /**
     * Configura delays padrão por tipo de teleporte
     */
    private void setupDefaultDelays() {
        teleportDelays.put(WARP, 3); // 3 segundos
        teleportDelays.put(SPAWN, 3); // 3 segundos
        teleportDelays.put(TP, 0); // Instantâneo (staff)
        teleportDelays.put(TPA, 5); // 5 segundos
        teleportDelays.put(HOME, 3); // 3 segundos
        teleportDelays.put(ARENA, 3); // 3 segundos
        teleportDelays.put(ADMIN, 0); // Instantâneo
    }

    // ==================== TELEPORTE PRINCIPAL ====================

    /**
     * Teleporta um jogador para uma localização.
     * Usa delay configurado para o tipo, com efeitos padrão.
     * 
     * @param player      Jogador a teleportar
     * @param destination Destino do teleporte
     * @param type        Tipo de teleporte (WARP, SPAWN, etc)
     */
    public void teleport(Player player, Location destination, String type) {
        teleport(player, destination, type, null, null);
    }

    /**
     * Teleporta um jogador para uma localização com callbacks.
     * 
     * @param player      Jogador a teleportar
     * @param destination Destino do teleporte
     * @param type        Tipo de teleporte
     * @param onSuccess   Callback executado em sucesso (pode ser null)
     * @param onCancel    Callback executado em cancelamento (pode ser null)
     */
    public void teleport(Player player, Location destination, String type,
            Consumer<Player> onSuccess, Consumer<Player> onCancel) {

        if (player == null || destination == null) {
            return;
        }

        // Cancelar teleporte pendente anterior
        cancelPendingTeleport(player);

        // Obter delay para o tipo
        int delay = getDelay(player, type);

        // Teleporte instantâneo
        if (delay <= 0) {
            executeTeleport(player, destination, true, true);
            if (onSuccess != null) {
                onSuccess.accept(player);
            }
            return;
        }

        // Verificar se está em combate
        if (isInCombat(player)) {
            ChatStorage.send(player, "teleport.in-combat");
            if (onCancel != null) {
                onCancel.accept(player);
            }
            return;
        }

        // Criar teleporte com delay
        TeleportTask task = new TeleportTask(player, destination, delay, type, onSuccess, onCancel);
        pendingTeleports.put(player.getUniqueId(), task);
        task.start();

        // Notificar jogador
        ChatStorage.send(player, "teleport.warmup", "seconds", String.valueOf(delay));
    }

    /**
     * Teleporta instantaneamente sem verificações (para admin/sistema)
     */
    public void teleportInstant(Player player, Location destination) {
        teleportInstant(player, destination, true, true);
    }

    /**
     * Teleporta instantaneamente com controle de efeitos
     */
    public void teleportInstant(Player player, Location destination, boolean effects, boolean sounds) {
        if (player == null || destination == null) {
            return;
        }

        cancelPendingTeleport(player);
        executeTeleport(player, destination, effects, sounds);
    }

    /**
     * Teleporta um jogador para outro jogador
     */
    public void teleportToPlayer(Player player, Player target, String type) {
        if (target == null || !target.isOnline()) {
            ChatStorage.send(player, "teleport.target-offline");
            return;
        }
        teleport(player, target.getLocation(), type);
    }

    // ==================== EXECUÇÃO DO TELEPORTE ====================

    /**
     * Executa o teleporte efetivamente
     */
    private void executeTeleport(Player player, Location destination, boolean effects, boolean sounds) {
        // Efeitos na origem
        if (effects) {
            playDepartureEffects(player.getLocation());
        }

        // Teleportar
        player.teleport(destination);

        // Efeitos no destino
        if (effects) {
            playArrivalEffects(destination);
        }

        // Sons
        if (sounds) {
            playTeleportSound(player);
        }
    }

    // ==================== EFEITOS VISUAIS ====================

    /**
     * Efeitos visuais na partida
     */
    private void playDepartureEffects(Location location) {
        // Partículas de fumaça
        location.getWorld().playEffect(location, Effect.ENDER_SIGNAL, 0);
        location.getWorld().playEffect(location.add(0, 1, 0), Effect.SMOKE, 4);
    }

    /**
     * Efeitos visuais na chegada
     */
    private void playArrivalEffects(Location location) {
        // Partículas de chegada
        location.getWorld().playEffect(location, Effect.ENDER_SIGNAL, 0);
        location.getWorld().playEffect(location.add(0, 1, 0), Effect.MOBSPAWNER_FLAMES, 0);
    }

    /**
     * Som de teleporte
     */
    private void playTeleportSound(Player player) {
        player.playSound(player.getLocation(), Sound.ENDERMAN_TELEPORT, 1.0f, 1.2f);
    }

    /**
     * Som de countdown
     */
    private void playCountdownSound(Player player, int secondsRemaining) {
        if (secondsRemaining <= 3) {
            player.playSound(player.getLocation(), Sound.NOTE_PLING, 0.5f, 1.0f + (0.2f * (3 - secondsRemaining)));
        } else {
            player.playSound(player.getLocation(), Sound.CLICK, 0.3f, 1.0f);
        }
    }

    // ==================== CANCELAMENTO ====================

    /**
     * Cancela um teleporte pendente
     */
    public void cancelPendingTeleport(Player player) {
        cancelPendingTeleport(player.getUniqueId(), "cancelled");
    }

    /**
     * Cancela um teleporte pendente por UUID
     */
    public void cancelPendingTeleport(UUID uuid, String reason) {
        TeleportTask task = pendingTeleports.remove(uuid);
        if (task != null) {
            task.cancel(reason);
        }
    }

    /**
     * Verifica se o jogador tem teleporte pendente
     */
    public boolean hasPendingTeleport(Player player) {
        return pendingTeleports.containsKey(player.getUniqueId());
    }

    /**
     * Chamado quando jogador se move (para cancelar teleporte)
     */
    public void onPlayerMove(Player player, Location from, Location to) {
        TeleportTask task = pendingTeleports.get(player.getUniqueId());
        if (task != null) {
            // Verificar se moveu mais que o permitido
            double distance = from.distance(to);
            if (distance > MOVE_CANCEL_DISTANCE) {
                cancelPendingTeleport(player.getUniqueId(), "movement");
                ChatStorage.send(player, "teleport.cancelled-movement");
            }
        }
    }

    /**
     * Chamado quando jogador recebe dano
     */
    public void onPlayerDamage(Player player) {
        if (hasPendingTeleport(player)) {
            cancelPendingTeleport(player.getUniqueId(), "damage");
            ChatStorage.send(player, "teleport.cancelled-damage");
        }
    }

    /**
     * Chamado quando jogador entra em combate
     */
    public void onPlayerCombat(Player player) {
        if (hasPendingTeleport(player)) {
            cancelPendingTeleport(player.getUniqueId(), "combat");
            ChatStorage.send(player, "teleport.cancelled-combat");
        }
    }

    // ==================== VERIFICAÇÕES ====================

    /**
     * Verifica se o jogador está em combate
     */
    private boolean isInCombat(Player player) {
        // Verificar via PlayerProfile
        if (plugin.getProfileManager() != null) {
            com.haumea.kitpvp.profile.PlayerProfile profile = plugin.getProfileManager().getProfile(player);
            if (profile != null && profile.isInCombat()) {
                return true;
            }
        }

        // Verificar via PlayerStateManager
        if (plugin.getStateManager() != null) {
            return plugin.getStateManager().isInCombat(player);
        }

        return false;
    }

    /**
     * Obtém o delay para um tipo de teleporte, considerando bypass
     */
    private int getDelay(Player player, String type) {
        // Staff tem bypass de delay
        if (hasDelayBypass(player)) {
            return 0;
        }

        return teleportDelays.getOrDefault(type.toLowerCase(), 3);
    }

    /**
     * Verifica se o jogador tem bypass de delay
     */
    private boolean hasDelayBypass(Player player) {
        // Permissão específica
        if (player.hasPermission("haumea.teleport.nodelay")) {
            return true;
        }

        // Staff superior (gerente+) tem bypass
        if (plugin.getGroupManager() != null) {
            return plugin.getGroupManager().isManager(player);
        }

        return false;
    }

    // ==================== CONFIGURAÇÃO ====================

    /**
     * Define o delay para um tipo de teleporte
     */
    public void setDelay(String type, int seconds) {
        teleportDelays.put(type.toLowerCase(), Math.max(0, seconds));
    }

    /**
     * Obtém o delay configurado para um tipo
     */
    public int getConfiguredDelay(String type) {
        return teleportDelays.getOrDefault(type.toLowerCase(), 3);
    }

    // ==================== LIMPEZA ====================

    /**
     * Cancela todos os teleportes pendentes (chamado no onDisable)
     */
    public void cancelAllPendingTeleports() {
        for (UUID uuid : pendingTeleports.keySet()) {
            cancelPendingTeleport(uuid, "shutdown");
        }
        pendingTeleports.clear();
    }

    /**
     * Remove teleporte pendente quando jogador sai
     */
    public void onPlayerQuit(Player player) {
        pendingTeleports.remove(player.getUniqueId());
    }

    // ==================== CLASSE INTERNA: TeleportTask ====================

    /**
     * Task que gerencia um teleporte com delay
     */
    private class TeleportTask {
        private final Player player;
        private final Location destination;
        private final Location startLocation;
        private final Consumer<Player> onSuccess;
        private final Consumer<Player> onCancel;

        private BukkitTask countdownTask;
        private int remainingSeconds;
        private boolean cancelled = false;

        public TeleportTask(Player player, Location destination, int seconds, String type,
                Consumer<Player> onSuccess, Consumer<Player> onCancel) {
            this.player = player;
            this.destination = destination;
            this.startLocation = player.getLocation().clone();
            this.remainingSeconds = seconds;
            this.onSuccess = onSuccess;
            this.onCancel = onCancel;
        }

        /**
         * Inicia o countdown do teleporte
         */
        public void start() {
            final TeleportTask self = this;

            countdownTask = new BukkitRunnable() {
                @Override
                public void run() {
                    if (cancelled || !player.isOnline()) {
                        this.cancel();
                        pendingTeleports.remove(player.getUniqueId());
                        return;
                    }

                    // Verificar movimento
                    if (player.getLocation().distance(startLocation) > MOVE_CANCEL_DISTANCE) {
                        self.cancelTask("movement");
                        ChatStorage.send(player, "teleport.cancelled-movement");
                        return;
                    }

                    // Verificar combate
                    if (isInCombat(player)) {
                        self.cancelTask("combat");
                        ChatStorage.send(player, "teleport.cancelled-combat");
                        return;
                    }

                    remainingSeconds--;

                    if (remainingSeconds <= 0) {
                        // Teleportar!
                        this.cancel();
                        pendingTeleports.remove(player.getUniqueId());
                        executeTeleport(player, destination, DEFAULT_EFFECTS, DEFAULT_SOUNDS);

                        ChatStorage.send(player, "teleport.success");

                        if (onSuccess != null) {
                            onSuccess.accept(player);
                        }
                    } else {
                        // Feedback de countdown
                        playCountdownSound(player, remainingSeconds);

                        // Mostrar action bar ou mensagem a cada segundo (opcional)
                        if (remainingSeconds <= 3) {
                            ChatStorage.sendRaw(player, "§eTeleportando em §f" + remainingSeconds + "§e...");
                        }
                    }
                }
            }.runTaskTimer(plugin, 20L, 20L); // A cada 1 segundo
        }

        /**
         * Cancela este teleporte (chamado externamente)
         */
        public void cancel(String reason) {
            cancelTask(reason);
        }

        /**
         * Cancela a task internamente
         */
        private void cancelTask(String reason) {
            if (cancelled)
                return;
            cancelled = true;

            if (countdownTask != null) {
                countdownTask.cancel();
            }

            pendingTeleports.remove(player.getUniqueId());

            if (onCancel != null && player.isOnline()) {
                onCancel.accept(player);
            }
        }
    }

    // ==================== DEBUG ====================

    /**
     * Retorna informações de debug do manager
     */
    public String getDebugInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("TeleportManager Debug:\n");
        sb.append("  Teleportes pendentes: ").append(pendingTeleports.size()).append("\n");
        sb.append("  Delays configurados:\n");
        for (Map.Entry<String, Integer> entry : teleportDelays.entrySet()) {
            sb.append("    - ").append(entry.getKey()).append(": ").append(entry.getValue()).append("s\n");
        }
        return sb.toString();
    }
}
