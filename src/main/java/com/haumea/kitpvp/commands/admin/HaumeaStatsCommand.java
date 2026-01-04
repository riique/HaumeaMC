package com.haumea.kitpvp.commands.admin;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.commands.base.BaseCommand;
import com.haumea.kitpvp.commands.base.CommandInfo;
import com.haumea.kitpvp.managers.LeagueManager;
import com.haumea.kitpvp.managers.MultiplierManager;
import com.haumea.kitpvp.managers.StatsManager;
import com.haumea.kitpvp.models.MultiplierType;
import com.haumea.kitpvp.models.PlayerData;
import com.haumea.kitpvp.models.PlayerRank;
import com.haumea.kitpvp.profile.PlayerProfile;
import com.haumea.kitpvp.utils.ChatStorage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Comando administrativo para gerenciar estatísticas de jogadores.
 * Suporta jogadores online e offline.
 * 
 * Uso: /haumeastats <jogador> <add|set|remove> <kills|deaths|streak|elo|moedas>
 * <valor>
 * 
 * @author HaumeaMC
 */
@CommandInfo(name = "haumeastats", aliases = { "hstats",
        "statsadmin" }, permission = "haumea.admin.stats", playerOnly = false, usage = "/haumeastats <jogador> <add|set|remove> <kills|deaths|streak|elo|moedas> <valor>")
public class HaumeaStatsCommand extends BaseCommand implements TabCompleter {

    private static final String PREFIX = "&6&lHAUMEAMC&f ";

    private static final List<String> OPERATIONS = Arrays.asList("add", "set", "remove");
    private static final List<String> STAT_TYPES = Arrays.asList("kills", "deaths", "streak", "elo", "moedas",
            "multiplicador1", "multiplicador2", "multiplicador3", "multiplicador4", "multiplicador5");
    private static final List<String> VALUE_SUGGESTIONS = Arrays.asList("1", "5", "10", "100", "500", "1000");

    public HaumeaStatsCommand(HaumeaMC plugin) {
        super(plugin);
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        // Validar argumentos
        // Validar argumentos
        if (args.length < 4) {
            for (String line : ChatStorage.getMessageList("command.stats.header")) {
                ChatStorage.sendRaw(sender, line);
            }
            return;
        }

        String targetName = args[0];
        String operation = args[1].toLowerCase();
        String statType = args[2].toLowerCase();
        String valueStr = args[3];

        // Validar operação
        if (!OPERATIONS.contains(operation)) {
            ChatStorage.send(sender, "command.stats.error.invalid-operation");
            return;
        }

        // Validar tipo de estatística
        if (!STAT_TYPES.contains(statType)) {
            ChatStorage.send(sender, "command.stats.error.invalid-type");
            return;
        }

        // Validar valor
        long value;
        try {
            value = Long.parseLong(valueStr);
        } catch (NumberFormatException e) {
            ChatStorage.send(sender, "command.stats.error.invalid-value");
            return;
        }

        // Buscar jogador (online ou offline)
        Player onlineTarget = Bukkit.getPlayer(targetName);

        if (onlineTarget != null && onlineTarget.isOnline()) {
            // Jogador online - usar métodos normais
            handleOnlinePlayer(sender, onlineTarget, operation, statType, value);
        } else {
            // Jogador offline - tentar carregar dados
            handleOfflinePlayer(sender, targetName, operation, statType, value);
        }
    }

    /**
     * Processa estatísticas de um jogador online
     */
    private void handleOnlinePlayer(CommandSender sender, Player target, String operation, String statType,
            long value) {
        StatsManager stats = plugin.getStatsManager();
        LeagueManager leagueManager = plugin.getLeagueManager();

        long oldValue = 0;
        long newValue = 0;
        String displayType = statType.toUpperCase();

        switch (statType) {
            case "kills":
                oldValue = stats.getKills(target);
                newValue = calculateNewValue(oldValue, operation, value);
                stats.setKills(target, (int) newValue);
                break;

            case "deaths":
                oldValue = stats.getDeaths(target);
                newValue = calculateNewValue(oldValue, operation, value);
                stats.setDeaths(target, (int) newValue);
                break;

            case "streak":
                oldValue = stats.getKillstreak(target);
                newValue = calculateNewValue(oldValue, operation, value);
                stats.setKillstreak(target, (int) newValue);
                displayType = "KILLSTREAK";
                break;

            case "elo":
                oldValue = stats.getElo(target);
                newValue = calculateNewValue(oldValue, operation, value);
                if (leagueManager != null) {
                    leagueManager.setElo(target, (int) newValue);
                }

                // Mostrar nova liga
                PlayerRank newRank = leagueManager != null ? leagueManager.getRank(target) : null;
                if (newRank != null) {
                    ChatStorage.send(sender, "command.stats.success.admin",
                            "type", displayType,
                            "player", target.getName(),
                            "operation", ChatStorage.getMessage("command.stats.operations.admin-" + operation),
                            "value", String.valueOf(newValue));
                    ChatStorage.send(sender, "command.stats.success.combo-rank", "rank", newRank.getFormattedName());
                    return;
                }
                break;

            case "moedas":
                oldValue = stats.getMoney(target);
                newValue = calculateNewValue(oldValue, operation, value);
                stats.setMoney(target, newValue);
                displayType = "MOEDAS";
                break;

            case "multiplicador1":
            case "multiplicador2":
            case "multiplicador3":
            case "multiplicador4":
            case "multiplicador5":
                MultiplierManager multiplierManager = plugin.getMultiplierManager();
                if (multiplierManager == null) {
                    ChatStorage.send(sender, "command.stats.error.manager-unavailable");
                    return;
                }
                MultiplierType type = getMultiplierTypeFromStat(statType);
                oldValue = multiplierManager.getInventoryCount(target, type);
                newValue = calculateNewValue(oldValue, operation, value);
                multiplierManager.setInventoryCount(target, type, (int) newValue);
                displayType = "MULTIPLICADOR (" + ChatStorage.colorize(type.getDisplayMultiplier()) + "&f)";
                break;
        }

        // Mensagem de sucesso para o admin
        ChatStorage.send(sender, "command.stats.success.admin",
                "type", displayType,
                "player", target.getName(),
                "operation", ChatStorage.getMessage("command.stats.operations.admin-" + operation),
                "value", ChatStorage.formatNumber(newValue));

        // Notificar o jogador afetado
        if (target.isOnline() && sender != target) {
            ChatStorage.send(target, "command.stats.notification",
                    "type", displayType,
                    "operation", ChatStorage.getMessage("command.stats.operations." + operation),
                    "value", ChatStorage.formatNumber(newValue));
        }
    }

    /**
     * Processa estatísticas de um jogador offline
     */
    private void handleOfflinePlayer(CommandSender sender, String targetName, String operation, String statType,
            long value) {
        // Tentar obter UUID do jogador offline
        @SuppressWarnings("deprecation")
        org.bukkit.OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(targetName);

        if (offlinePlayer == null || !offlinePlayer.hasPlayedBefore()) {
            ChatStorage.send(sender, "command.stats.error.player-not-found", "player", targetName);
            return;
        }

        UUID uuid = offlinePlayer.getUniqueId();
        String playerName = offlinePlayer.getName() != null ? offlinePlayer.getName() : targetName;

        // Carregar dados do MongoDB
        PlayerData data = plugin.getProfileManager().getRepository().load(uuid, playerName);

        if (data == null) {
            ChatStorage.send(sender, "command.stats.error.db-not-found", "player", targetName);
            return;
        }

        long oldValue = 0;
        long newValue = 0;
        String displayType = statType.toUpperCase();

        switch (statType) {
            case "kills":
                oldValue = data.getKills();
                newValue = calculateNewValue(oldValue, operation, value);
                data.setKills((int) newValue);
                break;

            case "deaths":
                oldValue = data.getDeaths();
                newValue = calculateNewValue(oldValue, operation, value);
                data.setDeaths((int) newValue);
                break;

            case "streak":
                oldValue = data.getKillStreak();
                newValue = calculateNewValue(oldValue, operation, value);
                data.setKillStreak((int) newValue);
                // Atualizar highestKillStreak se necessário
                if (newValue > data.getHighestKillStreak()) {
                    data.setHighestKillStreak((int) newValue);
                }
                displayType = "KILLSTREAK";
                break;

            case "elo":
                oldValue = data.getCustomData("elo", 100);
                newValue = calculateNewValue(oldValue, operation, value);
                data.setCustomData("elo", (int) Math.max(0, newValue));

                // Calcular nova liga para exibição
                LeagueManager leagueManager = plugin.getLeagueManager();
                if (leagueManager != null) {
                    PlayerRank newRank = leagueManager.getRankByElo((int) newValue);
                    ChatStorage.send(sender, "command.stats.success.admin-offline",
                            "type", displayType,
                            "player", playerName,
                            "operation", ChatStorage.getMessage("command.stats.operations.admin-" + operation),
                            "value", String.valueOf(newValue));
                    ChatStorage.send(sender, "command.stats.success.combo-rank", "rank", newRank.getFormattedName());

                    // Salvar no MongoDB
                    plugin.getProfileManager().getRepository().saveAsync(data);
                    return;
                }
                break;

            case "moedas":
                oldValue = data.getCoins();
                newValue = calculateNewValue(oldValue, operation, value);
                data.setCoins(newValue);
                displayType = "MOEDAS";
                break;

            case "multiplicador1":
            case "multiplicador2":
            case "multiplicador3":
            case "multiplicador4":
            case "multiplicador5":
                // Para jogadores offline, manipular diretamente via customData
                MultiplierType type = getMultiplierTypeFromStat(statType);
                Map<String, Object> customData = data.getCustomData();
                @SuppressWarnings("unchecked")
                Map<String, Integer> inventory = (Map<String, Integer>) customData.get("multiplier_inventory");
                if (inventory == null) {
                    inventory = new java.util.HashMap<>();
                }
                oldValue = inventory.getOrDefault(type.name(), 0);
                newValue = calculateNewValue(oldValue, operation, value);
                inventory.put(type.name(), (int) Math.max(0, newValue));
                customData.put("multiplier_inventory", inventory);
                displayType = "MULTIPLICADOR (" + ChatStorage.colorize(type.getDisplayMultiplier()) + "&f)";
                break;
        }

        // Salvar no MongoDB
        plugin.getProfileManager().getRepository().saveAsync(data);

        // Mensagem de sucesso
        // Mensagem de sucesso
        ChatStorage.send(sender, "command.stats.success.admin-offline",
                "type", displayType,
                "player", playerName,
                "operation", ChatStorage.getMessage("command.stats.operations.admin-" + operation),
                "value", ChatStorage.formatNumber(newValue));
    }

    /**
     * Calcula o novo valor baseado na operação
     */
    private long calculateNewValue(long oldValue, String operation, long value) {
        switch (operation) {
            case "add":
                return oldValue + value;
            case "set":
                return Math.max(0, value);
            case "remove":
                return Math.max(0, oldValue - value);
            default:
                return oldValue;
        }
    }

    /**
     * Retorna o texto da operação para exibição
     */
    private String getOperationText(String operation) {
        switch (operation) {
            case "add":
                return "alterada para";
            case "set":
                return "definida como";
            case "remove":
                return "alterada para";
            default:
                return "alterada para";
        }
    }

    /**
     * Retorna o texto da operação para notificação ao jogador
     */
    private String getPlayerNotificationText(String operation) {
        switch (operation) {
            case "add":
                return "aumentada para";
            case "set":
                return "definida como";
            case "remove":
                return "reduzida para";
            default:
                return "alterada para";
        }
    }

    /**
     * Converte o nome do stat type para o MultiplierType correspondente
     */
    private MultiplierType getMultiplierTypeFromStat(String statType) {
        switch (statType) {
            case "multiplicador1":
                return MultiplierType.X1_5;
            case "multiplicador2":
                return MultiplierType.X2_0;
            case "multiplicador3":
                return MultiplierType.X2_5;
            case "multiplicador4":
                return MultiplierType.X3_0;
            case "multiplicador5":
                return MultiplierType.X3_5;
            default:
                return MultiplierType.X1_5;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // Arg 0: Lista de jogadores online
            String prefix = args[0].toLowerCase();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(prefix)) {
                    completions.add(player.getName());
                }
            }
        } else if (args.length == 2) {
            // Arg 1: Operações
            String prefix = args[1].toLowerCase();
            for (String op : OPERATIONS) {
                if (op.startsWith(prefix)) {
                    completions.add(op);
                }
            }
        } else if (args.length == 3) {
            // Arg 2: Tipos de estatística
            String prefix = args[2].toLowerCase();
            for (String type : STAT_TYPES) {
                if (type.startsWith(prefix)) {
                    completions.add(type);
                }
            }
        } else if (args.length == 4) {
            // Arg 3: Sugestões de valor
            String prefix = args[3].toLowerCase();
            for (String suggestion : VALUE_SUGGESTIONS) {
                if (suggestion.startsWith(prefix)) {
                    completions.add(suggestion);
                }
            }
        }

        return completions;
    }
}
