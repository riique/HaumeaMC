package com.haumea.kitpvp.commands.essential;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.commands.base.BaseCommand;
import com.haumea.kitpvp.commands.base.CommandInfo;
import com.haumea.kitpvp.profile.PlayerProfile;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Comando /score - Ativa ou desativa a scoreboard
 * 
 * @author HaumeaMC
 */
@CommandInfo(name = "score", aliases = { "scoreboard",
        "sb" }, description = "Ativa ou desativa a scoreboard", playerOnly = true, usage = "/score <on/off>")
public class ScoreCommand extends BaseCommand {

    private static final String PREFIX = "§6§lSCOREBOARD §f";
    private static final String MSG_USAGE = PREFIX + "Utilize: /score <on> <off>";
    private static final String MSG_ACTIVATED = PREFIX + "Você §a§lATIVOU §fsua §e§lSCOREBOARD§f!";
    private static final String MSG_ALREADY_ACTIVE = PREFIX + "Sua §e§lSCOREBOARD §fjá está §a§lATIVADA§f!";
    private static final String MSG_DEACTIVATED = PREFIX + "Você §c§lDESATIVOU §fsua §e§lSCOREBOARD§f!";
    private static final String MSG_ALREADY_INACTIVE = PREFIX + "Sua §e§lSCOREBOARD §fjá está §c§lDESATIVADA§f!";

    public ScoreCommand(HaumeaMC plugin) {
        super(plugin);
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        Player player = getPlayer();

        if (args.length < 1) {
            sendRaw(MSG_USAGE);
            return;
        }

        String action = args[0].toLowerCase();

        // Obter preferência atual do jogador
        PlayerProfile profile = plugin.getProfileManager().getProfile(player);
        boolean currentState = true; // Padrão: ativada

        if (profile != null) {
            // Obter preferência salva (customData)
            Object savedState = profile.getData().getCustomData("scoreboard_enabled");
            if (savedState instanceof Boolean) {
                currentState = (Boolean) savedState;
            }
        }

        switch (action) {
            case "on":
            case "ativar":
            case "ligar":
                if (currentState && plugin.getScoreboardManager().hasBoard(player)) {
                    sendRaw(MSG_ALREADY_ACTIVE);
                    return;
                }

                // Ativar scoreboard
                if (profile != null) {
                    profile.getData().setCustomData("scoreboard_enabled", true);
                }
                plugin.getScoreboardManager().createBoard(player);
                sendRaw(MSG_ACTIVATED);
                break;

            case "off":
            case "desativar":
            case "desligar":
                if (!currentState && !plugin.getScoreboardManager().hasBoard(player)) {
                    sendRaw(MSG_ALREADY_INACTIVE);
                    return;
                }

                // Desativar scoreboard
                if (profile != null) {
                    profile.getData().setCustomData("scoreboard_enabled", false);
                }
                plugin.getScoreboardManager().removeBoard(player);
                sendRaw(MSG_DEACTIVATED);
                break;

            default:
                sendRaw(MSG_USAGE);
                break;
        }
    }
}
