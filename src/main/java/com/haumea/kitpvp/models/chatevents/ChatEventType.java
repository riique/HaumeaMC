package com.haumea.kitpvp.models.chatevents;

/**
 * Tipos de eventos de chat disponíveis no sistema.
 * 
 * @author HaumeaMC
 */
public enum ChatEventType {

    /**
     * Resolva uma operação matemática
     */
    MATH("§c§lMATEMÁTICA", "§cResolva a conta", "📐"),

    /**
     * Descubra a palavra embaralhada
     */
    DESCRAMBLE("§e§lDESCRAMBLE", "§eDescubra a palavra embaralhada", "🔤"),

    /**
     * Responda a pergunta de trivia
     */
    TRIVIA("§a§lTRIVIA", "§aResponda a pergunta", "❓"),

    /**
     * Digite a frase exata primeiro
     */
    TYPE_RACE("§b§lTYPE RACE", "§bDigite a frase exata", "⚡"),

    /**
     * Descubra o próximo número da sequência
     */
    SEQUENCE("§d§lSEQUÊNCIA", "§dDescubra o próximo número", "🔢"),

    /**
     * Adivinhe a música pelo trecho
     */
    MUSIC_GUESS("§5§lADIVINHE A MÚSICA", "§5Qual é a música?", "🎵"),

    /**
     * Adivinhe o mob pela descrição
     */
    MOB_GUESS("§2§lADIVINHE O MOB", "§2Qual é o mob?", "💀"),

    /**
     * Adivinhe o item pela descrição
     */
    ITEM_GUESS("§6§lADIVINHE O ITEM", "§6Qual é o item?", "📦"),

    /**
     * Resolva o enigma
     */
    RIDDLE("§9§lENIGMA", "§9Resolva o enigma", "🧠"),

    /**
     * Complete a palavra faltando letras
     */
    COMPLETE_WORD("§3§lCOMPLETE A PALAVRA", "§3Descubra a palavra", "🎨");

    private final String displayName;
    private final String description;
    private final String icon;

    ChatEventType(String displayName, String description, String icon) {
        this.displayName = displayName;
        this.description = description;
        this.icon = icon;
    }

    /**
     * Obtém o nome de exibição formatado
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Obtém a descrição do tipo de evento
     */
    public String getDescription() {
        return description;
    }

    /**
     * Obtém o ícone do tipo de evento
     */
    public String getIcon() {
        return icon;
    }

    /**
     * Obtém o nome completo com ícone
     */
    public String getFullName() {
        return icon + " " + displayName + " " + icon;
    }
}
