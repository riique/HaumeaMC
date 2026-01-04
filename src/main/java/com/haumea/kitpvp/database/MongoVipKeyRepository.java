package com.haumea.kitpvp.database;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.models.VipKey;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.UpdateOptions;
import org.bson.Document;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Repositório MongoDB para Chaves VIP
 * 
 * Gerencia persistência de chaves de ativação de VIP.
 * Usa operações atômicas para evitar race conditions na ativação.
 * 
 * @author HaumeaMC
 */
public class MongoVipKeyRepository {

    private final HaumeaMC plugin;
    private final MongoManager mongoManager;
    private MongoCollection<Document> collection;

    // Cache em memória para chaves não usadas (acesso rápido)
    private final Map<String, VipKey> unusedKeysCache = new ConcurrentHashMap<>();

    private static final String COLLECTION_NAME = "vipkeys";

    public MongoVipKeyRepository(HaumeaMC plugin, MongoManager mongoManager) {
        this.plugin = plugin;
        this.mongoManager = mongoManager;
        initCollection();
        loadUnusedKeys();
    }

    /**
     * Inicializa a coleção MongoDB
     */
    private void initCollection() {
        if (mongoManager != null && mongoManager.isConnected()) {
            this.collection = mongoManager.getDatabase().getCollection(COLLECTION_NAME);
            plugin.getLogger().info("[MongoDB] Coleção '" + COLLECTION_NAME + "' inicializada.");
        } else {
            plugin.getLogger().warning("[MongoDB] Não foi possível inicializar coleção de VIP keys.");
        }
    }

    /**
     * Carrega todas as chaves não usadas para o cache
     */
    public void loadUnusedKeys() {
        if (collection == null)
            return;

        unusedKeysCache.clear();

        try (MongoCursor<Document> cursor = collection.find(Filters.eq("usedBy", null)).iterator()) {
            int count = 0;
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                VipKey key = documentToVipKey(doc);
                if (key != null && !key.isKeyExpired()) {
                    unusedKeysCache.put(key.getKey().toUpperCase(), key);
                    count++;
                }
            }
            plugin.getLogger().info("[MongoDB] Carregadas " + count + " chaves VIP não usadas.");
        } catch (Exception e) {
            plugin.getLogger().severe("[MongoDB] Erro ao carregar chaves VIP: " + e.getMessage());
        }
    }

    // ==================== CRUD ====================

    /**
     * Salva uma chave VIP no banco
     */
    public void save(VipKey key) {
        if (collection == null)
            return;

        try {
            Document doc = vipKeyToDocument(key);
            collection.replaceOne(
                    Filters.eq("key", key.getKey().toUpperCase()),
                    doc,
                    new UpdateOptions().upsert(true));

            // Atualiza cache se não usada
            if (!key.isUsed()) {
                unusedKeysCache.put(key.getKey().toUpperCase(), key);
            } else {
                unusedKeysCache.remove(key.getKey().toUpperCase());
            }
        } catch (Exception e) {
            plugin.getLogger().severe("[MongoDB] Erro ao salvar chave VIP: " + e.getMessage());
        }
    }

    /**
     * Salva chave de forma assíncrona
     */
    public void saveAsync(VipKey key) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> save(key));
    }

    /**
     * Busca uma chave pelo código
     */
    public VipKey findByCode(String code) {
        if (collection == null)
            return null;

        String upperCode = code.toUpperCase();

        // Tenta do cache primeiro
        VipKey cached = unusedKeysCache.get(upperCode);
        if (cached != null) {
            return cached;
        }

        // Busca no banco (pode ser uma chave já usada)
        try {
            Document doc = collection.find(Filters.eq("key", upperCode)).first();
            if (doc != null) {
                return documentToVipKey(doc);
            }
        } catch (Exception e) {
            plugin.getLogger().severe("[MongoDB] Erro ao buscar chave VIP: " + e.getMessage());
        }

        return null;
    }

    /**
     * Ativa uma chave de forma ATÔMICA (thread-safe)
     * 
     * Usa findOneAndUpdate para garantir que apenas um jogador possa
     * usar a chave, mesmo com múltiplas tentativas simultâneas.
     * 
     * @param code       Código da chave
     * @param playerUUID UUID do jogador
     * @return A chave se ativada com sucesso, null se falhou
     */
    public VipKey redeemKey(String code, UUID playerUUID) {
        if (collection == null)
            return null;

        String upperCode = code.toUpperCase();
        long now = System.currentTimeMillis();

        try {
            // Operação atômica: só atualiza se usedBy for null
            Document result = collection.findOneAndUpdate(
                    Filters.and(
                            Filters.eq("key", upperCode),
                            Filters.eq("usedBy", null)),
                    new Document("$set", new Document()
                            .append("usedBy", playerUUID.toString())
                            .append("usedAt", now)),
                    new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER));

            if (result != null) {
                VipKey key = documentToVipKey(result);
                // Remove do cache de não usadas
                unusedKeysCache.remove(upperCode);
                return key;
            }
        } catch (Exception e) {
            plugin.getLogger().severe("[MongoDB] Erro ao resgatar chave VIP: " + e.getMessage());
        }

        return null;
    }

    /**
     * Deleta uma chave pelo código
     */
    public boolean delete(String code) {
        if (collection == null)
            return false;

        String upperCode = code.toUpperCase();

        try {
            long deleted = collection.deleteOne(Filters.eq("key", upperCode)).getDeletedCount();
            if (deleted > 0) {
                unusedKeysCache.remove(upperCode);
                return true;
            }
        } catch (Exception e) {
            plugin.getLogger().severe("[MongoDB] Erro ao deletar chave VIP: " + e.getMessage());
        }

        return false;
    }

    /**
     * Deleta chave de forma assíncrona
     */
    public void deleteAsync(String code) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> delete(code));
    }

    // ==================== CONSULTAS ====================

    /**
     * Lista todas as chaves não usadas
     */
    public Collection<VipKey> getUnusedKeys() {
        return unusedKeysCache.values();
    }

    /**
     * Lista chaves não usadas por grupo
     */
    public List<VipKey> getUnusedKeysByGroup(String groupName) {
        List<VipKey> result = new ArrayList<>();
        for (VipKey key : unusedKeysCache.values()) {
            if (key.getGroupName().equalsIgnoreCase(groupName)) {
                result.add(key);
            }
        }
        return result;
    }

    /**
     * Conta chaves não usadas
     */
    public int countUnused() {
        return unusedKeysCache.size();
    }

    /**
     * Conta chaves não usadas por grupo
     */
    public int countUnusedByGroup(String groupName) {
        int count = 0;
        for (VipKey key : unusedKeysCache.values()) {
            if (key.getGroupName().equalsIgnoreCase(groupName)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Verifica se uma chave existe (usada ou não)
     */
    public boolean exists(String code) {
        return findByCode(code) != null;
    }

    /**
     * Remove chaves expiradas do banco e cache
     */
    public int cleanupExpiredKeys() {
        if (collection == null)
            return 0;

        int removed = 0;
        long now = System.currentTimeMillis();

        try {
            // Remove do banco
            long deleted = collection.deleteMany(
                    Filters.and(
                            Filters.gt("keyExpiresAt", 0),
                            Filters.lt("keyExpiresAt", now)))
                    .getDeletedCount();
            removed = (int) deleted;

            // Atualiza cache
            Iterator<Map.Entry<String, VipKey>> iterator = unusedKeysCache.entrySet().iterator();
            while (iterator.hasNext()) {
                VipKey key = iterator.next().getValue();
                if (key.isKeyExpired()) {
                    iterator.remove();
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("[MongoDB] Erro ao limpar chaves expiradas: " + e.getMessage());
        }

        return removed;
    }

    // ==================== CONVERSÃO ====================

    /**
     * Converte Document MongoDB para VipKey
     */
    private VipKey documentToVipKey(Document doc) {
        try {
            String key = doc.getString("key");
            String groupName = doc.getString("groupName");
            long duration = doc.getLong("duration") != null ? doc.getLong("duration") : 0;
            long createdAt = doc.getLong("createdAt") != null ? doc.getLong("createdAt") : 0;
            String createdBy = doc.getString("createdBy");

            String usedByStr = doc.getString("usedBy");
            UUID usedBy = usedByStr != null ? UUID.fromString(usedByStr) : null;

            long usedAt = doc.getLong("usedAt") != null ? doc.getLong("usedAt") : 0;
            long keyExpiresAt = doc.getLong("keyExpiresAt") != null ? doc.getLong("keyExpiresAt") : 0;

            return new VipKey(key, groupName, duration, createdAt, createdBy, usedBy, usedAt, keyExpiresAt);
        } catch (Exception e) {
            plugin.getLogger().warning("[MongoDB] Erro ao converter documento para VipKey: " + e.getMessage());
            return null;
        }
    }

    /**
     * Converte VipKey para Document MongoDB
     */
    private Document vipKeyToDocument(VipKey key) {
        Document doc = new Document();
        doc.append("key", key.getKey().toUpperCase());
        doc.append("groupName", key.getGroupName());
        doc.append("duration", key.getDuration());
        doc.append("createdAt", key.getCreatedAt());
        doc.append("createdBy", key.getCreatedBy());
        doc.append("usedBy", key.getUsedBy() != null ? key.getUsedBy().toString() : null);
        doc.append("usedAt", key.getUsedAt());
        doc.append("keyExpiresAt", key.getKeyExpiresAt());
        return doc;
    }
}
