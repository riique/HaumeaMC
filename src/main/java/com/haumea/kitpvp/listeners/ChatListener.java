package com.haumea.kitpvp.listeners;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.managers.AntiFloodManager;
import com.haumea.kitpvp.managers.ChatManager;
import com.haumea.kitpvp.managers.GroupManager;
import com.haumea.kitpvp.models.Group;
import com.haumea.kitpvp.profile.PlayerProfile;
import com.haumea.kitpvp.utils.ChatStorage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

/**
 * Listener para gerenciar o chat do servidor.
 * Controla StaffChat, bloqueio de chat, antiflood e formatação de mensagens.
 * 
 * @author HaumeaMC
 */
public class ChatListener implements Listener {

    private final HaumeaMC plugin;

    /**
     * Construtor do ChatListener
     * 
     * @param plugin Instância do plugin
     */
    public ChatListener(HaumeaMC plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        // Se o evento já foi cancelado por outro listener, ignorar
        if (event.isCancelled()) {
            return;
        }

        Player player = event.getPlayer();
        String message = event.getMessage();

        // Verificar antiflood ANTES de qualquer processamento
        AntiFloodManager antiFloodManager = plugin.getAntiFloodManager();
        if (antiFloodManager != null) {
            AntiFloodManager.FloodResult floodResult = antiFloodManager.canSendMessage(player, message);

            if (!floodResult.isAllowed()) {
                event.setCancelled(true);

                // Formatar tempo restante
                double remaining = floodResult.getPunishmentSeconds();
                String timeFormatted;
                if (remaining < 1.0) {
                    timeFormatted = String.format("%.1f", remaining);
                } else {
                    timeFormatted = String.valueOf((int) Math.ceil(remaining));
                }

                // Mensagem diferente se foi detectado agora ou se ainda está em punição
                if (floodResult.isFloodDetected()) {
                    AntiFloodManager.FloodType type = floodResult.getFloodType();
                    String typeKey;
                    String alertType;

                    if (type == AntiFloodManager.FloodType.SPAM) {
                        typeKey = "antiflood.detected-spam";
                        alertType = "spam";
                    } else {
                        typeKey = "antiflood.detected-flood";
                        alertType = "flood";
                    }

                    ChatStorage.send(player, typeKey,
                            "time", timeFormatted,
                            "violations", String.valueOf(floodResult.getViolations()));

                    // Alertar staff
                    String staffAlert = ChatStorage.colorize(
                            "&e&l[ANTISPAM] &f" + player.getName() + " &7foi &c&lsilenciado &7por " + alertType + "! " +
                                    "&8(&e" + timeFormatted + "s&8)");
                    for (Player staff : Bukkit.getOnlinePlayers()) {
                        if (plugin.getGroupManager().isStaff(staff)) {
                            staff.sendMessage(staffAlert);
                        }
                    }
                } else {
                    // Ainda está em punição
                    ChatStorage.send(player, "antiflood.still-punished",
                            "time", timeFormatted);
                }

                return;
            }
        }

        PlayerProfile profile = plugin.getProfileManager().getProfile(player);
        ChatManager chatManager = plugin.getChatManager();

        if (profile == null) {
            return;
        }

        // Verificar se está no modo StaffChat
        if (profile.isStaffChatMode()) {
            event.setCancelled(true);
            sendStaffMessage(player, event.getMessage());
            return;
        }

        // Verificar se o chat global está desativado
        if (!chatManager.isChatEnabled()) {
            // Apenas staff pode falar
            if (!plugin.getGroupManager().isStaff(player)) {
                event.setCancelled(true);
                ChatStorage.send(player, "staff.chat-disabled");
                return;
            }
        }

        // Usar DisplayManager como fonte única de verdade para exibição
        String playerName;
        String prefix;
        String medalDisplay;
        String leagueSymbol = "";

        if (plugin.getDisplayManager() != null) {
            // DisplayManager: fonte unificada e sincronizada
            playerName = plugin.getDisplayManager().getNameToDisplay(player);
            prefix = plugin.getDisplayManager().getCurrentPrefix(player);
            medalDisplay = plugin.getDisplayManager().getMedalDisplay(player);
        } else {
            // Fallback caso DisplayManager não esteja disponível
            boolean hasFakeNick = plugin.getFakeNickManager() != null
                    && plugin.getFakeNickManager().hasFakeNick(player);
            playerName = hasFakeNick ? plugin.getFakeNickManager().getDisplayName(player) : player.getName();

            medalDisplay = "";
            if (plugin.getMedalManager() != null) {
                medalDisplay = plugin.getMedalManager().getPlayerMedalDisplay(player);
            }

            prefix = "";
            if (plugin.getTagManager() != null) {
                com.haumea.kitpvp.models.Tag playerTag = plugin.getTagManager().getPlayerTag(player);
                if (playerTag != null && !playerTag.getPrefix().isEmpty()) {
                    prefix = ChatStorage.colorize(playerTag.getPrefix());
                }
            }

            if (prefix.isEmpty()) {
                GroupManager groupManager = plugin.getGroupManager();
                Group playerGroup = groupManager.getPlayerGroup(player);
                if (playerGroup != null && !playerGroup.getPrefix().isEmpty()) {
                    prefix = ChatStorage.colorize(playerGroup.getPrefix());
                }
            }
        }

        // Obter símbolo da liga do jogador (via LeagueManager)
        if (plugin.getLeagueManager() != null) {
            com.haumea.kitpvp.models.PlayerRank rank = plugin.getLeagueManager().getRank(player);
            if (rank != null) {
                leagueSymbol = rank.getSymbol() + " "; // Ex: "§6✹ "
            }
        }

        // Verificar se o jogador tem permissão de mensagem destacada
        boolean hasHighlightPermission = player.hasPermission("haumea.chat.destaque");

        if (!prefix.isEmpty()) {
            // Formato: [MEDALHA] [SIMBOLO_LIGA] [TAG/GRUPO] Nome: Mensagem
            String format = medalDisplay + leagueSymbol + prefix + playerName + "§7: §f%2$s";

            if (hasHighlightPermission) {
                // Mensagem destacada com espaços antes e depois
                format = "\n" + medalDisplay + leagueSymbol + prefix + playerName + "§7: §f%2$s\n ";
            }

            event.setFormat(format);
        } else {
            // Formato padrão sem prefixo (mas com medalha e símbolo da liga)
            String format = medalDisplay + leagueSymbol + "§7" + playerName + "§7: §f%2$s";

            if (hasHighlightPermission) {
                format = "\n" + medalDisplay + leagueSymbol + "§7" + playerName + "§7: §f%2$s\n ";
            }

            event.setFormat(format);
        }
    }

    /**
     * Envia uma mensagem para todos os membros do staff
     * 
     * @param sender  Jogador que enviou
     * @param message Mensagem
     */
    private void sendStaffMessage(Player sender, String message) {
        // Obter prefixo do jogador (tag ou grupo)
        String prefix = "";

        // Tentar obter do DisplayManager primeiro (fonte única de verdade)
        if (plugin.getDisplayManager() != null) {
            prefix = plugin.getDisplayManager().getCurrentPrefix(sender);
        } else {
            // Fallback: buscar manualmente
            if (plugin.getTagManager() != null) {
                com.haumea.kitpvp.models.Tag playerTag = plugin.getTagManager().getPlayerTag(sender);
                if (playerTag != null && !playerTag.getPrefix().isEmpty()) {
                    prefix = ChatStorage.colorize(playerTag.getPrefix());
                }
            }

            if (prefix.isEmpty() && plugin.getGroupManager() != null) {
                Group playerGroup = plugin.getGroupManager().getPlayerGroup(sender);
                if (playerGroup != null && !playerGroup.getPrefix().isEmpty()) {
                    prefix = ChatStorage.colorize(playerGroup.getPrefix());
                }
            }
        }

        // Formato: §e§l[STAFF] [Tag][Nome]§7» §f[Mensagem]
        String formattedMessage = ChatStorage.colorize("&e&l[STAFF] " + prefix + sender.getName() + "&7» &f" + message);

        // Enviar para console
        Bukkit.getConsoleSender().sendMessage(formattedMessage);

        // Enviar para todos os staffs online
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (plugin.getGroupManager().isStaff(player)) {
                // Verificar preferência de ver as mensagens do staff-chat
                PlayerProfile staffProfile = plugin.getProfileManager().getProfile(player);
                if (staffProfile != null) {
                    boolean showStaffChat = staffProfile.getData().getCustomData("pref_staffchat_messages", true);
                    if (showStaffChat) {
                        player.sendMessage(formattedMessage);
                    }
                } else {
                    player.sendMessage(formattedMessage);
                }
            }
        }
    }
}
