package com.haumea.kitpvp.menu;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.managers.LeagueManager;
import com.haumea.kitpvp.models.EloLeague;
import com.haumea.kitpvp.models.PlayerRank;
import com.haumea.kitpvp.utils.ChatStorage;
import com.haumea.kitpvp.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Menu de Ranking/Ligas do HaumeaMC.
 * 
 * Layout organizado (54 slots - 6 linhas):
 * 
 * Linha 1: [Borda] [Borda] [Borda] [Borda] [Borda] [Borda] [Borda] [Borda]
 * [Borda]
 * Linha 2: [Borda] [Pri I] [Pri II] [PriIII] [Pri IV] [Pri V] [Bro I] [Bro II]
 * [Borda]
 * Linha 3: [Borda] [BroIII] [Bro IV] [Bro V] [Sil I] [Sil II] [SilIII] [Sil IV]
 * [Borda]
 * Linha 4: [Borda] [Sil V] [Gol I] [Gol II] [GolIII] [Gol IV] [Gol V] [Dia I]
 * [Borda]
 * Linha 5: [Borda] [Dia II] [DiaIII] [Dia IV] [Dia V] [Eme I] [Eme II] [EmeIII]
 * [Borda]
 * Linha 6: [Eme IV] [Eme V] [Mas I] [MasII] [LEGND] [MasIII] [Mas IV] [Mas V]
 * [INFO]
 * 
 * @author HaumeaMC
 */
public class RankingMenu extends BaseMenu {

    private static final String MENU_TITLE = "&6&lRANKING &8- &fLigas";
    private static final int MENU_SIZE = 54;

    private final LeagueManager leagueManager;

    public RankingMenu(HaumeaMC plugin, Player player) {
        super(plugin, player, MENU_TITLE, MENU_SIZE);
        this.leagueManager = plugin.getLeagueManager();
    }

    @Override
    protected void setupItems() {
        // Preencher tudo com vidro preto primeiro
        ItemStack blackPane = createGlassPane(15);
        for (int i = 0; i < MENU_SIZE; i++) {
            setItem(i, blackPane);
        }

        // Colocar as ligas nos slots apropriados
        placeLeagues();

        // Colocar informações do jogador no canto inferior direito
        placePlayerInfo();
    }

    /**
     * Coloca todas as ligas e divisões no menu em ordem organizada
     */
    private void placeLeagues() {
        List<EloLeague> leagues = leagueManager.getAllLeagues();
        PlayerRank currentRank = leagueManager.getRank(player);
        int currentElo = leagueManager.getElo(player);

        // Layout organizado: ligas em sequência da esquerda para direita, de cima para
        // baixo
        // Linhas 2-5 (slots internos), linha 6 para Master, Legendary e Info

        // Linha 2: Primary I-V (5 slots) + Bronze I-II (2 slots) = 7 slots
        // Slots: 10, 11, 12, 13, 14, 15, 16

        // Linha 3: Bronze III-V (3 slots) + Silver I-IV (4 slots) = 7 slots
        // Slots: 19, 20, 21, 22, 23, 24, 25

        // Linha 4: Silver V (1 slot) + Gold I-V (5 slots) + Diamond I (1 slot) = 7
        // slots
        // Slots: 28, 29, 30, 31, 32, 33, 34

        // Linha 5: Diamond II-V (4 slots) + Emerald I-III (3 slots) = 7 slots
        // Slots: 37, 38, 39, 40, 41, 42, 43

        // Linha 6: Emerald IV-V (2 slots) + Master I-II (2) + LEGENDARY (1) + Master
        // III-V (3) = 8 slots
        // Slots: 45, 46, 47, 48, 49, 50, 51, 52

        int[] rankSlots = {
                // Linha 2: Primary I-V, Bronze I-II
                10, 11, 12, 13, 14, 15, 16,
                // Linha 3: Bronze III-V, Silver I-IV
                19, 20, 21, 22, 23, 24, 25,
                // Linha 4: Silver V, Gold I-V, Diamond I
                28, 29, 30, 31, 32, 33, 34,
                // Linha 5: Diamond II-V, Emerald I-III
                37, 38, 39, 40, 41, 42, 43,
                // Linha 6: Emerald IV-V, Master I-V (sem slot 49 que é Legendary)
                45, 46, 47, 48, 50, 51, 52
        };

        int slotIndex = 0;
        for (EloLeague league : leagues) {
            if (league.hasDivisions()) {
                for (int div = 1; div <= 5; div++) {
                    if (slotIndex < rankSlots.length) {
                        ItemStack item = createLeagueItem(league, div, currentRank, currentElo);
                        setItem(rankSlots[slotIndex], item);
                        slotIndex++;
                    }
                }
            } else {
                // Legendary - slot 49 (centro da última linha)
                ItemStack item = createLeagueItem(league, 1, currentRank, currentElo);
                setItem(49, item);
            }
        }
    }

    /**
     * Cria o item de uma liga/divisão
     */
    @SuppressWarnings("deprecation")
    private ItemStack createLeagueItem(EloLeague league, int division, PlayerRank currentRank, int currentElo) {
        Material material;
        short dataValue;

        switch (league.getId()) {
            case "primary":
                material = Material.INK_SACK;
                dataValue = 10; // Lime Dye
                break;
            case "bronze":
                material = Material.INK_SACK;
                dataValue = 3; // Cocoa Beans (marrom)
                break;
            case "silver":
                material = Material.INK_SACK;
                dataValue = 7; // Light Gray Dye
                break;
            case "gold":
                material = Material.GOLD_INGOT;
                dataValue = 0;
                break;
            case "diamond":
                material = Material.DIAMOND;
                dataValue = 0;
                break;
            case "emerald":
                material = Material.EMERALD;
                dataValue = 0;
                break;
            case "master":
                material = Material.NETHER_STAR;
                dataValue = 0;
                break;
            case "legendary":
                material = Material.NETHER_STAR; // Usar Nether Star ao invés de Redstone
                dataValue = 0;
                break;
            default:
                material = Material.INK_SACK;
                dataValue = 8;
        }

        // Status do jogador em relação a este rank
        int requiredElo = league.getEloForDivision(division);
        boolean achieved = currentElo >= requiredElo;
        boolean isCurrent = currentRank.getLeague().getId().equals(league.getId())
                && currentRank.getDivision() == division;

        // Nome do item
        String displayName = league.getFormattedName(division);
        if (isCurrent) {
            displayName += " §7§l«§a§l ATUAL";
        } else if (achieved) {
            displayName += " §7§l«§7 Conquistado";
        }

        // Lore
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7Elo Necessário: §f" + ChatStorage.formatNumber(requiredElo));
        lore.add("§7Recompensa: §e" + league.getCoinsReward() + " Coins");
        lore.add("");

        if (isCurrent) {
            lore.add("§a§l➤ Você está neste rank!");
            if (leagueManager.getNextRank(player) != null) {
                int eloToNext = leagueManager.getEloToNextRank(player);
                int progress = leagueManager.getProgressPercent(player);
                lore.add("");
                lore.add("§7Progresso: " + getProgressBar(progress) + " §f" + progress + "%");
                lore.add("§7Faltam: §e" + eloToNext + " Elo");
            }
        } else if (achieved) {
            lore.add("§7✓ Rank conquistado");
        } else {
            int eloNeeded = requiredElo - currentElo;
            lore.add("§c✗ Faltam §e" + eloNeeded + " §cElo");
        }

        ItemBuilder builder = new ItemBuilder(material, 1, dataValue)
                .name(displayName)
                .lore(lore.toArray(new String[0]));

        if (isCurrent || league.getId().equals("legendary")) {
            builder.glow();
        }
        return builder.build();
    }

    /**
     * Coloca as informações do jogador no slot 53 (canto inferior direito)
     */
    private void placePlayerInfo() {
        PlayerRank currentRank = leagueManager.getRank(player);
        PlayerRank nextRank = leagueManager.getNextRank(player);
        int currentElo = leagueManager.getElo(player);

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7Seu Elo: §f" + ChatStorage.formatNumber(currentElo));
        lore.add("§7Sua Liga: " + currentRank.getFormattedName());
        lore.add("");

        if (nextRank != null) {
            int eloToNext = leagueManager.getEloToNextRank(player);
            int progress = leagueManager.getProgressPercent(player);

            lore.add("§7Próxima Liga: " + nextRank.getFormattedName());
            lore.add("§7Elo Necessário: §e" + nextRank.getRequiredElo());
            lore.add("§7Faltam: §e" + eloToNext + " Elo");
            lore.add("");
            lore.add("§7Progresso: " + getProgressBar(progress) + " §f" + progress + "%");
        } else {
            lore.add("§4§l★ RANK MÁXIMO ALCANÇADO! ★");
            lore.add("");
            lore.add("§7Você é um jogador §4§lLEGENDARY§7!");
        }

        lore.add("");
        lore.add("§6§lDicas:");
        lore.add("§7• Mate jogadores de ranks superiores");
        lore.add("§7  para ganhar mais Elo!");
        lore.add("§7• Killstreaks da vítima = bônus!");

        ItemStack playerItem = new ItemBuilder(Material.EXP_BOTTLE)
                .name("§e§lSeu Progresso")
                .lore(lore.toArray(new String[0]))
                .glow()
                .build();

        setItem(53, playerItem); // Canto inferior direito
    }

    /**
     * Gera uma barra de progresso visual
     */
    private String getProgressBar(int percent) {
        int filled = percent / 10;
        int empty = 10 - filled;

        StringBuilder bar = new StringBuilder("§8[");
        for (int i = 0; i < filled; i++) {
            bar.append("§a■");
        }
        for (int i = 0; i < empty; i++) {
            bar.append("§7■");
        }
        bar.append("§8]");

        return bar.toString();
    }
}
