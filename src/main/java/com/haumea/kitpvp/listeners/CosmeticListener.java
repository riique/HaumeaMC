package com.haumea.kitpvp.listeners;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.managers.CosmeticManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Listener para integração do sistema de cosméticos com eventos do jogo.
 * 
 * Responsável por:
 * - Aplicar efeitos de kill quando jogador mata outro
 * - Carregar cache de cosméticos no login
 * - Limpar cache de cosméticos no logout
 * 
 * @author HaumeaMC
 */
public class CosmeticListener implements Listener {

    private final HaumeaMC plugin;
    private final CosmeticManager cosmeticManager;

    public CosmeticListener(HaumeaMC plugin) {
        this.plugin = plugin;
        this.cosmeticManager = plugin.getCosmeticManager();
    }

    /**
     * Aplica efeitos de kill quando um jogador mata outro.
     * Prioridade MONITOR para garantir que outros listeners já processaram.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerKill(PlayerDeathEvent event) {
        if (cosmeticManager == null)
            return;

        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        // Verificar se foi morte por outro jogador
        if (killer == null || killer.equals(victim))
            return;

        // Aplicar efeitos de kill do killer
        cosmeticManager.applyKillEffects(killer, victim);
    }

    /**
     * Carrega o cache de cosméticos quando o jogador entra.
     * Prioridade NORMAL para dar tempo do perfil carregar.
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (cosmeticManager == null)
            return;

        Player player = event.getPlayer();

        // Carregar cache com delay para garantir que o perfil está carregado
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                cosmeticManager.updatePlayerCache(player);
            }
        }, 20L); // 1 segundo de delay
    }

    /**
     * Limpa o cache de cosméticos quando o jogador sai.
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (cosmeticManager == null)
            return;

        cosmeticManager.clearPlayerCache(event.getPlayer());
    }
}
