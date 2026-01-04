package com.haumea.kitpvp.listeners;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.managers.EventManager;
import com.haumea.kitpvp.utils.ChatStorage;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Listener para eventos do sistema de Eventos do servidor.
 * 
 * Controla:
 * - Dano entre jogadores (PvP)
 * - Dano geral
 * - Colocação/quebra de blocos (Build)
 * - Morte de jogadores (eliminação)
 * - Saída de jogadores
 * - Comandos bloqueados durante evento
 * - Interação com item de voltar
 * - Fome
 * 
 * @author HaumeaMC
 */
public class EventListener implements Listener {

    private final HaumeaMC plugin;
    private final EventManager eventManager;

    public EventListener(HaumeaMC plugin) {
        this.plugin = plugin;
        this.eventManager = plugin.getEventManager();
    }

    // ==================== DANO ENTRE JOGADORES (PvP) ====================

    /**
     * Controla PvP entre jogadores no evento
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player victim = (Player) event.getEntity();
        Player attacker = getAttacker(event);

        if (attacker == null) {
            return;
        }

        boolean victimInEvent = eventManager.isParticipant(victim);
        boolean attackerInEvent = eventManager.isParticipant(attacker);

        // Se ambos estão no evento
        if (victimInEvent && attackerInEvent) {
            // Verificar se PvP está habilitado
            if (!eventManager.isPvPEnabled()) {
                event.setCancelled(true);
                return;
            }
            // PvP está habilitado, permitir dano
            return;
        }

        // Se apenas um está no evento, bloquear dano
        if (victimInEvent || attackerInEvent) {
            event.setCancelled(true);
            // Mensagem opcional de que não pode atacar jogadores fora/dentro do evento
        }
    }

    /**
     * Obtém o atacante de um evento de dano
     */
    private Player getAttacker(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();

        if (damager instanceof Player) {
            return (Player) damager;
        }

        if (damager instanceof Projectile) {
            Projectile projectile = (Projectile) damager;
            if (projectile.getShooter() instanceof Player) {
                return (Player) projectile.getShooter();
            }
        }

        return null;
    }

    // ==================== DANO GERAL ====================

    /**
     * Controla dano geral para jogadores no evento
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();

        if (!eventManager.isParticipant(player)) {
            return;
        }

        // Se dano está desabilitado, cancelar
        if (!eventManager.isDamageEnabled()) {
            event.setCancelled(true);
        }
    }

    // ==================== BUILD (COLOCAÇÃO DE BLOCOS) ====================

    /**
     * Controla colocação de blocos no evento
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();

        if (!eventManager.isParticipant(player)) {
            return;
        }

        // Se build está desabilitado, cancelar
        if (!eventManager.isBuildEnabled()) {
            event.setCancelled(true);
            ChatStorage.send(player, "event.build-disabled");
        }
    }

    /**
     * Controla quebra de blocos no evento
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();

        if (!eventManager.isParticipant(player)) {
            return;
        }

        // Se build está desabilitado, cancelar
        if (!eventManager.isBuildEnabled()) {
            event.setCancelled(true);
            ChatStorage.send(player, "event.build-disabled");
        }
    }

    // ==================== MORTE DE JOGADOR ====================

    /**
     * Trata morte de jogador no evento (eliminação)
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        if (!eventManager.isParticipant(player)) {
            return;
        }

        // Remover do evento
        eventManager.removePlayerFromEvent(player, false);

        // Broadcast de eliminação
        Player killer = player.getKiller();
        if (killer != null && eventManager.isParticipant(killer)) {
            // Morto por outro jogador do evento
            ChatStorage.sendRaw(player, ChatStorage.getMessage("event.eliminated-by",
                    "killer", killer.getName()));
        } else {
            // Morreu sozinho
            ChatStorage.sendRaw(player, ChatStorage.getMessage("event.eliminated"));
        }

        // Limpar drops (opcional - evitar itens do evento vazando)
        event.getDrops().clear();
        event.setDroppedExp(0);
    }

    /**
     * Teleporta jogador ao spawn após morte no evento
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        // A remoção do evento já teleporta ao spawn via removePlayerFromEvent
        // Este handler é para garantir que o respawn seja no spawn
        if (plugin.getWarpsManager().warpExists("spawn")) {
            org.bukkit.Location spawnLoc = plugin.getWarpsManager().getWarp("spawn").toLocation();
            if (spawnLoc != null) {
                event.setRespawnLocation(spawnLoc);
            }
        }
    }

    // ==================== SAÍDA DE JOGADOR ====================

    /**
     * Remove jogador do evento quando desconecta
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        if (!eventManager.isParticipant(player)) {
            return;
        }

        // Remover do evento
        eventManager.removePlayerFromEvent(player, false);
    }

    // ==================== COMANDOS BLOQUEADOS ====================

    /**
     * Bloqueia certos comandos durante evento iniciado
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();

        if (!eventManager.isParticipant(player)) {
            return;
        }

        // Só bloquear se o evento estiver em andamento
        if (!eventManager.getState().isRunning()) {
            return;
        }

        String command = event.getMessage().toLowerCase();

        // Lista de comandos bloqueados
        if (command.startsWith("/spawn") ||
                command.startsWith("/warp") ||
                command.startsWith("/tp") ||
                command.startsWith("/home") ||
                command.startsWith("/tpa") ||
                command.startsWith("/back")) {

            event.setCancelled(true);
            ChatStorage.send(player, "event.command-blocked");
        }

        // Permitir /evento sair sempre
        if (command.startsWith("/evento sair") ||
                command.startsWith("/event leave") ||
                command.startsWith("/evento leave")) {
            event.setCancelled(false);
        }
    }

    // ==================== ITEM DE VOLTAR ====================

    /**
     * Trata interação com item de voltar ao spawn
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || item.getType() != Material.BED) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return;
        }

        // Verificar se é o item de voltar
        if (!meta.getDisplayName().contains("Voltar")) {
            return;
        }

        event.setCancelled(true);

        // Verificar se está no evento
        if (!eventManager.isParticipant(player)) {
            return;
        }

        // Se o evento está em andamento, bloquear saída via item
        if (eventManager.getState().isRunning()) {
            ChatStorage.send(player, "event.cannot-leave-running");
            return;
        }

        // Remover do evento
        eventManager.removePlayerFromEvent(player, true);
    }

    // ==================== FOME ====================

    /**
     * Controla fome para jogadores no evento
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();

        if (!eventManager.isParticipant(player)) {
            return;
        }

        // Se fome está desabilitada, manter barra cheia
        if (!eventManager.isHungerEnabled()) {
            event.setFoodLevel(20);
            ((Player) event.getEntity()).setSaturation(20f);
        }
    }
}
