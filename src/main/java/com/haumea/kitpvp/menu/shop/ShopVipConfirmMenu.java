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
 * Menu de confirmação de compra de VIP.
 * 
 * Design premium com informações claras sobre a compra.
 * Mostra resumo do VIP, preço e tempo que será adicionado.
 * 
 * Layout (27 slots):
 * - Linha 0: Header com info do VIP
 * - Linha 1: Botões de confirmar/cancelar
 * - Linha 2: Informações adicionais
 * 
 * @author HaumeaMC
 */
public class ShopVipConfirmMenu extends BaseMenu {

    private final VipOffer offer;

    public ShopVipConfirmMenu(HaumeaMC plugin, Player player, VipOffer offer) {
        super(plugin, player, "&a&l✔ CONFIRMAR COMPRA", 27);
        this.offer = offer;
    }

    @Override
    protected void setupItems() {
        VipShopManager vipShopManager = plugin.getVipShopManager();
        PlayerProfile profile = plugin.getProfileManager().getProfile(player);
        long playerCoins = profile != null ? profile.getCoins() : 0;

        // ============ FUNDO ============
        fillBorders(15); // Vidro preto nas bordas

        // Centro mais claro
        for (int i = 10; i <= 16; i++) {
            if (i != 13) {
                setItem(i, createGlassPane(7)); // Cinza
            }
        }

        // ============ HEADER ============

        // Decoração do header
        ItemStack headerPane = createGlassPane(5, "&a✔"); // Lime
        setItem(0, headerPane);
        setItem(1, headerPane);
        setItem(2, headerPane);
        setItem(3, headerPane);
        setItem(5, headerPane);
        setItem(6, headerPane);
        setItem(7, headerPane);
        setItem(8, headerPane);

        // Info do VIP no centro
        ItemStack vipInfo = createVipInfoItem(vipShopManager, playerCoins);
        setItem(4, vipInfo);

        // ============ BOTÕES DE AÇÃO ============

        // Verificar se já tem VIP ativo (para mostrar "estender")
        long timeRemaining = vipShopManager != null ? vipShopManager.getVipTimeRemaining(player, offer.getGroupName())
                : -1;
        boolean isExtension = timeRemaining > 0;

        // Botão CONFIRMAR (esquerda)
        String confirmName = isExtension ? "&a&l✔ ESTENDER VIP" : "&a&l✔ CONFIRMAR COMPRA";
        List<String> confirmLore = new ArrayList<>();
        confirmLore.add("");
        confirmLore.add("&fVocê irá " + (isExtension ? "estender" : "ativar") + ":");
        confirmLore.add("  " + offer.getDisplayName());
        confirmLore.add("");
        confirmLore.add("&fVigor: &a+7 dias");
        confirmLore.add("&fCusto: &e" + ChatStorage.formatNumber(offer.getPrice()) + " coins");
        confirmLore.add("");
        if (isExtension) {
            confirmLore.add("&7Seu tempo atual: &e" + ChatStorage.formatTime(timeRemaining));
            confirmLore.add("&7Novo tempo: &a" + ChatStorage.formatTime(timeRemaining + offer.getDuration()));
            confirmLore.add("");
        }
        confirmLore.add("&a▶ Clique para confirmar!");

        ItemStack confirmItem = new ItemBuilder(Material.STAINED_CLAY, 1, (short) 5) // Lime clay
                .name(confirmName)
                .lore(confirmLore.toArray(new String[0]))
                .build();

        setItem(11, confirmItem, (p, click) -> {
            processConfirm(vipShopManager);
        });

        // Info central (resumo visual)
        ItemStack summaryItem = new ItemBuilder(Material.PAPER)
                .name("&e&lRESUMO DA COMPRA")
                .lore(
                        "",
                        "&6VIP: " + offer.getDisplayName(),
                        "&6Duração: &f7 dias",
                        "",
                        "&fPreço: &e" + ChatStorage.formatNumber(offer.getPrice()) + " coins",
                        "&fSeu saldo: &e" + ChatStorage.formatNumber(playerCoins) + " coins",
                        "&fSaldo após: &a" + ChatStorage.formatNumber(playerCoins - offer.getPrice()) + " coins",
                        "",
                        "&7Clique no botão verde para confirmar",
                        "&7ou no vermelho para cancelar.",
                        "")
                .glow()
                .build();
        setItem(13, summaryItem);

        // Botão CANCELAR (direita)
        ItemStack cancelItem = new ItemBuilder(Material.STAINED_CLAY, 1, (short) 14) // Red clay
                .name("&c&l✘ CANCELAR")
                .lore(
                        "",
                        "&7Cancelar esta compra",
                        "&7e voltar à loja de VIPs.",
                        "",
                        "&c▶ Clique para cancelar")
                .build();

        setItem(15, cancelItem, (p, click) -> {
            new ShopVipMenu(plugin, player).open();
            playClickSound();
        });

        // ============ FOOTER ============

        // Garantia
        ItemStack warrantyItem = new ItemBuilder(Material.BOOK)
                .name("&b&lGARANTIA")
                .lore(
                        "",
                        "&7Sua compra é processada",
                        "&7instantaneamente!",
                        "",
                        "&7O VIP é ativado na hora",
                        "&7e você pode usar imediatamente.",
                        "",
                        "&7Em caso de problemas,",
                        "&7contate um administrador.",
                        "")
                .build();
        setItem(22, warrantyItem);

        // Botão voltar
        setItem(18, createBackButton(), (p, click) -> {
            new ShopVipMenu(plugin, player).open();
            playClickSound();
        });

        // Botão fechar
        setClickableItem(26, createCloseButton(), this::close);
    }

    /**
     * Cria o item de informação do VIP
     */
    private ItemStack createVipInfoItem(VipShopManager manager, long playerCoins) {
        // Determinar material baseado no VIP
        Material material;
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

        // Verificar status atual
        long timeRemaining = manager != null ? manager.getVipTimeRemaining(player, offer.getGroupName()) : -1;
        boolean hasTemp = timeRemaining > 0;

        List<String> lore = new ArrayList<>();
        lore.add("");

        // Descrição compacta
        for (String line : offer.getDescription()) {
            if (!line.isEmpty() && !line.startsWith("&8▬")) {
                lore.add(line);
            }
        }

        lore.add("");
        lore.add("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add("");

        if (hasTemp) {
            lore.add("&aVocê já possui este VIP!");
            lore.add("&fTempo atual: &e" + ChatStorage.formatTime(timeRemaining));
            lore.add("&fApós compra: &a" + ChatStorage.formatTime(timeRemaining + offer.getDuration()));
        } else {
            lore.add("&7Este VIP será ativado por &e7 dias&7.");
        }

        return new ItemBuilder(material)
                .name(offer.getDisplayName() + " &f- &e7 Dias")
                .lore(lore.toArray(new String[0]))
                .glow()
                .build();
    }

    /**
     * Processa a confirmação de compra
     */
    private void processConfirm(VipShopManager manager) {
        if (manager == null) {
            ChatStorage.sendRaw(player, "&c&lHAUMEAMC &fErro ao processar sua compra!");
            playErrorSound();
            close();
            return;
        }

        // Verificar novamente se pode comprar
        VipShopManager.PurchaseResult result = manager.canPurchase(player, offer);

        if (result != VipShopManager.PurchaseResult.CAN_PURCHASE) {
            switch (result) {
                case ALREADY_PERMANENT:
                    ChatStorage.sendRaw(player, "&c&lHAUMEAMC &fVocê já possui este VIP permanentemente!");
                    break;
                case NOT_ENOUGH_COINS:
                    ChatStorage.sendRaw(player, "&c&lHAUMEAMC &fVocê não possui coins suficientes!");
                    break;
                default:
                    ChatStorage.sendRaw(player, "&c&lHAUMEAMC &fErro ao processar sua compra!");
            }
            playErrorSound();
            close();
            return;
        }

        // Processar compra
        boolean success = manager.purchaseVip(player, offer);

        if (success) {
            playLevelUpSound();
            // A mensagem já é enviada pelo VipShopManager
        } else {
            ChatStorage.sendRaw(player, "&c&lHAUMEAMC &fErro ao processar sua compra!");
            playErrorSound();
        }

        close();
    }
}
