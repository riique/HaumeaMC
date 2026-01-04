package com.haumea.kitpvp.abilities.kits;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.abilities.Ability;
import com.haumea.kitpvp.abilities.AbilityRarity;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

/**
 * Kit Fireman (Bombeiro)
 * 
 * Raridade: RARE | Preço: 800 | Ícone: WATER_BUCKET | Cooldown: Nenhum
 * 
 * Funcionalidade:
 * - Imunidade total a dano de fogo, lava e fire tick
 * 
 * @author HaumeaMC
 */
public class FiremanAbility extends Ability {

    public FiremanAbility(HaumeaMC plugin) {
        super(plugin, AbilityRarity.RARE, Material.WATER_BUCKET, 0, 800);
    }

    @EventHandler(ignoreCancelled = true)
    public void onFireman(EntityDamageEvent event) {
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
            player.setFireTicks(0); // Remover fire ticks
        }
    }
}
