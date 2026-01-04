package com.haumea.kitpvp.commands.staff;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.commands.base.BaseCommand;
import com.haumea.kitpvp.commands.base.CommandInfo;
import com.haumea.kitpvp.utils.ChatStorage;
import com.haumea.kitpvp.utils.VisualManager;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.Arrays;

/**
 * Comando para reiniciar/recarregar o servidor com contagem regressiva.
 * 
 * Permite que staffs iniciem uma contagem regressiva para o reload,
 * avisando todos os jogadores com títulos e sons.
 * 
 * @author HaumeaMC
 */
@CommandInfo(name = "reiniciar", aliases = { "restart", "reload",
        "rl" }, description = "Reinicia o servidor com contagem", playerOnly = false, allowedGroups = { "dono",
                "diretor", "gerente" })
public class ReiniciarCommand extends BaseCommand {

    private static boolean restarting = false;
    private static int taskId = -1;
    private static String currentReason = null;

    public ReiniciarCommand(HaumeaMC plugin) {
        super(plugin);
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("cancelar")) {
            if (!restarting) {
                ChatStorage.send(sender, "command.reiniciar.not-starting");
                return;
            }
            cancelRestart();
            ChatStorage.send(sender, "command.reiniciar.cancelled");
            return;
        }

        if (restarting) {
            ChatStorage.send(sender, "command.reiniciar.already-starting");
            return;
        }

        int time = 10; // Default time
        String reason = null;

        // Parse arguments: [time] [reason...] or [reason...] (if first arg is not
        // number)
        if (args.length > 0) {
            boolean firstArgIsTime = false;
            try {
                time = Integer.parseInt(args[0]);
                firstArgIsTime = true;
            } catch (NumberFormatException e) {
                // First arg is not a number, so it's part of the reason
                firstArgIsTime = false;
            }

            if (firstArgIsTime) {
                // If first arg was time, check if there are more args for reason
                if (args.length > 1) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 1; i < args.length; i++) {
                        sb.append(args[i]).append(" ");
                    }
                    reason = sb.toString().trim();
                }
            } else {
                // If first arg was not time, all args are reason
                StringBuilder sb = new StringBuilder();
                for (String arg : args) {
                    sb.append(arg).append(" ");
                }
                reason = sb.toString().trim();
            }
        }

        startRestart(time, reason);

        // Feedback message
        if (reason != null) {
            ChatStorage.sendRaw(sender, "&aReinicialização iniciada em " + time + " segundos. Motivo: &f" + reason);
        } else {
            ChatStorage.sendRaw(sender, "&aReinicialização iniciada em " + time + " segundos.");
        }
    }

    private void cancelRestart() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
        restarting = false;
        currentReason = null;

        // Broadcast cancellation
        for (Player p : Bukkit.getOnlinePlayers()) {
            ChatStorage.sendRaw(p, "&c&lREINICIALIZAÇÃO CANCELADA!");
            VisualManager.sendTitle(p, "&c&lCANCELADO", "&fO reinício foi cancelado.", 10, 40, 10);
        }
    }

    private void startRestart(int time, String reason) {
        restarting = true;
        currentReason = reason;

        taskId = new BukkitRunnable() {
            int count = time;

            @Override
            public void run() {
                if (!restarting) {
                    this.cancel();
                    return;
                }

                if (count <= 0) {
                    ChatStorage.broadcast("command.reiniciar.now");

                    // Final title
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        VisualManager.sendTitle(p, "&4&lREINICIANDO", "&fO servidor está reiniciando...", 0, 100, 20);

                        // Manually kick players with custom message including reason
                        String kickMsgKey = "server.restart-kick";
                        String kickMsg = ChatStorage.getMessage(kickMsgKey);

                        if (currentReason != null) {
                            // If reason exists, we can append it or use a specific format
                            // Simple append for now:
                            kickMsg += "\n&7Motivo: &f" + currentReason;
                        }

                        p.kickPlayer(ChatStorage.colorize(kickMsg));
                    }

                    Bukkit.reload(); // Perform the reload
                    this.cancel();
                    restarting = false;
                    currentReason = null;
                    taskId = -1;
                    return;
                }

                // Smart broadcast logic to avoid flood
                boolean shouldBroadcast = false;

                if (count > 60 && count % 60 == 0) { // Every minute if > 60
                    shouldBroadcast = true;
                } else if (count == 60 || count == 30 || count == 15 || count == 10 || count <= 5) {
                    shouldBroadcast = true;
                }

                if (shouldBroadcast) {
                    // Formatação alinhada com a nova mensagem
                    String reasonSuffix = (currentReason != null) ? "\n\n        &c&lMOTIVO: &f" + currentReason : "";

                    // Broadcast message
                    ChatStorage.broadcast("command.reiniciar.countdown", "time", String.valueOf(count), "reason_suffix",
                            reasonSuffix);

                    // Titles
                    String title = ChatStorage.getMessage("command.reiniciar.countdown-title");
                    String subtitle = ChatStorage.getMessage("command.reiniciar.countdown-subtitle", "time",
                            String.valueOf(count));

                    for (Player p : Bukkit.getOnlinePlayers()) {
                        VisualManager.sendTitle(p, title, subtitle, 0, 25, 10);
                        p.playSound(p.getLocation(), Sound.NOTE_PLING, 1.0f, 2.0f);
                    }
                }

                count--;
            }
        }.runTaskTimer(plugin, 0L, 20L).getTaskId();
    }
}
