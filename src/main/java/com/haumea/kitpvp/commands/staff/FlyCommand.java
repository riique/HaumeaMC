package com.haumea.kitpvp.commands.staff;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.commands.base.BaseCommand;
import com.haumea.kitpvp.commands.base.CommandInfo;
import com.haumea.kitpvp.utils.ChatStorage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Comando para alternar modo de voo.
 * 
 * @author HaumeaMC
 */
@CommandInfo(name = "fly", aliases = {
        "voar" }, description = "Ativa ou desativa o modo de voo", playerOnly = true, allowedGroups = { "dono",
                "diretor", "gerente" })
public class FlyCommand extends BaseCommand {

    public FlyCommand(HaumeaMC plugin) {
        super(plugin);
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        Player player = getPlayer();

        boolean newState = !player.getAllowFlight();
        player.setAllowFlight(newState);

        if (newState) {
            player.setFlying(true);
        }

        String status = newState ? "ativado" : "desativado";
        ChatStorage.send(player, "staff.fly-toggle", "status", status);
    }
}
