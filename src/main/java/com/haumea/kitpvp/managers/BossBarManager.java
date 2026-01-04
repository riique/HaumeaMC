package com.haumea.kitpvp.managers;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.utils.ChatStorage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gerenciador de Boss Bar para Minecraft 1.8
 * Usa o hack do Wither para exibir uma barra de vida no topo da tela.
 * 
 * Como funciona:
 * 1. Spawna um Wither invisível extremamente distante do jogador
 * 2. Envia pacotes para atualizar o nome e vida do Wither
 * 3. O cliente exibe a barra de vida do "boss" no topo da tela
 * 
 * @author HaumeaMC
 */
public class BossBarManager {

    private final HaumeaMC plugin;
    private final Map<UUID, Object> witherEntities;
    private final Map<UUID, Integer> witherIds;
    private BukkitTask updateTask;

    // Intervalo de atualização (ticks)
    private static final long UPDATE_INTERVAL = 5L; // 0.25 segundos

    // Distância do Wither abaixo do jogador
    private static final double WITHER_DISTANCE = 200.0;

    // Cache de NMS
    private Class<?> entityWitherClass;
    private Class<?> packetPlayOutSpawnEntityLivingClass;
    private Class<?> packetPlayOutEntityDestroyClass;
    private Class<?> packetPlayOutEntityMetadataClass;
    private Class<?> packetPlayOutEntityTeleportClass;
    private Class<?> craftWorldClass;
    private Class<?> craftPlayerClass;
    private String nmsVersion;
    private boolean initialized = false;

    public BossBarManager(HaumeaMC plugin) {
        this.plugin = plugin;
        this.witherEntities = new ConcurrentHashMap<>();
        this.witherIds = new ConcurrentHashMap<>();
        initNMS();
    }

    /**
     * Inicializa as classes NMS via reflexão
     */
    private void initNMS() {
        try {
            // Detectar versão do NMS
            String packageName = Bukkit.getServer().getClass().getPackage().getName();
            nmsVersion = packageName.substring(packageName.lastIndexOf('.') + 1);

            // Carregar classes NMS
            entityWitherClass = Class.forName("net.minecraft.server." + nmsVersion + ".EntityWither");
            packetPlayOutSpawnEntityLivingClass = Class
                    .forName("net.minecraft.server." + nmsVersion + ".PacketPlayOutSpawnEntityLiving");
            packetPlayOutEntityDestroyClass = Class
                    .forName("net.minecraft.server." + nmsVersion + ".PacketPlayOutEntityDestroy");
            packetPlayOutEntityMetadataClass = Class
                    .forName("net.minecraft.server." + nmsVersion + ".PacketPlayOutEntityMetadata");
            packetPlayOutEntityTeleportClass = Class
                    .forName("net.minecraft.server." + nmsVersion + ".PacketPlayOutEntityTeleport");
            craftWorldClass = Class.forName("org.bukkit.craftbukkit." + nmsVersion + ".CraftWorld");
            craftPlayerClass = Class.forName("org.bukkit.craftbukkit." + nmsVersion + ".entity.CraftPlayer");

            initialized = true;
            plugin.getLogger().info("[BossBar] Sistema de Boss Bar inicializado via NMS.");
        } catch (Exception e) {
            plugin.getLogger()
                    .warning("[BossBar] Não foi possível inicializar o sistema de Boss Bar: " + e.getMessage());
            plugin.getLogger().warning("[BossBar] Este sistema requer Spigot 1.8.x com acesso ao NMS.");
            initialized = false;
        }
    }

    /**
     * Inicia o sistema de Boss Bar
     */
    public void start() {
        if (!initialized) {
            return;
        }

        // Task para manter o Wither na posição correta
        updateTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (witherEntities.containsKey(player.getUniqueId())) {
                        updateWitherPosition(player);
                    }
                }
            }
        }.runTaskTimer(plugin, UPDATE_INTERVAL, UPDATE_INTERVAL);
    }

    /**
     * Para o sistema de Boss Bar
     */
    public void stop() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }

        // Remover todos os Withers
        for (Player player : Bukkit.getOnlinePlayers()) {
            removeBossBar(player);
        }

        witherEntities.clear();
        witherIds.clear();
    }

    /**
     * Exibe uma Boss Bar para um jogador
     * 
     * @param player   Jogador
     * @param text     Texto da barra
     * @param progress Progresso (0.0 a 1.0)
     */
    public void showBossBar(Player player, String text, float progress) {
        if (!initialized || player == null || !player.isOnline()) {
            return;
        }

        try {
            UUID uuid = player.getUniqueId();

            // Se já tem um Wither, atualizar; senão, criar
            if (witherEntities.containsKey(uuid)) {
                updateBossBar(player, text, progress);
            } else {
                createBossBar(player, text, progress);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[BossBar] Erro ao mostrar boss bar: " + e.getMessage());
        }
    }

    /**
     * Cria um novo Wither para o jogador
     */
    private void createBossBar(Player player, String text, float progress) {
        try {
            // Obter mundo NMS
            Object craftWorld = craftWorldClass.cast(player.getWorld());
            Method getHandleWorld = craftWorld.getClass().getMethod("getHandle");
            Object nmsWorld = getHandleWorld.invoke(craftWorld);

            // Criar Wither
            Constructor<?> witherConstructor = entityWitherClass.getConstructor(
                    Class.forName("net.minecraft.server." + nmsVersion + ".World"));
            Object wither = witherConstructor.newInstance(nmsWorld);

            // Configurar posição (muito abaixo do jogador para ficar invisível)
            Location loc = getWitherLocation(player);
            Method setLocation = entityWitherClass.getMethod("setLocation",
                    double.class, double.class, double.class, float.class, float.class);
            setLocation.invoke(wither, loc.getX(), loc.getY(), loc.getZ(), 0.0f, 0.0f);

            // Configurar nome customizado
            Method setCustomName = entityWitherClass.getMethod("setCustomName", String.class);
            setCustomName.invoke(wither, ChatStorage.colorize(text));

            // Configurar vida (progress * 300, onde 300 é a vida máxima do Wither)
            Method setHealth = entityWitherClass.getMethod("setHealth", float.class);
            float health = Math.max(0.1f, progress * 300.0f);
            setHealth.invoke(wither, health);

            // Tornar invisível
            Method setInvisible = entityWitherClass.getMethod("setInvisible", boolean.class);
            setInvisible.invoke(wither, true);

            // Silenciar
            try {
                Method setSilent = entityWitherClass.getMethod("c", boolean.class); // setSilent em algumas versões
                setSilent.invoke(wither, true);
            } catch (NoSuchMethodException e) {
                // Ignorar se o método não existir
            }

            // Obter ID da entidade
            Method getId = entityWitherClass.getMethod("getId");
            int entityId = (Integer) getId.invoke(wither);

            // Guardar referências
            witherEntities.put(player.getUniqueId(), wither);
            witherIds.put(player.getUniqueId(), entityId);

            // Enviar pacote de spawn
            Constructor<?> spawnPacketConstructor = packetPlayOutSpawnEntityLivingClass.getConstructor(
                    Class.forName("net.minecraft.server." + nmsVersion + ".EntityLiving"));
            Object spawnPacket = spawnPacketConstructor.newInstance(wither);
            sendPacket(player, spawnPacket);

        } catch (Exception e) {
            plugin.getLogger().warning("[BossBar] Erro ao criar boss bar: " + e.getMessage());
        }
    }

    /**
     * Atualiza a Boss Bar existente
     */
    public void updateBossBar(Player player, String text, float progress) {
        if (!initialized || player == null || !player.isOnline()) {
            return;
        }

        try {
            Object wither = witherEntities.get(player.getUniqueId());
            if (wither == null) {
                createBossBar(player, text, progress);
                return;
            }

            // Atualizar nome
            Method setCustomName = entityWitherClass.getMethod("setCustomName", String.class);
            setCustomName.invoke(wither, ChatStorage.colorize(text));

            // Atualizar vida
            Method setHealth = entityWitherClass.getMethod("setHealth", float.class);
            float health = Math.max(0.1f, progress * 300.0f);
            setHealth.invoke(wither, health);

            // Enviar pacote de metadata
            Integer entityId = witherIds.get(player.getUniqueId());
            if (entityId != null) {
                sendMetadataPacket(player, wither, entityId);
            }

        } catch (Exception e) {
            plugin.getLogger().warning("[BossBar] Erro ao atualizar boss bar: " + e.getMessage());
        }
    }

    /**
     * Atualiza a posição do Wither para seguir o jogador
     */
    private void updateWitherPosition(Player player) {
        try {
            Object wither = witherEntities.get(player.getUniqueId());
            if (wither == null) {
                return;
            }

            Location loc = getWitherLocation(player);

            // Atualizar posição
            Method setLocation = entityWitherClass.getMethod("setLocation",
                    double.class, double.class, double.class, float.class, float.class);
            setLocation.invoke(wither, loc.getX(), loc.getY(), loc.getZ(), 0.0f, 0.0f);

            // Enviar pacote de teleporte
            Integer entityId = witherIds.get(player.getUniqueId());
            if (entityId != null) {
                sendTeleportPacket(player, wither);
            }

        } catch (Exception e) {
            // Ignorar erros de atualização
        }
    }

    /**
     * Remove a Boss Bar de um jogador
     */
    public void removeBossBar(Player player) {
        if (!initialized || player == null) {
            return;
        }

        try {
            UUID uuid = player.getUniqueId();
            Integer entityId = witherIds.remove(uuid);
            witherEntities.remove(uuid);

            if (entityId != null && player.isOnline()) {
                // Enviar pacote de destroy
                Constructor<?> destroyPacketConstructor = packetPlayOutEntityDestroyClass.getConstructor(int[].class);
                Object destroyPacket = destroyPacketConstructor.newInstance(new int[] { entityId });
                sendPacket(player, destroyPacket);
            }

        } catch (Exception e) {
            plugin.getLogger().warning("[BossBar] Erro ao remover boss bar: " + e.getMessage());
        }
    }

    /**
     * Calcula a posição onde o Wither deve ficar
     */
    private Location getWitherLocation(Player player) {
        Location loc = player.getLocation().clone();
        loc.setY(loc.getY() - WITHER_DISTANCE);
        return loc;
    }

    /**
     * Envia um pacote para o jogador
     */
    private void sendPacket(Player player, Object packet) {
        try {
            Object craftPlayer = craftPlayerClass.cast(player);
            Method getHandle = craftPlayer.getClass().getMethod("getHandle");
            Object entityPlayer = getHandle.invoke(craftPlayer);

            Field playerConnectionField = entityPlayer.getClass().getField("playerConnection");
            Object playerConnection = playerConnectionField.get(entityPlayer);

            Method sendPacket = playerConnection.getClass().getMethod("sendPacket",
                    Class.forName("net.minecraft.server." + nmsVersion + ".Packet"));
            sendPacket.invoke(playerConnection, packet);

        } catch (Exception e) {
            plugin.getLogger().warning("[BossBar] Erro ao enviar pacote: " + e.getMessage());
        }
    }

    /**
     * Envia pacote de metadata
     */
    private void sendMetadataPacket(Player player, Object wither, int entityId) {
        try {
            Method getDataWatcher = entityWitherClass.getMethod("getDataWatcher");
            Object dataWatcher = getDataWatcher.invoke(wither);

            // Obter lista de itens do DataWatcher
            Method getAllMethod;
            try {
                getAllMethod = dataWatcher.getClass().getMethod("c"); // 1.8
            } catch (NoSuchMethodException e) {
                getAllMethod = dataWatcher.getClass().getMethod("b"); // Alternativo
            }
            Object watchableObjects = getAllMethod.invoke(dataWatcher);

            // Criar pacote de metadata
            Constructor<?> metadataPacketConstructor = packetPlayOutEntityMetadataClass.getConstructor(
                    int.class,
                    Class.forName("net.minecraft.server." + nmsVersion + ".DataWatcher"),
                    boolean.class);
            Object metadataPacket = metadataPacketConstructor.newInstance(entityId, dataWatcher, true);
            sendPacket(player, metadataPacket);

        } catch (Exception e) {
            // Ignorar erros de metadata (pode variar entre versões)
        }
    }

    /**
     * Envia pacote de teleporte
     */
    private void sendTeleportPacket(Player player, Object wither) {
        try {
            Constructor<?> teleportPacketConstructor = packetPlayOutEntityTeleportClass.getConstructor(
                    Class.forName("net.minecraft.server." + nmsVersion + ".Entity"));
            Object teleportPacket = teleportPacketConstructor.newInstance(wither);
            sendPacket(player, teleportPacket);

        } catch (Exception e) {
            // Ignorar erros de teleporte
        }
    }

    /**
     * Verifica se o sistema está disponível
     */
    public boolean isAvailable() {
        return initialized;
    }

    /**
     * Verifica se um jogador tem Boss Bar ativa
     */
    public boolean hasBossBar(Player player) {
        return player != null && witherEntities.containsKey(player.getUniqueId());
    }
}
