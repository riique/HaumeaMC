package com.haumea.kitpvp.menu.duel;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.menu.BaseMenu;
import com.haumea.kitpvp.models.DuelSettings;
import com.haumea.kitpvp.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Menu de configuração de duelo.
 * 
 * Permite ao jogador configurar:
 * - Tipo de espada
 * - Tipo de armadura
 * - Modo de sopas
 * - Enchantments
 * - Recraft
 * 
 * @author HaumeaMC
 */
public class DuelSettingsMenu extends BaseMenu {

    private final DuelSettings settings;
    private final Player target; // null se for apenas configuração, não-null se for desafio

    /**
     * Construtor do menu de configuração.
     * 
     * @param plugin   Instância do plugin
     * @param player   Jogador que está configurando
     * @param settings Configurações a editar
     * @param target   Jogador alvo do desafio (null se não for desafio)
     */
    public DuelSettingsMenu(HaumeaMC plugin, Player player, DuelSettings settings, Player target) {
        super(plugin, player, "§8⚔ Configurar Duelo", 45);
        this.settings = settings != null ? settings : new DuelSettings();
        this.target = target;
    }

    @Override
    protected void setupItems() {
        // Preencher bordas
        fillBorders(15); // Vidro preto

        // ==================== ESPADA (Slot 10) ====================
        ItemStack swordItem = new ItemBuilder(settings.getSwordType().getMaterial())
                .name("§e§lTipo de Espada")
                .lore(
                        "",
                        "§7Atual: " + settings.getSwordType().getFormattedName(),
                        "",
                        "§8▪ §6Madeira §7- Dano baixo",
                        "§8▪ §7Pedra §7- Dano médio-baixo",
                        "§8▪ §fFerro §7- Dano médio-alto",
                        "§8▪ §bDiamante §7- Dano alto",
                        "",
                        "§eClique para alterar!")
                .build();

        setItem(10, swordItem, (p, click) -> {
            settings.cycleSwordType();
            playClickSound();
            refresh();
        });

        // ==================== ARMADURA (Slot 12) ====================
        ItemStack armorItem = new ItemBuilder(settings.getArmorType().getChestplate())
                .name("§e§lTipo de Armadura")
                .lore(
                        "",
                        "§7Atual: " + settings.getArmorType().getFormattedName(),
                        "",
                        "§8▪ §6Couro §7- Proteção baixa",
                        "§8▪ §7Malha §7- Proteção média-baixa",
                        "§8▪ §fFerro §7- Proteção média-alta",
                        "§8▪ §bDiamante §7- Proteção alta",
                        "",
                        "§eClique para alterar!")
                .build();

        setItem(12, armorItem, (p, click) -> {
            settings.cycleArmorType();
            playClickSound();
            refresh();
        });

        // ==================== SOPAS (Slot 14) ====================
        ItemStack soupItem = new ItemBuilder(Material.MUSHROOM_SOUP)
                .name("§e§lQuantidade de Sopas")
                .lore(
                        "",
                        "§7Atual: " + settings.getSoupMode().getFormattedName(),
                        "",
                        "§8▪ §e16 Sopas §7- Partida rápida",
                        "§8▪ §632 Sopas §7- Partida média",
                        "§8▪ §aInfinitas §7- Partida longa",
                        "",
                        "§eClique para alterar!")
                .build();

        setItem(14, soupItem, (p, click) -> {
            settings.cycleSoupMode();
            playClickSound();
            refresh();
        });

        // ==================== ENCHANTMENTS (Slot 16) ====================
        boolean hasEnchants = settings.hasEnchantments();
        ItemBuilder enchantBuilder = new ItemBuilder(Material.ENCHANTED_BOOK)
                .name("§e§lEnchantments")
                .lore(
                        "",
                        "§7Status: " + (hasEnchants ? "§aAtivado" : "§cDesativado"),
                        "",
                        "§7Quando ativado, adiciona:",
                        "§8▪ §dSharpness I §7na espada",
                        "§8▪ §dProtection I §7na armadura",
                        "",
                        "§eClique para " + (hasEnchants ? "desativar" : "ativar") + "!");
        if (hasEnchants) {
            enchantBuilder.glow();
        }
        ItemStack enchantItem = enchantBuilder.build();

        setItem(16, enchantItem, (p, click) -> {
            settings.toggleEnchantments();
            playClickSound();
            refresh();
        });

        // ==================== RECRAFT (Slot 28) ====================
        boolean hasRecraft = settings.hasRecraft();
        ItemBuilder recraftBuilder = new ItemBuilder(Material.BOWL)
                .amount(hasRecraft ? 24 : 1)
                .name("§e§lRecraft")
                .lore(
                        "",
                        "§7Status: " + (hasRecraft ? "§aAtivado" : "§cDesativado"),
                        "",
                        "§7Quando ativado, você recebe",
                        "§7materiais para craftar mais sopas:",
                        "§8▪ §fTigelas",
                        "§8▪ §cCogumelos Vermelhos",
                        "§8▪ §6Cogumelos Marrons",
                        "",
                        "§eClique para " + (hasRecraft ? "desativar" : "ativar") + "!");
        if (hasRecraft) {
            recraftBuilder.glow();
        }
        ItemStack recraftItem = recraftBuilder.build();

        setItem(28, recraftItem, (p, click) -> {
            settings.toggleRecraft();
            playClickSound();
            refresh();
        });

        // ==================== RESUMO (Slot 22) ====================
        ItemStack summaryItem = new ItemBuilder(Material.PAPER)
                .name("§f§lResumo das Configurações")
                .lore(
                        "",
                        settings.getSummary(),
                        "",
                        "§7Suas configurações atuais",
                        "§7para o próximo duelo.")
                .build();

        setItem(22, summaryItem);

        // ==================== CONFIRMAR (Slot 30) ====================
        String confirmLore;
        if (target != null) {
            confirmLore = "§7Desafiar §e" + target.getName();
        } else {
            confirmLore = "§7Entrar na fila com estas configurações";
        }

        ItemStack confirmItem = new ItemBuilder(Material.EMERALD_BLOCK)
                .name("§a§lConfirmar")
                .lore(
                        "",
                        confirmLore,
                        "",
                        "§aClique para confirmar!")
                .build();

        setItem(30, confirmItem, (p, click) -> {
            close();

            if (target != null) {
                // Enviar desafio
                if (plugin.getDuelManager() != null) {
                    plugin.getDuelManager().sendChallenge(player, target, settings);
                }
            } else {
                // Entrar na fila
                if (plugin.getDuelManager() != null) {
                    plugin.getDuelManager().joinQueue(player, settings);
                }
            }
        });

        // ==================== CANCELAR (Slot 32) ====================
        ItemStack cancelItem = new ItemBuilder(Material.REDSTONE_BLOCK)
                .name("§c§lCancelar")
                .lore(
                        "",
                        "§7Cancelar e voltar",
                        "",
                        "§cClique para cancelar!")
                .build();

        setItem(32, cancelItem, (p, click) -> {
            close();
            player.playSound(player.getLocation(), Sound.NOTE_BASS, 1.0f, 0.5f);
        });

        // Preencher espaços vazios
        fillEmpty(7); // Vidro cinza
    }
}
