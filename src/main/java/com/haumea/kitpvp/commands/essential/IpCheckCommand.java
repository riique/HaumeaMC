package com.haumea.kitpvp.commands.essential;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.commands.base.BaseCommand;
import com.haumea.kitpvp.commands.base.CommandInfo;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Comando /ipcheck - Mostra o IP de um jogador
 * 
 * @author HaumeaMC
 */
@CommandInfo(name = "ipcheck", aliases = {
        "ip" }, description = "Mostra o IP de um jogador", permission = "haumeamc.ipcheck", usage = "/ipcheck <jogador>")
public class IpCheckCommand extends BaseCommand {

    private static final String PREFIX = "§6§lIPCHECK §f";
    private static final String MSG_USAGE = PREFIX + "Utilize: /ipcheck <jogador>";
    private static final String MSG_OFFLINE = "§f§lOFFLINE §fO jogador §7(§e%player%§7) §festá offline.";
    private static final String MSG_NO_PERMISSION = "§e§lPERMISSAO §fVocê não possui §c§lPERMISSAO§f para executar este §b§lCOMANDO§f!";

    public IpCheckCommand(HaumeaMC plugin) {
        super(plugin);
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("haumeamc.ipcheck")) {
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

        // Obter IP do jogador
        String ip = target.getAddress().getHostString();

        sendRaw(PREFIX + "Jogador: §7(§e" + target.getName() + "§7)");
        sendRaw(PREFIX + "IP: §7(§e" + ip + "§7)");
    }
}
