package com.haumea.kitpvp.commands.essential;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.commands.base.BaseCommand;
import com.haumea.kitpvp.commands.base.CommandInfo;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Comando /puxar - Traz um jogador até você
 * 
 * @author HaumeaMC
 */
@CommandInfo(name = "puxar", description = "Traz um jogador até você", permission = "haumeamc.puxar", playerOnly = true, usage = "/puxar <jogador>")
public class PuxarCommand extends BaseCommand {

    private static final String PREFIX = "§6§lPUXAR §f";
    private static final String MSG_USAGE = PREFIX + "Utilize: /puxar <jogador>";
    private static final String MSG_SUCCESS = "§6§lPUXAR §cVocê puxou o(a) jogador(a) §7(§f%target%§7) §caté você.";
    private static final String MSG_OFFLINE = "§f§lOFFLINE §fO jogador §7(§e%player%§7) §festá offline.";
    private static final String MSG_NO_PERMISSION = "§e§lPERMISSAO §fVocê não possui §c§lPERMISSAO§f para executar este §b§lCOMANDO§f!";

    public PuxarCommand(HaumeaMC plugin) {
        super(plugin);
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        Player player = getPlayer();

        if (!sender.hasPermission("haumeamc.puxar")) {
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

        // Teleportar o alvo até você
        target.teleport(player.getLocation());
        sendRaw(MSG_SUCCESS.replace("%target%", target.getName()));
    }
}
