package com.haumea.kitpvp.commands.warp;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.managers.WarpsManager;
import com.haumea.kitpvp.models.Warp;
import com.haumea.kitpvp.utils.ChatStorage;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Comando /haumeawarp para gerenciamento de warps administrativas.
 * 
 * Subcomandos:
 * - /haumeawarp set <nome> - Define uma warp na sua localização atual
 * - /haumeawarp del <nome> - Remove uma warp
 * - /haumeawarp list - Lista todas as warps
 * - /haumeawarp tp <nome> - Teleporta para uma warp
 * - /haumeawarp reload - Recarrega as warps do arquivo
 * 
 * @author HaumeaMC
 */
public class HaumeaWarpCommand implements CommandExecutor, TabCompleter {

    private final HaumeaMC plugin;
    private final WarpsManager warpsManager;

    // Permissão administrativa
    private static final String PERMISSION_ADMIN = "haumea.warp.admin";

    public HaumeaWarpCommand(HaumeaMC plugin) {
        this.plugin = plugin;
        this.warpsManager = plugin.getWarpsManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Verificar permissão
        if (!sender.hasPermission(PERMISSION_ADMIN)) {
            ChatStorage.send(sender, "error.no-permission");
            return true;
        }

        // Sem argumentos - mostrar ajuda
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "set":
                return handleSet(sender, args);
            case "del":
            case "delete":
            case "remove":
                return handleDelete(sender, args);
            case "list":
            case "lista":
                return handleList(sender);
            case "tp":
            case "teleport":
            case "ir":
                return handleTeleport(sender, args);
            case "reload":
                return handleReload(sender);
            default:
                sendHelp(sender);
                return true;
        }
    }

    /**
     * Processa o subcomando 'set' - Define uma nova warp.
     * Uso: /haumeawarp set <nome> [raio]
     */
    private boolean handleSet(CommandSender sender, String[] args) {
        // Verificar se é jogador
        if (!(sender instanceof Player)) {
            ChatStorage.send(sender, "error.player-only");
            return true;
        }

        // Verificar argumento
        if (args.length < 2) {
            ChatStorage.send(sender, "warp.admin.set-usage");
            return true;
        }

        Player player = (Player) sender;
        String warpName = args[1].toLowerCase();
        Location location = player.getLocation();

        // Parsear raio opcional (terceiro argumento)
        double radius = 0.0;
        if (args.length >= 3) {
            try {
                radius = Double.parseDouble(args[2]);
                if (radius < 0) {
                    radius = 0;
                }
            } catch (NumberFormatException e) {
                ChatStorage.send(player, "warp.admin.invalid-radius");
                return true;
            }
        }

        // Verificar se já existe (avisar, mas permitir sobrescrever)
        boolean isUpdate = warpsManager.warpExists(warpName);

        // Criar/atualizar a warp com raio
        Warp warp = warpsManager.setWarp(warpName, location, radius);

        if (isUpdate) {
            if (radius > 0) {
                ChatStorage.send(player, "warp.admin.updated-with-radius",
                        "warp", warpName.toUpperCase(),
                        "radius", String.format("%.1f", radius));
            } else {
                ChatStorage.send(player, "warp.admin.updated", "warp", warpName.toUpperCase());
            }
        } else {
            if (radius > 0) {
                ChatStorage.send(player, "warp.admin.set-with-radius",
                        "warp", warpName.toUpperCase(),
                        "radius", String.format("%.1f", radius));
            } else {
                ChatStorage.send(player, "warp.admin.set", "warp", warpName.toUpperCase());
            }
        }

        return true;
    }

    /**
     * Processa o subcomando 'del' - Remove uma warp.
     */
    private boolean handleDelete(CommandSender sender, String[] args) {
        if (args.length < 2) {
            ChatStorage.send(sender, "warp.admin.del-usage");
            return true;
        }

        String warpName = args[1].toLowerCase();

        if (!warpsManager.warpExists(warpName)) {
            ChatStorage.send(sender, "warp.admin.not-found", "warp", warpName.toUpperCase());
            return true;
        }

        warpsManager.deleteWarp(warpName);
        ChatStorage.send(sender, "warp.admin.deleted", "warp", warpName.toUpperCase());

        return true;
    }

    /**
     * Processa o subcomando 'list' - Lista todas as warps.
     */
    private boolean handleList(CommandSender sender) {
        if (warpsManager.getWarpCount() == 0) {
            ChatStorage.send(sender, "warp.admin.no-warps");
            return true;
        }

        sender.sendMessage("");
        sender.sendMessage(
                ChatStorage.getMessage("warp.admin.list-header", "count", String.valueOf(warpsManager.getWarpCount())));
        sender.sendMessage("");

        for (Warp warp : warpsManager.getAllWarps()) {
            String status = warp.isValid() ? "§a✓" : "§c✗";
            sender.sendMessage(String.format("  %s §e%s §7- §f%s §7(%.1f, %.1f, %.1f)",
                    status,
                    warp.getName().toUpperCase(),
                    warp.getWorldName(),
                    warp.getX(),
                    warp.getY(),
                    warp.getZ()));
        }

        sender.sendMessage("");
        sender.sendMessage(ChatStorage.getMessage("system.separator"));
        sender.sendMessage("");

        return true;
    }

    /**
     * Processa o subcomando 'tp' - Teleporta para uma warp.
     */
    private boolean handleTeleport(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            ChatStorage.send(sender, "error.player-only");
            return true;
        }

        if (args.length < 2) {
            ChatStorage.send(sender, "warp.admin.tp-usage");
            return true;
        }

        Player player = (Player) sender;
        String warpName = args[1].toLowerCase();

        Warp warp = warpsManager.getWarp(warpName);
        if (warp == null) {
            ChatStorage.send(player, "warp.admin.not-found", "warp", warpName.toUpperCase());
            return true;
        }

        Location location = warp.toLocation();
        if (location == null) {
            ChatStorage.send(player, "warp.admin.world-not-loaded", "warp", warpName.toUpperCase());
            return true;
        }

        player.teleport(location);
        ChatStorage.send(player, "warp.admin.teleported", "warp", warpName.toUpperCase());

        return true;
    }

    /**
     * Processa o subcomando 'reload' - Recarrega as warps do arquivo.
     */
    private boolean handleReload(CommandSender sender) {
        warpsManager.reload();
        ChatStorage.send(sender, "warp.admin.reloaded", "count", String.valueOf(warpsManager.getWarpCount()));
        return true;
    }

    /**
     * Envia a mensagem de ajuda.
     */
    private void sendHelp(CommandSender sender) {
        sender.sendMessage("");
        sender.sendMessage(ChatStorage.getMessage("warp.admin.help-header"));
        sender.sendMessage("");
        ChatStorage.sendRaw(sender, "  &e/haumeawarp set <nome> [raio] &7- Define uma warp");
        ChatStorage.sendRaw(sender, "    &7Exemplo: &f/haumeawarp set fps 15 &8(raio de proteção de 15 blocos)");
        ChatStorage.sendRaw(sender, "  &e/haumeawarp del <nome> &7- Remove uma warp");
        ChatStorage.sendRaw(sender, "  &e/haumeawarp tp <nome> &7- Teleporta para uma warp");
        ChatStorage.sendRaw(sender, "  &e/haumeawarp list &7- Lista todas as warps");
        ChatStorage.sendRaw(sender, "  &e/haumeawarp reload &7- Recarrega as warps");
        sender.sendMessage("");
        sender.sendMessage(ChatStorage.getMessage("system.separator"));
        sender.sendMessage("");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (!sender.hasPermission(PERMISSION_ADMIN)) {
            return completions;
        }

        if (args.length == 1) {
            // Primeiro argumento - subcomandos
            List<String> subCommands = Arrays.asList("set", "del", "tp", "list", "reload");
            String partial = args[0].toLowerCase();
            completions = subCommands.stream()
                    .filter(s -> s.startsWith(partial))
                    .collect(Collectors.toList());
        } else if (args.length == 2) {
            // Segundo argumento - nome da warp (para del, tp)
            String subCommand = args[0].toLowerCase();
            if (subCommand.equals("del") || subCommand.equals("delete") ||
                    subCommand.equals("tp") || subCommand.equals("teleport") || subCommand.equals("ir")) {
                String partial = args[1].toLowerCase();
                completions = warpsManager.getWarpNames().stream()
                        .filter(s -> s.startsWith(partial))
                        .collect(Collectors.toList());
            }
        }

        return completions;
    }
}
