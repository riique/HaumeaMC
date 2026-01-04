package com.haumea.kitpvp.commands.essential;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.commands.base.BaseCommand;
import com.haumea.kitpvp.commands.base.CommandInfo;
import com.haumea.kitpvp.utils.ChatStorage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Comando /lobby - Envia o jogador de volta para o servidor de Lobby.
 * Usa o canal BungeeCord para transferir o jogador.
 * 
 * @author HaumeaMC
 */
@CommandInfo(name = "lobby", aliases = { "hub", "l" }, permission = "", playerOnly = true)
public class LobbyCommand extends BaseCommand {

    public LobbyCommand(HaumeaMC plugin) {
        super(plugin);
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;

        // Verificar se está em combate
        if (plugin.getStateManager() != null && plugin.getStateManager().isInCombat(player)) {
            ChatStorage.sendRaw(player, "&c&lCOMBATE &fVocê não pode ir ao lobby durante o combate!");
            return;
        }

        // Enviar mensagem
        ChatStorage.sendRaw(player, "&a&lLOBBY &fConectando ao lobby...");

        // Enviar para o lobby via BungeeCord
        sendToServer(player, "lobby");
    }

    /**
     * Envia o jogador para outro servidor via BungeeCord/Velocity
     */
    private void sendToServer(Player player, String serverName) {
        try {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("Connect");
            out.writeUTF(serverName);
            player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
        } catch (Exception e) {
            ChatStorage.sendRaw(player, "&cErro ao conectar ao servidor. Tente novamente.");
            plugin.getLogger().warning("[Lobby] Erro ao enviar jogador para " + serverName + ": " + e.getMessage());
        }
    }
}
