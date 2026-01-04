package com.haumea.kitpvp.models;

/**
 * Modelo de um desafio diário do HaumeaMC.
 * 
 * @author HaumeaMC
 */
public class DailyChallenge {

    /**
     * Tipos de desafios
     */
    public enum Type {
        KILLS, // Matar X jogadores
        KILLSTREAK, // Alcançar X killstreak
        DUELS_WON, // Vencer X duelos
        PLAYTIME, // Jogar por X minutos
        SOUPS_USED // Usar X sopas
    }

    /**
     * Template para gerar desafios
     */
    public static class Template {
        private final String id;
        private final Type type;
        private final String displayName;
        private final String description;
        private final int requirement;
        private final long reward;

        public Template(String id, Type type, String displayName, String description,
                int requirement, long reward) {
            this.id = id;
            this.type = type;
            this.displayName = displayName;
            this.description = description;
            this.requirement = requirement;
            this.reward = reward;
        }

        public String getId() {
            return id;
        }

        public Type getType() {
            return type;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }

        public int getRequirement() {
            return requirement;
        }

        public long getReward() {
            return reward;
        }
    }

    private final String id;
    private final Type type;
    private final String displayName;
    private final String description;
    private final int requirement;
    private final long reward;

    public DailyChallenge(Template template) {
        this.id = template.getId();
        this.type = template.getType();
        this.displayName = template.getDisplayName();
        this.description = template.getDescription();
        this.requirement = template.getRequirement();
        this.reward = template.getReward();
    }

    public String getId() {
        return id;
    }

    public Type getType() {
        return type;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public int getRequirement() {
        return requirement;
    }

    public long getReward() {
        return reward;
    }

    /**
     * Obtém o ícone do Material para GUI
     */
    public String getMaterialIcon() {
        switch (type) {
            case KILLS:
                return "DIAMOND_SWORD";
            case KILLSTREAK:
                return "BLAZE_POWDER";
            case DUELS_WON:
                return "IRON_SWORD";
            case PLAYTIME:
                return "WATCH";
            case SOUPS_USED:
                return "MUSHROOM_SOUP";
            default:
                return "PAPER";
        }
    }

    /**
     * Formata o progresso para exibição
     */
    public String formatProgress(int current) {
        return "§f" + current + "§7/§f" + requirement;
    }

    /**
     * Calcula porcentagem de progresso
     */
    public int getProgressPercent(int current) {
        return Math.min(100, (current * 100) / requirement);
    }
}
