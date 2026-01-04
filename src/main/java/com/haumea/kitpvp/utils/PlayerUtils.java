package com.haumea.kitpvp.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * Utilitários para operações com jogadores.
 * 
 * Centraliza operações comuns e seguras com jogadores,
 * especialmente verificações de null e online status.
 * 
 * @author HaumeaMC
 */
public class PlayerUtils {

    /**
     * Executa uma ação se o jogador está online.
     * Evita NPEs ao usar Bukkit.getPlayer().
     * 
     * @param uuid   UUID do jogador
     * @param action Ação a executar se jogador está online
     */
    public static void ifOnline(UUID uuid, Consumer<Player> action) {
        if (uuid == null || action == null)
            return;

        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline()) {
            action.accept(player);
        }
    }

    /**
     * Executa uma ação se o jogador está online (versão por nome).
     * 
     * @param playerName Nome do jogador
     * @param action     Ação a executar se jogador está online
     */
    public static void ifOnline(String playerName, Consumer<Player> action) {
        if (playerName == null || playerName.isEmpty() || action == null)
            return;

        Player player = Bukkit.getPlayerExact(playerName);
        if (player != null && player.isOnline()) {
            action.accept(player);
        }
    }

    /**
     * Obtém um jogador de forma segura.
     * 
     * @param uuid UUID do jogador
     * @return Player ou null se não está online
     */
    public static Player getOnline(UUID uuid) {
        if (uuid == null)
            return null;

        Player player = Bukkit.getPlayer(uuid);
        return (player != null && player.isOnline()) ? player : null;
    }

    /**
     * Obtém um jogador de forma segura (versão por nome).
     * 
     * @param playerName Nome do jogador
     * @return Player ou null se não está online
     */
    public static Player getOnline(String playerName) {
        if (playerName == null || playerName.isEmpty())
            return null;

        Player player = Bukkit.getPlayerExact(playerName);
        return (player != null && player.isOnline()) ? player : null;
    }

    /**
     * Verifica se um jogador está online.
     * 
     * @param uuid UUID do jogador
     * @return true se está online
     */
    public static boolean isOnline(UUID uuid) {
        return getOnline(uuid) != null;
    }

    /**
     * Verifica se um jogador está online (versão por nome).
     * 
     * @param playerName Nome do jogador
     * @return true se está online
     */
    public static boolean isOnline(String playerName) {
        return getOnline(playerName) != null;
    }

    /**
     * Envia uma mensagem para um jogador se ele está online.
     * 
     * @param uuid    UUID do jogador
     * @param message Mensagem a enviar (cores já convertidas)
     */
    public static void sendMessage(UUID uuid, String message) {
        ifOnline(uuid, player -> player.sendMessage(message));
    }

    /**
     * Executa uma ação para todos os jogadores online.
     * 
     * @param action Ação a executar para cada jogador
     */
    public static void forEachOnline(Consumer<Player> action) {
        if (action == null)
            return;

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player != null && player.isOnline()) {
                action.accept(player);
            }
        }
    }

    /**
     * Obtém o UUID de um jogador online pelo nome.
     * 
     * @param playerName Nome do jogador
     * @return UUID ou null se não está online
     */
    public static UUID getUUID(String playerName) {
        Player player = getOnline(playerName);
        return player != null ? player.getUniqueId() : null;
    }
}
