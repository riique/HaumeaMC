package com.haumea.kitpvp.models;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Representa um Kit de habilidade do servidor KitPvP.
 * 
 * Cada kit possui:
 * - Nome único de identificação
 * - Display name colorido
 * - Ícone para o menu
 * - Descrição das habilidades
 * - Preço para aluguel
 * - Permissão necessária
 * - Lista de kits incompatíveis (não podem ser usados juntos)
 * 
 * @author HaumeaMC
 */
public class Kit {

    private final String name;
    private final String displayName;
    private final Material icon;
    private final short iconData;
    private final List<String> description;
    private final int price;
    private final String permission;
    private final List<String> incompatibleKits;
    private final boolean isDefault;

    /**
     * Construtor do Kit
     * 
     * @param name             Nome único (lowercase, sem espaços)
     * @param displayName      Nome de exibição colorido
     * @param icon             Material do ícone
     * @param iconData         Data value do ícone (para variantes)
     * @param description      Descrição do kit (múltiplas linhas)
     * @param price            Preço para aluguel (0 = gratuito)
     * @param permission       Permissão necessária
     * @param incompatibleKits Lista de kits incompatíveis
     * @param isDefault        Se é um kit padrão (todos têm acesso)
     */
    public Kit(String name, String displayName, Material icon, short iconData,
            List<String> description, int price, String permission,
            List<String> incompatibleKits, boolean isDefault) {
        this.name = name.toLowerCase();
        this.displayName = displayName;
        this.icon = icon;
        this.iconData = iconData;
        this.description = description != null ? description : new ArrayList<>();
        this.price = price;
        this.permission = permission;
        this.incompatibleKits = incompatibleKits != null ? incompatibleKits : new ArrayList<>();
        this.isDefault = isDefault;
    }

    // ==================== BUILDER PATTERN ====================

    /**
     * Builder para criar kits de forma fluente
     */
    public static class Builder {
        private String name;
        private String displayName;
        private Material icon = Material.CHEST;
        private short iconData = 0;
        private List<String> description = new ArrayList<>();
        private int price = 0;
        private String permission;
        private List<String> incompatibleKits = new ArrayList<>();
        private boolean isDefault = false;

        public Builder(String name) {
            this.name = name;
            this.displayName = "§a" + name;
            this.permission = "haumea.kit." + name.toLowerCase();
        }

        public Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public Builder icon(Material icon) {
            this.icon = icon;
            return this;
        }

        public Builder icon(Material icon, short data) {
            this.icon = icon;
            this.iconData = data;
            return this;
        }

        public Builder description(String... lines) {
            for (String line : lines) {
                this.description.add(line);
            }
            return this;
        }

        public Builder price(int price) {
            this.price = price;
            return this;
        }

        public Builder permission(String permission) {
            this.permission = permission;
            return this;
        }

        public Builder incompatibleWith(String... kits) {
            for (String kit : kits) {
                this.incompatibleKits.add(kit.toLowerCase());
            }
            return this;
        }

        public Builder isDefault(boolean isDefault) {
            this.isDefault = isDefault;
            return this;
        }

        public Kit build() {
            return new Kit(name, displayName, icon, iconData, description,
                    price, permission, incompatibleKits, isDefault);
        }
    }

    /**
     * Cria um novo builder para o kit
     * 
     * @param name Nome do kit
     * @return Builder
     */
    public static Builder builder(String name) {
        return new Builder(name);
    }

    // ==================== GETTERS ====================

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Material getIcon() {
        return icon;
    }

    public short getIconData() {
        return iconData;
    }

    public List<String> getDescription() {
        return new ArrayList<>(description);
    }

    public int getPrice() {
        return price;
    }

    public String getPermission() {
        return permission;
    }

    public List<String> getIncompatibleKits() {
        return new ArrayList<>(incompatibleKits);
    }

    public boolean isDefault() {
        return isDefault;
    }

    /**
     * Cria o ItemStack representando este kit no menu
     * 
     * @return ItemStack para o menu
     */
    public ItemStack createIcon() {
        return new ItemStack(icon, 1, iconData);
    }

    /**
     * Verifica se este kit é incompatível com outro kit
     * 
     * @param otherKit Nome do outro kit
     * @return true se incompatíveis
     */
    public boolean isIncompatibleWith(String otherKit) {
        if (otherKit == null)
            return false;
        return incompatibleKits.contains(otherKit.toLowerCase());
    }

    @Override
    public String toString() {
        return "Kit{" +
                "name='" + name + '\'' +
                ", displayName='" + displayName + '\'' +
                ", price=" + price +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Kit kit = (Kit) o;
        return name.equals(kit.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
