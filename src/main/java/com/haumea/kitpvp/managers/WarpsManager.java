package com.haumea.kitpvp.managers;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.database.MongoWarpRepository;
import com.haumea.kitpvp.models.Warp;
import org.bukkit.Location;

import java.util.Collection;
import java.util.Set;

/**
 * Gerenciador de Warps do servidor HaumeaMC.
 * Responsável por salvar, carregar e gerenciar todas as warps.
 * 
 * Usa MongoDB para persistência.
 * 
 * @author HaumeaMC
 */
public class WarpsManager {

    private final HaumeaMC plugin;
    private MongoWarpRepository repository;

    public WarpsManager(HaumeaMC plugin) {
        this.plugin = plugin;

        // Inicializar repositório MongoDB
        initRepository();
    }

    /**
     * Inicializa o repositório MongoDB
     */
    private void initRepository() {
        if (plugin.getMongoManager() != null && plugin.getMongoManager().isConnected()) {
            this.repository = new MongoWarpRepository(plugin, plugin.getMongoManager());
            plugin.getLogger().info("[Warps] MongoDB repository inicializado.");
        } else {
            plugin.getLogger().warning("[Warps] MongoDB não disponível! Warps não serão persistidas.");
        }
    }

    /**
     * Cria ou atualiza uma warp.
     * 
     * @param name     Nome da warp
     * @param location Localização da warp
     * @return A warp criada/atualizada
     */
    public Warp setWarp(String name, Location location) {
        return setWarp(name, location, 0.0);
    }

    /**
     * Cria ou atualiza uma warp com raio de proteção.
     * 
     * @param name     Nome da warp
     * @param location Localização da warp
     * @param radius   Raio da área de proteção (0 = sem proteção)
     * @return A warp criada/atualizada
     */
    public Warp setWarp(String name, Location location, double radius) {
        Warp warp = new Warp(name, location, radius);

        if (repository != null) {
            repository.saveWarpAsync(warp);
        }

        return warp;
    }

    /**
     * Remove uma warp.
     * 
     * @param name Nome da warp
     * @return true se a warp foi removida, false se não existia
     */
    public boolean deleteWarp(String name) {
        if (repository != null) {
            return repository.deleteWarp(name);
        }
        return false;
    }

    /**
     * Obtém uma warp pelo nome.
     * 
     * @param name Nome da warp
     * @return A warp, ou null se não existir
     */
    public Warp getWarp(String name) {
        if (repository == null)
            return null;
        return repository.getWarp(name);
    }

    /**
     * Verifica se uma warp existe.
     * 
     * @param name Nome da warp
     * @return true se existe
     */
    public boolean warpExists(String name) {
        if (repository == null)
            return false;
        return repository.warpExists(name);
    }

    /**
     * Obtém todas as warps.
     * 
     * @return Coleção de todas as warps
     */
    public Collection<Warp> getAllWarps() {
        if (repository == null)
            return java.util.Collections.emptyList();
        return repository.getAllWarps();
    }

    /**
     * Obtém todos os nomes de warps.
     * 
     * @return Set com os nomes das warps
     */
    public Set<String> getWarpNames() {
        if (repository == null)
            return java.util.Collections.emptySet();
        return repository.getWarpNames();
    }

    /**
     * Obtém a quantidade de warps.
     * 
     * @return Número de warps
     */
    public int getWarpCount() {
        if (repository == null)
            return 0;
        return repository.getWarpCount();
    }

    /**
     * Recarrega as warps do MongoDB.
     */
    public void reload() {
        if (repository != null) {
            repository.loadAllWarps();
        }
    }
}
