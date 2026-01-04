package com.haumea.kitpvp.abilities.kits;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.abilities.Ability;
import com.haumea.kitpvp.abilities.AbilityRarity;
import org.bukkit.Material;

/**
 * Kit Quickdrop
 * 
 * Raridade: EPIC | Preço: 200 | Ícone: BOWL | Cooldown: Nenhum
 * 
 * Funcionalidade:
 * - Kit utilitário para PvP com sopas
 * - Permite dropar potes vazios (bowls) instantaneamente
 * - A lógica é implementada no SoupListener do plugin
 * 
 * Este kit não tem lógica de eventos própria, seu efeito é verificado
 * no SoupListener para auto-drop de bowls.
 * 
 * @author HaumeaMC
 */
public class QuickdropAbility extends Ability {

    public QuickdropAbility(HaumeaMC plugin) {
        super(plugin, AbilityRarity.EPIC, Material.BOWL, 0, 200);
    }

    // Este kit não precisa de eventos próprios
    // A lógica está integrada no SoupListener
}
