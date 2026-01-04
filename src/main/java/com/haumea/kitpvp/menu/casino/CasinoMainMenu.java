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
 * Menu principal do Cassino.
 * Hub central para acessar todos os jogos de cassino.
 * 
 * @author HaumeaMC
 */
public class CasinoMainMenu extends BaseMenu {

    private final CasinoManager casinoManager;

    public CasinoMainMenu(HaumeaMC plugin, Player player) {
        super(plugin, player, "&6&l✦ CASSINO DO SERVIDOR ✦", 54);
        this.casinoManager = plugin.getCasinoManager();
    }

    @Override
    protected void setupItems() {
        // Preencher bordas com vidro laranja
        fillBorders(1); // Laranja

        // Slot 4: Informações do jogador (cabeça)
        long coins = plugin.getStatsManager().getMoney(player);
        CasinoStats stats = casinoManager.getPlayerStats(player);

        ItemStack playerHead = ItemBuilder.playerHead(player.getName())
                .name("&6&lSEU SALDO")
                .lore(
                        "",
                        "&fCoins: &e" + casinoManager.formatCoins(coins),
                        "",
                        "&7Jogos jogados: &f" + stats.getGamesPlayed(),
                        "&7Total ganho: &a" + casinoManager.formatCoins(stats.getTotalWon()),
                        "&7Total perdido: &c" + casinoManager.formatCoins(stats.getTotalLost()),
                        "&7Lucro/Prejuízo: " + stats.getProfitFormatted())
                .build();
        setItem(4, playerHead);

        // Linha 1: Slots e Roleta

        // Slot 19: SLOTS
        ItemStack slotsItem = new ItemBuilder(Material.GOLD_BLOCK)
                .name("&6&l🎰 SLOTS")
                .lore(
                        "",
                        "&7Gire a máquina e ganhe!",
                        "&7Prêmios de até &e25x&7!",
                        "",
                        "&8▸ &f2 iguais: &a1.5x",
                        "&8▸ &f3 iguais: &a3x",
                        "&8▸ &f3 raros: &a10x",
                        "&8▸ &6JACKPOT: &a25x",
                        "",
                        "&eClique para jogar!")
                .glow()
                .build();
        setItem(19, slotsItem, (p, c) -> {
            playClickSound();
            new SlotsMenu(plugin, player).open();
        });

        // Slot 21: ROLETA
        ItemStack rouletteItem = new ItemBuilder(Material.WOOL, 1, (short) 14) // Red wool
                .name("&c&l🎡 ROLETA")
                .lore(
                        "",
                        "&7Aposte em cores ou números!",
                        "&7Prêmios de até &e36x&7!",
                        "",
                        "&8▸ &cVermelho&7/&8Preto: &a2x",
                        "&8▸ &ePar&7/&fÍmpar: &a2x",
                        "&8▸ &bNúmero: &a36x",
                        "",
                        "&eClique para jogar!")
                .glow()
                .build();
        setItem(21, rouletteItem, (p, c) -> {
            playClickSound();
            new RouletteMenu(plugin, player).open();
        });

        // Slot 23: BLACKJACK
        ItemStack blackjackItem = new ItemBuilder(Material.PAPER)
                .name("&f&l🃏 BLACKJACK")
                .lore(
                        "",
                        "&7Chegue mais perto de 21!",
                        "&7Sem estourar!",
                        "",
                        "&8▸ &fVitória: &a2x",
                        "&8▸ &6Blackjack: &a2.5x",
                        "",
                        "&eClique para jogar!")
                .glow()
                .build();
        setItem(23, blackjackItem, (p, c) -> {
            playClickSound();
            new BlackjackMenu(plugin, player).open();
        });

        // Slot 25: COINFLIP
        ItemStack coinflipItem = new ItemBuilder(Material.GOLD_INGOT)
                .name("&e&l🪙 COINFLIP")
                .lore(
                        "",
                        "&7Aposte contra outros jogadores!",
                        "&750/50 de chance!",
                        "",
                        "&7Coinflips ativos: &f" + casinoManager.getAvailableCoinflips().size(),
                        "",
                        "&eClique para ver lista!")
                .glow()
                .build();
        setItem(25, coinflipItem, (p, c) -> {
            playClickSound();
            new CoinflipMenu(plugin, player).open();
        });

        // Linha 3: Crash

        // Slot 31: CRASH
        String crashStatus;
        if (casinoManager.getCurrentCrashGame() != null) {
            switch (casinoManager.getCurrentCrashGame().getState()) {
                case WAITING:
                    crashStatus = "&7Próximo: &e" + casinoManager.getCrashCountdown() + "s";
                    break;
                case RUNNING:
                    crashStatus = "&aEM ANDAMENTO! &f" + casinoManager.getCurrentCrashGame().getFormattedMultiplier();
                    break;
                default:
                    crashStatus = "&cCrashou!";
            }
        } else {
            crashStatus = "&7Iniciando...";
        }

        ItemStack crashItem = new ItemBuilder(Material.TNT)
                .name("&c&l💥 CRASH")
                .lore(
                        "",
                        "&7O multiplicador sobe...",
                        "&7Saia antes de crashar!",
                        "",
                        "&8Status: " + crashStatus,
                        "",
                        "&eClique para jogar!")
                .glow()
                .build();
        setItem(31, crashItem, (p, c) -> {
            playClickSound();
            new CrashMenu(plugin, player).open();
        });

        // Slot 40: ESTATÍSTICAS
        ItemStack statsItem = new ItemBuilder(Material.BOOK)
                .name("&b&l📊 ESTATÍSTICAS")
                .lore(
                        "",
                        "&7Veja seu histórico completo",
                        "&7e resultados por jogo!",
                        "",
                        "&eClique para ver!")
                .build();
        setItem(40, statsItem, (p, c) -> {
            playClickSound();
            new CasinoStatsMenu(plugin, player).open();
        });

        // Slot 49: FECHAR
        setItem(49, createCloseButton(), (p, c) -> {
            playClickSound();
            close();
        });

        // Decoração adicional

        // Moedas decorativas
        ItemStack coinDecor = new ItemBuilder(Material.GOLD_NUGGET)
                .name("&6&l✦ &eCassino Haumea &6&l✦")
                .lore("&7Teste sua sorte!")
                .build();
        setItem(13, coinDecor);

        // Info de apostas
        ItemStack betInfo = new ItemBuilder(Material.PAPER)
                .name("&e&lINFORMAÇÕES")
                .lore(
                        "",
                        "&7Aposta mínima: &e" + casinoManager.formatCoins(casinoManager.getMinBet()),
                        "&7Aposta máxima: &e" + casinoManager.formatCoins(casinoManager.getMaxBet()),
                        "",
                        "&7Valores rápidos:",
                        formatPresetBets())
                .build();
        setItem(22, betInfo);
    }

    /**
     * Formata os valores de aposta predefinidos.
     */
    private String formatPresetBets() {
        StringBuilder sb = new StringBuilder();
        for (Long bet : casinoManager.getPresetBets()) {
            sb.append("&8• &f").append(casinoManager.formatCoins(bet)).append(" ");
        }
        return sb.toString().trim();
    }
}
