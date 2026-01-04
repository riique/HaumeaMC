package com.haumea.kitpvp.database;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.models.PlayerData;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import org.bson.Document;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Repositório para operações de PlayerData no MongoDB.
 * Substitui o PlayerDataStorage (YAML) por persistência em MongoDB.
 * 
 * Estrutura do documento:
 * {
 * "uuid": "uuid-string",
 * "lastKnownName": "NomeJogador",
 * "firstJoin": timestamp,
 * "lastJoin": timestamp,
 * "playTime": long,
 * "kills": int,
 * "deaths": int,
 * "killStreak": int,
 * "highestKillStreak": int,
 * "coins": long,
 * "cash": int,
 * "selectedTag": "tagId",
 * "selectedKit": "kitId",
 * "customData": { "key": value }
 * }
 * 
 * @author HaumeaMC
 */
public class MongoPlayerRepository {

    private final HaumeaMC plugin;
    private final MongoManager mongoManager;

    /**
     * Construtor do repositório
     * 
     * @param plugin       Instância do plugin
     * @param mongoManager Manager do MongoDB
     */
    public MongoPlayerRepository(HaumeaMC plugin, MongoManager mongoManager) {
        this.plugin = plugin;
        this.mongoManager = mongoManager;
    }

    /**
     * Obtém a coleção de players
     */
    private MongoCollection<Document> getCollection() {
        return mongoManager.getCollection(MongoManager.COLLECTION_PLAYERS);
    }

    /**
     * Verifica se o MongoDB está disponível
     */
    private boolean isAvailable() {
        return mongoManager != null && mongoManager.isConnected();
    }

    // ==================== OPERAÇÕES SÍNCRONAS ====================

    /**
     * Verifica se existe dados para o jogador
     * 
     * @param uuid UUID do jogador
     * @return true se existe
     */
    public boolean exists(UUID uuid) {
        if (!isAvailable())
            return false;

        try {
            Document doc = getCollection().find(Filters.eq("uuid", uuid.toString())).first();
            return doc != null;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[MongoDB] Erro ao verificar existência: " + uuid, e);
            return false;
        }
    }

    /**
     * Carrega os dados de um jogador do MongoDB
     * 
     * @param uuid       UUID do jogador
     * @param playerName Nome do jogador (para criar se não existir)
     * @return PlayerData carregado ou novo
     */
    public PlayerData load(UUID uuid, String playerName) {
        if (!isAvailable()) {
            plugin.getLogger().warning("[MongoDB] Não conectado, criando dados em memória para: " + playerName);
            return new PlayerData(uuid, playerName);
        }

        try {
            Document doc = getCollection().find(Filters.eq("uuid", uuid.toString())).first();

            if (doc == null) {
                plugin.getLogger().info("[MongoDB] Criando novo registro para: " + playerName);
                return new PlayerData(uuid, playerName);
            }

            return documentToPlayerData(doc, uuid, playerName);

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[MongoDB] Erro ao carregar dados de: " + playerName, e);
            return new PlayerData(uuid, playerName);
        }
    }

    /**
     * Salva os dados de um jogador no MongoDB
     * 
     * @param data PlayerData a ser salvo
     * @return true se salvou com sucesso
     */
    public boolean save(PlayerData data) {
        if (!isAvailable()) {
            plugin.getLogger().warning("[MongoDB] Não conectado, dados não salvos para: " + data.getLastKnownName());
            return false;
        }

        try {
            Document doc = playerDataToDocument(data);

            // Upsert: Atualiza se existir, insere se não
            getCollection().replaceOne(
                    Filters.eq("uuid", data.getUuid().toString()),
                    doc,
                    new ReplaceOptions().upsert(true));

            return true;

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[MongoDB] Erro ao salvar dados de: " + data.getLastKnownName(), e);
            return false;
        }
    }

    /**
     * Deleta os dados de um jogador
     * 
     * @param uuid UUID do jogador
     * @return true se deletou com sucesso
     */
    public boolean delete(UUID uuid) {
        if (!isAvailable())
            return false;

        try {
            getCollection().deleteOne(Filters.eq("uuid", uuid.toString()));
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[MongoDB] Erro ao deletar: " + uuid, e);
            return false;
        }
    }

    // ==================== OPERAÇÕES ASSÍNCRONAS ====================

    /**
     * Carrega dados de forma assíncrona
     * 
     * @param uuid       UUID do jogador
     * @param playerName Nome do jogador
     * @return CompletableFuture com PlayerData
     */
    public CompletableFuture<PlayerData> loadAsync(UUID uuid, String playerName) {
        return CompletableFuture.supplyAsync(() -> load(uuid, playerName));
    }

    /**
     * Salva dados de forma assíncrona
     * 
     * @param data PlayerData a ser salvo
     * @return CompletableFuture com resultado
     */
    public CompletableFuture<Boolean> saveAsync(PlayerData data) {
        return CompletableFuture.supplyAsync(() -> save(data));
    }

    // ==================== CONVERSÕES ====================

    /**
     * Converte um Document do MongoDB para PlayerData
     */
    private PlayerData documentToPlayerData(Document doc, UUID uuid, String playerName) {
        PlayerData data = new PlayerData(uuid, playerName);

        // Informações básicas
        data.setLastKnownName(doc.getString("lastKnownName"));
        data.setFirstJoin(doc.getLong("firstJoin") != null ? doc.getLong("firstJoin") : System.currentTimeMillis());
        data.setLastJoin(doc.getLong("lastJoin") != null ? doc.getLong("lastJoin") : System.currentTimeMillis());
        data.setPlayTime(doc.getLong("playTime") != null ? doc.getLong("playTime") : 0L);

        // Estatísticas de combate
        data.setKills(doc.getInteger("kills", 0));
        data.setDeaths(doc.getInteger("deaths", 0));
        data.setKillStreak(doc.getInteger("killStreak", 0));
        data.setHighestKillStreak(doc.getInteger("highestKillStreak", 0));

        // Economia
        data.setCoins(doc.getLong("coins") != null ? doc.getLong("coins") : 0L);
        data.setCash(doc.getInteger("cash", 0));

        // Preferências
        data.setSelectedTag(doc.getString("selectedTag"));
        data.setSelectedKit(doc.getString("selectedKit") != null ? doc.getString("selectedKit") : "default");

        // Dados customizados
        Document customData = doc.get("customData", Document.class);
        if (customData != null) {
            for (String key : customData.keySet()) {
                data.setCustomData(key, customData.get(key));
            }
        }

        plugin.getLogger().info("[MongoDB] Dados carregados para: " + playerName);
        return data;
    }

    /**
     * Converte PlayerData para um Document do MongoDB
     */
    private Document playerDataToDocument(PlayerData data) {
        Document doc = new Document();

        // Identificação
        doc.append("uuid", data.getUuid().toString());
        doc.append("lastKnownName", data.getLastKnownName());

        // Informações de tempo
        doc.append("firstJoin", data.getFirstJoin());
        doc.append("lastJoin", data.getLastJoin());
        doc.append("playTime", data.getPlayTime());

        // Estatísticas de combate
        doc.append("kills", data.getKills());
        doc.append("deaths", data.getDeaths());
        doc.append("killStreak", data.getKillStreak());
        doc.append("highestKillStreak", data.getHighestKillStreak());

        // Economia
        doc.append("coins", data.getCoins());
        doc.append("cash", data.getCash());

        // Preferências
        doc.append("selectedTag", data.getSelectedTag());
        doc.append("selectedKit", data.getSelectedKit());

        // Dados customizados
        Document customDoc = new Document();
        for (String key : data.getCustomDataKeys()) {
            Object value = data.getCustomData(key);
            if (value != null) {
                // MongoDB não suporta todos os tipos, converter se necessário
                if (value instanceof Map || value instanceof List ||
                        value instanceof String || value instanceof Number ||
                        value instanceof Boolean) {
                    customDoc.append(key, value);
                } else {
                    // Converter para String se tipo não suportado
                    customDoc.append(key, value.toString());
                }
            }
        }
        doc.append("customData", customDoc);

        return doc;
    }

    // ==================== CONSULTAS ESPECIAIS ====================

    /**
     * Busca um jogador pelo nome
     * 
     * @param playerName Nome do jogador
     * @return PlayerData ou null se não encontrado
     */
    public PlayerData findByName(String playerName) {
        if (!isAvailable())
            return null;

        try {
            Document doc = getCollection().find(
                    Filters.regex("lastKnownName", "^" + playerName + "$", "i")).first();

            if (doc != null) {
                String uuidStr = doc.getString("uuid");
                return documentToPlayerData(doc, UUID.fromString(uuidStr), playerName);
            }
            return null;

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[MongoDB] Erro ao buscar por nome: " + playerName, e);
            return null;
        }
    }

    /**
     * Obtém o top jogadores por kills
     * 
     * @param limit Quantidade máxima
     * @return Lista de PlayerData ordenada por kills
     */
    public List<PlayerData> getTopKills(int limit) {
        if (!isAvailable())
            return Collections.emptyList();

        try {
            List<PlayerData> result = new ArrayList<>();

            for (Document doc : getCollection()
                    .find()
                    .sort(new Document("kills", -1))
                    .limit(limit)) {

                String uuidStr = doc.getString("uuid");
                String name = doc.getString("lastKnownName");
                result.add(documentToPlayerData(doc, UUID.fromString(uuidStr), name));
            }

            return result;

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[MongoDB] Erro ao buscar top kills", e);
            return Collections.emptyList();
        }
    }

    /**
     * Obtém o top jogadores por coins
     * 
     * @param limit Quantidade máxima
     * @return Lista de PlayerData ordenada por coins
     */
    public List<PlayerData> getTopCoins(int limit) {
        if (!isAvailable())
            return Collections.emptyList();

        try {
            List<PlayerData> result = new ArrayList<>();

            for (Document doc : getCollection()
                    .find()
                    .sort(new Document("coins", -1))
                    .limit(limit)) {

                String uuidStr = doc.getString("uuid");
                String name = doc.getString("lastKnownName");
                result.add(documentToPlayerData(doc, UUID.fromString(uuidStr), name));
            }

            return result;

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[MongoDB] Erro ao buscar top coins", e);
            return Collections.emptyList();
        }
    }

    /**
     * Conta o total de jogadores registrados
     * 
     * @return Quantidade de jogadores
     */
    public long countPlayers() {
        if (!isAvailable())
            return 0;

        try {
            return getCollection().countDocuments();
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[MongoDB] Erro ao contar jogadores", e);
            return 0;
        }
    }

    /**
     * Salva múltiplos jogadores de uma vez usando BulkWrite.
     * MUITO mais eficiente que salvar um por um.
     * 
     * @param players Lista de PlayerData
     * @return Quantidade salva com sucesso
     */
    public int saveBatch(Collection<PlayerData> players) {
        if (!isAvailable() || players == null || players.isEmpty()) {
            return 0;
        }

        try {
            List<com.mongodb.client.model.WriteModel<Document>> operations = new ArrayList<>();

            for (PlayerData data : players) {
                Document doc = playerDataToDocument(data);

                // Criar operação de ReplaceOne com upsert
                com.mongodb.client.model.ReplaceOneModel<Document> replaceOp = new com.mongodb.client.model.ReplaceOneModel<>(
                        Filters.eq("uuid", data.getUuid().toString()),
                        doc,
                        new ReplaceOptions().upsert(true));

                operations.add(replaceOp);
            }

            // Executar todas as operações em batch
            com.mongodb.bulk.BulkWriteResult result = getCollection().bulkWrite(operations);

            int totalModified = result.getModifiedCount() + result.getUpserts().size();
            plugin.getLogger().info("[MongoDB] Batch save: " + totalModified + " perfis salvos em 1 operação.");

            return totalModified;

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[MongoDB] Erro no batch save, usando fallback", e);
            // Fallback para save individual
            return saveBulkFallback(players);
        }
    }

    /**
     * Fallback para save individual se batch falhar
     */
    private int saveBulkFallback(Collection<PlayerData> players) {
        int saved = 0;
        for (PlayerData data : players) {
            if (save(data)) {
                saved++;
            }
        }
        return saved;
    }

    /**
     * Salva múltiplos jogadores de forma assíncrona usando batch
     * 
     * @param players Lista de PlayerData
     * @return CompletableFuture com quantidade salva
     */
    public CompletableFuture<Integer> saveBatchAsync(Collection<PlayerData> players) {
        return CompletableFuture.supplyAsync(() -> saveBatch(players));
    }

    /**
     * Obtém top jogadores por um campo específico
     * 
     * @param fieldName Nome do campo (ex: "highestKillStreak")
     * @param limit     Quantidade máxima
     * @return Lista de PlayerData ordenada pelo campo
     */
    public List<PlayerData> getTopByField(String fieldName, int limit) {
        if (!isAvailable())
            return Collections.emptyList();

        try {
            List<PlayerData> result = new ArrayList<>();

            for (Document doc : getCollection()
                    .find()
                    .sort(new Document(fieldName, -1))
                    .limit(limit)) {

                String uuidStr = doc.getString("uuid");
                String name = doc.getString("lastKnownName");
                result.add(documentToPlayerData(doc, UUID.fromString(uuidStr), name));
            }

            return result;

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[MongoDB] Erro ao buscar top " + fieldName, e);
            return Collections.emptyList();
        }
    }

    /**
     * Obtém top jogadores por um campo dentro de customData
     * 
     * @param customFieldName Nome do campo em customData (ex: "elo")
     * @param limit           Quantidade máxima
     * @return Lista de PlayerData ordenada pelo campo
     */
    public List<PlayerData> getTopByCustomField(String customFieldName, int limit) {
        if (!isAvailable())
            return Collections.emptyList();

        try {
            List<PlayerData> result = new ArrayList<>();

            for (Document doc : getCollection()
                    .find()
                    .sort(new Document("customData." + customFieldName, -1))
                    .limit(limit)) {

                String uuidStr = doc.getString("uuid");
                String name = doc.getString("lastKnownName");
                result.add(documentToPlayerData(doc, UUID.fromString(uuidStr), name));
            }

            return result;

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[MongoDB] Erro ao buscar top customData." + customFieldName, e);
            return Collections.emptyList();
        }
    }
}
