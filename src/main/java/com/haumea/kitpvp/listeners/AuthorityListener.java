package com.haumea.kitpvp.listeners;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.permissions.AuthorityManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Listener responsável por gerenciar a injeção do sistema de autoridade
 * nos jogadores quando entram e saem do servidor.
 * 
 * O sistema de permissões customizado:
 * 1. Desativa o poder do /op (isOp() sempre retorna false)
 * 2. DONO recebe permissão * (tudo)
 * 3. Fallback para OP real em permissões vanilla não configuradas
 * 
 * @author HaumeaMC
 */
public class AuthorityListener implements Listener {

    private final HaumeaMC plugin;

    public AuthorityListener(HaumeaMC plugin) {
        this.plugin = plugin;
    }

    /**
     * Injeta o HaumeaPermissible quando o jogador entra.
     * 
     * Prioridade LOWEST para garantir que somos os primeiros a agir,
     * antes de qualquer outro listener que possa precisar verificar permissões.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        AuthorityManager authorityManager = plugin.getAuthorityManager();
        if (authorityManager == null) {
            return;
        }

        // Injetar nosso gerenciador de permissões
        boolean success = authorityManager.injectPermissible(player);

        if (!success) {
            // Se falhar, apenas log se em modo debug
            if (plugin.getConfigManager() != null && plugin.getConfigManager().isDebug()) {
                plugin.getLogger().warning("§eAuthorityListener: Falha ao injetar permissões em " + player.getName());
            }
        }
    }

    /**
     * Remove o HaumeaPermissible quando o jogador sai.
     * 
     * Prioridade HIGHEST para garantir que somos os últimos antes de sair.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        AuthorityManager authorityManager = plugin.getAuthorityManager();
        if (authorityManager == null) {
            return;
        }

        // Remover nosso gerenciador de permissões
        authorityManager.ejectPermissible(player);
    }
}
