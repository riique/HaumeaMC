package com.haumea.kitpvp.menu;

import com.haumea.kitpvp.HaumeaMC;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryHolder;

/**
 * Listener para processar eventos de inventário nos menus customizados.
 * 
 * Detecta automaticamente menus que estendem BaseMenu e
 * direciona os eventos para os métodos apropriados.
 * 
 * @author HaumeaMC
 */
public class MenuListener implements Listener {

    @SuppressWarnings("unused")
    public MenuListener(HaumeaMC plugin) {
        // Plugin reference kept for consistency with other listeners
    }

    /**
     * Processa cliques em inventários customizados
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getClickedInventory() == null)
            return;

        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof BaseMenu))
            return;

        BaseMenu menu = (BaseMenu) holder;

        // Cancelar todos os cliques se configurado
        if (menu.isCancelAllClicks()) {
            event.setCancelled(true);
        }

        // Verificar se o clique foi no inventário do menu (não no inventário do
        // jogador)
        if (!event.getClickedInventory().equals(event.getInventory())) {
            return;
        }

        // Processar o clique
        menu.onClick(
                menu.getPlayer(),
                event.getSlot(),
                event.getCurrentItem(),
                event.getClick());
    }

    /**
     * Impede arrastar itens em menus customizados
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof BaseMenu))
            return;

        BaseMenu menu = (BaseMenu) holder;
        if (menu.isCancelAllClicks()) {
            event.setCancelled(true);
        }
    }

    /**
     * Chamado quando o menu é fechado
     * Pode ser sobrescrito para cleanup
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof BaseMenu))
            return;

        // Pode adicionar lógica de cleanup aqui se necessário
    }
}
