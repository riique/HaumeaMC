package com.haumea.kitpvp.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Cache simples com suporte a TTL (Time To Live).
 * 
 * Usado para caching de rankings e outros dados que devem
 * expirar após um período.
 * 
 * @param <K> Tipo da chave
 * @param <V> Tipo do valor
 * 
 * @author HaumeaMC
 */
public class CacheWithTTL<K, V> {

    private final Map<K, CacheEntry<V>> cache;
    private final long defaultTtlMs;

    /**
     * Representa uma entrada no cache com timestamp de expiração.
     */
    private static class CacheEntry<V> {
        final V value;
        final long expiresAt;

        CacheEntry(V value, long expiresAt) {
            this.value = value;
            this.expiresAt = expiresAt;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }

    /**
     * Construtor com TTL padrão.
     * 
     * @param defaultTtlMs TTL padrão em milissegundos
     */
    public CacheWithTTL(long defaultTtlMs) {
        this.cache = new ConcurrentHashMap<>();
        this.defaultTtlMs = defaultTtlMs;
    }

    /**
     * Armazena um valor no cache com o TTL padrão.
     * 
     * @param key   Chave
     * @param value Valor
     */
    public void put(K key, V value) {
        put(key, value, defaultTtlMs);
    }

    /**
     * Armazena um valor no cache com TTL personalizado.
     * 
     * @param key   Chave
     * @param value Valor
     * @param ttlMs TTL em milissegundos
     */
    public void put(K key, V value, long ttlMs) {
        long expiresAt = System.currentTimeMillis() + ttlMs;
        cache.put(key, new CacheEntry<>(value, expiresAt));
    }

    /**
     * Obtém um valor do cache.
     * Retorna null se não existe ou expirou.
     * 
     * @param key Chave
     * @return Valor ou null
     */
    public V get(K key) {
        CacheEntry<V> entry = cache.get(key);
        if (entry == null) {
            return null;
        }

        if (entry.isExpired()) {
            cache.remove(key);
            return null;
        }

        return entry.value;
    }

    /**
     * Obtém um valor do cache ou calcula e armazena se não existe/expirou.
     * 
     * @param key      Chave
     * @param supplier Função que calcula o valor se necessário
     * @return Valor (do cache ou recém-calculado)
     */
    public V getOrCompute(K key, Supplier<V> supplier) {
        V cached = get(key);
        if (cached != null) {
            return cached;
        }

        V computed = supplier.get();
        if (computed != null) {
            put(key, computed);
        }
        return computed;
    }

    /**
     * Verifica se uma chave existe e está válida no cache.
     * 
     * @param key Chave
     * @return true se existe e não expirou
     */
    public boolean containsKey(K key) {
        return get(key) != null;
    }

    /**
     * Remove uma chave do cache.
     * 
     * @param key Chave
     */
    public void remove(K key) {
        cache.remove(key);
    }

    /**
     * Invalida (limpa) todo o cache.
     */
    public void invalidateAll() {
        cache.clear();
    }

    /**
     * Remove entradas expiradas do cache.
     * Chame periodicamente para liberar memória.
     */
    public void cleanup() {
        cache.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    /**
     * Obtém o tamanho atual do cache (incluindo expirados).
     * 
     * @return Número de entradas
     */
    public int size() {
        return cache.size();
    }

    /**
     * Obtém o tamanho do cache apenas com entradas válidas.
     * 
     * @return Número de entradas válidas
     */
    public int validSize() {
        int count = 0;
        for (CacheEntry<V> entry : cache.values()) {
            if (!entry.isExpired()) {
                count++;
            }
        }
        return count;
    }
}
