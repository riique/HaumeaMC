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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Menu de categorias de Kits da Loja.
 * 
 * Exibe todos os kits disponíveis para compra/aluguel.
 * Ao clicar em um kit, abre o menu de seleção de duração.
 * 
 * Layout (54 slots):
 * - Linha 0: Header com título e saldo
 * - Linhas 1-4: Kits disponíveis (28 slots)
 * - Linha 5: Navegação e ações
 * 
 * @author HaumeaMC
 */
public class ShopKitsCategoryMenu extends BaseMenu {

    private static final int[] KIT_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    private final int page;

    public ShopKitsCategoryMenu(HaumeaMC plugin, Player player) {
        this(plugin, player, 0);
    }

    public ShopKitsCategoryMenu(HaumeaMC plugin, Player player, int page) {
        super(plugin, player, "&6&l⚔ LOJA DE KITS &8- &fPágina " + (page + 1), 54);
        this.page = page;
    }

    @Override
    protected void setupItems() {
        KitManager kitManager = plugin.getKitManager();
        if (kitManager == null)
            return;

        PlayerProfile profile = plugin.getProfileManager().getProfile(player);
        long playerCoins = profile != null ? profile.getCoins() : 0;

        // ============ HEADER ============

        // Decoração do header
        ItemStack headerPane = createGlassPane(1, "&6⚔");
        for (int i = 0; i < 9; i++) {
            setItem(i, headerPane);
        }

        // Título central
        ItemStack titleItem = new ItemBuilder(Material.DIAMOND_SWORD)
                .name("&6&l⚔ LOJA DE KITS")
                .lore(
                        "",
                        "&7Compre ou alugue kits de",
                        "&7habilidades especiais!",
                        "",
                        "&6Opções disponíveis:",
                        "&8▪ &7Aluguel: &e1d &7/ &e7d &7/ &e30d",
                        "&8▪ &7Permanente: &a&lETERNO",
                        "")
                .hideAll()
                .glow()
                .build();
        setItem(4, titleItem);

        // Bordas laterais
        ItemStack sidePane = createGlassPane(7);
        for (int i = 9; i < 45; i++) {
            if (i % 9 == 0 || i % 9 == 8) {
                setItem(i, sidePane);
            }
        }

        // ============ KITS ============

        // Obter kits compráveis
        Collection<Kit> allKits = kitManager.getAllKits();
        List<Kit> purchasableKits = new ArrayList<>();
        for (Kit kit : allKits) {
            if (!kit.isDefault() && kit.getPrice() > 0) {
                purchasableKits.add(kit);
            }
        }

        // Listar kits da página atual
        int startIndex = page * KIT_SLOTS.length;
        int slotIndex = 0;

        for (int i = startIndex; i < purchasableKits.size() && slotIndex < KIT_SLOTS.length; i++) {
            Kit kit = purchasableKits.get(i);
            int slot = KIT_SLOTS[slotIndex];

            ItemStack kitItem = createKitItem(kit, kitManager, playerCoins);

            setItem(slot, kitItem, (p, click) -> {
                // Verificar se já possui permanentemente
                if (kitManager.hasKitPermission(player, kit)) {
                    long rentalTime = kitManager.getRentalTimeRemaining(player.getUniqueId(), kit.getName());
                    if (rentalTime == 0) {
                        // Possui permanente
                        ChatStorage.sendRaw(player, "&c&lHAUMEAMC &fVocê já possui este kit permanentemente!");
                        playErrorSound();
                        return;
                    }
                }

                // Abrir menu de seleção de duração
                new ShopKitPurchaseMenu(plugin, player, kit).open();
                playClickSound();
            });

            slotIndex++;
        }

        // ============ FOOTER ============

        // Linha inferior
        ItemStack footerPane = createGlassPane(15);
        for (int i = 45; i < 54; i++) {
            setItem(i, footerPane);
        }

        // Botão voltar
        setItem(45, createBackButton(), (p, click) -> {
            new ShopMainMenu(plugin, player).open();
            playClickSound();
        });

        // Saldo do jogador
        ItemStack balanceItem = new ItemBuilder(Material.GOLD_INGOT)
                .name("&e&lSEU SALDO")
                .lore(
                        "",
                        "&fMoedas: &e" + ChatStorage.formatNumber(playerCoins),
                        "",
                        "&7Clique em um kit para",
                        "&7ver opções de compra!",
                        "")
                .glow()
                .build();
        setItem(49, balanceItem);

        // Navegação de páginas
        if (purchasableKits.size() > (page + 1) * KIT_SLOTS.length) {
            ItemStack nextPage = new ItemBuilder(Material.ARROW)
                    .name("&ePróxima Página »")
                    .lore("&7Clique para ir à", "&7página " + (page + 2))
                    .build();
            setItem(53, nextPage, (p, click) -> {
                new ShopKitsCategoryMenu(plugin, player, page + 1).open();
                playClickSound();
            });
        }

        if (page > 0) {
            ItemStack prevPage = new ItemBuilder(Material.ARROW)
                    .name("&e« Página Anterior")
                    .lore("&7Clique para voltar à", "&7página " + page)
                    .build();
            setItem(45, prevPage, (p, click) -> {
                new ShopKitsCategoryMenu(plugin, player, page - 1).open();
                playClickSound();
            });
        }
    }

    /**
     * Cria o item visual de um kit para a loja
     */
    private ItemStack createKitItem(Kit kit, KitManager kitManager, long playerCoins) {
        List<String> lore = new ArrayList<>();

        // Descrição do kit
        for (String line : kit.getDescription()) {
            lore.add(ChatStorage.colorize(line));
        }

        lore.add("");
        lore.add("§6Preços:");

        // Preços com base no preço semanal
        int basePrice = kit.getPrice();
        int dailyPrice = (int) (basePrice / 7.0 * 1.5); // 1 dia = preço maior por dia
        int monthlyPrice = (int) (basePrice * 3.5); // 30 dias = desconto
        int permanentPrice = basePrice * 10; // Permanente = 10x o semanal

        lore.add("§8▪ §71 dia: §e" + ChatStorage.formatNumber(dailyPrice) + " coins");
        lore.add("§8▪ §71 semana: §e" + ChatStorage.formatNumber(basePrice) + " coins");
        lore.add("§8▪ §71 mês: §e" + ChatStorage.formatNumber(monthlyPrice) + " coins");
        lore.add("§8▪ §a§lPERMANENTE: §e" + ChatStorage.formatNumber(permanentPrice) + " coins");
        lore.add("");

        // Status
        boolean hasKit = kitManager.hasKitPermission(player, kit);
        long rentalTime = kitManager.getRentalTimeRemaining(player.getUniqueId(), kit.getName());

        if (hasKit) {
            if (rentalTime == 0) {
                lore.add("§a§l✔ VOCÊ JÁ POSSUI (Permanente)");
            } else if (rentalTime > 0) {
                lore.add("§a§l✔ ALUGADO");
                lore.add("§eExpira em: §f" + formatTimeRemaining(rentalTime));
            } else {
                lore.add("§a§l✔ VOCÊ JÁ POSSUI");
            }
        } else {
            lore.add("§e▶ Clique para comprar!");
        }

        ItemBuilder builder = new ItemBuilder(kit.getIcon(), 1, kit.getIconData())
                .name(kit.getDisplayName())
                .lore(lore);

        if (hasKit) {
            builder.glow();
        }

        return builder.build();
    }

    /**
     * Formata o tempo restante em formato legível
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
