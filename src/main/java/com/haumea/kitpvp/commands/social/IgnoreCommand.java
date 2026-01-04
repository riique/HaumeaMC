package com.haumea.kitpvp.commands.social;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.commands.base.BaseCommand;
import com.haumea.kitpvp.commands.base.CommandInfo;
import com.haumea.kitpvp.managers.IgnoreManager;
import com.haumea.kitpvp.utils.ChatStorage;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;

/**
 * Comando /ignore para ignorar jogadores.
 * 
 * Uso:
 * - /ignore <jogador> - Adiciona/remove jogador da lista de ignorados
 * - /ignore list - Lista jogadores ignorados
 * - /ignore clear - Limpa a lista de ignorados
 * 
 * @author HaumeaMC
 */
@CommandInfo(name = "ignore", aliases = { "ignorar", "block", "bloquear" }, permission = "", playerOnly = true)
public class IgnoreCommand extends BaseCommand {

    public IgnoreCommand(HaumeaMC plugin) {
        super(plugin);
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;
        IgnoreManager ignoreManager = plugin.getIgnoreManager();

        if (ignoreManager == null) {
            ChatStorage.send(player, "ignore.error.unavailable");
            return;
        }

        if (args.length == 0) {
            sendUsageMessage(player);
            return;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "list":
            case "lista":
                handleList(player, ignoreManager);
                break;

            case "clear":
            case "limpar":
                ignoreManager.clearIgnoreList(player);
                break;

            default:
                // Assume que é um nome de jogador
                handleToggleIgnore(player, args[0], ignoreManager);
                break;
        }
    }

    /**
     * Lista os jogadores ignorados.
     */
    private void handleList(Player player, IgnoreManager ignoreManager) {
        Set<UUID> ignored = ignoreManager.getIgnoredPlayers(player);

        if (ignored.isEmpty()) {
            ChatStorage.send(player, "ignore.list.empty");
            return;
        }

        ChatStorage.sendRaw(player, "");
        player.sendMessage(ChatStorage.getMessage("ignore.list.header", "count", String.valueOf(ignored.size())));
        ChatStorage.sendRaw(player, "");

        StringBuilder sb = new StringBuilder("  &7");
        int count = 0;
        for (UUID uuid : ignored) {
            // Tentar obter o nome do jogador
            String name = getPlayerName(uuid);
            if (count > 0) {
                sb.append("&8, &7");
            }
            sb.append(name);
            count++;

            // Quebrar linha a cada 5 nomes
            if (count % 5 == 0 && count < ignored.size()) {
                ChatStorage.sendRaw(player, sb.toString());
                sb = new StringBuilder("  &7");
            }
        }

        if (sb.length() > 4) {
            ChatStorage.sendRaw(player, sb.toString());
        }

        ChatStorage.sendRaw(player, "");
        ChatStorage.sendRaw(player, "  &7Use &e/ignore <jogador> &7para des-ignorar.");
        ChatStorage.sendRaw(player, "  &7Use &e/ignore clear &7para limpar a lista.");
        ChatStorage.sendRaw(player, "");
    }

    /**
     * Adiciona ou remove um jogador da lista de ignorados.
     */
    private void handleToggleIgnore(Player player, String targetName, IgnoreManager ignoreManager) {
        Player target = Bukkit.getPlayerExact(targetName);

        if (target == null) {
            // Jogador offline - verificar se já está ignorado pelo nome
            ChatStorage.send(player, "ignore.error.player-offline", "player", targetName);
            return;
        }

        // Verificar se já está ignorando
        if (ignoreManager.isIgnoring(player, target)) {
            ignoreManager.removeIgnore(player, target);
        } else {
            ignoreManager.addIgnore(player, target);
        }
    }

    /**
     * Obtém o nome de um jogador pelo UUID.
     */
    private String getPlayerName(UUID uuid) {
        Player online = Bukkit.getPlayer(uuid);
        if (online != null) {
            return online.getName();
        }

        // Tentar buscar do ProfileManager
        if (plugin.getProfileManager() != null) {
            com.haumea.kitpvp.models.PlayerData data = plugin.getProfileManager().findByName(uuid.toString());
            if (data != null) {
                return data.getLastKnownName();
            }
        }

        // Fallback: primeiros 8 caracteres do UUID
        return uuid.toString().substring(0, 8) + "...";
    }

    /**
     * Envia a mensagem de uso do comando.
     */
    private void sendUsageMessage(Player player) {
        ChatStorage.sendRaw(player, "");
        player.sendMessage(ChatStorage.getMessage("ignore.help.header"));
        ChatStorage.sendRaw(player, "");
        ChatStorage.sendRaw(player, "&e  Uso: &f/ignore <jogador|list|clear>");
        ChatStorage.sendRaw(player, "");
        ChatStorage.sendRaw(player, "&6  Subcomandos:");
        ChatStorage.sendRaw(player, "    &a• <jogador> &8- &7Adiciona/remove da lista");
        ChatStorage.sendRaw(player, "    &e• list &8- &7Lista jogadores ignorados");
        ChatStorage.sendRaw(player, "    &c• clear &8- &7Limpa toda a lista");
        ChatStorage.sendRaw(player, "");
        ChatStorage.sendRaw(player, "&6  Exemplo: &f/ignore Notch");
        ChatStorage.sendRaw(player, "");
    }
}
