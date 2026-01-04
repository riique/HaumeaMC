package com.haumea.kitpvp.managers;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.models.Group;
import com.haumea.kitpvp.models.PlayerRank;
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
 * Gerenciador centralizado de Nametags do HaumeaMC.
 * 
 * Responsável por:
 * - Gerenciar Teams do Scoreboard para ordenação e exibição
 * - Aplicar Prefixo (TAG do grupo/jogador)
 * - Aplicar Sufixo (Símbolo da Liga)
 * - Propagar Teams para todas as Scoreboards de todos os jogadores
 * 
 * BYPASS DO LIMITE DE 16 CARACTERES:
 * O Minecraft 1.8 limita cada campo (prefix, suffix, team name) a 16 chars.
 * Porém, o display visual combina: PREFIXO (16) + NOME + SUFIXO (16).
 * Isso nos permite exibir até 32 chars customizáveis + nome do jogador.
 * 
 * INTEGRAÇÃO COM FAKE NICK:
 * - O PREFIXO respeita: TAG selecionada > (se fake) neutro > prefixo do grupo
 * - O SUFIXO sempre mostra a liga ORIGINAL (baseada em UUID, não muda com fake)
 * - A ORDENAÇÃO no TAB usa a prioridade real do grupo (não afetada pelo fake)
 * 
 * Estrutura do Team Name (ID interno):
 * - Formato: {prioridade}_{uuid} cortado em 16 chars
 * - Isso garante ordenação correta no TAB e unicidade
 * 
 * @author HaumeaMC
 */
public class NametagManager {

    private final HaumeaMC plugin;

    // Cache de dados de nametag por jogador
    private final Map<UUID, NametagData> nametagCache;

    // Limite de caracteres do Minecraft 1.8
    private static final int MAX_TEAM_NAME_LENGTH = 16;
    private static final int MAX_PREFIX_LENGTH = 16;
    private static final int MAX_SUFFIX_LENGTH = 16;

    public NametagManager(HaumeaMC plugin) {
        this.plugin = plugin;
        this.nametagCache = new ConcurrentHashMap<>();
        plugin.getLogger().info("[NametagManager] Sistema de Nametags inicializado!");
    }

    // ==================== CLASSES INTERNAS ====================

    /**
     * Dados de nametag de um jogador
     */
    public static class NametagData {
        private String prefix; // TAG colorida (ex: "§d§lVIP §f")
        private String suffix; // Símbolo da liga (ex: " §6✹")
        private String teamName; // ID do team para ordenação
        private int priority; // Prioridade do grupo (para ordenação)

        public NametagData() {
            this.prefix = "§7";
            this.suffix = "";
            this.teamName = "";
            this.priority = 0;
        }

        // Getters
        public String getPrefix() {
            return prefix;
        }

        public String getSuffix() {
            return suffix;
        }

        public String getTeamName() {
            return teamName;
        }

        public int getPriority() {
            return priority;
        }

        // Setters (package-private)
        void setPrefix(String prefix) {
            this.prefix = prefix;
        }

        void setSuffix(String suffix) {
            this.suffix = suffix;
        }

        void setTeamName(String teamName) {
            this.teamName = teamName;
        }

        void setPriority(int priority) {
            this.priority = priority;
        }
    }

    // ==================== MÉTODOS PRINCIPAIS ====================

    /**
     * Atualiza completamente a nametag de um jogador.
     * Calcula prefixo, sufixo e aplica em todas as scoreboards.
     * 
     * @param player Jogador a atualizar
     */
    public void updateNametag(Player player) {
        if (player == null || !player.isOnline())
            return;

        NametagData data = getOrCreateData(player);

        // 1. Calcular prioridade e team name
        Group group = plugin.getGroupManager().getPlayerGroup(player);
        if (group == null) {
            group = plugin.getGroupManager().getGroup("membro");
        }

        int priority = group != null ? group.getPriority() : 0;
        data.setPriority(priority);

        // Team name: menor número = aparece primeiro (invertemos a prioridade)
        // Formato: {sortOrder}_{uuid} truncado para satisfazer limite de 16 chars
        int sortOrder = 9999 - priority;
        String teamNameBase = String.format("%04d", sortOrder) + "_" + player.getUniqueId().toString();
        String teamName = teamNameBase.substring(0, Math.min(teamNameBase.length(), MAX_TEAM_NAME_LENGTH));
        data.setTeamName(teamName);

        // 2. Calcular PREFIXO (TAG selecionada ou grupo)
        String prefix = calculatePrefix(player, group);
        data.setPrefix(prefix);

        // 3. Calcular SUFIXO (Símbolo da Liga)
        String suffix = calculateSuffix(player);
        data.setSuffix(suffix);

        // 4. Aplicar em todas as scoreboards
        applyToAllScoreboards(player, data);
    }

    /**
     * Calcula o prefixo (TAG) do jogador.
     * 
     * PRIORIDADE:
     * 1. TAG selecionada manualmente (se tiver permissão)
     * 2. Se está com FAKE NICK e sem tag: prefixo neutro ($7) para anonimato
     * 3. Se NÃO está com fake: prefixo do grupo normalmente
     * 
     * Isso permite que jogadores com fake escolham usar uma tag (ex: VIP)
     * ou fiquem com prefixo neutro para ficarem anônimos.
     */
    private String calculatePrefix(Player player, Group group) {
        String prefix = "";
        boolean hasFakeNick = plugin.getFakeNickManager() != null
                && plugin.getFakeNickManager().hasFakeNick(player);

        // 1. Verificar TAG selecionada (prioridade máxima)
        // Jogadores com fake podem escolher uma tag manualmente
        if (plugin.getProfileManager() != null && plugin.getTagManager() != null) {
            PlayerProfile profile = plugin.getProfileManager().getProfile(player);
            if (profile != null) {
                String selectedTagName = profile.getData().getSelectedTag();
                if (selectedTagName != null && !selectedTagName.isEmpty()) {
                    Tag tag = plugin.getTagManager().getTag(selectedTagName);
                    if (tag != null && player.hasPermission(tag.getPermission())) {
                        prefix = ChatStorage.colorize(tag.getPrefix());
                    }
                }
            }
        }

        // 2. Se está com FAKE NICK e NÃO tem tag selecionada: usar prefixo neutro
        // Isso garante anonimato - o jogador não mostra seu cargo real
        if (prefix.isEmpty() && hasFakeNick) {
            return "\u00a77"; // Prefixo neutro (cinza) para anonimato
        }

        // 3. Se NÃO está com fake e não tem tag: usar prefixo do grupo
        if (prefix.isEmpty() && group != null && !group.getPrefix().isEmpty()) {
            prefix = ChatStorage.colorize(group.getPrefix());
        }

        // 4. Fallback final: cor neutra
        if (prefix.isEmpty()) {
            prefix = "\u00a77";
        }

        // 5. Truncar se necessário (inteligente, sem cortar códigos de cor)
        return smartTruncate(prefix, MAX_PREFIX_LENGTH);
    }

    /**
     * Calcula o sufixo (Símbolo da Liga) do jogador.
     * Formato: " {cor}{símbolo}" (ex: " §6✹")
     * 
     * IMPORTANTE - INTEGRAÇÃO COM FAKE NICK:
     * A liga é carregada pelo UUID do jogador (que NUNCA muda, mesmo com fake).
     * Isso significa que um jogador com fake nick ainda mostra sua liga REAL.
     * 
     * Exemplo:
     * - Jogador "Henrique" com ELO 2500 (Diamond)
     * - Usa /fake para virar "JoaozinhoPVP"
     * - Nametag: [§7] [JoaozinhoPVP] [§b✦]
     * - A liga Diamond continua aparecendo porque é baseada no UUID
     */
    private String calculateSuffix(Player player) {
        if (plugin.getLeagueManager() == null) {
            return "";
        }

        // A liga é obtida pelo UUID do jogador, que NUNCA muda com fake nick
        // Isso garante que o símbolo seja o da conta ORIGINAL
        PlayerRank rank = plugin.getLeagueManager().getRank(player);
        if (rank == null) {
            return "";
        }

        // Formato: espaço + símbolo colorizado
        // Ex: " §6✹" ou " §c✠"
        String suffix = " " + rank.getSymbol();

        // Truncar se necessário (muito improvável, símbolos são curtos)
        return smartTruncate(suffix, MAX_SUFFIX_LENGTH);
    }

    /**
     * Aplica os dados de nametag em TODAS as scoreboards de todos os jogadores
     * online.
     * Isso é necessário porque cada jogador tem sua própria scoreboard (criada pelo
     * ScoreboardManager).
     */
    private void applyToAllScoreboards(Player player, NametagData data) {
        // 1. Aplicar na main scoreboard
        Scoreboard mainScoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        applyTeamToScoreboard(mainScoreboard, player, data);

        // 2. Aplicar em todas as scoreboards individuais
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            Scoreboard playerScoreboard = onlinePlayer.getScoreboard();
            if (playerScoreboard != null && playerScoreboard != mainScoreboard) {
                applyTeamToScoreboard(playerScoreboard, player, data);
            }
        }
    }

    /**
     * Aplica o team de um jogador em uma scoreboard específica.
     * 
     * CORREÇÃO DO BUG DE FAKE NICK:
     * O cliente Minecraft associa o Team (prefix/suffix) à entry que CORRESPONDE
     * EXATAMENTE ao nome que aparece na TabList. Portanto:
     * - Se o jogador está com fake nick: addEntry(fakeNick)
     * - Se NÃO está com fake nick: addEntry(player.getName())
     * 
     * NÃO adicionar ambos os nomes, pois isso cria confusão na renderização.
     * 
     * @param scoreboard Scoreboard alvo
     * @param player     Jogador dono do team
     * @param data       Dados de nametag
     */
    private void applyTeamToScoreboard(Scoreboard scoreboard, Player player, NametagData data) {
        if (scoreboard == null)
            return;

        String playerName = player.getName();
        String teamName = data.getTeamName();
        String prefix = data.getPrefix();
        String suffix = data.getSuffix();

        // Obter fake nick se existir
        String fakeNick = null;
        boolean hasFakeNick = false;
        if (plugin.getFakeNickManager() != null && plugin.getFakeNickManager().hasFakeNick(player)) {
            fakeNick = plugin.getFakeNickManager().getFakeNick(player);
            hasFakeNick = true;
        }

        // Determinar qual nome usar como entry do Team
        // CRÍTICO: Deve ser o nome que o CLIENTE está vendo na TabList
        String nameToUse = hasFakeNick ? fakeNick : playerName;

        // Remover jogador de teams antigos (ambos os nomes para limpeza completa)
        for (Team existingTeam : scoreboard.getTeams()) {
            if (existingTeam.hasEntry(playerName)) {
                existingTeam.removeEntry(playerName);
            }
            if (fakeNick != null && existingTeam.hasEntry(fakeNick)) {
                existingTeam.removeEntry(fakeNick);
            }
        }

        // Criar ou obter team
        Team team = scoreboard.getTeam(teamName);
        if (team == null) {
            team = scoreboard.registerNewTeam(teamName);
        }

        // Configurar prefixo e sufixo
        team.setPrefix(prefix);
        team.setSuffix(suffix);

        // CORREÇÃO: Adicionar APENAS o nome que o cliente está vendo
        // Se adicionarmos ambos, o cliente pode não renderizar corretamente
        team.addEntry(nameToUse);
    }

    // ==================== MÉTODOS DE CICLO DE VIDA ====================

    /**
     * Inicializa nametag quando jogador entra.
     * Também aplica teams de todos os jogadores online na scoreboard do novo
     * jogador.
     */
    public void onPlayerJoin(Player player) {
        // Criar dados iniciais
        nametagCache.put(player.getUniqueId(), new NametagData());

        // Atualizar após delay (para garantir que scoreboard foi criada)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                // 1. Atualizar este jogador
                updateNametag(player);

                // 2. Aplicar teams de TODOS os outros jogadores na scoreboard deste
                Scoreboard playerScoreboard = player.getScoreboard();
                if (playerScoreboard != null) {
                    for (Player other : Bukkit.getOnlinePlayers()) {
                        if (!other.equals(player)) {
                            NametagData otherData = nametagCache.get(other.getUniqueId());
                            if (otherData != null) {
                                applyTeamToScoreboard(playerScoreboard, other, otherData);
                            }
                        }
                    }
                }
            }
        }, 10L);
    }

    /**
     * Remove dados quando jogador sai.
     * Remove tanto o nome original quanto o fake nick dos teams.
     */
    public void onPlayerQuit(Player player) {
        // Obter fake nick antes de remover do cache
        String fakeNick = null;
        if (plugin.getFakeNickManager() != null && plugin.getFakeNickManager().hasFakeNick(player)) {
            fakeNick = plugin.getFakeNickManager().getFakeNick(player);
        }

        nametagCache.remove(player.getUniqueId());

        // Remover dos teams da main scoreboard (ambos os nomes)
        Scoreboard mainScoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        for (Team team : mainScoreboard.getTeams()) {
            if (team.hasEntry(player.getName())) {
                team.removeEntry(player.getName());
            }
            if (fakeNick != null && team.hasEntry(fakeNick)) {
                team.removeEntry(fakeNick);
            }
        }
    }

    // ==================== MÉTODOS UTILITÁRIOS ====================

    /**
     * Obtém ou cria dados de nametag para um jogador.
     */
    private NametagData getOrCreateData(Player player) {
        return nametagCache.computeIfAbsent(player.getUniqueId(), uuid -> new NametagData());
    }

    /**
     * Obtém dados de nametag de um jogador (pode ser null).
     */
    public NametagData getNametagData(Player player) {
        return nametagCache.get(player.getUniqueId());
    }

    /**
     * Trunca uma string de forma inteligente, evitando cortar códigos de cor.
     */
    private String smartTruncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }

        String truncated = text.substring(0, maxLength);

        // Se termina com §, remover para não deixar código incompleto
        if (truncated.endsWith("§")) {
            truncated = truncated.substring(0, truncated.length() - 1);
        }

        return truncated;
    }

    /**
     * Atualiza nametags de todos os jogadores online.
     */
    public void refreshAllNametags() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updateNametag(player);
        }
    }

    /**
     * Limpa teams vazios (manutenção).
     */
    public void cleanupEmptyTeams() {
        Scoreboard mainScoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        for (Team team : mainScoreboard.getTeams()) {
            // Teams criados por nós começam com número de ordenação
            if (team.getName().matches("\\d{4}_.*") && team.getSize() == 0) {
                team.unregister();
            }
        }
    }

    // ==================== GETTERS ====================

    /**
     * Obtém o prefixo atual de um jogador.
     */
    public String getPrefix(Player player) {
        NametagData data = nametagCache.get(player.getUniqueId());
        return data != null ? data.getPrefix() : "§7";
    }

    /**
     * Obtém o sufixo atual de um jogador.
     */
    public String getSuffix(Player player) {
        NametagData data = nametagCache.get(player.getUniqueId());
        return data != null ? data.getSuffix() : "";
    }
}
