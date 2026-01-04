package com.haumea.kitpvp.managers;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.models.Warp;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.EnumSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gerenciador Central de Estado do Jogador do HaumeaMC.
 * 
 * Centraliza TODOS os estados temporários de um jogador em um único lugar:
 * - Lobby/Spawn (área protegida)
 * - Combate (recebeu/deu dano recentemente)
 * - Arena (em área de PvP)
 * - Modo Admin (staff em serviço)
 * - Congelado (frozen por staff)
 * - Teleportando (em processo de teleporte)
 * 
 * Uso:
 * - plugin.getStateManager().isInSpawn(player)
 * - plugin.getStateManager().isProtected(player)
 * - plugin.getStateManager().setState(player, PlayerState.COMBAT)
 * 
 * @author HaumeaMC
 */
public class PlayerStateManager {

    // ==================== ESTADOS POSSÍVEIS ====================

    /**
     * Estados possíveis de um jogador
     */
    public enum PlayerState {
        /** Jogador está no spawn/lobby (protegido) */
        LOBBY,

        /** Jogador está em combate (recebeu/deu dano recentemente) */
        COMBAT,

        /** Jogador está na arena (pode PvP) */
        ARENA,

        /** Staff está em modo admin (invisível, god, voando) */
        ADMIN_MODE,

        /** Jogador está congelado por um staff */
        FROZEN,

        /** Jogador está em processo de teleporte */
        TELEPORTING,

        /** Jogador está em modo espectador */
        SPECTATING,

        /** Jogador está AFK */
        AFK,

        /** Jogador está em modo build (pode construir) */
        BUILD_MODE
    }

    // ==================== CAMPOS ====================

    private final HaumeaMC plugin;

    // Cache de estados: UUID -> Set de estados ativos
    private final Map<UUID, EnumSet<PlayerState>> playerStates;

    // Cache de combate: UUID -> timestamp de quando o combate expira
    private final Map<UUID, Long> combatTimers;

    // Cache de último atacante: UUID da vítima -> UUID do atacante
    private final Map<UUID, UUID> lastAttackers;

    // Tempo de duração do estado de combate (10 segundos)
    private static final long COMBAT_DURATION_MS = 10000;

    // Raio do spawn para detecção automática
    private double spawnRadius = 50.0;

    // ==================== CONSTRUTOR ====================

    public PlayerStateManager(HaumeaMC plugin) {
        this.plugin = plugin;
        this.playerStates = new ConcurrentHashMap<>();
        this.combatTimers = new ConcurrentHashMap<>();
        this.lastAttackers = new ConcurrentHashMap<>();

        // Carregar configurações do spawn
        loadConfig();

        plugin.getLogger().info("PlayerStateManager inicializado (raio=" + spawnRadius + ")");
    }

    /**
     * Carrega configurações do config.yml
     */
    public void loadConfig() {
        if (plugin.getConfig() != null) {
            spawnRadius = plugin.getConfig().getDouble("spawn.raio", 50.0);
        }
    }

    // ==================== GERENCIAMENTO DE ESTADOS ====================

    /**
     * Verifica se o jogador tem um estado específico
     */
    public boolean hasState(Player player, PlayerState state) {
        if (player == null)
            return false;

        // Estado de combate tem lógica especial (expira com tempo)
        if (state == PlayerState.COMBAT) {
            return isInCombat(player);
        }

        EnumSet<PlayerState> states = playerStates.get(player.getUniqueId());
        return states != null && states.contains(state);
    }

    /**
     * Verifica se o jogador tem um estado por UUID
     */
    public boolean hasState(UUID uuid, PlayerState state) {
        if (state == PlayerState.COMBAT) {
            return isInCombatRaw(uuid);
        }

        EnumSet<PlayerState> states = playerStates.get(uuid);
        return states != null && states.contains(state);
    }

    /**
     * Adiciona um estado ao jogador
     */
    public void setState(Player player, PlayerState state) {
        if (player == null)
            return;
        setState(player.getUniqueId(), state);
    }

    /**
     * Adiciona um estado por UUID
     */
    public void setState(UUID uuid, PlayerState state) {
        playerStates.computeIfAbsent(uuid, k -> EnumSet.noneOf(PlayerState.class)).add(state);
    }

    /**
     * Remove um estado do jogador
     */
    public void removeState(Player player, PlayerState state) {
        if (player == null)
            return;
        removeState(player.getUniqueId(), state);
    }

    /**
     * Remove um estado por UUID
     */
    public void removeState(UUID uuid, PlayerState state) {
        EnumSet<PlayerState> states = playerStates.get(uuid);
        if (states != null) {
            states.remove(state);
            if (states.isEmpty()) {
                playerStates.remove(uuid);
            }
        }

        // Limpar dados relacionados ao combate se necessário
        if (state == PlayerState.COMBAT) {
            combatTimers.remove(uuid);
            lastAttackers.remove(uuid);
        }
    }

    /**
     * Obtém todos os estados de um jogador
     */
    public EnumSet<PlayerState> getAllStates(Player player) {
        if (player == null)
            return EnumSet.noneOf(PlayerState.class);
        return getAllStates(player.getUniqueId());
    }

    /**
     * Obtém todos os estados por UUID
     */
    public EnumSet<PlayerState> getAllStates(UUID uuid) {
        EnumSet<PlayerState> states = playerStates.get(uuid);
        if (states == null) {
            return EnumSet.noneOf(PlayerState.class);
        }
        // Retornar cópia para evitar modificação externa
        return EnumSet.copyOf(states);
    }

    /**
     * Limpa todos os estados de um jogador
     */
    public void clearAllStates(Player player) {
        if (player == null)
            return;
        clearAllStates(player.getUniqueId());
    }

    /**
     * Limpa todos os estados por UUID
     */
    public void clearAllStates(UUID uuid) {
        playerStates.remove(uuid);
        combatTimers.remove(uuid);
        lastAttackers.remove(uuid);
    }

    // ==================== MÉTODOS DE CONVENIÊNCIA ====================

    /**
     * Verifica se o jogador está no spawn/lobby
     * 
     * @param player Jogador
     * @return true se está protegido no spawn
     */
    public boolean isInSpawn(Player player) {
        // Primeiro verifica estado manual
        if (hasState(player, PlayerState.LOBBY)) {
            return true;
        }

        // Depois verifica por localização
        return isInSpawnByLocation(player);
    }

    /**
     * Verifica se o jogador está em combate
     */
    public boolean isInCombat(Player player) {
        if (player == null)
            return false;
        return isInCombatRaw(player.getUniqueId());
    }

    /**
     * Verificação raw de combate
     */
    private boolean isInCombatRaw(UUID uuid) {
        Long endTime = combatTimers.get(uuid);
        if (endTime == null)
            return false;

        if (System.currentTimeMillis() >= endTime) {
            // Combate expirou
            combatTimers.remove(uuid);
            lastAttackers.remove(uuid);
            removeState(uuid, PlayerState.COMBAT);
            return false;
        }

        return true;
    }

    /**
     * Verifica se o jogador está protegido (não pode receber dano)
     * Um jogador está protegido se:
     * - Está no spawn/lobby
     * - Está em modo admin
     * - Está em modo espectador
     */
    public boolean isProtected(Player player) {
        return isInSpawn(player) ||
                hasState(player, PlayerState.ADMIN_MODE) ||
                hasState(player, PlayerState.SPECTATING);
    }

    /**
     * Verifica se o jogador pode fazer PvP
     */
    public boolean canPvP(Player player) {
        return !isProtected(player) && !hasState(player, PlayerState.FROZEN);
    }

    /**
     * Verifica se o jogador está em modo admin
     */
    public boolean isInAdminMode(Player player) {
        return hasState(player, PlayerState.ADMIN_MODE);
    }

    /**
     * Verifica se o jogador está congelado
     */
    public boolean isFrozen(Player player) {
        return hasState(player, PlayerState.FROZEN);
    }

    /**
     * Verifica se o jogador pode construir
     */
    public boolean canBuild(Player player) {
        return hasState(player, PlayerState.BUILD_MODE);
    }

    // ==================== SISTEMA DE COMBATE ====================

    /**
     * Marca o jogador como em combate
     * 
     * @param player   Jogador que entrou em combate
     * @param attacker Atacante (pode ser null)
     */
    public void enterCombat(Player player, Player attacker) {
        if (player == null)
            return;

        UUID uuid = player.getUniqueId();
        combatTimers.put(uuid, System.currentTimeMillis() + COMBAT_DURATION_MS);
        setState(uuid, PlayerState.COMBAT);

        if (attacker != null) {
            lastAttackers.put(uuid, attacker.getUniqueId());
        }
    }

    /**
     * Remove o jogador do estado de combate
     */
    public void leaveCombat(Player player) {
        if (player == null)
            return;
        removeState(player.getUniqueId(), PlayerState.COMBAT);
    }

    /**
     * Obtém o tempo restante de combate em segundos
     */
    public int getCombatTimeRemaining(Player player) {
        if (player == null)
            return 0;

        Long endTime = combatTimers.get(player.getUniqueId());
        if (endTime == null)
            return 0;

        long remaining = endTime - System.currentTimeMillis();
        return Math.max(0, (int) Math.ceil(remaining / 1000.0));
    }

    /**
     * Obtém o último atacante do jogador
     */
    public Player getLastAttacker(Player player) {
        if (player == null)
            return null;

        UUID attackerUuid = lastAttackers.get(player.getUniqueId());
        if (attackerUuid == null)
            return null;

        return plugin.getServer().getPlayer(attackerUuid);
    }

    /**
     * Obtém o UUID do último atacante
     */
    public UUID getLastAttackerUuid(Player player) {
        if (player == null)
            return null;
        return lastAttackers.get(player.getUniqueId());
    }

    // ==================== SISTEMA DE SPAWN ====================

    /**
     * Define o raio de detecção do spawn
     */
    public void setSpawnRadius(double radius) {
        this.spawnRadius = radius;
    }

    /**
     * Obtém o raio de detecção do spawn
     */
    public double getSpawnRadius() {
        return spawnRadius;
    }

    /**
     * Verifica se o jogador está no spawn por localização
     */
    public boolean isInSpawnByLocation(Player player) {
        if (player == null)
            return false;

        // Obter localização do spawn
        Location spawnLocation = getSpawnLocation();
        if (spawnLocation == null)
            return false;

        Location playerLoc = player.getLocation();

        // Verificar se está no mesmo mundo
        if (!playerLoc.getWorld().equals(spawnLocation.getWorld())) {
            return false;
        }

        // Verificar distância (ignorando Y para permitir spawn em altura)
        double dx = playerLoc.getX() - spawnLocation.getX();
        double dz = playerLoc.getZ() - spawnLocation.getZ();
        double distanceSquared = dx * dx + dz * dz;

        return distanceSquared <= (spawnRadius * spawnRadius);
    }

    /**
     * Obtém a localização do spawn
     */
    private Location getSpawnLocation() {
        if (plugin.getWarpsManager() == null)
            return null;

        Warp spawnWarp = plugin.getWarpsManager().getWarp("spawn");
        if (spawnWarp == null)
            return null;

        return spawnWarp.toLocation();
    }

    /**
     * Atualiza o estado do jogador baseado em sua localização
     * Chamado quando o jogador se move
     */
    public void updateFromLocation(Player player) {
        if (player == null)
            return;

        boolean inSpawn = isInSpawnByLocation(player);
        boolean hasLobbyState = hasState(player, PlayerState.LOBBY);

        if (inSpawn && !hasLobbyState) {
            // Entrou no spawn
            setState(player, PlayerState.LOBBY);
            removeState(player, PlayerState.ARENA);
        } else if (!inSpawn && hasLobbyState) {
            // Saiu do spawn
            removeState(player, PlayerState.LOBBY);
            setState(player, PlayerState.ARENA);
        }
    }

    // ==================== MODO ADMIN ====================

    /**
     * Ativa o modo admin para um jogador
     */
    public void enableAdminMode(Player player) {
        setState(player, PlayerState.ADMIN_MODE);
    }

    /**
     * Desativa o modo admin de um jogador
     */
    public void disableAdminMode(Player player) {
        removeState(player, PlayerState.ADMIN_MODE);
    }

    // ==================== FREEZE ====================

    /**
     * Congela um jogador
     */
    public void freeze(Player player) {
        setState(player, PlayerState.FROZEN);
    }

    /**
     * Descongela um jogador
     */
    public void unfreeze(Player player) {
        removeState(player, PlayerState.FROZEN);
    }

    // ==================== BUILD MODE ====================

    /**
     * Ativa o modo build para um jogador
     */
    public void enableBuildMode(Player player) {
        setState(player, PlayerState.BUILD_MODE);
    }

    /**
     * Desativa o modo build de um jogador
     */
    public void disableBuildMode(Player player) {
        removeState(player, PlayerState.BUILD_MODE);
    }

    /**
     * Alterna o modo build
     */
    public boolean toggleBuildMode(Player player) {
        if (hasState(player, PlayerState.BUILD_MODE)) {
            disableBuildMode(player);
            return false;
        } else {
            enableBuildMode(player);
            return true;
        }
    }

    // ==================== EVENTOS DE CICLO DE VIDA ====================

    /**
     * Chamado quando o jogador entra no servidor
     */
    public void onPlayerJoin(Player player) {
        // Iniciar no estado de lobby (spawn)
        setState(player, PlayerState.LOBBY);
    }

    /**
     * Chamado quando o jogador sai do servidor
     */
    public void onPlayerQuit(Player player) {
        clearAllStates(player);
    }

    // ==================== DEBUG E ESTATÍSTICAS ====================

    /**
     * Obtém informações de debug
     */
    public String getDebugInfo() {
        return String.format("PlayerStateManager: %d jogadores rastreados, %d em combate",
                playerStates.size(), combatTimers.size());
    }

    /**
     * Obtém uma descrição legível dos estados de um jogador
     */
    public String getStateDescription(Player player) {
        if (player == null)
            return "N/A";

        EnumSet<PlayerState> states = getAllStates(player);
        if (states.isEmpty())
            return "Nenhum estado";

        StringBuilder sb = new StringBuilder();
        for (PlayerState state : states) {
            if (sb.length() > 0)
                sb.append(", ");
            sb.append(state.name());
        }

        // Adicionar info de combate se aplicável
        if (isInCombat(player)) {
            sb.append(" (").append(getCombatTimeRemaining(player)).append("s)");
        }

        return sb.toString();
    }

    /**
     * Chamado quando o plugin desliga
     */
    public void shutdown() {
        playerStates.clear();
        combatTimers.clear();
        lastAttackers.clear();
    }
}
