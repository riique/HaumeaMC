package com.haumea.kitpvp.models.cosmetic;

import org.bukkit.*;
import org.bukkit.entity.Player;

/**
 * Cosmético de efeito visual ao matar.
 * Spawn partículas na localização da vítima.
 * 
 * @author HaumeaMC
 */
public class KillEffect extends Cosmetic {

    private final Effect effect;
    private final int effectData;
    private final int particleCount;
    private final float radius;
    private final boolean useParticleApi;
    private final String particleName;

    /**
     * Construtor para efeitos usando a API de Effect do Bukkit
     */
    public KillEffect(String id, String displayName, String[] description,
            CosmeticRarity rarity, Material icon, int price,
            Effect effect, int effectData, int particleCount, float radius) {
        super(id, displayName, description, CosmeticType.KILL_EFFECT, rarity, icon, price);
        this.effect = effect;
        this.effectData = effectData;
        this.particleCount = particleCount;
        this.radius = radius;
        this.useParticleApi = false;
        this.particleName = null;
    }

    /**
     * Construtor para efeitos customizados (usando nome de partícula)
     */
    public KillEffect(String id, String displayName, String[] description,
            CosmeticRarity rarity, Material icon, int price,
            String particleName, int particleCount, float radius) {
        super(id, displayName, description, CosmeticType.KILL_EFFECT, rarity, icon, price);
        this.effect = null;
        this.effectData = 0;
        this.particleCount = particleCount;
        this.radius = radius;
        this.useParticleApi = true;
        this.particleName = particleName;
    }

    @Override
    public void apply(Player killer, Player victim) {
        if (victim == null)
            return;

        Location loc = victim.getLocation().add(0, 1, 0);
        World world = loc.getWorld();

        if (useParticleApi && particleName != null) {
            // Usar sistema de partículas baseado em nome
            spawnNamedParticles(world, loc);
        } else if (effect != null) {
            // Usar Effect API do Bukkit
            spawnEffectParticles(world, loc);
        }
    }

    /**
     * Spawn partículas usando Effect API
     */
    private void spawnEffectParticles(World world, Location center) {
        for (int i = 0; i < particleCount; i++) {
            double offsetX = (Math.random() - 0.5) * radius * 2;
            double offsetY = Math.random() * radius;
            double offsetZ = (Math.random() - 0.5) * radius * 2;

            Location particleLoc = center.clone().add(offsetX, offsetY, offsetZ);
            world.playEffect(particleLoc, effect, effectData);
        }
    }

    /**
     * Spawn partículas baseadas em nome (para efeitos customizados)
     * Compatível com 1.8.8
     */
    private void spawnNamedParticles(World world, Location center) {
        // Em 1.8.8, usamos playEffect com Effect enum
        // Para partículas customizadas, mapeamos o nome para Effect
        switch (particleName.toUpperCase()) {
            case "FLAME":
                for (int i = 0; i < particleCount; i++) {
                    double angle = Math.PI * 2 * i / particleCount;
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    world.playEffect(center.clone().add(x, 0, z), Effect.MOBSPAWNER_FLAMES, 0);
                }
                break;
            case "HEART":
                for (int i = 0; i < particleCount; i++) {
                    double offsetX = (Math.random() - 0.5) * radius * 2;
                    double offsetY = Math.random() * radius;
                    double offsetZ = (Math.random() - 0.5) * radius * 2;
                    world.playEffect(center.clone().add(offsetX, offsetY, offsetZ), Effect.HEART, 0);
                }
                break;
            case "EXPLOSION":
                world.playEffect(center, Effect.EXPLOSION_LARGE, 0);
                break;
            case "SMOKE":
                for (int i = 0; i < particleCount; i++) {
                    double offsetX = (Math.random() - 0.5) * radius * 2;
                    double offsetY = Math.random() * radius;
                    double offsetZ = (Math.random() - 0.5) * radius * 2;
                    world.playEffect(center.clone().add(offsetX, offsetY, offsetZ), Effect.SMOKE, 4);
                }
                break;
            case "PORTAL":
                for (int i = 0; i < particleCount; i++) {
                    double offsetX = (Math.random() - 0.5) * radius * 2;
                    double offsetY = Math.random() * radius;
                    double offsetZ = (Math.random() - 0.5) * radius * 2;
                    world.playEffect(center.clone().add(offsetX, offsetY, offsetZ), Effect.PORTAL, 0);
                }
                break;
            case "ENCHANT":
                for (int i = 0; i < particleCount; i++) {
                    double offsetX = (Math.random() - 0.5) * radius * 2;
                    double offsetY = Math.random() * radius;
                    double offsetZ = (Math.random() - 0.5) * radius * 2;
                    world.playEffect(center.clone().add(offsetX, offsetY, offsetZ), Effect.FLYING_GLYPH, 0);
                }
                break;
            case "VILLAGER_HAPPY":
                for (int i = 0; i < particleCount; i++) {
                    double offsetX = (Math.random() - 0.5) * radius * 2;
                    double offsetY = Math.random() * radius;
                    double offsetZ = (Math.random() - 0.5) * radius * 2;
                    world.playEffect(center.clone().add(offsetX, offsetY, offsetZ), Effect.HAPPY_VILLAGER, 0);
                }
                break;
            case "LAVA":
                for (int i = 0; i < particleCount; i++) {
                    double offsetX = (Math.random() - 0.5) * radius * 2;
                    double offsetY = Math.random() * radius;
                    double offsetZ = (Math.random() - 0.5) * radius * 2;
                    world.playEffect(center.clone().add(offsetX, offsetY, offsetZ), Effect.LAVA_POP, 0);
                }
                break;
            case "WATER":
                for (int i = 0; i < particleCount; i++) {
                    double offsetX = (Math.random() - 0.5) * radius * 2;
                    double offsetY = Math.random() * radius * 0.5;
                    double offsetZ = (Math.random() - 0.5) * radius * 2;
                    world.playEffect(center.clone().add(offsetX, offsetY, offsetZ), Effect.WATERDRIP, 0);
                }
                break;
            case "WITCH":
                for (int i = 0; i < particleCount; i++) {
                    double offsetX = (Math.random() - 0.5) * radius * 2;
                    double offsetY = Math.random() * radius;
                    double offsetZ = (Math.random() - 0.5) * radius * 2;
                    world.playEffect(center.clone().add(offsetX, offsetY, offsetZ), Effect.WITCH_MAGIC, 0);
                }
                break;
            case "CRIT":
                for (int i = 0; i < particleCount; i++) {
                    double offsetX = (Math.random() - 0.5) * radius * 2;
                    double offsetY = Math.random() * radius;
                    double offsetZ = (Math.random() - 0.5) * radius * 2;
                    world.playEffect(center.clone().add(offsetX, offsetY, offsetZ), Effect.CRIT, 0);
                }
                break;
            case "MAGIC_CRIT":
                for (int i = 0; i < particleCount; i++) {
                    double offsetX = (Math.random() - 0.5) * radius * 2;
                    double offsetY = Math.random() * radius;
                    double offsetZ = (Math.random() - 0.5) * radius * 2;
                    world.playEffect(center.clone().add(offsetX, offsetY, offsetZ), Effect.MAGIC_CRIT, 0);
                }
                break;
            default:
                // Fallback para smoke
                for (int i = 0; i < particleCount; i++) {
                    world.playEffect(center, Effect.SMOKE, 4);
                }
                break;
        }
    }

    @Override
    public void preview(Player player) {
        // Mostrar partículas ao redor do jogador
        Location loc = player.getLocation().add(0, 1, 0);
        World world = loc.getWorld();

        if (useParticleApi && particleName != null) {
            spawnNamedParticles(world, loc);
        } else if (effect != null) {
            spawnEffectParticles(world, loc);
        }

        // Som de feedback
        player.playSound(player.getLocation(), Sound.ORB_PICKUP, 1.0f, 1.2f);
    }

    // ==================== GETTERS ====================

    public Effect getEffect() {
        return effect;
    }

    public int getEffectData() {
        return effectData;
    }

    public int getParticleCount() {
        return particleCount;
    }

    public float getRadius() {
        return radius;
    }

    public boolean isUsingParticleApi() {
        return useParticleApi;
    }

    public String getParticleName() {
        return particleName;
    }
}
