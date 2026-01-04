package com.haumea.kitpvp.managers;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.models.ActiveMultiplier;
import com.haumea.kitpvp.models.MultiplierType;
import com.haumea.kitpvp.models.PlayerData;
import com.haumea.kitpvp.profile.PlayerProfile;
import com.haumea.kitpvp.utils.ChatStorage;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gerenciador Unificado de Multiplicadores de Coins.
 * 
 * Este manager centraliza TODA a lógica de multiplicadores:
 * - Armazenamento e gerenciamento dos multiplicadores disponíveis
 * - Controle dos multiplicadores ativos por jogador
 * - Cálculo do multiplicador atual de cada jogador
 * - Verificação de expiração de multiplicadores
 * - Persistência dos dados de multiplicadores
 * 
 * Utiliza o sistema de customData do PlayerData para persistência.
 * 
 * Chaves de customData utilizadas:
 * - "multiplier_inventory": Map<String, Integer> (tipo -> quantidade)
 * - "multiplier_active": String serializado "TYPE:EXPIRATION"
 * 
 * @author HaumeaMC
 */
public class MultiplierManager {

    private final HaumeaMC plugin;

    // Cache de multiplicadores ativos (para acesso rápido)
    private final Map<UUID, ActiveMultiplier> activeMultipliers;

    // Chaves de persistência
    private static final String KEY_INVENTORY = "multiplier_inventory";
    private static final String KEY_ACTIVE = "multiplier_active";

    /**
     * Construtor do MultiplierManager
     * 
     * @param plugin Instância do plugin
     */
    public MultiplierManager(HaumeaMC plugin) {
        this.plugin = plugin;
        this.activeMultipliers = new ConcurrentHashMap<>();
    }

    // ==================== INVENTÁRIO DE MULTIPLICADORES ====================

    /**
     * Obtém a quantidade de um tipo de multiplicador no inventário do jogador
     * 
     * @param player Jogador
     * @param type   Tipo do multiplicador
     * @return Quantidade disponível
     */
    public int getInventoryCount(Player player, MultiplierType type) {
        PlayerProfile profile = plugin.getProfileManager().getProfile(player);
        if (profile == null)
            return 0;

        return getInventoryCountFromData(profile.getData(), type);
    }

    /**
     * Obtém a quantidade de um tipo de multiplicador dos dados do jogador
     */
    @SuppressWarnings("unchecked")
    private int getInventoryCountFromData(PlayerData data, MultiplierType type) {
        Object inventoryObj = data.getCustomData(KEY_INVENTORY);
        if (inventoryObj instanceof Map) {
            Map<String, Object> inventory = (Map<String, Object>) inventoryObj;
            Object count = inventory.get(type.name());
            if (count instanceof Number) {
                return ((Number) count).intValue();
            }
        }
        return 0;
    }

    /**
     * Obtém o inventário completo de multiplicadores do jogador
     * 
     * @param player Jogador
     * @return Mapa de tipo -> quantidade
     */
    public Map<MultiplierType, Integer> getFullInventory(Player player) {
        PlayerProfile profile = plugin.getProfileManager().getProfile(player);
        if (profile == null)
            return Collections.emptyMap();

        return getFullInventoryFromData(profile.getData());
    }

    /**
     * Obtém o inventário completo dos dados do jogador
     */
    @SuppressWarnings("unchecked")
    private Map<MultiplierType, Integer> getFullInventoryFromData(PlayerData data) {
        Map<MultiplierType, Integer> result = new LinkedHashMap<>();

        Object inventoryObj = data.getCustomData(KEY_INVENTORY);
        if (inventoryObj instanceof Map) {
            Map<String, Object> inventory = (Map<String, Object>) inventoryObj;
            for (MultiplierType type : MultiplierType.values()) {
                Object count = inventory.get(type.name());
                int amount = (count instanceof Number) ? ((Number) count).intValue() : 0;
                result.put(type, amount);
            }
        } else {
            // Inicializar com zeros
            for (MultiplierType type : MultiplierType.values()) {
                result.put(type, 0);
            }
        }

        return result;
    }

    /**
     * Adiciona multiplicadores ao inventário do jogador
     * 
     * @param player Jogador
     * @param type   Tipo do multiplicador
     * @param amount Quantidade a adicionar
     * @return true se adicionou com sucesso
     */
    public boolean addToInventory(Player player, MultiplierType type, int amount) {
        PlayerProfile profile = plugin.getProfileManager().getProfile(player);
        if (profile == null || amount <= 0)
            return false;

        return addToInventoryData(profile.getData(), type, amount);
    }

    /**
     * Adiciona multiplicadores diretamente nos dados
     */
    @SuppressWarnings("unchecked")
    private boolean addToInventoryData(PlayerData data, MultiplierType type, int amount) {
        Object inventoryObj = data.getCustomData(KEY_INVENTORY);
        Map<String, Object> inventory;

        if (inventoryObj instanceof Map) {
            inventory = new HashMap<>((Map<String, Object>) inventoryObj);
        } else {
            inventory = new HashMap<>();
        }

        int current = 0;
        Object currentObj = inventory.get(type.name());
        if (currentObj instanceof Number) {
            current = ((Number) currentObj).intValue();
        }

        inventory.put(type.name(), current + amount);
        data.setCustomData(KEY_INVENTORY, inventory);

        return true;
    }

    /**
     * Remove multiplicadores do inventário do jogador
     * 
     * @param player Jogador
     * @param type   Tipo do multiplicador
     * @param amount Quantidade a remover
     * @return true se removeu com sucesso (tinha quantidade suficiente)
     */
    public boolean removeFromInventory(Player player, MultiplierType type, int amount) {
        PlayerProfile profile = plugin.getProfileManager().getProfile(player);
        if (profile == null || amount <= 0)
            return false;

        return removeFromInventoryData(profile.getData(), type, amount);
    }

    /**
     * Remove multiplicadores diretamente dos dados
     */
    @SuppressWarnings("unchecked")
    private boolean removeFromInventoryData(PlayerData data, MultiplierType type, int amount) {
        Object inventoryObj = data.getCustomData(KEY_INVENTORY);
        if (!(inventoryObj instanceof Map))
            return false;

        Map<String, Object> inventory = new HashMap<>((Map<String, Object>) inventoryObj);

        Object currentObj = inventory.get(type.name());
        int current = (currentObj instanceof Number) ? ((Number) currentObj).intValue() : 0;

        if (current < amount)
            return false;

        inventory.put(type.name(), current - amount);
        data.setCustomData(KEY_INVENTORY, inventory);

        return true;
    }

    /**
     * Define a quantidade exata de um multiplicador no inventário
     * 
     * @param player Jogador
     * @param type   Tipo do multiplicador
     * @param amount Nova quantidade
     */
    @SuppressWarnings("unchecked")
    public void setInventoryCount(Player player, MultiplierType type, int amount) {
        PlayerProfile profile = plugin.getProfileManager().getProfile(player);
        if (profile == null)
            return;

        PlayerData data = profile.getData();
        Object inventoryObj = data.getCustomData(KEY_INVENTORY);
        Map<String, Object> inventory;

        if (inventoryObj instanceof Map) {
            inventory = new HashMap<>((Map<String, Object>) inventoryObj);
        } else {
            inventory = new HashMap<>();
        }

        inventory.put(type.name(), Math.max(0, amount));
        data.setCustomData(KEY_INVENTORY, inventory);
    }

    /**
     * Obtém o total de multiplicadores que o jogador possui
     * 
     * @param player Jogador
     * @return Total de multiplicadores de todos os tipos
     */
    public int getTotalInventoryCount(Player player) {
        Map<MultiplierType, Integer> inventory = getFullInventory(player);
        return inventory.values().stream().mapToInt(Integer::intValue).sum();
    }

    // ==================== MULTIPLICADOR ATIVO ====================

    /**
     * Obtém o multiplicador ativo do jogador
     * 
     * @param player Jogador
     * @return ActiveMultiplier ou null se não houver
     */
    public ActiveMultiplier getActiveMultiplier(Player player) {
        UUID uuid = player.getUniqueId();

        // Verificar cache primeiro
        ActiveMultiplier cached = activeMultipliers.get(uuid);
        if (cached != null) {
            // Verificar expiração
            if (cached.isExpired()) {
                clearActiveMultiplier(player, true);
                return null;
            }
            return cached;
        }

        // Carregar dos dados se não estiver em cache
        PlayerProfile profile = plugin.getProfileManager().getProfile(player);
        if (profile == null)
            return null;

        String serialized = profile.getData().getCustomData(KEY_ACTIVE, String.class);
        if (serialized == null || serialized.isEmpty())
            return null;

        ActiveMultiplier active = ActiveMultiplier.deserialize(uuid, serialized);
        if (active != null && !active.isExpired()) {
            activeMultipliers.put(uuid, active);
            return active;
        } else if (active != null) {
            // Expirado, limpar
            clearActiveMultiplier(player, true);
        }

        return null;
    }

    /**
     * Verifica se o jogador tem um multiplicador ativo
     * 
     * @param player Jogador
     * @return true se tem multiplicador ativo não expirado
     */
    public boolean hasActiveMultiplier(Player player) {
        return getActiveMultiplier(player) != null;
    }

    /**
     * Obtém o valor do multiplicador atual do jogador
     * 
     * @param player Jogador
     * @return Valor do multiplicador (1.0 se não houver ativo)
     */
    public double getMultiplierValue(Player player) {
        ActiveMultiplier active = getActiveMultiplier(player);
        return (active != null) ? active.getType().getValue() : 1.0;
    }

    /**
     * Ativa um multiplicador para o jogador.
     * 
     * Comportamento:
     * - Se NÃO tem multiplicador ativo: ativa o novo
     * - Se JÁ tem multiplicador ativo DO MESMO TIPO: estende o tempo
     * - Se JÁ tem multiplicador ativo DE TIPO DIFERENTE:
     * - Se o novo é maior: substitui pelo novo
     * - Se o novo é menor ou igual: estende o tempo do atual
     * 
     * @param player Jogador
     * @param type   Tipo do multiplicador a ativar
     * @return true se ativou com sucesso
     */
    public boolean activateMultiplier(Player player, MultiplierType type) {
        return activateMultiplier(player, type, type.getDefaultDuration());
    }

    /**
     * Ativa um multiplicador para o jogador com duração customizada
     * 
     * @param player         Jogador
     * @param type           Tipo do multiplicador a ativar
     * @param durationMillis Duração em milissegundos
     * @return true se ativou com sucesso
     */
    public boolean activateMultiplier(Player player, MultiplierType type, long durationMillis) {
        // Verificar se tem no inventário
        int available = getInventoryCount(player, type);
        if (available <= 0) {
            ChatStorage.send(player, "multiplier.not-available", "type", type.getDisplayMultiplier());
            return false;
        }

        UUID uuid = player.getUniqueId();
        ActiveMultiplier current = getActiveMultiplier(player);

        if (current != null) {
            // Já tem multiplicador ativo
            if (current.getType() == type) {
                // Mesmo tipo: estender tempo
                current.extendTime(durationMillis);
                saveActiveMultiplier(player, current);
                removeFromInventory(player, type, 1);

                ChatStorage.send(player, "multiplier.extended",
                        "type", type.getDisplayMultiplier(),
                        "time", current.getFormattedRemainingTime());

                return true;

            } else if (type.getValue() > current.getType().getValue()) {
                // Novo é maior: substituir
                ActiveMultiplier newActive = ActiveMultiplier.createWithDuration(uuid, type, durationMillis);
                activeMultipliers.put(uuid, newActive);
                saveActiveMultiplier(player, newActive);
                removeFromInventory(player, type, 1);

                ChatStorage.send(player, "multiplier.upgraded",
                        "old_type", current.getType().getDisplayMultiplier(),
                        "new_type", type.getDisplayMultiplier(),
                        "time", newActive.getFormattedRemainingTime());

                return true;

            } else {
                // Novo é menor ou igual: estender tempo do atual
                current.extendTime(durationMillis);
                saveActiveMultiplier(player, current);
                removeFromInventory(player, type, 1);

                ChatStorage.send(player, "multiplier.extended-different",
                        "used_type", type.getDisplayMultiplier(),
                        "active_type", current.getType().getDisplayMultiplier(),
                        "time", current.getFormattedRemainingTime());

                return true;
            }
        } else {
            // Não tem multiplicador ativo: criar novo
            ActiveMultiplier newActive = ActiveMultiplier.createWithDuration(uuid, type, durationMillis);
            activeMultipliers.put(uuid, newActive);
            saveActiveMultiplier(player, newActive);
            removeFromInventory(player, type, 1);

            ChatStorage.send(player, "multiplier.activated",
                    "type", type.getDisplayMultiplier(),
                    "value", String.valueOf(type.getValue()),
                    "bonus", String.valueOf(type.getBonusPercentage()),
                    "time", newActive.getFormattedRemainingTime());

            return true;
        }
    }

    /**
     * Ativa um multiplicador diretamente (admin), sem consumir do inventário
     * 
     * @param player         Jogador
     * @param type           Tipo do multiplicador
     * @param durationMillis Duração em milissegundos
     */
    public void forceActivateMultiplier(Player player, MultiplierType type, long durationMillis) {
        UUID uuid = player.getUniqueId();
        ActiveMultiplier active = ActiveMultiplier.createWithDuration(uuid, type, durationMillis);
        activeMultipliers.put(uuid, active);
        saveActiveMultiplier(player, active);

        ChatStorage.send(player, "multiplier.activated-admin",
                "type", type.getDisplayMultiplier(),
                "value", String.valueOf(type.getValue()),
                "time", active.getFormattedRemainingTime());
    }

    /**
     * Limpa o multiplicador ativo do jogador
     * 
     * @param player Jogador
     * @param notify Se deve notificar o jogador
     */
    public void clearActiveMultiplier(Player player, boolean notify) {
        UUID uuid = player.getUniqueId();
        ActiveMultiplier previous = activeMultipliers.remove(uuid);

        PlayerProfile profile = plugin.getProfileManager().getProfile(player);
        if (profile != null) {
            profile.getData().setCustomData(KEY_ACTIVE, null);
        }

        if (notify && previous != null) {
            ChatStorage.send(player, "multiplier.expired",
                    "type", previous.getType().getDisplayMultiplier());
        }
    }

    /**
     * Salva o multiplicador ativo nos dados do jogador
     */
    private void saveActiveMultiplier(Player player, ActiveMultiplier active) {
        PlayerProfile profile = plugin.getProfileManager().getProfile(player);
        if (profile != null) {
            profile.getData().setCustomData(KEY_ACTIVE, active.serialize());
        }
    }

    // ==================== CÁLCULO DE COINS ====================

    /**
     * Aplica o multiplicador aos coins ganhos.
     * Este método deve ser chamado pelo StatsManager ao registrar uma kill.
     * 
     * @param player    Jogador que ganhou os coins
     * @param baseCoins Quantidade base de coins
     * @return Quantidade final após aplicar multiplicador
     */
    public long applyMultiplier(Player player, long baseCoins) {
        ActiveMultiplier active = getActiveMultiplier(player);

        if (active == null || active.isExpired()) {
            // Limpar se expirado
            if (active != null) {
                clearActiveMultiplier(player, true);
            }
            return baseCoins;
        }

        double multiplier = active.getType().getValue();
        return Math.round(baseCoins * multiplier);
    }

    /**
     * Calcula a diferença de coins pelo multiplicador (bônus)
     * 
     * @param player    Jogador
     * @param baseCoins Coins base
     * @return Bônus de coins (total - base)
     */
    public long getBonusCoins(Player player, long baseCoins) {
        return applyMultiplier(player, baseCoins) - baseCoins;
    }

    // ==================== VERIFICAÇÃO DE EXPIRAÇÃO ====================

    /**
     * Verifica e limpa multiplicadores expirados para um jogador.
     * Deve ser chamado em momentos-chave (login, abrir menu, ganhar coins).
     * 
     * @param player Jogador
     * @return true se havia um multiplicador que expirou
     */
    public boolean checkExpiration(Player player) {
        UUID uuid = player.getUniqueId();
        ActiveMultiplier active = activeMultipliers.get(uuid);

        if (active != null && active.isExpired()) {
            clearActiveMultiplier(player, true);
            return true;
        }

        return false;
    }

    // ==================== GERENCIAMENTO DE SESSÃO ====================

    /**
     * Carrega o multiplicador ativo de um jogador ao entrar no servidor.
     * Chamado pelo PlayerListener no PlayerJoinEvent.
     * 
     * @param player Jogador
     */
    public void loadPlayerMultipliers(Player player) {
        UUID uuid = player.getUniqueId();
        PlayerProfile profile = plugin.getProfileManager().getProfile(player);
        if (profile == null)
            return;

        String serialized = profile.getData().getCustomData(KEY_ACTIVE, String.class);
        if (serialized == null || serialized.isEmpty())
            return;

        ActiveMultiplier active = ActiveMultiplier.deserialize(uuid, serialized);
        if (active != null) {
            if (active.isExpired()) {
                // Expirou enquanto estava offline
                profile.getData().setCustomData(KEY_ACTIVE, null);
                ChatStorage.send(player, "multiplier.expired-offline",
                        "type", active.getType().getDisplayMultiplier());
            } else {
                // Ainda válido
                activeMultipliers.put(uuid, active);
                ChatStorage.send(player, "multiplier.still-active",
                        "type", active.getType().getDisplayMultiplier(),
                        "time", active.getFormattedRemainingTime());
            }
        }
    }

    /**
     * Remove o jogador do cache ao sair do servidor.
     * Os dados já estão salvos no PlayerData.
     * 
     * @param player Jogador
     */
    public void unloadPlayerMultipliers(Player player) {
        UUID uuid = player.getUniqueId();

        // Salvar estado atual antes de remover do cache
        ActiveMultiplier active = activeMultipliers.get(uuid);
        if (active != null) {
            saveActiveMultiplier(player, active);
        }

        activeMultipliers.remove(uuid);
    }

    // ==================== ADMIN: DADOS OFFLINE ====================

    /**
     * Adiciona multiplicadores a um jogador offline
     * 
     * @param uuid   UUID do jogador
     * @param type   Tipo do multiplicador
     * @param amount Quantidade
     * @return true se adicionou com sucesso
     */
    public boolean addToInventoryOffline(UUID uuid, MultiplierType type, int amount) {
        PlayerData data = plugin.getProfileManager().loadOfflineData(uuid, null);
        if (data == null)
            return false;

        boolean success = addToInventoryData(data, type, amount);
        if (success) {
            plugin.getProfileManager().saveOfflineData(data);
        }
        return success;
    }

    /**
     * Remove multiplicadores de um jogador offline
     * 
     * @param uuid   UUID do jogador
     * @param type   Tipo do multiplicador
     * @param amount Quantidade
     * @return true se removeu com sucesso
     */
    public boolean removeFromInventoryOffline(UUID uuid, MultiplierType type, int amount) {
        PlayerData data = plugin.getProfileManager().loadOfflineData(uuid, null);
        if (data == null)
            return false;

        boolean success = removeFromInventoryData(data, type, amount);
        if (success) {
            plugin.getProfileManager().saveOfflineData(data);
        }
        return success;
    }

    /**
     * Obtém o inventário de multiplicadores de um jogador (online ou offline)
     * 
     * @param uuid UUID do jogador
     * @return Mapa de tipo -> quantidade
     */
    public Map<MultiplierType, Integer> getInventoryByUUID(UUID uuid) {
        // Verificar se está online
        PlayerProfile profile = plugin.getProfileManager().getProfile(uuid);
        if (profile != null) {
            return getFullInventoryFromData(profile.getData());
        }

        // Carregar dados offline
        PlayerData data = plugin.getProfileManager().loadOfflineData(uuid, null);
        if (data == null)
            return Collections.emptyMap();

        return getFullInventoryFromData(data);
    }

    // ==================== GETTERS ====================

    /**
     * Obtém o número de jogadores com multiplicador ativo no momento
     */
    public int getActiveMultiplierCount() {
        return activeMultipliers.size();
    }

    /**
     * Obtém todos os multiplicadores ativos (para debug/admin)
     */
    public Map<UUID, ActiveMultiplier> getAllActiveMultipliers() {
        return Collections.unmodifiableMap(activeMultipliers);
    }
}
