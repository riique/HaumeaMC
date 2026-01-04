package com.haumea.kitpvp.managers;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.*;
import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.models.npc.Hologram;
import com.haumea.kitpvp.models.npc.NPCData;
import com.haumea.kitpvp.utils.ChatStorage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gerencia NPCs do lobby usando ProtocolLib.
 * NPCs são jogadores virtuais com skins reais que direcionam para servidores.
 * 
 * Funcionalidades:
 * - Spawnar NPCs com skin de jogadores reais via Mojang API
 * - Hologramas dinâmicos com nome do servidor e quantidade de jogadores
 * - Detectar cliques nos NPCs via pacotes
 * - Atualização automática de player count
 * 
 * @author HaumeaMC
 */
public class NPCManager {

    private final HaumeaMC plugin;
    private final ProtocolManager protocolManager;

    // NPCs ativos (entityId -> NPC instance)
    private final Map<Integer, NPCInstance> activeNPCs;

    // Configuração dos NPCs
    private final Map<String, NPCData> npcConfigs;

    // Cache de skins (skinName -> textures)
    private final Map<String, SkinData> skinCache;

    // Hologramas ativos (npcId -> Hologram)
    private final Map<String, Hologram> holograms;

    // Task de atualização de hologramas
    private BukkitTask hologramUpdateTask;

    // Entity ID counter (para evitar conflitos com entidades reais)
    private int nextEntityId = 10000;

    public NPCManager(HaumeaMC plugin) {
        this.plugin = plugin;
        this.protocolManager = ProtocolLibrary.getProtocolManager();
        this.activeNPCs = new ConcurrentHashMap<>();
        this.npcConfigs = new LinkedHashMap<>();
        this.skinCache = new ConcurrentHashMap<>();
        this.holograms = new ConcurrentHashMap<>();

        loadConfig();
        registerPacketListener();
        startHologramUpdater();

        // Spawnar NPCs após 2 segundos (para o mundo carregar)
        Bukkit.getScheduler().runTaskLater(plugin, this::spawnAllNPCs, 40L);

        plugin.getLogger().info("[NPCManager] Sistema de NPCs do Lobby inicializado!");
    }

    /**
     * Carrega configuração dos NPCs do config.yml
     */
    private void loadConfig() {
        ConfigurationSection npcSection = plugin.getConfig().getConfigurationSection("lobby.npcs");

        if (npcSection == null) {
            plugin.getLogger().warning("[NPCManager] Nenhum NPC configurado em lobby.npcs!");
            return;
        }

        for (String npcId : npcSection.getKeys(false)) {
            ConfigurationSection section = npcSection.getConfigurationSection(npcId);
            if (section == null)
                continue;

            String displayName = section.getString("display-name", "&cServidor");
            String skinName = section.getString("skin", "Steve");
            String targetServer = section.getString("server", "");

            // Localização
            String worldName = section.getString("location.world", "world");
            double x = section.getDouble("location.x", 0);
            double y = section.getDouble("location.y", 64);
            double z = section.getDouble("location.z", 0);
            float yaw = (float) section.getDouble("location.yaw", 0);
            float pitch = (float) section.getDouble("location.pitch", 0);

            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                plugin.getLogger()
                        .warning("[NPCManager] Mundo '" + worldName + "' não encontrado para NPC '" + npcId + "'!");
                continue;
            }

            Location location = new Location(world, x, y, z, yaw, pitch);

            // Linhas do holograma
            List<String> hologramLines = section.getStringList("hologram");
            if (hologramLines.isEmpty()) {
                hologramLines = new ArrayList<>();
                hologramLines.add(displayName);
                hologramLines.add("&7Jogando: &a{players}");
                hologramLines.add("&e&lCLIQUE PARA ENTRAR");
            }

            NPCData npcData = new NPCData(npcId, displayName, skinName, targetServer, location, hologramLines);
            npcConfigs.put(npcId, npcData);

            plugin.getLogger()
                    .info("[NPCManager] NPC '" + npcId + "' carregado: skin=" + skinName + ", server=" + targetServer);
        }
    }

    /**
     * Spawna todos os NPCs configurados
     */
    public void spawnAllNPCs() {
        for (NPCData npcData : npcConfigs.values()) {
            spawnNPC(npcData);
        }
    }

    /**
     * Spawna um NPC específico
     */
    private void spawnNPC(NPCData data) {
        // Buscar skin assíncronamente
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            SkinData skinData = getSkin(data.getSkinName());

            // Voltar para a thread principal para spawnar
            Bukkit.getScheduler().runTask(plugin, () -> {
                int entityId = nextEntityId++;
                UUID npcUUID = UUID.randomUUID();

                NPCInstance instance = new NPCInstance(
                        entityId,
                        npcUUID,
                        data.getId(),
                        data.getLocation().clone(),
                        skinData);

                activeNPCs.put(entityId, instance);

                // Spawnar para todos os jogadores online
                for (Player player : Bukkit.getOnlinePlayers()) {
                    showNPCToPlayer(player, instance, data);
                }

                // Criar holograma acima do NPC
                Location holoLoc = data.getLocation().clone().add(0, 2.3, 0);
                List<String> lines = new ArrayList<>(data.getHologramLines());

                // Processar placeholder de players
                int playerCount = 0;
                if (plugin.getServerSelectorManager() != null) {
                    playerCount = plugin.getServerSelectorManager().getPlayerCount(data.getTargetServer());
                }
                for (int i = 0; i < lines.size(); i++) {
                    lines.set(i, lines.get(i).replace("{players}", String.valueOf(playerCount)));
                }

                Hologram hologram = new Hologram(holoLoc, lines);
                holograms.put(data.getId(), hologram);

                plugin.getLogger().info("[NPCManager] NPC '" + data.getId() + "' spawnado com entityId=" + entityId);
            });
        });
    }

    /**
     * Mostra um NPC para um jogador específico
     */
    public void showNPCToPlayer(Player player, NPCInstance npc, NPCData data) {
        try {
            // 1. Enviar PlayerInfo (ADD_PLAYER) para registrar o "jogador fake"
            PacketContainer playerInfoPacket = protocolManager.createPacket(PacketType.Play.Server.PLAYER_INFO);
            playerInfoPacket.getPlayerInfoAction().write(0, EnumWrappers.PlayerInfoAction.ADD_PLAYER);

            WrappedGameProfile gameProfile = new WrappedGameProfile(npc.uuid,
                    ChatStorage.colorize(data.getDisplayName()));

            // Aplicar skin se disponível
            if (npc.skinData != null && npc.skinData.value != null) {
                gameProfile.getProperties().put("textures",
                        new WrappedSignedProperty("textures", npc.skinData.value, npc.skinData.signature));
            }

            PlayerInfoData playerInfoData = new PlayerInfoData(
                    gameProfile,
                    0, // Ping
                    EnumWrappers.NativeGameMode.SURVIVAL,
                    WrappedChatComponent.fromText(ChatStorage.colorize(data.getDisplayName())));

            List<PlayerInfoData> infoList = new ArrayList<>();
            infoList.add(playerInfoData);
            playerInfoPacket.getPlayerInfoDataLists().write(0, infoList);

            protocolManager.sendServerPacket(player, playerInfoPacket);

            // 2. Pequeno delay antes de spawnar a entidade
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                try {
                    // Enviar NamedEntitySpawn para spawnar a entidade
                    PacketContainer spawnPacket = protocolManager
                            .createPacket(PacketType.Play.Server.NAMED_ENTITY_SPAWN);

                    spawnPacket.getIntegers().write(0, npc.entityId); // Entity ID
                    spawnPacket.getUUIDs().write(0, npc.uuid); // UUID

                    // Posição (em fixed-point)
                    Location loc = npc.location;
                    spawnPacket.getIntegers().write(1, (int) (loc.getX() * 32.0));
                    spawnPacket.getIntegers().write(2, (int) (loc.getY() * 32.0));
                    spawnPacket.getIntegers().write(3, (int) (loc.getZ() * 32.0));

                    // Rotação
                    spawnPacket.getBytes().write(0, (byte) (loc.getYaw() * 256.0F / 360.0F)); // Yaw
                    spawnPacket.getBytes().write(1, (byte) (loc.getPitch() * 256.0F / 360.0F)); // Pitch

                    // Item na mão (0 = vazio)
                    spawnPacket.getIntegers().write(4, 0);

                    // DataWatcher (metadata)
                    WrappedDataWatcher watcher = new WrappedDataWatcher();
                    // Skin layers (cape, jacket, sleeves, pants, hat)
                    watcher.setObject(10, (byte) 0x7F);
                    spawnPacket.getDataWatcherModifier().write(0, watcher);

                    protocolManager.sendServerPacket(player, spawnPacket);

                    // 3. Enviar head rotation para olhar na direção certa
                    sendHeadRotation(player, npc);

                    // 4. Remover da tab list após spawnar (não queremos o NPC na tablist)
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        removeFromTablist(player, npc);
                    }, 10L);

                } catch (Exception e) {
                    plugin.getLogger().warning(
                            "[NPCManager] Erro ao spawnar NPC para " + player.getName() + ": " + e.getMessage());
                }
            }, 2L);

        } catch (Exception e) {
            plugin.getLogger().warning("[NPCManager] Erro ao mostrar NPC: " + e.getMessage());
        }
    }

    /**
     * Envia pacote de rotação da cabeça
     */
    private void sendHeadRotation(Player player, NPCInstance npc) {
        try {
            PacketContainer headPacket = protocolManager.createPacket(PacketType.Play.Server.ENTITY_HEAD_ROTATION);
            headPacket.getIntegers().write(0, npc.entityId);
            headPacket.getBytes().write(0, (byte) (npc.location.getYaw() * 256.0F / 360.0F));
            protocolManager.sendServerPacket(player, headPacket);
        } catch (Exception e) {
            // Ignorar erro silenciosamente
        }
    }

    /**
     * Remove o NPC da tablist do jogador
     */
    private void removeFromTablist(Player player, NPCInstance npc) {
        try {
            PacketContainer removePacket = protocolManager.createPacket(PacketType.Play.Server.PLAYER_INFO);
            removePacket.getPlayerInfoAction().write(0, EnumWrappers.PlayerInfoAction.REMOVE_PLAYER);

            WrappedGameProfile profile = new WrappedGameProfile(npc.uuid, "NPC");
            PlayerInfoData data = new PlayerInfoData(profile, 0, EnumWrappers.NativeGameMode.SURVIVAL, null);

            List<PlayerInfoData> list = new ArrayList<>();
            list.add(data);
            removePacket.getPlayerInfoDataLists().write(0, list);

            protocolManager.sendServerPacket(player, removePacket);
        } catch (Exception e) {
            // Ignorar
        }
    }

    /**
     * Registra listener de pacotes para detectar cliques em NPCs
     */
    private void registerPacketListener() {
        protocolManager.addPacketListener(
                new com.comphenix.protocol.events.PacketAdapter(
                        plugin,
                        com.comphenix.protocol.events.ListenerPriority.NORMAL,
                        PacketType.Play.Client.USE_ENTITY) {
                    @Override
                    public void onPacketReceiving(com.comphenix.protocol.events.PacketEvent event) {
                        handleUseEntityPacket(event);
                    }
                });
    }

    /**
     * Processa pacote USE_ENTITY (clique em entidade)
     */
    private void handleUseEntityPacket(com.comphenix.protocol.events.PacketEvent event) {
        PacketContainer packet = event.getPacket();
        int entityId = packet.getIntegers().read(0);

        NPCInstance npc = activeNPCs.get(entityId);
        if (npc == null)
            return;

        // Obter tipo de interação
        EnumWrappers.EntityUseAction action = packet.getEntityUseActions().read(0);

        // Só processar INTERACT e ATTACK (clique esquerdo/direito)
        if (action == EnumWrappers.EntityUseAction.INTERACT || action == EnumWrappers.EntityUseAction.ATTACK) {
            Player player = event.getPlayer();

            // Cancelar evento para evitar outras interações
            event.setCancelled(true);

            // Processar clique na thread principal
            Bukkit.getScheduler().runTask(plugin, () -> {
                onNPCClick(player, npc);
            });
        }
    }

    /**
     * Processa clique em um NPC
     */
    private void onNPCClick(Player player, NPCInstance npc) {
        NPCData data = npcConfigs.get(npc.npcId);
        if (data == null)
            return;

        String targetServer = data.getTargetServer();
        if (targetServer == null || targetServer.isEmpty()) {
            ChatStorage.sendRaw(player, "&cEste NPC não está configurado corretamente!");
            return;
        }

        // Enviar para o servidor via ServerSelectorManager
        if (plugin.getServerSelectorManager() != null) {
            plugin.getServerSelectorManager().sendToServer(player, targetServer);
        } else {
            ChatStorage.sendRaw(player, "&cErro ao conectar ao servidor!");
        }
    }

    /**
     * Inicia task de atualização dos hologramas
     */
    private void startHologramUpdater() {
        hologramUpdateTask = new BukkitRunnable() {
            @Override
            public void run() {
                updateHolograms();
            }
        }.runTaskTimer(plugin, 100L, 100L); // Atualiza a cada 5 segundos
    }

    /**
     * Atualiza textos dos hologramas com contagem de jogadores
     */
    private void updateHolograms() {
        if (plugin.getServerSelectorManager() == null)
            return;

        for (Map.Entry<String, Hologram> entry : holograms.entrySet()) {
            String npcId = entry.getKey();
            Hologram hologram = entry.getValue();

            NPCData data = npcConfigs.get(npcId);
            if (data == null)
                continue;

            int playerCount = plugin.getServerSelectorManager().getPlayerCount(data.getTargetServer());

            List<String> newLines = new ArrayList<>();
            for (String line : data.getHologramLines()) {
                newLines.add(line.replace("{players}", String.valueOf(playerCount)));
            }

            hologram.updateLines(newLines);
        }
    }

    /**
     * Spawna NPCs para um jogador que acabou de entrar
     */
    public void spawnNPCsForPlayer(Player player) {
        for (Map.Entry<Integer, NPCInstance> entry : activeNPCs.entrySet()) {
            NPCInstance npc = entry.getValue();
            NPCData data = npcConfigs.get(npc.npcId);
            if (data != null) {
                showNPCToPlayer(player, npc, data);
            }
        }
    }

    /**
     * Busca skin de um jogador na API da Mojang
     */
    private SkinData getSkin(String playerName) {
        // Verificar cache primeiro
        if (skinCache.containsKey(playerName.toLowerCase())) {
            return skinCache.get(playerName.toLowerCase());
        }

        try {
            // 1. Obter UUID do nome
            URL uuidUrl = new URL("https://api.mojang.com/users/profiles/minecraft/" + playerName);
            HttpURLConnection uuidConn = (HttpURLConnection) uuidUrl.openConnection();
            uuidConn.setConnectTimeout(5000);
            uuidConn.setReadTimeout(5000);

            if (uuidConn.getResponseCode() != 200) {
                plugin.getLogger().warning("[NPCManager] Não foi possível obter UUID do jogador " + playerName);
                return null;
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(uuidConn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            // Parse UUID da resposta JSON simples
            String uuidResponse = response.toString();
            String uuid = parseJsonValue(uuidResponse, "id");
            if (uuid == null || uuid.isEmpty()) {
                return null;
            }

            // 2. Obter textures do UUID
            URL texturesUrl = new URL(
                    "https://sessionserver.mojang.com/session/minecraft/profile/" + uuid + "?unsigned=false");
            HttpURLConnection texturesConn = (HttpURLConnection) texturesUrl.openConnection();
            texturesConn.setConnectTimeout(5000);
            texturesConn.setReadTimeout(5000);

            if (texturesConn.getResponseCode() != 200) {
                return null;
            }

            reader = new BufferedReader(new InputStreamReader(texturesConn.getInputStream()));
            response = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            String texturesResponse = response.toString();

            // Parse textures da resposta
            String value = parseNestedJsonValue(texturesResponse, "value");
            String signature = parseNestedJsonValue(texturesResponse, "signature");

            if (value != null && !value.isEmpty()) {
                SkinData skinData = new SkinData(value, signature);
                skinCache.put(playerName.toLowerCase(), skinData);
                plugin.getLogger().info("[NPCManager] Skin de '" + playerName + "' carregada com sucesso!");
                return skinData;
            }

        } catch (Exception e) {
            plugin.getLogger().warning("[NPCManager] Erro ao buscar skin de " + playerName + ": " + e.getMessage());
        }

        return null;
    }

    /**
     * Parse simples de valor JSON
     */
    private String parseJsonValue(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start == -1)
            return null;

        start += search.length();
        int end = json.indexOf("\"", start);
        if (end == -1)
            return null;

        return json.substring(start, end);
    }

    /**
     * Parse de valor aninhado em properties[]
     */
    private String parseNestedJsonValue(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start == -1) {
            // Tentar sem aspas (para campos numéricos)
            search = "\"" + key + "\":";
            start = json.indexOf(search);
            if (start == -1)
                return null;
        }

        start += search.length();

        // Verificar se começa com aspas
        if (json.charAt(start) == '"') {
            start++;
            int end = json.indexOf("\"", start);
            if (end == -1)
                return null;
            return json.substring(start, end);
        } else {
            // Valor sem aspas (número ou booleano)
            int end = start;
            while (end < json.length() && json.charAt(end) != ',' && json.charAt(end) != '}') {
                end++;
            }
            return json.substring(start, end);
        }
    }

    /**
     * Desliga o manager e remove todos os NPCs
     */
    public void shutdown() {
        // Cancelar task de atualização
        if (hologramUpdateTask != null) {
            hologramUpdateTask.cancel();
            hologramUpdateTask = null;
        }

        // Remover hologramas
        for (Hologram hologram : holograms.values()) {
            hologram.despawn();
        }
        holograms.clear();

        // Remover NPCs da tab de jogadores (não necessário, eles não persistem)
        activeNPCs.clear();

        // Remover listeners de pacote
        protocolManager.removePacketListeners(plugin);

        plugin.getLogger().info("[NPCManager] Sistema de NPCs desligado!");
    }

    /**
     * Classe interna para dados de skin
     */
    private static class SkinData {
        final String value;
        final String signature;

        SkinData(String value, String signature) {
            this.value = value;
            this.signature = signature;
        }
    }

    /**
     * Classe interna para instância de NPC ativo
     */
    private static class NPCInstance {
        final int entityId;
        final UUID uuid;
        final String npcId;
        final Location location;
        final SkinData skinData;

        NPCInstance(int entityId, UUID uuid, String npcId, Location location, SkinData skinData) {
            this.entityId = entityId;
            this.uuid = uuid;
            this.npcId = npcId;
            this.location = location;
            this.skinData = skinData;
        }
    }
}
