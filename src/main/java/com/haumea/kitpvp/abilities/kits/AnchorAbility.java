package com.haumea.kitpvp.abilities.kits;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.abilities.Ability;
import com.haumea.kitpvp.abilities.AbilityRarity;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * Kit Anchor (Âncora)
 * 
 * Raridade: EPIC | Preço: 1.500 | Ícone: ANVIL | Cooldown: Nenhum
 * 
 * Funcionalidade:
 * - Remove completamente o knockback (repulsão) de ataques
 * - Quando o jogador com Anchor ataca OU é atacado, o knockback é cancelado
 * 
 * @author HaumeaMC
 */
public class AnchorAbility extends Ability {

    public AnchorAbility(HaumeaMC plugin) {
        super(plugin, AbilityRarity.EPIC, Material.ANVIL, 0, 1500);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player))
            return;
        if (!(event.getDamager() instanceof Player))
            return;

        Player victim = (Player) event.getEntity();
        Player damager = (Player) event.getDamager();

        boolean victimHasAnchor = hasAbility(victim);
        boolean damagerHasAnchor = hasAbility(damager);

        if (victimHasAnchor || damagerHasAnchor) {
            // Cancelar o evento e reaplicar dano sem knockback
            double damage = event.getFinalDamage();
            event.setCancelled(true);

            // Registrar combate
            registerCombat(victim, damager);

            // Aplicar dano diretamente (sem knockback)
            victim.damage(damage);

            // Efeito sonoro
            victim.getWorld().playSound(victim.getLocation(), Sound.ANVIL_USE, 0.5f, 1.0f);
        }
    }
}
