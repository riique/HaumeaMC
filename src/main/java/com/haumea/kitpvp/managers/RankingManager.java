package com.haumea.kitpvp.managers;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.database.MongoPlayerRepository;
import com.haumea.kitpvp.models.PlayerData;
import com.haumea.kitpvp.profile.PlayerProfile;

import java.util.*;

/**
 * Gerenciador de Rankings Globais do HaumeaMC.
 * 
 * Fornece rankings de kills, ELO, coins, KDR, etc.
 * Os dados são obtidos do MongoDB para incluir jogadores offline.
 * 
 * @author HaumeaMC
 */
public class RankingManager {

    private final HaumeaMC plugin;

    // Cache de rankings (atualizado periodicamente)
    private List<RankEntry> topKills;
    private List<RankEntry> topElo;
    private List<RankEntry> topCoins;
    private List<RankEntry> topKdr;
    private List<RankEntry> topKillstreak;

    private long lastUpdate = 0;
    private static final long CACHE_DURATION = 60000; // 1 minuto

    /**
     * Entrada de ranking
     */
    public static class RankEntry {
        public final String playerName;
        public final UUID uuid;
        public final long value;
        public final double valueDouble;

        public RankEntry(String playerName, UUID uuid, long value) {
            this.playerName = playerName;
            this.uuid = uuid;
            this.value = value;
            this.valueDouble = value;
        }

        public RankEntry(String playerName, UUID uuid, double value) {
            this.playerName = playerName;
            this.uuid = uuid;
            this.value = (long) value;
            this.valueDouble = value;
        }
    }

    public RankingManager(HaumeaMC plugin) {
        this.plugin = plugin;
        this.topKills = new ArrayList<>();
        this.topElo = new ArrayList<>();
        this.topCoins = new ArrayList<>();
        this.topKdr = new ArrayList<>();
        this.topKillstreak = new ArrayList<>();
    }

    /**
     * Atualiza todos os rankings se o cache expirou
     */
    private void updateIfNeeded() {
        if (System.currentTimeMillis() - lastUpdate < CACHE_DURATION) {
            return;
        }

        updateRankings();
        lastUpdate = System.currentTimeMillis();
    }

    /**
     * Força atualização de todos os rankings
     * Usa MongoDB para rankings globais, com fallback para jogadores online
     */
    public void updateRankings() {
        // Tentar usar MongoDB para rankings globais
        if (plugin.getProfileManager() != null && plugin.getProfileManager().getRepository() != null
                && plugin.getMongoManager() != null && plugin.getMongoManager().isConnected()) {
            updateFromMongoDB();
        } else {
            // Fallback para jogadores online
            updateFromOnlinePlayers();
        }
    }

    /**
     * Atualiza rankings usando queries do MongoDB
     */
    private void updateFromMongoDB() {
        try {
            MongoPlayerRepository repository = plugin.getProfileManager().getRepository();

            // Top Kills
            List<PlayerData> topKillsData = repository.getTopKills(100);
            topKills = buildRanking(topKillsData, "kills");

            // Top Coins
            List<PlayerData> topCoinsData = repository.getTopCoins(100);
            topCoins = buildRanking(topCoinsData, "coins");

            // Top ELO - buscar por customData.elo
            List<PlayerData> topEloData = repository.getTopByCustomField("elo", 100);
            topElo = buildRanking(topEloData, "elo");

            // Top Killstreak
            List<PlayerData> topKillstreakData = repository.getTopByField("highestKillStreak", 100);
            topKillstreak = buildRanking(topKillstreakData, "killstreak");

            // Top KDR - precisa calcular, então usa dados de kills
            topKdr = buildRanking(topKillsData, "kdr");

            plugin.getLogger().info("[Ranking] Rankings atualizados do MongoDB.");

        } catch (Exception e) {
            plugin.getLogger().warning("[Ranking] Erro ao atualizar do MongoDB, usando fallback: " + e.getMessage());
            updateFromOnlinePlayers();
        }
    }

    /**
     * Constrói um ranking a partir dos dados
     */
    private List<RankEntry> buildRanking(List<PlayerData> players, String type) {
        List<RankEntry> entries = new ArrayList<>();

        for (PlayerData data : players) {
            if (data == null || data.getLastKnownName() == null)
                continue;

            RankEntry entry;
            switch (type) {
                case "kills":
                    if (data.getKills() > 0) {
                        entry = new RankEntry(data.getLastKnownName(), data.getUuid(), data.getKills());
                        entries.add(entry);
                    }
                    break;
                case "elo":
                    int elo = getEloFromData(data);
                    if (elo > 1000) {
                        entry = new RankEntry(data.getLastKnownName(), data.getUuid(), elo);
                        entries.add(entry);
                    }
                    break;
                case "coins":
                    if (data.getCoins() > 0) {
                        entry = new RankEntry(data.getLastKnownName(), data.getUuid(), data.getCoins());
                        entries.add(entry);
                    }
                    break;
                case "kdr":
                    double kdr = data.getKDR();
                    if (kdr > 0 && data.getKills() >= 10) { // Mínimo 10 kills para entrar no ranking
                        entry = new RankEntry(data.getLastKnownName(), data.getUuid(), kdr);
                        entries.add(entry);
                    }
                    break;
                case "killstreak":
                    if (data.getHighestKillStreak() >= 5) {
                        entry = new RankEntry(data.getLastKnownName(), data.getUuid(), data.getHighestKillStreak());
                        entries.add(entry);
                    }
                    break;
            }
        }

        // Ordenar por valor (maior primeiro)
        if (type.equals("kdr")) {
            entries.sort((a, b) -> Double.compare(b.valueDouble, a.valueDouble));
        } else {
            entries.sort((a, b) -> Long.compare(b.value, a.value));
        }

        // Limitar a 100 entradas
        if (entries.size() > 100) {
            entries = entries.subList(0, 100);
        }

        return entries;
    }

    /**
     * Obtém ELO do PlayerData
     */
    private int getEloFromData(PlayerData data) {
        Object elo = data.getCustomData("elo");
        if (elo instanceof Number) {
            return ((Number) elo).intValue();
        }
        return 1000; // ELO inicial padrão
    }

    /**
     * Fallback: atualizar usando apenas jogadores online
     */
    private void updateFromOnlinePlayers() {
        List<RankEntry> kills = new ArrayList<>();
        List<RankEntry> elo = new ArrayList<>();
        List<RankEntry> coins = new ArrayList<>();
        List<RankEntry> kdr = new ArrayList<>();
        List<RankEntry> killstreak = new ArrayList<>();

        for (PlayerProfile profile : plugin.getProfileManager().getAllProfiles()) {
            if (profile == null)
                continue;
            PlayerData data = profile.getData();
            String name = profile.getName();
            UUID uuid = profile.getUniqueId();

            if (data.getKills() > 0) {
                kills.add(new RankEntry(name, uuid, data.getKills()));
            }

            int playerElo = plugin.getLeagueManager() != null ? plugin.getLeagueManager().getElo(profile.getPlayer())
                    : 1000;
            if (playerElo > 1000) {
                elo.add(new RankEntry(name, uuid, playerElo));
            }

            if (data.getCoins() > 0) {
                coins.add(new RankEntry(name, uuid, data.getCoins()));
            }

            if (data.getKDR() > 0 && data.getKills() >= 10) {
                kdr.add(new RankEntry(name, uuid, data.getKDR()));
            }

            if (data.getHighestKillStreak() >= 5) {
                killstreak.add(new RankEntry(name, uuid, data.getHighestKillStreak()));
            }
        }

        kills.sort((a, b) -> Long.compare(b.value, a.value));
        elo.sort((a, b) -> Long.compare(b.value, a.value));
        coins.sort((a, b) -> Long.compare(b.value, a.value));
        kdr.sort((a, b) -> Double.compare(b.valueDouble, a.valueDouble));
        killstreak.sort((a, b) -> Long.compare(b.value, a.value));

        this.topKills = kills;
        this.topElo = elo;
        this.topCoins = coins;
        this.topKdr = kdr;
        this.topKillstreak = killstreak;
    }

    // ==================== GETTERS ====================

    /**
     * Obtém top kills
     */
    public List<RankEntry> getTopKills(int limit) {
        updateIfNeeded();
        return topKills.subList(0, Math.min(limit, topKills.size()));
    }

    /**
     * Obtém top ELO
     */
    public List<RankEntry> getTopElo(int limit) {
        updateIfNeeded();
        return topElo.subList(0, Math.min(limit, topElo.size()));
    }

    /**
     * Obtém top coins
     */
    public List<RankEntry> getTopCoins(int limit) {
        updateIfNeeded();
        return topCoins.subList(0, Math.min(limit, topCoins.size()));
    }

    /**
     * Obtém top KDR
     */
    public List<RankEntry> getTopKdr(int limit) {
        updateIfNeeded();
        return topKdr.subList(0, Math.min(limit, topKdr.size()));
    }

    /**
     * Obtém top killstreak
     */
    public List<RankEntry> getTopKillstreak(int limit) {
        updateIfNeeded();
        return topKillstreak.subList(0, Math.min(limit, topKillstreak.size()));
    }

    /**
     * Obtém posição de um jogador em um ranking
     */
    public int getPosition(UUID uuid, String rankingType) {
        updateIfNeeded();

        List<RankEntry> ranking;
        switch (rankingType.toLowerCase()) {
            case "kills":
                ranking = topKills;
                break;
            case "elo":
                ranking = topElo;
                break;
            case "coins":
                ranking = topCoins;
                break;
            case "kdr":
                ranking = topKdr;
                break;
            case "killstreak":
                ranking = topKillstreak;
                break;
            default:
                return -1;
        }

        for (int i = 0; i < ranking.size(); i++) {
            if (ranking.get(i).uuid.equals(uuid)) {
                return i + 1;
            }
        }
        return -1; // Não está no ranking
    }

    /**
     * Força atualização do cache
     */
    public void forceUpdate() {
        updateRankings();
        lastUpdate = System.currentTimeMillis();
    }
}
