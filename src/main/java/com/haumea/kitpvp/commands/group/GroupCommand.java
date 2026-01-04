package com.haumea.kitpvp.commands.group;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.database.MongoAccountRepository;
import com.haumea.kitpvp.database.MongoAccountRepository.AccountData;
import com.haumea.kitpvp.managers.GroupManager;
import com.haumea.kitpvp.managers.PermissionManager;
import com.haumea.kitpvp.models.Group;
import com.haumea.kitpvp.utils.ChatStorage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Comando /haumeagroups - Sistema completo de gerenciamento de MÚLTIPLOS grupos
 * 
 * Apenas DONO, DIRETOR e GERENTE podem usar este comando!
 * 
 * SUPORTA MÚLTIPLOS GRUPOS POR JOGADOR!
 * O jogador pode ter vários grupos simultaneamente (ex: Trial + LIGHT + Membro)
 * O grupo com maior prioridade é usado para o prefixo/display.
 * 
 * Subcomandos:
 * - add <jogador> <grupo> [tempo] - ADICIONA um grupo ao jogador
 * - remove <jogador> <grupo> - Remove um grupo ESPECÍFICO do jogador
 * - list - Lista todos os grupos disponíveis
 * - info <jogador> - Mostra TODOS os grupos do jogador
 * - reload - Recarrega configurações e storage
 * 
 * Aliases: /haumeagrupos, /haumeagrupo, /haumeagroup, /hgroups, /setgroup,
 * /grupos
 * 
 * @author HaumeaMC
 */
public class GroupCommand implements CommandExecutor {

    private final HaumeaMC plugin;
    private final GroupManager groupManager;
    private final PermissionManager permissionManager;
    private final MongoAccountRepository accountRepository;

    // Grupos que podem usar o comando
    private static final String[] ALLOWED_GROUPS = { "dono", "diretor", "gerente" };

    // Grupos que recebem o staff broadcast
    private static final String[] STAFF_GROUPS = {
            "dono", "diretor", "gerente", "admin", "modplus", "modgc", "mod", "trial"
    };

    // Formatador de data
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    public GroupCommand(HaumeaMC plugin) {
        this.plugin = plugin;
        this.groupManager = plugin.getGroupManager();
        this.permissionManager = plugin.getPermissionManager();
        this.accountRepository = plugin.getMongoAccountRepository();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // Verificar permissão
        if (!hasPermission(sender)) {
            ChatStorage.send(sender, "group.no-permission");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender, label);
            return true;
        }

        // Verificar se é um subcomando especial (list, info, reload)
        String firstArg = args[0].toLowerCase();

        switch (firstArg) {
            case "list":
            case "lista":
            case "listar":
                handleList(sender);
                return true;

            case "reload":
            case "recarregar":
                handleReload(sender);
                return true;
        }

        // Nova sintaxe: /haumeagroups <jogador> <add|set|remove|info> <grupo> [tempo]
        // args[0] = jogador ou "info"
        // args[1] = operação (add, set, remove) OU nada se for info
        // args[2] = grupo
        // args[3] = tempo (opcional)

        // Comando info com jogador: /haumeagroups info <jogador> OU /haumeagroups
        // <jogador> info
        if (firstArg.equals("info") || firstArg.equals("ver") || firstArg.equals("check")
                || firstArg.equals("checar")) {
            handleInfo(sender, args, label);
            return true;
        }

        // Verificar se tem argumentos suficientes para operação com jogador
        if (args.length < 3) {
            // Pode ser /haumeagroups <jogador> info
            if (args.length == 2) {
                String operation = args[1].toLowerCase();
                if (operation.equals("info") || operation.equals("ver") || operation.equals("check")
                        || operation.equals("checar")) {
                    // Reorganizar: args[0] = jogador, args[1] = info
                    String[] newArgs = new String[] { "info", args[0] };
                    handleInfo(sender, newArgs, label);
                    return true;
                }
            }
            sendHelp(sender, label);
            return true;
        }

        String playerName = args[0];
        String operation = args[1].toLowerCase();
        String groupName = args[2];
        String timeStr = args.length >= 4 ? args[3] : null;

        switch (operation) {
            case "add":
            case "adicionar":
            case "set":
            case "definir":
                addPlayerGroup(sender, playerName, groupName, timeStr);
                break;

            case "remove":
            case "remover":
            case "delete":
            case "deletar":
                removePlayerGroup(sender, playerName, groupName);
                break;

            default:
                sender.sendMessage(ChatStorage.colorize("&6&lHAUMEAGROUPS&f Operação inválida: &c" + operation));
                sender.sendMessage(ChatStorage.colorize("&7Use: &aadd&7, &eset&7 ou &cremove"));
                break;
        }

        return true;
    }

    // ==================== HANDLERS DE SUBCOMANDOS ====================

    /**
     * Handler: /haumeagroups list
     */
    private void handleList(CommandSender sender) {
        Collection<Group> allGroups = groupManager.getAllGroups();

        sender.sendMessage(ChatStorage.getSeparator());
        sender.sendMessage(
                ChatStorage.colorize("&6&lHAUMEAGROUPS &8- &fGrupos Disponíveis &7(" + allGroups.size() + ")"));
        sender.sendMessage(ChatStorage.getSeparator());

        // Agrupar por categoria
        StringBuilder staff = new StringBuilder();
        StringBuilder vip = new StringBuilder();
        StringBuilder outros = new StringBuilder();

        for (Group group : allGroups) {
            String display = getGroupDisplayWithColor(group) + "&f";
            String name = group.getName();

            if (isStaffGroup(name)) {
                if (staff.length() > 0)
                    staff.append("&7, ");
                staff.append(display);
            } else if (isVipGroup(name)) {
                if (vip.length() > 0)
                    vip.append("&7, ");
                vip.append(display);
            } else {
                if (outros.length() > 0)
                    outros.append("&7, ");
                outros.append(display);
            }
        }

        if (staff.length() > 0) {
            sender.sendMessage(ChatStorage.colorize("&c&lSTAFF: &f" + staff.toString()));
        }
        if (vip.length() > 0) {
            sender.sendMessage(ChatStorage.colorize("&a&lVIP: &f" + vip.toString()));
        }
        if (outros.length() > 0) {
            sender.sendMessage(ChatStorage.colorize("&7&lOUTROS: &f" + outros.toString()));
        }

        sender.sendMessage(ChatStorage.getSeparator());
    }

    /**
     * Handler: /haumeagroups info <jogador>
     * Mostra TODOS os grupos do jogador
     */
    private void handleInfo(CommandSender sender, String[] args, String label) {
        if (args.length < 2) {
            sender.sendMessage(ChatStorage.colorize("&6&lHAUMEAGROUPS&f Uso: &e/" + label + " info <jogador>"));
            return;
        }

        String playerName = args[1];
        showPlayerInfo(sender, playerName);
    }

    /**
     * Handler: /haumeagroups reload
     */
    private void handleReload(CommandSender sender) {
        groupManager.reload();

        if (accountRepository != null) {
            accountRepository.clearCacheAndReload();
        }

        permissionManager.updateAllPlayers();

        sender.sendMessage(
                ChatStorage.colorize("&6&lHAUMEAGROUPS&f Configurações e storage &a&lrecarregados&f com sucesso!"));
        sender.sendMessage(
                ChatStorage.colorize("&7» Grupos: &e" + groupManager.getAllGroups().size() + " &7carregados"));
        if (accountRepository != null) {
            sender.sendMessage(
                    ChatStorage.colorize("&7» Contas: &e" + accountRepository.getTotalAccounts() + " &7em cache"));
        }
    }

    // ==================== LÓGICA PRINCIPAL ====================

    /**
     * ADICIONA um grupo ao jogador (não substitui os existentes)
     */
    private void addPlayerGroup(CommandSender sender, String playerName, String groupName, String timeStr) {
        // Verificar se o grupo existe
        if (!groupManager.groupExists(groupName)) {
            ChatStorage.send(sender, "group.not-found", "group", groupName);
            sender.sendMessage(ChatStorage.colorize("&7Use &e/haumeagroups list &7para ver os grupos disponíveis."));
            return;
        }

        // Obter informações do grupo
        Group group = groupManager.getGroup(groupName);
        String groupDisplayWithColor = getGroupDisplayWithColor(group);

        // Calcular tempo de expiração
        long expiration = 0; // 0 = permanente
        String timeDisplay = "&aPermanente";

        if (timeStr != null && !timeStr.isEmpty()) {
            expiration = parseTime(timeStr);
            if (expiration == -1) {
                for (String msg : ChatStorage.getMessageList("group.invalid-time-format")) {
                    sender.sendMessage(ChatStorage.colorize(msg));
                }
                return;
            }
            expiration = System.currentTimeMillis() + expiration;
            timeDisplay = timeStr;
        }

        String adminName = sender instanceof Player ? ((Player) sender).getName() : "CONSOLE";

        // Tentar encontrar jogador online
        Player targetPlayer = Bukkit.getPlayer(playerName);

        if (targetPlayer != null) {
            handleOnlinePlayerAdd(sender, targetPlayer, groupName, group, groupDisplayWithColor, expiration, timeStr,
                    timeDisplay, adminName);
        } else {
            handleOfflinePlayerAdd(sender, playerName, groupName, group, groupDisplayWithColor, expiration, timeStr,
                    timeDisplay, adminName);
        }
    }

    /**
     * Processa ADD para jogador ONLINE
     */
    private void handleOnlinePlayerAdd(CommandSender sender, Player targetPlayer, String groupName,
            Group group, String groupDisplayWithColor, long expiration, String timeStr, String timeDisplay,
            String adminName) {

        UUID targetUUID = targetPlayer.getUniqueId();

        // Verificar se já possui o grupo e obter tempo restante ANTES da adição
        boolean alreadyHas = groupManager.hasGroup(targetUUID, groupName);
        long previousTimeRemaining = 0;
        boolean wasPermanent = false;
        if (alreadyHas) {
            previousTimeRemaining = groupManager.getTimeRemaining(targetUUID, groupName);
            wasPermanent = (previousTimeRemaining == 0);
        }

        // ADICIONAR grupo (com acumulação de tempo)
        groupManager.addPlayerGroup(targetPlayer, groupName, expiration);

        // Salvar no MongoAccountRepository
        if (accountRepository != null) {
            accountRepository.addPlayerGroup(targetUUID, targetPlayer.getName(), groupName, expiration);
        }

        // Atualizar permissões
        permissionManager.setupPermissions(targetPlayer);

        // Obter novo tempo restante APÓS a adição
        long newTimeRemaining = groupManager.getTimeRemaining(targetUUID, groupName);

        // 1. FEEDBACK PARA O ADMIN
        if (alreadyHas) {
            if (wasPermanent) {
                sender.sendMessage(ChatStorage.colorize("&6&lHAUMEAGROUPS&f Grupo " + groupDisplayWithColor +
                        "&f do jogador &e&l" + targetPlayer.getName() + "&f já é &a&lPERMANENTE&f."));
                sender.sendMessage(ChatStorage.colorize("&7O tempo adicional foi ignorado (já é eterno)."));
            } else if (newTimeRemaining == 0) {
                sender.sendMessage(ChatStorage.colorize("&6&lHAUMEAGROUPS&f Grupo " + groupDisplayWithColor +
                        "&f do jogador &e&l" + targetPlayer.getName() + "&f agora é &a&lPERMANENTE&f!"));
            } else {
                sender.sendMessage(ChatStorage.colorize("&6&lHAUMEAGROUPS&f Tempo &b&lACUMULADO&f no grupo " +
                        groupDisplayWithColor + "&f do jogador &e&l" + targetPlayer.getName() + "&f!"));
                sender.sendMessage(ChatStorage.colorize("&7Tempo anterior: &e" +
                        (previousTimeRemaining > 0 ? formatTime(previousTimeRemaining) : "0s") +
                        " &7+ Novo: &e" + timeDisplay + " &7= &a&lTotal: " + formatTime(newTimeRemaining)));
            }
        } else {
            sender.sendMessage(ChatStorage.colorize("&6&lHAUMEAGROUPS&f Grupo " + groupDisplayWithColor +
                    "&f foi &a&lADICIONADO&f ao jogador &e&l" + targetPlayer.getName() + " &f(&7"
                    + targetUUID.toString() + "&f)."));

            if (timeStr != null) {
                sender.sendMessage(ChatStorage.colorize("&7Duração: &e" + timeDisplay));
            } else {
                sender.sendMessage(ChatStorage.colorize("&7Duração: &aPermanente"));
            }
        }

        // Mostrar todos os grupos atuais
        List<String> allGroups = groupManager.getPlayerGroupNames(targetUUID);
        sender.sendMessage(ChatStorage.colorize("&7Grupos atuais: &e" + String.join("&7, &e", allGroups)));

        // 2. FEEDBACK PARA O JOGADOR ALVO
        if (alreadyHas && !wasPermanent && timeStr != null) {
            targetPlayer.sendMessage(ChatStorage.colorize("&6&lHAUMEAMC&f Seu grupo " + groupDisplayWithColor +
                    "&f foi &b&lRENOVADO&f! Tempo total: &a" + formatTime(newTimeRemaining)));
        } else {
            targetPlayer.sendMessage(ChatStorage.colorize("&6&lHAUMEAMC&f O grupo " + groupDisplayWithColor +
                    "&f foi &a&lADICIONADO&f aos seus grupos!"));
        }

        // 3. TITLE NA TELA
        sendTitle(targetPlayer, groupDisplayWithColor);

        // 4. SINCRONIZAÇÃO DE TAG (usa grupo principal - maior prioridade)
        Group mainGroup = groupManager.getPlayerGroup(targetPlayer);
        if (mainGroup != null) {
            syncPlayerTag(targetPlayer, mainGroup.getName());
        }

        // 5. ATUALIZAR TABLIST
        updateTabList(targetPlayer);

        // 6. STAFF BROADCAST
        String action = alreadyHas ? (wasPermanent ? "tentou renovar (já permanente)" : "acumulou tempo no")
                : "adicionou";
        broadcastToStaff(adminName, targetPlayer.getName(), groupDisplayWithColor, action, sender);
    }

    /**
     * Processa ADD para jogador OFFLINE
     */
    private void handleOfflinePlayerAdd(CommandSender sender, String playerName, String groupName,
            Group group, String groupDisplayWithColor, long expiration, String timeStr, String timeDisplay,
            String adminName) {

        @SuppressWarnings("deprecation")
        org.bukkit.OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);

        if (offlinePlayer.hasPlayedBefore() || offlinePlayer.isOnline()) {
            UUID targetUUID = offlinePlayer.getUniqueId();

            // Verificar se já possui o grupo e obter tempo restante ANTES da adição
            boolean alreadyHas = groupManager.hasGroup(targetUUID, groupName);
            long previousTimeRemaining = 0;
            boolean wasPermanent = false;
            if (alreadyHas) {
                previousTimeRemaining = groupManager.getTimeRemaining(targetUUID, groupName);
                wasPermanent = (previousTimeRemaining == 0);
            }

            // ADICIONAR grupo (com acumulação de tempo)
            groupManager.addPlayerGroup(targetUUID, playerName, groupName, expiration);

            if (accountRepository != null) {
                accountRepository.addPlayerGroup(targetUUID, playerName, groupName, expiration);
            }

            // Obter novo tempo restante APÓS a adição
            long newTimeRemaining = groupManager.getTimeRemaining(targetUUID, groupName);

            // Feedback
            if (alreadyHas) {
                if (wasPermanent) {
                    sender.sendMessage(ChatStorage.colorize("&6&lHAUMEAGROUPS&f Grupo " + groupDisplayWithColor +
                            "&f do jogador offline &e&l" + playerName + "&f já é &a&lPERMANENTE&f."));
                    sender.sendMessage(ChatStorage.colorize("&7O tempo adicional foi ignorado (já é eterno)."));
                } else if (newTimeRemaining == 0) {
                    sender.sendMessage(ChatStorage.colorize("&6&lHAUMEAGROUPS&f Grupo " + groupDisplayWithColor +
                            "&f do jogador offline &e&l" + playerName + "&f agora é &a&lPERMANENTE&f!"));
                } else {
                    sender.sendMessage(ChatStorage.colorize("&6&lHAUMEAGROUPS&f Tempo &b&lACUMULADO&f no grupo " +
                            groupDisplayWithColor + "&f do jogador offline &e&l" + playerName + "&f!"));
                    sender.sendMessage(ChatStorage.colorize("&7Tempo anterior: &e" +
                            (previousTimeRemaining > 0 ? formatTime(previousTimeRemaining) : "0s") +
                            " &7+ Novo: &e" + timeDisplay + " &7= &a&lTotal: " + formatTime(newTimeRemaining)));
                }
            } else {
                sender.sendMessage(ChatStorage.colorize("&6&lHAUMEAGROUPS&f Grupo " + groupDisplayWithColor +
                        "&f foi &a&lADICIONADO&f ao jogador offline &e&l" + playerName + " &f(&7"
                        + targetUUID.toString() + "&f)."));

                if (timeStr != null) {
                    sender.sendMessage(ChatStorage.colorize("&7Duração: &e" + timeDisplay));
                } else {
                    sender.sendMessage(ChatStorage.colorize("&7Duração: &aPermanente"));
                }
            }

            // Mostrar todos os grupos
            List<String> allGroups = groupManager.getPlayerGroupNames(targetUUID);
            sender.sendMessage(ChatStorage.colorize("&7Grupos atuais: &e" + String.join("&7, &e", allGroups)));

            // Staff broadcast
            String action = alreadyHas ? (wasPermanent ? "tentou renovar (já permanente)" : "acumulou tempo no")
                    : "adicionou";
            broadcastToStaff(adminName, playerName + " &7(offline)", groupDisplayWithColor, action, sender);

        } else {
            ChatStorage.send(sender, "group.player-never-joined", "player", playerName);
        }
    }

    /**
     * Remove um grupo ESPECÍFICO de um jogador
     */
    private void removePlayerGroup(CommandSender sender, String playerName, String groupName) {
        String adminName = sender instanceof Player ? ((Player) sender).getName() : "CONSOLE";

        // Não permitir remover "membro" (é o padrão)
        if (groupName.equalsIgnoreCase("membro")) {
            sender.sendMessage(
                    ChatStorage.colorize("&c&lERRO: &cO grupo &7Membro&c não pode ser removido (é o padrão)."));
            return;
        }

        // Verificar se o grupo existe
        if (!groupManager.groupExists(groupName)) {
            ChatStorage.send(sender, "group.not-found", "group", groupName);
            return;
        }

        Group group = groupManager.getGroup(groupName);
        String groupDisplayWithColor = getGroupDisplayWithColor(group);

        // Tentar encontrar jogador online
        Player targetPlayer = Bukkit.getPlayer(playerName);

        if (targetPlayer != null) {
            UUID targetUUID = targetPlayer.getUniqueId();

            // Verificar se possui o grupo
            if (!groupManager.hasGroup(targetUUID, groupName)) {
                sender.sendMessage(ChatStorage.colorize("&6&lHAUMEAGROUPS&f O jogador &e" + playerName +
                        "&f não possui o grupo " + groupDisplayWithColor + "&f."));

                // Mostrar grupos que possui
                List<String> allGroups = groupManager.getPlayerGroupNames(targetUUID);
                sender.sendMessage(ChatStorage.colorize("&7Grupos atuais: &e" + String.join("&7, &e", allGroups)));
                return;
            }

            // Remover grupo específico
            groupManager.removePlayerGroup(targetUUID, groupName);

            if (accountRepository != null) {
                accountRepository.removePlayerGroup(targetUUID, groupName);
            }

            // Atualizar permissões
            permissionManager.setupPermissions(targetPlayer);

            // Feedback admin
            sender.sendMessage(ChatStorage.colorize("&6&lHAUMEAGROUPS&f Grupo " + groupDisplayWithColor +
                    "&f foi &c&lREMOVIDO&f do jogador &e&l" + targetPlayer.getName() + " &f(&7" + targetUUID.toString()
                    + "&f)."));

            // Mostrar grupos restantes
            List<String> allGroups = groupManager.getPlayerGroupNames(targetUUID);
            sender.sendMessage(ChatStorage.colorize("&7Grupos restantes: &e" + String.join("&7, &e", allGroups)));

            // Feedback jogador
            targetPlayer.sendMessage(ChatStorage.colorize("&6&lHAUMEAMC&f O grupo " + groupDisplayWithColor +
                    "&f foi &c&lREMOVIDO&f dos seus grupos."));

            // Sincronizar tag com novo grupo principal
            Group mainGroup = groupManager.getPlayerGroup(targetPlayer);
            if (mainGroup != null) {
                syncPlayerTag(targetPlayer, mainGroup.getName());
            }

            updateTabList(targetPlayer);
            broadcastToStaff(adminName, targetPlayer.getName(), groupDisplayWithColor, "removeu", sender);

        } else {
            // Jogador offline
            @SuppressWarnings("deprecation")
            org.bukkit.OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);

            if (offlinePlayer.hasPlayedBefore()) {
                UUID targetUUID = offlinePlayer.getUniqueId();

                // Verificar se possui o grupo
                if (!groupManager.hasGroup(targetUUID, groupName)) {
                    sender.sendMessage(ChatStorage.colorize("&6&lHAUMEAGROUPS&f O jogador &e" + playerName +
                            "&f não possui o grupo " + groupDisplayWithColor + "&f."));

                    List<String> allGroups = groupManager.getPlayerGroupNames(targetUUID);
                    sender.sendMessage(ChatStorage.colorize("&7Grupos atuais: &e" + String.join("&7, &e", allGroups)));
                    return;
                }

                groupManager.removePlayerGroup(targetUUID, groupName);

                if (accountRepository != null) {
                    accountRepository.removePlayerGroup(targetUUID, groupName);
                }

                sender.sendMessage(ChatStorage.colorize("&6&lHAUMEAGROUPS&f Grupo " + groupDisplayWithColor +
                        "&f foi &c&lREMOVIDO&f do jogador offline &e&l" + playerName + " &f(&7" + targetUUID.toString()
                        + "&f)."));

                List<String> allGroups = groupManager.getPlayerGroupNames(targetUUID);
                sender.sendMessage(ChatStorage.colorize("&7Grupos restantes: &e" + String.join("&7, &e", allGroups)));

            } else {
                ChatStorage.send(sender, "group.player-never-joined", "player", playerName);
            }
        }
    }

    /**
     * Mostra informações de TODOS os grupos do jogador
     */
    private void showPlayerInfo(CommandSender sender, String playerName) {
        Player targetPlayer = Bukkit.getPlayer(playerName);
        UUID targetUUID = null;
        String status = "&aOnline";

        if (targetPlayer != null) {
            targetUUID = targetPlayer.getUniqueId();
        } else {
            @SuppressWarnings("deprecation")
            org.bukkit.OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
            if (offlinePlayer.hasPlayedBefore()) {
                targetUUID = offlinePlayer.getUniqueId();
                status = "&cOffline";
            }
        }

        if (targetUUID == null) {
            ChatStorage.send(sender, "group.player-never-joined", "player", playerName);
            return;
        }

        // Obter dados
        Group mainGroup = groupManager.getPlayerGroup(targetUUID);
        String mainGroupDisplay = getGroupDisplayWithColor(mainGroup);
        List<String> allGroupNames = groupManager.getPlayerGroupNames(targetUUID);

        String firstJoin = "N/A";
        String lastSeen = "N/A";

        if (accountRepository != null) {
            AccountData data = accountRepository.getAccount(targetUUID);
            if (data != null) {
                if (data.getFirstJoin() > 0) {
                    firstJoin = DATE_FORMAT.format(new Date(data.getFirstJoin()));
                }
                if (data.getLastSeen() > 0) {
                    lastSeen = DATE_FORMAT.format(new Date(data.getLastSeen()));
                }
            }
        }

        // Exibir informações
        sender.sendMessage(ChatStorage.getSeparator());
        sender.sendMessage(ChatStorage.colorize("&6&lHAUMEAGROUPS &8- &fInformações do Jogador"));
        sender.sendMessage(ChatStorage.getSeparator());
        sender.sendMessage(ChatStorage.colorize("&7Jogador: &f" + playerName + " " + status));
        sender.sendMessage(ChatStorage.colorize("&7UUID: &8" + targetUUID.toString()));
        sender.sendMessage(ChatStorage.colorize("&7Grupo Principal: " + mainGroupDisplay + " &7(maior prioridade)"));
        sender.sendMessage("");
        sender.sendMessage(ChatStorage.colorize("&e&lTODOS OS GRUPOS:"));

        // Listar cada grupo com sua expiração
        for (String groupName : allGroupNames) {
            Group grp = groupManager.getGroup(groupName);
            String grpDisplay = grp != null ? getGroupDisplayWithColor(grp) : "&7" + groupName;

            long timeRemaining = groupManager.getTimeRemaining(targetUUID, groupName);
            String timeInfo;
            if (timeRemaining == 0) {
                timeInfo = "&a(Permanente)";
            } else if (timeRemaining > 0) {
                timeInfo = "&e(" + formatTime(timeRemaining) + ")";
            } else {
                timeInfo = "&c(Expirado)";
            }

            sender.sendMessage(ChatStorage.colorize("  &7• " + grpDisplay + " " + timeInfo));
        }

        sender.sendMessage("");
        sender.sendMessage(ChatStorage.colorize("&7Primeiro login: &f" + firstJoin));
        sender.sendMessage(ChatStorage.colorize("&7Último acesso: &f" + lastSeen));
        sender.sendMessage(ChatStorage.getSeparator());
    }

    // ==================== MÉTODOS AUXILIARES ====================

    private boolean hasPermission(CommandSender sender) {
        if (!(sender instanceof Player)) {
            return true;
        }

        Player player = (Player) sender;
        Group playerGroup = groupManager.getPlayerGroup(player);

        if (playerGroup == null)
            return false;

        for (String allowed : ALLOWED_GROUPS) {
            if (playerGroup.getName().equalsIgnoreCase(allowed)) {
                return true;
            }
        }

        return false;
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage("");
        sender.sendMessage(ChatStorage.colorize("&6&l  HAUMEA GROUPS &8- &7Gerenciador de Múltiplos Grupos"));
        sender.sendMessage("");
        sender.sendMessage(ChatStorage.colorize("&e  Uso Principal:"));
        sender.sendMessage(ChatStorage.colorize("    &f/" + label + " <jogador> <operação> <grupo> [tempo]"));
        sender.sendMessage("");
        sender.sendMessage(ChatStorage.colorize("&6  Operações:"));
        sender.sendMessage(
                ChatStorage.colorize("    &a• add &8- &7Adiciona um grupo ao jogador &8(aceita múltiplos!)"));
        sender.sendMessage(ChatStorage.colorize("    &e• set &8- &7Define/adiciona um grupo ao jogador"));
        sender.sendMessage(ChatStorage.colorize("    &c• remove &8- &7Remove um grupo específico do jogador"));
        sender.sendMessage("");
        sender.sendMessage(ChatStorage.colorize("&6  Outros Comandos:"));
        sender.sendMessage(ChatStorage.colorize("    &7• &f/" + label + " list &8- &7Lista todos os grupos"));
        sender.sendMessage(
                ChatStorage.colorize("    &7• &f/" + label + " info <jogador> &8- &7Mostra grupos do jogador"));
        sender.sendMessage(ChatStorage.colorize("    &7• &f/" + label + " reload &8- &7Recarrega grupos e storage"));
        sender.sendMessage("");
        for (String msg : ChatStorage.getMessageList("group.help.formats")) {
            sender.sendMessage(ChatStorage.colorize(msg));
        }
        sender.sendMessage("");
        sender.sendMessage(ChatStorage.colorize("&6  Exemplo: &f/" + label + " Steve add trial 1mo"));
        sender.sendMessage(ChatStorage.colorize("    &8➥ &7Adiciona o grupo Trial ao Steve por 1 mês"));
        sender.sendMessage("");
    }

    private String getGroupDisplayWithColor(Group group) {
        if (group == null)
            return "&7&lMEMBRO";

        String prefix = group.getPrefix();
        if (prefix != null && !prefix.isEmpty()) {
            String cleanPrefix = prefix.trim();
            if (cleanPrefix.length() > 2) {
                String lastTwo = cleanPrefix.substring(cleanPrefix.length() - 2);
                if (lastTwo.matches("&[0-9a-fk-or]")) {
                    cleanPrefix = cleanPrefix.substring(0, cleanPrefix.length() - 2).trim();
                }
            }
            return cleanPrefix;
        }
        return "&e&l" + group.getName().toUpperCase();
    }

    @SuppressWarnings("deprecation")
    private void sendTitle(Player player, String groupDisplay) {
        String title = ChatStorage.colorize(groupDisplay);
        String subtitle = ChatStorage.colorize("&fGrupo adicionado com sucesso!");
        player.sendTitle(title, subtitle);
    }

    private void syncPlayerTag(Player player, String groupName) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            player.performCommand("tag " + groupName);
        }, 5L);
    }

    private void updateTabList(Player player) {
        // Usar DisplayManager se disponível (fonte unificada)
        if (plugin.getDisplayManager() != null) {
            plugin.getDisplayManager().onGroupChange(player);
            return;
        }

        // Fallback: atualização via TabManager
        if (plugin.getTabManager() != null && plugin.getTabManager().isRunning()) {
            plugin.getTabManager().updatePlayerTeam(player);
            plugin.getTabManager().updateTabList(player);
        }
    }

    private void broadcastToStaff(String adminName, String targetName, String groupDisplay, String action,
            CommandSender excludeSender) {
        String message = ChatStorage.colorize("&e&l[HAUMEA-STAFF] &fO administrador &e&l" + adminName +
                " &f" + action + " o grupo " + groupDisplay + "&f do jogador &e&l" + targetName + "&f.");

        for (Player staffMember : Bukkit.getOnlinePlayers()) {
            Group staffGroup = groupManager.getPlayerGroup(staffMember);
            if (staffGroup != null && isStaffGroup(staffGroup.getName())) {
                if (excludeSender instanceof Player
                        && staffMember.getUniqueId().equals(((Player) excludeSender).getUniqueId())) {
                    continue;
                }
                staffMember.sendMessage(message);
            }
        }

        Bukkit.getConsoleSender().sendMessage(message);
    }

    private boolean isStaffGroup(String groupName) {
        for (String staff : STAFF_GROUPS) {
            if (staff.equalsIgnoreCase(groupName))
                return true;
        }
        return false;
    }

    private boolean isVipGroup(String groupName) {
        String[] vipGroups = { "ultra", "beta", "premium", "light", "ytplus", "yt", "miniyt" };
        for (String vip : vipGroups) {
            if (vip.equalsIgnoreCase(groupName))
                return true;
        }
        return false;
    }

    /**
     * Parseia uma string de tempo para milissegundos.
     * Delega para TimeUtils para centralização.
     * 
     * Formato: 1m (minuto), 1h (hora), 1d (dia), 1w (semana), 1mo (mês), 1y (ano)
     */
    private long parseTime(String timeStr) {
        if (timeStr == null || timeStr.length() < 2) {
            return -1;
        }

        // Delegar completamente para TimeUtils (agora suporta mo e y)
        return com.haumea.kitpvp.utils.TimeUtils.parseTime(timeStr);
    }

    /**
     * Formata tempo em milissegundos para string legível.
     * Delega para TimeUtils para centralização.
     */
    private String formatTime(long millis) {
        return com.haumea.kitpvp.utils.TimeUtils.formatTimeShort(millis);
    }
}
