package com.haumea.kitpvp.events;

import com.haumea.kitpvp.models.PlayerRank;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Evento disparado quando um jogador é promovido de rank.
 * 
 * Permite que outros sistemas reajam a promoções de liga.
 * 
 * @author HaumeaMC
 */
public class PlayerRankUpEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final PlayerRank oldRank;
    private final PlayerRank newRank;
    private final int rewardCoins;

    /**
     * Construtor do evento de promoção.
     * 
     * @param player      Jogador promovido
     * @param oldRank     Rank anterior
     * @param newRank     Novo rank
     * @param rewardCoins Coins de recompensa
     */
    public PlayerRankUpEvent(Player player, PlayerRank oldRank, PlayerRank newRank, int rewardCoins) {
        this.player = player;
        this.oldRank = oldRank;
        this.newRank = newRank;
        this.rewardCoins = rewardCoins;
    }

    /**
     * Obtém o jogador promovido
     * 
     * @return Player
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * Obtém o rank anterior
     * 
     * @return Rank antigo
     */
    public PlayerRank getOldRank() {
        return oldRank;
    }

    /**
     * Obtém o novo rank
     * 
     * @return Novo rank
     */
    public PlayerRank getNewRank() {
        return newRank;
    }

    /**
     * Obtém os coins de recompensa
     * 
     * @return Coins
     */
    public int getRewardCoins() {
        return rewardCoins;
    }

    /**
     * Verifica se foi um aumento de liga (não apenas divisão)
     * 
     * @return true se mudou de liga
     */
    public boolean isLeaguePromotion() {
        if (oldRank == null || newRank == null)
            return true;
        return !oldRank.getLeague().getId().equals(newRank.getLeague().getId());
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
