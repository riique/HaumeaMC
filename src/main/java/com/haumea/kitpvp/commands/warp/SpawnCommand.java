package com.haumea.kitpvp.commands.warp;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.managers.WarpsManager;
import com.haumea.kitpvp.models.Warp;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Comando /spawn - Atalho para teleportar à warp "spawn".
 * 
 * Este é um comando de conveniência que teleporta diretamente
 * para a warp chamada "spawn", se existir.
 * 
 * @author HaumeaMC
 */
public class SpawnCommand implements CommandExecutor {

    private final HaumeaMC plugin;
    private final WarpsManager warpsManager;

    private static final String PREFIX = "§6§lHAUMEAMC §f";
    private static final String SPAWN_WARP_NAME = "spawn";

    public SpawnCommand(HaumeaMC plugin) {
        this.plugin = plugin;
        this.warpsManager = plugin.getWarpsManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Verificar se é jogador
        if (!(sender instanceof Player)) {
            sender.sendMessage(PREFIX + "Este comando só pode ser executado por §c§lJOGADORES§f.");
            return true;
        }

        Player player = (Player) sender;

        // Verificar se está em combate
        if (plugin.getStateManager() != null && plugin.getStateManager().isInCombat(player)) {
            player.sendMessage(PREFIX + "§cVocê não pode ir ao spawn em combate!");
            return true;
        }

        // Buscar a warp "spawn"
        Warp spawnWarp = warpsManager.getWarp(SPAWN_WARP_NAME);

        if (spawnWarp == null) {
            player.sendMessage(PREFIX + "O spawn ainda não foi definido!");
            player.sendMessage(PREFIX + "Peça a um administrador para usar §e/haumeawarp set spawn");
            return true;
        }

        // Converter para Location
        Location location = spawnWarp.toLocation();
        if (location == null) {
            player.sendMessage(PREFIX + "O mundo do spawn não está carregado!");
            return true;
        }

        // IMPORTANTE: Remover jogador de qualquer warp FPS antes de teleportar
        if (plugin.getFPSWarpManager() != null) {
            String currentWarp = plugin.getFPSWarpManager().getPlayerWarp(player);
            if (currentWarp != null) {
                plugin.getFPSWarpManager().leaveWarp(player, currentWarp);
            }
        }

        // Teleportar
        player.teleport(location);

        // Dar itens de lobby
        if (plugin.getArenaItemsHandler() != null) {
            plugin.getArenaItemsHandler().giveLobbyItems(player);
        }

        player.sendMessage(PREFIX + "Você foi teleportado ao §e§lSPAWN§f!");

        return true;
    }
}
