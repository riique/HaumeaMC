package com.haumea.kitpvp.listeners;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.abilities.Ability;
import com.haumea.kitpvp.abilities.AbilityManager;
import com.haumea.kitpvp.managers.KitManager;
import com.haumea.kitpvp.utils.ChatStorage;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Listener responsável por detectar quando o jogador sai do spawn
 * e dar os itens de combate + itens do kit.
 * 
 * Funcionamento:
 * - Monitora movimentação do jogador
 * - Quando sai da área protegida (spawn), dá itens de combate
 * - Quando entra na área protegida (spawn), dá itens de lobby
 * 
 * @author HaumeaMC
 */
public class SpawnTransitionListener implements Listener {

    private final HaumeaMC plugin;

    /**
     * Cache de estado de spawn por jogador.
     * true = estava no spawn, false = não estava
     */
    private final Map<UUID, Boolean> wasInSpawn = new HashMap<>();

    public SpawnTransitionListener(HaumeaMC plugin) {
        this.plugin = plugin;
    }

    /**
     * Detecta transição entre spawn e área de combate.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        // Otimização: ignorar movimentos que não mudam de bloco
        Location from = event.getFrom();
        Location to = event.getTo();

        if (to == null)
            return;
        if (from.getBlockX() == to.getBlockX() &&
                from.getBlockY() == to.getBlockY() &&
                from.getBlockZ() == to.getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Verificar se jogador está em modo admin - ignorar
        if (plugin.getProfileManager() != null) {
            com.haumea.kitpvp.profile.PlayerProfile profile = plugin.getProfileManager().getProfile(player);
            if (profile != null && profile.isVanish()) {
                return;
            }
        }

        // Verificar estado atual de spawn
        boolean isInSpawn = isInSpawnArea(player);
        Boolean previousState = wasInSpawn.get(uuid);

        // Se não tinha estado anterior, definir e retornar
        if (previousState == null) {
            wasInSpawn.put(uuid, isInSpawn);
            return;
        }

        // IMPORTANTE: Ignorar jogadores em warps com proteção própria (FPS Mode)
        // Esses jogadores são gerenciados pelo FPSWarpListener
        if (plugin.getFPSWarpManager() != null) {
            String playerWarp = plugin.getFPSWarpManager().getPlayerWarp(player);
            if (playerWarp != null) {
                // Jogador está em uma warp gerenciada pelo FPSWarpManager
                // Não processar transição de spawn aqui
                wasInSpawn.put(uuid, isInSpawn);
                return;
            }
        }

        // Verificar transição
        if (previousState && !isInSpawn) {
            // Saiu do spawn -> dar itens de combate
            onLeaveSpawn(player);
        } else if (!previousState && isInSpawn) {
            // Entrou no spawn -> dar itens de lobby
            onEnterSpawn(player);
        }

        // Atualizar estado
        wasInSpawn.put(uuid, isInSpawn);
    }

    /**
     * Chamado quando o jogador sai do spawn.
     * Dá itens de combate + itens do kit.
     */
    private void onLeaveSpawn(Player player) {
        if (plugin.getArenaItemsHandler() == null)
            return;

        KitManager kitManager = plugin.getKitManager();
        AbilityManager abilityManager = plugin.getAbilityManager();

        boolean hasKit = false;
        int totalKitItems = 0;

        // Contar quantos itens de kit serão dados
        if (kitManager != null && abilityManager != null) {
            String primaryKit = kitManager.getPrimaryKit(player);
            if (primaryKit != null && !primaryKit.isEmpty()) {
                Ability ability = abilityManager.getAbility(primaryKit);
                if (ability != null && ability.getItems() != null) {
                    totalKitItems += ability.getItems().length;
                    hasKit = true;
                }
            }

            String secondaryKit = kitManager.getSecondaryKit(player);
            if (secondaryKit != null && !secondaryKit.isEmpty()) {
                Ability ability = abilityManager.getAbility(secondaryKit);
                if (ability != null && ability.getItems() != null) {
                    totalKitItems += ability.getItems().length;
                    hasKit = true;
                }
            }
        }

        // Dar itens de combate (espada, bússola, sopas) - reservando slots para kits
        plugin.getArenaItemsHandler().giveCombatItems(player, hasKit, totalKitItems);

        // Agora dar os itens do kit nos slots reservados (1, 2, 3...)
        if (kitManager != null && abilityManager != null) {
            String primaryKit = kitManager.getPrimaryKit(player);
            if (primaryKit != null && !primaryKit.isEmpty()) {
                Ability ability = abilityManager.getAbility(primaryKit);
                if (ability != null) {
                    giveAbilityItems(player, ability);
                }
            }

            String secondaryKit = kitManager.getSecondaryKit(player);
            if (secondaryKit != null && !secondaryKit.isEmpty()) {
                Ability ability = abilityManager.getAbility(secondaryKit);
                if (ability != null) {
                    giveAbilityItems(player, ability);
                }
            }
        }

        // Mensagem de feedback
        if (hasKit) {
            ChatStorage.sendCustom(player, "§aVocê está pronto para o combate! §7Boa sorte!");
        }
    }

    /**
     * Dá os itens de uma ability ao jogador.
     * Coloca os itens em slots vazios ou no primeiro slot disponível.
     */
    private void giveAbilityItems(Player player, Ability ability) {
        ItemStack[] items = ability.getItems();
        if (items == null || items.length == 0)
            return;

        for (ItemStack item : items) {
            if (item == null)
                continue;

            // Encontrar slot vazio para o item
            int emptySlot = findEmptyHotbarSlot(player);
            if (emptySlot != -1) {
                player.getInventory().setItem(emptySlot, item.clone());
            } else {
                // Se não tem slot vazio na hotbar, adicionar ao inventário
                player.getInventory().addItem(item.clone());
            }
        }

        player.updateInventory();
    }

    /**
     * Encontra um slot vazio na hotbar (slots 1-7, excluindo 0 e 8).
     * Slot 0 = espada, Slot 8 = bússola
     */
    private int findEmptyHotbarSlot(Player player) {
        // Priorizar slots 1, 2 para itens de kit
        int[] prioritySlots = { 1, 2, 3, 4, 5, 6, 7 };

        for (int slot : prioritySlots) {
            ItemStack item = player.getInventory().getItem(slot);
            if (item == null || item.getType() == org.bukkit.Material.AIR) {
                return slot;
            }
        }

        return -1; // Nenhum slot vazio
    }

    /**
     * Chamado quando o jogador entra no spawn.
     * Dá itens de lobby.
     */
    private void onEnterSpawn(Player player) {
        if (plugin.getArenaItemsHandler() == null)
            return;

        // Dar itens de lobby
        plugin.getArenaItemsHandler().giveLobbyItems(player);
    }

    /**
     * Verifica se o jogador está na área do spawn.
     */
    private boolean isInSpawnArea(Player player) {
        // Usar PlayerStateManager se disponível
        if (plugin.getStateManager() != null) {
            return plugin.getStateManager().isInSpawn(player);
        }

        // Fallback: verificar distância do spawn warp
        if (plugin.getWarpsManager() != null) {
            com.haumea.kitpvp.models.Warp spawnWarp = plugin.getWarpsManager().getWarp("spawn");
            if (spawnWarp != null && spawnWarp.isValid()) {
                Location spawnLoc = spawnWarp.toLocation();
                if (spawnLoc.getWorld().equals(player.getWorld())) {
                    return player.getLocation().distance(spawnLoc) < 30;
                }
            }
        }

        return false;
    }

    /**
     * Limpa o cache quando o jogador sai.
     */
    @EventHandler
    public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        wasInSpawn.remove(event.getPlayer().getUniqueId());
    }

    /**
     * Inicializa o estado quando o jogador entra.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(org.bukkit.event.player.PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // Definir estado inicial após delay
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                wasInSpawn.put(player.getUniqueId(), isInSpawnArea(player));
            }
        }, 15L);
    }
}
