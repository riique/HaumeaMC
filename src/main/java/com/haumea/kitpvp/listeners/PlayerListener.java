package com.haumea.kitpvp.listeners;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.commands.staff.AdminCommand;
import com.haumea.kitpvp.database.MongoAccountRepository;
import com.haumea.kitpvp.database.MongoAccountRepository.AccountData;
import com.haumea.kitpvp.database.MongoAccountRepository.GroupEntry;
import com.haumea.kitpvp.managers.GroupManager;
import com.haumea.kitpvp.managers.PermissionManager;
import com.haumea.kitpvp.managers.ProfileManager;
import com.haumea.kitpvp.managers.WarpsManager;
import com.haumea.kitpvp.models.Group;
import com.haumea.kitpvp.models.Warp;
import com.haumea.kitpvp.profile.PlayerProfile;
import com.haumea.kitpvp.utils.ChatStorage;
import com.haumea.kitpvp.utils.VisualManager;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.List;

/**
 * Listener para eventos de jogadores
 * 
 * Gerencia perfis, permissões, display names, visibilidade de admins,
 * teleporte ao spawn e integração com MongoAccountRepository
 * 
 * @author HaumeaMC
 */
public class PlayerListener implements Listener {

    private final HaumeaMC plugin;
    private final GroupManager groupManager;
    private final PermissionManager permissionManager;
    private final ProfileManager profileManager;
    private final MongoAccountRepository accountRepository;
    private final WarpsManager warpsManager;

    public PlayerListener(HaumeaMC plugin) {
        this.plugin = plugin;
        this.groupManager = plugin.getGroupManager();
        this.permissionManager = plugin.getPermissionManager();
        this.profileManager = plugin.getProfileManager();
        this.accountRepository = plugin.getMongoAccountRepository();
        this.warpsManager = plugin.getWarpsManager();
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Remover mensagem "player joined the game"
        event.setJoinMessage(null);

        // Registrar no MongoAccountRepository (salvamento assíncrono)
        if (accountRepository != null) {
            accountRepository.onPlayerJoin(player);

            // Sincronizar TODOS os grupos do storage com o GroupManager
            AccountData data = accountRepository.getAccount(player.getUniqueId());
            if (data != null) {
                List<GroupEntry> groups = data.getGroups();

                for (GroupEntry entry : groups) {
                    // Verificar se não expirou
                    if (!entry.isExpired()) {
                        // Adicionar grupo no GroupManager (se não tiver)
                        if (!groupManager.hasGroup(player.getUniqueId(), entry.getGroupName())) {
                            groupManager.addPlayerGroup(player, entry.getGroupName(), entry.getExpiration());
                        }
                    }
                }
            }
        }

        // Carregar perfil do jogador (carrega dados do MongoDB)
        profileManager.loadProfile(player);

        // Aplicar permissões de TODOS os grupos
        permissionManager.setupPermissions(player);

        // Inicializar DisplayManager para este jogador
        if (plugin.getDisplayManager() != null) {
            plugin.getDisplayManager().onPlayerJoin(player);
        } else {
            // Fallback: Atualizar display name manualmente
            Group mainGroup = groupManager.getPlayerGroup(player);
            if (mainGroup != null && !mainGroup.getPrefix().isEmpty()) {
                String prefix = mainGroup.getPrefix().replace("&", "§");
                String displayName = prefix + player.getName();
                player.setDisplayName(displayName);
                player.setPlayerListName(displayName);
            }
        }

        // Sincronizar killstreak com level do Minecraft (apenas no KitPvP)
        if (plugin.getStatsManager() != null) {
            plugin.getStatsManager().syncOnJoin(player);
        }

        // Carregar bounty do jogador do customData
        if (plugin.getBountyManager() != null) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    plugin.getBountyManager().loadPlayerBounty(player);
                }
            }, 10L); // Aguardar carregamento do profile
        }

        // Carregar lista de jogadores ignorados
        if (plugin.getIgnoreManager() != null) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    plugin.getIgnoreManager().loadIgnoredList(player);
                }
            }, 10L);
        }

        // Carregar recompensas de liga já coletadas
        if (plugin.getLeagueManager() != null) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    plugin.getLeagueManager().loadClaimedRewards(player);
                }
            }, 10L);
        }

        // Teleportar jogador ao spawn (com delay para garantir carregamento)
        teleportToSpawn(player);

        // Enviar mensagem de boas-vindas do KitPvP (apenas se não for Lobby)
        // O Lobby tem suas próprias mensagens no LobbyListener
        if (plugin.requiresCombat()) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    sendKitPvPWelcome(player);
                }
            }, 5L);
        }

        // Atualizar visibilidade de admins baseado em hierarquia

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            AdminCommand.updateVisibilityFor(plugin, player);

            // Carregar tag salva do jogador
            PlayerProfile profile = profileManager.getProfile(player);
            if (profile != null) {
                String savedTag = profile.getData().getSelectedTag();
                if (savedTag != null && !savedTag.isEmpty()) {
                    // Aplicar a tag salva silenciosamente
                    if (plugin.getTagManager() != null && plugin.getTagManager().tagExists(savedTag)) {
                        // Verificar se ainda tem permissão
                        com.haumea.kitpvp.models.Tag tag = plugin.getTagManager().getTag(savedTag);
                        if (tag != null && player.hasPermission(tag.getPermission())) {
                            plugin.getTagManager().setPlayerTag(player, savedTag);
                        }
                    }
                }

                // AUTO-ADMIN: Staff (exceto ajudantes) entram automaticamente em modo admin
                // IMPORTANTE: Só ativa se o jogador já foi autenticado!
                if (!isPlayerAuthenticated(player)) {
                    // Jogador não autenticado - agendar retry após autenticação
                    scheduleAutoAdminCheck(player, profile);
                    return;
                }

                Group playerGroup = groupManager.getPlayerGroup(player);
                if (playerGroup != null && player.hasPermission("haumea.admin")) {
                    String groupName = playerGroup.getName().toLowerCase();
                    // Lista de cargos que entram automaticamente em admin (exceto ajudante)
                    boolean isHigherStaff = groupName.equals("dono") || groupName.equals("diretor")
                            || groupName.equals("gerente") || groupName.equals("admin")
                            || groupName.equals("moderador");

                    if (isHigherStaff) {
                        // Verificar preferência de auto-vanish
                        boolean autoVanishEnabled = profile.getData().getCustomData("pref_auto_vanish", true);

                        if (autoVanishEnabled) {
                            // Ativar modo admin automaticamente
                            AdminCommand.enableAdminMode(plugin, player, profile);
                            ChatStorage.send(player, "staff.admin-enter");

                            // Atualizar scoreboard imediatamente
                            if (plugin.getScoreboardManager() != null) {
                                plugin.getScoreboardManager().forceUpdate(player);
                            }
                        }
                    }
                }
            }
        }, 5L);
    }

    /**
     * Verifica se o jogador está autenticado
     * Como o sistema de autenticação foi removido, todos os jogadores são
     * considerados autenticados
     */
    private boolean isPlayerAuthenticated(Player player) {
        return true; // Sem sistema de autenticação - todos os jogadores são autenticados
    }

    /**
     * Agenda verificação de auto-admin após autenticação
     */
    private void scheduleAutoAdminCheck(Player player, PlayerProfile profile) {
        // Verificar novamente após 3 segundos (dando tempo para autenticar)
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline())
                return;

            // Verificar de novo se está autenticado
            if (!isPlayerAuthenticated(player)) {
                // Ainda não autenticado - tentar de novo após mais 5 segundos
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if (!player.isOnline())
                        return;
                    if (!isPlayerAuthenticated(player))
                        return;

                    tryAutoAdmin(player, profile);
                }, 100L);
                return;
            }

            tryAutoAdmin(player, profile);
        }, 60L); // 3 segundos
    }

    /**
     * Tenta ativar auto-admin se o jogador for elegível
     */
    private void tryAutoAdmin(Player player, PlayerProfile profile) {
        if (profile == null) {
            profile = profileManager.getProfile(player);
        }
        if (profile == null)
            return;

        Group playerGroup = groupManager.getPlayerGroup(player);
        if (playerGroup != null && player.hasPermission("haumea.admin")) {
            String groupName = playerGroup.getName().toLowerCase();
            boolean isHigherStaff = groupName.equals("dono") || groupName.equals("diretor")
                    || groupName.equals("gerente") || groupName.equals("admin")
                    || groupName.equals("moderador");

            if (isHigherStaff && !profile.isVanish()) {
                // Verificar preferência de auto-vanish
                boolean autoVanishEnabled = profile.getData().getCustomData("pref_auto_vanish", true);

                if (autoVanishEnabled) {
                    AdminCommand.enableAdminMode(plugin, player, profile);
                    ChatStorage.send(player, "staff.admin-enter");

                    if (plugin.getScoreboardManager() != null) {
                        plugin.getScoreboardManager().forceUpdate(player);
                    }
                }
            }
        }
    }

    /**
     * Teleporta o jogador ao spawn do servidor.
     * Usa delay para garantir que o player está totalmente carregado.
     * 
     * @param player Jogador a teleportar
     */
    private void teleportToSpawn(Player player) {
        // Verificar se o WarpsManager está disponível
        if (warpsManager == null) {
            return;
        }

        // Buscar a warp "spawn"
        Warp spawnWarp = warpsManager.getWarp("spawn");
        if (spawnWarp == null) {
            return; // Spawn não definido, não faz nada
        }

        // Converter para Location
        Location spawnLocation = spawnWarp.toLocation();
        if (spawnLocation == null) {
            return; // Mundo não carregado
        }

        // Teleportar com delay de 1 tick para garantir que o player está pronto
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                player.teleport(spawnLocation);
            }
        }, 1L);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        // Verificar se o WarpsManager está disponível
        if (warpsManager == null) {
            return;
        }

        // Buscar a warp "spawn"
        Warp spawnWarp = warpsManager.getWarp("spawn");
        if (spawnWarp != null) {
            Location spawnLocation = spawnWarp.toLocation();
            if (spawnLocation != null) {
                event.setRespawnLocation(spawnLocation);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // Remover mensagem "player left the game"
        event.setQuitMessage(null);

        // Limpar DisplayManager
        if (plugin.getDisplayManager() != null) {
            plugin.getDisplayManager().onPlayerQuit(player);
        }

        // Limpar VisualManager (ActionBars persistentes)
        VisualManager.cleanupPlayer(player);

        // Atualizar último acesso no MongoAccountRepository
        if (accountRepository != null) {
            accountRepository.onPlayerQuit(player);
        }

        // Remover attachment de permissões
        permissionManager.removePermissions(player);

        // Limpar cooldowns expirados do jogador (otimização de memória)
        if (plugin.getCooldownManager() != null) {
            plugin.getCooldownManager().onPlayerQuit(player);
        }

        // Limpar pedidos de trade pendentes
        if (plugin.getTradeManager() != null) {
            plugin.getTradeManager().onPlayerQuit(player);
        }

        // Limpar cache de desafios diários
        if (plugin.getDailyChallengeManager() != null) {
            plugin.getDailyChallengeManager().onPlayerQuit(player);
        }

        // Salvar e limpar cache de bounties
        if (plugin.getBountyManager() != null) {
            plugin.getBountyManager().onPlayerQuit(player);
        }

        // Salvar e limpar cache de jogadores ignorados
        if (plugin.getIgnoreManager() != null) {
            plugin.getIgnoreManager().unloadPlayer(player);
        }

        // Limpar dados de antiflood
        if (plugin.getAntiFloodManager() != null) {
            plugin.getAntiFloodManager().clearPlayerData(player.getUniqueId());
        }

        // Salvar e limpar cache de recompensas de liga
        if (plugin.getLeagueManager() != null) {
            plugin.getLeagueManager().onPlayerQuit(player);
        }

        // Descarregar perfil (salva dados no MongoDB e remove da memória)
        profileManager.unloadProfile(player);
    }

    /**
     * Envia a mensagem de boas-vindas do KitPvP
     */
    private void sendKitPvPWelcome(Player player) {
        // Mensagens do messages.yml
        ChatStorage.sendRaw(player, ChatStorage.getMessage("kitpvp.welcome.header"));
        ChatStorage.sendRaw(player, ChatStorage.getMessage("kitpvp.welcome.title"));
        ChatStorage.sendRaw(player, ChatStorage.getMessage("kitpvp.welcome.header"));
        ChatStorage.sendRaw(player, ChatStorage.getMessage("kitpvp.welcome.kit-hint"));
        ChatStorage.sendRaw(player, ChatStorage.getMessage("kitpvp.welcome.help-hint"));
        ChatStorage.sendRaw(player, ChatStorage.getMessage("kitpvp.welcome.footer"));

        // Tocar som de boas-vindas
        player.playSound(player.getLocation(), Sound.LEVEL_UP, 1.0f, 1.0f);

        // Enviar Title de boas-vindas
        String title = ChatStorage.getMessage("kitpvp.title.main");
        String subtitle = ChatStorage.getMessage("kitpvp.title.subtitle");
        player.sendTitle(ChatStorage.colorize(title), ChatStorage.colorize(subtitle));
    }
}
