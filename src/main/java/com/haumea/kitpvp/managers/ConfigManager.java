package com.haumea.kitpvp.managers;

import com.haumea.kitpvp.HaumeaMC;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Gerenciador de configurações do plugin
 * Responsável por carregar e gerenciar configurações do config.yml
 * 
 * @author HaumeaMC
 */
public class ConfigManager {

    private final HaumeaMC plugin;
    private FileConfiguration config;

    // Configurações em cache
    private String prefixo;
    private boolean debug;

    public ConfigManager(HaumeaMC plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    /**
     * Carrega/recarrega as configurações do arquivo
     */
    public void loadConfig() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();

        // Carregar valores para cache
        this.prefixo = config.getString("configuracoes.prefixo", "&8[&6Haumea&8] ");
        this.debug = config.getBoolean("configuracoes.debug", false);
    }

    /**
     * Recarrega as configurações
     */
    public void reload() {
        loadConfig();
    }

    /**
     * Obtém a configuração do plugin
     * 
     * @return FileConfiguration
     */
    public FileConfiguration getConfig() {
        return config;
    }

    /**
     * Obtém o prefixo do plugin
     * 
     * @return String prefixo colorido
     */
    public String getPrefixo() {
        return prefixo;
    }

    /**
     * Verifica se o debug está ativado
     * 
     * @return boolean
     */
    public boolean isDebug() {
        return debug;
    }
}
