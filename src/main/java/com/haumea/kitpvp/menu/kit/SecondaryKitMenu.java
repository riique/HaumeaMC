package com.haumea.kitpvp.menu.kit;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.managers.KitManager;
import com.haumea.kitpvp.menu.BaseMenu;
import com.haumea.kitpvp.models.Kit;
import com.haumea.kitpvp.utils.ChatStorage;
import com.haumea.kitpvp.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Menu de seleção de Kit Secundário (54 slots - baú duplo).
 * 
 * Funciona igual ao PrimaryKitMenu, mas para o slot de kit secundário.
 * A verificação de compatibilidade é feita contra o kit primário selecionado.
 * 
 * Layout:
 * - Linhas 0-1 (slots 0-17): Bordas decorativas
 * - Linhas 2-4 (slots 18-44): Kits disponíveis
 * - Linha 5 (slots 45-53): Indicador kit atual, navegação
 * 
 * @author HaumeaMC
 */
public class SecondaryKitMenu extends BaseMenu {

    private static final int[] KIT_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    private static final int SLOT_NONE = 10;
    private static final int SLOT_CURRENT_KIT = 49;
    private static final int SLOT_NEXT_PAGE = 53;

    private final int page;

    /**
     * Construtor do menu de kits secundários
     */
    public SecondaryKitMenu(HaumeaMC plugin, Player player) {
        this(plugin, player, 0);
    }

    public SecondaryKitMenu(HaumeaMC plugin, Player player, int page) {
        super(plugin, player, "&9&lSeletor de Kits Secundários", 54);
        this.page = page;
    }

    @Override
    protected void setupItems() {
        KitManager kitManager = plugin.getKitManager();
        if (kitManager == null)
            return;

        // Preencher bordas com vidro azul
        fillBorders(11); // Azul

        // Decorar cantos
        ItemStack cornerPane = createGlassPane(3, " "); // Ciano
        setItem(0, cornerPane);
        setItem(8, cornerPane);
        setItem(45, cornerPane);

        // Opção "Nenhum" no primeiro slot útil
        ItemStack noneItem = new ItemBuilder(Material.BARRIER)
                .name("§7§lNenhum")
                .lore(
                        "§8▪ §7Clique para remover",
                        "§8▪ §7seu kit secundário.",
                        "",
                        "§eClique para selecionar!")
                .build();

        setItem(SLOT_NONE, noneItem, (p, click) -> {
            String currentKit = kitManager.getSecondaryKit(player);
            if (currentKit == null || currentKit.isEmpty()) {
                ChatStorage.sendCustom(player, "§cVocê já não está usando nenhum kit secundário!");
                playErrorSound();
                return;
            }

            kitManager.setSecondaryKit(player, null);
            ChatStorage.sendCustom(player, "§aKit secundário removido com sucesso!");
            playPlingSound();
            close();
        });

        // Listar kits disponíveis
        List<Kit> availableKits = kitManager.getAvailableKits(player);
        int startIndex = page * (KIT_SLOTS.length - 1);
        int slotIndex = 1;

        for (int i = startIndex; i < availableKits.size() && slotIndex < KIT_SLOTS.length; i++) {
            Kit kit = availableKits.get(i);
            int slot = KIT_SLOTS[slotIndex];

            // Criar item do kit
            ItemStack kitItem = createKitItem(kit);

            setItem(slot, kitItem, (p, click) -> {
                handleKitSelection(kit);
            });

            slotIndex++;
        }

        // Indicador do kit atual equipado (slot 49)
        String currentKit = kitManager.getSecondaryKit(player);
        ItemStack currentKitItem;

        if (currentKit == null || currentKit.isEmpty()) {
            currentKitItem = new ItemBuilder(Material.BARRIER)
                    .name("§9§lKit Secundário Atual")
                    .lore(
                            "",
                            "§7Kit selecionado: §cNenhum",
                            "",
                            "§8Selecione um kit acima!")
                    .build();
        } else {
            Kit kit = kitManager.getKit(currentKit);
            if (kit != null) {
                currentKitItem = new ItemBuilder(kit.getIcon(), 1, kit.getIconData())
                        .name("§9§lKit Secundário Atual")
                        .lore(
                                "",
                                "§7Kit selecionado: " + kit.getDisplayName(),
                                "",
                                "§8Clique em outro kit para trocar!")
                        .build();
            } else {
                currentKitItem = new ItemBuilder(Material.BARRIER)
                        .name("§9§lKit Secundário Atual")
                        .lore(
                                "",
                                "§7Kit selecionado: §cNenhum",
                                "")
                        .build();
            }
        }
        setItem(SLOT_CURRENT_KIT, currentKitItem);

        // Botão de próxima página
        if (availableKits.size() > (page + 1) * (KIT_SLOTS.length - 1)) {
            ItemStack nextPage = new ItemBuilder(Material.ARROW)
                    .name("§ePágina " + (page + 2))
                    .lore("§7Clique para ver", "§7mais kits!")
                    .build();

            setItem(SLOT_NEXT_PAGE, nextPage, (p, click) -> {
                new SecondaryKitMenu(plugin, player, page + 1).open();
                playClickSound();
            });
        }

        // Botão de página anterior
        if (page > 0) {
            ItemStack prevPage = new ItemBuilder(Material.ARROW)
                    .name("§ePágina " + page)
                    .lore("§7Clique para voltar!")
                    .build();

            setItem(45, prevPage, (p, click) -> {
                new SecondaryKitMenu(plugin, player, page - 1).open();
                playClickSound();
            });
        }
    }

    /**
     * Cria o ItemStack para exibir um kit no menu
     */
    private ItemStack createKitItem(Kit kit) {
        KitManager kitManager = plugin.getKitManager();
        String currentPrimaryKit = kitManager.getPrimaryKit(player);
        String currentSecondaryKit = kitManager.getSecondaryKit(player);

        List<String> lore = new ArrayList<>();

        // Descrição do kit
        for (String line : kit.getDescription()) {
            lore.add(ChatStorage.colorize(line));
        }

        lore.add("");

        // Status de compatibilidade (verificar contra kit primário)
        if (!kitManager.areKitsCompatible(kit.getName(), currentPrimaryKit)) {
            lore.add("§c§l⚠ INCOMPATÍVEL");
            lore.add("§cNão combina com seu kit primário!");
            lore.add("");
        }

        // Verificar se é o kit atual
        if (kit.getName().equalsIgnoreCase(currentSecondaryKit)) {
            lore.add("§9§lEQUIPADO!");
        } else {
            lore.add("§eClique para selecionar!");
        }

        return new ItemBuilder(kit.getIcon(), 1, kit.getIconData())
                .name(kit.getDisplayName())
                .lore(lore)
                .build();
    }

    /**
     * Processa a seleção de um kit
     */
    private void handleKitSelection(Kit kit) {
        KitManager kitManager = plugin.getKitManager();
        String currentPrimaryKit = kitManager.getPrimaryKit(player);
        String currentSecondaryKit = kitManager.getSecondaryKit(player);

        // Verificar se já está usando este kit
        if (kit.getName().equalsIgnoreCase(currentSecondaryKit)) {
            ChatStorage.sendCustom(player, "§cVocê já está usando este kit!");
            playErrorSound();
            return;
        }

        // Verificar compatibilidade com o kit primário
        if (!kitManager.areKitsCompatible(kit.getName(), currentPrimaryKit)) {
            ChatStorage.sendCustom(player, "§cEste kit é incompatível com seu kit primário!");
            playErrorSound();
            close();
            return;
        }

        // Selecionar o kit
        kitManager.setSecondaryKit(player, kit.getName());
        ChatStorage.sendCustom(player, "§aVocê selecionou o kit secundário " + kit.getDisplayName() + "§a!");
        playPlingSound();
        close();
    }
}
