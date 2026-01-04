package com.haumea.kitpvp.listeners;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.managers.ChatEventManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

/**
 * Listener para capturar respostas de eventos de chat.
 * 
 * Este listener intercepta mensagens no chat para verificar
 * se são respostas corretas para o evento ativo.
 * 
 * @author HaumeaMC
 */
public class ChatEventListener implements Listener {

    private final HaumeaMC plugin;
    private final ChatEventManager chatEventManager;

    public ChatEventListener(HaumeaMC plugin) {
        this.plugin = plugin;
        this.chatEventManager = plugin.getChatEventManager();
    }

    /**
     * Intercepta mensagens de chat para verificar respostas de eventos.
     * 
     * Prioridade LOWEST para processar antes de outros handlers de chat.
     * Não cancela o evento para que a mensagem ainda apareça normalmente.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        // Verificar se há evento ativo
        if (chatEventManager == null || !chatEventManager.isEventActive()) {
            return;
        }

        Player player = event.getPlayer();
        String message = event.getMessage();

        // Processar a resposta no thread principal (sync)
        // O método processAnswer já é thread-safe
        final String answer = message;

        // Executar no thread principal para evitar problemas de concorrência
        plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {
                chatEventManager.processAnswer(player, answer);
            }
        });
    }
}
