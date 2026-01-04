package com.haumea.kitpvp.abilities.kits;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.abilities.Ability;
import com.haumea.kitpvp.abilities.AbilityRarity;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * Kit Neo
 * 
 * Raridade: EPIC | Preço: 300 | Ícone: ARROW | Cooldown: Nenhum
 * 
 * Funcionalidade:
 * - Imunidade total a projéteis (flechas, bolas de neve, ovos, etc.)
 * - Quando atingido por projétil, o atirador recebe mensagem
 * - Especial: Jogadores com Neo NÃO podem ser puxados para arena do Gladiator
 * - Especial: Bolas de neve do kit Switcher não funcionam em jogadores com Neo
 * 
 * @author HaumeaMC
 */
public class NeoAbility extends Ability {

    public NeoAbility(HaumeaMC plugin) {
        super(plugin, AbilityRarity.EPIC, Material.ARROW, 0, 300);
    }

    @EventHandler(ignoreCancelled = true)
    public void onProjectileDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player))
            return;

        Player victim = (Player) event.getEntity();
        if (!hasAbility(victim))
            return;

        // Verificar se é projétil
        if (event.getDamager() instanceof Projectile) {
            event.setCancelled(true);

            Projectile projectile = (Projectile) event.getDamager();
            if (projectile.getShooter() instanceof Player) {
                Player shooter = (Player) projectile.getShooter();
                shooter.sendMessage("§c§lHAUMEAMC§f O jogador está utilizando o kit Neo!");
            }
        }
    }
}
