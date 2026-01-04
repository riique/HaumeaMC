package com.haumea.kitpvp.database;

import java.util.concurrent.CompletableFuture;

/**
 * Interface de Persistência para padronizar operações de banco de dados.
 * 
 * Todos os repositórios devem implementar esta interface para garantir
 * consistência nas operações de CRUD.
 * 
 * @param <T>  Tipo da entidade
 * @param <ID> Tipo do identificador (UUID, String, etc.)
 * 
 * @author HaumeaMC
 */
public interface Persistable<T, ID> {

    /**
     * Salva uma entidade de forma síncrona.
     * 
     * @param entity Entidade a salvar
     * @return true se salvo com sucesso
     */
    boolean save(T entity);

    /**
     * Salva uma entidade de forma assíncrona.
     * 
     * @param entity Entidade a salvar
     * @return CompletableFuture com resultado
     */
    CompletableFuture<Boolean> saveAsync(T entity);

    /**
     * Carrega uma entidade pelo identificador.
     * 
     * @param id Identificador da entidade
     * @return Entidade ou null se não encontrada
     */
    T findById(ID id);

    /**
     * Carrega uma entidade de forma assíncrona.
     * 
     * @param id Identificador da entidade
     * @return CompletableFuture com a entidade
     */
    CompletableFuture<T> findByIdAsync(ID id);

    /**
     * Verifica se uma entidade existe.
     * 
     * @param id Identificador da entidade
     * @return true se existe
     */
    boolean exists(ID id);

    /**
     * Remove uma entidade pelo identificador.
     * 
     * @param id Identificador da entidade
     * @return true se removido com sucesso
     */
    boolean delete(ID id);

    /**
     * Remove uma entidade de forma assíncrona.
     * 
     * @param id Identificador da entidade
     * @return CompletableFuture com resultado
     */
    CompletableFuture<Boolean> deleteAsync(ID id);

    /**
     * Conta o total de entidades.
     * 
     * @return Total de entidades
     */
    long count();
}
