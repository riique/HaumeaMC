package com.haumea.kitpvp.models.casino;

import java.util.UUID;

/**
 * Representa uma sessão ativa de jogo de cassino de um jogador.
 * Armazena o estado atual do jogo em andamento.
 * 
 * @author HaumeaMC
 */
public class CasinoSession {

    private final UUID playerId;
    private final CasinoGame game;
    private long currentBet;
    private Object gameState; // BlackjackHand, CrashState, etc
    private final long startTime;
    private boolean active;

    public CasinoSession(UUID playerId, CasinoGame game, long bet) {
        this.playerId = playerId;
        this.game = game;
        this.currentBet = bet;
        this.startTime = System.currentTimeMillis();
        this.active = true;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public CasinoGame getGame() {
        return game;
    }

    public long getCurrentBet() {
        return currentBet;
    }

    public void setCurrentBet(long currentBet) {
        this.currentBet = currentBet;
    }

    public Object getGameState() {
        return gameState;
    }

    public void setGameState(Object gameState) {
        this.gameState = gameState;
    }

    @SuppressWarnings("unchecked")
    public <T> T getGameState(Class<T> type) {
        if (gameState != null && type.isInstance(gameState)) {
            return (T) gameState;
        }
        return null;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getDuration() {
        return System.currentTimeMillis() - startTime;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void end() {
        this.active = false;
    }
}
