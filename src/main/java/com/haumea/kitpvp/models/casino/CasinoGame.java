package com.haumea.kitpvp.models.casino;

import org.bukkit.Material;

/**
 * Enum representando os tipos de jogos de cassino disponíveis.
 * 
 * @author HaumeaMC
 */
public enum CasinoGame {

    SLOTS("Slots", Material.GOLD_BLOCK, "slots"),
    ROULETTE("Roleta", Material.REDSTONE_BLOCK, "roulette"),
    BLACKJACK("Blackjack", Material.PAPER, "blackjack"),
    COINFLIP("Coinflip", Material.GOLD_INGOT, "coinflip"),
    CRASH("Crash", Material.TNT, "crash");

    private final String displayName;
    private final Material icon;
    private final String configKey;

    CasinoGame(String displayName, Material icon, String configKey) {
        this.displayName = displayName;
        this.icon = icon;
        this.configKey = configKey;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Material getIcon() {
        return icon;
    }

    public String getConfigKey() {
        return configKey;
    }
}
