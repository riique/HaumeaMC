package com.haumea.kitpvp.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Evento disparado quando um jogador mata outro.
 * 
 * Permite que outros sistemas reajam a kills sem depender diretamente
 * do CombatListener.
 * 
 * @author HaumeaMC
 */
public class PlayerKillEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player killer;
    private final Player victim;
    private final int newKillstreak;
    private final long coinsAwarded;
    private final int eloGained;

    /**
     * Construtor do evento de kill.
     * 
     * @param killer        Jogador que matou
     * @param victim        Jogador que morreu
     * @param newKillstreak Novo killstreak do killer
     * @param coinsAwarded  Coins recebidos pelo killer
     * @param eloGained     ELO ganho pelo killer
     */
    public PlayerKillEvent(Player killer, Player victim, int newKillstreak, long coinsAwarded, int eloGained) {
        this.killer = killer;
        this.victim = victim;
        this.newKillstreak = newKillstreak;
        this.coinsAwarded = coinsAwarded;
        this.eloGained = eloGained;
    }

    /**
     * Obtém o jogador que fez a kill
     * 
     * @return Killer
     */
    public Player getKiller() {
        return killer;
    }

    /**
     * Obtém o jogador que morreu
     * 
     * @return Victim
     */
    public Player getVictim() {
        return victim;
    }

    /**
     * Obtém o novo killstreak do killer
     * 
     * @return Killstreak atual
     */
    public int getNewKillstreak() {
        return newKillstreak;
    }

    /**
     * Obtém a quantidade de coins ganhos
     * 
     * @return Coins
     */
    public long getCoinsAwarded() {
        return coinsAwarded;
    }

    /**
     * Obtém a quantidade de ELO ganho
     * 
     * @return ELO
     */
    public int getEloGained() {
        return eloGained;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
