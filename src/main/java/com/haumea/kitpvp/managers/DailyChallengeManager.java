package com.haumea.kitpvp.managers;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.models.DailyChallenge;
import com.haumea.kitpvp.models.PlayerData;
import com.haumea.kitpvp.profile.PlayerProfile;
import com.haumea.kitpvp.utils.ChatStorage;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gerenciador de Desafios Diários do HaumeaMC.
 * 
 * Sistema de missões que resetam diariamente, incentivando
 * jogadores a se manterem ativos.
 * 
 * @author HaumeaMC
 */
public class DailyChallengeManager {

    private final HaumeaMC plugin;

    // Desafios disponíveis (templates)
    private final List<DailyChallenge.Template> challengeTemplates;

    // Desafios ativos do dia atual (gerados à meia-noite)
    private List<DailyChallenge> dailyChallenges;
    private long lastResetDay;

    // Cache de progresso dos jogadores (UUID -> Map<challengeId, progress>)
    private final Map<UUID, Map<String, Integer>> progressCache;

    // Constantes
    private static final int CHALLENGES_PER_DAY = 3;
    private static final String DATA_KEY_PREFIX = "daily_challenge_";
    private static final String DATA_KEY_LAST_RESET = "daily_challenge_reset";

    // Referência da task de atualização de playtime
    private int playtimeTaskId = -1;

    public DailyChallengeManager(HaumeaMC plugin) {
        this.plugin = plugin;
        this.challengeTemplates = new ArrayList<>();
        this.dailyChallenges = new ArrayList<>();
        this.progressCache = new ConcurrentHashMap<>();
        this.lastResetDay = getCurrentDay();

        registerChallengeTemplates();
        generateDailyChallenges();
        startMidnightResetTask();
        startPlaytimeTracker();
    }

    /**
     * Task para rastrear progresso de PLAYTIME.
     * A cada minuto, incrementa 1 minuto de progresso para todos jogadores online
     * com um desafio PLAYTIME ativo.
     */
    private void startPlaytimeTracker() {
        playtimeTaskId = new BukkitRunnable() {
            @Override
            public void run() {
                // Verificar se há algum desafio PLAYTIME ativo hoje
                boolean hasPlaytimeChallenge = false;
                for (DailyChallenge challenge : dailyChallenges) {
                    if (challenge.getType() == DailyChallenge.Type.PLAYTIME) {
                        hasPlaytimeChallenge = true;
                        break;
                    }
                }

                if (!hasPlaytimeChallenge) {
                    return; // Nenhum desafio de playtime ativo hoje
                }

                // Incrementar progresso para todos jogadores online
                for (org.bukkit.entity.Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
                    incrementProgress(player, DailyChallenge.Type.PLAYTIME, 1);
                }
            }
        }.runTaskTimer(plugin, 20L * 60, 20L * 60).getTaskId(); // A cada 1 minuto
    }

    /**
     * Registra templates de desafios
     */
    private void registerChallengeTemplates() {
        // KILLS
        challengeTemplates.add(new DailyChallenge.Template("kills_5", DailyChallenge.Type.KILLS,
                "§c⚔ Matador", "Mate 5 jogadores", 5, 250));
        challengeTemplates.add(new DailyChallenge.Template("kills_10", DailyChallenge.Type.KILLS,
                "§c⚔ Assassino", "Mate 10 jogadores", 10, 500));
        challengeTemplates.add(new DailyChallenge.Template("kills_20", DailyChallenge.Type.KILLS,
                "§c⚔ Executor", "Mate 20 jogadores", 20, 1000));

        // KILLSTREAK
        challengeTemplates.add(new DailyChallenge.Template("streak_3", DailyChallenge.Type.KILLSTREAK,
                "§e🔥 Em Chamas", "Alcance 3 kills sem morrer", 3, 300));
        challengeTemplates.add(new DailyChallenge.Template("streak_5", DailyChallenge.Type.KILLSTREAK,
                "§e🔥 Imparavel", "Alcance 5 kills sem morrer", 5, 600));
        challengeTemplates.add(new DailyChallenge.Template("streak_10", DailyChallenge.Type.KILLSTREAK,
                "§e🔥 Lenda", "Alcance 10 kills sem morrer", 10, 1500));

        // DUELS
        challengeTemplates.add(new DailyChallenge.Template("duels_3", DailyChallenge.Type.DUELS_WON,
                "§b⚔ Duelista", "Venca 3 duelos", 3, 400));
        challengeTemplates.add(new DailyChallenge.Template("duels_5", DailyChallenge.Type.DUELS_WON,
                "§b⚔ Campeao", "Venca 5 duelos", 5, 750));

        // PLAYTIME
        challengeTemplates.add(new DailyChallenge.Template("playtime_30", DailyChallenge.Type.PLAYTIME,
                "§a⏱ Dedicado", "Jogue por 30 minutos", 30, 200));
        challengeTemplates.add(new DailyChallenge.Template("playtime_60", DailyChallenge.Type.PLAYTIME,
                "§a⏱ Veterano", "Jogue por 1 hora", 60, 400));

        // SOUPS USED
        challengeTemplates.add(new DailyChallenge.Template("soups_20", DailyChallenge.Type.SOUPS_USED,
                "§6🥣 Chef", "Use 20 sopas", 20, 150));
        challengeTemplates.add(new DailyChallenge.Template("soups_50", DailyChallenge.Type.SOUPS_USED,
                "§6🥣 Master Chef", "Use 50 sopas", 50, 350));

        plugin.getLogger().info("[DailyChallenges] " + challengeTemplates.size() + " templates registrados.");
    }

    /**
     * Gera os desafios do dia
     */
    private void generateDailyChallenges() {
        dailyChallenges.clear();
        lastResetDay = getCurrentDay();

        // Selecionar aleatoriamente N desafios únicos
        List<DailyChallenge.Template> shuffled = new ArrayList<>(challengeTemplates);
        Collections.shuffle(shuffled);

        for (int i = 0; i < Math.min(CHALLENGES_PER_DAY, shuffled.size()); i++) {
            DailyChallenge.Template template = shuffled.get(i);
            dailyChallenges.add(new DailyChallenge(template));
        }

        plugin.getLogger().info("[DailyChallenges] Gerados " + dailyChallenges.size() + " desafios para hoje.");
    }

    /**
     * Task para resetar desafios à meia-noite
     */
    private void startMidnightResetTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                long currentDay = getCurrentDay();
                if (currentDay != lastResetDay) {
                    generateDailyChallenges();
                    progressCache.clear();
                    plugin.getLogger().info("[DailyChallenges] Desafios resetados para o novo dia!");
                }
            }
        }.runTaskTimer(plugin, 20L * 60, 20L * 60); // Verificar a cada minuto
    }

    /**
     * Obtém o dia atual (dias desde epoch)
     */
    private long getCurrentDay() {
        return System.currentTimeMillis() / (24 * 60 * 60 * 1000);
    }

    /**
     * Obtém os desafios do dia
     */
    public List<DailyChallenge> getDailyChallenges() {
        return new ArrayList<>(dailyChallenges);
    }

    /**
     * Incrementa progresso de um tipo de desafio
     */
    public void incrementProgress(Player player, DailyChallenge.Type type, int amount) {
        PlayerProfile profile = plugin.getProfileManager().getProfile(player);
        if (profile == null)
            return;

        // Verificar se já resetou hoje
        long playerLastReset = profile.getData().getCustomData(DATA_KEY_LAST_RESET, 0L);
        if (playerLastReset < lastResetDay) {
            resetPlayerProgress(player);
            profile.getData().setCustomData(DATA_KEY_LAST_RESET, lastResetDay);
        }

        for (DailyChallenge challenge : dailyChallenges) {
            if (challenge.getType() != type)
                continue;
            if (isCompleted(player, challenge.getId()))
                continue;

            int current = getProgress(player, challenge.getId());
            int newProgress = current + amount;
            setProgress(player, challenge.getId(), newProgress);

            if (newProgress >= challenge.getRequirement()) {
                completeChallenge(player, challenge);
            }
        }
    }

    /**
     * Marca desafio como completo e dá recompensa
     */
    private void completeChallenge(Player player, DailyChallenge challenge) {
        PlayerProfile profile = plugin.getProfileManager().getProfile(player);
        if (profile == null)
            return;

        // Marcar como completo
        profile.getData().setCustomData(DATA_KEY_PREFIX + challenge.getId() + "_complete", true);

        // Dar recompensa
        profile.addCoins(challenge.getReward());

        // Incrementar contador total de desafios completados (para conquistas)
        int totalCompleted = profile.getData().getCustomData("daily_challenges_completed", 0);
        profile.getData().setCustomData("daily_challenges_completed", totalCompleted + 1);

        // Verificar conquistas
        if (plugin.getAchievementManager() != null) {
            plugin.getAchievementManager().checkAchievements(player);
        }

        // Notificar
        player.sendMessage("");
        ChatStorage.sendRaw(player, "&8&m----------------------------------------");
        ChatStorage.sendRaw(player, "");
        ChatStorage.sendRaw(player, "  &a&l✓ DESAFIO COMPLETO!");
        ChatStorage.sendRaw(player, "");
        ChatStorage.sendRaw(player, "  " + challenge.getDisplayName());
        ChatStorage.sendRaw(player, "");
        ChatStorage.sendRaw(player, "  &aRecompensa: &e" + ChatStorage.formatNumber(challenge.getReward()) + " coins");
        ChatStorage.sendRaw(player, "");
        ChatStorage.sendRaw(player, "&8&m----------------------------------------");
        player.sendMessage("");

        player.playSound(player.getLocation(), Sound.LEVEL_UP, 1.0f, 1.2f);
    }

    /**
     * Obtém progresso de um desafio
     */
    public int getProgress(Player player, String challengeId) {
        UUID uuid = player.getUniqueId();
        Map<String, Integer> playerProgress = progressCache.get(uuid);

        if (playerProgress == null) {
            // Carregar do PlayerData
            PlayerProfile profile = plugin.getProfileManager().getProfile(player);
            if (profile == null)
                return 0;

            playerProgress = new HashMap<>();
            for (DailyChallenge challenge : dailyChallenges) {
                int progress = profile.getData().getCustomData(DATA_KEY_PREFIX + challenge.getId(), 0);
                playerProgress.put(challenge.getId(), progress);
            }
            progressCache.put(uuid, playerProgress);
        }

        return playerProgress.getOrDefault(challengeId, 0);
    }

    /**
     * Define progresso de um desafio
     */
    private void setProgress(Player player, String challengeId, int progress) {
        UUID uuid = player.getUniqueId();
        progressCache.computeIfAbsent(uuid, k -> new HashMap<>()).put(challengeId, progress);

        PlayerProfile profile = plugin.getProfileManager().getProfile(player);
        if (profile != null) {
            profile.getData().setCustomData(DATA_KEY_PREFIX + challengeId, progress);
        }
    }

    /**
     * Verifica se desafio está completo
     */
    public boolean isCompleted(Player player, String challengeId) {
        PlayerProfile profile = plugin.getProfileManager().getProfile(player);
        if (profile == null)
            return false;
        return profile.getData().getCustomData(DATA_KEY_PREFIX + challengeId + "_complete", false);
    }

    /**
     * Reseta progresso do jogador (chamado no reset diário)
     */
    private void resetPlayerProgress(Player player) {
        PlayerProfile profile = plugin.getProfileManager().getProfile(player);
        if (profile == null)
            return;

        // Limpar progresso antigo
        for (String key : new ArrayList<>(profile.getData().getCustomDataKeys())) {
            if (key.startsWith(DATA_KEY_PREFIX)) {
                profile.getData().removeCustomData(key);
            }
        }

        progressCache.remove(player.getUniqueId());
    }

    /**
     * Limpa cache ao deslogar
     */
    public void onPlayerQuit(Player player) {
        progressCache.remove(player.getUniqueId());
    }

    /**
     * Obtém contagem de desafios completos hoje
     */
    public int getCompletedCount(Player player) {
        int count = 0;
        for (DailyChallenge challenge : dailyChallenges) {
            if (isCompleted(player, challenge.getId())) {
                count++;
            }
        }
        return count;
    }
}
