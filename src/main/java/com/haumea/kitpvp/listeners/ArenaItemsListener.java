package com.haumea.kitpvp.listeners;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.utils.ItemActionHandler;
import com.haumea.kitpvp.utils.ItemBuilder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Listener responsável por processar interações com itens de arena.
 * 
 * Este listener:
 * - Processa cliques em itens interativos e executa os handlers correspondentes
 * - Dá itens de lobby ao jogador quando entra ou respawna
 * 
 * Funciona em conjunto com:
 * - {@link ItemBuilder} para criar itens com action IDs
 * - {@link ItemActionHandler} para executar os handlers
 * - ArenaItemsHandler para gerenciar os itens
 * 
 * @author HaumeaMC
 */
public class ArenaItemsListener implements Listener {

    private final HaumeaMC plugin;

    public ArenaItemsListener(HaumeaMC plugin) {
        this.plugin = plugin;
    }

    /**
     * Processa interações com itens interativos.
     * Extrai o action ID do item e executa o handler correspondente.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Verificar se é clique (direito ou esquerdo)
        Action action = event.getAction();
        if (action == Action.PHYSICAL) {
            return; // Ignorar pisadas em placas de pressão
        }

        Player player = event.getPlayer();
        ItemStack item = player.getItemInHand();

        // Verificar se o item existe e tem action ID
        if (item == null || item.getType() == org.bukkit.Material.AIR) {
            return;
        }

        String actionId = ItemBuilder.extractActionId(item);
        if (actionId == null) {
            return;
        }

        // Verificar se existe um handler para este action ID
        if (!ItemActionHandler.hasAction(actionId)) {
            return;
        }

        // Cancelar evento para evitar comportamento padrão
        event.setCancelled(true);

        // Executar o handler
        ItemActionHandler.executeAction(actionId, player);
    }

    /**
     * Dá itens de lobby quando o jogador entra no servidor.
     * NÃO dá itens se o jogador estiver em modo admin.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Dar itens de lobby após delay para garantir que:
        // 1. O perfil foi carregado
        // 2. O jogador foi teleportado ao spawn
        // 3. O modo admin já foi ativado (se aplicável)
        // 4. Tudo está sincronizado
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }

            // Verificar se está em modo admin - se sim, NÃO dar itens de lobby
            if (plugin.getProfileManager() != null) {
                com.haumea.kitpvp.profile.PlayerProfile profile = plugin.getProfileManager().getProfile(player);
                if (profile != null && profile.isVanish()) {
                    // Staff em modo admin - não dar itens de lobby
                    return;
                }
            }

            // Dar itens de lobby
            if (plugin.getArenaItemsHandler() != null) {
                plugin.getArenaItemsHandler().giveLobbyItems(player);
            }
        }, 10L); // 10 ticks = 0.5 segundos
    }

    /**
     * Dá itens de lobby quando o jogador respawna.
     * NÃO dá itens se o jogador estiver em modo admin.
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        // Dar itens de lobby após respawn
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }

            // Verificar se está em modo admin - se sim, NÃO dar itens de lobby
            if (plugin.getProfileManager() != null) {
                com.haumea.kitpvp.profile.PlayerProfile profile = plugin.getProfileManager().getProfile(player);
                if (profile != null && profile.isVanish()) {
                    // Staff em modo admin - manter itens de admin
                    return;
                }
            }

            // Dar itens de lobby
            if (plugin.getArenaItemsHandler() != null) {
                plugin.getArenaItemsHandler().giveLobbyItems(player);
            }
        }, 2L); // 2 ticks = 0.1 segundos
    }
}
