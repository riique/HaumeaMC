package com.haumea.kitpvp.models;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Modelo de dados persistentes do jogador.
 * Esta classe representa os dados que são salvos/carregados do disco.
 * 
 * É extensível através do sistema de customData, permitindo
 * adicionar novos tipos de informação sem reescrever o sistema.
 * 
 * @author HaumeaMC
 */
public class PlayerData {

    // Identificação
    private final UUID uuid;
    private String lastKnownName;

    // Informações de tempo
    private long firstJoin;
    private long lastJoin;
    private long playTime; // Tempo total jogado em milissegundos

    // Estatísticas de combate
    private int kills;
    private int deaths;
    private int killStreak;
    private int highestKillStreak;

    // Economia
    private long coins;
    private int cash; // Moeda premium

    // Preferências
    private String selectedTag;
    private String selectedKit;

    // Dados customizados (extensível)
    private final Map<String, Object> customData;

    /**
     * Construtor do PlayerData
     * 
     * @param uuid       UUID do jogador
     * @param playerName Nome do jogador
     */
    public PlayerData(UUID uuid, String playerName) {
        this.uuid = uuid;
        this.lastKnownName = playerName;
        this.firstJoin = System.currentTimeMillis();
        this.lastJoin = System.currentTimeMillis();
        this.playTime = 0L;
        this.kills = 0;
        this.deaths = 0;
        this.killStreak = 0;
        this.highestKillStreak = 0;
        this.coins = 0L;
        this.cash = 0;
        this.selectedTag = null;
        this.selectedKit = "default";
        this.customData = new HashMap<>();
    }

    // ==================== GETTERS & SETTERS ====================

    public UUID getUuid() {
        return uuid;
    }

    public String getLastKnownName() {
        return lastKnownName;
    }

    public void setLastKnownName(String lastKnownName) {
        this.lastKnownName = lastKnownName;
    }

    public long getFirstJoin() {
        return firstJoin;
    }

    public void setFirstJoin(long firstJoin) {
        this.firstJoin = firstJoin;
    }

    public long getLastJoin() {
        return lastJoin;
    }

    public void setLastJoin(long lastJoin) {
        this.lastJoin = lastJoin;
    }

    public long getPlayTime() {
        return playTime;
    }

    public void setPlayTime(long playTime) {
        this.playTime = playTime;
    }

    public int getKills() {
        return kills;
    }

    public void setKills(int kills) {
        this.kills = kills;
    }

    public int getDeaths() {
        return deaths;
    }

    public void setDeaths(int deaths) {
        this.deaths = deaths;
    }

    public int getKillStreak() {
        return killStreak;
    }

    public void setKillStreak(int killStreak) {
        this.killStreak = killStreak;
    }

    public int getHighestKillStreak() {
        return highestKillStreak;
    }

    public void setHighestKillStreak(int highestKillStreak) {
        this.highestKillStreak = highestKillStreak;
    }

    public long getCoins() {
        return coins;
    }

    public void setCoins(long coins) {
        this.coins = coins;
    }

    public int getCash() {
        return cash;
    }

    public void setCash(int cash) {
        this.cash = cash;
    }

    public String getSelectedTag() {
        return selectedTag;
    }

    public void setSelectedTag(String selectedTag) {
        this.selectedTag = selectedTag;
    }

    public String getSelectedKit() {
        return selectedKit;
    }

    public void setSelectedKit(String selectedKit) {
        this.selectedKit = selectedKit;
    }

    // ==================== CUSTOM DATA (EXTENSÍVEL) ====================

    /**
     * Define um dado customizado
     * Permite adicionar qualquer tipo de informação sem modificar a classe
     * 
     * @param key   Chave do dado
     * @param value Valor do dado (deve ser serializável em YAML)
     */
    public void setCustomData(String key, Object value) {
        if (value == null) {
            customData.remove(key);
        } else {
            customData.put(key, value);
        }
    }

    /**
     * Obtém um dado customizado
     * 
     * @param key Chave do dado
     * @return Valor do dado ou null se não existir
     */
    public Object getCustomData(String key) {
        return customData.get(key);
    }

    /**
     * Obtém um dado customizado com cast para o tipo esperado
     * 
     * @param key  Chave do dado
     * @param type Classe do tipo esperado
     * @param <T>  Tipo do dado
     * @return Valor do dado ou null se não existir ou tipo incorreto
     */
    @SuppressWarnings("unchecked")
    public <T> T getCustomData(String key, Class<T> type) {
        if (type == null) {
            return null;
        }
        Object value = customData.get(key);
        if (value != null && type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }

    /**
     * Obtém um dado customizado com valor padrão
     * 
     * @param key          Chave do dado
     * @param defaultValue Valor padrão se não existir
     * @param <T>          Tipo do dado
     * @return Valor do dado ou valor padrão
     */
    @SuppressWarnings("unchecked")
    public <T> T getCustomData(String key, T defaultValue) {
        Object value = customData.get(key);
        if (value != null && defaultValue.getClass().isInstance(value)) {
            return (T) value;
        }
        return defaultValue;
    }

    /**
     * Verifica se existe um dado customizado
     * 
     * @param key Chave do dado
     * @return true se existe
     */
    public boolean hasCustomData(String key) {
        return customData.containsKey(key);
    }

    /**
     * Remove um dado customizado
     * 
     * @param key Chave do dado
     * @return Valor removido ou null
     */
    public Object removeCustomData(String key) {
        return customData.remove(key);
    }

    /**
     * Obtém todas as chaves de dados customizados
     * 
     * @return Set com todas as chaves
     */
    public Set<String> getCustomDataKeys() {
        return customData.keySet();
    }

    /**
     * Obtém o mapa completo de dados customizados
     * 
     * @return Mapa de dados customizados
     */
    public Map<String, Object> getCustomData() {
        return customData;
    }

    // ==================== MÉTODOS UTILITÁRIOS ====================

    /**
     * Calcula o KDR (Kill/Death Ratio) do jogador
     * 
     * @return KDR formatado
     */
    public double getKDR() {
        if (deaths == 0) {
            return (double) kills;
        }
        return Math.round((double) kills / deaths * 100.0) / 100.0;
    }

    /**
     * Adiciona kills ao jogador
     * 
     * @param amount Quantidade a adicionar
     */
    public void addKills(int amount) {
        this.kills += amount;
        this.killStreak += amount;
        if (this.killStreak > this.highestKillStreak) {
            this.highestKillStreak = this.killStreak;
        }
    }

    /**
     * Adiciona deaths ao jogador e reseta o killStreak
     * 
     * @param amount Quantidade a adicionar
     */
    public void addDeaths(int amount) {
        this.deaths += amount;
        this.killStreak = 0;
    }

    /**
     * Adiciona coins ao jogador
     * 
     * @param amount Quantidade a adicionar
     */
    public void addCoins(long amount) {
        this.coins += amount;
    }

    /**
     * Remove coins do jogador
     * 
     * @param amount Quantidade a remover
     * @return true se tinha coins suficientes
     */
    public boolean removeCoins(long amount) {
        if (this.coins >= amount) {
            this.coins -= amount;
            return true;
        }
        return false;
    }

    /**
     * Adiciona cash ao jogador
     * 
     * @param amount Quantidade a adicionar
     */
    public void addCash(int amount) {
        this.cash += amount;
    }

    /**
     * Remove cash do jogador
     * 
     * @param amount Quantidade a remover
     * @return true se tinha cash suficiente
     */
    public boolean removeCash(int amount) {
        if (this.cash >= amount) {
            this.cash -= amount;
            return true;
        }
        return false;
    }

    /**
     * Adiciona tempo jogado
     * 
     * @param milliseconds Tempo em milissegundos
     */
    public void addPlayTime(long milliseconds) {
        this.playTime += milliseconds;
    }

    @Override
    public String toString() {
        return "PlayerData{" +
                "uuid=" + uuid +
                ", name='" + lastKnownName + '\'' +
                ", kills=" + kills +
                ", deaths=" + deaths +
                ", coins=" + coins +
                '}';
    }
}
