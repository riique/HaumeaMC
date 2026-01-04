package com.haumea.kitpvp.commands.skin;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.commands.base.BaseCommand;
import com.haumea.kitpvp.commands.base.CommandInfo;
import com.haumea.kitpvp.managers.SkinManager;
import com.haumea.kitpvp.menu.skin.SkinMenu;
import com.haumea.kitpvp.utils.ChatStorage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Comando /skin para gerenciar skins de jogadores.
 * 
 * Permite aos jogadores alterar sua aparência para a de
 * qualquer conta Premium do Minecraft.
 * 
 * Uso:
 * - /skin - Abre menu de skins
 * - /skin <nick> - Aplica a skin de um jogador premium
 * - /skin reset - Restaura a skin original
 * 
 * @author HaumeaMC
 */
@CommandInfo(name = "skin", aliases = {
        "skins" }, description = "Altera sua skin", usage = "/skin [nick|reset]", playerOnly = true)
public class SkinCommand extends BaseCommand {

    private final SkinManager skinManager;

    public SkinCommand(HaumeaMC plugin) {
        super(plugin);
        this.skinManager = plugin.getSkinManager();
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        Player player = getPlayer();

        if (args.length == 0) {
            // Sem argumentos: abrir menu de skins
            new SkinMenu(plugin, player).open();
            return;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "reset":
            case "resetar":
            case "original":
                handleReset(player);
                break;

            case "help":
            case "ajuda":
                sendHelp(player);
                break;

            default:
                // Argumento é um nome de jogador
                handleApply(player, args[0]);
                break;
        }
    }

    /**
     * Aplica uma skin de outro jogador
     */
    private void handleApply(Player player, String skinName) {
        // Verificar cooldown
        if (skinManager.isOnCooldown(player)) {
            long remaining = skinManager.getCooldownRemaining(player) / 1000;
            ChatStorage.send(player, "skin.cooldown", "time", String.valueOf(remaining));
            return;
        }

        // Informar que está buscando
        ChatStorage.send(player, "skin.searching", "skin", skinName);

        // Aplicar skin assincronamente
        skinManager.applySkinAsync(player, skinName, success -> {
            if (success) {
                ChatStorage.send(player, "skin.applied", "skin", skinName);
            } else {
                ChatStorage.send(player, "skin.not-found", "skin", skinName);
            }
        });
    }

    /**
     * Reseta a skin para a original
     */
    private void handleReset(Player player) {
        // Verificar cooldown
        if (skinManager.isOnCooldown(player)) {
            long remaining = skinManager.getCooldownRemaining(player) / 1000;
            ChatStorage.send(player, "skin.cooldown", "time", String.valueOf(remaining));
            return;
        }

        ChatStorage.send(player, "skin.resetting");
        skinManager.resetSkin(player);
    }

    /**
     * Envia ajuda do comando
     */
    private void sendHelp(Player player) {
        ChatStorage.sendRaw(player, "&8&m----------------------------------------");
        ChatStorage.sendRaw(player, "");
        player.sendMessage(ChatStorage.getMessage("skin.help.header"));
        ChatStorage.sendRaw(player, "");
        ChatStorage.sendRaw(player, "  &f/skin &7- Abre o menu de skins");
        ChatStorage.sendRaw(player, "  &f/skin <nick> &7- Usa a skin de um jogador premium");
        ChatStorage.sendRaw(player, "  &f/skin reset &7- Restaura sua skin original");
        ChatStorage.sendRaw(player, "");
        ChatStorage.sendRaw(player, "  &7Dica: Use nicks de jogadores premium como");
        ChatStorage.sendRaw(player, "  &7Dream, Notch, AuthenticGames, etc.");
        ChatStorage.sendRaw(player, "");
        ChatStorage.sendRaw(player, "&8&m----------------------------------------");
    }
}
