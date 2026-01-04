package com.haumea.kitpvp.models;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

/**
 * Modelo de Report (Denúncia) do HaumeaMC
 * 
 * Representa uma denúncia feita por um jogador contra outro.
 * Armazena informações como acusado, autor, motivo, data e status.
 * 
 * @author HaumeaMC
 */
public class Report {

    // Formatador de data padrão
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy 'às' HH:mm");

    // Identificador único do report
    private final String id;

    // Dados do acusado
    private final UUID targetUuid;
    private final String targetName;

    // Dados do autor (quem reportou)
    private final UUID reporterUuid;
    private final String reporterName;

    // Motivo da denúncia
    private final String reason;

    // Data/Hora do report
    private final long timestamp;

    // Status do report
    private ReportStatus status;

    /**
     * Enum para status do report
     */
    public enum ReportStatus {
        ABERTO("Aberto", "&e"),
        CONCLUIDO("Concluído", "&a"),
        CANCELADO("Cancelado", "&c");

        private final String displayName;
        private final String color;

        ReportStatus(String displayName, String color) {
            this.displayName = displayName;
            this.color = color;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getColor() {
            return color;
        }

        public String getFormattedName() {
            return color + displayName;
        }
    }

    /**
     * Construtor para criar um novo report
     */
    public Report(String id, UUID targetUuid, String targetName,
            UUID reporterUuid, String reporterName,
            String reason, long timestamp, ReportStatus status) {
        this.id = id;
        this.targetUuid = targetUuid;
        this.targetName = targetName;
        this.reporterUuid = reporterUuid;
        this.reporterName = reporterName;
        this.reason = reason;
        this.timestamp = timestamp;
        this.status = status;
    }

    /**
     * Construtor simplificado para criar um novo report (status = ABERTO)
     */
    public Report(String id, UUID targetUuid, String targetName,
            UUID reporterUuid, String reporterName, String reason) {
        this(id, targetUuid, targetName, reporterUuid, reporterName,
                reason, System.currentTimeMillis(), ReportStatus.ABERTO);
    }

    // ==================== GETTERS ====================

    public String getId() {
        return id;
    }

    public UUID getTargetUuid() {
        return targetUuid;
    }

    public String getTargetName() {
        return targetName;
    }

    public UUID getReporterUuid() {
        return reporterUuid;
    }

    public String getReporterName() {
        return reporterName;
    }

    public String getReason() {
        return reason;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public ReportStatus getStatus() {
        return status;
    }

    // ==================== SETTERS ====================

    public void setStatus(ReportStatus status) {
        this.status = status;
    }

    /**
     * Marca o report como concluído
     */
    public void markAsComplete() {
        this.status = ReportStatus.CONCLUIDO;
    }

    /**
     * Marca o report como cancelado
     */
    public void cancel() {
        this.status = ReportStatus.CANCELADO;
    }

    // ==================== FORMATAÇÃO ====================

    /**
     * Retorna a data/hora formatada
     */
    public String getFormattedDate() {
        return DATE_FORMAT.format(new Date(timestamp));
    }

    /**
     * Verifica se o report está aberto
     */
    public boolean isOpen() {
        return status == ReportStatus.ABERTO;
    }

    @Override
    public String toString() {
        return "Report{" +
                "id='" + id + '\'' +
                ", target='" + targetName + '\'' +
                ", reporter='" + reporterName + '\'' +
                ", reason='" + reason + '\'' +
                ", status=" + status.getDisplayName() +
                '}';
    }
}
