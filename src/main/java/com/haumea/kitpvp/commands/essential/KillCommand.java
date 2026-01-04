package com.haumea.kitpvp.commands.essential;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.commands.base.BaseCommand;
import com.haumea.kitpvp.commands.base.CommandInfo;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Comando /kill - Mata um jogador instantaneamente
 * 
 * @author HaumeaMC
 */
@CommandInfo(name = "kill", description = "Mata um jogador instantaneamente", permission = "haumeamc.kill", usage = "/kill <jogador>")
public class KillCommand extends BaseCommand {

    private static final String PREFIX = "§6§lKILL §f";
    private static final String MSG_USAGE = PREFIX + "Utilize: /kill <jogador>";
    private static final String MSG_SUCCESS = PREFIX + "Você matou o jogador §e%target%§f.";
    private static final String MSG_OFFLINE = "§f§lOFFLINE §fO jogador §7(§e%player%§7) §festá offline.";
    private static final String MSG_NO_PERMISSION = "§e§lPERMISSAO §fVocê não possui §c§lPERMISSAO§f para executar este §b§lCOMANDO§f!";

    public KillCommand(HaumeaMC plugin) {
        super(plugin);
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("haumeamc.kill")) {
            sendRaw(MSG_NO_PERMISSION);
            return;
        }

        if (args.length < 1) {
            sendRaw(MSG_USAGE);
            return;
        }

        String targetName = args[0];
        Player target = Bukkit.getPlayer(targetName);

        if (target == null || !target.isOnline()) {
            sendRaw(MSG_OFFLINE.replace("%player%", targetName));
            return;
        }

        // Matar o jogador
        target.setHealth(0);
        sendRaw(MSG_SUCCESS.replace("%target%", target.getName()));
    }
}
