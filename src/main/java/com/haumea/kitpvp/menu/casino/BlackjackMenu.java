package com.haumea.kitpvp.menu.casino;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.managers.CasinoManager;
import com.haumea.kitpvp.menu.BaseMenu;
import com.haumea.kitpvp.models.casino.BlackjackGame;
import com.haumea.kitpvp.models.casino.BlackjackHand;
import com.haumea.kitpvp.models.casino.Card;
import com.haumea.kitpvp.utils.ChatStorage;
import com.haumea.kitpvp.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Menu de Blackjack.
 * Inicia no estado de seleção de aposta.
 * Após apostar, mostra o jogo com cartas.
 * 
 * @author HaumeaMC
 */
public class BlackjackMenu extends BaseMenu {

    private final CasinoManager casinoManager;
    private long selectedBet;
    private BlackjackGame currentGame;
    private boolean animating;

    // Slots das cartas
    private static final int[] DEALER_CARD_SLOTS = { 11, 12, 13, 14, 15 };
    private static final int[] PLAYER_CARD_SLOTS = { 29, 30, 31, 32, 33 };

    public BlackjackMenu(HaumeaMC plugin, Player player) {
        super(plugin, player, "&f&l🃏 BLACKJACK - 21", 54);
        this.casinoManager = plugin.getCasinoManager();
        this.selectedBet = 0;
        this.currentGame = null;
        this.animating = false;
    }

    @Override
    protected void setupItems() {
        // Verificar se já tem um jogo em andamento
        if (casinoManager.hasActiveSession(player)) {
            com.haumea.kitpvp.models.casino.CasinoSession session = casinoManager.getActiveSession(player);
            if (session != null && session.getGame() == com.haumea.kitpvp.models.casino.CasinoGame.BLACKJACK) {
                currentGame = session.getGameState(BlackjackGame.class);
            }
        }

        if (currentGame == null || currentGame.isFinished()) {
            setupBettingState();
        } else {
            setupGameState();
        }
    }

    /**
     * Estado de seleção de aposta.
     */
    private void setupBettingState() {
        // Limpar inventário
        inventory.clear();

        // Preencher bordas com verde
        fillBorders(5); // Verde escuro

        // Slot 4: Saldo
        long coins = plugin.getStatsManager().getMoney(player);
        ItemStack balanceItem = ItemBuilder.playerHead(player.getName())
                .name("&6&lSEU SALDO")
                .lore(
                        "",
                        "&fCoins: &e" + casinoManager.formatCoins(coins),
                        "",
                        selectedBet > 0 ? "&7Aposta: &a" + casinoManager.formatCoins(selectedBet)
                                : "&7Selecione um valor")
                .build();
        setItem(4, balanceItem);

        // Logo Blackjack
        ItemStack logoItem = new ItemBuilder(Material.PAPER)
                .name("&f&l🃏 BLACKJACK 🃏")
                .lore(
                        "",
                        "&7Chegue o mais perto de 21",
                        "&7sem ultrapassar!",
                        "",
                        "&8Regras:",
                        "&8• &fCartas 2-10: valor facial",
                        "&8• &fJ, Q, K: 10 pontos",
                        "&8• &fÁs: 1 ou 11 pontos",
                        "&8• &fBlackjack (21 c/ 2): 2.5x")
                .glow()
                .build();
        setItem(13, logoItem);

        // Valores de aposta
        int[] betSlots = { 28, 29, 30, 32, 33, 34 };
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
            } else {
                builder = new ItemBuilder(Material.GOLD_INGOT);
            }

            builder.name((selected ? "&a&l" : "&e&l") + casinoManager.formatCoins(bet) + " coins")
                    .lore("", selected ? "&a✓ SELECIONADO"
                            : (canAfford ? "&eClique para selecionar" : "&cSaldo insuficiente"));

            if (selected)
                builder.glow();

            final long finalBet = bet;
            setItem(slot, builder.build(), (p, c) -> {
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

        // Botão de começar
        if (selectedBet > 0) {
            ItemStack startButton = new ItemBuilder(Material.SLIME_BALL)
                    .name("&a&lCOMEÇAR JOGO")
                    .lore(
                            "",
                            "&7Aposta: &e" + casinoManager.formatCoins(selectedBet),
                            "",
                            "&aClique para jogar!")
                    .glow()
                    .build();
            setItem(40, startButton, (p, c) -> startGame());
        } else {
            ItemStack disabledButton = new ItemBuilder(Material.STAINED_GLASS_PANE, 1, (short) 7)
                    .name("&7&lCOMEÇAR JOGO")
                    .lore("&cSelecione um valor primeiro")
                    .build();
            setItem(40, disabledButton);
        }

        // Voltar
        setItem(45, createBackButton(), (p, c) -> {
            playClickSound();
            new CasinoMainMenu(plugin, player).open();
        });

        // Fechar
        setItem(53, createCloseButton(), (p, c) -> {
            playClickSound();
            close();
        });
    }

    /**
     * Estado de jogo em andamento.
     */
    private void setupGameState() {
        // Limpar inventário
        inventory.clear();

        // Preencher bordas
        fillBorders(13); // Verde

        // Título Dealer
        ItemStack dealerTitle = new ItemBuilder(Material.SKULL_ITEM, 1, (short) 1) // Wither skull
                .name("&c&lDEALER")
                .lore(
                        "",
                        "&7Pontos: " + (currentGame.getState() == BlackjackGame.GameState.PLAYER_TURN ? "&f?"
                                : "&f" + currentGame.getDealerHand().getValue()))
                .build();
        setItem(4, dealerTitle);

        // Cartas do dealer
        showDealerCards();

        // Separador
        for (int i = 18; i <= 26; i++) {
            if (i == 18 || i == 26)
                continue; // Bordas já preenchidas
            ItemStack separator = new ItemBuilder(Material.STAINED_GLASS_PANE, 1, (short) 8)
                    .name(" ")
                    .build();
            setItem(i, separator);
        }

        // Título jogador
        ItemStack playerTitle = ItemBuilder.playerHead(player.getName())
                .name("&a&lSUA MÃO")
                .lore(
                        "",
                        "&7Pontos: &f" + currentGame.getPlayerHand().getValue(),
                        "&7Aposta: &e" + casinoManager.formatCoins(currentGame.getActualBet()))
                .build();
        setItem(22, playerTitle);

        // Cartas do jogador
        showPlayerCards();

        // Botões de ação (se for turno do jogador)
        if (currentGame.getState() == BlackjackGame.GameState.PLAYER_TURN && !animating) {
            setupActionButtons();
        } else if (currentGame.isFinished()) {
            showResultButtons();
        }

        // Status
        ItemStack statusItem = new ItemBuilder(Material.PAPER)
                .name("&e&l" + currentGame.getState().getDisplay())
                .lore(
                        "",
                        "&7Aposta: &e" + casinoManager.formatCoins(currentGame.getActualBet()),
                        currentGame.isDoubled() ? "&6Aposta dobrada!" : "")
                .build();
        setItem(40, statusItem);
    }

    /**
     * Mostra as cartas do dealer.
     */
    private void showDealerCards() {
        BlackjackHand hand = currentGame.getDealerHand();
        boolean hideFirst = currentGame.getState() == BlackjackGame.GameState.PLAYER_TURN;

        for (int i = 0; i < DEALER_CARD_SLOTS.length; i++) {
            if (i < hand.getCardCount()) {
                Card card = hand.getCards().get(i);

                if (hideFirst && i == 0) {
                    // Carta virada
                    ItemStack hiddenCard = new ItemBuilder(Material.OBSIDIAN)
                            .name("&7&l?")
                            .lore("&7Carta oculta")
                            .build();
                    setItem(DEALER_CARD_SLOTS[i], hiddenCard);
                } else {
                    setItem(DEALER_CARD_SLOTS[i], createCardItem(card));
                }
            }
        }
    }

    /**
     * Mostra as cartas do jogador.
     */
    private void showPlayerCards() {
        BlackjackHand hand = currentGame.getPlayerHand();

        for (int i = 0; i < PLAYER_CARD_SLOTS.length; i++) {
            if (i < hand.getCardCount()) {
                Card card = hand.getCards().get(i);
                setItem(PLAYER_CARD_SLOTS[i], createCardItem(card));
            }
        }
    }

    /**
     * Cria o item de uma carta.
     */
    private ItemStack createCardItem(Card card) {
        return new ItemBuilder(Material.PAPER)
                .name(card.getDisplay())
                .lore(
                        "",
                        "&7Valor: &f" + card.getValue())
                .build();
    }

    /**
     * Configura os botões de ação.
     */
    private void setupActionButtons() {
        // HIT (slot 46)
        ItemStack hitButton = new ItemBuilder(Material.SLIME_BALL)
                .name("&a&lHIT")
                .lore(
                        "",
                        "&7Pedir mais uma carta",
                        "",
                        "&eClique para pedir")
                .build();
        setItem(46, hitButton, (p, c) -> handleHit());

        // STAND (slot 48)
        ItemStack standButton = new ItemBuilder(Material.REDSTONE_BLOCK)
                .name("&e&lSTAND")
                .lore(
                        "",
                        "&7Parar de pedir cartas",
                        "",
                        "&eClique para parar")
                .build();
        setItem(48, standButton, (p, c) -> handleStand());

        // DOUBLE (slot 50) - só se pode dobrar
        if (currentGame.getPlayerHand().canDouble()) {
            long additionalBet = currentGame.getBet();
            boolean canAfford = plugin.getStatsManager().getMoney(player) >= additionalBet;

            ItemStack doubleButton = new ItemBuilder(Material.GOLD_BLOCK)
                    .name("&6&lDOUBLE")
                    .lore(
                            "",
                            "&7Dobrar aposta e receber",
                            "&7exatamente 1 carta",
                            "",
                            "&7Custo adicional: &e" + casinoManager.formatCoins(additionalBet),
                            "",
                            canAfford ? "&eClique para dobrar" : "&cSaldo insuficiente")
                    .build();
            setItem(50, doubleButton, (p, c) -> {
                if (canAfford)
                    handleDouble();
                else
                    playErrorSound();
            });
        }

        // Sair (perde aposta)
        ItemStack quitButton = new ItemBuilder(Material.BARRIER)
                .name("&c&lDESISTIR")
                .lore(
                        "",
                        "&cVocê perderá sua aposta!",
                        "",
                        "&7Clique para sair")
                .build();
        setItem(53, quitButton, (p, c) -> {
            casinoManager.cleanupSession(player);
            currentGame = null;
            selectedBet = 0;
            playErrorSound();
            refresh();
        });
    }

    /**
     * Mostra os botões de resultado.
     */
    private void showResultButtons() {
        // Novo jogo
        ItemStack newGameButton = new ItemBuilder(Material.EMERALD)
                .name("&a&lNOVO JOGO")
                .lore(
                        "",
                        "&eClique para jogar novamente")
                .build();
        setItem(48, newGameButton, (p, c) -> {
            currentGame = null;
            selectedBet = 0;
            playClickSound();
            refresh();
        });

        // Voltar ao menu
        setItem(50, createBackButton(), (p, c) -> {
            playClickSound();
            new CasinoMainMenu(plugin, player).open();
        });
    }

    /**
     * Inicia o jogo.
     */
    private void startGame() {
        if (selectedBet <= 0)
            return;

        if (!casinoManager.validateBet(player, selectedBet)) {
            playErrorSound();
            return;
        }

        currentGame = casinoManager.startBlackjack(player, selectedBet);
        player.playSound(player.getLocation(), Sound.NOTE_PLING, 1f, 1f);

        // Verificar resultado instantâneo (blackjack ou dealer blackjack)
        if (currentGame.isFinished()) {
            showGameResult();
        }

        refresh();
    }

    /**
     * Jogador pede carta.
     */
    private void handleHit() {
        if (animating)
            return;
        animating = true;

        Card card = casinoManager.blackjackHit(player);
        if (card == null) {
            animating = false;
            return;
        }

        player.playSound(player.getLocation(), Sound.CLICK, 1f, 1f);

        // Delay para revelar
        new BukkitRunnable() {
            @Override
            public void run() {
                animating = false;

                if (currentGame.isFinished()) {
                    showGameResult();
                }

                refresh();
            }
        }.runTaskLater(plugin, 10L);
    }

    /**
     * Jogador para.
     */
    private void handleStand() {
        if (animating)
            return;
        animating = true;

        casinoManager.blackjackStand(player);

        // Animação do dealer
        new BukkitRunnable() {
            @Override
            public void run() {
                animating = false;
                showGameResult();
                refresh();
            }
        }.runTaskLater(plugin, 30L);
    }

    /**
     * Jogador dobra.
     */
    private void handleDouble() {
        if (animating)
            return;
        animating = true;

        Card card = casinoManager.blackjackDouble(player);
        if (card == null) {
            animating = false;
            playErrorSound();
            return;
        }

        player.playSound(player.getLocation(), Sound.CLICK, 1f, 0.8f);

        // Delay para revelar
        new BukkitRunnable() {
            @Override
            public void run() {
                animating = false;
                showGameResult();
                refresh();
            }
        }.runTaskLater(plugin, 30L);
    }

    /**
     * Mostra o resultado do jogo.
     */
    private void showGameResult() {
        if (currentGame == null)
            return;

        long payout = currentGame.calculatePayout();
        long bet = currentGame.getActualBet();

        switch (currentGame.getState()) {
            case PLAYER_BLACKJACK:
                player.playSound(player.getLocation(), Sound.LEVEL_UP, 1f, 1f);
                player.sendTitle(
                        ChatStorage.colorize("&6&l★ BLACKJACK! ★"),
                        ChatStorage.colorize("&a+" + casinoManager.formatCoins(payout) + " coins &7(2.5x)"));
                ChatStorage.sendRaw(player,
                        "&6&l★ BLACKJACK! ★ &fVocê ganhou &e" + casinoManager.formatCoins(payout) + " coins &7(2.5x)");
                break;
            case PLAYER_WINS:
                player.playSound(player.getLocation(), Sound.ORB_PICKUP, 1f, 1.2f);
                player.sendTitle(
                        ChatStorage.colorize("&a&lVITÓRIA!"),
                        ChatStorage.colorize("&a+" + casinoManager.formatCoins(payout) + " coins"));
                ChatStorage.sendRaw(player,
                        "&a&lVITÓRIA! &fVocê ganhou &e" + casinoManager.formatCoins(payout) + " coins &7no Blackjack");
                break;
            case PUSH:
                player.playSound(player.getLocation(), Sound.NOTE_PIANO, 1f, 1f);
                player.sendTitle(
                        ChatStorage.colorize("&e&lEMPATE"),
                        ChatStorage.colorize("&fAposta devolvida: &e" + casinoManager.formatCoins(payout)));
                ChatStorage.sendRaw(player,
                        "&e&lEMPATE! &fAposta devolvida: &e" + casinoManager.formatCoins(payout) + " coins");
                break;
            case DEALER_WINS:
            case PLAYER_BUST:
                player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1f, 1f);
                player.sendTitle(
                        ChatStorage.colorize("&c&lDerrota"),
                        ChatStorage.colorize("&c-" + casinoManager.formatCoins(bet) + " coins"));
                ChatStorage.sendRaw(player,
                        "&c&lDerrota! &fVocê perdeu &e" + casinoManager.formatCoins(bet) + " coins &7no Blackjack");
                break;
            default:
                break;
        }
    }
}
