package com.haumea.kitpvp.commands.staff;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.commands.base.BaseCommand;
import com.haumea.kitpvp.commands.base.CommandInfo;
import com.haumea.kitpvp.utils.ChatStorage;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Comando de alteração de modo de jogo.
 * Aceita números (0, 1, 2, 3) ou nomes.
 * 
 * @author HaumeaMC
 */
@CommandInfo(name = "gm", aliases = {
        "gamemode" }, description = "Altera seu modo de jogo", usage = "/gm <0|1|2|3|survival|creative|adventure|spectator>", playerOnly = true, allowedGroups = {
                "dono", "diretor", "gerente" })
public class GameModeCommand extends BaseCommand {

    public GameModeCommand(HaumeaMC plugin) {
        super(plugin);
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        Player player = getPlayer();

        if (!hasArgs(1)) {
            sendUsage();
            return;
        }

        String input = args[0].toLowerCase();
        GameMode targetMode = parseGameMode(input);

        if (targetMode == null) {
            ChatStorage.send(player, "staff.gamemode-invalid", "input", input);
            return;
        }

        player.setGameMode(targetMode);

        String modeName = getGameModeName(targetMode);
        ChatStorage.send(player, "staff.gamemode-changed", "mode", modeName);
    }

    /**
     * Converte input em GameMode
     */
    private GameMode parseGameMode(String input) {
        switch (input) {
            case "0":
            case "survival":
            case "s":
                return GameMode.SURVIVAL;
            case "1":
            case "creative":
            case "c":
                return GameMode.CREATIVE;
            case "2":
            case "adventure":
            case "a":
                return GameMode.ADVENTURE;
            case "3":
            case "spectator":
            case "sp":
                return GameMode.SPECTATOR;
            default:
                return null;
        }
    }

    /**
     * Obtém nome amigável do modo com cores
     */
    private String getGameModeName(GameMode mode) {
        switch (mode) {
            case SURVIVAL:
                return "§a§lSURVIVAL";
            case CREATIVE:
                return "§e§lCREATIVE";
            case ADVENTURE:
                return "§b§lADVENTURE";
            case SPECTATOR:
                return "§7§lSPECTATOR";
            default:
                return mode.name();
        }
    }
}
