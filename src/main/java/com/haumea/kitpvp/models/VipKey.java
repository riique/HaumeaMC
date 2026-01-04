package com.haumea.kitpvp.models;

import java.util.UUID;

/**
 * Modelo para chaves de ativação de VIP
 * 
 * Cada chave representa um código único que pode ser usado para
 * ativar um grupo VIP por um determinado tempo.
 * 
 * @author HaumeaMC
 */
public class VipKey {

    private String key; // Código único: "HAUMEA-VIP30-ABCD1234"
    private String groupName; // Grupo a dar: "vip", "vip+"
    private long duration; // Duração do VIP em ms (0 = permanente)

    private long createdAt; // Timestamp de criação
    private String createdBy; // UUID, "CONSOLE", ou "API"

    private UUID usedBy; // UUID de quem usou (null se não usada)
    private long usedAt; // Timestamp de uso (0 se não usada)

    private long keyExpiresAt; // Quando a CHAVE expira (0 = nunca)

    /**
     * Construtor para nova chave
     */
    public VipKey(String key, String groupName, long duration, String createdBy) {
        this.key = key;
        this.groupName = groupName;
        this.duration = duration;
        this.createdAt = System.currentTimeMillis();
        this.createdBy = createdBy;
        this.usedBy = null;
        this.usedAt = 0;
        this.keyExpiresAt = 0;
    }

    /**
     * Construtor completo para carregar do MongoDB
     */
    public VipKey(String key, String groupName, long duration, long createdAt,
            String createdBy, UUID usedBy, long usedAt, long keyExpiresAt) {
        this.key = key;
        this.groupName = groupName;
        this.duration = duration;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.usedBy = usedBy;
        this.usedAt = usedAt;
        this.keyExpiresAt = keyExpiresAt;
    }

    // ==================== MÉTODOS DE VERIFICAÇÃO ====================

    /**
     * Verifica se a chave já foi usada
     */
    public boolean isUsed() {
        return usedBy != null;
    }

    /**
     * Verifica se a chave expirou (não pode mais ser usada)
     */
    public boolean isKeyExpired() {
        return keyExpiresAt > 0 && System.currentTimeMillis() > keyExpiresAt;
    }

    /**
     * Verifica se o VIP é permanente
     */
    public boolean isPermanent() {
        return duration == 0;
    }

    /**
     * Marca a chave como usada
     */
    public void markAsUsed(UUID player) {
        this.usedBy = player;
        this.usedAt = System.currentTimeMillis();
    }

    // ==================== GETTERS E SETTERS ====================

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public UUID getUsedBy() {
        return usedBy;
    }

    public void setUsedBy(UUID usedBy) {
        this.usedBy = usedBy;
    }

    public long getUsedAt() {
        return usedAt;
    }

    public void setUsedAt(long usedAt) {
        this.usedAt = usedAt;
    }

    public long getKeyExpiresAt() {
        return keyExpiresAt;
    }

    public void setKeyExpiresAt(long keyExpiresAt) {
        this.keyExpiresAt = keyExpiresAt;
    }

    @Override
    public String toString() {
        return "VipKey{" +
                "key='" + key + '\'' +
                ", groupName='" + groupName + '\'' +
                ", duration=" + duration +
                ", used=" + isUsed() +
                '}';
    }
}
