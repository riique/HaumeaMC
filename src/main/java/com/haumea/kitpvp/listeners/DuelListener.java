package com.haumea.kitpvp.listeners;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.managers.DuelManager;
import com.haumea.kitpvp.managers.PlayerStateManager.PlayerState;
import com.haumea.kitpvp.menu.duel.DuelSettingsMenu;
import com.haumea.kitpvp.models.DuelMatch;
import com.haumea.kitpvp.models.DuelSettings;
import com.haumea.kitpvp.models.Warp;
import com.haumea.kitpvp.utils.ChatStorage;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;

/**
 * Listener para eventos relacionados ao sistema de duelos 1v1.
 * 
 * Gerencia:
 * - Morte de jogadores em duelo
 * - Desconexão durante duelo
 * - Interação com itens do lobby de 1v1
 * - Proteção de jogadores em duelo
 * - Teleporte para o lobby de 1v1
 * - Contagem de sopas
 * 
 * @author HaumeaMC
 */
public class DuelListener implements Listener {

    private final HaumeaMC plugin;

    public DuelListener(HaumeaMC plugin) {
        this.plugin = plugin;
    }

    // ==================== MORTE EM DUELO ====================

    /**
     * Processa a morte de um jogador em duelo.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        DuelManager duelManager = plugin.getDuelManager();

        if (duelManager == null)
            return;

        DuelMatch match = duelManager.getPlayerMatch(player);
        if (match == null || !match.isFighting())
            return;

        // Cancelar drops e mensagem de morte padrão
        event.getDrops().clear();
        event.setDeathMessage(null);
        event.setKeepLevel(true);
        event.setDroppedExp(0);

        // Obter o vencedor
        Player winner = match.getOpponentPlayer(player);

        // Finalizar duelo
        duelManager.endDuel(winner, player);
    }

    // ==================== ENTRADA DE JOGADOR ====================

    /**
     * Esconde duelistas ativos de jogadores que entram no servidor.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player joiningPlayer = event.getPlayer();
        DuelManager duelManager = plugin.getDuelManager();

        if (duelManager == null)
            return;

        // Esconder duelistas ativos do novo jogador
        duelManager.hideActiveDuelistsFrom(joiningPlayer);
    }

    // ==================== DESCONEXÃO ====================

    /**
     * Trata desconexão durante duelo.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        handlePlayerLeave(player);
    }

    /**
     * Trata kick durante duelo.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerKick(PlayerKickEvent event) {
        Player player = event.getPlayer();
        handlePlayerLeave(player);
    }

    /**
     * Processa saída de jogador.
     */
    private void handlePlayerLeave(Player player) {
        DuelManager duelManager = plugin.getDuelManager();
        if (duelManager == null)
            return;

        // Remover da fila se estiver
        if (duelManager.isInQueue(player)) {
            duelManager.leaveQueue(player);
        }

        // Finalizar duelo se estiver em um
        DuelMatch match = duelManager.getPlayerMatch(player);
        if (match != null && !match.isEnded()) {
            duelManager.handlePlayerLeave(match, player.getUniqueId());
        }
    }

    // ==================== DANO EM DUELO ====================

    /**
     * Protege jogadores em countdown ou que não estão no duelo.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player))
            return;

        Player victim = (Player) event.getEntity();
        DuelManager duelManager = plugin.getDuelManager();

        if (duelManager == null)
            return;

        DuelMatch victimMatch = duelManager.getPlayerMatch(victim);

        // Se a vítima está em duelo
        if (victimMatch != null) {
            // Se está em countdown, cancelar dano
            if (victimMatch.getState() == DuelMatch.MatchState.COUNTDOWN) {
                event.setCancelled(true);
                return;
            }

            // Verificar se o atacante é do mesmo duelo
            if (event.getDamager() instanceof Player) {
                Player attacker = (Player) event.getDamager();

                if (!victimMatch.isInMatch(attacker)) {
                    // Atacante não está no duelo - cancelar
                    event.setCancelled(true);
                    ChatStorage.sendCustom(attacker, "§cVocê não pode interferir em duelos!");
                }
            }
        }

        // Impedir que jogadores em duelo ataquem jogadores fora de duelo
        if (event.getDamager() instanceof Player) {
            Player attacker = (Player) event.getDamager();
            DuelMatch attackerMatch = duelManager.getPlayerMatch(attacker);

            if (attackerMatch != null
                    && (victimMatch == null || !victimMatch.getMatchId().equals(attackerMatch.getMatchId()))) {
                event.setCancelled(true);
            }
        }
    }

    // ==================== BLOQUEIO DE TELEPORTE ====================

    /**
     * Bloqueia teleporte durante duelo E detecta entrada/saída do lobby via
     * teleporte.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        DuelManager duelManager = plugin.getDuelManager();

        if (duelManager == null)
            return;

        DuelMatch match = duelManager.getPlayerMatch(player);

        // Se está em duelo ativo, verificar se pode teleportar
        if (match != null) {
            // Permitir apenas teleportes do sistema de duelo (mesma arena)
            if (match.getState() == DuelMatch.MatchState.FIGHTING) {
                Location from = event.getFrom();
                Location to = event.getTo();

                // Se está tentando teleportar para fora da arena, cancelar
                Location spawn1 = match.getArena().getSpawn1();
                Location spawn2 = match.getArena().getSpawn2();

                if (spawn1 != null && spawn2 != null) {
                    double maxDistance = spawn1.distance(spawn2) + 50; // Margem de segurança

                    // Verificar se o destino está muito longe da arena
                    double distTo1 = to.distance(spawn1);
                    double distTo2 = to.distance(spawn2);

                    if (distTo1 > maxDistance && distTo2 > maxDistance) {
                        event.setCancelled(true);
                        ChatStorage.sendCustom(player, "§cVocê não pode teleportar durante um duelo!");
                    }
                }
            }
            return; // Não processar entrada/saída se está em duelo
        }

        // Detectar entrada/saída do lobby de 1v1 via teleporte (para warps)
        Location from = event.getFrom();
        Location to = event.getTo();

        boolean wasInLobby = isInDuelLobby(from);
        boolean isInLobby = isInDuelLobby(to);

        if (!wasInLobby && isInLobby) {
            // Entrou no lobby de 1v1 via teleporte - dar itens com delay para garantir
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline() && isInDuelLobby(player)) {
                    onEnterDuelLobby(player);
                }
            }, 2L);
        } else if (wasInLobby && !isInLobby) {
            // Saiu do lobby de 1v1 via teleporte
            onLeaveDuelLobby(player);
        }
    }

    // ==================== ITENS DO LOBBY DE 1v1 ====================

    /**
     * Processa interação com itens do lobby de 1v1.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR &&
                event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;

        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null)
            return;

        DuelManager duelManager = plugin.getDuelManager();
        if (duelManager == null)
            return;

        // Verificar se está no lobby de 1v1
        if (!isInDuelLobby(player))
            return;

        // Olho de Ender - Entrar/Sair da fila
        if (item.getType() == Material.EYE_OF_ENDER) {
            event.setCancelled(true);

            if (duelManager.isInQueue(player)) {
                duelManager.leaveQueue(player);
            } else {
                new DuelSettingsMenu(plugin, player, new DuelSettings(), null).open();
            }

            // Atualizar itens
            duelManager.giveDuelLobbyItems(player);
            return;
        }

        // Cama - Voltar ao spawn principal
        if (item.getType() == Material.BED) {
            event.setCancelled(true);

            // Verificar se está na fila
            if (duelManager.isInQueue(player)) {
                duelManager.leaveQueue(player);
            }

            // Teleportar ao spawn
            Warp spawn = plugin.getWarpsManager().getWarp("spawn");
            if (spawn != null && spawn.isValid()) {
                player.teleport(spawn.toLocation());
                plugin.getArenaItemsHandler().giveLobbyItems(player);
                ChatStorage.sendCustom(player, "§aTeleportado ao spawn!");
            } else {
                ChatStorage.sendCustom(player, "§cSpawn não configurado.");
            }
            return;
        }
    }

    /**
     * Processa clique em jogador com espada (desafio).
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        Entity rightClicked = event.getRightClicked();

        if (!(rightClicked instanceof Player))
            return;

        ItemStack item = player.getItemInHand();
        if (item == null || item.getType() != Material.DIAMOND_SWORD)
            return;

        DuelManager duelManager = plugin.getDuelManager();
        if (duelManager == null)
            return;

        // Verificar se está no lobby de 1v1
        if (!isInDuelLobby(player))
            return;

        event.setCancelled(true);

        Player target = (Player) rightClicked;

        // Verificar se o alvo também está no lobby de 1v1
        if (!isInDuelLobby(target)) {
            ChatStorage.sendCustom(player, "§c" + target.getName() + " não está no lobby de 1v1!");
            return;
        }

        // Abrir menu de configuração e desafiar
        new DuelSettingsMenu(plugin, player, new DuelSettings(), target).open();
    }

    // ==================== CONTAGEM DE SOPAS ====================

    /**
     * Atualiza a contagem de sopas do jogador em duelo.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onItemConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        DuelManager duelManager = plugin.getDuelManager();

        if (duelManager == null)
            return;

        DuelMatch match = duelManager.getPlayerMatch(player);
        if (match == null || !match.isFighting())
            return;

        // Contar sopas restantes após consumir
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            int soups = countSoups(player);
            match.setSoups(player.getUniqueId(), soups);
        }, 1L);
    }

    /**
     * Conta as sopas no inventário do jogador.
     */
    private int countSoups(Player player) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.MUSHROOM_SOUP) {
                count += item.getAmount();
            }
        }
        return count;
    }

    // ==================== ENTRADA NO LOBBY DE 1v1 ====================

    /**
     * Detecta entrada no lobby de 1v1 e dá os itens apropriados.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerMove(PlayerMoveEvent event) {
        // Otimização: só processar se mudou de bloco
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
                event.getFrom().getBlockY() == event.getTo().getBlockY() &&
                event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();

        // Verificar se entrou/saiu do lobby de 1v1
        boolean wasInLobby = isInDuelLobby(event.getFrom());
        boolean isInLobby = isInDuelLobby(event.getTo());

        if (!wasInLobby && isInLobby) {
            // Entrou no lobby de 1v1
            onEnterDuelLobby(player);
        } else if (wasInLobby && !isInLobby) {
            // Saiu do lobby de 1v1
            onLeaveDuelLobby(player);
        }
    }

    /**
     * Chamado quando o jogador entra no lobby de 1v1.
     */
    private void onEnterDuelLobby(Player player) {
        DuelManager duelManager = plugin.getDuelManager();
        if (duelManager == null)
            return;

        // Verificar se não está em duelo
        if (duelManager.isInDuel(player))
            return;

        // Dar itens de lobby de 1v1
        duelManager.giveDuelLobbyItems(player);

        // Adicionar estado
        plugin.getStateManager().setState(player, PlayerState.SPECTATING); // Usar estado temporário

        ChatStorage.sendCustom(player, "§eVocê entrou no lobby de §6§l1v1§e!");
    }

    /**
     * Chamado quando o jogador sai do lobby de 1v1.
     */
    private void onLeaveDuelLobby(Player player) {
        DuelManager duelManager = plugin.getDuelManager();
        if (duelManager == null)
            return;

        // Verificar se está em duelo (não remover itens se estiver)
        if (duelManager.isInDuel(player))
            return;

        // Remover da fila se estiver
        if (duelManager.isInQueue(player)) {
            duelManager.leaveQueue(player);
        }

        // Remover estado
        plugin.getStateManager().removeState(player, PlayerState.SPECTATING);
    }

    // ==================== UTILIDADES ====================

    /** Nomes possíveis para a warp do lobby de 1v1 */
    private static final String[] DUEL_LOBBY_WARP_NAMES = { "1v1", "duelos", "duel", "x1", "1x1", "duelo" };

    /**
     * Verifica se um jogador está no lobby de 1v1.
     */
    private boolean isInDuelLobby(Player player) {
        return isInDuelLobby(player.getLocation());
    }

    /**
     * Verifica se uma localização está no lobby de 1v1.
     * Procura por múltiplos nomes de warp possíveis.
     */
    private boolean isInDuelLobby(Location location) {
        if (location == null)
            return false;

        // Procurar por qualquer warp de duelo configurada
        Warp lobbyWarp = null;
        for (String warpName : DUEL_LOBBY_WARP_NAMES) {
            lobbyWarp = plugin.getWarpsManager().getWarp(warpName);
            if (lobbyWarp != null && lobbyWarp.isValid()) {
                break;
            }
        }

        if (lobbyWarp == null || !lobbyWarp.isValid())
            return false;

        Location lobbyLoc = lobbyWarp.toLocation();
        if (lobbyLoc == null)
            return false;

        // Verificar se está no mesmo mundo
        if (!location.getWorld().equals(lobbyLoc.getWorld()))
            return false;

        // Verificar distância (raio de 50 blocos para mais tolerância)
        double distance = location.distance(lobbyLoc);
        return distance <= 50;
    }

    /**
     * Respawn do jogador após morte em duelo.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        DuelManager duelManager = plugin.getDuelManager();

        if (duelManager == null)
            return;

        // Se o jogador estava em duelo, teleportar para o lobby de 1v1
        if (duelManager.isInDuel(player)) {
            Warp lobbyWarp = plugin.getWarpsManager().getWarp("1v1");
            if (lobbyWarp == null) {
                lobbyWarp = plugin.getWarpsManager().getWarp("spawn");
            }

            if (lobbyWarp != null && lobbyWarp.isValid()) {
                event.setRespawnLocation(lobbyWarp.toLocation());
            }
        }
    }

    // ==================== BLOQUEIO DE COMANDOS ====================

    /** Lista de comandos bloqueados durante duelo/fila */
    private static final String[] BLOCKED_COMMANDS = {
            "spawn", "warp", "lobby", "hub", "home", "tp", "tpa", "tpaccept",
            "back", "sethome", "kit", "warps", "kits"
    };

    /**
     * Bloqueia comandos de teleporte durante duelo ou na fila.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        DuelManager duelManager = plugin.getDuelManager();

        if (duelManager == null)
            return;

        // Verificar se está em duelo ou na fila
        boolean inDuel = duelManager.isInDuel(player);
        boolean inQueue = duelManager.isInQueue(player);

        if (!inDuel && !inQueue)
            return;

        // Extrair o comando (sem a barra)
        String message = event.getMessage().toLowerCase().substring(1);
        String baseCommand = message.split(" ")[0];

        // Verificar se é um comando bloqueado
        for (String blocked : BLOCKED_COMMANDS) {
            if (baseCommand.equals(blocked)) {
                event.setCancelled(true);

                if (inDuel) {
                    ChatStorage.sendCustom(player, "§cVocê não pode usar este comando durante um duelo!");
                } else {
                    ChatStorage.sendCustom(player,
                            "§cVocê não pode usar este comando enquanto está na fila! Use §e/duel sair §cprimeiro.");
                }
                return;
            }
        }
    }
}
