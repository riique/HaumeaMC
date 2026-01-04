package com.haumea.kitpvp.commands.staff;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.commands.base.BaseCommand;
import com.haumea.kitpvp.commands.base.CommandInfo;
import com.haumea.kitpvp.profile.PlayerProfile;
import com.haumea.kitpvp.utils.ChatStorage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Comando para alternar modo de imortalidade (god mode).
 * Quando ativado, o jogador não toma dano de nenhuma fonte:
 * - PvP (outros jogadores)
 * - Lava, fogo, cacto
 * - Queda, void
 * - Qualquer outra fonte de dano
 * 
 * @author HaumeaMC
 */
@CommandInfo(name = "god", aliases = { "imortal",
        "deus" }, description = "Ativa ou desativa o modo imortal", playerOnly = true, allowedGroups = { "dono",
                "diretor", "gerente", "admin", "mod", "helper" })
public class GodCommand extends BaseCommand {

    public GodCommand(HaumeaMC plugin) {
        super(plugin);
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        Player player = getPlayer();
        PlayerProfile profile = plugin.getProfileManager().getProfile(player);

        if (profile == null) {
            ChatStorage.send(player, "error.no-permission");
            return;
        }

        // Toggle god mode
        boolean newState = !profile.isGodMode();
        profile.setGodMode(newState);

        String status = newState ? "&a&lATIVADO" : "&c&lDESATIVADO";
        ChatStorage.send(player, "staff.god-toggle", "status", status);
    }
}
