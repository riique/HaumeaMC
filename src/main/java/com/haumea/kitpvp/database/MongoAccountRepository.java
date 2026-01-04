package com.haumea.kitpvp.database;

import com.haumea.kitpvp.HaumeaMC;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Repositório para contas do HaumeaMC (HaumeaAccounts) no MongoDB.
 * Substitui o HaumeaAccountStorage (YAML) por persistência em MongoDB.
 * 
 * Suporta MÚLTIPLOS GRUPOS por jogador.
 * 
 * Estrutura do documento:
 * {
 * "uuid": "uuid-string",
 * "name": "NomeJogador",
 * "firstJoin": timestamp,
 * "lastSeen": timestamp,
 * "groups": [
 * { "groupName": "membro", "expiration": 0 },
 * { "groupName": "vip", "expiration": 1703980800000 }
 * ],
 * "passwordHash": "hash" (opcional)
 * }
 * 
 * @author HaumeaMC
 */
public class MongoAccountRepository {

    private final HaumeaMC plugin;
    private final MongoManager mongoManager;

    // Cache em memória para acesso rápido (thread-safe)
    private final Map<UUID, AccountData> accountCache;

    /**
     * Construtor do repositório
     * 
     * @param plugin       Instância do plugin
     * @param mongoManager Manager do MongoDB
     */
    public MongoAccountRepository(HaumeaMC plugin, MongoManager mongoManager) {
        this.plugin = plugin;
        this.mongoManager = mongoManager;
        this.accountCache = new ConcurrentHashMap<>();

        // Carregar todas as contas para o cache
        loadAllToCache();

        // Iniciar task de auto-save
        startAutoSaveTask();
    }

    /**
     * Obtém a coleção de accounts
     */
    private MongoCollection<Document> getCollection() {
        return mongoManager.getCollection(MongoManager.COLLECTION_ACCOUNTS);
    }

    /**
     * Verifica se o MongoDB está disponível
     */
    private boolean isAvailable() {
        return mongoManager != null && mongoManager.isConnected();
    }

    // ==================== GERENCIAMENTO DE CACHE ====================

    /**
     * Carrega todas as contas do MongoDB para o cache
     */
    private void loadAllToCache() {
        if (!isAvailable()) {
            plugin.getLogger().warning("[MongoDB] Não conectado, cache de contas vazio.");
            return;
        }

        try {
            accountCache.clear();
            int count = 0;

            for (Document doc : getCollection().find()) {
                try {
                    UUID uuid = UUID.fromString(doc.getString("uuid"));
                    AccountData data = documentToAccountData(doc);
                    accountCache.put(uuid, data);
                    count++;
                } catch (Exception e) {
                    plugin.getLogger().warning("[MongoDB] Erro ao carregar conta: " + e.getMessage());
                }
            }

            plugin.getLogger().info("[MongoDB] " + count + " contas carregadas para o cache.");

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[MongoDB] Erro ao carregar contas", e);
        }
    }

    /**
     * Inicia task de auto-save a cada 5 minutos
     */
    private void startAutoSaveTask() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            saveAllToDatabase();
        }, 20L * 60L * 5L, 20L * 60L * 5L); // A cada 5 minutos
    }

    /**
     * Salva todas as contas do cache no MongoDB
     */
    public void saveAllToDatabase() {
        if (!isAvailable()) {
            plugin.getLogger().warning("[MongoDB] Não conectado, auto-save ignorado.");
            return;
        }

        int saved = 0;
        for (Map.Entry<UUID, AccountData> entry : accountCache.entrySet()) {
            if (saveAccountSync(entry.getKey(), entry.getValue())) {
                saved++;
            }
        }
        plugin.getLogger().info("[MongoDB] Auto-save: " + saved + " contas salvas.");
    }

    // ==================== OPERAÇÕES DE CONTA ====================

    /**
     * Obtém os dados de conta de um jogador (cria se não existir)
     * 
     * @param uuid UUID do jogador
     * @return AccountData
     */
    public AccountData getAccount(UUID uuid) {
        return accountCache.computeIfAbsent(uuid, k -> {
            // Tentar carregar do banco
            if (isAvailable()) {
                Document doc = getCollection().find(Filters.eq("uuid", uuid.toString())).first();
                if (doc != null) {
                    return documentToAccountData(doc);
                }
            }
            // Criar nova conta
            AccountData data = new AccountData();
            data.addGroup(new GroupEntry("membro", 0));
            data.setFirstJoin(System.currentTimeMillis());
            return data;
        });
    }

    /**
     * Obtém conta por nome do jogador
     * 
     * @param playerName Nome do jogador
     * @return AccountData ou null se não encontrado
     */
    public AccountData getAccountByName(String playerName) {
        for (Map.Entry<UUID, AccountData> entry : accountCache.entrySet()) {
            if (entry.getValue().getName() != null &&
                    entry.getValue().getName().equalsIgnoreCase(playerName)) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * Obtém UUID pelo nome do jogador
     * 
     * @param playerName Nome do jogador
     * @return UUID ou null se não encontrado
     */
    public UUID getUUIDByName(String playerName) {
        for (Map.Entry<UUID, AccountData> entry : accountCache.entrySet()) {
            if (entry.getValue().getName() != null &&
                    entry.getValue().getName().equalsIgnoreCase(playerName)) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Verifica se uma conta existe
     */
    public boolean hasAccount(UUID uuid) {
        return accountCache.containsKey(uuid);
    }

    // ==================== OPERAÇÕES DE GRUPOS ====================

    /**
     * Obtém todos os grupos de um jogador
     */
    public List<GroupEntry> getPlayerGroups(UUID uuid) {
        AccountData data = accountCache.get(uuid);
        if (data != null) {
            return new ArrayList<>(data.getGroups());
        }
        return Collections.singletonList(new GroupEntry("membro", 0));
    }

    /**
     * Verifica se um jogador possui um grupo específico
     */
    public boolean hasGroup(UUID uuid, String groupName) {
        AccountData data = accountCache.get(uuid);
        if (data != null) {
            return data.hasGroup(groupName);
        }
        return groupName.equalsIgnoreCase("membro");
    }

    /**
     * Adiciona um grupo ao jogador
     */
    public void addPlayerGroup(UUID uuid, String playerName, String groupName, long expiration) {
        AccountData data = getAccount(uuid);
        data.setName(playerName);
        data.setLastSeen(System.currentTimeMillis());

        if (!data.hasGroup(groupName)) {
            data.addGroup(new GroupEntry(groupName.toLowerCase(), expiration));
        } else {
            data.updateGroupExpiration(groupName, expiration);
        }

        saveAccountAsync(uuid, data);
    }

    /**
     * Remove um grupo específico de um jogador
     */
    public boolean removePlayerGroup(UUID uuid, String groupName) {
        AccountData data = accountCache.get(uuid);
        if (data != null) {
            boolean removed = data.removeGroup(groupName);
            if (removed) {
                if (data.getGroups().isEmpty()) {
                    data.addGroup(new GroupEntry("membro", 0));
                }
                saveAccountAsync(uuid, data);
                return true;
            }
        }
        return false;
    }

    /**
     * Remove todos os grupos de um jogador
     */
    public void clearPlayerGroups(UUID uuid) {
        AccountData data = accountCache.get(uuid);
        if (data != null) {
            data.clearGroups();
            data.addGroup(new GroupEntry("membro", 0));
            saveAccountAsync(uuid, data);
        }
    }

    // ==================== EVENTOS DE JOGADOR ====================

    /**
     * Chamado quando jogador entra no servidor.
     * IMPORTANTE: Invalida o cache e recarrega do MongoDB para sincronização
     * multi-servidor.
     */
    public void onPlayerJoin(Player player) {
        UUID uuid = player.getUniqueId();

        // CRÍTICO: Remover do cache para forçar reload do MongoDB
        // Isso garante sincronização quando grupos são alterados em outro servidor
        accountCache.remove(uuid);

        // Recarregar dados frescos do MongoDB
        AccountData data = reloadAccountFromDatabase(uuid);

        data.setName(player.getName());

        if (data.getFirstJoin() == 0) {
            data.setFirstJoin(System.currentTimeMillis());
        }
        data.setLastSeen(System.currentTimeMillis());

        // Verificar grupos expirados
        data.removeExpiredGroups();
        if (data.getGroups().isEmpty()) {
            data.addGroup(new GroupEntry("membro", 0));
        }

        plugin.getLogger().info("[MongoDB] " + player.getName() + " carregado (sync multi-servidor).");
    }

    /**
     * Recarrega conta diretamente do MongoDB, atualizando o cache.
     * Usado para garantir sincronização entre servidores.
     * 
     * @param uuid UUID do jogador
     * @return AccountData atualizado
     */
    public AccountData reloadAccountFromDatabase(UUID uuid) {
        if (isAvailable()) {
            try {
                Document doc = getCollection().find(Filters.eq("uuid", uuid.toString())).first();
                if (doc != null) {
                    AccountData data = documentToAccountData(doc);
                    accountCache.put(uuid, data);
                    return data;
                }
            } catch (Exception e) {
                plugin.getLogger().warning("[MongoDB] Erro ao recarregar conta do banco: " + e.getMessage());
            }
        }

        // Se não encontrou ou erro, criar nova conta
        AccountData data = new AccountData();
        data.addGroup(new GroupEntry("membro", 0));
        data.setFirstJoin(System.currentTimeMillis());
        accountCache.put(uuid, data);
        return data;
    }

    /**
     * Chamado quando jogador sai do servidor
     */
    public void onPlayerQuit(Player player) {
        UUID uuid = player.getUniqueId();
        AccountData data = accountCache.get(uuid);

        if (data != null) {
            data.setLastSeen(System.currentTimeMillis());
            saveAccountAsync(uuid, data);
        }
    }

    // ==================== SALVAMENTO ====================

    /**
     * Salva conta de forma assíncrona
     */
    public void saveAccountAsync(UUID uuid, AccountData data) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            saveAccountSync(uuid, data);
        });
    }

    /**
     * Salva conta de forma síncrona
     */
    private synchronized boolean saveAccountSync(UUID uuid, AccountData data) {
        if (!isAvailable())
            return false;

        try {
            Document doc = accountDataToDocument(uuid, data);
            getCollection().replaceOne(
                    Filters.eq("uuid", uuid.toString()),
                    doc,
                    new ReplaceOptions().upsert(true));
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[MongoDB] Erro ao salvar conta: " + uuid, e);
            return false;
        }
    }

    // ==================== CONVERSÕES ====================

    /**
     * Converte Document para AccountData
     */
    private AccountData documentToAccountData(Document doc) {
        AccountData data = new AccountData();

        data.setName(doc.getString("name"));
        data.setFirstJoin(doc.getLong("firstJoin") != null ? doc.getLong("firstJoin") : System.currentTimeMillis());
        data.setLastSeen(doc.getLong("lastSeen") != null ? doc.getLong("lastSeen") : System.currentTimeMillis());

        // Carregar grupos
        List<Document> groupsList = doc.getList("groups", Document.class);
        if (groupsList != null && !groupsList.isEmpty()) {
            for (Document groupDoc : groupsList) {
                String groupName = groupDoc.getString("groupName");
                long expiration = groupDoc.getLong("expiration") != null ? groupDoc.getLong("expiration") : 0;
                if (groupName != null) {
                    data.addGroup(new GroupEntry(groupName.toLowerCase(), expiration));
                }
            }
        }

        // Garantir pelo menos membro
        if (data.getGroups().isEmpty()) {
            data.addGroup(new GroupEntry("membro", 0));
        }

        // Carregar dados de autenticação
        data.setPasswordHash(doc.getString("passwordHash"));
        data.setSalt(doc.getString("salt"));
        data.setRegisteredAt(doc.getLong("registeredAt") != null ? doc.getLong("registeredAt") : 0);
        data.setLastLoginAt(doc.getLong("lastLoginAt") != null ? doc.getLong("lastLoginAt") : 0);
        data.setLastLoginIP(doc.getString("lastLoginIP"));
        data.setPremium(doc.getBoolean("premium", false));

        return data;
    }

    /**
     * Converte AccountData para Document
     */
    private Document accountDataToDocument(UUID uuid, AccountData data) {
        Document doc = new Document();

        doc.append("uuid", uuid.toString());
        doc.append("name", data.getName());
        doc.append("firstJoin", data.getFirstJoin());
        doc.append("lastSeen", data.getLastSeen());

        // Salvar grupos
        List<Document> groupsList = new ArrayList<>();
        for (GroupEntry entry : data.getGroups()) {
            Document groupDoc = new Document();
            groupDoc.append("groupName", entry.getGroupName());
            groupDoc.append("expiration", entry.getExpiration());
            groupsList.add(groupDoc);
        }
        doc.append("groups", groupsList);

        // Salvar dados de autenticação
        if (data.getPasswordHash() != null) {
            doc.append("passwordHash", data.getPasswordHash());
        }
        if (data.getSalt() != null) {
            doc.append("salt", data.getSalt());
        }
        doc.append("registeredAt", data.getRegisteredAt());
        doc.append("lastLoginAt", data.getLastLoginAt());
        if (data.getLastLoginIP() != null) {
            doc.append("lastLoginIP", data.getLastLoginIP());
        }
        doc.append("premium", data.isPremium());

        return doc;
    }

    // ==================== CONSULTAS ====================

    /**
     * Obtém contas por grupo
     */
    public Map<UUID, AccountData> getAccountsByGroup(String groupName) {
        Map<UUID, AccountData> result = new HashMap<>();
        for (Map.Entry<UUID, AccountData> entry : accountCache.entrySet()) {
            if (entry.getValue().hasGroup(groupName)) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    /**
     * Total de contas
     */
    public int getTotalAccounts() {
        return accountCache.size();
    }

    /**
     * Todas as contas
     */
    public Map<UUID, AccountData> getAllAccounts() {
        return new HashMap<>(accountCache);
    }

    /**
     * Obtém todas as contas que compartilham o mesmo IP
     * 
     * @param ip          IP a buscar
     * @param excludeUuid UUID a excluir da lista (normalmente o jogador alvo)
     * @return Lista de AccountData com o mesmo IP
     */
    public List<AccountData> getAccountsByIp(String ip, UUID excludeUuid) {
        List<AccountData> result = new ArrayList<>();

        if (ip == null || ip.isEmpty()) {
            return result;
        }

        for (Map.Entry<UUID, AccountData> entry : accountCache.entrySet()) {
            // Excluir o próprio jogador
            if (excludeUuid != null && entry.getKey().equals(excludeUuid)) {
                continue;
            }

            AccountData data = entry.getValue();
            if (data.getLastLoginIP() != null && data.getLastLoginIP().equals(ip)) {
                result.add(data);
            }
        }

        return result;
    }

    /**
     * Recarrega do banco
     */
    public void reload() {
        loadAllToCache();
        plugin.getLogger().info("[MongoDB] Contas recarregadas.");
    }

    /**
     * Limpa cache e recarrega do banco (alias para reload)
     */
    public void clearCacheAndReload() {
        reload();
    }

    // ==================== CLASSES INTERNAS ====================

    /**
     * Representa uma entrada de grupo
     */
    public static class GroupEntry {
        private final String groupName;
        private long expiration;

        public GroupEntry(String groupName, long expiration) {
            this.groupName = groupName.toLowerCase();
            this.expiration = expiration;
        }

        public String getGroupName() {
            return groupName;
        }

        public long getExpiration() {
            return expiration;
        }

        public void setExpiration(long expiration) {
            this.expiration = expiration;
        }

        public boolean isExpired() {
            if (expiration == 0)
                return false;
            return System.currentTimeMillis() >= expiration;
        }

        public long getTimeRemaining() {
            if (expiration == 0)
                return 0;
            long remaining = expiration - System.currentTimeMillis();
            return remaining > 0 ? remaining : -1;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof GroupEntry) {
                return groupName.equalsIgnoreCase(((GroupEntry) obj).groupName);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return groupName.toLowerCase().hashCode();
        }
    }

    /**
     * Dados de uma conta de jogador
     */
    public static class AccountData {
        private String name;
        private final List<GroupEntry> groups;
        private long firstJoin;
        private long lastSeen;

        // Flags de autenticação (não persistidas - sessão atual)
        private volatile boolean loggedIn = false;
        private volatile long joinTime = 0L;
        private volatile int loginAttempts = 0;

        // Dados de autenticação (PERSISTIDOS no MongoDB)
        private String passwordHash;
        private String salt;
        private long registeredAt;
        private long lastLoginAt;
        private String lastLoginIP;
        private boolean premium = false;

        public AccountData() {
            this.groups = new ArrayList<>();
            this.firstJoin = 0;
            this.lastSeen = 0;
            this.loggedIn = false;
            this.joinTime = 0L;
            this.loginAttempts = 0;
            this.passwordHash = null;
            this.salt = null;
            this.registeredAt = 0;
            this.lastLoginAt = 0;
            this.lastLoginIP = null;
            this.premium = false;
        }

        // Getters e Setters
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<GroupEntry> getGroups() {
            return new ArrayList<>(groups);
        }

        public void addGroup(GroupEntry entry) {
            if (!hasGroup(entry.getGroupName())) {
                groups.add(entry);
            }
        }

        public boolean hasGroup(String groupName) {
            for (GroupEntry entry : groups) {
                if (entry.getGroupName().equalsIgnoreCase(groupName)) {
                    return true;
                }
            }
            return false;
        }

        public boolean removeGroup(String groupName) {
            return groups.removeIf(entry -> entry.getGroupName().equalsIgnoreCase(groupName));
        }

        public void clearGroups() {
            groups.clear();
        }

        public void updateGroupExpiration(String groupName, long newExpiration) {
            for (GroupEntry entry : groups) {
                if (entry.getGroupName().equalsIgnoreCase(groupName)) {
                    entry.setExpiration(newExpiration);
                    break;
                }
            }
        }

        public void removeExpiredGroups() {
            groups.removeIf(GroupEntry::isExpired);
        }

        public long getFirstJoin() {
            return firstJoin;
        }

        public void setFirstJoin(long firstJoin) {
            this.firstJoin = firstJoin;
        }

        public long getLastSeen() {
            return lastSeen;
        }

        public void setLastSeen(long lastSeen) {
            this.lastSeen = lastSeen;
        }

        public List<String> getGroupNames() {
            List<String> names = new ArrayList<>();
            for (GroupEntry entry : groups) {
                names.add(entry.getGroupName());
            }
            return names;
        }

        // Autenticação
        public boolean isLoggedIn() {
            return loggedIn;
        }

        public void setLoggedIn(boolean loggedIn) {
            this.loggedIn = loggedIn;
        }

        public long getJoinTime() {
            return joinTime;
        }

        public void setJoinTime(long joinTime) {
            this.joinTime = joinTime;
        }

        // ==================== AUTENTICAÇÃO (PERSISTIDOS) ====================

        public String getPasswordHash() {
            return passwordHash;
        }

        public void setPasswordHash(String passwordHash) {
            this.passwordHash = passwordHash;
        }

        public String getSalt() {
            return salt;
        }

        public void setSalt(String salt) {
            this.salt = salt;
        }

        public long getRegisteredAt() {
            return registeredAt;
        }

        public void setRegisteredAt(long registeredAt) {
            this.registeredAt = registeredAt;
        }

        public long getLastLoginAt() {
            return lastLoginAt;
        }

        public void setLastLoginAt(long lastLoginAt) {
            this.lastLoginAt = lastLoginAt;
        }

        public String getLastLoginIP() {
            return lastLoginIP;
        }

        public void setLastLoginIP(String lastLoginIP) {
            this.lastLoginIP = lastLoginIP;
        }

        public int getLoginAttempts() {
            return loginAttempts;
        }

        public void setLoginAttempts(int loginAttempts) {
            this.loginAttempts = loginAttempts;
        }

        public void incrementLoginAttempts() {
            this.loginAttempts++;
        }

        public void resetLoginAttempts() {
            this.loginAttempts = 0;
        }

        public boolean isPremium() {
            return premium;
        }

        public void setPremium(boolean premium) {
            this.premium = premium;
        }

        /**
         * Verifica se o jogador está registrado (tem senha definida)
         */
        public boolean isRegistered() {
            return passwordHash != null && !passwordHash.isEmpty();
        }

        /**
         * Atualiza dados de último login
         */
        public void updateLastLogin(String ip) {
            this.lastLoginAt = System.currentTimeMillis();
            this.lastLoginIP = ip;
            this.loginAttempts = 0;
        }
    }
}
