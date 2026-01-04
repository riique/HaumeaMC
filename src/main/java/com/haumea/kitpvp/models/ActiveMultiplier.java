package com.haumea.kitpvp.models;

import java.util.UUID;

/**
 * Representa um multiplicador ativo de um jogador.
 * 
 * Armazena:
 * - Tipo do multiplicador ativo
 * - Timestamp absoluto de expiração (System.currentTimeMillis())
 * 
 * O uso de timestamp absoluto permite que o tempo continue
 * contando mesmo quando o jogador está offline.
 * 
 * @author HaumeaMC
 */
public class ActiveMultiplier {

    private final UUID playerUUID;
    private final MultiplierType type;
    private long expirationTimestamp;

    /**
     * Construtor do ActiveMultiplier
     * 
     * @param playerUUID          UUID do jogador
     * @param type                Tipo do multiplicador
     * @param expirationTimestamp Timestamp de expiração em milissegundos
     */
    public ActiveMultiplier(UUID playerUUID, MultiplierType type, long expirationTimestamp) {
        this.playerUUID = playerUUID;
        this.type = type;
        this.expirationTimestamp = expirationTimestamp;
    }

    /**
     * Cria um multiplicador com a duração padrão do tipo
     * 
     * @param playerUUID UUID do jogador
     * @param type       Tipo do multiplicador
     * @return Nova instância do ActiveMultiplier
     */
    public static ActiveMultiplier createWithDefaultDuration(UUID playerUUID, MultiplierType type) {
        long expiration = System.currentTimeMillis() + type.getDefaultDuration();
        return new ActiveMultiplier(playerUUID, type, expiration);
    }

    /**
     * Cria um multiplicador com duração customizada
     * 
     * @param playerUUID     UUID do jogador
     * @param type           Tipo do multiplicador
     * @param durationMillis Duração em milissegundos
     * @return Nova instância do ActiveMultiplier
     */
    public static ActiveMultiplier createWithDuration(UUID playerUUID, MultiplierType type, long durationMillis) {
        long expiration = System.currentTimeMillis() + durationMillis;
        return new ActiveMultiplier(playerUUID, type, expiration);
    }

    // ==================== GETTERS ====================

    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public MultiplierType getType() {
        return type;
    }

    public long getExpirationTimestamp() {
        return expirationTimestamp;
    }

    // ==================== MÉTODOS DE VERIFICAÇÃO ====================

    /**
     * Verifica se o multiplicador expirou
     * 
     * @return true se o tempo acabou
     */
    public boolean isExpired() {
        return System.currentTimeMillis() >= expirationTimestamp;
    }

    /**
     * Obtém o tempo restante em milissegundos
     * 
     * @return Tempo restante ou 0 se expirado
     */
    public long getRemainingTime() {
        long remaining = expirationTimestamp - System.currentTimeMillis();
        return Math.max(0, remaining);
    }

    /**
     * Obtém o tempo restante formatado (ex: "1h 30min 45s")
     */
    public String getFormattedRemainingTime() {
        long remaining = getRemainingTime();

        if (remaining <= 0) {
            return "Expirado";
        }

        long seconds = remaining / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;

        StringBuilder sb = new StringBuilder();
        if (hours > 0) {
            sb.append(hours).append("h ");
        }
        if (minutes % 60 > 0) {
            sb.append(minutes % 60).append("min ");
        }
        if (hours == 0 && seconds % 60 > 0) {
            sb.append(seconds % 60).append("s");
        }

        return sb.toString().trim();
    }

    /**
     * Obtém o tempo restante formatado de forma curta (ex: "01:30:45")
     */
    public String getShortFormattedTime() {
        long remaining = getRemainingTime();

        if (remaining <= 0) {
            return "00:00:00";
        }

        long seconds = remaining / 1000;
        long minutes = (seconds % 3600) / 60;
        long hours = seconds / 3600;
        seconds = seconds % 60;

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    // ==================== MÉTODOS DE MODIFICAÇÃO ====================

    /**
     * Estende o tempo do multiplicador
     * 
     * @param additionalMillis Tempo adicional em milissegundos
     */
    public void extendTime(long additionalMillis) {
        this.expirationTimestamp += additionalMillis;
    }

    /**
     * Define um novo tempo de expiração
     * 
     * @param newExpirationTimestamp Novo timestamp de expiração
     */
    public void setExpirationTimestamp(long newExpirationTimestamp) {
        this.expirationTimestamp = newExpirationTimestamp;
    }

    // ==================== SERIALIZAÇÃO ====================

    /**
     * Converte para string para salvamento
     * Formato: "TYPE:EXPIRATION"
     */
    public String serialize() {
        return type.name() + ":" + expirationTimestamp;
    }

    /**
     * Cria instância a partir de string serializada
     * 
     * @param playerUUID UUID do jogador
     * @param serialized String no formato "TYPE:EXPIRATION"
     * @return ActiveMultiplier ou null se inválido
     */
    public static ActiveMultiplier deserialize(UUID playerUUID, String serialized) {
        if (serialized == null || !serialized.contains(":")) {
            return null;
        }

        try {
            String[] parts = serialized.split(":");
            MultiplierType type = MultiplierType.valueOf(parts[0]);
            long expiration = Long.parseLong(parts[1]);
            return new ActiveMultiplier(playerUUID, type, expiration);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String toString() {
        return "ActiveMultiplier{" +
                "type=" + type +
                ", remaining=" + getFormattedRemainingTime() +
                ", expired=" + isExpired() +
                '}';
    }
}
