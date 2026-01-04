package com.haumea.kitpvp.commands.duel;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.commands.base.BaseCommand;
import com.haumea.kitpvp.commands.base.CommandInfo;
import com.haumea.kitpvp.managers.DuelManager;
import com.haumea.kitpvp.menu.duel.DuelSettingsMenu;
import com.haumea.kitpvp.menu.duel.DuelStatsMenu;
import com.haumea.kitpvp.models.DuelSettings;
import com.haumea.kitpvp.utils.ChatStorage;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Comando principal de duelos 1v1.
 * 
 * Uso:
 * - /duel <jogador> - Desafiar um jogador
 * - /duel accept - Aceitar desafio
 * - /duel deny - Recusar desafio
 * - /duel fila - Entrar/sair da fila
 * - /duel stats - Ver estatísticas
 * 
 * @author HaumeaMC
 */
@CommandInfo(name = "duel", aliases = { "duelo", "1v1",
        "x1" }, description = "Sistema de duelos 1v1", usage = "/duel <jogador|accept|deny|fila|stats>", playerOnly = true)
public class DuelCommand extends BaseCommand {

    public DuelCommand(HaumeaMC plugin) {
        super(plugin);
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        Player player = getPlayer();
        DuelManager duelManager = plugin.getDuelManager();

        if (duelManager == null) {
            ChatStorage.send(player, "duel.error.unavailable");
            return;
        }

        if (args.length == 0) {
            sendHelp(player);
            return;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "aceitar":
            case "accept":
                handleAccept(player, duelManager);
                break;

            case "recusar":
            case "deny":
            case "negar":
                handleDeny(player, duelManager);
                break;

            case "fila":
            case "queue":
            case "procurar":
                handleQueue(player, duelManager);
                break;

            case "sair":
            case "leave":
                handleLeaveQueue(player, duelManager);
                break;

            case "stats":
            case "estatisticas":
            case "status":
                handleStats(player, duelManager);
                break;

            case "config":
            case "configurar":
                handleConfig(player);
                break;

            default:
                // Tentar interpretar como nome de jogador
                handleChallenge(player, duelManager, args[0]);
                break;
        }
    }

    /**
     * Envia mensagem de ajuda.
     */
    private void sendHelp(Player player) {
        player.sendMessage("");
        player.sendMessage(ChatStorage.getMessage("duel.help.header"));
        player.sendMessage("");
        player.sendMessage(ChatStorage.getMessage("duel.help.title"));
        player.sendMessage("");
        ChatStorage.sendRaw(player, "  &e/duel <jogador> &8- &7Desafiar jogador");
        ChatStorage.sendRaw(player, "  &e/duel accept &8- &7Aceitar desafio");
        ChatStorage.sendRaw(player, "  &e/duel deny &8- &7Recusar desafio");
        ChatStorage.sendRaw(player, "  &e/duel fila &8- &7Entrar/sair da fila");
        ChatStorage.sendRaw(player, "  &e/duel stats &8- &7Ver estatísticas");
        ChatStorage.sendRaw(player, "  &e/duel config &8- &7Configurar preferências");
        player.sendMessage("");
        player.sendMessage(ChatStorage.getMessage("duel.help.footer"));
        player.sendMessage("");
    }

    /**
     * Trata aceitar desafio.
     */
    private void handleAccept(Player player, DuelManager duelManager) {
        duelManager.acceptChallenge(player);
    }

    /**
     * Trata recusar desafio.
     */
    private void handleDeny(Player player, DuelManager duelManager) {
        duelManager.denyChallenge(player);
    }

    /**
     * Trata entrada/saída da fila.
     */
    private void handleQueue(Player player, DuelManager duelManager) {
        if (duelManager.isInQueue(player)) {
            duelManager.leaveQueue(player);
        } else {
            duelManager.joinQueue(player, new DuelSettings());
        }
    }

    /**
     * Trata saída explícita da fila.
     */
    private void handleLeaveQueue(Player player, DuelManager duelManager) {
        duelManager.leaveQueue(player);
    }

    /**
     * Trata visualização de estatísticas.
     */
    private void handleStats(Player player, DuelManager duelManager) {
        int wins = duelManager.getDuelWins(player);
        int losses = duelManager.getDuelLosses(player);
        int streak = duelManager.getDuelStreak(player);
        double winRate = (wins + losses) > 0 ? (wins * 100.0 / (wins + losses)) : 0;

        player.sendMessage("");
        player.sendMessage(ChatStorage.getMessage("duel.stats.header"));
        player.sendMessage("");
        player.sendMessage(ChatStorage.getMessage("duel.stats.title"));
        player.sendMessage("");
        ChatStorage.sendRaw(player, "  &aVitórias: &f" + wins);
        ChatStorage.sendRaw(player, "  &cDerrotas: &f" + losses);
        ChatStorage.sendRaw(player, "  &eWinstreak: &f" + streak + (streak > 0 ? " 🔥" : ""));
        ChatStorage.sendRaw(player, "  &bWinrate: &f" + String.format("%.1f", winRate) + "%");
        player.sendMessage("");
        player.sendMessage(ChatStorage.getMessage("duel.stats.footer"));
        player.sendMessage("");
    }

    /**
     * Trata configuração de preferências.
     */
    private void handleConfig(Player player) {
        new DuelSettingsMenu(plugin, player, new DuelSettings(), null).open();
    }

    /**
     * Trata desafio a um jogador.
     */
    private void handleChallenge(Player player, DuelManager duelManager, String targetName) {
        Player target = Bukkit.getPlayer(targetName);

        if (target == null) {
            ChatStorage.send(player, "duel.error.player-offline", "player", targetName);
            return;
        }

        if (target.equals(player)) {
            ChatStorage.send(player, "duel.error.self");
            return;
        }

        // Abrir menu de configuração do duelo antes de enviar o desafio
        new DuelSettingsMenu(plugin, player, new DuelSettings(), target).open();
    }
}
