package com.haumea.kitpvp.abilities.kits;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.abilities.Ability;
import com.haumea.kitpvp.abilities.AbilityRarity;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerFishEvent.State;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

/**
 * Kit Fisherman (Pescador)
 * 
 * Raridade: RARE | Preço: 1.000 | Ícone: FISHING_ROD | Cooldown: 3s
 * 
 * Funcionalidade:
 * - Usa a vara de pesca para "pescar" jogadores
 * - Ao acertar um jogador com a vara, ele é puxado em direção ao usuário
 * 
 * @author HaumeaMC
 */
public class FishermanAbility extends Ability {

    public FishermanAbility(HaumeaMC plugin) {
        super(plugin, AbilityRarity.RARE, Material.FISHING_ROD, 3, 1000,
                new ItemStack[] { new ItemStack(Material.FISHING_ROD) });
    }

    @EventHandler(ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        Player fisher = event.getPlayer();

        if (!hasAbility(fisher))
            return;

        if (event.getState() != State.CAUGHT_ENTITY)
            return;

        if (!(event.getCaught() instanceof Player))
            return;

        Player victim = (Player) event.getCaught();

        // Não afetar jogadores protegidos
        if (isProtected(victim))
            return;

        // Verificar cooldown
        if (isInCooldown(fisher)) {
            sendCooldownMessage(fisher);
            return;
        }

        putInCooldown(fisher);

        // Calcular direção para puxar
        Vector direction = fisher.getLocation().toVector()
                .subtract(victim.getLocation().toVector())
                .normalize()
                .multiply(1.5);

        // Adicionar componente vertical
        direction.setY(0.5);

        // Aplicar velocidade
        victim.setVelocity(direction);

        // Registrar combate
        registerCombat(victim, fisher);
    }
}
