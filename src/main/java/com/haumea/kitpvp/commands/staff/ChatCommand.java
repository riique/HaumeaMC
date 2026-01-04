package com.haumea.kitpvp.commands.staff;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.commands.base.BaseCommand;
import com.haumea.kitpvp.commands.base.CommandInfo;
import com.haumea.kitpvp.managers.ChatManager;
import com.haumea.kitpvp.utils.ChatStorage;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

/**
 * Comando para ativar/desativar chat global.
 * Quando desativado, apenas staff pode falar no chat.
 * 
 * @author HaumeaMC
 */
@CommandInfo(name = "chat", aliases = { "mural" }, description = "Ativa ou desativa o chat global", allowedGroups = {
        "dono", "diretor", "gerente" })
public class ChatCommand extends BaseCommand {

    public ChatCommand(HaumeaMC plugin) {
        super(plugin);
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        ChatManager chatManager = plugin.getChatManager();

        boolean newState = !chatManager.isChatEnabled();
        chatManager.setChatEnabled(newState);

        String status = newState ? "ativado" : "desativado";
        String message = ChatStorage.getMessage("staff.chat-toggle", "status", status);
        Bukkit.broadcastMessage(message);
    }
}
