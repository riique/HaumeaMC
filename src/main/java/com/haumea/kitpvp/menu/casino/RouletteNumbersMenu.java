package com.haumea.kitpvp.menu.casino;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.menu.BaseMenu;
import com.haumea.kitpvp.models.casino.RouletteBet;
import com.haumea.kitpvp.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Menu de seleção de número específico para Roleta.
 * Grid com todos os números de 0 a 36.
 * 
 * @author HaumeaMC
 */
public class RouletteNumbersMenu extends BaseMenu {

    private final RouletteMenu parentMenu;

    public RouletteNumbersMenu(HaumeaMC plugin, Player player, RouletteMenu parentMenu) {
        super(plugin, player, "&6&lESCOLHA UM NÚMERO", 54);
        this.parentMenu = parentMenu;
    }

    @Override
    protected void setupItems() {
        // Preencher bordas
        fillBorders(4);

        // Zero no centro da primeira linha
        ItemStack zeroItem = new ItemBuilder(Material.WOOL, 1, (short) 5) // Verde
                .name("&a&l0")
                .lore(
                        "",
                        "&7Multiplicador: &a36x",
                        "&eClique para selecionar")
                .build();
        setItem(4, zeroItem, (p, c) -> selectNumber(0));

        // Números 1-36 em grid
        // Layout: 4 linhas de 9 números cada (linhas 1-4)
        int[] numberSlots = {
                // Linha 1 (slots 10-16)
                10, 11, 12, 13, 14, 15, 16,
                // Linha 2 (slots 19-25)
                19, 20, 21, 22, 23, 24, 25,
                // Linha 3 (slots 28-34)
                28, 29, 30, 31, 32, 33, 34,
                // Linha 4 (slots 37-43)
                37, 38, 39, 40, 41, 42, 43
        };

        for (int i = 0; i < Math.min(36, numberSlots.length); i++) {
            int number = i + 1;
            int slot = numberSlots[i];

            short color;
            if (RouletteBet.isRed(number)) {
                color = 14; // Vermelho
            } else {
                color = 15; // Preto
            }

            ItemStack numberItem = new ItemBuilder(Material.WOOL, 1, color)
                    .name(RouletteBet.getColoredNumber(number))
                    .lore(
                            "",
                            "&7Multiplicador: &a36x",
                            "&eClique para selecionar")
                    .build();

            final int finalNumber = number;
            setItem(slot, numberItem, (p, c) -> selectNumber(finalNumber));
        }

        // Slot 45: Voltar
        setItem(45, createBackButton(), (p, c) -> {
            playClickSound();
            parentMenu.open();
        });
    }

    /**
     * Seleciona um número e volta ao menu da roleta.
     */
    private void selectNumber(int number) {
        playSuccessSound();
        parentMenu.setSelectedNumber(number);
        parentMenu.open();
    }
}
