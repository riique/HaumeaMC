package com.haumea.kitpvp.menu.casino;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.managers.CasinoManager;
import com.haumea.kitpvp.menu.BaseMenu;
import com.haumea.kitpvp.models.casino.RouletteBet;
import com.haumea.kitpvp.utils.ChatStorage;
import com.haumea.kitpvp.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Random;

/**
 * Menu de Roleta.
 * O jogador seleciona tipo de aposta e valor.
 * 
 * @author HaumeaMC
 */
public class RouletteMenu extends BaseMenu {

    private final CasinoManager casinoManager;
    private long selectedBet;
    private RouletteBet selectedBetType;
    private int selectedNumber; // Para aposta em número específico
    private boolean spinning;

    private static final Random RANDOM = new Random();

    public RouletteMenu(HaumeaMC plugin, Player player) {
        super(plugin, player, "&c&l🎡 ROLETA", 54);
        this.casinoManager = plugin.getCasinoManager();
        this.selectedBet = 0;
        this.selectedBetType = null;
        this.selectedNumber = -1;
        this.spinning = false;
    }

    @Override
    protected void setupItems() {
        // Preencher bordas
        fillBorders(14); // Vermelho

        // Slot 4: Saldo e aposta atual
        long coins = plugin.getStatsManager().getMoney(player);
        ItemStack balanceItem = ItemBuilder.playerHead(player.getName())
                .name("&6&lSUA APOSTA")
                .lore(
                        "",
                        "&fSaldo: &e" + casinoManager.formatCoins(coins),
                        "",
                        selectedBet > 0 ? "&7Valor: &a" + casinoManager.formatCoins(selectedBet)
                                : "&7Valor: &cNão selecionado",
                        selectedBetType != null ? "&7Tipo: &e" + selectedBetType.getDisplayName()
                                : "&7Tipo: &cNão selecionado")
                .build();
        setItem(4, balanceItem);

        // Linha 1: Preview da roleta com números coloridos
        int[] previewSlots = { 10, 11, 12, 13, 14, 15, 16 };
        int[] previewNumbers = { 1, 2, 3, 0, 4, 5, 6 };
        for (int i = 0; i < previewSlots.length; i++) {
            int number = previewNumbers[i];
            short color;
            if (number == 0) {
                color = 5; // Verde
            } else if (RouletteBet.isRed(number)) {
                color = 14; // Vermelho
            } else {
                color = 15; // Preto
            }

            ItemStack numberItem = new ItemBuilder(Material.WOOL, 1, color)
                    .name(RouletteBet.getColoredNumber(number))
                    .build();
            setItem(previewSlots[i], numberItem);
        }

        // Linha 2-3: Tipos de aposta

        // Vermelho (slot 19)
        ItemStack redItem = new ItemBuilder(Material.WOOL, 1, (short) 14)
                .name("&c&lVERMELHO")
                .lore(
                        "",
                        "&7Multiplica: &a2x",
                        "",
                        selectedBetType == RouletteBet.RED ? "&a✓ SELECIONADO" : "&eClique para selecionar")
                .build();
        if (selectedBetType == RouletteBet.RED)
            redItem = new ItemBuilder(redItem).glow().build();
        setItem(19, redItem, (p, c) -> selectBetType(RouletteBet.RED));

        // Preto (slot 20)
        ItemStack blackItem = new ItemBuilder(Material.WOOL, 1, (short) 15)
                .name("&8&lPRETO")
                .lore(
                        "",
                        "&7Multiplica: &a2x",
                        "",
                        selectedBetType == RouletteBet.BLACK ? "&a✓ SELECIONADO" : "&eClique para selecionar")
                .build();
        if (selectedBetType == RouletteBet.BLACK)
            blackItem = new ItemBuilder(blackItem).glow().build();
        setItem(20, blackItem, (p, c) -> selectBetType(RouletteBet.BLACK));

        // Par (slot 22)
        ItemStack evenItem = new ItemBuilder(Material.IRON_BLOCK)
                .name("&7&lPAR")
                .lore(
                        "",
                        "&7Multiplica: &a2x",
                        "&72, 4, 6, 8...",
                        "",
                        selectedBetType == RouletteBet.EVEN ? "&a✓ SELECIONADO" : "&eClique para selecionar")
                .build();
        if (selectedBetType == RouletteBet.EVEN)
            evenItem = new ItemBuilder(evenItem).glow().build();
        setItem(22, evenItem, (p, c) -> selectBetType(RouletteBet.EVEN));

        // Ímpar (slot 23)
        ItemStack oddItem = new ItemBuilder(Material.GOLD_BLOCK)
                .name("&6&lÍMPAR")
                .lore(
                        "",
                        "&7Multiplica: &a2x",
                        "&71, 3, 5, 7...",
                        "",
                        selectedBetType == RouletteBet.ODD ? "&a✓ SELECIONADO" : "&eClique para selecionar")
                .build();
        if (selectedBetType == RouletteBet.ODD)
            oddItem = new ItemBuilder(oddItem).glow().build();
        setItem(23, oddItem, (p, c) -> selectBetType(RouletteBet.ODD));

        // 1-18 (slot 24)
        ItemStack lowItem = new ItemBuilder(Material.STONE)
                .name("&7&l1-18")
                .lore(
                        "",
                        "&7Multiplica: &a2x",
                        "&7Números baixos",
                        "",
                        selectedBetType == RouletteBet.LOW ? "&a✓ SELECIONADO" : "&eClique para selecionar")
                .build();
        if (selectedBetType == RouletteBet.LOW)
            lowItem = new ItemBuilder(lowItem).glow().build();
        setItem(24, lowItem, (p, c) -> selectBetType(RouletteBet.LOW));

        // 19-36 (slot 25)
        ItemStack highItem = new ItemBuilder(Material.DIAMOND_BLOCK)
                .name("&b&l19-36")
                .lore(
                        "",
                        "&7Multiplica: &a2x",
                        "&7Números altos",
                        "",
                        selectedBetType == RouletteBet.HIGH ? "&a✓ SELECIONADO" : "&eClique para selecionar")
                .build();
        if (selectedBetType == RouletteBet.HIGH)
            highItem = new ItemBuilder(highItem).glow().build();
        setItem(25, highItem, (p, c) -> selectBetType(RouletteBet.HIGH));

        // Número específico (slot 21)
        String numberLore = selectedBetType == RouletteBet.NUMBER
                ? "&a✓ Número: " + RouletteBet.getColoredNumber(selectedNumber)
                : "&eClique para escolher";
        ItemStack numberItem = new ItemBuilder(Material.NETHER_STAR)
                .name("&6&lNÚMERO ESPECÍFICO")
                .lore(
                        "",
                        "&7Multiplica: &a36x",
                        "&7Escolha um número!",
                        "",
                        numberLore)
                .build();
        if (selectedBetType == RouletteBet.NUMBER)
            numberItem = new ItemBuilder(numberItem).glow().build();
        setItem(21, numberItem, (p, c) -> {
            playClickSound();
            new RouletteNumbersMenu(plugin, player, this).open();
        });

        // Linha 3: Valores de aposta
        setupBetButtons();

        // Botão de girar
        updateSpinButton();

        // Slot 45: Voltar
        setItem(45, createBackButton(), (p, c) -> {
            playClickSound();
            new CasinoMainMenu(plugin, player).open();
        });

        // Slot 53: Fechar
        setItem(53, createCloseButton(), (p, c) -> {
            playClickSound();
            close();
        });
    }

    /**
     * Seleciona o tipo de aposta.
     */
    private void selectBetType(RouletteBet betType) {
        if (spinning)
            return;
        selectedBetType = betType;
        selectedNumber = -1;
        playClickSound();
        refresh();
    }

    /**
     * Define o número selecionado (chamado pelo RouletteNumbersMenu).
     */
    public void setSelectedNumber(int number) {
        this.selectedNumber = number;
        this.selectedBetType = RouletteBet.NUMBER;
    }

    /**
     * Configura os botões de valor.
     */
    private void setupBetButtons() {
        int[] betSlots = { 28, 29, 30, 32, 33, 34 };
        int index = 0;

        for (Long bet : casinoManager.getPresetBets()) {
            if (index >= betSlots.length)
                break;

            int slot = betSlots[index];
            boolean selected = selectedBet == bet;
            boolean canAfford = plugin.getStatsManager().getMoney(player) >= bet;

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
                if (spinning)
                    return;
                if (!canAfford) {
                    playErrorSound();
                    return;
                }
                selectedBet = finalBet;
                playClickSound();
                refresh();
            });

            index++;
        }
    }

    /**
     * Atualiza o botão de girar.
     */
    private void updateSpinButton() {
        boolean canSpin = selectedBet > 0 && selectedBetType != null && !spinning;

        if (selectedBetType == RouletteBet.NUMBER && selectedNumber < 0) {
            canSpin = false;
        }

        if (canSpin) {
            ItemStack spinButton = new ItemBuilder(Material.SLIME_BALL)
                    .name("&a&lGIRAR ROLETA!")
                    .lore(
                            "",
                            "&7Aposta: &e" + casinoManager.formatCoins(selectedBet),
                            "&7Tipo: &f" + selectedBetType.getDisplayName(),
                            selectedBetType == RouletteBet.NUMBER
                                    ? "&7Número: " + RouletteBet.getColoredNumber(selectedNumber)
                                    : "",
                            "",
                            "&aClique para girar!")
                    .glow()
                    .build();
            setItem(40, spinButton, (p, c) -> spin());
        } else if (spinning) {
            ItemStack spinningItem = new ItemBuilder(Material.FIREWORK)
                    .name("&e&lGIRANDO...")
                    .lore("&7Aguarde o resultado!")
                    .build();
            setItem(40, spinningItem);
        } else {
            ItemStack disabledButton = new ItemBuilder(Material.STAINED_GLASS_PANE, 1, (short) 7)
                    .name("&7&lGIRAR ROLETA")
                    .lore(
                            "&cPrimeiro:",
                            selectedBetType == null ? "&c• Selecione um tipo de aposta" : "&a✓ Tipo selecionado",
                            selectedBet <= 0 ? "&c• Selecione um valor" : "&a✓ Valor selecionado")
                    .build();
            setItem(40, disabledButton);
        }
    }

    /**
     * Executa a roleta.
     */
    private void spin() {
        if (spinning || selectedBet <= 0 || selectedBetType == null)
            return;

        if (!casinoManager.validateBet(player, selectedBet)) {
            playErrorSound();
            return;
        }

        spinning = true;
        refresh();

        // Jogar
        int result = casinoManager.playRoulette(player, selectedBet, selectedBetType, selectedNumber);
        boolean won = selectedBetType.isWinner(result, selectedNumber);
        long payout = won ? (long) (selectedBet * selectedBetType.getMultiplier()) : 0;

        // Animação
        new BukkitRunnable() {
            int ticks = 0;
            final int animationDuration = 60; // 3 segundos

            @Override
            public void run() {
                if (ticks >= animationDuration) {
                    // Mostrar resultado
                    showResult(result, won, payout);
                    this.cancel();
                    return;
                }

                // Mostrar números aleatórios nos slots de preview
                int[] previewSlots = { 10, 11, 12, 13, 14, 15, 16 };
                for (int slot : previewSlots) {
                    int randomNumber = RANDOM.nextInt(37);
                    short color;
                    if (randomNumber == 0) {
                        color = 5; // Verde
                    } else if (RouletteBet.isRed(randomNumber)) {
                        color = 14; // Vermelho
                    } else {
                        color = 15; // Preto
                    }

                    ItemStack item = new ItemBuilder(Material.WOOL, 1, color)
                            .name(RouletteBet.getColoredNumber(randomNumber))
                            .build();
                    inventory.setItem(slot, item);
                }

                // Som
                if (ticks % 3 == 0) {
                    player.playSound(player.getLocation(), Sound.NOTE_STICKS, 0.5f, 1.0f + (ticks / 60.0f));
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    /**
     * Mostra o resultado.
     */
    private void showResult(int result, boolean won, long payout) {
        spinning = false;

        // Mostrar resultado no centro
        short color;
        if (result == 0) {
            color = 5; // Verde
        } else if (RouletteBet.isRed(result)) {
            color = 14; // Vermelho
        } else {
            color = 15; // Preto
        }

        // Preencher todos os preview slots com o resultado
        int[] previewSlots = { 10, 11, 12, 13, 14, 15, 16 };
        for (int slot : previewSlots) {
            ItemStack resultItem = new ItemBuilder(Material.WOOL, 1, color)
                    .name(RouletteBet.getColoredNumber(result))
                    .lore("", "&7Resultado final!")
                    .glow()
                    .build();
            inventory.setItem(slot, resultItem);
        }

        // Título e som
        if (won) {
            player.playSound(player.getLocation(), Sound.LEVEL_UP, 1f, 1f);
            player.sendTitle(
                    ChatStorage.colorize("&a&lVITÓRIA!"),
                    ChatStorage.colorize("&fResultado: " + RouletteBet.getColoredNumber(result) + " &a+"
                            + casinoManager.formatCoins(payout) + " coins"));
        } else {
            player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1f, 1f);
            player.sendTitle(
                    ChatStorage.colorize("&c&lDerrota"),
                    ChatStorage.colorize("&fResultado: " + RouletteBet.getColoredNumber(result)));
        }

        // Reset após delay
        new BukkitRunnable() {
            @Override
            public void run() {
                selectedBet = 0;
                selectedBetType = null;
                selectedNumber = -1;
                refresh();
            }
        }.runTaskLater(plugin, 60L); // 3 segundos
    }
}
