package com.haumea.kitpvp.listeners;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.managers.PunishmentManager;
import com.haumea.kitpvp.models.Punishment;
import com.haumea.kitpvp.utils.ChatStorage;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.UUID;

/**
 * Listener para punições do HaumeaMC
 * 
 * Responsável por:
 * - Bloquear entrada de jogadores banidos (UUID e IP)
 * - Bloquear chat de jogadores mutados
 * - Bloquear comandos de jogadores mutados
 * 
 * @author HaumeaMC
 */
public class PunishmentListener implements Listener {

    private final HaumeaMC plugin;
    private final PunishmentManager punishmentManager;

    public PunishmentListener(HaumeaMC plugin) {
        this.plugin = plugin;
        this.punishmentManager = plugin.getPunishmentManager();
    }

    /**
     * Verifica ban ao tentar entrar no servidor
     * Usa AsyncPlayerPreLoginEvent para verificar antes de carregar o jogador
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        if (punishmentManager == null)
            return;

        UUID uuid = event.getUniqueId();
        String ip = event.getAddress().getHostAddress();

        // Verificar ban por UUID ou IP
        Punishment ban = punishmentManager.checkBan(uuid, ip);

        if (ban != null && ban.isCurrentlyActive()) {
            // Construir mensagem de ban
            String kickMessage = buildBanMessage(ban);
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, ChatStorage.colorize(kickMessage));

            plugin.getLogger().info("[BAN] Bloqueado login de " + event.getName() +
                    " (IP: " + ip + ") - Motivo: " + ban.getReason());
        }
    }

    /**
     * Bloqueia chat de jogadores mutados
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        if (punishmentManager == null)
            return;

        Player player = event.getPlayer();
        Punishment mute = punishmentManager.getActiveMute(player.getUniqueId());

        if (mute != null && mute.isCurrentlyActive()) {
            event.setCancelled(true);
            sendMuteMessage(player, mute);
        }
    }

    /**
     * Bloqueia comandos de jogadores mutados (exceto comandos permitidos)
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (punishmentManager == null)
            return;

        Player player = event.getPlayer();
        Punishment mute = punishmentManager.getActiveMute(player.getUniqueId());

        if (mute != null && mute.isCurrentlyActive()) {
            String command = event.getMessage().toLowerCase().split(" ")[0].substring(1);

            // Comandos permitidos mesmo mutado
            if (isAllowedCommand(command)) {
                return;
            }

            // Bloquear comandos de comunicação
            if (isChatCommand(command)) {
                event.setCancelled(true);
                sendMuteMessage(player, mute);
            }
        }
    }

    /**
     * Envia mensagem de mute para o jogador usando messages.yml
     */
    private void sendMuteMessage(Player player, Punishment mute) {
        boolean isPermanent = mute.getExpiration() == 0;

        // Header
        ChatStorage.sendRaw(player, "");
        ChatStorage.send(player, "punishment-listener.mute-chat.header");
        ChatStorage.sendRaw(player, "");

        // Tipo de mute (permanente ou temporario)
        if (isPermanent) {
            ChatStorage.send(player, "punishment-listener.mute-chat.permanent");
        } else {
            ChatStorage.send(player, "punishment-listener.mute-chat.temporary");
        }

        // Informacoes da punicao
        ChatStorage.send(player, "punishment-listener.mute-chat.staff", "staff", mute.getStaffName());
        ChatStorage.send(player, "punishment-listener.mute-chat.reason", "reason", mute.getReason());

        // Duracao (apenas se temporario)
        if (!isPermanent) {
            ChatStorage.send(player, "punishment-listener.mute-chat.duration", "duration",
                    mute.getFormattedTimeRemaining());
        }

        // Prova (se existir)
        if (mute.getProof() != null && !mute.getProof().isEmpty()) {
            ChatStorage.send(player, "punishment-listener.mute-chat.proof", "proof", mute.getProof());
        }

        // Links de apelacao e loja
        ChatStorage.sendRaw(player, "");
        ChatStorage.send(player, "punishment-listener.mute-chat.appeal");
        ChatStorage.send(player, "punishment-listener.mute-chat.store");

        // Footer
        ChatStorage.sendRaw(player, "");
        ChatStorage.send(player, "punishment-listener.mute-chat.footer");
        ChatStorage.sendRaw(player, "");
    }

    /**
     * Verifica se é um comando permitido mesmo mutado
     */
    private boolean isAllowedCommand(String command) {
        switch (command) {
            case "spawn":
            case "tag":
            case "tags":
            case "kit":
            case "kits":
            case "perfil":
            case "conta":
            case "score":
            case "ping":
            case "regras":
                return true;
            default:
                return false;
        }
    }

    /**
     * Verifica se é um comando de comunicação (bloqueado)
     */
    private boolean isChatCommand(String command) {
        switch (command) {
            case "tell":
            case "msg":
            case "whisper":
            case "w":
            case "m":
            case "pm":
            case "r":
            case "reply":
            case "say":
            case "me":
            case "bc":
            case "broadcast":
            case "shout":
            case "global":
            case "g":
            case "local":
            case "l":
            case "party":
            case "p":
            case "clan":
            case "c":
            case "team":
            case "t":
                return true;
            default:
                return false;
        }
    }

    /**
     * Constrói mensagem de ban para exibir na tela de login usando messages.yml
     */
    private String buildBanMessage(Punishment ban) {
        boolean isPermanent = ban.getExpiration() == 0;
        String type = isPermanent ? "PERMANENTEMENTE" : "TEMPORARIAMENTE";
        String duration = isPermanent ? "Permanente" : ban.getFormattedTimeRemaining();

        // Usar o template do messages.yml
        String template = ChatStorage.getMessage("punishment-listener.ban-screen",
                "type", type,
                "staff", ban.getStaffName(),
                "reason", ban.getReason(),
                "duration", duration,
                "proof", ban.getProof());

        return template;
    }
}
