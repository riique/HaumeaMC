package com.haumea.kitpvp.menu.profile;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.menu.BaseMenu;
import com.haumea.kitpvp.models.PlayerData;
import com.haumea.kitpvp.profile.PlayerProfile;
import com.haumea.kitpvp.utils.ChatStorage;
import com.haumea.kitpvp.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Menu de Estatísticas do Jogador (27 slots - 3 linhas).
 * 
 * Exibe estatísticas de combate em diferentes modos de jogo.
 * 
 * Layout:
 * - Slot 10: KitPvP (Arena principal)
 * - Slot 11: FPS (Arena otimizada)
 * - Slot 12: Duels (1v1)
 * - Slot 22: Botão Voltar
 * 
 * @author HaumeaMC
 */
public class StatsMenu extends BaseMenu {

        private static final int SLOT_KITPVP = 10;
        private static final int SLOT_FPS = 11;
        private static final int SLOT_DUELS = 12;
        private static final int SLOT_BACK = 22;

        private final Player targetPlayer;

        /**
         * Construtor para visualizar próprias estatísticas
         */
        public StatsMenu(HaumeaMC plugin, Player player) {
                this(plugin, player, player);
        }

        /**
         * Construtor para visualizar estatísticas de outro jogador
         */
        public StatsMenu(HaumeaMC plugin, Player viewer, Player target) {
                super(plugin, viewer,
                                target.equals(viewer) ? "&e&lSuas Estatísticas"
                                                : "&e&lEstatísticas de " + target.getName(),
                                27);
                this.targetPlayer = target;
        }

        @Override
        protected void setupItems() {
                // Preencher bordas
                fillBorders(15); // Preto

                // Decorar
                ItemStack goldPane = createGlassPane(4, " "); // Amarelo
                setItem(0, goldPane);
                setItem(8, goldPane);
                setItem(18, goldPane);
                setItem(26, goldPane);

                PlayerProfile profile = plugin.getProfileManager().getProfile(targetPlayer);

                // === KITPVP - Arena Principal (Slot 10) ===
                int kills = 0, deaths = 0, killstreak = 0, maxKillstreak = 0;

                if (profile != null) {
                        kills = profile.getKills();
                        deaths = profile.getDeaths();
                        killstreak = profile.getKillStreak();
                        maxKillstreak = profile.getData().getHighestKillStreak();
                }

                double kdr = deaths > 0 ? (double) kills / deaths : kills;

                ItemStack kitpvpItem = new ItemBuilder(Material.MUSHROOM_SOUP)
                                .name("§a§lKitPvP")
                                .lore(
                                                "§7Arena principal do servidor",
                                                "",
                                                "§6Estatísticas:",
                                                "  §8▪ §fAbates: §e" + ChatStorage.formatNumber(kills),
                                                "  §8▪ §fMortes: §e" + ChatStorage.formatNumber(deaths),
                                                "  §8▪ §fK/D: §e" + String.format("%.2f", kdr),
                                                "",
                                                "§6Killstreak:",
                                                "  §8▪ §fAtual: §e" + killstreak,
                                                "  §8▪ §fRecorde: §e" + maxKillstreak,
                                                "")
                                .build();

                setItem(SLOT_KITPVP, kitpvpItem);

                // === FPS - Arena Otimizada (Slot 11) ===
                // Estatísticas FPS armazenadas em customData
                int fpsKills = 0, fpsDeaths = 0, fpsKillstreak = 0, fpsMaxKillstreak = 0;

                if (profile != null) {
                        PlayerData data = profile.getData();
                        fpsKills = data.getCustomData("fps_kills", 0);
                        fpsDeaths = data.getCustomData("fps_deaths", 0);
                        fpsKillstreak = data.getCustomData("fps_killstreak", 0);
                        fpsMaxKillstreak = data.getCustomData("fps_max_killstreak", 0);
                }

                double fpsKdr = fpsDeaths > 0 ? (double) fpsKills / fpsDeaths : fpsKills;

                ItemStack fpsItem = new ItemBuilder(Material.GLASS)
                                .name("§b§lFPS")
                                .lore(
                                                "§7Arena otimizada para",
                                                "§7jogadores de FPS baixo",
                                                "",
                                                "§6Estatísticas:",
                                                "  §8▪ §fAbates: §e" + ChatStorage.formatNumber(fpsKills),
                                                "  §8▪ §fMortes: §e" + ChatStorage.formatNumber(fpsDeaths),
                                                "  §8▪ §fK/D: §e" + String.format("%.2f", fpsKdr),
                                                "",
                                                "§6Killstreak:",
                                                "  §8▪ §fAtual: §e" + fpsKillstreak,
                                                "  §8▪ §fRecorde: §e" + fpsMaxKillstreak,
                                                "")
                                .build();

                setItem(SLOT_FPS, fpsItem);

                // === DUELS - 1v1 (Slot 12) ===
                int duelsWins = 0, duelsLosses = 0, duelsStreak = 0, duelsMaxStreak = 0;

                if (profile != null) {
                        PlayerData data = profile.getData();
                        duelsWins = data.getCustomData("duels_wins", 0);
                        duelsLosses = data.getCustomData("duels_losses", 0);
                        duelsStreak = data.getCustomData("duels_streak", 0);
                        duelsMaxStreak = data.getCustomData("duels_max_streak", 0);
                }

                double duelsWinRate = (duelsWins + duelsLosses) > 0
                                ? ((double) duelsWins / (duelsWins + duelsLosses)) * 100
                                : 0;

                ItemStack duelsItem = new ItemBuilder(Material.DIAMOND_SWORD)
                                .name("§c§lDuels")
                                .lore(
                                                "§7Combates 1v1 ranqueados",
                                                "",
                                                "§6Estatísticas:",
                                                "  §8▪ §fVitórias: §e" + ChatStorage.formatNumber(duelsWins),
                                                "  §8▪ §fDerrotas: §e" + ChatStorage.formatNumber(duelsLosses),
                                                "  §8▪ §fWinrate: §e" + String.format("%.1f", duelsWinRate) + "%",
                                                "",
                                                "§6Sequência:",
                                                "  §8▪ §fAtual: §e" + duelsStreak,
                                                "  §8▪ §fRecorde: §e" + duelsMaxStreak,
                                                "")
                                .glow()
                                .build();

                setItem(SLOT_DUELS, duelsItem);

                // === BOTÃO VOLTAR (Slot 22) ===
                ItemStack backItem = new ItemBuilder(Material.ARROW)
                                .name("§c§lVoltar")
                                .lore(
                                                "§7Clique para voltar",
                                                "§7ao menu de perfil.")
                                .build();

                setItem(SLOT_BACK, backItem, (p, click) -> {
                        new ProfileMenu(plugin, player, targetPlayer).open();
                        playClickSound();
                });
        }
}
