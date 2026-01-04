package com.haumea.kitpvp.commands.staff;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.commands.base.BaseCommand;
import com.haumea.kitpvp.commands.base.CommandInfo;
import com.haumea.kitpvp.profile.PlayerProfile;
import com.haumea.kitpvp.utils.ChatStorage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Comando para ativar/desativar modo StaffChat.
 * Quando ativo, as mensagens do jogador vão apenas para a equipe.
 * 
 * @author HaumeaMC
 */
@CommandInfo(name = "sc", aliases = { "s",
        "staffchat" }, description = "Entra ou sai do staffchat", playerOnly = true, allowedGroups = { "dono",
                "diretor", "gerente" })
public class StaffChatCommand extends BaseCommand {

    public StaffChatCommand(HaumeaMC plugin) {
        super(plugin);
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        Player player = getPlayer();
        PlayerProfile profile = plugin.getProfileManager().getProfile(player);

        if (profile == null) {
            ChatStorage.send(player, "error.profile-load");
            return;
        }

        boolean newState = !profile.isStaffChatMode();
        profile.setStaffChatMode(newState);

        if (newState) {
            ChatStorage.send(player, "staff.staffchat-enter");
        } else {
            ChatStorage.send(player, "staff.staffchat-exit");
        }
    }
}
