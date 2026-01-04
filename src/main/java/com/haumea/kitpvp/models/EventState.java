package com.haumea.kitpvp.models;

/**
 * Estados possíveis de um evento no servidor.
 * 
 * Fluxo normal:
 * NONE -> CREATED -> OPEN -> CLOSED -> STARTED -> FINISHED -> NONE
 * 
 * Fluxos alternativos:
 * - Qualquer estado -> NONE (cancelamento)
 * - STARTED -> PAUSED -> STARTED (pausa temporária)
 * 
 * @author HaumeaMC
 */
public enum EventState {

    /**
     * Nenhum evento ativo
     */
    NONE("§7Nenhum", "§7Sem evento"),

    /**
     * Evento criado, inscrições fechadas
     */
    CREATED("§eCriado", "§eEvento criado, aguardando abertura"),

    /**
     * Inscrições abertas, jogadores podem entrar
     */
    OPEN("§aAberto", "§aInscrições abertas"),

    /**
     * Inscrições fechadas, aguardando início
     */
    CLOSED("§cFechado", "§cInscrições fechadas"),

    /**
     * Evento em andamento
     */
    STARTED("§b§lIniciado", "§bEvento em andamento"),

    /**
     * Evento pausado temporariamente
     */
    PAUSED("§6Pausado", "§6Evento pausado"),

    /**
     * Evento encerrado, vencedor definido
     */
    FINISHED("§dFinalizado", "§dEvento finalizado");

    private final String displayName;
    private final String description;

    EventState(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    /**
     * Obtém o nome formatado do estado
     * 
     * @return Nome com cores
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Obtém a descrição do estado
     * 
     * @return Descrição com cores
     */
    public String getDescription() {
        return description;
    }

    /**
     * Verifica se o evento está ativo (pode ter jogadores)
     * 
     * @return true se há um evento ativo
     */
    public boolean isActive() {
        return this == CREATED || this == OPEN || this == CLOSED || this == STARTED || this == PAUSED;
    }

    /**
     * Verifica se as inscrições estão abertas
     * 
     * @return true se jogadores podem entrar
     */
    public boolean canJoin() {
        return this == OPEN;
    }

    /**
     * Verifica se o evento está em andamento
     * 
     * @return true se o evento está rodando
     */
    public boolean isRunning() {
        return this == STARTED;
    }

    /**
     * Verifica se está pausado
     * 
     * @return true se pausado
     */
    public boolean isPaused() {
        return this == PAUSED;
    }
}
