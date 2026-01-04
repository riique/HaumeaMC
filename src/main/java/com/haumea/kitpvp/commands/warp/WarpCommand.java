package com.haumea.kitpvp.commands.warp;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.managers.WarpsManager;
import com.haumea.kitpvp.models.Warp;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Comando /warp para teleportar jogadores às warps.
 * 
 * Uso: /warp <nome>
 * 
 * Este comando permite que qualquer jogador vá para warps públicas.
 * Para criar warps, use /haumeawarp set <nome>
 * 
 * @author HaumeaMC
 */
public class WarpCommand implements CommandExecutor, TabCompleter {

    private final HaumeaMC plugin;
    private final WarpsManager warpsManager;

    private static final String PREFIX = "§6§lHAUMEAMC §f";

    public WarpCommand(HaumeaMC plugin) {
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

        // Sem argumentos - listar warps disponíveis
        if (args.length == 0) {
            if (warpsManager.getWarpCount() == 0) {
                player.sendMessage(PREFIX + "Não há nenhuma warp disponível.");
                return true;
            }

            String warpList = warpsManager.getWarpNames().stream()
                    .map(name -> "§e" + name)
                    .collect(Collectors.joining("§7, "));

            player.sendMessage(PREFIX + "Warps disponíveis: " + warpList);
            player.sendMessage(PREFIX + "Use: §e/warp <nome>");
            return true;
        }

        String warpName = args[0].toLowerCase();

        // Buscar a warp
        Warp warp = warpsManager.getWarp(warpName);
        if (warp == null) {
            player.sendMessage(PREFIX + "A warp §e§l" + warpName.toUpperCase() + " §fnão existe!");
            return true;
        }

        // Converter para Location
        Location location = warp.toLocation();
        if (location == null) {
            player.sendMessage(PREFIX + "O mundo da warp §e§l" + warpName.toUpperCase() + " §fnão está carregado!");
            return true;
        }

        // Verificar se está em combate
        if (plugin.getStateManager() != null && plugin.getStateManager().isInCombat(player)) {
            player.sendMessage(PREFIX + "§cVocê não pode trocar de warp em combate!");
            return true;
        }

        // Verificar se a warp tem proteção de spawn (FPS Mode)
        if (warp.hasProtectionRadius() && plugin.getFPSWarpManager() != null) {
            // Usar FPSWarpManager para gerenciar a entrada na warp
            // Isso limpa inventário, teleporta e adiciona proteção
            Warp previousWarp = null;
            String prevWarpName = plugin.getFPSWarpManager().getPlayerWarp(player);
            if (prevWarpName != null) {
                previousWarp = warpsManager.getWarp(prevWarpName);
            }

            plugin.getFPSWarpManager().joinWarp(player, warp, previousWarp);
            player.sendMessage(
                    PREFIX + "Você foi teleportado para §e§l" + warpName.toUpperCase() + "§f! §7(Proteção ativa)");
        } else {
            // Warp normal sem proteção - teleportar diretamente
            player.teleport(location);
            player.sendMessage(PREFIX + "Você foi teleportado para §e§l" + warpName.toUpperCase() + "§f!");
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            completions = warpsManager.getWarpNames().stream()
                    .filter(s -> s.startsWith(partial))
                    .collect(Collectors.toList());
        }

        return completions;
    }
}
