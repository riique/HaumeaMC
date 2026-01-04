package com.haumea.kitpvp.models.casino;

import java.util.UUID;

/**
 * Representa um jogo de Blackjack em andamento.
 * Gerencia as mãos do jogador e dealer, e o estado do jogo.
 * 
 * @author HaumeaMC
 */
public class BlackjackGame {

    private final UUID playerId;
    private final long bet;
    private final Deck deck;
    private final BlackjackHand playerHand;
    private final BlackjackHand dealerHand;
    private GameState state;
    private boolean doubled;

    public BlackjackGame(UUID playerId, long bet) {
        this.playerId = playerId;
        this.bet = bet;
        this.deck = new Deck(2); // 2 decks
        this.playerHand = new BlackjackHand();
        this.dealerHand = new BlackjackHand();
        this.state = GameState.BETTING;
        this.doubled = false;
    }

    /**
     * Inicia o jogo distribuindo as cartas iniciais.
     */
    public void start() {
        playerHand.clear();
        dealerHand.clear();

        // Distribuir 2 cartas para cada
        playerHand.addCard(deck.draw());
        dealerHand.addCard(deck.draw());
        playerHand.addCard(deck.draw());
        dealerHand.addCard(deck.draw());

        state = GameState.PLAYER_TURN;

        // Verificar Blackjack natural
        if (playerHand.isBlackjack()) {
            if (dealerHand.isBlackjack()) {
                state = GameState.PUSH; // Empate
            } else {
                state = GameState.PLAYER_BLACKJACK;
            }
        } else if (dealerHand.isBlackjack()) {
            state = GameState.DEALER_WINS;
        }
    }

    /**
     * Jogador pede mais uma carta.
     */
    public Card hit() {
        if (state != GameState.PLAYER_TURN)
            return null;

        Card card = deck.draw();
        playerHand.addCard(card);

        if (playerHand.isBust()) {
            state = GameState.PLAYER_BUST;
        } else if (playerHand.getValue() == 21) {
            dealerTurn();
        }

        return card;
    }

    /**
     * Jogador para de pedir cartas.
     */
    public void stand() {
        if (state != GameState.PLAYER_TURN)
            return;

        playerHand.stand();
        dealerTurn();
    }

    /**
     * Jogador dobra a aposta e recebe exatamente 1 carta.
     */
    public Card doubleDown() {
        if (state != GameState.PLAYER_TURN || !playerHand.canDouble())
            return null;

        doubled = true;
        Card card = deck.draw();
        playerHand.addCard(card);
        playerHand.stand();

        if (playerHand.isBust()) {
            state = GameState.PLAYER_BUST;
        } else {
            dealerTurn();
        }

        return card;
    }

    /**
     * Executa a vez do dealer.
     */
    private void dealerTurn() {
        state = GameState.DEALER_TURN;

        // Dealer compra até ter 17 ou mais
        while (dealerHand.getValue() < 17) {
            dealerHand.addCard(deck.draw());
        }

        determineWinner();
    }

    /**
     * Determina o vencedor após as jogadas.
     */
    private void determineWinner() {
        int playerValue = playerHand.getValue();
        int dealerValue = dealerHand.getValue();

        if (dealerHand.isBust()) {
            state = GameState.PLAYER_WINS;
        } else if (playerValue > dealerValue) {
            state = GameState.PLAYER_WINS;
        } else if (playerValue < dealerValue) {
            state = GameState.DEALER_WINS;
        } else {
            state = GameState.PUSH;
        }
    }

    /**
     * Verifica se o jogo terminou.
     */
    public boolean isFinished() {
        return state != GameState.BETTING && state != GameState.PLAYER_TURN && state != GameState.DEALER_TURN;
    }

    /**
     * Calcula o pagamento baseado no resultado.
     * 
     * @return Valor a receber (0 se perdeu, aposta se empate, 2x se ganhou, 2.5x se
     *         blackjack)
     */
    public long calculatePayout() {
        long actualBet = doubled ? bet * 2 : bet;

        switch (state) {
            case PLAYER_BLACKJACK:
                return (long) (actualBet * 2.5);
            case PLAYER_WINS:
                return actualBet * 2;
            case PUSH:
                return actualBet;
            default:
                return 0;
        }
    }

    /**
     * Obtém a aposta atual (considerando double).
     */
    public long getActualBet() {
        return doubled ? bet * 2 : bet;
    }

    // ==================== GETTERS ====================

    public UUID getPlayerId() {
        return playerId;
    }

    public long getBet() {
        return bet;
    }

    public BlackjackHand getPlayerHand() {
        return playerHand;
    }

    public BlackjackHand getDealerHand() {
        return dealerHand;
    }

    public GameState getState() {
        return state;
    }

    public boolean isDoubled() {
        return doubled;
    }

    // ==================== ESTADOS ====================

    public enum GameState {
        BETTING("Apostando"),
        PLAYER_TURN("Sua vez"),
        DEALER_TURN("Vez do Dealer"),
        PLAYER_BUST("Você estourou!"),
        PLAYER_BLACKJACK("BLACKJACK!"),
        PLAYER_WINS("Você venceu!"),
        DEALER_WINS("Dealer venceu"),
        PUSH("Empate");

        private final String display;

        GameState(String display) {
            this.display = display;
        }

        public String getDisplay() {
            return display;
        }
    }
}
