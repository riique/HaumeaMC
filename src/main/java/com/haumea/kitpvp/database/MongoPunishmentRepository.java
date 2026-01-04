package com.haumea.kitpvp.database;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.models.Punishment;
import com.haumea.kitpvp.models.Punishment.PunishmentType;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import org.bson.Document;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Repositório MongoDB para Punições
 * 
 * Substitui o armazenamento em punishments.yml por MongoDB.
 * Gerencia bans, mutes, warnings e kicks.
 * 
 * @author HaumeaMC
 */
public class MongoPunishmentRepository {

    private final HaumeaMC plugin;
    private final MongoManager mongoManager;
    private MongoCollection<Document> collection;

    // Cache em memória para acesso rápido
    private final Map<String, Punishment> punishmentsById = new ConcurrentHashMap<>();
    private final Map<UUID, List<Punishment>> punishmentsByPlayer = new ConcurrentHashMap<>();
    private final Map<String, List<Punishment>> punishmentsByIp = new ConcurrentHashMap<>();

    private static final String COLLECTION_NAME = "punishments";

    public MongoPunishmentRepository(HaumeaMC plugin, MongoManager mongoManager) {
        this.plugin = plugin;
        this.mongoManager = mongoManager;
        initCollection();
        loadAllPunishments();
    }

    /**
     * Inicializa a coleção MongoDB
     */
    private void initCollection() {
        if (mongoManager != null && mongoManager.isConnected()) {
            this.collection = mongoManager.getDatabase().getCollection(COLLECTION_NAME);
            plugin.getLogger().info("[MongoDB] Coleção '" + COLLECTION_NAME + "' inicializada.");
        } else {
            plugin.getLogger().warning("[MongoDB] Não foi possível inicializar coleção de punições.");
        }
    }

    /**
     * Carrega todas as punições do MongoDB
     */
    public void loadAllPunishments() {
        if (collection == null)
            return;

        punishmentsById.clear();
        punishmentsByPlayer.clear();
        punishmentsByIp.clear();

        try (MongoCursor<Document> cursor = collection.find().iterator()) {
            int count = 0;
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                Punishment punishment = documentToPunishment(doc);
                if (punishment != null) {
                    cachePunishment(punishment);
                    count++;
                }
            }
            plugin.getLogger().info("[MongoDB] Carregadas " + count + " punições.");
        } catch (Exception e) {
            plugin.getLogger().severe("[MongoDB] Erro ao carregar punições: " + e.getMessage());
        }
    }

    /**
     * Adiciona ou atualiza punição no cache.
     * Garante que não haja duplicatas na lista.
     */
    private void cachePunishment(Punishment punishment) {
        punishmentsById.put(punishment.getId(), punishment);

        // Cache por jogador
        List<Punishment> playerList = punishmentsByPlayer.computeIfAbsent(punishment.getTargetUuid(),
                k -> new ArrayList<>());
        // Remover versão antiga se existir (para evitar duplicatas em updates)
        playerList.removeIf(p -> p.getId().equals(punishment.getId()));
        playerList.add(punishment);

        // Cache por IP (se existir)
        if (punishment.getTargetIp() != null && !punishment.getTargetIp().isEmpty()) {
            List<Punishment> ipList = punishmentsByIp.computeIfAbsent(punishment.getTargetIp(), k -> new ArrayList<>());
            ipList.removeIf(p -> p.getId().equals(punishment.getId()));
            ipList.add(punishment);
        }
    }

    /**
     * Escrita interna no MongoDB (sem atualizar cache)
     */
    private void writeToMongo(Punishment punishment) {
        if (collection == null)
            return;

        try {
            Document doc = punishmentToDocument(punishment);
            collection.replaceOne(
                    Filters.eq("id", punishment.getId()),
                    doc,
                    new UpdateOptions().upsert(true));
        } catch (Exception e) {
            plugin.getLogger().severe("[MongoDB] Erro ao salvar punição: " + e.getMessage());
        }
    }

    /**
     * Salva uma punição (Síncrono)
     * Atualiza cache e escreve no banco na thread atual.
     */
    public void savePunishment(Punishment punishment) {
        cachePunishment(punishment);
        writeToMongo(punishment);
    }

    /**
     * Salva punição de forma assíncrona
     * ATENÇÃO: Atualiza o cache IMEDIATAMENTE na thread atual para consistência,
     * e agenda a escrita no banco para depois.
     */
    public void savePunishmentAsync(Punishment punishment) {
        // Atualiza memória imediatamente (Read-Your-Writes consistency para a sessão)
        cachePunishment(punishment);

        // Persiste em background
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> writeToMongo(punishment));
    }

    /**
     * Remove uma punição do banco
     */
    public void deletePunishment(String id) {
        if (collection == null)
            return;

        // Atualizar cache imediatamente
        Punishment punishment = punishmentsById.remove(id);
        if (punishment != null) {
            // Remover do cache de jogador
            List<Punishment> playerList = punishmentsByPlayer.get(punishment.getTargetUuid());
            if (playerList != null) {
                playerList.removeIf(p -> p.getId().equals(id));
            }

            // Remover do cache de IP
            if (punishment.getTargetIp() != null) {
                List<Punishment> ipList = punishmentsByIp.get(punishment.getTargetIp());
                if (ipList != null) {
                    ipList.removeIf(p -> p.getId().equals(id));
                }
            }
        }

        // Deletar do banco
        try {
            collection.deleteOne(Filters.eq("id", id));
        } catch (Exception e) {
            plugin.getLogger().severe("[MongoDB] Erro ao deletar punição: " + e.getMessage());
        }
    }

    // ==================== CONSULTAS ====================

    /**
     * Obtém punição por ID
     */
    public Punishment getPunishment(String id) {
        return punishmentsById.get(id);
    }

    /**
     * Obtém todas as punições de um jogador
     */
    public List<Punishment> getPlayerPunishments(UUID uuid) {
        return punishmentsByPlayer.getOrDefault(uuid, new ArrayList<>());
    }

    /**
     * Obtém ban ativo de um jogador
     */
    public Punishment getActiveBan(UUID uuid) {
        List<Punishment> punishments = getPlayerPunishments(uuid);
        for (Punishment p : punishments) {
            if (p.getType() == PunishmentType.BAN && p.isCurrentlyActive()) {
                return p;
            }
        }
        return null;
    }

    /**
     * Obtém ban por IP
     */
    public Punishment getIpBan(String ip) {
        List<Punishment> punishments = punishmentsByIp.getOrDefault(ip, new ArrayList<>());
        for (Punishment p : punishments) {
            if (p.getType() == PunishmentType.BAN && p.isCurrentlyActive()) {
                return p;
            }
        }
        return null;
    }

    /**
     * Obtém mute ativo de um jogador
     */
    public Punishment getActiveMute(UUID uuid) {
        List<Punishment> punishments = getPlayerPunishments(uuid);
        for (Punishment p : punishments) {
            if (p.getType() == PunishmentType.MUTE && p.isCurrentlyActive()) {
                return p;
            }
        }
        return null;
    }

    /**
     * Conta warnings ativos de um jogador
     */
    public int countActiveWarnings(UUID uuid) {
        List<Punishment> punishments = getPlayerPunishments(uuid);
        int count = 0;
        for (Punishment p : punishments) {
            if (p.getType() == PunishmentType.WARN && p.isCurrentlyActive()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Obtém todas as punições
     */
    public Collection<Punishment> getAllPunishments() {
        return punishmentsById.values();
    }

    // ==================== CONVERSÃO ====================

    /**
     * Converte Document MongoDB para Punishment
     */
    private Punishment documentToPunishment(Document doc) {
        try {
            String id = doc.getString("id");
            PunishmentType type = PunishmentType.valueOf(doc.getString("type"));
            UUID targetUuid = UUID.fromString(doc.getString("targetUuid"));
            String targetName = doc.getString("targetName");
            String targetIp = doc.getString("targetIp");

            UUID staffUuid = null;
            if (doc.getString("staffUuid") != null) {
                staffUuid = UUID.fromString(doc.getString("staffUuid"));
            }

            String staffName = doc.getString("staffName");
            String reason = doc.getString("reason");
            String proof = doc.getString("proof");
            long timestamp = doc.getLong("timestamp") != null ? doc.getLong("timestamp") : 0;
            long expiration = doc.getLong("expiration") != null ? doc.getLong("expiration") : 0;
            boolean active = doc.getBoolean("active", true);

            return new Punishment(id, type, targetUuid, targetName, targetIp,
                    staffUuid, staffName, reason, proof, timestamp, expiration, active);
        } catch (Exception e) {
            plugin.getLogger().warning("[MongoDB] Erro ao converter documento para Punishment: " + e.getMessage());
            return null;
        }
    }

    /**
     * Converte Punishment para Document MongoDB
     */
    private Document punishmentToDocument(Punishment p) {
        Document doc = new Document();
        doc.append("id", p.getId());
        doc.append("type", p.getType().name());
        doc.append("targetUuid", p.getTargetUuid().toString());
        doc.append("targetName", p.getTargetName());
        doc.append("targetIp", p.getTargetIp());
        doc.append("staffUuid", p.getStaffUuid() != null ? p.getStaffUuid().toString() : null);
        doc.append("staffName", p.getStaffName());
        doc.append("reason", p.getReason());
        doc.append("proof", p.getProof());
        doc.append("timestamp", p.getTimestamp());
        doc.append("expiration", p.getExpiration());
        doc.append("active", p.isActive());
        return doc;
    }
}
