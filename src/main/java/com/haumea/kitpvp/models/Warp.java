package com.haumea.kitpvp.models;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

/**
 * Modelo que representa uma Warp do servidor.
 * Armazena informações de localização para teleporte.
 * 
 * @author HaumeaMC
 */
public class Warp {

    private final String name;
    private final String worldName;
    private final double x;
    private final double y;
    private final double z;
    private final float yaw;
    private final float pitch;

    /**
     * Raio da área de proteção do spawn (em blocos).
     * Se for 0 ou negativo, não há proteção de spawn.
     */
    private final double radius;

    /**
     * Cria uma nova Warp a partir de uma Location.
     * 
     * @param name     Nome da warp
     * @param location Localização da warp
     */
    public Warp(String name, Location location) {
        this(name, location, 0.0);
    }

    /**
     * Cria uma nova Warp a partir de uma Location com raio de proteção.
     * 
     * @param name     Nome da warp
     * @param location Localização da warp
     * @param radius   Raio da área de proteção (0 = sem proteção)
     */
    public Warp(String name, Location location, double radius) {
        this.name = name.toLowerCase();
        this.worldName = location.getWorld().getName();
        this.x = location.getX();
        this.y = location.getY();
        this.z = location.getZ();
        this.yaw = location.getYaw();
        this.pitch = location.getPitch();
        this.radius = Math.max(0, radius);
    }

    /**
     * Cria uma nova Warp a partir de valores individuais.
     * Usado ao carregar do arquivo YAML.
     * 
     * @param name      Nome da warp
     * @param worldName Nome do mundo
     * @param x         Coordenada X
     * @param y         Coordenada Y
     * @param z         Coordenada Z
     * @param yaw       Rotação horizontal
     * @param pitch     Rotação vertical
     */
    public Warp(String name, String worldName, double x, double y, double z, float yaw, float pitch) {
        this(name, worldName, x, y, z, yaw, pitch, 0.0);
    }

    /**
     * Cria uma nova Warp a partir de valores individuais com raio.
     * Usado ao carregar do MongoDB.
     * 
     * @param name      Nome da warp
     * @param worldName Nome do mundo
     * @param x         Coordenada X
     * @param y         Coordenada Y
     * @param z         Coordenada Z
     * @param yaw       Rotação horizontal
     * @param pitch     Rotação vertical
     * @param radius    Raio da área de proteção
     */
    public Warp(String name, String worldName, double x, double y, double z, float yaw, float pitch, double radius) {
        this.name = name.toLowerCase();
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.radius = Math.max(0, radius);
    }

    /**
     * Obtém o nome da warp.
     * 
     * @return Nome da warp (sempre em lowercase)
     */
    public String getName() {
        return name;
    }

    /**
     * Obtém o nome do mundo.
     * 
     * @return Nome do mundo
     */
    public String getWorldName() {
        return worldName;
    }

    /**
     * Obtém a coordenada X.
     * 
     * @return Coordenada X
     */
    public double getX() {
        return x;
    }

    /**
     * Obtém a coordenada Y.
     * 
     * @return Coordenada Y
     */
    public double getY() {
        return y;
    }

    /**
     * Obtém a coordenada Z.
     * 
     * @return Coordenada Z
     */
    public double getZ() {
        return z;
    }

    /**
     * Obtém a rotação horizontal (Yaw).
     * 
     * @return Yaw
     */
    public float getYaw() {
        return yaw;
    }

    /**
     * Obtém a rotação vertical (Pitch).
     * 
     * @return Pitch
     */
    public float getPitch() {
        return pitch;
    }

    /**
     * Converte a Warp para um objeto Location do Bukkit.
     * 
     * @return Location correspondente, ou null se o mundo não existir
     */
    public Location toLocation() {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }
        return new Location(world, x, y, z, yaw, pitch);
    }

    /**
     * Verifica se o mundo da warp existe.
     * 
     * @return true se o mundo existe
     */
    public boolean isValid() {
        return Bukkit.getWorld(worldName) != null;
    }

    /**
     * Obtém o raio de proteção da warp.
     * 
     * @return Raio em blocos (0 = sem proteção)
     */
    public double getRadius() {
        return radius;
    }

    /**
     * Verifica se a warp possui raio de proteção configurado.
     * 
     * @return true se possui proteção de spawn
     */
    public boolean hasProtectionRadius() {
        return radius > 0;
    }

    /**
     * Verifica se uma localização está dentro do raio de proteção da warp.
     * Usa apenas coordenadas X e Z (ignora altura Y).
     * 
     * @param location Localização a verificar
     * @return true se está dentro do raio de proteção
     */
    public boolean isWithinRadius(Location location) {
        if (location == null || !hasProtectionRadius()) {
            return false;
        }

        // Verificar mesmo mundo
        if (!location.getWorld().getName().equals(worldName)) {
            return false;
        }

        // Calcular distância 2D (ignorar Y)
        double dx = location.getX() - x;
        double dz = location.getZ() - z;
        double distanceSquared = dx * dx + dz * dz;

        return distanceSquared <= (radius * radius);
    }

    @Override
    public String toString() {
        if (radius > 0) {
            return String.format("Warp{name='%s', world='%s', x=%.2f, y=%.2f, z=%.2f, radius=%.1f}",
                    name, worldName, x, y, z, radius);
        }
        return String.format("Warp{name='%s', world='%s', x=%.2f, y=%.2f, z=%.2f}",
                name, worldName, x, y, z);
    }
}
