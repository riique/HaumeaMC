package com.haumea.kitpvp.listeners;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.managers.CasinoManager;
import com.haumea.kitpvp.menu.casino.CasinoMainMenu;
import com.haumea.kitpvp.utils.ItemBuilder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Listener para eventos relacionados ao Cassino.
 * 
 * @author HaumeaMC
 */
public class CasinoListener implements Listener {

    private final HaumeaMC plugin;
    private final CasinoManager casinoManager;

    public CasinoListener(HaumeaMC plugin) {
        this.plugin = plugin;
        this.casinoManager = plugin.getCasinoManager();
    }

    /**
     * Verifica se um item é o item do cassino.
     */
    public static boolean isCasinoItem(ItemStack item) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
            return false;
        }

        String name = item.getItemMeta().getDisplayName();
        return name.contains("CASSINO") || name.contains("✦");
    }

    /**
     * Abre o menu do cassino para o jogador.
     */
    public void openCasino(Player player) {
        if (!casinoManager.isEnabled()) {
            return;
        }

        new CasinoMainMenu(plugin, player).open();
    }

    /**
     * Limpar sessões quando o jogador sair.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        casinoManager.cleanupSession(player);
    }
}
