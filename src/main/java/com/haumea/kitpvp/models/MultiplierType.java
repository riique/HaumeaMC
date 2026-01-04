package com.haumea.kitpvp.models;

import org.bukkit.Material;

/**
 * Enum que define todos os tipos de multiplicadores de coins disponíveis.
 * 
 * Cada multiplicador possui:
 * - Valor do multiplicador (ex: 1.5 = 50% a mais)
 * - Duração padrão em milissegundos
 * - Material para exibição no menu
 * - Cor da Glass Pane para decoração
 * - Display name formatado
 * 
 * @author HaumeaMC
 */
public enum MultiplierType {

    /**
     * Multiplicador básico - 50% a mais de coins
     */
    X1_5(1.5, 3600000L, Material.IRON_INGOT, 8, "&7x1.5", "&7&lBÁSICO"),

    /**
     * Multiplicador intermediário - 100% a mais de coins
     */
    X2_0(2.0, 3600000L, Material.GOLD_INGOT, 1, "&6x2.0", "&6&lINTERMEDIÁRIO"),

    /**
     * Multiplicador avançado - 150% a mais de coins
     */
    X2_5(2.5, 3600000L, Material.DIAMOND, 3, "&bx2.5", "&b&lAVANÇADO"),

    /**
     * Multiplicador premium - 200% a mais de coins
     */
    X3_0(3.0, 3600000L, Material.EMERALD, 5, "&ax3.0", "&a&lPREMIUM"),

    /**
     * Multiplicador máximo - 250% a mais de coins
     */
    X3_5(3.5, 3600000L, Material.NETHER_STAR, 10, "&dx3.5", "&d&l✦ MÁXIMO ✦");

    private final double value;
    private final long defaultDuration;
    private final Material material;
    private final int glassColor;
    private final String displayMultiplier;
    private final String displayRarity;

    /**
     * Construtor do MultiplierType
     * 
     * @param value             Valor do multiplicador
     * @param defaultDuration   Duração padrão em milissegundos
     * @param material          Material para exibição no menu
     * @param glassColor        Cor do vidro decorativo (0-15)
     * @param displayMultiplier Texto do multiplicador formatado
     * @param displayRarity     Nome da raridade formatado
     */
    MultiplierType(double value, long defaultDuration, Material material, int glassColor,
            String displayMultiplier, String displayRarity) {
        this.value = value;
        this.defaultDuration = defaultDuration;
        this.material = material;
        this.glassColor = glassColor;
        this.displayMultiplier = displayMultiplier;
        this.displayRarity = displayRarity;
    }

    /**
     * Obtém o valor numérico do multiplicador
     */
    public double getValue() {
        return value;
    }

    /**
     * Obtém a duração padrão em milissegundos (1 hora por padrão)
     */
    public long getDefaultDuration() {
        return defaultDuration;
    }

    /**
     * Obtém o Material para exibição no menu
     */
    public Material getMaterial() {
        return material;
    }

    /**
     * Obtém a cor do vidro decorativo
     */
    public int getGlassColor() {
        return glassColor;
    }

    /**
     * Obtém o texto do multiplicador formatado (ex: "&6x2.0")
     */
    public String getDisplayMultiplier() {
        return displayMultiplier;
    }

    /**
     * Obtém o nome da raridade formatado (ex: "&6&lINTERMEDIÁRIO")
     */
    public String getDisplayRarity() {
        return displayRarity;
    }

    /**
     * Obtém a porcentagem de bônus (ex: 2.0 -> 100%)
     */
    public int getBonusPercentage() {
        return (int) ((value - 1.0) * 100);
    }

    /**
     * Formata a duração padrão como texto legível
     */
    public String getFormattedDuration() {
        long minutes = defaultDuration / 60000;
        if (minutes >= 60) {
            long hours = minutes / 60;
            return hours + "h";
        }
        return minutes + "min";
    }

    /**
     * Busca um MultiplierType por seu nome (case-insensitive)
     * 
     * @param name Nome do tipo (ex: "x2_0", "X2.0", "2.0")
     * @return MultiplierType ou null se não encontrado
     */
    public static MultiplierType fromString(String name) {
        if (name == null)
            return null;

        // Normalizar entrada
        String normalized = name.toUpperCase()
                .replace(".", "_")
                .replace(",", "_");

        // Se não começa com X, adicionar
        if (!normalized.startsWith("X")) {
            normalized = "X" + normalized;
        }

        try {
            return valueOf(normalized);
        } catch (IllegalArgumentException e) {
            // Tentar buscar pelo valor numérico
            for (MultiplierType type : values()) {
                String valueStr = String.valueOf(type.value);
                if (valueStr.equals(name) || valueStr.replace(".", "_").equals(normalized)) {
                    return type;
                }
            }
            return null;
        }
    }

    /**
     * Obtém um array com todos os valores ordenados por valor
     */
    public static MultiplierType[] getOrderedValues() {
        return values(); // Já estão ordenados na declaração
    }
}
