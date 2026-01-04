package com.haumea.kitpvp.commands;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.commands.base.BaseCommand;
import com.haumea.kitpvp.commands.base.CommandInfo;
import com.haumea.kitpvp.menu.kit.KitTypeMenu;
import com.haumea.kitpvp.utils.ChatStorage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Comando /kit - Abre o menu de seleção de kits.
 * 
 * Uso:
 * - /kit - Abre o menu de seleção de kits
 * - /kit <nome> - Seleciona um kit primário diretamente
 * 
 * @author HaumeaMC
 */
@CommandInfo(name = "kit", aliases = { "kits", "abilities", "habilidades" }, permission = "", playerOnly = true)
public class KitCommand extends BaseCommand {

    public KitCommand(HaumeaMC plugin) {
        super(plugin);
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;

        // Verificar se o jogador está em área protegida (spawn)
        if (plugin.getStateManager() != null && !plugin.getStateManager().isProtected(player)) {
            // Verificar se está em combate
            if (plugin.getStateManager().isInCombat(player)) {
                ChatStorage.sendCustom(player, "§cVocê não pode trocar de kit em combate!");
                return;
            }
        }

        // Se nenhum argumento, abrir menu
        if (args.length == 0) {
            new KitTypeMenu(plugin, player).open();
            return;
        }

        // Se argumento fornecido, tentar selecionar kit diretamente
        String kitName = args[0].toLowerCase();

        // Verificar se o kit existe
        if (plugin.getAbilityManager() == null || plugin.getAbilityManager().getAbility(kitName) == null) {
            // Verificar no KitManager tradicional
            if (plugin.getKitManager() == null || plugin.getKitManager().getKit(kitName) == null) {
                ChatStorage.sendCustom(player, "§cKit não encontrado: §e" + kitName);
                return;
            }
        }

        // Verificar acesso via permissão ou aluguel
        if (!plugin.getKitManager().hasKitPermission(player, kitName) &&
                !plugin.getKitManager().hasRentedKit(player.getUniqueId(), kitName)) {
            ChatStorage.sendCustom(player, "§cVocê não tem acesso a este kit!");
            return;
        }

        // Verificar compatibilidade com kit secundário
        String secondaryKit = plugin.getKitManager().getSecondaryKit(player);
        if (secondaryKit != null && !plugin.getKitManager().areKitsCompatible(kitName, secondaryKit)) {
            ChatStorage.sendCustom(player, "§cEste kit é incompatível com seu kit secundário!");
            return;
        }

        // Selecionar como kit primário
        plugin.getKitManager().setPrimaryKit(player, kitName);

        // Obter nome formatado
        String displayName = kitName;
        if (plugin.getAbilityManager() != null && plugin.getAbilityManager().getAbility(kitName) != null) {
            displayName = plugin.getAbilityManager().getAbility(kitName).getDisplayName();
        }

        ChatStorage.sendCustom(player, "§aVocê selecionou o kit " + displayName + "§a como primário!");
    }
}
