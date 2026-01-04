package com.haumea.kitpvp.managers;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.models.Group;
import com.haumea.kitpvp.permissions.AuthorityManager;
import com.haumea.kitpvp.permissions.HaumeaPermissible;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Gerenciador de Permissões
 * 
 * Responsável por aplicar e remover permissões dos jogadores em tempo real.
 * Integra com o AuthorityManager para atualização instantânea de cache.
 * 
 * SISTEMA DE SEGURANÇA:
 * - O AuthorityManager "sequestra" o gerenciador de permissões do Bukkit
 * - Este PermissionManager funciona como camada adicional de compatibilidade
 * - Quando permissões mudam, notifica o AuthorityManager para atualizar cache
 * 
 * @author HaumeaMC
 */
public class PermissionManager {

    private final HaumeaMC plugin;
    private final GroupManager groupManager;

    // Map de attachments de permissão (UUID -> PermissionAttachment)
    // Mantido para compatibilidade com plugins que usam o sistema padrão
    private final Map<UUID, PermissionAttachment> attachments;

    public PermissionManager(HaumeaMC plugin) {
        this.plugin = plugin;
        this.groupManager = plugin.getGroupManager();
        this.attachments = new ConcurrentHashMap<>();
    }

    /**
     * Aplica as permissões do grupo ao jogador.
     * 
     * Este método:
     * 1. Limpa permissões antigas
     * 2. Aplica novas permissões via PermissionAttachment (compatibilidade)
     * 3. Notifica o AuthorityManager para atualizar cache
     */
    public void setupPermissions(Player player) {
        // Remover permissões antigas
        removePermissions(player);

        // Obter grupo do jogador
        Group group = groupManager.getPlayerGroup(player);
        if (group == null)
            return;

        // Criar novo attachment
        PermissionAttachment attachment = player.addAttachment(plugin);
        attachments.put(player.getUniqueId(), attachment);

        // Obter todas as permissões (incluindo herança)
        Set<String> permissions = groupManager.getAllPermissions(group);

        // Aplicar permissões
        for (String permission : permissions) {
            if (permission.startsWith("-")) {
                // Permissão negada
                attachment.setPermission(permission.substring(1), false);
            } else {
                attachment.setPermission(permission, true);
            }
        }

        // Recalcular permissões
        player.recalculatePermissions();

        // Notificar o AuthorityManager para atualizar cache
        notifyAuthorityManager(player);
    }

    /**
     * Remove as permissões do jogador
     */
    public void removePermissions(Player player) {
        PermissionAttachment attachment = attachments.remove(player.getUniqueId());
        if (attachment != null) {
            player.removeAttachment(attachment);
        }
    }

    /**
     * Atualiza as permissões de um jogador.
     * 
     * Chamado quando:
     * - O cargo do jogador muda
     * - Um VIP expira
     * - O administrador força atualização
     */
    public void updatePermissions(Player player) {
        setupPermissions(player);
    }

    /**
     * Atualiza as permissões de todos os jogadores online.
     * 
     * Útil após recarregar configurações de grupos.
     */
    public void updateAllPlayers() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            setupPermissions(player);
        }
    }

    /**
     * Verifica se o jogador tem uma permissão através do grupo.
     * 
     * Este método consulta diretamente o GroupManager, sem passar
     * pelo sistema de cache do AuthorityManager.
     */
    public boolean hasGroupPermission(Player player, String permission) {
        return groupManager.hasPermission(player, permission);
    }

    /**
     * Notifica o AuthorityManager para atualizar o cache de permissões.
     * 
     * Chamado após qualquer mudança de permissões para garantir
     * atualização instantânea.
     */
    private void notifyAuthorityManager(Player player) {
        AuthorityManager authorityManager = plugin.getAuthorityManager();
        if (authorityManager != null) {
            // Verificar se o jogador tem nosso permissible injetado
            HaumeaPermissible permissible = authorityManager.getPermissible(player);
            if (permissible != null) {
                // Forçar atualização do cache
                permissible.forceRefresh();
            }
        }
    }

    /**
     * Força atualização instantânea de permissões de um jogador.
     * 
     * Usado quando há mudança de cargo ou expiração de VIP.
     * Garante que o jogador não precise sair e entrar de novo.
     */
    public void forceUpdate(Player player) {
        // Atualizar permissões via attachment
        setupPermissions(player);

        // Notificar AuthorityManager
        notifyAuthorityManager(player);
    }

    /**
     * Força atualização de permissões por UUID.
     * 
     * @param uuid UUID do jogador
     */
    public void forceUpdate(UUID uuid) {
        Player player = plugin.getServer().getPlayer(uuid);
        if (player != null && player.isOnline()) {
            forceUpdate(player);
        }
    }
}
