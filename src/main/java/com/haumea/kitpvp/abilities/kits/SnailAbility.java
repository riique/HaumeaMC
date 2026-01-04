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
 * Kit Snail (Caracol)
 * 
 * Raridade: RARE | Preço: 700 | Ícone: SOUL_SAND | Cooldown: Nenhum
 * 
 * Funcionalidade:
 * - Ao atacar um jogador, aplica efeito de Slowness (Lentidão)
 * - Duração: 3 segundos
 * 
 * @author HaumeaMC
 */
public class SnailAbility extends Ability {

    public SnailAbility(HaumeaMC plugin) {
        super(plugin, AbilityRarity.RARE, Material.SOUL_SAND, 0, 700);
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

        // Aplicar Slowness por 3 segundos (60 ticks)
        victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 60, 0));
    }
}
