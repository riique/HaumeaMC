package com.haumea.kitpvp.managers;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.models.Group;
import com.haumea.kitpvp.models.Warp;
import com.haumea.kitpvp.profile.PlayerProfile;
import com.haumea.kitpvp.utils.ChatStorage;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.ConcurrentHashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Gerenciador centralizado de interacoes especiais do HaumeaMC.
 * 
 * Este manager controla:
 * - Lapis-lazuli automatico para mesas de encantamento
 * - Placas de suprimentos (Sopas, Recraft, Cocoabean, Cactus)
 * - Placas de desafio/completar (Easy, Medium, Hard, Extreme)
 * - Placas de rank (grupos temporarios)
 * - Formatacao automatica de cores em placas
 * 
 * @author HaumeaMC
 */
public class InteractionManager implements Listener {

    private final HaumeaMC plugin;

    // ==================== CONSTANTS ====================

    // Prefixos de categorias de placas - Estilo HaumeaMC (SEM ACENTOS)
    private static final String SIGN_CATEGORY_KITPVP = "&6&lHAUMEA";
    private static final String SIGN_CATEGORY_COMPLETAR = "&b&lDESAFIO";
    private static final String SIGN_CATEGORY_RANK = "&d&lRANK";

    // Separador visual
    private static final String SIGN_SEPARATOR = "&8&m--------";

    // Duracao do grupo temporario (30 minutos + 1 segundo)
    private static final long TEMP_GROUP_DURATION_MS = (30 * 60 * 1000) + 1000;

    // ==================== ENUMS ====================

    /**
     * Tipos de placas de suprimentos (categoria KITPVP)
     * Estilo visual do HaumeaMC (SEM ACENTOS)
     */
    public enum SupplySignType {
        SOPAS("Sopas", "&c", "&c&lSOPAS", "&fClique aqui"),
        RECRAFT("Recraft", "&e", "&e&lRECRAFT", "&7Cogumelos"),
        COCOABEAN("Cocoabean", "&6", "&6&lCOCOA", "&7CocoaBeans"),
        CACTUS("Cactus", "&a", "&a&lCACTUS", "&7Cactus");

        private final String displayName;
        private final String color;
        private final String signTitle;
        private final String signSubtitle;
        private final int inventorySize;

        SupplySignType(String displayName, String color, String signTitle, String signSubtitle) {
            this.displayName = displayName;
            this.color = color;
            this.signTitle = signTitle;
            this.signSubtitle = signSubtitle;
            this.inventorySize = displayName.equals("Sopas") ? 54 : 9;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getColor() {
            return color;
        }

        public int getInventorySize() {
            return inventorySize;
        }

        public String getSignTitle() {
            return ChatStorage.colorize(signTitle);
        }

        public String getSignSubtitle() {
            return ChatStorage.colorize(signSubtitle);
        }

        public String getFormattedName() {
            return ChatStorage.colorize(signTitle);
        }
    }

    /**
     * Niveis de desafio (categoria COMPLETAR)
     * Estilo visual com cores progressivas (SEM ACENTOS)
     */
    public enum ChallengeLevel {
        EASY("Facil", "&a", 20, false, "&a&lFACIL", "&a+20 coins"),
        MEDIUM("Medio", "&e", 50, false, "&e&lMEDIO", "&e+50 coins"),
        HARD("Dificil", "&c", 100, false, "&c&lDIFICIL", "&c+100 coins"),
        EXTREME("Extreme", "&4", 300, true, "&4&lEXTREME", "&4+300 coins");

        private final String displayName;
        private final String color;
        private final int coinReward;
        private final boolean broadcast;
        private final String signTitle;
        private final String signReward;

        ChallengeLevel(String displayName, String color, int coinReward, boolean broadcast,
                String signTitle, String signReward) {
            this.displayName = displayName;
            this.color = color;
            this.coinReward = coinReward;
            this.broadcast = broadcast;
            this.signTitle = signTitle;
            this.signReward = signReward;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getColor() {
            return color;
        }

        public int getCoinReward() {
            return coinReward;
        }

        public boolean shouldBroadcast() {
            return broadcast;
        }

        public String getSignTitle() {
            return ChatStorage.colorize(signTitle);
        }

        public String getSignReward() {
            return ChatStorage.colorize(signReward);
        }

        public String getFormattedName() {
            return ChatStorage.colorize(signTitle);
        }
    }

    // ==================== TRACKING SETS ====================

    /**
     * Jogadores que estao com inventario de encantamento aberto
     */
    private final Set<UUID> enchantingPlayers = new HashSet<>();

    /**
     * Mapeamento de palavras-chave para tipos de placas de suprimentos
     */
    private final Map<String, SupplySignType> supplyKeywords = new ConcurrentHashMap<>();

    /**
     * Mapeamento de palavras-chave para niveis de desafio
     */
    private final Map<String, ChallengeLevel> challengeKeywords = new ConcurrentHashMap<>();

    // ==================== CONSTRUCTOR ====================

    public InteractionManager(HaumeaMC plugin) {
        this.plugin = plugin;
        initKeywordMappings();
        plugin.getLogger().info("[Interacoes] InteractionManager inicializado com sucesso!");
    }

    /**
     * Inicializa os mapeamentos de palavras-chave para tipos de placas
     */
    private void initKeywordMappings() {
        // Palavras-chave para placas de suprimentos
        supplyKeywords.put("sopa", SupplySignType.SOPAS);
        supplyKeywords.put("sopas", SupplySignType.SOPAS);
        supplyKeywords.put("recraft", SupplySignType.RECRAFT);
        supplyKeywords.put("recrafts", SupplySignType.RECRAFT);
        supplyKeywords.put("cocoa", SupplySignType.COCOABEAN);
        supplyKeywords.put("cocoabean", SupplySignType.COCOABEAN);
        supplyKeywords.put("cactu", SupplySignType.CACTUS);
        supplyKeywords.put("cactus", SupplySignType.CACTUS);

        // Palavras-chave para placas de desafio
        challengeKeywords.put("easy", ChallengeLevel.EASY);
        challengeKeywords.put("medium", ChallengeLevel.MEDIUM);
        challengeKeywords.put("hard", ChallengeLevel.HARD);
        challengeKeywords.put("extreme", ChallengeLevel.EXTREME);
    }

    // ==================== SIGN CREATION (SignChangeEvent) ====================

    /**
     * Processa a criacao/edicao de placas.
     * - Verifica palavras-chave e formata placas especiais
     * - Converte & para secao em qualquer placa
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSignChange(SignChangeEvent event) {
        String[] lines = event.getLines();

        // Primeiro, converter cores em todas as linhas
        for (int i = 0; i < 4; i++) {
            if (lines[i] != null) {
                event.setLine(i, ChatStorage.colorize(lines[i]));
            }
        }

        // Verificar se ha palavras-chave na primeira linha
        String firstLine = lines[0] != null ? lines[0].toLowerCase().trim() : "";

        // Verificar placas de suprimentos
        SupplySignType supplyType = supplyKeywords.get(firstLine);
        if (supplyType != null) {
            formatSupplySign(event, supplyType);
            return;
        }

        // Verificar placas de desafio
        ChallengeLevel challengeLevel = challengeKeywords.get(firstLine);
        if (challengeLevel != null) {
            formatChallengeSign(event, challengeLevel);
            return;
        }

        // Verificar placas de rank (formato: rank:NOME_DO_GRUPO)
        if (firstLine.startsWith("rank:")) {
            String groupName = lines[0].substring(5).trim();
            formatRankSign(event, groupName);
        }
    }

    /**
     * Formata uma placa de suprimentos no padrao visual do HaumeaMC.
     * 
     * Layout:
     * [Linha 1] HAUMEA (laranja)
     * [Linha 2] SOPAS (vermelho)
     * [Linha 3] --------
     * [Linha 4] Clique aqui
     */
    private void formatSupplySign(SignChangeEvent event, SupplySignType type) {
        event.setLine(0, ChatStorage.colorize(SIGN_CATEGORY_KITPVP));
        event.setLine(1, type.getSignTitle());
        event.setLine(2, ChatStorage.colorize(SIGN_SEPARATOR));
        event.setLine(3, type.getSignSubtitle());
    }

    /**
     * Formata uma placa de desafio no padrao visual do HaumeaMC.
     * 
     * Layout:
     * [Linha 1] DESAFIO (aqua)
     * [Linha 2] FACIL (verde)
     * [Linha 3] +20 coins
     * [Linha 4] --------
     */
    private void formatChallengeSign(SignChangeEvent event, ChallengeLevel level) {
        event.setLine(0, ChatStorage.colorize(SIGN_CATEGORY_COMPLETAR));
        event.setLine(1, level.getSignTitle());
        event.setLine(2, level.getSignReward());
        event.setLine(3, ChatStorage.colorize(SIGN_SEPARATOR));
    }

    /**
     * Formata uma placa de rank no padrao visual do HaumeaMC.
     * 
     * Layout:
     * [Linha 1] RANK (rosa)
     * [Linha 2] (prefixo do grupo)
     * [Linha 3] 30 minutos
     * [Linha 4] Clique!
     */
    private void formatRankSign(SignChangeEvent event, String groupName) {
        GroupManager groupManager = plugin.getGroupManager();
        Group group = groupManager.getGroup(groupName);

        String displayName;
        if (group != null) {
            // Usar o prefixo colorido do grupo
            displayName = group.getPrefix();
            if (displayName == null || displayName.isEmpty()) {
                displayName = "&e&l" + group.getDisplayName().toUpperCase();
            }
        } else {
            // Grupo nao existe, mostrar nome em vermelho
            displayName = "&c" + groupName;
        }

        event.setLine(0, ChatStorage.colorize(SIGN_CATEGORY_RANK));
        event.setLine(1, ChatStorage.colorize(displayName));
        event.setLine(2, ChatStorage.colorize("&730 minutos"));
        event.setLine(3, ChatStorage.colorize("&a&lCLIQUE!"));
    }

    // ==================== SIGN INTERACTIONS (PlayerInteractEvent)
    // ====================

    /**
     * Processa cliques em placas especiais
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }

        // Verificar se e uma placa
        if (block.getType() != Material.WALL_SIGN && block.getType() != Material.SIGN_POST) {
            return;
        }

        if (!(block.getState() instanceof Sign)) {
            return;
        }

        Sign sign = (Sign) block.getState();
        String[] lines = sign.getLines();
        String firstLine = lines[0];

        Player player = event.getPlayer();

        // Verificar categoria da placa (comparar sem cores)
        String strippedFirstLine = ChatStorage.stripColors(firstLine);

        if (strippedFirstLine.equals("HAUMEA")) {
            handleSupplySign(event, player, lines);
        } else if (strippedFirstLine.equals("DESAFIO")) {
            handleChallengeSign(event, player, lines);
        } else if (strippedFirstLine.equals("RANK")) {
            handleRankSign(event, player, lines, block);
        }
    }

    /**
     * Processa clique em placa de suprimentos
     */
    private void handleSupplySign(PlayerInteractEvent event, Player player, String[] lines) {
        event.setCancelled(true);

        String line1 = ChatStorage.stripColors(lines[1]);
        if (line1 == null || line1.isEmpty())
            return;

        // Identificar o tipo pelo nome
        SupplySignType type = null;
        for (SupplySignType t : SupplySignType.values()) {
            if (line1.equalsIgnoreCase(t.getDisplayName()) || line1.equalsIgnoreCase(t.name())) {
                type = t;
                break;
            }
        }

        if (type == null)
            return;

        // Abrir inventario de suprimentos
        Inventory inventory = createSupplyInventory(type);
        player.openInventory(inventory);
    }

    /**
     * Cria um inventario de suprimentos baseado no tipo
     */
    private Inventory createSupplyInventory(SupplySignType type) {
        String title = ChatStorage.colorize("&8" + type.getDisplayName());
        Inventory inventory = Bukkit.createInventory(null, type.getInventorySize(), title);

        switch (type) {
            case SOPAS:
                // Preencher todos os 54 slots com Mushroom Soup
                ItemStack soup = new ItemStack(Material.MUSHROOM_SOUP);
                for (int i = 0; i < 54; i++) {
                    inventory.setItem(i, soup.clone());
                }
                break;

            case RECRAFT:
                // Slots 3, 4, 5 (centro): 64 Bowls, 64 Red Mushroom, 64 Brown Mushroom
                inventory.setItem(3, new ItemStack(Material.BOWL, 64));
                inventory.setItem(4, new ItemStack(Material.RED_MUSHROOM, 64));
                inventory.setItem(5, new ItemStack(Material.BROWN_MUSHROOM, 64));
                break;

            case COCOABEAN:
                // Slots 3, 4, 5: 64 Bowls, 64 Cocoa Beans, 64 Cocoa Beans
                inventory.setItem(3, new ItemStack(Material.BOWL, 64));
                // Cocoa Beans = INK_SACK com data 3
                @SuppressWarnings("deprecation")
                ItemStack cocoaBeans = new ItemStack(Material.INK_SACK, 64, DyeColor.BROWN.getDyeData());
                inventory.setItem(4, cocoaBeans.clone());
                inventory.setItem(5, cocoaBeans.clone());
                break;

            case CACTUS:
                // Slots 3, 4, 5: 64 Bowls, 64 Cactus, 64 Cactus
                inventory.setItem(3, new ItemStack(Material.BOWL, 64));
                inventory.setItem(4, new ItemStack(Material.CACTUS, 64));
                inventory.setItem(5, new ItemStack(Material.CACTUS, 64));
                break;
        }

        return inventory;
    }

    /**
     * Processa clique em placa de desafio
     */
    private void handleChallengeSign(PlayerInteractEvent event, Player player, String[] lines) {
        event.setCancelled(true);

        String line1 = ChatStorage.stripColors(lines[1]);
        if (line1 == null || line1.isEmpty())
            return;

        // Identificar o nivel pelo nome
        ChallengeLevel level = null;
        for (ChallengeLevel l : ChallengeLevel.values()) {
            if (line1.equalsIgnoreCase(l.getDisplayName()) || line1.equalsIgnoreCase(l.name())) {
                level = l;
                break;
            }
        }

        if (level == null)
            return;

        // Processar conclusao do desafio
        processChallengeCompletion(player, level);
    }

    /**
     * Processa a conclusao de um desafio
     */
    private void processChallengeCompletion(Player player, ChallengeLevel level) {
        // 1. Teleportar ao spawn
        teleportToSpawn(player);

        // 2. Incrementar contador de conclusoes
        incrementChallengeCount(player, level);

        // 3. Adicionar coins
        addCoinsReward(player, level.getCoinReward());

        // 4. Enviar mensagem de parabens
        sendCongratulationMessage(player, level);

        // 5. Broadcast se for Extreme
        if (level.shouldBroadcast()) {
            broadcastChallengeCompletion(player, level);
        }
    }

    /**
     * Teleporta o jogador ao spawn
     */
    private void teleportToSpawn(Player player) {
        WarpsManager warpsManager = plugin.getWarpsManager();
        if (warpsManager == null)
            return;

        Warp spawn = warpsManager.getWarp("spawn");
        if (spawn != null) {
            player.teleport(spawn.toLocation());
        }
    }

    /**
     * Incrementa o contador de conclusoes do desafio no perfil do jogador
     */
    private void incrementChallengeCount(Player player, ChallengeLevel level) {
        PlayerProfile profile = plugin.getProfileManager().getProfile(player);
        if (profile == null)
            return;

        String key = "challenge_" + level.name().toLowerCase() + "_completions";
        Integer currentCount = profile.getData().getCustomData(key, Integer.class);
        if (currentCount == null) {
            currentCount = 0;
        }
        profile.getData().setCustomData(key, currentCount + 1);
    }

    /**
     * Adiciona coins de recompensa ao jogador
     */
    private void addCoinsReward(Player player, int amount) {
        StatsManager statsManager = plugin.getStatsManager();
        if (statsManager == null)
            return;

        statsManager.addMoney(player, amount);
    }

    /**
     * Envia mensagem de parabens personalizada
     */
    private void sendCongratulationMessage(Player player, ChallengeLevel level) {
        ChatStorage.sendRaw(player, "&aParabens, voce completou o nivel " + level.getDisplayName() + "!");
        ChatStorage.sendRaw(player, "&a+" + level.getCoinReward() + " COINS");
    }

    /**
     * Faz broadcast da conclusao do desafio Extreme
     */
    private void broadcastChallengeCompletion(Player player, ChallengeLevel level) {
        String message = ChatStorage
                .colorize("&b" + player.getName() + " &ecompletou o nivel &bextreme &eda &blava challenge&e!");
        Bukkit.broadcastMessage(message);
    }

    /**
     * Processa clique em placa de rank
     */
    private void handleRankSign(PlayerInteractEvent event, Player player, String[] lines, Block block) {
        event.setCancelled(true);

        // Verificar grupo pelo prefixo na linha 2
        String groupLine = ChatStorage.stripColors(lines[1]).trim().toLowerCase();
        if (groupLine.isEmpty())
            return;

        GroupManager groupManager = plugin.getGroupManager();
        if (groupManager == null)
            return;

        // Tentar encontrar o grupo pelo display name
        String groupName = null;
        for (Group g : groupManager.getAllGroups()) {
            if (g.getDisplayName().equalsIgnoreCase(groupLine) ||
                    g.getName().equalsIgnoreCase(groupLine)) {
                groupName = g.getName();
                break;
            }
        }

        if (groupName == null || !groupManager.groupExists(groupName)) {
            ChatStorage.sendRaw(player, "&cAlgo de errado nao esta certo :(");
            return;
        }

        // 1. Destruir a placa naturalmente
        block.breakNaturally();

        // 2. Conceder o grupo temporario (30 minutos + 1 segundo)
        long expiration = System.currentTimeMillis() + TEMP_GROUP_DURATION_MS;
        groupManager.addPlayerGroup(player, groupName, expiration);

        // 3. Mensagem de confirmacao
        Group group = groupManager.getGroup(groupName);
        String displayName = group != null ? group.getDisplayName() : groupName;
        ChatStorage.sendRaw(player, "&aVoce recebeu o grupo &e" + displayName + " &apor &e30 minutos&a!");

        // 4. Atualizar display do jogador
        if (plugin.getDisplayManager() != null) {
            plugin.getDisplayManager().refreshPlayer(player);
        }
    }

    // ==================== ENCHANTING TABLE LAPIS SYSTEM ====================

    /**
     * Quando um jogador abre uma mesa de encantamento, insere 64 lapis-lazuli
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }

        InventoryView view = event.getView();
        if (view.getTopInventory().getType() != InventoryType.ENCHANTING) {
            return;
        }

        Player player = (Player) event.getPlayer();

        // Criar lapis-lazuli (INK_SACK com data 4 = Lapis Lazuli)
        @SuppressWarnings("deprecation")
        ItemStack lapisLazuli = new ItemStack(Material.INK_SACK, 64, DyeColor.BLUE.getDyeData());

        // Slot 1 e o slot do lapis-lazuli na mesa de encantamento
        // Atrasar 1 tick para garantir que o inventario esta aberto
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && player.getOpenInventory() != null
                    && player.getOpenInventory().getTopInventory().getType() == InventoryType.ENCHANTING) {
                player.getOpenInventory().getTopInventory().setItem(1, lapisLazuli);
                enchantingPlayers.add(player.getUniqueId());
            }
        }, 1L);
    }

    /**
     * Bloqueia a remocao do lapis-lazuli do slot 1 da mesa de encantamento
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        InventoryView view = event.getView();
        if (view.getTopInventory().getType() != InventoryType.ENCHANTING) {
            return;
        }

        // Verificar se esta clicando no slot do lapis (slot 1)
        if (event.getRawSlot() == 1) {
            // Cancelar qualquer interacao com o slot do lapis
            event.setCancelled(true);
            return;
        }

        // Tambem bloquear shift-click que poderia mover o lapis
        if (event.isShiftClick() && event.getRawSlot() == 1) {
            event.setCancelled(true);
        }
    }

    /**
     * Remove o lapis-lazuli quando o jogador fecha a mesa de encantamento
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getPlayer();

        // Verificar se era um inventario de encantamento
        if (event.getView().getTopInventory().getType() != InventoryType.ENCHANTING) {
            return;
        }

        // Remover o lapis-lazuli se o jogador estava encantando
        if (enchantingPlayers.remove(player.getUniqueId())) {
            // Limpar o slot do lapis para evitar duplicacao
            event.getView().getTopInventory().setItem(1, null);
        }
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Obtem o numero de conclusoes de um desafio especifico
     * 
     * @param player Jogador
     * @param level  Nivel do desafio
     * @return Numero de conclusoes
     */
    public int getChallengeCompletions(Player player, ChallengeLevel level) {
        PlayerProfile profile = plugin.getProfileManager().getProfile(player);
        if (profile == null)
            return 0;

        String key = "challenge_" + level.name().toLowerCase() + "_completions";
        Integer count = profile.getData().getCustomData(key, Integer.class);
        return count != null ? count : 0;
    }

    /**
     * Obtem o total de conclusoes de todos os desafios
     * 
     * @param player Jogador
     * @return Total de conclusoes
     */
    public int getTotalChallengeCompletions(Player player) {
        int total = 0;
        for (ChallengeLevel level : ChallengeLevel.values()) {
            total += getChallengeCompletions(player, level);
        }
        return total;
    }

    /**
     * Verifica se um jogador ja completou um desafio especifico
     * 
     * @param player Jogador
     * @param level  Nivel do desafio
     * @return true se ja completou pelo menos uma vez
     */
    public boolean hasCompletedChallenge(Player player, ChallengeLevel level) {
        return getChallengeCompletions(player, level) > 0;
    }

    /**
     * Limpa o estado de um jogador ao sair (cleanup)
     * 
     * @param player Jogador
     */
    public void cleanup(Player player) {
        enchantingPlayers.remove(player.getUniqueId());
    }
}
