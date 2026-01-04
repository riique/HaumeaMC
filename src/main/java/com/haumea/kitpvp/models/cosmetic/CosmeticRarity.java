package com.haumea.kitpvp.models.cosmetic;

/**
 * Raridades dos cosméticos.
 * Define cor, preço base e multiplicadores.
 * 
 * @author HaumeaMC
 */
public enum CosmeticRarity {

    COMMON("Comum", "&a", 1000, 1.0),
    UNCOMMON("Incomum", "&e", 2500, 1.5),
    RARE("Raro", "&b", 5000, 2.0),
    EPIC("Epico", "&d", 10000, 3.0),
    LEGENDARY("Lendario", "&6", 25000, 5.0),
    MYTHIC("Mitico", "&c", 50000, 10.0);

    private final String displayName;
    private final String colorCode;
    private final int basePrice;
    private final double multiplier;

    CosmeticRarity(String displayName, String colorCode, int basePrice, double multiplier) {
        this.displayName = displayName;
        this.colorCode = colorCode;
        this.basePrice = basePrice;
        this.multiplier = multiplier;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getColorCode() {
        return colorCode;
    }

    public String getColoredName() {
        return colorCode + displayName;
    }

    public int getBasePrice() {
        return basePrice;
    }

    public double getMultiplier() {
        return multiplier;
    }

    /**
     * Retorna a cor do Minecraft formatada
     */
    public String getFormattedColor() {
        return colorCode.replace("&", "§");
    }
}
