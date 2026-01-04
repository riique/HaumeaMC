package com.haumea.kitpvp.abilities.kits;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.abilities.Ability;
import com.haumea.kitpvp.abilities.AbilityRarity;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Kit Kangaroo (Canguru)
 * 
 * Raridade: MYSTIC | Preço: 2.000 | Ícone: FIREWORK | Cooldown: 5s (após entrar
 * em combate)
 * 
 * Funcionalidade:
 * - Permite pulos duplos (um no chão, um no ar)
 * - Em pé: Pulo alto vertical
 * - Agachado: Pulo longo horizontal
 * - Dano de queda limitado a 6 corações máximo
 * 
 * @author HaumeaMC
 */
public class KangarooAbility extends Ability {

    // Rastreamento de pulos no ar
    private final Map<UUID, Boolean> usedAirJump = new HashMap<>();

    public KangarooAbility(HaumeaMC plugin) {
        super(plugin, AbilityRarity.MYSTIC, Material.FIREWORK, 5, 2000,
                new ItemStack[] { createKangarooItem() });
    }

    private static ItemStack createKangarooItem() {
        ItemStack item = new ItemStack(Material.FIREWORK);
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§a§lBoost");
        java.util.List<String> lore = new java.util.ArrayList<>();
        lore.add("§7Clique direito para pular!");
        lore.add("§7Shift + clique = pulo longo");
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

        boolean onGround = player.isOnGround();
        UUID uuid = player.getUniqueId();

        // Verificar se pode pular
        if (!onGround) {
            // Verificar se já usou o pulo no ar
            if (usedAirJump.getOrDefault(uuid, false)) {
                player.sendMessage("§c§lHAUMEAMC§f Você já usou seu pulo no ar!");
                return;
            }

            // Verificar cooldown apenas para pulo no ar
            if (isInCooldown(player)) {
                sendCooldownMessage(player);
                return;
            }

            usedAirJump.put(uuid, true);
            putInCooldown(player);
        } else {
            // Resetar pulo no ar quando no chão
            usedAirJump.put(uuid, false);
        }

        // Calcular velocidade
        Vector velocity = player.getEyeLocation().getDirection();

        if (player.isSneaking()) {
            // Pulo longo horizontal (agachado)
            velocity.multiply(0.3F * 2.0);
            velocity.setY(0.55F);
        } else {
            // Pulo alto vertical (em pé)
            velocity.multiply(0.3F * 1.5);
            velocity.setY(0.9F * 1.5);
        }

        player.setVelocity(velocity);

        // Efeito sonoro
        player.playSound(player.getLocation(), Sound.HORSE_JUMP, 1.0f, 1.0f);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onFallDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player))
            return;
        if (event.getCause() != DamageCause.FALL)
            return;

        Player player = (Player) event.getEntity();
        if (!hasAbility(player))
            return;

        // Resetar pulo no ar ao cair
        usedAirJump.put(player.getUniqueId(), false);

        // Limitar dano de queda a 12 (6 corações)
        if (event.getDamage() > 12.0) {
            event.setDamage(12.0);
        }
    }
}
