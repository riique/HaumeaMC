package com.haumea.kitpvp.models;

import java.util.UUID;

/**
 * Representa uma punição aplicada a um jogador.
 * 
 * Tipos de punição:
 * - BAN: Banimento do servidor (inclui IP ban)
 * - MUTE: Silenciamento no chat
 * - WARN: Aviso ao jogador
 * - KICK: Expulsão do servidor
 * 
 * @author HaumeaMC
 */
public class Punishment {

    /**
     * Tipos de punição disponíveis
     */
    public enum PunishmentType {
        BAN("BAN", "&c&lBANIDO", "&c"),
        MUTE("MUTE", "&6&lMUTADO", "&6"),
        WARN("WARN", "&e&lAVISO", "&e"),
        KICK("KICK", "&c&lEXPULSO", "&c");

        private final String name;
        private final String display;
        private final String color;

        PunishmentType(String name, String display, String color) {
            this.name = name;
            this.display = display;
            this.color = color;
        }

        public String getName() {
            return name;
        }

        public String getDisplay() {
            return display;
        }

        public String getColor() {
            return color;
        }
    }

    // ==================== CAMPOS ====================

    private final String id; // ID único da punição
    private final PunishmentType type; // Tipo da punição
    private final UUID targetUuid; // UUID do jogador punido
    private final String targetName; // Nome do jogador punido
    private final String targetIp; // IP do jogador (para IP ban)
    private final UUID staffUuid; // UUID do staff que puniu
    private final String staffName; // Nome do staff que puniu
    private final String reason; // Motivo da punição
    private final String proof; // Link da prova
    private final long timestamp; // Quando foi aplicada
    private final long expiration; // Quando expira (0 = permanente)
    private boolean active; // Se está ativa

    // ==================== CONSTRUTOR ====================

    public Punishment(String id, PunishmentType type, UUID targetUuid, String targetName,
            String targetIp, UUID staffUuid, String staffName, String reason,
            String proof, long timestamp, long expiration, boolean active) {
        this.id = id;
        this.type = type;
        this.targetUuid = targetUuid;
        this.targetName = targetName;
        this.targetIp = targetIp;
        this.staffUuid = staffUuid;
        this.staffName = staffName;
        this.reason = reason;
        this.proof = proof;
        this.timestamp = timestamp;
        this.expiration = expiration;
        this.active = active;
    }

    // ==================== MÉTODOS ====================

    /**
     * Verifica se a punição está expirada
     */
    public boolean isExpired() {
        if (expiration == 0)
            return false; // Permanente nunca expira
        return System.currentTimeMillis() >= expiration;
    }

    /**
     * Verifica se a punição está ativa (não expirada e not revoked)
     */
    public boolean isCurrentlyActive() {
        return active && !isExpired();
    }

    /**
     * Revoga a punição
     */
    public void revoke() {
        this.active = false;
    }

    /**
     * Obtém o tempo restante em milissegundos
     * 
     * @return tempo restante, 0 se permanente, -1 se expirada
     */
    public long getTimeRemaining() {
        if (expiration == 0)
            return 0; // Permanente
        long remaining = expiration - System.currentTimeMillis();
        return remaining > 0 ? remaining : -1;
    }

    /**
     * Formata o tempo restante para exibição
     */
    public String getFormattedTimeRemaining() {
        if (expiration == 0)
            return "Permanente";

        long remaining = getTimeRemaining();
        if (remaining < 0)
            return "Expirado";

        return formatDuration(remaining);
    }

    /**
     * Formata a duração total da punição
     */
    public String getFormattedDuration() {
        if (expiration == 0)
            return "Permanente";
        long duration = expiration - timestamp;
        return formatDuration(duration);
    }

    /**
     * Formata duração em texto legível
     */
    private String formatDuration(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        StringBuilder sb = new StringBuilder();
        if (days > 0)
            sb.append(days).append("d ");
        if (hours % 24 > 0)
            sb.append(hours % 24).append("h ");
        if (minutes % 60 > 0)
            sb.append(minutes % 60).append("m ");
        if (seconds % 60 > 0 || sb.length() == 0)
            sb.append(seconds % 60).append("s");

        return sb.toString().trim();
    }

    // ==================== GETTERS ====================

    public String getId() {
        return id;
    }

    public PunishmentType getType() {
        return type;
    }

    public UUID getTargetUuid() {
        return targetUuid;
    }

    public String getTargetName() {
        return targetName;
    }

    public String getTargetIp() {
        return targetIp;
    }

    public UUID getStaffUuid() {
        return staffUuid;
    }

    public String getStaffName() {
        return staffName;
    }

    public String getReason() {
        return reason;
    }

    public String getProof() {
        return proof;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public long getExpiration() {
        return expiration;
    }

    public boolean isActive() {
        return active;
    }
}
