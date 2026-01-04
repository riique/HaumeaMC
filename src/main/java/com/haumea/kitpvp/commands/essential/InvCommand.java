package com.haumea.kitpvp.commands.essential;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.commands.base.BaseCommand;
import com.haumea.kitpvp.commands.base.CommandInfo;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Comando /inv - Abre o inventário de outro jogador
 * 
 * @author HaumeaMC
 */
@CommandInfo(name = "inv", aliases = { "invsee",
        "inventario" }, description = "Abre o inventário de outro jogador", permission = "haumeamc.inv", playerOnly = true, usage = "/inv <jogador>")
public class InvCommand extends BaseCommand {

    private static final String PREFIX = "§6§lINVENTARIO §7";
    private static final String MSG_USAGE = PREFIX + "Utilize: /inv <jogador>";
    private static final String MSG_OFFLINE = "§f§lOFFLINE §fO jogador §7(§e%player%§7) §festá offline.";
    private static final String MSG_NO_PERMISSION = "§e§lPERMISSAO §fVocê não possui §c§lPERMISSAO§f para executar este §b§lCOMANDO§f!";

    public InvCommand(HaumeaMC plugin) {
        super(plugin);
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        Player player = getPlayer();

        if (!sender.hasPermission("haumeamc.inv")) {
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

        // Abrir inventário do alvo
        player.openInventory(target.getInventory());
    }
}
