package com.haumea.kitpvp.managers;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.utils.ChatStorage;
import com.haumea.kitpvp.utils.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Gerenciador Central do Sistema de Feast do HaumeaMC.
 * 
 * Este manager é responsável por todo o ciclo de vida do Feast:
 * - Gerenciamento de localizações de baús
 * - Controle do timer do evento
 * - Geração e distribuição de itens
 * - Ciclo de vida do evento (início, execução, término)
 * - Persistência de configurações
 * - Broadcast de mensagens
 * 
 * O Feast é um evento periódico onde baús com itens valiosos aparecem
 * em localizações pré-definidas, incentivando jogadores a competir
 * pelos recursos.
 * 
 * @author HaumeaMC
 */
public class FeastManager {

    private final HaumeaMC plugin;

    // ==================== PERSISTÊNCIA ====================
    private File feastFile;
    private FileConfiguration feastConfig;

    // ==================== LOCALIZAÇÕES DE BAÚS ====================
    private final List<FeastLocation> chestLocations;

    // ==================== ESTADO DO FEAST ====================
    private FeastState state;
    private int timerSeconds;
    private int defaultTimerSeconds;
    private int cleanupDelaySeconds;

    // ==================== TASKS ====================
    private BukkitTask countdownTask;
    private BukkitTask cleanupTask;

    // ==================== BAÚS SPAWANADOS ====================
    private final Set<Location> spawnedChests;

    // ==================== ITENS DO FEAST ====================
    private final List<FeastItem> possibleItems;

    // ==================== CONFIGURAÇÕES ====================
    private static final String CONFIG_PATH = "feast.yml";
    private static final int MIN_ITEMS_PER_CHEST = 5;
    private static final int MAX_ITEMS_PER_CHEST = 12;

    /**
     * Estado do Feast
     */
    public enum FeastState {
        IDLE, // Sem feast ativo, aguardando próximo
        COUNTDOWN, // Contagem regressiva ativa
        ACTIVE, // Feast spawnou, baús disponíveis
        CLEANUP // Festa encerrada, limpando baús
    }

    /**
     * Representa uma localização de baú do Feast
     */
    public static class FeastLocation {
        private final String worldName;
        private final int x, y, z;
        private final String id;

        public FeastLocation(Location location) {
            this.worldName = location.getWorld().getName();
            this.x = location.getBlockX();
            this.y = location.getBlockY();
            this.z = location.getBlockZ();
            this.id = UUID.randomUUID().toString().substring(0, 8);
        }

        public FeastLocation(String worldName, int x, int y, int z, String id) {
            this.worldName = worldName;
            this.x = x;
            this.y = y;
            this.z = z;
            this.id = id;
        }

        public Location toLocation() {
            World world = Bukkit.getWorld(worldName);
            if (world == null)
                return null;
            return new Location(world, x, y, z);
        }

        public boolean isValid() {
            return Bukkit.getWorld(worldName) != null;
        }

        public String getId() {
            return id;
        }

        public String getWorldName() {
            return worldName;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public int getZ() {
            return z;
        }

        public double distanceSquared(Location loc) {
            if (!loc.getWorld().getName().equals(worldName))
                return Double.MAX_VALUE;
            double dx = x - loc.getX();
            double dy = y - loc.getY();
            double dz = z - loc.getZ();
            return dx * dx + dy * dy + dz * dz;
        }

        @Override
        public String toString() {
            return String.format("%s: %s (%d, %d, %d)", id, worldName, x, y, z);
        }
    }

    /**
     * Representa um item possível no Feast
     */
    public static class FeastItem {
        private final Material material;
        private final String name;
        private final int minAmount;
        private final int maxAmount;
        private final int weight; // Peso para probabilidade
        private final Map<Enchantment, Integer> enchantments;

        public FeastItem(Material material, String name, int minAmount, int maxAmount, int weight) {
            this.material = material;
            this.name = name;
            this.minAmount = minAmount;
            this.maxAmount = maxAmount;
            this.weight = weight;
            this.enchantments = new HashMap<>();
        }

        public FeastItem addEnchantment(Enchantment enchant, int level) {
            enchantments.put(enchant, level);
            return this;
        }

        public ItemStack generate() {
            int amount = randomBetween(minAmount, maxAmount);
            ItemBuilder builder = new ItemBuilder(material)
                    .amount(amount);

            if (name != null && !name.isEmpty()) {
                builder.name(name);
            }

            for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
                builder.enchant(entry.getKey(), entry.getValue());
            }

            return builder.build();
        }

        public int getWeight() {
            return weight;
        }

        public Material getMaterial() {
            return material;
        }
    }

    public FeastManager(HaumeaMC plugin) {
        this.plugin = plugin;
        this.chestLocations = new ArrayList<>();
        this.spawnedChests = new HashSet<>();
        this.possibleItems = new ArrayList<>();
        this.state = FeastState.IDLE;
        this.defaultTimerSeconds = 1800; // 30 minutos
        this.cleanupDelaySeconds = 300; // 5 minutos

        loadConfiguration();
        initializeDefaultItems();

        plugin.getLogger()
                .info("FeastManager inicializado. " + chestLocations.size() + " localização(ões) configurada(s).");
    }

    // ==================== PERSISTÊNCIA ====================

    /**
     * Carrega as configurações do arquivo feast.yml
     */
    private void loadConfiguration() {
        feastFile = new File(plugin.getDataFolder(), CONFIG_PATH);

        if (!feastFile.exists()) {
            try {
                feastFile.createNewFile();
                saveDefaultConfiguration();
            } catch (IOException e) {
                plugin.getLogger().severe("Erro ao criar feast.yml: " + e.getMessage());
            }
        }

        feastConfig = YamlConfiguration.loadConfiguration(feastFile);

        // Carregar configurações gerais
        defaultTimerSeconds = feastConfig.getInt("settings.timer-seconds", 1800);
        cleanupDelaySeconds = feastConfig.getInt("settings.cleanup-delay", 300);
        timerSeconds = defaultTimerSeconds;

        // Carregar localizações
        chestLocations.clear();
        ConfigurationSection locationsSection = feastConfig.getConfigurationSection("locations");
        if (locationsSection != null) {
            for (String key : locationsSection.getKeys(false)) {
                String path = "locations." + key;
                String worldName = feastConfig.getString(path + ".world", "world");
                int x = feastConfig.getInt(path + ".x", 0);
                int y = feastConfig.getInt(path + ".y", 64);
                int z = feastConfig.getInt(path + ".z", 0);

                FeastLocation loc = new FeastLocation(worldName, x, y, z, key);
                chestLocations.add(loc);
            }
        }
    }

    /**
     * Salva a configuração padrão
     */
    private void saveDefaultConfiguration() {
        feastConfig = new YamlConfiguration();

        // Configurações gerais
        feastConfig.set("settings.timer-seconds", 1800);
        feastConfig.set("settings.cleanup-delay", 300);
        feastConfig.set("settings.min-items-per-chest", MIN_ITEMS_PER_CHEST);
        feastConfig.set("settings.max-items-per-chest", MAX_ITEMS_PER_CHEST);

        // Broadcast times (em segundos)
        feastConfig.set("settings.broadcast-times", Arrays.asList(900, 600, 300, 180, 60, 30, 10, 5, 4, 3, 2, 1));

        saveConfiguration();
    }

    /**
     * Salva as configurações no arquivo
     */
    public void saveConfiguration() {
        // Salvar configurações
        feastConfig.set("settings.timer-seconds", defaultTimerSeconds);
        feastConfig.set("settings.cleanup-delay", cleanupDelaySeconds);

        // Salvar localizações
        feastConfig.set("locations", null); // Limpar seção
        for (FeastLocation loc : chestLocations) {
            String path = "locations." + loc.getId();
            feastConfig.set(path + ".world", loc.getWorldName());
            feastConfig.set(path + ".x", loc.getX());
            feastConfig.set(path + ".y", loc.getY());
            feastConfig.set(path + ".z", loc.getZ());
        }

        try {
            feastConfig.save(feastFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Erro ao salvar feast.yml: " + e.getMessage());
        }
    }

    /**
     * Salva as configurações de forma assíncrona
     */
    public void saveConfigurationAsync() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, this::saveConfiguration);
    }

    /**
     * Recarrega as configurações do arquivo
     */
    public void reload() {
        stopFeast(true);
        loadConfiguration();
        plugin.getLogger().info("Configurações do Feast recarregadas.");
    }

    // ==================== GERENCIAMENTO DE LOCALIZAÇÕES ====================

    /**
     * Adiciona uma nova localização de baú
     * 
     * @param location Localização a adicionar
     * @return FeastLocation criada
     */
    public FeastLocation addChestLocation(Location location) {
        FeastLocation feastLoc = new FeastLocation(location);
        chestLocations.add(feastLoc);
        saveConfigurationAsync();
        return feastLoc;
    }

    /**
     * Remove a localização mais próxima de uma posição
     * 
     * @param location Posição de referência
     * @return true se removeu, false se não encontrou
     */
    public boolean removeNearestChestLocation(Location location) {
        if (chestLocations.isEmpty())
            return false;

        FeastLocation nearest = null;
        double nearestDist = Double.MAX_VALUE;

        for (FeastLocation feastLoc : chestLocations) {
            double dist = feastLoc.distanceSquared(location);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = feastLoc;
            }
        }

        if (nearest != null && nearestDist < 100) { // Máximo 10 blocos
            chestLocations.remove(nearest);
            saveConfigurationAsync();
            return true;
        }

        return false;
    }

    /**
     * Remove uma localização pelo ID
     * 
     * @param id ID da localização
     * @return true se removeu
     */
    public boolean removeChestLocation(String id) {
        boolean removed = chestLocations.removeIf(loc -> loc.getId().equals(id));
        if (removed) {
            saveConfigurationAsync();
        }
        return removed;
    }

    /**
     * Obtém todas as localizações configuradas
     */
    public List<FeastLocation> getChestLocations() {
        return new ArrayList<>(chestLocations);
    }

    /**
     * Verifica se há localizações configuradas
     */
    public boolean hasChestLocations() {
        return !chestLocations.isEmpty();
    }

    /**
     * Obtém a quantidade de localizações
     */
    public int getChestLocationCount() {
        return chestLocations.size();
    }

    // ==================== CICLO DE VIDA DO FEAST ====================

    /**
     * Inicia a contagem regressiva do Feast
     * 
     * @param starter Jogador que iniciou (pode ser null)
     * @return true se iniciou com sucesso
     */
    public boolean startCountdown(Player starter) {
        if (state != FeastState.IDLE) {
            if (starter != null) {
                ChatStorage.send(starter, "feast.already-active");
            }
            return false;
        }

        if (!hasChestLocations()) {
            if (starter != null) {
                ChatStorage.send(starter, "feast.no-locations");
            }
            return false;
        }

        state = FeastState.COUNTDOWN;
        timerSeconds = defaultTimerSeconds;

        // Broadcast de início
        broadcast("feast.countdown-started", "time", formatTime(timerSeconds));

        // Iniciar task de contagem regressiva
        startCountdownTask();

        plugin.getLogger().info("Feast countdown iniciado por " +
                (starter != null ? starter.getName() : "Sistema") +
                ". Tempo: " + timerSeconds + "s");

        return true;
    }

    /**
     * Inicia o Feast imediatamente (pula contagem regressiva)
     * 
     * @param starter Jogador que iniciou
     * @return true se iniciou com sucesso
     */
    public boolean startFeastNow(Player starter) {
        if (state == FeastState.ACTIVE) {
            if (starter != null) {
                ChatStorage.send(starter, "feast.already-active");
            }
            return false;
        }

        if (!hasChestLocations()) {
            if (starter != null) {
                ChatStorage.send(starter, "feast.no-locations");
            }
            return false;
        }

        // Cancelar countdown se estiver rodando
        cancelCountdownTask();

        // Spawnar o feast
        spawnFeast();

        plugin.getLogger().info("Feast iniciado imediatamente por " +
                (starter != null ? starter.getName() : "Sistema"));

        return true;
    }

    /**
     * Para o Feast atual
     * 
     * @param silent Se true, não envia broadcasts
     * @return true se parou
     */
    public boolean stopFeast(boolean silent) {
        if (state == FeastState.IDLE) {
            return false;
        }

        cancelCountdownTask();
        cancelCleanupTask();

        // Remover baús spawados
        cleanupChests();

        FeastState previousState = state;
        state = FeastState.IDLE;
        timerSeconds = defaultTimerSeconds;

        if (!silent && previousState != FeastState.IDLE) {
            broadcast("feast.cancelled");
        }

        plugin.getLogger().info("Feast parado.");

        return true;
    }

    /**
     * Define o tempo do timer
     * 
     * @param seconds Segundos para o próximo feast
     */
    public void setTimer(int seconds) {
        if (seconds < 1)
            seconds = 1;

        if (state == FeastState.COUNTDOWN) {
            timerSeconds = seconds;
        } else {
            defaultTimerSeconds = seconds;
            timerSeconds = seconds;
        }

        saveConfigurationAsync();
    }

    /**
     * Obtém o tempo restante em segundos
     */
    public int getTimeRemaining() {
        return timerSeconds;
    }

    /**
     * Obtém o estado atual
     */
    public FeastState getState() {
        return state;
    }

    /**
     * Verifica se o feast está ativo (baús spawados)
     */
    public boolean isFeastActive() {
        return state == FeastState.ACTIVE;
    }

    /**
     * Verifica se há countdown em andamento
     */
    public boolean isCountdownActive() {
        return state == FeastState.COUNTDOWN;
    }

    // ==================== TASKS ====================

    /**
     * Inicia a task de contagem regressiva
     */
    private void startCountdownTask() {
        cancelCountdownTask();

        countdownTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (state != FeastState.COUNTDOWN) {
                    cancel();
                    return;
                }

                timerSeconds--;

                // Verificar se deve fazer broadcast
                if (shouldBroadcast(timerSeconds)) {
                    broadcastCountdown();
                }

                // Verificar se chegou a zero
                if (timerSeconds <= 0) {
                    cancel();
                    spawnFeast();
                }
            }
        }.runTaskTimer(plugin, 20L, 20L); // A cada segundo
    }

    /**
     * Cancela a task de contagem regressiva
     */
    private void cancelCountdownTask() {
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
    }

    /**
     * Inicia a task de limpeza
     */
    private void startCleanupTask() {
        cancelCleanupTask();

        cleanupTask = new BukkitRunnable() {
            @Override
            public void run() {
                cleanupFeast();
            }
        }.runTaskLater(plugin, cleanupDelaySeconds * 20L);
    }

    /**
     * Cancela a task de limpeza
     */
    private void cancelCleanupTask() {
        if (cleanupTask != null) {
            cleanupTask.cancel();
            cleanupTask = null;
        }
    }

    /**
     * Verifica se deve fazer broadcast
     */
    private boolean shouldBroadcast(int seconds) {
        // Tempos de broadcast: 15min, 10min, 5min, 3min, 1min, 30s, 10s, 5s, 4s, 3s,
        // 2s, 1s
        return seconds == 900 || seconds == 600 || seconds == 300 ||
                seconds == 180 || seconds == 60 ||
                seconds == 30 || seconds == 10 ||
                (seconds <= 5 && seconds >= 1);
    }

    /**
     * Envia broadcast de contagem regressiva
     */
    private void broadcastCountdown() {
        String timeStr = formatTime(timerSeconds);
        broadcast("feast.countdown", "time", timeStr);
    }

    // ==================== SPAWN DO FEAST ====================

    /**
     * Spawna o Feast (coloca os baús)
     */
    private void spawnFeast() {
        state = FeastState.ACTIVE;
        spawnedChests.clear();

        // Determinar localização principal para broadcast
        Location mainLocation = null;
        int chestsSpawned = 0;

        for (FeastLocation feastLoc : chestLocations) {
            Location loc = feastLoc.toLocation();
            if (loc == null || !feastLoc.isValid())
                continue;

            // Verificar se o bloco está livre ou pode ser substituído
            Block block = loc.getBlock();
            if (block.getType() != Material.AIR &&
                    block.getType() != Material.CHEST &&
                    block.getType() != Material.GRASS &&
                    block.getType() != Material.LONG_GRASS) {
                // Não substituir blocos importantes
                continue;
            }

            // Colocar baú
            block.setType(Material.CHEST);

            // Preencher com itens
            if (block.getState() instanceof Chest) {
                Chest chest = (Chest) block.getState();
                fillChest(chest.getInventory());
            }

            spawnedChests.add(loc);
            chestsSpawned++;

            if (mainLocation == null) {
                mainLocation = loc;
            }
        }

        // Broadcast de spawn
        if (mainLocation != null) {
            broadcast("feast.spawned",
                    "x", String.valueOf(mainLocation.getBlockX()),
                    "y", String.valueOf(mainLocation.getBlockY()),
                    "z", String.valueOf(mainLocation.getBlockZ()),
                    "count", String.valueOf(chestsSpawned));
        }

        // Iniciar task de limpeza
        startCleanupTask();

        plugin.getLogger().info("Feast spawnou com " + chestsSpawned + " baú(s).");
    }

    /**
     * Limpa o Feast (encerra período ativo)
     */
    private void cleanupFeast() {
        state = FeastState.CLEANUP;

        cleanupChests();

        broadcast("feast.cleaned");

        state = FeastState.IDLE;
        timerSeconds = defaultTimerSeconds;

        // Reiniciar ciclo automaticamente
        if (hasChestLocations()) {
            startCountdown(null);
        }

        plugin.getLogger().info("Feast limpo. Próximo ciclo iniciado.");
    }

    /**
     * Remove todos os baús spawados
     */
    private void cleanupChests() {
        for (Location loc : spawnedChests) {
            if (loc != null && loc.getWorld() != null) {
                Block block = loc.getBlock();
                if (block.getType() == Material.CHEST) {
                    // Limpar inventário primeiro
                    if (block.getState() instanceof Chest) {
                        ((Chest) block.getState()).getInventory().clear();
                    }
                    block.setType(Material.AIR);
                }
            }
        }
        spawnedChests.clear();
    }

    // ==================== GERAÇÃO DE ITENS ====================

    /**
     * Inicializa a lista de itens padrão do Feast
     */
    private void initializeDefaultItems() {
        possibleItems.clear();

        // ===== SOPAS (peso alto - item principal do KitPvP) =====
        possibleItems.add(new FeastItem(Material.MUSHROOM_SOUP, null, 3, 8, 30));

        // ===== ARMADURAS =====
        // Diamante (raro)
        possibleItems.add(new FeastItem(Material.DIAMOND_HELMET, "§b§lCapacete de Feast", 1, 1, 5));
        possibleItems.add(new FeastItem(Material.DIAMOND_CHESTPLATE, "§b§lPeitoral de Feast", 1, 1, 3));
        possibleItems.add(new FeastItem(Material.DIAMOND_LEGGINGS, "§b§lCalças de Feast", 1, 1, 4));
        possibleItems.add(new FeastItem(Material.DIAMOND_BOOTS, "§b§lBotas de Feast", 1, 1, 5));

        // Ferro (comum)
        possibleItems.add(new FeastItem(Material.IRON_HELMET, null, 1, 1, 15));
        possibleItems.add(new FeastItem(Material.IRON_CHESTPLATE, null, 1, 1, 12));
        possibleItems.add(new FeastItem(Material.IRON_LEGGINGS, null, 1, 1, 13));
        possibleItems.add(new FeastItem(Material.IRON_BOOTS, null, 1, 1, 15));

        // ===== ESPADAS =====
        possibleItems.add(new FeastItem(Material.DIAMOND_SWORD, "§b§lEspada de Feast", 1, 1, 8)
                .addEnchantment(Enchantment.DAMAGE_ALL, 1));
        possibleItems.add(new FeastItem(Material.IRON_SWORD, null, 1, 1, 15));

        // ===== CONSUMÍVEIS =====
        possibleItems.add(new FeastItem(Material.GOLDEN_APPLE, null, 1, 3, 10));
        possibleItems.add(new FeastItem(Material.ENDER_PEARL, null, 2, 5, 12));
        possibleItems.add(new FeastItem(Material.POTION, null, 1, 2, 8)); // Poções variadas

        // ===== MATERIAIS =====
        possibleItems.add(new FeastItem(Material.DIAMOND, null, 1, 3, 6));
        possibleItems.add(new FeastItem(Material.IRON_INGOT, null, 3, 8, 12));
        possibleItems.add(new FeastItem(Material.GOLD_INGOT, null, 2, 5, 10));

        // ===== BLOCOS ÚTEIS =====
        possibleItems.add(new FeastItem(Material.COBBLESTONE, null, 16, 32, 8));
        possibleItems.add(new FeastItem(Material.WOOD, null, 8, 16, 6));

        // ===== ARCOS E FLECHAS =====
        possibleItems.add(new FeastItem(Material.BOW, null, 1, 1, 10)
                .addEnchantment(Enchantment.ARROW_DAMAGE, 1));
        possibleItems.add(new FeastItem(Material.ARROW, null, 8, 32, 12));

        // ===== ITENS ESPECIAIS (raros) =====
        possibleItems.add(new FeastItem(Material.GOLDEN_APPLE, "§6§lGolden Apple Encantada", 1, 1, 2));
        possibleItems.add(new FeastItem(Material.DIAMOND_SWORD, "§d§lLâmina Amaldiçoada", 1, 1, 1)
                .addEnchantment(Enchantment.DAMAGE_ALL, 3)
                .addEnchantment(Enchantment.FIRE_ASPECT, 1));
    }

    /**
     * Preenche um baú com itens aleatórios
     */
    private void fillChest(Inventory inventory) {
        inventory.clear();

        int itemCount = randomBetween(MIN_ITEMS_PER_CHEST, MAX_ITEMS_PER_CHEST);
        int totalWeight = possibleItems.stream().mapToInt(FeastItem::getWeight).sum();

        Set<Integer> usedSlots = new HashSet<>();

        for (int i = 0; i < itemCount; i++) {
            // Selecionar item baseado em peso
            FeastItem selectedItem = selectWeightedItem(totalWeight);
            if (selectedItem == null)
                continue;

            // Gerar o item
            ItemStack item = selectedItem.generate();

            // Encontrar slot aleatório
            int slot;
            int attempts = 0;
            do {
                slot = randomBetween(0, 26); // 27 slots em um baú
                attempts++;
            } while (usedSlots.contains(slot) && attempts < 30);

            if (!usedSlots.contains(slot)) {
                inventory.setItem(slot, item);
                usedSlots.add(slot);
            }
        }
    }

    /**
     * Seleciona um item baseado em peso
     */
    private FeastItem selectWeightedItem(int totalWeight) {
        int random = randomBetween(1, totalWeight);
        int cumulative = 0;

        for (FeastItem item : possibleItems) {
            cumulative += item.getWeight();
            if (random <= cumulative) {
                return item;
            }
        }

        return possibleItems.isEmpty() ? null : possibleItems.get(0);
    }

    // ==================== BROADCASTS ====================

    /**
     * Envia broadcast global
     */
    private void broadcast(String key, String... replacements) {
        String message = ChatStorage.getMessage(key, replacements);
        Bukkit.broadcastMessage(message);
    }

    // ==================== UTILITÁRIOS ====================

    /**
     * Formata tempo em segundos para string legível
     */
    private String formatTime(int seconds) {
        if (seconds >= 60) {
            int minutes = seconds / 60;
            int secs = seconds % 60;
            if (secs == 0) {
                return minutes + " minuto" + (minutes > 1 ? "s" : "");
            }
            return minutes + "m " + secs + "s";
        }
        return seconds + " segundo" + (seconds != 1 ? "s" : "");
    }

    /**
     * Gera número aleatório entre min e max (inclusivo)
     */
    private static int randomBetween(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    // ==================== SHUTDOWN ====================

    /**
     * Método chamado ao desligar o plugin
     */
    public void shutdown() {
        stopFeast(true);
        saveConfiguration();
        plugin.getLogger().info("FeastManager desligado.");
    }

    // ==================== INFORMAÇÕES ====================

    /**
     * Obtém uma lista formatada das localizações
     */
    public List<String> getFormattedLocations() {
        List<String> formatted = new ArrayList<>();
        for (FeastLocation loc : chestLocations) {
            formatted.add(String.format("§e%s §7- §f%s §7(§e%d§7, §e%d§7, §e%d§7)",
                    loc.getId(), loc.getWorldName(), loc.getX(), loc.getY(), loc.getZ()));
        }
        return formatted;
    }

    /**
     * Obtém a localização de feast mais próxima
     */
    public FeastLocation getNearestLocation(Location location) {
        if (chestLocations.isEmpty())
            return null;

        FeastLocation nearest = null;
        double nearestDist = Double.MAX_VALUE;

        for (FeastLocation feastLoc : chestLocations) {
            double dist = feastLoc.distanceSquared(location);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = feastLoc;
            }
        }

        return nearest;
    }
}
