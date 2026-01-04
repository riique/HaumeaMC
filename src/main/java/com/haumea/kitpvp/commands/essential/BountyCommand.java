package com.haumea.kitpvp.commands.essential;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.commands.base.BaseCommand;
import com.haumea.kitpvp.commands.base.CommandInfo;
import com.haumea.kitpvp.managers.BountyManager;
import com.haumea.kitpvp.utils.ChatStorage;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Comando /bounty - Sistema de recompensas
 * 
 * Uso:
 * /bounty <jogador> <quantia> - Coloca bounty em um jogador
 * /bounty top - Mostra jogadores com maior bounty
 * /bounty info <jogador> - Mostra bounty de um jogador
 * 
 * @author HaumeaMC
 */
@CommandInfo(name = "bounty", aliases = { "recompensa", "wanted" }, permission = "", playerOnly = true)
public class BountyCommand extends BaseCommand {

    public BountyCommand(HaumeaMC plugin) {
        super(plugin);
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;
        BountyManager bountyManager = plugin.getBountyManager();

        if (bountyManager == null) {
            ChatStorage.send(player, "bounty.error.unavailable");
            return;
        }

        if (args.length == 0) {
            sendUsageMessage(player);
            return;
        }

        String subCommand = args[0].toLowerCase();

        // /bounty top
        if (subCommand.equals("top") || subCommand.equals("lista")) {
            showTopBounties(player, bountyManager);
            return;
        }

        // /bounty info <jogador>
        if (subCommand.equals("info") || subCommand.equals("ver")) {
            if (args.length < 2) {
                ChatStorage.send(player, "bounty.error.info-usage");
                return;
            }
            showPlayerBounty(player, args[1], bountyManager);
            return;
        }

        // /bounty <jogador> <quantia>
        if (args.length < 2) {
            sendUsageMessage(player);
            return;
        }

        // Buscar jogador alvo
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            ChatStorage.send(player, "bounty.error.player-offline", "player", args[0]);
            return;
        }

        // Parsear quantia
        long amount;
        try {
            String amountStr = args[1].toLowerCase()
                    .replace("k", "000")
                    .replace("m", "000000");
            amount = Long.parseLong(amountStr);
        } catch (NumberFormatException e) {
            ChatStorage.send(player, "bounty.error.invalid-amount");
            return;
        }

        if (amount <= 0) {
            ChatStorage.send(player, "bounty.error.zero-amount");
            return;
        }

        // Adicionar bounty
        bountyManager.addBounty(player, target, amount);
    }

    private void showTopBounties(Player player, BountyManager bountyManager) {
        List<Map.Entry<UUID, Long>> topBounties = bountyManager.getTopBounties(10);

        ChatStorage.sendRaw(player, "");
        player.sendMessage(ChatStorage.getMessage("bounty.top.header"));
        ChatStorage.sendRaw(player, "");

        if (topBounties.isEmpty()) {
            player.sendMessage(ChatStorage.getMessage("bounty.top.empty"));
        } else {
            int position = 1;
            for (Map.Entry<UUID, Long> entry : topBounties) {
                String targetName = Bukkit.getOfflinePlayer(entry.getKey()).getName();
                if (targetName == null)
                    targetName = "Desconhecido";

                String medal = position <= 3 ? getMedal(position) : "&7#" + position;
                ChatStorage.sendRaw(player, "  " + medal + " &f" + targetName + " &8- &c" +
                        ChatStorage.formatNumber(entry.getValue()) + " coins");
                position++;
            }
        }

        ChatStorage.sendRaw(player, "");
        player.sendMessage(ChatStorage.getMessage("bounty.top.count", "count",
                String.valueOf(bountyManager.getActiveBountiesCount())));
        ChatStorage.sendRaw(player, "");
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

    private void showPlayerBounty(Player player, String targetName, BountyManager bountyManager) {
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            ChatStorage.send(player, "bounty.error.player-offline", "player", targetName);
            return;
        }

        long bounty = bountyManager.getBounty(target);
        if (bounty <= 0) {
            ChatStorage.send(player, "bounty.info.none", "player", target.getName());
        } else {
            ChatStorage.send(player, "bounty.info.has-bounty", "player", target.getName(), "amount",
                    ChatStorage.formatNumber(bounty));
        }
    }

    private void sendUsageMessage(Player player) {
        ChatStorage.sendRaw(player, "");
        player.sendMessage(ChatStorage.getMessage("bounty.help.header"));
        ChatStorage.sendRaw(player, "");
        ChatStorage.sendRaw(player, "&e  Uso:");
        ChatStorage.sendRaw(player, "    &f/bounty <jogador> <quantia> &8- &7Colocar bounty");
        ChatStorage.sendRaw(player, "    &f/bounty top &8- &7Ver top bounties");
        ChatStorage.sendRaw(player, "    &f/bounty info <jogador> &8- &7Ver bounty de um jogador");
        ChatStorage.sendRaw(player, "");
        ChatStorage.sendRaw(player, "&6  Informacoes:");
        ChatStorage.sendRaw(player, "    &c• &7Minimo: &e100 coins");
        ChatStorage.sendRaw(player, "    &c• &7Maximo: &e1.000.000 coins &7(total)");
        ChatStorage.sendRaw(player, "    &c• &7Taxa: &e5% &7para o servidor");
        ChatStorage.sendRaw(player, "");
        ChatStorage.sendRaw(player, "&6  Exemplo: &f/bounty Steve 5000");
        ChatStorage.sendRaw(player, "");
    }
}
