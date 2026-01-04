package com.haumea.kitpvp.models.casino;

import java.util.ArrayList;
import java.util.List;

/**
 * Representa uma mão de Blackjack (cartas de um jogador ou dealer).
 * Calcula automaticamente o valor considerando Ases como 1 ou 11.
 * 
 * @author HaumeaMC
 */
public class BlackjackHand {

    private final List<Card> cards;
    private boolean standing;

    public BlackjackHand() {
        this.cards = new ArrayList<>();
        this.standing = false;
    }

    /**
     * Adiciona uma carta à mão.
     */
    public void addCard(Card card) {
        cards.add(card);
    }

    /**
     * Obtém todas as cartas da mão.
     */
    public List<Card> getCards() {
        return cards;
    }

    /**
     * Obtém o número de cartas na mão.
     */
    public int getCardCount() {
        return cards.size();
    }

    /**
     * Calcula o valor da mão.
     * Ases contam como 11, mas são reduzidos a 1 se o total passar de 21.
     */
    public int getValue() {
        int value = 0;
        int aces = 0;

        for (Card card : cards) {
            value += card.getValue();
            if (card.getRank().isAce()) {
                aces++;
            }
        }

        // Reduzir Ases de 11 para 1 enquanto estivermos acima de 21
        while (value > 21 && aces > 0) {
            value -= 10;
            aces--;
        }

        return value;
    }

    /**
     * Verifica se a mão estourou (valor > 21).
     */
    public boolean isBust() {
        return getValue() > 21;
    }

    /**
     * Verifica se é um Blackjack natural (21 com 2 cartas).
     */
    public boolean isBlackjack() {
        return cards.size() == 2 && getValue() == 21;
    }

    /**
     * Verifica se a mão pode receber mais cartas.
     */
    public boolean canHit() {
        return !standing && !isBust() && getValue() < 21;
    }

    /**
     * Verifica se a mão pode fazer double (apenas com 2 cartas).
     */
    public boolean canDouble() {
        return cards.size() == 2 && !standing && !isBust();
    }

    /**
     * Marca que o jogador parou (stand).
     */
    public void stand() {
        this.standing = true;
    }

    /**
     * Verifica se o jogador parou.
     */
    public boolean isStanding() {
        return standing;
    }

    /**
     * Verifica se a mão está finalizada (parou ou estourou).
     */
    public boolean isFinished() {
        return standing || isBust() || getValue() == 21;
    }

    /**
     * Obtém o display das cartas para o menu.
     * 
     * @param hideFirst Se true, esconde a primeira carta (para dealer)
     */
    public String getDisplay(boolean hideFirst) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cards.size(); i++) {
            if (i > 0)
                sb.append(" ");
            if (hideFirst && i == 0) {
                sb.append("&7&l[?]");
            } else {
                sb.append("[").append(cards.get(i).getDisplay()).append("&f]");
            }
        }
        return sb.toString();
    }

    /**
     * Limpa a mão.
     */
    public void clear() {
        cards.clear();
        standing = false;
    }

    @Override
    public String toString() {
        return getDisplay(false) + " = " + getValue();
    }
}
