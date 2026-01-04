package com.haumea.kitpvp.server;

/**
 * Define os tipos de servidor suportados pelo plugin.
 * Cada tipo carrega apenas os sistemas necessários.
 */
public enum ServerType {

    /**
     * Servidor de Lobby - Carrega apenas sistemas essenciais:
     * - Chat, Tags, Grupos
     * - Banco de dados MongoDB
     * - Seletor de servidores
     * - Tab/Scoreboard básico
     */
    LOBBY("Lobby"),

    /**
     * Servidor de KitPvP - Carrega todos os sistemas:
     * - Kits, Sopas, Combate
     * - Duelos, Eventos, Feast
     * - Cassino, Cosméticos
     * - Bounties, Rankings
     * - E todos os outros sistemas
     */
    KITPVP("KitPvP");

    private final String displayName;

    ServerType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Verifica se este tipo de servidor precisa de sistemas de combate.
     * 
     * @return true se for um servidor de combate (KitPvP, etc)
     */
    public boolean requiresCombat() {
        return this == KITPVP;
    }

    /**
     * Verifica se este tipo de servidor é um lobby.
     * 
     * @return true se for um lobby
     */
    public boolean isLobby() {
        return this == LOBBY;
    }

    /**
     * Obtém o tipo de servidor a partir de uma string.
     * 
     * @param name Nome do tipo (case insensitive)
     * @return ServerType correspondente, ou KITPVP se não encontrado
     */
    public static ServerType fromString(String name) {
        if (name == null || name.isEmpty()) {
            return KITPVP;
        }

        try {
            return valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return KITPVP;
        }
    }
}
