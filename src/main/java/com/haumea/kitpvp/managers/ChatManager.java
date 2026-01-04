package com.haumea.kitpvp.managers;

import com.haumea.kitpvp.HaumeaMC;

/**
 * Gerenciador de estado global do chat.
 * Controla se o chat está ativado ou desativado.
 * 
 * @author HaumeaMC
 */
public class ChatManager {

    private final HaumeaMC plugin;
    private boolean chatEnabled;
    private int slowModeSeconds;

    /**
     * Construtor do ChatManager
     * 
     * @param plugin Instância do plugin
     */
    public ChatManager(HaumeaMC plugin) {
        this.plugin = plugin;
        this.chatEnabled = true;
        this.slowModeSeconds = 0;
    }

    /**
     * Verifica se o chat global está ativado
     * 
     * @return true se chat está ativado
     */
    public boolean isChatEnabled() {
        return chatEnabled;
    }

    /**
     * Define se o chat global está ativado
     * 
     * @param enabled Estado do chat
     */
    public void setChatEnabled(boolean enabled) {
        this.chatEnabled = enabled;
    }

    /**
     * Obtém o tempo de slow mode (0 = desativado)
     * 
     * @return Segundos entre mensagens
     */
    public int getSlowModeSeconds() {
        return slowModeSeconds;
    }

    /**
     * Define o slow mode do chat
     * 
     * @param seconds Segundos entre mensagens (0 para desativar)
     */
    public void setSlowModeSeconds(int seconds) {
        this.slowModeSeconds = Math.max(0, seconds);
    }

    /**
     * Verifica se o slow mode está ativo
     * 
     * @return true se slow mode ativo
     */
    public boolean isSlowModeEnabled() {
        return slowModeSeconds > 0;
    }
}
