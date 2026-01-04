package com.haumea.kitpvp.commands.staff;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.commands.base.BaseCommand;
import com.haumea.kitpvp.commands.base.CommandInfo;
import com.haumea.kitpvp.permissions.AuthorityManager;
import com.haumea.kitpvp.permissions.HaumeaPermissible;
import com.haumea.kitpvp.utils.ChatStorage;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Set;

/**
 * Comando de debug para o sistema de autoridade.
 * 
 * Uso:
 * - /authority debug - Mostra informações do sistema
 * - /authority check <jogador> - Verifica permissões de um jogador
 * - /authority refresh <jogador> - Força atualização de permissões
 * - /authority refresh all - Força atualização de todos
 * 
 * @author HaumeaMC
 */
@CommandInfo(name = "authority", aliases = { "auth",
        "authoritydebug" }, description = "Gerencia o sistema de autoridade de permissões", usage = "/authority <debug|check|refresh> [jogador|all]", permission = "haumea.admin.authority", allowedGroups = {
                "dono", "diretor" })
public class AuthorityCommand extends BaseCommand {

    public AuthorityCommand(HaumeaMC plugin) {
        super(plugin);
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        AuthorityManager authorityManager = plugin.getAuthorityManager();

        if (authorityManager == null) {
            ChatStorage.send(sender, "authority.error.unavailable");
            return;
        }

        if (args.length == 0) {
            showUsage(sender);
            return;
        }

        String subcommand = args[0].toLowerCase();

        switch (subcommand) {
            case "debug":
                showDebug(sender, authorityManager);
                break;

            case "check":
                if (args.length < 2) {
                    ChatStorage.send(sender, "authority.error.check-usage");
                    return;
                }
                checkPlayer(sender, authorityManager, args);
                break;

            case "refresh":
                if (args.length < 2) {
                    ChatStorage.send(sender, "authority.error.refresh-usage");
                    return;
                }
                refreshPermissions(sender, authorityManager, args[1]);
                break;

            default:
                showUsage(sender);
        }
    }

    /**
     * Mostra informações de debug do sistema
     */
    private void showDebug(CommandSender sender, AuthorityManager authorityManager) {
        ChatStorage.send(sender, "authority.debug-header");
        ChatStorage.sendRaw(sender, "");

        // Status da reflexão
        String status = authorityManager.isReflectionWorking() ? "&aSIM" : "&cNÃO";
        ChatStorage.send(sender, "authority.debug-reflection", "status", status);

        // Jogadores injetados
        int injectedCount = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (authorityManager.hasInjectedPermissible(player)) {
                injectedCount++;
            }
        }
        ChatStorage.send(sender, "authority.debug-players", "count", String.valueOf(injectedCount));

        ChatStorage.sendRaw(sender, "");
        sender.sendMessage(ChatStorage.getMessage("authority.debug-permissible-list"));

        // Lista de jogadores
        for (Player player : Bukkit.getOnlinePlayers()) {
            HaumeaPermissible perm = authorityManager.getPermissible(player);
            if (perm != null) {
                Set<String> perms = perm.getCachedPermissions();
                ChatStorage.send(sender, "authority.debug-cache",
                        "player", player.getName(),
                        "perms", String.valueOf(perms.size()));
            } else {
                ChatStorage.send(sender, "authority.debug-not-injected", "player", player.getName());
            }
        }

        ChatStorage.sendRaw(sender, "");
        ChatStorage.sendRaw(sender, "&8&m====================================");
    }

    /**
     * Verifica permissões de um jogador específico
     */
    private void checkPlayer(CommandSender sender, AuthorityManager authorityManager, String[] args) {
        String playerName = args[1];
        Player target = Bukkit.getPlayer(playerName);

        if (target == null) {
            ChatStorage.send(sender, "authority.error.player-offline", "player", playerName);
            return;
        }

        HaumeaPermissible perm = authorityManager.getPermissible(target);

        if (perm == null) {
            ChatStorage.send(sender, "authority.error.not-injected", "player", playerName);
            return;
        }

        ChatStorage.sendRaw(sender, "&8&m=====&r &6&lPERMISSÕES DE " + target.getName().toUpperCase() + " &8&m=====");
        ChatStorage.sendRaw(sender, "");

        // Se especificou uma permissão para verificar
        if (args.length >= 3) {
            String permission = args[2];
            boolean has = perm.hasPermission(permission);
            String result = has ? "&aSIM" : "&cNÃO";
            ChatStorage.sendRaw(sender, "&fPermissão &e" + permission + "&f: " + result);
        } else {
            // Mostrar todas as permissões em cache
            Set<String> perms = perm.getCachedPermissions();
            ChatStorage.sendRaw(sender, "&fTotal de permissões em cache: &e" + perms.size());
            ChatStorage.sendRaw(sender, "");

            // Mostrar as 20 primeiras
            int count = 0;
            for (String p : perms) {
                if (count >= 20) {
                    ChatStorage.sendRaw(sender, "&7... e mais " + (perms.size() - 20) + " permissões");
                    break;
                }
                ChatStorage.sendRaw(sender, "&7- " + p);
                count++;
            }
        }

        ChatStorage.sendRaw(sender, "");
        ChatStorage.sendRaw(sender, "&fÉ OP real? " + (target.getServer().getOperators().stream()
                .anyMatch(op -> op.getUniqueId().equals(target.getUniqueId())) ? "&aSIM" : "&cNÃO"));
        ChatStorage.sendRaw(sender,
                "&fÉ OP via HaumeaPermissible? " + (perm.isOp() ? "&aSIM" : "&cNÃO &7(sempre falso)"));
        ChatStorage.sendRaw(sender, "");
        ChatStorage.sendRaw(sender, "&8&m====================================");
    }

    /**
     * Força atualização de permissões
     */
    private void refreshPermissions(CommandSender sender, AuthorityManager authorityManager, String target) {
        if (target.equalsIgnoreCase("all")) {
            // Atualizar todos
            authorityManager.updateAllPermissions();
            ChatStorage.send(sender, "authority.refresh.all");
            return;
        }

        // Atualizar jogador específico
        Player player = Bukkit.getPlayer(target);
        if (player == null) {
            ChatStorage.send(sender, "authority.error.player-offline", "player", target);
            return;
        }

        authorityManager.updatePermissions(player);
        ChatStorage.send(sender, "authority.refresh.player", "player", player.getName());
    }

    /**
     * Mostra o uso do comando
     */
    private void showUsage(CommandSender sender) {
        ChatStorage.sendRaw(sender, "&8&m=====&r &6&lAUTHORITY COMMAND &8&m=====");
        ChatStorage.sendRaw(sender, "");
        ChatStorage.sendRaw(sender, "&e/authority debug &7- Mostra informações do sistema");
        ChatStorage.sendRaw(sender, "&e/authority check <jogador> [permissão] &7- Verifica permissões");
        ChatStorage.sendRaw(sender, "&e/authority refresh <jogador|all> &7- Força atualização");
        ChatStorage.sendRaw(sender, "");
        ChatStorage.sendRaw(sender, "&8&m====================================");
    }
}
