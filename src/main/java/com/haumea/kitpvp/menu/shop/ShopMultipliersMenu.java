package com.haumea.kitpvp.menu.shop;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.managers.MultiplierManager;
import com.haumea.kitpvp.menu.BaseMenu;
import com.haumea.kitpvp.menu.MultiplierMenu;
import com.haumea.kitpvp.models.MultiplierType;
import com.haumea.kitpvp.profile.PlayerProfile;
import com.haumea.kitpvp.utils.ChatStorage;
import com.haumea.kitpvp.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * Menu de compra de Multiplicadores na Loja.
 * 
 * @author HaumeaMC
 */
public class ShopMultipliersMenu extends BaseMenu {

    private static final int PRICE_X1_5 = 2000;
    private static final int PRICE_X2_0 = 5000;
    private static final int PRICE_X2_5 = 10000;
    private static final int PRICE_X3_0 = 20000;
    private static final int PRICE_X3_5 = 50000;

    public ShopMultipliersMenu(HaumeaMC plugin, Player player) {
        super(plugin, player, "&b&l✧ LOJA DE MULTIPLICADORES", 45);
    }

    @Override
    protected void setupItems() {
        MultiplierManager mm = plugin.getMultiplierManager();
        PlayerProfile profile = plugin.getProfileManager().getProfile(player);
        long coins = profile != null ? profile.getCoins() : 0;
        Map<MultiplierType, Integer> inv = mm.getFullInventory(player);

        // Header
        ItemStack headerPane = createGlassPane(3, "&b✧");
        for (int i = 0; i < 9; i++)
            setItem(i, headerPane);

        setItem(4, new ItemBuilder(Material.GOLDEN_APPLE)
                .name("&b&l✧ LOJA DE MULTIPLICADORES")
                .lore("", "&7Compre multiplicadores de coins!", "&eDuração: &f1h por multiplicador", "")
                .glow().build());

        // Bordas
        ItemStack sidePane = createGlassPane(7);
        setItem(9, sidePane);
        setItem(17, sidePane);
        setItem(18, sidePane);
        setItem(26, sidePane);

        // Multiplicadores para compra (linha central)
        setItem(20, createBuyItem(MultiplierType.X1_5, PRICE_X1_5, coins, inv),
                (p, c) -> handlePurchase(MultiplierType.X1_5, PRICE_X1_5, coins));
        setItem(21, createBuyItem(MultiplierType.X2_0, PRICE_X2_0, coins, inv),
                (p, c) -> handlePurchase(MultiplierType.X2_0, PRICE_X2_0, coins));
        setItem(22, createBuyItem(MultiplierType.X2_5, PRICE_X2_5, coins, inv),
                (p, c) -> handlePurchase(MultiplierType.X2_5, PRICE_X2_5, coins));
        setItem(23, createBuyItem(MultiplierType.X3_0, PRICE_X3_0, coins, inv),
                (p, c) -> handlePurchase(MultiplierType.X3_0, PRICE_X3_0, coins));
        setItem(24, createBuyItem(MultiplierType.X3_5, PRICE_X3_5, coins, inv),
                (p, c) -> handlePurchase(MultiplierType.X3_5, PRICE_X3_5, coins));

        // Separador inferior
        ItemStack sep = createGlassPane(8, "&8▬▬▬");
        for (int i = 27; i < 36; i++)
            setItem(i, sep);

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
        setItem(44, new ItemBuilder(Material.NETHER_STAR).name("&a&l⚡ ATIVAR MULTIPLICADOR")
                .lore("", "&7Abrir menu de ativação", "").glow().build(),
                (p, c) -> {
                    MultiplierMenu.open(plugin, player);
                    playClickSound();
                });
    }

    private ItemStack createBuyItem(MultiplierType type, int price, long coins, Map<MultiplierType, Integer> inv) {
        boolean afford = coins >= price;
        int count = inv.getOrDefault(type, 0);
        return new ItemBuilder(type.getMaterial())
                .name(type.getDisplayRarity() + " " + type.getDisplayMultiplier())
                .amount(Math.max(1, Math.min(count, 64)))
                .lore("", "&fBônus: &a+" + type.getBonusPercentage() + "%",
                        "&fDuração: &e" + type.getFormattedDuration(),
                        "", "&fPreço: " + (afford ? "&e" : "&c") + ChatStorage.formatNumber(price),
                        "&fVocê possui: &a" + count, "",
                        afford ? "&a▶ Clique para comprar!" : "&c✘ Coins insuficientes")
                .build();
    }

    private void handlePurchase(MultiplierType type, int price, long coins) {
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

        if (plugin.getMultiplierManager().addToInventory(player, type, 1)) {
            ChatStorage.sendRaw(player, "&a&lHAUMEAMC &fComprou 1x " + type.getDisplayMultiplier() + "&f!");
            playSuccessSound();
            refresh();
        } else {
            profile.addCoins(price);
            playErrorSound();
        }
    }
}
