package com.haumea.kitpvp.commands.staff;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.commands.base.BaseCommand;
import com.haumea.kitpvp.commands.base.CommandInfo;
import com.haumea.kitpvp.database.MongoAccountRepository;
import com.haumea.kitpvp.database.MongoAccountRepository.AccountData;
import com.haumea.kitpvp.models.Punishment;
import com.haumea.kitpvp.utils.ChatStorage;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Comando /check para verificar informacoes de jogadores.
 * 
 * Funcionalidades:
 * - Visualizar todas as punicoes de um jogador (ativas, expiradas, revogadas)
 * - Verificar contas com mesmo IP (alt detection)
 * - Limpar todas as punicoes de um jogador
 * 
 * @author HaumeaMC
 */
@CommandInfo(name = "check", aliases = { "verificar",
        "checar" }, description = "Verifica informacoes detalhadas de um jogador", usage = "/check <jogador> [punishments|alts|clear]", allowedGroups = {
                "dono", "diretor", "gerente", "admin", "moderador", "mod", "helper" })
public class CheckCommand extends BaseCommand {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm");
    private static final String MSG_PREFIX = "punishment.check.";

    public CheckCommand(HaumeaMC plugin) {
        super(plugin);
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sendUsageMessage(sender);
            return;
        }

        String targetName = args[0];
        String subCommand = args.length >= 2 ? args[1].toLowerCase() : null;

        // Buscar jogador (online ou offline)
        OfflinePlayer targetOffline = Bukkit.getOfflinePlayer(targetName);
        Player targetOnline = Bukkit.getPlayer(targetName);

        if (targetOffline == null || (!targetOffline.hasPlayedBefore() && targetOnline == null)) {
            ChatStorage.send(sender, MSG_PREFIX + "player-not-found", "player", targetName);
            return;
        }

        UUID targetUuid = targetOffline.getUniqueId();
        String playerName = targetOnline != null ? targetOnline.getName() : targetOffline.getName();

        // Determinar subcomando
        if (subCommand == null) {
            showFullOverview(sender, targetUuid, playerName);
        } else if (subCommand.equals("punishments") || subCommand.equals("punicoes") || subCommand.equals("p")) {
            showPunishments(sender, targetUuid, playerName);
        } else if (subCommand.equals("alts") || subCommand.equals("contas") || subCommand.equals("ips")
                || subCommand.equals("a")) {
            showAlts(sender, targetUuid, playerName);
        } else if (subCommand.equals("clear") || subCommand.equals("limpar") || subCommand.equals("c")) {
            clearPunishments(sender, targetUuid, playerName);
        } else {
            sendUsageMessage(sender);
        }
    }

    /**
     * Mostra overview completo do jogador
     */
    private void showFullOverview(CommandSender sender, UUID targetUuid, String playerName) {
        MongoAccountRepository accountRepo = plugin.getAccountRepository();
        AccountData accountData = accountRepo != null ? accountRepo.getAccount(targetUuid) : null;

        String ip = null;
        if (accountData != null) {
            ip = accountData.getLastLoginIP();
        }

        // Header
        sendHeader(sender, playerName);

        // Informacoes basicas da conta
        sendAccountInfo(sender, targetUuid, playerName, accountData);

        ChatStorage.sendRaw(sender, "");

        // Resumo de punicoes
        sendPunishmentSummary(sender, targetUuid);

        ChatStorage.sendRaw(sender, "");

        // Contas com mesmo IP (se disponivel)
        if (ip != null && !ip.isEmpty()) {
            sendAltsSummary(sender, ip, targetUuid);
        }

        // Footer
        msg(sender, "separator");

        // Dicas de comandos
        ChatStorage.sendRaw(sender, "");
        msg(sender, "tips-header");
        sender.sendMessage(ChatStorage.getMessage(MSG_PREFIX + "tips-punishments", "player", playerName));
        sender.sendMessage(ChatStorage.getMessage(MSG_PREFIX + "tips-alts", "player", playerName));
        sender.sendMessage(ChatStorage.getMessage(MSG_PREFIX + "tips-clear", "player", playerName));
        ChatStorage.sendRaw(sender, "");
    }

    /**
     * Mostra informacoes da conta
     */
    private void sendAccountInfo(CommandSender sender, UUID targetUuid, String playerName, AccountData accountData) {
        msg(sender, "account-title");
        ChatStorage.sendRaw(sender, "");

        // Status online/offline
        Player online = Bukkit.getPlayer(targetUuid);
        String statusText = online != null
                ? ChatStorage.getMessage(MSG_PREFIX + "account-online")
                : ChatStorage.getMessage(MSG_PREFIX + "account-offline");

        sender.sendMessage(
                ChatStorage.getMessage(MSG_PREFIX + "account-name", "player", playerName, "status", statusText));
        sender.sendMessage(
                ChatStorage.getMessage(MSG_PREFIX + "account-uuid", "uuid", targetUuid.toString().substring(0, 8)));

        if (accountData != null) {
            // Primeiro join
            if (accountData.getFirstJoin() > 0) {
                sender.sendMessage(ChatStorage.getMessage(MSG_PREFIX + "account-first-join",
                        "date", DATE_FORMAT.format(new Date(accountData.getFirstJoin()))));
            }

            // Ultimo acesso
            if (accountData.getLastSeen() > 0) {
                sender.sendMessage(ChatStorage.getMessage(MSG_PREFIX + "account-last-seen",
                        "date", DATE_FORMAT.format(new Date(accountData.getLastSeen()))));
            }

            // IP
            if (accountData.getLastLoginIP() != null && !accountData.getLastLoginIP().isEmpty()) {
                sender.sendMessage(ChatStorage.getMessage(MSG_PREFIX + "account-last-ip",
                        "ip", accountData.getLastLoginIP()));
            }

            // Grupos
            List<String> groups = accountData.getGroupNames();
            if (!groups.isEmpty()) {
                sender.sendMessage(ChatStorage.getMessage(MSG_PREFIX + "account-groups",
                        "groups", String.join("&7, &e", groups)));
            }

            // Status de registro
            String regStatus = accountData.isRegistered()
                    ? ChatStorage.getMessage(MSG_PREFIX + "account-registered")
                    : ChatStorage.getMessage(MSG_PREFIX + "account-not-registered");
            sender.sendMessage(ChatStorage.getMessage(MSG_PREFIX + "account-auth", "status", regStatus));

            // Premium
            String premiumStatus = accountData.isPremium()
                    ? ChatStorage.getMessage(MSG_PREFIX + "account-premium")
                    : ChatStorage.getMessage(MSG_PREFIX + "account-cracked");
            sender.sendMessage(ChatStorage.getMessage(MSG_PREFIX + "account-type", "status", premiumStatus));
        }
    }

    /**
     * Mostra resumo das punicoes
     */
    private void sendPunishmentSummary(CommandSender sender, UUID targetUuid) {
        List<Punishment> punishments = plugin.getPunishmentManager().getHistory(targetUuid);

        msg(sender, "punishments-title");
        ChatStorage.sendRaw(sender, "");

        if (punishments.isEmpty()) {
            msg(sender, "punishments-none");
            return;
        }

        // Contadores
        int totalBans = 0, activeBans = 0;
        int totalMutes = 0, activeMutes = 0;
        int totalWarns = 0, activeWarns = 0;
        int totalKicks = 0;

        for (Punishment p : punishments) {
            boolean isActive = p.isCurrentlyActive();
            switch (p.getType()) {
                case BAN:
                    totalBans++;
                    if (isActive)
                        activeBans++;
                    break;
                case MUTE:
                    totalMutes++;
                    if (isActive)
                        activeMutes++;
                    break;
                case WARN:
                    totalWarns++;
                    if (isActive)
                        activeWarns++;
                    break;
                case KICK:
                    totalKicks++;
                    break;
            }
        }

        // Mostrar resumo
        String banStatus = activeBans > 0
                ? ChatStorage.getMessage(MSG_PREFIX + "punishments-active-count", "count", String.valueOf(activeBans))
                : ChatStorage.getMessage(MSG_PREFIX + "punishments-inactive-count");
        String muteStatus = activeMutes > 0
                ? ChatStorage.getMessage(MSG_PREFIX + "punishments-active-count", "count", String.valueOf(activeMutes))
                : ChatStorage.getMessage(MSG_PREFIX + "punishments-inactive-count");
        String warnStatus = activeWarns > 0
                ? ChatStorage.getMessage(MSG_PREFIX + "punishments-active-count", "count", String.valueOf(activeWarns))
                : ChatStorage.getMessage(MSG_PREFIX + "punishments-inactive-count");

        sender.sendMessage(ChatStorage.getMessage(MSG_PREFIX + "punishments-ban-line",
                "total", String.valueOf(totalBans), "status", banStatus));
        sender.sendMessage(ChatStorage.getMessage(MSG_PREFIX + "punishments-mute-line",
                "total", String.valueOf(totalMutes), "status", muteStatus));
        sender.sendMessage(ChatStorage.getMessage(MSG_PREFIX + "punishments-warn-line",
                "total", String.valueOf(totalWarns), "status", warnStatus));
        sender.sendMessage(ChatStorage.getMessage(MSG_PREFIX + "punishments-kick-line",
                "total", String.valueOf(totalKicks)));

        // Mostrar ultima punicao se houver
        if (!punishments.isEmpty()) {
            Punishment lastPunishment = punishments.get(punishments.size() - 1);
            ChatStorage.sendRaw(sender, "");
            sender.sendMessage(ChatStorage.getMessage(MSG_PREFIX + "punishments-last",
                    "type", lastPunishment.getType().getColor() + lastPunishment.getType().getName(),
                    "reason", lastPunishment.getReason()));
        }
    }

    /**
     * Mostra resumo de contas alternativas
     */
    private void sendAltsSummary(CommandSender sender, String ip, UUID excludeUuid) {
        msg(sender, "alts-summary-title");
        ChatStorage.sendRaw(sender, "");

        List<AccountData> alts = plugin.getAccountRepository().getAccountsByIp(ip, excludeUuid);

        if (alts.isEmpty()) {
            msg(sender, "alts-summary-none");
            return;
        }

        sender.sendMessage(
                ChatStorage.getMessage(MSG_PREFIX + "alts-summary-found", "count", String.valueOf(alts.size())));
        ChatStorage.sendRaw(sender, "");

        int shown = 0;
        for (AccountData alt : alts) {
            if (shown >= 5) {
                sender.sendMessage(ChatStorage.getMessage(MSG_PREFIX + "alts-summary-more",
                        "count", String.valueOf(alts.size() - 5)));
                break;
            }

            String name = alt.getName() != null ? alt.getName() : "???";

            // Verificar se esta banido
            UUID altUuid = getUuidByName(name);
            boolean banned = false;
            if (altUuid != null) {
                Punishment ban = plugin.getPunishmentManager().getActiveBan(altUuid);
                banned = ban != null;
            }

            if (banned) {
                sender.sendMessage(ChatStorage.getMessage(MSG_PREFIX + "alts-summary-entry-banned", "name", name));
            } else {
                sender.sendMessage(ChatStorage.getMessage(MSG_PREFIX + "alts-summary-entry-ok", "name", name));
            }
            shown++;
        }
    }

    /**
     * Mostra todas as punicoes detalhadas
     */
    private void showPunishments(CommandSender sender, UUID targetUuid, String playerName) {
        List<Punishment> punishments = plugin.getPunishmentManager().getHistory(targetUuid);

        // Header
        ChatStorage.sendRaw(sender, "");
        msg(sender, "separator");
        sender.sendMessage(ChatStorage.getMessage(MSG_PREFIX + "list-title", "player", playerName));
        msg(sender, "separator");

        if (punishments.isEmpty()) {
            msg(sender, "list-clean");
            msg(sender, "list-clean-sub");
            msg(sender, "separator");
            ChatStorage.sendRaw(sender, "");
            return;
        }

        // Ordenar por data (mais recente primeiro)
        Collections.sort(punishments, new Comparator<Punishment>() {
            @Override
            public int compare(Punishment p1, Punishment p2) {
                return Long.compare(p2.getTimestamp(), p1.getTimestamp());
            }
        });

        ChatStorage.sendRaw(sender, "");

        int count = 0;
        for (Punishment p : punishments) {
            if (count >= 10) {
                sender.sendMessage(ChatStorage.getMessage(MSG_PREFIX + "list-more",
                        "count", String.valueOf(punishments.size() - 10)));
                break;
            }

            // Status da punicao
            String statusIcon;
            String statusText;
            if (!p.isActive()) {
                statusIcon = ChatStorage.getMessage(MSG_PREFIX + "list-icon-revoked");
                statusText = ChatStorage.getMessage(MSG_PREFIX + "list-status-revoked");
            } else if (p.isExpired()) {
                statusIcon = ChatStorage.getMessage(MSG_PREFIX + "list-icon-expired");
                statusText = ChatStorage.getMessage(MSG_PREFIX + "list-status-expired");
            } else {
                statusIcon = ChatStorage.getMessage(MSG_PREFIX + "list-icon-active");
                statusText = ChatStorage.getMessage(MSG_PREFIX + "list-status-active");
            }

            // Tipo e cor
            String typeIcon;
            switch (p.getType()) {
                case BAN:
                    typeIcon = ChatStorage.getMessage(MSG_PREFIX + "list-icon-ban");
                    break;
                case MUTE:
                    typeIcon = ChatStorage.getMessage(MSG_PREFIX + "list-icon-mute");
                    break;
                case WARN:
                    typeIcon = ChatStorage.getMessage(MSG_PREFIX + "list-icon-warn");
                    break;
                case KICK:
                    typeIcon = ChatStorage.getMessage(MSG_PREFIX + "list-icon-kick");
                    break;
                default:
                    typeIcon = "&7[?]";
            }

            // Linha principal
            sender.sendMessage(ChatStorage.getMessage(MSG_PREFIX + "list-entry-main",
                    "status_icon", statusIcon,
                    "type_icon", typeIcon,
                    "type_color", p.getType().getColor(),
                    "type_name", p.getType().getName(),
                    "status_text", statusText));

            // Detalhes
            sender.sendMessage(ChatStorage.getMessage(MSG_PREFIX + "list-entry-id-date",
                    "id", p.getId(),
                    "date", DATE_FORMAT.format(new Date(p.getTimestamp()))));
            sender.sendMessage(ChatStorage.getMessage(MSG_PREFIX + "list-entry-staff-reason",
                    "staff", p.getStaffName(),
                    "reason", p.getReason()));

            // Duracao/Expiracao
            if (p.getExpiration() == 0) {
                msg(sender, "list-entry-duration-perm");
            } else {
                String remaining = "";
                if (p.isActive() && !p.isExpired()) {
                    remaining = ChatStorage.getMessage(MSG_PREFIX + "list-entry-remaining",
                            "time", p.getFormattedTimeRemaining());
                }
                sender.sendMessage(ChatStorage.getMessage(MSG_PREFIX + "list-entry-duration-temp",
                        "duration", p.getFormattedDuration(),
                        "remaining", remaining));
            }

            ChatStorage.sendRaw(sender, "");
            count++;
        }

        // Estatisticas
        msg(sender, "separator");
        sender.sendMessage(ChatStorage.getMessage(MSG_PREFIX + "list-total",
                "count", String.valueOf(punishments.size())));
        msg(sender, "separator");
        ChatStorage.sendRaw(sender, "");
    }

    /**
     * Mostra todas as contas com mesmo IP
     */
    private void showAlts(CommandSender sender, UUID targetUuid, String playerName) {
        MongoAccountRepository accountRepo = plugin.getAccountRepository();
        AccountData accountData = accountRepo.getAccount(targetUuid);

        String ip = accountData != null ? accountData.getLastLoginIP() : null;

        // Header
        ChatStorage.sendRaw(sender, "");
        msg(sender, "separator");
        sender.sendMessage(ChatStorage.getMessage(MSG_PREFIX + "alts-title", "player", playerName));
        msg(sender, "separator");

        if (ip == null || ip.isEmpty()) {
            msg(sender, "alts-no-ip");
            msg(sender, "alts-no-ip-sub");
            msg(sender, "separator");
            ChatStorage.sendRaw(sender, "");
            return;
        }

        ChatStorage.sendRaw(sender, "");
        sender.sendMessage(ChatStorage.getMessage(MSG_PREFIX + "alts-ip-analyzed", "ip", ip));
        ChatStorage.sendRaw(sender, "");

        List<AccountData> alts = accountRepo.getAccountsByIp(ip, targetUuid);

        if (alts.isEmpty()) {
            msg(sender, "alts-unique");
            msg(sender, "alts-unique-sub");
            msg(sender, "separator");
            ChatStorage.sendRaw(sender, "");
            return;
        }

        // Contar banidos
        int bannedCount = 0;
        for (AccountData alt : alts) {
            UUID altUuid = getUuidByName(alt.getName());
            if (altUuid != null && plugin.getPunishmentManager().getActiveBan(altUuid) != null) {
                bannedCount++;
            }
        }

        // Aviso se houver conta banida
        if (bannedCount > 0) {
            sender.sendMessage(ChatStorage.getMessage(MSG_PREFIX + "alts-banned-alert",
                    "count", String.valueOf(bannedCount)));
            ChatStorage.sendRaw(sender, "");
        }

        sender.sendMessage(ChatStorage.getMessage(MSG_PREFIX + "alts-found",
                "count", String.valueOf(alts.size())));
        ChatStorage.sendRaw(sender, "");

        int count = 0;
        for (AccountData alt : alts) {
            String altName = alt.getName() != null ? alt.getName() : "???";
            UUID altUuid = getUuidByName(altName);

            // Status
            boolean isOnline = Bukkit.getPlayer(altName) != null;
            boolean isBanned = altUuid != null && plugin.getPunishmentManager().getActiveBan(altUuid) != null;
            boolean isMuted = altUuid != null && plugin.getPunishmentManager().getActiveMute(altUuid) != null;

            // Construir linha de status
            StringBuilder status = new StringBuilder();
            if (isBanned) {
                status.append(ChatStorage.getMessage(MSG_PREFIX + "alts-status-banned"));
            }
            if (isMuted) {
                status.append(ChatStorage.getMessage(MSG_PREFIX + "alts-status-muted"));
            }
            if (isOnline) {
                status.append(ChatStorage.getMessage(MSG_PREFIX + "alts-status-online"));
            }
            if (status.length() == 0) {
                status.append(ChatStorage.getMessage(MSG_PREFIX + "alts-status-ok"));
            }

            // Last seen
            String lastSeen = alt.getLastSeen() > 0 ? DATE_FORMAT.format(new Date(alt.getLastSeen())) : "???";

            sender.sendMessage(ChatStorage.getMessage(MSG_PREFIX + "alts-entry-name",
                    "status", status.toString(), "name", altName));
            sender.sendMessage(ChatStorage.getMessage(MSG_PREFIX + "alts-entry-last-seen", "date", lastSeen));
            ChatStorage.sendRaw(sender, "");

            count++;
            if (count >= 15) {
                sender.sendMessage(ChatStorage.getMessage(MSG_PREFIX + "alts-more",
                        "count", String.valueOf(alts.size() - 15)));
                break;
            }
        }

        msg(sender, "separator");

        // Se houver contas banidas, sugerir acao
        if (bannedCount > 0) {
            msg(sender, "alts-consider-ban");
        }

        ChatStorage.sendRaw(sender, "");
    }

    /**
     * Limpa todas as punicoes de um jogador
     */
    private void clearPunishments(CommandSender sender, UUID targetUuid, String playerName) {
        // Verificar permissao de admin
        if (sender instanceof Player) {
            Player staff = (Player) sender;
            if (!plugin.getPunishmentManager().isAdmin(staff)) {
                ChatStorage.send(sender, MSG_PREFIX + "clear-no-permission");
                return;
            }
        }

        List<Punishment> punishments = plugin.getPunishmentManager().getHistory(targetUuid);

        if (punishments.isEmpty()) {
            ChatStorage.send(sender, MSG_PREFIX + "no-punishments", "player", playerName);
            return;
        }

        int count = punishments.size();

        // Criar lista de IDs para evitar ConcurrentModificationException
        List<String> idsToDelete = new ArrayList<>();
        for (Punishment p : punishments) {
            idsToDelete.add(p.getId());
        }

        // Deletar todas as punicoes
        for (String id : idsToDelete) {
            plugin.getPunishmentManager().deletePunishment(id);
        }

        // Mensagem de sucesso
        ChatStorage.sendRaw(sender, "");
        msg(sender, "separator");
        msg(sender, "clear-title");
        sender.sendMessage(ChatStorage.getMessage(MSG_PREFIX + "clear-player", "player", playerName));
        sender.sendMessage(ChatStorage.getMessage(MSG_PREFIX + "clear-count", "count", String.valueOf(count)));
        msg(sender, "separator");
        ChatStorage.sendRaw(sender, "");

        // Log
        String staffName = sender instanceof Player ? ((Player) sender).getName() : "Console";
        plugin.getLogger().info("[CHECK] " + staffName + " limpou " + count + " punicoes de " + playerName);
    }

    /**
     * Header decorativo
     */
    private void sendHeader(CommandSender sender, String playerName) {
        ChatStorage.sendRaw(sender, "");
        msg(sender, "separator");
        msg(sender, "header-title");
        sender.sendMessage(ChatStorage.getMessage(MSG_PREFIX + "header-analyzing", "player", playerName));
        msg(sender, "separator");
    }

    /**
     * Envia uma mensagem do messages.yml
     */
    private void msg(CommandSender sender, String key) {
        sender.sendMessage(ChatStorage.getMessage(MSG_PREFIX + key));
    }

    /**
     * Mensagem de uso do comando
     */
    private void sendUsageMessage(CommandSender sender) {
        ChatStorage.sendRaw(sender, "");
        msg(sender, "help-title");
        ChatStorage.sendRaw(sender, "");
        msg(sender, "help-usage");
        ChatStorage.sendRaw(sender, "");
        msg(sender, "help-options-header");
        msg(sender, "help-option-none");
        msg(sender, "help-option-punishments");
        msg(sender, "help-option-alts");
        msg(sender, "help-option-clear");
        ChatStorage.sendRaw(sender, "");
        msg(sender, "help-aliases");
        ChatStorage.sendRaw(sender, "");
        msg(sender, "help-examples-header");
        msg(sender, "help-example-1");
        msg(sender, "help-example-2");
        msg(sender, "help-example-3");
        ChatStorage.sendRaw(sender, "");
    }

    /**
     * Obtem UUID pelo nome do jogador
     */
    private UUID getUuidByName(String playerName) {
        if (playerName == null)
            return null;

        // Tentar online primeiro
        Player online = Bukkit.getPlayer(playerName);
        if (online != null) {
            return online.getUniqueId();
        }

        // Tentar pelo repositorio
        MongoAccountRepository repo = plugin.getAccountRepository();
        if (repo != null) {
            return repo.getUUIDByName(playerName);
        }

        // Fallback para OfflinePlayer
        OfflinePlayer offline = Bukkit.getOfflinePlayer(playerName);
        if (offline.hasPlayedBefore()) {
            return offline.getUniqueId();
        }

        return null;
    }
}
