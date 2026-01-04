package com.haumea.kitpvp.menu.kit;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.abilities.Ability;
import com.haumea.kitpvp.abilities.AbilityManager;
import com.haumea.kitpvp.abilities.AbilityRarity;
import com.haumea.kitpvp.managers.KitManager;
import com.haumea.kitpvp.menu.BaseMenu;
import com.haumea.kitpvp.utils.ChatStorage;
import com.haumea.kitpvp.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Menu de seleção de Abilities (Habilidades de Kits).
 * 
 * Exibe todas as abilities disponíveis organizadas por raridade.
 * 
 * Layout (54 slots):
 * - Linha 0: Bordas decorativas com título
 * - Linhas 1-4: Abilities organizadas por raridade
 * - Linha 5: Informações e navegação
 * 
 * @author HaumeaMC
 */
public class AbilitiesMenu extends BaseMenu {

    /**
     * Slots para kits por raridade
     */
    private static final int[] RARE_SLOTS = { 10, 11, 12, 13, 14, 15, 16, 19, 20, 21 };
    private static final int[] EPIC_SLOTS = { 22, 23, 24, 25 };
    private static final int[] LEGENDARY_SLOTS = { 28, 29, 30 };
    private static final int[] MYSTIC_SLOTS = { 32, 33, 34 };

    private static final int SLOT_INFO = 49;
    private static final int SLOT_CLOSE = 53;
    private static final int SLOT_BACK = 45;

    /**
     * Tipo de seleção: true = primário, false = secundário
     */
    private final boolean isPrimary;

    public AbilitiesMenu(HaumeaMC plugin, Player player, boolean isPrimary) {
        super(plugin, player, isPrimary ? "§6§lSELECIONAR KIT PRIMÁRIO" : "§e§lSELECIONAR KIT SECUNDÁRIO", 54);
        this.isPrimary = isPrimary;
    }

    @Override
    protected void setupItems() {
        AbilityManager abilityManager = plugin.getAbilityManager();
        KitManager kitManager = plugin.getKitManager();

        if (abilityManager == null || kitManager == null)
            return;

        // Preencher bordas
        fillBorders(7);

        // Headers por raridade
        setItem(9, createRarityHeader(AbilityRarity.RARE, "§9§lRARA"));
        setItem(27, createRarityHeader(AbilityRarity.LEGENDARY, "§6§lLENDÁRIA"));
        setItem(31, createRarityHeader(AbilityRarity.MYSTIC, "§d§lMÍSTICA"));

        // Separador EPIC
        ItemStack epicHeader = new ItemBuilder(Material.STAINED_GLASS_PANE, 1, (short) 10)
                .name("§5§lKITS ÉPICOS")
                .build();
        setItem(18, epicHeader);

        // Preencher kits por raridade
        List<Ability> rareAbilities = abilityManager.getAbilitiesByRarity(AbilityRarity.RARE);
        List<Ability> epicAbilities = abilityManager.getAbilitiesByRarity(AbilityRarity.EPIC);
        List<Ability> legendaryAbilities = abilityManager.getAbilitiesByRarity(AbilityRarity.LEGENDARY);
        List<Ability> mysticAbilities = abilityManager.getAbilitiesByRarity(AbilityRarity.MYSTIC);

        // Colocar kits nos slots
        placeAbilities(rareAbilities, RARE_SLOTS, kitManager);
        placeAbilities(epicAbilities, EPIC_SLOTS, kitManager);
        placeAbilities(legendaryAbilities, LEGENDARY_SLOTS, kitManager);
        placeAbilities(mysticAbilities, MYSTIC_SLOTS, kitManager);

        // Opção "Nenhum" no slot 37
        ItemStack noneItem = new ItemBuilder(Material.BARRIER)
                .name("§c§lRemover Kit")
                .lore(
                        "",
                        "§7Clique para remover",
                        "§7seu kit " + (isPrimary ? "primário" : "secundário") + ".",
                        "",
                        "§eClique para remover!")
                .build();

        setItem(37, noneItem, (p, click) -> {
            if (isPrimary) {
                kitManager.setPrimaryKit(player, null);
                ChatStorage.sendCustom(player, "§aKit primário removido!");
            } else {
                kitManager.setSecondaryKit(player, null);
                ChatStorage.sendCustom(player, "§aKit secundário removido!");
            }
            playPlingSound();
            close();
        });

        // Informação do kit atual
        String currentKit = isPrimary ? kitManager.getPrimaryKit(player) : kitManager.getSecondaryKit(player);
        ItemStack infoItem = createCurrentKitInfo(currentKit, abilityManager);
        setItem(SLOT_INFO, infoItem);

        // Botão fechar
        setItem(SLOT_CLOSE, createCloseButton(), (p, click) -> {
            close();
            playClickSound();
        });

        // Botão voltar
        setItem(SLOT_BACK, createBackButton(), (p, click) -> {
            // Voltar ao menu de seleção de tipo
            new KitTypeMenu(plugin, player).open();
            playClickSound();
        });
    }

    /**
     * Coloca abilities nos slots especificados
     */
    private void placeAbilities(List<Ability> abilities, int[] slots, KitManager kitManager) {
        int index = 0;
        for (Ability ability : abilities) {
            if (index >= slots.length)
                break;

            int slot = slots[index];
            ItemStack item = createAbilityItem(ability, kitManager);

            setItem(slot, item, (p, click) -> handleAbilitySelection(ability, kitManager));
            index++;
        }
    }

    /**
     * Cria item para uma ability
     */
    private ItemStack createAbilityItem(Ability ability, KitManager kitManager) {
        String currentPrimary = kitManager.getPrimaryKit(player);
        String currentSecondary = kitManager.getSecondaryKit(player);
        String abilityName = ability.getName().toLowerCase();

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ability.getRarity().getLorePrefix());
        lore.add("");

        // Descrição do ChatStorage
        String descKey = "ability." + abilityName + ".description";
        String desc = ChatStorage.getMessage(descKey);
        if (!desc.equals(descKey)) {
            lore.add("§7" + desc);
            lore.add("");
        }

        // Cooldown
        if (ability.getCooldownSeconds() > 0) {
            lore.add("§eCooldown: §f" + ability.getCooldownSeconds() + "s");
        }

        // Preço
        if (!ability.isFree()) {
            lore.add("§ePreço: §f" + ChatStorage.formatNumber(ability.getPrice()) + " coins/semana");
        } else {
            lore.add("§aGratuito");
        }

        lore.add("");

        // Verificar se já está equipado
        boolean isEquippedPrimary = abilityName.equalsIgnoreCase(currentPrimary);
        boolean isEquippedSecondary = abilityName.equalsIgnoreCase(currentSecondary);

        if ((isPrimary && isEquippedPrimary) || (!isPrimary && isEquippedSecondary)) {
            lore.add("§a§l✔ EQUIPADO");
        } else if (isEquippedPrimary || isEquippedSecondary) {
            lore.add("§7Equipado como " + (isEquippedPrimary ? "primário" : "secundário"));
            lore.add("");
            lore.add("§eClique para selecionar!");
        } else {
            // Verificar compatibilidade
            String otherKit = isPrimary ? currentSecondary : currentPrimary;
            if (otherKit != null && !kitManager.areKitsCompatible(ability.getName(), otherKit)) {
                lore.add("§c§l⚠ INCOMPATÍVEL");
                lore.add("§cNão combina com seu outro kit!");
            } else {
                // Verificar se tem o kit (permissão ou aluguel)
                if (kitManager.hasKitPermission(player, ability.getName()) ||
                        kitManager.hasRentedKit(player.getUniqueId(), ability.getName())) {
                    lore.add("§eClique para selecionar!");
                } else {
                    lore.add("§c§l✘ BLOQUEADO");
                    lore.add("§7Compre ou alugue este kit!");
                }
            }
        }

        return new ItemBuilder(ability.getIcon())
                .name(ability.getDisplayName())
                .lore(lore)
                .build();
    }

    /**
     * Processa a seleção de uma ability
     */
    private void handleAbilitySelection(Ability ability, KitManager kitManager) {
        String abilityName = ability.getName();
        String currentPrimary = kitManager.getPrimaryKit(player);
        String currentSecondary = kitManager.getSecondaryKit(player);

        // Verificar se já está usando
        if (isPrimary && abilityName.equalsIgnoreCase(currentPrimary)) {
            ChatStorage.sendCustom(player, "§cVocê já está usando este kit como primário!");
            playErrorSound();
            return;
        }
        if (!isPrimary && abilityName.equalsIgnoreCase(currentSecondary)) {
            ChatStorage.sendCustom(player, "§cVocê já está usando este kit como secundário!");
            playErrorSound();
            return;
        }

        // Verificar acesso (permissão ou aluguel)
        if (!kitManager.hasKitPermission(player, abilityName) &&
                !kitManager.hasRentedKit(player.getUniqueId(), abilityName)) {
            ChatStorage.sendCustom(player, "§cVocê não tem acesso a este kit! Alugue-o na loja.");
            playErrorSound();
            return;
        }

        // Verificar compatibilidade
        String otherKit = isPrimary ? currentSecondary : currentPrimary;
        if (otherKit != null && !kitManager.areKitsCompatible(abilityName, otherKit)) {
            ChatStorage.sendCustom(player, "§cEste kit é incompatível com seu " +
                    (isPrimary ? "kit secundário" : "kit primário") + "!");
            playErrorSound();
            return;
        }

        // Selecionar o kit
        if (isPrimary) {
            kitManager.setPrimaryKit(player, abilityName);
        } else {
            kitManager.setSecondaryKit(player, abilityName);
        }

        ChatStorage.sendCustom(player, "§aVocê selecionou o kit " + ability.getDisplayName() + "§a!");
        playPlingSound();
        close();
    }

    /**
     * Cria header de raridade
     */
    private ItemStack createRarityHeader(AbilityRarity rarity, String name) {
        short color;
        switch (rarity) {
            case RARE:
                color = 3; // Azul
                break;
            case EPIC:
                color = 10; // Roxo
                break;
            case LEGENDARY:
                color = 1; // Laranja
                break;
            case MYSTIC:
                color = 2; // Magenta
                break;
            default:
                color = 0;
        }

        return new ItemBuilder(Material.STAINED_GLASS_PANE, 1, color)
                .name(name)
                .build();
    }

    /**
     * Cria item de informação do kit atual
     */
    private ItemStack createCurrentKitInfo(String kitName, AbilityManager abilityManager) {
        if (kitName == null || kitName.isEmpty()) {
            return new ItemBuilder(Material.PAPER)
                    .name("§7Kit " + (isPrimary ? "Primário" : "Secundário") + " Atual")
                    .lore(
                            "",
                            "§7Selecionado: §cNenhum",
                            "",
                            "§8Selecione um kit acima!")
                    .build();
        }

        Ability ability = abilityManager.getAbility(kitName);
        if (ability == null) {
            return new ItemBuilder(Material.PAPER)
                    .name("§7Kit " + (isPrimary ? "Primário" : "Secundário") + " Atual")
                    .lore(
                            "",
                            "§7Selecionado: §e" + kitName,
                            "",
                            "§8Clique em outro para trocar!")
                    .build();
        }

        return new ItemBuilder(ability.getIcon())
                .name("§a§lKit " + (isPrimary ? "Primário" : "Secundário") + " Atual")
                .lore(
                        "",
                        "§7Selecionado: " + ability.getDisplayName(),
                        "",
                        "§8Clique em outro para trocar!")
                .build();
    }
}
