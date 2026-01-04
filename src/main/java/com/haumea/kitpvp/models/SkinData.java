package com.haumea.kitpvp.models;

/**
 * Modelo de dados de uma skin.
 * Armazena as propriedades de textura obtidas da API Mojang.
 * 
 * A skin é identificada por:
 * - Value: Código Base64 contendo a textura
 * - Signature: Assinatura Mojang que valida a textura
 * 
 * @author HaumeaMC
 */
public class SkinData {

    private final String ownerName;
    private final String ownerUUID;
    private final String value;
    private final String signature;
    private final long fetchedAt;

    /**
     * Construtor do SkinData
     * 
     * @param ownerName Nome do dono original da skin
     * @param ownerUUID UUID do dono original
     * @param value     Valor Base64 da textura
     * @param signature Assinatura Mojang
     */
    public SkinData(String ownerName, String ownerUUID, String value, String signature) {
        this.ownerName = ownerName;
        this.ownerUUID = ownerUUID;
        this.value = value;
        this.signature = signature;
        this.fetchedAt = System.currentTimeMillis();
    }

    /**
     * Construtor completo com timestamp customizado
     */
    public SkinData(String ownerName, String ownerUUID, String value, String signature, long fetchedAt) {
        this.ownerName = ownerName;
        this.ownerUUID = ownerUUID;
        this.value = value;
        this.signature = signature;
        this.fetchedAt = fetchedAt;
    }

    /**
     * Obtém o nome do dono original da skin
     */
    public String getOwnerName() {
        return ownerName;
    }

    /**
     * Obtém o UUID do dono original
     */
    public String getOwnerUUID() {
        return ownerUUID;
    }

    /**
     * Obtém o valor Base64 da textura
     */
    public String getValue() {
        return value;
    }

    /**
     * Obtém a assinatura Mojang
     */
    public String getSignature() {
        return signature;
    }

    /**
     * Obtém o timestamp de quando a skin foi buscada
     */
    public long getFetchedAt() {
        return fetchedAt;
    }

    /**
     * Verifica se a skin ainda é válida (cache de 1 hora)
     */
    public boolean isExpired() {
        long oneHour = 60 * 60 * 1000;
        return System.currentTimeMillis() - fetchedAt > oneHour;
    }

    /**
     * Verifica se os dados são válidos (não nulos)
     */
    public boolean isValid() {
        return value != null && !value.isEmpty() && signature != null && !signature.isEmpty();
    }

    @Override
    public String toString() {
        return "SkinData{" +
                "ownerName='" + ownerName + '\'' +
                ", ownerUUID='" + ownerUUID + '\'' +
                ", valid=" + isValid() +
                ", expired=" + isExpired() +
                '}';
    }
}
