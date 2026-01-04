package com.haumea.kitpvp.commands.vip;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.commands.base.BaseCommand;
import com.haumea.kitpvp.commands.base.CommandInfo;
import com.haumea.kitpvp.managers.VipKeyManager;
import com.haumea.kitpvp.managers.VipKeyManager.RedeemResult;
import com.haumea.kitpvp.models.Group;
import com.haumea.kitpvp.models.VipKey;
import com.haumea.kitpvp.utils.ChatStorage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Comando /haumeavip
 * 
 * Jogadores: Ativar chaves VIP e ver informações
 * Staff: Criar, listar e gerenciar chaves
 * 
 * @author HaumeaMC
 */
@CommandInfo(name = "haumeavip", aliases = { "hvip", "vipkey" }, permission = "", playerOnly = false)
public class HaumeaVipCommand extends BaseCommand {

    public HaumeaVipCommand(HaumeaMC plugin) {
        super(plugin);
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sendUsage(sender);
            return;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "info":
                handleInfo(sender);
                break;
            case "criar":
            case "create":
                handleCreate(sender, args);
                break;
            case "listar":
            case "list":
                handleList(sender, args);
                break;
            case "deletar":
            case "delete":
                handleDelete(sender, args);
                break;
            case "check":
            case "verificar":
                handleCheck(sender, args);
                break;
            default:
                // Tentar ativar como código
                if (sender instanceof Player) {
                    handleRedeem((Player) sender, args[0]);
                } else {
                    ChatStorage.send(sender, "vipkey.error.player-only");
                }
                break;
        }
    }

    /**
     * Ativa uma chave VIP
     */
    private void handleRedeem(Player player, String code) {
        VipKeyManager manager = plugin.getVipKeyManager();
        RedeemResult result = manager.redeemKey(player, code);

        switch (result) {
            case SUCCESS:
            case SUCCESS_EXTENDED:
                // Mensagens já são enviadas pelo manager
                break;
            case KEY_NOT_FOUND:
                ChatStorage.send(player, "vipkey.invalid");
                break;
            case KEY_ALREADY_USED:
                ChatStorage.send(player, "vipkey.already_used");
                break;
            case KEY_EXPIRED:
                ChatStorage.send(player, "vipkey.expired");
                break;
            case GROUP_NOT_FOUND:
                ChatStorage.send(player, "vipkey.error.group-not-found");
                break;
        }
    }

    /**
     * Mostra informações do VIP ativo do jogador
     */
    private void handleInfo(CommandSender sender) {
        if (!(sender instanceof Player)) {
            ChatStorage.send(sender, "error.player-only");
            return;
        }

        Player player = (Player) sender;
        UUID uuid = player.getUniqueId();

        // Obtém grupos VIP do jogador
        List<String> groups = plugin.getGroupManager().getPlayerGroupNames(uuid);

        if (groups.isEmpty() || (groups.size() == 1 && groups.get(0).equalsIgnoreCase("membro"))) {
            ChatStorage.send(player, "vipkey.no_vip");
            return;
        }

        ChatStorage.sendRaw(player, "");
        player.sendMessage(ChatStorage.getMessage("vipkey.info.header"));
        ChatStorage.sendRaw(player, "");

        for (String groupName : groups) {
            if (groupName.equalsIgnoreCase("membro"))
                continue;

            Group group = plugin.getGroupManager().getGroup(groupName);
            if (group == null)
                continue;

            String display = group.getDisplayName();
            long expiration = plugin.getGroupManager().getGroupExpiration(uuid, groupName);

            String timeStr;
            if (expiration == 0) {
                timeStr = "&6Permanente";
            } else {
                long remaining = expiration - System.currentTimeMillis();
                if (remaining <= 0) {
                    timeStr = "&cExpirado";
                } else {
                    timeStr = "&e" + ChatStorage.formatTime(remaining);
                }
            }

            ChatStorage.sendRaw(player, "  " + display + " &8- " + timeStr);
        }

        ChatStorage.sendRaw(player, "");
    }

    /**
     * Cria uma ou mais chaves VIP (admin)
     */
    private void handleCreate(CommandSender sender, String[] args) {
        if (!hasAdminPermission(sender)) {
            ChatStorage.send(sender, "error.no-permission");
            return;
        }

        // /haumeavip criar <grupo> <duração> [quantidade] [validade]
        if (args.length < 3) {
            sendCreateUsage(sender);
            return;
        }

        String groupName = args[1].toLowerCase();
        String durationStr = args[2];
        int quantity = 1;
        long keyExpiration = 0;

        // Quantidade (opcional)
        if (args.length >= 4) {
            try {
                quantity = Integer.parseInt(args[3]);
                if (quantity < 1)
                    quantity = 1;
                if (quantity > 100)
                    quantity = 100;
            } catch (NumberFormatException e) {
                ChatStorage.send(sender, "vipkey.error.invalid-quantity", "value", args[3]);
                return;
            }
        }

        // Validade da chave (opcional)
        if (args.length >= 5) {
            keyExpiration = VipKeyManager.parseDuration(args[4]);
            if (keyExpiration == -1) {
                ChatStorage.send(sender, "vipkey.error.invalid-expiry", "value", args[4]);
                return;
            }
            if (keyExpiration > 0) {
                keyExpiration = System.currentTimeMillis() + keyExpiration;
            }
        }

        // Verifica grupo
        if (!plugin.getGroupManager().groupExists(groupName)) {
            ChatStorage.send(sender, "vipkey.admin.group_invalid", "grupo", groupName);
            return;
        }

        // Converte duração
        long duration = VipKeyManager.parseDuration(durationStr);
        if (duration == -1) {
            ChatStorage.send(sender, "vipkey.error.invalid-duration", "value", durationStr);
            return;
        }

        // Quem criou
        String createdBy = sender instanceof Player
                ? ((Player) sender).getUniqueId().toString()
                : "CONSOLE";

        // Cria as chaves
        VipKeyManager manager = plugin.getVipKeyManager();
        List<VipKey> keys = manager.createKeys(groupName, duration, keyExpiration, createdBy, quantity);

        if (keys.isEmpty()) {
            ChatStorage.send(sender, "vipkey.error.create-failed");
            return;
        }

        // Feedback
        if (quantity == 1) {
            ChatStorage.sendRaw(sender, "");
            sender.sendMessage(ChatStorage.getMessage("vipkey.admin.created-single"));
            ChatStorage.sendRaw(sender, "");
            ChatStorage.sendRaw(sender, "&e  " + keys.get(0).getKey());
            ChatStorage.sendRaw(sender, "");
            ChatStorage.sendRaw(sender, "&7  Grupo: &f" + groupName);
            ChatStorage.sendRaw(sender, "&7  Duração: &f" + VipKeyManager.formatDuration(duration));
            if (keyExpiration > 0) {
                ChatStorage.sendRaw(sender,
                        "&7  Validade: &f" + ChatStorage.formatTime(keyExpiration - System.currentTimeMillis()));
            }
            ChatStorage.sendRaw(sender, "");
        } else {
            ChatStorage.sendRaw(sender, "");
            sender.sendMessage(ChatStorage.getMessage("vipkey.admin.created-multi", "count", String.valueOf(quantity)));
            ChatStorage.sendRaw(sender, "");
            for (VipKey key : keys) {
                ChatStorage.sendRaw(sender, "  &e" + key.getKey());
            }
            ChatStorage.sendRaw(sender, "");
            ChatStorage.sendRaw(sender, "&7  Grupo: &f" + groupName);
            ChatStorage.sendRaw(sender, "&7  Duração: &f" + VipKeyManager.formatDuration(duration));
            ChatStorage.sendRaw(sender, "");
        }
    }

    /**
     * Lista chaves não usadas (admin)
     */
    private void handleList(CommandSender sender, String[] args) {
        if (!hasAdminPermission(sender)) {
            ChatStorage.send(sender, "error.no-permission");
            return;
        }

        VipKeyManager manager = plugin.getVipKeyManager();
        Collection<VipKey> keys;
        String filterGroup = null;

        if (args.length >= 2) {
            filterGroup = args[1].toLowerCase();
            keys = manager.getUnusedKeysByGroup(filterGroup);
        } else {
            keys = manager.getUnusedKeys();
        }

        if (keys.isEmpty()) {
            if (filterGroup != null) {
                ChatStorage.send(sender, "vipkey.admin.no-keys-group", "group", filterGroup);
            } else {
                ChatStorage.send(sender, "vipkey.admin.no-keys");
            }
            return;
        }

        ChatStorage.sendRaw(sender, "");
        String header = filterGroup != null
                ? "&6&l  CHAVES NÃO USADAS &8(" + filterGroup + ")"
                : "&6&l  CHAVES NÃO USADAS";
        ChatStorage.sendRaw(sender, header);
        ChatStorage.sendRaw(sender, "");

        int count = 0;
        for (VipKey key : keys) {
            if (count >= 20) {
                ChatStorage.sendRaw(sender, "  &7... e mais " + (keys.size() - 20) + " chaves");
                break;
            }

            String duration = VipKeyManager.formatDuration(key.getDuration());
            ChatStorage.sendRaw(sender, "  &e" + key.getKey() + " &8- &7" + key.getGroupName() + " &8| &7" + duration);
            count++;
        }

        ChatStorage.sendRaw(sender, "");
        ChatStorage.sendRaw(sender, "&7  Total: &f" + keys.size() + " chave(s)");
        ChatStorage.sendRaw(sender, "");
    }

    /**
     * Deleta uma chave (admin)
     */
    private void handleDelete(CommandSender sender, String[] args) {
        if (!hasAdminPermission(sender)) {
            ChatStorage.send(sender, "error.no-permission");
            return;
        }

        if (args.length < 2) {
            ChatStorage.send(sender, "vipkey.admin.delete-usage");
            return;
        }

        String code = args[1].toUpperCase();
        VipKeyManager manager = plugin.getVipKeyManager();

        VipKey key = manager.getKeyInfo(code);
        if (key == null) {
            ChatStorage.send(sender, "vipkey.admin.not_found");
            return;
        }

        if (key.isUsed()) {
            ChatStorage.send(sender, "vipkey.admin.already-used");
            return;
        }

        if (manager.deleteKey(code)) {
            ChatStorage.send(sender, "vipkey.admin.deleted", "chave", code);
        } else {
            ChatStorage.send(sender, "vipkey.error.delete-failed");
        }
    }

    /**
     * Verifica informações de uma chave (admin)
     */
    private void handleCheck(CommandSender sender, String[] args) {
        if (!hasAdminPermission(sender)) {
            ChatStorage.send(sender, "error.no-permission");
            return;
        }

        if (args.length < 2) {
            ChatStorage.send(sender, "vipkey.admin.check-usage");
            return;
        }

        String code = args[1].toUpperCase();
        VipKeyManager manager = plugin.getVipKeyManager();
        VipKey key = manager.getKeyInfo(code);

        if (key == null) {
            ChatStorage.send(sender, "vipkey.admin.not_found");
            return;
        }

        ChatStorage.sendRaw(sender, "");
        sender.sendMessage(ChatStorage.getMessage("vipkey.admin.info-header"));
        ChatStorage.sendRaw(sender, "");
        ChatStorage.sendRaw(sender, "&7  Código: &e" + key.getKey());
        ChatStorage.sendRaw(sender, "&7  Grupo: &f" + key.getGroupName());
        ChatStorage.sendRaw(sender, "&7  Duração VIP: &f" + VipKeyManager.formatDuration(key.getDuration()));
        ChatStorage.sendRaw(sender, "&7  Criada por: &f" + key.getCreatedBy());

        String status;
        if (key.isUsed()) {
            status = "&cUsada por " + key.getUsedBy();
        } else if (key.isKeyExpired()) {
            status = "&cExpirada";
        } else {
            status = "&aDisponível";
        }
        ChatStorage.sendRaw(sender, "&7  Status: " + status);

        if (key.getKeyExpiresAt() > 0 && !key.isUsed()) {
            long remaining = key.getKeyExpiresAt() - System.currentTimeMillis();
            String expStr = remaining > 0 ? ChatStorage.formatTime(remaining) : "Expirada";
            ChatStorage.sendRaw(sender, "&7  Validade: &f" + expStr);
        }

        ChatStorage.sendRaw(sender, "");
    }

    /**
     * Verifica se o sender tem permissão de admin
     */
    private boolean hasAdminPermission(CommandSender sender) {
        if (!(sender instanceof Player)) {
            return true; // Console tem permissão
        }

        Player player = (Player) sender;
        return player.hasPermission("haumea.vip.admin") ||
                plugin.getGroupManager().isAdmin(player);
    }

    /**
     * Envia mensagem de uso geral
     */
    private void sendUsage(CommandSender sender) {
        ChatStorage.sendRaw(sender, "");
        sender.sendMessage(ChatStorage.getMessage("vipkey.help.header"));
        ChatStorage.sendRaw(sender, "");
        ChatStorage.sendRaw(sender, "&e  Uso para jogadores:");
        ChatStorage.sendRaw(sender, "    &a• /haumeavip <código> &8- &7Ativar uma chave VIP");
        ChatStorage.sendRaw(sender, "    &a• /haumeavip info &8- &7Ver seus grupos VIP ativos");

        if (hasAdminPermission(sender)) {
            ChatStorage.sendRaw(sender, "");
            ChatStorage.sendRaw(sender, "&e  Uso para administradores:");
            ChatStorage.sendRaw(sender, "    &c• /haumeavip criar <grupo> <duração> [qtd] [validade]");
            ChatStorage.sendRaw(sender, "    &c• /haumeavip listar [grupo]");
            ChatStorage.sendRaw(sender, "    &c• /haumeavip check <código>");
            ChatStorage.sendRaw(sender, "    &c• /haumeavip deletar <código>");
        }

        ChatStorage.sendRaw(sender, "");
    }

    /**
     * Envia uso do subcomando criar
     */
    private void sendCreateUsage(CommandSender sender) {
        ChatStorage.sendRaw(sender, "");
        sender.sendMessage(ChatStorage.getMessage("vipkey.help.create-header"));
        ChatStorage.sendRaw(sender, "");
        ChatStorage.sendRaw(sender, "&e  Uso: &f/haumeavip criar <grupo> <duração> [quantidade] [validade]");
        ChatStorage.sendRaw(sender, "");
        ChatStorage.sendRaw(sender, "&6  Argumentos:");
        ChatStorage.sendRaw(sender, "    &a• grupo &8- &7Nome do grupo (vip, vip+, midia...)");
        ChatStorage.sendRaw(sender, "    &e• duração &8- &7Tempo do VIP (30d, 1w, permanent...)");
        ChatStorage.sendRaw(sender, "    &7• quantidade &8- &7Quantas chaves criar (opcional, máx 100)");
        ChatStorage.sendRaw(sender, "    &7• validade &8- &7Tempo para usar a chave (opcional)");
        ChatStorage.sendRaw(sender, "");
        for (String msg : ChatStorage.getMessageList("vipkey.help.formats")) {
            ChatStorage.sendRaw(sender, msg);
        }
        ChatStorage.sendRaw(sender, "");
        ChatStorage.sendRaw(sender, "&6  Exemplos:");
        ChatStorage.sendRaw(sender, "    &f/haumeavip criar vip 30d");
        ChatStorage.sendRaw(sender, "    &f/haumeavip criar vip+ 1w 5");
        ChatStorage.sendRaw(sender, "    &f/haumeavip criar vip 1mo");
        ChatStorage.sendRaw(sender, "    &f/haumeavip criar midia permanent 1 7d");
        ChatStorage.sendRaw(sender, "");
    }
}
