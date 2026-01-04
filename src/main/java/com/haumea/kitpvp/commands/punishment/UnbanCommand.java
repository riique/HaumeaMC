package com.haumea.kitpvp.commands.punishment;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.commands.base.BaseCommand;
import com.haumea.kitpvp.commands.base.CommandInfo;
import com.haumea.kitpvp.managers.PunishmentManager;
import com.haumea.kitpvp.models.Punishment;
import com.haumea.kitpvp.utils.ChatStorage;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Comando de unban do HaumeaMC
 * 
 * Uso: /unban <nick>
 * - Apenas ADMIN ou superior pode desbanir
 * 
 * @author HaumeaMC
 */
@CommandInfo(name = "unban", aliases = { "desban", "desbanir",
        "pardon" }, description = "Remove o banimento de um jogador", usage = "/unban <nick>", allowedGroups = { "dono",
                "diretor", "gerente", "admin" })
public class UnbanCommand extends BaseCommand {

    public UnbanCommand(HaumeaMC plugin) {
        super(plugin);
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sendUsageMessage(sender);
            return;
        }

        String targetName = args[0];

        PunishmentManager punishmentManager = plugin.getPunishmentManager();
        Player staffPlayer = sender instanceof Player ? (Player) sender : null;

        // Buscar jogador offline
        @SuppressWarnings("deprecation")
        OfflinePlayer targetOffline = Bukkit.getOfflinePlayer(targetName);

        if (targetOffline == null || !targetOffline.hasPlayedBefore()) {
            ChatStorage.send(sender, "punishment.common.unknown-player", "player", targetName);
            return;
        }

        java.util.UUID targetUuid = targetOffline.getUniqueId();
        String playerName = targetOffline.getName();

        // Verificar se está banido
        Punishment ban = punishmentManager.getActiveBan(targetUuid);
        if (ban == null) {
            ChatStorage.send(sender, "punishment.unban.not-banned", "player", playerName);
            return;
        }

        // Desbanir
        boolean success = punishmentManager.unban(targetUuid);

        if (success) {
            String staffName = staffPlayer != null ? staffPlayer.getName() : "Console";

            // Broadcast para staff
            broadcastUnban(playerName, targetUuid.toString(), staffName);

            // Log
            plugin.getLogger().info("[UNBAN] " + playerName + " foi desbanido por " + staffName);
        } else {
            ChatStorage.sendRaw(sender, "&c&lUNBAN &fOcorreu um erro ao desbanir o jogador!");
        }
    }

    /**
     * Broadcast do unban para o servidor
     */
    private void broadcastUnban(String player, String uuid, String staff) {
        String header = ChatStorage.getMessage("punishment.unban.broadcast.header");
        String msg = ChatStorage.getMessage("punishment.unban.broadcast.msg",
                "player", player, "uuid", uuid.substring(0, 8), "staff", staff);
        String footer = ChatStorage.getMessage("punishment.unban.broadcast.footer");

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage("");
            p.sendMessage(header);
            p.sendMessage("");
            p.sendMessage(msg);
            p.sendMessage("");
            p.sendMessage(ChatStorage.colorize("&7O jogador agora pode entrar no servidor."));
            p.sendMessage("");
            p.sendMessage(footer);
            p.sendMessage("");
        }

        // Console também
        Bukkit.getConsoleSender().sendMessage("");
        Bukkit.getConsoleSender().sendMessage(header);
        Bukkit.getConsoleSender().sendMessage("");
        Bukkit.getConsoleSender().sendMessage(msg);
        Bukkit.getConsoleSender().sendMessage("");
        Bukkit.getConsoleSender().sendMessage(ChatStorage.colorize("&7O jogador agora pode entrar no servidor."));
        Bukkit.getConsoleSender().sendMessage("");
        Bukkit.getConsoleSender().sendMessage(footer);
        Bukkit.getConsoleSender().sendMessage("");
    }

    /**
     * Envia mensagem de uso do comando
     */
    private void sendUsageMessage(CommandSender sender) {
        ChatStorage.sendRaw(sender, "");
        sender.sendMessage(ChatStorage.getMessage("punishment.unban.help.title"));
        ChatStorage.sendRaw(sender, "");
        sender.sendMessage(ChatStorage.getMessage("punishment.unban.help.usage"));
        ChatStorage.sendRaw(sender, "");
        sender.sendMessage(ChatStorage.getMessage("punishment.unban.help.args-header"));
        ChatStorage.sendRaw(sender, "    &7• &fnick &8- &7Nome do jogador a ser desbanido");
        ChatStorage.sendRaw(sender, "");
        sender.sendMessage(ChatStorage.getMessage("punishment.unban.help.examples-header"));
        ChatStorage.sendRaw(sender, "    &f/unban Steve");
        ChatStorage.sendRaw(sender, "");
    }
}
