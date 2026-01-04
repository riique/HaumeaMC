package com.haumea.kitpvp.menu.shop;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.menu.BaseMenu;
import com.haumea.kitpvp.menu.cosmetic.CosmeticMainMenu;
import com.haumea.kitpvp.profile.PlayerProfile;
import com.haumea.kitpvp.utils.ChatStorage;
import com.haumea.kitpvp.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Menu principal da Loja do HaumeaMC.
 * 
 * Este é o hub central da loja, onde o jogador pode acessar:
 * - Loja de Kits (aluguel/compra)
 * - Loja de Multiplicadores
 * - Reset de Estatísticas
 * 
 * Layout (54 slots):
 * - Linha 0: Header decorativo com título
 * - Linha 1-2: Informações do jogador
 * - Linha 3: Categorias da loja
 * - Linha 4-5: Borda decorativa
 * 
 * @author HaumeaMC
 */
public class ShopMainMenu extends BaseMenu {

    public ShopMainMenu(HaumeaMC plugin, Player player) {
        super(plugin, player, "&6&l✦ LOJA DO SERVIDOR ✦", 54);
    }

    @Override
    protected void setupItems() {
        PlayerProfile profile = plugin.getProfileManager().getProfile(player);
        long coins = profile != null ? profile.getCoins() : 0;

        // Fundo gradiente escuro
        fillWithGradient();

        // ============ HEADER DECORATIVO ============

        // Título central
        ItemStack titleItem = new ItemBuilder(Material.NETHER_STAR)
                .name("&6&l✦ LOJA DO SERVIDOR ✦")
                .lore(
                        "",
                        "&7Bem-vindo à loja do &6HaumeaMC&7!",
                        "",
                        "&7Aqui você pode adquirir:",
                        "&f• &eKits de habilidades",
                        "&f• &bMultiplicadores de coins",
                        "&f• &dCosméticos de kill",
                        "&f• &cReset de estatísticas",
                        "",
                        "&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬")
                .glow()
                .build();
        setItem(4, titleItem);

        // Decoração do header com vidro laranja
        ItemStack headerPane = createGlassPane(1, "&6✦");
        setItem(0, headerPane);
        setItem(1, headerPane);
        setItem(2, headerPane);
        setItem(3, headerPane);
        setItem(5, headerPane);
        setItem(6, headerPane);
        setItem(7, headerPane);
        setItem(8, headerPane);

        // ============ INFORMAÇÕES DO JOGADOR ============

        ItemStack balanceItem = new ItemBuilder(Material.GOLD_INGOT)
                .name("&e&l⛃ SEU SALDO")
                .lore(
                        "",
                        "&fMoedas: &e" + ChatStorage.formatNumber(coins) + " coins",
                        "",
                        "&7Use suas moedas para comprar",
                        "&7itens incríveis na loja!",
                        "")
                .glow()
                .build();
        setItem(13, balanceItem);

        // ============ CATEGORIAS DA LOJA ============

        // VIPs (NOVA CATEGORIA - destaque especial)
        ItemStack vipsItem = new ItemBuilder(Material.DIAMOND)
                .name("&d&l✦ VIPs SEMANAIS ✦")
                .lore(
                        "",
                        "&7Adquira VIPs usando suas",
                        "&emoedas do servidor&7!",
                        "",
                        "&6VIPs disponíveis:",
                        "&8▪ &a&lLIGHT &8- &e15.000 coins",
                        "&8▪ &6&lPREMIUM &8- &e35.000 coins",
                        "&8▪ &1&lBETA &8- &e60.000 coins",
                        "&8▪ &d&lULTRA &8- &e100.000 coins",
                        "",
                        "&6Duração: &f7 dias por compra",
                        "&7Tempos se &aacumulam&7!",
                        "",
                        "&a▶ Clique para acessar!")
                .glow()
                .build();
        setItem(20, vipsItem, (p, click) -> {
            new ShopVipMenu(plugin, player).open();
            playClickSound();
        });

        // Kits
        ItemStack kitsItem = new ItemBuilder(Material.DIAMOND_SWORD)
                .name("&e&l⚔ LOJA DE KITS")
                .lore(
                        "",
                        "&7Adquira kits de habilidades",
                        "&7especiais para batalhas!",
                        "",
                        "&6Opções de compra:",
                        "&8▪ &fAluguel por &e1 dia",
                        "&8▪ &fAluguel por &e1 semana",
                        "&8▪ &fAluguel por &e1 mês",
                        "&8▪ &fCompra &a&lPERMANENTE",
                        "",
                        "&a▶ Clique para acessar!")
                .hideAll()
                .build();
        setItem(22, kitsItem, (p, click) -> {
            new ShopKitsCategoryMenu(plugin, player).open();
            playClickSound();
        });

        // Multiplicadores
        ItemStack multipliersItem = new ItemBuilder(Material.GOLDEN_APPLE)
                .name("&b&l✧ MULTIPLICADORES")
                .lore(
                        "",
                        "&7Aumente seus ganhos de",
                        "&7coins por kill!",
                        "",
                        "&6Multiplicadores disponíveis:",
                        "&8▪ &7Básico &7(&fx1.5&7)",
                        "&8▪ &6Intermediário &7(&fx2.0&7)",
                        "&8▪ &bAvançado &7(&fx2.5&7)",
                        "&8▪ &aPremium &7(&fx3.0&7)",
                        "&8▪ &d✦ Máximo ✦ &7(&fx3.5&7)",
                        "",
                        "&a▶ Clique para acessar!")
                .build();
        setItem(24, multipliersItem, (p, click) -> {
            new ShopMultipliersMenu(plugin, player).open();
            playClickSound();
        });

        // Cosméticos
        ItemStack cosmeticsItem = new ItemBuilder(Material.BLAZE_POWDER)
                .name("&d&l✦ COSMÉTICOS")
                .lore(
                        "",
                        "&7Personalize sua experiência",
                        "&7de combate com cosméticos!",
                        "",
                        "&6Categorias disponíveis:",
                        "&8▪ &cEfeitos de Kill",
                        "&8▪ &eSons de Kill",
                        "&8▪ &bMensagens de Kill",
                        "",
                        "&7Cosméticos aparecem quando",
                        "&7você mata outro jogador!",
                        "",
                        "&a▶ Clique para acessar!")
                .glow()
                .build();
        setItem(30, cosmeticsItem, (p, click) -> {
            new CosmeticMainMenu(plugin, player).open();
            playClickSound();
        });

        // Reset de Stats
        ItemStack resetItem = new ItemBuilder(Material.ANVIL)
                .name("&c&l⚡ RESET DE STATS")
                .lore(
                        "",
                        "&7Recomece do zero!",
                        "&7Resete suas estatísticas",
                        "&7de combate.",
                        "",
                        "&6Opções de reset:",
                        "&8▪ &fReset de &cKills",
                        "&8▪ &fReset de &cDeaths",
                        "&8▪ &fReset de &cKDR",
                        "&8▪ &fReset &c&lCOMPLETO",
                        "",
                        "&a▶ Clique para acessar!")
                .build();
        setItem(32, resetItem, (p, click) -> {
            new ShopResetStatsMenu(plugin, player).open();
            playClickSound();
        });

        // ============ BOTÕES INFERIORES ============

        // Botão de fechar
        setClickableItem(49, createCloseButton(), this::close);

        // Informação
        ItemStack infoItem = new ItemBuilder(Material.BOOK)
                .name("&e&lINFORMAÇÕES")
                .lore(
                        "",
                        "&7Como ganhar moedas:",
                        "&8▪ &fMatando jogadores",
                        "&8▪ &fParticipando de eventos",
                        "&8▪ &fRecompensas diárias",
                        "",
                        "&7Dica: Use multiplicadores",
                        "&7para ganhar mais coins!",
                        "")
                .build();
        setItem(45, infoItem);
    }

    /**
     * Preenche o menu com um gradiente de cores escuras
     */
    private void fillWithGradient() {
        // Linha 1 (row 1) - Preto
        for (int i = 9; i < 18; i++) {
            setItem(i, createGlassPane(15));
        }

        // Linhas 2-3 - Cinza escuro
        for (int i = 18; i < 36; i++) {
            if (i % 9 == 0 || i % 9 == 8) {
                setItem(i, createGlassPane(7)); // Bordas cinza
            } else {
                setItem(i, createGlassPane(15)); // Centro preto
            }
        }

        // Linhas 4-5 - Borda decorativa
        for (int i = 36; i < 54; i++) {
            setItem(i, createGlassPane(15));
        }
    }

    /**
     * Método estático para abrir o menu
     */
    public static void open(HaumeaMC plugin, Player player) {
        new ShopMainMenu(plugin, player).open();
    }
}
