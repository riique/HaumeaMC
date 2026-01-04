package com.haumea.kitpvp.managers;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.database.MongoManager;
import com.haumea.kitpvp.database.MongoPlayerRepository;
import com.haumea.kitpvp.models.PlayerData;
import com.haumea.kitpvp.profile.PlayerProfile;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manager central para gerenciamento de perfis de jogadores.
 * 
 * Este manager é responsável por:
 * - Carregar/criar perfis quando jogadores entram
 * - Salvar/remover perfis quando jogadores saem
 * - Fornecer acesso instantâneo a perfis por nome ou UUID
 * 
 * Agora utiliza MongoDB para persistência através do MongoPlayerRepository.
 * 
 * @author HaumeaMC
 */
public class ProfileManager {

    private final HaumeaMC plugin;
    private final MongoPlayerRepository repository;

    // Cache de perfis online - acesso O(1) por UUID
    private final Map<UUID, PlayerProfile> profilesByUUID;
    // Cache de nomes para UUIDs - acesso O(1) por nome
    private final Map<String, UUID> nameToUUID;

    /**
     * Construtor do ProfileManager
     * 
     * @param plugin       Instância do plugin principal
     * @param mongoManager Manager do MongoDB
     */
    public ProfileManager(HaumeaMC plugin, MongoManager mongoManager) {
        this.plugin = plugin;
        this.repository = new MongoPlayerRepository(plugin, mongoManager);
        this.profilesByUUID = new ConcurrentHashMap<>();
        this.nameToUUID = new ConcurrentHashMap<>();

        // Iniciar auto-save periódico
        startAutoSaveTask();
    }

    /**
     * Inicia task de auto-save a cada 5 minutos
     */
    private void startAutoSaveTask() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            saveAllProfiles();
        }, 20L * 60L * 5L, 20L * 60L * 5L); // A cada 5 minutos
    }

    // ==================== GERENCIAMENTO DE SESSÃO ====================

    /**
     * Carrega o perfil de um jogador quando ele entra no servidor.
     * Deve ser chamado no PlayerJoinEvent.
     * 
     * @param player Jogador que entrou
     * @return PlayerProfile carregado/criado
     */
    public PlayerProfile loadProfile(Player player) {
        UUID uuid = player.getUniqueId();
        String name = player.getName();

        // Verificar se já existe (reconexão rápida)
        if (profilesByUUID.containsKey(uuid)) {
            plugin.getLogger().warning("Perfil já existia para " + name + ", recarregando...");
            unloadProfile(player);
        }

        // Carregar dados do MongoDB
        PlayerData data = repository.load(uuid, name);

        // Atualizar último join
        data.setLastJoin(System.currentTimeMillis());

        // Criar perfil
        PlayerProfile profile = new PlayerProfile(player, data);

        // Registrar nos caches
        profilesByUUID.put(uuid, profile);
        nameToUUID.put(name.toLowerCase(), uuid);

        plugin.getLogger().info("[MongoDB] Perfil carregado: " + name);
        return profile;
    }

    /**
     * Descarrega o perfil de um jogador quando ele sai do servidor.
     * Salva os dados antes de remover. Deve ser chamado no PlayerQuitEvent.
     * 
     * @param player Jogador que saiu
     */
    public void unloadProfile(Player player) {
        UUID uuid = player.getUniqueId();
        String name = player.getName();

        PlayerProfile profile = profilesByUUID.remove(uuid);
        nameToUUID.remove(name.toLowerCase());

        if (profile != null) {
            // Preparar dados para salvamento (atualizar tempo de jogo, etc.)
            profile.prepareForSave();

            // Salvar no MongoDB de forma assíncrona
            repository.saveAsync(profile.getData()).thenAccept(success -> {
                if (success) {
                    plugin.getLogger().info("[MongoDB] Perfil salvo: " + name);
                } else {
                    plugin.getLogger().severe("[MongoDB] Falha ao salvar perfil: " + name);
                }
            });
        }
    }

    /**
     * Salva todos os perfis online sem descarregá-los.
     * Usa batch save para melhor performance.
     * Útil para auto-save periódico.
     */
    public void saveAllProfiles() {
        if (profilesByUUID.isEmpty()) {
            return;
        }

        // Preparar todos os dados para save
        List<PlayerData> dataToSave = new ArrayList<>();
        for (PlayerProfile profile : profilesByUUID.values()) {
            profile.prepareForSave();
            dataToSave.add(profile.getData());
        }

        // Usar batch save (muito mais eficiente)
        int saved = repository.saveBatch(dataToSave);

        if (saved > 0) {
            plugin.getLogger().info("[MongoDB] Auto-save: " + saved + "/" + dataToSave.size() + " perfis salvos.");
        }
    }

    /**
     * Descarrega todos os perfis (usado no onDisable do plugin)
     */
    public void unloadAllProfiles() {
        plugin.getLogger().info("[MongoDB] Salvando todos os perfis antes de desligar...");

        for (Map.Entry<UUID, PlayerProfile> entry : profilesByUUID.entrySet()) {
            PlayerProfile profile = entry.getValue();
            profile.prepareForSave();
            repository.save(profile.getData());
        }

        int count = profilesByUUID.size();
        profilesByUUID.clear();
        nameToUUID.clear();

        plugin.getLogger().info("[MongoDB] " + count + " perfis salvos e descarregados.");
    }

    // ==================== BUSCA DE PERFIS ====================

    /**
     * Obtém o perfil de um jogador por UUID.
     * Acesso O(1) - instantâneo.
     * 
     * @param uuid UUID do jogador
     * @return PlayerProfile ou null se não estiver online
     */
    public PlayerProfile getProfile(UUID uuid) {
        return profilesByUUID.get(uuid);
    }

    /**
     * Obtém o perfil de um jogador por nome.
     * Acesso O(1) - instantâneo.
     * Nome é case-insensitive.
     * 
     * @param name Nome do jogador
     * @return PlayerProfile ou null se não estiver online
     */
    public PlayerProfile getProfile(String name) {
        UUID uuid = nameToUUID.get(name.toLowerCase());
        if (uuid == null)
            return null;
        return profilesByUUID.get(uuid);
    }

    /**
     * Obtém o perfil de um jogador pelo objeto Player.
     * 
     * @param player Player do Bukkit
     * @return PlayerProfile ou null se não estiver registrado
     */
    public PlayerProfile getProfile(Player player) {
        return getProfile(player.getUniqueId());
    }

    /**
     * Verifica se um jogador tem perfil carregado (está online).
     * 
     * @param uuid UUID do jogador
     * @return true se está online e com perfil carregado
     */
    public boolean hasProfile(UUID uuid) {
        return profilesByUUID.containsKey(uuid);
    }

    /**
     * Verifica se um jogador tem perfil carregado (está online).
     * 
     * @param name Nome do jogador
     * @return true se está online e com perfil carregado
     */
    public boolean hasProfile(String name) {
        return nameToUUID.containsKey(name.toLowerCase());
    }

    /**
     * Obtém todos os perfis online.
     * 
     * @return Coleção de todos os perfis online
     */
    public Collection<PlayerProfile> getAllProfiles() {
        return profilesByUUID.values();
    }

    /**
     * Obtém a quantidade de perfis carregados.
     * 
     * @return Número de jogadores online
     */
    public int getProfileCount() {
        return profilesByUUID.size();
    }

    // ==================== ACESSO DIRETO AO REPOSITÓRIO ====================

    /**
     * Obtém o MongoPlayerRepository para operações avançadas.
     * Use com cautela - prefira os métodos do ProfileManager.
     * 
     * @return MongoPlayerRepository
     */
    public MongoPlayerRepository getRepository() {
        return repository;
    }

    /**
     * Carrega dados de um jogador offline diretamente do MongoDB.
     * Útil para comandos administrativos.
     * 
     * @param uuid UUID do jogador
     * @param name Nome do jogador (para criar dados se não existir)
     * @return PlayerData carregado
     */
    public PlayerData loadOfflineData(UUID uuid, String name) {
        // Se estiver online, retornar dados do perfil
        PlayerProfile profile = getProfile(uuid);
        if (profile != null) {
            return profile.getData();
        }

        // Carregar do MongoDB
        return repository.load(uuid, name);
    }

    /**
     * Salva dados de um jogador offline diretamente no MongoDB.
     * Útil para comandos administrativos.
     * 
     * @param data PlayerData a salvar
     * @return true se salvou com sucesso
     */
    public boolean saveOfflineData(PlayerData data) {
        return repository.save(data);
    }

    /**
     * Verifica se existe dados para um jogador no MongoDB.
     * 
     * @param uuid UUID do jogador
     * @return true se existe
     */
    public boolean hasDataFile(UUID uuid) {
        return repository.exists(uuid);
    }

    /**
     * Busca jogador por nome no MongoDB (offline também)
     * 
     * @param playerName Nome do jogador
     * @return PlayerData ou null
     */
    public PlayerData findByName(String playerName) {
        // Primeiro checar se está online
        PlayerProfile profile = getProfile(playerName);
        if (profile != null) {
            return profile.getData();
        }

        // Buscar no MongoDB
        return repository.findByName(playerName);
    }

    /**
     * Obtém top jogadores por kills
     * 
     * @param limit Quantidade
     * @return Lista ordenada por kills
     */
    public java.util.List<PlayerData> getTopKills(int limit) {
        return repository.getTopKills(limit);
    }

    /**
     * Obtém top jogadores por coins
     * 
     * @param limit Quantidade
     * @return Lista ordenada por coins
     */
    public java.util.List<PlayerData> getTopCoins(int limit) {
        return repository.getTopCoins(limit);
    }

    /**
     * Conta total de jogadores registrados
     * 
     * @return Quantidade
     */
    public long getTotalPlayers() {
        return repository.countPlayers();
    }
}
