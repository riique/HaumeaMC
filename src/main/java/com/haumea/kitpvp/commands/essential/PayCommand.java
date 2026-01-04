package com.haumea.kitpvp.commands.essential;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.commands.base.BaseCommand;
import com.haumea.kitpvp.commands.base.CommandInfo;
import com.haumea.kitpvp.managers.TradeManager;
import com.haumea.kitpvp.utils.ChatStorage;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Comando /pagar - Sistema de pagamento/transferência de coins
 * 
 * Permite que jogadores transfiram coins para outros jogadores.
 * NÃO É UMA TROCA - é uma transferência unilateral com confirmação.
 * 
 * Uso:
 * /pagar <jogador> <quantia> - Envia coins para outro jogador
 * /pagar aceitar - Aceita um pagamento pendente
 * /pagar recusar - Recusa um pagamento pendente
 * 
 * @author HaumeaMC
 */
@CommandInfo(name = "pagar", aliases = { "pay", "transfer", "enviar" }, permission = "", playerOnly = true)
public class PayCommand extends BaseCommand {

    public PayCommand(HaumeaMC plugin) {
        super(plugin);
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;
        TradeManager tradeManager = plugin.getTradeManager();

        if (tradeManager == null) {
            ChatStorage.send(player, "pay.error.unavailable");
            return;
        }

        if (args.length == 0) {
            sendUsageMessage(player);
            return;
        }

        String subCommand = args[0].toLowerCase();

        // Aceitar pagamento
        if (subCommand.equals("aceitar") || subCommand.equals("accept")) {
            tradeManager.acceptTrade(player);
            return;
        }

        // Recusar pagamento
        if (subCommand.equals("recusar") || subCommand.equals("deny") || subCommand.equals("negar")) {
            tradeManager.denyTrade(player);
            return;
        }

        // Enviar pagamento: /pagar <jogador> <quantia>
        if (args.length < 2) {
            sendUsageMessage(player);
            return;
        }

        // Buscar jogador alvo
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            ChatStorage.send(player, "pay.error.player-offline", "player", args[0]);
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
            ChatStorage.send(player, "pay.error.invalid-amount");
            return;
        }

        if (amount <= 0) {
            ChatStorage.send(player, "pay.error.zero-amount");
            return;
        }

        // Enviar pagamento
        tradeManager.sendTradeRequest(player, target, amount);
    }

    private void sendUsageMessage(Player player) {
        ChatStorage.sendRaw(player, "");
        ChatStorage.sendRaw(player, "&6&l  PAGAR &8- &7Transfira coins para outros jogadores");
        ChatStorage.sendRaw(player, "");
        ChatStorage.sendRaw(player, "&e  Uso: &f/pagar <jogador> <quantia>");
        ChatStorage.sendRaw(player, "");
        ChatStorage.sendRaw(player, "&6  Comandos:");
        ChatStorage.sendRaw(player, "    &a /pagar <jogador> <quantia> &8- &7Enviar coins");
        ChatStorage.sendRaw(player, "    &e /pagar aceitar &8- &7Aceitar pagamento recebido");
        ChatStorage.sendRaw(player, "    &c /pagar recusar &8- &7Recusar pagamento recebido");
        ChatStorage.sendRaw(player, "");
        ChatStorage.sendRaw(player, "&6  Informações:");
        ChatStorage.sendRaw(player, "    &7• Taxa: &e2% &7(descontado do valor)");
        ChatStorage.sendRaw(player, "    &7• Mínimo: &e10 coins");
        ChatStorage.sendRaw(player, "    &7• Máximo: &e100.000 coins");
        ChatStorage.sendRaw(player, "");
        ChatStorage.sendRaw(player, "&6  Exemplo: &f/pagar Steve 1000");
        ChatStorage.sendRaw(player, "");
    }
}
