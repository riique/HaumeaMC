package com.haumea.kitpvp.commands.multiplier;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.commands.base.BaseCommand;
import com.haumea.kitpvp.commands.base.CommandInfo;
import com.haumea.kitpvp.managers.MultiplierManager;
import com.haumea.kitpvp.menu.MultiplierMenu;
import com.haumea.kitpvp.models.ActiveMultiplier;
import com.haumea.kitpvp.models.MultiplierType;
import com.haumea.kitpvp.utils.ChatStorage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

/**
 * Comando de multiplicadores para jogadores.
 * 
 * Uso:
 * - /multiplicador - Abre o menu de multiplicadores
 * - /multiplicador status - Mostra status do multiplicador ativo
 * - /multiplicador lista - Lista multiplicadores no inventário
 * - /multiplicador ativar <tipo> - Ativa um multiplicador
 * 
 * Aliases: /multi, /mult, /mp
 * 
 * @author HaumeaMC
 */
@CommandInfo(name = "multiplicador", aliases = { "multi", "mult",
        "mp" }, description = "Gerencia seus multiplicadores de coins", playerOnly = true, usage = "/multiplicador [status|lista|ativar <tipo>]")
public class MultiplierCommand extends BaseCommand {

    private final MultiplierManager multiplierManager;

    public MultiplierCommand(HaumeaMC plugin) {
        super(plugin);
        this.multiplierManager = plugin.getMultiplierManager();
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        Player player = getPlayer();

        // Sem argumentos: abrir menu
        if (args.length == 0) {
            MultiplierMenu.open(plugin, player);
            return;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "status":
            case "info":
                showStatus(player);
                break;

            case "lista":
            case "list":
            case "inventario":
            case "inv":
                showInventory(player);
                break;

            case "ativar":
            case "activate":
            case "usar":
            case "use":
                if (args.length < 2) {
                    sendRaw("&c&lUSO: &f/multiplicador ativar <tipo>");
                    sendRaw("&7Tipos disponíveis: &f1.5, 2.0, 2.5, 3.0, 3.5");
                    return;
                }
                activateMultiplier(player, args[1]);
                break;

            case "menu":
                MultiplierMenu.open(plugin, player);
                break;

            case "ajuda":
            case "help":
            default:
                showHelp();
                break;
        }
    }

    /**
     * Mostra o status do multiplicador ativo
     */
    private void showStatus(Player player) {
        ActiveMultiplier active = multiplierManager.getActiveMultiplier(player);

        sendRaw("");
        sendRaw("&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        sendRaw("&6&l⚡ MULTIPLICADOR ATIVO");
        sendRaw("");

        if (active != null) {
            MultiplierType type = active.getType();
            sendRaw("&fTipo: " + type.getDisplayRarity() + " " + type.getDisplayMultiplier());
            sendRaw("&fBônus: &a+" + type.getBonusPercentage() + "% &7de coins por kill");
            sendRaw("&fTempo restante: &e" + active.getFormattedRemainingTime());
            sendRaw("");
            sendRaw("&7O tempo continua contando mesmo offline!");
        } else {
            sendRaw("&cVocê não possui nenhum multiplicador ativo.");
            sendRaw("");
            sendRaw("&7Use &e/multiplicador &7para abrir o menu");
            sendRaw("&7e ativar um multiplicador.");
        }

        sendRaw("");
        sendRaw("&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    /**
     * Mostra o inventário de multiplicadores
     */
    private void showInventory(Player player) {
        Map<MultiplierType, Integer> inventory = multiplierManager.getFullInventory(player);
        int total = inventory.values().stream().mapToInt(Integer::intValue).sum();

        sendRaw("");
        sendRaw("&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        sendRaw("&6&l⚡ SEUS MULTIPLICADORES");
        sendRaw("");

        if (total == 0) {
            sendRaw("&cVocê não possui nenhum multiplicador.");
            sendRaw("");
            sendRaw("&7Adquira multiplicadores na loja");
            sendRaw("&7ou em eventos especiais!");
        } else {
            for (MultiplierType type : MultiplierType.values()) {
                int count = inventory.getOrDefault(type, 0);
                if (count > 0) {
                    sendRaw("&f• " + type.getDisplayRarity() + " " + type.getDisplayMultiplier() +
                            " &7(+" + type.getBonusPercentage() + "%) &8- &a" + count + " disponível(is)");
                } else {
                    sendRaw("&8• " + type.getDisplayMultiplier() + " - 0 disponível");
                }
            }
            sendRaw("");
            sendRaw("&fTotal: &e" + total + " multiplicador(es)");
        }

        sendRaw("");
        sendRaw("&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    /**
     * Ativa um multiplicador pelo comando
     */
    private void activateMultiplier(Player player, String typeArg) {
        MultiplierType type = MultiplierType.fromString(typeArg);

        if (type == null) {
            sendRaw("&c&lTIPO INVÁLIDO!");
            sendRaw("&7Tipos disponíveis: &f1.5, 2.0, 2.5, 3.0, 3.5");
            return;
        }

        // Verificar se tem no inventário
        int available = multiplierManager.getInventoryCount(player, type);
        if (available <= 0) {
            ChatStorage.send(player, "multiplier.not-available", "type", type.getDisplayMultiplier());
            return;
        }

        // Ativar
        multiplierManager.activateMultiplier(player, type);
    }

    /**
     * Mostra a ajuda do comando
     */
    private void showHelp() {
        sendRaw("");
        sendRaw("&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        sendRaw("&6&l⚡ MULTIPLICADORES - AJUDA");
        sendRaw("");
        sendRaw("&e/multiplicador &8- &7Abre o menu");
        sendRaw("&e/multiplicador status &8- &7Mostra multiplicador ativo");
        sendRaw("&e/multiplicador lista &8- &7Lista seus multiplicadores");
        sendRaw("&e/multiplicador ativar <tipo> &8- &7Ativa um multiplicador");
        sendRaw("");
        sendRaw("&7Tipos: &f1.5, 2.0, 2.5, 3.0, 3.5");
        sendRaw("");
        sendRaw("&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }
}
