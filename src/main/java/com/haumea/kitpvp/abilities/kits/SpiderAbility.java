package com.haumea.kitpvp.abilities.kits;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.abilities.Ability;
import com.haumea.kitpvp.abilities.AbilityRarity;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Kit Spider (Aranha)
 * 
 * Raridade: RARE | Preço: 1.200 | Ícone: WEB | Cooldown: 3s
 * 
 * Funcionalidade:
 * - Ao clicar com teia, lança uma bola de neve especial
 * - A teia prende jogadores temporariamente
 * - Teias desaparecem após 3 segundos
 * 
 * @author HaumeaMC
 */
public class SpiderAbility extends Ability {

    private static final String SPIDER_METADATA = "SpiderWeb";

    public SpiderAbility(HaumeaMC plugin) {
        super(plugin, AbilityRarity.RARE, Material.WEB, 3, 1200,
                new ItemStack[] { createSpiderItem() });
    }

    private static ItemStack createSpiderItem() {
        ItemStack item = new ItemStack(Material.WEB, 16);
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§a§lTeia de Aranha");
        java.util.List<String> lore = new java.util.ArrayList<>();
        lore.add("§7Clique direito para lançar");
        lore.add("§7uma teia que prende inimigos!");
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
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK)
            return;

        event.setCancelled(true);

        if (isInCooldown(player)) {
            sendCooldownMessage(player);
            return;
        }

        putInCooldown(player);

        // Lançar snowball com metadata
        Snowball snowball = player.launchProjectile(Snowball.class);
        snowball.setVelocity(player.getLocation().getDirection().multiply(2.0));
        snowball.setMetadata(SPIDER_METADATA, new FixedMetadataValue(plugin, player.getUniqueId().toString()));

        // Som
        player.playSound(player.getLocation(), Sound.SPIDER_WALK, 1.0f, 1.0f);
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Snowball))
            return;

        Snowball snowball = (Snowball) event.getEntity();
        if (!snowball.hasMetadata(SPIDER_METADATA))
            return;

        Location hitLoc = snowball.getLocation();
        Block block = hitLoc.getBlock();

        // Encontrar local válido para colocar teia
        if (block.getType() == Material.AIR) {
            placeTemporaryWeb(block);
        } else {
            // Tentar bloco acima
            Block above = block.getRelative(org.bukkit.block.BlockFace.UP);
            if (above.getType() == Material.AIR) {
                placeTemporaryWeb(above);
            }
        }
    }

    /**
     * Coloca uma teia temporária
     */
    private void placeTemporaryWeb(Block block) {
        block.setType(Material.WEB);

        // Remover após 3 segundos
        new BukkitRunnable() {
            @Override
            public void run() {
                if (block.getType() == Material.WEB) {
                    block.setType(Material.AIR);
                }
            }
        }.runTaskLater(plugin, 60L); // 3 segundos
    }
}
