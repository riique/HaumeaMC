package com.haumea.kitpvp.menu.duel;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.menu.BaseMenu;
import com.haumea.kitpvp.utils.ChatStorage;
import com.haumea.kitpvp.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Menu de estatísticas de duelo do jogador.
 * 
 * @author HaumeaMC
 */
public class DuelStatsMenu extends BaseMenu {

        private final Player targetPlayer; // Jogador cujas stats serão exibidas

        public DuelStatsMenu(HaumeaMC plugin, Player viewer, Player targetPlayer) {
                super(plugin, viewer, "§8⚔ Estatísticas de " + targetPlayer.getName(), 27);
                this.targetPlayer = targetPlayer;
        }

        @Override
        protected void setupItems() {
                // Preencher bordas
                fillBorders(15);

                // Obter estatísticas
                int wins = plugin.getDuelManager().getDuelWins(targetPlayer);
                int losses = plugin.getDuelManager().getDuelLosses(targetPlayer);
                int streak = plugin.getDuelManager().getDuelStreak(targetPlayer);
                int total = wins + losses;
                double winRate = total > 0 ? (wins * 100.0 / total) : 0;

                // ==================== VITÓRIAS (Slot 11) ====================
                ItemStack winsItem = new ItemBuilder(Material.EMERALD)
                                .amount(Math.max(1, Math.min(64, wins)))
                                .name("§a§lVitórias")
                                .lore(
                                                "",
                                                "§7Total: §a" + ChatStorage.formatNumber(wins),
                                                "",
                                                "§7Vitórias em duelos 1v1")
                                .glow()
                                .build();

                setItem(11, winsItem);

                // ==================== DERROTAS (Slot 13) ====================
                ItemStack lossesItem = new ItemBuilder(Material.REDSTONE)
                                .amount(Math.max(1, Math.min(64, losses)))
                                .name("§c§lDerrotas")
                                .lore(
                                                "",
                                                "§7Total: §c" + ChatStorage.formatNumber(losses),
                                                "",
                                                "§7Derrotas em duelos 1v1")
                                .build();

                setItem(13, lossesItem);

                // ==================== WINRATE (Slot 15) ====================
                String winRateColor = winRate >= 60 ? "§a" : (winRate >= 40 ? "§e" : "§c");
                ItemStack winRateItem = new ItemBuilder(Material.NETHER_STAR)
                                .name("§b§lWinrate")
                                .lore(
                                                "",
                                                "§7Taxa: " + winRateColor + String.format("%.1f", winRate) + "%",
                                                "",
                                                "§7Porcentagem de vitórias")
                                .glow()
                                .build();

                setItem(15, winRateItem);

                // ==================== STREAK (Slot 22) ====================
                String streakIcon = streak > 0 ? " §c🔥" : "";
                ItemBuilder streakBuilder = new ItemBuilder(Material.BLAZE_POWDER)
                                .amount(Math.max(1, Math.min(64, streak)))
                                .name("§e§lWinstreak" + streakIcon)
                                .lore(
                                                "",
                                                "§7Atual: §e" + streak,
                                                "",
                                                "§7Vitórias consecutivas");
                if (streak > 0) {
                        streakBuilder.glow();
                }
                ItemStack streakItem = streakBuilder.build();

                setItem(22, streakItem);

                // Preencher espaços vazios
                fillEmpty(7);
        }
}
