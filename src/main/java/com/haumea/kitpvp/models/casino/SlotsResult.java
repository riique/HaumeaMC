package com.haumea.kitpvp.models.casino;

import org.bukkit.Material;

/**
 * Representa o resultado de uma jogada no Slots.
 * 
 * @author HaumeaMC
 */
public class SlotsResult {

    private final SlotSymbol[] symbols;
    private final ResultType resultType;
    private final double multiplier;
    private final long payout;

    public SlotsResult(SlotSymbol[] symbols, ResultType resultType, double multiplier, long payout) {
        this.symbols = symbols;
        this.resultType = resultType;
        this.multiplier = multiplier;
        this.payout = payout;
    }

    public SlotSymbol[] getSymbols() {
        return symbols;
    }

    public ResultType getResultType() {
        return resultType;
    }

    public double getMultiplier() {
        return multiplier;
    }

    public long getPayout() {
        return payout;
    }

    public boolean isWin() {
        return resultType != ResultType.LOSE;
    }

    public boolean isJackpot() {
        return resultType == ResultType.JACKPOT;
    }

    /**
     * Formata o resultado para exibição.
     */
    public String getDisplay() {
        StringBuilder sb = new StringBuilder();
        for (SlotSymbol symbol : symbols) {
            sb.append("[").append(symbol.getEmoji()).append("] ");
        }
        return sb.toString().trim();
    }

    // ==================== TIPOS DE RESULTADO ====================

    public enum ResultType {
        LOSE("Derrota", 0),
        TWO_MATCH("2 iguais", 1.5),
        THREE_COMMON("3 iguais", 3.0),
        THREE_RARE("3 raros", 10.0),
        JACKPOT("JACKPOT", 25.0);

        private final String display;
        private final double defaultMultiplier;

        ResultType(String display, double defaultMultiplier) {
            this.display = display;
            this.defaultMultiplier = defaultMultiplier;
        }

        public String getDisplay() {
            return display;
        }

        public double getDefaultMultiplier() {
            return defaultMultiplier;
        }
    }

    // ==================== SÍMBOLOS DO SLOT ====================

    public enum SlotSymbol {
        CHERRY(Material.APPLE, "&c🍒", "Cereja", false),
        LEMON(Material.GOLDEN_APPLE, "&e🍋", "Limão", false),
        MELON(Material.MELON, "&a🍉", "Melancia", false),
        GRAPE(Material.INK_SACK, "&5🍇", "Uva", false), // Purple dye
        DIAMOND(Material.DIAMOND, "&b💎", "Diamante", true),
        STAR(Material.NETHER_STAR, "&6⭐", "Estrela", true),
        SEVEN(Material.REDSTONE_BLOCK, "&c&l7", "Sete", true);

        private final Material material;
        private final String emoji;
        private final String name;
        private final boolean rare;

        SlotSymbol(Material material, String emoji, String name, boolean rare) {
            this.material = material;
            this.emoji = emoji;
            this.name = name;
            this.rare = rare;
        }

        public Material getMaterial() {
            return material;
        }

        public String getEmoji() {
            return emoji;
        }

        public String getName() {
            return name;
        }

        public boolean isRare() {
            return rare;
        }

        /**
         * Obtém todos os símbolos comuns.
         */
        public static SlotSymbol[] getCommon() {
            return new SlotSymbol[] { CHERRY, LEMON, MELON, GRAPE };
        }

        /**
         * Obtém todos os símbolos raros.
         */
        public static SlotSymbol[] getRare() {
            return new SlotSymbol[] { DIAMOND, STAR, SEVEN };
        }
    }
}
