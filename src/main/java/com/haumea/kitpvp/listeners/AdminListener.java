package com.haumea.kitpvp.listeners;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.commands.report.ReportMenuGUI;
import com.haumea.kitpvp.managers.GroupManager;
import com.haumea.kitpvp.models.Group;
import com.haumea.kitpvp.profile.PlayerProfile;
import com.haumea.kitpvp.utils.ChatStorage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Listener para funcionalidades do modo Admin.
 * Gerencia cliques nos itens de inspeção e atualizações de visibilidade.
 * 
 * @author HaumeaMC
 */
public class AdminListener implements Listener {

    private final HaumeaMC plugin;
    private final GroupManager groupManager;

    // Sistema de CPS Checker
    // Mapa: UUID do jogador sendo monitorado -> dados do monitoramento
    private final Map<UUID, CPSTracker> cpsTrackers = new HashMap<>();

    public AdminListener(HaumeaMC plugin) {
        this.plugin = plugin;
        this.groupManager = plugin.getGroupManager();
    }

    /**
     * Detecta cliques com itens na mão (ação no ar ou bloco)
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        PlayerProfile profile = plugin.getProfileManager().getProfile(player);

        // Só processa se está em modo admin
        if (profile == null || !profile.isVanish()) {
            return;
        }

        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta()) {
            return;
        }

        // Verificar se é clique direito
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        String displayName = meta.getDisplayName();

        if (displayName == null) {
            return;
        }

        // Item REPORTS (Paper - slot 1)
        if (displayName.contains("REPORTS")) {
            event.setCancelled(true);
            ReportMenuGUI.openMenu(plugin, player, 1);
        }
    }

    /**
     * Detecta cliques em entidades (jogadores)
     */
    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        PlayerProfile profile = plugin.getProfileManager().getProfile(player);

        // Só processa se está em modo admin
        if (profile == null || !profile.isVanish()) {
            return;
        }

        // Verificar se clicou em um jogador
        if (!(event.getRightClicked() instanceof Player)) {
            return;
        }

        Player target = (Player) event.getRightClicked();
        ItemStack item = player.getItemInHand();

        if (item == null || !item.hasItemMeta()) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        String displayName = meta.getDisplayName();

        if (displayName == null) {
            return;
        }

        // Item INFORMAÇÕES (Book - slot 0)
        if (displayName.contains("INFORMAÇÕES")) {
            event.setCancelled(true);
            showPlayerInfo(player, target);
        }

        // Item INVENTÁRIO (Watch - slot 2)
        else if (displayName.contains("INVENTÁRIO")) {
            event.setCancelled(true);
            openPlayerInventory(player, target);
        }

        // Item CPS CHECKER (Fishing Rod - slot 3)
        else if (displayName.contains("CPS CHECKER")) {
            event.setCancelled(true);
            startCPSCheck(player, target);
        }
    }

    // ==================== CPS CHECKER SYSTEM ====================

    /**
     * Inicia o monitoramento de CPS de um jogador
     */
    private void startCPSCheck(Player staff, Player target) {
        UUID targetUUID = target.getUniqueId();

        // Verificar se o jogador já está sendo monitorado
        if (cpsTrackers.containsKey(targetUUID)) {
            ChatStorage.send(staff, "admin.cps.already-monitoring");
            return;
        }

        // Criar tracker
        CPSTracker tracker = new CPSTracker(staff.getUniqueId(), target.getUniqueId());
        cpsTrackers.put(targetUUID, tracker);

        // Mensagem inicial para o staff
        List<String> lines = ChatStorage.getMessageList("admin.cps.started");
        for (String line : lines) {
            ChatStorage.sendRaw(staff, line.replace("{player}", target.getName()));
        }

        // Mostrar countdown via ActionBar
        tracker.countdownTask = new BukkitRunnable() {
            int remaining = 10;

            @Override
            public void run() {
                if (remaining <= 0 || !cpsTrackers.containsKey(targetUUID)) {
                    this.cancel();
                    return;
                }

                Player staffPlayer = Bukkit.getPlayer(staff.getUniqueId());
                if (staffPlayer != null && staffPlayer.isOnline()) {
                    String bar = createProgressBar(remaining, 10);
                    String actionBarMsg = ChatStorage.getMessage("admin.cps.action-bar")
                            .replace("{player}", target.getName())
                            .replace("{bar}", bar)
                            .replace("{time}", String.valueOf(remaining));
                    sendActionBar(staffPlayer, actionBarMsg);
                }
                remaining--;
            }
        }.runTaskTimer(plugin, 0L, 20L);

        // Agendar finalização após 10 segundos
        tracker.finishTask = new BukkitRunnable() {
            @Override
            public void run() {
                finishCPSCheck(targetUUID);
            }
        }.runTaskLater(plugin, 200L); // 10 segundos = 200 ticks
    }

    /**
     * Conta cliques do jogador (left click = ataque)
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) {
            return;
        }

        Player attacker = (Player) event.getDamager();
        UUID attackerUUID = attacker.getUniqueId();

        // Verificar se está sendo monitorado
        CPSTracker tracker = cpsTrackers.get(attackerUUID);
        if (tracker != null) {
            tracker.clicks++;
        }
    }

    /**
     * Conta cliques do jogador (interações gerais)
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        // Verificar se está sendo monitorado
        CPSTracker tracker = cpsTrackers.get(playerUUID);
        if (tracker != null) {
            // Contar cliques esquerdo e direito
            if (event.getAction() == Action.LEFT_CLICK_AIR ||
                    event.getAction() == Action.LEFT_CLICK_BLOCK ||
                    event.getAction() == Action.RIGHT_CLICK_AIR ||
                    event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                tracker.clicks++;
            }
        }
    }

    /**
     * Finaliza o monitoramento e mostra os resultados
     */
    private void finishCPSCheck(UUID targetUUID) {
        CPSTracker tracker = cpsTrackers.remove(targetUUID);
        if (tracker == null) {
            return;
        }

        // Cancelar tasks
        if (tracker.countdownTask != null) {
            tracker.countdownTask.cancel();
        }

        Player staff = Bukkit.getPlayer(tracker.staffUUID);
        Player target = Bukkit.getPlayer(targetUUID);

        if (staff == null || !staff.isOnline()) {
            return;
        }

        String targetName = target != null ? target.getName() : "Jogador Offline";
        int totalClicks = tracker.clicks;
        double cps = totalClicks / 10.0;

        // Determinar status baseado no CPS
        String status;
        String statusColor;
        if (cps <= 8) {
            status = "NORMAL";
            statusColor = "&a";
        } else if (cps <= 12) {
            status = "SUSPEITO";
            statusColor = "&e";
        } else if (cps <= 16) {
            status = "MUITO ALTO";
            statusColor = "&c";
        } else {
            status = "POSSÍVEL AUTO-CLICKER";
            statusColor = "&4&l";
        }

        // Criar barra visual do CPS
        String cpsBar = createCPSBar(cps);

        // Limpar ActionBar
        sendActionBar(staff, "");

        // Mostrar resultados
        List<String> lines = ChatStorage.getMessageList("admin.cps.result");
        for (String line : lines) {
            ChatStorage.sendRaw(staff, line
                    .replace("{player}", targetName)
                    .replace("{clicks}", String.valueOf(totalClicks))
                    .replace("{cps}", String.format("%.1f", cps))
                    .replace("{bar}", cpsBar)
                    .replace("{status}", statusColor + status));
        }
    }

    /**
     * Remove tracker quando jogador sai
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();

        // Se era o alvo do monitoramento
        CPSTracker tracker = cpsTrackers.remove(uuid);
        if (tracker != null) {
            if (tracker.countdownTask != null) {
                tracker.countdownTask.cancel();
            }
            if (tracker.finishTask != null) {
                tracker.finishTask.cancel();
            }

            Player staff = Bukkit.getPlayer(tracker.staffUUID);
            if (staff != null && staff.isOnline()) {
                ChatStorage.send(staff, "admin.cps.quit");
                sendActionBar(staff, "");
            }
        }
    }

    /**
     * Cria barra de progresso visual
     */
    private String createProgressBar(int current, int max) {
        StringBuilder bar = new StringBuilder("&8[");
        int filled = (int) ((current / (double) max) * 10);
        for (int i = 0; i < 10; i++) {
            if (i < filled) {
                bar.append("&c█");
            } else {
                bar.append("&7█");
            }
        }
        bar.append("&8]");
        return ChatStorage.colorize(bar.toString());
    }

    /**
     * Cria barra visual de CPS com cores graduais
     */
    private String createCPSBar(double cps) {
        StringBuilder bar = new StringBuilder("&8[");
        int filled = Math.min((int) cps, 20);

        for (int i = 0; i < 20; i++) {
            if (i < filled) {
                // Cores baseadas no nível
                if (i < 8) {
                    bar.append("&a▌"); // Verde (normal)
                } else if (i < 12) {
                    bar.append("&e▌"); // Amarelo (suspeito)
                } else if (i < 16) {
                    bar.append("&c▌"); // Vermelho (muito alto)
                } else {
                    bar.append("&4▌"); // Vermelho escuro (auto-clicker)
                }
            } else {
                bar.append("&8▌");
            }
        }
        bar.append("&8]");
        return ChatStorage.colorize(bar.toString());
    }

    /**
     * Envia ActionBar para o jogador (1.8 compatible)
     */
    private void sendActionBar(Player player, String message) {
        try {
            Object chatComponent = Class.forName(
                    "net.minecraft.server.v1_8_R3.IChatBaseComponent$ChatSerializer")
                    .getMethod("a", String.class)
                    .invoke(null, "{\"text\":\"" + message + "\"}");

            Object packetPlayOutChat = Class.forName(
                    "net.minecraft.server.v1_8_R3.PacketPlayOutChat")
                    .getConstructor(
                            Class.forName("net.minecraft.server.v1_8_R3.IChatBaseComponent"),
                            byte.class)
                    .newInstance(chatComponent, (byte) 2);

            Object handle = player.getClass().getMethod("getHandle").invoke(player);
            Object playerConnection = handle.getClass().getField("playerConnection").get(handle);
            playerConnection.getClass().getMethod("sendPacket",
                    Class.forName("net.minecraft.server.v1_8_R3.Packet"))
                    .invoke(playerConnection, packetPlayOutChat);
        } catch (Exception e) {
            // Fallback silencioso
        }
    }

    /**
     * Classe interna para rastrear CPS
     */
    private static class CPSTracker {
        final UUID staffUUID;
        final UUID targetUUID;
        int clicks = 0;
        BukkitTask countdownTask;
        BukkitTask finishTask;

        CPSTracker(UUID staffUUID, UUID targetUUID) {
            this.staffUUID = staffUUID;
            this.targetUUID = targetUUID;
        }
    }

    // ==================== OUTROS MÉTODOS ====================

    /**
     * Impede modificação de inventário visualizado
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player))
            return;

        Player player = (Player) event.getWhoClicked();
        String title = event.getInventory().getTitle();

        // Se é um inventário de inspeção, impedir modificações
        if (title != null && title.startsWith("§6Inventário de ")) {
            event.setCancelled(true);
        }
    }

    /**
     * Mostra informações do jogador alvo usando messages.yml
     */
    private void showPlayerInfo(Player staff, Player target) {
        PlayerProfile targetProfile = plugin.getProfileManager().getProfile(target);
        Group targetGroup = groupManager.getPlayerGroup(target);
        String groupName = targetGroup != null ? targetGroup.getDisplayName() : "Membro";

        staff.sendMessage(ChatStorage.getMessage("admin.info.header"));
        staff.sendMessage(ChatStorage.getMessage("admin.info.title", "player", target.getName()));
        staff.sendMessage(ChatStorage.getMessage("admin.info.group", "group", groupName));
        staff.sendMessage(
                ChatStorage.getMessage("admin.info.health", "health", String.format("%.1f", target.getHealth())));
        staff.sendMessage(ChatStorage.getMessage("admin.info.ping", "ping", String.valueOf(getPing(target))));

        if (targetProfile != null) {
            staff.sendMessage(
                    ChatStorage.getMessage("admin.info.kills", "kills", String.valueOf(targetProfile.getKills())));
            staff.sendMessage(
                    ChatStorage.getMessage("admin.info.deaths", "deaths", String.valueOf(targetProfile.getDeaths())));
            staff.sendMessage(
                    ChatStorage.getMessage("admin.info.kdr", "kdr", String.format("%.2f", targetProfile.getKDR())));
            staff.sendMessage(ChatStorage.getMessage("admin.info.coins", "coins",
                    ChatStorage.formatNumber(targetProfile.getCoins())));

            String combatStatus = targetProfile.isInCombat() ? "&cSim" : "&aNão";
            String frozenStatus = targetProfile.isFrozen() ? "&cSim" : "&aNão";
            staff.sendMessage(
                    ChatStorage.getMessage("admin.info.combat", "status", ChatStorage.colorize(combatStatus)));
            staff.sendMessage(
                    ChatStorage.getMessage("admin.info.frozen", "status", ChatStorage.colorize(frozenStatus)));
        }
        staff.sendMessage(ChatStorage.getMessage("admin.info.footer"));
    }

    /**
     * Abre o inventário do jogador para visualização
     */
    private void openPlayerInventory(Player staff, Player target) {
        String title = ChatStorage.getMessage("admin.inventory-title", "player", target.getName());
        Inventory view = Bukkit.createInventory(null, 45, title);

        // Inventário principal (slots 0-35)
        ItemStack[] contents = target.getInventory().getContents();
        for (int i = 0; i < contents.length && i < 36; i++) {
            view.setItem(i, contents[i]);
        }

        // Armadura (slots 36-39)
        ItemStack[] armor = target.getInventory().getArmorContents();
        view.setItem(36, armor[3]); // Capacete
        view.setItem(37, armor[2]); // Peitoral
        view.setItem(38, armor[1]); // Calças
        view.setItem(39, armor[0]); // Botas

        // Informações adicionais (slot 44)
        ItemStack info = new ItemStack(org.bukkit.Material.PAPER);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.setDisplayName(ChatStorage.colorize("&e&lINFO"));
        infoMeta.setLore(java.util.Arrays.asList(
                ChatStorage.colorize("&7Vida: &c" + String.format("%.1f", target.getHealth()) + "/20"),
                ChatStorage.colorize("&7Fome: &a" + target.getFoodLevel() + "/20"),
                ChatStorage.colorize("&7XP Level: &b" + target.getLevel())));
        info.setItemMeta(infoMeta);
        view.setItem(44, info);

        staff.openInventory(view);
    }

    /**
     * Obtém o ping do jogador (reflexão para 1.8)
     */
    private int getPing(Player player) {
        try {
            Object entityPlayer = player.getClass().getMethod("getHandle").invoke(player);
            return (int) entityPlayer.getClass().getField("ping").get(entityPlayer);
        } catch (Exception e) {
            return 0;
        }
    }
}
