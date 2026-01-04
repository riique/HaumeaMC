package com.haumea.kitpvp.commands.punishment;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.commands.base.BaseCommand;
import com.haumea.kitpvp.commands.base.CommandInfo;
import com.haumea.kitpvp.managers.PunishmentManager;
import com.haumea.kitpvp.models.Punishment;
import com.haumea.kitpvp.models.Punishment.PunishmentType;
import com.haumea.kitpvp.utils.ChatStorage;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Comando de banimento do HaumeaMC
 * 
 * Uso: /ban <nick> <motivo> [tempo] [prova]
 * - Prova é obrigatória para staff que não são ADMIN
 * - Tempo opcional (padrão: permanente)
 * - Bane também o IP do jogador
 * 
 * @author HaumeaMC
 */
@CommandInfo(name = "ban", description = "Bane um jogador do servidor", usage = "/ban <nick> <motivo> [tempo] [prova]", allowedGroups = {
        "dono", "diretor", "gerente", "admin", "moderador", "mod", "helper" })
public class BanCommand extends BaseCommand {

    public BanCommand(HaumeaMC plugin) {
        super(plugin);
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendUsageMessage(sender);
            return;
        }

        String targetName = args[0];
        String reason = args[1];
        String timeArg = args.length >= 3 ? args[2] : null;
        String proof = args.length >= 4 ? args[3] : null;

        // Verificar se precisa de prova
        PunishmentManager punishmentManager = plugin.getPunishmentManager();
        Player staffPlayer = sender instanceof Player ? (Player) sender : null;
        boolean isAdmin = punishmentManager.isAdmin(staffPlayer);

        // Staff que não são ADMIN precisam de prova válida
        if (!isAdmin) {
            if (proof == null || !punishmentManager.isValidProof(proof)) {
                ChatStorage.send(sender, "punishment.ban.proof-required");
                ChatStorage.send(sender, "punishment.ban.usage-hint");
                return;
            }
        }

        // Se não tem prova, definir como "Não informado" (só admins)
        if (proof == null) {
            proof = "Não informado";
        }

        // Parsear tempo
        long duration = 0; // Permanente por padrão
        if (timeArg != null) {
            duration = punishmentManager.parseTime(timeArg);
            if (duration < 0) {
                ChatStorage.send(sender, "punishment.ban.invalid-time");
                return;
            }
        }

        // Buscar jogador (online ou offline)
        Player targetOnline = Bukkit.getPlayer(targetName);
        OfflinePlayer targetOffline = Bukkit.getOfflinePlayer(targetName);

        if (targetOffline == null || !targetOffline.hasPlayedBefore() && targetOnline == null) {
            ChatStorage.send(sender, "punishment.ban.unknown-player", "player", targetName);
            return;
        }

        // Obter dados do alvo
        java.util.UUID targetUuid = targetOffline.getUniqueId();
        String playerName = targetOnline != null ? targetOnline.getName() : targetOffline.getName();

        // Obter IP do jogador (se online)
        String targetIp = "";
        if (targetOnline != null && targetOnline.getAddress() != null) {
            targetIp = targetOnline.getAddress().getAddress().getHostAddress();
        }

        // Verificar se o alvo é DONO (protegido)
        if (punishmentManager.isProtected(targetUuid)) {
            sendBlockedOwnerMessage(sender, playerName);

            // Notificar todos os donos online
            String staffName = staffPlayer != null ? staffPlayer.getName() : "Console";
            punishmentManager.notifyOwners(staffName, playerName, "BANIR");
            return;
        }

        // Verificar se já está banido
        Punishment existingBan = punishmentManager.getActiveBan(targetUuid);
        if (existingBan != null) {
            ChatStorage.send(sender, "punishment.ban.already-banned");
            ChatStorage.send(sender, "punishment.common.current-reason", "reason", existingBan.getReason());
            ChatStorage.send(sender, "punishment.common.expires-in", "time", existingBan.getFormattedTimeRemaining());
            return;
        }

        // Aplicar ban
        Punishment ban = punishmentManager.applyPunishment(
                PunishmentType.BAN,
                targetUuid,
                playerName,
                targetIp,
                staffPlayer,
                reason,
                proof,
                duration);

        // Broadcast para todos
        String staffName = staffPlayer != null ? staffPlayer.getName() : "Console";
        String staffTag = "";
        if (staffPlayer != null && plugin.getDisplayManager() != null) {
            staffTag = plugin.getDisplayManager().getCurrentPrefix(staffPlayer);
        }
        String durationText = duration == 0 ? "Permanente" : ban.getFormattedDuration();

        broadcastBan(playerName, targetUuid.toString(), staffName, staffTag, reason, durationText, proof);

        // Log no console
        plugin.getLogger().info("[BAN] " + playerName + " foi banido por " + staffName +
                " | Motivo: " + reason + " | Duração: " + durationText);
    }

    /**
     * Mensagem de ação bloqueada (jogador é dono)
     */
    private void sendBlockedOwnerMessage(CommandSender sender, String playerName) {
        ChatStorage.sendRaw(sender, "");
        ChatStorage.send(sender, "punishment.common.blocked-owner-title");
        ChatStorage.sendRaw(sender, "");
        ChatStorage.send(sender, "punishment.common.blocked-owner-msg", "player", playerName);
        ChatStorage.send(sender, "punishment.common.blocked-owner-notice");
        ChatStorage.sendRaw(sender, "");
    }

    /**
     * Broadcast do banimento para todo o servidor
     */
    private void broadcastBan(String player, String uuid, String staff, String staffTag,
            String reason, String duration, String proof) {

        String header = ChatStorage.getMessage("punishment.ban.broadcast.header");
        String title = ChatStorage.getMessage("punishment.ban.broadcast.title");
        String msg = ChatStorage.getMessage("punishment.ban.broadcast.msg",
                "player", player, "uuid", uuid.substring(0, 8));
        String staffLine = ChatStorage.getMessage("punishment.ban.broadcast.staff",
                "staff_tag", staffTag, "staff", staff);
        String reasonLine = ChatStorage.getMessage("punishment.ban.broadcast.reason", "reason", reason);
        String durationLine = ChatStorage.getMessage("punishment.ban.broadcast.duration", "duration", duration);
        String proofLine = ChatStorage.getMessage("punishment.ban.broadcast.proof", "proof", proof);
        String footer = ChatStorage.getMessage("punishment.ban.broadcast.footer");

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage("");
            p.sendMessage(header);
            p.sendMessage("");
            p.sendMessage(title);
            p.sendMessage("");
            p.sendMessage(msg);
            p.sendMessage(staffLine);
            p.sendMessage(reasonLine);
            p.sendMessage(durationLine);
            p.sendMessage(proofLine);
            p.sendMessage("");
            p.sendMessage(footer);
            p.sendMessage("");
        }

        // Console também
        Bukkit.getConsoleSender().sendMessage("");
        Bukkit.getConsoleSender().sendMessage(header);
        Bukkit.getConsoleSender().sendMessage("");
        Bukkit.getConsoleSender().sendMessage(title);
        Bukkit.getConsoleSender().sendMessage("");
        Bukkit.getConsoleSender().sendMessage(msg);
        Bukkit.getConsoleSender().sendMessage(staffLine);
        Bukkit.getConsoleSender().sendMessage(reasonLine);
        Bukkit.getConsoleSender().sendMessage(durationLine);
        Bukkit.getConsoleSender().sendMessage(proofLine);
        Bukkit.getConsoleSender().sendMessage("");
        Bukkit.getConsoleSender().sendMessage(footer);
        Bukkit.getConsoleSender().sendMessage("");
    }

    /**
     * Envia mensagem de uso do comando
     */
    private void sendUsageMessage(CommandSender sender) {
        ChatStorage.sendRaw(sender, "");
        sender.sendMessage(ChatStorage.getMessage("punishment.ban.help.title"));
        ChatStorage.sendRaw(sender, "");
        sender.sendMessage(ChatStorage.getMessage("punishment.ban.help.usage"));
        ChatStorage.sendRaw(sender, "");
        sender.sendMessage(ChatStorage.getMessage("punishment.ban.help.args-header"));
        ChatStorage.sendRaw(sender, "    &7• &fnick &8- &7Nome do jogador a ser banido");
        ChatStorage.sendRaw(sender, "    &7• &fmotivo &8- &7Razão do banimento");
        ChatStorage.sendRaw(sender, "    &7• &ftempo &8- &7Duração (30d, 1h, 30m, perm)");
        ChatStorage.sendRaw(sender, "    &7• &fprova &8- &7Link da prova (obrigatório para Mod/Helper)");
        ChatStorage.sendRaw(sender, "");
        sender.sendMessage(ChatStorage.getMessage("punishment.ban.help.time-formats"));
        sender.sendMessage(ChatStorage.getMessage("punishment.ban.help.time-examples"));
        ChatStorage.sendRaw(sender, "");
        sender.sendMessage(ChatStorage.getMessage("punishment.ban.help.examples-header"));
        ChatStorage.sendRaw(sender, "    &f/ban Steve Hack 30d https://prnt.sc/xxx");
        ChatStorage.sendRaw(sender, "    &f/ban Steve \"Uso de cheat\" perm https://prnt.sc/xxx");
        ChatStorage.sendRaw(sender, "");
        sender.sendMessage(ChatStorage.getMessage("punishment.ban.help.important"));
        ChatStorage.sendRaw(sender, "");
    }
}
