package com.haumea.kitpvp.menu.kit;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.abilities.Ability;
import com.haumea.kitpvp.abilities.AbilityManager;
import com.haumea.kitpvp.managers.KitManager;
import com.haumea.kitpvp.menu.BaseMenu;
import com.haumea.kitpvp.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Menu de seleção do tipo de kit (Primário ou Secundário).
 * 
 * Layout (27 slots):
 * - Linha 0: Bordas decorativas
 * - Linha 1: Opções de kit primário e secundário + info
 * - Linha 2: Bordas decorativas
 * 
 * @author HaumeaMC
 */
public class KitTypeMenu extends BaseMenu {

    private static final int SLOT_PRIMARY = 11;
    private static final int SLOT_SECONDARY = 13;
    private static final int SLOT_INFO = 15;
    private static final int SLOT_CLOSE = 22;

    public KitTypeMenu(HaumeaMC plugin, Player player) {
        super(plugin, player, "§6§lSELECIONAR KIT", 27);
    }

    @Override
    protected void setupItems() {
        KitManager kitManager = plugin.getKitManager();
        AbilityManager abilityManager = plugin.getAbilityManager();

        // Preencher bordas
        fillBorders(7);

        // Decoração central
        ItemStack goldPane = createGlassPane(4, " "); // Amarelo
        setItem(12, goldPane);
        setItem(14, goldPane);

        // Botão Kit Primário
        String primaryKit = kitManager != null ? kitManager.getPrimaryKit(player) : null;
        ItemStack primaryItem = createKitTypeItem(
                Material.DIAMOND_SWORD,
                "§6§lKIT PRIMÁRIO",
                primaryKit,
                abilityManager,
                "§eClique para selecionar",
                "§eseu kit primário.");

        setItem(SLOT_PRIMARY, primaryItem, (p, click) -> {
            new AbilitiesMenu(plugin, player, true).open();
            playClickSound();
        });

        // Botão Kit Secundário
        String secondaryKit = kitManager != null ? kitManager.getSecondaryKit(player) : null;
        ItemStack secondaryItem = createKitTypeItem(
                Material.IRON_SWORD,
                "§e§lKIT SECUNDÁRIO",
                secondaryKit,
                abilityManager,
                "§eClique para selecionar",
                "§eseu kit secundário.");

        setItem(SLOT_SECONDARY, secondaryItem, (p, click) -> {
            new AbilitiesMenu(plugin, player, false).open();
            playClickSound();
        });

        // Informações
        ItemStack infoItem = createInfoItem(primaryKit, secondaryKit, abilityManager);
        setItem(SLOT_INFO, infoItem);

        // Botão fechar
        setItem(SLOT_CLOSE, createCloseButton(), (p, click) -> {
            close();
            playClickSound();
        });
    }

    /**
     * Cria item para seleção de tipo de kit
     */
    private ItemStack createKitTypeItem(Material material, String name, String currentKit,
            AbilityManager abilityManager, String... extraLore) {
        List<String> lore = new ArrayList<>();
        lore.add("");

        if (currentKit == null || currentKit.isEmpty()) {
            lore.add("§7Atual: §cNenhum");
        } else {
            Ability ability = abilityManager != null ? abilityManager.getAbility(currentKit) : null;
            if (ability != null) {
                lore.add("§7Atual: " + ability.getDisplayName());
            } else {
                lore.add("§7Atual: §e" + currentKit);
            }
        }

        lore.add("");
        for (String line : extraLore) {
            lore.add(line);
        }

        return new ItemBuilder(material)
                .name(name)
                .lore(lore)
                .build();
    }

    /**
     * Cria item de informações sobre os kits
     */
    private ItemStack createInfoItem(String primaryKit, String secondaryKit, AbilityManager abilityManager) {
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7Suas habilidades:");
        lore.add("");

        // Kit Primário
        if (primaryKit == null || primaryKit.isEmpty()) {
            lore.add("§6Primário: §cNenhum");
        } else {
            Ability ability = abilityManager != null ? abilityManager.getAbility(primaryKit) : null;
            if (ability != null) {
                lore.add("§6Primário: " + ability.getDisplayName());
            } else {
                lore.add("§6Primário: §e" + primaryKit);
            }
        }

        // Kit Secundário
        if (secondaryKit == null || secondaryKit.isEmpty()) {
            lore.add("§eSecundário: §cNenhum");
        } else {
            Ability ability = abilityManager != null ? abilityManager.getAbility(secondaryKit) : null;
            if (ability != null) {
                lore.add("§eSecundário: " + ability.getDisplayName());
            } else {
                lore.add("§eSecundário: §e" + secondaryKit);
            }
        }

        lore.add("");
        lore.add("§8Você pode ter até 2 kits");
        lore.add("§8equipados ao mesmo tempo!");

        return new ItemBuilder(Material.BOOK)
                .name("§a§lINFORMAÇÕES")
                .lore(lore)
                .build();
    }
}
