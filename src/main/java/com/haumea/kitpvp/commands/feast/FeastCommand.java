package com.haumea.kitpvp.commands.feast;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.managers.FeastManager;
import com.haumea.kitpvp.managers.FeastManager.FeastLocation;
import com.haumea.kitpvp.utils.ChatStorage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Comando principal do Sistema de Feast.
 * 
 * Subcomandos:
 * - start: Inicia o Feast imediatamente
 * - stop: Para o Feast atual e cancela o timer
 * - timer <segundos>: Define o tempo até o próximo Feast
 * - addchest: Adiciona a localização atual como ponto de baú
 * - removechest: Remove a localização de baú mais próxima
 * - list: Lista todas as localizações de baús
 * - reload: Recarrega as configurações do arquivo
 * - status: Mostra o status atual do sistema
 * 
 * @author HaumeaMC
 */
public class FeastCommand implements CommandExecutor, TabCompleter {

    private final HaumeaMC plugin;
    private final FeastManager feastManager;

    // Lista de grupos que podem usar este comando
    private static final List<String> ALLOWED_GROUPS = Arrays.asList("dono", "diretor", "gerente");

    // Subcomandos disponíveis
    private static final List<String> SUBCOMMANDS = Arrays.asList(
            "start", "stop", "timer", "addchest", "removechest", "list", "reload", "status");

    public FeastCommand(HaumeaMC plugin) {
        this.plugin = plugin;
        this.feastManager = plugin.getFeastManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Verificar se é jogador para comandos que precisam de jogador
        boolean isPlayer = sender instanceof Player;
        Player player = isPlayer ? (Player) sender : null;

        // Verificar permissão
        if (isPlayer && !hasPermission(player)) {
            ChatStorage.send(sender, "feast.no-permission");
            return true;
        }

        // Sem argumentos - mostrar ajuda
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subcommand = args[0].toLowerCase();

        switch (subcommand) {
            case "start":
                handleStart(sender, player);
                break;

            case "stop":
                handleStop(sender, player);
                break;

            case "timer":
                handleTimer(sender, args);
                break;

            case "addchest":
                handleAddChest(sender, player);
                break;

            case "removechest":
            case "delchest":
                handleRemoveChest(sender, player);
                break;

            case "list":
                handleList(sender);
                break;

            case "reload":
                handleReload(sender);
                break;

            case "status":
                handleStatus(sender);
                break;

            default:
                ChatStorage.send(sender, "feast.unknown-subcommand");
                sendHelp(sender);
                break;
        }

        return true;
    }

    /**
     * Verifica se o jogador tem permissão para usar o comando
     */
    private boolean hasPermission(Player player) {
        // Verificar grupo
        com.haumea.kitpvp.models.Group group = plugin.getGroupManager().getPlayerGroup(player);
        if (group != null && ALLOWED_GROUPS.contains(group.getName().toLowerCase())) {
            return true;
        }

        // Verificar permissão específica
        return player.hasPermission("haumea.feast.admin");
    }

    /**
     * Handler: start - Inicia o Feast
     */
    private void handleStart(CommandSender sender, Player player) {
        if (feastManager.isFeastActive()) {
            ChatStorage.send(sender, "feast.already-active");
            return;
        }

        if (!feastManager.hasChestLocations()) {
            ChatStorage.send(sender, "feast.no-locations");
            return;
        }

        // Se já está em countdown, forçar spawn imediato
        if (feastManager.isCountdownActive()) {
            feastManager.startFeastNow(player);
            ChatStorage.send(sender, "feast.started-force");
        } else {
            // Iniciar countdown
            feastManager.startCountdown(player);
            ChatStorage.send(sender, "feast.countdown-admin-started");
        }
    }

    /**
     * Handler: stop - Para o Feast
     */
    private void handleStop(CommandSender sender, Player player) {
        if (feastManager.getState() == FeastManager.FeastState.IDLE) {
            ChatStorage.send(sender, "feast.not-active");
            return;
        }

        feastManager.stopFeast(false);
        ChatStorage.send(sender, "feast.stopped");
    }

    /**
     * Handler: timer - Define o timer
     */
    private void handleTimer(CommandSender sender, String[] args) {
        if (args.length < 2) {
            ChatStorage.send(sender, "feast.timer-usage");
            ChatStorage.sendRaw(sender, "§7Timer atual: §e" + feastManager.getTimeRemaining() + "s");
            return;
        }

        try {
            int seconds = Integer.parseInt(args[1]);

            if (seconds < 10) {
                ChatStorage.send(sender, "feast.timer-min");
                return;
            }

            if (seconds > 3600) {
                ChatStorage.send(sender, "feast.timer-max");
                return;
            }

            feastManager.setTimer(seconds);
            ChatStorage.send(sender, "feast.timer-set", "time", formatTime(seconds));

        } catch (NumberFormatException e) {
            ChatStorage.send(sender, "error.invalid-number", "value", args[1]);
        }
    }

    /**
     * Handler: addchest - Adiciona localização de baú
     */
    private void handleAddChest(CommandSender sender, Player player) {
        if (player == null) {
            ChatStorage.send(sender, "error.player-only");
            return;
        }

        FeastLocation location = feastManager.addChestLocation(player.getLocation());

        ChatStorage.send(sender, "feast.chest-added",
                "id", location.getId(),
                "x", String.valueOf(location.getX()),
                "y", String.valueOf(location.getY()),
                "z", String.valueOf(location.getZ()));

        ChatStorage.sendRaw(sender, "§7Total de localizações: §e" + feastManager.getChestLocationCount());
    }

    /**
     * Handler: removechest - Remove localização de baú
     */
    private void handleRemoveChest(CommandSender sender, Player player) {
        if (player == null) {
            ChatStorage.send(sender, "error.player-only");
            return;
        }

        if (!feastManager.hasChestLocations()) {
            ChatStorage.send(sender, "feast.no-locations");
            return;
        }

        FeastLocation nearest = feastManager.getNearestLocation(player.getLocation());
        if (nearest == null) {
            ChatStorage.send(sender, "feast.no-nearby-chest");
            return;
        }

        // Verificar distância (máximo 10 blocos)
        double distance = Math.sqrt(nearest.distanceSquared(player.getLocation()));
        if (distance > 10) {
            ChatStorage.send(sender, "feast.chest-too-far", "distance", String.format("%.1f", distance));
            return;
        }

        feastManager.removeChestLocation(nearest.getId());

        ChatStorage.send(sender, "feast.chest-removed",
                "id", nearest.getId(),
                "x", String.valueOf(nearest.getX()),
                "y", String.valueOf(nearest.getY()),
                "z", String.valueOf(nearest.getZ()));

        ChatStorage.sendRaw(sender, "§7Localizações restantes: §e" + feastManager.getChestLocationCount());
    }

    /**
     * Handler: list - Lista localizações
     */
    private void handleList(CommandSender sender) {
        if (!feastManager.hasChestLocations()) {
            ChatStorage.send(sender, "feast.no-locations");
            ChatStorage.sendRaw(sender, "§7Use §e/feast addchest §7para adicionar localizações.");
            return;
        }

        ChatStorage.sendRaw(sender, "");
        ChatStorage.sendRaw(sender, "§6§l🎁 LOCALIZAÇÕES DE FEAST");
        ChatStorage.sendRaw(sender, "§8§m-----------------------------");

        for (String line : feastManager.getFormattedLocations()) {
            ChatStorage.sendRaw(sender, " §8• " + line);
        }

        ChatStorage.sendRaw(sender, "§8§m-----------------------------");
        ChatStorage.sendRaw(sender, "§7Total: §e" + feastManager.getChestLocationCount() + " §7localização(ões)");
        ChatStorage.sendRaw(sender, "");
    }

    /**
     * Handler: reload - Recarrega configurações
     */
    private void handleReload(CommandSender sender) {
        feastManager.reload();
        ChatStorage.send(sender, "feast.reload");
    }

    /**
     * Handler: status - Mostra status
     */
    private void handleStatus(CommandSender sender) {
        FeastManager.FeastState state = feastManager.getState();

        ChatStorage.sendRaw(sender, "");
        ChatStorage.sendRaw(sender, "§6§l🎁 STATUS DO FEAST");
        ChatStorage.sendRaw(sender, "§8§m-----------------------------");

        // Estado
        String stateDisplay = getStateDisplay(state);
        ChatStorage.sendRaw(sender, " §fEstado: " + stateDisplay);

        // Timer
        if (state == FeastManager.FeastState.COUNTDOWN) {
            ChatStorage.sendRaw(sender, " §fTempo restante: §e" + formatTime(feastManager.getTimeRemaining()));
        } else if (state == FeastManager.FeastState.IDLE) {
            ChatStorage.sendRaw(sender, " §fPróximo em: §7(aguardando início)");
        }

        // Localizações
        int locations = feastManager.getChestLocationCount();
        String locColor = locations > 0 ? "§a" : "§c";
        ChatStorage.sendRaw(sender, " §fLocalizações: " + locColor + locations);

        ChatStorage.sendRaw(sender, "§8§m-----------------------------");
        ChatStorage.sendRaw(sender, "");
    }

    /**
     * Envia ajuda do comando
     */
    private void sendHelp(CommandSender sender) {
        ChatStorage.sendRaw(sender, "");
        ChatStorage.sendRaw(sender, "&6&l  🎁 FEAST &8- &7Sistema de Eventos");
        ChatStorage.sendRaw(sender, "");
        ChatStorage.sendRaw(sender, "&6  Subcomandos:");
        ChatStorage.sendRaw(sender, "");
        ChatStorage.sendRaw(sender, "    &a• start &8- &7Inicia o feast (ou força spawn)");
        ChatStorage.sendRaw(sender, "    &c• stop &8- &7Para o feast atual");
        ChatStorage.sendRaw(sender, "    &e• timer <seg> &8- &7Define o timer (ex: 300)");
        ChatStorage.sendRaw(sender, "");
        ChatStorage.sendRaw(sender, "&6  Localizações:");
        ChatStorage.sendRaw(sender, "");
        ChatStorage.sendRaw(sender, "    &b• addchest &8- &7Adiciona localização de baú");
        ChatStorage.sendRaw(sender, "    &c• removechest &8- &7Remove baú mais próximo");
        ChatStorage.sendRaw(sender, "    &7• list &8- &7Lista todas as localizações");
        ChatStorage.sendRaw(sender, "");
        ChatStorage.sendRaw(sender, "&6  Outros:");
        ChatStorage.sendRaw(sender, "");
        ChatStorage.sendRaw(sender, "    &d• status &8- &7Status do sistema");
        ChatStorage.sendRaw(sender, "    &e• reload &8- &7Recarrega configurações");
        ChatStorage.sendRaw(sender, "");
    }

    /**
     * Obtém display formatado do estado
     */
    private String getStateDisplay(FeastManager.FeastState state) {
        switch (state) {
            case IDLE:
                return "§7Inativo";
            case COUNTDOWN:
                return "§e⏱ Contagem Regressiva";
            case ACTIVE:
                return "§a✔ Ativo";
            case CLEANUP:
                return "§6Limpando...";
            default:
                return "§8Desconhecido";
        }
    }

    /**
     * Formata tempo em segundos
     */
    private String formatTime(int seconds) {
        if (seconds >= 60) {
            int minutes = seconds / 60;
            int secs = seconds % 60;
            if (secs == 0) {
                return minutes + " minuto" + (minutes > 1 ? "s" : "");
            }
            return minutes + "m " + secs + "s";
        }
        return seconds + " segundo" + (seconds != 1 ? "s" : "");
    }

    // ==================== TAB COMPLETION ====================

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filterCompletions(SUBCOMMANDS, args[0]);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("timer")) {
            // Sugestões de tempo
            return Arrays.asList("60", "120", "300", "600", "900");
        }

        return new ArrayList<>();
    }

    /**
     * Filtra completions baseado no input
     */
    private List<String> filterCompletions(List<String> options, String input) {
        String lowerInput = input.toLowerCase();
        return options.stream()
                .filter(opt -> opt.toLowerCase().startsWith(lowerInput))
                .collect(Collectors.toList());
    }
}
