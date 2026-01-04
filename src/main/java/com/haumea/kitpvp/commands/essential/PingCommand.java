package com.haumea.kitpvp.commands.essential;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.commands.base.BaseCommand;
import com.haumea.kitpvp.commands.base.CommandInfo;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Comando /ping - Mostra o ping/latência
 * 
 * @author HaumeaMC
 */
@CommandInfo(name = "ping", aliases = { "latency",
        "latencia" }, description = "Mostra seu ping ou de outro jogador", playerOnly = true, usage = "/ping [jogador]")
public class PingCommand extends BaseCommand {

    private static final String PREFIX = "§a§lPING §a";
    private static final String MSG_OFFLINE = "§f§lOFFLINE §fO jogador §7(§e%player%§7) §festá offline.";

    public PingCommand(HaumeaMC plugin) {
        super(plugin);
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        Player player = getPlayer();

        if (args.length == 0) {
            // Mostrar ping próprio
            int ping = getPing(player);
            sendRaw(PREFIX + "Seu ping é de §7(§f" + ping + " ms§7)");
        } else {
            // Mostrar ping de outro jogador
            String targetName = args[0];
            Player target = Bukkit.getPlayer(targetName);

            if (target == null || !target.isOnline()) {
                sendRaw(MSG_OFFLINE.replace("%player%", targetName));
                return;
            }

            int ping = getPing(target);
            sendRaw(PREFIX + "O ping do(a) jogador(a) §7(§f" + target.getName() + "§7) §aé de §7(§f" + ping + " ms§7)");
        }
    }

    /**
     * Obtém o ping de um jogador usando reflexão (compatível com 1.8+)
     * 
     * @param player Jogador
     * @return Ping em millisegundos
     */
    private int getPing(Player player) {
        try {
            // Obter CraftPlayer
            Object craftPlayer = player.getClass().getMethod("getHandle").invoke(player);

            // Tentar obter o campo 'ping' (1.8 - 1.16)
            try {
                Field pingField = craftPlayer.getClass().getField("ping");
                return pingField.getInt(craftPlayer);
            } catch (NoSuchFieldException e) {
                // 1.17+ usa connection.m ou e (latência)
                try {
                    // Tentar pegar via PlayerConnection
                    Field connectionField = craftPlayer.getClass().getField("playerConnection");
                    Object connection = connectionField.get(craftPlayer);

                    // Tentar obter latência do connection
                    Method getLatencyMethod = connection.getClass().getMethod("getLatency");
                    return (int) getLatencyMethod.invoke(connection);
                } catch (Exception ex) {
                    // Fallback: retornar -1
                    return -1;
                }
            }
        } catch (Exception e) {
            return -1;
        }
    }
}
