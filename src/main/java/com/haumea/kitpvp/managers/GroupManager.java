package com.haumea.kitpvp.managers;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.database.MongoAccountRepository;
import com.haumea.kitpvp.database.MongoAccountRepository.AccountData;
import com.haumea.kitpvp.database.MongoAccountRepository.GroupEntry;
import com.haumea.kitpvp.models.Group;
import com.haumea.kitpvp.utils.ChatStorage;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import com.haumea.kitpvp.permissions.AuthorityManager;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Gerenciador de Grupos/Cargos do servidor
 * 
 * MIGRADO PARA MONGODB!
 * - Definições de grupos continuam em groups.yml (configuração estática)
 * - Grupos de JOGADORES são armazenados no MongoDB via MongoAccountRepository
 * 
 * SUPORTA MÚLTIPLOS GRUPOS POR JOGADOR!
 * O grupo com maior prioridade é usado para prefixo/display.
 * 
 * @author HaumeaMC
 */
public class GroupManager {

    private final HaumeaMC plugin;
    private File groupsFile;
    private FileConfiguration groupsConfig;

    // Map de grupos (nome -> Group) - definições estáticas do groups.yml
    private final Map<String, Group> groups;

    public GroupManager(HaumeaMC plugin) {
        this.plugin = plugin;
        this.groups = new LinkedHashMap<>();

        loadGroupsFile();
        loadGroups();

        // Iniciar task para verificar expiração
        startExpirationTask();

        plugin.getLogger().info("[Groups] Gerenciador de grupos inicializado (MongoDB).");
    }

    /**
     * Obtém o repositório de contas do MongoDB
     */
    private MongoAccountRepository getAccountRepo() {
        return plugin.getMongoAccountRepository();
    }

    // ==================== CARREGAMENTO DE DEFINIÇÕES ====================

    /**
     * Carrega o arquivo groups.yml (definições de grupos)
     */
    private void loadGroupsFile() {
        groupsFile = new File(plugin.getDataFolder(), "groups.yml");

        if (!groupsFile.exists()) {
            plugin.saveResource("groups.yml", false);
        }

        groupsConfig = YamlConfiguration.loadConfiguration(groupsFile);
    }

    /**
     * Carrega todos os grupos do arquivo (definições)
     */
    public void loadGroups() {
        groups.clear();

        ConfigurationSection section = groupsConfig.getConfigurationSection("groups");
        if (section == null)
            return;

        for (String groupName : section.getKeys(false)) {
            String path = "groups." + groupName;

            String prefix = groupsConfig.getString(path + ".prefix", "");
            String displayName = groupsConfig.getString(path + ".display", groupName);
            int priority = groupsConfig.getInt(path + ".priority", 0);
            List<String> permissions = groupsConfig.getStringList(path + ".permissions");
            List<String> inheritance = groupsConfig.getStringList(path + ".inheritance");

            Group group = new Group(groupName, prefix, displayName, priority, permissions, inheritance);
            groups.put(groupName.toLowerCase(), group);
        }

        plugin.getLogger().info("Carregados " + groups.size() + " grupos.");
    }

    /**
     * Recarrega as definições de grupos do arquivo groups.yml
     */
    public void reload() {
        loadGroupsFile();
        loadGroups();
        plugin.getLogger().info("[Groups] Grupos recarregados do arquivo.");
    }

    // ==================== SALVAMENTO DE DEFINIÇÕES ====================

    /**
     * Salva as definições de grupos no arquivo
     */
    public void saveGroups() {
        for (Group group : groups.values()) {
            String path = "groups." + group.getName();

            groupsConfig.set(path + ".prefix", group.getPrefix());
            groupsConfig.set(path + ".display", group.getDisplayName());
            groupsConfig.set(path + ".priority", group.getPriority());
            groupsConfig.set(path + ".permissions", group.getPermissions());
            groupsConfig.set(path + ".inheritance", group.getInheritance());
        }

        try {
            groupsConfig.save(groupsFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ==================== GETTERS DE GRUPOS ====================

    /**
     * Obtém um grupo pelo nome
     */
    public Group getGroup(String name) {
        return groups.get(name.toLowerCase());
    }

    /**
     * Verifica se um grupo existe
     */
    public boolean groupExists(String name) {
        return groups.containsKey(name.toLowerCase());
    }

    /**
     * Obtém todos os grupos
     */
    public Collection<Group> getAllGroups() {
        return groups.values();
    }

    // ==================== MÚLTIPLOS GRUPOS - JOGADORES (MONGODB)
    // ====================

    /**
     * Adiciona um grupo ao jogador (NÃO substitui, ADICIONA)
     */
    public void addPlayerGroup(Player player, String groupName, long expiration) {
        addPlayerGroup(player.getUniqueId(), player.getName(), groupName, expiration);
    }

    /**
     * Adiciona um grupo ao jogador por UUID.
     * 
     * SISTEMA DE ACUMULAÇÃO DE TEMPO:
     * - Se já tem o grupo permanente (0), continua permanente
     * - Se já tem o grupo temporário, SOMA o tempo adicional
     * - Se não tem o grupo, adiciona normalmente
     * 
     * @param uuid       UUID do jogador
     * @param playerName Nome do jogador
     * @param groupName  Nome do grupo
     * @param expiration Timestamp de expiração (0 = permanente, ou tempo futuro)
     */
    public void addPlayerGroup(UUID uuid, String playerName, String groupName, long expiration) {
        if (!groupExists(groupName))
            return;

        MongoAccountRepository repo = getAccountRepo();
        if (repo == null) {
            plugin.getLogger().warning("[Groups] MongoDB não disponível para adicionar grupo!");
            return;
        }

        AccountData account = repo.getAccount(uuid);
        account.setName(playerName);
        account.setLastSeen(System.currentTimeMillis());

        // Verificar se já possui o grupo
        if (account.hasGroup(groupName)) {
            // SISTEMA DE ACUMULAÇÃO
            GroupEntry existingEntry = findGroupEntry(account, groupName);
            if (existingEntry != null) {
                long newExpiration = calculateAccumulatedExpiration(existingEntry.getExpiration(), expiration);
                existingEntry.setExpiration(newExpiration);
            }
        } else {
            account.addGroup(new GroupEntry(groupName.toLowerCase(), expiration));
        }

        // Salvar no MongoDB
        repo.saveAccountAsync(uuid, account);

        // Atualizar displays se online
        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline()) {
            notifyPermissionsChanged(player);
            updatePlayerDisplay(player);
        }
    }

    /**
     * Encontra uma entrada de grupo por nome
     */
    private GroupEntry findGroupEntry(AccountData account, String groupName) {
        for (GroupEntry entry : account.getGroups()) {
            if (entry.getGroupName().equalsIgnoreCase(groupName)) {
                return entry;
            }
        }
        return null;
    }

    /**
     * Calcula a expiração acumulada quando o jogador já possui o grupo.
     * 
     * Regras:
     * - Se já é permanente (0), continua permanente
     * - Se o novo é permanente (0), fica permanente
     * - Se ambos são temporários, SOMA o tempo restante + tempo adicional
     * 
     * @param currentExpiration Expiração atual (0 = permanente, ou timestamp)
     * @param newExpiration     Nova expiração (0 = permanente, ou timestamp)
     * @return Expiração calculada
     */
    private long calculateAccumulatedExpiration(long currentExpiration, long newExpiration) {
        // Se já é permanente ou novo é permanente, fica permanente
        if (currentExpiration == 0 || newExpiration == 0) {
            return 0;
        }

        long now = System.currentTimeMillis();

        // Calcular tempo restante do grupo atual
        long remainingTime = currentExpiration - now;
        if (remainingTime < 0) {
            remainingTime = 0; // Expirado, não tem tempo para somar
        }

        // Calcular tempo adicional do novo grupo
        long additionalTime = newExpiration - now;
        if (additionalTime < 0) {
            additionalTime = 0;
        }

        // Nova expiração = agora + tempo restante + tempo adicional
        return now + remainingTime + additionalTime;
    }

    /**
     * Remove um grupo específico do jogador
     * 
     * @return true se removido, false se não possuía
     */
    public boolean removePlayerGroup(UUID uuid, String groupName) {
        MongoAccountRepository repo = getAccountRepo();
        if (repo == null)
            return false;

        boolean removed = repo.removePlayerGroup(uuid, groupName);

        if (removed) {
            // Atualizar displays se online
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                notifyPermissionsChanged(player);
                updatePlayerDisplay(player);
            }
        }

        return removed;
    }

    /**
     * Remove um grupo específico do jogador online
     */
    public boolean removePlayerGroup(Player player, String groupName) {
        return removePlayerGroup(player.getUniqueId(), groupName);
    }

    /**
     * Verifica se um jogador possui um grupo específico
     */
    public boolean hasGroup(UUID uuid, String groupName) {
        MongoAccountRepository repo = getAccountRepo();
        if (repo == null)
            return groupName.equalsIgnoreCase("membro");

        return repo.hasGroup(uuid, groupName);
    }

    /**
     * Verifica se um jogador online possui um grupo
     */
    public boolean hasGroup(Player player, String groupName) {
        return hasGroup(player.getUniqueId(), groupName);
    }

    /**
     * Obtém todos os grupos de um jogador
     */
    public List<String> getPlayerGroupNames(UUID uuid) {
        MongoAccountRepository repo = getAccountRepo();
        if (repo == null) {
            return Collections.singletonList("membro");
        }

        AccountData account = repo.getAccount(uuid);

        // Remover grupos expirados primeiro
        account.removeExpiredGroups();
        if (account.getGroups().isEmpty()) {
            account.addGroup(new GroupEntry("membro", 0));
            repo.saveAccountAsync(uuid, account);
        }

        return account.getGroupNames();
    }

    /**
     * Obtém o grupo PRINCIPAL (maior prioridade) de um jogador
     * Este é usado para prefixo e display
     */
    public Group getPlayerGroup(Player player) {
        return getPlayerGroup(player.getUniqueId());
    }

    /**
     * Obtém o grupo PRINCIPAL (maior prioridade) de um jogador por UUID
     */
    public Group getPlayerGroup(UUID uuid) {
        List<String> groupNames = getPlayerGroupNames(uuid);

        // Encontrar grupo com maior prioridade
        Group highestGroup = null;
        int highestPriority = Integer.MIN_VALUE;

        for (String groupName : groupNames) {
            Group group = getGroup(groupName);
            if (group != null && group.getPriority() > highestPriority) {
                highestPriority = group.getPriority();
                highestGroup = group;
            }
        }

        return highestGroup != null ? highestGroup : getGroup("membro");
    }

    /**
     * Obtém o nome do grupo principal de um jogador
     */
    public String getPlayerGroupName(UUID uuid) {
        Group group = getPlayerGroup(uuid);
        return group != null ? group.getName() : "membro";
    }

    /**
     * Atualiza o display de um jogador baseado no grupo principal.
     * USA O DISPLAYMANAGER COMO FONTE UNIFICADA.
     */
    private void updatePlayerDisplay(Player player) {
        // Usar DisplayManager se disponível (fonte unificada)
        if (plugin.getDisplayManager() != null) {
            plugin.getDisplayManager().onGroupChange(player);
            return;
        }

        // Fallback: atualização manual
        Group group = getPlayerGroup(player);
        if (group != null && !group.getPrefix().isEmpty()) {
            String prefix = group.getPrefix().replace("&", "§");
            player.setDisplayName(prefix + player.getName());
            player.setPlayerListName(prefix + player.getName());
        } else {
            player.setDisplayName("§7" + player.getName());
            player.setPlayerListName("§7" + player.getName());
        }

        // Atualizar TabList
        if (plugin.getTabManager() != null && plugin.getTabManager().isRunning()) {
            plugin.getTabManager().updatePlayerTeam(player);
            plugin.getTabManager().updateTabList(player);
        }
    }

    /**
     * Notifica o sistema de permissões que houve mudança no grupo do jogador.
     * 
     * Este método é chamado sempre que:
     * - Um grupo é adicionado ao jogador
     * - Um grupo é removido do jogador
     * - Um grupo expira
     * 
     * Atualiza tanto o PermissionManager (attachments) quanto o
     * AuthorityManager (cache de permissões customizado).
     * 
     * @param player O jogador que teve mudança de grupo
     */
    private void notifyPermissionsChanged(Player player) {
        // 1. Atualizar PermissionManager (attachments padrão do Bukkit)
        if (plugin.getPermissionManager() != null) {
            plugin.getPermissionManager().forceUpdate(player);
        }

        // 2. Atualizar AuthorityManager (nosso sistema customizado)
        AuthorityManager authorityManager = plugin.getAuthorityManager();
        if (authorityManager != null) {
            authorityManager.updatePermissions(player);
        }
    }

    // ==================== MÉTODOS LEGADOS (compatibility) ====================

    /**
     * Define o grupo de um jogador (SUBSTITUI por único grupo - uso legado)
     * 
     * @deprecated Use addPlayerGroup para adicionar grupos
     */
    @Deprecated
    public void setPlayerGroup(Player player, String groupName, long expiration) {
        setPlayerGroup(player.getUniqueId(), player.getName(), groupName, expiration);
    }

    /**
     * Define o grupo de um jogador por UUID (SUBSTITUI por único grupo - uso
     * legado)
     * 
     * @deprecated Use addPlayerGroup para adicionar grupos
     */
    @Deprecated
    public void setPlayerGroup(UUID uuid, String playerName, String groupName, long expiration) {
        if (!groupExists(groupName))
            return;

        MongoAccountRepository repo = getAccountRepo();
        if (repo == null)
            return;

        AccountData account = repo.getAccount(uuid);
        account.setName(playerName);

        // Limpar grupos anteriores e definir apenas este
        account.clearGroups();
        account.addGroup(new GroupEntry(groupName.toLowerCase(), expiration));

        // Salvar no MongoDB
        repo.saveAccountAsync(uuid, account);

        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline()) {
            notifyPermissionsChanged(player);
            updatePlayerDisplay(player);
        }
    }

    /**
     * Método legado - não faz mais nada, grupos são salvos automaticamente no
     * MongoDB
     * 
     * @deprecated Grupos são salvos automaticamente no MongoDB
     */
    @Deprecated
    public void savePlayerGroups() {
        // Não faz nada - grupos são salvos automaticamente no MongoDB
        plugin.getLogger().info("[Groups] savePlayerGroups() chamado - ignorado (MongoDB auto-save)");
    }

    /**
     * Método legado - não faz mais nada
     * 
     * @deprecated Grupos são carregados do MongoDB
     */
    @Deprecated
    public void loadPlayerGroups() {
        // Não faz nada - grupos são carregados do MongoDB
        plugin.getLogger().info("[Groups] loadPlayerGroups() chamado - ignorado (MongoDB)");
    }

    // ==================== EXPIRAÇÃO ====================

    /**
     * Inicia task para verificar expiração de grupos
     * Executa a cada 1 minuto
     */
    private void startExpirationTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                checkAndRemoveExpiredGroups(player.getUniqueId());
            }
        }, 20L, 20L); // A cada 1 segundo (Instantâneo)
    }

    /**
     * Verifica e remove grupos expirados de um jogador
     */
    private void checkAndRemoveExpiredGroups(UUID uuid) {
        MongoAccountRepository repo = getAccountRepo();
        if (repo == null)
            return;

        AccountData account = repo.getAccount(uuid);

        // Verificar se há grupos expirados
        List<GroupEntry> groupsBefore = new ArrayList<>(account.getGroups());
        account.removeExpiredGroups();

        boolean changed = groupsBefore.size() != account.getGroups().size();

        if (changed) {
            // Garantir pelo menos membro
            if (account.getGroups().isEmpty()) {
                account.addGroup(new GroupEntry("membro", 0));
            }
            repo.saveAccountAsync(uuid, account);

            // Notificar jogador se online
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                ChatStorage.send(player, "group.expired-alert");
                notifyPermissionsChanged(player);
                updatePlayerDisplay(player);
            }
        }
    }

    /**
     * Obtém o tempo restante do grupo principal de um jogador
     * 
     * @return tempo restante em milissegundos, 0 = permanente, -1 = expirado
     */
    public long getTimeRemaining(UUID uuid, String groupName) {
        MongoAccountRepository repo = getAccountRepo();
        if (repo == null)
            return -1;

        AccountData account = repo.getAccount(uuid);
        for (GroupEntry entry : account.getGroups()) {
            if (entry.getGroupName().equalsIgnoreCase(groupName)) {
                return entry.getTimeRemaining();
            }
        }
        return -1;
    }

    /**
     * Obtém o timestamp de expiração de um grupo específico para um jogador
     * 
     * @return timestamp de expiração, 0 = permanente, -1 = não possui
     */
    public long getGroupExpiration(UUID uuid, String groupName) {
        MongoAccountRepository repo = getAccountRepo();
        if (repo == null)
            return -1;

        AccountData account = repo.getAccount(uuid);
        for (GroupEntry entry : account.getGroups()) {
            if (entry.getGroupName().equalsIgnoreCase(groupName)) {
                return entry.getExpiration();
            }
        }
        return -1;
    }

    // ==================== PERMISSÕES ====================

    /**
     * Obtém todas as permissões de TODOS os grupos de um jogador
     */
    public Set<String> getAllPermissions(UUID uuid) {
        Set<String> allPermissions = new HashSet<>();

        List<String> groupNames = getPlayerGroupNames(uuid);
        for (String groupName : groupNames) {
            Group group = getGroup(groupName);
            if (group != null) {
                allPermissions.addAll(getAllPermissions(group));
            }
        }

        return allPermissions;
    }

    /**
     * Obtém todas as permissões de um grupo (incluindo herança)
     */
    public Set<String> getAllPermissions(Group group) {
        Set<String> allPermissions = new HashSet<>(group.getPermissions());

        // Adicionar permissões herdadas
        for (String inheritName : group.getInheritance()) {
            Group inheritGroup = getGroup(inheritName);
            if (inheritGroup != null) {
                allPermissions.addAll(getAllPermissions(inheritGroup));
            }
        }

        return allPermissions;
    }

    /**
     * Verifica se um jogador tem uma permissão (considera TODOS os grupos)
     */
    public boolean hasPermission(Player player, String permission) {
        Set<String> allPerms = getAllPermissions(player.getUniqueId());

        // Verificar permissão exata ou wildcard
        if (allPerms.contains("*") || allPerms.contains(permission.toLowerCase())) {
            return true;
        }

        // Verificar wildcards parciais (ex: haumea.tag.*)
        String[] parts = permission.split("\\.");
        StringBuilder check = new StringBuilder();
        for (int i = 0; i < parts.length - 1; i++) {
            if (i > 0)
                check.append(".");
            check.append(parts[i]);
            if (allPerms.contains(check.toString() + ".*")) {
                return true;
            }
        }

        return false;
    }

    // ==================== VERIFICAÇÃO DE STAFF ====================

    /**
     * Níveis de staff do servidor.
     * Maior número = maior autoridade.
     */
    public static final int STAFF_LEVEL_NONE = 0; // Jogador comum
    public static final int STAFF_LEVEL_HELPER = 1; // Ajudante
    public static final int STAFF_LEVEL_MOD = 2; // Moderador
    public static final int STAFF_LEVEL_ADMIN = 3; // Admin
    public static final int STAFF_LEVEL_MANAGER = 4; // Gerente
    public static final int STAFF_LEVEL_DIRECTOR = 5; // Diretor
    public static final int STAFF_LEVEL_OWNER = 6; // Dono

    /**
     * Verifica se um jogador é QUALQUER tipo de staff (ajudante ou superior).
     * 
     * @param player Jogador a verificar
     * @return true se é staff
     */
    public boolean isStaff(Player player) {
        if (player == null)
            return false;
        return getStaffLevel(player) >= STAFF_LEVEL_HELPER;
    }

    /**
     * Verifica se um jogador é staff por UUID.
     */
    public boolean isStaff(UUID uuid) {
        return getStaffLevel(uuid) >= STAFF_LEVEL_HELPER;
    }

    /**
     * Verifica se um jogador é STAFF SUPERIOR (moderador ou superior).
     * Exclui ajudantes.
     * 
     * @param player Jogador a verificar
     * @return true se é moderador+
     */
    public boolean isHigherStaff(Player player) {
        if (player == null)
            return false;
        return getStaffLevel(player) >= STAFF_LEVEL_MOD;
    }

    /**
     * Verifica se um jogador é staff superior por UUID.
     */
    public boolean isHigherStaff(UUID uuid) {
        return getStaffLevel(uuid) >= STAFF_LEVEL_MOD;
    }

    /**
     * Verifica se um jogador é ADMIN ou superior.
     * 
     * @param player Jogador a verificar
     * @return true se é admin+
     */
    public boolean isAdmin(Player player) {
        if (player == null)
            return false;
        return getStaffLevel(player) >= STAFF_LEVEL_ADMIN;
    }

    /**
     * Verifica se um jogador é admin por UUID.
     */
    public boolean isAdmin(UUID uuid) {
        return getStaffLevel(uuid) >= STAFF_LEVEL_ADMIN;
    }

    /**
     * Verifica se um jogador é GERENTE ou superior.
     * Staff com bypass de cooldown.
     * 
     * @param player Jogador a verificar
     * @return true se é gerente+
     */
    public boolean isManager(Player player) {
        if (player == null)
            return false;
        return getStaffLevel(player) >= STAFF_LEVEL_MANAGER;
    }

    /**
     * Verifica se um jogador é gerente por UUID.
     */
    public boolean isManager(UUID uuid) {
        return getStaffLevel(uuid) >= STAFF_LEVEL_MANAGER;
    }

    /**
     * Verifica se um jogador é DONO ou DIRETOR.
     * 
     * @param player Jogador a verificar
     * @return true se é diretor+
     */
    public boolean isDirector(Player player) {
        if (player == null)
            return false;
        return getStaffLevel(player) >= STAFF_LEVEL_DIRECTOR;
    }

    /**
     * Verifica se um jogador é diretor por UUID.
     */
    public boolean isDirector(UUID uuid) {
        return getStaffLevel(uuid) >= STAFF_LEVEL_DIRECTOR;
    }

    /**
     * Verifica se um jogador é DONO.
     * 
     * @param player Jogador a verificar
     * @return true se é dono
     */
    public boolean isOwner(Player player) {
        if (player == null)
            return false;
        return getStaffLevel(player) >= STAFF_LEVEL_OWNER;
    }

    /**
     * Verifica se um jogador é dono por UUID.
     */
    public boolean isOwner(UUID uuid) {
        return getStaffLevel(uuid) >= STAFF_LEVEL_OWNER;
    }

    /**
     * Obtém o nível de staff de um jogador.
     * Considera TODOS os grupos que o jogador possui.
     * 
     * @param player Jogador
     * @return Nível de staff (0 = comum)
     */
    public int getStaffLevel(Player player) {
        return getStaffLevel(player.getUniqueId());
    }

    /**
     * Obtém o nível de staff por UUID.
     * Considera TODOS os grupos que o jogador possui.
     */
    public int getStaffLevel(UUID uuid) {
        int highestLevel = STAFF_LEVEL_NONE;

        for (String groupName : getPlayerGroupNames(uuid)) {
            int level = getStaffLevelForGroup(groupName);
            if (level > highestLevel) {
                highestLevel = level;
            }
        }

        return highestLevel;
    }

    /**
     * Obtém o nível de staff para um nome de grupo específico.
     */
    private int getStaffLevelForGroup(String groupName) {
        String name = groupName.toLowerCase();
        switch (name) {
            case "dono":
                return STAFF_LEVEL_OWNER;
            case "diretor":
                return STAFF_LEVEL_DIRECTOR;
            case "gerente":
                return STAFF_LEVEL_MANAGER;
            case "admin":
            case "administrador":
                return STAFF_LEVEL_ADMIN;
            case "mod":
            case "moderador":
                return STAFF_LEVEL_MOD;
            case "ajudante":
            case "helper":
                return STAFF_LEVEL_HELPER;
            default:
                return STAFF_LEVEL_NONE;
        }
    }

    /**
     * Verifica se um jogador pode gerenciar outro baseado no nível de staff.
     * Um staff só pode gerenciar outro se tiver nível MAIOR.
     * 
     * @param manager Jogador que quer gerenciar
     * @param target  Jogador alvo
     * @return true se pode gerenciar
     */
    public boolean canManage(Player manager, Player target) {
        if (manager == null || target == null)
            return false;

        // Mesma pessoa não pode se gerenciar
        if (manager.getUniqueId().equals(target.getUniqueId()))
            return false;

        int managerLevel = getStaffLevel(manager);
        int targetLevel = getStaffLevel(target);

        // Precisa ter nível MAIOR
        return managerLevel > targetLevel;
    }

    /**
     * Verifica se um jogador pode gerenciar outro por UUID.
     */
    public boolean canManage(UUID managerUuid, UUID targetUuid) {
        if (managerUuid == null || targetUuid == null)
            return false;
        if (managerUuid.equals(targetUuid))
            return false;

        int managerLevel = getStaffLevel(managerUuid);
        int targetLevel = getStaffLevel(targetUuid);

        return managerLevel > targetLevel;
    }

    /**
     * Formata o tempo restante de um grupo em String legível.
     * 
     * @param uuid      UUID do jogador
     * @param groupName Nome do grupo
     * @return String formatada ou "Permanente" ou "Expirado"
     */
    public String formatTimeRemaining(UUID uuid, String groupName) {
        long remaining = getTimeRemaining(uuid, groupName);

        if (remaining == 0)
            return "§aPermanente";
        if (remaining < 0)
            return "§cExpirado";

        // Converter para unidades
        long seconds = remaining / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return "§e" + days + " dia(s)";
        } else if (hours > 0) {
            return "§e" + hours + " hora(s)";
        } else if (minutes > 0) {
            return "§e" + minutes + " minuto(s)";
        } else {
            return "§e" + seconds + " segundo(s)";
        }
    }

    /**
     * Lista todos os grupos de um jogador formatados.
     * 
     * @param uuid UUID do jogador
     * @return Lista de strings formatadas
     */
    public List<String> listPlayerGroupsFormatted(UUID uuid) {
        List<String> result = new ArrayList<>();

        MongoAccountRepository repo = getAccountRepo();
        if (repo == null) {
            result.add("§7membro §8(Permanente)");
            return result;
        }

        AccountData account = repo.getAccount(uuid);
        for (GroupEntry entry : account.getGroups()) {
            Group group = getGroup(entry.getGroupName());
            String displayName = group != null ? group.getDisplayName() : entry.getGroupName();
            String timeStr = entry.getExpiration() == 0 ? "§aPermanente"
                    : formatTimeRemaining(uuid, entry.getGroupName());
            result.add("§f" + displayName + " §8(" + timeStr + "§8)");
        }

        if (result.isEmpty()) {
            result.add("§7membro §8(Permanente)");
        }

        return result;
    }
}
