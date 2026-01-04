package com.haumea.kitpvp.database;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.models.Report;
import com.haumea.kitpvp.models.Report.ReportStatus;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import org.bson.Document;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Repositório MongoDB para Reports (Denúncias)
 * 
 * Substitui o armazenamento em reports.yml por MongoDB.
 * 
 * @author HaumeaMC
 */
public class MongoReportRepository {

    private final HaumeaMC plugin;
    private final MongoManager mongoManager;
    private MongoCollection<Document> collection;

    // Cache em memória para acesso rápido
    private final Map<String, Report> reportsById = new ConcurrentHashMap<>();

    private static final String COLLECTION_NAME = "reports";

    public MongoReportRepository(HaumeaMC plugin, MongoManager mongoManager) {
        this.plugin = plugin;
        this.mongoManager = mongoManager;
        initCollection();
        loadAllReports();
    }

    /**
     * Inicializa a coleção MongoDB
     */
    private void initCollection() {
        if (mongoManager != null && mongoManager.isConnected()) {
            this.collection = mongoManager.getDatabase().getCollection(COLLECTION_NAME);
            plugin.getLogger().info("[MongoDB] Coleção '" + COLLECTION_NAME + "' inicializada.");
        } else {
            plugin.getLogger().warning("[MongoDB] Não foi possível inicializar coleção de reports.");
        }
    }

    /**
     * Carrega todos os reports do MongoDB
     */
    public void loadAllReports() {
        if (collection == null)
            return;

        reportsById.clear();

        try (MongoCursor<Document> cursor = collection.find().iterator()) {
            int count = 0;
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                Report report = documentToReport(doc);
                if (report != null) {
                    reportsById.put(report.getId(), report);
                    count++;
                }
            }
            plugin.getLogger().info("[MongoDB] Carregados " + count + " reports.");
        } catch (Exception e) {
            plugin.getLogger().severe("[MongoDB] Erro ao carregar reports: " + e.getMessage());
        }
    }

    /**
     * Salva um report no MongoDB
     */
    public void saveReport(Report report) {
        if (collection == null)
            return;

        try {
            Document doc = reportToDocument(report);
            collection.replaceOne(
                    Filters.eq("id", report.getId()),
                    doc,
                    new UpdateOptions().upsert(true));
            reportsById.put(report.getId(), report);
        } catch (Exception e) {
            plugin.getLogger().severe("[MongoDB] Erro ao salvar report: " + e.getMessage());
        }
    }

    /**
     * Salva report de forma assíncrona
     */
    public void saveReportAsync(Report report) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> saveReport(report));
    }

    /**
     * Remove um report do banco
     */
    public void deleteReport(String id) {
        if (collection == null)
            return;

        try {
            reportsById.remove(id);
            collection.deleteOne(Filters.eq("id", id));
        } catch (Exception e) {
            plugin.getLogger().severe("[MongoDB] Erro ao deletar report: " + e.getMessage());
        }
    }

    /**
     * Remove report de forma assíncrona
     */
    public void deleteReportAsync(String id) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> deleteReport(id));
    }

    // ==================== CONSULTAS ====================

    /**
     * Obtém report por ID
     */
    public Report getReport(String id) {
        return reportsById.get(id);
    }

    /**
     * Obtém todos os reports abertos
     */
    public List<Report> getOpenReports() {
        return reportsById.values().stream()
                .filter(Report::isOpen)
                .sorted((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()))
                .collect(Collectors.toList());
    }

    /**
     * Obtém todos os reports
     */
    public Collection<Report> getAllReports() {
        return reportsById.values();
    }

    /**
     * Conta reports abertos
     */
    public int countOpenReports() {
        return (int) reportsById.values().stream()
                .filter(Report::isOpen)
                .count();
    }

    /**
     * Limpa reports concluídos
     */
    public int clearCompletedReports() {
        if (collection == null)
            return 0;

        List<String> toRemove = reportsById.values().stream()
                .filter(r -> r.getStatus() == ReportStatus.CONCLUIDO)
                .map(Report::getId)
                .collect(Collectors.toList());

        for (String id : toRemove) {
            reportsById.remove(id);
        }

        try {
            collection.deleteMany(Filters.eq("status", ReportStatus.CONCLUIDO.name()));
        } catch (Exception e) {
            plugin.getLogger().severe("[MongoDB] Erro ao limpar reports concluídos: " + e.getMessage());
        }

        return toRemove.size();
    }

    /**
     * Limpa reports antigos (mais de X dias)
     */
    public int clearOldReports(int days) {
        if (collection == null)
            return 0;

        long threshold = System.currentTimeMillis() - (days * 24L * 60L * 60L * 1000L);

        List<String> toRemove = reportsById.values().stream()
                .filter(r -> r.getTimestamp() < threshold)
                .map(Report::getId)
                .collect(Collectors.toList());

        for (String id : toRemove) {
            reportsById.remove(id);
        }

        try {
            collection.deleteMany(Filters.lt("timestamp", threshold));
        } catch (Exception e) {
            plugin.getLogger().severe("[MongoDB] Erro ao limpar reports antigos: " + e.getMessage());
        }

        return toRemove.size();
    }

    // ==================== CONVERSÃO ====================

    /**
     * Converte Document MongoDB para Report
     */
    private Report documentToReport(Document doc) {
        try {
            String id = doc.getString("id");
            UUID targetUuid = UUID.fromString(doc.getString("targetUuid"));
            String targetName = doc.getString("targetName");
            UUID reporterUuid = UUID.fromString(doc.getString("reporterUuid"));
            String reporterName = doc.getString("reporterName");
            String reason = doc.getString("reason");
            long timestamp = doc.getLong("timestamp") != null ? doc.getLong("timestamp") : 0;
            ReportStatus status = ReportStatus.valueOf(doc.getString("status"));

            return new Report(id, targetUuid, targetName, reporterUuid, reporterName,
                    reason, timestamp, status);
        } catch (Exception e) {
            plugin.getLogger().warning("[MongoDB] Erro ao converter documento para Report: " + e.getMessage());
            return null;
        }
    }

    /**
     * Converte Report para Document MongoDB
     */
    private Document reportToDocument(Report r) {
        Document doc = new Document();
        doc.append("id", r.getId());
        doc.append("targetUuid", r.getTargetUuid().toString());
        doc.append("targetName", r.getTargetName());
        doc.append("reporterUuid", r.getReporterUuid().toString());
        doc.append("reporterName", r.getReporterName());
        doc.append("reason", r.getReason());
        doc.append("timestamp", r.getTimestamp());
        doc.append("status", r.getStatus().name());
        return doc;
    }
}
