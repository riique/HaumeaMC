package com.haumea.kitpvp.managers;

import com.haumea.kitpvp.HaumeaMC;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gerenciador Central de Cooldowns do HaumeaMC.
 * 
 * Centraliza TODOS os cooldowns do servidor em um único lugar,
 * evitando duplicação de código e inconsistências.
 * 
 * Uso:
 * - plugin.getCooldownManager().isOnCooldown(player, CooldownManager.SKIN)
 * - plugin.getCooldownManager().setCooldown(player, CooldownManager.REPORT)
 * 
 * @author HaumeaMC
 */
public class CooldownManager {

    // ==================== SISTEMAS REGISTRADOS ====================

    /** Sistema de troca de skin */
    public static final String SKIN = "skin";

    /** Sistema de report */
    public static final String REPORT = "report";

    /** Sistema de fake nick */
    public static final String FAKE_NICK = "fakenick";

    /** Sistema de warp/teleporte */
    public static final String WARP = "warp";

    /** Sistema de soup/curar */
    public static final String SOUP = "soup";

    /** Sistema de kit */
    public static final String KIT = "kit";

    /** Sistema de comando geral */
    public static final String COMMAND = "command";

    // ==================== CAMPOS ====================

    private final HaumeaMC plugin;

    // Cache: "sistema:uuid" -> timestamp de quando acabará o cooldown
    private final Map<String, Long> cooldowns;

    // Configurações: sistema -> duração em ms
    private final Map<String, Long> cooldownDurations;

    // Bypass: sistema -> permissão para bypass
    private final Map<String, String> bypassPermissions;

    // ==================== CONSTRUTOR ====================

    public CooldownManager(HaumeaMC plugin) {
        this.plugin = plugin;
        this.cooldowns = new ConcurrentHashMap<>();
        this.cooldownDurations = new ConcurrentHashMap<>();
        this.bypassPermissions = new ConcurrentHashMap<>();

        // Registrar sistemas padrão
        registerDefaults();

        // Iniciar task de limpeza periódica
        startCleanupTask();
    }

    /**
     * Registra os sistemas de cooldown padrão
     * Carrega valores do config.yml se disponível
     */
    private void registerDefaults() {
        // Tentar carregar do config.yml
        if (plugin.getConfig().isConfigurationSection("cooldowns")) {
            loadFromConfig();
        } else {
            // Fallback para valores padrão hardcoded
            registerHardcodedDefaults();
        }

        plugin.getLogger().info("CooldownManager: " + cooldownDurations.size() + " sistemas registrados");
    }

    /**
     * Carrega cooldowns do config.yml
     */
    private void loadFromConfig() {
        // Comandos
        register(COMMAND, getConfigCooldown("cooldowns.comandos.tell", 2), "haumea.cooldown.bypass.command");
        register(REPORT, getConfigCooldown("cooldowns.comandos.report", 30), "haumea.cooldown.bypass.report");
        register("pay", getConfigCooldown("cooldowns.comandos.pay", 5), "haumea.cooldown.bypass.pay");
        register("trade", getConfigCooldown("cooldowns.comandos.trade", 10), "haumea.cooldown.bypass.trade");

        // Kits
        register(KIT, getConfigCooldown("cooldowns.kits.selecionar", 0), "haumea.cooldown.bypass.kit");
        register("ability", getConfigCooldown("cooldowns.kits.habilidade", 5), "haumea.cooldown.bypass.ability");

        // Teleporte
        register(WARP, getConfigCooldown("cooldowns.teleporte.warp", 3), "haumea.cooldown.bypass.warp");
        register("spawn", getConfigCooldown("cooldowns.teleporte.spawn", 3), "haumea.cooldown.bypass.spawn");

        // Combate
        register(SOUP, getConfigCooldown("cooldowns.combate.sopa", 0), null);

        // Itens
        register("supply", getConfigCooldown("cooldowns.itens.supply", 30), "haumea.cooldown.bypass.supply");

        // Sistemas adicionais (não configuráveis)
        register(SKIN, 5000, "haumea.cooldown.bypass.skin");
        register(FAKE_NICK, 60000, "haumea.cooldown.bypass.fake");
    }

    /**
     * Obtém um cooldown do config em milissegundos
     */
    private long getConfigCooldown(String path, int defaultSeconds) {
        int seconds = plugin.getConfig().getInt(path, defaultSeconds);
        if (seconds < 0)
            return 0; // -1 = desativado
        return seconds * 1000L;
    }

    /**
     * Registra valores padrão hardcoded (fallback)
     */
    private void registerHardcodedDefaults() {
        register(SKIN, 5000, "haumea.cooldown.bypass.skin");
        register(REPORT, 30000, "haumea.cooldown.bypass.report");
        register(FAKE_NICK, 60000, "haumea.cooldown.bypass.fake");
        register(WARP, 3000, "haumea.cooldown.bypass.warp");
        register(SOUP, 0, null);
        register(KIT, 5000, "haumea.cooldown.bypass.kit");
        register(COMMAND, 1000, "haumea.cooldown.bypass.command");
    }

    /**
     * Inicia task para limpar cooldowns expirados a cada 5 minutos
     */
    private void startCleanupTask() {
        // Usar runTaskTimer (sync) em vez de async para compatibilidade com Java
        // 8/Spigot 1.8
        plugin.getServer().getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                cleanup();
            }
        }, 20L * 60L * 5L, 20L * 60L * 5L);
    }

    // ==================== REGISTRO DE SISTEMAS ====================

    /**
     * Registra um novo sistema de cooldown
     * 
     * @param system           Nome do sistema (use constantes)
     * @param durationMs       Duração em milissegundos (0 = sem cooldown)
     * @param bypassPermission Permissão para bypass (null = sem bypass por
     *                         permissão)
     */
    public void register(String system, long durationMs, String bypassPermission) {
        cooldownDurations.put(system.toLowerCase(), durationMs);
        if (bypassPermission != null && !bypassPermission.isEmpty()) {
            bypassPermissions.put(system.toLowerCase(), bypassPermission);
        }
    }

    /**
     * Registra um sistema sem permissão de bypass
     */
    public void register(String system, long durationMs) {
        register(system, durationMs, null);
    }

    /**
     * Obtém a duração configurada para um sistema
     * 
     * @param system Nome do sistema
     * @return Duração em milissegundos ou 0 se não registrado
     */
    public long getDuration(String system) {
        return cooldownDurations.getOrDefault(system.toLowerCase(), 0L);
    }

    // ==================== VERIFICAÇÃO DE COOLDOWN ====================

    /**
     * Verifica se um jogador está em cooldown para um sistema
     * 
     * @param player Jogador a verificar
     * @param system Sistema de cooldown
     * @return true se está em cooldown
     */
    public boolean isOnCooldown(Player player, String system) {
        if (player == null)
            return false;

        // Sistema com duração 0 = sem cooldown
        if (getDuration(system) == 0) {
            return false;
        }

        // Verificar bypass por permissão específica
        String bypassPerm = bypassPermissions.get(system.toLowerCase());
        if (bypassPerm != null && player.hasPermission(bypassPerm)) {
            return false;
        }

        // Verificar bypass global
        if (player.hasPermission("haumea.cooldown.bypass.*")) {
            return false;
        }

        // Verificar bypass por grupo staff (dono, diretor, gerente)
        if (hasStaffBypass(player)) {
            return false;
        }

        return isOnCooldownRaw(player.getUniqueId(), system);
    }

    /**
     * Verifica cooldown por UUID (sem verificar bypass)
     */
    public boolean isOnCooldown(UUID uuid, String system) {
        if (getDuration(system) == 0) {
            return false;
        }
        return isOnCooldownRaw(uuid, system);
    }

    /**
     * Verificação raw de cooldown (interno)
     */
    private boolean isOnCooldownRaw(UUID uuid, String system) {
        String key = buildKey(system, uuid);
        Long endTime = cooldowns.get(key);

        if (endTime == null)
            return false;

        if (System.currentTimeMillis() >= endTime) {
            // Expirou, remover do cache
            cooldowns.remove(key);
            return false;
        }

        return true;
    }

    // ==================== TEMPO RESTANTE ====================

    /**
     * Obtém o tempo restante de cooldown em milissegundos
     * 
     * @param player Jogador
     * @param system Sistema
     * @return Tempo restante em ms (0 se não está em cooldown)
     */
    public long getRemainingTime(Player player, String system) {
        return getRemainingTime(player.getUniqueId(), system);
    }

    /**
     * Obtém o tempo restante por UUID
     */
    public long getRemainingTime(UUID uuid, String system) {
        String key = buildKey(system, uuid);
        Long endTime = cooldowns.get(key);

        if (endTime == null)
            return 0;

        long remaining = endTime - System.currentTimeMillis();
        return Math.max(0, remaining);
    }

    /**
     * Obtém o tempo restante em segundos (arredondado para cima)
     */
    public int getRemainingSeconds(Player player, String system) {
        return (int) Math.ceil(getRemainingTime(player, system) / 1000.0);
    }

    /**
     * Obtém o tempo restante em segundos por UUID
     */
    public int getRemainingSeconds(UUID uuid, String system) {
        return (int) Math.ceil(getRemainingTime(uuid, system) / 1000.0);
    }

    /**
     * Obtém o tempo restante formatado (ex: "5s", "1m 30s")
     */
    public String getRemainingFormatted(Player player, String system) {
        long ms = getRemainingTime(player, system);
        if (ms <= 0)
            return "0s";

        long seconds = ms / 1000;
        if (seconds < 60) {
            return seconds + "s";
        }

        long minutes = seconds / 60;
        seconds = seconds % 60;

        if (seconds == 0) {
            return minutes + "m";
        }
        return minutes + "m " + seconds + "s";
    }

    // ==================== APLICAR COOLDOWN ====================

    /**
     * Aplica cooldown a um jogador
     * 
     * @param player Jogador
     * @param system Sistema
     */
    public void setCooldown(Player player, String system) {
        setCooldown(player.getUniqueId(), system);
    }

    /**
     * Aplica cooldown por UUID
     */
    public void setCooldown(UUID uuid, String system) {
        Long duration = cooldownDurations.get(system.toLowerCase());
        if (duration == null || duration == 0) {
            return; // Sistema não registrado ou sem cooldown
        }

        String key = buildKey(system, uuid);
        cooldowns.put(key, System.currentTimeMillis() + duration);
    }

    /**
     * Aplica cooldown com duração customizada
     * 
     * @param player     Jogador
     * @param system     Sistema
     * @param durationMs Duração em milissegundos
     */
    public void setCooldown(Player player, String system, long durationMs) {
        setCooldown(player.getUniqueId(), system, durationMs);
    }

    /**
     * Aplica cooldown com duração customizada por UUID
     */
    public void setCooldown(UUID uuid, String system, long durationMs) {
        if (durationMs <= 0)
            return;

        String key = buildKey(system, uuid);
        cooldowns.put(key, System.currentTimeMillis() + durationMs);
    }

    // ==================== REMOVER COOLDOWN ====================

    /**
     * Remove cooldown de um jogador para um sistema
     */
    public void removeCooldown(Player player, String system) {
        removeCooldown(player.getUniqueId(), system);
    }

    /**
     * Remove cooldown por UUID
     */
    public void removeCooldown(UUID uuid, String system) {
        String key = buildKey(system, uuid);
        cooldowns.remove(key);
    }

    /**
     * Remove TODOS os cooldowns de um jogador
     */
    public void removeAllCooldowns(Player player) {
        removeAllCooldowns(player.getUniqueId());
    }

    /**
     * Remove TODOS os cooldowns por UUID
     */
    public void removeAllCooldowns(UUID uuid) {
        String suffix = ":" + uuid.toString();
        cooldowns.keySet().removeIf(key -> key.endsWith(suffix));
    }

    // ==================== HELPERS ====================

    /**
     * Constrói a chave do cache
     */
    private String buildKey(String system, UUID uuid) {
        return system.toLowerCase() + ":" + uuid.toString();
    }

    /**
     * Verifica se o jogador é staff com bypass automático.
     * Usa o GroupManager para verificação centralizada.
     */
    private boolean hasStaffBypass(Player player) {
        if (plugin.getGroupManager() == null) {
            return false;
        }
        // Gerentes e superiores (dono, diretor, gerente) têm bypass
        return plugin.getGroupManager().isManager(player);
    }

    /**
     * Limpa cooldowns expirados do cache (manutenção)
     */
    public void cleanup() {
        long now = System.currentTimeMillis();
        int removed = 0;

        Iterator<Map.Entry<String, Long>> iterator = cooldowns.entrySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getValue() < now) {
                iterator.remove();
                removed++;
            }
        }

        if (removed > 0) {
            plugin.getLogger().fine("CooldownManager: " + removed + " cooldowns expirados limpos");
        }
    }

    /**
     * Obtém estatísticas de debug
     */
    public String getDebugInfo() {
        return String.format("CooldownManager: %d cooldowns ativos, %d sistemas registrados",
                cooldowns.size(), cooldownDurations.size());
    }

    /**
     * Chamado quando o plugin desliga
     */
    public void shutdown() {
        cooldowns.clear();
    }

    /**
     * Chamado quando um jogador sai do servidor.
     * Limpa cooldowns expirados deste jogador para economizar memória.
     * 
     * @param player Jogador que saiu
     */
    public void onPlayerQuit(Player player) {
        if (player == null)
            return;

        UUID uuid = player.getUniqueId();
        String suffix = ":" + uuid.toString();
        long now = System.currentTimeMillis();

        // Remover apenas cooldowns EXPIRADOS deste jogador
        cooldowns.entrySet().removeIf(entry -> entry.getKey().endsWith(suffix) && entry.getValue() < now);
    }
}
