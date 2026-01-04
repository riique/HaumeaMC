package com.haumea.kitpvp.abilities.kits;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.abilities.Ability;
import com.haumea.kitpvp.abilities.AbilityRarity;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.LightningStrike;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Kit Thor
 * 
 * Raridade: RARE | Preço: 1.200 | Ícone: WOOD_AXE | Cooldown: 7s
 * 
 * Funcionalidade:
 * - Ao clicar com botão direito em um bloco, invoca um raio no bloco alvo
 * - O raio causa 2.5 corações de dano e coloca fogo
 * - O usuário do kit é imune aos seus próprios raios
 * 
 * @author HaumeaMC
 */
public class ThorAbility extends Ability {

    public ThorAbility(HaumeaMC plugin) {
        super(plugin, AbilityRarity.RARE, Material.WOOD_AXE, 7, 1200,
                new ItemStack[] { createMjolnirItem() });
    }

    private static ItemStack createMjolnirItem() {
        ItemStack item = new ItemStack(Material.WOOD_AXE);
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§e§lMjölnir");
        java.util.List<String> lore = new java.util.ArrayList<>();
        lore.add("§7O martelo do Deus do Trovão!");
        lore.add("");
        lore.add("§7Clique direito em um bloco");
        lore.add("§7para invocar um raio!");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (player.getItemInHand() == null)
            return;
        if (player.getItemInHand().getType() != Material.WOOD_AXE)
            return;
        if (!hasAbility(player))
            return;

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK &&
                event.getAction() != Action.RIGHT_CLICK_AIR)
            return;

        event.setCancelled(true);

        if (isInCooldown(player)) {
            sendCooldownMessage(player);
            return;
        }

        // Obter bloco alvo
        @SuppressWarnings("deprecation")
        Block targetBlock = player.getTargetBlock((java.util.HashSet<Byte>) null, 50);

        if (targetBlock == null || targetBlock.getType() == Material.AIR) {
            return;
        }

        putInCooldown(player);

        // Obter bloco mais alto na coluna
        Location loc = targetBlock.getLocation();
        loc = loc.getWorld().getHighestBlockAt(loc).getLocation().add(0, 1, 0);

        // Invocar raio real (causa dano)
        player.getWorld().strikeLightning(loc);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onLightningDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player))
            return;
        if (!(event.getDamager() instanceof LightningStrike))
            return;

        Player victim = (Player) event.getEntity();

        // Se a vítima tem Thor, é imune a raios
        if (hasAbility(victim)) {
            event.setDamage(0.0);
            event.setCancelled(true);
            return;
        }

        // Dano padrão para jogadores sem Thor: 5 (2.5 corações)
        event.setDamage(5.0);
        victim.setFireTicks(40); // 2 segundos de fogo
    }
}
