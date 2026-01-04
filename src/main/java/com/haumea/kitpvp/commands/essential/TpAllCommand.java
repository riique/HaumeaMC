package com.haumea.kitpvp.commands.essential;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.commands.base.BaseCommand;
import com.haumea.kitpvp.commands.base.CommandInfo;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Comando /tpall - Teleporta todos os jogadores até você
 * 
 * @author HaumeaMC
 */
@CommandInfo(name = "tpall", description = "Teleporta todos os jogadores até você", permission = "haumeamc.tpall", playerOnly = true)
public class TpAllCommand extends BaseCommand {

    private static final String PREFIX = "§6§lTPALL §f";
    private static final String MSG_SUCCESS = PREFIX + "Todos os jogadores foram §e§lTELEPORTADOS §fpara: §f%sender%";
    private static final String MSG_NO_PERMISSION = "§e§lPERMISSAO §fVocê não possui §c§lPERMISSAO§f para executar este §b§lCOMANDO§f!";

    public TpAllCommand(HaumeaMC plugin) {
        super(plugin);
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        Player player = getPlayer();

        if (!sender.hasPermission("haumeamc.tpall")) {
            sendRaw(MSG_NO_PERMISSION);
            return;
        }

        // Teleportar todos os jogadores para o sender
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.equals(player)) {
                online.teleport(player.getLocation());
            }
        }

        // Enviar mensagem de sucesso
        sendRaw(MSG_SUCCESS.replace("%sender%", player.getName()));
    }
}
