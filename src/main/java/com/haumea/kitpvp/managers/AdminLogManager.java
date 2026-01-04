package com.haumea.kitpvp.managers;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.database.MongoManager;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import org.bson.Document;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Gerenciador de Logs Administrativos do HaumeaMC.
 * 
 * Registra todas as ações administrativas no MongoDB para
 * auditoria e transparência.
 * 
 * Tipos de ações registradas:
 * - Punições (ban, mute, kick, warn)
 * - Gerenciamento de grupos
 * - Gerenciamento de kits
 * - Teleportes administrativos
 * - Mudanças de configuração
 * - Ações de staff (vanish, admin mode)
 * 
 * @author HaumeaMC
 */
public class AdminLogManager {

    private final HaumeaMC plugin;
    private final MongoManager mongoManager;
    private MongoCollection<Document> collection;

    // Nome da coleção no MongoDB
    private static final String COLLECTION_NAME = "admin_logs";

    // Formato de data para exibição
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

    /**
     * Tipos de ações administrativas
     */
    public enum ActionType {
        // Punições
        BAN("Ban aplicado"),
        UNBAN("Ban removido"),
        MUTE("Mute aplicado"),
        UNMUTE("Mute removido"),
        KICK("Kick aplicado"),
        WARN("Warn aplicado"),

        // Grupos
        GROUP_ADD("Grupo adicionado"),
        GROUP_REMOVE("Grupo removido"),
        GROUP_SET("Grupo definido"),

        // Kits
        KIT_CREATE("Kit criado"),
        KIT_DELETE("Kit deletado"),
        KIT_MODIFY("Kit modificado"),
        KIT_GIVE("Kit dado"),

        // Teleporte
        TELEPORT("Teleporte"),
        TELEPORT_ALL("Teleporte em massa"),

        // Staff
        VANISH_TOGGLE("Vanish alternado"),
        ADMIN_MODE("Modo admin"),
        GOD_MODE("Modo deus"),
        FLY_TOGGLE("Voo alternado"),

        // Economia
        COINS_ADD("Coins adicionados"),
        COINS_REMOVE("Coins removidos"),
        COINS_SET("Coins definidos"),

        // Eventos
        EVENT_CREATE("Evento criado"),
        EVENT_START("Evento iniciado"),
        EVENT_END("Evento finalizado"),

        // Sistema
        CONFIG_CHANGE("Configuração alterada"),
        RELOAD("Plugin recarregado"),

        // Outros
        OTHER("Outra ação");

        private final String description;

        ActionType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    public AdminLogManager(HaumeaMC plugin, MongoManager mongoManager) {
        this.plugin = plugin;
        this.mongoManager = mongoManager;
        initCollection();
        createIndexes();
        plugin.getLogger().info("[AdminLogs] Sistema de logs administrativos inicializado.");
    }

    /**
     * Inicializa a coleção MongoDB
     */
    private void initCollection() {
        if (mongoManager != null && mongoManager.isConnected()) {
            this.collection = mongoManager.getDatabase().getCollection(COLLECTION_NAME);
        }
    }

    /**
     * Cria índices necessários para performance
     */
    private void createIndexes() {
        if (collection == null)
            return;

        try {
            // Índice para busca por staff
            collection.createIndex(new Document("staffUuid", 1));
            // Índice para busca por target
            collection.createIndex(new Document("targetUuid", 1));
            // Índice para busca por tipo de ação
            collection.createIndex(new Document("actionType", 1));
            // Índice para ordenação por timestamp (mais recentes primeiro)
            collection.createIndex(new Document("timestamp", -1));
            // Índice composto para queries comuns
            collection.createIndex(new Document("staffUuid", 1).append("timestamp", -1));
        } catch (Exception e) {
            plugin.getLogger().warning("[AdminLogs] Erro ao criar índices: " + e.getMessage());
        }
    }

    // ==================== LOGGING METHODS ====================

    /**
     * Registra uma ação administrativa.
     * 
     * @param staff      Staff que executou a ação
     * @param actionType Tipo da ação
     * @param target     Jogador alvo (pode ser null)
     * @param details    Detalhes adicionais
     */
    public void log(Player staff, ActionType actionType, Player target, String details) {
        log(staff.getUniqueId(), staff.getName(), actionType,
                target != null ? target.getUniqueId() : null,
                target != null ? target.getName() : null,
                details);
    }

    /**
     * Registra uma ação administrativa por UUID.
     */
    public void log(UUID staffUuid, String staffName, ActionType actionType,
            UUID targetUuid, String targetName, String details) {

        if (collection == null)
            return;

        // Executar assincronamente para não bloquear main thread
        CompletableFuture.runAsync(() -> {
            try {
                Document doc = new Document()
                        .append("id", UUID.randomUUID().toString())
                        .append("timestamp", System.currentTimeMillis())
                        .append("staffUuid", staffUuid.toString())
                        .append("staffName", staffName)
                        .append("actionType", actionType.name())
                        .append("actionDescription", actionType.getDescription())
                        .append("targetUuid", targetUuid != null ? targetUuid.toString() : null)
                        .append("targetName", targetName)
                        .append("details", details)
                        .append("server", "KitPvP"); // Identifica o servidor de origem

                collection.insertOne(doc);
            } catch (Exception e) {
                plugin.getLogger().warning("[AdminLogs] Erro ao salvar log: " + e.getMessage());
            }
        });
    }

    /**
     * Registra uma ação sem target.
     */
    public void log(Player staff, ActionType actionType, String details) {
        log(staff, actionType, null, details);
    }

    /**
     * Registra uma ação do console.
     */
    public void logConsole(ActionType actionType, String targetName, String details) {
        log(new UUID(0, 0), "CONSOLE", actionType, null, targetName, details);
    }

    // ==================== SHORTCUT METHODS ====================

    /**
     * Registra um ban.
     */
    public void logBan(Player staff, String targetName, String reason, String duration) {
        log(staff.getUniqueId(), staff.getName(), ActionType.BAN, null, targetName,
                "Motivo: " + reason + " | Duração: " + duration);
    }

    /**
     * Registra um unban.
     */
    public void logUnban(Player staff, String targetName) {
        log(staff.getUniqueId(), staff.getName(), ActionType.UNBAN, null, targetName, "Desbanido");
    }

    /**
     * Registra um mute.
     */
    public void logMute(Player staff, String targetName, String reason, String duration) {
        log(staff.getUniqueId(), staff.getName(), ActionType.MUTE, null, targetName,
                "Motivo: " + reason + " | Duração: " + duration);
    }

    /**
     * Registra um unmute.
     */
    public void logUnmute(Player staff, String targetName) {
        log(staff.getUniqueId(), staff.getName(), ActionType.UNMUTE, null, targetName, "Desmutado");
    }

    /**
     * Registra um kick.
     */
    public void logKick(Player staff, String targetName, String reason) {
        log(staff.getUniqueId(), staff.getName(), ActionType.KICK, null, targetName,
                "Motivo: " + reason);
    }

    /**
     * Registra um warn.
     */
    public void logWarn(Player staff, String targetName, String reason) {
        log(staff.getUniqueId(), staff.getName(), ActionType.WARN, null, targetName,
                "Motivo: " + reason);
    }

    /**
     * Registra mudança de grupo.
     */
    public void logGroupChange(Player staff, Player target, String action, String groupName) {
        log(staff, ActionType.GROUP_SET, target, action + ": " + groupName);
    }

    /**
     * Registra adição de coins.
     */
    public void logCoinsChange(Player staff, Player target, String action, long amount) {
        ActionType type = action.equals("add") ? ActionType.COINS_ADD
                : action.equals("remove") ? ActionType.COINS_REMOVE : ActionType.COINS_SET;
        log(staff, type, target, "Quantidade: " + amount);
    }

    // ==================== QUERY METHODS ====================

    /**
     * Obtém logs recentes de um staff.
     * 
     * @param staffUuid UUID do staff
     * @param limit     Quantidade máxima
     * @return Lista de logs
     */
    public List<Document> getLogsByStaff(UUID staffUuid, int limit) {
        if (collection == null)
            return Collections.emptyList();

        List<Document> logs = new ArrayList<>();
        try (MongoCursor<Document> cursor = collection.find(
                Filters.eq("staffUuid", staffUuid.toString()))
                .sort(Sorts.descending("timestamp"))
                .limit(limit)
                .iterator()) {

            while (cursor.hasNext()) {
                logs.add(cursor.next());
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[AdminLogs] Erro ao buscar logs: " + e.getMessage());
        }
        return logs;
    }

    /**
     * Obtém logs recentes sobre um target.
     * 
     * @param targetName Nome do target
     * @param limit      Quantidade máxima
     * @return Lista de logs
     */
    public List<Document> getLogsByTarget(String targetName, int limit) {
        if (collection == null)
            return Collections.emptyList();

        List<Document> logs = new ArrayList<>();
        try (MongoCursor<Document> cursor = collection.find(
                Filters.eq("targetName", targetName))
                .sort(Sorts.descending("timestamp"))
                .limit(limit)
                .iterator()) {

            while (cursor.hasNext()) {
                logs.add(cursor.next());
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[AdminLogs] Erro ao buscar logs: " + e.getMessage());
        }
        return logs;
    }

    /**
     * Obtém logs recentes por tipo de ação.
     * 
     * @param actionType Tipo da ação
     * @param limit      Quantidade máxima
     * @return Lista de logs
     */
    public List<Document> getLogsByType(ActionType actionType, int limit) {
        if (collection == null)
            return Collections.emptyList();

        List<Document> logs = new ArrayList<>();
        try (MongoCursor<Document> cursor = collection.find(
                Filters.eq("actionType", actionType.name()))
                .sort(Sorts.descending("timestamp"))
                .limit(limit)
                .iterator()) {

            while (cursor.hasNext()) {
                logs.add(cursor.next());
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[AdminLogs] Erro ao buscar logs: " + e.getMessage());
        }
        return logs;
    }

    /**
     * Obtém logs recentes (todos).
     * 
     * @param limit Quantidade máxima
     * @return Lista de logs
     */
    public List<Document> getRecentLogs(int limit) {
        if (collection == null)
            return Collections.emptyList();

        List<Document> logs = new ArrayList<>();
        try (MongoCursor<Document> cursor = collection.find()
                .sort(Sorts.descending("timestamp"))
                .limit(limit)
                .iterator()) {

            while (cursor.hasNext()) {
                logs.add(cursor.next());
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[AdminLogs] Erro ao buscar logs: " + e.getMessage());
        }
        return logs;
    }

    // ==================== FORMATTING ====================

    /**
     * Formata um log para exibição.
     * 
     * @param doc Documento do log
     * @return String formatada
     */
    public String formatLog(Document doc) {
        long timestamp = doc.getLong("timestamp");
        String date = DATE_FORMAT.format(new Date(timestamp));
        String staffName = doc.getString("staffName");
        String actionDesc = doc.getString("actionDescription");
        String targetName = doc.getString("targetName");
        String details = doc.getString("details");

        StringBuilder sb = new StringBuilder();
        sb.append("§7[§e").append(date).append("§7] ");
        sb.append("§f").append(staffName).append(" §7- ");
        sb.append("§6").append(actionDesc);

        if (targetName != null && !targetName.isEmpty()) {
            sb.append(" §7para §e").append(targetName);
        }

        if (details != null && !details.isEmpty()) {
            sb.append(" §8(§7").append(details).append("§8)");
        }

        return sb.toString();
    }

    /**
     * Conta total de logs.
     * 
     * @return Total de logs
     */
    public long getTotalLogs() {
        if (collection == null)
            return 0;
        try {
            return collection.countDocuments();
        } catch (Exception e) {
            return 0;
        }
    }
}
