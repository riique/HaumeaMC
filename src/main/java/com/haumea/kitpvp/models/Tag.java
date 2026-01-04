package com.haumea.kitpvp.models;

/**
 * Classe que representa uma Tag do servidor
 * 
 * @author HaumeaMC
 */
public class Tag {

    private final String name;
    private final String display;
    private final String prefix;
    private final String permission;
    private final String category;

    /**
     * Construtor da Tag (compatibilidade com código antigo)
     * 
     * @param name       Nome interno da tag (ex: "dono")
     * @param display    Nome de exibição colorido (ex: "§4DONO")
     * @param prefix     Prefixo a ser aplicado no chat (ex: "§4[DONO] ")
     * @param permission Permissão necessária para usar a tag
     */
    public Tag(String name, String display, String prefix, String permission) {
        this(name, display, prefix, permission, "comum");
    }

    /**
     * Construtor completo da Tag com categoria
     * 
     * @param name       Nome interno da tag (ex: "dono")
     * @param display    Nome de exibição colorido (ex: "§4DONO")
     * @param prefix     Prefixo a ser aplicado no chat (ex: "§4[DONO] ")
     * @param permission Permissão necessária para usar a tag
     * @param category   Categoria da tag (staff, vip, festiva, comum)
     */
    public Tag(String name, String display, String prefix, String permission, String category) {
        this.name = name;
        this.display = display;
        this.prefix = prefix;
        this.permission = permission;
        this.category = category != null ? category.toLowerCase() : "comum";
    }

    /**
     * Obtém o nome interno da tag
     * 
     * @return Nome da tag
     */
    public String getName() {
        return name;
    }

    /**
     * Obtém o nome de exibição da tag (com códigos de cor)
     * 
     * @return Display da tag
     */
    public String getDisplay() {
        return display;
    }

    /**
     * Obtém o nome de exibição formatado (com cores aplicadas)
     * 
     * @return Display da tag formatado
     */
    public String getFormattedDisplay() {
        return display.replace("&", "§");
    }

    /**
     * Obtém o prefixo da tag (com códigos de cor)
     * 
     * @return Prefixo da tag
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * Obtém a permissão necessária para usar a tag
     * 
     * @return Permissão da tag
     */
    public String getPermission() {
        return permission;
    }

    /**
     * Obtém a categoria da tag
     * 
     * @return Categoria (staff, vip, festiva, comum)
     */
    public String getCategory() {
        return category;
    }
}
