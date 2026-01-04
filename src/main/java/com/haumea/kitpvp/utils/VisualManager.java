package com.haumea.kitpvp.utils;

import com.haumea.kitpvp.HaumeaMC;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gerenciador Unificado de Feedback Visual do HaumeaMC.
 * 
 * Centraliza todas as exibições visuais para jogadores:
 * - ActionBar: Mensagens acima do hotbar
 * - Title/Subtitle: Mensagens grandes centralizadas
 * 
 * Características:
 * - Compatível com clientes 1.7.x e 1.8.x
 * - Sistema de prioridade para conflitos
 * - Mensagens temporárias e persistentes
 * - Barras de progresso e formatação de tempo
 * - Thread-safe e performático
 * 
 * Uso:
 * - VisualManager.sendActionBar(player, "Mensagem");
 * - VisualManager.sendTitle(player, "Título", "Subtítulo", Preset.NORMAL);
 * - VisualManager.sendPersistentActionBar(player, "cooldown", "Mensagem",
 * Priority.HIGH);
 * 
 * @author HaumeaMC
 */
public class VisualManager {

    // ==================== CONSTANTES ====================

    /**
     * Intervalo em ticks para re-envio de ActionBar persistente.
     * ActionBar some após ~2 segundos, então re-enviamos a cada 40 ticks (2s).
     */
    private static final int ACTIONBAR_REFRESH_TICKS = 40;

    // ==================== ENUMS ====================

    /**
     * Níveis de prioridade para mensagens de ActionBar.
     * Mensagens com maior prioridade substituem as de menor.
     */
    public enum Priority {
        /** Mensagens de baixa prioridade (dicas, info) */
        LOW(0),
        /** Mensagens normais (feedback de ações) */
        NORMAL(1),
        /** Mensagens de alta prioridade (alertas) */
        HIGH(2),
        /** Mensagens críticas (combate, morte iminente) */
        CRITICAL(3),
        /** Prioridade máxima (sistema) - não pode ser substituída */
        SYSTEM(9);

        private final int level;

        Priority(int level) {
            this.level = level;
        }

        public int getLevel() {
            return level;
        }

        public boolean overrides(Priority other) {
            return this.level >= other.level;
        }
    }

    /**
     * Presets de tempo para títulos.
     * Define tempos de fade-in, stay e fade-out.
     */
    public enum TitlePreset {
        /** Muito rápido: 5 ticks in, 20 ticks stay, 5 ticks out */
        INSTANT(5, 20, 5),
        /** Rápido: 10 ticks in, 40 ticks stay, 10 ticks out */
        FAST(10, 40, 10),
        /** Normal: 10 ticks in, 60 ticks stay, 20 ticks out */
        NORMAL(10, 60, 20),
        /** Lento: 20 ticks in, 80 ticks stay, 20 ticks out */
        SLOW(20, 80, 20),
        /** Dramático: 30 ticks in, 100 ticks stay, 30 ticks out */
        DRAMATIC(30, 100, 30),
        /** Permanente: 0 fade, fica até ser limpo */
        PERMANENT(0, Integer.MAX_VALUE, 0);

        public final int fadeIn;
        public final int stay;
        public final int fadeOut;

        TitlePreset(int fadeIn, int stay, int fadeOut) {
            this.fadeIn = fadeIn;
            this.stay = stay;
            this.fadeOut = fadeOut;
        }
    }

    // ==================== ESTADO E CACHE ====================

    /** ActionBars persistentes ativas por jogador */
    private static final Map<UUID, PersistentActionBar> persistentActionBars = new ConcurrentHashMap<>();

    /** Task de refresh de ActionBar */
    private static BukkitTask refreshTask = null;

    /** Cache de reflexão */
    private static String nmsVersion;
    private static Class<?> packetChatClass;
    private static Class<?> chatComponentClass;
    private static Method chatSerializerMethod;
    private static Constructor<?> packetChatConstructor;
    private static Class<?> packetTitleClass;
    private static Class<?> titleActionEnum;
    private static boolean reflectionInitialized = false;
    private static boolean supportsActionBar = false;
    private static boolean supportsTitle = false;

    // ==================== INICIALIZAÇÃO ====================

    static {
        initReflection();
    }

    /**
     * Inicializa a reflexão NMS para envio de pacotes.
     * Detecta automaticamente a versão do servidor.
     */
    private static void initReflection() {
        try {
            nmsVersion = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
            String nmsPackage = "net.minecraft.server." + nmsVersion;

            // Classes comuns
            chatComponentClass = Class.forName(nmsPackage + ".IChatBaseComponent");
            Class<?> chatSerializerClass = Class.forName(nmsPackage + ".IChatBaseComponent$ChatSerializer");
            chatSerializerMethod = chatSerializerClass.getMethod("a", String.class);

            // PacketPlayOutChat (para ActionBar)
            try {
                packetChatClass = Class.forName(nmsPackage + ".PacketPlayOutChat");
                // Em 1.8.8, o construtor aceita IChatBaseComponent e byte (type 2 = actionbar)
                packetChatConstructor = packetChatClass.getConstructor(chatComponentClass, byte.class);
                supportsActionBar = true;
            } catch (Exception e) {
                // Versão não suporta ActionBar (1.7.x)
                supportsActionBar = false;
            }

            // PacketPlayOutTitle (para Titles)
            try {
                packetTitleClass = Class.forName(nmsPackage + ".PacketPlayOutTitle");
                titleActionEnum = Class.forName(nmsPackage + ".PacketPlayOutTitle$EnumTitleAction");
                supportsTitle = true;
            } catch (Exception e) {
                // Versão não suporta Title (1.7.x)
                supportsTitle = false;
            }

            reflectionInitialized = true;
        } catch (Exception e) {
            reflectionInitialized = false;
        }
    }

    /**
     * Inicializa o sistema (chamado pelo plugin no onEnable).
     * Inicia a task de refresh de ActionBar persistente.
     * 
     * @param plugin Instância do plugin
     */
    public static void initialize(HaumeaMC plugin) {
        if (refreshTask != null) {
            refreshTask.cancel();
        }

        // Task que re-envia ActionBars persistentes a cada 2 segundos
        refreshTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (Map.Entry<UUID, PersistentActionBar> entry : persistentActionBars.entrySet()) {
                    Player player = Bukkit.getPlayer(entry.getKey());
                    if (player == null || !player.isOnline()) {
                        persistentActionBars.remove(entry.getKey());
                        continue;
                    }

                    PersistentActionBar pab = entry.getValue();

                    // Verificar expiração
                    if (pab.hasExpired()) {
                        persistentActionBars.remove(entry.getKey());
                        clearActionBar(player);
                        continue;
                    }

                    // Re-enviar mensagem
                    sendActionBarPacket(player, pab.getMessage());
                }
            }
        }.runTaskTimerAsynchronously(plugin, ACTIONBAR_REFRESH_TICKS, ACTIONBAR_REFRESH_TICKS);

        if (plugin != null) {
            plugin.getLogger().info("[VisualManager] Sistema de feedback visual inicializado!");
            plugin.getLogger().info("[VisualManager] ActionBar: " + (supportsActionBar ? "§aAtivo" : "§cDesativado"));
            plugin.getLogger().info("[VisualManager] Title: " + (supportsTitle ? "§aAtivo" : "§cDesativado"));
        }
    }

    /**
     * Desliga o sistema (chamado pelo plugin no onDisable).
     */
    public static void shutdown() {
        if (refreshTask != null) {
            refreshTask.cancel();
            refreshTask = null;
        }
        persistentActionBars.clear();
    }

    /**
     * Limpa todos os dados de um jogador (chamado no logout).
     * 
     * @param player Jogador que está saindo
     */
    public static void cleanupPlayer(Player player) {
        if (player != null) {
            persistentActionBars.remove(player.getUniqueId());
        }
    }

    // ==================== ACTIONBAR ====================

    /**
     * Envia uma mensagem simples na ActionBar.
     * A mensagem desaparece automaticamente após ~2 segundos.
     * 
     * @param player  Jogador destinatário
     * @param message Mensagem (suporta códigos de cor § e &)
     */
    public static void sendActionBar(Player player, String message) {
        if (!isValidPlayer(player))
            return;
        if (!supportsActionBar)
            return;

        String colorized = ChatStorage.colorize(message);
        sendActionBarPacket(player, colorized);
    }

    /**
     * Envia uma mensagem temporizada na ActionBar.
     * A mensagem é re-enviada até expirar.
     * 
     * @param player     Jogador destinatário
     * @param message    Mensagem (suporta códigos de cor)
     * @param durationMs Duração em milissegundos
     */
    public static void sendActionBar(Player player, String message, long durationMs) {
        sendActionBar(player, message, durationMs, Priority.NORMAL);
    }

    /**
     * Envia uma mensagem temporizada na ActionBar com prioridade.
     * 
     * @param player     Jogador destinatário
     * @param message    Mensagem (suporta códigos de cor)
     * @param durationMs Duração em milissegundos
     * @param priority   Prioridade da mensagem
     */
    public static void sendActionBar(Player player, String message, long durationMs, Priority priority) {
        if (!isValidPlayer(player))
            return;
        if (!supportsActionBar)
            return;

        String colorized = ChatStorage.colorize(message);
        UUID uuid = player.getUniqueId();

        // Verificar se pode substituir mensagem existente
        PersistentActionBar existing = persistentActionBars.get(uuid);
        if (existing != null && !priority.overrides(existing.getPriority())) {
            return; // Não pode substituir
        }

        // Criar nova mensagem persistente (expira em durationMs)
        long expiresAt = System.currentTimeMillis() + durationMs;
        persistentActionBars.put(uuid, new PersistentActionBar(colorized, priority, expiresAt, null));

        // Enviar imediatamente
        sendActionBarPacket(player, colorized);
    }

    /**
     * Envia uma mensagem persistente na ActionBar.
     * A mensagem permanece até ser explicitamente removida.
     * 
     * @param player   Jogador destinatário
     * @param id       Identificador único da mensagem (ex: "combat_tag",
     *                 "cooldown_kit")
     * @param message  Mensagem (suporta códigos de cor)
     * @param priority Prioridade da mensagem
     */
    public static void sendPersistentActionBar(Player player, String id, String message, Priority priority) {
        if (!isValidPlayer(player))
            return;
        if (!supportsActionBar)
            return;

        String colorized = ChatStorage.colorize(message);
        UUID uuid = player.getUniqueId();

        // Verificar se pode substituir mensagem existente
        PersistentActionBar existing = persistentActionBars.get(uuid);
        if (existing != null && !priority.overrides(existing.getPriority())) {
            return; // Não pode substituir
        }

        // Criar nova mensagem persistente (sem expiração)
        persistentActionBars.put(uuid, new PersistentActionBar(colorized, priority, -1, id));

        // Enviar imediatamente
        sendActionBarPacket(player, colorized);
    }

    /**
     * Atualiza a mensagem de uma ActionBar persistente.
     * Útil para atualizar tempo restante sem recriar a mensagem.
     * 
     * @param player  Jogador
     * @param id      Identificador da mensagem persistente
     * @param message Nova mensagem
     */
    public static void updatePersistentActionBar(Player player, String id, String message) {
        if (!isValidPlayer(player))
            return;

        UUID uuid = player.getUniqueId();
        PersistentActionBar existing = persistentActionBars.get(uuid);

        if (existing != null && id.equals(existing.getId())) {
            String colorized = ChatStorage.colorize(message);
            existing.setMessage(colorized);
            sendActionBarPacket(player, colorized);
        }
    }

    /**
     * Remove uma ActionBar persistente pelo ID.
     * 
     * @param player Jogador
     * @param id     Identificador da mensagem
     */
    public static void removePersistentActionBar(Player player, String id) {
        if (!isValidPlayer(player))
            return;

        UUID uuid = player.getUniqueId();
        PersistentActionBar existing = persistentActionBars.get(uuid);

        if (existing != null && id.equals(existing.getId())) {
            persistentActionBars.remove(uuid);
            clearActionBar(player);
        }
    }

    /**
     * Limpa a ActionBar de um jogador.
     * 
     * @param player Jogador
     */
    public static void clearActionBar(Player player) {
        if (!isValidPlayer(player))
            return;

        persistentActionBars.remove(player.getUniqueId());

        if (supportsActionBar) {
            sendActionBarPacket(player, "");
        }
    }

    /**
     * Verifica se um jogador tem ActionBar ativa.
     * 
     * @param player Jogador
     * @return true se tem ActionBar ativa
     */
    public static boolean hasActiveActionBar(Player player) {
        if (player == null)
            return false;
        return persistentActionBars.containsKey(player.getUniqueId());
    }

    /**
     * Obtém o ID da ActionBar ativa de um jogador.
     * 
     * @param player Jogador
     * @return ID da ActionBar ou null se não houver
     */
    public static String getActiveActionBarId(Player player) {
        if (player == null)
            return null;
        PersistentActionBar pab = persistentActionBars.get(player.getUniqueId());
        return pab != null ? pab.getId() : null;
    }

    // ==================== TITLE ====================

    /**
     * Envia um título simples (apenas título principal).
     * Usa preset NORMAL para tempos.
     * 
     * @param player Jogador destinatário
     * @param title  Texto do título (suporta códigos de cor)
     */
    public static void sendTitle(Player player, String title) {
        sendTitle(player, title, "", TitlePreset.NORMAL);
    }

    /**
     * Envia um título com subtítulo.
     * Usa preset NORMAL para tempos.
     * 
     * @param player   Jogador destinatário
     * @param title    Texto do título
     * @param subtitle Texto do subtítulo
     */
    public static void sendTitle(Player player, String title, String subtitle) {
        sendTitle(player, title, subtitle, TitlePreset.NORMAL);
    }

    /**
     * Envia um título com subtítulo e preset de tempo.
     * 
     * @param player   Jogador destinatário
     * @param title    Texto do título
     * @param subtitle Texto do subtítulo
     * @param preset   Preset de tempo (FAST, NORMAL, SLOW, etc)
     */
    public static void sendTitle(Player player, String title, String subtitle, TitlePreset preset) {
        sendTitle(player, title, subtitle, preset.fadeIn, preset.stay, preset.fadeOut);
    }

    /**
     * Envia um título com subtítulo e tempos customizados.
     * 
     * @param player   Jogador destinatário
     * @param title    Texto do título
     * @param subtitle Texto do subtítulo
     * @param fadeIn   Tempo de fade in em ticks
     * @param stay     Tempo de permanência em ticks
     * @param fadeOut  Tempo de fade out em ticks
     */
    public static void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        if (!isValidPlayer(player))
            return;
        if (!supportsTitle)
            return;

        String colorizedTitle = ChatStorage.colorize(title);
        String colorizedSubtitle = ChatStorage.colorize(subtitle);

        // Garantir execução no main thread
        if (!Bukkit.isPrimaryThread()) {
            HaumeaMC plugin = HaumeaMC.getInstance();
            if (plugin != null) {
                Bukkit.getScheduler().runTask(plugin,
                        () -> sendTitlePackets(player, colorizedTitle, colorizedSubtitle, fadeIn, stay, fadeOut));
            }
        } else {
            sendTitlePackets(player, colorizedTitle, colorizedSubtitle, fadeIn, stay, fadeOut);
        }
    }

    /**
     * Envia apenas um subtítulo (título em branco).
     * 
     * @param player   Jogador destinatário
     * @param subtitle Texto do subtítulo
     */
    public static void sendSubtitle(Player player, String subtitle) {
        sendTitle(player, "", subtitle, TitlePreset.NORMAL);
    }

    /**
     * Envia apenas um subtítulo com preset de tempo.
     * 
     * @param player   Jogador destinatário
     * @param subtitle Texto do subtítulo
     * @param preset   Preset de tempo
     */
    public static void sendSubtitle(Player player, String subtitle, TitlePreset preset) {
        sendTitle(player, "", subtitle, preset);
    }

    /**
     * Limpa títulos ativos de um jogador.
     * 
     * @param player Jogador
     */
    public static void clearTitle(Player player) {
        if (!isValidPlayer(player))
            return;
        if (!supportsTitle)
            return;

        try {
            // Enviar pacote CLEAR
            Object clearAction = titleActionEnum.getEnumConstants()[3]; // CLEAR
            Constructor<?> clearConstructor = packetTitleClass.getConstructor(titleActionEnum, chatComponentClass);
            Object clearPacket = clearConstructor.newInstance(clearAction, null);
            sendPacket(player, clearPacket);
        } catch (Exception ignored) {
        }
    }

    /**
     * Reseta completamente os títulos de um jogador.
     * Remove todos os tempos e configurações.
     * 
     * @param player Jogador
     */
    public static void resetTitle(Player player) {
        if (!isValidPlayer(player))
            return;
        if (!supportsTitle)
            return;

        try {
            // Enviar pacote RESET
            Object resetAction = titleActionEnum.getEnumConstants()[4]; // RESET
            Constructor<?> resetConstructor = packetTitleClass.getConstructor(titleActionEnum, chatComponentClass);
            Object resetPacket = resetConstructor.newInstance(resetAction, null);
            sendPacket(player, resetPacket);
        } catch (Exception ignored) {
        }
    }

    // ==================== UTILIDADES ====================

    /**
     * Formata tempo restante para exibição.
     * Ex: 3500ms -> "3.5s", 65000ms -> "1m 5s"
     * 
     * @param remainingMs Tempo restante em milissegundos
     * @return String formatada
     */
    public static String formatTimeRemaining(long remainingMs) {
        if (remainingMs <= 0)
            return "0s";

        long seconds = remainingMs / 1000;
        long tenths = (remainingMs % 1000) / 100;

        if (seconds < 60) {
            if (tenths > 0) {
                return seconds + "." + tenths + "s";
            }
            return seconds + "s";
        }

        long minutes = seconds / 60;
        long secs = seconds % 60;

        if (secs > 0) {
            return minutes + "m " + secs + "s";
        }
        return minutes + "m";
    }

    /**
     * Cria uma barra de progresso visual usando caracteres.
     * Ex: createProgressBar(0.7, 10, '|', "§a", "§c") -> "§a|||||||§c|||"
     * 
     * @param progress    Progresso de 0.0 a 1.0
     * @param totalBars   Número total de barras
     * @param barChar     Caractere da barra
     * @param filledColor Cor das barras preenchidas
     * @param emptyColor  Cor das barras vazias
     * @return String formatada com a barra de progresso
     */
    public static String createProgressBar(double progress, int totalBars, char barChar,
            String filledColor, String emptyColor) {
        progress = Math.max(0, Math.min(1, progress));
        int filled = (int) Math.round(progress * totalBars);
        int empty = totalBars - filled;

        StringBuilder bar = new StringBuilder();
        bar.append(filledColor);
        for (int i = 0; i < filled; i++) {
            bar.append(barChar);
        }
        bar.append(emptyColor);
        for (int i = 0; i < empty; i++) {
            bar.append(barChar);
        }

        return bar.toString();
    }

    /**
     * Cria uma barra de progresso com cores padrão (verde/vermelho).
     * 
     * @param progress  Progresso de 0.0 a 1.0
     * @param totalBars Número total de barras
     * @return String formatada
     */
    public static String createProgressBar(double progress, int totalBars) {
        return createProgressBar(progress, totalBars, '|', "§a", "§c");
    }

    /**
     * Cria uma mensagem de cooldown formatada para ActionBar.
     * Ex: "§cNinja §8| §a|||||||§c||| §f2.5s"
     * 
     * @param label       Nome/label (ex: "Ninja", "Combat")
     * @param remainingMs Tempo restante em ms
     * @param totalMs     Tempo total do cooldown em ms
     * @param totalBars   Número de barras
     * @return String formatada pronta para ActionBar
     */
    public static String createCooldownBar(String label, long remainingMs, long totalMs, int totalBars) {
        double progress = 1.0 - ((double) remainingMs / totalMs);
        String progressBar = createProgressBar(progress, totalBars);
        String timeRemaining = formatTimeRemaining(remainingMs);

        return "§c" + label + " §8| " + progressBar + " §f" + timeRemaining;
    }

    /**
     * Cria uma mensagem de combat tag formatada para ActionBar.
     * 
     * @param remainingSeconds Segundos restantes de combate
     * @return String formatada
     */
    public static String createCombatTagBar(int remainingSeconds) {
        return "§c§lEM COMBATE §8- §f" + remainingSeconds + "s";
    }

    /**
     * Cria uma mensagem de teleporte formatada para ActionBar.
     * 
     * @param remainingSeconds Segundos restantes
     * @return String formatada
     */
    public static String createTeleportCountdown(int remainingSeconds) {
        return "§eTeleportando em §c" + remainingSeconds + "§e segundo" + (remainingSeconds != 1 ? "s" : "") + "...";
    }

    // ==================== MÉTODOS INTERNOS ====================

    /**
     * Verifica se o jogador é válido para receber mensagens.
     */
    private static boolean isValidPlayer(Player player) {
        return player != null && player.isOnline();
    }

    /**
     * Envia o pacote de ActionBar via reflexão.
     */
    private static void sendActionBarPacket(Player player, String message) {
        if (!reflectionInitialized || !supportsActionBar)
            return;

        try {
            // Criar componente de chat
            Object chatComponent = chatSerializerMethod.invoke(null,
                    "{\"text\":\"" + escapeJson(message) + "\"}");

            // Criar pacote (type 2 = actionbar)
            Object packet = packetChatConstructor.newInstance(chatComponent, (byte) 2);

            // Enviar pacote
            sendPacket(player, packet);
        } catch (Exception ignored) {
        }
    }

    /**
     * Envia os pacotes de Title via reflexão.
     */
    private static void sendTitlePackets(Player player, String title, String subtitle,
            int fadeIn, int stay, int fadeOut) {
        if (!reflectionInitialized || !supportsTitle)
            return;

        try {
            // Enviar TIMES primeiro
            Constructor<?> timesConstructor = packetTitleClass.getConstructor(int.class, int.class, int.class);
            Object timesPacket = timesConstructor.newInstance(fadeIn, stay, fadeOut);
            sendPacket(player, timesPacket);

            // Enviar TITLE se não estiver vazio
            if (title != null && !title.isEmpty()) {
                Object titleAction = titleActionEnum.getEnumConstants()[0]; // TITLE
                Object titleComponent = chatSerializerMethod.invoke(null,
                        "{\"text\":\"" + escapeJson(title) + "\"}");
                Constructor<?> titleConstructor = packetTitleClass.getConstructor(titleActionEnum, chatComponentClass);
                Object titlePacket = titleConstructor.newInstance(titleAction, titleComponent);
                sendPacket(player, titlePacket);
            }

            // Enviar SUBTITLE se não estiver vazio
            if (subtitle != null && !subtitle.isEmpty()) {
                Object subtitleAction = titleActionEnum.getEnumConstants()[1]; // SUBTITLE
                Object subtitleComponent = chatSerializerMethod.invoke(null,
                        "{\"text\":\"" + escapeJson(subtitle) + "\"}");
                Constructor<?> subtitleConstructor = packetTitleClass.getConstructor(titleActionEnum,
                        chatComponentClass);
                Object subtitlePacket = subtitleConstructor.newInstance(subtitleAction, subtitleComponent);
                sendPacket(player, subtitlePacket);
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * Envia um pacote para um jogador via reflexão.
     */
    private static void sendPacket(Player player, Object packet) throws Exception {
        Object handle = player.getClass().getMethod("getHandle").invoke(player);
        Object connection = handle.getClass().getField("playerConnection").get(handle);
        connection.getClass().getMethod("sendPacket",
                Class.forName("net.minecraft.server." + nmsVersion + ".Packet"))
                .invoke(connection, packet);
    }

    /**
     * Escapa caracteres especiais para JSON.
     */
    private static String escapeJson(String text) {
        if (text == null)
            return "";
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "")
                .replace("\t", "\\t");
    }

    // ==================== GETTERS DE STATUS ====================

    /**
     * Verifica se o sistema de ActionBar está disponível.
     */
    public static boolean isActionBarSupported() {
        return supportsActionBar;
    }

    /**
     * Verifica se o sistema de Title está disponível.
     */
    public static boolean isTitleSupported() {
        return supportsTitle;
    }

    /**
     * Verifica se a reflexão foi inicializada com sucesso.
     */
    public static boolean isReflectionInitialized() {
        return reflectionInitialized;
    }

    /**
     * Obtém informações de debug do sistema.
     */
    public static String getDebugInfo() {
        return "VisualManager Debug:\n" +
                "  NMS Version: " + nmsVersion + "\n" +
                "  Reflection: " + reflectionInitialized + "\n" +
                "  ActionBar: " + supportsActionBar + "\n" +
                "  Title: " + supportsTitle + "\n" +
                "  Active ActionBars: " + persistentActionBars.size();
    }

    // ==================== CLASSE INTERNA ====================

    /**
     * Representa uma ActionBar persistente.
     */
    private static class PersistentActionBar {
        private String message;
        private final Priority priority;
        private final long expiresAt; // -1 = nunca expira
        private final String id;

        public PersistentActionBar(String message, Priority priority, long expiresAt, String id) {
            this.message = message;
            this.priority = priority;
            this.expiresAt = expiresAt;
            this.id = id;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public Priority getPriority() {
            return priority;
        }

        public String getId() {
            return id;
        }

        public boolean hasExpired() {
            return expiresAt > 0 && System.currentTimeMillis() >= expiresAt;
        }
    }
}
