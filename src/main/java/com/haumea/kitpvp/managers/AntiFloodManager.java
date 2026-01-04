package com.haumea.kitpvp.managers;

import com.haumea.kitpvp.HaumeaMC;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;

/**
 * Sistema inteligente de antiflood e antispam.
 * 
 * NÃO limita mensagens normais. Detecta:
 * 1. FLOOD: Muitas mensagens em pouco tempo
 * 2. SPAM: Mensagens iguais/similares consecutivas (independente do tempo)
 * 
 * @author HaumeaMC
 */
public class AntiFloodManager {

    private final HaumeaMC plugin;

    // Dados de cada jogador
    private final Map<UUID, FloodData> playerData;

    // === CONFIGURAÇÕES DE FLOOD ===
    private int maxMessagesInWindow; // Máximo de mensagens na janela
    private long windowTimeMs; // Janela de tempo em ms

    // === CONFIGURAÇÕES DE SPAM ===
    private int consecutiveSimilar; // Quantas mensagens similares consecutivas = spam
    private double similarityThreshold; // Threshold para considerar similar

    // === CONFIGURAÇÕES DE PUNIÇÃO ===
    private long basePunishmentMs; // Punição base em ms
    private double punishmentMultiplier; // Multiplicador por violação
    private long maxPunishmentMs; // Punição máxima em ms
    private long violationDecayMs; // Tempo para violações resetarem

    // === CONFIGURAÇÃO DE HISTÓRICO ===
    private int historySize; // Quantas mensagens guardar no histórico

    public AntiFloodManager(HaumeaMC plugin) {
        this.plugin = plugin;
        this.playerData = new HashMap<>();
        loadConfig();
    }

    /**
     * Carrega configurações do config.yml
     */
    public void loadConfig() {
        // Flood
        this.maxMessagesInWindow = plugin.getConfig().getInt("antiflood.max-messages", 5);
        this.windowTimeMs = plugin.getConfig().getLong("antiflood.window-seconds", 3) * 1000;

        // Spam
        this.consecutiveSimilar = plugin.getConfig().getInt("antiflood.consecutive-similar", 2);
        this.similarityThreshold = plugin.getConfig().getDouble("antiflood.similarity-threshold", 0.7);

        // Punição
        this.basePunishmentMs = plugin.getConfig().getLong("antiflood.punishment-seconds", 5) * 1000;
        this.punishmentMultiplier = plugin.getConfig().getDouble("antiflood.punishment-multiplier", 2.0);
        this.maxPunishmentMs = plugin.getConfig().getLong("antiflood.max-punishment-seconds", 60) * 1000;
        this.violationDecayMs = plugin.getConfig().getLong("antiflood.violation-decay-seconds", 120) * 1000;

        // Histórico
        this.historySize = plugin.getConfig().getInt("antiflood.history-size", 10);
    }

    /**
     * Verifica se um jogador pode enviar uma mensagem.
     */
    public FloodResult canSendMessage(Player player, String message) {
        // Staff tem bypass
        if (player.hasPermission("haumea.chat.bypass.flood")) {
            return FloodResult.allowed();
        }

        UUID uuid = player.getUniqueId();
        FloodData data = playerData.computeIfAbsent(uuid, k -> new FloodData());

        long now = System.currentTimeMillis();

        // === VERIFICAR SE ESTÁ EM PUNIÇÃO ===
        if (data.isPunished(now)) {
            long remainingMs = data.punishmentEndTime - now;
            double remainingSeconds = remainingMs / 1000.0;
            return FloodResult.punished(remainingSeconds, data.violations);
        }

        // === VERIFICAR DECAY DE VIOLAÇÕES ===
        if (now - data.lastViolationTime > violationDecayMs && data.violations > 0) {
            data.violations = 0;
        }

        String normalizedMessage = normalizeMessage(message);

        // === DETECTAR SPAM (MENSAGENS IGUAIS/SIMILARES CONSECUTIVAS) ===
        // Isso funciona INDEPENDENTE do tempo!
        if (!data.messageHistory.isEmpty()) {
            int consecutiveCount = 0;

            // Verificar as últimas mensagens do histórico (do mais recente pro mais antigo)
            for (int i = data.messageHistory.size() - 1; i >= 0 && consecutiveCount < consecutiveSimilar; i--) {
                String historicMsg = data.messageHistory.get(i);
                double similarity = calculateSimilarity(historicMsg, normalizedMessage);

                if (similarity >= similarityThreshold) {
                    consecutiveCount++;
                } else {
                    break; // Parar quando encontrar mensagem diferente
                }
            }

            if (consecutiveCount >= consecutiveSimilar - 1) {
                // SPAM DETECTADO! Mensagens repetidas
                return applyPunishment(data, now, FloodType.SPAM);
            }
        }

        // === LIMPAR TIMESTAMPS ANTIGOS DA JANELA ===
        long windowStart = now - windowTimeMs;
        while (!data.messageTimestamps.isEmpty() && data.messageTimestamps.peekFirst() < windowStart) {
            data.messageTimestamps.pollFirst();
        }

        // === DETECTAR FLOOD (MUITAS MENSAGENS RÁPIDAS) ===
        if (data.messageTimestamps.size() >= maxMessagesInWindow) {
            return applyPunishment(data, now, FloodType.FLOOD);
        }

        // === MENSAGEM PERMITIDA ===
        data.messageTimestamps.addLast(now);
        data.messageHistory.addLast(normalizedMessage);

        // Manter histórico limitado
        while (data.messageHistory.size() > historySize) {
            data.messageHistory.pollFirst();
        }

        return FloodResult.allowed();
    }

    /**
     * Aplica punição ao jogador.
     */
    private FloodResult applyPunishment(FloodData data, long now, FloodType type) {
        data.violations++;
        data.lastViolationTime = now;

        // Calcular duração da punição (aumenta com violações)
        long punishmentDuration = (long) (basePunishmentMs * Math.pow(punishmentMultiplier, data.violations - 1));
        punishmentDuration = Math.min(punishmentDuration, maxPunishmentMs);

        data.punishmentEndTime = now + punishmentDuration;

        // Limpar histórico
        data.messageTimestamps.clear();
        data.messageHistory.clear();

        double punishmentSeconds = punishmentDuration / 1000.0;
        return FloodResult.floodDetected(punishmentSeconds, data.violations, type);
    }

    /**
     * Normaliza uma mensagem para comparação.
     */
    private String normalizeMessage(String message) {
        return message.toLowerCase()
                .replaceAll("\\s+", " ")
                .replaceAll("[^a-z0-9 ]", "")
                .trim();
    }

    /**
     * Calcula similaridade entre duas strings (0.0 a 1.0).
     */
    private double calculateSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null)
            return 0.0;
        if (s1.equals(s2))
            return 1.0;
        if (s1.isEmpty() || s2.isEmpty())
            return 0.0;

        int maxLen = Math.max(s1.length(), s2.length());
        int distance = levenshteinDistance(s1, s2);
        return 1.0 - ((double) distance / maxLen);
    }

    /**
     * Distância de Levenshtein.
     */
    private int levenshteinDistance(String s1, String s2) {
        int len1 = s1.length();
        int len2 = s2.length();
        int[][] dp = new int[len1 + 1][len2 + 1];

        for (int i = 0; i <= len1; i++)
            dp[i][0] = i;
        for (int j = 0; j <= len2; j++)
            dp[0][j] = j;

        for (int i = 1; i <= len1; i++) {
            for (int j = 1; j <= len2; j++) {
                int cost = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost);
            }
        }
        return dp[len1][len2];
    }

    /**
     * Limpa dados de um jogador.
     */
    public void clearPlayerData(UUID uuid) {
        playerData.remove(uuid);
    }

    /**
     * Obtém violações de um jogador.
     */
    public int getViolations(UUID uuid) {
        FloodData data = playerData.get(uuid);
        return data != null ? data.violations : 0;
    }

    // === CLASSES INTERNAS ===

    /**
     * Dados por jogador.
     */
    private static class FloodData {
        LinkedList<Long> messageTimestamps = new LinkedList<>(); // Para flood (com tempo)
        LinkedList<String> messageHistory = new LinkedList<>(); // Para spam (sem tempo)
        long punishmentEndTime = 0;
        long lastViolationTime = 0;
        int violations = 0;

        boolean isPunished(long now) {
            return punishmentEndTime > now;
        }
    }

    /**
     * Tipo de detecção.
     */
    public enum FloodType {
        FLOOD, // Muitas mensagens em pouco tempo
        SPAM // Mensagens iguais/similares consecutivas
    }

    /**
     * Resultado da verificação.
     */
    public static class FloodResult {
        private final boolean allowed;
        private final boolean floodDetected;
        private final double punishmentSeconds;
        private final int violations;
        private final FloodType floodType;

        private FloodResult(boolean allowed, boolean floodDetected, double punishmentSeconds,
                int violations, FloodType floodType) {
            this.allowed = allowed;
            this.floodDetected = floodDetected;
            this.punishmentSeconds = punishmentSeconds;
            this.violations = violations;
            this.floodType = floodType;
        }

        public static FloodResult allowed() {
            return new FloodResult(true, false, 0, 0, null);
        }

        public static FloodResult punished(double remainingSeconds, int violations) {
            return new FloodResult(false, false, remainingSeconds, violations, null);
        }

        public static FloodResult floodDetected(double punishmentSeconds, int violations, FloodType type) {
            return new FloodResult(false, true, punishmentSeconds, violations, type);
        }

        public boolean isAllowed() {
            return allowed;
        }

        public boolean isFloodDetected() {
            return floodDetected;
        }

        public double getPunishmentSeconds() {
            return punishmentSeconds;
        }

        public int getViolations() {
            return violations;
        }

        public FloodType getFloodType() {
            return floodType;
        }
    }
}
