package com.haumea.kitpvp.listeners;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.abilities.Ability;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Listener responsável pelo sistema de regeneração por sopa (KitPvP style).
 * 
 * Funcionalidades:
 * - Clique direito com sopa regenera 3.5 corações (7.0 de vida)
 * - Se a vida estiver cheia, regenera 7 pontos de fome
 * - A sopa é transformada em tigela (bowl) após uso
 * - Kit Quickdrop: Bowl é dropado automaticamente no chão
 * 
 * @author HaumeaMC
 */
public class SoupListener implements Listener {

    private final HaumeaMC plugin;

    /**
     * Quantidade de vida recuperada por sopa (3.5 corações = 7.0 de vida)
     */
    private static final double HEALTH_REGEN = 7.0;

    /**
     * Quantidade de fome recuperada por sopa
     */
    private static final int FOOD_REGEN = 7;

    /**
     * Vida máxima do jogador
     */
    private static final double MAX_HEALTH = 20.0;

    /**
     * Fome máxima do jogador
     */
    private static final int MAX_FOOD = 20;

    public SoupListener(HaumeaMC plugin) {
        this.plugin = plugin;
    }

    /**
     * Processa o uso de sopa para regeneração de vida/fome.
     * 
     * @param event Evento de interação do jogador
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Verificar se é clique direito (ar ou bloco)
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack itemInHand = player.getItemInHand();

        // Verificar se o item na mão é sopa de cogumelo
        if (itemInHand == null || itemInHand.getType() != Material.MUSHROOM_SOUP) {
            return;
        }

        double currentHealth = player.getHealth();
        int currentFood = player.getFoodLevel();

        // Verificar se precisa de cura (vida não está cheia)
        if (currentHealth < MAX_HEALTH) {
            // Calcular nova vida (não ultrapassar o máximo)
            double newHealth = Math.min(currentHealth + HEALTH_REGEN, MAX_HEALTH);
            player.setHealth(newHealth);

            // Transformar sopa em tigela
            consumeSoup(player, itemInHand);

            // Cancelar evento para evitar comportamento padrão
            event.setCancelled(true);
            return;
        }

        // Se vida está cheia, verificar fome
        if (currentFood < MAX_FOOD) {
            // Calcular nova fome (não ultrapassar o máximo)
            int newFood = Math.min(currentFood + FOOD_REGEN, MAX_FOOD);
            player.setFoodLevel(newFood);

            // Também restaurar saturação para evitar perda rápida de fome
            player.setSaturation(Math.min(player.getSaturation() + 7.0f, (float) newFood));

            // Transformar sopa em tigela
            consumeSoup(player, itemInHand);

            // Cancelar evento para evitar comportamento padrão
            event.setCancelled(true);
        }

        // Se vida e fome estão cheias, não faz nada (mantém a sopa)
    }

    /**
     * Consome a sopa e a transforma em uma tigela vazia.
     * Se o jogador tem o kit Quickdrop, dropa o bowl no chão automaticamente.
     * 
     * @param player   Jogador que usou a sopa
     * @param soupItem Item de sopa a ser consumido
     */
    private void consumeSoup(Player player, ItemStack soupItem) {
        // Verificar se o jogador tem o kit Quickdrop
        boolean hasQuickdrop = Ability.hasAbility(player, "Quickdrop", plugin);

        // Se a quantidade for 1, substituir diretamente por tigela ou dropar
        if (soupItem.getAmount() == 1) {
            if (hasQuickdrop) {
                // Quickdrop: remover item e dropar bowl
                player.setItemInHand(null);
                player.getWorld().dropItemNaturally(player.getLocation(), new ItemStack(Material.BOWL));
            } else {
                // Normal: substituir por bowl na mão
                player.setItemInHand(new ItemStack(Material.BOWL));
            }
        } else {
            // Se houver mais de uma sopa, decrementar
            soupItem.setAmount(soupItem.getAmount() - 1);

            if (hasQuickdrop) {
                // Quickdrop: dropar bowl
                player.getWorld().dropItemNaturally(player.getLocation(), new ItemStack(Material.BOWL));
            } else {
                // Normal: adicionar bowl ao inventário
                player.getInventory().addItem(new ItemStack(Material.BOWL));
            }
        }

        // Atualizar inventário para garantir sincronização
        player.updateInventory();
    }
}
