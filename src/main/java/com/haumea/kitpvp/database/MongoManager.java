package com.haumea.kitpvp.database;

import com.haumea.kitpvp.HaumeaMC;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manager central para conexão com MongoDB.
 * 
 * Gerencia a conexão única com o banco de dados,
 * fornecendo acesso às coleções para outros managers.
 * 
 * Configuração via config.yml:
 * mongodb:
 * enabled: true
 * host: localhost
 * port: 27017
 * database: haumeamc
 * username: ""
 * password: ""
 * auth-database: admin
 * 
 * @author HaumeaMC
 */
public class MongoManager {

    private final HaumeaMC plugin;
    private MongoClient mongoClient;
    private MongoDatabase database;
    private boolean connected = false;

    // Nomes das coleções
    public static final String COLLECTION_PLAYERS = "players";
    public static final String COLLECTION_ACCOUNTS = "accounts";
    public static final String COLLECTION_PUNISHMENTS = "punishments";
    public static final String COLLECTION_REPORTS = "reports";
    public static final String COLLECTION_WARPS = "warps";
    public static final String COLLECTION_KITS = "kits";

    /**
     * Construtor do MongoManager
     * 
     * @param plugin Instância do plugin principal
     */
    public MongoManager(HaumeaMC plugin) {
        this.plugin = plugin;

        // Silenciar logs verbosos do MongoDB driver
        Logger mongoLogger = Logger.getLogger("org.mongodb.driver");
        mongoLogger.setLevel(Level.WARNING);
    }

    /**
     * Conecta ao MongoDB usando as configurações do config.yml
     * 
     * @return true se conectou com sucesso
     */
    public boolean connect() {
        FileConfiguration config = plugin.getConfig();

        // Verificar se MongoDB está habilitado
        if (!config.getBoolean("mongodb.enabled", true)) {
            plugin.getLogger().warning("[MongoDB] MongoDB está desabilitado no config.yml!");
            return false;
        }

        String host = config.getString("mongodb.host", "localhost");
        int port = config.getInt("mongodb.port", 27017);
        String databaseName = config.getString("mongodb.database", "haumeamc");
        String username = config.getString("mongodb.username", "");
        String password = config.getString("mongodb.password", "");
        String authDatabase = config.getString("mongodb.auth-database", "admin");

        try {
            plugin.getLogger().info("[MongoDB] Conectando ao MongoDB em " + host + ":" + port + "...");

            // Configurações de conexão otimizadas com connection pooling
            MongoClientOptions.Builder optionsBuilder = MongoClientOptions.builder()
                    .connectTimeout(5000) // 5 segundos para conectar
                    .socketTimeout(10000) // 10 segundos para operações
                    .serverSelectionTimeout(5000) // 5 segundos para selecionar servidor
                    .maxWaitTime(5000) // 5 segundos máximo de espera
                    // === OTIMIZAÇÕES DE CONNECTION POOLING ===
                    .connectionsPerHost(10) // Pool size
                    .minConnectionsPerHost(2) // Mínimo de conexões ativas
                    .threadsAllowedToBlockForConnectionMultiplier(5); // Multiplicador de threads bloqueantes

            ServerAddress serverAddress = new ServerAddress(host, port);

            // Verificar se precisa de autenticação
            if (username != null && !username.isEmpty() && password != null && !password.isEmpty()) {
                MongoCredential credential = MongoCredential.createCredential(
                        username,
                        authDatabase,
                        password.toCharArray());
                mongoClient = new MongoClient(
                        serverAddress,
                        credential,
                        optionsBuilder.build());
                plugin.getLogger().info("[MongoDB] Usando autenticação com usuário: " + username);
            } else {
                mongoClient = new MongoClient(serverAddress, optionsBuilder.build());
                plugin.getLogger().info("[MongoDB] Conectando sem autenticação (desenvolvimento local)");
            }

            // Obter referência ao banco de dados
            database = mongoClient.getDatabase(databaseName);

            // Testar conexão
            database.runCommand(new Document("ping", 1));

            connected = true;
            plugin.getLogger().info("[MongoDB] ✅ Conectado com sucesso ao banco: " + databaseName);

            // Criar índices necessários
            createIndexes();

            return true;

        } catch (Exception e) {
            plugin.getLogger().severe("[MongoDB] ❌ Falha ao conectar: " + e.getMessage());
            plugin.getLogger()
                    .severe("[MongoDB] Verifique se o MongoDB está rodando e as configurações estão corretas.");
            connected = false;
            return false;
        }
    }

    /**
     * Cria índices necessários para performance
     */
    private void createIndexes() {
        try {
            // Índice único para UUID dos jogadores
            getCollection(COLLECTION_PLAYERS).createIndex(new Document("uuid", 1));
            getCollection(COLLECTION_PLAYERS).createIndex(new Document("lastKnownName", 1));

            // Índices para ranking (consultas de top jogadores)
            getCollection(COLLECTION_PLAYERS).createIndex(new Document("kills", -1));
            getCollection(COLLECTION_PLAYERS).createIndex(new Document("coins", -1));
            getCollection(COLLECTION_PLAYERS).createIndex(new Document("highestKillStreak", -1));

            // Índices para customData (rankings de ELO e duelos)
            getCollection(COLLECTION_PLAYERS).createIndex(new Document("customData.elo", -1));
            getCollection(COLLECTION_PLAYERS).createIndex(new Document("customData.duel_wins", -1));
            // Índice para daily rewards
            getCollection(COLLECTION_PLAYERS).createIndex(new Document("customData.daily_last_claim", -1));

            // Índice para contas
            getCollection(COLLECTION_ACCOUNTS).createIndex(new Document("uuid", 1));
            getCollection(COLLECTION_ACCOUNTS).createIndex(new Document("name", 1));
            // Índice para IP bans
            getCollection(COLLECTION_ACCOUNTS).createIndex(new Document("lastIp", 1));

            // Índice para punições
            getCollection(COLLECTION_PUNISHMENTS).createIndex(new Document("targetUuid", 1));
            getCollection(COLLECTION_PUNISHMENTS).createIndex(new Document("active", 1));
            // Índice composto para busca de punições ativas por UUID
            getCollection(COLLECTION_PUNISHMENTS).createIndex(new Document("targetUuid", 1).append("active", 1));
            // Índice para IP bans na coleção de punições
            getCollection(COLLECTION_PUNISHMENTS).createIndex(new Document("targetIp", 1));

            // Índice para reports
            getCollection(COLLECTION_REPORTS).createIndex(new Document("reportedUuid", 1));
            getCollection(COLLECTION_REPORTS).createIndex(new Document("resolved", 1));
            // Índice composto para busca de reports pendentes
            getCollection(COLLECTION_REPORTS).createIndex(new Document("resolved", 1).append("timestamp", -1));

            // Índice para warps
            getCollection(COLLECTION_WARPS).createIndex(new Document("name", 1));

            // Índices para admin_logs
            getCollection("admin_logs").createIndex(new Document("staffUuid", 1));
            getCollection("admin_logs").createIndex(new Document("targetUuid", 1));
            getCollection("admin_logs").createIndex(new Document("actionType", 1));
            getCollection("admin_logs").createIndex(new Document("timestamp", -1));
            getCollection("admin_logs").createIndex(new Document("staffUuid", 1).append("timestamp", -1));

            plugin.getLogger().info("[MongoDB] Índices criados/verificados com sucesso.");

        } catch (Exception e) {
            plugin.getLogger().warning("[MongoDB] Erro ao criar índices (não crítico): " + e.getMessage());
        }
    }

    /**
     * Desconecta do MongoDB
     */
    public void disconnect() {
        if (mongoClient != null) {
            try {
                mongoClient.close();
                plugin.getLogger().info("[MongoDB] Desconectado do MongoDB.");
            } catch (Exception e) {
                plugin.getLogger().warning("[MongoDB] Erro ao desconectar: " + e.getMessage());
            }
        }
        connected = false;
    }

    /**
     * Obtém uma coleção do banco de dados
     * 
     * @param collectionName Nome da coleção
     * @return MongoCollection ou null se não conectado
     */
    public MongoCollection<Document> getCollection(String collectionName) {
        if (!connected || database == null) {
            plugin.getLogger().severe("[MongoDB] Tentativa de acessar coleção sem conexão ativa!");
            return null;
        }
        return database.getCollection(collectionName);
    }

    /**
     * Verifica se está conectado ao MongoDB
     * 
     * @return true se conectado
     */
    public boolean isConnected() {
        return connected;
    }

    /**
     * Obtém a instância do banco de dados
     * 
     * @return MongoDatabase ou null se não conectado
     */
    public MongoDatabase getDatabase() {
        return database;
    }

    /**
     * Obtém instância do MongoClient
     * 
     * @return MongoClient ou null se não conectado
     */
    public MongoClient getMongoClient() {
        return mongoClient;
    }

    /**
     * Verifica a saúde da conexão com ping
     * 
     * @return true se o banco responde
     */
    public boolean ping() {
        if (!connected || database == null) {
            return false;
        }
        try {
            database.runCommand(new Document("ping", 1));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Obtém estatísticas do banco de dados
     * 
     * @return Document com stats ou null se erro
     */
    public Document getStats() {
        if (!connected || database == null) {
            return null;
        }
        try {
            return database.runCommand(new Document("dbStats", 1));
        } catch (Exception e) {
            return null;
        }
    }
}
