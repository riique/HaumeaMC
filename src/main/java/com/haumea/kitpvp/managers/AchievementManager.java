package com.haumea.kitpvp.managers;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.models.Achievement;
import com.haumea.kitpvp.models.PlayerData;
import com.haumea.kitpvp.profile.PlayerProfile;
import com.haumea.kitpvp.utils.ChatStorage;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Gerenciador de Conquistas do HaumeaMC.
 * 
 * Sistema de achievements que recompensa jogadores por marcos alcançados.
 * Conquistas são desbloqueadas automaticamente baseado em estatísticas.
 * 
 * @author HaumeaMC
 */
public class AchievementManager {

    private final HaumeaMC plugin;
    private final Map<String, Achievement> achievements;

    // Constantes de chave customData
    private static final String DATA_KEY = "achievements";

    public AchievementManager(HaumeaMC plugin) {
        this.plugin = plugin;
        this.achievements = new LinkedHashMap<>();
        registerAchievements();
    }

    /**
     * Registra todas as conquistas disponíveis
     */
    private void registerAchievements() {

        // ╔══════════════════════════════════════════════════════════════════╗
        // ║ CONQUISTAS DE KILLS (10) ║
        // ╚══════════════════════════════════════════════════════════════════╝
        register(new Achievement("first_blood", "§c§lFirst Blood", "§7Mate seu primeiro jogador",
                "IRON_SWORD", 100, Achievement.Type.KILLS, 1));
        register(new Achievement("novice_killer", "§e§lNovato", "§7Mate 25 jogadores",
                "IRON_SWORD", 250, Achievement.Type.KILLS, 25));
        register(new Achievement("hunter", "§e§lCacador", "§7Mate 50 jogadores",
                "GOLD_SWORD", 500, Achievement.Type.KILLS, 50));
        register(new Achievement("slayer", "§6§lSlayer", "§7Mate 100 jogadores",
                "GOLD_SWORD", 1000, Achievement.Type.KILLS, 100));
        register(new Achievement("warrior", "§6§lGuerreiro", "§7Mate 250 jogadores",
                "DIAMOND_SWORD", 1500, Achievement.Type.KILLS, 250));
        register(new Achievement("assassin", "§c§lAssassino", "§7Mate 500 jogadores",
                "DIAMOND_SWORD", 2500, Achievement.Type.KILLS, 500));
        register(new Achievement("executioner", "§4§lCarrasco", "§7Mate 1000 jogadores",
                "DIAMOND_SWORD", 5000, Achievement.Type.KILLS, 1000));
        register(new Achievement("warlord", "§4§lSenhor da Guerra", "§7Mate 2500 jogadores",
                "DIAMOND_SWORD", 10000, Achievement.Type.KILLS, 2500));
        register(new Achievement("legend", "§d§lLenda", "§7Mate 5000 jogadores",
                "NETHER_STAR", 25000, Achievement.Type.KILLS, 5000));
        register(new Achievement("god_of_war", "§5§lDeus da Guerra", "§7Mate 10000 jogadores",
                "NETHER_STAR", 50000, Achievement.Type.KILLS, 10000));

        // ╔══════════════════════════════════════════════════════════════════╗
        // ║ CONQUISTAS DE KILLSTREAK (8) ║
        // ╚══════════════════════════════════════════════════════════════════╝
        register(new Achievement("triple_kill", "§e§lTriple Kill", "§7Alcance 3 kills sem morrer",
                "BLAZE_POWDER", 150, Achievement.Type.KILLSTREAK, 3));
        register(new Achievement("rampage", "§e§lRampage", "§7Alcance 5 kills sem morrer",
                "BLAZE_POWDER", 300, Achievement.Type.KILLSTREAK, 5));
        register(new Achievement("unstoppable", "§6§lImparavel", "§7Alcance 10 kills sem morrer",
                "MAGMA_CREAM", 750, Achievement.Type.KILLSTREAK, 10));
        register(new Achievement("dominating", "§6§lDominando", "§7Alcance 15 kills sem morrer",
                "MAGMA_CREAM", 1200, Achievement.Type.KILLSTREAK, 15));
        register(new Achievement("godlike", "§c§lGodlike", "§7Alcance 20 kills sem morrer",
                "GOLDEN_APPLE", 2000, Achievement.Type.KILLSTREAK, 20));
        register(new Achievement("legendary", "§c§lLendario", "§7Alcance 30 kills sem morrer",
                "GOLDEN_APPLE", 4000, Achievement.Type.KILLSTREAK, 30));
        register(new Achievement("immortal", "§5§lImortal", "§7Alcance 50 kills sem morrer",
                "ENCHANTED_BOOK", 10000, Achievement.Type.KILLSTREAK, 50));
        register(new Achievement("beyond_godlike", "§d§lAlem de Deus", "§7Alcance 100 kills sem morrer",
                "NETHER_STAR", 50000, Achievement.Type.KILLSTREAK, 100));

        // ╔══════════════════════════════════════════════════════════════════╗
        // ║ CONQUISTAS DE COINS (6) ║
        // ╚══════════════════════════════════════════════════════════════════╝
        register(new Achievement("saver", "§e§lEconomico", "§7Acumule 5.000 coins",
                "GOLD_NUGGET", 0, Achievement.Type.COINS, 5000));
        register(new Achievement("wealthy", "§e§lRico", "§7Acumule 10.000 coins",
                "GOLD_INGOT", 0, Achievement.Type.COINS, 10000));
        register(new Achievement("rich", "§6§lEndinheirado", "§7Acumule 50.000 coins",
                "GOLD_INGOT", 0, Achievement.Type.COINS, 50000));
        register(new Achievement("millionaire", "§6§lMilionario", "§7Acumule 100.000 coins",
                "GOLD_BLOCK", 0, Achievement.Type.COINS, 100000));
        register(new Achievement("billionaire", "§a§lBilionario", "§7Acumule 500.000 coins",
                "GOLD_BLOCK", 0, Achievement.Type.COINS, 500000));
        register(new Achievement("tycoon", "§a§lMagnata", "§7Acumule 1.000.000 coins",
                "DIAMOND_BLOCK", 0, Achievement.Type.COINS, 1000000));

        // ╔══════════════════════════════════════════════════════════════════╗
        // ║ CONQUISTAS DE ELO (12) ║
        // ╚══════════════════════════════════════════════════════════════════╝
        register(new Achievement("bronze_player", "§8§lBronze", "§7Alcance a liga Bronze",
                "BRICK", 200, Achievement.Type.ELO, 1100));
        register(new Achievement("silver_player", "§7§lPrata", "§7Alcance a liga Silver",
                "IRON_INGOT", 500, Achievement.Type.ELO, 1300));
        register(new Achievement("gold_player", "§6§lOuro", "§7Alcance a liga Gold",
                "GOLD_INGOT", 1000, Achievement.Type.ELO, 1600));
        register(new Achievement("diamond_player", "§b§lDiamante", "§7Alcance a liga Diamond",
                "DIAMOND", 2000, Achievement.Type.ELO, 2000));
        register(new Achievement("emerald_player", "§a§lEsmeralda", "§7Alcance a liga Emerald",
                "EMERALD", 3500, Achievement.Type.ELO, 2500));
        register(new Achievement("master_player", "§d§lMestre", "§7Alcance a liga Master",
                "ENDER_PEARL", 5000, Achievement.Type.ELO, 3100));
        register(new Achievement("legendary_player", "§c§lLendario", "§7Alcance a liga Legendary",
                "NETHER_STAR", 10000, Achievement.Type.ELO, 3800));
        register(new Achievement("elo_1500", "§e§lEstrela Nascente", "§7Alcance 1500 ELO",
                "FIREWORK", 300, Achievement.Type.ELO, 1500));
        register(new Achievement("elo_2000", "§e§lCompetidor", "§7Alcance 2000 ELO",
                "FIREWORK", 600, Achievement.Type.ELO, 2000));
        register(new Achievement("elo_2500", "§6§lElite", "§7Alcance 2500 ELO",
                "EYE_OF_ENDER", 1500, Achievement.Type.ELO, 2500));
        register(new Achievement("elo_3000", "§c§lCampeao", "§7Alcance 3000 ELO",
                "DRAGON_EGG", 3000, Achievement.Type.ELO, 3000));
        register(new Achievement("elo_4000", "§5§lLenda Viva", "§7Alcance 4000 ELO",
                "NETHER_STAR", 10000, Achievement.Type.ELO, 4000));

        // ╔══════════════════════════════════════════════════════════════════╗
        // ║ CONQUISTAS DE DUELOS (12) ║
        // ╚══════════════════════════════════════════════════════════════════╝
        register(new Achievement("first_duel", "§a§lPrimeiro Duelo", "§7Venca seu primeiro duelo",
                "IRON_SWORD", 150, Achievement.Type.DUELS, 1));
        register(new Achievement("duelist", "§a§lDuelista", "§7Venca 10 duelos",
                "IRON_SWORD", 400, Achievement.Type.DUELS, 10));
        register(new Achievement("gladiator", "§e§lGladiador", "§7Venca 25 duelos",
                "GOLD_SWORD", 750, Achievement.Type.DUELS, 25));
        register(new Achievement("champion", "§e§lCampeao", "§7Venca 50 duelos",
                "GOLD_SWORD", 1500, Achievement.Type.DUELS, 50));
        register(new Achievement("arena_master", "§6§lMestre da Arena", "§7Venca 100 duelos",
                "DIAMOND_SWORD", 3000, Achievement.Type.DUELS, 100));
        register(new Achievement("undefeated", "§c§lInvicto", "§7Venca 250 duelos",
                "DIAMOND_SWORD", 7500, Achievement.Type.DUELS, 250));
        register(new Achievement("duel_legend", "§d§lLenda dos Duelos", "§7Venca 500 duelos",
                "NETHER_STAR", 15000, Achievement.Type.DUELS, 500));
        register(new Achievement("duel_god", "§5§lDeus dos Duelos", "§7Venca 1000 duelos",
                "NETHER_STAR", 35000, Achievement.Type.DUELS, 1000));
        // Vitórias consecutivas (especiais)
        register(new Achievement("duel_streak_3", "§a§lTriplice", "§7Venca 3 duelos seguidos",
                "TRIPWIRE_HOOK", 300, Achievement.Type.SPECIAL, 3));
        register(new Achievement("duel_streak_5", "§e§lPenta", "§7Venca 5 duelos seguidos",
                "BLAZE_ROD", 750, Achievement.Type.SPECIAL, 5));
        register(new Achievement("duel_streak_10", "§c§lDominio Total", "§7Venca 10 duelos seguidos",
                "END_CRYSTAL", 2500, Achievement.Type.SPECIAL, 10));
        register(new Achievement("duel_streak_20", "§d§lInvencivel", "§7Venca 20 duelos seguidos",
                "NETHER_STAR", 10000, Achievement.Type.SPECIAL, 20));

        // ╔══════════════════════════════════════════════════════════════════╗
        // ║ CONQUISTAS DE TEMPO JOGADO (8) ║
        // ╚══════════════════════════════════════════════════════════════════╝
        register(new Achievement("newcomer", "§a§lRecém-Chegado", "§7Jogue por 1 hora",
                "WATCH", 100, Achievement.Type.PLAYTIME, 1));
        register(new Achievement("regular", "§a§lFrequentador", "§7Jogue por 5 horas",
                "WATCH", 300, Achievement.Type.PLAYTIME, 5));
        register(new Achievement("veteran", "§e§lVeterano", "§7Jogue por 24 horas",
                "COMPASS", 1000, Achievement.Type.PLAYTIME, 24));
        register(new Achievement("dedicated", "§e§lDedicado", "§7Jogue por 50 horas",
                "COMPASS", 2000, Achievement.Type.PLAYTIME, 50));
        register(new Achievement("addict", "§6§lViciado", "§7Jogue por 100 horas",
                "REDSTONE_BLOCK", 5000, Achievement.Type.PLAYTIME, 100));
        register(new Achievement("no_life", "§6§lSem Vida Social", "§7Jogue por 250 horas",
                "REDSTONE_BLOCK", 10000, Achievement.Type.PLAYTIME, 250));
        register(new Achievement("eternal", "§c§lEterno", "§7Jogue por 500 horas",
                "BEACON", 25000, Achievement.Type.PLAYTIME, 500));
        register(new Achievement("immortalized", "§d§lImortalizado", "§7Jogue por 1000 horas",
                "NETHER_STAR", 100000, Achievement.Type.PLAYTIME, 1000));

        // ╔══════════════════════════════════════════════════════════════════╗
        // ║ CONQUISTAS DE MORTES - COMICAS (8) ║
        // ╚══════════════════════════════════════════════════════════════════╝
        register(new Achievement("first_death", "§7§lBem-vindo ao KitPvP", "§7Morra pela primeira vez",
                "BONE", 50, Achievement.Type.DEATHS, 1));
        register(new Achievement("learning", "§7§lAprendendo", "§7Morra 10 vezes",
                "BONE", 100, Achievement.Type.DEATHS, 10));
        register(new Achievement("persistent", "§e§lPersistente", "§7Morra 50 vezes",
                "SKULL_ITEM", 200, Achievement.Type.DEATHS, 50));
        register(new Achievement("resilient", "§e§lResilient", "§7Morra 100 vezes",
                "SKULL_ITEM", 400, Achievement.Type.DEATHS, 100));
        register(new Achievement("never_give_up", "§6§lNunca Desista", "§7Morra 250 vezes",
                "SKULL_ITEM", 750, Achievement.Type.DEATHS, 250));
        register(new Achievement("respawn_king", "§6§lRei do Respawn", "§7Morra 500 vezes",
                "SKULL_ITEM", 1500, Achievement.Type.DEATHS, 500));
        register(new Achievement("immortal_soul", "§c§lAlma Imortal", "§7Morra 1000 vezes",
                "SOUL_SAND", 3000, Achievement.Type.DEATHS, 1000));
        register(new Achievement("death_incarnate", "§5§lMorte Encarnada", "§7Morra 2500 vezes",
                "WITHER_SKELETON_SKULL", 7500, Achievement.Type.DEATHS, 2500));

        // ╔══════════════════════════════════════════════════════════════════╗
        // ║ CONQUISTAS DE DESAFIOS DIARIOS (6) ║
        // ╚══════════════════════════════════════════════════════════════════╝
        register(new Achievement("first_challenge", "§a§lPrimeiro Desafio", "§7Complete seu primeiro desafio diario",
                "BOOK", 100, Achievement.Type.DAILY, 1));
        register(new Achievement("challenger", "§a§lDesafiador", "§7Complete 10 desafios diarios",
                "BOOK", 400, Achievement.Type.DAILY, 10));
        register(new Achievement("daily_warrior", "§e§lGuerreiro Diario", "§7Complete 50 desafios diarios",
                "ENCHANTED_BOOK", 1500, Achievement.Type.DAILY, 50));
        register(new Achievement("mission_master", "§6§lMestre das Missoes", "§7Complete 100 desafios diarios",
                "ENCHANTED_BOOK", 3500, Achievement.Type.DAILY, 100));
        register(new Achievement("daily_legend", "§c§lLenda Diaria", "§7Complete 250 desafios diarios",
                "WRITTEN_BOOK", 10000, Achievement.Type.DAILY, 250));
        register(new Achievement("daily_god", "§d§lDeus dos Desafios", "§7Complete 500 desafios diarios",
                "NETHER_STAR", 25000, Achievement.Type.DAILY, 500));

        // ╔══════════════════════════════════════════════════════════════════╗
        // ║ CONQUISTAS DE EVENTOS (10) ║
        // ╚══════════════════════════════════════════════════════════════════╝
        register(new Achievement("first_event", "§a§lPrimeiro Evento", "§7Participe de seu primeiro evento de chat",
                "PAPER", 75, Achievement.Type.EVENTS, 1));
        register(new Achievement("event_player", "§a§lJogador de Eventos", "§7Venca 10 eventos de chat",
                "PAPER", 300, Achievement.Type.EVENTS, 10));
        register(new Achievement("quick_thinker", "§e§lPensador Rapido", "§7Venca 25 eventos de chat",
                "BOOK_AND_QUILL", 600, Achievement.Type.EVENTS, 25));
        register(new Achievement("event_master", "§e§lMestre dos Eventos", "§7Venca 50 eventos de chat",
                "BOOK_AND_QUILL", 1200, Achievement.Type.EVENTS, 50));
        register(new Achievement("trivia_king", "§6§lRei do Trivia", "§7Venca 100 eventos de chat",
                "ENCHANTED_BOOK", 2500, Achievement.Type.EVENTS, 100));
        register(new Achievement("event_legend", "§c§lLenda dos Eventos", "§7Venca 250 eventos de chat",
                "NETHER_STAR", 7500, Achievement.Type.EVENTS, 250));
        // Feast events
        register(new Achievement("feast_survivor", "§6§lSobrevivente do Feast", "§7Colete um bau no Feast",
                "CHEST", 200, Achievement.Type.SPECIAL, 1));
        register(new Achievement("feast_hunter", "§6§lCacador do Feast", "§7Colete 10 baus no Feast",
                "CHEST", 800, Achievement.Type.SPECIAL, 10));
        register(new Achievement("feast_champion", "§c§lCampeao do Feast", "§7Colete 50 baus no Feast",
                "TRAPPED_CHEST", 3000, Achievement.Type.SPECIAL, 50));
        register(new Achievement("feast_dominator", "§d§lDominador do Feast", "§7Colete 100 baus no Feast",
                "ENDER_CHEST", 7500, Achievement.Type.SPECIAL, 100));

        // ╔══════════════════════════════════════════════════════════════════╗
        // ║ CONQUISTAS ESPECIAIS CRIATIVAS (20) ║
        // ╚══════════════════════════════════════════════════════════════════╝

        // Bounty
        register(new Achievement("bounty_hunter", "§c§lCacador de Recompensas", "§7Mate 10 jogadores com bounty",
                "SKULL_ITEM", 3000, Achievement.Type.SPECIAL, 10));
        register(new Achievement("bounty_master", "§c§lMestre Cacador", "§7Mate 50 jogadores com bounty",
                "SKULL_ITEM", 10000, Achievement.Type.SPECIAL, 50));
        register(new Achievement("bounty_legend", "§5§lLenda da Caca", "§7Mate 100 jogadores com bounty",
                "WITHER_SKELETON_SKULL", 25000, Achievement.Type.SPECIAL, 100));

        // Pagamentos/Generosidade
        register(new Achievement("generous", "§a§lGeneroso", "§7Envie coins para 10 jogadores diferentes",
                "EMERALD", 500, Achievement.Type.SPECIAL, 10));
        register(new Achievement("philanthropist", "§a§lFilantropo", "§7Envie coins para 50 jogadores diferentes",
                "EMERALD_BLOCK", 2000, Achievement.Type.SPECIAL, 50));
        register(new Achievement("receiver", "§e§lBeneficiario", "§7Receba coins de 10 jogadores diferentes",
                "GOLD_INGOT", 250, Achievement.Type.SPECIAL, 10));

        // Reports
        register(new Achievement("vigilante", "§e§lVigilante", "§7Envie 5 reports validos",
                "PAPER", 250, Achievement.Type.SPECIAL, 5));
        register(new Achievement("community_guardian", "§6§lGuardiao da Comunidade", "§7Envie 25 reports validos",
                "SHIELD", 1000, Achievement.Type.SPECIAL, 25));

        // Combate especial
        register(new Achievement("streak_breaker", "§c§lQuebra-Streaks", "§7Encerre 10 killstreaks de 5+",
                "REDSTONE", 1500, Achievement.Type.SPECIAL, 10));
        register(new Achievement("giant_slayer", "§4§lMata-Gigantes", "§7Encerre 25 killstreaks de 10+",
                "DIAMOND_AXE", 5000, Achievement.Type.SPECIAL, 25));
        register(new Achievement("underdog", "§e§lAzarao", "§7Mate alguem com ELO 500+ acima do seu",
                "GOLDEN_APPLE", 1000, Achievement.Type.SPECIAL, 1));
        register(new Achievement("underdog_master", "§6§lMestre Azarao", "§7Mate 10 jogadores com ELO 500+ acima",
                "ENCHANTED_BOOK", 5000, Achievement.Type.SPECIAL, 10));

        // Kits
        register(new Achievement("kit_explorer", "§b§lExplorador de Kits", "§7Use 5 kits diferentes",
                "LEATHER_CHESTPLATE", 300, Achievement.Type.SPECIAL, 5));
        register(new Achievement("kit_collector", "§b§lColecionador de Kits", "§7Desbloqueie 10 kits",
                "IRON_CHESTPLATE", 1000, Achievement.Type.SPECIAL, 10));
        register(new Achievement("kit_master", "§6§lMestre dos Kits", "§7Desbloqueie todos os kits",
                "DIAMOND_CHESTPLATE", 5000, Achievement.Type.SPECIAL, 1));

        // Social
        register(new Achievement("social_butterfly", "§d§lBorboleta Social", "§7Adicione 10 amigos",
                "RED_ROSE", 200, Achievement.Type.SPECIAL, 10));
        register(new Achievement("popular", "§d§lPopular", "§7Adicione 50 amigos",
                "RED_ROSE", 750, Achievement.Type.SPECIAL, 50));

        // Comebacks
        register(new Achievement("comeback_king", "§e§lRei do Comeback", "§7Alcance 5 kills apos voltar de 0",
                "GOLDEN_APPLE", 500, Achievement.Type.SPECIAL, 5));
        register(new Achievement("phoenix", "§c§lFenix", "§7Alcance 10 kills apos morrer em killstreak de 10+",
                "BLAZE_POWDER", 2000, Achievement.Type.SPECIAL, 1));

        // Especiais raras
        register(new Achievement("lucky", "§a§lSortudo", "§7Ganhe um evento de chat como 1° lugar 3x seguidas",
                "EMERALD", 3000, Achievement.Type.SPECIAL, 3));

        plugin.getLogger().info("[Achievements] " + achievements.size() + " conquistas registradas.");
    }

    /**
     * Registra uma conquista
     */
    private void register(Achievement achievement) {
        achievements.put(achievement.getId(), achievement);
    }

    /**
     * Obtém uma conquista por ID
     */
    public Achievement getAchievement(String id) {
        return achievements.get(id);
    }

    /**
     * Obtém todas as conquistas
     */
    public Collection<Achievement> getAllAchievements() {
        return achievements.values();
    }

    /**
     * Verifica se um jogador tem uma conquista
     */
    public boolean hasAchievement(Player player, String achievementId) {
        PlayerProfile profile = plugin.getProfileManager().getProfile(player);
        if (profile == null)
            return false;

        Set<String> unlocked = getUnlockedAchievements(profile.getData());
        return unlocked.contains(achievementId);
    }

    /**
     * Desbloqueia uma conquista para o jogador
     */
    public boolean unlockAchievement(Player player, String achievementId) {
        if (hasAchievement(player, achievementId)) {
            return false; // Já desbloqueada
        }

        Achievement achievement = achievements.get(achievementId);
        if (achievement == null) {
            return false;
        }

        PlayerProfile profile = plugin.getProfileManager().getProfile(player);
        if (profile == null)
            return false;

        // Adicionar à lista de desbloqueadas
        Set<String> unlocked = getUnlockedAchievements(profile.getData());
        unlocked.add(achievementId);
        saveUnlockedAchievements(profile.getData(), unlocked);

        // Dar recompensa em coins
        if (achievement.getReward() > 0) {
            profile.addCoins(achievement.getReward());
        }

        // Notificar jogador
        notifyUnlock(player, achievement);

        return true;
    }

    /**
     * Notifica o jogador sobre conquista desbloqueada
     */
    private void notifyUnlock(Player player, Achievement achievement) {
        player.sendMessage("");
        ChatStorage.sendRaw(player, "&8&m----------------------------------------");
        ChatStorage.sendRaw(player, "");
        ChatStorage.sendRaw(player, "  &6&l★ CONQUISTA DESBLOQUEADA! ★");
        ChatStorage.sendRaw(player, "");
        ChatStorage.sendRaw(player, "  " + achievement.getDisplayName());
        ChatStorage.sendRaw(player, "  " + achievement.getDescription());
        if (achievement.getReward() > 0) {
            ChatStorage.sendRaw(player, "");
            ChatStorage.sendRaw(player,
                    "  &aRecompensa: &e" + ChatStorage.formatNumber(achievement.getReward()) + " coins");
        }
        ChatStorage.sendRaw(player, "");
        ChatStorage.sendRaw(player, "&8&m----------------------------------------");
        player.sendMessage("");

        // Som de conquista
        player.playSound(player.getLocation(), Sound.LEVEL_UP, 1.0f, 1.5f);
    }

    /**
     * Verifica e atualiza conquistas para um jogador
     * Chamado quando estatísticas mudam
     */
    public void checkAchievements(Player player) {
        PlayerProfile profile = plugin.getProfileManager().getProfile(player);
        if (profile == null)
            return;

        PlayerData data = profile.getData();
        int kills = data.getKills();
        int deaths = data.getDeaths();
        int killstreak = data.getHighestKillStreak();
        long coins = data.getCoins();
        int elo = plugin.getLeagueManager() != null ? plugin.getLeagueManager().getElo(player) : 0;

        // Obter progresso de tipos especiais via customData
        int duelWins = data.getCustomData("duel_wins", 0);
        int playtimeHours = (int) (data.getPlayTime() / 3600000L); // ms -> horas
        int dailyChallenges = data.getCustomData("daily_challenges_completed", 0);
        int eventsWon = data.getCustomData("chat_events_won", 0);

        for (Achievement achievement : achievements.values()) {
            if (hasAchievement(player, achievement.getId())) {
                continue; // Já desbloqueada
            }

            boolean shouldUnlock = false;

            switch (achievement.getType()) {
                case KILLS:
                    shouldUnlock = kills >= achievement.getRequirement();
                    break;
                case KILLSTREAK:
                    shouldUnlock = killstreak >= achievement.getRequirement();
                    break;
                case COINS:
                    shouldUnlock = coins >= achievement.getRequirement();
                    break;
                case ELO:
                    shouldUnlock = elo >= achievement.getRequirement();
                    break;
                case DUELS:
                    shouldUnlock = duelWins >= achievement.getRequirement();
                    break;
                case PLAYTIME:
                    shouldUnlock = playtimeHours >= achievement.getRequirement();
                    break;
                case DEATHS:
                    shouldUnlock = deaths >= achievement.getRequirement();
                    break;
                case DAILY:
                    shouldUnlock = dailyChallenges >= achievement.getRequirement();
                    break;
                case EVENTS:
                    shouldUnlock = eventsWon >= achievement.getRequirement();
                    break;
                case SPECIAL:
                    // Conquistas especiais são desbloqueadas manualmente
                    break;
            }

            if (shouldUnlock) {
                unlockAchievement(player, achievement.getId());
            }
        }
    }

    /**
     * Incrementa progresso de conquista especial
     */
    public void incrementSpecialAchievement(Player player, String achievementId) {
        PlayerProfile profile = plugin.getProfileManager().getProfile(player);
        if (profile == null)
            return;

        Achievement achievement = achievements.get(achievementId);
        if (achievement == null || achievement.getType() != Achievement.Type.SPECIAL)
            return;

        String progressKey = "achievement_progress_" + achievementId;
        int current = profile.getData().getCustomData(progressKey, 0);
        current++;
        profile.getData().setCustomData(progressKey, current);

        if (current >= achievement.getRequirement()) {
            unlockAchievement(player, achievementId);
        }
    }

    /**
     * Obtém progresso de uma conquista
     */
    public int getProgress(Player player, Achievement achievement) {
        PlayerProfile profile = plugin.getProfileManager().getProfile(player);
        if (profile == null)
            return 0;

        PlayerData data = profile.getData();

        switch (achievement.getType()) {
            case KILLS:
                return data.getKills();
            case KILLSTREAK:
                return data.getHighestKillStreak();
            case COINS:
                return (int) Math.min(data.getCoins(), Integer.MAX_VALUE);
            case ELO:
                return plugin.getLeagueManager() != null ? plugin.getLeagueManager().getElo(player) : 0;
            case DUELS:
                return data.getCustomData("duel_wins", 0);
            case PLAYTIME:
                return (int) (data.getPlayTime() / 3600000L); // ms -> horas
            case DEATHS:
                return data.getDeaths();
            case DAILY:
                return data.getCustomData("daily_challenges_completed", 0);
            case EVENTS:
                return data.getCustomData("chat_events_won", 0);
            case SPECIAL:
                String progressKey = "achievement_progress_" + achievement.getId();
                return data.getCustomData(progressKey, 0);
            default:
                return 0;
        }
    }

    /**
     * Obtém contagem de conquistas desbloqueadas
     */
    public int getUnlockedCount(Player player) {
        PlayerProfile profile = plugin.getProfileManager().getProfile(player);
        if (profile == null)
            return 0;
        return getUnlockedAchievements(profile.getData()).size();
    }

    /**
     * Obtém total de conquistas disponíveis
     */
    public int getTotalCount() {
        return achievements.size();
    }

    /**
     * Obtém conquistas desbloqueadas do PlayerData
     */
    @SuppressWarnings("unchecked")
    private Set<String> getUnlockedAchievements(PlayerData data) {
        Object obj = data.getCustomData(DATA_KEY);
        if (obj instanceof Collection) {
            return new HashSet<>((Collection<String>) obj);
        }
        return new HashSet<>();
    }

    /**
     * Salva conquistas desbloqueadas no PlayerData
     */
    private void saveUnlockedAchievements(PlayerData data, Set<String> unlocked) {
        data.setCustomData(DATA_KEY, new ArrayList<>(unlocked));
    }
}
