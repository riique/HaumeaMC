package com.haumea.kitpvp.managers;

import com.haumea.kitpvp.HaumeaMC;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gerenciador de mensagens do HaumeaMC.
 * 
 * Carrega e gerencia mensagens do arquivo messages.yml,
 * permitindo customização fácil sem recompilação.
 * 
 * @author HaumeaMC
 */
public class MessageManager {

    private final HaumeaMC plugin;
    private final Map<String, String> messages;
    private File messagesFile;
    private FileConfiguration messagesConfig;

    public MessageManager(HaumeaMC plugin) {
        this.plugin = plugin;
        this.messages = new HashMap<>();
        loadMessages();
    }

    /**
     * Carrega ou recarrega as mensagens do arquivo
     */
    public void loadMessages() {
        messages.clear();

        // Criar arquivo se não existir
        saveDefaultMessages();

        // Carregar arquivo
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);

        // Carregar defaults do JAR para merge
        InputStream defaultStream = plugin.getResource("messages.yml");
        if (defaultStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
            messagesConfig.setDefaults(defaultConfig);
        }

        // Carregar todas as mensagens recursivamente
        loadSection("", messagesConfig);

        plugin.getLogger().info("[MessageManager] Carregadas " + messages.size() + " mensagens do messages.yml");
    }

    /**
     * Carrega uma seção do YAML recursivamente
     */
    private void loadSection(String prefix, ConfigurationSection section) {
        for (String key : section.getKeys(false)) {
            String fullKey = prefix.isEmpty() ? key : prefix + "." + key;

            if (section.isConfigurationSection(key)) {
                // Seção aninhada - recursão
                loadSection(fullKey, section.getConfigurationSection(key));
            } else if (section.isList(key)) {
                // Lista de strings - juntar com quebra de linha
                List<String> list = section.getStringList(key);
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < list.size(); i++) {
                    sb.append(list.get(i));
                    if (i < list.size() - 1) {
                        sb.append("\n");
                    }
                }
                messages.put(fullKey, sb.toString());
            } else {
                // Valor simples
                String value = section.getString(key);
                if (value != null) {
                    messages.put(fullKey, value);
                }
            }
        }
    }

    /**
     * Salva o arquivo messages.yml padrão se não existir
     */
    private void saveDefaultMessages() {
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
            plugin.getLogger().info("[MessageManager] Arquivo messages.yml criado com sucesso!");
        }
    }

    /**
     * Obtém uma mensagem pelo caminho (chave)
     * 
     * @param path Caminho da mensagem (ex: "error.no-permission")
     * @return A mensagem ou null se não encontrada
     */
    public String getMessage(String path) {
        return messages.get(path);
    }

    /**
     * Obtém uma mensagem com placeholders substituídos
     * 
     * @param path         Caminho da mensagem
     * @param replacements Pares de placeholder/valor
     * @return Mensagem com placeholders substituídos
     */
    public String getMessage(String path, String... replacements) {
        String message = messages.get(path);
        if (message == null) {
            return null;
        }

        // Substituir placeholders
        for (int i = 0; i < replacements.length - 1; i += 2) {
            String placeholder = "{" + replacements[i] + "}";
            String value = replacements[i + 1];
            if (value != null) {
                message = message.replace(placeholder, value);
            }
        }

        return message;
    }

    /**
     * Verifica se uma mensagem existe
     */
    public boolean hasMessage(String path) {
        return messages.containsKey(path) || (messagesConfig != null && messagesConfig.contains(path));
    }

    /**
     * Obtém uma lista de mensagens pelo caminho (chave)
     * Retorna diretamente a lista do YAML sem juntar em uma única string.
     * 
     * @param path Caminho da mensagem (ex: "vipkey.broadcast")
     * @return Lista de strings ou null se não encontrada
     */
    public List<String> getMessageList(String path) {
        if (messagesConfig != null && messagesConfig.isList(path)) {
            return messagesConfig.getStringList(path);
        }
        return null;
    }

    /**
     * Obtém a lista de todas as chaves carregadas
     */
    public Map<String, String> getAllMessages() {
        return new HashMap<>(messages);
    }

    /**
     * Recarrega as mensagens do arquivo
     */
    public void reload() {
        loadMessages();
    }

    /**
     * Obtém o FileConfiguration das mensagens
     */
    public FileConfiguration getConfig() {
        return messagesConfig;
    }

    /**
     * Salva alterações no arquivo de mensagens
     */
    public void save() {
        try {
            messagesConfig.save(messagesFile);
        } catch (IOException e) {
            plugin.getLogger().severe("[MessageManager] Erro ao salvar messages.yml: " + e.getMessage());
        }
    }
}
