package com.haumea.kitpvp.abilities.kits;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.abilities.Ability;
import com.haumea.kitpvp.abilities.AbilityRarity;
import com.haumea.kitpvp.abilities.Ejectable;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Kit Grappler (Gancho)
 * 
 * Raridade: LEGENDARY | Preço: 2.000 | Ícone: LEASH | Cooldown: 5s
 * 
 * Funcionalidade:
 * - Botão esquerdo: Marca a localização do gancho
 * - Botão direito: Puxa o jogador em direção ao gancho marcado
 * 
 * @author HaumeaMC
 */
public class GrapplerAbility extends Ability implements Ejectable {

    // Localização do gancho por jogador
    private final Map<UUID, Location> hookLocations = new HashMap<>();

    public GrapplerAbility(HaumeaMC plugin) {
        super(plugin, AbilityRarity.LEGENDARY, Material.LEASH, 5, 2000,
                new ItemStack[] { createGrapplerItem() });
    }

    private static ItemStack createGrapplerItem() {
        ItemStack item = new ItemStack(Material.LEASH);
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§a§lGrappler");
        java.util.List<String> lore = new java.util.ArrayList<>();
        lore.add("§7Clique esquerdo: Define ponto");
        lore.add("§7Clique direito: Puxa você");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (!hasAbility(player))
            return;
        if (!isMainItem(player.getItemInHand()))
            return;

        Action action = event.getAction();

        // Botão esquerdo - definir ponto do gancho
        if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
            event.setCancelled(true);

            // Obter bloco alvo
            @SuppressWarnings("deprecation")
            org.bukkit.block.Block target = player.getTargetBlock((java.util.HashSet<Byte>) null, 50);

            if (target != null && target.getType() != Material.AIR) {
                hookLocations.put(player.getUniqueId(), target.getLocation().add(0.5, 0.5, 0.5));
                player.sendMessage("§6§lHAUMEAMC§f Ponto do gancho definido!");
                player.playSound(player.getLocation(), Sound.NOTE_PLING, 1.0f, 2.0f);
            }
            return;
        }

        // Botão direito - puxar em direção ao gancho
        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true);

            Location hookLoc = hookLocations.get(player.getUniqueId());

            if (hookLoc == null) {
                player.sendMessage("§c§lHAUMEAMC§f Defina um ponto primeiro (clique esquerdo)!");
                return;
            }

            if (isInCooldown(player)) {
                sendCooldownMessage(player);
                return;
            }

            // Verificar se o gancho está no mesmo mundo
            if (!hookLoc.getWorld().equals(player.getWorld())) {
                hookLocations.remove(player.getUniqueId());
                player.sendMessage("§c§lHAUMEAMC§f O ponto do gancho está em outro mundo!");
                return;
            }

            putInCooldown(player);

            // Calcular velocidade para puxar
            Location playerLoc = player.getLocation();
            double distance = hookLoc.distance(playerLoc);
            double t = distance;

            double v_x = (1.0D + 0.04D * t) * ((hookLoc.getX() - playerLoc.getX()) / t);
            double v_y = (0.9D + 0.03D * t) * ((hookLoc.getY() - playerLoc.getY()) / t);
            double v_z = (1.0D + 0.04D * t) * ((hookLoc.getZ() - playerLoc.getZ()) / t);

            Vector velocity = new Vector(v_x, v_y, v_z).multiply(1.2);

            // Limitar velocidade máxima
            if (velocity.length() > 4.0) {
                velocity = velocity.normalize().multiply(4.0);
            }

            player.setVelocity(velocity);
            player.setFallDistance(0);

            // Efeito sonoro
            player.playSound(player.getLocation(), Sound.ENDERDRAGON_WINGS, 0.5f, 1.5f);

            // Limpar ponto após usar
            hookLocations.remove(player.getUniqueId());
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        eject(event.getPlayer());
    }

    @Override
    public void eject(Player player) {
        hookLocations.remove(player.getUniqueId());
    }
}
