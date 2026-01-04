package com.haumea.kitpvp.managers;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.database.MongoReportRepository;
import com.haumea.kitpvp.models.Report;
import com.haumea.kitpvp.utils.ChatStorage;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Gerenciador de Reports (Denúncias) do HaumeaMC
 * 
 * Responsável por:
 * - Criar e gerenciar reports
 * - Persistência em MongoDB
 * - Controle de cooldown
 * - Notificação de staff
 * 
 * @author HaumeaMC
 */
public class ReportManager {

    private final HaumeaMC plugin;
    private MongoReportRepository repository;

    // Grupos que podem ver reports
    private static final Set<String> STAFF_GROUPS = new HashSet<>(Arrays.asList(
            "dono", "diretor", "gerente", "admin", "moderador", "mod", "helper"));

    public ReportManager(HaumeaMC plugin) {
        this.plugin = plugin;

        // Inicializar repositório MongoDB
        initRepository();
    }

    /**
     * Inicializa o repositório MongoDB
     */
    private void initRepository() {
        if (plugin.getMongoManager() != null && plugin.getMongoManager().isConnected()) {
            this.repository = new MongoReportRepository(plugin, plugin.getMongoManager());
            plugin.getLogger().info("[Reports] MongoDB repository inicializado.");
        } else {
            plugin.getLogger().warning("[Reports] MongoDB não disponível! Reports não serão persistidos.");
        }
    }

    // ==================== CRIAÇÃO DE REPORTS ====================

    /**
     * Cria um novo report
     * 
     * @return O report criado ou null se em cooldown
     */
    public Report createReport(Player reporter, Player target, String reason) {
        // Usar CooldownManager central
        CooldownManager cooldownManager = plugin.getCooldownManager();
        if (cooldownManager != null && cooldownManager.isOnCooldown(reporter, CooldownManager.REPORT)) {
            return null;
        }

        String id = generateId();
        Report report = new Report(id, target.getUniqueId(), target.getName(),
                reporter.getUniqueId(), reporter.getName(), reason);

        // Salvar no MongoDB
        if (repository != null) {
            repository.saveReportAsync(report);
        }

        // Aplicar cooldown via CooldownManager
        if (cooldownManager != null) {
            cooldownManager.setCooldown(reporter, CooldownManager.REPORT);
        }

        // Notificar staff
        notifyStaff(report);

        return report;
    }

    /**
     * Notifica toda a staff online sobre um novo report
     */
    private void notifyStaff(Report report) {
        // Obter mensagens do YAML
        String header = ChatStorage.getMessage("report.staff-notification.header");
        String title = ChatStorage.getMessage("report.staff-notification.title");
        String targetLabel = ChatStorage.getMessage("report.staff-notification.target-label");
        String targetLine = ChatStorage.getMessage("report.staff-notification.target", "target",
                report.getTargetName());
        String reasonLabel = ChatStorage.getMessage("report.staff-notification.reason-label");
        String reasonLine = ChatStorage.getMessage("report.staff-notification.reason", "reason", report.getReason());
        String reporterLabel = ChatStorage.getMessage("report.staff-notification.reporter-label");
        String reporterLine = ChatStorage.getMessage("report.staff-notification.reporter", "reporter",
                report.getReporterName());
        String timeLabel = ChatStorage.getMessage("report.staff-notification.time-label");
        String timeLine = ChatStorage.getMessage("report.staff-notification.time", "time", report.getFormattedDate());
        String actionHint = ChatStorage.getMessage("report.staff-notification.action-hint");
        String footer = ChatStorage.getMessage("report.staff-notification.footer");

        // Grupos permitidos para usar /admin (mesmos do AdminCommand)
        java.util.Set<String> adminGroups = new java.util.HashSet<>(java.util.Arrays.asList(
                "dono", "diretor", "gerente", "admin", "moderador", "ajudante"));

        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (isStaff(staff)) {
                // Verificar preferência de notificação de reports
                com.haumea.kitpvp.profile.PlayerProfile staffProfile = plugin.getProfileManager().getProfile(staff);
                if (staffProfile != null) {
                    boolean notifyNeeded = staffProfile.getData().getCustomData("pref_notify_reports", true);
                    if (!notifyNeeded)
                        continue;
                }

                // Enviar mensagem formatada
                staff.sendMessage("");
                staff.sendMessage(header);
                staff.sendMessage("");
                staff.sendMessage(title);
                staff.sendMessage("");
                staff.sendMessage(targetLabel);
                staff.sendMessage(targetLine);
                staff.sendMessage("");
                staff.sendMessage(reasonLabel);
                staff.sendMessage(reasonLine);
                staff.sendMessage("");
                staff.sendMessage(reporterLabel);
                staff.sendMessage(reporterLine);
                staff.sendMessage("");
                staff.sendMessage(timeLabel);
                staff.sendMessage(timeLine);
                staff.sendMessage("");

                // Verificar se o staffer tem permissão de /admin para mostrar botão [OBSERVAR]
                boolean hasAdminPermission = false;
                if (plugin.getGroupManager() != null) {
                    com.haumea.kitpvp.models.Group staffGroup = plugin.getGroupManager().getPlayerGroup(staff);
                    if (staffGroup != null) {
                        hasAdminPermission = adminGroups.contains(staffGroup.getName().toLowerCase());
                    }
                }

                if (hasAdminPermission) {
                    // Criar botão clicável [OBSERVAR]
                    TextComponent observeButton = new TextComponent("  §a§l[OBSERVAR]");
                    observeButton.setClickEvent(new ClickEvent(
                            ClickEvent.Action.RUN_COMMAND,
                            "/gotowatch " + report.getTargetName()));
                    observeButton.setHoverEvent(new HoverEvent(
                            HoverEvent.Action.SHOW_TEXT,
                            new ComponentBuilder("§eClique para ir até §f" + report.getTargetName()
                                    + "\n§7Você será teleportado e o modo\n§7admin será ativado automaticamente.")
                                    .create()));

                    // Criar botão [VER REPORTS]
                    TextComponent reportsButton = new TextComponent("  §b§l[VER REPORTS]");
                    reportsButton.setClickEvent(new ClickEvent(
                            ClickEvent.Action.RUN_COMMAND,
                            "/reports"));
                    reportsButton.setHoverEvent(new HoverEvent(
                            HoverEvent.Action.SHOW_TEXT,
                            new ComponentBuilder("§eClique para abrir o menu de reports").create()));

                    // Combinar botões
                    TextComponent actionLine = new TextComponent("");
                    actionLine.addExtra(observeButton);
                    actionLine.addExtra(new TextComponent("  "));
                    actionLine.addExtra(reportsButton);

                    staff.spigot().sendMessage(actionLine);
                } else {
                    // Mostrar apenas hint textual sem botão de observar
                    staff.sendMessage(actionHint);
                }

                staff.sendMessage("");
                staff.sendMessage(footer);
                staff.sendMessage("");
            }
        }

        // Log no console
        plugin.getLogger().info("[REPORT] " + report.getReporterName() + " reportou " +
                report.getTargetName() + " - Motivo: " + report.getReason());
    }

    // ==================== GERENCIAMENTO ====================

    /**
     * Remove um report por ID
     */
    public boolean removeReport(String id) {
        if (repository == null)
            return false;

        Report report = repository.getReport(id);
        if (report != null) {
            repository.deleteReportAsync(id);
            return true;
        }
        return false;
    }

    /**
     * Marca um report como concluído
     */
    public boolean completeReport(String id) {
        if (repository == null)
            return false;

        Report report = repository.getReport(id);
        if (report != null) {
            report.markAsComplete();
            repository.saveReportAsync(report);
            return true;
        }
        return false;
    }

    /**
     * Obtém um report por ID
     */
    public Report getReport(String id) {
        if (repository == null)
            return null;
        return repository.getReport(id);
    }

    /**
     * Obtém todos os reports abertos
     */
    public List<Report> getOpenReports() {
        if (repository == null)
            return new ArrayList<>();
        return repository.getOpenReports();
    }

    /**
     * Obtém todos os reports (incluindo concluídos)
     */
    public List<Report> getAllReports() {
        if (repository == null)
            return new ArrayList<>();
        return new ArrayList<>(repository.getAllReports());
    }

    /**
     * Conta reports abertos
     */
    public int countOpenReports() {
        if (repository == null)
            return 0;
        return repository.countOpenReports();
    }

    /**
     * Limpa todos os reports concluídos
     */
    public int clearCompletedReports() {
        if (repository == null)
            return 0;
        return repository.clearCompletedReports();
    }

    /**
     * Limpa reports antigos (mais de X dias)
     */
    public int clearOldReports(int days) {
        if (repository == null)
            return 0;
        return repository.clearOldReports(days);
    }

    // ==================== COOLDOWN (DELEGADO PARA COOLDOWNMANAGER)
    // ====================

    /**
     * Verifica se um jogador está em cooldown.
     * Delega para o CooldownManager central.
     * 
     * @deprecated Use plugin.getCooldownManager().isOnCooldown(uuid,
     *             CooldownManager.REPORT)
     */
    @Deprecated
    public boolean isOnCooldown(UUID uuid) {
        CooldownManager cooldownManager = plugin.getCooldownManager();
        if (cooldownManager != null) {
            return cooldownManager.isOnCooldown(uuid, CooldownManager.REPORT);
        }
        return false;
    }

    /**
     * Retorna o tempo restante de cooldown em segundos.
     * Delega para o CooldownManager central.
     * 
     * @deprecated Use plugin.getCooldownManager().getRemainingSeconds(uuid,
     *             CooldownManager.REPORT)
     */
    @Deprecated
    public long getCooldownRemaining(UUID uuid) {
        CooldownManager cooldownManager = plugin.getCooldownManager();
        if (cooldownManager != null) {
            return cooldownManager.getRemainingSeconds(uuid, CooldownManager.REPORT);
        }
        return 0;
    }

    // ==================== VERIFICAÇÕES ====================

    /**
     * Verifica se um jogador é staff.
     * Delega para o GroupManager centralizado.
     */
    public boolean isStaff(Player player) {
        if (player == null)
            return false;
        return plugin.getGroupManager() != null && plugin.getGroupManager().isStaff(player);
    }

    // ==================== UTILIDADES ====================

    /**
     * Gera um ID único para report
     */
    private String generateId() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * Recarrega reports do MongoDB
     */
    public void reload() {
        if (repository != null) {
            repository.loadAllReports();
        }
    }
}
