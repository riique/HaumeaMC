package com.haumea.kitpvp.commands.essential;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.commands.base.BaseCommand;
import com.haumea.kitpvp.commands.base.CommandInfo;
import com.haumea.kitpvp.models.PlayerData;
import com.haumea.kitpvp.profile.PlayerProfile;
import com.haumea.kitpvp.utils.ChatStorage;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Comando /tempo - Mostra o tempo de jogo total do jogador
 * 
 * @author HaumeaMC
 */
@CommandInfo(name = "tempo", aliases = { "playtime", "tempodejogo",
        "played" }, description = "Mostra seu tempo de jogo ou de outro jogador", playerOnly = false, usage = "/tempo [jogador]")
public class TempoCommand extends BaseCommand {

    public TempoCommand(HaumeaMC plugin) {
        super(plugin);
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            // Mostrar próprio tempo de jogo
            if (!(sender instanceof Player)) {
                sendRaw("&cUse: /tempo <jogador>");
                return;
            }

            Player player = (Player) sender;
            PlayerProfile profile = plugin.getProfileManager().getProfile(player);

            if (profile == null) {
                sendRaw("&cErro ao carregar seus dados.");
                return;
            }

            // Tempo total = playTime salvo + sessão atual
            long totalTime = profile.getData().getPlayTime() + profile.getSessionTime();
            showPlaytime(sender, player.getName(), totalTime, profile.getSessionTime());
        } else {
            // Mostrar tempo de jogo de outro jogador
            String targetName = args[0];
            Player target = Bukkit.getPlayer(targetName);

            if (target != null && target.isOnline()) {
                // Jogador online - incluir sessão atual
                PlayerProfile profile = plugin.getProfileManager().getProfile(target);

                if (profile == null) {
                    sendRaw("&cErro ao carregar dados do jogador.");
                    return;
                }

                long totalTime = profile.getData().getPlayTime() + profile.getSessionTime();
                showPlaytime(sender, target.getName(), totalTime, profile.getSessionTime());
            } else {
                // Jogador offline - carregar do banco de dados de forma assíncrona
                final String searchName = targetName;
                final CommandSender finalSender = sender;

                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    PlayerData data = plugin.getProfileManager().getRepository().findByName(searchName);

                    if (data == null) {
                        Bukkit.getScheduler().runTask(plugin, () -> ChatStorage.sendRaw(finalSender,
                                "&cJogador &e" + searchName + " &cnao encontrado."));
                        return;
                    }

                    final String name = data.getLastKnownName();
                    final long playTime = data.getPlayTime();

                    Bukkit.getScheduler().runTask(plugin, () -> showPlaytime(finalSender, name, playTime, 0));
                });
            }
        }
    }

    /**
     * Exibe o tempo de jogo formatado
     * 
     * @param sender     Quem receberá a mensagem
     * @param playerName Nome do jogador
     * @param totalMs    Tempo total em milissegundos
     * @param sessionMs  Tempo da sessão atual em milissegundos (0 se offline)
     */
    private void showPlaytime(CommandSender sender, String playerName, long totalMs, long sessionMs) {
        String formattedTotal = formatTime(totalMs);
        String formattedSession = formatTime(sessionMs);

        // Calcular estatísticas
        long totalHours = totalMs / 3600000L;
        long totalDays = totalMs / 86400000L;

        sendRaw("");
        sendRaw("&8&m----------------------------------------");
        sendRaw("");
        sendRaw("  &6&lTEMPO DE JOGO &8- &e" + playerName);
        sendRaw("");
        sendRaw("  &aTempo Total: &f" + formattedTotal);

        if (sessionMs > 0) {
            sendRaw("  &eSessao Atual: &f" + formattedSession);
        } else {
            sendRaw("  &7Status: &cOffline");
        }

        sendRaw("");
        sendRaw("  &6Estatisticas:");
        sendRaw("    &e" + totalHours + " &7horas totais");
        sendRaw("    &e" + totalDays + " &7dias equivalentes");
        sendRaw("");
        sendRaw("&8&m----------------------------------------");
        sendRaw("");
    }

    /**
     * Formata tempo em milissegundos para formato legível
     * Formato: Xd Xh Xm Xs
     */
    private String formatTime(long ms) {
        if (ms <= 0) {
            return "0s";
        }

        long seconds = ms / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        seconds %= 60;
        minutes %= 60;
        hours %= 24;

        StringBuilder sb = new StringBuilder();

        if (days > 0) {
            sb.append("&e").append(days).append("&7d ");
        }
        if (hours > 0 || days > 0) {
            sb.append("&e").append(hours).append("&7h ");
        }
        if (minutes > 0 || hours > 0 || days > 0) {
            sb.append("&e").append(minutes).append("&7m ");
        }
        sb.append("&e").append(seconds).append("&7s");

        return ChatStorage.colorize(sb.toString().trim());
    }
}
