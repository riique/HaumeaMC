package com.haumea.kitpvp.commands.essential;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.commands.base.BaseCommand;
import com.haumea.kitpvp.commands.base.CommandInfo;
import com.haumea.kitpvp.managers.RankingManager;
import com.haumea.kitpvp.utils.ChatStorage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Comando /top - Rankings globais
 * 
 * Uso:
 * /top kills - Top kills
 * /top elo - Top ELO
 * /top coins - Top coins (riqueza)
 * /top kdr - Top KDR
 * /top killstreak - Top killstreak
 * 
 * @author HaumeaMC
 */
@CommandInfo(name = "top", aliases = { "ranking", "leaderboard" }, permission = "", playerOnly = false)
public class TopCommand extends BaseCommand {

    public TopCommand(HaumeaMC plugin) {
        super(plugin);
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        RankingManager rankingManager = plugin.getRankingManager();

        if (rankingManager == null) {
            ChatStorage.send(sender, "ranking.error.unavailable");
            return;
        }

        if (args.length == 0) {
            sendUsageMessage(sender);
            return;
        }

        String type = args[0].toLowerCase();
        int limit = 10;

        if (args.length >= 2) {
            try {
                limit = Math.min(25, Math.max(5, Integer.parseInt(args[1])));
            } catch (NumberFormatException ignored) {
            }
        }

        switch (type) {
            case "kills":
            case "kill":
            case "abates":
                showTopKills(sender, rankingManager, limit);
                break;
            case "elo":
            case "rank":
                showTopElo(sender, rankingManager, limit);
                break;
            case "coins":
            case "money":
            case "riqueza":
            case "moedas":
                showTopCoins(sender, rankingManager, limit);
                break;
            case "kdr":
            case "kd":
                showTopKdr(sender, rankingManager, limit);
                break;
            case "killstreak":
            case "streak":
            case "ks":
                showTopKillstreak(sender, rankingManager, limit);
                break;
            default:
                sendUsageMessage(sender);
                break;
        }
    }

    private void showTopKills(CommandSender sender, RankingManager manager, int limit) {
        List<RankingManager.RankEntry> top = manager.getTopKills(limit);
        showRanking(sender, top, "KILLS", "&c⚔ ", " kills", false);
    }

    private void showTopElo(CommandSender sender, RankingManager manager, int limit) {
        List<RankingManager.RankEntry> top = manager.getTopElo(limit);
        showRanking(sender, top, "ELO", "&d✦ ", " ELO", false);
    }

    private void showTopCoins(CommandSender sender, RankingManager manager, int limit) {
        List<RankingManager.RankEntry> top = manager.getTopCoins(limit);
        showRanking(sender, top, "RIQUEZA", "&6⛁ ", " coins", false);
    }

    private void showTopKdr(CommandSender sender, RankingManager manager, int limit) {
        List<RankingManager.RankEntry> top = manager.getTopKdr(limit);
        showRanking(sender, top, "KDR", "&e★ ", " K/D", true);
    }

    private void showTopKillstreak(CommandSender sender, RankingManager manager, int limit) {
        List<RankingManager.RankEntry> top = manager.getTopKillstreak(limit);
        showRanking(sender, top, "KILLSTREAK", "&e🔥 ", " streak", false);
    }

    private void showRanking(CommandSender sender, List<RankingManager.RankEntry> entries,
            String title, String prefix, String suffix, boolean isDouble) {
        ChatStorage.sendRaw(sender, "");
        sender.sendMessage(ChatStorage.getMessage("ranking.header"));
        ChatStorage.sendRaw(sender, "");
        ChatStorage.sendRaw(sender, "  &6&l★ TOP " + title + " ★");
        ChatStorage.sendRaw(sender, "");

        if (entries.isEmpty()) {
            sender.sendMessage(ChatStorage.getMessage("ranking.empty"));
        } else {
            int position = 1;
            for (RankingManager.RankEntry entry : entries) {
                String medal = getMedal(position);
                String valueStr;

                if (isDouble) {
                    valueStr = String.format("%.2f", entry.valueDouble);
                } else {
                    valueStr = ChatStorage.formatNumber(entry.value);
                }

                ChatStorage.sendRaw(sender, "  " + medal + " " + prefix + "&f" + entry.playerName +
                        " &8- &e" + valueStr + suffix);
                position++;
            }
        }

        ChatStorage.sendRaw(sender, "");
        sender.sendMessage(ChatStorage.getMessage("ranking.footer"));
        ChatStorage.sendRaw(sender, "");

        // Mostrar posição do jogador se for Player
        if (sender instanceof Player) {
            Player player = (Player) sender;
            String rankType = title.toLowerCase().replace(" ", "");
            int playerPos = plugin.getRankingManager().getPosition(player.getUniqueId(), rankType);

            if (playerPos > 0) {
                sender.sendMessage(
                        ChatStorage.getMessage("ranking.your-position", "position", String.valueOf(playerPos)));
            } else {
                sender.sendMessage(ChatStorage.getMessage("ranking.not-ranked"));
            }
            ChatStorage.sendRaw(sender, "");
        }
    }

    private String getMedal(int position) {
        switch (position) {
            case 1:
                return "&6&l#1";
            case 2:
                return "&7&l#2";
            case 3:
                return "&c&l#3";
            default:
                return "&7#" + position;
        }
    }

    private void sendUsageMessage(CommandSender sender) {
        ChatStorage.sendRaw(sender, "");
        sender.sendMessage(ChatStorage.getMessage("ranking.help.header"));
        ChatStorage.sendRaw(sender, "");
        ChatStorage.sendRaw(sender, "&e  Rankings disponiveis:");
        ChatStorage.sendRaw(sender, "    &f/top kills &8- &7Jogadores com mais kills");
        ChatStorage.sendRaw(sender, "    &f/top elo &8- &7Jogadores com maior ELO");
        ChatStorage.sendRaw(sender, "    &f/top coins &8- &7Jogadores mais ricos");
        ChatStorage.sendRaw(sender, "    &f/top kdr &8- &7Jogadores com melhor K/D");
        ChatStorage.sendRaw(sender, "    &f/top killstreak &8- &7Maiores killstreaks");
        ChatStorage.sendRaw(sender, "");
        ChatStorage.sendRaw(sender, "&6  Exemplo: &f/top kills 15");
        ChatStorage.sendRaw(sender, "");
    }
}
