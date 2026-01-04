package com.haumea.kitpvp.listeners;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.managers.FPSWarpManager;
import com.haumea.kitpvp.models.Warp;
import com.haumea.kitpvp.utils.ChatStorage;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * Listener para sistema de proteção de spawn em warps FPS.
 * 
 * Funcionalidades:
 * - Detecta quando jogador sai do raio de proteção
 * - Bloqueia dano a jogadores protegidos
 * - Remove proteção se jogador protegido atacar
 * - Impede dano entre jogadores de warps diferentes
 * - Processa clique no item de voltar ao spawn
 * 
 * @author HaumeaMC
 */
public class FPSWarpListener implements Listener {

    private final HaumeaMC plugin;
    private final FPSWarpManager fpsManager;

    public FPSWarpListener(HaumeaMC plugin) {
        this.plugin = plugin;
        this.fpsManager = plugin.getFPSWarpManager();
    }

    // ==================== MOVIMENTO ====================

    /**
     * Detecta quando um jogador protegido sai do raio de proteção.
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        // Otimização: ignorar movimentos que não mudam de bloco
        Location from = event.getFrom();
        Location to = event.getTo();

        if (to == null)
            return;
        if (from.getBlockX() == to.getBlockX() &&
                from.getBlockY() == to.getBlockY() &&
                from.getBlockZ() == to.getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();

        // Verificar se jogador está protegido
        if (fpsManager == null || !fpsManager.isProtected(player)) {
            return;
        }

        // Verificar se saiu do raio de proteção
        if (!fpsManager.isWithinProtectionRadius(player)) {
            // Remover proteção e dar kit
            fpsManager.unprotect(player);
        }
    }

    // ==================== DANO ====================

    /**
     * Bloqueia dano a jogadores protegidos.
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player victim = (Player) event.getEntity();

        // Bloquear dano se jogador está protegido
        if (fpsManager != null && fpsManager.isProtected(victim)) {
            event.setCancelled(true);
        }
    }

    /**
     * Gerencia dano entre jogadores.
     * - Jogadores protegidos não recebem dano
     * - Jogador protegido que ataca perde proteção imediatamente
     * - Jogadores de warps diferentes não podem se atacar
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerDamagePlayer(EntityDamageByEntityEvent event) {
        // Verificar se é PvP
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        if (!(event.getDamager() instanceof Player)) {
            return;
        }

        Player victim = (Player) event.getEntity();
        Player attacker = (Player) event.getDamager();

        if (fpsManager == null)
            return;

        // Caso 1: Vítima está protegida - cancelar dano
        if (fpsManager.isProtected(victim)) {
            event.setCancelled(true);
            ChatStorage.send(attacker, "warp.target-protected");
            return;
        }

        // Caso 2: Atacante está protegido e vítima não - atacante perde proteção
        if (fpsManager.isProtected(attacker) && !fpsManager.isProtected(victim)) {
            fpsManager.unprotect(attacker);
            // Não cancela o dano - o ataque continua
        }

        // Caso 3: Verificar warps diferentes (APENAS se pelo menos um está em warp
        // gerenciada)
        String attackerWarp = fpsManager.getPlayerWarp(attacker);
        String victimWarp = fpsManager.getPlayerWarp(victim);

        // Se ambos estão no spawn global (sem warp gerenciada), permitir dano
        // normalmente
        if (attackerWarp == null && victimWarp == null) {
            return; // Deixar outros listeners processarem
        }

        // Se estão em warps diferentes, cancelar dano
        if (!fpsManager.areInSameWarp(attacker, victim)) {
            event.setCancelled(true);
            return;
        }
    }

    // ==================== ITEM DE SPAWN ====================

    /**
     * Processa clique no item de voltar ao spawn (cama).
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR &&
                event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = player.getItemInHand();

        if (item == null || item.getType() != Material.BED) {
            return;
        }

        // Verificar se é o item de voltar ao spawn
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            String displayName = item.getItemMeta().getDisplayName();
            if (displayName.contains("Voltar ao Spawn")) {
                event.setCancelled(true);

                // Verificar se está em combate
                if (plugin.getStateManager() != null && plugin.getStateManager().isInCombat(player)) {
                    ChatStorage.send(player, "warp.in-combat");
                    return;
                }

                // Executar comando /spawn
                player.performCommand("spawn");
            }
        }
    }

    // ==================== LOGOUT ====================

    /**
     * Limpa dados do jogador ao sair.
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (fpsManager != null) {
            fpsManager.onPlayerQuit(event.getPlayer());
        }
    }
}
