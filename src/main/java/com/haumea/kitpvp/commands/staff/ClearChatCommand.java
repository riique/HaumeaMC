package com.haumea.kitpvp.commands.staff;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.commands.base.BaseCommand;
import com.haumea.kitpvp.commands.base.CommandInfo;
import com.haumea.kitpvp.utils.ChatStorage;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Comando para limpar o chat global.
 * Envia 100 linhas em branco para todos os jogadores.
 * 
 * @author HaumeaMC
 */
@CommandInfo(name = "cc", aliases = { "clearchat",
        "limparchat" }, description = "Limpa o chat do servidor", allowedGroups = { "dono", "diretor", "gerente" })
public class ClearChatCommand extends BaseCommand {

    public ClearChatCommand(HaumeaMC plugin) {
        super(plugin);
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        // Enviar 100 linhas em branco para TODOS os jogadores
        for (Player player : Bukkit.getOnlinePlayers()) {
            for (int i = 0; i < 100; i++) {
                player.sendMessage("");
            }
        }

        // Anúncio global
        String message = ChatStorage.getMessage("staff.chat-cleared");
        Bukkit.broadcastMessage(message);
    }
}
