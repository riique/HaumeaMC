package com.haumea.kitpvp.managers;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.profile.PlayerProfile;
import com.haumea.kitpvp.utils.ChatStorage;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gerenciador do Sistema de Ignore do HaumeaMC.
 * 
 * Permite que jogadores ignorem outros jogadores,
 * bloqueando mensagens privadas e menções no chat.
 * 
 * Persistência: Salva lista de ignorados no customData do PlayerData.
 * 
 * @author HaumeaMC
 */
public class IgnoreManager {

    private final HaumeaMC plugin;

    // Cache em memória: UUID do jogador -> Set de UUIDs ignorados
    private final Map<UUID, Set<UUID>> ignoreCache;

    // Chave para persistência no customData
    private static final String DATA_KEY_IGNORED = "ignored_players";

    // Limite máximo de jogadores que podem ser ignorados
    private static final int MAX_IGNORED_PLAYERS = 100;

    public IgnoreManager(HaumeaMC plugin) {
        this.plugin = plugin;
        this.ignoreCache = new ConcurrentHashMap<>();
        plugin.getLogger().info("[IgnoreManager] Sistema de Ignore inicializado.");
    }

    // ==================== CORE METHODS ====================

    /**
     * Adiciona um jogador à lista de ignorados.
     * 
     * @param player Jogador que está ignorando
     * @param target Jogador a ser ignorado
     * @return true se adicionado com sucesso
     */
    public boolean addIgnore(Player player, Player target) {
        if (player.equals(target)) {
            ChatStorage.send(player, "ignore.error.self");
            return false;
        }

        UUID playerUuid = player.getUniqueId();
        UUID targetUuid = target.getUniqueId();

        Set<UUID> ignored = getIgnoredSet(playerUuid);

        if (ignored.size() >= MAX_IGNORED_PLAYERS) {
            ChatStorage.send(player, "ignore.error.limit", "limit", String.valueOf(MAX_IGNORED_PLAYERS));
            return false;
        }

        if (ignored.contains(targetUuid)) {
            ChatStorage.send(player, "ignore.error.already", "player", target.getName());
            return false;
        }

        ignored.add(targetUuid);
        saveIgnoredList(player);

        ChatStorage.send(player, "ignore.added", "player", target.getName());
        return true;
    }

    /**
     * Remove um jogador da lista de ignorados.
     * 
     * @param player Jogador que está des-ignorando
     * @param target Jogador a ser removido da lista
     * @return true se removido com sucesso
     */
    public boolean removeIgnore(Player player, Player target) {
        UUID playerUuid = player.getUniqueId();
        UUID targetUuid = target.getUniqueId();

        Set<UUID> ignored = getIgnoredSet(playerUuid);

        if (!ignored.contains(targetUuid)) {
            ChatStorage.send(player, "ignore.error.not-ignoring", "player", target.getName());
            return false;
        }

        ignored.remove(targetUuid);
        saveIgnoredList(player);

        ChatStorage.send(player, "ignore.removed", "player", target.getName());
        return true;
    }

    /**
     * Remove um jogador da lista de ignorados pelo UUID.
     * 
     * @param player     Jogador que está des-ignorando
     * @param targetUuid UUID do jogador a ser removido
     * @param targetName Nome do jogador (para mensagem)
     * @return true se removido com sucesso
     */
    public boolean removeIgnore(Player player, UUID targetUuid, String targetName) {
        UUID playerUuid = player.getUniqueId();
        Set<UUID> ignored = getIgnoredSet(playerUuid);

        if (!ignored.contains(targetUuid)) {
            ChatStorage.send(player, "ignore.error.not-ignoring", "player", targetName);
            return false;
        }

        ignored.remove(targetUuid);
        saveIgnoredList(player);

        ChatStorage.send(player, "ignore.removed", "player", targetName);
        return true;
    }

    /**
     * Verifica se um jogador está ignorando outro.
     * 
     * @param player Jogador que potencialmente está ignorando
     * @param target Jogador que potencialmente está sendo ignorado
     * @return true se player está ignorando target
     */
    public boolean isIgnoring(Player player, Player target) {
        return isIgnoring(player.getUniqueId(), target.getUniqueId());
    }

    /**
     * Verifica se um jogador está ignorando outro (versão UUID).
     * 
     * @param playerUuid UUID do jogador que potencialmente está ignorando
     * @param targetUuid UUID do jogador que potencialmente está sendo ignorado
     * @return true se está ignorando
     */
    public boolean isIgnoring(UUID playerUuid, UUID targetUuid) {
        Set<UUID> ignored = ignoreCache.get(playerUuid);
        return ignored != null && ignored.contains(targetUuid);
    }

    /**
     * Obtém a lista de jogadores ignorados.
     * 
     * @param player Jogador
     * @return Set de UUIDs ignorados (nunca null)
     */
    public Set<UUID> getIgnoredPlayers(Player player) {
        return getIgnoredSet(player.getUniqueId());
    }

    /**
     * Limpa a lista de ignorados de um jogador.
     * 
     * @param player Jogador
     */
    public void clearIgnoreList(Player player) {
        UUID playerUuid = player.getUniqueId();
        Set<UUID> ignored = ignoreCache.get(playerUuid);

        if (ignored != null && !ignored.isEmpty()) {
            ignored.clear();
            saveIgnoredList(player);
            ChatStorage.send(player, "ignore.cleared");
        } else {
            ChatStorage.send(player, "ignore.error.empty");
        }
    }

    // ==================== PERSISTENCE ====================

    /**
     * Carrega a lista de ignorados do PlayerData para o cache.
     * Chamado quando o jogador entra no servidor.
     * 
     * @param player Jogador
     */
    public void loadIgnoredList(Player player) {
        PlayerProfile profile = plugin.getProfileManager().getProfile(player);
        if (profile == null)
            return;

        Set<UUID> ignored = new HashSet<>();

        // Carregar do customData
        Object data = profile.getData().getCustomData(DATA_KEY_IGNORED);
        if (data instanceof List) {
            List<?> list = (List<?>) data;
            for (Object item : list) {
                if (item instanceof String) {
                    try {
                        ignored.add(UUID.fromString((String) item));
                    } catch (IllegalArgumentException e) {
                        // UUID inválido, ignorar
                    }
                }
            }
        }

        ignoreCache.put(player.getUniqueId(), ignored);
    }

    /**
     * Salva a lista de ignorados no PlayerData.
     * 
     * @param player Jogador
     */
    public void saveIgnoredList(Player player) {
        PlayerProfile profile = plugin.getProfileManager().getProfile(player);
        if (profile == null)
            return;

        Set<UUID> ignored = ignoreCache.get(player.getUniqueId());
        if (ignored == null) {
            ignored = new HashSet<>();
        }

        // Converter para lista de strings para salvar
        List<String> uuidStrings = new ArrayList<>();
        for (UUID uuid : ignored) {
            uuidStrings.add(uuid.toString());
        }

        profile.getData().setCustomData(DATA_KEY_IGNORED, uuidStrings);
    }

    /**
     * Remove o cache do jogador quando ele sai.
     * 
     * @param player Jogador
     */
    public void unloadPlayer(Player player) {
        // Salvar antes de remover do cache
        saveIgnoredList(player);
        ignoreCache.remove(player.getUniqueId());
    }

    // ==================== HELPERS ====================

    /**
     * Obtém ou cria o Set de jogadores ignorados.
     */
    private Set<UUID> getIgnoredSet(UUID playerUuid) {
        return ignoreCache.computeIfAbsent(playerUuid, k -> new HashSet<>());
    }

    /**
     * Obtém a quantidade de jogadores ignorados.
     * 
     * @param player Jogador
     * @return Quantidade de jogadores ignorados
     */
    public int getIgnoreCount(Player player) {
        Set<UUID> ignored = ignoreCache.get(player.getUniqueId());
        return ignored != null ? ignored.size() : 0;
    }

    /**
     * Obtém o limite máximo de jogadores que podem ser ignorados.
     * 
     * @return Limite máximo
     */
    public int getMaxIgnoredPlayers() {
        return MAX_IGNORED_PLAYERS;
    }
}
