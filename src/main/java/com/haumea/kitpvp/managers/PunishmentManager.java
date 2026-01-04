package com.haumea.kitpvp.managers;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.database.MongoPunishmentRepository;
import com.haumea.kitpvp.models.Punishment;
import com.haumea.kitpvp.models.Punishment.PunishmentType;
import com.haumea.kitpvp.utils.ChatStorage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Gerenciador de Punições do HaumeaMC
 * 
 * Responsável por:
 * - Aplicar e revogar punições
 * - Verificar bans/mutes ativos
 * - IP Banning
 * - Persistência em MongoDB
 * 
 * @author HaumeaMC
 */
public class PunishmentManager {

    private final HaumeaMC plugin;
    private MongoPunishmentRepository repository;

    public PunishmentManager(HaumeaMC plugin) {
        this.plugin = plugin;

        // Inicializar repositório MongoDB
        initRepository();
        startExpirationTask();
    }

    /**
     * Inicializa o repositório MongoDB
     */
    private void initRepository() {
        if (plugin.getMongoManager() != null && plugin.getMongoManager().isConnected()) {
            this.repository = new MongoPunishmentRepository(plugin, plugin.getMongoManager());
            plugin.getLogger().info("[Punishments] MongoDB repository inicializado.");
        } else {
            plugin.getLogger().warning("[Punishments] MongoDB não disponível! Punições não serão persistidas.");
        }
    }

    // ==================== APLICAÇÃO DE PUNIÇÕES ====================

    /**
     * Aplica uma punição a um jogador
     * 
     * @param type       Tipo da punição
     * @param targetUuid UUID do alvo
     * @param targetName Nome do alvo
     * @param targetIp   IP do alvo (para ban)
     * @param staff      Staff que aplicou (pode ser null para console)
     * @param reason     Motivo
     * @param proof      Link da prova
     * @param duration   Duração em milissegundos (0 = permanente)
     * @return A punição criada
     */
    public Punishment applyPunishment(PunishmentType type, UUID targetUuid, String targetName,
            String targetIp, Player staff, String reason,
            String proof, long duration) {

        String id = generateId();
        long timestamp = System.currentTimeMillis();
        long expiration = duration > 0 ? timestamp + duration : 0;

        UUID staffUuid = staff != null ? staff.getUniqueId() : null;
        String staffName = staff != null ? staff.getName() : "Console";

        Punishment punishment = new Punishment(id, type, targetUuid, targetName, targetIp,
                staffUuid, staffName, reason, proof, timestamp, expiration, true);

        // Salvar no MongoDB
        if (repository != null) {
            repository.savePunishmentAsync(punishment);
        }

        // Aplicar efeitos imediatos
        Player targetPlayer = Bukkit.getPlayer(targetUuid);
        if (targetPlayer != null && targetPlayer.isOnline()) {
            applyPunishmentEffect(punishment, targetPlayer);
        }

        return punishment;
    }

    /**
     * Aplica o efeito da punição no jogador online
     */
    private void applyPunishmentEffect(Punishment punishment, Player player) {
        switch (punishment.getType()) {
            case BAN:
            case KICK:
                // Kickar o jogador com mensagem formatada
                String kickMessage = buildKickMessage(punishment);
                player.kickPlayer(ChatStorage.colorize(kickMessage));
                break;

            case MUTE:
                // Mensagem bonita multilinha de mute
                boolean isMutePermanent = punishment.getExpiration() == 0;

                ChatStorage.sendRaw(player, "");
                ChatStorage.send(player, "punishment.muted.header");
                ChatStorage.sendRaw(player, "");

                if (isMutePermanent) {
                    ChatStorage.send(player, "punishment.muted.permanent");
                } else {
                    ChatStorage.send(player, "punishment.muted.temporary",
                            "duration", punishment.getFormattedTimeRemaining());
                }

                ChatStorage.send(player, "punishment.muted.staff", "staff", punishment.getStaffName());
                ChatStorage.send(player, "punishment.muted.reason", "reason", punishment.getReason());

                if (punishment.getProof() != null && !punishment.getProof().isEmpty()) {
                    ChatStorage.send(player, "punishment.muted.proof", "proof", punishment.getProof());
                }

                ChatStorage.sendRaw(player, "");
                ChatStorage.send(player, "punishment.muted.appeal");
                ChatStorage.send(player, "punishment.muted.store");
                ChatStorage.sendRaw(player, "");
                ChatStorage.send(player, "punishment.muted.footer");
                ChatStorage.sendRaw(player, "");
                break;

            case WARN:
                // Mensagem bonita multilinha de warning
                int warnCount = countActiveWarnings(player.getUniqueId());

                ChatStorage.sendRaw(player, "");
                ChatStorage.send(player, "punishment.warned.header");
                ChatStorage.sendRaw(player, "");
                ChatStorage.send(player, "punishment.warned.title");
                ChatStorage.send(player, "punishment.warned.staff", "staff", punishment.getStaffName());
                ChatStorage.send(player, "punishment.warned.reason", "reason", punishment.getReason());

                if (punishment.getProof() != null && !punishment.getProof().isEmpty()) {
                    ChatStorage.send(player, "punishment.warned.proof", "proof", punishment.getProof());
                }

                ChatStorage.send(player, "punishment.warned.count", "count", String.valueOf(warnCount));
                ChatStorage.sendRaw(player, "");
                ChatStorage.send(player, "punishment.warned.warning");
                ChatStorage.sendRaw(player, "");
                ChatStorage.send(player, "punishment.warned.footer");
                ChatStorage.sendRaw(player, "");
                break;
        }
    }

    /**
     * Constrói a mensagem de kick/ban
     */
    /**
     * Constrói a mensagem de kick/ban
     */
    private String buildKickMessage(Punishment punishment) {
        StringBuilder sb = new StringBuilder();

        if (punishment.getType() == PunishmentType.BAN) {
            boolean isPermanent = punishment.getExpiration() == 0;
            List<String> lines;

            if (isPermanent) {
                lines = ChatStorage.getMessageList("punishment.screen.ban.permanent");
            } else {
                lines = ChatStorage.getMessageList("punishment.screen.ban.temporary");
            }

            for (String line : lines) {
                sb.append(line
                        .replace("{staff}", punishment.getStaffName())
                        .replace("{reason}", punishment.getReason())
                        .replace("{proof}", punishment.getProof())
                        .replace("{time}", punishment.getFormattedTimeRemaining()))
                        .append("\n");
            }
        } else {
            // KICK
            List<String> lines = ChatStorage.getMessageList("punishment.screen.kick");
            for (String line : lines) {
                sb.append(line
                        .replace("{staff}", punishment.getStaffName())
                        .replace("{reason}", punishment.getReason())
                        .replace("{proof}", punishment.getProof()))
                        .append("\n");
            }
        }

        return sb.toString();
    }

    // ==================== VERIFICAÇÕES ====================

    /**
     * Verifica se um jogador está banido
     */
    public Punishment getActiveBan(UUID uuid) {
        if (repository == null)
            return null;
        return repository.getActiveBan(uuid);
    }

    /**
     * Verifica se um IP está banido
     */
    public Punishment getIpBan(String ip) {
        if (repository == null)
            return null;
        return repository.getIpBan(ip);
    }

    /**
     * Verifica se um jogador está mutado
     */
    public Punishment getActiveMute(UUID uuid) {
        if (repository == null)
            return null;
        return repository.getActiveMute(uuid);
    }

    /**
     * Verifica se um jogador está banido (UUID ou IP)
     */
    public Punishment checkBan(UUID uuid, String ip) {
        // Primeiro verificar por UUID
        Punishment ban = getActiveBan(uuid);
        if (ban != null)
            return ban;

        // Depois verificar por IP
        return getIpBan(ip);
    }

    /**
     * Conta warnings ativos de um jogador
     */
    public int countActiveWarnings(UUID uuid) {
        if (repository == null)
            return 0;
        return repository.countActiveWarnings(uuid);
    }

    /**
     * Obtém histórico de punições de um jogador
     */
    public List<Punishment> getHistory(UUID uuid) {
        if (repository == null)
            return new ArrayList<>();
        return repository.getPlayerPunishments(uuid);
    }

    // ==================== REVOGAÇÃO ====================

    /**
     * Revoga (desban) um jogador por UUID
     * 
     * @return true se havia ban ativo para revogar
     */
    public boolean unban(UUID uuid) {
        Punishment ban = getActiveBan(uuid);
        if (ban == null)
            return false;

        ban.revoke();

        if (repository != null) {
            repository.savePunishmentAsync(ban);
        }
        return true;
    }

    /**
     * Revoga (unmute) um jogador por UUID
     * 
     * @return true se havia mute ativo para revogar
     */
    public boolean unmute(UUID uuid) {
        Punishment mute = getActiveMute(uuid);
        if (mute == null)
            return false;

        mute.revoke();

        if (repository != null) {
            repository.savePunishmentAsync(mute);
        }
        return true;
    }

    // ==================== VERIFICAÇÃO DE PERMISSÃO ====================

    /**
     * Verifica se um jogador é ADMIN ou superior (pode banir sem prova).
     * Delega para o GroupManager central.
     */
    public boolean isAdmin(Player player) {
        if (player == null) {
            return true; // Console é sempre admin
        }

        if (plugin.getGroupManager() != null) {
            return plugin.getGroupManager().isAdmin(player);
        }

        return false;
    }

    /**
     * Verifica se um jogador online é DONO (protegido de punições).
     * Delega para o GroupManager central.
     */
    public boolean isOwner(Player player) {
        if (player == null) {
            return false;
        }

        if (plugin.getGroupManager() != null) {
            return plugin.getGroupManager().isOwner(player);
        }

        return false;
    }

    /**
     * Verifica se um jogador (por UUID) é DONO (protegido de punições).
     * Delega para o GroupManager central.
     */
    public boolean isOwner(UUID uuid) {
        if (plugin.getGroupManager() != null) {
            return plugin.getGroupManager().isOwner(uuid);
        }

        return false;
    }

    /**
     * Verifica se um jogador é protegido contra punições.
     * Atualmente apenas DONOS são protegidos.
     * 
     * @return true se protegido
     */
    public boolean isProtected(UUID uuid) {
        return isOwner(uuid);
    }

    /**
     * Verifica se um link é uma prova válida (URL)
     */
    public boolean isValidProof(String proof) {
        if (proof == null || proof.isEmpty())
            return false;
        return proof.startsWith("http://") || proof.startsWith("https://");
    }

    /**
     * Notifica todos os donos online sobre uma tentativa de punição bloqueada
     * 
     * @param staffName  Nome do staffer que tentou punir
     * @param targetName Nome do dono que foi alvo
     * @param action     Ação que foi tentada (banir, kickar, mutar, avisar)
     */
    public void notifyOwners(String staffName, String targetName, String action) {
        for (Player owner : Bukkit.getOnlinePlayers()) {
            if (isOwner(owner)) {
                List<String> lines = ChatStorage.getMessageList("punishment.protected-alert");
                for (String line : lines) {
                    ChatStorage.sendRaw(owner, line
                            .replace("{staff}", staffName)
                            .replace("{target}", targetName)
                            .replace("{action}", action));
                }
            }
        }

        // Log no console também
        plugin.getLogger().warning("[PROTEÇÃO] " + staffName + " tentou " + action + " o DONO " + targetName);
    }

    // ==================== UTILIDADES ====================

    /**
     * Gera um ID único para punição
     */
    private String generateId() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * Inicia task para verificar expiração de punições
     */
    private void startExpirationTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            // O repositório já lida com expiração automaticamente
            // Apenas recarregar para atualizar cache se necessário
        }, 20L * 60L, 20L * 60L); // A cada 1 minuto
    }

    /**
     * Parseia uma string de tempo para milissegundos.
     * Delega para TimeUtils para centralização.
     * 
     * Formato: 1d (dias), 1h (horas), 1m (minutos), 1s (segundos)
     * Exemplo: "30d" = 30 dias, "1h30m" = 1 hora e 30 minutos
     * 
     * @return duração em milissegundos, 0 se permanente, -1 se inválido
     */
    public long parseTime(String timeStr) {
        return com.haumea.kitpvp.utils.TimeUtils.parseTime(timeStr);
    }

    /**
     * Recarrega punições do MongoDB
     */
    public void reload() {
        if (repository != null) {
            repository.loadAllPunishments();
        }
    }

    /**
     * Deleta uma punição específica por ID
     * 
     * @param id ID da punição
     */
    public void deletePunishment(String id) {
        if (repository != null) {
            repository.deletePunishment(id);
        }
    }

    /**
     * Deleta todas as punições de um jogador
     * 
     * @param uuid UUID do jogador
     * @return Número de punições deletadas
     */
    public int deleteAllPlayerPunishments(UUID uuid) {
        if (repository == null)
            return 0;

        List<Punishment> punishments = repository.getPlayerPunishments(uuid);
        int count = 0;

        for (Punishment p : punishments) {
            repository.deletePunishment(p.getId());
            count++;
        }

        return count;
    }

    /**
     * Obtém todas as punições relacionadas a um IP (para IP ban)
     * 
     * @param ip IP a buscar
     * @return Lista de punições
     */
    public List<Punishment> getPunishmentsByIp(String ip) {
        if (repository == null)
            return new ArrayList<>();

        List<Punishment> result = new ArrayList<>();
        for (Punishment p : repository.getAllPunishments()) {
            if (ip != null && ip.equals(p.getTargetIp())) {
                result.add(p);
            }
        }
        return result;
    }
}
