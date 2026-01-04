package com.haumea.kitpvp.listeners.base;

import com.haumea.kitpvp.HaumeaMC;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Central de registro de Listeners organizada por módulos.
 * 
 * Em vez de registrar listeners um por um na classe principal,
 * esta classe organiza todos os eventos por categoria/módulo.
 * 
 * Uso:
 * 1. Crie seu listener implementando Listener
 * 2. Registre via: ListenerRegistry.register("modulo", listener)
 * 3. Ou registre múltiplos: ListenerRegistry.registerModule("combate",
 * listener1, listener2)
 * 
 * @author HaumeaMC
 */
public class ListenerRegistry {

    private final HaumeaMC plugin;
    private final Map<String, List<Listener>> modules;
    private int totalListeners;

    /**
     * Construtor do ListenerRegistry
     * 
     * @param plugin Instância do plugin
     */
    public ListenerRegistry(HaumeaMC plugin) {
        this.plugin = plugin;
        this.modules = new HashMap<>();
        this.totalListeners = 0;
    }

    /**
     * Registra um listener em um módulo específico
     * 
     * @param moduleName Nome do módulo (ex: "combate", "chat", "tags")
     * @param listener   Instância do listener
     */
    public void register(String moduleName, Listener listener) {
        // Adicionar ao mapa de módulos
        modules.computeIfAbsent(moduleName.toLowerCase(), k -> new ArrayList<>()).add(listener);

        // Registrar no Bukkit
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);
        totalListeners++;

        plugin.getLogger().info("Listener registrado: " + listener.getClass().getSimpleName() +
                " [" + moduleName + "]");
    }

    /**
     * Registra múltiplos listeners em um módulo
     * 
     * @param moduleName Nome do módulo
     * @param listeners  Listeners a registrar
     */
    public void registerModule(String moduleName, Listener... listeners) {
        plugin.getLogger().info("Registrando módulo: " + moduleName);

        for (Listener listener : listeners) {
            register(moduleName, listener);
        }
    }

    /**
     * Registra um listener sem módulo específico (módulo "geral")
     * 
     * @param listener Instância do listener
     */
    public void register(Listener listener) {
        register("geral", listener);
    }

    /**
     * Obtém todos os listeners de um módulo
     * 
     * @param moduleName Nome do módulo
     * @return Lista de listeners do módulo
     */
    public List<Listener> getModuleListeners(String moduleName) {
        return modules.getOrDefault(moduleName.toLowerCase(), new ArrayList<>());
    }

    /**
     * Obtém todos os módulos registrados
     * 
     * @return Lista de nomes de módulos
     */
    public List<String> getModules() {
        return new ArrayList<>(modules.keySet());
    }

    /**
     * Obtém a quantidade de listeners em um módulo
     * 
     * @param moduleName Nome do módulo
     * @return Quantidade de listeners
     */
    public int getModuleSize(String moduleName) {
        List<Listener> listeners = modules.get(moduleName.toLowerCase());
        return listeners != null ? listeners.size() : 0;
    }

    /**
     * Obtém o total de listeners registrados
     * 
     * @return Total de listeners
     */
    public int getTotalListeners() {
        return totalListeners;
    }

    /**
     * Exibe um resumo dos módulos registrados no console
     */
    public void printSummary() {
        plugin.getLogger().info("=== Listeners Registrados ===");
        for (Map.Entry<String, List<Listener>> entry : modules.entrySet()) {
            StringBuilder sb = new StringBuilder();
            sb.append("  ").append(entry.getKey()).append(": ");

            List<String> names = new ArrayList<>();
            for (Listener listener : entry.getValue()) {
                names.add(listener.getClass().getSimpleName());
            }
            sb.append(String.join(", ", names));

            plugin.getLogger().info(sb.toString());
        }
        plugin.getLogger().info("Total: " + totalListeners + " listeners em " + modules.size() + " módulos");
    }
}
