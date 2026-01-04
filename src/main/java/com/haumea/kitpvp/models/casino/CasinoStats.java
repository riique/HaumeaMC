package com.haumea.kitpvp.models.casino;

/**
 * Estatísticas de cassino de um jogador.
 * 
 * @author HaumeaMC
 */
public class CasinoStats {

    // Estatísticas gerais
    private long totalWagered;
    private long totalWon;
    private long totalLost;
    private int gamesPlayed;
    private long biggestWin;

    // Estatísticas por jogo
    private int slotsPlayed;
    private int slotsWins;
    private int slotsJackpots;

    private int roulettePlayed;
    private int rouletteWins;

    private int blackjackPlayed;
    private int blackjackWins;
    private int blackjackNaturals;

    private int coinflipPlayed;
    private int coinflipWins;

    private int crashPlayed;
    private double crashBiggestMult;

    public CasinoStats() {
        // Valores padrão
    }

    // ==================== MÉTODOS GERAIS ====================

    public long getProfit() {
        return totalWon - totalLost;
    }

    public String getProfitFormatted() {
        long profit = getProfit();
        if (profit >= 0) {
            return "&a+" + formatNumber(profit);
        }
        return "&c-" + formatNumber(Math.abs(profit));
    }

    public double getWinRate() {
        if (gamesPlayed == 0)
            return 0;
        int totalWins = slotsWins + rouletteWins + blackjackWins + coinflipWins;
        return Math.round((double) totalWins / gamesPlayed * 10000) / 100.0;
    }

    private String formatNumber(long number) {
        if (number >= 1000000) {
            return String.format("%.1fM", number / 1000000.0);
        }
        if (number >= 1000) {
            return String.format("%.1fK", number / 1000.0);
        }
        return String.valueOf(number);
    }

    // ==================== GETTERS & SETTERS ====================

    public long getTotalWagered() {
        return totalWagered;
    }

    public void setTotalWagered(long totalWagered) {
        this.totalWagered = totalWagered;
    }

    public void addWagered(long amount) {
        this.totalWagered += amount;
    }

    public long getTotalWon() {
        return totalWon;
    }

    public void setTotalWon(long totalWon) {
        this.totalWon = totalWon;
    }

    public void addWon(long amount) {
        this.totalWon += amount;
    }

    public long getTotalLost() {
        return totalLost;
    }

    public void setTotalLost(long totalLost) {
        this.totalLost = totalLost;
    }

    public void addLost(long amount) {
        this.totalLost += amount;
    }

    public int getGamesPlayed() {
        return gamesPlayed;
    }

    public void setGamesPlayed(int gamesPlayed) {
        this.gamesPlayed = gamesPlayed;
    }

    public void incrementGamesPlayed() {
        this.gamesPlayed++;
    }

    public long getBiggestWin() {
        return biggestWin;
    }

    public void setBiggestWin(long biggestWin) {
        this.biggestWin = biggestWin;
    }

    public void checkBiggestWin(long amount) {
        if (amount > biggestWin) {
            this.biggestWin = amount;
        }
    }

    // Slots
    public int getSlotsPlayed() {
        return slotsPlayed;
    }

    public void setSlotsPlayed(int slotsPlayed) {
        this.slotsPlayed = slotsPlayed;
    }

    public void incrementSlotsPlayed() {
        this.slotsPlayed++;
    }

    public int getSlotsWins() {
        return slotsWins;
    }

    public void setSlotsWins(int slotsWins) {
        this.slotsWins = slotsWins;
    }

    public void incrementSlotsWins() {
        this.slotsWins++;
    }

    public int getSlotsJackpots() {
        return slotsJackpots;
    }

    public void setSlotsJackpots(int slotsJackpots) {
        this.slotsJackpots = slotsJackpots;
    }

    public void incrementSlotsJackpots() {
        this.slotsJackpots++;
    }

    // Roulette
    public int getRoulettePlayed() {
        return roulettePlayed;
    }

    public void setRoulettePlayed(int roulettePlayed) {
        this.roulettePlayed = roulettePlayed;
    }

    public void incrementRoulettePlayed() {
        this.roulettePlayed++;
    }

    public int getRouletteWins() {
        return rouletteWins;
    }

    public void setRouletteWins(int rouletteWins) {
        this.rouletteWins = rouletteWins;
    }

    public void incrementRouletteWins() {
        this.rouletteWins++;
    }

    // Blackjack
    public int getBlackjackPlayed() {
        return blackjackPlayed;
    }

    public void setBlackjackPlayed(int blackjackPlayed) {
        this.blackjackPlayed = blackjackPlayed;
    }

    public void incrementBlackjackPlayed() {
        this.blackjackPlayed++;
    }

    public int getBlackjackWins() {
        return blackjackWins;
    }

    public void setBlackjackWins(int blackjackWins) {
        this.blackjackWins = blackjackWins;
    }

    public void incrementBlackjackWins() {
        this.blackjackWins++;
    }

    public int getBlackjackNaturals() {
        return blackjackNaturals;
    }

    public void setBlackjackNaturals(int blackjackNaturals) {
        this.blackjackNaturals = blackjackNaturals;
    }

    public void incrementBlackjackNaturals() {
        this.blackjackNaturals++;
    }

    // Coinflip
    public int getCoinflipPlayed() {
        return coinflipPlayed;
    }

    public void setCoinflipPlayed(int coinflipPlayed) {
        this.coinflipPlayed = coinflipPlayed;
    }

    public void incrementCoinflipPlayed() {
        this.coinflipPlayed++;
    }

    public int getCoinflipWins() {
        return coinflipWins;
    }

    public void setCoinflipWins(int coinflipWins) {
        this.coinflipWins = coinflipWins;
    }

    public void incrementCoinflipWins() {
        this.coinflipWins++;
    }

    // Crash
    public int getCrashPlayed() {
        return crashPlayed;
    }

    public void setCrashPlayed(int crashPlayed) {
        this.crashPlayed = crashPlayed;
    }

    public void incrementCrashPlayed() {
        this.crashPlayed++;
    }

    public double getCrashBiggestMult() {
        return crashBiggestMult;
    }

    public void setCrashBiggestMult(double crashBiggestMult) {
        this.crashBiggestMult = crashBiggestMult;
    }

    public void checkCrashBiggestMult(double mult) {
        if (mult > crashBiggestMult) {
            this.crashBiggestMult = mult;
        }
    }
}
