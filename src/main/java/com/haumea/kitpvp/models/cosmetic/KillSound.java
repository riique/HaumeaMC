package com.haumea.kitpvp.models.cosmetic;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/**
 * Cosmético de som que toca ao matar.
 * 
 * @author HaumeaMC
 */
public class KillSound extends Cosmetic {

    private final Sound sound;
    private final float volume;
    private final float pitch;
    private final boolean broadcast;
    private final int broadcastRadius;

    /**
     * Construtor para sons simples
     */
    public KillSound(String id, String displayName, String[] description,
            CosmeticRarity rarity, Material icon, int price,
            Sound sound, float volume, float pitch) {
        this(id, displayName, description, rarity, icon, price, sound, volume, pitch, false, 0);
    }

    /**
     * Construtor completo com broadcast
     * 
     * @param broadcast       Se true, toca para todos próximos
     * @param broadcastRadius Raio do broadcast em blocos
     */
    public KillSound(String id, String displayName, String[] description,
            CosmeticRarity rarity, Material icon, int price,
            Sound sound, float volume, float pitch,
            boolean broadcast, int broadcastRadius) {
        super(id, displayName, description, CosmeticType.KILL_SOUND, rarity, icon, price);
        this.sound = sound;
        this.volume = volume;
        this.pitch = pitch;
        this.broadcast = broadcast;
        this.broadcastRadius = broadcastRadius;
    }

    @Override
    public void apply(Player killer, Player victim) {
        if (victim == null)
            return;

        Location loc = victim.getLocation();

        if (broadcast && broadcastRadius > 0) {
            // Tocar para todos os jogadores próximos
            for (Player nearby : loc.getWorld().getPlayers()) {
                if (nearby.getLocation().distanceSquared(loc) <= broadcastRadius * broadcastRadius) {
                    nearby.playSound(loc, sound, volume, pitch);
                }
            }
        } else {
            // Tocar apenas para o killer
            killer.playSound(loc, sound, volume, pitch);
        }
    }

    @Override
    public void preview(Player player) {
        // Tocar som para o jogador vendo a prévia
        player.playSound(player.getLocation(), sound, volume, pitch);
    }

    // ==================== GETTERS ====================

    public Sound getSound() {
        return sound;
    }

    public float getVolume() {
        return volume;
    }

    public float getPitch() {
        return pitch;
    }

    public boolean isBroadcast() {
        return broadcast;
    }

    public int getBroadcastRadius() {
        return broadcastRadius;
    }
}
