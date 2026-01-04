package com.haumea.kitpvp.menu;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.managers.MultiplierManager;
import com.haumea.kitpvp.models.ActiveMultiplier;
import com.haumea.kitpvp.models.MultiplierType;
import com.haumea.kitpvp.utils.ChatStorage;
import com.haumea.kitpvp.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;

/**
 * Menu GUI para visualização e ativação de multiplicadores de coins.
 * 
 * Layout do menu (54 slots):
 * - Linha 0: Borda decorativa + título
 * - Linha 1: Informação do multiplicador ativo (se houver)
 * - Linhas 2-4: Multiplicadores disponíveis
 * - Linha 5: Borda decorativa + botão fechar
 * 
 * @author HaumeaMC
 */
public class MultiplierMenu extends BaseMenu {

    private final MultiplierManager multiplierManager;

    /**
     * Construtor do MultiplierMenu
     * 
     * @param plugin Instância do plugin
     * @param player Jogador visualizando o menu
     */
    public MultiplierMenu(HaumeaMC plugin, Player player) {
        super(plugin, player, "&6&lMULTIPLICADORES &8- &fMeus Multiplicadores", 54);
        this.multiplierManager = plugin.getMultiplierManager();
    }

    @Override
    protected void setupItems() {
        // Preencher borda com vidro decorativo
        fillBorders(15); // Preto

        // Título no centro (slot 4)
        setItem(4, new ItemBuilder(Material.GOLDEN_APPLE)
                .name("&6&l⚡ MULTIPLICADORES DE COINS")
                .lore(
                        "",
                        "&7Ative multiplicadores para ganhar",
                        "&7mais coins ao matar jogadores!",
                        "",
                        "&eDica: &7Clique em um multiplicador",
                        "&7para ativá-lo.",
                        "")
                .build());

        // Informação do multiplicador ativo (slots 10-16)
        setupActiveMultiplierInfo();

        // Linha separadora (linha 2 = slots 18-26)
        for (int i = 18; i <= 26; i++) {
            setItem(i, createGlassPane(8, "&8▬▬▬▬▬▬▬▬▬"));
        }

        // Multiplicadores disponíveis (linhas 3-4)
        setupMultiplierItems();

        // Botão de fechar (slot 49)
        setClickableItem(49, createCloseButton(), this::close);

        // Informações extras (slots 45 e 53)
        setItem(45, new ItemBuilder(Material.PAPER)
                .name("&e&lINFORMAÇÕES")
                .lore(
                        "",
                        "&7• Multiplicadores aumentam seus",
                        "&7  ganhos de coins por kill.",
                        "",
                        "&7• Apenas &fUM multiplicador&7 pode",
                        "&7  estar ativo por vez.",
                        "",
                        "&7• Ao ativar um multiplicador do",
                        "&7  mesmo tipo, o &etempo é estendido&7.",
                        "",
                        "&7• Se ativar um multiplicador maior,",
                        "&7  ele &asubstitui&7 o atual.",
                        "")
                .build());

        setItem(53, new ItemBuilder(Material.BOOK)
                .name("&b&lCOMO CONSEGUIR")
                .lore(
                        "",
                        "&7Você pode obter multiplicadores:",
                        "",
                        "&f• &eLoja do servidor",
                        "&f• &eEventos especiais",
                        "&f• &ePromoções",
                        "&f• &eCaixas de recompensa",
                        "")
                .build());
    }

    /**
     * Configura a área do multiplicador ativo
     */
    private void setupActiveMultiplierInfo() {
        ActiveMultiplier active = multiplierManager.getActiveMultiplier(player);

        if (active != null) {
            MultiplierType type = active.getType();

            // Ícone do multiplicador ativo (centro)
            setItem(13, new ItemBuilder(type.getMaterial())
                    .name("&a&l✓ MULTIPLICADOR ATIVO")
                    .lore(
                            "",
                            "&fTipo: " + type.getDisplayMultiplier() + " &7(" + type.getDisplayRarity() + "&7)",
                            "&fBônus: &a+" + type.getBonusPercentage() + "% &7de coins",
                            "",
                            "&fTempo restante:",
                            "&e⏱ " + active.getFormattedRemainingTime(),
                            "",
                            "&7O tempo continua contando",
                            "&7mesmo quando você está offline.",
                            "")
                    .glow()
                    .build());

            // Decoração lateral
            setItem(10, createGlassPane(type.getGlassColor(), type.getDisplayMultiplier()));
            setItem(11, createGlassPane(type.getGlassColor(), "&a&l>>"));
            setItem(12, createGlassPane(type.getGlassColor()));
            setItem(14, createGlassPane(type.getGlassColor()));
            setItem(15, createGlassPane(type.getGlassColor(), "&a&l<<"));
            setItem(16, createGlassPane(type.getGlassColor(), type.getDisplayMultiplier()));

        } else {
            // Nenhum multiplicador ativo
            setItem(13, new ItemBuilder(Material.BARRIER)
                    .name("&c&l✗ NENHUM MULTIPLICADOR ATIVO")
                    .lore(
                            "",
                            "&7Você não possui nenhum",
                            "&7multiplicador ativo no momento.",
                            "",
                            "&eClique em um multiplicador abaixo",
                            "&epara ativá-lo!",
                            "")
                    .build());

            // Decoração lateral cinza
            for (int i = 10; i <= 16; i++) {
                if (i != 13) {
                    setItem(i, createGlassPane(7));
                }
            }
        }
    }

    /**
     * Configura os itens dos multiplicadores disponíveis
     */
    private void setupMultiplierItems() {
        Map<MultiplierType, Integer> inventory = multiplierManager.getFullInventory(player);

        // Posições dos multiplicadores (linha 3, centralizados)
        // Slot 20 = x1.5, 21 = x2.0, 22 = x2.5, 23 = x3.0, 24 = x3.5
        int[] slots = { 20, 21, 22, 23, 24 };
        MultiplierType[] types = MultiplierType.values();

        for (int i = 0; i < types.length && i < slots.length; i++) {
            MultiplierType type = types[i];
            int slot = slots[i];
            int count = inventory.getOrDefault(type, 0);

            // Decoração acima (linha anterior)
            setItem(slot - 9, createGlassPane(type.getGlassColor()));

            // Item do multiplicador
            setItem(slot, createMultiplierItem(type, count), (p, click) -> {
                if (count > 0) {
                    attemptActivate(type);
                } else {
                    playErrorSound();
                    ChatStorage.send(player, "multiplier.not-available", "type", type.getDisplayMultiplier());
                }
            });

            // Decoração abaixo (linha seguinte)
            setItem(slot + 9, createGlassPane(type.getGlassColor()));
        }

        // Decorações laterais
        setItem(19, createGlassPane(15));
        setItem(28, createGlassPane(15));
        setItem(25, createGlassPane(15));
        setItem(34, createGlassPane(15));
    }

    /**
     * Cria o item de um multiplicador para exibição
     */
    private org.bukkit.inventory.ItemStack createMultiplierItem(MultiplierType type, int count) {
        ItemBuilder builder = new ItemBuilder(type.getMaterial())
                .name(type.getDisplayRarity() + " " + type.getDisplayMultiplier());

        if (count > 0) {
            builder.amount(Math.min(count, 64))
                    .lore(
                            "",
                            "&fMultiplicador: " + type.getDisplayMultiplier(),
                            "&fBônus: &a+" + type.getBonusPercentage() + "% &7de coins",
                            "&fDuração: &e" + type.getFormattedDuration(),
                            "",
                            "&fQuantidade: &a" + count,
                            "",
                            "&a▶ Clique para ativar!",
                            "");
        } else {
            builder.lore(
                    "",
                    "&fMultiplicador: " + type.getDisplayMultiplier(),
                    "&fBônus: &a+" + type.getBonusPercentage() + "% &7de coins",
                    "&fDuração: &e" + type.getFormattedDuration(),
                    "",
                    "&cQuantidade: &c0",
                    "",
                    "&8✗ Você não possui este multiplicador",
                    "");
        }

        return builder.build();
    }

    /**
     * Tenta ativar um multiplicador
     */
    private void attemptActivate(MultiplierType type) {
        player.closeInventory();

        // Executar no próximo tick para evitar problemas com o inventário
        new BukkitRunnable() {
            @Override
            public void run() {
                boolean success = multiplierManager.activateMultiplier(player, type);

                if (success) {
                    // Efeitos visuais de sucesso
                    player.playSound(player.getLocation(), Sound.LEVEL_UP, 1.0f, 1.5f);

                    // Exibir título
                    sendTitle(
                            ChatStorage.colorize("&a&l✓ MULTIPLICADOR ATIVADO"),
                            ChatStorage.colorize(type.getDisplayRarity() + " " + type.getDisplayMultiplier()),
                            10, 40, 10);
                } else {
                    player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1.0f, 1.0f);
                }
            }
        }.runTaskLater(plugin, 1L);
    }

    /**
     * Envia título para o jogador (compatível com 1.8)
     */
    private void sendTitle(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        try {
            player.sendTitle(title, subtitle);
        } catch (Exception e) {
            // Fallback para versões antigas
            ChatStorage.sendRaw(player, title);
            ChatStorage.sendRaw(player, subtitle);
        }
    }

    /**
     * Abre o menu para o jogador
     */
    public static void open(HaumeaMC plugin, Player player) {
        // Verificar expiração antes de abrir
        plugin.getMultiplierManager().checkExpiration(player);

        MultiplierMenu menu = new MultiplierMenu(plugin, player);
        menu.open();
    }
}
