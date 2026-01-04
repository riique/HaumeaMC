package com.haumea.kitpvp.listeners;

import com.haumea.kitpvp.HaumeaMC;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Sistema de Lançadores (Jump Pads & Dashes) para KitPvP.
 * 
 * Tipos de lançadores:
 * - Esponja (SPONGE): Lançador vertical com vetor Y=3.5
 * - Bloco de Esmeralda (EMERALD_BLOCK): Dash direcional na direção que o
 * jogador olha
 * 
 * @author HaumeaMC
 */
public class LauncherListener implements Listener {

    private final HaumeaMC plugin;

    private static final double SPONGE_VERTICAL_VELOCITY = 3.5D;
    private static final double DASH_MULTIPLIER = 3.0D;
    private static final double DASH_VERTICAL_VELOCITY = 0.55D;
    private static final float SOUND_VOLUME = 10.0F;
    private static final float SOUND_PITCH = 1.0F;
    private static final long LAUNCH_COOLDOWN = 500L;

    private final Set<UUID> launchedPlayers = new HashSet<>();
    private final Map<UUID, Long> launchCooldowns = new HashMap<>();

    public LauncherListener(HaumeaMC plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location to = event.getTo();

        if (to == null)
            return;
        if (player.isFlying())
            return;

        UUID playerId = player.getUniqueId();
        Long lastLaunch = launchCooldowns.get(playerId);
        if (lastLaunch != null && System.currentTimeMillis() - lastLaunch < LAUNCH_COOLDOWN) {
            return;
        }

        Block blockBelow = to.clone().subtract(0, 1, 0).getBlock();
        Material type = blockBelow.getType();

        if (type == Material.SPONGE) {
            launchVertical(player);
        } else if (type == Material.EMERALD_BLOCK) {
            launchDirectional(player);
        }
    }

    private void launchVertical(Player player) {
        player.setVelocity(new Vector(0.0D, SPONGE_VERTICAL_VELOCITY, 0.0D));
        player.playSound(player.getLocation(), Sound.HORSE_JUMP, SOUND_VOLUME, SOUND_PITCH);

        UUID playerId = player.getUniqueId();
        launchedPlayers.add(playerId);
        launchCooldowns.put(playerId, System.currentTimeMillis());
    }

    private void launchDirectional(Player player) {
        Vector direction = player.getEyeLocation().getDirection().normalize();
        Vector velocity = new Vector(
                direction.getX() * DASH_MULTIPLIER,
                DASH_VERTICAL_VELOCITY,
                direction.getZ() * DASH_MULTIPLIER);

        player.setVelocity(velocity);
        player.playSound(player.getLocation(), Sound.HORSE_JUMP, SOUND_VOLUME, SOUND_PITCH);

        UUID playerId = player.getUniqueId();
        launchedPlayers.add(playerId);
        launchCooldowns.put(playerId, System.currentTimeMillis());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player))
            return;

        Player player = (Player) event.getEntity();
        UUID playerId = player.getUniqueId();

        if (!launchedPlayers.contains(playerId))
            return;

        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            event.setCancelled(true);
        }
        launchedPlayers.remove(playerId);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        launchedPlayers.remove(playerId);
        launchCooldowns.remove(playerId);
    }

    public void cleanup(Player player) {
        UUID playerId = player.getUniqueId();
        launchedPlayers.remove(playerId);
        launchCooldowns.remove(playerId);
    }

    public boolean isLaunched(Player player) {
        return launchedPlayers.contains(player.getUniqueId());
    }

    public void removeLaunchProtection(Player player) {
        launchedPlayers.remove(player.getUniqueId());
    }
}
