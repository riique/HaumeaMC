package com.haumea.kitpvp.listeners;

import com.haumea.kitpvp.HaumeaMC;

import com.haumea.kitpvp.profile.PlayerProfile;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Listener central para regras do mundo KitPvP.
 * 
 * Gerencia todas as regras de ambiente do servidor:
 * - Proteção de blocos (mundo indestrutível)
 * - Sistema de fome desativado
 * - Ciclo solar fixo (dia permanente)
 * - Clima fixo (sem chuva)
 * 
 * @author HaumeaMC
 */
public class WorldListener implements Listener {

    private final HaumeaMC plugin;

    // Tempo fixo do mundo (6000 = meio dia)
    private static final long FIXED_TIME = 6000L;

    // Task de controle de tempo
    private org.bukkit.scheduler.BukkitTask timeControlTask;

    /**
     * Construtor do WorldListener
     * 
     * @param plugin Instância do plugin principal
     */
    public WorldListener(HaumeaMC plugin) {
        this.plugin = plugin;

        // Iniciar task de controle de tempo
        startTimeController();

        // Aplicar configurações iniciais aos mundos
        setupWorlds();
    }

    // ==================== PROTEÇÃO DE BLOCOS ====================

    /**
     * Bloqueia a quebra de blocos para jogadores comuns.
     * Apenas staff autorizado pode quebrar blocos.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();

        // Jogadores em creative sempre podem (para construção)
        if (player.getGameMode() == GameMode.CREATIVE && canBuild(player)) {
            return;
        }

        // Bloquear para todos os outros
        if (!canBuild(player)) {
            event.setCancelled(true);
        }
    }

    /**
     * Bloqueia a colocação de blocos para jogadores comuns.
     * Apenas staff autorizado pode colocar blocos.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();

        // Jogadores em creative sempre podem (para construção)
        if (player.getGameMode() == GameMode.CREATIVE && canBuild(player)) {
            return;
        }

        // Bloquear para todos os outros
        if (!canBuild(player)) {
            event.setCancelled(true);
        }
    }

    /**
     * Verifica se o jogador tem permissão para construir/destruir.
     * APENAS buildMode ativado via /build permite construir!
     * 
     * @param player Jogador a verificar
     * @return true se pode construir
     */
    private boolean canBuild(Player player) {
        // APENAS buildMode ativado permite construir (comando /build)
        PlayerProfile profile = plugin.getProfileManager().getProfile(player);
        return profile != null && profile.isBuildMode();
    }

    // ==================== SISTEMA DE FOME ====================

    /**
     * Desativa completamente o sistema de fome.
     * A barra de comida nunca diminui.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        // Cancelar qualquer mudança no nível de fome
        event.setCancelled(true);

        // Garantir que a barra está sempre cheia
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            player.setFoodLevel(20);
            player.setSaturation(20.0f);
        }
    }

    /**
     * Garante que jogadores entrem com fome cheia.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoinHunger(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        player.setFoodLevel(20);
        player.setSaturation(20.0f);
        player.setExhaustion(0.0f);
    }

    // ==================== CONTROLE DE TEMPO E CLIMA ====================

    /**
     * Bloqueia mudanças de clima (sem chuva/tempestade).
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onWeatherChange(WeatherChangeEvent event) {
        // Se está tentando mudar para chuva, cancelar
        if (event.toWeatherState()) {
            event.setCancelled(true);
        }
    }

    /**
     * Configura os mundos com as regras iniciais.
     */
    private void setupWorlds() {
        for (World world : Bukkit.getWorlds()) {
            // Definir tempo fixo
            world.setTime(FIXED_TIME);

            // Parar ciclo de tempo
            world.setGameRuleValue("doDaylightCycle", "false");

            // Desativar clima
            world.setStorm(false);
            world.setThundering(false);
            world.setWeatherDuration(Integer.MAX_VALUE);

            // Outras regras úteis para KitPvP
            world.setGameRuleValue("doFireTick", "false"); // Fogo não espalha
            world.setGameRuleValue("mobGriefing", "false"); // Mobs não destroem blocos
            world.setGameRuleValue("doMobSpawning", "false"); // Sem spawn de mobs
            world.setGameRuleValue("keepInventory", "false"); // Dropa itens ao morrer (KitPvP)

            plugin.getLogger().info("Mundo configurado: " + world.getName());
        }
    }

    /**
     * Inicia a task que mantém o tempo fixo.
     * Executa a cada 5 minutos para garantir sincronia.
     */
    private void startTimeController() {
        timeControlTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (World world : Bukkit.getWorlds()) {
                    // Reforçar tempo fixo
                    world.setTime(FIXED_TIME);

                    // Reforçar clima limpo
                    if (world.hasStorm() || world.isThundering()) {
                        world.setStorm(false);
                        world.setThundering(false);
                    }
                }
            }
        }.runTaskTimer(plugin, 20L * 60, 20L * 60 * 5); // Primeira execução após 1 min, depois a cada 5 min
    }

    /**
     * Desliga o WorldListener, cancelando tasks
     */
    public void shutdown() {
        if (timeControlTask != null) {
            timeControlTask.cancel();
            timeControlTask = null;
        }
    }

    // ==================== MÉTODOS PÚBLICOS ====================

    /**
     * Força a aplicação das regras do mundo.
     * Útil se um mundo for carregado depois do plugin.
     * 
     * @param world Mundo a configurar
     */
    public void applyRules(World world) {
        world.setTime(FIXED_TIME);
        world.setGameRuleValue("doDaylightCycle", "false");
        world.setStorm(false);
        world.setThundering(false);
        world.setWeatherDuration(Integer.MAX_VALUE);
        world.setGameRuleValue("doFireTick", "false");
        world.setGameRuleValue("mobGriefing", "false");
        world.setGameRuleValue("doMobSpawning", "false");
    }

    /**
     * Recarrega as configurações do mundo.
     * Útil para comandos de admin.
     */
    public void reloadWorldSettings() {
        setupWorlds();
        plugin.getLogger().info("Configurações de mundo recarregadas!");
    }
}
