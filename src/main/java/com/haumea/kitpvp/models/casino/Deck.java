package com.haumea.kitpvp.models.casino;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Representa um baralho de cartas para Blackjack.
 * Suporta múltiplos decks combinados.
 * 
 * @author HaumeaMC
 */
public class Deck {

    private final List<Card> cards;
    private int currentIndex;

    /**
     * Cria um baralho com 1 deck (52 cartas).
     */
    public Deck() {
        this(1);
    }

    /**
     * Cria um baralho com múltiplos decks.
     * 
     * @param numDecks Número de decks (padrão: 6 para blackjack de cassino)
     */
    public Deck(int numDecks) {
        this.cards = new ArrayList<>();
        this.currentIndex = 0;

        for (int d = 0; d < numDecks; d++) {
            for (Card.Suit suit : Card.Suit.values()) {
                for (Card.Rank rank : Card.Rank.values()) {
                    cards.add(new Card(suit, rank));
                }
            }
        }

        shuffle();
    }

    /**
     * Embaralha o baralho.
     */
    public void shuffle() {
        Collections.shuffle(cards);
        currentIndex = 0;
    }

    /**
     * Retira uma carta do topo do baralho.
     */
    public Card draw() {
        if (currentIndex >= cards.size()) {
            shuffle(); // Re-embaralha se acabaram as cartas
        }
        return cards.get(currentIndex++);
    }

    /**
     * Retorna quantas cartas restam no baralho.
     */
    public int remaining() {
        return cards.size() - currentIndex;
    }

    /**
     * Retorna o total de cartas no baralho.
     */
    public int size() {
        return cards.size();
    }

    /**
     * Verifica se o baralho precisa ser re-embaralhado.
     * Geralmente quando resta menos de 25% das cartas.
     */
    public boolean needsReshuffle() {
        return remaining() < (size() / 4);
    }
}
