package com.haumea.kitpvp.menu.casino;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.managers.CasinoManager;
import com.haumea.kitpvp.menu.BaseMenu;
import com.haumea.kitpvp.utils.ChatStorage;
import com.haumea.kitpvp.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Menu para criar um Coinflip.
 * Permite selecionar o valor da aposta.
 * 
 * @author HaumeaMC
 */
public class CoinflipCreateMenu extends BaseMenu {

    private final CasinoManager casinoManager;
    private long selectedBet;

    public CoinflipCreateMenu(HaumeaMC plugin, Player player) {
        super(plugin, player, "&e&lCRIAR COINFLIP", 45);
        this.casinoManager = plugin.getCasinoManager();
        this.selectedBet = 0;
    }

    @Override
    protected void setupItems() {
        // Preencher bordas
        fillBorders(4); // Amarelo

        // Slot 4: Saldo
        long coins = plugin.getStatsManager().getMoney(player);
        ItemStack balanceItem = ItemBuilder.playerHead(player.getName())
                .name("&6&lSEU SALDO")
                .lore(
                        "",
                        "&fCoins: &e" + casinoManager.formatCoins(coins),
                        "",
                        selectedBet > 0 ? "&7Valor selecionado: &a" + casinoManager.formatCoins(selectedBet)
                                : "&7Selecione um valor abaixo")
                .build();
        setItem(4, balanceItem);

        // Botões de aposta (linha 2)
        int[] betSlots = { 19, 20, 21, 23, 24, 25 };
        int index = 0;

        for (Long bet : casinoManager.getPresetBets()) {
            if (index >= betSlots.length)
                break;

            int slot = betSlots[index];
            boolean selected = selectedBet == bet;
            boolean canAfford = coins >= bet;

            ItemBuilder builder;
            if (bet >= 10000) {
                builder = new ItemBuilder(Material.EMERALD);
            } else if (bet >= 5000) {
                builder = new ItemBuilder(Material.DIAMOND);
            } else if (bet >= 1000) {
                builder = new ItemBuilder(Material.GOLD_BLOCK);
            } else if (bet >= 500) {
                builder = new ItemBuilder(Material.GOLD_INGOT);
            } else {
                builder = new ItemBuilder(Material.GOLD_NUGGET);
            }

            String status = selected ? "&a✓ SELECIONADO"
                    : (canAfford ? "&eClique para selecionar" : "&cSaldo insuficiente");

            builder.name((selected ? "&a&l" : "&e&l") + casinoManager.formatCoins(bet) + " coins")
                    .lore("", status);

            if (selected) {
                builder.glow();
            }

            final long finalBet = bet;
            setItem(slot, builder.build(), (p, c) -> {
                if (!canAfford) {
                    playErrorSound();
                    ChatStorage.send(p, "casino.error.not-enough-coins");
                    return;
                }
                selectedBet = finalBet;
                playClickSound();
                refresh();
            });

            index++;
        }

        // Botão de confirmar (slot 31)
        updateConfirmButton();

        // Slot 36: Voltar
        setItem(36, createBackButton(), (p, c) -> {
            playClickSound();
            new CoinflipMenu(plugin, player).open();
        });

        // Slot 44: Fechar
        setItem(44, createCloseButton(), (p, c) -> {
            playClickSound();
            close();
        });
    }

    /**
     * Atualiza o botão de confirmar.
     */
    private void updateConfirmButton() {
        if (selectedBet > 0) {
            ItemStack confirmButton = new ItemBuilder(Material.SLIME_BALL)
                    .name("&a&l✓ CRIAR COINFLIP")
                    .lore(
                            "",
                            "&7Valor: &e" + casinoManager.formatCoins(selectedBet),
                            "",
                            "&aClique para criar!")
                    .glow()
                    .build();
            setItem(31, confirmButton, (p, c) -> createCoinflip());
        } else {
            ItemStack disabledButton = new ItemBuilder(Material.STAINED_GLASS_PANE, 1, (short) 7)
                    .name("&7&lCRIAR COINFLIP")
                    .lore("&cSelecione um valor primeiro")
                    .build();
            setItem(31, disabledButton);
        }
    }

    /**
     * Cria o coinflip.
     */
    private void createCoinflip() {
        if (selectedBet <= 0)
            return;

        if (casinoManager.createCoinflip(player, selectedBet) != null) {
            playSuccessSound();
            new CoinflipMenu(plugin, player).open();
        } else {
            playErrorSound();
        }
    }
}
