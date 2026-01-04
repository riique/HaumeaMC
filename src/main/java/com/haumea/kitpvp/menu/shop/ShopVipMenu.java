package com.haumea.kitpvp.menu.shop;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.managers.VipShopManager;
import com.haumea.kitpvp.managers.VipShopManager.VipOffer;
import com.haumea.kitpvp.menu.BaseMenu;
import com.haumea.kitpvp.profile.PlayerProfile;
import com.haumea.kitpvp.utils.ChatStorage;
import com.haumea.kitpvp.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Menu de VIPs na Loja do HaumeaMC.
 * 
 * Exibe todos os VIPs disponíveis para compra semanal por coins.
 * Design premium com visual atraente e informativo.
 * 
 * Layout (54 slots):
 * - Linha 0: Header decorativo premium
 * - Linha 1: Separador e saldo
 * - Linhas 2-3: VIPs disponíveis
 * - Linha 4: Separador
 * - Linha 5: Navegação
 * 
 * @author HaumeaMC
 */
public class ShopVipMenu extends BaseMenu {

    public ShopVipMenu(HaumeaMC plugin, Player player) {
        super(plugin, player, "&d&l✦ LOJA DE VIP ✦", 54);
    }

    @Override
    protected void setupItems() {
        VipShopManager vipShopManager = plugin.getVipShopManager();
        PlayerProfile profile = plugin.getProfileManager().getProfile(player);
        long playerCoins = profile != null ? profile.getCoins() : 0;

        // ============ FUNDO GRADIENTE PREMIUM ============
        fillPremiumBackground();

        // ============ HEADER DECORATIVO ============

        // Decoração do header com vidro roxo
        ItemStack headerPane = createGlassPane(10, "&d✦"); // Roxo
        for (int i = 0; i < 9; i++) {
            if (i != 4) {
                setItem(i, headerPane);
            }
        }

        // Título central
        ItemStack titleItem = new ItemBuilder(Material.DIAMOND)
                .name("&d&l✦ LOJA DE VIP ✦")
                .lore(
                        "",
                        "&7Adquira VIPs com suas &ecoins&7!",
                        "",
                        "&7VIPs semanais comprados aqui",
                        "&7se &aacumulam &7com compras anteriores.",
                        "",
                        "&6&l» &fDuração: &e1 semana &fpor compra",
                        "",
                        "&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬")
                .glow()
                .build();
        setItem(4, titleItem);

        // ============ SALDO DO JOGADOR ============

        ItemStack balanceItem = new ItemBuilder(Material.GOLD_INGOT)
                .name("&e&l⛃ SEU SALDO")
                .lore(
                        "",
                        "&fMoedas: &e" + ChatStorage.formatNumber(playerCoins) + " coins",
                        "",
                        "&7Use suas moedas para",
                        "&7adquirir VIPs semanais!",
                        "",
                        "&7Dica: VIPs de moedas",
                        "&7se &asomam &7com VIPs reais!",
                        "")
                .glow()
                .build();
        setItem(13, balanceItem);

        // ============ SEPARADOR ============

        ItemStack separator = createGlassPane(8, "&8▬▬▬");
        for (int i = 9; i < 18; i++) {
            if (i != 13) {
                setItem(i, separator);
            }
        }

        // ============ VIPs DISPONÍVEIS ============

        if (vipShopManager != null) {
            List<VipOffer> offers = vipShopManager.getVipOffers();

            // Posições para os 4 VIPs (centrados)
            int[] vipSlots = { 20, 22, 24, 31 }; // Slots centralizados

            // Se tiver até 4 VIPs
            if (offers.size() <= 4) {
                // Reposicionar para ficar mais centralizado
                if (offers.size() == 4) {
                    vipSlots = new int[] { 19, 21, 23, 25 };
                }
            }

            for (int i = 0; i < offers.size() && i < vipSlots.length; i++) {
                VipOffer offer = offers.get(i);
                int slot = vipSlots[i];

                // Criar item do VIP
                ItemStack vipItem = createVipItem(offer, playerCoins, vipShopManager);

                setItem(slot, vipItem, (p, click) -> {
                    handleVipClick(offer, vipShopManager);
                });
            }
        }

        // ============ SEPARADOR INFERIOR ============

        for (int i = 36; i < 45; i++) {
            setItem(i, separator);
        }

        // ============ FOOTER ============

        // Info sobre VIPs reais
        ItemStack infoItem = new ItemBuilder(Material.BOOK)
                .name("&b&lINFORMAÇÕES")
                .lore(
                        "",
                        "&6VIPs por Coins:",
                        "&7• Duração de &e1 semana",
                        "&7• Pode comprar várias vezes",
                        "&7• Tempo &ase acumula",
                        "",
                        "&6VIPs por Dinheiro Real:",
                        "&7• Permanentes ou mensais",
                        "&7• Mais benefícios",
                        "&7• Acesse: &ehaumeamc.com/loja",
                        "",
                        "&7Se você já tem VIP real,",
                        "&7o VIP de coins &aestende &7o tempo!",
                        "")
                .build();
        setItem(45, infoItem);

        // Botão voltar
        setItem(48, createBackButton(), (p, click) -> {
            new ShopMainMenu(plugin, player).open();
            playClickSound();
        });

        // Botão fechar
        setClickableItem(50, createCloseButton(), this::close);

        // Comparativo de VIPs
        ItemStack compareItem = new ItemBuilder(Material.PAPER)
                .name("&e&lCOMPARATIVO")
                .lore(
                        "",
                        "&a&lLIGHT &7- Básico, acessível",
                        "&6&lPREMIUM &7- Balanceado",
                        "&1&lBETA &7- Avançado",
                        "&d&lULTRA &7- Máximo poder",
                        "",
                        "&7Quanto maior o VIP,",
                        "&7mais benefícios você tem!",
                        "")
                .build();
        setItem(53, compareItem);
    }

    /**
     * Cria o item de exibição de um VIP
     */
    private ItemStack createVipItem(VipOffer offer, long playerCoins, VipShopManager manager) {
        // Determinar material baseado no VIP
        Material material;
        short data = 0;

        switch (offer.getGroupName().toLowerCase()) {
            case "light":
                material = Material.EMERALD;
                break;
            case "premium":
                material = Material.GOLD_BLOCK;
                break;
            case "beta":
                material = Material.LAPIS_BLOCK;
                break;
            case "ultra":
                material = Material.DIAMOND_BLOCK;
                break;
            default:
                material = Material.NETHER_STAR;
        }

        // Verificar status
        boolean canAfford = playerCoins >= offer.getPrice();
        boolean hasPermanent = manager.hasPermanentVip(player, offer.getGroupName());
        long timeRemaining = manager.getVipTimeRemaining(player, offer.getGroupName());
        boolean hasTemp = timeRemaining > 0;

        // Construir lore
        List<String> lore = new ArrayList<>();

        // Descrição do VIP
        for (String line : offer.getDescription()) {
            lore.add(line);
        }

        // Preço e duração
        lore.add("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add("");
        lore.add("&6Preço: " + (canAfford ? "&e" : "&c") + ChatStorage.formatNumber(offer.getPrice()) + " coins");
        lore.add("&6Duração: &f1 semana");
        lore.add("&fSeu saldo: &e" + ChatStorage.formatNumber(playerCoins));
        lore.add("");

        // Status
        if (hasPermanent) {
            lore.add("&a&l✔ VOCÊ POSSUI PERMANENTE!");
            lore.add("&7Comprado com dinheiro real.");
            lore.add("");
            lore.add("&8(Não é possível comprar)");
        } else if (hasTemp) {
            lore.add("&a&l✔ VIP ATIVO");
            lore.add("&fTempo restante: &e" + ChatStorage.formatTime(timeRemaining));
            lore.add("");
            if (canAfford) {
                lore.add("&a▶ Clique para ESTENDER +7 dias!");
            } else {
                lore.add("&c✘ Coins insuficientes para estender");
            }
        } else {
            if (canAfford) {
                lore.add("&a▶ Clique para comprar!");
            } else {
                lore.add("&c✘ Coins insuficientes");
            }
        }

        // Criar item
        ItemBuilder builder = new ItemBuilder(material, 1, data)
                .name(offer.getDisplayName() + " &8[SEMANAL]")
                .lore(lore.toArray(new String[0]));

        // Glow se pode comprar ou tem ativo
        if ((canAfford && !hasPermanent) || hasTemp) {
            builder.glow();
        }

        return builder.build();
    }

    /**
     * Processa o clique em um VIP
     */
    private void handleVipClick(VipOffer offer, VipShopManager manager) {
        VipShopManager.PurchaseResult result = manager.canPurchase(player, offer);

        switch (result) {
            case CAN_PURCHASE:
                // Abrir menu de confirmação
                new ShopVipConfirmMenu(plugin, player, offer).open();
                playClickSound();
                break;

            case ALREADY_PERMANENT:
                ChatStorage.sendRaw(player, "&c&lHAUMEAMC &fVocê já possui " +
                        ChatStorage.colorize(offer.getDisplayName()) + " &fpermanentemente!");
                playErrorSound();
                break;

            case NOT_ENOUGH_COINS:
                ChatStorage.sendRaw(player, "&c&lHAUMEAMC &fVocê não possui coins suficientes!");
                ChatStorage.sendRaw(player, "&7Preço: &e" + ChatStorage.formatNumber(offer.getPrice()) + " coins");
                playErrorSound();
                break;

            default:
                ChatStorage.sendRaw(player, "&c&lHAUMEAMC &fErro ao processar sua compra!");
                playErrorSound();
                break;
        }
    }

    /**
     * Preenche o fundo com um gradiente premium
     */
    private void fillPremiumBackground() {
        // Linhas 2-3 (área dos VIPs)
        for (int i = 18; i < 36; i++) {
            int col = i % 9;
            if (col == 0 || col == 8) {
                setItem(i, createGlassPane(10)); // Bordas roxas
            } else {
                setItem(i, createGlassPane(15)); // Centro preto
            }
        }

        // Linha 5 (footer)
        for (int i = 45; i < 54; i++) {
            setItem(i, createGlassPane(15)); // Preto
        }
    }

    /**
     * Método estático para abrir o menu
     */
    public static void open(HaumeaMC plugin, Player player) {
        new ShopVipMenu(plugin, player).open();
    }
}
