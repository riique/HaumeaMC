package com.haumea.kitpvp.commands.report;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.commands.base.BaseCommand;
import com.haumea.kitpvp.commands.base.CommandInfo;
import com.haumea.kitpvp.managers.ReportManager;
import com.haumea.kitpvp.models.Report;
import com.haumea.kitpvp.utils.ChatStorage;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Comando de report (denúncia) do HaumeaMC
 * 
 * Uso: /report <jogador> <motivo>
 * Uso Staff: /report list - Abre o menu de reports
 * 
 * @author HaumeaMC
 */
@CommandInfo(name = "report", aliases = { "reportar",
        "denunciar" }, description = "Denuncia um jogador à equipe", usage = "/report <jogador> <motivo>", playerOnly = true)
public class ReportCommand extends BaseCommand {

    public ReportCommand(HaumeaMC plugin) {
        super(plugin);
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        Player player = getPlayer();
        ReportManager reportManager = plugin.getReportManager();

        // /report list - Abre menu (apenas staff)
        if (args.length >= 1 && args[0].equalsIgnoreCase("list")) {
            if (!reportManager.isStaff(player)) {
                ChatStorage.send(player, "report.list.no-permission");
                return;
            }

            // Abrir menu GUI
            ReportMenuGUI.openMenu(plugin, player, 1);
            return;
        }

        // /report clear - Limpa reports concluídos (apenas staff)
        if (args.length >= 1 && args[0].equalsIgnoreCase("clear")) {
            if (!reportManager.isStaff(player)) {
                ChatStorage.send(player, "report.list.clear-no-permission");
                return;
            }

            int removed = reportManager.clearCompletedReports();
            ChatStorage.send(player, "report.list.cleared", "count", String.valueOf(removed));
            return;
        }

        // /report - Sem argumentos
        if (args.length < 2) {
            sendUsageMessage(player);

            // Mostrar quantidade de reports para staff
            if (reportManager.isStaff(player)) {
                int openReports = reportManager.countOpenReports();
                ChatStorage.sendRaw(player, "");
                ChatStorage.send(player, "report.list.header", "count", String.valueOf(openReports));
            }
            return;
        }

        // Verificar cooldown
        if (reportManager.isOnCooldown(player.getUniqueId())) {
            long remaining = reportManager.getCooldownRemaining(player.getUniqueId());
            ChatStorage.send(player, "report.cooldown", "time", String.valueOf(remaining));
            return;
        }

        String targetName = args[0];

        // Construir motivo (pode ter múltiplas palavras)
        StringBuilder reasonBuilder = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            reasonBuilder.append(args[i]);
            if (i < args.length - 1) {
                reasonBuilder.append(" ");
            }
        }
        String reason = reasonBuilder.toString();

        // Buscar jogador alvo
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            ChatStorage.send(player, "report.error.offline", "player", targetName);
            return;
        }

        // Não pode reportar a si mesmo
        if (target.equals(player)) {
            ChatStorage.send(player, "report.error.self");
            return;
        }

        // Criar report
        Report report = reportManager.createReport(player, target, reason);

        if (report != null) {
            // Feedback para quem reportou
            ChatStorage.sendRaw(player, "");
            ChatStorage.send(player, "report.success.header", "target", target.getName());
            ChatStorage.send(player, "report.success.footer");
            ChatStorage.sendRaw(player, "");
        } else {
            ChatStorage.send(player, "report.error.generic");
        }
    }

    /**
     * Envia mensagem de uso do comando
     */
    private void sendUsageMessage(Player player) {
        ChatStorage.sendRaw(player, "");
        player.sendMessage(ChatStorage.getMessage("report.usage.header"));
        ChatStorage.sendRaw(player, "");
        player.sendMessage(ChatStorage.getMessage("report.usage.usage"));
        ChatStorage.sendRaw(player, "");

        for (String line : ChatStorage.getMessageList("report.usage.args")) {
            ChatStorage.sendRaw(player, line);
        }

        ChatStorage.sendRaw(player, "");

        for (String line : ChatStorage.getMessageList("report.usage.examples")) {
            ChatStorage.sendRaw(player, line);
        }

        ChatStorage.sendRaw(player, "");
        player.sendMessage(ChatStorage.getMessage("report.usage.footer"));
        ChatStorage.sendRaw(player, "");
    }
}
