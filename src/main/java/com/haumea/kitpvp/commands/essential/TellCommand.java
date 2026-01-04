package com.haumea.kitpvp.commands.essential;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.commands.base.BaseCommand;
import com.haumea.kitpvp.commands.base.CommandInfo;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Comando /tell - Envia mensagem privada
 * 
 * @author HaumeaMC
 */
@CommandInfo(name = "tell", aliases = { "msg", "w", "whisper",
        "pm" }, description = "Envia uma mensagem privada para outro jogador", playerOnly = true, usage = "/tell <jogador> <mensagem>")
public class TellCommand extends BaseCommand {

    private static final String MSG_USAGE = "§6§lTELL §fUtilize: /tell <jogador> <mensagem>";
    private static final String MSG_OFFLINE = "§f§lOFFLINE §fO jogador §7(§e%player%§7) §festá offline.";

    public TellCommand(HaumeaMC plugin) {
        super(plugin);
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        Player player = getPlayer();

        if (args.length < 2) {
            sendRaw(MSG_USAGE);
            return;
        }

        String targetName = args[0];
        Player target = Bukkit.getPlayer(targetName);

        if (target == null || !target.isOnline()) {
            sendRaw(MSG_OFFLINE.replace("%player%", targetName));
            return;
        }

        // Verificar preferência de mensagens privadas do destinatário
        com.haumea.kitpvp.profile.PlayerProfile targetProfile = plugin.getProfileManager().getProfile(target);
        if (targetProfile != null) {
            boolean pmEnabled = targetProfile.getData().getCustomData("pref_private_messages", true);
            if (!pmEnabled && !player.hasPermission("haumea.staff")) {
                sendRaw("§cO jogador §e" + target.getName() + " §cdesativou as mensagens privadas.");
                return;
            }
        }

        // Verificar se o destinatário está ignorando o remetente
        if (plugin.getIgnoreManager() != null) {
            if (plugin.getIgnoreManager().isIgnoring(target, player)) {
                // Silenciosamente não enviar (remetente não sabe que foi ignorado)
                sendRaw("§6§lTELL §fMensagem enviada para §e" + target.getName() + "§f.");
                return;
            }
        }

        // Juntar mensagem
        StringBuilder message = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            message.append(args[i]);
            if (i < args.length - 1) {
                message.append(" ");
            }
        }

        String formattedMessage = ChatColor.translateAlternateColorCodes('&', message.toString());

        // Formato para quem envia
        String senderFormat = "§7(§eEu §f» §e" + target.getName() + "§7) §e" + formattedMessage;
        // Formato para quem recebe
        String targetFormat = "§7(§e" + player.getName() + " §f» §eEu§7) §e" + formattedMessage;

        player.sendMessage(senderFormat);
        target.sendMessage(targetFormat);
    }
}
