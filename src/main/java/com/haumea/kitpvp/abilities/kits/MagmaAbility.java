package com.haumea.kitpvp.abilities.kits;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.abilities.Ability;
import com.haumea.kitpvp.abilities.AbilityRarity;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import java.util.Random;

/**
 * Kit Magma
 * 
 * Raridade: RARE | Preço: 1.000 | Ícone: MAGMA_CREAM | Cooldown: Nenhum
 * 
 * Funcionalidade:
 * - Imunidade a fogo, lava e fire tick (igual ao Fireman)
 * - Adicional: 10% de chance de colocar fogo no atacante quando é atacado
 * 
 * @author HaumeaMC
 */
public class MagmaAbility extends Ability {

    private final Random random = new Random();

    public MagmaAbility(HaumeaMC plugin) {
        super(plugin, AbilityRarity.RARE, Material.MAGMA_CREAM, 0, 1000);
    }

    @EventHandler(ignoreCancelled = true)
    public void onFireDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player))
            return;

        Player player = (Player) event.getEntity();
        if (!hasAbility(player))
            return;

        DamageCause cause = event.getCause();

        if (cause == DamageCause.FIRE ||
                cause == DamageCause.LAVA ||
                cause == DamageCause.FIRE_TICK) {
            event.setCancelled(true);
            player.setFireTicks(0);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player))
            return;
        if (!(event.getDamager() instanceof Player))
            return;

        Player victim = (Player) event.getEntity();
        Player attacker = (Player) event.getDamager();

        if (!hasAbility(victim))
            return;
        if (isProtected(attacker))
            return;

        // 10% de chance de colocar fogo no atacante
        if (random.nextInt(10) == 0) {
            attacker.setFireTicks(40); // 2 segundos de fogo
        }
    }
}
