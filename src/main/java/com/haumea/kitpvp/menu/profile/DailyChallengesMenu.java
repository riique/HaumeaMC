package com.haumea.kitpvp.menu.profile;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.managers.DailyChallengeManager;
import com.haumea.kitpvp.menu.BaseMenu;
import com.haumea.kitpvp.models.DailyChallenge;
import com.haumea.kitpvp.utils.ChatStorage;
import com.haumea.kitpvp.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Menu de Desafios Diários.
 * 
 * Exibe os desafios do dia com progresso e recompensas.
 * 
 * @author HaumeaMC
 */
public class DailyChallengesMenu extends BaseMenu {

    public DailyChallengesMenu(HaumeaMC plugin, Player player) {
        super(plugin, player, "§6§lDesafios Diarios", 45);
    }

    @Override
    protected void setupItems() {
        // Preencher bordas
        fillBorders(15); // Preto

        DailyChallengeManager challengeManager = plugin.getDailyChallengeManager();
        if (challengeManager == null) {
            setItem(22, new ItemBuilder(Material.BARRIER)
                    .name("§cSistema Indisponivel")
                    .lore("§7O sistema de desafios nao esta ativo.")
                    .build());
            return;
        }

        List<DailyChallenge> challenges = challengeManager.getDailyChallenges();
        int completed = challengeManager.getCompletedCount(player);

        // Cabeçalho
        ItemStack headerItem = new ItemBuilder(Material.WATCH)
                .name("§6§l★ Desafios de Hoje ★")
                .lore(
                        "",
                        "§7Complete desafios para ganhar",
                        "§7recompensas em coins!",
                        "",
                        "§7Completados: §a" + completed + "§7/§e" + challenges.size(),
                        "",
                        "§8Os desafios resetam a meia-noite.")
                .glow()
                .build();
        setItem(4, headerItem);

        // Slots para os 3 desafios
        int[] slots = { 20, 22, 24 };

        for (int i = 0; i < Math.min(challenges.size(), slots.length); i++) {
            DailyChallenge challenge = challenges.get(i);
            int progress = challengeManager.getProgress(player, challenge.getId());
            boolean isCompleted = challengeManager.isCompleted(player, challenge.getId());

            ItemStack item = createChallengeItem(challenge, progress, isCompleted);
            setItem(slots[i], item);
        }

        // Recompensa por completar todos
        boolean allCompleted = completed >= challenges.size();
        ItemStack bonusItem = new ItemBuilder(allCompleted ? Material.CHEST : Material.ENDER_CHEST)
                .name(allCompleted ? "§a§l✓ BONUS COLETADO!" : "§6§lBonus de Conclusao")
                .lore(
                        "",
                        allCompleted ? "§7Voce completou todos os desafios!" : "§7Complete todos os desafios",
                        allCompleted ? "" : "§7para receber um bonus extra!",
                        "",
                        "§7Bonus: §e1.000 coins",
                        "",
                        allCompleted ? "§a✓ Bonus recebido!"
                                : "§c✗ " + completed + "/" + challenges.size() + " desafios completos")
                .build();

        if (allCompleted) {
            new ItemBuilder(bonusItem).glow();
        }
        setItem(40, bonusItem);

        // Botão voltar
        setItem(36, new ItemBuilder(Material.ARROW)
                .name("§c← Voltar")
                .build(), (p, click) -> {
                    new ProfileMenu(plugin, player).open();
                    playClickSound();
                });
    }

    /**
     * Cria o item de um desafio
     */
    private ItemStack createChallengeItem(DailyChallenge challenge, int progress, boolean completed) {
        Material material;
        try {
            material = Material.valueOf(challenge.getMaterialIcon());
        } catch (IllegalArgumentException e) {
            material = Material.PAPER;
        }

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§f" + challenge.getDescription());
        lore.add("");

        if (completed) {
            lore.add("§a§l✓ COMPLETO!");
            lore.add("");
            lore.add("§7Recompensa recebida: §e" + ChatStorage.formatNumber(challenge.getReward()) + " coins");
        } else {
            // Barra de progresso
            int percent = challenge.getProgressPercent(progress);
            String progressBar = createProgressBar(percent);

            lore.add("§7Progresso: " + progressBar);
            lore.add("§7" + challenge.formatProgress(progress));
            lore.add("");
            lore.add("§7Recompensa: §e" + ChatStorage.formatNumber(challenge.getReward()) + " coins");
        }

        ItemBuilder builder = new ItemBuilder(material)
                .name((completed ? "§a✓ " : "§e") + challenge.getDisplayName())
                .lore(lore);

        if (completed) {
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
        bar.append("§8] §f" + percent + "%");

        return bar.toString();
    }
}
