package com.haumea.kitpvp.managers;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.models.Tag;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gerenciador de Tags do plugin
 * Responsável por carregar, gerenciar e aplicar tags aos jogadores
 * 
 * @author HaumeaMC
 */
public class TagManager {

    private final HaumeaMC plugin;

    // Map de tags carregadas (nome -> Tag)
    private final Map<String, Tag> tags;

    // Map de tags ativas dos jogadores (UUID -> nome da tag)
    private final Map<UUID, String> playerTags;

    // Flag para lazy loading - tags só são carregadas quando necessárias
    private volatile boolean tagsLoaded = false;

    public TagManager(HaumeaMC plugin) {
        this.plugin = plugin;
        this.tags = new LinkedHashMap<>();
        this.playerTags = new ConcurrentHashMap<>();
        // NÃO carrega tags aqui - será lazy loaded
        // loadTags(); // REMOVIDO para lazy loading
        plugin.getLogger().info("[TagManager] Inicializado (lazy loading ativado)");
    }

    /**
     * Garante que as tags estão carregadas (lazy loading)
     */
    private void ensureLoaded() {
        if (!tagsLoaded) {
            synchronized (tags) {
                if (!tagsLoaded) {
                    loadTagsInternal();
                    tagsLoaded = true;
                }
            }
        }
    }

    /**
     * Carrega todas as tags do config.yml
     * Chamado publicamente para forçar reload
     */
    public void loadTags() {
        synchronized (tags) {
            loadTagsInternal();
            tagsLoaded = true;
        }
    }

    /**
     * Carrega tags internamente
     */
    private void loadTagsInternal() {
        tags.clear();

        ConfigurationSection section = plugin.getConfig().getConfigurationSection("tags.lista");
        if (section == null)
            return;

        for (String key : section.getKeys(false)) {
            String display = section.getString(key + ".display", "§7" + key.toUpperCase());
            String prefixo = section.getString(key + ".prefixo", "§7");
            String permissao = section.getString(key + ".permissao", "haumea.tag." + key);
            String categoria = section.getString(key + ".categoria", "comum");

            Tag tag = new Tag(key, display, prefixo, permissao, categoria);
            tags.put(key.toLowerCase(), tag);
        }

        plugin.getLogger().info("[TagManager] " + tags.size() + " tags carregadas.");
    }

    /**
     * Obtém todas as tags carregadas
     * 
     * @return Collection de Tags
     */
    public Collection<Tag> getAllTags() {
        ensureLoaded();
        return tags.values();
    }

    /**
     * Obtém uma tag pelo nome
     * 
     * @param name Nome da tag
     * @return Tag ou null se não existir
     */
    public Tag getTag(String name) {
        ensureLoaded();
        return tags.get(name.toLowerCase());
    }

    /**
     * Verifica se uma tag existe
     * 
     * @param name Nome da tag
     * @return boolean
     */
    public boolean tagExists(String name) {
        ensureLoaded();
        return tags.containsKey(name.toLowerCase());
    }

    /**
     * Obtém as tags disponíveis para um jogador
     * 
     * @param player Jogador
     * @return Lista de tags que o jogador tem permissão
     */
    public List<Tag> getAvailableTags(Player player) {
        List<Tag> available = new ArrayList<>();

        for (Tag tag : tags.values()) {
            // Verificar permissão via GroupManager ou Bukkit
            if (hasTagPermission(player, tag.getPermission())) {
                available.add(tag);
            }
        }

        return available;
    }

    /**
     * Verifica se o jogador tem permissão para uma tag
     * Usa o GroupManager se disponível, senão usa permissões do Bukkit
     */
    private boolean hasTagPermission(Player player, String permission) {
        // Tentar via GroupManager primeiro
        if (plugin.getGroupManager() != null) {
            if (plugin.getGroupManager().hasPermission(player, permission)) {
                return true;
            }
        }
        // Fallback para permissões do Bukkit/OP
        return player.hasPermission(permission);
    }

    /**
     * Define a tag ativa de um jogador
     * PERSISTE a escolha no PlayerData para manter entre sessões!
     * 
     * @param player  Jogador
     * @param tagName Nome da tag
     * @return true se a tag foi definida com sucesso
     */
    public boolean setPlayerTag(Player player, String tagName) {
        Tag tag = getTag(tagName);
        if (tag == null)
            return false;

        if (!player.hasPermission(tag.getPermission()))
            return false;

        // Mapa em memória (para sessão)
        playerTags.put(player.getUniqueId(), tagName.toLowerCase());

        // PERSISTÊNCIA: Salvar a tag escolhida no PlayerData (fonte única de verdade)
        if (plugin.getProfileManager() != null) {
            com.haumea.kitpvp.profile.PlayerProfile profile = plugin.getProfileManager().getProfile(player);
            if (profile != null) {
                profile.getData().setSelectedTag(tagName.toLowerCase());
            }
        }

        // Notificar DisplayManager para atualização unificada
        if (plugin.getDisplayManager() != null) {
            plugin.getDisplayManager().onTagChange(player);
        } else {
            // Fallback: atualizar diretamente
            updatePlayerDisplay(player, tag);
        }

        return true;
    }

    /**
     * Obtém a tag ativa de um jogador
     * FONTE ÚNICA DE VERDADE: PlayerData.selectedTag
     * 
     * @param player Jogador
     * @return Tag ativa ou null
     */
    public Tag getPlayerTag(Player player) {
        // PRIMEIRO: Verificar PlayerData (fonte persistente)
        if (plugin.getProfileManager() != null) {
            com.haumea.kitpvp.profile.PlayerProfile profile = plugin.getProfileManager().getProfile(player);
            if (profile != null) {
                String savedTag = profile.getData().getSelectedTag();
                if (savedTag != null && !savedTag.isEmpty()) {
                    Tag tag = getTag(savedTag);
                    if (tag != null && player.hasPermission(tag.getPermission())) {
                        return tag;
                    }
                }
            }
        }

        // FALLBACK: Mapa em memória
        String tagName = playerTags.get(player.getUniqueId());
        if (tagName != null) {
            Tag tag = getTag(tagName);
            if (tag != null && player.hasPermission(tag.getPermission())) {
                return tag;
            }
        }

        // ÚLTIMO FALLBACK: Tag padrão (null = usar prefixo do grupo)
        return null;
    }

    /**
     * Atualiza o display do jogador com a tag
     * NOTA: Este método é mantido para fallback, mas o DisplayManager deve ser
     * usado
     * 
     * @param player Jogador
     * @param tag    Tag a ser aplicada
     */
    private void updatePlayerDisplay(Player player, Tag tag) {
        String prefix = tag.getPrefix().replace("&", "§");
        String displayName = prefix + player.getName();

        player.setDisplayName(displayName);
        player.setPlayerListName(displayName);
    }

    /**
     * Obtém o título do menu de tags
     * 
     * @return String título formatado
     */
    public String getMenuTitle() {
        return plugin.getConfig().getString("tags.menu.titulo", "&eTAGS &fSelecione uma das &eTAGS &fListadas abaixo!");
    }

    /**
     * Obtém a mensagem de uso do comando tag
     * 
     * @return String mensagem de uso
     */
    public String getUsageMessage() {
        return plugin.getConfig().getString("tags.menu.uso", "&fUse: &e/tag <nome da tag>");
    }

    /**
     * Recarrega as tags
     */
    public void reload() {
        loadTags();
    }
}
