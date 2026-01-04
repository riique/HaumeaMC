package com.haumea.kitpvp.commands.essential;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.commands.base.BaseCommand;
import com.haumea.kitpvp.commands.base.CommandInfo;
import com.haumea.kitpvp.managers.GroupManager;
import com.haumea.kitpvp.models.Group;
import com.haumea.kitpvp.utils.ChatStorage;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Comando /staff - Mostra a lista de staffs online
 * 
 * Exibe todos os membros da equipe que estão online,
 * organizados por cargo/prioridade.
 * 
 * @author HaumeaMC
 */
@CommandInfo(name = "staff", aliases = { "equipe", "staffs",
        "team" }, description = "Mostra os membros da equipe online", playerOnly = false, usage = "/staff")
public class StaffCommand extends BaseCommand {

    // Definição dos cargos de staff com seus prefixos e cores
    private static final String[] STAFF_GROUPS = {
            "dono", "diretor", "gerente", "admin", "mod", "helper"
    };

    public StaffCommand(HaumeaMC plugin) {
        super(plugin);
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        GroupManager groupManager = plugin.getGroupManager();

        // Coletar staffs online organizados por grupo
        Map<String, List<Player>> staffByGroup = new LinkedHashMap<>();

        // Inicializar grupos na ordem de prioridade
        for (String groupName : STAFF_GROUPS) {
            staffByGroup.put(groupName.toLowerCase(), new ArrayList<Player>());
        }

        // Coletar jogadores online que são staff
        int totalStaff = 0;
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (groupManager.isStaff(online)) {
                Group playerGroup = groupManager.getPlayerGroup(online);
                if (playerGroup != null) {
                    String groupName = playerGroup.getName().toLowerCase();
                    if (staffByGroup.containsKey(groupName)) {
                        staffByGroup.get(groupName).add(online);
                        totalStaff++;
                    }
                }
            }
        }

        // Se não há staffs online
        if (totalStaff == 0) {
            sendStaffHeader(sender);
            ChatStorage.sendRaw(sender, "");
            ChatStorage.sendRaw(sender, ChatStorage.getMessage("stafflist.no-staff"));
            ChatStorage.sendRaw(sender, "");
            sendStaffFooter(sender);
            return;
        }

        // Enviar header
        sendStaffHeader(sender);
        ChatStorage.sendRaw(sender, "");

        // Enviar contagem total
        String totalMsg = ChatStorage.getMessage("stafflist.total")
                .replace("{count}", String.valueOf(totalStaff))
                .replace("{online}", String.valueOf(Bukkit.getOnlinePlayers().size()));
        ChatStorage.sendRaw(sender, totalMsg);
        ChatStorage.sendRaw(sender, "");

        // Listar staffs por grupo
        boolean foundAny = false;
        for (String groupName : STAFF_GROUPS) {
            List<Player> staffs = staffByGroup.get(groupName.toLowerCase());
            if (staffs != null && !staffs.isEmpty()) {
                foundAny = true;

                // Obter informações do grupo
                Group group = groupManager.getGroup(groupName);
                String groupDisplay = group != null ? ChatStorage.colorize(group.getDisplayName()) : groupName;
                String groupPrefix = group != null ? ChatStorage.colorize(group.getPrefix()) : "";

                // Header do grupo
                String groupHeader = ChatStorage.getMessage("stafflist.group-header")
                        .replace("{group}", groupDisplay)
                        .replace("{count}", String.valueOf(staffs.size()));
                ChatStorage.sendRaw(sender, groupHeader);

                // Listar jogadores do grupo
                for (Player staff : staffs) {
                    String playerEntry = ChatStorage.getMessage("stafflist.player-entry")
                            .replace("{prefix}", groupPrefix)
                            .replace("{player}", staff.getName());
                    ChatStorage.sendRaw(sender, playerEntry);
                }

                ChatStorage.sendRaw(sender, "");
            }
        }

        // Footer
        sendStaffFooter(sender);
    }

    /**
     * Envia o header da lista de staffs
     */
    private void sendStaffHeader(CommandSender sender) {
        List<String> headerLines = ChatStorage.getMessageList("stafflist.header");
        for (String line : headerLines) {
            ChatStorage.sendRaw(sender, line);
        }
    }

    /**
     * Envia o footer da lista de staffs
     */
    private void sendStaffFooter(CommandSender sender) {
        List<String> footerLines = ChatStorage.getMessageList("stafflist.footer");
        for (String line : footerLines) {
            ChatStorage.sendRaw(sender, line);
        }
    }
}
