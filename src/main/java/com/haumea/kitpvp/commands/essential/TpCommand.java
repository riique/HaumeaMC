package com.haumea.kitpvp.commands.essential;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.commands.base.BaseCommand;
import com.haumea.kitpvp.commands.base.CommandInfo;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Comando /tp - Teleporta até um jogador
 * 
 * @author HaumeaMC
 */
@CommandInfo(name = "tp", description = "Teleporta você até um jogador", permission = "haumeamc.tp", playerOnly = true, usage = "/tp <jogador>")
public class TpCommand extends BaseCommand {

    private static final String PREFIX = "§6§lTELEPORT §f";
    private static final String MSG_USAGE = PREFIX
            + "Utilize apenas: /tp <jogador>, o §6§lTELEPORT §fpara coordenadas não está §c§lDISPONIVEL.";
    private static final String MSG_OFFLINE = "§f§lOFFLINE §fO jogador §7(§e%player%§7) §festá offline.";
    private static final String MSG_NO_PERMISSION = "§e§lPERMISSAO §fVocê não possui §c§lPERMISSAO§f para executar este §b§lCOMANDO§f!";

    public TpCommand(HaumeaMC plugin) {
        super(plugin);
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        Player player = getPlayer();

        if (!sender.hasPermission("haumeamc.tp")) {
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

        // Teleportar silenciosamente
        player.teleport(target.getLocation());
    }
}
