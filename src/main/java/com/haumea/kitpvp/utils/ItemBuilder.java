package com.haumea.kitpvp.utils;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * API fluente para construção de ItemStacks com suporte a handlers de
 * interação.
 * 
 * Exemplo de uso:
 * 
 * <pre>
 * ItemStack item = new ItemBuilder(Material.DIAMOND_SWORD)
 *         .name("§6Espada Épica")
 *         .lore("§7Uma espada lendária", "§eDano: §c+50")
 *         .enchant(Enchantment.DAMAGE_ALL, 5)
 *         .unbreakable()
 *         .onClick(player -> player.sendMessage("Clicou!"))
 *         .build();
 * </pre>
 * 
 * @author HaumeaMC
 */
public class ItemBuilder {

    private final ItemStack item;
    private final ItemMeta meta;
    private Consumer<org.bukkit.entity.Player> clickHandler;
    private String actionId;

    /**
     * Cria um ItemBuilder com o material especificado.
     * 
     * @param material Material do item
     */
    public ItemBuilder(Material material) {
        this(material, 1);
    }

    /**
     * Cria um ItemBuilder com o material e quantidade especificados.
     * 
     * @param material Material do item
     * @param amount   Quantidade
     */
    public ItemBuilder(Material material, int amount) {
        this.item = new ItemStack(material, amount);
        this.meta = item.getItemMeta();
    }

    /**
     * Cria um ItemBuilder com material, quantidade e data (durability).
     * 
     * @param material Material do item
     * @param amount   Quantidade
     * @param data     Data value (para variantes como cores de lã)
     */
    public ItemBuilder(Material material, int amount, short data) {
        this.item = new ItemStack(material, amount, data);
        this.meta = item.getItemMeta();
    }

    /**
     * Cria um ItemBuilder a partir de um ItemStack existente.
     * 
     * @param item ItemStack base
     */
    public ItemBuilder(ItemStack item) {
        this.item = item.clone();
        this.meta = this.item.getItemMeta();
    }

    // ==================== PROPRIEDADES BÁSICAS ====================

    /**
     * Define o nome de exibição do item.
     * 
     * @param name Nome com códigos de cor
     * @return Este builder
     */
    public ItemBuilder name(String name) {
        meta.setDisplayName(ChatStorage.colorize(name));
        return this;
    }

    /**
     * Define a quantidade do item.
     * 
     * @param amount Quantidade (1-64)
     * @return Este builder
     */
    public ItemBuilder amount(int amount) {
        item.setAmount(Math.min(Math.max(amount, 1), 64));
        return this;
    }

    /**
     * Define a durabilidade/data do item.
     * 
     * @param durability Valor de durabilidade
     * @return Este builder
     */
    public ItemBuilder durability(short durability) {
        item.setDurability(durability);
        return this;
    }

    // ==================== LORE ====================

    /**
     * Define o lore (descrição) do item.
     * 
     * @param lore Linhas do lore
     * @return Este builder
     */
    public ItemBuilder lore(String... lore) {
        List<String> coloredLore = new ArrayList<>();
        for (String line : lore) {
            coloredLore.add(ChatStorage.colorize(line));
        }
        meta.setLore(coloredLore);
        return this;
    }

    /**
     * Define o lore do item a partir de uma lista.
     * 
     * @param lore Lista de linhas
     * @return Este builder
     */
    public ItemBuilder lore(List<String> lore) {
        List<String> coloredLore = new ArrayList<>();
        for (String line : lore) {
            coloredLore.add(ChatStorage.colorize(line));
        }
        meta.setLore(coloredLore);
        return this;
    }

    /**
     * Adiciona linhas ao lore existente.
     * 
     * @param lines Linhas a adicionar
     * @return Este builder
     */
    public ItemBuilder addLore(String... lines) {
        List<String> currentLore = meta.getLore();
        if (currentLore == null) {
            currentLore = new ArrayList<>();
        }
        for (String line : lines) {
            currentLore.add(ChatStorage.colorize(line));
        }
        meta.setLore(currentLore);
        return this;
    }

    // ==================== ENCANTAMENTOS ====================

    /**
     * Adiciona um encantamento ao item.
     * 
     * @param enchantment Encantamento
     * @param level       Nível do encantamento
     * @return Este builder
     */
    public ItemBuilder enchant(Enchantment enchantment, int level) {
        meta.addEnchant(enchantment, level, true);
        return this;
    }

    /**
     * Adiciona um encantamento visual (brilho) sem efeito.
     * Adiciona DURABILITY 1 e esconde os encantamentos.
     * 
     * @return Este builder
     */
    public ItemBuilder glow() {
        meta.addEnchant(Enchantment.DURABILITY, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        return this;
    }

    /**
     * Adiciona um encantamento visual (brilho) condicionalmente.
     * 
     * @param shouldGlow Se true, adiciona brilho
     * @return Este builder
     */
    public ItemBuilder glow(boolean shouldGlow) {
        if (shouldGlow) {
            return glow();
        }
        return this;
    }

    /**
     * Define o data value (variante) do item.
     * Usado para cores de lã, tipos de madeira, etc.
     * 
     * @param data Data value do item
     * @return Este builder
     */
    public ItemBuilder data(short data) {
        item.setDurability(data);
        return this;
    }

    /**
     * Remove um encantamento do item.
     * 
     * @param enchantment Encantamento a remover
     * @return Este builder
     */
    public ItemBuilder removeEnchant(Enchantment enchantment) {
        meta.removeEnchant(enchantment);
        return this;
    }

    // ==================== FLAGS ====================

    /**
     * Torna o item inquebrável.
     * 
     * @return Este builder
     */
    public ItemBuilder unbreakable() {
        meta.spigot().setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        return this;
    }

    /**
     * Adiciona flags ao item para esconder informações.
     * 
     * @param flags Flags a adicionar
     * @return Este builder
     */
    public ItemBuilder addFlags(ItemFlag... flags) {
        meta.addItemFlags(flags);
        return this;
    }

    /**
     * Esconde todos os atributos, encantamentos e outras informações.
     * 
     * @return Este builder
     */
    public ItemBuilder hideAll() {
        meta.addItemFlags(ItemFlag.values());
        return this;
    }

    // ==================== TIPOS ESPECÍFICOS ====================

    /**
     * Define o dono de um SKULL_ITEM (cabeça de jogador).
     * O item deve ser do tipo SKULL_ITEM com data 3.
     * 
     * @param playerName Nome do jogador
     * @return Este builder
     */
    public ItemBuilder skullOwner(String playerName) {
        if (meta instanceof SkullMeta) {
            ((SkullMeta) meta).setOwner(playerName);
        }
        return this;
    }

    /**
     * Define a cor de armadura de couro.
     * O item deve ser uma peça de armadura de couro.
     * 
     * @param color Cor da armadura
     * @return Este builder
     */
    public ItemBuilder leatherColor(Color color) {
        if (meta instanceof LeatherArmorMeta) {
            ((LeatherArmorMeta) meta).setColor(color);
        }
        return this;
    }

    // ==================== HANDLERS DE INTERAÇÃO ====================

    /**
     * Define o handler de clique para este item.
     * O handler será executado quando o jogador clicar com este item.
     * 
     * IMPORTANTE: Para o handler funcionar, o ArenaItemsListener deve estar
     * registrado.
     * 
     * @param handler Consumer que recebe o jogador que clicou
     * @return Este builder
     */
    public ItemBuilder onClick(Consumer<org.bukkit.entity.Player> handler) {
        this.clickHandler = handler;
        return this;
    }

    /**
     * Define um ID de ação única para este item.
     * Este ID é salvo na metadata do item e usado para identificar
     * qual handler executar quando o jogador clicar.
     * 
     * @param actionId ID único da ação
     * @return Este builder
     */
    public ItemBuilder actionId(String actionId) {
        this.actionId = actionId;
        return this;
    }

    // ==================== BUILD ====================

    /**
     * Constrói o ItemStack final.
     * Se um handler de clique foi definido, o item será registrado
     * no ItemActionHandler para processamento de cliques.
     * 
     * @return ItemStack construído
     */
    public ItemStack build() {
        item.setItemMeta(meta);

        // Se há um handler, registrar no sistema de ações
        if (clickHandler != null || actionId != null) {
            String id = actionId != null ? actionId : generateActionId();

            // Adicionar o ID ao lore (invisível) para identificação
            addHiddenActionId(id);

            // Registrar o handler se houver
            if (clickHandler != null) {
                ItemActionHandler.registerAction(id, clickHandler);
            }
        }

        return item;
    }

    /**
     * Constrói o ItemStack e retorna uma cópia.
     * Útil quando você precisa de múltiplas cópias do mesmo item base.
     * 
     * @return Clone do ItemStack construído
     */
    public ItemStack buildClone() {
        return build().clone();
    }

    // ==================== MÉTODOS AUXILIARES ====================

    /**
     * Gera um ID de ação único baseado no material e nome.
     */
    private String generateActionId() {
        String baseName = meta.hasDisplayName() ? meta.getDisplayName() : item.getType().name();
        return "action_" + baseName.hashCode() + "_" + System.nanoTime();
    }

    /**
     * Adiciona o ID da ação como uma linha invisível no lore.
     * Usa caracteres especiais para tornar a linha invisível mas identificável.
     */
    private void addHiddenActionId(String actionId) {
        List<String> lore = meta.getLore();
        if (lore == null) {
            lore = new ArrayList<>();
        }

        // Prefixo especial para identificar a linha do action ID
        // §k§r§k§r é usado como marcador (caracteres invisíveis seguidos de reset)
        String hiddenLine = "§k§r§k§r" + actionId;
        lore.add(hiddenLine);

        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    /**
     * Extrai o ID de ação de um ItemStack, se existir.
     * 
     * @param item Item para verificar
     * @return ID da ação ou null se não encontrado
     */
    public static String extractActionId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }

        ItemMeta meta = item.getItemMeta();
        if (!meta.hasLore()) {
            return null;
        }

        for (String line : meta.getLore()) {
            if (line.startsWith("§k§r§k§r")) {
                return line.substring(8); // Remove o prefixo marcador
            }
        }

        return null;
    }

    // ==================== FACTORY METHODS ====================

    /**
     * Cria uma cabeça de jogador.
     * 
     * @param playerName Nome do jogador para a textura da cabeça
     * @return ItemBuilder configurado
     */
    public static ItemBuilder playerHead(String playerName) {
        return new ItemBuilder(Material.SKULL_ITEM, 1, (short) 3)
                .skullOwner(playerName);
    }

    /**
     * Cria uma bússola.
     * 
     * @return ItemBuilder com bússola
     */
    public static ItemBuilder compass() {
        return new ItemBuilder(Material.COMPASS);
    }

    /**
     * Cria uma espada de pedra.
     * 
     * @return ItemBuilder com espada de pedra
     */
    public static ItemBuilder stoneSword() {
        return new ItemBuilder(Material.STONE_SWORD);
    }

    /**
     * Cria uma sopa de cogumelo.
     * 
     * @return ItemBuilder com sopa
     */
    public static ItemBuilder mushroomSoup() {
        return new ItemBuilder(Material.MUSHROOM_SOUP);
    }
}
