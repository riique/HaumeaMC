package com.haumea.kitpvp.models;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Representa um duelo 1v1 ativo.
 * 
 * Armazena informações sobre os participantes, arena, configurações
 * e estado atual do duelo.
 * 
 * @author HaumeaMC
 */
public class DuelMatch {

    /**
     * Estados possíveis de um duelo
     */
    public enum MatchState {
        /** Aguardando countdown */
        COUNTDOWN,
        /** Em andamento */
        FIGHTING,
        /** Finalizado */
        ENDED
    }

    // ==================== CAMPOS ====================

    private final UUID matchId;
    private final UUID player1Id;
    private final UUID player2Id;
    private final DuelArena arena;
    private final DuelSettings settings;

    private MatchState state;
    private long startTime;
    private long endTime;

    // Contadores de sopas (para exibição na scoreboard)
    private int player1Soups;
    private int player2Soups;

    // Vencedor (null até o fim do duelo)
    private UUID winnerId;

    // ==================== CONSTRUTOR ====================

    /**
     * Cria um novo duelo
     * 
     * @param player1  Primeiro jogador
     * @param player2  Segundo jogador
     * @param arena    Arena do duelo
     * @param settings Configurações do duelo
     */
    public DuelMatch(Player player1, Player player2, DuelArena arena, DuelSettings settings) {
        this.matchId = UUID.randomUUID();
        this.player1Id = player1.getUniqueId();
        this.player2Id = player2.getUniqueId();
        this.arena = arena;
        this.settings = settings.copy(); // Copia para evitar modificações externas
        this.state = MatchState.COUNTDOWN;
        this.startTime = System.currentTimeMillis();

        // Inicializar contadores de sopas
        int initialSoups = settings.getSoupMode().isUnlimited() ? 32 : settings.getSoupMode().getAmount();
        this.player1Soups = initialSoups;
        this.player2Soups = initialSoups;
    }

    // ==================== GETTERS ====================

    public UUID getMatchId() {
        return matchId;
    }

    public UUID getPlayer1Id() {
        return player1Id;
    }

    public UUID getPlayer2Id() {
        return player2Id;
    }

    public Player getPlayer1() {
        return Bukkit.getPlayer(player1Id);
    }

    public Player getPlayer2() {
        return Bukkit.getPlayer(player2Id);
    }

    public DuelArena getArena() {
        return arena;
    }

    public DuelSettings getSettings() {
        return settings;
    }

    public MatchState getState() {
        return state;
    }

    public void setState(MatchState state) {
        this.state = state;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public UUID getWinnerId() {
        return winnerId;
    }

    // ==================== MÉTODOS DE SOPAS ====================

    public int getPlayer1Soups() {
        return player1Soups;
    }

    public void setPlayer1Soups(int soups) {
        this.player1Soups = soups;
    }

    public int getPlayer2Soups() {
        return player2Soups;
    }

    public void setPlayer2Soups(int soups) {
        this.player2Soups = soups;
    }

    /**
     * Obtém a quantidade de sopas de um jogador específico
     */
    public int getSoups(UUID playerId) {
        if (player1Id.equals(playerId))
            return player1Soups;
        if (player2Id.equals(playerId))
            return player2Soups;
        return 0;
    }

    /**
     * Define a quantidade de sopas de um jogador específico
     */
    public void setSoups(UUID playerId, int soups) {
        if (player1Id.equals(playerId))
            player1Soups = soups;
        else if (player2Id.equals(playerId))
            player2Soups = soups;
    }

    // ==================== MÉTODOS DE UTILIDADE ====================

    /**
     * Verifica se um jogador está neste duelo
     */
    public boolean isInMatch(UUID playerId) {
        return player1Id.equals(playerId) || player2Id.equals(playerId);
    }

    /**
     * Verifica se um jogador está neste duelo
     */
    public boolean isInMatch(Player player) {
        return player != null && isInMatch(player.getUniqueId());
    }

    /**
     * Obtém o oponente de um jogador
     */
    public UUID getOpponent(UUID playerId) {
        if (player1Id.equals(playerId))
            return player2Id;
        if (player2Id.equals(playerId))
            return player1Id;
        return null;
    }

    /**
     * Obtém o oponente de um jogador como Player
     */
    public Player getOpponentPlayer(Player player) {
        UUID opponentId = getOpponent(player.getUniqueId());
        return opponentId != null ? Bukkit.getPlayer(opponentId) : null;
    }

    /**
     * Verifica se o duelo está em andamento
     */
    public boolean isFighting() {
        return state == MatchState.FIGHTING;
    }

    /**
     * Verifica se o duelo terminou
     */
    public boolean isEnded() {
        return state == MatchState.ENDED;
    }

    /**
     * Obtém a duração do duelo em segundos
     */
    public int getDurationSeconds() {
        long end = state == MatchState.ENDED ? endTime : System.currentTimeMillis();
        return (int) ((end - startTime) / 1000);
    }

    /**
     * Obtém a duração formatada (MM:SS)
     */
    public String getFormattedDuration() {
        int seconds = getDurationSeconds();
        int minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    // ==================== FINALIZAÇÃO ====================

    /**
     * Finaliza o duelo com um vencedor
     * 
     * @param winner Jogador vencedor
     */
    public void end(Player winner) {
        this.state = MatchState.ENDED;
        this.endTime = System.currentTimeMillis();
        this.winnerId = winner != null ? winner.getUniqueId() : null;

        // Liberar arena
        if (arena != null) {
            arena.setInUse(false);
        }
    }

    /**
     * Finaliza o duelo sem vencedor (empate/cancelado)
     */
    public void cancel() {
        this.state = MatchState.ENDED;
        this.endTime = System.currentTimeMillis();
        this.winnerId = null;

        // Liberar arena
        if (arena != null) {
            arena.setInUse(false);
        }
    }

    @Override
    public String toString() {
        return "DuelMatch{" +
                "id=" + matchId +
                ", player1=" + player1Id +
                ", player2=" + player2Id +
                ", arena=" + (arena != null ? arena.getName() : "null") +
                ", state=" + state +
                '}';
    }
}
