package com.haumea.kitpvp.commands.essential;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.commands.base.BaseCommand;
import com.haumea.kitpvp.commands.base.CommandInfo;
import com.haumea.kitpvp.utils.ChatStorage;
import com.haumea.kitpvp.utils.VisualManager;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Comando /bc - Broadcast premium para o servidor
 * Envia mensagens com visual extremamente bonito no chat
 * e também exibe um título na tela de todos os jogadores.
 * 
 * @author HaumeaMC
 */
@CommandInfo(name = "bc", aliases = { "broadcast",
        "anuncio" }, description = "Envia uma mensagem global para o servidor", permission = "haumeamc.bc", usage = "/bc <mensagem>")
public class BcCommand extends BaseCommand {

    public BcCommand(HaumeaMC plugin) {
        super(plugin);
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("haumeamc.bc")) {
            ChatStorage.send(sender, "error.no-permission");
            return;
        }

        if (args.length < 1) {
            sendUsageHelp(sender);
            return;
        }

        // Juntar todos os argumentos em uma mensagem
        StringBuilder messageBuilder = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            messageBuilder.append(args[i]);
            if (i < args.length - 1) {
                messageBuilder.append(" ");
            }
        }

        String rawMessage = messageBuilder.toString();
        String formattedMessage = ChatStorage.colorize(rawMessage);

        // Obter nome do remetente
        String senderName = sender instanceof Player ? ((Player) sender).getName() : "CONSOLE";

        // Enviar broadcast premium para todos os jogadores
        sendPremiumBroadcast(formattedMessage, senderName);

        // Feedback para quem enviou
        if (sender instanceof Player) {
            ChatStorage.send(sender, "broadcast.sent", "count", String.valueOf(Bukkit.getOnlinePlayers().size()));
        }

        // Log no console
        Bukkit.getConsoleSender()
                .sendMessage(ChatStorage.colorize("&6&lBROADCAST &f[" + senderName + "] &7" + formattedMessage));
    }

    /**
     * Envia o broadcast com visual premium para todos os jogadores online
     */
    private void sendPremiumBroadcast(String message, String senderName) {
        // Obter configurações do messages.yml
        List<String> headerLines = ChatStorage.getMessageList("broadcast.format.header");
        String messageLine = ChatStorage.getMessage("broadcast.format.message");
        List<String> footerLines = ChatStorage.getMessageList("broadcast.format.footer");

        String title = ChatStorage.getMessage("broadcast.title.main");
        String subtitle = ChatStorage.getMessage("broadcast.title.subtitle");

        // Substituir placeholders
        if (messageLine == null || messageLine.isEmpty()) {
            messageLine = "&f{message}";
        }
        messageLine = messageLine.replace("{message}", message).replace("{sender}", senderName);

        if (subtitle != null) {
            subtitle = subtitle.replace("{message}", message).replace("{sender}", senderName);
        }

        // Enviar para todos os jogadores
        for (Player player : Bukkit.getOnlinePlayers()) {
            // Enviar linhas do header
            if (headerLines != null) {
                for (String line : headerLines) {
                    player.sendMessage(ChatStorage.colorize(line));
                }
            }

            // Enviar mensagem principal
            player.sendMessage(ChatStorage.colorize(messageLine));

            // Enviar linhas do footer
            if (footerLines != null) {
                for (String line : footerLines) {
                    player.sendMessage(ChatStorage.colorize(line));
                }
            }

            // Enviar título na tela
            if (title != null && !title.isEmpty()) {
                VisualManager.sendTitle(
                        player,
                        title,
                        subtitle != null ? subtitle : "",
                        10, 60, 20);
            }

            // Tocar som de notificação
            try {
                player.playSound(player.getLocation(), Sound.NOTE_PLING, 1.0f, 1.2f);
                // Segundo som mais grave para efeito
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline()) {
                        player.playSound(player.getLocation(), Sound.NOTE_PLING, 0.8f, 0.8f);
                    }
                }, 3L);
            } catch (Exception ignored) {
                // Som pode não existir em algumas versões
            }
        }
    }

    /**
     * Exibe ajuda de uso do comando
     */
    private void sendUsageHelp(CommandSender sender) {
        sendRaw("");
        sendRaw("&6&l  BROADCAST &8- &7Envie avisos para o servidor");
        sendRaw("");
        sendRaw("&e  Uso: &f/bc <mensagem>");
        sendRaw("");
        sendRaw("&6  Informações:");
        sendRaw("    &7★ &fUse &e& &fpara cores (ex: &a&aVerde&f)");
        sendRaw("    &7★ &fA mensagem aparece no &echat &fe na &etela");
        sendRaw("    &7★ &fTodos os jogadores receberão um &esom");
        sendRaw("");
        sendRaw("&6  Exemplo: &f/bc &a&lEvento de PvP iniciado!");
        sendRaw("");
    }
}
