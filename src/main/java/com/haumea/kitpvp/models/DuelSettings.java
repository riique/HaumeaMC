package com.haumea.kitpvp.models;

import org.bukkit.Material;

/**
 * Configurações de um duelo 1v1.
 * 
 * Define os equipamentos, suprimentos e regras do duelo.
 * 
 * @author HaumeaMC
 */
public class DuelSettings {

    /**
     * Tipos de espada disponíveis
     */
    public enum SwordType {
        WOOD("Madeira", Material.WOOD_SWORD, "§6"),
        STONE("Pedra", Material.STONE_SWORD, "§7"),
        IRON("Ferro", Material.IRON_SWORD, "§f"),
        DIAMOND("Diamante", Material.DIAMOND_SWORD, "§b");

        private final String displayName;
        private final Material material;
        private final String color;

        SwordType(String displayName, Material material, String color) {
            this.displayName = displayName;
            this.material = material;
            this.color = color;
        }

        public String getDisplayName() {
            return displayName;
        }

        public Material getMaterial() {
            return material;
        }

        public String getColor() {
            return color;
        }

        public String getFormattedName() {
            return color + displayName;
        }
    }

    /**
     * Tipos de armadura disponíveis
     */
    public enum ArmorType {
        LEATHER("Couro", "§6", Material.LEATHER_HELMET, Material.LEATHER_CHESTPLATE, Material.LEATHER_LEGGINGS,
                Material.LEATHER_BOOTS),
        CHAIN("Malha", "§7", Material.CHAINMAIL_HELMET, Material.CHAINMAIL_CHESTPLATE, Material.CHAINMAIL_LEGGINGS,
                Material.CHAINMAIL_BOOTS),
        IRON("Ferro", "§f", Material.IRON_HELMET, Material.IRON_CHESTPLATE, Material.IRON_LEGGINGS,
                Material.IRON_BOOTS),
        DIAMOND("Diamante", "§b", Material.DIAMOND_HELMET, Material.DIAMOND_CHESTPLATE, Material.DIAMOND_LEGGINGS,
                Material.DIAMOND_BOOTS);

        private final String displayName;
        private final String color;
        private final Material helmet, chestplate, leggings, boots;

        ArmorType(String displayName, String color, Material helmet, Material chestplate, Material leggings,
                Material boots) {
            this.displayName = displayName;
            this.color = color;
            this.helmet = helmet;
            this.chestplate = chestplate;
            this.leggings = leggings;
            this.boots = boots;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getColor() {
            return color;
        }

        public String getFormattedName() {
            return color + displayName;
        }

        public Material getHelmet() {
            return helmet;
        }

        public Material getChestplate() {
            return chestplate;
        }

        public Material getLeggings() {
            return leggings;
        }

        public Material getBoots() {
            return boots;
        }
    }

    /**
     * Modos de suprimentos de sopas
     */
    public enum SoupMode {
        LIMITED_16("16 Sopas", 16, "§e"),
        LIMITED_32("32 Sopas", 32, "§6"),
        UNLIMITED("Infinitas", -1, "§a");

        private final String displayName;
        private final int amount;
        private final String color;

        SoupMode(String displayName, int amount, String color) {
            this.displayName = displayName;
            this.amount = amount;
            this.color = color;
        }

        public String getDisplayName() {
            return displayName;
        }

        public int getAmount() {
            return amount;
        }

        public String getColor() {
            return color;
        }

        public String getFormattedName() {
            return color + displayName;
        }

        public boolean isUnlimited() {
            return amount == -1;
        }
    }

    // ==================== CAMPOS ====================

    private SwordType swordType;
    private ArmorType armorType;
    private SoupMode soupMode;
    private boolean enchantments; // Sharpness/Protection
    private boolean recraft; // Materiais para craft de sopas

    // ==================== CONSTRUTOR ====================

    /**
     * Cria configurações padrão
     */
    public DuelSettings() {
        this.swordType = SwordType.DIAMOND;
        this.armorType = ArmorType.IRON;
        this.soupMode = SoupMode.UNLIMITED;
        this.enchantments = false;
        this.recraft = true;
    }

    /**
     * Cria uma cópia das configurações
     */
    public DuelSettings copy() {
        DuelSettings copy = new DuelSettings();
        copy.swordType = this.swordType;
        copy.armorType = this.armorType;
        copy.soupMode = this.soupMode;
        copy.enchantments = this.enchantments;
        copy.recraft = this.recraft;
        return copy;
    }

    // ==================== GETTERS E SETTERS ====================

    public SwordType getSwordType() {
        return swordType;
    }

    public void setSwordType(SwordType swordType) {
        this.swordType = swordType;
    }

    /**
     * Cicla para o próximo tipo de espada
     */
    public void cycleSwordType() {
        SwordType[] types = SwordType.values();
        int next = (swordType.ordinal() + 1) % types.length;
        this.swordType = types[next];
    }

    public ArmorType getArmorType() {
        return armorType;
    }

    public void setArmorType(ArmorType armorType) {
        this.armorType = armorType;
    }

    /**
     * Cicla para o próximo tipo de armadura
     */
    public void cycleArmorType() {
        ArmorType[] types = ArmorType.values();
        int next = (armorType.ordinal() + 1) % types.length;
        this.armorType = types[next];
    }

    public SoupMode getSoupMode() {
        return soupMode;
    }

    public void setSoupMode(SoupMode soupMode) {
        this.soupMode = soupMode;
    }

    /**
     * Cicla para o próximo modo de sopas
     */
    public void cycleSoupMode() {
        SoupMode[] modes = SoupMode.values();
        int next = (soupMode.ordinal() + 1) % modes.length;
        this.soupMode = modes[next];
    }

    public boolean hasEnchantments() {
        return enchantments;
    }

    public void setEnchantments(boolean enchantments) {
        this.enchantments = enchantments;
    }

    /**
     * Alterna enchantments
     */
    public void toggleEnchantments() {
        this.enchantments = !this.enchantments;
    }

    public boolean hasRecraft() {
        return recraft;
    }

    public void setRecraft(boolean recraft) {
        this.recraft = recraft;
    }

    /**
     * Alterna recraft
     */
    public void toggleRecraft() {
        this.recraft = !this.recraft;
    }

    // ==================== FORMATAÇÃO ====================

    /**
     * Obtém uma descrição resumida das configurações
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append(swordType.getFormattedName()).append(" §7+ ");
        sb.append(armorType.getFormattedName()).append(" §7| ");
        sb.append(soupMode.getFormattedName());
        if (enchantments)
            sb.append(" §7+ §dEnchants");
        if (recraft)
            sb.append(" §7+ §aRecraft");
        return sb.toString();
    }

    @Override
    public String toString() {
        return "DuelSettings{" +
                "sword=" + swordType +
                ", armor=" + armorType +
                ", soups=" + soupMode +
                ", enchants=" + enchantments +
                ", recraft=" + recraft +
                '}';
    }
}
