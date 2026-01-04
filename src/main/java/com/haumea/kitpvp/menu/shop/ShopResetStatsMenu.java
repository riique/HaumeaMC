package com.haumea.kitpvp.menu.shop;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.managers.StatsManager;
import com.haumea.kitpvp.menu.BaseMenu;
import com.haumea.kitpvp.profile.PlayerProfile;
import com.haumea.kitpvp.utils.ChatStorage;
import com.haumea.kitpvp.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Menu de Reset de Estatísticas na Loja.
 * 
 * Permite ao jogador resetar suas estatísticas:
 * - Reset de Kills
 * - Reset de Killstreak
 * - Reset Completo (tudo)
 * 
 * @author HaumeaMC
 */
public class ShopResetStatsMenu extends BaseMenu {

    private static final int PRICE_RESET_KILLS = 25000;
    private static final int PRICE_RESET_KILLSTREAK = 10000;
    private static final int PRICE_RESET_ALL = 75000;

    public ShopResetStatsMenu(HaumeaMC plugin, Player player) {
        super(plugin, player, "&c&l⚡ RESET DE ESTATÍSTICAS", 45);
    }

    @Override
    protected void setupItems() {
        StatsManager stats = plugin.getStatsManager();
        PlayerProfile profile = plugin.getProfileManager().getProfile(player);
        long coins = profile != null ? profile.getCoins() : 0;

        int kills = stats.getKills(player);
        int deaths = stats.getDeaths(player);
        int killstreak = stats.getKillstreak(player);
        String kdr = stats.getKdrFormatted(player);

        // Header
        ItemStack headerPane = createGlassPane(14, "&c⚡");
        for (int i = 0; i < 9; i++)
            setItem(i, headerPane);

        setItem(4, new ItemBuilder(Material.ANVIL)
                .name("&c&l⚡ RESET DE ESTATÍSTICAS")
                .lore("", "&7Recomece do zero!", "", "&fSuas estatísticas atuais:",
                        "&8▪ &fKills: &a" + kills, "&8▪ &fDeaths: &c" + deaths,
                        "&8▪ &fKillstreak: &e" + killstreak, "&8▪ &fKDR: &b" + kdr, "")
                .build());

        // Bordas
        ItemStack sidePane = createGlassPane(7);
        setItem(9, sidePane);
        setItem(17, sidePane);
        setItem(18, sidePane);
        setItem(26, sidePane);
        setItem(27, sidePane);
        setItem(35, sidePane);

        // Reset Kills (slot 11)
        boolean canKills = coins >= PRICE_RESET_KILLS && kills > 0;
        setItem(11, new ItemBuilder(Material.IRON_SWORD)
                .name("&e&l☠ RESET DE KILLS")
                .lore("", "&7Zere suas kills.", "", "&fKills atuais: &a" + kills,
                        "&fPreço: " + (canKills ? "&e" : "&c") + ChatStorage.formatNumber(PRICE_RESET_KILLS), "",
                        kills == 0 ? "&7Sem kills para resetar"
                                : canKills ? "&a▶ Clique para resetar!" : "&c✘ Coins insuficientes")
                .hideAll().build(),
                (p, c) -> handleReset("kills", PRICE_RESET_KILLS, kills > 0, coins));

        // Reset Killstreak (slot 13)
        boolean canKS = coins >= PRICE_RESET_KILLSTREAK && killstreak > 0;
        setItem(13, new ItemBuilder(Material.BLAZE_POWDER)
                .name("&6&l🔥 RESET DE KILLSTREAK")
                .lore("", "&7Zere seu killstreak.", "",
                        "&fKillstreak atual: &e" + killstreak,
                        "&fPreço: " + (canKS ? "&e" : "&c") + ChatStorage.formatNumber(PRICE_RESET_KILLSTREAK), "",
                        killstreak == 0 ? "&7Sem killstreak para resetar"
                                : canKS ? "&a▶ Clique para resetar!" : "&c✘ Coins insuficientes")
                .build(),
                (p, c) -> handleReset("killstreak", PRICE_RESET_KILLSTREAK, killstreak > 0, coins));

        // Reset Completo (slot 15)
        boolean hasStats = kills > 0 || deaths > 0 || killstreak > 0;
        boolean canAll = coins >= PRICE_RESET_ALL && hasStats;
        setItem(15, new ItemBuilder(Material.TNT)
                .name("&4&l⚠ RESET COMPLETO ⚠")
                .lore("", "&cRESETA TUDO:", "&8▪ &fKills → &c0", "&8▪ &fDeaths → &c0",
                        "&8▪ &fKillstreak → &c0", "",
                        "&fPreço: " + (canAll ? "&e" : "&c") + ChatStorage.formatNumber(PRICE_RESET_ALL), "",
                        !hasStats ? "&7Sem estatísticas para resetar"
                                : canAll ? "&c▶ CLIQUE PARA RESETAR TUDO!" : "&c✘ Coins insuficientes")
                .build(),
                (p, c) -> handleReset("all", PRICE_RESET_ALL, hasStats, coins));

        // Footer
        ItemStack footer = createGlassPane(15);
        for (int i = 36; i < 45; i++)
            setItem(i, footer);

        setItem(36, createBackButton(), (p, c) -> {
            new ShopMainMenu(plugin, player).open();
            playClickSound();
        });
        setItem(40, new ItemBuilder(Material.GOLD_INGOT).name("&e&lSEU SALDO")
                .lore("", "&fMoedas: &e" + ChatStorage.formatNumber(coins), "").glow().build());
        setClickableItem(44, createCloseButton(), this::close);
    }

    private void handleReset(String type, int price, boolean hasValue, long coins) {
        if (!hasValue) {
            ChatStorage.sendRaw(player, "&c&lHAUMEAMC &fNada para resetar!");
            playErrorSound();
            return;
        }
        if (coins < price) {
            ChatStorage.sendRaw(player, "&c&lHAUMEAMC &fCoins insuficientes!");
            playErrorSound();
            return;
        }

        PlayerProfile profile = plugin.getProfileManager().getProfile(player);
        if (profile == null || !profile.removeCoins(price)) {
            playErrorSound();
            return;
        }

        StatsManager stats = plugin.getStatsManager();
        switch (type) {
            case "kills":
                stats.setKills(player, 0);
                break;
            case "killstreak":
                stats.setKillstreak(player, 0);
                break;
            case "all":
                stats.setKills(player, 0);
                stats.setDeaths(player, 0);
                stats.setKillstreak(player, 0);
                break;
        }

        String msg = type.equals("all") ? "Todas as estatísticas"
                : type.equals("kills") ? "Kills" : "Killstreak";
        ChatStorage.sendRaw(player, "&a&lHAUMEAMC &f" + msg + " resetado(as) com sucesso!");
        playLevelUpSound();
        refresh();
    }
}
