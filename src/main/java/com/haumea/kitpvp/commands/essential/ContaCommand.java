package com.haumea.kitpvp.commands.essential;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.commands.base.BaseCommand;
import com.haumea.kitpvp.commands.base.CommandInfo;
import com.haumea.kitpvp.managers.StatsManager;
import com.haumea.kitpvp.models.Group;
import com.haumea.kitpvp.models.PlayerRank;
import com.haumea.kitpvp.utils.ChatStorage;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Comando /conta - Mostra estatísticas do jogador
 * 
 * @author HaumeaMC
 */
@CommandInfo(name = "conta", aliases = { "stats", "estatisticas",
        "account" }, description = "Mostra estatísticas do jogador", playerOnly = true, usage = "/conta [jogador]")
public class ContaCommand extends BaseCommand {

    private static final String MSG_OFFLINE = "§f§lOFFLINE §fO jogador §7(§e%player%§7) §festá offline.";
    private static final String PREFIX = "§e§lCONTA §f";

    public ContaCommand(HaumeaMC plugin) {
        super(plugin);
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        Player player = getPlayer();
        Player target;

        if (args.length == 0) {
            // Ver própria conta
            target = player;
        } else {
            // Ver conta de outro jogador
            String targetName = args[0];
            target = Bukkit.getPlayer(targetName);

            if (target == null || !target.isOnline()) {
                sendRaw(MSG_OFFLINE.replace("%player%", targetName));
                return;
            }
        }

        // Obter dados do jogador
        StatsManager statsManager = plugin.getStatsManager();

        int kills = statsManager.getKills(target);
        int deaths = statsManager.getDeaths(target);
        int killstreak = statsManager.getKillstreak(target);
        int elo = statsManager.getElo(target);
        long coins = statsManager.getMoney(target);

        // Obter grupo
        Group group = groupManager.getPlayerGroup(target);
        String groupName = group != null ? group.getPrefix().replace("&", "§") : "§7Membro";

        // Obter rank/liga
        PlayerRank rank = statsManager.getRank(target);
        String leagueName = rank != null ? rank.getFormattedName() : "§8Unranked";

        // Exibir informações
        sendRaw("");
        if (target.equals(player)) {
            sendRaw("§f§l-= §6§lSUA CONTA §f§l=-");
        } else {
            sendRaw("§f§l-= §6§lCONTA DE " + target.getName().toUpperCase() + " §f§l=-");
        }
        sendRaw("");
        sendRaw(PREFIX + "KILLS: §7(§e" + ChatStorage.formatNumber(kills) + "§7)");
        sendRaw(PREFIX + "DEATHS: §7(§e" + ChatStorage.formatNumber(deaths) + "§7)");
        sendRaw(PREFIX + "KILLSTREAK: §7(§e" + killstreak + "§7)");
        sendRaw(PREFIX + "ELO: §7(§e" + ChatStorage.formatNumber(elo) + "§7)");
        sendRaw(PREFIX + "MOEDAS: §7(§e" + ChatStorage.formatNumber(coins) + "§7)");
        sendRaw(PREFIX + "GRUPO: §7" + groupName);
        sendRaw(PREFIX + "LIGA: §7" + leagueName);
        sendRaw("");
    }
}
