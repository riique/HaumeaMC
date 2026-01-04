package com.haumea.kitpvp.managers;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.models.Warp;
import com.haumea.kitpvp.utils.ChatStorage;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gerenciador de Warps com Proteção de Spawn (FPS Mode).
 * 
 * Funcionalidades:
 * - Rastreia jogadores em cada warp
 * - Gerencia proteção de spawn por warp
 * - Dá kit FPS ao sair da área de proteção
 * - Impede dano entre jogadores de warps diferentes
 * 
 * @author HaumeaMC
 */
public class FPSWarpManager {

    private final HaumeaMC plugin;

    // Jogadores em cada warp: warpName -> Set<UUID>
    private final Map<String, Set<UUID>> playersInWarp;

    // Jogadores protegidos em cada warp: warpName -> Set<UUID>
    private final Map<String, Set<UUID>> protectedPlayers;

    // Warp atual de cada jogador: UUID -> warpName
    private final Map<UUID, String> playerWarp;

    public FPSWarpManager(HaumeaMC plugin) {
        this.plugin = plugin;
        this.playersInWarp = new ConcurrentHashMap<>();
        this.protectedPlayers = new ConcurrentHashMap<>();
        this.playerWarp = new ConcurrentHashMap<>();

        plugin.getLogger().info("[FPSWarp] Manager inicializado.");
    }

    // ==================== ENTRADA E SAÍDA DE WARP ====================

    /**
     * Processa a entrada de um jogador em uma warp.
     * 
     * @param player Jogador
     * @param warp   Warp para entrar
     * @param from   Warp de origem (pode ser null)
     */
    public void joinWarp(Player player, Warp warp, Warp from) {
        UUID uuid = player.getUniqueId();
        String warpName = warp.getName().toLowerCase();

        // Remover da warp anterior se existir
        if (from != null) {
            leaveWarp(player, from.getName());
        } else {
            // Verificar se estava em outra warp
            String previousWarp = playerWarp.get(uuid);
            if (previousWarp != null && !previousWarp.equals(warpName)) {
                leaveWarp(player, previousWarp);
            }
        }

        // Limpar efeitos de poção
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }

        // Resetar vida e fome
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setSaturation(20);

        // Limpar inventário
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);

        // Teleportar para o spawn da warp
        Location spawnLoc = warp.toLocation();
        if (spawnLoc != null) {
            player.teleport(spawnLoc);
        }

        // Adicionar à lista de jogadores na warp
        playersInWarp.computeIfAbsent(warpName, k -> ConcurrentHashMap.newKeySet()).add(uuid);
        playerWarp.put(uuid, warpName);

        // Se a warp tem proteção de spawn, adicionar à lista de protegidos
        if (warp.hasProtectionRadius()) {
            protectedPlayers.computeIfAbsent(warpName, k -> ConcurrentHashMap.newKeySet()).add(uuid);

            // Dar item de voltar ao spawn no slot 8
            giveSpawnItem(player);
        }

        // Chamar callback de entrada
        onPlayerJoinWarp(player, warp);
    }

    /**
     * Processa a saída de um jogador de uma warp.
     * 
     * @param player   Jogador
     * @param warpName Nome da warp
     */
    public void leaveWarp(Player player, String warpName) {
        if (warpName == null)
            return;

        UUID uuid = player.getUniqueId();
        String key = warpName.toLowerCase();

        // Remover da lista de jogadores
        Set<UUID> players = playersInWarp.get(key);
        if (players != null) {
            players.remove(uuid);
        }

        // Remover da lista de protegidos
        Set<UUID> protected_ = protectedPlayers.get(key);
        if (protected_ != null) {
            protected_.remove(uuid);
        }

        // Limpar warp atual se for a mesma
        if (key.equals(playerWarp.get(uuid))) {
            playerWarp.remove(uuid);
        }
    }

    // ==================== PROTEÇÃO DE SPAWN ====================

    /**
     * Verifica se um jogador está protegido em sua warp atual.
     * 
     * @param player Jogador
     * @return true se está protegido
     */
    public boolean isProtected(Player player) {
        if (player == null)
            return false;

        UUID uuid = player.getUniqueId();
        String warpName = playerWarp.get(uuid);
        if (warpName == null)
            return false;

        Set<UUID> protected_ = protectedPlayers.get(warpName);
        return protected_ != null && protected_.contains(uuid);
    }

    /**
     * Verifica se um jogador está protegido em qualquer warp.
     * 
     * @param uuid UUID do jogador
     * @return true se está protegido
     */
    public boolean isProtected(UUID uuid) {
        if (uuid == null)
            return false;

        String warpName = playerWarp.get(uuid);
        if (warpName == null)
            return false;

        Set<UUID> protected_ = protectedPlayers.get(warpName);
        return protected_ != null && protected_.contains(uuid);
    }

    /**
     * Remove a proteção de um jogador e dá o kit de combate.
     * 
     * @param player Jogador
     */
    public void unprotect(Player player) {
        if (player == null)
            return;

        UUID uuid = player.getUniqueId();
        String warpName = playerWarp.get(uuid);
        if (warpName == null)
            return;

        Set<UUID> protected_ = protectedPlayers.get(warpName);
        if (protected_ != null && protected_.remove(uuid)) {
            // Obter a warp
            Warp warp = plugin.getWarpsManager().getWarp(warpName);
            if (warp != null) {
                // Chamar callback de saída da proteção
                onSpawnLeave(player, warp);
            }

            // Enviar mensagem
            ChatStorage.send(player, "warp.lost-protection");
        }
    }

    /**
     * Verifica se um jogador está dentro do raio de proteção de sua warp.
     * 
     * @param player Jogador
     * @return true se está dentro do raio
     */
    public boolean isWithinProtectionRadius(Player player) {
        if (player == null)
            return false;

        UUID uuid = player.getUniqueId();
        String warpName = playerWarp.get(uuid);
        if (warpName == null)
            return false;

        Warp warp = plugin.getWarpsManager().getWarp(warpName);
        if (warp == null || !warp.hasProtectionRadius()) {
            return false;
        }

        return warp.isWithinRadius(player.getLocation());
    }

    // ==================== DETECÇÃO DE WARP ====================

    /**
     * Obtém a warp atual de um jogador.
     * 
     * @param player Jogador
     * @return Nome da warp ou null
     */
    public String getPlayerWarp(Player player) {
        if (player == null)
            return null;
        return playerWarp.get(player.getUniqueId());
    }

    /**
     * Obtém a warp atual por UUID.
     * 
     * @param uuid UUID do jogador
     * @return Nome da warp ou null
     */
    public String getPlayerWarp(UUID uuid) {
        return playerWarp.get(uuid);
    }

    /**
     * Verifica se dois jogadores estão na mesma warp.
     * 
     * @param player1 Primeiro jogador
     * @param player2 Segundo jogador
     * @return true se estão na mesma warp
     */
    public boolean areInSameWarp(Player player1, Player player2) {
        if (player1 == null || player2 == null)
            return false;

        String warp1 = playerWarp.get(player1.getUniqueId());
        String warp2 = playerWarp.get(player2.getUniqueId());

        // Se ambos não estão em warp, consideram como mesma "warp" (geral)
        if (warp1 == null && warp2 == null)
            return true;

        // Se apenas um está em warp, são diferentes
        if (warp1 == null || warp2 == null)
            return false;

        return warp1.equals(warp2);
    }

    /**
     * Obtém o número de jogadores em uma warp (ativos, fora da proteção).
     * 
     * @param warpName Nome da warp
     * @return Número de jogadores ativos
     */
    public int getActivePlayerCount(String warpName) {
        if (warpName == null)
            return 0;

        String key = warpName.toLowerCase();
        Set<UUID> players = playersInWarp.get(key);
        Set<UUID> protected_ = protectedPlayers.get(key);

        if (players == null)
            return 0;
        if (protected_ == null)
            return players.size();

        // Contar jogadores que não estão protegidos
        int active = 0;
        for (UUID uuid : players) {
            if (!protected_.contains(uuid)) {
                active++;
            }
        }
        return active;
    }

    /**
     * Obtém o número total de jogadores em uma warp.
     * 
     * @param warpName Nome da warp
     * @return Número total de jogadores
     */
    public int getTotalPlayerCount(String warpName) {
        if (warpName == null)
            return 0;

        String key = warpName.toLowerCase();
        Set<UUID> players = playersInWarp.get(key);
        return players != null ? players.size() : 0;
    }

    // ==================== CALLBACKS (PODEM SER SOBRESCRITOS) ====================

    /**
     * Chamado quando um jogador entra em uma warp.
     * Dá item de voltar ao spawn.
     * 
     * @param player Jogador
     * @param warp   Warp
     */
    protected void onPlayerJoinWarp(Player player, Warp warp) {
        // Comportamento padrão: dar item de voltar ao spawn
        if (warp.hasProtectionRadius()) {
            player.getInventory().setHeldItemSlot(8);
        }
    }

    /**
     * Chamado quando um jogador sai da área de proteção.
     * Dá kit FPS (espada, sopas, refill).
     * 
     * @param player Jogador
     * @param warp   Warp
     */
    protected void onSpawnLeave(Player player, Warp warp) {
        // Limpar inventário
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);

        // Dar kit FPS
        giveFPSKit(player);
    }

    /**
     * Dá o kit FPS completo para o jogador.
     * 
     * @param player Jogador
     */
    private void giveFPSKit(Player player) {
        PlayerInventory inv = player.getInventory();

        // Slot 0: Espada de Madeira com Sharpness 1, Unbreakable
        ItemStack sword = new ItemStack(Material.WOOD_SWORD);
        ItemMeta swordMeta = sword.getItemMeta();
        swordMeta.setDisplayName("§fEspada FPS");
        swordMeta.addEnchant(Enchantment.DAMAGE_ALL, 1, true);
        swordMeta.spigot().setUnbreakable(true);
        swordMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES);
        sword.setItemMeta(swordMeta);
        inv.setItem(0, sword);

        // Slots 13-15: Refill (Bowl, Red Mushroom, Brown Mushroom)
        inv.setItem(13, new ItemStack(Material.BOWL, 32));
        inv.setItem(14, new ItemStack(Material.RED_MUSHROOM, 32));
        inv.setItem(15, new ItemStack(Material.BROWN_MUSHROOM, 32));

        // Preencher slots restantes com Sopas de Cogumelo
        ItemStack soup = new ItemStack(Material.MUSHROOM_SOUP);

        for (int slot = 1; slot < 36; slot++) {
            // Pular slots reservados
            if (slot == 0 || slot == 8 || slot == 13 || slot == 14 || slot == 15) {
                continue;
            }
            if (inv.getItem(slot) == null) {
                inv.setItem(slot, soup.clone());
            }
        }

        player.updateInventory();
    }

    /**
     * Dá o item de voltar ao spawn.
     * 
     * @param player Jogador
     */
    private void giveSpawnItem(Player player) {
        ItemStack bed = new ItemStack(Material.BED);
        ItemMeta meta = bed.getItemMeta();
        meta.setDisplayName("§c§lVoltar ao Spawn");
        meta.setLore(Arrays.asList(
                "§7Clique para voltar",
                "§7ao spawn do servidor.",
                "",
                "§eClique direito para usar!"));
        bed.setItemMeta(meta);

        player.getInventory().setItem(8, bed);
        player.updateInventory();
    }

    // ==================== CLEANUP ====================

    /**
     * Limpa dados de um jogador ao sair do servidor.
     * 
     * @param player Jogador
     */
    public void onPlayerQuit(Player player) {
        if (player == null)
            return;

        UUID uuid = player.getUniqueId();
        String warpName = playerWarp.remove(uuid);

        if (warpName != null) {
            Set<UUID> players = playersInWarp.get(warpName);
            if (players != null) {
                players.remove(uuid);
            }

            Set<UUID> protected_ = protectedPlayers.get(warpName);
            if (protected_ != null) {
                protected_.remove(uuid);
            }
        }
    }

    /**
     * Desliga o manager e limpa todos os dados.
     */
    public void shutdown() {
        playersInWarp.clear();
        protectedPlayers.clear();
        playerWarp.clear();
        plugin.getLogger().info("[FPSWarp] Manager desligado.");
    }
}
