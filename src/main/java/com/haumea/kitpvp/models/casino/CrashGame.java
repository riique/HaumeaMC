package com.haumea.kitpvp.models.casino;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Representa uma rodada de Crash.
 * O jogo funciona assim:
 * 1. Jogadores entram com suas apostas
 * 2. O multiplicador começa a subir
 * 3. Jogadores podem sair (cash out) a qualquer momento
 * 4. Quando o jogo "crashar", quem não saiu perde tudo
 * 
 * @author HaumeaMC
 */
public class CrashGame {

    private final Map<UUID, Long> participants; // jogador -> aposta
    private final Map<UUID, Double> cashouts; // quem já saiu -> multiplicador
    private double currentMultiplier;
    private final double crashPoint; // multiplicador em que vai crashar
    private GameState state;
    private long startTime;
    private long lastTickTime;

    private static final Random RANDOM = new Random();

    public CrashGame() {
        this.participants = new HashMap<>();
        this.cashouts = new HashMap<>();
        this.currentMultiplier = 1.00;
        this.crashPoint = generateCrashPoint();
        this.state = GameState.WAITING;
        this.startTime = 0;
        this.lastTickTime = 0;
    }

    /**
     * Gera o ponto de crash usando uma distribuição exponencial.
     * Favorece crashes em multiplicadores baixos, mas permite altos ocasionalmente.
     */
    private double generateCrashPoint() {
        // Fórmula: crash point = 0.99 / (1 - random)
        // Isso cria uma curva onde:
        // - 50% das vezes crashar abaixo de 2x
        // - 10% das vezes chegar acima de 10x
        // - 1% das vezes chegar acima de 100x
        double random = RANDOM.nextDouble();

        // Garantir que não seja 1.0 (divisão por zero)
        if (random >= 0.99) {
            random = 0.99;
        }

        // Aplicar house edge de 3%
        double crash = (0.97) / (1.0 - random);

        // Mínimo de 1.01x, máximo de 100x
        return Math.max(1.01, Math.min(100.0, crash));
    }

    /**
     * Adiciona um jogador ao jogo.
     */
    public boolean join(UUID playerId, long bet) {
        if (state != GameState.WAITING || participants.containsKey(playerId)) {
            return false;
        }
        participants.put(playerId, bet);
        return true;
    }

    /**
     * Remove um jogador do jogo antes de começar.
     */
    public boolean leave(UUID playerId) {
        if (state != GameState.WAITING) {
            return false;
        }
        return participants.remove(playerId) != null;
    }

    /**
     * Inicia o jogo.
     */
    public void start() {
        if (state != GameState.WAITING)
            return;

        state = GameState.RUNNING;
        startTime = System.currentTimeMillis();
        lastTickTime = startTime;
        currentMultiplier = 1.00;
    }

    /**
     * Atualiza o multiplicador (chamado periodicamente).
     * 
     * @return true se o jogo ainda está rodando, false se crashou
     */
    public boolean tick() {
        if (state != GameState.RUNNING)
            return false;

        long now = System.currentTimeMillis();
        long elapsed = now - lastTickTime;
        lastTickTime = now;

        // Aumentar multiplicador (cerca de 6% por segundo)
        double increase = 1.0 + (elapsed / 1000.0 * 0.06);
        currentMultiplier *= increase;

        // Verificar se crashou
        if (currentMultiplier >= crashPoint) {
            crash();
            return false;
        }

        return true;
    }

    /**
     * Jogador faz cash out.
     */
    public boolean cashout(UUID playerId) {
        if (state != GameState.RUNNING)
            return false;
        if (!participants.containsKey(playerId))
            return false;
        if (cashouts.containsKey(playerId))
            return false;

        cashouts.put(playerId, currentMultiplier);
        return true;
    }

    /**
     * Calcula o pagamento de um jogador que fez cash out.
     */
    public long calculatePayout(UUID playerId) {
        Long bet = participants.get(playerId);
        Double mult = cashouts.get(playerId);

        if (bet == null || mult == null)
            return 0;

        return (long) (bet * mult);
    }

    /**
     * Finaliza o jogo com crash.
     */
    private void crash() {
        state = GameState.CRASHED;
        currentMultiplier = crashPoint;
    }

    /**
     * Verifica se um jogador fez cash out.
     */
    public boolean hasCashedOut(UUID playerId) {
        return cashouts.containsKey(playerId);
    }

    /**
     * Obtém o multiplicador de cash out de um jogador.
     */
    public double getCashoutMultiplier(UUID playerId) {
        return cashouts.getOrDefault(playerId, 0.0);
    }

    /**
     * Verifica se um jogador está participando.
     */
    public boolean isParticipant(UUID playerId) {
        return participants.containsKey(playerId);
    }

    /**
     * Obtém a aposta de um jogador.
     */
    public long getBet(UUID playerId) {
        return participants.getOrDefault(playerId, 0L);
    }

    /**
     * Obtém o número de participantes.
     */
    public int getParticipantCount() {
        return participants.size();
    }

    // ==================== GETTERS ====================

    public Map<UUID, Long> getParticipants() {
        return participants;
    }

    public Map<UUID, Double> getCashouts() {
        return cashouts;
    }

    public double getCurrentMultiplier() {
        return currentMultiplier;
    }

    public double getCrashPoint() {
        return crashPoint;
    }

    public GameState getState() {
        return state;
    }

    public long getStartTime() {
        return startTime;
    }

    /**
     * Obtém o tempo desde o início do jogo.
     */
    public long getElapsedTime() {
        if (startTime == 0)
            return 0;
        return System.currentTimeMillis() - startTime;
    }

    /**
     * Formata o multiplicador atual.
     */
    public String getFormattedMultiplier() {
        return String.format("%.2fx", currentMultiplier);
    }

    // ==================== ESTADOS ====================

    public enum GameState {
        WAITING("Aguardando"),
        RUNNING("Em andamento"),
        CRASHED("Crashou");

        private final String display;

        GameState(String display) {
            this.display = display;
        }

        public String getDisplay() {
            return display;
        }
    }
}
