package com.haumea.kitpvp.managers;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.database.MongoVipKeyRepository;
import com.haumea.kitpvp.models.Group;
import com.haumea.kitpvp.models.VipKey;
import com.haumea.kitpvp.utils.ChatStorage;
import com.haumea.kitpvp.utils.VisualManager;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.security.SecureRandom;
import java.util.*;

/**
 * Gerenciador de Chaves VIP
 * 
 * Responsável por:
 * - Gerar códigos únicos
 * - Criar e validar chaves
 * - Processar ativação com efeitos visuais
 * - Integração com GroupManager
 * 
 * @author HaumeaMC
 */
public class VipKeyManager {

    private final HaumeaMC plugin;
    private final MongoVipKeyRepository repository;
    private final SecureRandom random;

    // Caracteres para geração de código
    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int RANDOM_LENGTH = 8;

    /**
     * Resultado da tentativa de ativação de chave
     */
    public enum RedeemResult {
        SUCCESS, // VIP ativado com sucesso
        SUCCESS_EXTENDED, // VIP estendido (já tinha o grupo)
        KEY_NOT_FOUND, // Chave não existe
        KEY_ALREADY_USED, // Chave já foi usada
        KEY_EXPIRED, // Chave expirou
        GROUP_NOT_FOUND // Grupo não existe
    }

    public VipKeyManager(HaumeaMC plugin, MongoVipKeyRepository repository) {
        this.plugin = plugin;
        this.repository = repository;
        this.random = new SecureRandom();
    }

    // ==================== GERAÇÃO DE CHAVES ====================

    /**
     * Gera um código único no formato HAUMEA-{GRUPO}{DIAS}-{RANDOM}
     * Ex: HAUMEA-VIP30-ABCD1234
     */
    public String generateCode(String groupName, long durationDays) {
        String groupPart = groupName.toUpperCase().replace("+", "PLUS");
        String daysPart = durationDays == 0 ? "PERM" : String.valueOf(durationDays);
        String randomPart = generateRandomString(RANDOM_LENGTH);

        String code = "HAUMEA-" + groupPart + daysPart + "-" + randomPart;

        // Garante unicidade
        while (repository.exists(code)) {
            randomPart = generateRandomString(RANDOM_LENGTH);
            code = "HAUMEA-" + groupPart + daysPart + "-" + randomPart;
        }

        return code;
    }

    /**
     * Gera uma string aleatória
     */
    private String generateRandomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    // ==================== CRIAÇÃO DE CHAVES ====================

    /**
     * Cria uma nova chave VIP
     * 
     * @param groupName     Nome do grupo
     * @param duration      Duração do VIP em ms (0 = permanente)
     * @param keyExpiration Quando a chave expira (0 = nunca)
     * @param createdBy     Quem criou (UUID, "CONSOLE", "API")
     * @return A chave criada
     */
    public VipKey createKey(String groupName, long duration, long keyExpiration, String createdBy) {
        // Verifica se o grupo existe
        if (!plugin.getGroupManager().groupExists(groupName)) {
            return null;
        }

        // Calcula dias para o código
        long durationDays = duration / (1000 * 60 * 60 * 24);

        // Gera código
        String code = generateCode(groupName, durationDays);

        // Cria a chave
        VipKey key = new VipKey(code, groupName, duration, createdBy);
        if (keyExpiration > 0) {
            key.setKeyExpiresAt(keyExpiration);
        }

        // Salva no banco
        repository.saveAsync(key);

        return key;
    }

    /**
     * Cria múltiplas chaves VIP
     */
    public List<VipKey> createKeys(String groupName, long duration, long keyExpiration,
            String createdBy, int quantity) {
        List<VipKey> keys = new ArrayList<>();
        for (int i = 0; i < quantity; i++) {
            VipKey key = createKey(groupName, duration, keyExpiration, createdBy);
            if (key != null) {
                keys.add(key);
            }
        }
        return keys;
    }

    // ==================== ATIVAÇÃO DE CHAVES ====================

    /**
     * Tenta ativar uma chave para um jogador
     * 
     * @param player Jogador
     * @param code   Código da chave
     * @return Resultado da ativação
     */
    public RedeemResult redeemKey(Player player, String code) {
        // Busca a chave primeiro para validar
        VipKey existingKey = repository.findByCode(code);

        if (existingKey == null) {
            return RedeemResult.KEY_NOT_FOUND;
        }

        if (existingKey.isUsed()) {
            return RedeemResult.KEY_ALREADY_USED;
        }

        if (existingKey.isKeyExpired()) {
            return RedeemResult.KEY_EXPIRED;
        }

        // Verifica se o grupo existe
        Group group = plugin.getGroupManager().getGroup(existingKey.getGroupName());
        if (group == null) {
            return RedeemResult.GROUP_NOT_FOUND;
        }

        // Tenta ativar atomicamente
        VipKey redeemedKey = repository.redeemKey(code, player.getUniqueId());

        if (redeemedKey == null) {
            // Alguém ativou antes (race condition evitada)
            return RedeemResult.KEY_ALREADY_USED;
        }

        // Verifica se já tinha o grupo (para saber se é extensão)
        boolean hadGroup = plugin.getGroupManager().hasGroup(player, redeemedKey.getGroupName());

        // Calcula expiração do VIP
        long expiration = 0;
        if (redeemedKey.getDuration() > 0) {
            expiration = System.currentTimeMillis() + redeemedKey.getDuration();
        }

        // Aplica o grupo via GroupManager (já lida com acumulação)
        plugin.getGroupManager().addPlayerGroup(
                player.getUniqueId(),
                player.getName(),
                redeemedKey.getGroupName(),
                expiration);

        // Efeitos visuais e mensagens
        applyVisualEffects(player, redeemedKey, hadGroup);

        return hadGroup ? RedeemResult.SUCCESS_EXTENDED : RedeemResult.SUCCESS;
    }

    /**
     * Aplica efeitos visuais quando VIP é ativado
     */
    private void applyVisualEffects(Player player, VipKey key, boolean extended) {
        Group group = plugin.getGroupManager().getGroup(key.getGroupName());

        // Usa o prefixo colorido do grupo (como a tag aparece)
        String groupDisplay = group != null ? ChatStorage.colorize(group.getPrefix()) : key.getGroupName();
        String groupName = group != null ? group.getDisplayName() : key.getGroupName();
        String duration = key.isPermanent() ? "Permanente" : ChatStorage.formatTime(key.getDuration());

        // Mensagem bonita para o jogador (múltiplas linhas)
        String messageKey;
        if (extended) {
            messageKey = "vipkey.activation.extended";
        } else if (key.isPermanent()) {
            messageKey = "vipkey.activation.permanent";
        } else {
            messageKey = "vipkey.activation.temporary";
        }

        // Envia cada linha da mensagem de ativação
        List<String> activationLines = ChatStorage.getMessageList(messageKey);
        for (String line : activationLines) {
            String formatted = ChatStorage.colorize(line
                    .replace("{grupo}", groupDisplay)
                    .replace("{duracao}", duration)
                    .replace("{player}", player.getName()));
            player.sendMessage(formatted);
        }

        // Título na tela - usa o prefixo colorido
        String title = ChatStorage.colorize("&a&l✦ VIP ATIVADO ✦");
        String subtitle = groupDisplay;
        VisualManager.sendTitle(player, title, subtitle, 10, 80, 20);

        // Som para o jogador
        player.playSound(player.getLocation(), Sound.LEVEL_UP, 1.0f, 1.0f);

        // Broadcast global bonito (múltiplas linhas para todos)
        List<String> broadcastLines = ChatStorage.getMessageList("vipkey.broadcast");
        for (Player online : Bukkit.getOnlinePlayers()) {
            for (String line : broadcastLines) {
                String formatted = ChatStorage.colorize(line
                        .replace("{player}", player.getName())
                        .replace("{grupo}", groupDisplay)
                        .replace("{duracao}", duration));
                online.sendMessage(formatted);
            }
            // Som de notificação
            if (!online.equals(player)) {
                online.playSound(online.getLocation(), Sound.NOTE_PLING, 0.5f, 1.2f);
            }
        }
    }

    // ==================== CONSULTAS ====================

    /**
     * Obtém informações de uma chave
     */
    public VipKey getKeyInfo(String code) {
        return repository.findByCode(code);
    }

    /**
     * Lista chaves não usadas
     */
    public Collection<VipKey> getUnusedKeys() {
        return repository.getUnusedKeys();
    }

    /**
     * Lista chaves não usadas por grupo
     */
    public List<VipKey> getUnusedKeysByGroup(String groupName) {
        return repository.getUnusedKeysByGroup(groupName);
    }

    /**
     * Conta chaves não usadas
     */
    public int countUnused() {
        return repository.countUnused();
    }

    /**
     * Deleta uma chave
     */
    public boolean deleteKey(String code) {
        return repository.delete(code);
    }

    // ==================== MANUTENÇÃO ====================

    /**
     * Limpa chaves expiradas
     */
    public int cleanupExpiredKeys() {
        return repository.cleanupExpiredKeys();
    }

    // ==================== PARSING DE DURAÇÃO ====================

    /**
     * Converte string de duração para milissegundos
     * 
     * Formatos aceitos:
     * - 60m = minutos
     * - 24h = horas
     * - 30d = dias
     * - 1w = semanas
     * - 1mo = meses (30 dias)
     * - 1y = anos (365 dias)
     * - permanent/perm/permanente = permanente
     * 
     * @param input String de duração
     * @return Duração em ms, 0 se permanente, -1 se inválido
     */
    public static long parseDuration(String input) {
        if (input == null || input.isEmpty()) {
            return -1;
        }

        String lower = input.toLowerCase().trim();

        if (lower.equals("permanent") || lower.equals("perm") || lower.equals("permanente")) {
            return 0;
        }

        try {
            // Verificar sufixo de 2 caracteres (mo)
            if (lower.endsWith("mo")) {
                long value = Long.parseLong(lower.substring(0, lower.length() - 2));
                return value * 30L * 24L * 60L * 60L * 1000L; // meses (30 dias)
            }

            char unit = lower.charAt(lower.length() - 1);
            long value = Long.parseLong(lower.substring(0, lower.length() - 1));

            switch (unit) {
                case 'm':
                    return value * 60L * 1000L; // minutos
                case 'h':
                    return value * 60L * 60L * 1000L; // horas
                case 'd':
                    return value * 24L * 60L * 60L * 1000L; // dias
                case 'w':
                    return value * 7L * 24L * 60L * 60L * 1000L; // semanas
                case 'y':
                    return value * 365L * 24L * 60L * 60L * 1000L; // anos
                default:
                    return -1;
            }
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Formata duração em ms para string legível
     */
    public static String formatDuration(long ms) {
        if (ms == 0)
            return "Permanente";
        return ChatStorage.formatTime(ms);
    }
}
