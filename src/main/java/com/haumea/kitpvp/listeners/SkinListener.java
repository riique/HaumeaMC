package com.haumea.kitpvp.listeners;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.managers.SkinManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Listener para eventos relacionados a skins.
 * 
 * Responsável por:
 * - Restaurar skins salvas quando jogador entra
 * 
 * @author HaumeaMC
 */
public class SkinListener implements Listener {

    private final SkinManager skinManager;

    public SkinListener(HaumeaMC plugin) {
        this.skinManager = plugin.getSkinManager();
    }

    /**
     * Restaura a skin salva quando o jogador entra no servidor
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Restaurar skin salva (se existir)
        if (skinManager != null) {
            skinManager.restoreSavedSkin(event.getPlayer());
        }
    }
}
