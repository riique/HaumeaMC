package com.haumea.kitpvp.abilities;

import org.bukkit.entity.Player;

/**
 * Interface para abilities que precisam limpar dados temporários
 * quando um jogador sai, morre ou é descarregado.
 * 
 * Kits que armazenam dados temporários (como alvos de teleporte,
 * arenas do gladiator, etc.) devem implementar esta interface.
 * 
 * @author HaumeaMC
 */
public interface Ejectable {

    /**
     * Limpa todos os dados temporários associados ao jogador.
     * 
     * Este método é chamado automaticamente quando:
     * - O jogador sai do servidor
     * - O jogador morre
     * - O jogador troca de kit
     * 
     * @param player Jogador cujos dados devem ser limpos
     */
    void eject(Player player);
}
