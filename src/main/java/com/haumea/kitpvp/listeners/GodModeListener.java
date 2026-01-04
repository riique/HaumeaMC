package com.haumea.kitpvp.listeners;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.profile.PlayerProfile;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * Listener para o sistema de God Mode.
 * 
 * Cancela TODO tipo de dano para jogadores em god mode:
 * - PvP (dano de outros jogadores)
 * - PvE (dano de mobs)
 * - Dano ambiental (lava, fogo, cacto, afogamento)
 * - Queda
 * - Void
 * - Qualquer outra fonte de dano
 * 
 * @author HaumeaMC
 */
public class GodModeListener implements Listener {

    private final HaumeaMC plugin;

    public GodModeListener(HaumeaMC plugin) {
        this.plugin = plugin;
    }

    /**
     * Intercepta TODO tipo de dano em entidades.
     * Se o jogador estiver em god mode, cancela o dano completamente.
     * 
     * Prioridade HIGHEST para garantir que este listener execute depois
     * de outros que podem modificar o dano, mas antes de MONITOR.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        // Verificar se a entidade danificada é um jogador
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();
        PlayerProfile profile = plugin.getProfileManager().getProfile(player);

        // Se o jogador está em god mode, cancelar TODO dano
        if (profile != null && profile.isGodMode()) {
            event.setCancelled(true);

            // Resetar fire ticks se estava pegando fogo
            if (player.getFireTicks() > 0) {
                player.setFireTicks(0);
            }
        }
    }

    /**
     * Listener adicional para dano de entidades (PvP/PvE).
     * Executado com prioridade mais baixa para interceptar antes do evento
     * genérico.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // Verificar se a entidade danificada é um jogador
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();
        PlayerProfile profile = plugin.getProfileManager().getProfile(player);

        // Se o jogador está em god mode, cancelar dano
        if (profile != null && profile.isGodMode()) {
            event.setCancelled(true);
        }
    }
}
