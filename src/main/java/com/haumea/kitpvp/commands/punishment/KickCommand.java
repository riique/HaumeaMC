package com.haumea.kitpvp.commands.punishment;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.commands.base.BaseCommand;
import com.haumea.kitpvp.commands.base.CommandInfo;
import com.haumea.kitpvp.managers.PunishmentManager;
import com.haumea.kitpvp.models.Punishment.PunishmentType;
import com.haumea.kitpvp.utils.ChatStorage;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Comando de kick do HaumeaMC
 * 
 * Uso: /kick <nick> <motivo> [prova]
 * - Prova é obrigatória para staff que não são ADMIN
 * - Expulsa o jogador do servidor
 * 
 * @author HaumeaMC
 */
@CommandInfo(name = "kick", description = "Expulsa um jogador do servidor", usage = "/kick <nick> <motivo> [prova]", allowedGroups = {
        "dono", "diretor", "gerente", "admin", "moderador", "mod", "helper" })
public class KickCommand extends BaseCommand {

    public KickCommand(HaumeaMC plugin) {
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
                ChatStorage.send(sender, "punishment.kick.proof-required");
                ChatStorage.send(sender, "punishment.kick.usage-hint");
                return;
            }
        }

        // Se não tem prova, definir como "Não informado" (só admins)
        if (proof == null) {
            proof = "Não informado";
        }

        // Buscar jogador online
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            ChatStorage.send(sender, "punishment.kick.player-offline", "player", targetName);
            return;
        }

        // Obter dados do alvo
        java.util.UUID targetUuid = target.getUniqueId();
        String playerName = target.getName();

        // Verificar se o alvo é DONO (protegido)
        if (punishmentManager.isProtected(targetUuid)) {
            sendBlockedOwnerMessage(sender, playerName);

            // Notificar todos os donos online
            String staffName = staffPlayer != null ? staffPlayer.getName() : "Console";
            punishmentManager.notifyOwners(staffName, playerName, "KICKAR");
            return;
        }

        // Aplicar kick (registrar na história)
        punishmentManager.applyPunishment(
                PunishmentType.KICK,
                targetUuid,
                playerName,
                "", // Kick não precisa de IP
                staffPlayer,
                reason,
                proof,
                0 // Kick é instantâneo
        );

        // Broadcast para todos
        String staffName = staffPlayer != null ? staffPlayer.getName() : "Console";
        String staffTag = "";
        if (staffPlayer != null && plugin.getDisplayManager() != null) {
            staffTag = plugin.getDisplayManager().getCurrentPrefix(staffPlayer);
        }

        broadcastKick(playerName, targetUuid.toString(), staffName, staffTag, reason, proof);

        // Log no console
        plugin.getLogger().info("[KICK] " + playerName + " foi expulso por " + staffName +
                " | Motivo: " + reason);
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
     * Broadcast do kick para todo o servidor
     */
    private void broadcastKick(String player, String uuid, String staff, String staffTag,
            String reason, String proof) {
        String header = ChatStorage.getMessage("punishment.kick.broadcast.header");
        String title = ChatStorage.getMessage("punishment.kick.broadcast.title");
        String msg = ChatStorage.getMessage("punishment.kick.broadcast.msg",
                "player", player, "uuid", uuid.substring(0, 8));
        String staffLine = ChatStorage.getMessage("punishment.kick.broadcast.staff",
                "staff_tag", staffTag, "staff", staff);
        String reasonLine = ChatStorage.getMessage("punishment.kick.broadcast.reason", "reason", reason);
        String proofLine = ChatStorage.getMessage("punishment.kick.broadcast.proof", "proof", proof);
        String footer = ChatStorage.getMessage("punishment.kick.broadcast.footer");

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
        Bukkit.getConsoleSender().sendMessage("");
        Bukkit.getConsoleSender().sendMessage(footer);
        Bukkit.getConsoleSender().sendMessage("");
    }

    /**
     * Envia mensagem de uso do comando
     */
    private void sendUsageMessage(CommandSender sender) {
        ChatStorage.sendRaw(sender, "");
        sender.sendMessage(ChatStorage.getMessage("punishment.kick.help.title"));
        ChatStorage.sendRaw(sender, "");
        sender.sendMessage(ChatStorage.getMessage("punishment.kick.help.usage"));
        ChatStorage.sendRaw(sender, "");
        sender.sendMessage(ChatStorage.getMessage("punishment.kick.help.args-header"));
        ChatStorage.sendRaw(sender, "    &7• &fnick &8- &7Nome do jogador a ser expulso");
        ChatStorage.sendRaw(sender, "    &7• &fmotivo &8- &7Razão da expulsão");
        ChatStorage.sendRaw(sender, "    &7• &fprova &8- &7Link da prova (obrigatório para Mod/Helper)");
        ChatStorage.sendRaw(sender, "");
        sender.sendMessage(ChatStorage.getMessage("punishment.kick.help.examples-header"));
        ChatStorage.sendRaw(sender, "    &f/kick Steve \"Spam no chat\" https://prnt.sc/xxx");
        ChatStorage.sendRaw(sender, "    &f/kick Steve Flood https://prnt.sc/xxx");
        ChatStorage.sendRaw(sender, "");
        sender.sendMessage(ChatStorage.getMessage("punishment.kick.help.important"));
        ChatStorage.sendRaw(sender, "");
    }
}
