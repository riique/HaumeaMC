package com.haumea.kitpvp.commands.punishment;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.commands.base.BaseCommand;
import com.haumea.kitpvp.commands.base.CommandInfo;
import com.haumea.kitpvp.managers.PunishmentManager;
import com.haumea.kitpvp.models.Punishment.PunishmentType;
import com.haumea.kitpvp.utils.ChatStorage;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Comando de warn (aviso) do HaumeaMC
 * 
 * Uso: /warn <nick> <motivo> [prova]
 * - Prova é obrigatória para staff que não são ADMIN
 * - Avisos acumulam e podem levar a punições automáticas
 * 
 * @author HaumeaMC
 */
@CommandInfo(name = "warn", description = "Aplica um aviso a um jogador", usage = "/warn <nick> <motivo> [prova]", allowedGroups = {
        "dono", "diretor", "gerente", "admin", "moderador", "mod", "helper" })
public class WarnCommand extends BaseCommand {

    public WarnCommand(HaumeaMC plugin) {
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
        String proof = args.length >= 3 ? args[2] : null;

        // Verificar se precisa de prova
        PunishmentManager punishmentManager = plugin.getPunishmentManager();
        Player staffPlayer = sender instanceof Player ? (Player) sender : null;
        boolean isAdmin = punishmentManager.isAdmin(staffPlayer);

        // Staff que não são ADMIN precisam de prova válida
        if (!isAdmin) {
            if (proof == null || !punishmentManager.isValidProof(proof)) {
                ChatStorage.send(sender, "punishment.warn.proof-required");
                ChatStorage.send(sender, "punishment.warn.usage-hint");
                return;
            }
        }

        // Se não tem prova, definir como "Não informado" (só admins)
        if (proof == null) {
            proof = "Não informado";
        }

        // Buscar jogador (online ou offline)
        Player targetOnline = Bukkit.getPlayer(targetName);
        OfflinePlayer targetOffline = Bukkit.getOfflinePlayer(targetName);

        if (targetOffline == null || !targetOffline.hasPlayedBefore() && targetOnline == null) {
            ChatStorage.send(sender, "punishment.warn.unknown-player", "player", targetName);
            return;
        }

        // Obter dados do alvo
        java.util.UUID targetUuid = targetOffline.getUniqueId();
        String playerName = targetOnline != null ? targetOnline.getName() : targetOffline.getName();

        // Verificar se o alvo é DONO (protegido)
        if (punishmentManager.isProtected(targetUuid)) {
            sendBlockedOwnerMessage(sender, playerName);

            // Notificar todos os donos online
            String staffName = staffPlayer != null ? staffPlayer.getName() : "Console";
            punishmentManager.notifyOwners(staffName, playerName, "AVISAR");
            return;
        }

        // Aplicar warn
        punishmentManager.applyPunishment(
                PunishmentType.WARN,
                targetUuid,
                playerName,
                "", // Warn não precisa de IP
                staffPlayer,
                reason,
                proof,
                0 // Warns não expiram (ficam no histórico)
        );

        // Contar warns ativos
        int warnCount = punishmentManager.countActiveWarnings(targetUuid);

        // Broadcast para todos
        String staffName = staffPlayer != null ? staffPlayer.getName() : "Console";
        String staffTag = "";
        if (staffPlayer != null && plugin.getDisplayManager() != null) {
            staffTag = plugin.getDisplayManager().getCurrentPrefix(staffPlayer);
        }

        broadcastWarn(playerName, targetUuid.toString(), staffName, staffTag, reason, proof, warnCount);

        // Notificar o jogador se estiver online
        if (targetOnline != null) {
            sendWarnToPlayer(targetOnline, reason, staffName, warnCount);
        }

        // Log no console
        plugin.getLogger().info("[WARN] " + playerName + " recebeu um aviso de " + staffName +
                " | Motivo: " + reason + " | Total: " + warnCount + " warns");
    }

    /**
     * Mensagem de ação bloqueada (jogador é dono)
     */
    private void sendBlockedOwnerMessage(CommandSender sender, String playerName) {
        ChatStorage.sendRaw(sender, "");
        ChatStorage.sendRaw(sender, "&c&l⚠ AÇÃO BLOQUEADA!");
        ChatStorage.sendRaw(sender, "");
        ChatStorage.sendRaw(sender, "&fO jogador &e" + playerName + " &fé um &4&lDONO &fe não pode ser punido.");
        ChatStorage.sendRaw(sender, "&7Esta tentativa foi registrada e notificada.");
        ChatStorage.sendRaw(sender, "");
    }

    /**
     * Broadcast do warn para todo o servidor
     */
    private void broadcastWarn(String player, String uuid, String staff, String staffTag,
            String reason, String proof, int totalWarns) {

        String header = ChatStorage.getMessage("punishment.warn.broadcast.header");
        String title = ChatStorage.getMessage("punishment.warn.broadcast.title");
        String msg = ChatStorage.getMessage("punishment.warn.broadcast.msg",
                "player", player, "uuid", uuid.substring(0, 8));
        String staffLine = ChatStorage.getMessage("punishment.warn.broadcast.staff",
                "staff_tag", staffTag, "staff", staff);
        String reasonLine = ChatStorage.getMessage("punishment.warn.broadcast.reason", "reason", reason);
        String proofLine = ChatStorage.getMessage("punishment.warn.broadcast.proof", "proof", proof);
        String totalLine = ChatStorage.getMessage("punishment.warn.broadcast.total", "count",
                String.valueOf(totalWarns));
        String footer = ChatStorage.getMessage("punishment.warn.broadcast.footer");

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage("");
            p.sendMessage(header);
            p.sendMessage("");
            p.sendMessage(title);
            p.sendMessage("");
            p.sendMessage(msg);
            p.sendMessage(staffLine);
            p.sendMessage(reasonLine);
            p.sendMessage(proofLine);
            p.sendMessage(totalLine);
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
        Bukkit.getConsoleSender().sendMessage(proofLine);
        Bukkit.getConsoleSender().sendMessage(totalLine);
        Bukkit.getConsoleSender().sendMessage("");
        Bukkit.getConsoleSender().sendMessage(footer);
        Bukkit.getConsoleSender().sendMessage("");
    }

    /**
     * Envia mensagem especial para o jogador avisado
     */
    private void sendWarnToPlayer(Player player, String reason, String staff, int totalWarns) {
        String header = ChatStorage.getMessage("punishment.warn.player.header");
        String title = ChatStorage.getMessage("punishment.warn.player.title");
        String reasonLine = ChatStorage.getMessage("punishment.warn.player.reason", "reason", reason);
        String staffLine = ChatStorage.getMessage("punishment.warn.player.staff", "staff", staff);
        String totalLine = ChatStorage.getMessage("punishment.warn.player.total", "count", String.valueOf(totalWarns));
        String warning = ChatStorage.getMessage("punishment.warn.player.warning");
        String footer = ChatStorage.getMessage("punishment.warn.player.footer");

        player.sendMessage("");
        player.sendMessage(header);
        player.sendMessage("");
        player.sendMessage(title);
        player.sendMessage("");
        player.sendMessage(reasonLine);
        player.sendMessage(staffLine);
        player.sendMessage(totalLine);
        player.sendMessage("");
        player.sendMessage(warning);
        player.sendMessage("");
        player.sendMessage(footer);
        player.sendMessage("");
    }

    /**
     * Envia mensagem de uso do comando
     */
    private void sendUsageMessage(CommandSender sender) {
        String title = ChatStorage.getMessage("punishment.warn.help.title");
        String usage = ChatStorage.getMessage("punishment.warn.help.usage");
        String argsHeader = ChatStorage.getMessage("punishment.warn.help.args-header");
        String examplesHeader = ChatStorage.getMessage("punishment.warn.help.examples-header");

        ChatStorage.sendRaw(sender, "");
        sender.sendMessage(title);
        ChatStorage.sendRaw(sender, "");
        sender.sendMessage(usage);
        ChatStorage.sendRaw(sender, "");
        sender.sendMessage(argsHeader);

        // Argumentos (lista do YAML)
        ChatStorage.sendRaw(sender, "    &7• &fnick &8- &7Nome do jogador a ser avisado");
        ChatStorage.sendRaw(sender, "    &7• &fmotivo &8- &7Razão do aviso");
        ChatStorage.sendRaw(sender, "    &7• &fprova &8- &7Link da prova (obrigatório para Mod/Helper)");

        ChatStorage.sendRaw(sender, "");
        sender.sendMessage(examplesHeader);
        ChatStorage.sendRaw(sender, "    &f/warn Steve \"Flood no chat\" https://prnt.sc/xxx");
        ChatStorage.sendRaw(sender, "    &f/warn Steve Spam https://prnt.sc/xxx");
        ChatStorage.sendRaw(sender, "");
        ChatStorage.sendRaw(sender, "&c  ⚠ A prova é obrigatória para Moderadores/Helpers.");
        ChatStorage.sendRaw(sender, "&7  Avisos acumulados podem resultar em mute ou ban.");
        ChatStorage.sendRaw(sender, "");
    }
}
