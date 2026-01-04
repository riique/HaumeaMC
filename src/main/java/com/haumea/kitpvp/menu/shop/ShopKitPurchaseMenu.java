package com.haumea.kitpvp.menu.shop;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.managers.KitManager;
import com.haumea.kitpvp.menu.BaseMenu;
import com.haumea.kitpvp.models.Kit;
import com.haumea.kitpvp.profile.PlayerProfile;
import com.haumea.kitpvp.utils.ChatStorage;
import com.haumea.kitpvp.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Menu de seleção de duração para compra/aluguel de kit.
 * 
 * Mostra as opções de duração disponíveis:
 * - 1 Dia (preço maior por dia)
 * - 1 Semana (preço base)
 * - 1 Mês (desconto por volume)
 * - Permanente (compra definitiva)
 * 
 * Layout (45 slots):
 * - Linha 0: Header com info do kit
 * - Linha 1: Separador
 * - Linha 2: Opções de duração
 * - Linha 3: Separador
 * - Linha 4: Navegação
 * 
 * @author HaumeaMC
 */
public class ShopKitPurchaseMenu extends BaseMenu {

    private final Kit kit;

    // Preços calculados baseados no preço semanal
    private final int dailyPrice;
    private final int weeklyPrice;
    private final int monthlyPrice;
    private final int permanentPrice;

    public ShopKitPurchaseMenu(HaumeaMC plugin, Player player, Kit kit) {
        super(plugin, player, "&6&l⚔ " + kit.getDisplayName(), 45);
        this.kit = kit;

        // Calcular preços
        this.weeklyPrice = kit.getPrice();
        this.dailyPrice = (int) (weeklyPrice / 7.0 * 1.5);
        this.monthlyPrice = (int) (weeklyPrice * 3.5);
        this.permanentPrice = weeklyPrice * 10;
    }

    @Override
    protected void setupItems() {
        KitManager kitManager = plugin.getKitManager();
        PlayerProfile profile = plugin.getProfileManager().getProfile(player);
        long playerCoins = profile != null ? profile.getCoins() : 0;

        // ============ HEADER ============

        // Background do header
        ItemStack headerPane = createGlassPane(1, "&6✦");
        for (int i = 0; i < 9; i++) {
            setItem(i, headerPane);
        }

        // Info do kit no centro
        ItemStack kitInfo = new ItemBuilder(kit.getIcon(), 1, kit.getIconData())
                .name(kit.getDisplayName())
                .lore(kit.getDescription().toArray(new String[0]))
                .glow()
                .build();
        setItem(4, kitInfo);

        // ============ SEPARADOR ============

        ItemStack separator = createGlassPane(8, "&8▬▬▬");
        for (int i = 9; i < 18; i++) {
            setItem(i, separator);
        }

        // ============ OPÇÕES DE DURAÇÃO ============

        // Verificar status atual
        boolean hasKit = kitManager != null && kitManager.hasKitPermission(player, kit);
        long rentalTime = kitManager != null ? kitManager.getRentalTimeRemaining(player.getUniqueId(), kit.getName())
                : -1;
        boolean hasPermanent = hasKit && rentalTime == 0;

        // 1 Dia
        boolean canAffordDaily = playerCoins >= dailyPrice;
        ItemStack dailyItem = new ItemBuilder(Material.COAL)
                .name("&e&l☀ 1 DIA")
                .lore(
                        "",
                        "&7Alugue este kit por",
                        "&71 dia de uso.",
                        "",
                        "&fPreço: " + (canAffordDaily ? "&e" : "&c") + ChatStorage.formatNumber(dailyPrice) + " coins",
                        "&fSeu saldo: &e" + ChatStorage.formatNumber(playerCoins),
                        "",
                        hasPermanent ? "&c&l✘ Você já possui permanente!"
                                : canAffordDaily ? "&a▶ Clique para comprar!" : "&c✘ Coins insuficientes")
                .build();
        setItem(19, dailyItem, (p, click) -> {
            processPurchase(1, dailyPrice, hasPermanent, canAffordDaily);
        });

        // 1 Semana
        boolean canAffordWeekly = playerCoins >= weeklyPrice;
        ItemStack weeklyItem = new ItemBuilder(Material.IRON_INGOT)
                .name("&b&l✦ 1 SEMANA")
                .lore(
                        "",
                        "&7Alugue este kit por",
                        "&71 semana de uso.",
                        "",
                        "&6&lMELHOR CUSTO-BENEFÍCIO!",
                        "",
                        "&fPreço: " + (canAffordWeekly ? "&e" : "&c") + ChatStorage.formatNumber(weeklyPrice)
                                + " coins",
                        "&fSeu saldo: &e" + ChatStorage.formatNumber(playerCoins),
                        "",
                        hasPermanent ? "&c&l✘ Você já possui permanente!"
                                : canAffordWeekly ? "&a▶ Clique para comprar!" : "&c✘ Coins insuficientes")
                .glow()
                .build();
        setItem(21, weeklyItem, (p, click) -> {
            processPurchase(7, weeklyPrice, hasPermanent, canAffordWeekly);
        });

        // 1 Mês
        boolean canAffordMonthly = playerCoins >= monthlyPrice;
        int savings = (weeklyPrice * 4) - monthlyPrice;
        ItemStack monthlyItem = new ItemBuilder(Material.GOLD_INGOT)
                .name("&6&l✧ 1 MÊS")
                .lore(
                        "",
                        "&7Alugue este kit por",
                        "&730 dias de uso.",
                        "",
                        "&a&lECONOMIZE: &e" + ChatStorage.formatNumber(savings) + " coins!",
                        "",
                        "&fPreço: " + (canAffordMonthly ? "&e" : "&c") + ChatStorage.formatNumber(monthlyPrice)
                                + " coins",
                        "&fSeu saldo: &e" + ChatStorage.formatNumber(playerCoins),
                        "",
                        hasPermanent ? "&c&l✘ Você já possui permanente!"
                                : canAffordMonthly ? "&a▶ Clique para comprar!" : "&c✘ Coins insuficientes")
                .build();
        setItem(23, monthlyItem, (p, click) -> {
            processPurchase(30, monthlyPrice, hasPermanent, canAffordMonthly);
        });

        // Permanente
        boolean canAffordPermanent = playerCoins >= permanentPrice;
        ItemStack permanentItem = new ItemBuilder(Material.DIAMOND)
                .name("&a&l★ PERMANENTE ★")
                .lore(
                        "",
                        "&7Compre este kit para",
                        "&7sempre! Nunca expira.",
                        "",
                        "&d&l✦ ACESSO ETERNO ✦",
                        "",
                        "&fPreço: " + (canAffordPermanent ? "&e" : "&c") + ChatStorage.formatNumber(permanentPrice)
                                + " coins",
                        "&fSeu saldo: &e" + ChatStorage.formatNumber(playerCoins),
                        "",
                        hasPermanent ? "&a&l✔ VOCÊ JÁ POSSUI!"
                                : canAffordPermanent ? "&a▶ Clique para comprar!" : "&c✘ Coins insuficientes")
                .glow()
                .build();
        setItem(25, permanentItem, (p, click) -> {
            if (hasPermanent) {
                ChatStorage.sendRaw(player, "&c&lHAUMEAMC &fVocê já possui este kit permanentemente!");
                playErrorSound();
                return;
            }
            if (!canAffordPermanent) {
                ChatStorage.sendRaw(player, "&c&lHAUMEAMC &fVocê não possui coins suficientes!");
                playErrorSound();
                return;
            }
            // Processar compra permanente
            processPermanentPurchase();
        });

        // ============ SEPARADOR INFERIOR ============

        for (int i = 27; i < 36; i++) {
            setItem(i, separator);
        }

        // ============ FOOTER ============

        ItemStack footerPane = createGlassPane(15);
        for (int i = 36; i < 45; i++) {
            setItem(i, footerPane);
        }

        // Botão voltar
        setItem(36, createBackButton(), (p, click) -> {
            new ShopKitsCategoryMenu(plugin, player).open();
            playClickSound();
        });

        // Status atual (se alugado)
        if (hasKit && !hasPermanent && rentalTime > 0) {
            ItemStack statusItem = new ItemBuilder(Material.WATCH)
                    .name("&a&l✔ KIT ALUGADO")
                    .lore(
                            "",
                            "&7Você possui este kit alugado.",
                            "",
                            "&fTempo restante: &e" + formatTimeRemaining(rentalTime),
                            "",
                            "&6Dica: &7Compre mais tempo para",
                            "&7estender seu aluguel!")
                    .glow()
                    .build();
            setItem(40, statusItem);
        }

        // Botão fechar
        setClickableItem(44, createCloseButton(), this::close);
    }

    /**
     * Processa a compra de aluguel
     */
    private void processPurchase(int days, int price, boolean hasPermanent, boolean canAfford) {
        if (hasPermanent) {
            ChatStorage.sendRaw(player, "&c&lHAUMEAMC &fVocê já possui este kit permanentemente!");
            playErrorSound();
            return;
        }

        if (!canAfford) {
            ChatStorage.sendRaw(player, "&c&lHAUMEAMC &fVocê não possui coins suficientes!");
            playErrorSound();
            return;
        }

        KitManager kitManager = plugin.getKitManager();
        if (kitManager == null) {
            ChatStorage.sendRaw(player, "&c&lHAUMEAMC &fErro ao processar sua compra!");
            playErrorSound();
            close();
            return;
        }

        // Remover coins primeiro
        PlayerProfile profile = plugin.getProfileManager().getProfile(player);
        if (profile == null || !profile.removeCoins(price)) {
            ChatStorage.sendRaw(player, "&c&lHAUMEAMC &fErro ao processar pagamento!");
            playErrorSound();
            close();
            return;
        }

        // Alugar o kit
        boolean success = kitManager.rentKit(player, kit.getName(), days, false); // false = já removemos as coins

        if (success) {
            String duration = days == 1 ? "1 dia" : days == 7 ? "1 semana" : "1 mês";
            ChatStorage.sendRaw(player, "");
            ChatStorage.sendRaw(player, "&a&l  ✔ COMPRA REALIZADA!");
            ChatStorage.sendRaw(player, "");
            ChatStorage.sendRaw(player,
                    "&f  Você alugou o kit " + kit.getDisplayName() + " &fpor &e" + duration + "&f!");
            ChatStorage.sendRaw(player, "&f  Coins gastos: &e" + ChatStorage.formatNumber(price));
            ChatStorage.sendRaw(player, "");
            playLevelUpSound();
        } else {
            // Devolver coins se falhou
            profile.addCoins(price);
            ChatStorage.sendRaw(player, "&c&lHAUMEAMC &fErro ao processar sua compra!");
            playErrorSound();
        }

        close();
    }

    /**
     * Processa a compra permanente
     */
    private void processPermanentPurchase() {
        KitManager kitManager = plugin.getKitManager();
        if (kitManager == null) {
            ChatStorage.sendRaw(player, "&c&lHAUMEAMC &fErro ao processar sua compra!");
            playErrorSound();
            close();
            return;
        }

        PlayerProfile profile = plugin.getProfileManager().getProfile(player);
        if (profile == null || !profile.removeCoins(permanentPrice)) {
            ChatStorage.sendRaw(player, "&c&lHAUMEAMC &fErro ao processar pagamento!");
            playErrorSound();
            close();
            return;
        }

        // Alugar com 0 dias = permanente
        boolean success = kitManager.rentKit(player, kit.getName(), 0, false);

        if (success) {
            ChatStorage.sendRaw(player, "");
            ChatStorage.sendRaw(player, "&a&l  ★ COMPRA PERMANENTE ★");
            ChatStorage.sendRaw(player, "");
            ChatStorage.sendRaw(player, "&f  Você comprou o kit " + kit.getDisplayName() + " &a&lPARA SEMPRE&f!");
            ChatStorage.sendRaw(player, "&f  Coins gastos: &e" + ChatStorage.formatNumber(permanentPrice));
            ChatStorage.sendRaw(player, "");
            playLevelUpSound();

            // Título especial para compra permanente
            try {
                player.sendTitle(
                        ChatStorage.colorize("&a&l★ KIT ADQUIRIDO ★"),
                        ChatStorage.colorize(kit.getDisplayName() + " &f- &a&lPERMANENTE"));
            } catch (Exception ignored) {
            }
        } else {
            profile.addCoins(permanentPrice);
            ChatStorage.sendRaw(player, "&c&lHAUMEAMC &fErro ao processar sua compra!");
            playErrorSound();
        }

        close();
    }

    /**
     * Formata o tempo restante
     */
    private String formatTimeRemaining(long millis) {
        if (millis <= 0)
            return "Expirado";

        long days = millis / (24 * 60 * 60 * 1000);
        long hours = (millis % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000);
        long minutes = (millis % (60 * 60 * 1000)) / (60 * 1000);

        StringBuilder sb = new StringBuilder();
        if (days > 0)
            sb.append(days).append("d ");
        if (hours > 0)
            sb.append(hours).append("h ");
        if (minutes > 0 || sb.length() == 0)
            sb.append(minutes).append("m");

        return sb.toString().trim();
    }
}
