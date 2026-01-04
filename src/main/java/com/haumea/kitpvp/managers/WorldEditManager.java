package com.haumea.kitpvp.managers;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.utils.ChatStorage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Gerenciador Central de WorldEdit Assíncrono do HaumeaMC.
 * 
 * Responsável por:
 * - Executar operações de bloco de forma assíncrona para evitar lag
 * - Enfileirar operações com sistema de prioridades
 * - Fornecer feedback de progresso em tempo real
 * - Suportar undo/redo com histórico por jogador
 * - Pausar automaticamente em caso de baixo TPS
 * - Limitar operações para segurança do servidor
 * 
 * Operações suportadas:
 * - Fill: Preencher área com um tipo de bloco
 * - Replace: Substituir um tipo de bloco por outro
 * - Hollow: Criar estrutura oca (apenas bordas)
 * - Walls: Criar apenas paredes (sem teto/chão)
 * - Clear: Remover blocos de uma área (substituir por ar)
 * - Copy/Paste: Copiar e colar estruturas
 * - Undo/Redo: Desfazer/refazer operações
 * 
 * @author HaumeaMC
 */
public class WorldEditManager {

    private final HaumeaMC plugin;

    // ==================== CONFIGURAÇÕES ====================

    /** Blocos processados por tick */
    private int blocksPerTick = 1000;

    /** TPS mínimo antes de pausar operações */
    private double minTpsThreshold = 15.0;

    /** Limite máximo de blocos por operação */
    private int maxBlocksPerOperation = 1_000_000;

    /** Limite de operações no histórico por jogador */
    private int maxHistoryPerPlayer = 10;

    /** Rate limit: operações por minuto por jogador */
    private int operationsPerMinute = 5;

    /** Se deve pausar em TPS baixo */
    private boolean autoPauseOnLowTps = true;

    // ==================== FILAS DE OPERAÇÕES ====================

    /** Fila de alta prioridade (admin, sistema) */
    private final Queue<WorldEditOperation> highPriorityQueue = new ConcurrentLinkedQueue<>();

    /** Fila normal (jogadores) */
    private final Queue<WorldEditOperation> normalQueue = new ConcurrentLinkedQueue<>();

    /** Operação atualmente em processamento */
    private volatile WorldEditOperation currentOperation = null;

    /** Task de processamento */
    private BukkitTask processingTask = null;

    /** Se o processamento está pausado */
    private volatile boolean paused = false;

    // ==================== HISTÓRICO ====================

    /** Histórico de operações por jogador (UUID -> Stack de estados anteriores) */
    private final Map<UUID, Deque<HistoryEntry>> undoHistory = new ConcurrentHashMap<>();

    /** Histórico de operações desfeitas para redo */
    private final Map<UUID, Deque<HistoryEntry>> redoHistory = new ConcurrentHashMap<>();

    // ==================== SELEÇÕES ====================

    /** Primeira posição de seleção por jogador */
    private final Map<UUID, Location> pos1 = new ConcurrentHashMap<>();

    /** Segunda posição de seleção por jogador */
    private final Map<UUID, Location> pos2 = new ConcurrentHashMap<>();

    // ==================== RATE LIMITING ====================

    /** Timestamp das últimas operações por jogador */
    private final Map<UUID, List<Long>> operationTimestamps = new ConcurrentHashMap<>();

    // ==================== CLIPBOARD ====================

    /** Clipboard por jogador (estrutura copiada) */
    private final Map<UUID, BlockClipboard> clipboards = new ConcurrentHashMap<>();

    // ==================== ESTATÍSTICAS ====================

    /** Contador de operações totais */
    private final AtomicLong totalOperations = new AtomicLong(0);

    /** Contador de blocos modificados totais */
    private final AtomicLong totalBlocksModified = new AtomicLong(0);

    // ==================== TPS TRACKING ====================

    /** Última medição de TPS */
    private volatile double currentTps = 20.0;

    /** Task de monitoramento de TPS */
    private BukkitTask tpsTask = null;

    // ==================== CONSTRUTOR ====================

    public WorldEditManager(HaumeaMC plugin) {
        this.plugin = plugin;

        // Iniciar task de processamento
        startProcessingTask();

        // Iniciar monitoramento de TPS
        startTpsMonitor();

        plugin.getLogger().info("WorldEditManager inicializado - Operações assíncronas ativas");
    }

    // ==================== SELEÇÃO DE ÁREA ====================

    /**
     * Define a primeira posição de seleção
     */
    public void setPos1(Player player, Location location) {
        pos1.put(player.getUniqueId(), location.clone());
        ChatStorage.sendCustom(player, "§aPos1 definida: §f" + formatLocation(location));
    }

    /**
     * Define a segunda posição de seleção
     */
    public void setPos2(Player player, Location location) {
        pos2.put(player.getUniqueId(), location.clone());
        ChatStorage.sendCustom(player, "§aPos2 definida: §f" + formatLocation(location));

        // Mostrar volume da seleção
        Location p1 = pos1.get(player.getUniqueId());
        if (p1 != null) {
            long volume = calculateVolume(p1, location);
            ChatStorage.sendCustom(player, "§eVolume da seleção: §f" + formatNumber(volume) + " blocos");
        }
    }

    /**
     * Obtém a primeira posição de seleção
     */
    public Location getPos1(Player player) {
        return pos1.get(player.getUniqueId());
    }

    /**
     * Obtém a segunda posição de seleção
     */
    public Location getPos2(Player player) {
        return pos2.get(player.getUniqueId());
    }

    /**
     * Limpa a seleção de um jogador
     */
    public void clearSelection(Player player) {
        pos1.remove(player.getUniqueId());
        pos2.remove(player.getUniqueId());
    }

    /**
     * Verifica se o jogador tem uma seleção válida
     */
    public boolean hasValidSelection(Player player) {
        Location p1 = pos1.get(player.getUniqueId());
        Location p2 = pos2.get(player.getUniqueId());
        return p1 != null && p2 != null && p1.getWorld().equals(p2.getWorld());
    }

    // ==================== OPERAÇÕES ====================

    /**
     * Preenche uma área com um tipo de bloco
     *
     * @param sender   Quem iniciou a operação
     * @param p1       Primeira posição
     * @param p2       Segunda posição
     * @param material Material a preencher
     * @param data     Data value do bloco (0 para padrão)
     * @param priority Se é alta prioridade
     * @param callback Callback ao finalizar
     * @return ID da operação ou -1 se falhar
     */
    public long fill(CommandSender sender, Location p1, Location p2, Material material,
            byte data, boolean priority, Consumer<OperationResult> callback) {
        // Validações
        if (!validateOperation(sender, p1, p2)) {
            return -1;
        }

        // Criar lista de blocos a modificar
        List<BlockChange> changes = new ArrayList<>();
        World world = p1.getWorld();

        int minX = Math.min(p1.getBlockX(), p2.getBlockX());
        int minY = Math.min(p1.getBlockY(), p2.getBlockY());
        int minZ = Math.min(p1.getBlockZ(), p2.getBlockZ());
        int maxX = Math.max(p1.getBlockX(), p2.getBlockX());
        int maxY = Math.max(p1.getBlockY(), p2.getBlockY());
        int maxZ = Math.max(p1.getBlockZ(), p2.getBlockZ());

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    changes.add(new BlockChange(
                            new Location(world, x, y, z),
                            block.getType(), block.getData(),
                            material, data));
                }
            }
        }

        return queueOperation(sender, "Fill", changes, priority, callback);
    }

    /**
     * Preenche a seleção atual do jogador
     */
    public long fill(Player player, Material material, byte data, Consumer<OperationResult> callback) {
        if (!hasValidSelection(player)) {
            ChatStorage.sendCustom(player, "§cVocê precisa definir pos1 e pos2 primeiro!");
            return -1;
        }
        return fill(player, pos1.get(player.getUniqueId()), pos2.get(player.getUniqueId()),
                material, data, false, callback);
    }

    /**
     * Substitui um tipo de bloco por outro
     */
    public long replace(CommandSender sender, Location p1, Location p2,
            Material from, Material to, byte toData,
            boolean priority, Consumer<OperationResult> callback) {
        if (!validateOperation(sender, p1, p2)) {
            return -1;
        }

        List<BlockChange> changes = new ArrayList<>();
        World world = p1.getWorld();

        int minX = Math.min(p1.getBlockX(), p2.getBlockX());
        int minY = Math.min(p1.getBlockY(), p2.getBlockY());
        int minZ = Math.min(p1.getBlockZ(), p2.getBlockZ());
        int maxX = Math.max(p1.getBlockX(), p2.getBlockX());
        int maxY = Math.max(p1.getBlockY(), p2.getBlockY());
        int maxZ = Math.max(p1.getBlockZ(), p2.getBlockZ());

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (block.getType() == from) {
                        changes.add(new BlockChange(
                                new Location(world, x, y, z),
                                block.getType(), block.getData(),
                                to, toData));
                    }
                }
            }
        }

        if (changes.isEmpty()) {
            if (sender instanceof Player) {
                ChatStorage.sendCustom((Player) sender, "§cNenhum bloco " + from.name() + " encontrado na área!");
            }
            return -1;
        }

        return queueOperation(sender, "Replace", changes, priority, callback);
    }

    /**
     * Substitui na seleção atual
     */
    public long replace(Player player, Material from, Material to, byte toData,
            Consumer<OperationResult> callback) {
        if (!hasValidSelection(player)) {
            ChatStorage.sendCustom(player, "§cVocê precisa definir pos1 e pos2 primeiro!");
            return -1;
        }
        return replace(player, pos1.get(player.getUniqueId()), pos2.get(player.getUniqueId()),
                from, to, toData, false, callback);
    }

    /**
     * Cria uma estrutura oca (hollow)
     */
    public long hollow(CommandSender sender, Location p1, Location p2, Material material, byte data,
            boolean priority, Consumer<OperationResult> callback) {
        if (!validateOperation(sender, p1, p2)) {
            return -1;
        }

        List<BlockChange> changes = new ArrayList<>();
        World world = p1.getWorld();

        int minX = Math.min(p1.getBlockX(), p2.getBlockX());
        int minY = Math.min(p1.getBlockY(), p2.getBlockY());
        int minZ = Math.min(p1.getBlockZ(), p2.getBlockZ());
        int maxX = Math.max(p1.getBlockX(), p2.getBlockX());
        int maxY = Math.max(p1.getBlockY(), p2.getBlockY());
        int maxZ = Math.max(p1.getBlockZ(), p2.getBlockZ());

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    // Apenas bordas - pelo menos uma coordenada é mín ou máx
                    if (x == minX || x == maxX ||
                            y == minY || y == maxY ||
                            z == minZ || z == maxZ) {

                        Block block = world.getBlockAt(x, y, z);
                        changes.add(new BlockChange(
                                new Location(world, x, y, z),
                                block.getType(), block.getData(),
                                material, data));
                    }
                }
            }
        }

        return queueOperation(sender, "Hollow", changes, priority, callback);
    }

    /**
     * Cria apenas paredes (walls) - sem teto e chão
     */
    public long walls(CommandSender sender, Location p1, Location p2, Material material, byte data,
            boolean priority, Consumer<OperationResult> callback) {
        if (!validateOperation(sender, p1, p2)) {
            return -1;
        }

        List<BlockChange> changes = new ArrayList<>();
        World world = p1.getWorld();

        int minX = Math.min(p1.getBlockX(), p2.getBlockX());
        int minY = Math.min(p1.getBlockY(), p2.getBlockY());
        int minZ = Math.min(p1.getBlockZ(), p2.getBlockZ());
        int maxX = Math.max(p1.getBlockX(), p2.getBlockX());
        int maxY = Math.max(p1.getBlockY(), p2.getBlockY());
        int maxZ = Math.max(p1.getBlockZ(), p2.getBlockZ());

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    // Apenas paredes laterais - X ou Z é mín/máx, Y pode ser qualquer
                    if ((x == minX || x == maxX || z == minZ || z == maxZ) &&
                            y > minY && y < maxY) {

                        Block block = world.getBlockAt(x, y, z);
                        changes.add(new BlockChange(
                                new Location(world, x, y, z),
                                block.getType(), block.getData(),
                                material, data));
                    }
                }
            }
        }

        return queueOperation(sender, "Walls", changes, priority, callback);
    }

    /**
     * Limpa uma área (substitui por ar)
     */
    public long clear(CommandSender sender, Location p1, Location p2,
            boolean priority, Consumer<OperationResult> callback) {
        return fill(sender, p1, p2, Material.AIR, (byte) 0, priority, callback);
    }

    /**
     * Limpa a seleção atual
     */
    public long clear(Player player, Consumer<OperationResult> callback) {
        if (!hasValidSelection(player)) {
            ChatStorage.sendCustom(player, "§cVocê precisa definir pos1 e pos2 primeiro!");
            return -1;
        }
        return clear(player, pos1.get(player.getUniqueId()), pos2.get(player.getUniqueId()), false, callback);
    }

    /**
     * Cria uma esfera
     */
    public long sphere(CommandSender sender, Location center, int radius, Material material, byte data,
            boolean hollow, boolean priority, Consumer<OperationResult> callback) {
        if (radius > 100) {
            if (sender instanceof Player) {
                ChatStorage.sendCustom((Player) sender, "§cRaio máximo permitido: 100 blocos!");
            }
            return -1;
        }

        long volume = (long) (4.0 / 3.0 * Math.PI * radius * radius * radius);
        if (volume > maxBlocksPerOperation) {
            if (sender instanceof Player) {
                ChatStorage.sendCustom((Player) sender, "§cOperação muito grande! Máximo: " +
                        formatNumber(maxBlocksPerOperation) + " blocos");
            }
            return -1;
        }

        List<BlockChange> changes = new ArrayList<>();
        World world = center.getWorld();
        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();
        int radiusSquared = radius * radius;
        int innerRadiusSquared = (radius - 1) * (radius - 1);

        for (int x = cx - radius; x <= cx + radius; x++) {
            for (int y = cy - radius; y <= cy + radius; y++) {
                for (int z = cz - radius; z <= cz + radius; z++) {
                    int dx = x - cx;
                    int dy = y - cy;
                    int dz = z - cz;
                    int distSquared = dx * dx + dy * dy + dz * dz;

                    if (distSquared <= radiusSquared) {
                        // Se hollow, apenas borda
                        if (hollow && distSquared < innerRadiusSquared) {
                            continue;
                        }

                        Block block = world.getBlockAt(x, y, z);
                        changes.add(new BlockChange(
                                new Location(world, x, y, z),
                                block.getType(), block.getData(),
                                material, data));
                    }
                }
            }
        }

        return queueOperation(sender, "Sphere", changes, priority, callback);
    }

    /**
     * Cria um cilindro
     */
    public long cylinder(CommandSender sender, Location base, int radius, int height, Material material, byte data,
            boolean hollow, boolean priority, Consumer<OperationResult> callback) {
        if (radius > 100 || height > 256) {
            if (sender instanceof Player) {
                ChatStorage.sendCustom((Player) sender, "§cRaio máximo: 100, Altura máxima: 256!");
            }
            return -1;
        }

        long volume = (long) (Math.PI * radius * radius * height);
        if (volume > maxBlocksPerOperation) {
            if (sender instanceof Player) {
                ChatStorage.sendCustom((Player) sender, "§cOperação muito grande! Máximo: " +
                        formatNumber(maxBlocksPerOperation) + " blocos");
            }
            return -1;
        }

        List<BlockChange> changes = new ArrayList<>();
        World world = base.getWorld();
        int cx = base.getBlockX();
        int cz = base.getBlockZ();
        int minY = base.getBlockY();
        int maxY = minY + height - 1;
        int radiusSquared = radius * radius;
        int innerRadiusSquared = (radius - 1) * (radius - 1);

        for (int x = cx - radius; x <= cx + radius; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = cz - radius; z <= cz + radius; z++) {
                    int dx = x - cx;
                    int dz = z - cz;
                    int distSquared = dx * dx + dz * dz;

                    if (distSquared <= radiusSquared) {
                        if (hollow && distSquared < innerRadiusSquared && y > minY && y < maxY) {
                            continue;
                        }

                        Block block = world.getBlockAt(x, y, z);
                        changes.add(new BlockChange(
                                new Location(world, x, y, z),
                                block.getType(), block.getData(),
                                material, data));
                    }
                }
            }
        }

        return queueOperation(sender, "Cylinder", changes, priority, callback);
    }

    // ==================== COPY / PASTE ====================

    /**
     * Copia uma área para o clipboard do jogador
     */
    public boolean copy(Player player) {
        if (!hasValidSelection(player)) {
            ChatStorage.sendCustom(player, "§cVocê precisa definir pos1 e pos2 primeiro!");
            return false;
        }

        Location p1 = this.pos1.get(player.getUniqueId());
        Location p2 = this.pos2.get(player.getUniqueId());

        long volume = calculateVolume(p1, p2);
        if (volume > maxBlocksPerOperation) {
            ChatStorage.sendCustom(player, "§cÁrea muito grande para copiar! Máximo: " +
                    formatNumber(maxBlocksPerOperation) + " blocos");
            return false;
        }

        World world = p1.getWorld();
        List<BlockData> blocks = new ArrayList<>();
        Location origin = player.getLocation().clone();

        int minX = Math.min(p1.getBlockX(), p2.getBlockX());
        int minY = Math.min(p1.getBlockY(), p2.getBlockY());
        int minZ = Math.min(p1.getBlockZ(), p2.getBlockZ());
        int maxX = Math.max(p1.getBlockX(), p2.getBlockX());
        int maxY = Math.max(p1.getBlockY(), p2.getBlockY());
        int maxZ = Math.max(p1.getBlockZ(), p2.getBlockZ());

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (block.getType() != Material.AIR) {
                        // Posição relativa ao jogador
                        int relX = x - origin.getBlockX();
                        int relY = y - origin.getBlockY();
                        int relZ = z - origin.getBlockZ();
                        blocks.add(new BlockData(relX, relY, relZ, block.getType(), block.getData()));
                    }
                }
            }
        }

        clipboards.put(player.getUniqueId(), new BlockClipboard(blocks, origin));
        ChatStorage.sendCustom(player, "§aCopiados §f" + formatNumber(blocks.size()) + " §ablocos para o clipboard!");
        return true;
    }

    /**
     * Cola o clipboard do jogador
     */
    public long paste(Player player, boolean ignoreAir, Consumer<OperationResult> callback) {
        BlockClipboard clipboard = clipboards.get(player.getUniqueId());
        if (clipboard == null || clipboard.blocks.isEmpty()) {
            ChatStorage.sendCustom(player, "§cClipboard vazio! Use /copy primeiro.");
            return -1;
        }

        Location targetOrigin = player.getLocation();
        World world = targetOrigin.getWorld();
        List<BlockChange> changes = new ArrayList<>();

        for (BlockData data : clipboard.blocks) {
            int x = targetOrigin.getBlockX() + data.relX;
            int y = targetOrigin.getBlockY() + data.relY;
            int z = targetOrigin.getBlockZ() + data.relZ;

            if (y < 0 || y > 255)
                continue;

            if (ignoreAir && data.material == Material.AIR)
                continue;

            Block block = world.getBlockAt(x, y, z);
            changes.add(new BlockChange(
                    new Location(world, x, y, z),
                    block.getType(), block.getData(),
                    data.material, data.data));
        }

        return queueOperation(player, "Paste", changes, false, callback);
    }

    // ==================== UNDO / REDO ====================

    /**
     * Desfaz a última operação do jogador
     */
    public long undo(Player player, Consumer<OperationResult> callback) {
        Deque<HistoryEntry> history = undoHistory.get(player.getUniqueId());
        if (history == null || history.isEmpty()) {
            ChatStorage.sendCustom(player, "§cNenhuma operação para desfazer!");
            return -1;
        }

        HistoryEntry entry = history.pop();
        List<BlockChange> changes = new ArrayList<>();

        // Salvar estado atual para redo
        List<BlockChange> redoChanges = new ArrayList<>();
        for (BlockChange change : entry.changes) {
            Block block = change.location.getBlock();
            redoChanges.add(new BlockChange(
                    change.location.clone(),
                    block.getType(), block.getData(),
                    change.oldMaterial, change.oldData));

            // Operação inversa - voltar para o estado anterior
            changes.add(new BlockChange(
                    change.location.clone(),
                    block.getType(), block.getData(),
                    change.oldMaterial, change.oldData));
        }

        // Adicionar ao redo
        Deque<HistoryEntry> redo = redoHistory.computeIfAbsent(player.getUniqueId(), k -> new LinkedList<>());
        redo.push(new HistoryEntry(entry.operationType, redoChanges));

        return queueOperation(player, "Undo", changes, true, callback, false); // Não salvar undo do undo
    }

    /**
     * Refaz a última operação desfeita
     */
    public long redo(Player player, Consumer<OperationResult> callback) {
        Deque<HistoryEntry> redo = redoHistory.get(player.getUniqueId());
        if (redo == null || redo.isEmpty()) {
            ChatStorage.sendCustom(player, "§cNenhuma operação para refazer!");
            return -1;
        }

        HistoryEntry entry = redo.pop();
        List<BlockChange> changes = new ArrayList<>();

        for (BlockChange change : entry.changes) {
            // Operação inversa - aplicar o que foi salvo no redo
            changes.add(new BlockChange(
                    change.location.clone(),
                    change.oldMaterial, change.oldData,
                    change.newMaterial, change.newData));
        }

        return queueOperation(player, "Redo", changes, true, callback, true); // Salvar para undo
    }

    // ==================== FILA DE OPERAÇÕES ====================

    private long queueOperation(CommandSender sender, String type, List<BlockChange> changes,
            boolean priority, Consumer<OperationResult> callback) {
        return queueOperation(sender, type, changes, priority, callback, true);
    }

    private long queueOperation(CommandSender sender, String type, List<BlockChange> changes,
            boolean priority, Consumer<OperationResult> callback, boolean saveHistory) {
        if (changes.isEmpty()) {
            if (sender instanceof Player) {
                ChatStorage.sendCustom((Player) sender, "§cNenhum bloco a modificar!");
            }
            return -1;
        }

        // Rate limiting para jogadores
        if (sender instanceof Player && !priority) {
            if (!checkRateLimit((Player) sender)) {
                ChatStorage.sendCustom((Player) sender,
                        "§cVocê está fazendo operações muito rápido! Aguarde um momento.");
                return -1;
            }
        }

        UUID senderUuid = sender instanceof Player ? ((Player) sender).getUniqueId() : null;
        long operationId = System.currentTimeMillis();

        WorldEditOperation operation = new WorldEditOperation(
                operationId, senderUuid, type, changes, callback, saveHistory);

        // Adicionar à fila apropriada
        if (priority) {
            highPriorityQueue.offer(operation);
        } else {
            normalQueue.offer(operation);
        }

        // Feedback
        if (sender instanceof Player) {
            ChatStorage.sendCustom((Player) sender, "§eOperação §f" + type + " §eenfileirada! §7(" +
                    formatNumber(changes.size()) + " blocos)");
        } else {
            plugin.getLogger().info("[WorldEdit] Operação " + type + " enfileirada: " +
                    formatNumber(changes.size()) + " blocos");
        }

        // Registrar timestamp para rate limiting
        if (senderUuid != null) {
            operationTimestamps.computeIfAbsent(senderUuid, k -> new ArrayList<>())
                    .add(System.currentTimeMillis());
        }

        totalOperations.incrementAndGet();
        return operationId;
    }

    /**
     * Valida se a operação pode ser executada
     */
    private boolean validateOperation(CommandSender sender, Location p1, Location p2) {
        if (p1 == null || p2 == null) {
            if (sender instanceof Player) {
                ChatStorage.sendCustom((Player) sender, "§cPosições inválidas!");
            }
            return false;
        }

        if (!p1.getWorld().equals(p2.getWorld())) {
            if (sender instanceof Player) {
                ChatStorage.sendCustom((Player) sender, "§cAs posições devem estar no mesmo mundo!");
            }
            return false;
        }

        long volume = calculateVolume(p1, p2);
        if (volume > maxBlocksPerOperation) {
            if (sender instanceof Player) {
                ChatStorage.sendCustom((Player) sender, "§cOperação muito grande! Máximo: " +
                        formatNumber(maxBlocksPerOperation) + " blocos. Selecionado: " + formatNumber(volume));
            }
            return false;
        }

        return true;
    }

    /**
     * Verifica rate limiting para jogador
     */
    private boolean checkRateLimit(Player player) {
        List<Long> timestamps = operationTimestamps.get(player.getUniqueId());
        if (timestamps == null) {
            return true;
        }

        long now = System.currentTimeMillis();
        long oneMinuteAgo = now - 60000;

        // Remover timestamps antigos
        timestamps.removeIf(t -> t < oneMinuteAgo);

        return timestamps.size() < operationsPerMinute;
    }

    // ==================== PROCESSAMENTO ====================

    private void startProcessingTask() {
        processingTask = new BukkitRunnable() {
            @Override
            public void run() {
                processQueue();
            }
        }.runTaskTimer(plugin, 1L, 1L); // A cada tick
    }

    private void processQueue() {
        // Verificar se deve pausar
        if (paused || (autoPauseOnLowTps && currentTps < minTpsThreshold)) {
            return;
        }

        // Continuar operação atual ou pegar nova
        if (currentOperation == null) {
            // Priorizar fila de alta prioridade
            currentOperation = highPriorityQueue.poll();
            if (currentOperation == null) {
                currentOperation = normalQueue.poll();
            }

            if (currentOperation == null) {
                return; // Nenhuma operação pendente
            }
        }

        // Processar blocos
        int processed = 0;
        List<BlockChange> changes = currentOperation.changes;

        while (processed < blocksPerTick && currentOperation.currentIndex < changes.size()) {
            BlockChange change = changes.get(currentOperation.currentIndex);

            // Aplicar mudança de bloco
            Block block = change.location.getBlock();
            block.setType(change.newMaterial);
            block.setData(change.newData);

            currentOperation.currentIndex++;
            processed++;
        }

        currentOperation.blocksProcessed += processed;
        totalBlocksModified.addAndGet(processed);

        // Feedback de progresso
        if (currentOperation.currentIndex % 10000 == 0) {
            sendProgressUpdate(currentOperation);
        }

        // Verificar se terminou
        if (currentOperation.currentIndex >= changes.size()) {
            finishOperation(currentOperation);
            currentOperation = null;
        }
    }

    private void sendProgressUpdate(WorldEditOperation operation) {
        if (operation.senderUuid == null) {
            return;
        }

        Player player = Bukkit.getPlayer(operation.senderUuid);
        if (player == null || !player.isOnline()) {
            return;
        }

        int total = operation.changes.size();
        int done = operation.currentIndex;
        int percent = (int) ((done * 100.0) / total);

        ChatStorage.sendRaw(player, "§8[§eWorldEdit§8] §7Progresso: §f" + percent + "% §7(" +
                formatNumber(done) + "/" + formatNumber(total) + ")");
    }

    private void finishOperation(WorldEditOperation operation) {
        long elapsed = System.currentTimeMillis() - operation.operationId;

        // Salvar no histórico
        if (operation.saveHistory && operation.senderUuid != null) {
            saveToHistory(operation);
        }

        // Callback
        if (operation.callback != null) {
            OperationResult result = new OperationResult(
                    operation.operationId,
                    operation.type,
                    operation.changes.size(),
                    elapsed,
                    true,
                    null);

            // Executar callback no main thread
            Bukkit.getScheduler().runTask(plugin, () -> operation.callback.accept(result));
        }

        // Feedback
        if (operation.senderUuid != null) {
            Player player = Bukkit.getPlayer(operation.senderUuid);
            if (player != null && player.isOnline()) {
                ChatStorage.sendCustom(player, "§aOperação §f" + operation.type + " §aconcluída! §7(" +
                        formatNumber(operation.changes.size()) + " blocos em " + formatTime(elapsed) + ")");
            }
        } else {
            plugin.getLogger().info("[WorldEdit] Operação " + operation.type + " concluída: " +
                    formatNumber(operation.changes.size()) + " blocos em " + formatTime(elapsed));
        }
    }

    private void saveToHistory(WorldEditOperation operation) {
        Deque<HistoryEntry> history = undoHistory.computeIfAbsent(
                operation.senderUuid, k -> new LinkedList<>());

        // Limitar tamanho do histórico
        while (history.size() >= maxHistoryPerPlayer) {
            history.pollLast();
        }

        history.push(new HistoryEntry(operation.type, operation.changes));

        // Limpar redo quando nova operação é feita
        redoHistory.remove(operation.senderUuid);
    }

    // ==================== TPS MONITOR ====================

    private void startTpsMonitor() {
        final long[] lastTick = { System.currentTimeMillis() };
        final int[] tickCount = { 0 };

        tpsTask = new BukkitRunnable() {
            @Override
            public void run() {
                tickCount[0]++;

                if (tickCount[0] >= 100) { // Medir a cada 100 ticks (5 segundos)
                    long now = System.currentTimeMillis();
                    long elapsed = now - lastTick[0];

                    // TPS = ticks / (elapsed / 1000)
                    currentTps = (tickCount[0] * 1000.0) / elapsed;
                    currentTps = Math.min(20.0, currentTps); // Cap em 20

                    tickCount[0] = 0;
                    lastTick[0] = now;
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    // ==================== CONTROLE ====================

    /**
     * Pausa o processamento de operações
     */
    public void pause() {
        paused = true;
        plugin.getLogger().info("[WorldEdit] Processamento pausado");
    }

    /**
     * Resume o processamento de operações
     */
    public void resume() {
        paused = false;
        plugin.getLogger().info("[WorldEdit] Processamento resumido");
    }

    /**
     * Verifica se está pausado
     */
    public boolean isPaused() {
        return paused;
    }

    /**
     * Cancela uma operação por ID
     */
    public boolean cancelOperation(long operationId) {
        // Verificar operação atual
        if (currentOperation != null && currentOperation.operationId == operationId) {
            currentOperation = null;
            return true;
        }

        // Verificar filas
        return highPriorityQueue.removeIf(op -> op.operationId == operationId) ||
                normalQueue.removeIf(op -> op.operationId == operationId);
    }

    /**
     * Verifica se jogador tem operação em andamento
     */
    public boolean hasOperationInProgress(Player player) {
        UUID uuid = player.getUniqueId();

        if (currentOperation != null && uuid.equals(currentOperation.senderUuid)) {
            return true;
        }

        return highPriorityQueue.stream().anyMatch(op -> uuid.equals(op.senderUuid)) ||
                normalQueue.stream().anyMatch(op -> uuid.equals(op.senderUuid));
    }

    /**
     * Obtém progresso da operação atual de um jogador
     */
    public int getOperationProgress(Player player) {
        if (currentOperation != null && player.getUniqueId().equals(currentOperation.senderUuid)) {
            int total = currentOperation.changes.size();
            int done = currentOperation.currentIndex;
            return total > 0 ? (int) ((done * 100.0) / total) : 0;
        }
        return -1; // Sem operação em andamento
    }

    // ==================== UTILIDADES ====================

    private long calculateVolume(Location p1, Location p2) {
        long dx = Math.abs(p1.getBlockX() - p2.getBlockX()) + 1;
        long dy = Math.abs(p1.getBlockY() - p2.getBlockY()) + 1;
        long dz = Math.abs(p1.getBlockZ() - p2.getBlockZ()) + 1;
        return dx * dy * dz;
    }

    private String formatLocation(Location loc) {
        return loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ();
    }

    private String formatNumber(long number) {
        return String.format("%,d", number).replace(',', '.');
    }

    private String formatTime(long ms) {
        if (ms < 1000) {
            return ms + "ms";
        } else if (ms < 60000) {
            return String.format("%.1fs", ms / 1000.0);
        } else {
            return String.format("%.1fmin", ms / 60000.0);
        }
    }

    public double getCurrentTps() {
        return currentTps;
    }

    public int getQueueSize() {
        return highPriorityQueue.size() + normalQueue.size();
    }

    /**
     * Obtém informações de debug
     */
    public String getDebugInfo() {
        return String.format(
                "WorldEditManager: TPS=%.1f, Queue=%d/%d, Paused=%s, " +
                        "Total=%d ops, %s blocos",
                currentTps, highPriorityQueue.size(), normalQueue.size(),
                paused ? "Sim" : "Não",
                totalOperations.get(),
                formatNumber(totalBlocksModified.get()));
    }

    // ==================== GETTERS/SETTERS ====================

    public int getBlocksPerTick() {
        return blocksPerTick;
    }

    public void setBlocksPerTick(int blocksPerTick) {
        this.blocksPerTick = blocksPerTick;
    }

    public int getMaxBlocksPerOperation() {
        return maxBlocksPerOperation;
    }

    public void setMaxBlocksPerOperation(int max) {
        this.maxBlocksPerOperation = max;
    }

    public double getMinTpsThreshold() {
        return minTpsThreshold;
    }

    public void setMinTpsThreshold(double threshold) {
        this.minTpsThreshold = threshold;
    }

    // ==================== SHUTDOWN ====================

    public void shutdown() {
        if (processingTask != null) {
            processingTask.cancel();
        }
        if (tpsTask != null) {
            tpsTask.cancel();
        }

        // Limpar filas
        highPriorityQueue.clear();
        normalQueue.clear();
        currentOperation = null;

        // Limpar dados
        undoHistory.clear();
        redoHistory.clear();
        clipboards.clear();
        pos1.clear();
        pos2.clear();

        plugin.getLogger().info("WorldEditManager desligado");
    }

    /**
     * Limpa dados de um jogador específico
     */
    public void clearPlayerData(Player player) {
        UUID uuid = player.getUniqueId();
        undoHistory.remove(uuid);
        redoHistory.remove(uuid);
        clipboards.remove(uuid);
        pos1.remove(uuid);
        pos2.remove(uuid);
        operationTimestamps.remove(uuid);
    }

    // ==================== CLASSES INTERNAS ====================

    /**
     * Representa uma mudança de bloco
     */
    public static class BlockChange {
        public final Location location;
        public final Material oldMaterial;
        public final byte oldData;
        public final Material newMaterial;
        public final byte newData;

        public BlockChange(Location location, Material oldMaterial, byte oldData,
                Material newMaterial, byte newData) {
            this.location = location;
            this.oldMaterial = oldMaterial;
            this.oldData = oldData;
            this.newMaterial = newMaterial;
            this.newData = newData;
        }
    }

    /**
     * Representa uma operação WorldEdit
     */
    private static class WorldEditOperation {
        public final long operationId;
        public final UUID senderUuid;
        public final String type;
        public final List<BlockChange> changes;
        public final Consumer<OperationResult> callback;
        public final boolean saveHistory;
        public int currentIndex = 0;
        public int blocksProcessed = 0;

        public WorldEditOperation(long operationId, UUID senderUuid, String type,
                List<BlockChange> changes, Consumer<OperationResult> callback,
                boolean saveHistory) {
            this.operationId = operationId;
            this.senderUuid = senderUuid;
            this.type = type;
            this.changes = changes;
            this.callback = callback;
            this.saveHistory = saveHistory;
        }
    }

    /**
     * Resultado de uma operação
     */
    public static class OperationResult {
        public final long operationId;
        public final String type;
        public final int blocksModified;
        public final long elapsedMs;
        public final boolean success;
        public final String error;

        public OperationResult(long operationId, String type, int blocksModified,
                long elapsedMs, boolean success, String error) {
            this.operationId = operationId;
            this.type = type;
            this.blocksModified = blocksModified;
            this.elapsedMs = elapsedMs;
            this.success = success;
            this.error = error;
        }
    }

    /**
     * Entrada no histórico
     */
    private static class HistoryEntry {
        public final String operationType;
        public final List<BlockChange> changes;

        public HistoryEntry(String operationType, List<BlockChange> changes) {
            this.operationType = operationType;
            this.changes = changes;
        }
    }

    /**
     * Dados de bloco para clipboard
     */
    private static class BlockData {
        public final int relX, relY, relZ;
        public final Material material;
        public final byte data;

        public BlockData(int relX, int relY, int relZ, Material material, byte data) {
            this.relX = relX;
            this.relY = relY;
            this.relZ = relZ;
            this.material = material;
            this.data = data;
        }
    }

    /**
     * Clipboard de blocos
     */
    private static class BlockClipboard {
        public final List<BlockData> blocks;
        public final Location origin;

        public BlockClipboard(List<BlockData> blocks, Location origin) {
            this.blocks = blocks;
            this.origin = origin;
        }
    }
}
