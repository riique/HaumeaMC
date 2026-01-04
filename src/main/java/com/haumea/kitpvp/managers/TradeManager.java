package com.haumea.kitpvp.managers;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.models.PlayerData;
import com.haumea.kitpvp.profile.PlayerProfile;
import com.haumea.kitpvp.utils.ChatStorage;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gerenciador de Pagamentos/Transferências de Coins do HaumeaMC.
 * 
 * Sistema que permite jogadores transferirem coins para outros.
 * NÃO É UMA TROCA - é uma transferência unilateral com confirmação.
 * 
 * Implementa validações, taxas e cooldowns.
 * 
 * PERSISTÊNCIA:
 * - "payment_count" -> Total de pagamentos realizados (customData)
 * - "total_coins_sent" -> Total de coins enviados (customData)
 * - "total_coins_received" -> Total de coins recebidos (customData)
 * 
 * @author HaumeaMC
 */
public class TradeManager {

    private final HaumeaMC plugin;

    // Pagamentos pendentes (receiver UUID -> PaymentRequest)
    private final Map<UUID, PaymentRequest> pendingPayments;

    // Constantes
    private static final long MIN_AMOUNT = 10;
    private static final long MAX_AMOUNT = 100000;
    private static final double TAX_RATE = 0.02; // 2% de taxa
    private static final long MIN_TAX = 1; // Taxa mínima de 1 coin
    private static final long REQUEST_EXPIRE_MS = 60000; // 60 segundos

    // Chaves de customData
    private static final String DATA_KEY_PAYMENT_COUNT = "payment_count";
    private static final String DATA_KEY_COINS_SENT = "total_coins_sent";
    private static final String DATA_KEY_COINS_RECEIVED = "total_coins_received";

    /**
     * Representa um pagamento pendente
     */
    private static class PaymentRequest {
        final UUID sender;
        final String senderName;
        final long amount;
        final long timestamp;

        PaymentRequest(UUID sender, String senderName, long amount) {
            this.sender = sender;
            this.senderName = senderName;
            this.amount = amount;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > REQUEST_EXPIRE_MS;
        }
    }

    public TradeManager(HaumeaMC plugin) {
        this.plugin = plugin;
        this.pendingPayments = new ConcurrentHashMap<>();

        // Iniciar task de limpeza de pagamentos expirados (a cada 5 minutos)
        startCleanupTask();
    }

    /**
     * Inicia task para limpar pagamentos expirados periodicamente
     */
    private void startCleanupTask() {
        plugin.getServer().getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                cleanupExpired();
            }
        }, 20L * 60L * 5L, 20L * 60L * 5L); // A cada 5 minutos
    }

    /**
     * Envia um pagamento para outro jogador
     * 
     * @param sender   Jogador que está pagando
     * @param receiver Jogador que vai receber
     * @param amount   Quantidade de coins
     * @return true se o pedido foi enviado
     */
    public boolean sendTradeRequest(Player sender, Player receiver, long amount) {
        // Validações
        if (sender.equals(receiver)) {
            ChatStorage.send(sender, "pay.error.self");
            return false;
        }

        if (amount < MIN_AMOUNT) {
            ChatStorage.send(sender, "pay.error.min-amount", "min", String.valueOf(MIN_AMOUNT));
            return false;
        }

        if (amount > MAX_AMOUNT) {
            ChatStorage.send(sender, "pay.error.max-amount", "max", ChatStorage.formatNumber(MAX_AMOUNT));
            return false;
        }

        // Verificar saldo do sender
        PlayerProfile senderProfile = plugin.getProfileManager().getProfile(sender);
        if (senderProfile == null || senderProfile.getCoins() < amount) {
            ChatStorage.send(sender, "pay.error.not-enough");
            return false;
        }

        // Verificar cooldown
        CooldownManager cooldownManager = plugin.getCooldownManager();
        if (cooldownManager != null && cooldownManager.isOnCooldown(sender, "pay")) {
            String remaining = cooldownManager.getRemainingFormatted(sender, "pay");
            ChatStorage.send(sender, "pay.error.cooldown", "time", remaining);
            return false;
        }

        // Verificar se já tem pedido pendente para este receiver
        UUID receiverUuid = receiver.getUniqueId();
        PaymentRequest existing = pendingPayments.get(receiverUuid);
        if (existing != null && !existing.isExpired() && existing.sender.equals(sender.getUniqueId())) {
            ChatStorage.send(sender, "pay.error.pending");
            return false;
        }

        // Criar pedido
        pendingPayments.put(receiverUuid, new PaymentRequest(sender.getUniqueId(), sender.getName(), amount));

        // Calcular taxa para mostrar (arredondamento para cima, mínimo 1 coin)
        long tax = calculateTax(amount);
        long netAmount = amount - tax;

        // Notificar sender (quem está pagando)
        ChatStorage.sendRaw(sender, "");
        ChatStorage.send(sender, "pay.sent.header");
        ChatStorage.sendRaw(sender, "");
        ChatStorage.send(sender, "pay.sent.title", "player", receiver.getName());
        ChatStorage.sendRaw(sender, "");
        ChatStorage.send(sender, "pay.sent.amount", "amount", ChatStorage.formatNumber(amount));
        ChatStorage.send(sender, "pay.sent.tax", "tax", ChatStorage.formatNumber(tax));
        ChatStorage.send(sender, "pay.sent.net", "net", ChatStorage.formatNumber(netAmount));
        ChatStorage.sendRaw(sender, "");
        ChatStorage.send(sender, "pay.sent.waiting", "player", receiver.getName());
        ChatStorage.send(sender, "pay.sent.expiry");
        ChatStorage.sendRaw(sender, "");
        ChatStorage.send(sender, "pay.sent.footer");
        ChatStorage.sendRaw(sender, "");

        // Notificar receiver (quem vai receber)
        ChatStorage.sendRaw(receiver, "");
        ChatStorage.send(receiver, "pay.received.header");
        ChatStorage.sendRaw(receiver, "");
        ChatStorage.send(receiver, "pay.received.title", "player", sender.getName());
        ChatStorage.sendRaw(receiver, "");
        ChatStorage.send(receiver, "pay.received.amount", "net", ChatStorage.formatNumber(netAmount));
        ChatStorage.sendRaw(receiver, "");
        ChatStorage.send(receiver, "pay.received.accept");
        ChatStorage.send(receiver, "pay.received.deny");
        ChatStorage.sendRaw(receiver, "");
        ChatStorage.send(receiver, "pay.received.expiry");
        ChatStorage.sendRaw(receiver, "");
        ChatStorage.send(receiver, "pay.received.footer");
        ChatStorage.sendRaw(receiver, "");

        receiver.playSound(receiver.getLocation(), Sound.NOTE_PLING, 1.0f, 1.5f);

        return true;
    }

    /**
     * Aceita um pagamento pendente
     */
    public boolean acceptTrade(Player receiver) {
        UUID receiverUuid = receiver.getUniqueId();
        PaymentRequest request = pendingPayments.remove(receiverUuid);

        if (request == null) {
            ChatStorage.send(receiver, "pay.error.no-pending");
            return false;
        }

        if (request.isExpired()) {
            ChatStorage.send(receiver, "pay.error.expired", "player", request.senderName);
            return false;
        }

        // Verificar se sender está online
        Player sender = plugin.getServer().getPlayer(request.sender);
        if (sender == null || !sender.isOnline()) {
            ChatStorage.send(receiver, "pay.error.sender-offline");
            return false;
        }

        // Verificar saldo do sender novamente
        PlayerProfile senderProfile = plugin.getProfileManager().getProfile(sender);
        if (senderProfile == null || senderProfile.getCoins() < request.amount) {
            ChatStorage.send(receiver, "pay.error.sender-insufficient");
            ChatStorage.send(sender, "pay.error.not-enough");
            return false;
        }

        // Executar transferência
        long tax = calculateTax(request.amount);
        long netAmount = request.amount - tax;

        senderProfile.removeCoins(request.amount);

        PlayerProfile receiverProfile = plugin.getProfileManager().getProfile(receiver);
        if (receiverProfile != null) {
            receiverProfile.addCoins(netAmount);
        }

        // Registrar estatísticas no customData para persistência
        PlayerData senderData = senderProfile.getData();
        PlayerData receiverData = receiverProfile != null ? receiverProfile.getData() : null;

        // Atualizar contagem de pagamentos
        int senderPaymentCount = senderData.getCustomData(DATA_KEY_PAYMENT_COUNT, 0);
        senderData.setCustomData(DATA_KEY_PAYMENT_COUNT, senderPaymentCount + 1);

        if (receiverData != null) {
            int receiverPaymentCount = receiverData.getCustomData(DATA_KEY_PAYMENT_COUNT, 0);
            receiverData.setCustomData(DATA_KEY_PAYMENT_COUNT, receiverPaymentCount + 1);
        }

        // Atualizar total de coins enviados/recebidos
        long totalSent = senderData.getCustomData(DATA_KEY_COINS_SENT, 0L);
        senderData.setCustomData(DATA_KEY_COINS_SENT, totalSent + request.amount);

        if (receiverData != null) {
            long totalReceived = receiverData.getCustomData(DATA_KEY_COINS_RECEIVED, 0L);
            receiverData.setCustomData(DATA_KEY_COINS_RECEIVED, totalReceived + netAmount);
        }

        // Verificar conquista
        if (plugin.getAchievementManager() != null) {
            plugin.getAchievementManager().incrementSpecialAchievement(sender, "generous");
            plugin.getAchievementManager().incrementSpecialAchievement(receiver, "receiver");
        }

        // Aplicar cooldown no sender
        CooldownManager cooldownManager = plugin.getCooldownManager();
        if (cooldownManager != null) {
            cooldownManager.setCooldown(sender, "pay", 30000); // 30 segundos
        }

        // Notificar sender (quem pagou)
        ChatStorage.sendRaw(sender, "");
        ChatStorage.send(sender, "pay.success.sender.title");
        ChatStorage.send(sender, "pay.success.sender.message",
                "amount", ChatStorage.formatNumber(request.amount),
                "player", receiver.getName());
        ChatStorage.send(sender, "pay.success.sender.tax", "tax", ChatStorage.formatNumber(tax));
        ChatStorage.sendRaw(sender, "");
        sender.playSound(sender.getLocation(), Sound.LEVEL_UP, 1.0f, 1.0f);

        // Notificar receiver (quem recebeu)
        ChatStorage.sendRaw(receiver, "");
        ChatStorage.send(receiver, "pay.success.receiver.title");
        ChatStorage.send(receiver, "pay.success.receiver.message",
                "net", ChatStorage.formatNumber(netAmount),
                "player", sender.getName());
        ChatStorage.sendRaw(receiver, "");
        receiver.playSound(receiver.getLocation(), Sound.LEVEL_UP, 1.0f, 1.0f);

        return true;
    }

    /**
     * Recusa um pagamento pendente
     */
    public boolean denyTrade(Player receiver) {
        UUID receiverUuid = receiver.getUniqueId();
        PaymentRequest request = pendingPayments.remove(receiverUuid);

        if (request == null) {
            ChatStorage.send(receiver, "pay.error.no-pending");
            return false;
        }

        // Notificar receiver
        ChatStorage.send(receiver, "pay.denied.receiver", "player", request.senderName);

        // Notificar sender se online
        Player sender = plugin.getServer().getPlayer(request.sender);
        if (sender != null && sender.isOnline()) {
            ChatStorage.send(sender, "pay.denied.sender", "player", receiver.getName());
        }

        return true;
    }

    /**
     * Verifica se jogador tem pagamento pendente
     */
    public boolean hasPendingTrade(Player receiver) {
        PaymentRequest request = pendingPayments.get(receiver.getUniqueId());
        return request != null && !request.isExpired();
    }

    /**
     * Obtém quantidade de pagamentos de um jogador (do PlayerData persistido)
     */
    public int getTradeCount(UUID uuid) {
        Player player = plugin.getServer().getPlayer(uuid);
        if (player != null) {
            PlayerProfile profile = plugin.getProfileManager().getProfile(player);
            if (profile != null) {
                return profile.getData().getCustomData(DATA_KEY_PAYMENT_COUNT, 0);
            }
        }
        return 0;
    }

    /**
     * Obtém total de coins enviados por um jogador
     */
    public long getTotalCoinsSent(Player player) {
        PlayerProfile profile = plugin.getProfileManager().getProfile(player);
        if (profile != null) {
            return profile.getData().getCustomData(DATA_KEY_COINS_SENT, 0L);
        }
        return 0L;
    }

    /**
     * Obtém total de coins recebidos por um jogador
     */
    public long getTotalCoinsReceived(Player player) {
        PlayerProfile profile = plugin.getProfileManager().getProfile(player);
        if (profile != null) {
            return profile.getData().getCustomData(DATA_KEY_COINS_RECEIVED, 0L);
        }
        return 0L;
    }

    /**
     * Limpa pagamentos expirados
     */
    public void cleanupExpired() {
        pendingPayments.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    /**
     * Limpa pedido ao jogador sair
     */
    public void onPlayerQuit(Player player) {
        pendingPayments.remove(player.getUniqueId());
    }

    /**
     * Calcula a taxa de transferência.
     * Usa arredondamento matemático (não truncamento) e garante taxa mínima de 1
     * coin.
     * 
     * @param amount Valor da transferência
     * @return Taxa calculada (mínimo 1 coin)
     */
    private long calculateTax(long amount) {
        // Calcula 2% com arredondamento
        long tax = Math.round(amount * TAX_RATE);

        // Garante taxa mínima de 1 coin
        if (tax < MIN_TAX) {
            tax = MIN_TAX;
        }

        return tax;
    }
}
