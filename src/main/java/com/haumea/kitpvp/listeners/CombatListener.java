package com.haumea.kitpvp.listeners;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.managers.LeagueManager;
import com.haumea.kitpvp.managers.StatsManager;
import com.haumea.kitpvp.models.DailyChallenge;
import com.haumea.kitpvp.models.PlayerRank;
import com.haumea.kitpvp.utils.ChatStorage;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import com.haumea.kitpvp.profile.PlayerProfile;
import com.haumea.kitpvp.models.PlayerData;

/**
 * Listener responsável por eventos de combate.
 * Gerencia as estatísticas de kills/deaths e o sistema de killstreak.
 * Utiliza o sistema de ELO via LeagueManager.
 * 
 * @author HaumeaMC
 */
public class CombatListener implements Listener {

    private final HaumeaMC plugin;
    private final StatsManager statsManager;

    // Configurações de coins por kill (ELO é calculado pelo LeagueManager)
    private static final long COINS_PER_KILL = 25;
    private static final long COINS_BONUS_PER_KILLSTREAK = 5;

    public CombatListener(HaumeaMC plugin) {
        this.plugin = plugin;
        this.statsManager = plugin.getStatsManager();
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerDeath(PlayerDeathEvent event) {
        // Remover mensagem de morte padrão do Minecraft
        event.setDeathMessage(null);

        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        // Salvar rank atual antes das modificações
        LeagueManager leagueManager = plugin.getLeagueManager();
        PlayerRank victimRankBefore = leagueManager != null ? leagueManager.getRank(victim) : null;

        // Obter killstreak da vítima ANTES de resetar
        int victimKillstreak = statsManager.getKillstreak(victim);

        // Registrar morte da vítima (stats)
        statsManager.addDeath(victim);

        // Calcular e aplicar perda de ELO
        int eloLoss = 0;
        if (leagueManager != null && killer != null) {
            eloLoss = leagueManager.calculateEloLoss(victim);
            leagueManager.removeElo(victim, eloLoss);
        }

        // Registrar morte na warp FPS (se estiver nela)
        if (plugin.getFPSWarpManager() != null) {
            String victimWarp = plugin.getFPSWarpManager().getPlayerWarp(victim);
            if (victimWarp != null && victimWarp.equalsIgnoreCase("fps")) {
                registerFPSDeath(victim);
            }
        }

        // Verificar se foi rebaixado de rank
        PlayerRank victimRankAfter = leagueManager != null ? leagueManager.getRank(victim) : null;
        if (victimRankBefore != null && victimRankAfter != null) {
            if (victimRankAfter.compareTo(victimRankBefore) < 0) {
                // O rebaixamento já é notificado pelo LeagueManager
            }
        }

        // Mensagem de killstreak encerrado (se tinha algum)
        if (victimKillstreak > 2 && killer != null) {
            // Usar nome fake do killer se tiver
            String killerDisplayName = getDisplayName(killer);
            ChatStorage.send(victim, "stats.killstreak-ended",
                    "streak", String.valueOf(victimKillstreak),
                    "killer", killerDisplayName);

            // Broadcast quando killstreak alto (5+) é encerrado
            if (victimKillstreak >= 5) {
                String victimDisplayName = getDisplayName(victim);
                long bonusCoins = victimKillstreak * 10; // Bônus por encerrar killstreak

                // Dar bônus de coins ao killer
                statsManager.addMoney(killer, bonusCoins);

                // Broadcast elaborado
                broadcastKillstreakEnded(killerDisplayName, victimDisplayName, victimKillstreak, bonusCoins);

                // Conquistas de streak breaker
                if (plugin.getAchievementManager() != null) {
                    // Encerrar streak de 5+ -> streak_breaker
                    plugin.getAchievementManager().incrementSpecialAchievement(killer, "streak_breaker");

                    // Encerrar streak de 10+ -> giant_slayer
                    if (victimKillstreak >= 10) {
                        plugin.getAchievementManager().incrementSpecialAchievement(killer, "giant_slayer");
                    }
                }
            }
        } else {
            // Mensagem simples de morte
            ChatStorage.send(victim, "stats.death", "elo", String.valueOf(eloLoss));
        }

        // Se foi morto por outro jogador
        if (killer != null && !killer.equals(victim)) {
            handleKill(killer, victim);

            // Processar bounty (se houver)
            if (plugin.getBountyManager() != null) {
                plugin.getBountyManager().processDeath(victim, killer);
            }

            // Verificar conquista "underdog" (matar alguém com ELO 500+ acima)
            if (plugin.getLeagueManager() != null && plugin.getAchievementManager() != null) {
                int killerElo = plugin.getLeagueManager().getElo(killer);
                int victimElo = plugin.getLeagueManager().getElo(victim);
                if (victimElo - killerElo >= 500) {
                    plugin.getAchievementManager().incrementSpecialAchievement(killer, "underdog");
                    plugin.getAchievementManager().incrementSpecialAchievement(killer, "underdog_master");
                }
            }
        }

        // Verificar conquistas do jogador (killer: kills, ELO, etc.)
        if (plugin.getAchievementManager() != null && killer != null) {
            plugin.getAchievementManager().checkAchievements(killer);
        }

        // Verificar conquistas de mortes da vítima (conquistas cômicas)
        if (plugin.getAchievementManager() != null) {
            plugin.getAchievementManager().checkAchievements(victim);
        }

        // Renascer instantaneamente (se não estiver em duelo)
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            boolean inDuel = false;
            // Verificar se o DuelManager existe e se o jogador está em duelo
            if (plugin.getDuelManager() != null && plugin.getDuelManager().isInDuel(victim)) {
                inDuel = true;
            }

            if (!inDuel && victim.isOnline() && victim.isDead()) {
                victim.spigot().respawn();
            }
        }, 1L);
    }

    /**
     * Processa uma kill, atualizando estatísticas e enviando mensagens
     * 
     * @param killer Jogador que matou
     * @param victim Jogador que morreu
     */
    private void handleKill(Player killer, Player victim) {
        LeagueManager leagueManager = plugin.getLeagueManager();

        // Salvar rank atual antes das modificações
        PlayerRank killerRankBefore = leagueManager != null ? leagueManager.getRank(killer) : null;

        // Registrar kill (adiciona coins internamente)
        statsManager.addKill(killer);

        // Calcular e aplicar ganho de ELO
        int eloGained = 0;
        if (leagueManager != null) {
            eloGained = leagueManager.calculateEloGain(killer, victim);
            leagueManager.addElo(killer, eloGained);
        }

        // Calcular valores ganhos (para mensagem)
        int currentStreak = statsManager.getKillstreak(killer);
        long coinsGained = COINS_PER_KILL + (currentStreak * COINS_BONUS_PER_KILLSTREAK);

        // Usar nome fake da vítima se tiver
        String victimDisplayName = getDisplayName(victim);

        // Mensagem de kill
        ChatStorage.send(killer, "stats.kill",
                "victim", victimDisplayName,
                "elo", String.valueOf(eloGained),
                "coins", String.valueOf(coinsGained));

        // Verificar se foi promovido de rank (já notificado pelo LeagueManager)
        PlayerRank killerRankAfter = leagueManager != null ? leagueManager.getRank(killer) : null;
        if (killerRankBefore != null && killerRankAfter != null) {
            // A promoção já é notificada pelo LeagueManager.addElo()
        }

        // Mensagem de killstreak (a cada 3, 5, 10, 15, 20, etc.)
        if (shouldAnnounceKillstreak(currentStreak)) {
            ChatStorage.send(killer, "stats.killstreak", "streak", String.valueOf(currentStreak));

            // Broadcast para o servidor em killstreaks (3, 5, 10, 15, 20...)
            String killerDisplayName = getDisplayName(killer);
            broadcastKillstreak(killerDisplayName, currentStreak);
        }

        // Atualizar progresso de desafios diários
        if (plugin.getDailyChallengeManager() != null) {
            plugin.getDailyChallengeManager().incrementProgress(killer, DailyChallenge.Type.KILLS, 1);
            if (currentStreak >= 3) {
                plugin.getDailyChallengeManager().incrementProgress(killer, DailyChallenge.Type.KILLSTREAK, 0); // Just
                                                                                                                // check
            }
        }

        // Registrar kill na warp FPS (se estiver nela)
        if (plugin.getFPSWarpManager() != null) {
            String killerWarp = plugin.getFPSWarpManager().getPlayerWarp(killer);
            if (killerWarp != null && killerWarp.equalsIgnoreCase("fps")) {
                registerFPSKill(killer);
            }
        }
    }

    /**
     * Registra uma kill na warp FPS (incrementa fps_kills e fps_killstreak)
     * 
     * @param killer Jogador que matou
     */
    private void registerFPSKill(Player killer) {
        PlayerProfile profile = plugin.getProfileManager().getProfile(killer);
        if (profile == null)
            return;

        PlayerData data = profile.getData();

        // Incrementar kills
        int fpsKills = data.getCustomData("fps_kills", 0) + 1;
        data.setCustomData("fps_kills", fpsKills);

        // Incrementar killstreak
        int fpsStreak = data.getCustomData("fps_killstreak", 0) + 1;
        data.setCustomData("fps_killstreak", fpsStreak);

        // Atualizar recorde de killstreak
        int fpsMaxStreak = data.getCustomData("fps_max_killstreak", 0);
        if (fpsStreak > fpsMaxStreak) {
            data.setCustomData("fps_max_killstreak", fpsStreak);
        }
        // PlayerData é salvo automaticamente quando o jogador sai
    }

    /**
     * Registra uma morte na warp FPS (incrementa fps_deaths e reseta
     * fps_killstreak)
     * 
     * @param victim Jogador que morreu
     */
    private void registerFPSDeath(Player victim) {
        PlayerProfile profile = plugin.getProfileManager().getProfile(victim);
        if (profile == null)
            return;

        PlayerData data = profile.getData();

        // Incrementar mortes
        int fpsDeaths = data.getCustomData("fps_deaths", 0) + 1;
        data.setCustomData("fps_deaths", fpsDeaths);

        // Resetar killstreak
        data.setCustomData("fps_killstreak", 0);
        // PlayerData é salvo automaticamente quando o jogador sai
    }

    /**
     * Verifica se o killstreak deve ser anunciado
     * Anuncia em: 3, 5, 10, 15, 20, 25, 30, etc.
     * 
     * @param streak Killstreak atual
     * @return true se deve anunciar
     */
    private boolean shouldAnnounceKillstreak(int streak) {
        if (streak <= 0)
            return false;
        if (streak == 3 || streak == 5)
            return true;
        return streak >= 10 && streak % 5 == 0;
    }

    /**
     * Obtém o nome de exibição do jogador (fake nick ou real)
     * 
     * @param player Jogador
     * @return Nome a ser exibido
     */
    private String getDisplayName(Player player) {
        if (plugin.getFakeNickManager() != null && plugin.getFakeNickManager().hasFakeNick(player)) {
            return plugin.getFakeNickManager().getFakeNick(player);
        }
        return player.getName();
    }

    /**
     * Envia broadcast de killstreak para todos os jogadores
     * Formato elaborado com header/message/footer
     * 
     * @param playerName Nome do jogador com killstreak
     * @param streak     Quantidade de kills seguidas
     */
    private void broadcastKillstreak(String playerName, int streak) {
        String header = ChatStorage.getMessage("stats.killstreak-broadcast.header");
        String message = ChatStorage.getMessage("stats.killstreak-broadcast.message",
                "player", playerName,
                "streak", String.valueOf(streak));
        String footer = ChatStorage.getMessage("stats.killstreak-broadcast.footer");

        for (Player online : plugin.getServer().getOnlinePlayers()) {
            online.sendMessage("");
            online.sendMessage(header);
            online.sendMessage(message);
            online.sendMessage(footer);
            online.sendMessage("");
        }
    }

    /**
     * Envia broadcast quando um killstreak é encerrado
     * Formato elaborado com header/message/reward/footer
     * 
     * @param killerName Nome de quem encerrou
     * @param victimName Nome de quem tinha o killstreak
     * @param streak     Killstreak que foi encerrado
     * @param bonusCoins Bônus de coins ganho
     */
    private void broadcastKillstreakEnded(String killerName, String victimName, int streak, long bonusCoins) {
        String header = ChatStorage.getMessage("stats.killstreak-ended-broadcast.header");
        String message = ChatStorage.getMessage("stats.killstreak-ended-broadcast.message",
                "killer", killerName,
                "victim", victimName,
                "streak", String.valueOf(streak));
        String reward = ChatStorage.getMessage("stats.killstreak-ended-broadcast.reward",
                "coins", ChatStorage.formatNumber(bonusCoins));
        String footer = ChatStorage.getMessage("stats.killstreak-ended-broadcast.footer");

        for (Player online : plugin.getServer().getOnlinePlayers()) {
            online.sendMessage("");
            online.sendMessage(header);
            online.sendMessage(message);
            online.sendMessage(reward);
            online.sendMessage(footer);
            online.sendMessage("");
        }
    }
}
