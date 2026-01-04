package com.haumea.kitpvp.commands.staff;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.commands.base.BaseCommand;
import com.haumea.kitpvp.commands.base.CommandInfo;

import com.haumea.kitpvp.managers.GroupManager;
import com.haumea.kitpvp.models.Group;
import com.haumea.kitpvp.profile.PlayerProfile;
import com.haumea.kitpvp.utils.ChatStorage;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

/**
 * Comando para modo Admin (Vanish com hierarquia).
 * 
 * Sistema de Privacidade por Hierarquia:
 * - Staff em modo admin só é visto por staffs de cargo igual ou superior
 * - Salva estado completo (inventário, armadura, XP, GameMode)
 * - Fornece ferramentas de inspeção
 * 
 * @author HaumeaMC
 */
@CommandInfo(name = "admin", aliases = { "v", "vanish",
        "administrador" }, description = "Entra ou sai do modo admin", playerOnly = true, allowedGroups = { "dono",
                "diretor", "gerente", "admin", "moderador", "ajudante" })
public class AdminCommand extends BaseCommand {

    public AdminCommand(HaumeaMC plugin) {
        super(plugin);
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        Player player = getPlayer();
        PlayerProfile profile = plugin.getProfileManager().getProfile(player);

        if (profile == null) {
            sendRaw("&cErro ao carregar seu perfil!");
            return;
        }

        if (profile.isVanish()) {
            // Desativar modo admin
            disableAdminMode(plugin, player, profile);
            ChatStorage.send(player, "staff.admin-exit");
        } else {
            // Ativar modo admin
            enableAdminMode(plugin, player, profile);
            ChatStorage.send(player, "staff.admin-enter");

            // Notificar apenas staffs de cargo igual ou superior
            notifyHigherStaff(player);
        }
    }

    /**
     * Ativa o modo Admin com invisibilidade hierárquica
     */
    public static void enableAdminMode(HaumeaMC plugin, Player player, PlayerProfile profile) {
        // Salvar estado completo (inventário, armadura, XP, GameMode)
        profile.saveInventory();

        // Aplicar invisibilidade hierárquica
        applyHierarchyVisibility(plugin, player, true);

        // Limpar inventário e colocar em Creative
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.setGameMode(GameMode.CREATIVE);

        // Dar ferramentas de inspeção
        giveInspectionItems(player);

        // Ativar voo e god mode
        player.setAllowFlight(true);
        player.setFlying(true);
        profile.setGodMode(true);
        profile.setVanish(true);
    }

    /**
     * Desativa o modo Admin
     */
    public static void disableAdminMode(HaumeaMC plugin, Player player, PlayerProfile profile) {
        // Mostrar para todos os jogadores
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online != player) {
                online.showPlayer(player);
            }
        }

        // Desativar estados ANTES de mudar inventário
        profile.setGodMode(false);
        profile.setVanish(false);
        player.setAllowFlight(false);
        player.setFlying(false);

        // Limpar inventário completamente e voltar para SURVIVAL
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.setGameMode(GameMode.SURVIVAL);

        // Dar itens de lobby diretamente (evita restaurar inventário potencialmente
        // desatualizado)
        if (plugin.getArenaItemsHandler() != null) {
            plugin.getArenaItemsHandler().giveLobbyItems(player);
        }

        // Limpar inventário salvo para evitar restauração futura incorreta
        profile.clearSavedInventory();

        // Usar DisplayManager para reaplicar display correto (FONTE UNIFICADA)
        if (plugin.getDisplayManager() != null) {
            plugin.getDisplayManager().refreshPlayer(player);
        } else {
            // Fallback: atualização manual
            if (plugin.getTagManager() != null) {
                String savedTag = profile.getData().getSelectedTag();
                if (savedTag != null && !savedTag.isEmpty()) {
                    com.haumea.kitpvp.models.Tag tag = plugin.getTagManager().getTag(savedTag);
                    if (tag != null && player.hasPermission(tag.getPermission())) {
                        String prefix = tag.getPrefix().replace("&", "§");
                        player.setDisplayName(prefix + player.getName());
                        player.setPlayerListName(prefix + player.getName());
                    }
                } else {
                    Group mainGroup = plugin.getGroupManager().getPlayerGroup(player);
                    if (mainGroup != null && !mainGroup.getPrefix().isEmpty()) {
                        String prefix = mainGroup.getPrefix().replace("&", "§");
                        player.setDisplayName(prefix + player.getName());
                        player.setPlayerListName(prefix + player.getName());
                    }
                }
            }

            if (plugin.getTabManager() != null) {
                plugin.getTabManager().updatePlayerTeam(player);
            }
        }

        // Atualizar scoreboard IMEDIATAMENTE para voltar ao título normal
        if (plugin.getScoreboardManager() != null) {
            // Forçar recriação da scoreboard para atualização imediata do título
            plugin.getScoreboardManager().createBoard(player);
        }
    }

    /**
     * Aplica ou remove a invisibilidade baseada em hierarquia.
     * 
     * Lógica: Staff em admin só é visto por staffs de cargo IGUAL ou SUPERIOR.
     * Prioridade maior = cargo mais alto.
     * 
     * @param adminPlayer Jogador que está entrando/saindo do modo admin
     * @param entering    true se está entrando, false se está saindo
     */
    public static void applyHierarchyVisibility(HaumeaMC plugin, Player adminPlayer, boolean entering) {
        GroupManager groupManager = plugin.getGroupManager();
        Group adminGroup = groupManager.getPlayerGroup(adminPlayer);
        int adminPriority = adminGroup != null ? adminGroup.getPriority() : 0;

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online == adminPlayer)
                continue;

            if (entering) {
                // Verificar se o online pode ver o admin
                Group onlineGroup = groupManager.getPlayerGroup(online);
                int onlinePriority = onlineGroup != null ? onlineGroup.getPriority() : 0;

                // Só esconde se:
                // 1. Não é staff (prioridade baixa)
                // 2. É staff de cargo inferior
                if (onlinePriority < adminPriority) {
                    online.hidePlayer(adminPlayer);
                } else {
                    // Staffs de cargo igual ou superior podem ver
                    online.showPlayer(adminPlayer);
                }
            } else {
                // Ao sair, mostrar para todos
                online.showPlayer(adminPlayer);
            }
        }
    }

    /**
     * Notifica staffs de cargo igual ou superior sobre a entrada no modo admin
     */
    private void notifyHigherStaff(Player adminPlayer) {
        GroupManager groupManager = plugin.getGroupManager();
        Group adminGroup = groupManager.getPlayerGroup(adminPlayer);
        int adminPriority = adminGroup != null ? adminGroup.getPriority() : 0;

        String message = ChatStorage.colorize("&8[&6ADMIN&8] &7" + adminPlayer.getName() + " entrou no modo admin.");

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online == adminPlayer)
                continue;

            Group onlineGroup = groupManager.getPlayerGroup(online);
            int onlinePriority = onlineGroup != null ? onlineGroup.getPriority() : 0;

            // Notificar apenas staffs de cargo igual ou superior
            if (onlinePriority >= adminPriority && groupManager.isStaff(online)) {
                online.sendMessage(message);
            }
        }
    }

    /**
     * Entrega itens de inspeção para o staff
     */
    public static void giveInspectionItems(Player player) {
        // Slot 0: Book (Ver informações do jogador)
        ItemStack book = new ItemStack(Material.BOOK);
        ItemMeta bookMeta = book.getItemMeta();
        bookMeta.setDisplayName(ChatStorage.colorize("&b&lINFORMAÇÕES"));
        bookMeta.setLore(Arrays.asList(
                ChatStorage.colorize("&7Clique direito em um jogador"),
                ChatStorage.colorize("&7para ver suas informações.")));
        book.setItemMeta(bookMeta);
        player.getInventory().setItem(0, book);

        // Slot 1: Paper (Ver Reports/Denúncias)
        ItemStack reports = new ItemStack(Material.PAPER);
        ItemMeta reportsMeta = reports.getItemMeta();
        reportsMeta.setDisplayName(ChatStorage.colorize("&3&lREPORTS"));
        reportsMeta.setLore(Arrays.asList(
                ChatStorage.colorize("&7Clique direito para ver"),
                ChatStorage.colorize("&7as denúncias abertas.")));
        reports.setItemMeta(reportsMeta);
        player.getInventory().setItem(1, reports);

        // Slot 2: Watch (Ver inventário do jogador)
        ItemStack watch = new ItemStack(Material.WATCH);
        ItemMeta watchMeta = watch.getItemMeta();
        watchMeta.setDisplayName(ChatStorage.colorize("&6&lINVENTÁRIO"));
        watchMeta.setLore(Arrays.asList(
                ChatStorage.colorize("&7Clique direito em um jogador"),
                ChatStorage.colorize("&7para ver seu inventário.")));
        watch.setItemMeta(watchMeta);
        player.getInventory().setItem(2, watch);

        // Slot 3: Blaze Rod (CPS Checker)
        ItemStack cpsChecker = new ItemStack(Material.BLAZE_ROD);
        ItemMeta cpsMeta = cpsChecker.getItemMeta();
        cpsMeta.setDisplayName(ChatStorage.colorize("&c&lCPS CHECKER"));
        cpsMeta.setLore(Arrays.asList(
                ChatStorage.colorize("&7Clique direito em um jogador"),
                ChatStorage.colorize("&7para monitorar seu CPS"),
                ChatStorage.colorize("&7durante &e10 segundos&7.")));
        cpsChecker.setItemMeta(cpsMeta);
        player.getInventory().setItem(3, cpsChecker);
    }

    /**
     * Método estático para atualizar a visibilidade de todos os admins
     * quando um novo jogador entra ou alguém troca de cargo.
     * 
     * @param plugin Instância do plugin
     * @param viewer O jogador que deve ter sua visão atualizada
     */
    public static void updateVisibilityFor(HaumeaMC plugin, Player viewer) {
        GroupManager groupManager = plugin.getGroupManager();
        Group viewerGroup = groupManager.getPlayerGroup(viewer);
        int viewerPriority = viewerGroup != null ? viewerGroup.getPriority() : 0;

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online == viewer)
                continue;

            PlayerProfile onlineProfile = plugin.getProfileManager().getProfile(online);
            if (onlineProfile != null && onlineProfile.isVanish()) {
                // Este jogador está em modo admin
                Group adminGroup = groupManager.getPlayerGroup(online);
                int adminPriority = adminGroup != null ? adminGroup.getPriority() : 0;

                // Viewer só vê o admin se tem cargo igual ou superior
                if (viewerPriority >= adminPriority) {
                    viewer.showPlayer(online);
                } else {
                    viewer.hidePlayer(online);
                }
            }
        }
    }

    /**
     * Atualiza a visibilidade de um admin específico para todos os jogadores.
     * Usado quando alguém troca de cargo.
     * 
     * @param plugin      Instância do plugin
     * @param adminPlayer O jogador em modo admin
     */
    public static void refreshAdminVisibility(HaumeaMC plugin, Player adminPlayer) {
        PlayerProfile profile = plugin.getProfileManager().getProfile(adminPlayer);
        if (profile == null || !profile.isVanish())
            return;

        GroupManager groupManager = plugin.getGroupManager();
        Group adminGroup = groupManager.getPlayerGroup(adminPlayer);
        int adminPriority = adminGroup != null ? adminGroup.getPriority() : 0;

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online == adminPlayer)
                continue;

            Group onlineGroup = groupManager.getPlayerGroup(online);
            int onlinePriority = onlineGroup != null ? onlineGroup.getPriority() : 0;

            if (onlinePriority >= adminPriority) {
                online.showPlayer(adminPlayer);
            } else {
                online.hidePlayer(adminPlayer);
            }
        }
    }
}
