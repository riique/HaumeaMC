package com.haumea.kitpvp.abilities.kits;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.abilities.Ability;
import com.haumea.kitpvp.abilities.AbilityRarity;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;

/**
 * Kit Flash
 * 
 * Raridade: RARE | Preço: 2.000 | Ícone: REDSTONE_TORCH_ON | Cooldown: 15s
 * 
 * Funcionalidade:
 * - Ao clicar com botão direito, teleporta instantaneamente para o bloco que
 * está olhando
 * - Alcance máximo: 200 blocos
 * - Cria um efeito visual de raio no local de partida
 * 
 * @author HaumeaMC
 */
public class FlashAbility extends Ability {

    public FlashAbility(HaumeaMC plugin) {
        super(plugin, AbilityRarity.RARE, Material.REDSTONE_TORCH_ON, 15, 2000,
                new ItemStack[] { createFlashItem() });
    }

    private static ItemStack createFlashItem() {
        ItemStack item = new ItemStack(Material.REDSTONE_TORCH_ON);
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§a§lFlash");
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

        // Obter bloco alvo (até 200 blocos de distância)
        @SuppressWarnings("deprecation")
        Block target = player.getTargetBlock((HashSet<Byte>) null, 200);

        if (target == null || target.getType() == Material.AIR) {
            return;
        }

        // Encontrar local seguro acima do bloco
        Block safeBlock = target.getRelative(BlockFace.UP);
        while (safeBlock.getType() != Material.AIR && safeBlock.getY() < 256) {
            safeBlock = safeBlock.getRelative(BlockFace.UP);
        }

        putInCooldown(player);

        // Efeito visual de raio no local de partida (apenas efeito visual)
        player.getWorld().strikeLightningEffect(player.getLocation());

        // Teleportar
        Location teleportLoc = safeBlock.getLocation().add(0.5, 0, 0.5);
        teleportLoc.setYaw(player.getLocation().getYaw());
        teleportLoc.setPitch(player.getLocation().getPitch());

        player.teleport(teleportLoc);
        player.setFallDistance(0);
    }
}
