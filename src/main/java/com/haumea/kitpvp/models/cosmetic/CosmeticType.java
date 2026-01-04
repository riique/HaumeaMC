package com.haumea.kitpvp.models.cosmetic;

/**
 * Tipos de cosméticos disponíveis no sistema.
 * 
 * @author HaumeaMC
 */
public enum CosmeticType {

    /**
     * Efeitos visuais que aparecem quando você mata alguém.
     * Inclui partículas, explosões, animações.
     */
    KILL_EFFECT("Efeito de Kill", "kill_effect", "&c"),

    /**
     * Sons que tocam quando você mata alguém.
     * Inclui trovões, músicas, efeitos sonoros.
     */
    KILL_SOUND("Som de Kill", "kill_sound", "&e"),

    /**
     * Mensagens customizadas exibidas no chat quando você mata.
     */
    KILL_MESSAGE("Mensagem de Kill", "kill_message", "&b"),

    /**
     * Efeitos visuais que aparecem quando você morre.
     */
    DEATH_EFFECT("Efeito de Morte", "death_effect", "&8"),

    /**
     * Partículas que seguem o jogador (futuro).
     */
    TRAIL("Rastro", "trail", "&d");

    private final String displayName;
    private final String dataKey;
    private final String colorCode;

    CosmeticType(String displayName, String dataKey, String colorCode) {
        this.displayName = displayName;
        this.dataKey = dataKey;
        this.colorCode = colorCode;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Chave usada no customData do PlayerData
     */
    public String getDataKey() {
        return dataKey;
    }

    public String getColorCode() {
        return colorCode;
    }

    public String getColoredName() {
        return colorCode + displayName;
    }
}
