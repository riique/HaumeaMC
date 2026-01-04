package com.haumea.kitpvp.abilities;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.utils.ChatStorage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * Classe base abstrata para todas as habilidades (kits) do servidor.
 * 
 * Cada habilidade estende esta classe e implementa Listener para
 * capturar eventos específicos da habilidade.
 * 
 * O nome do kit é extraído automaticamente do nome da classe.
 * 
 * @author HaumeaMC
 */
public abstract class Ability implements Listener {

    protected final HaumeaMC plugin;

    // Metadados do kit
    private final String name;
    private final String description;
    private final AbilityRarity rarity;
    private final Material icon;
    private final ItemStack[] items;
    private final long cooldownSeconds;
    private final int price;
    private final boolean free;

    // Sistema de cooldown por jogador
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    /**
     * Construtor da Ability
     * 
     * @param plugin          Plugin principal
     * @param rarity          Raridade do kit
     * @param icon            Material do ícone
     * @param cooldownSeconds Cooldown em segundos (0 = sem cooldown)
     * @param price           Preço do kit
     */
    protected Ability(HaumeaMC plugin, AbilityRarity rarity, Material icon,
            long cooldownSeconds, int price) {
        this(plugin, rarity, icon, cooldownSeconds, price, null);
    }

    /**
     * Construtor completo da Ability
     * 
     * @param plugin          Plugin principal
     * @param rarity          Raridade do kit
     * @param icon            Material do ícone
     * @param cooldownSeconds Cooldown em segundos (0 = sem cooldown)
     * @param price           Preço do kit
     * @param items           Itens especiais que o kit fornece
     */
    protected Ability(HaumeaMC plugin, AbilityRarity rarity, Material icon,
            long cooldownSeconds, int price, ItemStack[] items) {
        this.plugin = plugin;
        this.name = extractName();
        this.description = "ability." + name.toLowerCase() + ".description";
        this.rarity = rarity;
        this.icon = icon;
        this.items = items != null ? items.clone() : new ItemStack[0];
        this.cooldownSeconds = cooldownSeconds;
        this.price = price;
        this.free = price <= 0;
    }

    /**
     * Extrai o nome do kit do nome da classe.
     * Remove o sufixo "Ability" se existir.
     */
    private String extractName() {
        String className = getClass().getSimpleName();

        // Remover sufixo "Ability" se existir
        if (className.endsWith("Ability")) {
            className = className.substring(0, className.length() - 7);
        }

        return className;
    }

    // ==================== VERIFICAÇÕES ====================

    /**
     * Verifica se o jogador tem esta habilidade equipada.
     * 
     * @param player Jogador a verificar
     * @return true se o jogador tem este kit equipado (primário ou secundário)
     */
    public boolean hasAbility(Player player) {
        if (player == null || !player.isOnline()) {
            return false;
        }

        String primaryKit = plugin.getKitManager().getPrimaryKit(player);
        String secondaryKit = plugin.getKitManager().getSecondaryKit(player);

        String kitName = getName().toLowerCase();

        return kitName.equals(primaryKit != null ? primaryKit.toLowerCase() : null) ||
                kitName.equals(secondaryKit != null ? secondaryKit.toLowerCase() : null);
    }

    /**
     * Verifica se um jogador específico tem uma habilidade específica.
     * Método estático utilitário.
     * 
     * @param player      Jogador a verificar
     * @param abilityName Nome da habilidade
     * @param plugin      Plugin principal
     * @return true se o jogador tem a habilidade
     */
    public static boolean hasAbility(Player player, String abilityName, HaumeaMC plugin) {
        if (player == null || !player.isOnline() || abilityName == null) {
            return false;
        }

        String primaryKit = plugin.getKitManager().getPrimaryKit(player);
        String secondaryKit = plugin.getKitManager().getSecondaryKit(player);

        String kit = abilityName.toLowerCase();

        return kit.equals(primaryKit != null ? primaryKit.toLowerCase() : null) ||
                kit.equals(secondaryKit != null ? secondaryKit.toLowerCase() : null);
    }

    // ==================== SISTEMA DE COOLDOWN ====================

    /**
     * Coloca o jogador em cooldown para esta habilidade.
     * 
     * @param player Jogador a colocar em cooldown
     */
    public void putInCooldown(Player player) {
        if (cooldownSeconds <= 0)
            return;
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
    }

    /**
     * Verifica se o jogador está em cooldown.
     * 
     * @param player Jogador a verificar
     * @return true se está em cooldown
     */
    public boolean isInCooldown(Player player) {
        if (cooldownSeconds <= 0)
            return false;

        Long lastUse = cooldowns.get(player.getUniqueId());
        if (lastUse == null)
            return false;

        long elapsed = System.currentTimeMillis() - lastUse;
        long required = cooldownSeconds * 1000L;

        return elapsed < required;
    }

    /**
     * Obtém o tempo restante de cooldown em segundos.
     * 
     * @param player Jogador
     * @return Segundos restantes (0 se não está em cooldown)
     */
    public int getRemainingCooldown(Player player) {
        if (cooldownSeconds <= 0)
            return 0;

        Long lastUse = cooldowns.get(player.getUniqueId());
        if (lastUse == null)
            return 0;

        long elapsed = System.currentTimeMillis() - lastUse;
        long required = cooldownSeconds * 1000L;
        long remaining = required - elapsed;

        if (remaining <= 0)
            return 0;

        return (int) Math.ceil(remaining / 1000.0);
    }

    /**
     * Envia mensagem de cooldown para o jogador.
     * 
     * @param player Jogador
     */
    public void sendCooldownMessage(Player player) {
        int remaining = getRemainingCooldown(player);
        ChatStorage.send(player, "ability.cooldown",
                "time", String.valueOf(remaining),
                "kit", getName());
    }

    /**
     * Remove o cooldown do jogador.
     * 
     * @param player Jogador
     */
    public void removeCooldown(Player player) {
        cooldowns.remove(player.getUniqueId());
    }

    // ==================== ITENS ====================

    /**
     * Obtém o item principal do kit.
     * Este é o primeiro item do array de itens, se existir.
     * 
     * @return ItemStack principal ou null se não houver
     */
    public ItemStack getMainItem() {
        if (items == null || items.length == 0) {
            return null;
        }
        return items[0].clone();
    }

    /**
     * Verifica se o item é o item principal deste kit.
     * 
     * @param item Item a verificar
     * @return true se é o item principal
     */
    public boolean isMainItem(ItemStack item) {
        ItemStack mainItem = getMainItem();
        if (mainItem == null || item == null) {
            return false;
        }

        // Verificar tipo
        if (item.getType() != mainItem.getType()) {
            return false;
        }

        // Verificar nome se tiver
        if (mainItem.hasItemMeta() && mainItem.getItemMeta().hasDisplayName()) {
            if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
                return false;
            }
            return item.getItemMeta().getDisplayName().equals(mainItem.getItemMeta().getDisplayName());
        }

        return true;
    }

    /**
     * Cria um ItemStack com nome customizado para o kit.
     * 
     * @param material    Material do item
     * @param displayName Nome de exibição
     * @return ItemStack criado
     */
    protected ItemStack createKitItem(Material material, String displayName) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatStorage.colorize(displayName));
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Cria um ItemStack com nome e lore customizados.
     * 
     * @param material    Material do item
     * @param displayName Nome de exibição
     * @param lore        Lista de linhas da lore
     * @return ItemStack criado
     */
    protected ItemStack createKitItem(Material material, String displayName, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatStorage.colorize(displayName));

        List<String> coloredLore = new ArrayList<>();
        for (String line : lore) {
            coloredLore.add(ChatStorage.colorize(line));
        }
        meta.setLore(coloredLore);

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Dá os itens do kit ao jogador.
     * 
     * @param player Jogador
     */
    public void giveItems(Player player) {
        if (items == null || items.length == 0)
            return;

        for (ItemStack item : items) {
            if (item != null) {
                player.getInventory().addItem(item.clone());
            }
        }
        player.updateInventory();
    }

    // ==================== PROTEÇÃO ====================

    /**
     * Verifica se o jogador está na área protegida (spawn).
     * 
     * @param player Jogador
     * @return true se está protegido
     */
    protected boolean isProtected(Player player) {
        if (plugin.getStateManager() != null) {
            return plugin.getStateManager().isProtected(player);
        }
        if (plugin.getDamageManager() != null) {
            return plugin.getDamageManager().isProtectedFromDamage(player);
        }
        return false;
    }

    /**
     * Verifica se o jogador está em modo espectador ou admin.
     * 
     * @param player Jogador
     * @return true se está em modo espectador
     */
    protected boolean isSpectator(Player player) {
        // Verificar GameMode diretamente
        return player.getGameMode() == org.bukkit.GameMode.SPECTATOR ||
                player.getGameMode() == org.bukkit.GameMode.CREATIVE;
    }

    /**
     * Registra combate entre dois jogadores.
     * 
     * @param victim   Vítima
     * @param attacker Atacante
     */
    protected void registerCombat(Player victim, Player attacker) {
        if (plugin.getStateManager() != null) {
            plugin.getStateManager().enterCombat(victim, attacker);
            plugin.getStateManager().enterCombat(attacker, victim);
        }
    }

    // ==================== GETTERS ====================

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public AbilityRarity getRarity() {
        return rarity;
    }

    public Material getIcon() {
        return icon;
    }

    public ItemStack[] getItems() {
        return items != null ? items.clone() : new ItemStack[0];
    }

    public long getCooldownSeconds() {
        return cooldownSeconds;
    }

    public int getPrice() {
        return price;
    }

    public boolean isFree() {
        return free;
    }

    /**
     * Obtém o display name formatado do kit.
     */
    public String getDisplayName() {
        return rarity.getColor() + "§l" + name;
    }

    /**
     * Obtém a descrição da lore para menus.
     */
    public List<String> getLore() {
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(rarity.getLorePrefix());
        lore.add("");

        // Adicionar descrição do ChatStorage se existir
        String desc = ChatStorage.getMessage(description);
        if (!desc.equals(description)) {
            lore.add("§7" + desc);
            lore.add("");
        }

        if (cooldownSeconds > 0) {
            lore.add("§eCooldown: §f" + cooldownSeconds + "s");
        }

        if (!free) {
            lore.add("§ePreço: §f" + price + " coins");
        } else {
            lore.add("§aGratuito");
        }

        return lore;
    }

    @Override
    public String toString() {
        return "Ability{" +
                "name='" + name + '\'' +
                ", rarity=" + rarity +
                ", price=" + price +
                ", cooldown=" + cooldownSeconds + "s" +
                '}';
    }
}
