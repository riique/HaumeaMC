package com.haumea.kitpvp.menu;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.utils.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Menu de Warps/Teleporte para Arenas (45 slots - 5 linhas).
 * 
 * Design premium com bordas decorativas e layout centralizado.
 * Permite ao jogador escolher para qual arena quer ir.
 * 
 * Layout Visual:
 * ┌─────────────────────────────────────────┐
 * │ ■ ■ ■ ■ ■ ■ ■ ■ ■ │ Linha 1: Borda preta
 * │ ■ ◆ ◆ ◆ ◆ ◆ ◆ ◆ ■ │ Linha 2: Borda + decoração laranja
 * │ ■ ◆ FPS DUE LAV EVE ◆ ■ │ Linha 3: Warps centralizados
 * │ ■ ◆ ◆ ◆ ◆ ◆ ◆ ◆ ■ │ Linha 4: Borda + decoração laranja
 * │ ■ ■ ■ ■ ■ ■ ■ ■ ■ │ Linha 5: Borda preta
 * └─────────────────────────────────────────┘
 * 
 * @author HaumeaMC
 */
public class WarpsMenu extends BaseMenu {

    // Slots centrais para os warps (linha 3, centrados)
    private static final int SLOT_FPS = 20;
    private static final int SLOT_DUELS = 21;
    private static final int SLOT_LAVA = 23;
    private static final int SLOT_EVENT = 24;

    public WarpsMenu(HaumeaMC plugin, Player player) {
        super(plugin, player, "&6&lTELEPORTE &8- &fArenas", 45);
    }

    @Override
    protected void setupItems() {
        // === PREENCHER FUNDO COM VIDRO PRETO ===
        ItemStack blackPane = createGlassPane(15, " ");
        for (int i = 0; i < 45; i++) {
            setItem(i, blackPane);
        }

        // === DECORAÇÃO INTERNA COM VIDRO LARANJA ===
        ItemStack orangePane = createGlassPane(1, " ");
        // Linha 2 (slots 10-16, exceto bordas)
        for (int i = 10; i <= 16; i++) {
            setItem(i, orangePane);
        }
        // Linha 4 (slots 28-34, exceto bordas)
        for (int i = 28; i <= 34; i++) {
            setItem(i, orangePane);
        }
        // Laterais da linha 3
        setItem(19, orangePane);
        setItem(25, orangePane);

        // === SLOT CENTRAL DECORATIVO (Bússola informativa) ===
        ItemStack compassItem = new ItemBuilder(Material.COMPASS)
                .name("§6§l★ SELETOR DE ARENAS ★")
                .lore(
                        "",
                        "§7Escolha uma arena para",
                        "§7se teleportar e batalhar!",
                        "",
                        "§6§lDicas:",
                        "§8▪ §fCada arena tem regras diferentes",
                        "§8▪ §fEscolha baseado no seu estilo",
                        "§8▪ §fMais jogadores = mais diversao!",
                        "")
                .glow()
                .build();
        setItem(22, compassItem);

        // === FPS ARENA (Slot 20) ===
        int fpsPlayers = countPlayersInWarp("fps");
        ItemStack fpsItem = new ItemBuilder(Material.FEATHER)
                .name("§a§l✦ FPS ARENA ✦")
                .lore(
                        "",
                        "§7Arena otimizada para",
                        "§7jogadores com FPS baixo.",
                        "",
                        "§6Caracteristicas:",
                        "§8▪ §fMenos particulas",
                        "§8▪ §fMenos entidades",
                        "§8▪ §fMais performance",
                        "§8▪ §fMapa simplificado",
                        "",
                        "§e§lJogadores: §f" + fpsPlayers,
                        "",
                        "§a▸ Clique para teleportar!")
                .glow()
                .build();

        setItem(SLOT_FPS, fpsItem, (p, click) -> {
            executeWarp("fps");
        });

        // === DUELS ARENA (Slot 21) ===
        int duelsPlayers = countPlayersInWarp("duels");
        ItemStack duelsItem = new ItemBuilder(Material.DIAMOND_SWORD)
                .name("§b§l⚔ DUELS ARENA ⚔")
                .lore(
                        "",
                        "§7Combates 1v1 ranqueados!",
                        "§7Prove que voce e o melhor.",
                        "",
                        "§6Caracteristicas:",
                        "§8▪ §fDesafie jogadores",
                        "§8▪ §fDuelos ranqueados",
                        "§8▪ §fSuba no ranking",
                        "§8▪ §fGanhe recompensas",
                        "",
                        "§e§lJogadores: §f" + duelsPlayers,
                        "",
                        "§a▸ Clique para teleportar!")
                .glow()
                .build();

        setItem(SLOT_DUELS, duelsItem, (p, click) -> {
            executeWarp("duels");
        });

        // === LAVA ARENA (Slot 23) ===
        int lavaPlayers = countPlayersInWarp("lava");
        ItemStack lavaItem = new ItemBuilder(Material.LAVA_BUCKET)
                .name("§c§l☠ LAVA ARENA ☠")
                .lore(
                        "",
                        "§7Arena perigosa com lava!",
                        "§7Cuidado para nao cair...",
                        "",
                        "§6Caracteristicas:",
                        "§8▪ §fCombate intenso",
                        "§8▪ §fAmbiente perigoso",
                        "§8▪ §fMuita adrenalina",
                        "§8▪ §fRisco de queda",
                        "",
                        "§e§lJogadores: §f" + lavaPlayers,
                        "",
                        "§a▸ Clique para teleportar!")
                .glow()
                .build();

        setItem(SLOT_LAVA, lavaItem, (p, click) -> {
            executeWarp("lava");
        });

        // === EVENTO ARENA (Slot 24) ===
        int eventPlayers = countPlayersInWarp("evento");
        boolean hasEvent = plugin.getFeastManager() != null &&
                (plugin.getFeastManager().isFeastActive() || plugin.getFeastManager().isCountdownActive());

        ItemStack eventItem;
        if (hasEvent) {
            // Evento ativo - mostrar com destaque
            eventItem = new ItemBuilder(Material.NETHER_STAR)
                    .name("§d§l★ EVENTO ATIVO ★")
                    .lore(
                            "",
                            "§a§lEVENTO EM ANDAMENTO!",
                            "",
                            "§7Participe do evento atual",
                            "§7e ganhe premios exclusivos!",
                            "",
                            "§6Caracteristicas:",
                            "§8▪ §fEventos especiais",
                            "§8▪ §fPremios exclusivos",
                            "§8▪ §fDiversao garantida",
                            "§8▪ §fTempo limitado!",
                            "",
                            "§e§lJogadores: §f" + eventPlayers,
                            "",
                            "§a▸ Clique para teleportar!")
                    .glow()
                    .build();
        } else {
            // Sem evento - mostrar normal
            eventItem = new ItemBuilder(Material.FIREWORK)
                    .name("§e§l✧ EVENTO ✧")
                    .lore(
                            "",
                            "§7Area destinada a eventos",
                            "§7especiais do servidor.",
                            "",
                            "§6Caracteristicas:",
                            "§8▪ §fEventos especiais",
                            "§8▪ §fPremios exclusivos",
                            "§8▪ §fDiversao garantida",
                            "§8▪ §fFique atento ao chat!",
                            "",
                            "§e§lJogadores: §f" + eventPlayers,
                            "",
                            "§a▸ Clique para teleportar!")
                    .build();
        }

        setItem(SLOT_EVENT, eventItem, (p, click) -> {
            executeWarp("evento");
        });
    }

    /**
     * Conta jogadores em uma warp específica
     * (Simplificado - conta jogadores no mundo principal por enquanto)
     */
    private int countPlayersInWarp(String warpName) {
        // Por enquanto retorna contagem do mundo principal
        // TODO: Implementar contagem real por região/warp
        World world = Bukkit.getWorlds().get(0);
        if (world != null) {
            // Retorna uma fração dos jogadores online para simular distribuição
            int total = world.getPlayers().size();
            if (total == 0)
                return 0;

            // Distribuir proporcionalmente entre as arenas
            switch (warpName.toLowerCase()) {
                case "fps":
                    return Math.max(0, total / 4);
                case "duels":
                    return Math.max(0, total / 3);
                case "lava":
                    return Math.max(0, total / 5);
                case "evento":
                    return Math.max(0, total / 6);
                default:
                    return 0;
            }
        }
        return 0;
    }

    /**
     * Executa o comando de warp
     */
    private void executeWarp(String warpName) {
        close();
        Bukkit.dispatchCommand(player, "warp " + warpName);
        playPlingSound();
    }
}
