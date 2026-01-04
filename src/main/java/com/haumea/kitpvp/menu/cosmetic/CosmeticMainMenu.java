package com.haumea.kitpvp.menu.cosmetic;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.managers.CosmeticManager;
import com.haumea.kitpvp.menu.BaseMenu;
import com.haumea.kitpvp.models.cosmetic.CosmeticType;
import com.haumea.kitpvp.utils.ChatStorage;
import com.haumea.kitpvp.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/**
 * Menu principal de cosméticos.
 * Exibe as categorias de cosméticos disponíveis.
 * 
 * @author HaumeaMC
 */
public class CosmeticMainMenu extends BaseMenu {

    private final CosmeticManager cosmeticManager;

    public CosmeticMainMenu(HaumeaMC plugin, Player player) {
        super(plugin, player, "&8&l* &6&lCOSMETICOS &8&l*", 45);
        this.cosmeticManager = plugin.getCosmeticManager();
    }

    @Override
    protected void setupItems() {
        // Borda decorativa
        fillBorders(15); // Vidro preto

        // Header informativo
        setItem(4, new ItemBuilder(Material.NETHER_STAR)
                .name("&6&lSEUS COSMETICOS")
                .lore(
                        "&7Personalize seus efeitos de kill!",
                        "",
                        "&fDesbloqueados: &e" + cosmeticManager.getUnlockedCount(player) +
                                " &7/ " + cosmeticManager.getAllCosmetics().size(),
                        "",
                        "&7Cosmeticos aparecem quando",
                        "&7voce mata outro jogador!")
                .build());

        // ========== CATEGORIAS ==========

        // Efeitos de Kill (Partículas)
        int killEffectCount = cosmeticManager.getUnlockedCount(player, CosmeticType.KILL_EFFECT);
        int totalKillEffects = cosmeticManager.getCosmeticsByType(CosmeticType.KILL_EFFECT).size();
        String selectedEffect = cosmeticManager.getSelectedCosmetic(player, CosmeticType.KILL_EFFECT);
        String effectStatus = selectedEffect != null
                ? "&aEquipado: " + cosmeticManager.getCosmetic(selectedEffect).getDisplayName()
                : "&7Nenhum equipado";

        setItem(20, new ItemBuilder(Material.BLAZE_POWDER)
                .name("&c&lEFEITOS DE KILL")
                .lore(
                        "&7Particulas que aparecem",
                        "&7quando voce mata alguem!",
                        "",
                        "&fDesbloqueados: &e" + killEffectCount + " &7/ " + totalKillEffects,
                        effectStatus,
                        "",
                        "&eClique para ver!")
                .glow(selectedEffect != null)
                .build(),
                (p, c) -> {
                    new CosmeticCategoryMenu(plugin, player, CosmeticType.KILL_EFFECT).open();
                    playClickSound();
                });

        // Sons de Kill
        int killSoundCount = cosmeticManager.getUnlockedCount(player, CosmeticType.KILL_SOUND);
        int totalKillSounds = cosmeticManager.getCosmeticsByType(CosmeticType.KILL_SOUND).size();
        String selectedSound = cosmeticManager.getSelectedCosmetic(player, CosmeticType.KILL_SOUND);
        String soundStatus = selectedSound != null
                ? "&aEquipado: " + cosmeticManager.getCosmetic(selectedSound).getDisplayName()
                : "&7Nenhum equipado";

        setItem(22, new ItemBuilder(Material.NOTE_BLOCK)
                .name("&e&lSONS DE KILL")
                .lore(
                        "&7Sons que tocam quando",
                        "&7voce mata alguem!",
                        "",
                        "&fDesbloqueados: &e" + killSoundCount + " &7/ " + totalKillSounds,
                        soundStatus,
                        "",
                        "&eClique para ver!")
                .glow(selectedSound != null)
                .build(),
                (p, c) -> {
                    new CosmeticCategoryMenu(plugin, player, CosmeticType.KILL_SOUND).open();
                    playClickSound();
                });

        // Mensagens de Kill
        int killMsgCount = cosmeticManager.getUnlockedCount(player, CosmeticType.KILL_MESSAGE);
        int totalKillMsgs = cosmeticManager.getCosmeticsByType(CosmeticType.KILL_MESSAGE).size();
        String selectedMsg = cosmeticManager.getSelectedCosmetic(player, CosmeticType.KILL_MESSAGE);
        String msgStatus = selectedMsg != null
                ? "&aEquipado: " + cosmeticManager.getCosmetic(selectedMsg).getDisplayName()
                : "&7Nenhum equipado";

        setItem(24, new ItemBuilder(Material.PAPER)
                .name("&b&lMENSAGENS DE KILL")
                .lore(
                        "&7Mensagens customizadas no chat",
                        "&7quando voce mata alguem!",
                        "",
                        "&fDesbloqueados: &e" + killMsgCount + " &7/ " + totalKillMsgs,
                        msgStatus,
                        "",
                        "&eClique para ver!")
                .glow(selectedMsg != null)
                .build(),
                (p, c) -> {
                    new CosmeticCategoryMenu(plugin, player, CosmeticType.KILL_MESSAGE).open();
                    playClickSound();
                });

        // ========== TOGGLE E INFO ==========

        // Toggle geral
        boolean enabled = cosmeticManager.areCosmeticsEnabled(player);
        setItem(40, new ItemBuilder(enabled ? Material.EMERALD_BLOCK : Material.REDSTONE_BLOCK)
                .name(enabled ? "&a&lCOSMETICOS ATIVADOS" : "&c&lCOSMETICOS DESATIVADOS")
                .lore(
                        "",
                        enabled ? "&7Seus cosmeticos estao &aativos&7!" : "&7Seus cosmeticos estao &cdesativados&7.",
                        "",
                        "&eClique para " + (enabled ? "desativar" : "ativar") + "!")
                .build(),
                (p, c) -> {
                    cosmeticManager.toggleCosmetics(player);
                    refresh();
                    playClickSound();
                });

        // Fechar
        setItem(44, createCloseButton(), (p, c) -> {
            player.closeInventory();
            playClickSound();
        });

        // Preencher espaços vazios
        fillEmpty(7); // Vidro cinza
    }
}
