package com.haumea.kitpvp.abilities;

/**
 * Enum que representa a raridade de uma habilidade/kit.
 * 
 * Cada raridade tem uma cor associada e um nome de exibição.
 * 
 * @author HaumeaMC
 */
public enum AbilityRarity {

    /**
     * Kits básicos e baratos
     */
    COMMON("§7", "Comum", 1),

    /**
     * Kits intermediários
     */
    RARE("§9", "Raro", 2),

    /**
     * Kits avançados
     */
    EPIC("§5", "Épico", 3),

    /**
     * Kits muito poderosos
     */
    LEGENDARY("§6", "Lendário", 4),

    /**
     * Kits extremamente raros e poderosos
     */
    MYSTIC("§d", "Místico", 5);

    private final String color;
    private final String displayName;
    private final int tier;

    AbilityRarity(String color, String displayName, int tier) {
        this.color = color;
        this.displayName = displayName;
        this.tier = tier;
    }

    /**
     * Obtém a cor formatada da raridade
     */
    public String getColor() {
        return color;
    }

    /**
     * Obtém o nome de exibição da raridade
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Obtém o nome colorido completo
     */
    public String getColoredName() {
        return color + displayName;
    }

    /**
     * Obtém o tier numérico (1-5)
     */
    public int getTier() {
        return tier;
    }

    /**
     * Obtém o prefixo para a lore do item
     */
    public String getLorePrefix() {
        return "§7Raridade: " + getColoredName();
    }
}
