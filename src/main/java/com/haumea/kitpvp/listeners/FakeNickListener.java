package com.haumea.kitpvp.listeners;

import com.haumea.kitpvp.HaumeaMC;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;

/**
 * Listener para o sistema de Fake Nick.
 * 
 * Gerencia eventos relacionados ao fake nick:
 * - Restaurar fake nick ao logar
 * - Limpar fake nick ao deslogar
 * - Formatar chat com nome fake
 * 
 * @author HaumeaMC
 */
public class FakeNickListener implements Listener {

    private final HaumeaMC plugin;

    public FakeNickListener(HaumeaMC plugin) {
        this.plugin = plugin;
    }

    /**
     * Evento de entrada do jogador
     * Restaura o fake nick se o jogador estava usando um antes de sair
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (plugin.getFakeNickManager() != null) {
            plugin.getFakeNickManager().onPlayerJoin(event.getPlayer());
        }
    }

    /**
     * Evento de saída do jogador
     * Notifica o FakeNickManager para lidar com a saída
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (plugin.getFakeNickManager() != null) {
            plugin.getFakeNickManager().onPlayerQuit(event.getPlayer());
        }
    }

    /**
     * Evento de chat
     * Garante que o nome exibido no chat seja o fake nick
     * 
     * NOTA: Este evento tem prioridade LOWEST para permitir
     * que outros plugins modifiquem o formato depois
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (plugin.getFakeNickManager() == null) {
            return;
        }

        // Se o jogador está usando fake nick, o displayName já foi configurado
        // pelo FakeNickManager ou TabManager, então não precisamos fazer nada aqui
        // O ChatListener já usa player.getDisplayName() para formatar
    }
}
