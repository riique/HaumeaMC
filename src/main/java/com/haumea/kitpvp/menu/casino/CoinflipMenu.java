package com.haumea.kitpvp.menu.casino;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.managers.CasinoManager;
import com.haumea.kitpvp.menu.BaseMenu;
import com.haumea.kitpvp.models.casino.CoinflipRequest;
import com.haumea.kitpvp.utils.ChatStorage;
import com.haumea.kitpvp.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Menu de Coinflip.
 * Lista coinflips disponíveis e permite criar novos.
 * 
 * @author HaumeaMC
 */
public class CoinflipMenu extends BaseMenu {

    private final CasinoManager casinoManager;
    private int currentPage;
    private static final int ITEMS_PER_PAGE = 21;

    public CoinflipMenu(HaumeaMC plugin, Player player) {
        super(plugin, player, "&e&l🪙 COINFLIP", 54);
        this.casinoManager = plugin.getCasinoManager();
        this.currentPage = 0;
    }

    @Override
    protected void setupItems() {
        // Preencher bordas
        fillBorders(4); // Amarelo

        // Slot 2: Criar Coinflip
        ItemStack createItem = new ItemBuilder(Material.EMERALD)
                .name("&a&lCRIAR COINFLIP")
                .lore(
                        "",
                        "&7Crie um coinflip e",
                        "&7aguarde um oponente!",
                        "",
                        "&eClique para criar")
                .glow()
                .build();
        setItem(2, createItem, (p, c) -> {
            playClickSound();
            new CoinflipCreateMenu(plugin, player).open();
        });

        // Slot 4: Saldo
        long coins = plugin.getStatsManager().getMoney(player);
        ItemStack balanceItem = ItemBuilder.playerHead(player.getName())
                .name("&6&lSEU SALDO")
                .lore(
                        "",
                        "&fCoins: &e" + casinoManager.formatCoins(coins))
                .build();
        setItem(4, balanceItem);

        // Slot 6: Atualizar lista
        ItemStack refreshItem = new ItemBuilder(Material.BOOK)
                .name("&e&lATUALIZAR LISTA")
                .lore(
                        "",
                        "&7Clique para atualizar",
                        "&7a lista de coinflips")
                .build();
        setItem(6, refreshItem, (p, c) -> {
            playClickSound();
            refresh();
        });

        // Lista de coinflips (slots 19-43, exceto bordas)
        List<CoinflipRequest> available = casinoManager.getAvailableCoinflips();
        int[] coinflipSlots = {
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34
        };

        int startIndex = currentPage * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, available.size());

        // Limpar slots de coinflip primeiro
        for (int slot : coinflipSlots) {
            inventory.setItem(slot, null);
        }

        if (available.isEmpty()) {
            // Nenhum coinflip disponível
            ItemStack emptyItem = new ItemBuilder(Material.BARRIER)
                    .name("&c&lNenhum coinflip disponível")
                    .lore(
                            "",
                            "&7Seja o primeiro a criar!",
                            "&7Clique em &aCRIAR COINFLIP")
                    .build();
            setItem(22, emptyItem);
        } else {
            for (int i = startIndex; i < endIndex; i++) {
                int slotIndex = i - startIndex;
                if (slotIndex >= coinflipSlots.length)
                    break;

                CoinflipRequest request = available.get(i);
                int slot = coinflipSlots[slotIndex];

                boolean isOwn = request.getCreatorId().equals(player.getUniqueId());
                boolean canAfford = plugin.getStatsManager().getMoney(player) >= request.getAmount();

                ItemStack coinflipItem = ItemBuilder.playerHead(request.getCreatorName())
                        .name("&e" + request.getCreatorName() + " &8- &a"
                                + casinoManager.formatCoins(request.getAmount()) + " coins")
                        .lore(
                                "",
                                "&7Criado há: &f" + request.getTimeAgo(),
                                "",
                                isOwn ? "&c&lSeu coinflip"
                                        : (canAfford ? "&aClique para aceitar!" : "&cSaldo insuficiente"))
                        .build();

                setItem(slot, coinflipItem, (p, c) -> {
                    if (isOwn) {
                        // Cancelar próprio coinflip
                        if (casinoManager.cancelCoinflip(player)) {
                            playSuccessSound();
                            refresh();
                        }
                    } else if (canAfford) {
                        // Aceitar coinflip
                        playClickSound();
                        close();

                        if (casinoManager.acceptCoinflip(player, request)) {
                            // Coinflip executado - animação poderia ser adicionada aqui
                        }
                    } else {
                        playErrorSound();
                        ChatStorage.send(p, "casino.error.not-enough-coins");
                    }
                });
            }
        }

        // Navegação
        int totalPages = (int) Math.ceil((double) available.size() / ITEMS_PER_PAGE);

        // Página anterior
        if (currentPage > 0) {
            ItemStack prevPage = new ItemBuilder(Material.ARROW)
                    .name("&e◀ Página Anterior")
                    .lore("&7Página " + currentPage + " de " + Math.max(1, totalPages))
                    .build();
            setItem(48, prevPage, (p, c) -> {
                playClickSound();
                currentPage--;
                refresh();
            });
        }

        // Próxima página
        if (currentPage < totalPages - 1) {
            ItemStack nextPage = new ItemBuilder(Material.ARROW)
                    .name("&ePróxima Página ▶")
                    .lore("&7Página " + (currentPage + 2) + " de " + totalPages)
                    .build();
            setItem(50, nextPage, (p, c) -> {
                playClickSound();
                currentPage++;
                refresh();
            });
        }

        // Info
        ItemStack infoItem = new ItemBuilder(Material.PAPER)
                .name("&e&lCOMO FUNCIONA")
                .lore(
                        "",
                        "&71. Crie um coinflip com seu valor",
                        "&72. Aguarde alguém aceitar",
                        "&73. 50/50 de chance de ganhar!",
                        "",
                        "&cTaxa da casa: 5%")
                .build();
        setItem(40, infoItem);

        // Slot 45: Voltar
        setItem(45, createBackButton(), (p, c) -> {
            playClickSound();
            new CasinoMainMenu(plugin, player).open();
        });

        // Slot 53: Fechar
        setItem(53, createCloseButton(), (p, c) -> {
            playClickSound();
            close();
        });
    }
}
