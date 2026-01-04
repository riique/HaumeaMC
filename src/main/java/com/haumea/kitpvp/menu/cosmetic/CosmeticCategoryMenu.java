package com.haumea.kitpvp.menu.cosmetic;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.managers.CosmeticManager;
import com.haumea.kitpvp.menu.BaseMenu;
import com.haumea.kitpvp.models.cosmetic.Cosmetic;
import com.haumea.kitpvp.models.cosmetic.CosmeticType;
import com.haumea.kitpvp.utils.ChatStorage;
import com.haumea.kitpvp.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.ArrayList;
import java.util.List;

/**
 * Menu de uma categoria específica de cosméticos.
 * Exibe todos os cosméticos disponíveis para compra/seleção.
 * 
 * @author HaumeaMC
 */
public class CosmeticCategoryMenu extends BaseMenu {

    private final CosmeticManager cosmeticManager;
    private final CosmeticType type;
    private final List<Cosmetic> cosmetics;
    private int page;
    private static final int ITEMS_PER_PAGE = 21; // 3 linhas de 7 itens

    public CosmeticCategoryMenu(HaumeaMC plugin, Player player, CosmeticType type) {
        this(plugin, player, type, 0);
    }

    public CosmeticCategoryMenu(HaumeaMC plugin, Player player, CosmeticType type, int page) {
        super(plugin, player, "&8" + type.getColorCode() + "&l" + type.getDisplayName().toUpperCase(), 54);
        this.cosmeticManager = plugin.getCosmeticManager();
        this.type = type;
        this.cosmetics = cosmeticManager.getCosmeticsByType(type);
        this.page = page;
    }

    @Override
    protected void setupItems() {
        // Borda decorativa
        fillBorders(15); // Vidro preto

        // Header
        String selectedId = cosmeticManager.getSelectedCosmetic(player, type);
        setItem(4, new ItemBuilder(Material.NETHER_STAR)
                .name(type.getColorCode() + "&l" + type.getDisplayName().toUpperCase())
                .lore(
                        "",
                        "&7Desbloqueados: &e" + cosmeticManager.getUnlockedCount(player, type) +
                                " &7/ " + cosmetics.size(),
                        "",
                        selectedId != null ? "&aEquipado: " + cosmeticManager.getCosmetic(selectedId).getDisplayName()
                                : "&7Nenhum equipado",
                        "",
                        "&eClique esquerdo &7para equipar",
                        "&eClique direito &7para previa")
                .build());

        // Calcular paginação
        int totalPages = (int) Math.ceil((double) cosmetics.size() / ITEMS_PER_PAGE);
        int startIndex = page * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, cosmetics.size());

        // Slots disponíveis para itens (linhas 2, 3, 4)
        int[] slots = {
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34
        };

        int slotIndex = 0;
        for (int i = startIndex; i < endIndex && slotIndex < slots.length; i++) {
            final Cosmetic cosmetic = cosmetics.get(i);
            final int slot = slots[slotIndex++];

            boolean owned = cosmeticManager.hasUnlocked(player, cosmetic.getId());
            boolean selected = cosmetic.getId().equals(selectedId);

            // Construir lore
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add("&fRaridade: " + cosmetic.getRarity().getColoredName());
            lore.add("");

            // Descrição do cosmético
            for (String line : cosmetic.getDescription()) {
                lore.add(line);
            }
            lore.add("");

            // Status
            if (selected) {
                lore.add("&a&l[EQUIPADO]");
                lore.add("");
                lore.add("&cClique esquerdo para desequipar");
            } else if (owned) {
                lore.add("&a[DESBLOQUEADO]");
                lore.add("");
                lore.add("&eClique esquerdo para equipar");
            } else {
                lore.add("&c[BLOQUEADO]");
                lore.add("");
                lore.add("&fPreco: &e" + ChatStorage.formatNumber(cosmetic.getPrice()) + " coins");
                lore.add("");
                lore.add("&eClique esquerdo para comprar");
            }

            lore.add("&7Clique direito para previa");

            // Criar item
            ItemBuilder builder = new ItemBuilder(owned ? cosmetic.getIcon() : Material.BARRIER)
                    .name(cosmetic.getRarity().getFormattedColor() + cosmetic.getDisplayName())
                    .lore(lore.toArray(new String[0]));

            if (!owned) {
                builder.data((short) 0);
            } else if (selected) {
                builder.glow(true);
            }

            setItem(slot, builder.build(), (p, clickType) -> {
                if (clickType == ClickType.RIGHT) {
                    // Prévia
                    if (owned) {
                        cosmetic.preview(player);
                        ChatStorage.sendRaw(player, "&6&lCOSMETICOS &7Previa de " +
                                cosmetic.getRarity().getFormattedColor() + cosmetic.getDisplayName());
                    } else {
                        ChatStorage.sendRaw(player, "&c&lERRO &fVoce precisa desbloquear para ver a previa!");
                        playErrorSound();
                    }
                } else {
                    // Clique esquerdo = comprar/equipar/desequipar
                    if (selected) {
                        // Desequipar
                        cosmeticManager.deselectCosmetic(player, type);
                        playClickSound();
                        refresh();
                    } else if (owned) {
                        // Equipar
                        cosmeticManager.selectCosmetic(player, cosmetic.getId());
                        playSuccessSound();
                        refresh();
                    } else {
                        // Comprar
                        if (cosmeticManager.purchaseCosmetic(player, cosmetic.getId())) {
                            playLevelUpSound();
                            refresh();
                        } else {
                            playErrorSound();
                        }
                    }
                }
            });
        }

        // ========== NAVEGAÇÃO ==========

        // Página anterior
        if (page > 0) {
            setItem(45, new ItemBuilder(Material.ARROW)
                    .name("&e<< Pagina Anterior")
                    .lore("&7Pagina " + page + "/" + totalPages)
                    .build(),
                    (p, c) -> {
                        new CosmeticCategoryMenu(plugin, player, type, page - 1).open();
                        playClickSound();
                    });
        }

        // Página seguinte
        if (page < totalPages - 1) {
            setItem(53, new ItemBuilder(Material.ARROW)
                    .name("&ePagina Seguinte >>")
                    .lore("&7Pagina " + (page + 2) + "/" + totalPages)
                    .build(),
                    (p, c) -> {
                        new CosmeticCategoryMenu(plugin, player, type, page + 1).open();
                        playClickSound();
                    });
        }

        // Info de página
        if (totalPages > 1) {
            setItem(49, new ItemBuilder(Material.BOOK)
                    .name("&ePagina " + (page + 1) + "/" + totalPages)
                    .lore("&7Total de " + cosmetics.size() + " cosmeticos")
                    .build());
        }

        // Voltar ao menu principal
        setItem(48, new ItemBuilder(Material.ARROW)
                .name("&cVoltar")
                .lore("&7Voltar ao menu principal")
                .build(),
                (p, c) -> {
                    new CosmeticMainMenu(plugin, player).open();
                    playClickSound();
                });

        // Desequipar atual
        if (selectedId != null) {
            setItem(50, new ItemBuilder(Material.REDSTONE_BLOCK)
                    .name("&c&lRESETAR")
                    .lore(
                            "&7Remove o cosmetico equipado",
                            "&7desta categoria.",
                            "",
                            "&eClique para remover!")
                    .build(),
                    (p, c) -> {
                        cosmeticManager.deselectCosmetic(player, type);
                        playClickSound();
                        refresh();
                    });
        }

        // Preencher espaços vazios
        fillEmpty(7); // Vidro cinza
    }
}
