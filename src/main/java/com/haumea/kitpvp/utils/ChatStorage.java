package com.haumea.kitpvp.utils;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.managers.MessageManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Sistema centralizado de mensagens e identidade visual do HaumeaMC.
 * 
 * Esta classe é o coração da comunicação com jogadores.
 * Mensagens são carregadas do arquivo messages.yml através do MessageManager.
 * 
 * @author HaumeaMC
 */
public final class ChatStorage {

        // ==================== INSTÂNCIA DO PLUGIN ====================

        private static HaumeaMC plugin;
        private static MessageManager messageManager;

        // ==================== FALLBACK MESSAGES ====================

        /**
         * Mapa de fallback para mensagens quando o YAML não está disponível
         */
        private static final Map<String, String> FALLBACK_MESSAGES = new HashMap<>();

        static {
                // Mensagens críticas de fallback (usadas antes do YAML carregar ou se falhar)
                FALLBACK_MESSAGES.put("error.no-permission", "&c&lERRO&f Você não tem permissão para isso!");
                FALLBACK_MESSAGES.put("error.player-only",
                                "&c&lERRO&f Este comando só pode ser executado por jogadores.");
                FALLBACK_MESSAGES.put("error.console-only",
                                "&c&lERRO&f Este comando só pode ser executado pelo console.");
        }

        // ==================== PREFIXOS (FALLBACK) ====================

        public static final String PREFIX = "&6&lHAUMEA&f&lMC&8 »&f ";
        public static final String PREFIX_TAGS = "&9&lTAGS&f ";
        public static final String PREFIX_ERROR = "&c&lERRO&f ";
        public static final String PREFIX_SUCCESS = "&a&lSUCESSO&f ";
        public static final String PREFIX_ADMIN = "&4&lADMIN&f ";
        public static final String PREFIX_GROUPS = "&6&lHAUMEAGROUPS&f ";
        public static final String PREFIX_STAFF_BROADCAST = "&e&l[HAUMEA-STAFF] &f";
        public static final String PREFIX_PUNISHMENT = "&4&lPUNISH &f";
        public static final String SEPARATOR = "&8&m----------------------------------------";

        // ==================== CORES TEMÁTICAS ====================

        public static final String COLOR_PRIMARY = "&6";
        public static final String COLOR_SECONDARY = "&f";
        public static final String COLOR_HIGHLIGHT = "&e";
        public static final String COLOR_ERROR = "&c";
        public static final String COLOR_SUCCESS = "&a";
        public static final String COLOR_INFO = "&7";

        // ==================== INICIALIZAÇÃO ====================

        /**
         * Inicializa o ChatStorage com a instância do plugin.
         * Deve ser chamado no onEnable do plugin.
         * 
         * @param pluginInstance Instância do plugin HaumeaMC
         */
        public static void init(HaumeaMC pluginInstance) {
                plugin = pluginInstance;
                messageManager = pluginInstance.getMessageManager();
                plugin.getLogger().info("[ChatStorage] Inicializado com MessageManager");
        }

        // ==================== MÉTODOS DE CONVERSÃO ====================

        /**
         * Converte códigos de cor '&' para '§'
         * 
         * @param message Mensagem com códigos '&'
         * @return Mensagem com códigos '§'
         */
        public static String colorize(String message) {
                if (message == null)
                        return "";
                return message.replace("&", "§");
        }

        /**
         * Remove todos os códigos de cor de uma mensagem
         * 
         * @param message Mensagem com códigos de cor
         * @return Mensagem sem códigos de cor
         */
        public static String stripColors(String message) {
                if (message == null)
                        return "";
                return message.replaceAll("[&§][0-9a-fk-or]", "");
        }

        // ==================== MÉTODOS DE ENVIO ====================

        /**
         * Envia uma mensagem do dicionário para um jogador.
         * A mensagem já vem colorizada automaticamente.
         * 
         * @param player Jogador destinatário
         * @param key    Chave da mensagem no dicionário
         */
        public static void send(Player player, String key) {
                send(player, key, new String[0]);
        }

        /**
         * Envia uma mensagem do dicionário para um jogador com placeholders.
         * 
         * @param player       Jogador destinatário
         * @param key          Chave da mensagem no dicionário
         * @param replacements Pares de placeholder/valor (ex: "player", "Steve",
         *                     "amount", "100")
         */
        public static void send(Player player, String key, String... replacements) {
                String message = getMessage(key, replacements);
                player.sendMessage(message);
        }

        /**
         * Envia uma mensagem do dicionário para um CommandSender.
         * 
         * @param sender Destinatário (Player ou Console)
         * @param key    Chave da mensagem no dicionário
         */
        public static void send(CommandSender sender, String key) {
                send(sender, key, new String[0]);
        }

        /**
         * Envia uma mensagem do dicionário para um CommandSender com placeholders.
         * 
         * @param sender       Destinatário (Player ou Console)
         * @param key          Chave da mensagem no dicionário
         * @param replacements Pares de placeholder/valor
         */
        public static void send(CommandSender sender, String key, String... replacements) {
                String message = getMessage(key, replacements);
                sender.sendMessage(message);
        }

        /**
         * Envia uma mensagem do dicionário para todos os jogadores online.
         * 
         * @param key Chave da mensagem
         */
        public static void broadcast(String key) {
                broadcast(key, new String[0]);
        }

        /**
         * Envia uma mensagem do dicionário para todos os jogadores online com
         * placeholders.
         * 
         * @param key          Chave da mensagem
         * @param replacements Pares de placeholder/valor
         */
        public static void broadcast(String key, String... replacements) {
                String message = getMessage(key, replacements);
                for (Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
                        player.sendMessage(message);
                }
                plugin.getLogger().info("[BROADCAST] " + stripColors(message));
        }

        /**
         * Envia uma mensagem personalizada (não do dicionário) para um jogador.
         * Adiciona o prefixo do servidor automaticamente.
         * 
         * @param player  Jogador destinatário
         * @param message Mensagem a enviar
         */
        public static void sendCustom(Player player, String message) {
                player.sendMessage(colorize(getPrefix() + message));
        }

        /**
         * Envia uma mensagem personalizada sem prefixo.
         * 
         * @param player  Jogador destinatário
         * @param message Mensagem a enviar
         */
        public static void sendRaw(Player player, String message) {
                player.sendMessage(colorize(message));
        }

        /**
         * Envia uma mensagem personalizada sem prefixo para CommandSender.
         * 
         * @param sender  Destinatário
         * @param message Mensagem a enviar
         */
        public static void sendRaw(CommandSender sender, String message) {
                sender.sendMessage(colorize(message));
        }

        // ==================== MÉTODOS DE OBTENÇÃO ====================

        /**
         * Obtém uma mensagem do dicionário já colorizada.
         * Primeiro tenta buscar do messages.yml, depois do fallback.
         * 
         * @param key Chave da mensagem
         * @return Mensagem colorizada ou chave se não encontrada
         */
        public static String getMessage(String key) {
                return getMessage(key, new String[0]);
        }

        /**
         * Obtém uma mensagem do dicionário com placeholders substituídos.
         * Primeiro tenta buscar do messages.yml, depois do fallback, depois retorna a
         * chave.
         * 
         * @param key          Chave da mensagem
         * @param replacements Pares de placeholder/valor
         * @return Mensagem colorizada com placeholders substituídos
         */
        public static String getMessage(String key, String... replacements) {
                String message = null;

                // 1. Tentar buscar do MessageManager (messages.yml)
                if (messageManager != null) {
                        message = messageManager.getMessage(key, replacements);
                }

                // 2. Se não encontrou, tentar fallback
                if (message == null) {
                        message = FALLBACK_MESSAGES.get(key);
                        if (message != null) {
                                // Aplicar replacements ao fallback
                                for (int i = 0; i < replacements.length - 1; i += 2) {
                                        String placeholder = "{" + replacements[i] + "}";
                                        String value = replacements[i + 1];
                                        if (value != null) {
                                                message = message.replace(placeholder, value);
                                        }
                                }
                        }
                }

                // 3. Se ainda não encontrou, retornar a própria chave
                if (message == null) {
                        // Log de debug para chaves não encontradas
                        if (plugin != null) {
                                plugin.getLogger().warning("[ChatStorage] Mensagem não encontrada: " + key);
                        }
                        return colorize("&c[MSG:" + key + "]");
                }

                return colorize(message);
        }

        /**
         * Obtém uma lista de mensagens do dicionário (para mensagens multilinha).
         * Suporta tanto listas YAML quanto strings simples (converte para lista de 1
         * elemento).
         * 
         * @param key Chave da mensagem
         * @return Lista de mensagens ou lista vazia se não encontrada
         */
        public static List<String> getMessageList(String key) {
                List<String> result = new ArrayList<>();

                if (messageManager != null) {
                        List<String> list = messageManager.getMessageList(key);
                        if (list != null && !list.isEmpty()) {
                                return list;
                        }

                        // Fallback: tentar como string simples
                        String single = messageManager.getMessage(key);
                        if (single != null) {
                                result.add(single);
                                return result;
                        }
                }

                // Log de debug para chaves não encontradas
                if (plugin != null) {
                        plugin.getLogger().warning("[ChatStorage] Lista de mensagens não encontrada: " + key);
                }

                return result;
        }

        /**
         * Obtém o prefixo do servidor já colorizado.
         * Primeiro tenta buscar do messages.yml, depois usa fallback.
         * 
         * @return Prefixo colorizado
         */
        public static String getPrefix() {
                if (messageManager != null && messageManager.hasMessage("system.prefix.general")) {
                        return colorize(messageManager.getMessage("system.prefix.general"));
                }
                return colorize(PREFIX);
        }

        /**
         * Obtém o prefixo de tags já colorizado.
         * 
         * @return Prefixo de tags colorizado
         */
        public static String getTagsPrefix() {
                if (messageManager != null && messageManager.hasMessage("system.prefix.tags")) {
                        return colorize(messageManager.getMessage("system.prefix.tags"));
                }
                return colorize(PREFIX_TAGS);
        }

        /**
         * Obtém o prefixo de erro já colorizado.
         * 
         * @return Prefixo de erro colorizado
         */
        public static String getErrorPrefix() {
                if (messageManager != null && messageManager.hasMessage("system.prefix.error")) {
                        return colorize(messageManager.getMessage("system.prefix.error"));
                }
                return colorize(PREFIX_ERROR);
        }

        /**
         * Obtém o separador visual já colorizado.
         * 
         * @return Separador colorizado
         */
        public static String getSeparator() {
                if (messageManager != null && messageManager.hasMessage("system.separator")) {
                        return colorize(messageManager.getMessage("system.separator"));
                }
                return colorize(SEPARATOR);
        }

        // ==================== MÉTODOS UTILITÁRIOS ====================

        /**
         * Verifica se uma chave de mensagem existe no dicionário.
         * 
         * @param key Chave a verificar
         * @return true se existe
         */
        public static boolean hasMessage(String key) {
                if (messageManager != null) {
                        return messageManager.hasMessage(key);
                }
                return FALLBACK_MESSAGES.containsKey(key);
        }

        /**
         * Adiciona ou substitui uma mensagem no dicionário de fallback em runtime.
         * Útil para carregar mensagens de configuração externa.
         * 
         * @param key     Chave da mensagem
         * @param message Conteúdo da mensagem
         */
        public static void setMessage(String key, String message) {
                FALLBACK_MESSAGES.put(key, message);
        }

        /**
         * Formata um número com separadores de milhar.
         * Ex: 1000000 -> 1.000.000
         * 
         * @param number Número a formatar
         * @return String formatada
         */
        public static String formatNumber(long number) {
                return String.format("%,d", number).replace(",", ".");
        }

        /**
         * Formata um número inteiro com separadores de milhar.
         * Ex: 1000 -> 1.000
         * 
         * @param number Número a formatar
         * @return String formatada
         */
        public static String formatNumber(int number) {
                return formatNumber((long) number);
        }

        /**
         * Formata um tempo em milissegundos para texto legível.
         * Ex: 3661000 -> "1h 1m 1s"
         * 
         * @param millis Tempo em milissegundos
         * @return Tempo formatado
         */
        public static String formatTime(long millis) {
                long seconds = millis / 1000;
                long minutes = seconds / 60;
                long hours = minutes / 60;
                long days = hours / 24;

                StringBuilder sb = new StringBuilder();
                if (days > 0)
                        sb.append(days).append("d ");
                if (hours % 24 > 0)
                        sb.append(hours % 24).append("h ");
                if (minutes % 60 > 0)
                        sb.append(minutes % 60).append("m ");
                if (seconds % 60 > 0 || sb.length() == 0)
                        sb.append(seconds % 60).append("s");

                return sb.toString().trim();
        }

        /**
         * Recarrega as mensagens do arquivo YAML.
         * Deve ser chamado após alterações no messages.yml
         */
        public static void reload() {
                if (messageManager != null) {
                        messageManager.reload();
                        if (plugin != null) {
                                plugin.getLogger().info("[ChatStorage] Mensagens recarregadas!");
                        }
                }
        }

        // Construtor privado para impedir instâncias
        private ChatStorage() {
                throw new UnsupportedOperationException("Esta classe não pode ser instanciada!");
        }
}
