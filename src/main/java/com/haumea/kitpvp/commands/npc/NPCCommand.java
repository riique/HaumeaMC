package com.haumea.kitpvp.commands.npc;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.commands.base.BaseCommand;
import com.haumea.kitpvp.commands.base.CommandInfo;
import com.haumea.kitpvp.managers.NPCManager;
import com.haumea.kitpvp.utils.ChatStorage;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Comando para gerenciar NPCs do Lobby.
 * 
 * Uso:
 * /npc create <id> <servidor> <skin> - Cria um NPC na sua posição atual
 * /npc remove <id> - Remove um NPC
 * /npc list - Lista todos os NPCs
 * /npc reload - Recarrega os NPCs
 * /npc teleport <id> - Teleporta para um NPC
 * 
 * @author HaumeaMC
 */
@CommandInfo(name = "npc", permission = "haumea.admin.npc", playerOnly = true, description = "Gerencia NPCs do Lobby")
public class NPCCommand extends BaseCommand {

    public NPCCommand(HaumeaMC plugin) {
        super(plugin);
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;

        // Verificar se está no Lobby
        if (!plugin.isLobby()) {
            ChatStorage.sendRaw(player, "&c&lERRO! &7Este comando só funciona no servidor de Lobby.");
            return;
        }

        NPCManager npcManager = plugin.getNPCManager();
        if (npcManager == null) {
            ChatStorage.sendRaw(player, "&c&lERRO! &7Sistema de NPCs não está disponível.");
            ChatStorage.sendRaw(player, "&7Verifique se o ProtocolLib está instalado.");
            return;
        }

        if (args.length == 0) {
            showHelp(player);
            return;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "create":
            case "criar":
                handleCreate(player, args);
                break;

            case "remove":
            case "remover":
            case "delete":
            case "deletar":
                handleRemove(player, args);
                break;

            case "list":
            case "lista":
                handleList(player);
                break;

            case "reload":
            case "recarregar":
                handleReload(player);
                break;

            case "tp":
            case "teleport":
            case "ir":
                handleTeleport(player, args);
                break;

            case "setskin":
            case "skin":
                handleSetSkin(player, args);
                break;

            default:
                showHelp(player);
                break;
        }
    }

    /**
     * Mostra ajuda do comando
     */
    private void showHelp(Player player) {
        ChatStorage.sendRaw(player, "");
        ChatStorage.sendRaw(player, "&6&l  NPC &8- &7Gerenciador de NPCs do Lobby");
        ChatStorage.sendRaw(player, "");
        ChatStorage.sendRaw(player, "&e  Comandos:");
        ChatStorage.sendRaw(player, "    &a/npc create <id> <servidor> <skin> &8- &7Cria NPC na sua posição");
        ChatStorage.sendRaw(player, "    &a/npc remove <id> &8- &7Remove um NPC");
        ChatStorage.sendRaw(player, "    &a/npc list &8- &7Lista todos os NPCs");
        ChatStorage.sendRaw(player, "    &a/npc tp <id> &8- &7Teleporta para um NPC");
        ChatStorage.sendRaw(player, "    &a/npc setskin <id> <skin> &8- &7Altera a skin de um NPC");
        ChatStorage.sendRaw(player, "    &a/npc reload &8- &7Recarrega os NPCs");
        ChatStorage.sendRaw(player, "");
        ChatStorage.sendRaw(player, "&7  Exemplo: &f/npc create kitpvp kitpvp Technoblade");
        ChatStorage.sendRaw(player, "");
    }

    /**
     * Cria um novo NPC na posição do jogador
     */
    private void handleCreate(Player player, String[] args) {
        if (args.length < 4) {
            ChatStorage.sendRaw(player, "&c&lUSO: &f/npc create <id> <servidor> <skin>");
            ChatStorage.sendRaw(player, "");
            ChatStorage.sendRaw(player, "&7  id &8- &fIdentificador único do NPC (ex: kitpvp)");
            ChatStorage.sendRaw(player, "&7  servidor &8- &fNome do servidor no Velocity (ex: kitpvp)");
            ChatStorage.sendRaw(player, "&7  skin &8- &fNome de um jogador Mojang (ex: Technoblade)");
            return;
        }

        String id = args[1].toLowerCase();
        String server = args[2];
        String skin = args[3];

        // Pegar posição do jogador
        Location loc = player.getLocation();

        // Salvar no config
        String basePath = "lobby.npcs." + id;
        plugin.getConfig().set(basePath + ".display-name", "&6&l" + id.toUpperCase());
        plugin.getConfig().set(basePath + ".skin", skin);
        plugin.getConfig().set(basePath + ".server", server);
        plugin.getConfig().set(basePath + ".location.world", loc.getWorld().getName());
        plugin.getConfig().set(basePath + ".location.x", Math.floor(loc.getX()) + 0.5);
        plugin.getConfig().set(basePath + ".location.y", loc.getY());
        plugin.getConfig().set(basePath + ".location.z", Math.floor(loc.getZ()) + 0.5);
        plugin.getConfig().set(basePath + ".location.yaw", loc.getYaw());
        plugin.getConfig().set(basePath + ".location.pitch", 0.0);

        // Hologram padrão
        java.util.List<String> hologram = new java.util.ArrayList<>();
        hologram.add("&6&l" + id.toUpperCase());
        hologram.add("&7Jogando: &a{players}");
        hologram.add("");
        hologram.add("&e&lCLIQUE PARA JOGAR");
        plugin.getConfig().set(basePath + ".hologram", hologram);

        plugin.saveConfig();

        ChatStorage.sendRaw(player, "");
        ChatStorage.sendRaw(player, "&a&l✓ NPC CRIADO!");
        ChatStorage.sendRaw(player, "");
        ChatStorage.sendRaw(player, "&7  ID: &f" + id);
        ChatStorage.sendRaw(player, "&7  Servidor: &f" + server);
        ChatStorage.sendRaw(player, "&7  Skin: &f" + skin);
        ChatStorage.sendRaw(player, "&7  Posição: &f" + formatLocation(loc));
        ChatStorage.sendRaw(player, "");
        ChatStorage.sendRaw(player, "&e  Use &f/npc reload &epara aplicar as mudanças!");
        ChatStorage.sendRaw(player, "");
    }

    /**
     * Remove um NPC
     */
    private void handleRemove(Player player, String[] args) {
        if (args.length < 2) {
            ChatStorage.sendRaw(player, "&c&lUSO: &f/npc remove <id>");
            return;
        }

        String id = args[1].toLowerCase();
        String basePath = "lobby.npcs." + id;

        if (!plugin.getConfig().contains(basePath)) {
            ChatStorage.sendRaw(player, "&c&lERRO! &7NPC '" + id + "' não encontrado.");
            return;
        }

        plugin.getConfig().set(basePath, null);
        plugin.saveConfig();

        ChatStorage.sendRaw(player, "&a&l✓ &7NPC &f" + id + " &7removido com sucesso!");
        ChatStorage.sendRaw(player, "&e  Use &f/npc reload &epara aplicar as mudanças!");
    }

    /**
     * Lista todos os NPCs configurados
     */
    private void handleList(Player player) {
        org.bukkit.configuration.ConfigurationSection npcsSection = plugin.getConfig()
                .getConfigurationSection("lobby.npcs");

        if (npcsSection == null || npcsSection.getKeys(false).isEmpty()) {
            ChatStorage.sendRaw(player, "&c&lNenhum NPC configurado!");
            ChatStorage.sendRaw(player, "&7Use &f/npc create <id> <servidor> <skin> &7para criar.");
            return;
        }

        ChatStorage.sendRaw(player, "");
        ChatStorage.sendRaw(player, "&6&l  NPCs CONFIGURADOS");
        ChatStorage.sendRaw(player, "");

        for (String id : npcsSection.getKeys(false)) {
            String server = plugin.getConfig().getString("lobby.npcs." + id + ".server", "?");
            String skin = plugin.getConfig().getString("lobby.npcs." + id + ".skin", "?");
            String world = plugin.getConfig().getString("lobby.npcs." + id + ".location.world", "?");
            double x = plugin.getConfig().getDouble("lobby.npcs." + id + ".location.x", 0);
            double y = plugin.getConfig().getDouble("lobby.npcs." + id + ".location.y", 0);
            double z = plugin.getConfig().getDouble("lobby.npcs." + id + ".location.z", 0);

            ChatStorage.sendRaw(player, "  &e" + id);
            ChatStorage.sendRaw(player, "    &7Servidor: &f" + server + " &8| &7Skin: &f" + skin);
            ChatStorage.sendRaw(player, "    &7Pos: &f" + world + " " + (int) x + ", " + (int) y + ", " + (int) z);
        }

        ChatStorage.sendRaw(player, "");
    }

    /**
     * Recarrega os NPCs
     */
    private void handleReload(Player player) {
        NPCManager npcManager = plugin.getNPCManager();
        if (npcManager == null) {
            ChatStorage.sendRaw(player, "&c&lERRO! &7Sistema de NPCs não está disponível.");
            return;
        }

        // Desligar manager atual
        npcManager.shutdown();

        // Recarregar config
        plugin.reloadConfig();

        // Recriar NPCManager
        ChatStorage.sendRaw(player, "&7Recarregando NPCs...");

        // Nota: O NPCManager será recriado automaticamente no próximo restart
        // Por agora, vamos apenas informar que precisa reiniciar
        ChatStorage.sendRaw(player, "");
        ChatStorage.sendRaw(player, "&a&l✓ &7Configuração recarregada!");
        ChatStorage.sendRaw(player, "&e  Reinicie o servidor para aplicar as mudanças nos NPCs.");
        ChatStorage.sendRaw(player, "");
    }

    /**
     * Teleporta para um NPC
     */
    private void handleTeleport(Player player, String[] args) {
        if (args.length < 2) {
            ChatStorage.sendRaw(player, "&c&lUSO: &f/npc tp <id>");
            return;
        }

        String id = args[1].toLowerCase();
        String basePath = "lobby.npcs." + id;

        if (!plugin.getConfig().contains(basePath)) {
            ChatStorage.sendRaw(player, "&c&lERRO! &7NPC '" + id + "' não encontrado.");
            return;
        }

        String worldName = plugin.getConfig().getString(basePath + ".location.world", "world");
        org.bukkit.World world = org.bukkit.Bukkit.getWorld(worldName);

        if (world == null) {
            ChatStorage.sendRaw(player, "&c&lERRO! &7Mundo '" + worldName + "' não encontrado.");
            return;
        }

        double x = plugin.getConfig().getDouble(basePath + ".location.x", 0);
        double y = plugin.getConfig().getDouble(basePath + ".location.y", 64);
        double z = plugin.getConfig().getDouble(basePath + ".location.z", 0);
        float yaw = (float) plugin.getConfig().getDouble(basePath + ".location.yaw", 0);
        float pitch = (float) plugin.getConfig().getDouble(basePath + ".location.pitch", 0);

        Location loc = new Location(world, x, y, z, yaw, pitch);
        player.teleport(loc);

        ChatStorage.sendRaw(player, "&a&l✓ &7Teleportado para o NPC &f" + id + "&7!");
    }

    /**
     * Altera a skin de um NPC
     */
    private void handleSetSkin(Player player, String[] args) {
        if (args.length < 3) {
            ChatStorage.sendRaw(player, "&c&lUSO: &f/npc setskin <id> <skin>");
            return;
        }

        String id = args[1].toLowerCase();
        String skin = args[2];
        String basePath = "lobby.npcs." + id;

        if (!plugin.getConfig().contains(basePath)) {
            ChatStorage.sendRaw(player, "&c&lERRO! &7NPC '" + id + "' não encontrado.");
            return;
        }

        plugin.getConfig().set(basePath + ".skin", skin);
        plugin.saveConfig();

        ChatStorage.sendRaw(player, "&a&l✓ &7Skin do NPC &f" + id + " &7alterada para &f" + skin + "&7!");
        ChatStorage.sendRaw(player, "&e  Reinicie o servidor para aplicar a mudança.");
    }

    /**
     * Formata localização para exibição
     */
    private String formatLocation(Location loc) {
        return String.format("%s %.1f, %.1f, %.1f",
                loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ());
    }
}
