package com.haumea.kitpvp.database;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.models.Warp;
import com.haumea.kitpvp.server.ServerType;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Repositório MongoDB para Warps
 * 
 * MULTI-SERVER: Filtra warps por serverType.
 * Cada servidor (LOBBY, KITPVP) tem suas próprias warps.
 * 
 * @author HaumeaMC
 */
public class MongoWarpRepository {

    private final HaumeaMC plugin;
    private final MongoManager mongoManager;
    private MongoCollection<Document> collection;

    // Cache em memória para acesso rápido (apenas warps deste servidor)
    private final Map<String, Warp> warpsByName = new ConcurrentHashMap<>();

    private static final String COLLECTION_NAME = "warps";

    // Tipo de servidor atual (para filtrar warps)
    private final String serverType;

    public MongoWarpRepository(HaumeaMC plugin, MongoManager mongoManager) {
        this.plugin = plugin;
        this.mongoManager = mongoManager;

        // Obter tipo de servidor atual
        ServerType type = plugin.getServerType();
        this.serverType = (type != null) ? type.name() : "KITPVP";

        initCollection();
        loadAllWarps();
    }

    /**
     * Inicializa a coleção MongoDB
     */
    private void initCollection() {
        if (mongoManager != null && mongoManager.isConnected()) {
            this.collection = mongoManager.getDatabase().getCollection(COLLECTION_NAME);
            plugin.getLogger()
                    .info("[MongoDB] Coleção '" + COLLECTION_NAME + "' inicializada (servidor: " + serverType + ").");
        } else {
            plugin.getLogger().warning("[MongoDB] Não foi possível inicializar coleção de warps.");
        }
    }

    /**
     * Cria o filtro para buscar warps deste servidor
     */
    private Bson serverFilter() {
        return Filters.eq("server", serverType);
    }

    /**
     * Cria filtro combinado: servidor + nome
     */
    private Bson serverAndNameFilter(String name) {
        return Filters.and(
                Filters.eq("server", serverType),
                Filters.eq("name", name.toLowerCase()));
    }

    /**
     * Carrega todas as warps do MongoDB (apenas deste servidor)
     */
    public void loadAllWarps() {
        if (collection == null)
            return;

        warpsByName.clear();

        try (MongoCursor<Document> cursor = collection.find(serverFilter()).iterator()) {
            int count = 0;
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                Warp warp = documentToWarp(doc);
                if (warp != null) {
                    warpsByName.put(warp.getName().toLowerCase(), warp);
                    count++;
                }
            }
            plugin.getLogger().info("[MongoDB] Carregadas " + count + " warps para servidor " + serverType + ".");
        } catch (Exception e) {
            plugin.getLogger().severe("[MongoDB] Erro ao carregar warps: " + e.getMessage());
        }
    }

    /**
     * Salva uma warp no MongoDB (associada a este servidor)
     */
    public void saveWarp(Warp warp) {
        if (collection == null)
            return;

        try {
            Document doc = warpToDocument(warp);
            collection.replaceOne(
                    serverAndNameFilter(warp.getName()),
                    doc,
                    new UpdateOptions().upsert(true));
            warpsByName.put(warp.getName().toLowerCase(), warp);
        } catch (Exception e) {
            plugin.getLogger().severe("[MongoDB] Erro ao salvar warp: " + e.getMessage());
        }
    }

    /**
     * Salva warp de forma assíncrona
     */
    public void saveWarpAsync(Warp warp) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> saveWarp(warp));
    }

    /**
     * Remove uma warp do banco (apenas deste servidor)
     */
    public boolean deleteWarp(String name) {
        if (collection == null)
            return false;

        String key = name.toLowerCase();
        Warp removed = warpsByName.remove(key);

        if (removed != null) {
            try {
                collection.deleteOne(serverAndNameFilter(key));
                return true;
            } catch (Exception e) {
                plugin.getLogger().severe("[MongoDB] Erro ao deletar warp: " + e.getMessage());
            }
        }
        return false;
    }

    /**
     * Remove warp de forma assíncrona
     */
    public void deleteWarpAsync(String name) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> deleteWarp(name));
    }

    // ==================== CONSULTAS ====================

    /**
     * Obtém warp por nome (deste servidor)
     */
    public Warp getWarp(String name) {
        return warpsByName.get(name.toLowerCase());
    }

    /**
     * Verifica se uma warp existe (neste servidor)
     */
    public boolean warpExists(String name) {
        return warpsByName.containsKey(name.toLowerCase());
    }

    /**
     * Obtém todas as warps (deste servidor)
     */
    public Collection<Warp> getAllWarps() {
        return warpsByName.values();
    }

    /**
     * Obtém todos os nomes de warps (deste servidor)
     */
    public Set<String> getWarpNames() {
        return warpsByName.keySet();
    }

    /**
     * Conta o número de warps (deste servidor)
     */
    public int getWarpCount() {
        return warpsByName.size();
    }

    // ==================== CONVERSÃO ====================

    /**
     * Converte Document MongoDB para Warp
     */
    private Warp documentToWarp(Document doc) {
        try {
            String name = doc.getString("name");
            String worldName = doc.getString("worldName");
            double x = doc.getDouble("x") != null ? doc.getDouble("x") : 0;
            double y = doc.getDouble("y") != null ? doc.getDouble("y") : 64;
            double z = doc.getDouble("z") != null ? doc.getDouble("z") : 0;
            float yaw = doc.getDouble("yaw") != null ? doc.getDouble("yaw").floatValue() : 0;
            float pitch = doc.getDouble("pitch") != null ? doc.getDouble("pitch").floatValue() : 0;
            double radius = doc.getDouble("radius") != null ? doc.getDouble("radius") : 0;

            return new Warp(name, worldName, x, y, z, yaw, pitch, radius);
        } catch (Exception e) {
            plugin.getLogger().warning("[MongoDB] Erro ao converter documento para Warp: " + e.getMessage());
            return null;
        }
    }

    /**
     * Converte Warp para Document MongoDB (inclui campo server)
     */
    private Document warpToDocument(Warp warp) {
        Document doc = new Document();
        doc.append("name", warp.getName().toLowerCase());
        doc.append("server", serverType); // Campo para identificar o servidor
        doc.append("worldName", warp.getWorldName());
        doc.append("x", warp.getX());
        doc.append("y", warp.getY());
        doc.append("z", warp.getZ());
        doc.append("yaw", (double) warp.getYaw());
        doc.append("pitch", (double) warp.getPitch());
        doc.append("radius", warp.getRadius());
        return doc;
    }
}
