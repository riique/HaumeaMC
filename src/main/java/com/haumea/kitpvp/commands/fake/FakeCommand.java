package com.haumea.kitpvp.commands.fake;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.commands.base.BaseCommand;
import com.haumea.kitpvp.commands.base.CommandInfo;
import com.haumea.kitpvp.utils.ChatStorage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Comando /fake - Sistema de Fake Nick do HaumeaMC.
 * 
 * Permite que jogadores alterem seu nome de exibição mantendo
 * suas permissões, tags e benefícios originais.
 * 
 * Subcomandos:
 * - /fake <nome> : Define um nome específico
 * - /fake random : Escolhe um nome aleatório
 * - /fake reset : Restaura o nome original
 * 
 * @author HaumeaMC
 */
@CommandInfo(name = "fake", aliases = { "nick",
        "fakenick" }, description = "Altera seu nome de exibição", usage = "/fake <nome|random|reset>", allowedGroups = {
                "dono", "diretor", "gerente", "admin", "modplus", "modgc", "mod" }, playerOnly = true)
public class FakeCommand extends BaseCommand implements TabCompleter {

    public FakeCommand(HaumeaMC plugin) {
        super(plugin);
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        Player player = getPlayer();

        if (args.length == 0) {
            sendHelpMessage(player);
            return;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "random":
            case "aleatorio":
            case "r":
                handleRandom(player);
                break;

            case "reset":
            case "off":
            case "remover":
            case "desativar":
                handleReset(player);
                break;

            case "info":
            case "status":
                handleInfo(player);
                break;

            default:
                // Tentar como nome específico
                handleSetName(player, args[0]);
                break;
        }
    }

    /**
     * Define um nome específico
     */
    private void handleSetName(Player player, String name) {
        // Verificar se o FakeNickManager está disponível
        if (plugin.getFakeNickManager() == null) {
            ChatStorage.send(player, "fake.error.unavailable");
            return;
        }

        // Aplicar o fake nick
        plugin.getFakeNickManager().setFakeNick(player, name);
    }

    /**
     * Define um nome aleatório
     */
    private void handleRandom(Player player) {
        if (plugin.getFakeNickManager() == null) {
            ChatStorage.send(player, "fake.error.unavailable");
            return;
        }

        plugin.getFakeNickManager().setRandomFakeNick(player);
    }

    /**
     * Restaura o nome original
     */
    private void handleReset(Player player) {
        if (plugin.getFakeNickManager() == null) {
            ChatStorage.send(player, "fake.error.unavailable");
            return;
        }

        plugin.getFakeNickManager().resetFakeNick(player);
    }

    /**
     * Mostra informações sobre o fake nick atual
     */
    private void handleInfo(Player player) {
        if (plugin.getFakeNickManager() == null) {
            ChatStorage.send(player, "fake.error.unavailable");
            return;
        }

        ChatStorage.sendRaw(player, "&8&m----------------------------------------");
        ChatStorage.sendRaw(player, "");
        player.sendMessage(ChatStorage.getMessage("fake.info.header"));
        ChatStorage.sendRaw(player, "");

        if (plugin.getFakeNickManager().hasFakeNick(player)) {
            String fakeNick = plugin.getFakeNickManager().getFakeNick(player);
            String originalName = plugin.getFakeNickManager().getOriginalName(player);
            String prefix = plugin.getFakeNickManager().getCurrentPrefix(player);

            player.sendMessage(ChatStorage.getMessage("fake.info.status-active"));
            player.sendMessage(ChatStorage.getMessage("fake.info.original-name", "name", originalName));
            player.sendMessage(ChatStorage.getMessage("fake.info.fake-name", "name", fakeNick));
            player.sendMessage(ChatStorage.getMessage("fake.info.prefix", "prefix", prefix));
            ChatStorage.sendRaw(player, "");
            player.sendMessage(ChatStorage.getMessage("fake.info.reset-hint"));
        } else {
            player.sendMessage(ChatStorage.getMessage("fake.info.status-inactive"));
            player.sendMessage(ChatStorage.getMessage("fake.info.real-name", "name", player.getName()));
            ChatStorage.sendRaw(player, "");
            player.sendMessage(ChatStorage.getMessage("fake.info.activate-hint"));
        }

        ChatStorage.sendRaw(player, "");
        ChatStorage.sendRaw(player, "&8&m----------------------------------------");
    }

    /**
     * Envia a mensagem de ajuda
     */
    private void sendHelpMessage(Player player) {
        ChatStorage.sendRaw(player, "");
        player.sendMessage(ChatStorage.getMessage("fake.help.header"));
        ChatStorage.sendRaw(player, "");
        player.sendMessage(ChatStorage.getMessage("fake.help.usage"));
        ChatStorage.sendRaw(player, "");

        for (String line : ChatStorage.getMessageList("fake.help.subcommands")) {
            ChatStorage.sendRaw(player, line);
        }

        ChatStorage.sendRaw(player, "");

        for (String line : ChatStorage.getMessageList("fake.help.examples")) {
            ChatStorage.sendRaw(player, line);
        }

        ChatStorage.sendRaw(player, "");
        player.sendMessage(ChatStorage.getMessage("fake.help.note"));
        ChatStorage.sendRaw(player, "");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String partial = args[0].toLowerCase();

            List<String> options = Arrays.asList(
                    "random", "reset", "info");

            for (String option : options) {
                if (option.startsWith(partial)) {
                    completions.add(option);
                }
            }
        }

        return completions;
    }
}
