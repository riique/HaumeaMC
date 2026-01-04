package com.haumea.kitpvp.tablist;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.models.Group;
import com.haumea.kitpvp.utils.ChatStorage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Gerenciador da TabList do servidor HaumeaMC.
 * 
 * Responsável por:
 * - Gerenciar o header e footer da TabList
 * - Ordenar jogadores por hierarquia usando Teams
 * - Atualizar dinamicamente a cada 10 segundos
 * 
 * Sistema de ordenação:
 * - Usa Teams do Scoreboard para organizar por prioridade
 * - Jogadores com maior prioridade aparecem no topo
 * 
 * @author HaumeaMC
 */
public class TabManager {

    private final HaumeaMC plugin;
    private TabTask tabTask;
    private boolean taskCancelled;

    // Scoreboard principal para ordenação
    private Scoreboard mainScoreboard;

    // Reflexão - cacheado para performance
    private Class<?> packetClass;
    private Class<?> chatComponentClass;
    private Method chatSerializerMethod;
    private Constructor<?> packetConstructor;
    private Field headerField;
    private Field footerField;
    private boolean reflectionInitialized;

    /**
     * Intervalo de atualização da TabList em ticks (20 ticks = 1 segundo)
     * 10 segundos = 200 ticks
     */
    private static final int UPDATE_INTERVAL_TICKS = 200;

    // ==================== CONSTRUTOR ====================

    public TabManager(HaumeaMC plugin) {
        this.plugin = plugin;
        this.taskCancelled = true;

        // Inicializar scoreboard
        initScoreboard();

        // Inicializar reflexão para packets
        initReflection();

        plugin.getLogger().info("[TabManager] Sistema de TabList inicializado!");
    }

    /**
     * Inicializa o scoreboard principal para ordenação de teams
     */
    private void initScoreboard() {
        mainScoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
    }

    /**
     * Inicializa os campos de reflexão para envio de packets
     */
    private void initReflection() {
        try {
            // Obter versão do servidor
            String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];

            // Classes NMS
            String nmsPackage = "net.minecraft.server." + version;

            packetClass = Class.forName(nmsPackage + ".PacketPlayOutPlayerListHeaderFooter");
            chatComponentClass = Class.forName(nmsPackage + ".IChatBaseComponent");

            // Obter ChatSerializer (pode estar como inner class)
            Class<?> chatSerializerClass = Class.forName(nmsPackage + ".IChatBaseComponent$ChatSerializer");
            chatSerializerMethod = chatSerializerClass.getMethod("a", String.class);

            // Campos do packet
            packetConstructor = packetClass.getDeclaredConstructor();
            headerField = packetClass.getDeclaredField("a");
            footerField = packetClass.getDeclaredField("b");

            headerField.setAccessible(true);
            footerField.setAccessible(true);

            reflectionInitialized = true;
            plugin.getLogger().info("[TabManager] Reflexão NMS inicializada para " + version);

        } catch (Exception e) {
            reflectionInitialized = false;
            plugin.getLogger().warning("[TabManager] Falha ao inicializar reflexão: " + e.getMessage());
        }
    }

    // ==================== CICLO DE VIDA ====================

    /**
     * Inicia o sistema de TabList
     */
    public void start() {
        if (tabTask != null) {
            stop();
        }

        // Aplicar teams para jogadores já online
        for (Player player : Bukkit.getOnlinePlayers()) {
            updatePlayerTeam(player);
            updateTabList(player);
        }

        tabTask = new TabTask(plugin, this);
        tabTask.runTaskTimerAsynchronously(plugin, 20L, UPDATE_INTERVAL_TICKS);
        taskCancelled = false;

        plugin.getLogger().info("[TabManager] Sistema de TabList iniciado!");
    }

    /**
     * Para o sistema de TabList
     */
    public void stop() {
        if (tabTask != null) {
            try {
                tabTask.cancel();
            } catch (Exception ignored) {
            }
            tabTask = null;
            taskCancelled = true;
            plugin.getLogger().info("[TabManager] Sistema de TabList parado!");
        }
    }

    /**
     * Reinicia o sistema de TabList
     */
    public void restart() {
        stop();
        start();
    }

    /**
     * Atualiza o team de um jogador baseado no seu grupo/tag.
     * USA O DISPLAYMANAGER COMO FONTE DE DADOS QUANDO DISPONÍVEL.
     * 
     * Isso controla a ordenação e prefixo na TabList
     * 
     * @param player Jogador a atualizar
     */
    public void updatePlayerTeam(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }

        // Obter grupo do jogador (usado para ordenação)
        Group group = plugin.getGroupManager().getPlayerGroup(player);
        if (group == null) {
            group = plugin.getGroupManager().getGroup("membro");
        }

        // Calcular ordem (inversa da prioridade para ordenação correta)
        // Teams são ordenadas alfabeticamente, então usamos números
        // Maior prioridade = menor número = aparece primeiro
        int sortOrder = 9999 - group.getPriority();
        String teamName = String.format("%04d_%s", sortOrder, group.getName());

        // Limitar nome do team a 16 caracteres (limite do Minecraft 1.8)
        if (teamName.length() > 16) {
            teamName = teamName.substring(0, 16);
        }

        // DADOS DE EXIBIÇÃO
        // O NametagManager agora cuida dos prefixos e sufixos (símbolo da liga)
        // via Scoreboard Teams. Aqui apenas delegamos para ele.
        if (plugin.getNametagManager() != null) {
            plugin.getNametagManager().updateNametag(player);
        }

        String medalDisplay = "";
        String tagPrefix;
        String playerName;
        boolean hasFakeNick;

        if (plugin.getDisplayManager() != null) {
            // DisplayManager: fonte unificada de dados
            medalDisplay = plugin.getDisplayManager().getMedalDisplay(player);
            tagPrefix = plugin.getDisplayManager().getCurrentPrefix(player);
            playerName = plugin.getDisplayManager().getNameToDisplay(player);
            hasFakeNick = plugin.getDisplayManager().isUsingFakeNick(player);
        } else {
            // Fallback: calcular manualmente
            if (plugin.getMedalManager() != null) {
                medalDisplay = plugin.getMedalManager().getPlayerMedalDisplay(player);
            }

            tagPrefix = "";
            if (plugin.getTagManager() != null && plugin.getProfileManager() != null) {
                com.haumea.kitpvp.profile.PlayerProfile profile = plugin.getProfileManager().getProfile(player);
                if (profile != null) {
                    String savedTag = profile.getData().getSelectedTag();
                    if (savedTag != null && !savedTag.isEmpty()) {
                        com.haumea.kitpvp.models.Tag tag = plugin.getTagManager().getTag(savedTag);
                        if (tag != null && player.hasPermission(tag.getPermission())) {
                            tagPrefix = ChatStorage.colorize(tag.getPrefix());
                        }
                    }
                }
            }
            if (tagPrefix.isEmpty()) {
                tagPrefix = ChatStorage.colorize(group.getPrefix());
            }

            hasFakeNick = plugin.getFakeNickManager() != null && plugin.getFakeNickManager().hasFakeNick(player);
            playerName = hasFakeNick ? plugin.getFakeNickManager().getDisplayName(player) : player.getName();
        }

        // ===== PREFIXO PARA TABLIST: apenas tag/grupo (SEM símbolo da liga) =====
        // O símbolo da liga aparece apenas no chat via DisplayName
        // Limite de 16 caracteres no Minecraft 1.8
        String tabPrefix = tagPrefix;
        if (tabPrefix.length() > 16) {
            tabPrefix = tabPrefix.substring(0, 16);
        }

        // NOTA: A aplicação de teams agora é feita pelo NametagManager
        // Mantemos apenas para fallback caso NametagManager não esteja disponível
        if (plugin.getNametagManager() == null) {
            applyTeamToScoreboard(mainScoreboard, player, teamName, tabPrefix);
        }

        // NOTA: A aplicação em TODAS as scoreboards de todos os jogadores
        // agora é feita pelo NametagManager.updateNametag()

        // ===== DISPLAYNAME PARA CHAT: medalha + símbolo da liga + tag/grupo + nome
        // =====
        // O símbolo da liga é obtido do DisplayManager (se disponível)
        String leagueSymbol = "";
        if (plugin.getDisplayManager() != null) {
            leagueSymbol = plugin.getDisplayManager().getLeagueSymbol(player);
        } else if (plugin.getLeagueManager() != null) {
            // Fallback: obter diretamente do LeagueManager
            com.haumea.kitpvp.models.PlayerRank rank = plugin.getLeagueManager().getRank(player);
            if (rank != null) {
                leagueSymbol = rank.getSymbol() + " ";
            }
        }
        String displayName = medalDisplay + leagueSymbol + tagPrefix + playerName;
        player.setDisplayName(displayName);

        // Para a TabList:
        // IMPORTANTE: O prefixo/sufixo são aplicados pelo Team (via NametagManager)
        // O setPlayerListName deve conter APENAS o nome que será exibido
        // O Team concatena: prefix + playerListName + suffix
        if (hasFakeNick) {
            // Com fake nick: usar apenas o nome fake
            // O Team do NametagManager aplica prefixo e sufixo automaticamente
            player.setPlayerListName(playerName);
        } else {
            // Sem fake nick: usar o nome real
            // O Team do NametagManager aplica prefixo e sufixo automaticamente
            player.setPlayerListName(player.getName());
        }
    }

    /**
     * Aplica um team a um scoreboard específico.
     * 
     * NOTA: Este é um FALLBACK quando o NametagManager não está disponível.
     * CORREÇÃO: Adiciona APENAS o nome que o cliente está vendo.
     * 
     * @param scoreboard Scoreboard alvo
     * @param player     Jogador
     * @param teamName   Nome do team
     * @param prefix     Prefixo do team
     */
    private void applyTeamToScoreboard(Scoreboard scoreboard, Player player, String teamName, String prefix) {
        if (scoreboard == null)
            return;

        // Obter fake nick se existir
        String fakeNick = null;
        boolean hasFakeNick = false;
        if (plugin.getFakeNickManager() != null && plugin.getFakeNickManager().hasFakeNick(player)) {
            fakeNick = plugin.getFakeNickManager().getFakeNick(player);
            hasFakeNick = true;
        }

        // Determinar qual nome usar (o que o cliente vê na TabList)
        String nameToUse = hasFakeNick ? fakeNick : player.getName();

        // Remover jogador de teams antigos (ambos os nomes para limpeza)
        for (Team team : scoreboard.getTeams()) {
            if (team.hasEntry(player.getName())) {
                team.removeEntry(player.getName());
            }
            if (fakeNick != null && team.hasEntry(fakeNick)) {
                team.removeEntry(fakeNick);
            }
        }

        // Criar ou obter team
        Team team = scoreboard.getTeam(teamName);
        if (team == null) {
            team = scoreboard.registerNewTeam(teamName);
        }

        // Configurar prefixo
        team.setPrefix(prefix);

        // CORREÇÃO: Adicionar APENAS o nome que o cliente está vendo
        team.addEntry(nameToUse);
    }

    /**
     * Remove um jogador do sistema de teams.
     * Remove tanto o nome original quanto o fake nick.
     * 
     * @param player Jogador a remover
     */
    public void removePlayerFromTeams(Player player) {
        // Obter fake nick se existir
        String fakeNick = null;
        if (plugin.getFakeNickManager() != null && plugin.getFakeNickManager().hasFakeNick(player)) {
            fakeNick = plugin.getFakeNickManager().getFakeNick(player);
        }

        for (Team team : mainScoreboard.getTeams()) {
            if (team.hasEntry(player.getName())) {
                team.removeEntry(player.getName());
            }
            if (fakeNick != null && team.hasEntry(fakeNick)) {
                team.removeEntry(fakeNick);
            }
        }
    }

    /**
     * Limpa teams vazios (manutenção)
     */
    public void cleanupEmptyTeams() {
        for (Team team : mainScoreboard.getTeams()) {
            if (team.getName().matches("\\d{4}_.*") && team.getSize() == 0) {
                team.unregister();
            }
        }
    }

    // ==================== CONSTRUÇÃO DO HEADER ====================

    /**
     * Constrói o header da TabList
     * 
     * @return Header formatado
     */
    public String buildHeader() {
        StringBuilder header = new StringBuilder();

        // Linha em branco para espaçamento
        header.append("\n");

        // Nome do servidor em destaque
        header.append("§6§lHAUMEA§f§lMC §8- §e§lKITPVP\n");

        // Linha de jogadores online
        int online = Bukkit.getOnlinePlayers().size();
        int max = Bukkit.getMaxPlayers();
        header.append("§fJogadores online: §a").append(online).append("§7/§a").append(max).append("\n");

        // Linha em branco para separar da lista
        header.append("\n");

        return header.toString();
    }

    // ==================== CONSTRUÇÃO DO FOOTER ====================

    /**
     * Constrói o footer da TabList
     * 
     * @return Footer formatado
     */
    public String buildFooter() {
        StringBuilder footer = new StringBuilder();

        // Linha em branco para espaçamento
        footer.append("\n");

        // Linha de separação sutil
        footer.append("§8§m-----------------------\n");

        // Linha em branco
        footer.append("\n");

        // Informações do servidor
        footer.append("§bLoja: §fhaumeamc.com.br\n");
        footer.append("§9Discord: §fdiscord.gg/haumeamc\n");

        // Linha em branco final
        footer.append("\n");

        return footer.toString();
    }

    // ==================== ENVIO DA TABLIST ====================

    /**
     * Atualiza a TabList de um jogador específico
     * 
     * @param player Jogador a atualizar
     */
    public void updateTabList(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }

        if (!reflectionInitialized) {
            return;
        }

        String header = buildHeader();
        String footer = buildFooter();

        try {
            sendTabListPacket(player, header, footer);
        } catch (Exception e) {
            // Fallback silencioso
        }
    }

    /**
     * Envia o packet de TabList via reflexão
     * 
     * @param player Jogador
     * @param header Texto do header
     * @param footer Texto do footer
     */
    private void sendTabListPacket(Player player, String header, String footer) throws Exception {
        // Criar componentes de texto via reflexão
        Object headerComponent = chatSerializerMethod.invoke(null,
                "{\"text\":\"" + escapeJson(header) + "\"}");
        Object footerComponent = chatSerializerMethod.invoke(null,
                "{\"text\":\"" + escapeJson(footer) + "\"}");

        // Criar o packet
        Object packet = packetConstructor.newInstance();

        // Definir campos
        headerField.set(packet, headerComponent);
        footerField.set(packet, footerComponent);

        // Enviar packet via reflexão
        sendPacket(player, packet);
    }

    /**
     * Envia um packet para um jogador via reflexão
     */
    private void sendPacket(Player player, Object packet) throws Exception {
        Object handle = player.getClass().getMethod("getHandle").invoke(player);
        Object connection = handle.getClass().getField("playerConnection").get(handle);
        connection.getClass().getMethod("sendPacket",
                Class.forName("net.minecraft.server." + getVersion() + ".Packet"))
                .invoke(connection, packet);
    }

    /**
     * Obtém a versão do servidor
     */
    private String getVersion() {
        return Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
    }

    /**
     * Escapa caracteres especiais para JSON
     * 
     * @param text Texto a escapar
     * @return Texto escapado
     */
    private String escapeJson(String text) {
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "")
                .replace("\t", "\\t");
    }

    /**
     * Atualiza a TabList de todos os jogadores online
     */
    public void updateAllPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updateTabList(player);
        }
    }

    /**
     * Atualiza o team e a TabList de todos os jogadores
     * Usado quando há mudanças de cargo/tag
     */
    public void refreshAllPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updatePlayerTeam(player);
            updateTabList(player);
        }
    }

    // ==================== GETTERS ====================

    /**
     * Verifica se o sistema está rodando
     */
    public boolean isRunning() {
        return tabTask != null && !taskCancelled;
    }

    /**
     * Obtém o scoreboard principal
     */
    public Scoreboard getMainScoreboard() {
        return mainScoreboard;
    }
}
