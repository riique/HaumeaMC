package com.haumea.kitpvp.models.casino;

import java.util.UUID;

/**
 * Representa uma solicitação de Coinflip pendente.
 * 
 * @author HaumeaMC
 */
public class CoinflipRequest {

    private final UUID requestId;
    private final UUID creatorId;
    private final String creatorName;
    private final long amount;
    private final long createdAt;
    private final long expiresAt;
    private boolean accepted;
    private UUID acceptorId;

    private static final long EXPIRATION_TIME = 5 * 60 * 1000L; // 5 minutos

    public CoinflipRequest(UUID creatorId, String creatorName, long amount) {
        this.requestId = UUID.randomUUID();
        this.creatorId = creatorId;
        this.creatorName = creatorName;
        this.amount = amount;
        this.createdAt = System.currentTimeMillis();
        this.expiresAt = createdAt + EXPIRATION_TIME;
        this.accepted = false;
    }

    public UUID getRequestId() {
        return requestId;
    }

    public UUID getCreatorId() {
        return creatorId;
    }

    public String getCreatorName() {
        return creatorName;
    }

    public long getAmount() {
        return amount;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    /**
     * Verifica se o coinflip expirou.
     */
    public boolean isExpired() {
        return System.currentTimeMillis() > expiresAt;
    }

    /**
     * Verifica se o coinflip ainda está disponível.
     */
    public boolean isAvailable() {
        return !accepted && !isExpired();
    }

    /**
     * Obtém o tempo desde a criação em formato legível.
     */
    public String getTimeAgo() {
        long elapsed = System.currentTimeMillis() - createdAt;
        long seconds = elapsed / 1000;

        if (seconds < 60) {
            return seconds + "s";
        } else {
            long minutes = seconds / 60;
            return minutes + "m";
        }
    }

    /**
     * Marca o coinflip como aceito.
     */
    public void accept(UUID acceptorId) {
        this.accepted = true;
        this.acceptorId = acceptorId;
    }

    public boolean isAccepted() {
        return accepted;
    }

    public UUID getAcceptorId() {
        return acceptorId;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CoinflipRequest) {
            return requestId.equals(((CoinflipRequest) obj).requestId);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return requestId.hashCode();
    }
}
