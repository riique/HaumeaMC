package com.haumea.kitpvp.listeners;

import com.haumea.kitpvp.HaumeaMC;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Listener para eventos relacionados à TabList.
 * 
 * Responsável por:
 * - Configurar team e TabList quando um jogador entra
 * - Remover jogador dos teams quando sai
 * - Atualizar posição na lista conforme hierarquia
 * 
 * @author HaumeaMC
 */
public class TabListener implements Listener {

    private final HaumeaMC plugin;

    /**
     * Construtor do listener
     * 
     * @param plugin Instância do plugin
     */
    public TabListener(HaumeaMC plugin) {
        this.plugin = plugin;
    }

    /**
     * Configura a TabList quando um jogador entra no servidor.
     * Usa prioridade MONITOR para executar após outros plugins
     * terem processado o evento de login.
     * 
     * @param event Evento de entrada do jogador
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Pequeno delay para garantir que o jogador esteja totalmente carregado
        // e que as permissões já tenham sido aplicadas
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (event.getPlayer().isOnline()) {
                // Inicializar nametag via NametagManager (centralizado)
                if (plugin.getNametagManager() != null) {
                    plugin.getNametagManager().onPlayerJoin(event.getPlayer());
                }

                // Configurar team do jogador (ordenação por hierarquia) - fallback
                if (plugin.getTabManager() != null && plugin.getTabManager().isRunning()) {
                    plugin.getTabManager().updatePlayerTeam(event.getPlayer());

                    // Enviar header e footer
                    plugin.getTabManager().updateTabList(event.getPlayer());

                    // Atualizar todos os jogadores para refletir novo online count
                    plugin.getTabManager().updateAllPlayers();
                }
            }
        }, 10L); // 10 ticks = 0.5 segundos
    }

    /**
     * Remove o jogador do sistema de teams quando sai.
     * 
     * @param event Evento de saída do jogador
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Limpar cache do NametagManager
        if (plugin.getNametagManager() != null) {
            plugin.getNametagManager().onPlayerQuit(event.getPlayer());
        }

        // Remover jogador dos teams do TabManager
        if (plugin.getTabManager() != null) {
            plugin.getTabManager().removePlayerFromTeams(event.getPlayer());

            // Atualizar contagem para outros jogadores (com delay)
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (plugin.getTabManager().isRunning()) {
                    plugin.getTabManager().updateAllPlayers();
                    plugin.getTabManager().cleanupEmptyTeams();
                }
            }, 5L);
        }

        // Limpar teams vazios do NametagManager
        if (plugin.getNametagManager() != null) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                plugin.getNametagManager().cleanupEmptyTeams();
            }, 5L);
        }
    }
}
