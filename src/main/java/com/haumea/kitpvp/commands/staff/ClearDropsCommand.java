package com.haumea.kitpvp.commands.staff;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.commands.base.BaseCommand;
import com.haumea.kitpvp.commands.base.CommandInfo;
import com.haumea.kitpvp.utils.ChatStorage;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;

/**
 * Comando para remover itens dropados no chão.
 * Limpa todas as entidades do tipo Item de todos os mundos.
 * 
 * @author HaumeaMC
 */
@CommandInfo(name = "cleardrops", aliases = {
        "limparitens" }, description = "Remove todos os itens do chão", allowedGroups = { "dono", "diretor",
                "gerente" })
public class ClearDropsCommand extends BaseCommand {

    public ClearDropsCommand(HaumeaMC plugin) {
        super(plugin);
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        int removedCount = 0;

        // Iterar por todos os mundos
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof Item) {
                    entity.remove();
                    removedCount++;
                }
            }
        }

        ChatStorage.send(sender, "staff.drops-cleared", "count", String.valueOf(removedCount));
    }
}
