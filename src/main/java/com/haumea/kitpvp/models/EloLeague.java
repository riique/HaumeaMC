package com.haumea.kitpvp.models;

/**
 * Representa uma Liga/Rank do sistema de Elo do HaumeaMC.
 * 
 * Cada liga possui:
 * - Nome e cor única
 * - Símbolo identificador
 * - 5 divisões (I a V), exceto Legendary que é único
 * - Requisitos de Elo para cada divisão
 * - Recompensas em coins por promoção
 * 
 * @author HaumeaMC
 */
public class EloLeague {

    private final String id;
    private final String displayName;
    private final String colorCode;
    private final String symbol;
    private final int baseElo;
    private final int eloPerDivision;
    private final int coinsReward;
    private final int order;
    private final boolean hasDivisions;

    /**
     * Construtor completo de EloLeague
     * 
     * @param id             Identificador único (ex: "primary", "bronze")
     * @param displayName    Nome de exibição (ex: "Primary", "Bronze")
     * @param colorCode      Código de cor Minecraft (ex: "&a", "&8")
     * @param symbol         Símbolo da liga (ex: "✥", "✱")
     * @param baseElo        Elo mínimo da primeira divisão
     * @param eloPerDivision Elo adicional por divisão (100 = I:100, II:200, etc.)
     * @param coinsReward    Coins de recompensa por subir de divisão
     * @param order          Ordem hierárquica (maior = melhor liga)
     * @param hasDivisions   Se a liga possui divisões I-V
     */
    public EloLeague(String id, String displayName, String colorCode, String symbol,
            int baseElo, int eloPerDivision, int coinsReward, int order, boolean hasDivisions) {
        this.id = id;
        this.displayName = displayName;
        this.colorCode = colorCode;
        this.symbol = symbol;
        this.baseElo = baseElo;
        this.eloPerDivision = eloPerDivision;
        this.coinsReward = coinsReward;
        this.order = order;
        this.hasDivisions = hasDivisions;
    }

    /**
     * Construtor simplificado para ligas com divisões
     */
    public EloLeague(String id, String displayName, String colorCode, String symbol,
            int baseElo, int eloPerDivision, int coinsReward, int order) {
        this(id, displayName, colorCode, symbol, baseElo, eloPerDivision, coinsReward, order, true);
    }

    // ==================== GETTERS ====================

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getColorCode() {
        return colorCode;
    }

    /**
     * Obtém o código de cor convertido para § (uso em jogo)
     */
    public String getFormattedColorCode() {
        return colorCode.replace("&", "§");
    }

    public String getSymbol() {
        return symbol;
    }

    public int getBaseElo() {
        return baseElo;
    }

    public int getEloPerDivision() {
        return eloPerDivision;
    }

    public int getCoinsReward() {
        return coinsReward;
    }

    public int getOrder() {
        return order;
    }

    public boolean hasDivisions() {
        return hasDivisions;
    }

    // ==================== MÉTODOS DE DIVISÃO ====================

    /**
     * Obtém o Elo necessário para uma divisão específica
     * 
     * @param division Divisão (1-5, onde 1=I, 5=V)
     * @return Elo mínimo necessário
     */
    public int getEloForDivision(int division) {
        if (!hasDivisions) {
            return baseElo;
        }
        return baseElo + (division - 1) * eloPerDivision;
    }

    /**
     * Obtém a divisão atual baseado no Elo
     * 
     * @param elo Elo do jogador
     * @return Divisão (1-5) ou 0 se não está nesta liga
     */
    public int getDivisionForElo(int elo) {
        if (!hasDivisions) {
            return elo >= baseElo ? 1 : 0;
        }

        for (int div = 5; div >= 1; div--) {
            if (elo >= getEloForDivision(div)) {
                return div;
            }
        }
        return 0;
    }

    /**
     * Verifica se um Elo pertence a esta liga
     * 
     * @param elo Elo do jogador
     * @return true se pertence
     */
    public boolean containsElo(int elo) {
        return getDivisionForElo(elo) > 0;
    }

    /**
     * Converte número de divisão para algarismo romano
     * 
     * @param division Número (1-5)
     * @return Romano (I-V)
     */
    public static String toRoman(int division) {
        switch (division) {
            case 1:
                return "I";
            case 2:
                return "II";
            case 3:
                return "III";
            case 4:
                return "IV";
            case 5:
                return "V";
            default:
                return "";
        }
    }

    // ==================== MÉTODOS DE FORMATAÇÃO ====================

    /**
     * Obtém o nome completo da liga com divisão
     * Ex: "Gold III"
     * 
     * @param division Divisão (1-5)
     * @return Nome formatado
     */
    public String getFullName(int division) {
        if (!hasDivisions) {
            return displayName;
        }
        return displayName + " " + toRoman(division);
    }

    /**
     * Obtém o nome colorizado da liga com símbolo
     * Ex: "§6✹ Gold III"
     * 
     * @param division Divisão (1-5)
     * @return Nome colorizado
     */
    public String getFormattedName(int division) {
        String color = getFormattedColorCode();
        if (!hasDivisions) {
            return color + symbol + " " + displayName;
        }
        return color + symbol + " " + displayName + " " + toRoman(division);
    }

    /**
     * Obtém apenas o símbolo colorizado
     * Ex: "§6✹"
     */
    public String getFormattedSymbol() {
        return getFormattedColorCode() + symbol;
    }

    /**
     * Obtém o prefixo para chat/nametag
     * Ex: "§6✹ §6"
     */
    public String getPrefix() {
        String color = getFormattedColorCode();
        return color + symbol + " " + color;
    }

    /**
     * Obtém o sufixo para tab/nametag
     * Ex: " §6✹"
     */
    public String getSuffix() {
        return " " + getFormattedColorCode() + symbol;
    }

    @Override
    public String toString() {
        return "EloLeague{" +
                "id='" + id + '\'' +
                ", displayName='" + displayName + '\'' +
                ", symbol='" + symbol + '\'' +
                ", baseElo=" + baseElo +
                ", order=" + order +
                '}';
    }
}
