package com.haumea.kitpvp.models;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

/**
 * Representa uma arena de duelo 1v1.
 * 
 * Cada arena possui dois pontos de spawn para os duelistas
 * e informações sobre se está em uso ou não.
 * 
 * @author HaumeaMC
 */
public class DuelArena {

    private final String name;
    private String worldName;

    // Spawn do jogador 1
    private double spawn1X, spawn1Y, spawn1Z;
    private float spawn1Yaw, spawn1Pitch;

    // Spawn do jogador 2
    private double spawn2X, spawn2Y, spawn2Z;
    private float spawn2Yaw, spawn2Pitch;

    // Status
    private boolean inUse;
    private boolean enabled;

    /**
     * Construtor da arena
     * 
     * @param name Nome da arena
     */
    public DuelArena(String name) {
        this.name = name;
        this.inUse = false;
        this.enabled = true;
    }

    /**
     * Construtor completo
     */
    public DuelArena(String name, String worldName,
            double spawn1X, double spawn1Y, double spawn1Z, float spawn1Yaw, float spawn1Pitch,
            double spawn2X, double spawn2Y, double spawn2Z, float spawn2Yaw, float spawn2Pitch) {
        this.name = name;
        this.worldName = worldName;
        this.spawn1X = spawn1X;
        this.spawn1Y = spawn1Y;
        this.spawn1Z = spawn1Z;
        this.spawn1Yaw = spawn1Yaw;
        this.spawn1Pitch = spawn1Pitch;
        this.spawn2X = spawn2X;
        this.spawn2Y = spawn2Y;
        this.spawn2Z = spawn2Z;
        this.spawn2Yaw = spawn2Yaw;
        this.spawn2Pitch = spawn2Pitch;
        this.inUse = false;
        this.enabled = true;
    }

    // ==================== GETTERS E SETTERS ====================

    public String getName() {
        return name;
    }

    public String getWorldName() {
        return worldName;
    }

    public void setWorldName(String worldName) {
        this.worldName = worldName;
    }

    public World getWorld() {
        return worldName != null ? Bukkit.getWorld(worldName) : null;
    }

    public boolean isInUse() {
        return inUse;
    }

    public void setInUse(boolean inUse) {
        this.inUse = inUse;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    // ==================== SPAWN 1 ====================

    public void setSpawn1(Location location) {
        if (location == null)
            return;
        this.worldName = location.getWorld().getName();
        this.spawn1X = location.getX();
        this.spawn1Y = location.getY();
        this.spawn1Z = location.getZ();
        this.spawn1Yaw = location.getYaw();
        this.spawn1Pitch = location.getPitch();
    }

    public Location getSpawn1() {
        World world = getWorld();
        if (world == null)
            return null;
        return new Location(world, spawn1X, spawn1Y, spawn1Z, spawn1Yaw, spawn1Pitch);
    }

    public double getSpawn1X() {
        return spawn1X;
    }

    public double getSpawn1Y() {
        return spawn1Y;
    }

    public double getSpawn1Z() {
        return spawn1Z;
    }

    public float getSpawn1Yaw() {
        return spawn1Yaw;
    }

    public float getSpawn1Pitch() {
        return spawn1Pitch;
    }

    // ==================== SPAWN 2 ====================

    public void setSpawn2(Location location) {
        if (location == null)
            return;
        this.worldName = location.getWorld().getName();
        this.spawn2X = location.getX();
        this.spawn2Y = location.getY();
        this.spawn2Z = location.getZ();
        this.spawn2Yaw = location.getYaw();
        this.spawn2Pitch = location.getPitch();
    }

    public Location getSpawn2() {
        World world = getWorld();
        if (world == null)
            return null;
        return new Location(world, spawn2X, spawn2Y, spawn2Z, spawn2Yaw, spawn2Pitch);
    }

    public double getSpawn2X() {
        return spawn2X;
    }

    public double getSpawn2Y() {
        return spawn2Y;
    }

    public double getSpawn2Z() {
        return spawn2Z;
    }

    public float getSpawn2Yaw() {
        return spawn2Yaw;
    }

    public float getSpawn2Pitch() {
        return spawn2Pitch;
    }

    // ==================== VALIDAÇÃO ====================

    /**
     * Verifica se a arena está pronta para uso
     * 
     * @return true se ambos os spawns estão configurados
     */
    public boolean isReady() {
        return worldName != null && getWorld() != null;
    }

    /**
     * Verifica se a arena está disponível para um novo duelo
     * 
     * @return true se está pronta, habilitada e não em uso
     */
    public boolean isAvailable() {
        return isReady() && enabled && !inUse;
    }

    @Override
    public String toString() {
        return "DuelArena{" +
                "name='" + name + '\'' +
                ", world='" + worldName + '\'' +
                ", inUse=" + inUse +
                ", enabled=" + enabled +
                '}';
    }
}
