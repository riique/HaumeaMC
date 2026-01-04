package com.haumea.kitpvp.menu.profile;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.menu.BaseMenu;
import com.haumea.kitpvp.models.PlayerData;
import com.haumea.kitpvp.profile.PlayerProfile;
import com.haumea.kitpvp.utils.ChatStorage;
import com.haumea.kitpvp.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Menu de Presença Diária.
 * 
 * Permite ao jogador resgatar uma recompensa diária de moedas
 * a cada 24 horas.
 * 
 * Layout bonito com:
 * - Informações sobre a recompensa
 * - Tempo restante para próximo resgate
 * - Streak de dias consecutivos (bônus)
 * 
 * @author HaumeaMC
 */
public class DailyRewardMenu extends BaseMenu {

    private static final int SLOT_REWARD = 13;
    private static final int SLOT_INFO = 11;
    private static final int SLOT_STREAK = 15;
    private static final int SLOT_BACK = 31;

    // Configurações de recompensa
    private static final long BASE_REWARD = 500; // Moedas base
    private static final long STREAK_BONUS = 50; // Bônus por dia consecutivo
    private static final int MAX_STREAK_BONUS = 14; // Máximo de dias para bônus (2 semanas)
    private static final long COOLDOWN_MILLIS = TimeUnit.HOURS.toMillis(24); // 24 horas

    // Chaves no customData
    private static final String KEY_LAST_DAILY = "daily_last_claim";
    private static final String KEY_DAILY_STREAK = "daily_streak";

    public DailyRewardMenu(HaumeaMC plugin, Player player) {
        super(plugin, player, "&6&lPresenca Diaria", 45);
    }

    @Override
    protected void setupItems() {
        // Preencher bordas decorativas
        fillBorders(15); // Preto

        // Decorar com vidros amarelos nos cantos internos
        ItemStack goldPane = createGlassPane(4, " "); // Amarelo
        setItem(10, goldPane);
        setItem(16, goldPane);
        setItem(28, goldPane);
        setItem(34, goldPane);

        // Vidros laranjas para decoração extra
        ItemStack orangePane = createGlassPane(1, " "); // Laranja
        setItem(19, orangePane);
        setItem(25, orangePane);

        // Obter dados do jogador
        PlayerProfile profile = plugin.getProfileManager().getProfile(player);
        if (profile == null) {
            close();
            return;
        }

        PlayerData data = profile.getData();
        long lastClaim = getLastClaimTime(data);
        int streak = getDailyStreak(data);
        boolean canClaim = canClaimDaily(lastClaim);

        // === INFORMAÇÕES (Slot 11) ===
        setupInfoItem(streak);

        // === RECOMPENSA PRINCIPAL (Slot 13) ===
        setupRewardItem(canClaim, lastClaim, streak);

        // === STREAK (Slot 15) ===
        setupStreakItem(streak);

        // === BOTÃO VOLTAR (Slot 31) ===
        setItem(SLOT_BACK, createBackButton(), (p, click) -> {
            new ProfileMenu(plugin, player).open();
            playClickSound();
        });
    }

    /**
     * Configura o item de informações
     */
    private void setupInfoItem(int streak) {
        long currentReward = calculateReward(streak);

        List<String> lore = new ArrayList<>();
        lore.add("§7Resgate sua recompensa");
        lore.add("§7diaria de moedas!");
        lore.add("");
        lore.add("§6Como funciona:");
        lore.add("§8• §fResgate a cada §e24 horas");
        lore.add("§8• §fGanhe §6" + ChatStorage.formatNumber(BASE_REWARD) + " moedas §fbase");
        lore.add("§8• §f+§e" + ChatStorage.formatNumber(STREAK_BONUS) + " moedas §fpor dia consecutivo");
        lore.add("§8• §fBonus maximo: §a" + MAX_STREAK_BONUS + " dias");
        lore.add("");
        lore.add("§aSua proxima recompensa: §6" + ChatStorage.formatNumber(currentReward) + " moedas");

        ItemStack infoItem = new ItemBuilder(Material.BOOK)
                .name("§e§lInformacoes")
                .lore(lore)
                .build();

        setItem(SLOT_INFO, infoItem);
    }

    /**
     * Configura o item de recompensa principal
     */
    private void setupRewardItem(boolean canClaim, long lastClaim, int streak) {
        if (canClaim) {
            // Pode resgatar - mostrar baú aberto
            long reward = calculateReward(streak);

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add("§aRecompensa disponivel!");
            lore.add("");
            lore.add("§6Voce recebera:");
            lore.add("§8• §e" + ChatStorage.formatNumber(reward) + " moedas");
            if (streak > 0) {
                lore.add("§8• §7(Inclui bonus de §a" + streak + " dia(s)§7)");
            }
            lore.add("");
            lore.add("§e▶ Clique para resgatar!");

            ItemStack rewardItem = new ItemBuilder(Material.CHEST)
                    .name("§a§l★ RESGATAR RECOMPENSA ★")
                    .lore(lore)
                    .glow()
                    .build();

            setItem(SLOT_REWARD, rewardItem, (p, click) -> {
                claimDailyReward();
            });
        } else {
            // Não pode resgatar - mostrar tempo restante
            long timeRemaining = getTimeRemaining(lastClaim);
            String timeFormatted = formatTimeRemaining(timeRemaining);

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add("§cVoce ja resgatou hoje!");
            lore.add("");
            lore.add("§7Tempo restante:");
            lore.add("§e" + timeFormatted);
            lore.add("");
            lore.add("§7Volte mais tarde!");

            ItemStack lockedItem = new ItemBuilder(Material.ENDER_CHEST)
                    .name("§c§lAGUARDANDO...")
                    .lore(lore)
                    .build();

            setItem(SLOT_REWARD, lockedItem, (p, click) -> {
                ChatStorage.sendRaw(player,
                        "&c&lHAUMEAMC &fVoce precisa esperar &e" + timeFormatted + " &fpara resgatar novamente!");
                playErrorSound();
            });
        }
    }

    /**
     * Configura o item de streak
     */
    private void setupStreakItem(int streak) {
        Material material = streak >= 7 ? Material.DIAMOND : (streak >= 3 ? Material.GOLD_INGOT : Material.IRON_INGOT);
        String streakColor = streak >= 7 ? "§b" : (streak >= 3 ? "§6" : "§7");

        List<String> lore = new ArrayList<>();
        lore.add("§7Dias consecutivos resgatando");
        lore.add("§7a presenca diaria.");
        lore.add("");
        lore.add("§6Seu streak: " + streakColor + streak + " dia(s)");
        lore.add("");
        lore.add("§7Bonus atual: §e+" + ChatStorage.formatNumber(Math.min(streak, MAX_STREAK_BONUS) * STREAK_BONUS)
                + " moedas");
        lore.add("");
        if (streak >= MAX_STREAK_BONUS) {
            lore.add("§a§l★ BONUS MAXIMO ATINGIDO! ★");
        } else {
            int daysToMax = MAX_STREAK_BONUS - streak;
            lore.add("§7Faltam §e" + daysToMax + " dia(s) §7para bonus maximo!");
        }

        ItemStack streakItem = new ItemBuilder(material)
                .name("§e§lSeu Streak")
                .amount(Math.max(1, Math.min(streak, 64)))
                .lore(lore)
                .build();

        setItem(SLOT_STREAK, streakItem);
    }

    /**
     * Processa o resgate da recompensa diária
     */
    private void claimDailyReward() {
        PlayerProfile profile = plugin.getProfileManager().getProfile(player);
        if (profile == null) {
            return;
        }

        PlayerData data = profile.getData();
        long lastClaim = getLastClaimTime(data);

        // Verificar se pode resgatar
        if (!canClaimDaily(lastClaim)) {
            ChatStorage.sendRaw(player, "&c&lHAUMEAMC &fVoce ainda precisa esperar para resgatar!");
            playErrorSound();
            return;
        }

        // Calcular streak
        int currentStreak = getDailyStreak(data);
        boolean isConsecutive = isConsecutiveDay(lastClaim);

        if (isConsecutive) {
            currentStreak++;
        } else {
            currentStreak = 1; // Resetar para 1 (primeiro dia)
        }

        // Calcular recompensa
        long reward = calculateReward(currentStreak - 1); // -1 porque o streak é atualizado antes

        // Aplicar recompensa
        profile.addCoins(reward);

        // Atualizar dados
        data.setCustomData(KEY_LAST_DAILY, System.currentTimeMillis());
        data.setCustomData(KEY_DAILY_STREAK, currentStreak);

        // Salvar imediatamente no MongoDB para persistir entre reloads
        plugin.getProfileManager().getRepository().saveAsync(profile.getData());

        // Feedback visual e sonoro
        player.playSound(player.getLocation(), Sound.LEVEL_UP, 1.0f, 1.0f);

        // Mensagem de sucesso
        ChatStorage.sendRaw(player, "");
        ChatStorage.sendRaw(player, "&6&l★ PRESENCA DIARIA ★");
        ChatStorage.sendRaw(player, "");
        ChatStorage.sendRaw(player, "&fVoce resgatou &6" + ChatStorage.formatNumber(reward) + " moedas&f!");
        if (currentStreak > 1) {
            ChatStorage.sendRaw(player, "&7Streak: &e" + currentStreak + " dia(s) consecutivos!");
        }
        ChatStorage.sendRaw(player, "");
        ChatStorage.sendRaw(player, "&aVolte amanha para continuar seu streak!");
        ChatStorage.sendRaw(player, "");

        // Atualizar menu
        refresh();
    }

    // ==================== MÉTODOS UTILITÁRIOS ====================

    /**
     * Obtém o último tempo de resgate
     */
    private long getLastClaimTime(PlayerData data) {
        Object value = data.getCustomData(KEY_LAST_DAILY);
        if (value instanceof Long) {
            return (Long) value;
        } else if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return 0L;
    }

    /**
     * Obtém o streak atual
     */
    private int getDailyStreak(PlayerData data) {
        Object value = data.getCustomData(KEY_DAILY_STREAK);
        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return 0;
    }

    /**
     * Verifica se pode resgatar a recompensa diária
     */
    private boolean canClaimDaily(long lastClaim) {
        if (lastClaim == 0) {
            return true; // Nunca resgatou
        }
        return System.currentTimeMillis() - lastClaim >= COOLDOWN_MILLIS;
    }

    /**
     * Verifica se é um dia consecutivo (dentro de 48h do último resgate)
     */
    private boolean isConsecutiveDay(long lastClaim) {
        if (lastClaim == 0) {
            return false;
        }
        long timeSince = System.currentTimeMillis() - lastClaim;
        // Consecutivo se resgatou entre 24h e 48h atrás
        return timeSince >= COOLDOWN_MILLIS && timeSince < TimeUnit.HOURS.toMillis(48);
    }

    /**
     * Obtém o tempo restante para o próximo resgate
     */
    private long getTimeRemaining(long lastClaim) {
        if (lastClaim == 0) {
            return 0;
        }
        long elapsed = System.currentTimeMillis() - lastClaim;
        return Math.max(0, COOLDOWN_MILLIS - elapsed);
    }

    /**
     * Calcula a recompensa baseada no streak
     */
    private long calculateReward(int streak) {
        int cappedStreak = Math.min(streak, MAX_STREAK_BONUS);
        return BASE_REWARD + (cappedStreak * STREAK_BONUS);
    }

    /**
     * Formata o tempo restante em formato legível
     */
    private String formatTimeRemaining(long millis) {
        if (millis <= 0) {
            return "Disponivel!";
        }

        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;

        if (hours > 0) {
            return String.format("%02dh %02dm %02ds", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%02dm %02ds", minutes, seconds);
        } else {
            return String.format("%02ds", seconds);
        }
    }
}
