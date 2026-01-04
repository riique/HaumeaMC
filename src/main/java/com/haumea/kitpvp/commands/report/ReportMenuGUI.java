package com.haumea.kitpvp.commands.report;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.managers.ReportManager;
import com.haumea.kitpvp.models.Report;
import com.haumea.kitpvp.utils.ChatStorage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.SkullType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

/**
 * Menu GUI de Reports para Staff do HaumeaMC
 * 
 * Exibe todos os reports abertos em um inventário de 54 slots
 * com cabeças representando cada denúncia.
 * 
 * @author HaumeaMC
 */
public class ReportMenuGUI implements Listener {

    private static final String MENU_TITLE = ChatStorage.getMessage("menu.reports.title");
    private static final int SLOTS_PER_PAGE = 45; // 54 - 9 (linha inferior para navegação)

    // Cache de páginas abertas por jogador
    private static final Map<UUID, Integer> playerPages = new HashMap<>();

    // Cache de reports sendo exibidos por jogador
    private static final Map<UUID, List<Report>> playerReports = new HashMap<>();

    private final HaumeaMC plugin;

    public ReportMenuGUI(HaumeaMC plugin) {
        this.plugin = plugin;
    }

    /**
     * Abre o menu de reports para um jogador
     */
    public static void openMenu(HaumeaMC plugin, Player player, int page) {
        ReportManager reportManager = plugin.getReportManager();
        List<Report> reports = reportManager.getOpenReports();

        if (reports.isEmpty()) {
            ChatStorage.send(player, "menu.reports.empty");
            return;
        }

        // Calcular páginas
        int totalPages = (int) Math.ceil((double) reports.size() / SLOTS_PER_PAGE);
        if (page < 1)
            page = 1;
        if (page > totalPages)
            page = totalPages;

        // Criar inventário
        Inventory inv = Bukkit.createInventory(null, 54, MENU_TITLE + " §7(Página " + page + "/" + totalPages + ")");

        // Calcular índices
        int startIndex = (page - 1) * SLOTS_PER_PAGE;
        int endIndex = Math.min(startIndex + SLOTS_PER_PAGE, reports.size());

        // Adicionar cabeças dos jogadores reportados
        for (int i = startIndex; i < endIndex; i++) {
            Report report = reports.get(i);
            int slot = i - startIndex;

            ItemStack skull = createReportHead(report);
            inv.setItem(slot, skull);
        }

        // Linha inferior - Navegação
        // Página anterior (slot 45)
        if (page > 1) {
            ItemStack prevPage = createNavigationItem(Material.ARROW,
                    ChatStorage.getMessage("menu.reports.items.navigation.prev"), page - 1);
            inv.setItem(45, prevPage);
        }

        // Info central (slot 49)
        ItemStack info = createInfoItem(reports.size(), page, totalPages);
        inv.setItem(49, info);

        // Próxima página (slot 53)
        if (page < totalPages) {
            ItemStack nextPage = createNavigationItem(Material.ARROW,
                    ChatStorage.getMessage("menu.reports.items.navigation.next"), page + 1);
            inv.setItem(53, nextPage);
        }

        // Salvar estado
        playerPages.put(player.getUniqueId(), page);
        playerReports.put(player.getUniqueId(), reports);

        // Abrir inventário
        player.openInventory(inv);
    }

    /**
     * Cria uma cabeça representando um report
     */
    @SuppressWarnings("deprecation")
    private static ItemStack createReportHead(Report report) {
        ItemStack skull = new ItemStack(Material.SKULL_ITEM, 1, (short) SkullType.PLAYER.ordinal());
        SkullMeta meta = (SkullMeta) skull.getItemMeta();

        meta.setOwner(report.getTargetName());
        meta.setDisplayName(ChatStorage.getMessage("menu.reports.items.head.name", "player", report.getTargetName()));

        List<String> rawLore = ChatStorage.getMessageList("menu.reports.items.head.lore");
        List<String> lore = new ArrayList<>();

        for (String line : rawLore) {
            lore.add(line
                    .replace("{id}", report.getId())
                    .replace("{reporter}", report.getReporterName())
                    .replace("{reason}", report.getReason())
                    .replace("{date}", report.getFormattedDate())
                    .replace("{status}", report.getStatus().getFormattedName()));
        }

        meta.setLore(lore);
        skull.setItemMeta(meta);

        return skull;
    }

    /**
     * Cria um item de navegação
     */
    private static ItemStack createNavigationItem(Material material, String name, int targetPage) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);

        List<String> lore = new ArrayList<>();
        lore.add(ChatStorage.getMessage("menu.reports.items.navigation.lore", "page", String.valueOf(targetPage)));
        meta.setLore(lore);

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Cria um item de informações
     */
    private static ItemStack createInfoItem(int totalReports, int currentPage, int totalPages) {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatStorage.getMessage("menu.reports.items.info.name"));

        List<String> rawLore = ChatStorage.getMessageList("menu.reports.items.info.lore");
        List<String> lore = new ArrayList<>();

        for (String line : rawLore) {
            lore.add(line
                    .replace("{total}", String.valueOf(totalReports))
                    .replace("{page}", String.valueOf(currentPage))
                    .replace("{pages}", String.valueOf(totalPages)));
        }
        meta.setLore(lore);

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Evento de clique no inventário
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player))
            return;

        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();

        // Verificar se é o menu de reports
        if (!title.startsWith(MENU_TITLE))
            return;

        event.setCancelled(true);

        int slot = event.getRawSlot();
        if (slot < 0 || slot > 53)
            return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR)
            return;

        Integer currentPage = playerPages.get(player.getUniqueId());
        List<Report> reports = playerReports.get(player.getUniqueId());

        if (currentPage == null || reports == null)
            return;

        ReportManager reportManager = plugin.getReportManager();

        // Navegação
        if (slot == 45 && clicked.getType() == Material.ARROW) {
            // Página anterior
            openMenu(plugin, player, currentPage - 1);
            return;
        }

        if (slot == 53 && clicked.getType() == Material.ARROW) {
            // Próxima página
            openMenu(plugin, player, currentPage + 1);
            return;
        }

        // Clique em report (cabeça)
        if (slot < 45 && clicked.getType() == Material.SKULL_ITEM) {
            int reportIndex = (currentPage - 1) * SLOTS_PER_PAGE + slot;

            if (reportIndex >= reports.size())
                return;

            Report report = reports.get(reportIndex);

            if (event.isLeftClick()) {
                // Teleportar para o jogador
                Player target = Bukkit.getPlayer(report.getTargetUuid());
                if (target != null && target.isOnline()) {
                    player.closeInventory();
                    player.teleport(target.getLocation());
                    ChatStorage.send(player, "menu.reports.teleported", "player", target.getName());
                } else {
                    ChatStorage.send(player, "menu.reports.open-error", "player", report.getTargetName());
                }
            } else if (event.isRightClick()) {
                // Remover report
                boolean removed = reportManager.removeReport(report.getId());
                if (removed) {
                    ChatStorage.send(player, "menu.reports.removed", "player", report.getTargetName());
                    // Atualizar menu
                    player.closeInventory();
                    Bukkit.getScheduler().runTaskLater(plugin, () -> openMenu(plugin, player, currentPage), 2L);
                }
            }
        }
    }

    /**
     * Limpa os dados do jogador ao fechar o inventário
     */
    public static void cleanup(UUID uuid) {
        playerPages.remove(uuid);
        playerReports.remove(uuid);
    }
}
