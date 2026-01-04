package com.haumea.kitpvp.menu.casino;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.managers.CasinoManager;
import com.haumea.kitpvp.menu.BaseMenu;
import com.haumea.kitpvp.models.casino.CasinoStats;
import com.haumea.kitpvp.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Menu de estatísticas do cassino.
 * Mostra histórico e resultados do jogador.
 * 
 * @author HaumeaMC
 */
public class CasinoStatsMenu extends BaseMenu {

    private final CasinoManager casinoManager;

    public CasinoStatsMenu(HaumeaMC plugin, Player player) {
        super(plugin, player, "&6&l📊 SUAS ESTATÍSTICAS", 45);
        this.casinoManager = plugin.getCasinoManager();
    }

    @Override
    protected void setupItems() {
        // Preencher bordas
        fillBorders(3); // Azul claro

        CasinoStats stats = casinoManager.getPlayerStats(player);

        // Slot 4: Cabeça do jogador com resumo geral
        ItemStack playerHead = ItemBuilder.playerHead(player.getName())
                .name("&6&l" + player.getName())
                .lore(
                        "",
                        "&7Estatísticas do Cassino",
                        "",
                        "&7Total apostado: &e" + casinoManager.formatCoins(stats.getTotalWagered()),
                        "&7Total ganho: &a" + casinoManager.formatCoins(stats.getTotalWon()),
                        "&7Total perdido: &c" + casinoManager.formatCoins(stats.getTotalLost()),
                        "",
                        "&7Lucro/Prejuízo: " + stats.getProfitFormatted(),
                        "&7Maior vitória: &a" + casinoManager.formatCoins(stats.getBiggestWin()))
                .build();
        setItem(4, playerHead);

        // Estatísticas gerais
        ItemStack generalStats = new ItemBuilder(Material.PAPER)
                .name("&e&lESTATÍSTICAS GERAIS")
                .lore(
                        "",
                        "&7Jogos jogados: &f" + stats.getGamesPlayed(),
                        "&7Taxa de vitória: &f" + stats.getWinRate() + "%",
                        "",
                        "&7Total apostado: &e" + casinoManager.formatCoins(stats.getTotalWagered()),
                        "&7Total ganho: &a" + casinoManager.formatCoins(stats.getTotalWon()),
                        "&7Total perdido: &c" + casinoManager.formatCoins(stats.getTotalLost()),
                        "",
                        "&7Lucro/Prejuízo: " + stats.getProfitFormatted(),
                        "&7Maior vitória: &a" + casinoManager.formatCoins(stats.getBiggestWin()))
                .build();
        setItem(20, generalStats);

        // Slots
        int slotsWinRate = stats.getSlotsPlayed() > 0
                ? (int) ((double) stats.getSlotsWins() / stats.getSlotsPlayed() * 100)
                : 0;

        ItemStack slotsStats = new ItemBuilder(Material.GOLD_BLOCK)
                .name("&6&l🎰 SLOTS")
                .lore(
                        "",
                        "&7Jogos: &f" + stats.getSlotsPlayed(),
                        "&7Vitórias: &a" + stats.getSlotsWins(),
                        "&7Taxa de vitória: &f" + slotsWinRate + "%",
                        "",
                        "&6Jackpots: &e" + stats.getSlotsJackpots())
                .build();
        setItem(21, slotsStats);

        // Roleta
        int rouletteWinRate = stats.getRoulettePlayed() > 0
                ? (int) ((double) stats.getRouletteWins() / stats.getRoulettePlayed() * 100)
                : 0;

        ItemStack rouletteStats = new ItemBuilder(Material.WOOL, 1, (short) 14)
                .name("&c&l🎡 ROLETA")
                .lore(
                        "",
                        "&7Jogos: &f" + stats.getRoulettePlayed(),
                        "&7Vitórias: &a" + stats.getRouletteWins(),
                        "&7Taxa de vitória: &f" + rouletteWinRate + "%")
                .build();
        setItem(22, rouletteStats);

        // Blackjack
        int blackjackWinRate = stats.getBlackjackPlayed() > 0
                ? (int) ((double) stats.getBlackjackWins() / stats.getBlackjackPlayed() * 100)
                : 0;

        ItemStack blackjackStats = new ItemBuilder(Material.PAPER)
                .name("&f&l🃏 BLACKJACK")
                .lore(
                        "",
                        "&7Jogos: &f" + stats.getBlackjackPlayed(),
                        "&7Vitórias: &a" + stats.getBlackjackWins(),
                        "&7Taxa de vitória: &f" + blackjackWinRate + "%",
                        "",
                        "&6Blackjacks: &e" + stats.getBlackjackNaturals())
                .build();
        setItem(23, blackjackStats);

        // Coinflip
        int coinflipTotal = stats.getCoinflipPlayed();
        int coinflipWinRate = coinflipTotal > 0 ? (int) ((double) stats.getCoinflipWins() / coinflipTotal * 100) : 0;

        ItemStack coinflipStats = new ItemBuilder(Material.GOLD_INGOT)
                .name("&e&l🪙 COINFLIP")
                .lore(
                        "",
                        "&7Jogos: &f" + coinflipTotal,
                        "&7Vitórias: &a" + stats.getCoinflipWins(),
                        "&7Derrotas: &c" + (coinflipTotal - stats.getCoinflipWins()),
                        "&7Taxa de vitória: &f" + coinflipWinRate + "%")
                .build();
        setItem(24, coinflipStats);

        // Crash
        ItemStack crashStats = new ItemBuilder(Material.TNT)
                .name("&c&l💥 CRASH")
                .lore(
                        "",
                        "&7Jogos: &f" + stats.getCrashPlayed(),
                        "&7Maior multiplicador: &a" + String.format("%.2fx", stats.getCrashBiggestMult()))
                .build();
        setItem(31, crashStats);

        // Voltar
        setItem(36, createBackButton(), (p, c) -> {
            playClickSound();
            new CasinoMainMenu(plugin, player).open();
        });

        // Fechar
        setItem(44, createCloseButton(), (p, c) -> {
            playClickSound();
            close();
        });
    }
}
