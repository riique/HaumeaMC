package com.haumea.kitpvp.menu.skin;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.managers.SkinManager;
import com.haumea.kitpvp.menu.BaseMenu;
import com.haumea.kitpvp.utils.ChatStorage;
import com.haumea.kitpvp.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Menu visual para seleção de skins.
 * 
 * Exibe skins pré-definidas organizadas por categoria:
 * - YouTubers Brasileiros
 * - YouTubers Internacionais
 * - Skins Clássicas (Mobs)
 * - Opção de Reset
 * 
 * @author HaumeaMC
 */
public class SkinMenu extends BaseMenu {

    private final SkinManager skinManager;

    public SkinMenu(HaumeaMC plugin, Player player) {
        super(plugin, player, "&6&lSKINS &8- &fEscolha sua Skin", 54);
        this.skinManager = plugin.getSkinManager();
    }

    @Override
    protected void setupItems() {
        // Limpar inventário
        inventory.clear();

        // Bordas decorativas
        fillBorders(15); // Vidro preto

        // Cabeçalho informativo
        setItem(4, createInfoItem());

        // Botão de reset no centro superior
        setItem(22, createResetButton(), (p, c) -> {
            playClickSound();
            p.closeInventory();
            skinManager.resetSkin(p);
        });

        // === SKINS PRÉ-DEFINIDAS ===

        // Linha 1: YouTubers BR (slots 10-16)
        setupYoutubersBR();

        // Linha 2: Skins Internacionais (slots 28-34)
        setupInternational();

        // Linha 3: Skins Clássicas (slots 37-43)
        setupClassics();

        // Botão de fechar
        setItem(49, createCloseButton(), (p, c) -> {
            playClickSound();
            p.closeInventory();
        });

        // Skin atual do jogador
        setItem(45, createCurrentSkinInfo());

        // Preencher slots vazios
        fillEmpty(7); // Vidro cinza
    }

    /**
     * Cria o item de informação do menu
     */
    private ItemStack createInfoItem() {
        String currentSkin = skinManager.getSavedSkin(player.getUniqueId());
        String skinDisplay = currentSkin != null ? "§e" + currentSkin : "§7Original";

        return new ItemBuilder(Material.SKULL_ITEM, 1, (short) 3)
                .name("§6§lSISTEMA DE SKINS")
                .lore(
                        "",
                        "§7Altere sua aparência para a skin",
                        "§7de qualquer jogador premium!",
                        "",
                        "§fSkin atual: " + skinDisplay,
                        "",
                        "§e➜ Clique em uma skin para aplicar",
                        "§e➜ Use /skin <nick> para outras skins")
                .build();
    }

    /**
     * Cria o botão de reset
     */
    private ItemStack createResetButton() {
        boolean hasCustom = skinManager.hasCustomSkin(player.getUniqueId());

        return new ItemBuilder(Material.BARRIER)
                .name("§c§lRESETAR SKIN")
                .lore(
                        "",
                        "§7Restaura sua skin original",
                        "§7baseada no seu nick.",
                        "",
                        hasCustom ? "§e➜ Clique para resetar" : "§7Você já está usando sua skin original",
                        "")
                .build();
    }

    /**
     * Cria info da skin atual
     */
    private ItemStack createCurrentSkinInfo() {
        String currentSkin = skinManager.getSavedSkin(player.getUniqueId());

        return new ItemBuilder(Material.PAPER)
                .name("§e§lSUA SKIN ATUAL")
                .lore(
                        "",
                        currentSkin != null
                                ? "§fUsando skin de: §e" + currentSkin
                                : "§fUsando sua skin §eoriginal",
                        "",
                        "§7Use §f/skin <nick> §7para",
                        "§7usar qualquer outra skin!")
                .build();
    }

    /**
     * Configura skins de YouTubers brasileiros
     */
    private void setupYoutubersBR() {
        int[] slots = { 10, 11, 12, 13, 14, 15, 16 };
        String[][] skins = {
                { "AuthenticGames", "§c§lAUTHENTIC", "§7YouTuber BR famoso" },
                { "RezendeEvil", "§e§lREZENDE", "§7YouTuber BR de Minecraft" },
                { "VenomExtreme", "§a§lVENOM", "§7YouTuber BR de Minecraft" },
                { "TazerCraft", "§b§lTAZERCRAFT", "§7YouTuber BR de Minecraft" },
                { "Jazzghost", "§d§lJAZZGHOST", "§7YouTuber BR de Minecraft" },
                { "FlakesPower", "§6§lFLAKES", "§7YouTuber BR de Minecraft" },
                { "Cellbit", "§5§lCELLBIT", "§7Streamer BR famoso" }
        };

        for (int i = 0; i < slots.length && i < skins.length; i++) {
            final String skinName = skins[i][0];
            final String displayName = skins[i][1];
            final String description = skins[i][2];

            ItemStack item = new ItemBuilder(Material.SKULL_ITEM, 1, (short) 3)
                    .name(displayName)
                    .lore(
                            "",
                            description,
                            "",
                            "§fSkin de: §e" + skinName,
                            "",
                            "§a➜ Clique para usar esta skin")
                    .build();

            setItem(slots[i], item, (p, c) -> {
                handleSkinClick(skinName);
            });
        }
    }

    /**
     * Configura skins internacionais
     */
    private void setupInternational() {
        int[] slots = { 28, 29, 30, 31, 32, 33, 34 };
        String[][] skins = {
                { "Notch", "§6§lNOTCH", "§7Criador do Minecraft" },
                { "Dream", "§a§lDREAM", "§7YouTuber famoso mundial" },
                { "Technoblade", "§c§lTECHNOBLADE", "§7Lenda do Minecraft" },
                { "jeb_", "§b§lJEB", "§7Desenvolvedor Mojang" },
                { "Dinnerbone", "§e§lDINNERBONE", "§7Desenvolvedor Mojang" },
                { "Ph1LzA", "§d§lPH1LZA", "§7Hardcore legend" },
                { "TommyInnit", "§9§lTOMMYINNIT", "§7Streamer famoso" }
        };

        for (int i = 0; i < slots.length && i < skins.length; i++) {
            final String skinName = skins[i][0];
            final String displayName = skins[i][1];
            final String description = skins[i][2];

            ItemStack item = new ItemBuilder(Material.SKULL_ITEM, 1, (short) 3)
                    .name(displayName)
                    .lore(
                            "",
                            description,
                            "",
                            "§fSkin de: §e" + skinName,
                            "",
                            "§a➜ Clique para usar esta skin")
                    .build();

            setItem(slots[i], item, (p, c) -> {
                handleSkinClick(skinName);
            });
        }
    }

    /**
     * Configura skins clássicas de mobs/personagens
     */
    private void setupClassics() {
        int[] slots = { 37, 38, 39, 40, 41, 42, 43 };
        String[][] skins = {
                { "MHF_Steve", "§f§lSTEVE", "§7Skin clássica padrão" },
                { "MHF_Alex", "§6§lALEX", "§7Skin clássica feminina" },
                { "MHF_Herobrine", "§8§lHEROBRINE", "§7Lenda do Minecraft" },
                { "MHF_Creeper", "§a§lCREEPER", "§7Skin de Creeper" },
                { "MHF_Enderman", "§5§lENDERMAN", "§7Skin de Enderman" },
                { "MHF_Pig", "§d§lPIG", "§7Skin de Porquinho" },
                { "MHF_Villager", "§e§lVILLAGER", "§7Skin de Aldeão" }
        };

        for (int i = 0; i < slots.length && i < skins.length; i++) {
            final String skinName = skins[i][0];
            final String displayName = skins[i][1];
            final String description = skins[i][2];

            ItemStack item = new ItemBuilder(Material.SKULL_ITEM, 1, (short) 3)
                    .name(displayName)
                    .lore(
                            "",
                            description,
                            "",
                            "§fSkin de: §e" + skinName,
                            "",
                            "§a➜ Clique para usar esta skin")
                    .build();

            setItem(slots[i], item, (p, c) -> {
                handleSkinClick(skinName);
            });
        }
    }

    /**
     * Processa clique em uma skin
     */
    private void handleSkinClick(String skinName) {
        // Verificar cooldown
        if (skinManager.isOnCooldown(player)) {
            long remaining = skinManager.getCooldownRemaining(player) / 1000;
            ChatStorage.send(player, "skin.cooldown", "time", String.valueOf(remaining));
            playErrorSound();
            return;
        }

        playClickSound();
        player.closeInventory();

        // Informar que está buscando
        ChatStorage.send(player, "skin.searching", "skin", skinName);

        // Aplicar skin
        skinManager.applySkinAsync(player, skinName, success -> {
            if (success) {
                ChatStorage.send(player, "skin.applied", "skin", skinName);
            } else {
                ChatStorage.send(player, "skin.not-found", "skin", skinName);
            }
        });
    }
}
