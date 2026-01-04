package com.haumea.kitpvp.models.casino;

/**
 * Representa uma carta de baralho para o Blackjack.
 * 
 * @author HaumeaMC
 */
public class Card {

    private final Suit suit;
    private final Rank rank;

    public Card(Suit suit, Rank rank) {
        this.suit = suit;
        this.rank = rank;
    }

    public Suit getSuit() {
        return suit;
    }

    public Rank getRank() {
        return rank;
    }

    /**
     * Obtém o valor da carta para Blackjack.
     * Ás conta como 11 (pode ser ajustado para 1 na mão).
     */
    public int getValue() {
        return rank.getValue();
    }

    /**
     * Retorna o display colorido da carta.
     * Exemplo: "&c&lK♥" ou "&f&lA♠"
     */
    public String getDisplay() {
        String color = suit.isRed() ? "&c" : "&f";
        return color + "&l" + rank.getSymbol() + suit.getSymbol();
    }

    /**
     * Retorna o nome completo da carta.
     */
    public String getFullName() {
        return rank.getName() + " de " + suit.getName();
    }

    @Override
    public String toString() {
        return rank.getSymbol() + suit.getSymbol();
    }

    // ==================== NAIPES ====================

    public enum Suit {
        HEARTS("Copas", "♥", true),
        DIAMONDS("Ouros", "♦", true),
        CLUBS("Paus", "♣", false),
        SPADES("Espadas", "♠", false);

        private final String name;
        private final String symbol;
        private final boolean red;

        Suit(String name, String symbol, boolean red) {
            this.name = name;
            this.symbol = symbol;
            this.red = red;
        }

        public String getName() {
            return name;
        }

        public String getSymbol() {
            return symbol;
        }

        public boolean isRed() {
            return red;
        }
    }

    // ==================== RANKS ====================

    public enum Rank {
        ACE("Ás", "A", 11),
        TWO("Dois", "2", 2),
        THREE("Três", "3", 3),
        FOUR("Quatro", "4", 4),
        FIVE("Cinco", "5", 5),
        SIX("Seis", "6", 6),
        SEVEN("Sete", "7", 7),
        EIGHT("Oito", "8", 8),
        NINE("Nove", "9", 9),
        TEN("Dez", "10", 10),
        JACK("Valete", "J", 10),
        QUEEN("Dama", "Q", 10),
        KING("Rei", "K", 10);

        private final String name;
        private final String symbol;
        private final int value;

        Rank(String name, String symbol, int value) {
            this.name = name;
            this.symbol = symbol;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public String getSymbol() {
            return symbol;
        }

        public int getValue() {
            return value;
        }

        public boolean isAce() {
            return this == ACE;
        }
    }
}
