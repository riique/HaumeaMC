package com.haumea.kitpvp.models;

/**
 * Modelo de uma conquista do HaumeaMC.
 * 
 * @author HaumeaMC
 */
public class Achievement {

    /**
     * Tipos de conquista baseados em qual estatística rastrear
     */
    public enum Type {
        KILLS, // Total de kills
        KILLSTREAK, // Maior killstreak
        COINS, // Coins acumulados
        ELO, // ELO alcançado
        SPECIAL, // Conquistas especiais (bounty, trades, etc.)
        DUELS, // Vitórias em duelos
        PLAYTIME, // Tempo jogado (em horas)
        DEATHS, // Mortes (conquistas cômicas)
        DAILY, // Desafios diários completados
        EVENTS // Participação em eventos (chat events, feast)
    }

    private final String id;
    private final String displayName;
    private final String description;
    private final String icon; // Nome do Material para GUI
    private final long reward; // Coins de recompensa
    private final Type type;
    private final int requirement; // Valor necessário para desbloquear

    public Achievement(String id, String displayName, String description,
            String icon, long reward, Type type, int requirement) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.icon = icon;
        this.reward = reward;
        this.type = type;
        this.requirement = requirement;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public String getIcon() {
        return icon;
    }

    public long getReward() {
        return reward;
    }

    public Type getType() {
        return type;
    }

    public int getRequirement() {
        return requirement;
    }

    /**
     * Obtém o ícone formatado para a categoria
     */
    public String getCategoryIcon() {
        switch (type) {
            case KILLS:
                return "§c⚔";
            case KILLSTREAK:
                return "§e🔥";
            case COINS:
                return "§6⛁";
            case ELO:
                return "§d✦";
            case SPECIAL:
                return "§b★";
            case DUELS:
                return "§a⚔";
            case PLAYTIME:
                return "§3⏱";
            case DEATHS:
                return "§4💀";
            case DAILY:
                return "§e✔";
            case EVENTS:
                return "§5🎉";
            default:
                return "§f●";
        }
    }

    /**
     * Formata o requisito para exibição
     */
    public String getFormattedRequirement() {
        switch (type) {
            case KILLS:
                return requirement + " kills";
            case KILLSTREAK:
                return requirement + " killstreak";
            case COINS:
                return formatNumber(requirement) + " coins";
            case ELO:
                return requirement + " ELO";
            case SPECIAL:
                return requirement + "x";
            case DUELS:
                return requirement + " vitorias";
            case PLAYTIME:
                return requirement + " horas";
            case DEATHS:
                return requirement + " mortes";
            case DAILY:
                return requirement + " desafios";
            case EVENTS:
                return requirement + " eventos";
            default:
                return String.valueOf(requirement);
        }
    }

    private String formatNumber(long number) {
        if (number >= 1000000) {
            return String.format("%.1fM", number / 1000000.0);
        } else if (number >= 1000) {
            return String.format("%.1fK", number / 1000.0);
        }
        return String.valueOf(number);
    }
}
