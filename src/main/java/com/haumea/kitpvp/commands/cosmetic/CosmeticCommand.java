package com.haumea.kitpvp.commands.cosmetic;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.commands.base.BaseCommand;
import com.haumea.kitpvp.commands.base.CommandInfo;
import com.haumea.kitpvp.managers.CosmeticManager;
import com.haumea.kitpvp.menu.cosmetic.CosmeticMainMenu;
import com.haumea.kitpvp.models.cosmetic.Cosmetic;
import com.haumea.kitpvp.models.cosmetic.CosmeticRarity;
import com.haumea.kitpvp.models.cosmetic.CosmeticType;
import com.haumea.kitpvp.utils.ChatStorage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Comando para gerenciar cosméticos.
 * 
 * Uso:
 * - /cosmeticos (abre menu)
 * - /cosmeticos toggle (ativa/desativa)
 * - /cosmeticos info (mostra status)
 * - /cosmeticos list (admin - lista todos)
 * - /cosmeticos give <player> <cosmetic> (admin)
 * 
 * @author HaumeaMC
 */
@CommandInfo(name = "cosmeticos", aliases = { "cosmetics", "cosmetic",
        "cosm" }, permission = "", playerOnly = true, description = "Gerencia seus cosmeticos")
public class CosmeticCommand extends BaseCommand {

    public CosmeticCommand(HaumeaMC plugin) {
        super(plugin);
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;
        CosmeticManager cosmeticManager = plugin.getCosmeticManager();

        if (cosmeticManager == null) {
            ChatStorage.sendRaw(player, "&c&lERRO &fSistema de cosmeticos nao disponivel!");
            return;
        }

        // Sem argumentos = abrir menu
        if (args.length == 0) {
            new CosmeticMainMenu(plugin, player).open();
            return;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "toggle":
            case "alternar":
                cosmeticManager.toggleCosmetics(player);
                break;

            case "info":
            case "status":
                showInfo(player, cosmeticManager);
                break;

            case "list":
            case "lista":
                if (!player.hasPermission("haumea.cosmetics.admin")) {
                    ChatStorage.sendRaw(player, "&c&lERRO &fSem permissao!");
                    return;
                }
                listCosmetics(player, cosmeticManager);
                break;

            case "give":
            case "dar":
                if (!player.hasPermission("haumea.cosmetics.admin")) {
                    ChatStorage.sendRaw(player, "&c&lERRO &fSem permissao!");
                    return;
                }
                if (args.length < 3) {
                    showGiveUsage(player);
                    return;
                }
                giveCosmetic(player, args, cosmeticManager);
                break;

            case "help":
            case "ajuda":
                showHelp(player);
                break;

            default:
                ChatStorage.sendRaw(player, "&c&lERRO &fSubcomando desconhecido! Use &e/cosmeticos ajuda");
                break;
        }
    }

    private void showInfo(Player player, CosmeticManager cosmeticManager) {
        ChatStorage.sendRaw(player, "");
        ChatStorage.sendRaw(player, "&8&m----------&r  &6&lSEUS COSMETICOS  &8&m----------");
        ChatStorage.sendRaw(player, "");

        boolean enabled = cosmeticManager.areCosmeticsEnabled(player);
        ChatStorage.sendRaw(player, "  &fStatus: " + (enabled ? "&aAtivado" : "&cDesativado"));
        ChatStorage.sendRaw(player, "  &fDesbloqueados: &e" + cosmeticManager.getUnlockedCount(player) +
                " &7/ " + cosmeticManager.getAllCosmetics().size());
        ChatStorage.sendRaw(player, "");

        // Mostrar equipados
        for (CosmeticType type : CosmeticType.values()) {
            String selectedId = cosmeticManager.getSelectedCosmetic(player, type);
            if (selectedId != null) {
                Cosmetic cosmetic = cosmeticManager.getCosmetic(selectedId);
                if (cosmetic != null) {
                    ChatStorage.sendRaw(player, "  " + type.getColorCode() + type.getDisplayName() +
                            "&f: " + cosmetic.getRarity().getFormattedColor() + cosmetic.getDisplayName());
                }
            }
        }

        ChatStorage.sendRaw(player, "");
        ChatStorage.sendRaw(player, "  &7Use &e/cosmeticos &7para abrir o menu!");
        ChatStorage.sendRaw(player, "");
        ChatStorage.sendRaw(player, "&8&m-----------------------------------------");
        ChatStorage.sendRaw(player, "");
    }

    private void listCosmetics(Player player, CosmeticManager cosmeticManager) {
        ChatStorage.sendRaw(player, "");
        ChatStorage.sendRaw(player, "&8&m----------&r  &6&lLISTA DE COSMETICOS  &8&m----------");
        ChatStorage.sendRaw(player, "");

        for (CosmeticType type : CosmeticType.values()) {
            List<Cosmetic> cosmetics = cosmeticManager.getCosmeticsByType(type);
            if (cosmetics.isEmpty())
                continue;

            ChatStorage.sendRaw(player,
                    type.getColorCode() + "&l" + type.getDisplayName() + " &7(" + cosmetics.size() + "):");
            for (Cosmetic cosmetic : cosmetics) {
                ChatStorage.sendRaw(player, "  &8- " + cosmetic.getRarity().getFormattedColor() +
                        cosmetic.getId() + " &7(" + cosmetic.getPrice() + " coins)");
            }
            ChatStorage.sendRaw(player, "");
        }

        ChatStorage.sendRaw(player, "&8&m----------------------------------------------");
    }

    private void giveCosmetic(Player player, String[] args, CosmeticManager cosmeticManager) {
        String targetName = args[1];
        String cosmeticId = args[2];

        Player target = plugin.getServer().getPlayer(targetName);
        if (target == null) {
            ChatStorage.sendRaw(player, "&c&lERRO &fJogador &e" + targetName + " &fnao encontrado!");
            return;
        }

        Cosmetic cosmetic = cosmeticManager.getCosmetic(cosmeticId);
        if (cosmetic == null) {
            ChatStorage.sendRaw(player, "&c&lERRO &fCosmetico &e" + cosmeticId + " &fnao encontrado!");
            return;
        }

        if (cosmeticManager.hasUnlocked(target, cosmeticId)) {
            ChatStorage.sendRaw(player, "&c&lERRO &f" + target.getName() + " &fja possui este cosmetico!");
            return;
        }

        cosmeticManager.unlockCosmetic(target, cosmeticId);

        ChatStorage.sendRaw(player, "&a&lSUCESSO &fCosmetico &e" + cosmetic.getDisplayName() +
                " &fdado para &e" + target.getName() + "&f!");
        ChatStorage.sendRaw(target, "&6&lCOSMETICOS &fVoce recebeu o cosmetico " +
                cosmetic.getRarity().getFormattedColor() + cosmetic.getDisplayName() + " &fde presente!");
    }

    private void showGiveUsage(Player player) {
        ChatStorage.sendRaw(player, "");
        ChatStorage.sendRaw(player, "&c&l  GIVE COSMETIC &8- &7Dar cosmetico a jogador");
        ChatStorage.sendRaw(player, "");
        ChatStorage.sendRaw(player, "&e  Uso: &f/cosmeticos give <jogador> <id>");
        ChatStorage.sendRaw(player, "");
        ChatStorage.sendRaw(player, "&6  Argumentos:");
        ChatStorage.sendRaw(player, "    &a jogador &8- &7Nome do jogador");
        ChatStorage.sendRaw(player, "    &e id &8- &7ID do cosmetico");
        ChatStorage.sendRaw(player, "");
        ChatStorage.sendRaw(player, "&6  Exemplo: &f/cosmeticos give Steve kill_flames");
        ChatStorage.sendRaw(player, "");
    }

    private void showHelp(Player player) {
        ChatStorage.sendRaw(player, "");
        ChatStorage.sendRaw(player, "&6&l  COSMETICOS &8- &7Personalize seus efeitos");
        ChatStorage.sendRaw(player, "");
        ChatStorage.sendRaw(player, "&e  Uso: &f/cosmeticos [subcomando]");
        ChatStorage.sendRaw(player, "");
        ChatStorage.sendRaw(player, "&6  Subcomandos:");
        ChatStorage.sendRaw(player, "    &a (nenhum) &8- &7Abre o menu de cosmeticos");
        ChatStorage.sendRaw(player, "    &e toggle &8- &7Ativa/desativa seus cosmeticos");
        ChatStorage.sendRaw(player, "    &b info &8- &7Mostra seus cosmeticos equipados");
        ChatStorage.sendRaw(player, "");
        if (player.hasPermission("haumea.cosmetics.admin")) {
            ChatStorage.sendRaw(player, "&c  Admin:");
            ChatStorage.sendRaw(player, "    &c list &8- &7Lista todos os cosmeticos");
            ChatStorage.sendRaw(player, "    &c give &8- &7Da cosmetico a jogador");
            ChatStorage.sendRaw(player, "");
        }
        ChatStorage.sendRaw(player, "&6  Aliases: &f/cosmetics, /cosm");
        ChatStorage.sendRaw(player, "");
    }
}
