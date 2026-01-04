package com.haumea.kitpvp.scoreboard;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.managers.KitManager;
import com.haumea.kitpvp.managers.ServerSelectorManager;
import com.haumea.kitpvp.managers.StatsManager;
import com.haumea.kitpvp.models.Group;
import com.haumea.kitpvp.models.Kit;
import com.haumea.kitpvp.models.PlayerData;
import com.haumea.kitpvp.models.PlayerRank;
import com.haumea.kitpvp.profile.PlayerProfile;
import com.haumea.kitpvp.utils.ChatStorage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gerenciador central de scoreboards Premium do HaumeaMC.
 * 
 * Design Premium com:
 * - Título animado impactante
 * - Separadores elegantes
 * - Seção de Kits em destaque
 * - Estatísticas estilizadas com ícones
 * - Paleta: Branco, Ouro, Azul/Ciano
 * 
 * @author HaumeaMC
 */
public class ScoreboardManager {

    private final HaumeaMC plugin;
    private final Map<UUID, PlayerBoard> boards;
    private final TitleAnimation titleAnimation;
    private BukkitTask updateTask;

    // Configurações de atualização
    private static final long UPDATE_INTERVAL = 20L; // Ticks (20 ticks = 1 segundo)
    private static final long TITLE_ANIMATION_INTERVAL = 3L; // A cada 3 atualizações (~3 segundos)

    // ==================== ÍCONES E SÍMBOLOS ====================

    private static final String ICON_FIRE = "";
    private static final String ICON_COIN = "$";
    private static final String SEPARATOR = "§7§m-----------------";
    private static final String SERVER_URL = "§e www.haumeamc.com.br";

    // ==================== CORES DA PALETA ====================
    private static final String COLOR_LABEL = "§f"; // Branco para labels
    private static final String COLOR_KILLS = "§b"; // Ciano para kills
    private static final String COLOR_DEATHS = "§c"; // Vermelho para mortes
    private static final String COLOR_STREAK = "§e"; // Amarelo para streak
    private static final String COLOR_COINS = "§6"; // Ouro para moedas

    private static final String COLOR_KIT_ACTIVE = "§a"; // Verde para kit ativo
    private static final String COLOR_KIT_NONE = "§7"; // Cinza para sem kit

    private int updateCounter = 0;

    // Sistema de dirty flag para otimização (atualiza apenas quando mudou)
    private final java.util.Set<UUID> dirtyBoards;

    /**
     * Construtor do ScoreboardManager
     * 
     * @param plugin Instância do plugin
     */
    public ScoreboardManager(HaumeaMC plugin) {
        this.plugin = plugin;
        this.boards = new ConcurrentHashMap<>();
        this.titleAnimation = new TitleAnimation();
        this.dirtyBoards = ConcurrentHashMap.newKeySet();
    }

    /**
     * Inicia o sistema de scoreboard
     */
    public void start() {
        if (updateTask != null) {
            updateTask.cancel();
        }

        updateTask = new BukkitRunnable() {
            @Override
            public void run() {
                updateCounter++;

                // Atualizar título animado a cada N ciclos (todos precisam atualizar se título
                // mudou)
                boolean updateTitle = (updateCounter % TITLE_ANIMATION_INTERVAL == 0);
                if (updateTitle) {
                    titleAnimation.next();
                    // Marcar todos como dirty para atualizar título
                    dirtyBoards.addAll(boards.keySet());
                }

                // Atualizar apenas scoreboards marcadas como dirty
                java.util.Iterator<UUID> it = dirtyBoards.iterator();
                while (it.hasNext()) {
                    UUID uuid = it.next();
                    PlayerBoard board = boards.get(uuid);
                    if (board != null && board.isValid()) {
                        updateBoard(board, updateTitle);
                    }
                    it.remove();
                }

                // Atualizar scoreboards de admin sempre (dados mudam frequentemente)
                for (PlayerBoard board : boards.values()) {
                    if (board.isValid() && board.getPlayer() != null) {
                        Player p = board.getPlayer();
                        boolean isAdmin = plugin.getStateManager() != null && plugin.getStateManager().isInAdminMode(p);
                        if (isAdmin) {
                            updateBoard(board, updateTitle);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, UPDATE_INTERVAL);

        plugin.getLogger().info("Sistema de Scoreboard Premium iniciado (otimizado com dirty flags).");
    }

    /**
     * Para o sistema de scoreboard
     */
    public void stop() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }

        // Remover todas as scoreboards
        for (PlayerBoard board : boards.values()) {
            board.remove();
        }
        boards.clear();
        dirtyBoards.clear();

        plugin.getLogger().info("Sistema de Scoreboard parado.");
    }

    /**
     * Marca a scoreboard de um jogador como "dirty" para ser atualizada no próximo
     * tick.
     * Chamado quando estatísticas mudam (kills, deaths, coins, etc.)
     * 
     * @param player Jogador
     */
    public void markDirty(Player player) {
        if (player != null) {
            dirtyBoards.add(player.getUniqueId());
        }
    }

    /**
     * Marca a scoreboard de um jogador como "dirty" por UUID
     * 
     * @param uuid UUID do jogador
     */
    public void markDirty(UUID uuid) {
        if (uuid != null) {
            dirtyBoards.add(uuid);
        }
    }

    /**
     * Cria uma scoreboard para um jogador
     * 
     * @param player Jogador
     */
    public void createBoard(Player player) {
        if (player == null || !player.isOnline())
            return;

        // Remover scoreboard antiga se existir
        removeBoard(player);

        // Criar nova scoreboard
        PlayerBoard board = new PlayerBoard(player);
        boards.put(player.getUniqueId(), board);

        // Primeira atualização
        updateBoard(board, true);

        // Reaplicar teams do TabManager na nova scoreboard (para prefixos na tab)
        if (plugin.getTabManager() != null) {
            plugin.getTabManager().updatePlayerTeam(player);
        }

        // CRÍTICO: Aplicar os teams de TODOS os outros jogadores online na nova
        // scoreboard!
        // Isso garante que o jogador veja as tags/nametags corretas de todos.
        if (plugin.getDisplayManager() != null) {
            org.bukkit.scoreboard.Scoreboard playerScoreboard = player.getScoreboard();
            if (playerScoreboard != null) {
                for (Player otherPlayer : org.bukkit.Bukkit.getOnlinePlayers()) {
                    if (!otherPlayer.equals(player)) {
                        // Atualizar o team do outro jogador na scoreboard deste jogador
                        plugin.getDisplayManager().refreshPlayer(otherPlayer);
                    }
                }
            }
        }
    }

    /**
     * Remove a scoreboard de um jogador
     * 
     * @param player Jogador
     */
    public void removeBoard(Player player) {
        if (player == null)
            return;

        PlayerBoard board = boards.remove(player.getUniqueId());
        if (board != null) {
            board.remove();
        }
    }

    /**
     * Obtém a scoreboard de um jogador
     * 
     * @param player Jogador
     * @return PlayerBoard ou null
     */
    public PlayerBoard getBoard(Player player) {
        if (player == null)
            return null;
        return boards.get(player.getUniqueId());
    }

    /**
     * Verifica se o jogador tem scoreboard
     * 
     * @param player Jogador
     * @return true se tem scoreboard
     */
    public boolean hasBoard(Player player) {
        return player != null && boards.containsKey(player.getUniqueId());
    }

    /**
     * Atualiza a scoreboard de um jogador
     * 
     * @param board       PlayerBoard a atualizar
     * @param updateTitle Se deve atualizar o título animado
     */
    private void updateBoard(PlayerBoard board, boolean updateTitle) {
        Player player = board.getPlayer();
        if (player == null || !player.isOnline())
            return;

        // Verificar se está em modo admin (vanish)
        PlayerProfile profile = plugin.getProfileManager().getProfile(player);
        boolean isAdmin = profile != null && profile.isVanish();

        // Atualizar título
        if (updateTitle) {
            if (isAdmin) {
                // Título especial para modo vanish
                board.setTitle("§c§lMODO ADMIN");
            } else {
                // Título animado premium
                board.setTitle(titleAnimation.current());
            }
        }

        // Construir linhas baseado no estado do jogador e tipo de servidor
        String[] lines;
        if (isAdmin) {
            lines = buildAdminLines(player);
        } else if (plugin.isLobby()) {
            lines = buildLobbyLines(player);
        } else {
            lines = buildPremiumLines(player);
        }
        board.setLines(lines);
    }

    /**
     * Constrói as linhas da scoreboard para modo Admin
     * 
     * @param player Jogador em modo admin
     * @return Array de linhas
     */
    private String[] buildAdminLines(Player player) {
        // Obter grupo do jogador
        com.haumea.kitpvp.models.Group group = plugin.getGroupManager().getPlayerGroup(player);
        String cargoDisplay;
        if (group != null && !group.getPrefix().isEmpty()) {
            String prefix = ChatStorage.colorize(group.getPrefix()).trim();
            if (prefix.endsWith("§") || prefix.matches(".*§[0-9a-fk-or]$")) {
                prefix = prefix.substring(0, prefix.length() - 2).trim();
            }
            cargoDisplay = prefix;
        } else if (group != null) {
            cargoDisplay = ChatStorage.colorize(group.getDisplayName());
        } else {
            cargoDisplay = "§7Membro";
        }

        // Contar jogadores online (excluindo admins em vanish)
        int playersOnline = 0;
        for (Player online : org.bukkit.Bukkit.getOnlinePlayers()) {
            PlayerProfile onlineProfile = plugin.getProfileManager().getProfile(online);
            if (onlineProfile == null || !onlineProfile.isVanish()) {
                playersOnline++;
            }
        }

        // Integrar com sistema de denúncias
        int denuncias = 0;
        if (plugin.getReportManager() != null) {
            denuncias = plugin.getReportManager().countOpenReports();
        }

        return new String[] {
                SEPARATOR,
                "",
                COLOR_LABEL + "Cargo: " + cargoDisplay,
                COLOR_LABEL + "Players: §a" + playersOnline,
                " ",
                COLOR_LABEL + "Denúncias: §c#" + denuncias,
                "  ",
                SEPARATOR,
                SERVER_URL
        };
    }

    /**
     * Constrói as linhas premium da scoreboard (Design Moderno)
     * 
     * Layout:
     * -----------------
     * 
     * Kit 1: §a{kit_1}
     * Kit 2: §a{kit_2}
     * 
     * Kills: §b{kills}
     * Mortes: §c{deaths}
     * Streak: §e{streak} 🔥
     * K/D: §a{kd}
     * 
     * Moedas: §6{coins} ⛁
     * ELO: §d{elo} ✦
     * Liga: {liga}
     * 
     * §e www.haumeamc.com.br
     * -----------------
     * 
     * @param player Jogador
     * @return Array de linhas
     */
    private String[] buildPremiumLines(Player player) {
        StatsManager stats = plugin.getStatsManager();
        KitManager kitManager = plugin.getKitManager();

        // ==================== ESTATÍSTICAS ====================
        int kills = stats.getKills(player);
        int deaths = stats.getDeaths(player);
        int killstreak = stats.getKillstreak(player);
        long money = stats.getMoney(player);
        int elo = stats.getElo(player);
        PlayerRank rank = stats.getRank(player);

        // ==================== KITS ====================
        String primaryKitName = kitManager != null ? kitManager.getPrimaryKit(player) : null;
        String secondaryKitName = kitManager != null ? kitManager.getSecondaryKit(player) : null;

        // Formatar nomes dos kits
        String kit1Display = formatKitDisplay(kitManager, primaryKitName);
        String kit2Display = formatKitDisplay(kitManager, secondaryKitName);

        // ==================== FORMATAÇÃO ====================
        String killsFormatted = ChatStorage.formatNumber(kills);
        String deathsFormatted = ChatStorage.formatNumber(deaths);
        String coinsFormatted = ChatStorage.formatNumber(money);
        String eloFormatted = ChatStorage.formatNumber(elo);
        String leagueFormatted = rank != null ? rank.getFormattedName() : "§8Unranked";

        // Streak
        String streakDisplay = COLOR_STREAK + killstreak;

        // K/D ratio (evitar divisão por zero)
        double kdRatio = deaths > 0 ? (double) kills / deaths : 0;
        String kdFormatted = String.format("%.2f", kdRatio);

        // ==================== CONSTRUÇÃO DO LAYOUT ====================
        return new String[] {
                SEPARATOR, // Linha 1: Separador topo
                "", // Linha 2: Espaço
                COLOR_LABEL + " Kit 1: " + kit1Display, // Linha 3: Kit 1
                COLOR_LABEL + " Kit 2: " + kit2Display, // Linha 4: Kit 2
                " ", // Linha 5: Espaço
                COLOR_LABEL + " Kills: " + COLOR_KILLS + killsFormatted, // Linha 6: Kills
                COLOR_LABEL + " Mortes: " + COLOR_DEATHS + deathsFormatted, // Linha 7: Mortes
                COLOR_LABEL + " Streak: " + streakDisplay, // Linha 8: Killstreak
                COLOR_LABEL + " K/D: §a" + kdFormatted, // Linha 9: K/D Ratio
                "  ", // Linha 10: Espaço
                COLOR_LABEL + " Moedas: " + COLOR_COINS + coinsFormatted, // Linha 11: Moedas
                COLOR_LABEL + " ELO: §d" + eloFormatted + " ✦", // Linha 12: ELO
                COLOR_LABEL + " Liga: " + leagueFormatted, // Linha 13: Liga
                "   ", // Linha 14: Espaço
                SEPARATOR, // Linha 15: Separador inferior
                SERVER_URL // Linha 16: URL
        };
    }

    /**
     * Constrói as linhas da scoreboard para o LOBBY
     * 
     * Layout:
     * -----------------
     * 
     * Bem-vindo, {player}
     * 
     * Servidores:
     * ⚔ KitPvP: {online}
     * 
     * Coins: {coins}
     * Grupo: {grupo}
     * 
     * play.haumeamc.com
     * -----------------
     * 
     * @param player Jogador
     * @return Array de linhas
     */
    private String[] buildLobbyLines(Player player) {
        // Obter dados do jogador
        PlayerProfile profile = plugin.getProfileManager().getProfile(player);
        long coins = 0;
        String groupName = "§7Membro";

        if (profile != null && profile.getData() != null) {
            PlayerData data = profile.getData();
            coins = data.getCoins();
        }

        // Obter grupo do jogador
        Group group = plugin.getGroupManager().getPlayerGroup(player);
        if (group != null) {
            groupName = ChatStorage.colorize(group.getDisplayName());
        }

        // Obter contagem de jogadores no KitPvP (cache do ServerSelectorManager)
        int kitpvpOnline = 0;
        ServerSelectorManager selectorManager = plugin.getServerSelectorManager();
        if (selectorManager != null) {
            kitpvpOnline = selectorManager.getPlayerCount("kitpvp");
        }

        // Formatação
        String coinsFormatted = ChatStorage.formatNumber(coins);
        String playerName = player.getName();

        // Se tem fake nick ativo, usar o fake
        if (plugin.getFakeNickManager() != null && plugin.getFakeNickManager().hasFakeNick(player)) {
            playerName = plugin.getFakeNickManager().getDisplayName(player);
        }

        return new String[] {
                SEPARATOR, // Linha 1: Separador
                "", // Linha 2: Espaço
                "§fBem-vindo, §a" + playerName, // Linha 3: Saudação
                " ", // Linha 4: Espaço
                "§7Servidores:", // Linha 5: Título
                " §c⚔ §fKitPvP: §a" + kitpvpOnline, // Linha 6: KitPvP
                "  ", // Linha 7: Espaço
                "§eCoins: §f" + coinsFormatted, // Linha 8: Coins
                "§bGrupo: §f" + groupName, // Linha 9: Grupo
                "   ", // Linha 10: Espaço
                SEPARATOR, // Linha 11: Separador
                "§7play.haumeamc.com" // Linha 12: URL
        };
    }

    /**
     * Formata a exibição de um kit para a scoreboard
     * 
     * @param kitManager KitManager
     * @param kitName    Nome do kit (pode ser null)
     * @return String formatada para exibição
     */
    private String formatKitDisplay(KitManager kitManager, String kitName) {
        if (kitName == null || kitName.isEmpty()) {
            return COLOR_KIT_NONE + "Nenhum";
        }

        // Tentar obter o display name do kit
        if (kitManager != null) {
            Kit kit = kitManager.getKit(kitName);
            if (kit != null) {
                // Usar o display name do kit (já pode conter cores)
                String displayName = kit.getDisplayName();
                if (displayName != null && !displayName.isEmpty()) {
                    return ChatStorage.colorize(displayName);
                }
            }
        }

        // Fallback: capitalizar o nome do kit
        return COLOR_KIT_ACTIVE + capitalize(kitName);
    }

    /**
     * Capitaliza a primeira letra de uma string
     * 
     * @param str String
     * @return String capitalizada
     */
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    /**
     * Força uma atualização completa da scoreboard de um jogador
     * 
     * @param player Jogador
     */
    public void forceUpdate(Player player) {
        PlayerBoard board = getBoard(player);
        if (board != null) {
            updateBoard(board, true);
        }
    }

    /**
     * Força atualização de todas as scoreboards
     */
    public void forceUpdateAll() {
        for (PlayerBoard board : boards.values()) {
            if (board.isValid()) {
                updateBoard(board, true);
            }
        }
    }

    /**
     * Obtém o número de scoreboards ativas
     * 
     * @return Número de scoreboards
     */
    public int getActiveBoards() {
        return boards.size();
    }

    /**
     * Obtém a animação do título
     * 
     * @return TitleAnimation
     */
    public TitleAnimation getTitleAnimation() {
        return titleAnimation;
    }
}
