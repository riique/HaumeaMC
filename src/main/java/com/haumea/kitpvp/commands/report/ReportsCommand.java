package com.haumea.kitpvp.commands.report;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.commands.base.BaseCommand;
import com.haumea.kitpvp.commands.base.CommandInfo;
import com.haumea.kitpvp.utils.ChatStorage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Comando para abrir o menu de reports (apenas staff)
 * 
 * Uso: /reports ou /denuncias
 * 
 * @author HaumeaMC
 */
@CommandInfo(name = "reports", aliases = {
        "denuncias" }, description = "Abre o menu de reports", usage = "/reports", playerOnly = true)
public class ReportsCommand extends BaseCommand {

    public ReportsCommand(HaumeaMC plugin) {
        super(plugin);
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        Player player = getPlayer();

        if (!plugin.getReportManager().isStaff(player)) {
            ChatStorage.send(player, "report.list.no-permission");
            return;
        }

        // Abrir menu GUI
        ReportMenuGUI.openMenu(plugin, player, 1);
    }
}
