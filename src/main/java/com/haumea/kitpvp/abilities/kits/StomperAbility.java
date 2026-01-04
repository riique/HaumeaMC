package com.haumea.kitpvp.abilities.kits;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.abilities.Ability;
import com.haumea.kitpvp.abilities.AbilityRarity;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

/**
 * Kit Stomper (Pisoteador)
 * 
 * Raridade: MYSTIC | Preço: 3.000 | Ícone: IRON_BOOTS | Cooldown: Nenhum
 * 
 * Funcionalidade:
 * - Ao cair de altura, transfere o dano de queda para jogadores próximos
 * - Raio de efeito: 5 blocos
 * - Seu próprio dano de queda é limitado a 2 corações
 * - Jogadores agachados ou com kit Steelhead não recebem dano
 * 
 * @author HaumeaMC
 */
public class StomperAbility extends Ability {

    public StomperAbility(HaumeaMC plugin) {
        super(plugin, AbilityRarity.MYSTIC, Material.IRON_BOOTS, 0, 3000);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onFallDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player))
            return;
        if (event.getCause() != DamageCause.FALL)
            return;

        Player stomper = (Player) event.getEntity();
        if (!hasAbility(stomper))
            return;

        double fallDamage = event.getDamage();

        // Transferir dano para jogadores próximos
        for (Player nearby : Bukkit.getOnlinePlayers()) {
            if (nearby.equals(stomper))
                continue;

            // Verificar distância
            if (!nearby.getWorld().equals(stomper.getWorld()))
                continue;
            double distance = stomper.getLocation().distance(nearby.getLocation());
            if (distance >= 5)
                continue;

            // Verificar proteções
            if (isProtected(nearby) || isSpectator(nearby))
                continue;

            // Calcular dano
            double damage = fallDamage;

            // Jogador agachado é imune
            if (nearby.isSneaking()) {
                nearby.sendMessage("§6§lHAUMEAMC§f Você se abaixou e evitou o impacto!");
                continue;
            }

            // Jogador com Steelhead é imune
            if (Ability.hasAbility(nearby, "Steelhead", plugin)) {
                nearby.sendMessage("§6§lHAUMEAMC§f Seu capacete de aço protegeu você!");
                stomper.sendMessage("§c§lHAUMEAMC §e" + nearby.getName() + "§f tem Steelhead!");
                continue;
            }

            // Aplicar dano
            registerCombat(nearby, stomper);
            nearby.damage(damage, stomper);

            // Mensagem para a vítima
            nearby.sendMessage("§c§lHAUMEAMC§f Você foi atingido pelo Stomper de §e" + stomper.getName() + "§f!");
        }

        // Limitar dano próprio a 4 (2 corações)
        if (event.getDamage() > 4.0) {
            event.setDamage(4.0);
        }

        // Efeito sonoro
        stomper.playSound(stomper.getLocation(), Sound.ANVIL_USE, 1.0f, 1.0f);
    }
}
