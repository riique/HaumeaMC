package com.haumea.kitpvp.abilities.kits;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.abilities.Ability;
import com.haumea.kitpvp.abilities.AbilityRarity;
import com.haumea.kitpvp.abilities.Ejectable;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

/**
 * Kit Gladiator (Gladiador)
 * 
 * Raridade: MYSTIC | Preço: 2.500 | Ícone: IRON_FENCE | Cooldown: 5s
 * 
 * Funcionalidade COMPLEXA:
 * - Ao clicar com botão direito em outro jogador, ambos são teleportados para
 * uma arena de vidro no céu
 * - A arena é gerada dinamicamente 100 blocos acima do local atual
 * - Dimensões da arena: 15x15x8 blocos de vidro
 * - Jogadores não podem usar comandos /spawn ou /warp dentro da arena
 * - Jogadores não podem colocar blocos dentro da arena
 * - Se a luta demorar mais de 2 minutos, ambos recebem efeito de Wither
 * - Se a luta demorar mais de 3 minutos, a arena é destruída e ambos são
 * teleportados de volta
 * - A arena é destruída quando um jogador morre, sai do servidor ou sai dos
 * limites
 * 
 * Regras:
 * - Jogadores com kit Neo NÃO podem ser puxados para a arena
 * - Verifica se o desafiado já está em outra luta
 * 
 * @author HaumeaMC
 */
public class GladiatorAbility extends Ability implements Ejectable {

    // Arenas ativas: UUID do gladiator -> GladiatorArena
    private final Map<UUID, GladiatorArena> arenas = new HashMap<>();

    // Reverse lookup: UUID de qualquer jogador na arena -> UUID do gladiator
    private final Map<UUID, UUID> playerToArena = new HashMap<>();

    public GladiatorAbility(HaumeaMC plugin) {
        super(plugin, AbilityRarity.MYSTIC, Material.IRON_FENCE, 5, 2500,
                new ItemStack[] { createGladiatorItem() });
    }

    private static ItemStack createGladiatorItem() {
        ItemStack item = new ItemStack(Material.IRON_FENCE);
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§a§lGladiator");
        java.util.List<String> lore = new java.util.ArrayList<>();
        lore.add("§7Clique direito em um jogador");
        lore.add("§7para desafia-lo na arena!");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();

        if (!hasAbility(player))
            return;
        if (!isMainItem(player.getItemInHand()))
            return;

        if (!(event.getRightClicked() instanceof Player))
            return;

        Player target = (Player) event.getRightClicked();

        event.setCancelled(true);

        // Verificações
        if (isProtected(player) || isProtected(target)) {
            player.sendMessage("§c§lHAUMEAMC§f Você ou o alvo está em área protegida!");
            return;
        }

        if (isInCooldown(player)) {
            sendCooldownMessage(player);
            return;
        }

        // Verificar se o alvo tem Neo
        if (Ability.hasAbility(target, "Neo", plugin)) {
            player.sendMessage("§c§lHAUMEAMC§f O jogador está utilizando o kit Neo!");
            return;
        }

        // Verificar se já estão em arena
        if (isInArena(player)) {
            player.sendMessage("§c§lHAUMEAMC§f Você já está em uma arena!");
            return;
        }

        if (isInArena(target)) {
            player.sendMessage("§c§lHAUMEAMC§f O jogador já está em uma arena!");
            return;
        }

        putInCooldown(player);
        createArena(player, target);
    }

    /**
     * Cria a arena do gladiador
     */
    private void createArena(Player gladiator, Player target) {
        Location baseLoc = gladiator.getLocation().clone();
        baseLoc.setY(baseLoc.getY() + 100);

        // Garantir que a arena não ultrapasse o limite do mundo
        if (baseLoc.getY() > 200) {
            baseLoc.setY(200);
        }

        GladiatorArena arena = new GladiatorArena(gladiator, target, baseLoc);
        arenas.put(gladiator.getUniqueId(), arena);
        playerToArena.put(gladiator.getUniqueId(), gladiator.getUniqueId());
        playerToArena.put(target.getUniqueId(), gladiator.getUniqueId());

        // Construir arena
        buildArena(baseLoc);

        // Teleportar jogadores
        Location gladiatorSpawn = baseLoc.clone().add(2, 1, 7);
        gladiatorSpawn.setYaw(0);
        Location targetSpawn = baseLoc.clone().add(12, 1, 7);
        targetSpawn.setYaw(180);

        gladiator.teleport(gladiatorSpawn);
        target.teleport(targetSpawn);

        gladiator.setFallDistance(0);
        target.setFallDistance(0);

        // Mensagens
        gladiator.sendMessage("§6§lHAUMEAMC§f Arena criada! Lute até a morte!");
        target.sendMessage("§6§lHAUMEAMC§f Você foi desafiado por §e" + gladiator.getName() + "§f! Lute!");

        // Som
        gladiator.playSound(gladiator.getLocation(), Sound.WITHER_SPAWN, 0.5f, 1.5f);
        target.playSound(target.getLocation(), Sound.WITHER_SPAWN, 0.5f, 1.5f);

        // Iniciar timer de Wither (2 minutos)
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!arenas.containsKey(gladiator.getUniqueId())) {
                    cancel();
                    return;
                }

                GladiatorArena currentArena = arenas.get(gladiator.getUniqueId());
                if (currentArena == null) {
                    cancel();
                    return;
                }

                // Aplicar Wither
                if (gladiator.isOnline()) {
                    gladiator.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 600, 0));
                    gladiator.sendMessage("§c§lHAUMEAMC§f A luta está demorando! Wither aplicado!");
                }
                if (target.isOnline()) {
                    target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 600, 0));
                    target.sendMessage("§c§lHAUMEAMC§f A luta está demorando! Wither aplicado!");
                }
            }
        }.runTaskLater(plugin, 20L * 120L); // 2 minutos

        // Iniciar timer de destruição (3 minutos)
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!arenas.containsKey(gladiator.getUniqueId())) {
                    cancel();
                    return;
                }

                // Forçar empate
                destroyArena(gladiator.getUniqueId(), true);

                if (gladiator.isOnline()) {
                    gladiator.sendMessage("§c§lHAUMEAMC§f Tempo esgotado! Arena destruída!");
                }
                if (target.isOnline()) {
                    target.sendMessage("§c§lHAUMEAMC§f Tempo esgotado! Arena destruída!");
                }
            }
        }.runTaskLater(plugin, 20L * 180L); // 3 minutos
    }

    /**
     * Constrói a estrutura da arena
     */
    private void buildArena(Location baseLoc) {
        int width = 15;
        int height = 8;

        // Construir paredes, chão e teto
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                for (int z = 0; z < width; z++) {
                    boolean isWall = x == 0 || x == width - 1 ||
                            y == 0 || y == height - 1 ||
                            z == 0 || z == width - 1;

                    if (isWall) {
                        Block block = baseLoc.clone().add(x, y, z).getBlock();
                        block.setType(Material.GLASS);
                    }
                }
            }
        }
    }

    /**
     * Remove a estrutura da arena
     */
    private void removeArenaBlocks(Location baseLoc) {
        int width = 15;
        int height = 8;

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                for (int z = 0; z < width; z++) {
                    Block block = baseLoc.clone().add(x, y, z).getBlock();
                    if (block.getType() == Material.GLASS) {
                        block.setType(Material.AIR);
                    }
                }
            }
        }
    }

    /**
     * Destrói uma arena e teleporta os jogadores de volta
     */
    public void destroyArena(UUID gladiatorUUID, boolean teleportBack) {
        GladiatorArena arena = arenas.remove(gladiatorUUID);
        if (arena == null)
            return;

        playerToArena.remove(arena.gladiatorUUID);
        playerToArena.remove(arena.targetUUID);

        // Remover blocos
        removeArenaBlocks(arena.baseLocation);

        // Teleportar de volta se necessário
        if (teleportBack) {
            Player gladiator = Bukkit.getPlayer(arena.gladiatorUUID);
            Player target = Bukkit.getPlayer(arena.targetUUID);

            if (gladiator != null && gladiator.isOnline()) {
                gladiator.teleport(arena.originalGladiatorLoc);
                gladiator.setFallDistance(0);
                gladiator.removePotionEffect(PotionEffectType.WITHER);
            }

            if (target != null && target.isOnline()) {
                target.teleport(arena.originalTargetLoc);
                target.setFallDistance(0);
                target.removePotionEffect(PotionEffectType.WITHER);
            }
        }
    }

    /**
     * Verifica se um jogador está em uma arena
     */
    public boolean isInArena(Player player) {
        return playerToArena.containsKey(player.getUniqueId());
    }

    /**
     * Obtém o UUID do gladiator da arena onde o jogador está
     */
    public UUID getArenaOwner(Player player) {
        return playerToArena.get(player.getUniqueId());
    }

    // ==================== EVENTOS ====================

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        UUID arenaOwner = playerToArena.get(player.getUniqueId());

        if (arenaOwner != null) {
            destroyArena(arenaOwner, true);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        eject(event.getPlayer());
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (isInArena(event.getPlayer())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§c§lHAUMEAMC§f Você não pode colocar blocos na arena!");
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (isInArena(event.getPlayer())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§c§lHAUMEAMC§f Você não pode quebrar blocos na arena!");
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID arenaOwner = playerToArena.get(player.getUniqueId());

        if (arenaOwner == null)
            return;

        GladiatorArena arena = arenas.get(arenaOwner);
        if (arena == null)
            return;

        // Verificar se saiu dos limites da arena
        Location loc = player.getLocation();
        Location base = arena.baseLocation;

        if (loc.getX() < base.getX() - 1 || loc.getX() > base.getX() + 16 ||
                loc.getY() < base.getY() - 1 || loc.getY() > base.getY() + 9 ||
                loc.getZ() < base.getZ() - 1 || loc.getZ() > base.getZ() + 16) {

            // Saiu dos limites, destruir arena
            destroyArena(arenaOwner, true);
            player.sendMessage("§c§lHAUMEAMC§f Você saiu dos limites da arena!");
        }
    }

    @Override
    public void eject(Player player) {
        UUID arenaOwner = playerToArena.get(player.getUniqueId());
        if (arenaOwner != null) {
            destroyArena(arenaOwner, true);
        }
    }

    // ==================== CLASSE INTERNA ====================

    private static class GladiatorArena {
        final UUID gladiatorUUID;
        final UUID targetUUID;
        final Location baseLocation;
        final Location originalGladiatorLoc;
        final Location originalTargetLoc;
        final long createdAt;

        GladiatorArena(Player gladiator, Player target, Location baseLoc) {
            this.gladiatorUUID = gladiator.getUniqueId();
            this.targetUUID = target.getUniqueId();
            this.baseLocation = baseLoc;
            this.originalGladiatorLoc = gladiator.getLocation().clone();
            this.originalTargetLoc = target.getLocation().clone();
            this.createdAt = System.currentTimeMillis();
        }
    }
}
