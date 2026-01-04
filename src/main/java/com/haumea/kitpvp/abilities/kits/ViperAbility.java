package com.haumea.kitpvp.abilities.kits;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.abilities.Ability;
import com.haumea.kitpvp.abilities.AbilityRarity;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Kit Viper (Víbora)
 * 
 * Raridade: RARE | Preço: 800 | Ícone: SPIDER_EYE | Cooldown: Nenhum
 * 
 * Funcionalidade:
 * - Ao atacar um jogador, tem ~54% de chance de aplicar Veneno
 * - Veneno dura 3.5 segundos (70 ticks) com amplificador 1
 * 
 * @author HaumeaMC
 */
public class ViperAbility extends Ability {

    public ViperAbility(HaumeaMC plugin) {
        super(plugin, AbilityRarity.RARE, Material.SPIDER_EYE, 0, 800);
    }

    @EventHandler(ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player))
            return;
        if (!(event.getEntity() instanceof Player))
            return;

        Player attacker = (Player) event.getDamager();
        Player victim = (Player) event.getEntity();

        if (!hasAbility(attacker))
            return;
        if (isProtected(victim))
            return;

        // ~54% de chance (random > 0.4 AND random > 0.1)
        if (Math.random() > 0.4D && Math.random() > 0.1D) {
            // Veneno por 3.5 segundos (70 ticks) com amplificador 1
            victim.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 70, 1));
        }
    }
}
