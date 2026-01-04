package com.haumea.kitpvp.tablist;

import com.haumea.kitpvp.HaumeaMC;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Task de atualização da TabList.
 * 
 * Esta task roda assincronamente para não impactar a performance
 * do servidor, atualizando o header e footer da TabList de todos
 * os jogadores online periodicamente (a cada 10 segundos).
 * 
 * Características:
 * - Execução assíncrona para mínimo impacto no TPS
 * - Atualização do contador de jogadores online
 * 
 * @author HaumeaMC
 */
public class TabTask extends BukkitRunnable {

    private final HaumeaMC plugin;
    private final TabManager tabManager;

    /**
     * Construtor da task
     * 
     * @param plugin     Instância do plugin
     * @param tabManager Gerenciador da TabList
     */
    public TabTask(HaumeaMC plugin, TabManager tabManager) {
        this.plugin = plugin;
        this.tabManager = tabManager;
    }

    /**
     * Método executado a cada ciclo da task (10 segundos)
     * 
     * Atualiza a TabList de todos os jogadores online
     * com as informações atualizadas (jogadores online, etc)
     */
    @Override
    public void run() {
        // Verificar se há jogadores online
        if (Bukkit.getOnlinePlayers().isEmpty()) {
            return;
        }

        // Atualizar todos os jogadores no thread principal
        // (necessário para interagir com packets/API do Bukkit)
        Bukkit.getScheduler().runTask(plugin, () -> {
            tabManager.updateAllPlayers();
        });
    }
}
