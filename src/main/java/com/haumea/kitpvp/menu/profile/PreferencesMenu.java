package com.haumea.kitpvp.menu.profile;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.menu.BaseMenu;
import com.haumea.kitpvp.models.PlayerData;
import com.haumea.kitpvp.profile.PlayerProfile;
import com.haumea.kitpvp.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Menu de Preferências do Jogador (27 slots - 3 linhas).
 * 
 * Permite ativar/desativar configurações pessoais.
 * Usa Ink Sacks coloridos para representar estados:
 * - Verde (data 10): Ativado
 * - Cinza (data 8): Desativado
 * 
 * Layout:
 * - Slot 10: Entrar no modo vanish (admin)
 * - Slot 11: Notificar novos reports (staff)
 * - Slot 12: Mensagens privadas
 * - Slot 13: Mensagens do staff-chat (staff)
 * - Slot 22: Botão Voltar
 * 
 * @author HaumeaMC
 */
public class PreferencesMenu extends BaseMenu {

    private static final int SLOT_VANISH = 10;
    private static final int SLOT_BACK = 22;

    // Data values para Ink Sack
    private static final short DYE_GREEN = 10; // Lime dye
    private static final short DYE_GRAY = 8; // Gray dye

    public PreferencesMenu(HaumeaMC plugin, Player player) {
        super(plugin, player, "&d&lPreferências", 27);
    }

    @Override
    protected void setupItems() {
        // Preencher bordas
        fillBorders(15); // Preto

        // Decorar
        ItemStack pinkPane = createGlassPane(6, " "); // Rosa
        setItem(0, pinkPane);
        setItem(8, pinkPane);
        setItem(18, pinkPane);
        setItem(26, pinkPane);

        PlayerProfile profile = plugin.getProfileManager().getProfile(player);
        if (profile == null)
            return;

        PlayerData data = profile.getData();
        int currentSlot = SLOT_VANISH;

        // === VANISH AUTOMÁTICO (Slot 10) - Apenas Admin ===
        if (player.hasPermission("haumea.admin")) {
            boolean vanishEnabled = data.getCustomData("pref_auto_vanish", false);

            ItemStack vanishItem = new ItemBuilder(Material.INK_SACK, 1, vanishEnabled ? DYE_GREEN : DYE_GRAY)
                    .name("§e§lEntrar no modo vanish")
                    .lore(
                            "§7Quando ativado, você entrará",
                            "§7automaticamente invisível ao",
                            "§7entrar no servidor.",
                            "",
                            "§7Status: " + (vanishEnabled ? "§a§lATIVADO" : "§c§lDESATIVADO"),
                            "",
                            "§eClique para alternar!")
                    .build();

            setItem(currentSlot, vanishItem, (p, click) -> {
                togglePreference("pref_auto_vanish");
            });
            currentSlot++;
        }

        // === NOTIFICAR REPORTS (Slot 11 ou 10) - Apenas Staff ===
        if (player.hasPermission("haumea.reports.view")) {
            boolean reportsEnabled = data.getCustomData("pref_notify_reports", true);

            ItemStack reportsItem = new ItemBuilder(Material.INK_SACK, 1, reportsEnabled ? DYE_GREEN : DYE_GRAY)
                    .name("§e§lNotificar novos reports")
                    .lore(
                            "§7Quando ativado, você receberá",
                            "§7notificações quando novos",
                            "§7reports forem criados.",
                            "",
                            "§7Status: " + (reportsEnabled ? "§a§lATIVADO" : "§c§lDESATIVADO"),
                            "",
                            "§eClique para alternar!")
                    .build();

            setItem(currentSlot, reportsItem, (p, click) -> {
                togglePreference("pref_notify_reports");
            });
            currentSlot++;
        }

        // === MENSAGENS PRIVADAS (Sempre visível) ===
        boolean pmEnabled = data.getCustomData("pref_private_messages", true);

        ItemStack pmItem = new ItemBuilder(Material.INK_SACK, 1, pmEnabled ? DYE_GREEN : DYE_GRAY)
                .name("§e§lMensagens privadas")
                .lore(
                        "§7Quando ativado, você poderá",
                        "§7receber mensagens privadas",
                        "§7de outros jogadores.",
                        "",
                        "§7Status: " + (pmEnabled ? "§a§lATIVADO" : "§c§lDESATIVADO"),
                        "",
                        "§eClique para alternar!")
                .build();

        setItem(currentSlot, pmItem, (p, click) -> {
            togglePreference("pref_private_messages");
        });
        currentSlot++;

        // === MENSAGENS STAFF-CHAT - Apenas Staff ===
        if (player.hasPermission("haumea.staffchat")) {
            boolean staffchatEnabled = data.getCustomData("pref_staffchat_messages", true);

            ItemStack staffchatItem = new ItemBuilder(Material.INK_SACK, 1, staffchatEnabled ? DYE_GREEN : DYE_GRAY)
                    .name("§e§lMensagens do staff-chat")
                    .lore(
                            "§7Quando ativado, você verá",
                            "§7as mensagens do chat",
                            "§7exclusivo da equipe.",
                            "",
                            "§7Status: " + (staffchatEnabled ? "§a§lATIVADO" : "§c§lDESATIVADO"),
                            "",
                            "§eClique para alternar!")
                    .build();

            setItem(currentSlot, staffchatItem, (p, click) -> {
                togglePreference("pref_staffchat_messages");
            });
        }

        // === BOTÃO VOLTAR (Slot 22) ===
        ItemStack backItem = new ItemBuilder(Material.ARROW)
                .name("§c§lVoltar")
                .lore(
                        "§7Clique para voltar",
                        "§7ao menu de perfil.")
                .build();

        setItem(SLOT_BACK, backItem, (p, click) -> {
            new ProfileMenu(plugin, player).open();
            playClickSound();
        });
    }

    /**
     * Alterna uma preferência e atualiza o menu
     */
    private void togglePreference(String key) {
        PlayerProfile profile = plugin.getProfileManager().getProfile(player);
        if (profile == null)
            return;

        PlayerData data = profile.getData();

        // Obter valor atual e inverter
        boolean currentValue = data.getCustomData(key, false);
        boolean newValue = !currentValue;

        // Salvar nova preferência
        data.setCustomData(key, newValue);

        // PERSISTÊNCIA: Salvar no MongoDB imediatamente para garantir que não se perca
        profile.prepareForSave();
        plugin.getProfileManager().saveOfflineData(data);

        // Tocar som
        if (newValue) {
            playPlingSound();
        } else {
            playClickSound();
        }

        // Atualizar menu sem fechar
        setupItems();
        player.updateInventory();
    }
}
