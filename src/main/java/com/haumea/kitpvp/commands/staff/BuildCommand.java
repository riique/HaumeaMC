package com.haumea.kitpvp.commands.staff;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.commands.base.BaseCommand;
import com.haumea.kitpvp.commands.base.CommandInfo;
import com.haumea.kitpvp.profile.PlayerProfile;
import com.haumea.kitpvp.utils.ChatStorage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Comando para ativar modo de construção.
 * Permite que o staff ignore a proteção global de blocos.
 * 
 * @author HaumeaMC
 */
@CommandInfo(name = "build", aliases = {
        "construir" }, description = "Ativa ou desativa o modo de construção", playerOnly = true, allowedGroups = {
                "dono", "diretor", "gerente" })
public class BuildCommand extends BaseCommand {

    public BuildCommand(HaumeaMC plugin) {
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

        boolean newState = !profile.isBuildMode();
        profile.setBuildMode(newState);

        String status = newState ? "ativado" : "desativado";
        ChatStorage.send(player, "staff.build-toggle", "status", status);
    }
}
