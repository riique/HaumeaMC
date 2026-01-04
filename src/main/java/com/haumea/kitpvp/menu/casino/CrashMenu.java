package com.haumea.kitpvp.menu.casino;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.managers.CasinoManager;
import com.haumea.kitpvp.menu.BaseMenu;
import com.haumea.kitpvp.models.casino.CrashGame;
import com.haumea.kitpvp.utils.ChatStorage;
import com.haumea.kitpvp.utils.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;

/**
 * Menu de Crash.
 * Mostra o estado atual do jogo e permite entrar/sair/cashout.
 * 
 * @author HaumeaMC
 */
public class CrashMenu extends BaseMenu {

    private final CasinoManager casinoManager;
    private long selectedBet;
    private BukkitTask updateTask;

    public CrashMenu(HaumeaMC plugin, Player player) {
        super(plugin, player, "&c&l💥 CRASH", 54);
        this.casinoManager = plugin.getCasinoManager();
        this.selectedBet = 0;
    }

    @Override
    protected void setupItems() {
        CrashGame game = casinoManager.getCurrentCrashGame();

        if (game == null) {
            showWaiting();
            return;
        }

        switch (game.getState()) {
            case WAITING:
                setupWaitingState(game);
                break;
            case RUNNING:
                setupRunningState(game);
                break;
            case CRASHED:
                setupCrashedState(game);
                break;
        }
    }

    /**
     * Mostra estado de aguardando jogo.
     */
    private void showWaiting() {
        fillBorders(14); // Vermelho

        ItemStack waitingItem = new ItemBuilder(Material.WATCH)
                .name("&e&lCARREGANDO...")
                .lore("&7Aguarde o jogo iniciar")
                .build();
        setItem(22, waitingItem);

        setItem(49, createCloseButton(), (p, c) -> close());
    }

    /**
     * Estado de espera (jogadores podem entrar).
     */
    private void setupWaitingState(CrashGame game) {
        // Preencher bordas
        fillBorders(14);

        // Slot 4: Saldo e info
        long coins = plugin.getStatsManager().getMoney(player);
        boolean isParticipant = game.isParticipant(player.getUniqueId());

        ItemStack balanceItem = ItemBuilder.playerHead(player.getName())
                .name("&6&lSEU SALDO")
                .lore(
                        "",
                        "&fCoins: &e" + casinoManager.formatCoins(coins),
                        "",
                        isParticipant ? "&a✓ Você está participando!" : "&7Você não está participando",
                        isParticipant
                                ? "&7Sua aposta: &e" + casinoManager.formatCoins(game.getBet(player.getUniqueId()))
                                : "")
                .build();
        setItem(4, balanceItem);

        // Countdown
        ItemStack countdownItem = new ItemBuilder(Material.WATCH)
                .name("&e&lPRÓXIMO JOGO")
                .lore(
                        "",
                        "&7Iniciando em: &f" + casinoManager.getCrashCountdown() + "s",
                        "",
                        "&7Participantes: &f" + game.getParticipantCount())
                .build();
        setItem(13, countdownItem);

        // Info de como jogar
        ItemStack infoItem = new ItemBuilder(Material.PAPER)
                .name("&e&lCOMO JOGAR")
                .lore(
                        "",
                        "&71. Entre com uma aposta",
                        "&72. O multiplicador começa a subir",
                        "&73. Saia antes que crashe!",
                        "",
                        "&cSe crashar, você perde tudo!")
                .build();
        setItem(22, infoItem);

        // Se não está participando, mostrar opções de aposta
        if (!isParticipant) {
            setupBetButtons();

            // Botão de entrar
            if (selectedBet > 0) {
                ItemStack joinButton = new ItemBuilder(Material.SLIME_BALL)
                        .name("&a&lENTRAR NO JOGO")
                        .lore(
                                "",
                                "&7Aposta: &e" + casinoManager.formatCoins(selectedBet),
                                "",
                                "&aClique para entrar!")
                        .glow()
                        .build();
                setItem(40, joinButton, (p, c) -> joinGame());
            } else {
                ItemStack disabledButton = new ItemBuilder(Material.STAINED_GLASS_PANE, 1, (short) 7)
                        .name("&7&lENTRAR NO JOGO")
                        .lore("&cSelecione um valor primeiro")
                        .build();
                setItem(40, disabledButton);
            }
        } else {
            // Botão de sair
            ItemStack leaveButton = new ItemBuilder(Material.BARRIER)
                    .name("&c&lSAIR DO JOGO")
                    .lore(
                            "",
                            "&7Sua aposta será devolvida",
                            "",
                            "&eClique para sair")
                    .build();
            setItem(40, leaveButton, (p, c) -> leaveGame());
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

        // Iniciar atualização automática
        startUpdateTask();
    }

    /**
     * Estado de jogo em andamento.
     */
    private void setupRunningState(CrashGame game) {
        // Preencher bordas baseado no multiplicador
        double mult = game.getCurrentMultiplier();
        short borderColor;
        if (mult < 2.0) {
            borderColor = 5; // Verde
        } else if (mult < 5.0) {
            borderColor = 4; // Amarelo
        } else if (mult < 10.0) {
            borderColor = 1; // Laranja
        } else {
            borderColor = 14; // Vermelho
        }
        fillBorders(borderColor);

        boolean isParticipant = game.isParticipant(player.getUniqueId());
        boolean hasCashedOut = game.hasCashedOut(player.getUniqueId());

        // Multiplicador gigante no centro
        String multColor;
        if (mult < 2.0)
            multColor = "&a";
        else if (mult < 5.0)
            multColor = "&e";
        else if (mult < 10.0)
            multColor = "&6";
        else
            multColor = "&c";

        ItemStack multiplierItem = new ItemBuilder(Material.DIAMOND_BLOCK)
                .name(multColor + "&l" + String.format("%.2fx", mult))
                .lore(
                        "",
                        "&7O multiplicador está subindo!",
                        "&cPode crashar a qualquer momento!")
                .glow()
                .build();
        setItem(22, multiplierItem);

        // Status do jogador
        if (isParticipant) {
            long bet = game.getBet(player.getUniqueId());
            long potentialPayout = (long) (bet * mult);

            if (hasCashedOut) {
                double cashoutMult = game.getCashoutMultiplier(player.getUniqueId());
                long payout = game.calculatePayout(player.getUniqueId());

                ItemStack cashedOutItem = new ItemBuilder(Material.EMERALD)
                        .name("&a&l✓ VOCÊ SAIU!")
                        .lore(
                                "",
                                "&7Multiplicador: &a" + String.format("%.2fx", cashoutMult),
                                "&7Ganho: &a" + casinoManager.formatCoins(payout),
                                "",
                                "&7Aguarde o crash...")
                        .glow()
                        .build();
                setItem(40, cashedOutItem);
            } else {
                ItemStack cashoutButton = new ItemBuilder(Material.SLIME_BALL)
                        .name("&a&lSAIR AGORA!")
                        .lore(
                                "",
                                "&7Aposta: &e" + casinoManager.formatCoins(bet),
                                "&7Ganho atual: &a" + casinoManager.formatCoins(potentialPayout),
                                "",
                                "&a&lCLIQUE PARA SAIR!")
                        .glow()
                        .build();
                setItem(40, cashoutButton, (p, c) -> cashout());
            }
        } else {
            ItemStack spectatorItem = new ItemBuilder(Material.COMPASS)
                    .name("&7&lESPECTADOR")
                    .lore(
                            "",
                            "&7Você não está participando",
                            "&7Entre no próximo jogo!")
                    .build();
            setItem(40, spectatorItem);
        }

        // Lista de participantes
        ItemStack participantsItem = new ItemBuilder(Material.SKULL_ITEM)
                .name("&e&lPARTICIPANTES")
                .lore(buildParticipantsList(game))
                .build();
        setItem(4, participantsItem);

        // Fechar (não pode voltar durante o jogo se estiver participando)
        if (!isParticipant || hasCashedOut) {
            setItem(53, createCloseButton(), (p, c) -> close());
        }

        // Atualizar automaticamente
        startUpdateTask();
    }

    /**
     * Estado de crash (jogo terminou).
     */
    private void setupCrashedState(CrashGame game) {
        fillBorders(14);

        ItemStack crashedItem = new ItemBuilder(Material.TNT)
                .name("&c&l💥 CRASHOU!")
                .lore(
                        "",
                        "&7Multiplicador final: &c" + String.format("%.2fx", game.getCrashPoint()),
                        "",
                        "&7Próximo jogo em breve...")
                .build();
        setItem(22, crashedItem);

        // Voltar
        setItem(45, createBackButton(), (p, c) -> {
            playClickSound();
            new CasinoMainMenu(plugin, player).open();
        });

        // Fechar
        setItem(53, createCloseButton(), (p, c) -> close());

        // Atualizar para pegar o próximo jogo
        startUpdateTask();
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
    }

    /**
     * Constrói a lista de participantes para o lore.
     */
    private String[] buildParticipantsList(CrashGame game) {
        java.util.List<String> lines = new java.util.ArrayList<>();
        lines.add("");
        lines.add("&7Total: &f" + game.getParticipantCount());
        lines.add("");

        int count = 0;
        for (Map.Entry<UUID, Long> entry : game.getParticipants().entrySet()) {
            if (count >= 5) {
                lines.add("&7... e mais " + (game.getParticipantCount() - 5));
                break;
            }

            Player p = Bukkit.getPlayer(entry.getKey());
            String name = p != null ? p.getName() : "???";
            boolean cashedOut = game.hasCashedOut(entry.getKey());

            if (cashedOut) {
                double mult = game.getCashoutMultiplier(entry.getKey());
                lines.add("&a✓ " + name + " &7(" + String.format("%.2fx", mult) + ")");
            } else {
                lines.add("&e• " + name + " &7(" + casinoManager.formatCoins(entry.getValue()) + ")");
            }
            count++;
        }

        return lines.toArray(new String[0]);
    }

    /**
     * Entra no jogo.
     */
    private void joinGame() {
        if (selectedBet <= 0)
            return;

        if (casinoManager.joinCrash(player, selectedBet)) {
            playSuccessSound();
            selectedBet = 0;
            refresh();
        } else {
            playErrorSound();
        }
    }

    /**
     * Sai do jogo.
     */
    private void leaveGame() {
        if (casinoManager.leaveCrash(player)) {
            playSuccessSound();
            refresh();
        } else {
            playErrorSound();
        }
    }

    /**
     * Faz cashout.
     */
    private void cashout() {
        if (casinoManager.cashoutCrash(player)) {
            playLevelUpSound();
            refresh();
        } else {
            playErrorSound();
        }
    }

    /**
     * Inicia a task de atualização automática.
     */
    private void startUpdateTask() {
        if (updateTask != null) {
            updateTask.cancel();
        }

        updateTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || player.getOpenInventory() == null ||
                        !(player.getOpenInventory().getTopInventory().getHolder() instanceof CrashMenu)) {
                    this.cancel();
                    return;
                }
                refresh();
            }
        }.runTaskTimer(plugin, 10L, 10L); // A cada 0.5 segundos
    }

    @Override
    public void close() {
        if (updateTask != null) {
            updateTask.cancel();
        }
        super.close();
    }
}
