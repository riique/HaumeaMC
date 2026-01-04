package com.haumea.kitpvp.commands.essential;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.commands.base.BaseCommand;
import com.haumea.kitpvp.commands.base.CommandInfo;
import org.bukkit.command.CommandSender;

/**
 * Comando /regras - Mostra as regras do servidor
 * 
 * @author HaumeaMC
 */
@CommandInfo(name = "regras", aliases = { "rules" }, description = "Mostra as regras do servidor")
public class RegrasCommand extends BaseCommand {

    public RegrasCommand(HaumeaMC plugin) {
        super(plugin);
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        sendRaw("");
        sendRaw("§f§l-= §6§lREGRAS DO HAUMEAMC §f§l=-");
        sendRaw("");
        sendRaw("§f1. Sem uso de Hack/Cheats.");
        sendRaw("§f2. Respeite a Staff e os jogadores.");
        sendRaw("§f3. Sem divulgação de outros servidores.");
        sendRaw("§f4. Divirta-se!");
        sendRaw("");
    }
}
