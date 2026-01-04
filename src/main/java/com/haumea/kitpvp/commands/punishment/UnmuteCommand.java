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
 * Comando de unmute do HaumeaMC
 * 
 * Uso: /unmute <nick>
 * - Apenas ADMIN ou superior pode desmutar
 * 
 * @author HaumeaMC
 */
@CommandInfo(name = "unmute", aliases = { "desmute",
        "desmutar" }, description = "Remove o silenciamento de um jogador", usage = "/unmute <nick>", allowedGroups = {
                "dono", "diretor", "gerente", "admin" })
public class UnmuteCommand extends BaseCommand {

    public UnmuteCommand(HaumeaMC plugin) {
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

        // Verificar se está mutado
        Punishment mute = punishmentManager.getActiveMute(targetUuid);
        if (mute == null) {
            ChatStorage.send(sender, "punishment.unmute.not-muted", "player", playerName);
            return;
        }

        // Desmutar
        boolean success = punishmentManager.unmute(targetUuid);

        if (success) {
            String staffName = staffPlayer != null ? staffPlayer.getName() : "Console";

            // Broadcast para staff
            broadcastUnmute(playerName, targetUuid.toString(), staffName);

            // Notificar o jogador se online
            Player targetOnline = Bukkit.getPlayer(targetUuid);
            if (targetOnline != null && targetOnline.isOnline()) {
                notifyPlayer(targetOnline);
            }

            // Log
            plugin.getLogger().info("[UNMUTE] " + playerName + " foi desmutado por " + staffName);
        } else {
            ChatStorage.sendRaw(sender, "&a&lUNMUTE &fOcorreu um erro ao desmutar o jogador!");
        }
    }

    /**
     * Broadcast do unmute para o servidor
     */
    private void broadcastUnmute(String player, String uuid, String staff) {
        String header = ChatStorage.getMessage("punishment.unmute.broadcast.header");
        String msg = ChatStorage.getMessage("punishment.unmute.broadcast.msg",
                "player", player, "uuid", uuid.substring(0, 8), "staff", staff);
        String footer = ChatStorage.getMessage("punishment.unmute.broadcast.footer");

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage("");
            p.sendMessage(header);
            p.sendMessage("");
            p.sendMessage(msg);
            p.sendMessage("");
            p.sendMessage(ChatStorage.colorize("&7O jogador agora pode falar no chat."));
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
        Bukkit.getConsoleSender().sendMessage(ChatStorage.colorize("&7O jogador agora pode falar no chat."));
        Bukkit.getConsoleSender().sendMessage("");
        Bukkit.getConsoleSender().sendMessage(footer);
        Bukkit.getConsoleSender().sendMessage("");
    }

    /**
     * Notifica o jogador que foi desmutado
     */
    private void notifyPlayer(Player player) {
        ChatStorage.send(player, "punishment.unmuted");
    }

    /**
     * Envia mensagem de uso do comando
     */
    private void sendUsageMessage(CommandSender sender) {
        ChatStorage.sendRaw(sender, "");
        sender.sendMessage(ChatStorage.getMessage("punishment.unmute.help.title"));
        ChatStorage.sendRaw(sender, "");
        sender.sendMessage(ChatStorage.getMessage("punishment.unmute.help.usage"));
        ChatStorage.sendRaw(sender, "");
        sender.sendMessage(ChatStorage.getMessage("punishment.unmute.help.args-header"));
        ChatStorage.sendRaw(sender, "    &7• &fnick &8- &7Nome do jogador a ser desmutado");
        ChatStorage.sendRaw(sender, "");
        sender.sendMessage(ChatStorage.getMessage("punishment.unmute.help.examples-header"));
        ChatStorage.sendRaw(sender, "    &f/unmute Steve");
        ChatStorage.sendRaw(sender, "");
    }
}
