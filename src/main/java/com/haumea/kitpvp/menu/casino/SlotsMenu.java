package com.haumea.kitpvp.menu.casino;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.managers.CasinoManager;
import com.haumea.kitpvp.menu.BaseMenu;
import com.haumea.kitpvp.models.casino.SlotsResult;
import com.haumea.kitpvp.utils.ChatStorage;
import com.haumea.kitpvp.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Menu de Slots (Caça-níqueis).
 * O jogador seleciona um valor e gira os slots.
 * 
 * @author HaumeaMC
 */
public class SlotsMenu extends BaseMenu {

    private final CasinoManager casinoManager;
    private long selectedBet;
    private boolean spinning;

    // Slots de display dos símbolos
    private static final int[] SLOT_POSITIONS = { 20, 21, 22 };

    // Slot do botão de girar
    private static final int SPIN_SLOT = 31;

    public SlotsMenu(HaumeaMC plugin, Player player) {
        super(plugin, player, "&6&l🎰 CAÇA-NÍQUEIS", 54);
        this.casinoManager = plugin.getCasinoManager();
        this.selectedBet = 0;
        this.spinning = false;
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
                        selectedBet > 0 ? "&7Aposta: &a" + casinoManager.formatCoins(selectedBet)
                                : "&7Selecione um valor abaixo")
                .build();
        setItem(4, balanceItem);

        // Slots de exibição (inicialmente com interrogação)
        for (int pos : SLOT_POSITIONS) {
            ItemStack mystery = new ItemBuilder(Material.COAL_BLOCK)
                    .name("&7&l?")
                    .lore("&7Gire para revelar!")
                    .build();
            setItem(pos, mystery);
        }

        // Linha de apostas (linha 3, slots 28-35)
        setupBetButtons();

        // Botão de girar (aparece após selecionar aposta)
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

        // Info
        ItemStack infoItem = new ItemBuilder(Material.PAPER)
                .name("&e&lCOMO JOGAR")
                .lore(
                        "",
                        "&71. Selecione o valor da aposta",
                        "&72. Clique em GIRAR",
                        "&73. Torça pelos símbolos!",
                        "",
                        "&8Prêmios:",
                        "&8▸ &f2 iguais: &a1.5x",
                        "&8▸ &f3 comuns: &a3x",
                        "&8▸ &f3 raros: &a10x",
                        "&8▸ &6JACKPOT (777): &a25x")
                .build();
        setItem(13, infoItem);
    }

    /**
     * Configura os botões de aposta.
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
                    ChatStorage.sendRaw(p, "&c&lCASSINO &fVocê não tem coins suficientes!");
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
        if (selectedBet > 0 && !spinning) {
            ItemStack spinButton = new ItemBuilder(Material.SLIME_BALL)
                    .name("&a&lGIRAR!")
                    .lore(
                            "",
                            "&7Apostar: &e" + casinoManager.formatCoins(selectedBet),
                            "",
                            "&aClique para girar!")
                    .glow()
                    .build();
            setItem(SPIN_SLOT, spinButton, (p, c) -> spin());
        } else if (spinning) {
            ItemStack spinningItem = new ItemBuilder(Material.FIREWORK)
                    .name("&e&lGIRANDO...")
                    .lore("&7Aguarde o resultado!")
                    .build();
            setItem(SPIN_SLOT, spinningItem);
        } else {
            ItemStack disabledButton = new ItemBuilder(Material.STAINED_GLASS_PANE, 1, (short) 7)
                    .name("&7&lGIRAR")
                    .lore("&cSelecione um valor primeiro")
                    .build();
            setItem(SPIN_SLOT, disabledButton);
        }
    }

    /**
     * Executa a animação e lógica de girar.
     */
    private void spin() {
        if (spinning || selectedBet <= 0)
            return;

        if (!casinoManager.validateBet(player, selectedBet)) {
            playErrorSound();
            return;
        }

        spinning = true;
        updateSpinButton();

        // Jogar
        SlotsResult result = casinoManager.playSlots(player, selectedBet);

        // Animação
        new BukkitRunnable() {
            int ticks = 0;
            final int animationDuration = 40; // 2 segundos
            final SlotsResult.SlotSymbol[] symbols = SlotsResult.SlotSymbol.values();

            @Override
            public void run() {
                if (ticks >= animationDuration) {
                    // Mostrar resultado final
                    showResult(result);
                    this.cancel();
                    return;
                }

                // Trocar símbolos aleatoriamente
                for (int i = 0; i < SLOT_POSITIONS.length; i++) {
                    // No final, mostrar gradualmente os resultados
                    if (ticks > animationDuration - 15 && i == 0) {
                        showSymbol(SLOT_POSITIONS[i], result.getSymbols()[i]);
                    } else if (ticks > animationDuration - 10 && i == 1) {
                        showSymbol(SLOT_POSITIONS[i], result.getSymbols()[i]);
                    } else if (ticks > animationDuration - 5 && i == 2) {
                        showSymbol(SLOT_POSITIONS[i], result.getSymbols()[i]);
                    } else {
                        // Símbolo aleatório
                        SlotsResult.SlotSymbol randomSymbol = symbols[(int) (Math.random() * symbols.length)];
                        showSymbol(SLOT_POSITIONS[i], randomSymbol);
                    }
                }

                // Som de tick
                if (ticks % 3 == 0) {
                    player.playSound(player.getLocation(), Sound.NOTE_STICKS, 0.5f, 1.0f);
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    /**
     * Mostra um símbolo em um slot.
     */
    private void showSymbol(int slot, SlotsResult.SlotSymbol symbol) {
        short data = 0;
        if (symbol.getMaterial() == Material.INK_SACK) {
            data = 5; // Purple dye
        }

        ItemStack item = new ItemBuilder(symbol.getMaterial(), 1, data)
                .name(symbol.getEmoji() + " &f" + symbol.getName())
                .build();
        inventory.setItem(slot, item);
    }

    /**
     * Mostra o resultado final.
     */
    private void showResult(SlotsResult result) {
        spinning = false;

        // Mostrar símbolos finais
        for (int i = 0; i < SLOT_POSITIONS.length; i++) {
            showSymbol(SLOT_POSITIONS[i], result.getSymbols()[i]);
        }

        // Efeitos baseados no resultado
        if (result.isJackpot()) {
            player.playSound(player.getLocation(), Sound.LEVEL_UP, 1f, 0.5f);
            player.sendTitle(
                    ChatStorage.colorize("&6&l⭐ JACKPOT! ⭐"),
                    ChatStorage.colorize("&a+" + casinoManager.formatCoins(result.getPayout()) + " coins"));

            // Mensagem no chat
            ChatStorage.sendRaw(player, "&6&l⭐ JACKPOT! ⭐ &fVocê ganhou &e&l"
                    + casinoManager.formatCoins(result.getPayout()) + " coins&f!");

            // Broadcast para todos os jogadores
            casinoManager.broadcastJackpotPublic(player, result.getPayout());

        } else if (result.isWin()) {
            player.playSound(player.getLocation(), Sound.ORB_PICKUP, 1f, 1.2f);
            player.sendTitle(
                    ChatStorage.colorize("&a&lVITÓRIA!"),
                    ChatStorage.colorize("&a+" + casinoManager.formatCoins(result.getPayout()) + " coins &7("
                            + result.getMultiplier() + "x)"));

            // Mensagem no chat
            ChatStorage.sendRaw(player, "&a&lVITÓRIA! &fVocê ganhou &e" + casinoManager.formatCoins(result.getPayout())
                    + " coins &7(" + result.getMultiplier() + "x)");

        } else {
            player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1f, 1f);
            player.sendTitle(
                    ChatStorage.colorize("&c&lDerrota"),
                    ChatStorage.colorize("&7Tente novamente!"));

            // Mensagem no chat
            ChatStorage.sendRaw(player,
                    "&c&lDerrota! &fVocê perdeu &e" + casinoManager.formatCoins(selectedBet) + " coins");
        }

        // Atualizar menu após um pequeno delay
        final long betToShow = selectedBet;
        new BukkitRunnable() {
            @Override
            public void run() {
                selectedBet = 0;
                refresh();
            }
        }.runTaskLater(plugin, 40L); // 2 segundos
    }
}
