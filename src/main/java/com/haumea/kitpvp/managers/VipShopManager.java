package com.haumea.kitpvp.managers;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.models.Group;
import com.haumea.kitpvp.profile.PlayerProfile;
import com.haumea.kitpvp.utils.ChatStorage;
import com.haumea.kitpvp.utils.VisualManager;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Gerenciador de Loja de VIPs por Coins
 * 
 * Responsável por:
 * - Configurar preços dos VIPs semanais
 * - Processar compras por coins
 * - Integrar com GroupManager para ativação
 * 
 * @author HaumeaMC
 */
public class VipShopManager {

    private final HaumeaMC plugin;

    // Definição dos VIPs disponíveis para compra
    private final List<VipOffer> vipOffers;

    // Tempo de 1 semana em milissegundos
    private static final long ONE_WEEK_MS = 7L * 24L * 60L * 60L * 1000L;

    public VipShopManager(HaumeaMC plugin) {
        this.plugin = plugin;
        this.vipOffers = new ArrayList<>();
        loadVipOffers();

        plugin.getLogger().info("[VipShop] Gerenciador de loja de VIPs inicializado.");
    }

    /**
     * Carrega as ofertas de VIP disponíveis
     * VIPs organizados do mais barato ao mais caro
     */
    private void loadVipOffers() {
        vipOffers.clear();

        // VIP Light - Mais acessível
        vipOffers.add(new VipOffer(
                "light",
                "&a&lLIGHT",
                "§a§lLIGHT §a",
                15000, // 15k coins por semana
                ONE_WEEK_MS,
                new String[] {
                        "&7O essencial para começar!",
                        "",
                        "&6Benefícios:",
                        "&8▪ &fTag exclusiva &a&lLIGHT",
                        "&8▪ &fKit especial de VIP",
                        "&8▪ &fCoins bônus por kill (+10%)",
                        "&8▪ &fAcesso ao /craft",
                        ""
                }));

        // VIP Premium - Intermediário
        vipOffers.add(new VipOffer(
                "premium",
                "&6&lPREMIUM",
                "§6§lPREMIUM §6",
                35000, // 35k coins por semana
                ONE_WEEK_MS,
                new String[] {
                        "&7O equilíbrio perfeito!",
                        "",
                        "&6Benefícios:",
                        "&8▪ &fTudo do &a&lLIGHT &f+",
                        "&8▪ &fTag exclusiva &6&lPREMIUM",
                        "&8▪ &fKit premium aprimorado",
                        "&8▪ &fCoins bônus por kill (+25%)",
                        "&8▪ &fAcesso ao /fly no lobby",
                        "&8▪ &f2 kits extras gratuitos",
                        ""
                }));

        // VIP Beta - Avançado
        vipOffers.add(new VipOffer(
                "beta",
                "&1&lBETA",
                "§1§lBETA §1",
                60000, // 60k coins por semana
                ONE_WEEK_MS,
                new String[] {
                        "&7Para jogadores experientes!",
                        "",
                        "&6Benefícios:",
                        "&8▪ &fTudo do &6&lPREMIUM &f+",
                        "&8▪ &fTag exclusiva &1&lBETA",
                        "&8▪ &fKit beta exclusivo",
                        "&8▪ &fCoins bônus por kill (+40%)",
                        "&8▪ &fMultiplicador 2x em eventos",
                        "&8▪ &f4 kits extras gratuitos",
                        "&8▪ &fPrioridade em filas",
                        ""
                }));

        // VIP Ultra - Máximo
        vipOffers.add(new VipOffer(
                "ultra",
                "&d&lULTRA",
                "§d§lULTRA §d",
                100000, // 100k coins por semana
                ONE_WEEK_MS,
                new String[] {
                        "&dO melhor do HaumeaMC!",
                        "",
                        "&6Benefícios:",
                        "&8▪ &fTudo do &1&lBETA &f+",
                        "&8▪ &fTag exclusiva &d&lULTRA",
                        "&8▪ &fKit ultra lendário",
                        "&8▪ &fCoins bônus por kill (+60%)",
                        "&8▪ &fMultiplicador 3x em eventos",
                        "&8▪ &fTodos os kits gratuitos",
                        "&8▪ &fEfeitos visuais exclusivos",
                        "&8▪ &fNick colorido no chat",
                        ""
                }));
    }

    /**
     * Obtém todas as ofertas de VIP disponíveis
     */
    public List<VipOffer> getVipOffers() {
        return Collections.unmodifiableList(vipOffers);
    }

    /**
     * Obtém uma oferta específica pelo nome do grupo
     */
    public VipOffer getOfferByGroup(String groupName) {
        for (VipOffer offer : vipOffers) {
            if (offer.getGroupName().equalsIgnoreCase(groupName)) {
                return offer;
            }
        }
        return null;
    }

    /**
     * Verifica se o jogador pode comprar o VIP
     * 
     * @return Resultado da verificação
     */
    public PurchaseResult canPurchase(Player player, VipOffer offer) {
        ProfileManager profileManager = plugin.getProfileManager();
        GroupManager groupManager = plugin.getGroupManager();

        if (profileManager == null || groupManager == null) {
            return PurchaseResult.ERROR;
        }

        PlayerProfile profile = profileManager.getProfile(player);
        if (profile == null) {
            return PurchaseResult.ERROR;
        }

        // Verificar se já possui VIP permanente (comprado com dinheiro real)
        // VIP permanente tem expiration = 0
        long expiration = groupManager.getGroupExpiration(player.getUniqueId(), offer.getGroupName());
        if (expiration == 0) {
            // -1 significa que não possui, 0 significa permanente
            if (groupManager.hasGroup(player, offer.getGroupName())) {
                return PurchaseResult.ALREADY_PERMANENT;
            }
        }

        // Verificar coins
        if (profile.getCoins() < offer.getPrice()) {
            return PurchaseResult.NOT_ENOUGH_COINS;
        }

        return PurchaseResult.CAN_PURCHASE;
    }

    /**
     * Processa a compra do VIP
     * 
     * @return true se a compra foi bem sucedida
     */
    public boolean purchaseVip(Player player, VipOffer offer) {
        ProfileManager profileManager = plugin.getProfileManager();
        GroupManager groupManager = plugin.getGroupManager();

        if (profileManager == null || groupManager == null) {
            return false;
        }

        PlayerProfile profile = profileManager.getProfile(player);
        if (profile == null) {
            return false;
        }

        // Verificar novamente antes de cobrar
        PurchaseResult check = canPurchase(player, offer);
        if (check != PurchaseResult.CAN_PURCHASE) {
            return false;
        }

        // Remover coins
        if (!profile.removeCoins(offer.getPrice())) {
            return false;
        }

        // Verificar se já possui o grupo (para extensão)
        boolean hadGroup = groupManager.hasGroup(player, offer.getGroupName());

        // Calcular expiração (1 semana a partir de agora, ou soma se já possui)
        long newExpiration = System.currentTimeMillis() + offer.getDuration();

        // Adicionar grupo (GroupManager já lida com acumulação de tempo)
        groupManager.addPlayerGroup(
                player.getUniqueId(),
                player.getName(),
                offer.getGroupName(),
                newExpiration);

        // Efeitos visuais
        applyPurchaseEffects(player, offer, hadGroup);

        // Log
        plugin.getLogger().info("[VipShop] " + player.getName() + " comprou " +
                offer.getGroupName() + " por " + offer.getPrice() + " coins (1 semana)");

        return true;
    }

    /**
     * Aplica efeitos visuais quando VIP é comprado
     */
    private void applyPurchaseEffects(Player player, VipOffer offer, boolean extended) {
        String groupDisplay = ChatStorage.colorize(offer.getDisplayName());
        String duration = "7 dias";

        // Título na tela
        if (extended) {
            String title = ChatStorage.colorize("&a&l✦ VIP ESTENDIDO ✦");
            String subtitle = groupDisplay + " §f+ 7 dias";
            VisualManager.sendTitle(player, title, subtitle, 10, 80, 20);
        } else {
            String title = ChatStorage.colorize("&a&l★ VIP ATIVADO ★");
            String subtitle = groupDisplay;
            VisualManager.sendTitle(player, title, subtitle, 10, 80, 20);
        }

        // Som para o jogador
        player.playSound(player.getLocation(), Sound.LEVEL_UP, 1.0f, 1.0f);

        // Mensagem bonita no chat
        ChatStorage.sendRaw(player, "");
        ChatStorage.sendRaw(player, "&8&m━━━━━━━━━━&r  " + groupDisplay + "  &8&m━━━━━━━━━━&r");
        ChatStorage.sendRaw(player, "");
        if (extended) {
            ChatStorage.sendRaw(player, "  &a&l✔ VIP ESTENDIDO COM SUCESSO!");
            ChatStorage.sendRaw(player, "");
            ChatStorage.sendRaw(player, "  &fSeu " + groupDisplay + " &ffoi estendido!");
        } else {
            ChatStorage.sendRaw(player, "  &a&l★ VIP ATIVADO COM SUCESSO! ★");
            ChatStorage.sendRaw(player, "");
            ChatStorage.sendRaw(player, "  &fVocê agora é " + groupDisplay + "&f!");
        }
        ChatStorage.sendRaw(player, "  &fDuração: &e" + duration);
        ChatStorage.sendRaw(player, "");
        ChatStorage.sendRaw(player, "  &7Aproveite seus benefícios exclusivos!");
        ChatStorage.sendRaw(player, "");
        ChatStorage.sendRaw(player, "&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━&r");
        ChatStorage.sendRaw(player, "");

        // Broadcast para o servidor (apenas se não for extensão)
        if (!extended) {
            String broadcastMsg = ChatStorage.colorize(
                    "&8[&6&lVIP&8] &e" + player.getName() + " &fadquiriu " + groupDisplay + "&f!");
            for (Player online : Bukkit.getOnlinePlayers()) {
                online.sendMessage(broadcastMsg);
                if (!online.equals(player)) {
                    online.playSound(online.getLocation(), Sound.NOTE_PLING, 0.5f, 1.2f);
                }
            }
        }
    }

    /**
     * Obtém o tempo restante do VIP de um jogador
     * 
     * @return tempo em ms, 0 se permanente, -1 se não possui
     */
    public long getVipTimeRemaining(Player player, String groupName) {
        GroupManager groupManager = plugin.getGroupManager();
        if (groupManager == null) {
            return -1;
        }
        return groupManager.getTimeRemaining(player.getUniqueId(), groupName);
    }

    /**
     * Verifica se o jogador possui VIP permanente (comprado com dinheiro real)
     */
    public boolean hasPermanentVip(Player player, String groupName) {
        GroupManager groupManager = plugin.getGroupManager();
        if (groupManager == null) {
            return false;
        }

        if (!groupManager.hasGroup(player, groupName)) {
            return false;
        }

        // Expiração 0 = permanente
        long expiration = groupManager.getGroupExpiration(player.getUniqueId(), groupName);
        return expiration == 0;
    }

    // ==================== CLASSES INTERNAS ====================

    /**
     * Representa uma oferta de VIP na loja
     */
    public static class VipOffer {
        private final String groupName;
        private final String displayName;
        private final String prefix;
        private final int price;
        private final long duration;
        private final String[] description;

        public VipOffer(String groupName, String displayName, String prefix,
                int price, long duration, String[] description) {
            this.groupName = groupName;
            this.displayName = displayName;
            this.prefix = prefix;
            this.price = price;
            this.duration = duration;
            this.description = description;
        }

        public String getGroupName() {
            return groupName;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getPrefix() {
            return prefix;
        }

        public int getPrice() {
            return price;
        }

        public long getDuration() {
            return duration;
        }

        public String[] getDescription() {
            return description;
        }
    }

    /**
     * Resultado da verificação de compra
     */
    public enum PurchaseResult {
        CAN_PURCHASE, // Pode comprar
        NOT_ENOUGH_COINS, // Coins insuficientes
        ALREADY_PERMANENT, // Já possui permanente
        ERROR // Erro genérico
    }
}
