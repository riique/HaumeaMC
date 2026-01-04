package com.haumea.kitpvp.listeners;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.profile.PlayerProfile;
import com.haumea.kitpvp.scoreboard.ScoreboardManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Listener para gerenciar scoreboards dos jogadores.
 * Cria scoreboard ao entrar (se preferência ativa) e remove ao sair.
 * 
 * @author HaumeaMC
 */
public class ScoreboardListener implements Listener {

    private final HaumeaMC plugin;
    private final ScoreboardManager scoreboardManager;

    public ScoreboardListener(HaumeaMC plugin) {
        this.plugin = plugin;
        this.scoreboardManager = plugin.getScoreboardManager();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Criar scoreboard com delay para garantir que o perfil e permissões foram
        // carregados
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                // Verificar preferência do jogador
                boolean scoreboardEnabled = isScoreboardEnabled(player);

                if (scoreboardEnabled) {
                    scoreboardManager.createBoard(player);
                }
            }
        }, 15L); // 15 ticks de delay (~750ms) - após o TabListener (10 ticks)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // Remover scoreboard
        scoreboardManager.removeBoard(player);
    }

    /**
     * Verifica se a scoreboard está ativada para o jogador
     * 
     * @param player Jogador
     * @return true se ativada (padrão: true)
     */
    private boolean isScoreboardEnabled(Player player) {
        PlayerProfile profile = plugin.getProfileManager().getProfile(player);
        if (profile == null) {
            return true; // Padrão: ativada
        }

        Object saved = profile.getData().getCustomData("scoreboard_enabled");
        if (saved instanceof Boolean) {
            return (Boolean) saved;
        }

        return true; // Padrão: ativada
    }
}
