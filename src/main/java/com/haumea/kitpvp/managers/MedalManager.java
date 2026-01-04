package com.haumea.kitpvp.managers;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.models.Medal;
import com.haumea.kitpvp.models.PlayerData;
import com.haumea.kitpvp.profile.PlayerProfile;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Gerenciador do sistema de Medalhas do HaumeaMC.
 * 
 * Responsável por:
 * - Armazenar todas as medalhas disponíveis
 * - Gerenciar medalhas equipadas dos jogadores
 * - Verificar permissões de medalhas
 * - Validar medalhas expiradas/sem permissão
 * 
 * @author HaumeaMC
 */
public class MedalManager {

    private final HaumeaMC plugin;

    // Mapa de medalhas: nome -> Medal
    private final Map<String, Medal> medals;

    // Chave para armazenar medalha no PlayerData customData
    private static final String MEDAL_KEY = "selectedMedal";

    // Intervalo de verificação em ticks (20 ticks = 1 segundo, 600 = 30 segundos)
    private static final long VERIFICATION_INTERVAL = 600L;

    /**
     * Construtor do MedalManager
     * 
     * @param plugin Instância do plugin
     */
    public MedalManager(HaumeaMC plugin) {
        this.plugin = plugin;
        this.medals = new LinkedHashMap<>(); // Mantém ordem de inserção

        // Registrar todas as medalhas
        registerMedals();

        // Iniciar task de verificação periódica
        startVerificationTask();

        plugin.getLogger().info("[MedalManager] " + medals.size() + " medalhas registradas.");
    }

    /**
     * Registra todas as medalhas disponíveis no servidor
     */
    private void registerMedals() {
        // 1. PEACE_LOVE - Verde (a) - ✌
        registerMedal("PEACE_LOVE", "✌", "a");

        // 2. HEART - Vermelho (4) - ❤
        registerMedal("HEART", "❤", "4");

        // 3. UPSET_HEART - Vermelho (4) - ❥
        registerMedal("UPSET_HEART", "❥", "4");

        // 4. SMILE - Amarelo (e) - ツ
        registerMedal("SMILE", "ツ", "e");

        // 5. TOXIC - Amarelo (e) - ☣
        registerMedal("TOXIC", "☣", "e");

        // 6. RAY - Dourado (6) - ⚡
        registerMedal("RAY", "⚡", "6");

        // 7. MUSIC - Roxo (5) - ♫
        registerMedal("MUSIC", "♫", "5");

        // 8. COFFEE - Cinza Escuro (8) - ☕
        registerMedal("COFFEE", "☕", "8");

        // 9. RADIOACTIVE - Dourado (6) - ☢
        registerMedal("RADIOACTIVE", "☢", "6");

        // 10. SKELETON - Cinza (7) - ☠
        registerMedal("SKELETON", "☠", "7");

        // 11. UMBRELLA - Rosa (d) - ☂
        registerMedal("UMBRELLA", "☂", "d");

        // 12. HALLOWEEN_CROSS - Branco (f) - ✞
        registerMedal("HALLOWEEN_CROSS", "✞", "f");

        // 13. CROSS - Cinza (7) - ✠
        registerMedal("CROSS", "✠", "7");

        // 14. WRONG - Vermelho Claro (c) - ✘
        registerMedal("WRONG", "✘", "c");

        // 15. CORRECT - Verde (a) - ✔
        registerMedal("CORRECT", "✔", "a");

        // 16. YIN_YANG - Branco (f) - ☯
        registerMedal("YIN_YANG", "☯", "f");

        // 17. BATTLEBITS - Azul (9) - ⚔
        registerMedal("BATTLEBITS", "⚔", "9");

        // 18. STARRY - Azul Claro (b) - ❄
        registerMedal("STARRY", "❄", "b");

        // 19. SIGNAL - Verde Escuro (2) - ♻
        registerMedal("SIGNAL", "♻", "2");

        // 20. STUDENT - Amarelo (e) - ✍
        registerMedal("STUDENT", "✍", "e");

        // 21. EMAIL - Amarelo (e) - ✉
        registerMedal("EMAIL", "✉", "e");

        // 22. F - Branco (f) - Ⓕ
        registerMedal("F", "Ⓕ", "f");
    }

    /**
     * Registra uma medalha no sistema
     * 
     * @param name      Nome da medalha
     * @param symbol    Símbolo unicode
     * @param colorCode Código de cor
     */
    private void registerMedal(String name, String symbol, String colorCode) {
        Medal medal = new Medal(name, symbol, colorCode);
        medals.put(name.toUpperCase(), medal);
    }

    /**
     * Inicia a task de verificação periódica de permissões
     */
    private void startVerificationTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                validatePlayerMedal(player);
            }
        }, VERIFICATION_INTERVAL, VERIFICATION_INTERVAL);
    }

    // ==================== MÉTODOS DE ACESSO ====================

    /**
     * Obtém uma medalha pelo nome
     * 
     * @param name Nome da medalha
     * @return Medal ou null se não existir
     */
    public Medal getMedal(String name) {
        if (name == null)
            return null;
        return medals.get(name.toUpperCase());
    }

    /**
     * Verifica se uma medalha existe
     * 
     * @param name Nome da medalha
     * @return true se existe
     */
    public boolean medalExists(String name) {
        if (name == null)
            return false;
        return medals.containsKey(name.toUpperCase());
    }

    /**
     * Obtém todas as medalhas registradas
     * 
     * @return Coleção de medalhas (ordem mantida)
     */
    public Collection<Medal> getAllMedals() {
        return medals.values();
    }

    /**
     * Obtém todas as medalhas que o jogador tem permissão
     * 
     * @param player Jogador
     * @return Lista de medalhas disponíveis
     */
    public List<Medal> getAvailableMedals(Player player) {
        List<Medal> available = new ArrayList<>();
        for (Medal medal : medals.values()) {
            if (hasPermission(player, medal)) {
                available.add(medal);
            }
        }
        return available;
    }

    // ==================== PERMISSÕES ====================

    /**
     * Verifica se o jogador tem permissão para usar uma medalha
     * 
     * @param player Jogador
     * @param medal  Medalha
     * @return true se tem permissão
     */
    public boolean hasPermission(Player player, Medal medal) {
        if (player == null || medal == null)
            return false;

        // Verificar permissão específica
        if (player.hasPermission(medal.getPermission())) {
            return true;
        }

        // Verificar permissão wildcard
        if (player.hasPermission("medal.*")) {
            return true;
        }

        return false;
    }

    /**
     * Verifica se o jogador tem permissão para uma medalha pelo nome
     * 
     * @param player    Jogador
     * @param medalName Nome da medalha
     * @return true se tem permissão
     */
    public boolean hasPermission(Player player, String medalName) {
        Medal medal = getMedal(medalName);
        return hasPermission(player, medal);
    }

    // ==================== SELEÇÃO DE MEDALHA ====================

    /**
     * Define a medalha ativa de um jogador
     * 
     * @param player Jogador
     * @param medal  Medalha (null para remover)
     * @return true se equipada com sucesso
     */
    public boolean setPlayerMedal(Player player, Medal medal) {
        PlayerProfile profile = plugin.getProfileManager().getProfile(player);
        if (profile == null)
            return false;

        PlayerData data = profile.getData();

        if (medal == null) {
            // Remover medalha
            data.setCustomData(MEDAL_KEY, null);
        } else {
            // Verificar permissão antes de equipar
            if (!hasPermission(player, medal)) {
                return false;
            }
            data.setCustomData(MEDAL_KEY, medal.getName());
        }

        // Atualizar tablist e nametag imediatamente
        updatePlayerVisuals(player);

        return true;
    }

    /**
     * Define a medalha ativa por nome
     * 
     * @param player    Jogador
     * @param medalName Nome da medalha (null ou "remover" para limpar)
     * @return true se equipada com sucesso
     */
    public boolean setPlayerMedal(Player player, String medalName) {
        if (medalName == null || medalName.equalsIgnoreCase("remover")) {
            return setPlayerMedal(player, (Medal) null);
        }

        Medal medal = getMedal(medalName);
        if (medal == null)
            return false;

        return setPlayerMedal(player, medal);
    }

    /**
     * Obtém a medalha ativa de um jogador
     * 
     * @param player Jogador
     * @return Medal ou null se não tiver medalha equipada
     */
    public Medal getPlayerMedal(Player player) {
        PlayerProfile profile = plugin.getProfileManager().getProfile(player);
        if (profile == null)
            return null;

        String medalName = profile.getData().getCustomData(MEDAL_KEY, String.class);
        if (medalName == null || medalName.isEmpty()) {
            return null;
        }

        return getMedal(medalName);
    }

    /**
     * Obtém o display da medalha do jogador (para uso no chat/tablist)
     * 
     * @param player Jogador
     * @return Display formatado ou string vazia se não tiver medalha
     */
    public String getPlayerMedalDisplay(Player player) {
        Medal medal = getPlayerMedal(player);
        if (medal == null) {
            return "";
        }
        return medal.getDisplayWithSpace();
    }

    /**
     * Verifica se o jogador tem uma medalha equipada
     * 
     * @param player Jogador
     * @return true se tem medalha
     */
    public boolean hasEquippedMedal(Player player) {
        return getPlayerMedal(player) != null;
    }

    /**
     * Remove a medalha ativa do jogador
     * 
     * @param player Jogador
     */
    public void removePlayerMedal(Player player) {
        setPlayerMedal(player, (Medal) null);
    }

    // ==================== VALIDAÇÃO ====================

    /**
     * Valida se o jogador ainda tem permissão para sua medalha atual
     * Se não tiver, remove automaticamente
     * 
     * @param player Jogador a validar
     */
    public void validatePlayerMedal(Player player) {
        Medal currentMedal = getPlayerMedal(player);
        if (currentMedal == null)
            return;

        if (!hasPermission(player, currentMedal)) {
            // Remover medalha sem permissão
            removePlayerMedal(player);

            // Notificar jogador
            player.sendMessage(com.haumea.kitpvp.utils.ChatStorage.getMessage("medal.expired"));
        }
    }

    /**
     * Valida e atualiza visuais ao jogador entrar
     * 
     * @param player Jogador que entrou
     */
    public void onPlayerJoin(Player player) {
        // Validar medalha
        validatePlayerMedal(player);

        // Atualizar visuais
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            updatePlayerVisuals(player);
        }, 5L); // Pequeno delay para garantir que outros sistemas carregaram
    }

    /**
     * Atualiza a tablist e nametag do jogador com a medalha.
     * USA O DISPLAYMANAGER COMO FONTE UNIFICADA.
     * 
     * @param player Jogador
     */
    public void updatePlayerVisuals(Player player) {
        if (player == null || !player.isOnline())
            return;

        // Usar DisplayManager se disponível (fonte unificada)
        if (plugin.getDisplayManager() != null) {
            plugin.getDisplayManager().onMedalChange(player);
            return;
        }

        // Fallback: Atualizar TabManager diretamente
        if (plugin.getTabManager() != null) {
            plugin.getTabManager().updatePlayerTeam(player);
        }
    }

    /**
     * Atualiza visuais de todos os jogadores online
     */
    public void updateAllPlayerVisuals() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updatePlayerVisuals(player);
        }
    }

    // ==================== GETTERS ====================

    /**
     * Obtém o número total de medalhas registradas
     */
    public int getTotalMedals() {
        return medals.size();
    }
}
