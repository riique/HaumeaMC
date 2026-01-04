package com.haumea.kitpvp.managers;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.models.PlayerData;
import com.haumea.kitpvp.models.PlayerRank;
import com.haumea.kitpvp.profile.PlayerProfile;
import com.haumea.kitpvp.utils.ChatStorage;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * API centralizada de estatísticas do HaumeaMC.
 * 
 * Esta classe fornece uma API unificada para manipular todas as
 * estatísticas dos jogadores: kills, deaths, killstreak, ELO, money, etc.
 * 
 * Em vez de ter várias APIs separadas (DeathAPI, MoneyAPI, etc.),
 * este manager centraliza todas as operações de estatísticas.
 * 
 * Características:
 * - Null-safe: Todos os métodos verificam nulidade
 * - Eficiente: Usa o cache do ProfileManager (O(1))
 * - Sistema de Ligas: Ranks baseados em ELO (delegado ao LeagueManager)
 * - Sincronização: Killstreak sincronizado com level do Minecraft
 * 
 * @author HaumeaMC
 */
public class StatsManager {

    private final HaumeaMC plugin;

    // Configurações de economia
    private static final long COINS_PER_KILL = 25;
    private static final long COINS_BONUS_PER_KILLSTREAK = 5;

    /**
     * Construtor do StatsManager
     * 
     * @param plugin Instância do plugin principal
     */
    public StatsManager(HaumeaMC plugin) {
        this.plugin = plugin;
    }

    // ==================== MÉTODOS DE KILL ====================

    /**
     * Registra uma kill para o jogador.
     * Automaticamente:
     * - Adiciona +1 kill
     * - Incrementa killstreak
     * - Atualiza maxKillstreak se necessário
     * - Adiciona ELO (via LeagueManager)
     * - Adiciona coins (com bônus de killstreak E multiplicador)
     * - Sincroniza level do Minecraft
     * 
     * @param player Jogador que fez a kill
     */
    public void addKill(Player player) {
        if (player == null)
            return;

        PlayerProfile profile = getProfile(player);
        if (profile == null)
            return;

        PlayerData data = profile.getData();

        // Incrementar kills e killstreak
        data.addKills(1);

        int currentStreak = data.getKillStreak();

        // Calcular coins com bônus de killstreak
        long baseCoins = COINS_PER_KILL + (currentStreak * COINS_BONUS_PER_KILLSTREAK);

        // Aplicar multiplicador de coins (se ativo)
        long finalCoins = baseCoins;
        MultiplierManager multiplierManager = plugin.getMultiplierManager();
        if (multiplierManager != null) {
            finalCoins = multiplierManager.applyMultiplier(player, baseCoins);
        }

        // Aplicar coins
        addMoney(player, finalCoins);

        // Sincronizar level do Minecraft com killstreak
        syncMinecraftLevel(player, currentStreak);

        // Marcar scoreboard como dirty para atualização
        markScoreboardDirty(player);
    }

    /**
     * Obtém o total de kills de um jogador
     * 
     * @param player Jogador
     * @return Total de kills ou 0
     */
    public int getKills(Player player) {
        PlayerProfile profile = getProfile(player);
        return profile != null ? profile.getKills() : 0;
    }

    /**
     * Define o total de kills de um jogador
     * 
     * @param player Jogador
     * @param kills  Novo valor de kills
     */
    public void setKills(Player player, int kills) {
        if (player == null)
            return;
        PlayerProfile profile = getProfile(player);
        if (profile != null) {
            profile.getData().setKills(Math.max(0, kills));
        }
    }

    // ==================== MÉTODOS DE DEATH ====================

    /**
     * Registra uma morte para o jogador.
     * Automaticamente:
     * - Adiciona +1 death
     * - Reseta killstreak para 0
     * - Perda de ELO é tratada no CombatListener via LeagueManager
     * - Reseta level do Minecraft para 0
     * 
     * @param player Jogador que morreu
     */
    public void addDeath(Player player) {
        if (player == null)
            return;

        PlayerProfile profile = getProfile(player);
        if (profile == null)
            return;

        PlayerData data = profile.getData();

        // Incrementar deaths e resetar killstreak
        data.addDeaths(1);

        // Resetar level do Minecraft
        syncMinecraftLevel(player, 0);

        // Marcar scoreboard como dirty para atualização
        markScoreboardDirty(player);
    }

    /**
     * Obtém o total de mortes de um jogador
     * 
     * @param player Jogador
     * @return Total de mortes ou 0
     */
    public int getDeaths(Player player) {
        PlayerProfile profile = getProfile(player);
        return profile != null ? profile.getDeaths() : 0;
    }

    /**
     * Define o total de mortes de um jogador
     * 
     * @param player Jogador
     * @param deaths Novo valor de mortes
     */
    public void setDeaths(Player player, int deaths) {
        if (player == null)
            return;
        PlayerProfile profile = getProfile(player);
        if (profile != null) {
            profile.getData().setDeaths(Math.max(0, deaths));
        }
    }

    // ==================== MÉTODOS DE KILLSTREAK ====================

    /**
     * Obtém o killstreak atual do jogador
     * 
     * @param player Jogador
     * @return Killstreak atual ou 0
     */
    public int getKillstreak(Player player) {
        PlayerProfile profile = getProfile(player);
        return profile != null ? profile.getKillStreak() : 0;
    }

    /**
     * Obtém o maior killstreak já alcançado pelo jogador
     * 
     * @param player Jogador
     * @return Maior killstreak ou 0
     */
    public int getMaxKillstreak(Player player) {
        PlayerProfile profile = getProfile(player);
        return profile != null ? profile.getData().getHighestKillStreak() : 0;
    }

    /**
     * Define o killstreak do jogador
     * Atualiza maxKillstreak se necessário
     * 
     * @param player     Jogador
     * @param killstreak Novo valor
     */
    public void setKillstreak(Player player, int killstreak) {
        if (player == null)
            return;
        PlayerProfile profile = getProfile(player);
        if (profile != null) {
            PlayerData data = profile.getData();
            killstreak = Math.max(0, killstreak);
            data.setKillStreak(killstreak);
            if (killstreak > data.getHighestKillStreak()) {
                data.setHighestKillStreak(killstreak);
            }
            syncMinecraftLevel(player, killstreak);
        }
    }

    /**
     * Reseta o killstreak do jogador para 0
     * 
     * @param player Jogador
     */
    public void resetKillstreak(Player player) {
        setKillstreak(player, 0);
    }

    // ==================== MÉTODOS DE ELO ====================

    /**
     * Obtém o ELO do jogador
     * Delega ao LeagueManager para consistência
     * 
     * @param player Jogador
     * @return ELO atual ou valor inicial
     */
    public int getElo(Player player) {
        LeagueManager leagueManager = plugin.getLeagueManager();
        if (leagueManager == null) {
            return 100; // Valor inicial padrão
        }
        return leagueManager.getElo(player);
    }

    /**
     * Obtém o ELO de um jogador por UUID (para jogadores offline)
     * Delega ao LeagueManager para consistência
     * 
     * @param uuid UUID do jogador
     * @return ELO ou valor inicial
     */
    public int getElo(UUID uuid) {
        LeagueManager leagueManager = plugin.getLeagueManager();
        if (leagueManager == null) {
            return 100; // Valor inicial padrão
        }
        return leagueManager.getElo(uuid);
    }

    /**
     * Define o ELO do jogador
     * Delega ao LeagueManager para consistência
     * 
     * @param player Jogador
     * @param elo    Novo valor de ELO
     */
    public void setElo(Player player, int elo) {
        LeagueManager leagueManager = plugin.getLeagueManager();
        if (leagueManager != null) {
            leagueManager.setElo(player, elo);
        }
    }

    /**
     * Adiciona ELO ao jogador
     * Delega ao LeagueManager para consistência
     * 
     * @param player Jogador
     * @param amount Quantidade a adicionar
     * @return true se houve promoção de rank
     */
    public boolean addElo(Player player, int amount) {
        LeagueManager leagueManager = plugin.getLeagueManager();
        if (leagueManager != null) {
            return leagueManager.addElo(player, amount);
        }
        return false;
    }

    /**
     * Remove ELO do jogador
     * Delega ao LeagueManager para consistência
     * 
     * @param player Jogador
     * @param amount Quantidade a remover
     * @return true se houve rebaixamento de rank
     */
    public boolean removeElo(Player player, int amount) {
        LeagueManager leagueManager = plugin.getLeagueManager();
        if (leagueManager != null) {
            return leagueManager.removeElo(player, amount);
        }
        return false;
    }

    // ==================== MÉTODOS DE DINHEIRO (MONEY) ====================

    /**
     * Obtém o saldo de moedas do jogador
     * 
     * @param player Jogador
     * @return Saldo de moedas ou 0
     */
    public long getMoney(Player player) {
        PlayerProfile profile = getProfile(player);
        return profile != null ? profile.getCoins() : 0;
    }

    /**
     * Define o saldo de moedas do jogador
     * 
     * @param player Jogador
     * @param amount Novo saldo
     */
    public void setMoney(Player player, long amount) {
        if (player == null)
            return;
        PlayerProfile profile = getProfile(player);
        if (profile != null) {
            profile.getData().setCoins(Math.max(0, amount));
        }
    }

    /**
     * Adiciona moedas ao jogador
     * 
     * @param player Jogador
     * @param amount Quantidade a adicionar
     */
    public void addMoney(Player player, long amount) {
        if (player == null)
            return;
        PlayerProfile profile = getProfile(player);
        if (profile != null) {
            profile.addCoins(Math.abs(amount));
            markScoreboardDirty(player);
        }
    }

    /**
     * Remove moedas do jogador
     * 
     * @param player Jogador
     * @param amount Quantidade a remover
     * @return true se tinha saldo suficiente
     */
    public boolean removeMoney(Player player, long amount) {
        if (player == null)
            return false;
        PlayerProfile profile = getProfile(player);
        if (profile != null) {
            return profile.removeCoins(Math.abs(amount));
        }
        return false;
    }

    /**
     * Verifica se o jogador tem saldo suficiente
     * 
     * @param player Jogador
     * @param amount Quantidade necessária
     * @return true se tem saldo suficiente
     */
    public boolean hasMoney(Player player, long amount) {
        return getMoney(player) >= amount;
    }

    // ==================== MÉTODOS DE KDR ====================

    /**
     * Calcula o KDR (Kill/Death Ratio) do jogador
     * 
     * @param player Jogador
     * @return KDR como double (0 mortes = kills)
     */
    public double getKdr(Player player) {
        PlayerProfile profile = getProfile(player);
        return profile != null ? profile.getKDR() : 0.0;
    }

    /**
     * Obtém o KDR formatado como String
     * 
     * @param player Jogador
     * @return KDR formatado (ex: "2.50")
     */
    public String getKdrFormatted(Player player) {
        return String.format("%.2f", getKdr(player));
    }

    // ==================== SISTEMA DE LIGAS (DELEGA AO LeagueManager)
    // ====================

    /**
     * Obtém o rank atual do jogador baseado no ELO
     * Delega ao LeagueManager
     * 
     * @param player Jogador
     * @return Rank atual
     */
    public PlayerRank getRank(Player player) {
        LeagueManager leagueManager = plugin.getLeagueManager();
        if (leagueManager == null) {
            return null;
        }
        return leagueManager.getRank(player);
    }

    /**
     * Obtém o nome colorizado da liga do jogador
     * 
     * @param player Jogador
     * @return Nome da liga com cores (§)
     */
    public String getLeagueTag(Player player) {
        PlayerRank rank = getRank(player);
        if (rank == null) {
            return "§8Unranked";
        }
        return rank.getFormattedName();
    }

    /**
     * Obtém o próximo rank do jogador
     * 
     * @param player Jogador
     * @return Próximo rank ou null se já está no máximo
     */
    public PlayerRank getNextRank(Player player) {
        LeagueManager leagueManager = plugin.getLeagueManager();
        if (leagueManager == null) {
            return null;
        }
        return leagueManager.getNextRank(player);
    }

    /**
     * Obtém o ELO necessário para o próximo rank
     * 
     * @param player Jogador
     * @return ELO necessário ou 0 se já está no máximo
     */
    public int getEloToNextRank(Player player) {
        LeagueManager leagueManager = plugin.getLeagueManager();
        if (leagueManager == null) {
            return 0;
        }
        return leagueManager.getEloToNextRank(player);
    }

    /**
     * Obtém o progresso do jogador para o próximo rank (0-100)
     * 
     * @param player Jogador
     * @return Progresso percentual
     */
    public int getRankProgress(Player player) {
        LeagueManager leagueManager = plugin.getLeagueManager();
        if (leagueManager == null) {
            return 0;
        }
        return leagueManager.getProgressPercent(player);
    }

    // ==================== SINCRONIZAÇÃO ====================

    /**
     * Sincroniza o level do Minecraft com o killstreak atual
     * 
     * @param player     Jogador
     * @param killstreak Killstreak para sincronizar
     */
    public void syncMinecraftLevel(Player player, int killstreak) {
        if (player == null || !player.isOnline())
            return;
        player.setLevel(killstreak);
        player.setExp(0); // Barra de XP vazia
    }

    /**
     * Atualiza a sincronização do jogador ao entrar
     * 
     * @param player Jogador
     */
    public void syncOnJoin(Player player) {
        if (player == null)
            return;
        int killstreak = getKillstreak(player);
        syncMinecraftLevel(player, killstreak);
    }

    // ==================== MÉTODOS UTILITÁRIOS ====================

    /**
     * Obtém o perfil do jogador de forma segura
     * 
     * @param player Jogador
     * @return PlayerProfile ou null
     */
    private PlayerProfile getProfile(Player player) {
        if (player == null)
            return null;
        return plugin.getProfileManager().getProfile(player);
    }

    /**
     * Obtém as estatísticas completas do jogador formatadas
     * 
     * @param player Jogador
     * @return Array de strings com as estatísticas
     */
    public String[] getStatsFormatted(Player player) {
        if (player == null)
            return new String[0];

        return new String[] {
                "§6Kills: §f" + ChatStorage.formatNumber(getKills(player)),
                "§6Deaths: §f" + ChatStorage.formatNumber(getDeaths(player)),
                "§6KDR: §f" + getKdrFormatted(player),
                "§6Killstreak: §f" + getKillstreak(player) + " §7(Máximo: " + getMaxKillstreak(player) + ")",
                "§6ELO: §f" + ChatStorage.formatNumber(getElo(player)),
                "§6Liga: " + getLeagueTag(player),
                "§6Coins: §f" + ChatStorage.formatNumber(getMoney(player))
        };
    }

    /**
     * Reseta todas as estatísticas do jogador para valores padrão
     * 
     * @param player Jogador
     */
    public void resetStats(Player player) {
        if (player == null)
            return;
        PlayerProfile profile = getProfile(player);
        if (profile != null) {
            PlayerData data = profile.getData();
            data.setKills(0);
            data.setDeaths(0);
            data.setKillStreak(0);
            data.setHighestKillStreak(0);
            data.setCoins(0);
            setElo(player, 100); // Reseta para ELO inicial
            syncMinecraftLevel(player, 0);

            // Marcar scoreboard como dirty para atualização
            markScoreboardDirty(player);
        }
    }

    /**
     * Marca a scoreboard do jogador como dirty para atualização
     * 
     * @param player Jogador
     */
    private void markScoreboardDirty(Player player) {
        if (player == null)
            return;
        if (plugin.getScoreboardManager() != null) {
            plugin.getScoreboardManager().markDirty(player);
        }
    }
}
