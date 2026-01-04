package com.haumea.kitpvp.models.cosmetic;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/**
 * Cosmético de mensagem customizada ao matar.
 * Exibe uma mensagem especial no chat do killer.
 * 
 * @author HaumeaMC
 */
public class KillMessage extends Cosmetic {

    private final String messageFormat;
    private final boolean showToVictim;
    private final Sound sound;

    /**
     * Construtor para mensagens de kill
     * 
     * @param messageFormat Formato da mensagem (placeholders: {killer}, {victim})
     * @param showToVictim  Se true, mostra também para a vítima
     */
    public KillMessage(String id, String displayName, String[] description,
            CosmeticRarity rarity, Material icon, int price,
            String messageFormat, boolean showToVictim) {
        this(id, displayName, description, rarity, icon, price, messageFormat, showToVictim, null);
    }

    /**
     * Construtor completo com som
     */
    public KillMessage(String id, String displayName, String[] description,
            CosmeticRarity rarity, Material icon, int price,
            String messageFormat, boolean showToVictim, Sound sound) {
        super(id, displayName, description, CosmeticType.KILL_MESSAGE, rarity, icon, price);
        this.messageFormat = messageFormat;
        this.showToVictim = showToVictim;
        this.sound = sound;
    }

    @Override
    public void apply(Player killer, Player victim) {
        if (victim == null)
            return;

        String killerName = killer.getName();
        String victimName = victim.getName();

        // Formatar mensagem
        String message = formatMessage(killerName, victimName);

        // Enviar para o killer
        killer.sendMessage(message);

        // Som para o killer
        if (sound != null) {
            killer.playSound(killer.getLocation(), sound, 1.0f, 1.0f);
        }

        // Enviar para a vítima se configurado
        if (showToVictim) {
            victim.sendMessage(message);
        }
    }

    /**
     * Formata a mensagem substituindo placeholders
     */
    public String formatMessage(String killerName, String victimName) {
        return messageFormat
                .replace("{killer}", killerName)
                .replace("{victim}", victimName)
                .replace("&", "§");
    }

    @Override
    public void preview(Player player) {
        // Mostrar prévia da mensagem
        String message = formatMessage(player.getName(), "Alvo");
        player.sendMessage("§7[Prévia] " + message);

        if (sound != null) {
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        }
    }

    // ==================== GETTERS ====================

    public String getMessageFormat() {
        return messageFormat;
    }

    public boolean isShowToVictim() {
        return showToVictim;
    }

    public Sound getSound() {
        return sound;
    }
}
