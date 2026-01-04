package com.haumea.kitpvp.profile;

import com.haumea.kitpvp.models.PlayerData;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * Representa o perfil de um jogador enquanto está online.
 * Esta classe encapsula o PlayerData e adiciona funcionalidades
 * específicas para a sessão do jogador.
 * 
 * O perfil é criado quando o jogador entra e removido quando sai.
 * Toda manipulação de dados do jogador online deve ser feita através
 * desta classe, mantendo a lógica de memória separada da persistência.
 * 
 * @author HaumeaMC
 */
public class PlayerProfile {

    private final Player player;
    private final PlayerData data;
    private final long sessionStart;

    // Dados de sessão (não persistentes)
    private boolean godMode;
    private boolean vanish;
    private boolean inCombat;
    private long lastCombatTime;
    private Player lastAttacker;
    private boolean frozen;
    private String lastMessage;
    private long lastMessageTime;

    // Novos dados de staff
    private boolean buildMode;
    private boolean staffChatMode;
    private ItemStack[] savedInventory;
    private ItemStack[] savedArmor;

    // Dados salvos para modo admin
    private int savedLevel;
    private float savedExp;
    private GameMode savedGameMode;

    /**
     * Construtor do PlayerProfile
     * 
     * @param player Player do Bukkit
     * @param data   Dados persistentes carregados do disco
     */
    public PlayerProfile(Player player, PlayerData data) {
        this.player = player;
        this.data = data;
        this.sessionStart = System.currentTimeMillis();

        // Inicializar dados de sessão
        this.godMode = false;
        this.vanish = false;
        this.inCombat = false;
        this.lastCombatTime = 0L;
        this.lastAttacker = null;
        this.frozen = false;
        this.lastMessage = null;
        this.lastMessageTime = 0L;

        // Atualizar último login
        data.setLastJoin(sessionStart);
        data.setLastKnownName(player.getName());
    }

    // ==================== IDENTIFICAÇÃO ====================

    /**
     * Obtém o UUID do jogador
     * 
     * @return UUID do jogador
     */
    public UUID getUniqueId() {
        return player.getUniqueId();
    }

    /**
     * Obtém o nome do jogador
     * 
     * @return Nome do jogador
     */
    public String getName() {
        return player.getName();
    }

    /**
     * Obtém o Player do Bukkit
     * 
     * @return Player do Bukkit
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * Verifica se o jogador está online
     * 
     * @return true se online
     */
    public boolean isOnline() {
        return player.isOnline();
    }

    // ==================== DADOS PERSISTENTES ====================

    /**
     * Obtém o PlayerData (dados persistentes)
     * Use este método para acessar dados que são salvos no disco.
     * 
     * @return PlayerData do jogador
     */
    public PlayerData getData() {
        return data;
    }

    // ==================== ESTATÍSTICAS RÁPIDAS ====================

    /**
     * Obtém o total de kills
     */
    public int getKills() {
        return data.getKills();
    }

    /**
     * Obtém o total de deaths
     */
    public int getDeaths() {
        return data.getDeaths();
    }

    /**
     * Obtém o killStreak atual
     */
    public int getKillStreak() {
        return data.getKillStreak();
    }

    /**
     * Obtém o KDR
     */
    public double getKDR() {
        return data.getKDR();
    }

    /**
     * Obtém as coins
     */
    public long getCoins() {
        return data.getCoins();
    }

    /**
     * Obtém o cash
     */
    public int getCash() {
        return data.getCash();
    }

    /**
     * Registra uma kill
     */
    public void addKill() {
        data.addKills(1);
    }

    /**
     * Registra uma death
     */
    public void addDeath() {
        data.addDeaths(1);
    }

    /**
     * Adiciona coins
     */
    public void addCoins(long amount) {
        data.addCoins(amount);
    }

    /**
     * Remove coins
     * 
     * @return true se tinha coins suficientes
     */
    public boolean removeCoins(long amount) {
        return data.removeCoins(amount);
    }

    // ==================== DADOS DE SESSÃO (NÃO PERSISTENTES) ====================

    /**
     * Obtém o momento em que a sessão começou
     */
    public long getSessionStart() {
        return sessionStart;
    }

    /**
     * Obtém o tempo da sessão atual em milissegundos
     */
    public long getSessionTime() {
        return System.currentTimeMillis() - sessionStart;
    }

    /**
     * Verifica se está em god mode
     */
    public boolean isGodMode() {
        return godMode;
    }

    /**
     * Define god mode
     */
    public void setGodMode(boolean godMode) {
        this.godMode = godMode;
    }

    /**
     * Verifica se está vanish
     */
    public boolean isVanish() {
        return vanish;
    }

    /**
     * Define vanish
     */
    public void setVanish(boolean vanish) {
        this.vanish = vanish;
    }

    /**
     * Verifica se está em combate
     */
    public boolean isInCombat() {
        if (!inCombat)
            return false;
        // Combat tag dura 15 segundos
        if (System.currentTimeMillis() - lastCombatTime > 15000) {
            inCombat = false;
            lastAttacker = null;
            return false;
        }
        return true;
    }

    /**
     * Define estado de combate
     * 
     * @param attacker Último atacante
     */
    public void setInCombat(Player attacker) {
        this.inCombat = true;
        this.lastCombatTime = System.currentTimeMillis();
        this.lastAttacker = attacker;
    }

    /**
     * Remove estado de combate
     */
    public void removeFromCombat() {
        this.inCombat = false;
        this.lastAttacker = null;
    }

    /**
     * Obtém o último atacante
     */
    public Player getLastAttacker() {
        return lastAttacker;
    }

    /**
     * Verifica se está congelado
     */
    public boolean isFrozen() {
        return frozen;
    }

    /**
     * Define estado congelado
     */
    public void setFrozen(boolean frozen) {
        this.frozen = frozen;
    }

    /**
     * Obtém a última mensagem enviada
     */
    public String getLastMessage() {
        return lastMessage;
    }

    /**
     * Define a última mensagem enviada
     */
    public void setLastMessage(String message) {
        this.lastMessage = message;
        this.lastMessageTime = System.currentTimeMillis();
    }

    /**
     * Obtém o tempo da última mensagem
     */
    public long getLastMessageTime() {
        return lastMessageTime;
    }

    // ==================== MÉTODOS UTILITÁRIOS ====================

    /**
     * Envia uma mensagem para o jogador
     * 
     * @param message Mensagem a enviar (suporta &)
     */
    public void sendMessage(String message) {
        player.sendMessage(message.replace("&", "§"));
    }

    /**
     * Prepara os dados para salvamento ao deslogar
     * Atualiza o tempo jogado com a sessão atual
     */
    public void prepareForSave() {
        data.addPlayTime(getSessionTime());
    }

    /**
     * Verifica se o jogador pode receber dano
     * 
     * @return true se pode receber dano
     */
    public boolean canTakeDamage() {
        return !godMode && !frozen;
    }

    /**
     * Verifica se o jogador pode se mover
     * 
     * @return true se pode se mover
     */
    public boolean canMove() {
        return !frozen;
    }

    // ==================== DADOS DE STAFF ====================

    /**
     * Verifica se está no modo de construção
     */
    public boolean isBuildMode() {
        return buildMode;
    }

    /**
     * Define modo de construção
     */
    public void setBuildMode(boolean buildMode) {
        this.buildMode = buildMode;
    }

    /**
     * Verifica se está no modo staffchat
     */
    public boolean isStaffChatMode() {
        return staffChatMode;
    }

    /**
     * Define modo staffchat
     */
    public void setStaffChatMode(boolean staffChatMode) {
        this.staffChatMode = staffChatMode;
    }

    /**
     * Salva o estado completo do jogador (inventário, armadura, XP, GameMode)
     */
    public void saveInventory() {
        this.savedInventory = player.getInventory().getContents().clone();
        this.savedArmor = player.getInventory().getArmorContents().clone();
        this.savedLevel = player.getLevel();
        this.savedExp = player.getExp();
        this.savedGameMode = player.getGameMode();
    }

    /**
     * Restaura o estado completo do jogador
     */
    public void restoreInventory() {
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);

        if (savedInventory != null) {
            player.getInventory().setContents(savedInventory);
        }
        if (savedArmor != null) {
            player.getInventory().setArmorContents(savedArmor);
        }

        // Restaurar XP e GameMode
        player.setLevel(savedLevel);
        player.setExp(savedExp);
        if (savedGameMode != null) {
            player.setGameMode(savedGameMode);
        }

        player.updateInventory();
        savedInventory = null;
        savedArmor = null;
        savedGameMode = null;
    }

    /**
     * Verifica se tem inventário salvo
     */
    public boolean hasSavedInventory() {
        return savedInventory != null;
    }

    /**
     * Limpa o inventário salvo sem restaurar.
     * Usado quando o inventário salvo está desatualizado.
     */
    public void clearSavedInventory() {
        savedInventory = null;
        savedArmor = null;
        savedLevel = 0;
        savedExp = 0;
        savedGameMode = null;
    }

    @Override
    public String toString() {
        return "PlayerProfile{" +
                "name='" + player.getName() + '\'' +
                ", kills=" + getKills() +
                ", deaths=" + getDeaths() +
                ", coins=" + getCoins() +
                ", sessionTime=" + (getSessionTime() / 1000) + "s" +
                '}';
    }
}
