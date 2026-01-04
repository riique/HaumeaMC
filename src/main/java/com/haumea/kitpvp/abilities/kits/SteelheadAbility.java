package com.haumea.kitpvp.abilities.kits;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.abilities.Ability;
import com.haumea.kitpvp.abilities.AbilityRarity;
import org.bukkit.Material;

/**
 * Kit Steelhead (Cabeça de Aço)
 * 
 * Raridade: RARE | Preço: 2.200 | Ícone: GOLD_HELMET | Cooldown: Nenhum
 * 
 * Funcionalidade:
 * - Contra-kit específico: Anula completamente o dano do kit Stomper
 * - Quando um Stomper cai perto de você, você não recebe dano
 * 
 * NOTA: A lógica está implementada DENTRO do kit Stomper
 * para verificar se a vítima tem Steelhead equipado.
 * 
 * @author HaumeaMC
 */
public class SteelheadAbility extends Ability {

    public SteelheadAbility(HaumeaMC plugin) {
        super(plugin, AbilityRarity.RARE, Material.GOLD_HELMET, 0, 2200);
    }

    // Este kit não precisa de eventos próprios
    // A lógica de contra-kit está no StomperAbility
}
