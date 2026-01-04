package com.haumea.kitpvp.menu.profile;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.managers.AchievementManager;
import com.haumea.kitpvp.menu.BaseMenu;
import com.haumea.kitpvp.models.Achievement;
import com.haumea.kitpvp.utils.ChatStorage;
import com.haumea.kitpvp.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Menu de Conquistas do Jogador.
 * 
 * Exibe todas as conquistas disponíveis com progresso e status.
 * 
 * @author HaumeaMC
 */
public class AchievementsMenu extends BaseMenu {

    private static final int ITEMS_PER_PAGE = 28;
    private int currentPage = 0;

    public AchievementsMenu(HaumeaMC plugin, Player player) {
        super(plugin, player, "§6§lConquistas", 54);
    }

    @Override
    protected void setupItems() {
        // Preencher bordas
        fillBorders(15); // Preto

        AchievementManager achievementManager = plugin.getAchievementManager();
        if (achievementManager == null) {
            setItem(22, new ItemBuilder(Material.BARRIER)
                    .name("§cSistema Indisponivel")
                    .lore("§7O sistema de conquistas nao esta ativo.")
                    .build());
            return;
        }

        // Cabeçalho com estatísticas
        int unlocked = achievementManager.getUnlockedCount(player);
        int total = achievementManager.getTotalCount();
        int percent = total > 0 ? (unlocked * 100) / total : 0;

        ItemStack headerItem = new ItemBuilder(Material.NETHER_STAR)
                .name("§6§l★ Suas Conquistas ★")
                .lore(
                        "",
                        "§7Progresso: §e" + unlocked + "§7/§e" + total,
                        "§7Completado: §a" + percent + "%",
                        "",
                        "§8Desbloqueie conquistas para ganhar",
                        "§8recompensas em coins!")
                .glow()
                .build();
        setItem(4, headerItem);

        // Listar conquistas
        List<Achievement> achievements = new ArrayList<>(achievementManager.getAllAchievements());
        int startIndex = currentPage * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, achievements.size());

        // Slots disponíveis para conquistas (excluindo bordas)
        int[] slots = {
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34,
                37, 38, 39, 40, 41, 42, 43
        };

        int slotIndex = 0;
        for (int i = startIndex; i < endIndex && slotIndex < slots.length; i++) {
            Achievement achievement = achievements.get(i);
            boolean hasAchievement = achievementManager.hasAchievement(player, achievement.getId());
            int progress = achievementManager.getProgress(player, achievement);

            ItemStack item = createAchievementItem(achievement, hasAchievement, progress);
            setItem(slots[slotIndex], item);
            slotIndex++;
        }

        // Navegação
        if (currentPage > 0) {
            setItem(45, new ItemBuilder(Material.ARROW)
                    .name("§e← Pagina Anterior")
                    .build(), (p, click) -> {
                        currentPage--;
                        refresh();
                        playClickSound();
                    });
        }

        int totalPages = (achievements.size() + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE;
        if (currentPage < totalPages - 1) {
            setItem(53, new ItemBuilder(Material.ARROW)
                    .name("§ePagina Seguinte →")
                    .build(), (p, click) -> {
                        currentPage++;
                        refresh();
                        playClickSound();
                    });
        }

        // Página atual
        setItem(49, new ItemBuilder(Material.PAPER)
                .name("§fPagina §e" + (currentPage + 1) + "§7/§e" + Math.max(1, totalPages))
                .build());

        // Botão voltar
        setItem(48, new ItemBuilder(Material.ARROW)
                .name("§c← Voltar")
                .build(), (p, click) -> {
                    new ProfileMenu(plugin, player).open();
                    playClickSound();
                });
    }

    /**
     * Cria o item de uma conquista
     */
    private ItemStack createAchievementItem(Achievement achievement, boolean unlocked, int progress) {
        Material material;
        try {
            material = Material.valueOf(achievement.getIcon());
        } catch (IllegalArgumentException e) {
            material = Material.PAPER;
        }

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(achievement.getDescription());
        lore.add("");

        if (unlocked) {
            lore.add("§a§l✓ DESBLOQUEADA!");
            if (achievement.getReward() > 0) {
                lore.add("");
                lore.add("§7Recompensa recebida: §e" + ChatStorage.formatNumber(achievement.getReward()) + " coins");
            }
        } else {
            // Barra de progresso
            int requirement = achievement.getRequirement();
            int percent = requirement > 0 ? Math.min(100, (progress * 100) / requirement) : 0;

            String progressBar = createProgressBar(percent);
            lore.add("§7Progresso: " + progressBar + " §f" + percent + "%");
            lore.add("§7" + ChatStorage.formatNumber(progress) + "§8/§7" + ChatStorage.formatNumber(requirement));

            if (achievement.getReward() > 0) {
                lore.add("");
                lore.add("§7Recompensa: §e" + ChatStorage.formatNumber(achievement.getReward()) + " coins");
            }
        }

        ItemBuilder builder = new ItemBuilder(material)
                .name((unlocked ? "§a" : "§c") + achievement.getCategoryIcon() + " " + achievement.getDisplayName())
                .lore(lore);

        if (unlocked) {
            builder.glow();
        }

        return builder.build();
    }

    /**
     * Cria uma barra de progresso visual
     */
    private String createProgressBar(int percent) {
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

    /**
     * Atualiza o menu
     */
    public void refresh() {
        inventory.clear();
        setupItems();
    }
}
