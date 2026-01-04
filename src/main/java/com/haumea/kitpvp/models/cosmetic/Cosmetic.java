package com.haumea.kitpvp.models.cosmetic;

import org.bukkit.Material;
import org.bukkit.entity.Player;

/**
 * Modelo base para todos os cosméticos do servidor.
 * 
 * Cada cosmético possui:
 * - Identificador único
 * - Nome de exibição
 * - Descrição
 * - Tipo (kill effect, sound, etc)
 * - Raridade (define preço e cor)
 * - Ícone para menus
 * - Permissão opcional
 * 
 * @author HaumeaMC
 */
public abstract class Cosmetic {

    protected final String id;
    protected final String displayName;
    protected final String[] description;
    protected final CosmeticType type;
    protected final CosmeticRarity rarity;
    protected final Material icon;
    protected final short iconData;
    protected final String permission;
    protected final int price;

    /**
     * Construtor do cosmético
     * 
     * @param id          ID único (lowercase, sem espaços)
     * @param displayName Nome de exibição (com cores)
     * @param description Descrição para o menu
     * @param type        Tipo do cosmético
     * @param rarity      Raridade
     * @param icon        Material do ícone
     * @param iconData    Data value do ícone (0 para padrão)
     * @param permission  Permissão necessária (null para nenhuma)
     * @param price       Preço em coins (0 para grátis)
     */
    public Cosmetic(String id, String displayName, String[] description,
            CosmeticType type, CosmeticRarity rarity,
            Material icon, short iconData, String permission, int price) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.type = type;
        this.rarity = rarity;
        this.icon = icon;
        this.iconData = iconData;
        this.permission = permission;
        this.price = price > 0 ? price : rarity.getBasePrice();
    }

    /**
     * Construtor simplificado (sem data value e permissão)
     */
    public Cosmetic(String id, String displayName, String[] description,
            CosmeticType type, CosmeticRarity rarity, Material icon, int price) {
        this(id, displayName, description, type, rarity, icon, (short) 0, null, price);
    }

    // ==================== MÉTODOS ABSTRATOS ====================

    /**
     * Executa o efeito do cosmético.
     * Implementado por cada tipo de cosmético.
     * 
     * @param player Jogador que possui o cosmético
     * @param target Alvo do efeito (pode ser null dependendo do tipo)
     */
    public abstract void apply(Player player, Player target);

    /**
     * Retorna uma prévia do efeito (para exibição no menu).
     * 
     * @param player Jogador que está vendo a prévia
     */
    public abstract void preview(Player player);

    // ==================== GETTERS ====================

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Nome de exibição com cor da raridade
     */
    public String getFormattedName() {
        return rarity.getFormattedColor() + displayName;
    }

    public String[] getDescription() {
        return description;
    }

    public CosmeticType getType() {
        return type;
    }

    public CosmeticRarity getRarity() {
        return rarity;
    }

    public Material getIcon() {
        return icon;
    }

    public short getIconData() {
        return iconData;
    }

    public String getPermission() {
        return permission;
    }

    public boolean hasPermission() {
        return permission != null && !permission.isEmpty();
    }

    public int getPrice() {
        return price;
    }

    /**
     * Verifica se o jogador tem permissão para usar este cosmético
     */
    public boolean canUse(Player player) {
        if (!hasPermission()) {
            return true;
        }
        return player.hasPermission(permission);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        Cosmetic cosmetic = (Cosmetic) obj;
        return id.equals(cosmetic.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Cosmetic{" +
                "id='" + id + '\'' +
                ", type=" + type +
                ", rarity=" + rarity +
                '}';
    }
}
