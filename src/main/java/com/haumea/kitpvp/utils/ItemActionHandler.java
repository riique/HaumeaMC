package com.haumea.kitpvp.utils;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Sistema de registro e execução de handlers de ação para itens interativos.
 * 
 * Este sistema permite vincular ações (callbacks) a itens específicos usando
 * um ID único armazenado na metadata do item. Quando o jogador clica no item,
 * o handler correspondente é executado automaticamente.
 * 
 * Funciona em conjunto com:
 * - {@link ItemBuilder} para criar itens com handlers
 * - ArenaItemsListener para processar eventos de clique
 * 
 * Exemplo de uso:
 * 
 * <pre>
 * // Registrar uma ação
 * ItemActionHandler.registerAction("abrir_kits", player -> {
 *     player.sendMessage("Abrindo menu de kits...");
 *     // Abrir GUI de kits
 * });
 * 
 * // Executar uma ação quando o jogador clicar
 * ItemActionHandler.executeAction("abrir_kits", player);
 * </pre>
 * 
 * @author HaumeaMC
 */
public class ItemActionHandler {

    /**
     * Mapa de handlers registrados.
     * Key: ID único da ação
     * Value: Consumer que recebe o jogador e executa a ação
     */
    private static final Map<String, Consumer<Player>> handlers = new HashMap<>();

    /**
     * Registra um handler de ação para um ID específico.
     * Se já existir um handler com este ID, ele será substituído.
     * 
     * @param actionId ID único da ação
     * @param handler  Handler a ser executado quando a ação for ativada
     */
    public static void registerAction(String actionId, Consumer<Player> handler) {
        if (actionId == null || handler == null) {
            return;
        }
        handlers.put(actionId, handler);
    }

    /**
     * Remove um handler de ação registrado.
     * 
     * @param actionId ID da ação a remover
     */
    public static void unregisterAction(String actionId) {
        handlers.remove(actionId);
    }

    /**
     * Verifica se existe um handler registrado para um ID.
     * 
     * @param actionId ID a verificar
     * @return true se existe um handler registrado
     */
    public static boolean hasAction(String actionId) {
        return actionId != null && handlers.containsKey(actionId);
    }

    /**
     * Executa o handler associado a um ID de ação.
     * 
     * @param actionId ID da ação
     * @param player   Jogador que ativou a ação
     * @return true se o handler foi executado, false se não existe handler
     */
    public static boolean executeAction(String actionId, Player player) {
        if (actionId == null || player == null) {
            return false;
        }

        Consumer<Player> handler = handlers.get(actionId);
        if (handler == null) {
            return false;
        }

        try {
            handler.accept(player);
            return true;
        } catch (Exception e) {
            // Log do erro sem quebrar o servidor
            System.err.println("[HaumeaMC] Erro ao executar handler de item: " + actionId);
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Limpa todos os handlers registrados.
     * Útil para reload do plugin.
     */
    public static void clearAll() {
        handlers.clear();
    }

    /**
     * Retorna o número de handlers registrados.
     * Útil para debug.
     * 
     * @return Quantidade de handlers ativos
     */
    public static int getRegisteredCount() {
        return handlers.size();
    }

    // ==================== AÇÕES PRÉ-DEFINIDAS ====================
    // IDs de ação padrão usados pelo ArenaItemsHandler

    /** Ação: Abrir menu de seleção de Kit 1 */
    public static final String ACTION_SELECT_KIT_1 = "haumea_select_kit_1";

    /** Ação: Abrir menu de seleção de Kit 2 */
    public static final String ACTION_SELECT_KIT_2 = "haumea_select_kit_2";

    /** Ação: Abrir menu de perfil/status */
    public static final String ACTION_PROFILE = "haumea_profile";

    /** Ação: Abrir menu de warps */
    public static final String ACTION_WARPS = "haumea_warps";

    /** Ação: Abrir loja */
    public static final String ACTION_SHOP = "haumea_shop";

    /** Ação: Ir para evento */
    public static final String ACTION_EVENT = "haumea_event";

    /** Ação: Bússola rastreadora */
    public static final String ACTION_TRACKER = "haumea_tracker";

    /** Ação: Abrir menu do cassino */
    public static final String ACTION_CASINO = "haumea_casino";
}
