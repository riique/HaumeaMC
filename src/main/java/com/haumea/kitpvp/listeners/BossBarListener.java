package com.haumea.kitpvp.listeners;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.managers.BossBarManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Listener para gerenciar a Boss Bar dos jogadores.
 * Mostra a Boss Bar ao entrar e remove ao sair.
 * 
 * - LOBBY: Exibe "✦ LOBBY ✦" em verde
 * - KITPVP: Exibe "⚔ KITPVP ⚔" em vermelho
 * 
 * @author HaumeaMC
 */
public class BossBarListener implements Listener {

    private final HaumeaMC plugin;
    private final BossBarManager bossBarManager;

    // Textos da Boss Bar por tipo de servidor
    private static final String LOBBY_TEXT = "&a&l✦ LOBBY ✦";
    private static final String KITPVP_TEXT = "&c&l⚔ KITPVP ⚔";

    public BossBarListener(HaumeaMC plugin) {
        this.plugin = plugin;
        this.bossBarManager = plugin.getBossBarManager();
    }

    /**
     * Mostra a Boss Bar ao jogador entrar
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (bossBarManager == null || !bossBarManager.isAvailable()) {
            return;
        }

        final Player player = event.getPlayer();

        // Delay para garantir que o jogador está totalmente carregado
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    return;
                }

                // Determinar texto baseado no tipo de servidor
                String text = plugin.isLobby() ? LOBBY_TEXT : KITPVP_TEXT;

                // Mostrar Boss Bar com 100% de progresso (barra cheia)
                bossBarManager.showBossBar(player, text, 1.0f);
            }
        }.runTaskLater(plugin, 20L); // 1 segundo de delay
    }

    /**
     * Remove a Boss Bar ao jogador sair
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (bossBarManager == null) {
            return;
        }

        bossBarManager.removeBossBar(event.getPlayer());
    }
}
