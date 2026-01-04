package com.haumea.kitpvp.managers;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.models.Group;
import com.haumea.kitpvp.models.Tag;
import com.haumea.kitpvp.profile.PlayerProfile;
import com.haumea.kitpvp.utils.ChatStorage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gerenciador Unificado de Exibição do Jogador.
 * 
 * CENTRALIZA toda a lógica de exibição do jogador em um único lugar:
 * - Nome exibido (real ou fake nick)
 * - Prefixo (tag selecionada ou prefixo do grupo)
 * - Medalha
 * - Atualização da TabList
 * - Atualização do Nametag (Scoreboard Teams)
 * - Atualização do DisplayName (Chat)
 * 
 * Isso resolve os problemas de dessincronização entre:
 * - FakeNickManager
 * - TagManager
 * - TabManager
 * - ChatListener
 * 
 * @author HaumeaMC
 */
public class DisplayManager {

    private final HaumeaMC plugin;

    // Cache de dados de exibição atual de cada jogador
    private final Map<UUID, DisplayData> displayCache;

    public DisplayManager(HaumeaMC plugin) {
        this.plugin = plugin;
        this.displayCache = new ConcurrentHashMap<>();
        plugin.getLogger().info("[DisplayManager] Sistema de exibição unificado inicializado!");
    }

    // ==================== CLASSES INTERNAS ====================

    /**
     * Dados de exibição de um jogador
     */
    public static class DisplayData {
        private final String realName;
        private String displayName;
        private String prefix;
        private String medalDisplay;
        private String leagueSymbol;
        private boolean usingFakeNick;
        private String fakeNickName;

        public DisplayData(String realName) {
            this.realName = realName;
            this.displayName = realName;
            this.prefix = "§7";
            this.medalDisplay = "";
            this.leagueSymbol = "";
            this.usingFakeNick = false;
            this.fakeNickName = null;
        }

        // Getters
        public String getRealName() {
            return realName;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getPrefix() {
            return prefix;
        }

        public String getMedalDisplay() {
            return medalDisplay;
        }

        public boolean isUsingFakeNick() {
            return usingFakeNick;
        }

        public String getFakeNickName() {
            return fakeNickName;
        }

        public String getLeagueSymbol() {
            return leagueSymbol;
        }

        // Getters calculados
        public String getFullDisplayName() {
            return medalDisplay + leagueSymbol + prefix + displayName;
        }

        public String getNameToShow() {
            return usingFakeNick && fakeNickName != null ? fakeNickName : realName;
        }

        // Setters
        void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        void setPrefix(String prefix) {
            this.prefix = prefix;
        }

        void setMedalDisplay(String medalDisplay) {
            this.medalDisplay = medalDisplay;
        }

        void setUsingFakeNick(boolean usingFakeNick) {
            this.usingFakeNick = usingFakeNick;
        }

        void setFakeNickName(String fakeNickName) {
            this.fakeNickName = fakeNickName;
        }

        void setLeagueSymbol(String leagueSymbol) {
            this.leagueSymbol = leagueSymbol;
        }
    }

    // ==================== MÉTODOS PRINCIPAIS ====================

    /**
     * Obtém ou cria os dados de exibição de um jogador
     */
    public DisplayData getDisplayData(Player player) {
        return displayCache.computeIfAbsent(player.getUniqueId(),
                uuid -> new DisplayData(player.getName()));
    }

    /**
     * Obtém os dados de exibição por UUID
     */
    public DisplayData getDisplayData(UUID uuid) {
        return displayCache.get(uuid);
    }

    /**
     * ATUALIZAÇÃO COMPLETA de exibição de um jogador.
     * Deve ser chamado sempre que algo mudar (tag, fake nick, medalha, grupo).
     */
    public void refreshPlayer(Player player) {
        if (player == null || !player.isOnline())
            return;

        DisplayData data = getDisplayData(player);

        // 1. Atualizar estado de fake nick
        boolean hasFakeNick = plugin.getFakeNickManager() != null
                && plugin.getFakeNickManager().hasFakeNick(player);
        data.setUsingFakeNick(hasFakeNick);

        if (hasFakeNick) {
            data.setFakeNickName(plugin.getFakeNickManager().getFakeNick(player));
            data.setDisplayName(data.getFakeNickName());
        } else {
            data.setFakeNickName(null);
            data.setDisplayName(player.getName());
        }

        // 2. Atualizar medalha
        if (plugin.getMedalManager() != null) {
            data.setMedalDisplay(plugin.getMedalManager().getPlayerMedalDisplay(player));
        } else {
            data.setMedalDisplay("");
        }

        // 3. Obter símbolo da liga do jogador (via LeagueManager)
        if (plugin.getLeagueManager() != null) {
            com.haumea.kitpvp.models.PlayerRank rank = plugin.getLeagueManager().getRank(player);
            if (rank != null) {
                data.setLeagueSymbol(rank.getSymbol() + " "); // Ex: "§6✹ "
            } else {
                data.setLeagueSymbol("");
            }
        } else {
            data.setLeagueSymbol("");
        }

        // 4. Calcular prefixo (TAG selecionada > Grupo)
        String prefix = calculatePrefix(player, hasFakeNick);
        data.setPrefix(prefix);

        // 5. Aplicar visualmente
        applyDisplayToPlayer(player, data);

        // 6. Atualizar Tab e Nametag
        updateScoreboardTeam(player, data);
    }

    /**
     * Calcula o prefixo correto para o jogador.
     * Ordem de prioridade:
     * 1. Se está com fake nick E não tem tag selecionada: sem prefixo (§7)
     * 2. Tag selecionada pelo jogador (se tiver permissão)
     * 3. Prefixo do grupo como fallback
     */
    private String calculatePrefix(Player player, boolean hasFakeNick) {
        // 1. Verificar tag selecionada no PlayerData
        if (plugin.getProfileManager() != null && plugin.getTagManager() != null) {
            PlayerProfile profile = plugin.getProfileManager().getProfile(player);
            if (profile != null) {
                String selectedTagName = profile.getData().getSelectedTag();
                if (selectedTagName != null && !selectedTagName.isEmpty()) {
                    Tag tag = plugin.getTagManager().getTag(selectedTagName);
                    if (tag != null && player.hasPermission(tag.getPermission())) {
                        return ChatStorage.colorize(tag.getPrefix());
                    }
                }
            }
        }

        // 2. Se está com fake nick e não tem tag: usar prefixo neutro
        if (hasFakeNick) {
            return "§7";
        }

        // 3. Fallback: usar prefixo do grupo
        if (plugin.getGroupManager() != null) {
            Group group = plugin.getGroupManager().getPlayerGroup(player);
            if (group != null && !group.getPrefix().isEmpty()) {
                return ChatStorage.colorize(group.getPrefix());
            }
        }

        return "§7";
    }

    /**
     * Aplica os dados de exibição ao jogador (DisplayName e PlayerListName)
     */
    private void applyDisplayToPlayer(Player player, DisplayData data) {
        // DisplayName: usado no chat
        String fullDisplayName = data.getFullDisplayName();
        player.setDisplayName(fullDisplayName);

        // PlayerListName: o que aparece na TabList
        // IMPORTANTE: O prefixo/sufixo são aplicados pelo Team (via NametagManager)
        // O setPlayerListName deve conter APENAS o nome que será exibido
        // O Team concatena automaticamente: prefix + playerListName + suffix
        if (data.isUsingFakeNick()) {
            // Com fake nick: usar apenas o nome fake
            // O Team do NametagManager aplica prefixo e sufixo automaticamente
            player.setPlayerListName(data.getDisplayName());
        } else {
            // Sem fake nick: usar o nome real
            // O Team do NametagManager aplica prefixo e sufixo automaticamente
            player.setPlayerListName(player.getName());
        }
    }

    /**
     * Atualiza a nametag do jogador via NametagManager.
     * 
     * O NametagManager agora é responsável por:
     * - Aplicar prefixo (TAG/grupo)
     * - Aplicar sufixo (símbolo da liga)
     * - Propagar para todas as scoreboards
     */
    private void updateScoreboardTeam(Player player, DisplayData data) {
        // Delegar ao NametagManager que é agora o responsável central
        if (plugin.getNametagManager() != null) {
            plugin.getNametagManager().updateNametag(player);
            return;
        }

        // ============ FALLBACK (se NametagManager não estiver disponível) ============
        // Obter grupo para ordenação
        com.haumea.kitpvp.models.Group group = plugin.getGroupManager().getPlayerGroup(player);
        if (group == null) {
            group = plugin.getGroupManager().getGroup("membro");
        }

        // Calcular ordem (maior prioridade = menor número = aparece primeiro)
        int sortOrder = 9999 - (group != null ? group.getPriority() : 0);
        String teamName = String.format("%04d_%s", sortOrder, group != null ? group.getName() : "membro");

        // Limitar a 16 caracteres
        if (teamName.length() > 16) {
            teamName = teamName.substring(0, 16);
        }

        String teamPrefix = data.getPrefix();
        if (teamPrefix.length() > 16) {
            teamPrefix = smartTruncate(teamPrefix, 16);
        }

        // Aplicar apenas na mainScoreboard como fallback
        Scoreboard mainScoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        applyTeamToScoreboard(mainScoreboard, player, teamName, teamPrefix, "");
    }

    /**
     * Extrai o último código de cor de uma string formatada.
     * Usado para garantir que a cor do nome seja preservada.
     */
    private String extractLastColorCode(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        String lastColor = "";
        for (int i = 0; i < text.length() - 1; i++) {
            if (text.charAt(i) == '§') {
                char code = text.charAt(i + 1);
                // Apenas códigos de cor (0-9, a-f), não formatação (l, m, n, o, k, r)
                if ((code >= '0' && code <= '9') || (code >= 'a' && code <= 'f')) {
                    lastColor = "§" + code;
                }
            }
        }
        return lastColor;
    }

    /**
     * Trunca uma string de forma inteligente, evitando cortar no meio de códigos de
     * cor.
     */
    private String smartTruncate(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }

        // Verificar se cortaria no meio de um código de cor
        String truncated = text.substring(0, maxLength);

        // Se o último caractere é §, remover para não ficar código incompleto
        if (truncated.endsWith("§")) {
            truncated = truncated.substring(0, truncated.length() - 1);
        }

        return truncated;
    }

    /**
     * Aplica um team a um scoreboard específico.
     * 
     * NOTA: Este é um FALLBACK quando o NametagManager não está disponível.
     * O NametagManager é o responsável principal por teams.
     * 
     * CORREÇÃO: Adiciona APENAS o nome que o cliente está vendo (fake ou real).
     */
    private void applyTeamToScoreboard(Scoreboard scoreboard, Player player, String teamName, String prefix,
            String suffix) {
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

        // Remover jogador de teams antigos (ambos os nomes para limpeza completa)
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

        // Configurar suffix
        if (suffix != null && !suffix.isEmpty()) {
            team.setSuffix(suffix);
        }

        // CORREÇÃO: Adicionar APENAS o nome que o cliente está vendo
        team.addEntry(nameToUse);
    }

    // ==================== EVENTOS DE CICLO DE VIDA ====================

    /**
     * Inicializa os dados de exibição quando o jogador entra.
     * IMPORTANTE: Também aplica os teams de todos os outros jogadores
     * na scoreboard do novo jogador para que ele veja as tags/nametags corretas.
     */
    public void onPlayerJoin(Player player) {
        // Criar dados iniciais
        DisplayData data = new DisplayData(player.getName());
        displayCache.put(player.getUniqueId(), data);

        // Atualizar após um pequeno delay para garantir que tudo foi carregado
        // (incluindo a scoreboard criada pelo ScoreboardManager)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                // 1. Atualizar este jogador (aplica o team dele em todas as scoreboards)
                refreshPlayer(player);

                // 2. CRÍTICO: Aplicar os teams de TODOS os outros jogadores online
                // na scoreboard do novo jogador para que ele veja as tags deles!
                Scoreboard playerScoreboard = player.getScoreboard();
                if (playerScoreboard != null) {
                    for (Player otherPlayer : Bukkit.getOnlinePlayers()) {
                        if (!otherPlayer.equals(player)) {
                            // Obter dados do outro jogador e aplicar seu team nesta scoreboard
                            DisplayData otherData = displayCache.get(otherPlayer.getUniqueId());
                            if (otherData != null) {
                                applyOtherPlayerTeam(otherPlayer, otherData, playerScoreboard);
                            }
                        }
                    }
                }
            }
        }, 10L); // 10 ticks = 0.5s (dar mais tempo para scoreboard ser criada)
    }

    /**
     * Aplica o team de um jogador em uma scoreboard específica.
     * Usado para aplicar teams de jogadores já online na scoreboard de um novo
     * jogador.
     * 
     * NOTA: Este método agora delega ao NametagManager quando disponível.
     */
    private void applyOtherPlayerTeam(Player player, DisplayData data, Scoreboard targetScoreboard) {
        // Delegar ao NametagManager se disponível
        if (plugin.getNametagManager() != null) {
            // O NametagManager já aplicou em todas as scoreboards
            // Basta atualizar a nametag deste jogador
            plugin.getNametagManager().updateNametag(player);
            return;
        }

        // ============ FALLBACK ============
        // Obter grupo para ordenação
        com.haumea.kitpvp.models.Group group = plugin.getGroupManager().getPlayerGroup(player);
        if (group == null) {
            group = plugin.getGroupManager().getGroup("membro");
        }

        // Calcular ordem
        int sortOrder = 9999 - (group != null ? group.getPriority() : 0);
        String teamName = String.format("%04d_%s", sortOrder, group != null ? group.getName() : "membro");
        if (teamName.length() > 16) {
            teamName = teamName.substring(0, 16);
        }

        String teamPrefix = data.getPrefix();
        if (teamPrefix.length() > 16) {
            teamPrefix = smartTruncate(teamPrefix, 16);
        }

        // Aplicar na scoreboard alvo
        applyTeamToScoreboard(targetScoreboard, player, teamName, teamPrefix, "");
    }

    /**
     * Remove os dados de exibição quando o jogador sai
     */
    public void onPlayerQuit(Player player) {
        displayCache.remove(player.getUniqueId());
    }

    // ==================== MÉTODOS DE CONVENIÊNCIA ====================

    /**
     * Atualiza todos os jogadores online
     */
    public void refreshAllPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            refreshPlayer(player);
        }
    }

    /**
     * Obtém o nome que deve ser exibido para um jogador
     * Usado pelo ChatListener e outros sistemas
     */
    public String getNameToDisplay(Player player) {
        DisplayData data = displayCache.get(player.getUniqueId());
        if (data == null) {
            return player.getName();
        }
        return data.getNameToShow();
    }

    /**
     * Obtém o prefixo atual de um jogador
     * Usado pelo ChatListener
     */
    public String getCurrentPrefix(Player player) {
        DisplayData data = displayCache.get(player.getUniqueId());
        if (data == null) {
            // Fallback: calcular na hora
            boolean hasFakeNick = plugin.getFakeNickManager() != null
                    && plugin.getFakeNickManager().hasFakeNick(player);
            return calculatePrefix(player, hasFakeNick);
        }
        return data.getPrefix();
    }

    /**
     * Obtém o display name completo (medalha + prefixo + nome)
     */
    public String getFullDisplayName(Player player) {
        DisplayData data = displayCache.get(player.getUniqueId());
        if (data == null) {
            return player.getName();
        }
        return data.getFullDisplayName();
    }

    /**
     * Obtém a medalha do jogador formatada
     */
    public String getMedalDisplay(Player player) {
        DisplayData data = displayCache.get(player.getUniqueId());
        if (data == null) {
            if (plugin.getMedalManager() != null) {
                return plugin.getMedalManager().getPlayerMedalDisplay(player);
            }
            return "";
        }
        return data.getMedalDisplay();
    }

    /**
     * Obtém o símbolo da liga do jogador
     */
    public String getLeagueSymbol(Player player) {
        DisplayData data = displayCache.get(player.getUniqueId());
        if (data == null) {
            // Fallback: calcular na hora
            if (plugin.getLeagueManager() != null) {
                com.haumea.kitpvp.models.PlayerRank rank = plugin.getLeagueManager().getRank(player);
                if (rank != null) {
                    return rank.getSymbol() + " ";
                }
            }
            return "";
        }
        return data.getLeagueSymbol();
    }

    /**
     * Verifica se o jogador está usando fake nick
     */
    public boolean isUsingFakeNick(Player player) {
        DisplayData data = displayCache.get(player.getUniqueId());
        if (data == null) {
            return plugin.getFakeNickManager() != null
                    && plugin.getFakeNickManager().hasFakeNick(player);
        }
        return data.isUsingFakeNick();
    }

    // ==================== MÉTODOS DE NOTIFICAÇÃO ====================

    /**
     * Deve ser chamado quando o fake nick é alterado.
     * Faz refresh completo incluindo visibilidade para atualizar a nametag.
     */
    public void onFakeNickChange(Player player) {
        refreshPlayer(player);

        // Forçar atualização da nametag escondendo/mostrando o jogador
        refreshPlayerVisibility(player);
    }

    /**
     * Atualiza a visibilidade do jogador para todos (esconde e mostra)
     * Isso força a atualização do nametag no cliente
     */
    private void refreshPlayerVisibility(Player player) {
        // Executar no próximo tick para garantir sincronização
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline())
                return;

            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.equals(player))
                    continue;

                // Esconder jogador
                online.hidePlayer(player);
            }

            // Mostrar novamente após 2 ticks
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!player.isOnline())
                    return;

                for (Player online : Bukkit.getOnlinePlayers()) {
                    if (online.equals(player))
                        continue;
                    online.showPlayer(player);
                }
            }, 2L);
        }, 1L);
    }

    /**
     * Deve ser chamado quando a tag é alterada
     */
    public void onTagChange(Player player) {
        refreshPlayer(player);

        // Se o jogador tem fake nick, atualizar pacotes para mostrar a nova tag na
        // nametag
        if (plugin.getFakeNickManager() != null && plugin.getFakeNickManager().hasFakeNick(player)) {
            if (plugin.getFakeNickPacketListener() != null) {
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline()) {
                        plugin.getFakeNickPacketListener().refreshPlayerInfo(player);
                    }
                }, 3L);
            }
        }
    }

    /**
     * Deve ser chamado quando a medalha é alterada
     */
    public void onMedalChange(Player player) {
        refreshPlayer(player);
    }

    /**
     * Deve ser chamado quando o grupo é alterado
     */
    public void onGroupChange(Player player) {
        refreshPlayer(player);
    }
}
