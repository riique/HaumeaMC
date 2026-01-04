package com.haumea.kitpvp.menu;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.managers.ServerSelectorManager;
import com.haumea.kitpvp.managers.ServerSelectorManager.ServerInfo;
import com.haumea.kitpvp.utils.ChatStorage;
import com.haumea.kitpvp.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Menu GUI para seleção de servidores no Lobby.
 * Exibe todos os servidores configurados com ícones personalizados
 * e contagem de jogadores online em tempo real.
 */
public class ServerSelectorMenu extends BaseMenu {

    private static final int MENU_SIZE = 27; // 3 linhas

    // Mapeamento de slots para IDs de servidores
    private final Map<Integer, String> slotToServer;

    public ServerSelectorMenu(HaumeaMC plugin, Player player) {
        super(plugin, player, "&8&lSELECIONAR SERVIDOR", MENU_SIZE);
        this.slotToServer = new HashMap<>();
    }

    @Override
    protected void setupItems() {
        slotToServer.clear();

        // Decoração de fundo (vidro cinza)
        fillEmpty(7); // Cinza claro

        // Adicionar servidores configurados
        ServerSelectorManager selectorManager = plugin.getServerSelectorManager();
        if (selectorManager != null) {
            for (Map.Entry<String, ServerInfo> entry : selectorManager.getServers().entrySet()) {
                ServerInfo server = entry.getValue();
                int slot = server.getSlot();

                if (slot >= 0 && slot < size) {
                    setItem(slot, createServerItem(server, selectorManager), (p, c) -> {
                        p.closeInventory();
                        selectorManager.sendToServer(p, server.getId());
                    });
                    slotToServer.put(slot, server.getId());
                }
            }
        }

        // Item de informação no centro da linha inferior
        setItem(22, createInfoItem());
    }

    /**
     * Cria um item representando um servidor com contagem de jogadores
     */
    private ItemStack createServerItem(ServerInfo server, ServerSelectorManager selectorManager) {
        List<String> lore = new ArrayList<>();
        lore.add("");

        // Descrição do servidor
        if (server.getDescription() != null && !server.getDescription().isEmpty()) {
            for (String line : server.getDescription().split("\n")) {
                lore.add(ChatStorage.colorize(line));
            }
            lore.add("");
        }

        // Contagem de jogadores online
        int playerCount = selectorManager.getPlayerCount(server.getId());
        String onlineLine = ChatStorage.getMessage("lobby.server-selector.online-players");
        if (onlineLine == null || onlineLine.isEmpty()) {
            onlineLine = "&a{online} jogador(es) online";
        }
        onlineLine = onlineLine.replace("{online}", String.valueOf(playerCount));
        lore.add(ChatStorage.colorize(onlineLine));
        lore.add("");

        // Ação
        lore.add(ChatStorage.colorize(server.getAction()));

        return new ItemBuilder(server.getMaterial(), 1, (short) server.getData())
                .name(ChatStorage.colorize(server.getDisplayName()))
                .lore(lore)
                .build();
    }

    /**
     * Cria o item de informação
     */
    private ItemStack createInfoItem() {
        // Obter contagem total de jogadores
        int totalPlayers = 0;
        ServerSelectorManager selectorManager = plugin.getServerSelectorManager();
        if (selectorManager != null) {
            for (Integer count : selectorManager.getPlayerCounts().values()) {
                totalPlayers += count;
            }
        }

        return new ItemBuilder(Material.BOOK)
                .name("&e&lINFORMAÇÕES")
                .lore(
                        "",
                        "&7Clique em um servidor para entrar!",
                        "",
                        "&7Você está no: &a&lLOBBY",
                        "",
                        "&7Total na network: &e" + totalPlayers + " jogadores")
                .build();
    }

    @Override
    public void onClick(Player player, int slot, ItemStack item, ClickType clickType) {
        // O handler já foi registrado via setItem(), então o BaseMenu cuida disso
        super.onClick(player, slot, item, clickType);
    }
}
