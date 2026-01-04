package com.haumea.kitpvp.menu;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.utils.ChatStorage;
import com.haumea.kitpvp.utils.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Classe base para todos os menus GUI do servidor.
 * 
 * Implementa InventoryHolder para identificar menus customizados
 * e fornece métodos utilitários para criar e gerenciar menus.
 * 
 * Subclasses devem implementar:
 * - {@link #setupItems()} para popular o menu
 * - {@link #onClick(Player, int, ItemStack, ClickType)} para processar cliques
 * 
 * Recursos:
 * - Auto-registro de handlers de clique por slot
 * - Métodos utilitários para criar bordas, painéis, etc.
 * - Sons automáticos de feedback
 * - Integração com o ChatStorage para mensagens
 * 
 * @author HaumeaMC
 */
public abstract class BaseMenu implements InventoryHolder {

    protected final HaumeaMC plugin;
    protected final Player player;
    protected Inventory inventory;
    protected String title;
    protected int size;

    // Map de handlers por slot
    private final Map<Integer, BiConsumer<Player, ClickType>> clickHandlers;

    // Flag para cancelar todos os cliques (padrão: true)
    protected boolean cancelAllClicks = true;

    /**
     * Construtor do BaseMenu
     * 
     * @param plugin Instância do plugin
     * @param player Jogador que está visualizando o menu
     * @param title  Título do menu
     * @param size   Tamanho (múltiplo de 9, máximo 54)
     */
    public BaseMenu(HaumeaMC plugin, Player player, String title, int size) {
        this.plugin = plugin;
        this.player = player;
        this.title = ChatStorage.colorize(title);
        this.size = Math.min(Math.max(size, 9), 54);
        this.clickHandlers = new HashMap<>();
    }

    /**
     * Cria e retorna o inventário.
     * Este método é chamado pelo Bukkit.
     */
    @Override
    public Inventory getInventory() {
        if (inventory == null) {
            inventory = Bukkit.createInventory(this, size, title);
            setupItems();
        }
        return inventory;
    }

    /**
     * Método abstrato para popular o menu com itens.
     * Implementado pelas subclasses.
     */
    protected abstract void setupItems();

    /**
     * Método chamado quando um slot é clicado.
     * Pode ser sobrescrito pelas subclasses para comportamento customizado.
     * 
     * @param player    Jogador que clicou
     * @param slot      Slot clicado
     * @param item      Item no slot (pode ser null)
     * @param clickType Tipo de clique
     */
    public void onClick(Player player, int slot, ItemStack item, ClickType clickType) {
        // Verificar se há handler registrado para este slot
        BiConsumer<Player, ClickType> handler = clickHandlers.get(slot);
        if (handler != null) {
            handler.accept(player, clickType);
        }
    }

    /**
     * Abre o menu para o jogador
     */
    public void open() {
        player.openInventory(getInventory());
    }

    /**
     * Atualiza o menu sem fechá-lo
     */
    public void refresh() {
        setupItems();
        player.updateInventory();
    }

    /**
     * Fecha o menu para o jogador
     */
    public void close() {
        player.closeInventory();
    }

    // ==================== MÉTODOS UTILITÁRIOS ====================

    /**
     * Define um item no inventário
     * 
     * @param slot Slot (0-indexed)
     * @param item Item a colocar
     */
    protected void setItem(int slot, ItemStack item) {
        if (inventory != null && slot >= 0 && slot < inventory.getSize()) {
            inventory.setItem(slot, item);
        }
    }

    /**
     * Define um item com handler de clique
     * 
     * @param slot    Slot
     * @param item    Item
     * @param handler Handler de clique
     */
    protected void setItem(int slot, ItemStack item, BiConsumer<Player, ClickType> handler) {
        setItem(slot, item);
        if (handler != null) {
            clickHandlers.put(slot, handler);
        }
    }

    /**
     * Define um item clicável com ação simples (ignora tipo de clique)
     * 
     * @param slot   Slot
     * @param item   Item
     * @param action Ação ao clicar
     */
    protected void setClickableItem(int slot, ItemStack item, Runnable action) {
        setItem(slot, item, (p, c) -> action.run());
    }

    /**
     * Cria um painel de vidro decorativo
     * 
     * @param color Cor do vidro (0-15)
     * @return ItemStack
     */
    protected ItemStack createGlassPane(int color) {
        return new ItemBuilder(Material.STAINED_GLASS_PANE, 1, (short) color)
                .name(" ")
                .build();
    }

    /**
     * Cria um painel de vidro decorativo com nome
     * 
     * @param color Cor do vidro (0-15)
     * @param name  Nome do painel
     * @return ItemStack
     */
    protected ItemStack createGlassPane(int color, String name) {
        return new ItemBuilder(Material.STAINED_GLASS_PANE, 1, (short) color)
                .name(name)
                .build();
    }

    /**
     * Preenche as bordas do inventário com painéis de vidro
     * 
     * @param color Cor do vidro
     */
    protected void fillBorders(int color) {
        ItemStack pane = createGlassPane(color);

        for (int i = 0; i < size; i++) {
            // Primeira e última linha
            if (i < 9 || i >= size - 9) {
                setItem(i, pane);
                continue;
            }
            // Bordas laterais
            if (i % 9 == 0 || i % 9 == 8) {
                setItem(i, pane);
            }
        }
    }

    /**
     * Preenche todos os slots vazios com painéis de vidro
     * 
     * @param color Cor do vidro
     */
    protected void fillEmpty(int color) {
        ItemStack pane = createGlassPane(color);

        for (int i = 0; i < size; i++) {
            if (inventory.getItem(i) == null) {
                setItem(i, pane);
            }
        }
    }

    /**
     * Preenche uma linha inteira com painéis de vidro
     * 
     * @param row   Linha (0-indexed)
     * @param color Cor do vidro
     */
    protected void fillRow(int row, int color) {
        ItemStack pane = createGlassPane(color);
        int start = row * 9;
        for (int i = start; i < start + 9 && i < size; i++) {
            setItem(i, pane);
        }
    }

    /**
     * Cria um botão de voltar
     * 
     * @return ItemStack do botão voltar
     */
    protected ItemStack createBackButton() {
        return new ItemBuilder(Material.ARROW)
                .name("§cVoltar")
                .lore("§7Clique para voltar", "§7ao menu anterior.")
                .build();
    }

    /**
     * Cria um botão de fechar
     * 
     * @return ItemStack do botão fechar
     */
    protected ItemStack createCloseButton() {
        return new ItemBuilder(Material.BARRIER)
                .name("§c§lFechar")
                .lore("§7Clique para fechar", "§7este menu.")
                .build();
    }

    // ==================== SONS DE FEEDBACK ====================

    /**
     * Toca som de sucesso
     */
    protected void playSuccessSound() {
        player.playSound(player.getLocation(), Sound.ORB_PICKUP, 1.0f, 1.5f);
    }

    /**
     * Toca som de erro
     */
    protected void playErrorSound() {
        player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1.0f, 1.0f);
    }

    /**
     * Toca som de clique
     */
    protected void playClickSound() {
        player.playSound(player.getLocation(), Sound.CLICK, 0.5f, 1.0f);
    }

    /**
     * Toca som de pling (sucesso maior)
     */
    protected void playPlingSound() {
        player.playSound(player.getLocation(), Sound.NOTE_PLING, 1.0f, 2.0f);
    }

    /**
     * Toca som de level up
     */
    protected void playLevelUpSound() {
        player.playSound(player.getLocation(), Sound.LEVEL_UP, 0.7f, 1.5f);
    }

    // ==================== GETTERS ====================

    public Player getPlayer() {
        return player;
    }

    public HaumeaMC getPlugin() {
        return plugin;
    }

    public String getTitle() {
        return title;
    }

    public int getSize() {
        return size;
    }

    public boolean isCancelAllClicks() {
        return cancelAllClicks;
    }
}
