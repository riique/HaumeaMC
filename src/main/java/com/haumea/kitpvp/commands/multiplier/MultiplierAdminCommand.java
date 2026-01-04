package com.haumea.kitpvp.commands.multiplier;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.commands.base.BaseCommand;
import com.haumea.kitpvp.commands.base.CommandInfo;
import com.haumea.kitpvp.managers.MultiplierManager;
import com.haumea.kitpvp.models.ActiveMultiplier;
import com.haumea.kitpvp.models.MultiplierType;
import com.haumea.kitpvp.utils.ChatStorage;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

/**
 * Comando administrativo para gerenciar multiplicadores de jogadores.
 * 
 * Uso:
 * - /multadmin give <jogador> <tipo> [quantidade] - Dar multiplicadores
 * - /multadmin remove <jogador> <tipo> [quantidade] - Remover multiplicadores
 * - /multadmin activate <jogador> <tipo> [duração] - Ativar multiplicador
 * diretamente
 * - /multadmin clear <jogador> - Limpar multiplicador ativo
 * - /multadmin check <jogador> - Ver multiplicadores de um jogador
 * - /multadmin list - Listar jogadores com multiplicador ativo
 * 
 * Permissões: Apenas Dono, Diretor, Gerente
 * 
 * @author HaumeaMC
 */
@CommandInfo(name = "multadmin", aliases = { "multiplicadoradmin",
        "mpadmin" }, description = "Administração de multiplicadores", allowedGroups = { "dono", "diretor",
                "gerente" }, usage = "/multadmin <give|remove|activate|clear|check|list>")
public class MultiplierAdminCommand extends BaseCommand {

    private final MultiplierManager multiplierManager;

    public MultiplierAdminCommand(HaumeaMC plugin) {
        super(plugin);
        this.multiplierManager = plugin.getMultiplierManager();
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            showHelp();
            return;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "give":
            case "dar":
            case "add":
                handleGive(args);
                break;

            case "remove":
            case "remover":
            case "take":
                handleRemove(args);
                break;

            case "activate":
            case "ativar":
            case "force":
                handleActivate(args);
                break;

            case "clear":
            case "limpar":
            case "deactivate":
                handleClear(args);
                break;

            case "check":
            case "ver":
            case "info":
                handleCheck(args);
                break;

            case "list":
            case "lista":
            case "ativos":
                handleList();
                break;

            case "help":
            case "ajuda":
            default:
                showHelp();
                break;
        }
    }

    /**
     * Dar multiplicadores a um jogador
     * /multadmin give <jogador> <tipo> [quantidade]
     */
    private void handleGive(String[] args) {
        if (args.length < 3) {
            sendRaw("&c&lUSO: &f/multadmin give <jogador> <tipo> [quantidade]");
            sendRaw("&7Tipos: 1.5, 2.0, 2.5, 3.0, 3.5");
            return;
        }

        String playerName = args[1];
        MultiplierType type = MultiplierType.fromString(args[2]);
        int amount = (args.length >= 4) ? parseAmount(args[3]) : 1;

        if (type == null) {
            sendRaw("&cTipo inválido! Tipos: 1.5, 2.0, 2.5, 3.0, 3.5");
            return;
        }

        if (amount <= 0) {
            sendRaw("&cQuantidade inválida!");
            return;
        }

        Player target = Bukkit.getPlayer(playerName);
        if (target != null) {
            // Jogador online
            multiplierManager.addToInventory(target, type, amount);

            sendRaw("&a&lSUCESSO! &fAdicionado &e" + amount + "x " + type.getDisplayMultiplier() +
                    " &fpara &e" + target.getName());

            ChatStorage.send(target, "multiplier.received",
                    "amount", String.valueOf(amount),
                    "type", type.getDisplayMultiplier());
        } else {
            // Jogador offline
            UUID uuid = getUUIDByName(playerName);
            if (uuid == null) {
                sendRaw("&cJogador não encontrado!");
                return;
            }

            if (multiplierManager.addToInventoryOffline(uuid, type, amount)) {
                sendRaw("&a&lSUCESSO! &fAdicionado &e" + amount + "x " + type.getDisplayMultiplier() +
                        " &fpara &e" + playerName + " &7(offline)");
            } else {
                sendRaw("&cErro ao adicionar multiplicadores para jogador offline!");
            }
        }
    }

    /**
     * Remover multiplicadores de um jogador
     * /multadmin remove <jogador> <tipo> [quantidade]
     */
    private void handleRemove(String[] args) {
        if (args.length < 3) {
            sendRaw("&c&lUSO: &f/multadmin remove <jogador> <tipo> [quantidade]");
            return;
        }

        String playerName = args[1];
        MultiplierType type = MultiplierType.fromString(args[2]);
        int amount = (args.length >= 4) ? parseAmount(args[3]) : 1;

        if (type == null) {
            sendRaw("&cTipo inválido!");
            return;
        }

        Player target = Bukkit.getPlayer(playerName);
        if (target != null) {
            if (multiplierManager.removeFromInventory(target, type, amount)) {
                sendRaw("&a&lSUCESSO! &fRemovido &e" + amount + "x " + type.getDisplayMultiplier() +
                        " &fde &e" + target.getName());
            } else {
                sendRaw("&cO jogador não possui quantidade suficiente!");
            }
        } else {
            UUID uuid = getUUIDByName(playerName);
            if (uuid == null) {
                sendRaw("&cJogador não encontrado!");
                return;
            }

            if (multiplierManager.removeFromInventoryOffline(uuid, type, amount)) {
                sendRaw("&a&lSUCESSO! &fRemovido &e" + amount + "x " + type.getDisplayMultiplier() +
                        " &fde &e" + playerName + " &7(offline)");
            } else {
                sendRaw("&cO jogador não possui quantidade suficiente!");
            }
        }
    }

    /**
     * Ativar multiplicador diretamente para um jogador
     * /multadmin activate <jogador> <tipo> [duração em minutos]
     */
    private void handleActivate(String[] args) {
        if (args.length < 3) {
            sendRaw("&c&lUSO: &f/multadmin activate <jogador> <tipo> [duração_minutos]");
            sendRaw("&7Duração padrão: 60 minutos");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sendRaw("&cJogador precisa estar online para ativar multiplicador!");
            return;
        }

        MultiplierType type = MultiplierType.fromString(args[2]);
        if (type == null) {
            sendRaw("&cTipo inválido!");
            return;
        }

        // Duração em minutos (padrão: 60)
        int durationMinutes = (args.length >= 4) ? parseAmount(args[3]) : 60;
        long durationMillis = durationMinutes * 60 * 1000L;

        multiplierManager.forceActivateMultiplier(target, type, durationMillis);

        sendRaw("&a&lSUCESSO! &fMultiplicador " + type.getDisplayMultiplier() +
                " &fativado para &e" + target.getName() + " &fpor &e" + durationMinutes + " &fminutos.");
    }

    /**
     * Limpar multiplicador ativo de um jogador
     * /multadmin clear <jogador>
     */
    private void handleClear(String[] args) {
        if (args.length < 2) {
            sendRaw("&c&lUSO: &f/multadmin clear <jogador>");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sendRaw("&cJogador precisa estar online!");
            return;
        }

        ActiveMultiplier active = multiplierManager.getActiveMultiplier(target);
        if (active == null) {
            sendRaw("&cO jogador não possui multiplicador ativo!");
            return;
        }

        multiplierManager.clearActiveMultiplier(target, true);
        sendRaw("&a&lSUCESSO! &fMultiplicador ativo de &e" + target.getName() + " &ffoi removido.");
    }

    /**
     * Ver multiplicadores de um jogador
     * /multadmin check <jogador>
     */
    private void handleCheck(String[] args) {
        if (args.length < 2) {
            sendRaw("&c&lUSO: &f/multadmin check <jogador>");
            return;
        }

        String playerName = args[1];
        Player target = Bukkit.getPlayer(playerName);
        UUID uuid;

        if (target != null) {
            uuid = target.getUniqueId();
        } else {
            uuid = getUUIDByName(playerName);
            if (uuid == null) {
                sendRaw("&cJogador não encontrado!");
                return;
            }
        }

        // Obter dados
        Map<MultiplierType, Integer> inventory = multiplierManager.getInventoryByUUID(uuid);
        ActiveMultiplier active = (target != null) ? multiplierManager.getActiveMultiplier(target) : null;

        sendRaw("");
        sendRaw("&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        sendRaw("&6&l⚡ MULTIPLICADORES DE &e" + playerName.toUpperCase());
        sendRaw("&7UUID: " + uuid);
        sendRaw("");

        // Multiplicador ativo
        sendRaw("&eMultiplicador Ativo:");
        if (active != null) {
            sendRaw("&f• " + active.getType().getDisplayMultiplier() + " &7- Tempo: &e" +
                    active.getFormattedRemainingTime());
        } else {
            sendRaw("&8• Nenhum");
        }

        sendRaw("");
        sendRaw("&eInventário de Multiplicadores:");

        int total = 0;
        for (MultiplierType type : MultiplierType.values()) {
            int count = inventory.getOrDefault(type, 0);
            total += count;
            if (count > 0) {
                sendRaw("&f• " + type.getDisplayMultiplier() + " &8- &a" + count);
            }
        }

        if (total == 0) {
            sendRaw("&8• Nenhum multiplicador no inventário");
        }

        sendRaw("");
        sendRaw("&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    /**
     * Listar jogadores com multiplicador ativo
     */
    private void handleList() {
        Map<UUID, ActiveMultiplier> actives = multiplierManager.getAllActiveMultipliers();

        sendRaw("");
        sendRaw("&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        sendRaw("&6&l⚡ MULTIPLICADORES ATIVOS");
        sendRaw("");

        if (actives.isEmpty()) {
            sendRaw("&cNenhum jogador possui multiplicador ativo no momento.");
        } else {
            sendRaw("&fJogadores com multiplicador ativo: &e" + actives.size());
            sendRaw("");

            for (Map.Entry<UUID, ActiveMultiplier> entry : actives.entrySet()) {
                Player player = Bukkit.getPlayer(entry.getKey());
                String name = (player != null) ? player.getName() : entry.getKey().toString().substring(0, 8) + "...";
                ActiveMultiplier active = entry.getValue();

                sendRaw("&f• &e" + name + " &8- " + active.getType().getDisplayMultiplier() +
                        " &7(&e" + active.getShortFormattedTime() + "&7)");
            }
        }

        sendRaw("");
        sendRaw("&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    /**
     * Mostra ajuda do comando
     */
    private void showHelp() {
        sendRaw("");
        sendRaw("&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        sendRaw("&6&l⚡ MULTIPLICADORES - ADMIN");
        sendRaw("");
        sendRaw("&e/multadmin give <jogador> <tipo> [qtd]");
        sendRaw("&7 → Dar multiplicadores a um jogador");
        sendRaw("");
        sendRaw("&e/multadmin remove <jogador> <tipo> [qtd]");
        sendRaw("&7 → Remover multiplicadores de um jogador");
        sendRaw("");
        sendRaw("&e/multadmin activate <jogador> <tipo> [minutos]");
        sendRaw("&7 → Ativar multiplicador diretamente");
        sendRaw("");
        sendRaw("&e/multadmin clear <jogador>");
        sendRaw("&7 → Remover multiplicador ativo");
        sendRaw("");
        sendRaw("&e/multadmin check <jogador>");
        sendRaw("&7 → Ver multiplicadores de um jogador");
        sendRaw("");
        sendRaw("&e/multadmin list");
        sendRaw("&7 → Listar multiplicadores ativos");
        sendRaw("");
        sendRaw("&7Tipos: 1.5, 2.0, 2.5, 3.0, 3.5");
        sendRaw("&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    /**
     * Converte string para quantidade inteira
     */
    private int parseAmount(String str) {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Obtém UUID por nome (do cache de contas MongoDB)
     */
    private UUID getUUIDByName(String name) {
        // Usar MongoAccountRepository
        if (plugin.getMongoAccountRepository() != null) {
            return plugin.getMongoAccountRepository().getUUIDByName(name);
        }
        return null;
    }
}
