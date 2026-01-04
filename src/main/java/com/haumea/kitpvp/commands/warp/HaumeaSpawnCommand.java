package com.haumea.kitpvp.commands.warp;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.managers.WarpsManager;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Comando /haumeaspawn - Define o spawn do servidor.
 * 
 * Este comando define o local onde todos os jogadores irão spawnar
 * quando entrarem no servidor pela primeira vez ou ao morrer (se configurado).
 * 
 * O spawn é salvo como uma warp especial chamada "spawn" no warps.yml.
 * 
 * Uso:
 * - /haumeaspawn - Define o spawn na sua localização atual (raio padrão do
 * config)
 * - /haumeaspawn <raio> - Define o spawn com um raio de proteção específico
 * 
 * @author HaumeaMC
 */
public class HaumeaSpawnCommand implements CommandExecutor {

    private final HaumeaMC plugin;
    private final WarpsManager warpsManager;

    private static final String PREFIX = "§6§lHAUMEAMC §f";
    private static final String PERMISSION = "haumea.spawn.admin";
    private static final String SPAWN_WARP_NAME = "spawn";

    public HaumeaSpawnCommand(HaumeaMC plugin) {
        this.plugin = plugin;
        this.warpsManager = plugin.getWarpsManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Verificar permissão
        if (!sender.hasPermission(PERMISSION)) {
            sender.sendMessage(PREFIX + "Você não possui §4§lPERMISSÃO §fpara isso.");
            return true;
        }

        // Verificar se é jogador
        if (!(sender instanceof Player)) {
            sender.sendMessage(PREFIX + "Este comando só pode ser executado por §c§lJOGADORES§f.");
            return true;
        }

        Player player = (Player) sender;
        Location location = player.getLocation();

        // Verificar se foi passado um raio
        double novoRaio = -1;
        if (args.length >= 1) {
            try {
                novoRaio = Double.parseDouble(args[0]);
                if (novoRaio <= 0) {
                    player.sendMessage(PREFIX + "O raio deve ser um número §e§lpositivo§f!");
                    return true;
                }
                if (novoRaio > 500) {
                    player.sendMessage(PREFIX + "O raio máximo permitido é §e§l500 blocos§f!");
                    return true;
                }
            } catch (NumberFormatException e) {
                player.sendMessage(PREFIX + "Uso: §e/setspawn §7ou §e/setspawn <raio>");
                player.sendMessage(PREFIX + "Exemplo: §e/setspawn 50");
                return true;
            }
        }

        // Verificar se já existe
        boolean isUpdate = warpsManager.warpExists(SPAWN_WARP_NAME);

        // Criar/atualizar a warp "spawn"
        warpsManager.setWarp(SPAWN_WARP_NAME, location);

        // Atualizar o raio no config se foi especificado
        double raioAtual;
        if (novoRaio > 0) {
            plugin.getConfig().set("spawn.raio", novoRaio);
            plugin.saveConfig();
            raioAtual = novoRaio;

            // Atualizar o StateManager
            if (plugin.getStateManager() != null) {
                plugin.getStateManager().setSpawnRadius(novoRaio);
            }
        } else {
            raioAtual = plugin.getConfig().getDouble("spawn.raio", 50.0);
        }

        // Feedback visual
        player.sendMessage("");
        player.sendMessage("§8§m----------------------------------------");
        player.sendMessage("");
        player.sendMessage("  §6§lSPAWN DO SERVIDOR " + (isUpdate ? "§e§lATUALIZADO" : "§a§lDEFINIDO"));
        player.sendMessage("");
        player.sendMessage("  §fMundo: §e" + location.getWorld().getName());
        player.sendMessage("  §fCoordenadas: §e" + String.format("%.2f, %.2f, %.2f",
                location.getX(), location.getY(), location.getZ()));
        player.sendMessage("  §fRotação: §e" + String.format("%.1f, %.1f",
                location.getYaw(), location.getPitch()));
        player.sendMessage("");
        player.sendMessage("  §6Raio de proteção: §e" + (int) raioAtual + " blocos");
        player.sendMessage("");
        player.sendMessage("  §7Jogadores dentro deste raio estão protegidos.");
        player.sendMessage("  §7Use §e/spawn §7para teleportar ao spawn.");
        if (novoRaio <= 0) {
            player.sendMessage("  §8Dica: Use /setspawn <raio> para alterar o raio.");
        }
        player.sendMessage("");
        player.sendMessage("§8§m----------------------------------------");
        player.sendMessage("");

        return true;
    }
}
