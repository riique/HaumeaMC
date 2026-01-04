package com.haumea.kitpvp.managers;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.models.SkinData;
import com.haumea.kitpvp.utils.ChatStorage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * Gerenciador de Skins do HaumeaMC.
 * 
 * Responsabilidades:
 * - Buscar texturas da API Mojang
 * - Aplicar skins nos jogadores via pacotes NMS
 * - Gerenciar cooldowns e cache
 * - Persistir skins selecionadas
 * 
 * Este sistema utiliza reflexão para modificar o GameProfile do jogador
 * e enviar pacotes de refresh, compatível com Spigot 1.8.8.
 * 
 * @author HaumeaMC
 */
public class SkinManager {

    private final HaumeaMC plugin;

    // Cache de skins buscadas (evita requests repetidos)
    private final Map<String, SkinData> skinCache;

    // Skins pré-definidas populares
    private final Map<String, String> presetSkins;

    // Arquivo de persistência
    private File skinsFile;
    private YamlConfiguration skinsConfig;

    // Versão NMS (ex: v1_8_R3)
    private String nmsVersion;

    /**
     * Construtor do SkinManager
     */
    public SkinManager(HaumeaMC plugin) {
        this.plugin = plugin;
        this.skinCache = new ConcurrentHashMap<>();
        this.presetSkins = new ConcurrentHashMap<>();

        // Detectar versão NMS
        this.nmsVersion = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];

        initPresetSkins();
        loadSkinsFile();
    }

    /**
     * Inicializa skins pré-definidas populares
     */
    private void initPresetSkins() {
        // YouTubers/Streamers famosos
        presetSkins.put("Notch", "Notch");
        presetSkins.put("Dream", "Dream");
        presetSkins.put("Technoblade", "Technoblade");
        presetSkins.put("TazerCraft", "TazerCraft");
        presetSkins.put("AuthenticGames", "AuthenticGames");
        presetSkins.put("Rezende", "RezendeEvil");
        presetSkins.put("VenomExtreme", "VenomExtreme");
        presetSkins.put("Jazzghost", "Jazzghost");
        presetSkins.put("Flakes", "FlakesPower");
        presetSkins.put("Cellbit", "Cellbit");

        // Skins Clássicas
        presetSkins.put("Steve", "MHF_Steve");
        presetSkins.put("Alex", "MHF_Alex");
        presetSkins.put("Herobrine", "MHF_Herobrine");
        presetSkins.put("Creeper", "MHF_Creeper");
        presetSkins.put("Enderman", "MHF_Enderman");
        presetSkins.put("Ghast", "MHF_Ghast");
        presetSkins.put("Pig", "MHF_Pig");
        presetSkins.put("Villager", "MHF_Villager");
    }

    /**
     * Carrega o arquivo de skins salvas
     */
    private void loadSkinsFile() {
        skinsFile = new File(plugin.getDataFolder(), "skins.yml");
        if (!skinsFile.exists()) {
            try {
                skinsFile.createNewFile();
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Erro ao criar skins.yml", e);
            }
        }
        skinsConfig = YamlConfiguration.loadConfiguration(skinsFile);
    }

    /**
     * Salva as skins no arquivo
     */
    public void saveSkinsFile() {
        try {
            skinsConfig.save(skinsFile);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Erro ao salvar skins.yml", e);
        }
    }

    // ==================== API MOJANG ====================

    /**
     * Busca o UUID de um jogador pelo nome (API Mojang)
     * 
     * @param username Nome do jogador
     * @return UUID como string ou null se não encontrado
     */
    public String fetchUUID(String username) {
        try {
            URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + username);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            connection.setRequestProperty("User-Agent", "HaumeaMC-SkinPlugin");

            int responseCode = connection.getResponseCode();
            plugin.getLogger().info("[Skin] Buscando UUID de '" + username + "' - Response: " + responseCode);

            if (responseCode == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                String json = response.toString();
                plugin.getLogger().info("[Skin] Resposta UUID: " + json);

                // Parse mais robusto - procurar por "id" e extrair o valor
                String uuid = extractJsonValue(json, "id");
                if (uuid != null) {
                    plugin.getLogger().info("[Skin] UUID encontrado: " + uuid);
                    return uuid;
                }
            } else if (responseCode == 204 || responseCode == 404) {
                plugin.getLogger().warning("[Skin] Jogador '" + username + "' não encontrado na API Mojang");
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "[Skin] Erro ao buscar UUID de " + username, e);
        }
        return null;
    }

    /**
     * Extrai um valor de um campo JSON simples
     * Suporta formatos: "key":"value" ou "key": "value"
     */
    private String extractJsonValue(String json, String key) {
        // Procurar por "key" seguido de : e "
        String searchKey = "\"" + key + "\"";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex == -1)
            return null;

        // Encontrar o : após a key
        int colonIndex = json.indexOf(":", keyIndex + searchKey.length());
        if (colonIndex == -1)
            return null;

        // Encontrar a primeira aspas após o :
        int valueStart = json.indexOf("\"", colonIndex);
        if (valueStart == -1)
            return null;
        valueStart++; // Pular a aspas de abertura

        // Encontrar a aspas de fechamento
        int valueEnd = json.indexOf("\"", valueStart);
        if (valueEnd == -1)
            return null;

        return json.substring(valueStart, valueEnd);
    }

    /**
     * Busca os dados de skin de um jogador (API SessionServer)
     * 
     * @param username Nome do jogador
     * @param uuid     UUID do jogador (sem hífens)
     * @return SkinData com value e signature ou null se não encontrado
     */
    public SkinData fetchSkinData(String username, String uuid) {
        try {
            URL url = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid + "?unsigned=false");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            connection.setRequestProperty("User-Agent", "HaumeaMC-SkinPlugin");

            int responseCode = connection.getResponseCode();
            plugin.getLogger()
                    .info("[Skin] Buscando skin data de '" + username + "' (" + uuid + ") - Response: " + responseCode);

            if (responseCode == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                String json = response.toString();

                // Extrair value e signature usando o método robusto
                String value = extractJsonValue(json, "value");
                String signature = extractJsonValue(json, "signature");

                plugin.getLogger().info(
                        "[Skin] Value encontrado: " + (value != null ? "sim (" + value.length() + " chars)" : "não"));
                plugin.getLogger().info("[Skin] Signature encontrada: "
                        + (signature != null ? "sim (" + signature.length() + " chars)" : "não"));

                if (value != null && signature != null) {
                    return new SkinData(username, uuid, value, signature);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "[Skin] Erro ao buscar skin de " + uuid, e);
        }
        return null;
    }

    /**
     * Busca skin completa pelo nome do jogador (com cache)
     * 
     * @param username Nome do jogador premium
     * @return SkinData ou null se não encontrado
     */
    public SkinData fetchSkin(String username) {
        // Verificar cache
        String cacheKey = username.toLowerCase();
        if (skinCache.containsKey(cacheKey)) {
            SkinData cached = skinCache.get(cacheKey);
            if (!cached.isExpired()) {
                return cached;
            }
            skinCache.remove(cacheKey);
        }

        // Buscar da API
        String uuid = fetchUUID(username);
        if (uuid == null) {
            return null;
        }

        SkinData skinData = fetchSkinData(username, uuid);
        if (skinData != null && skinData.isValid()) {
            skinCache.put(cacheKey, skinData);
        }

        return skinData;
    }

    // ==================== APLICAÇÃO DE SKINS VIA REFLEXÃO ====================

    /**
     * Aplica uma skin em um jogador usando reflexão pura
     * 
     * @param player   Jogador alvo
     * @param skinData Dados da skin a aplicar
     * @return true se aplicou com sucesso
     */
    public boolean applySkin(Player player, SkinData skinData) {
        if (skinData == null || !skinData.isValid()) {
            return false;
        }

        try {
            // Obter o EntityPlayer
            Object entityPlayer = getHandle(player);

            // Obter o GameProfile
            Object gameProfile = getGameProfile(entityPlayer);

            // Obter PropertyMap do profile
            Object propertyMap = getPropertyMap(gameProfile);

            // Limpar texturas antigas
            clearTextures(propertyMap);

            // Criar nova Property de textura
            Object textureProperty = createTextureProperty(skinData.getValue(), skinData.getSignature());

            // Adicionar nova textura
            addTextureProperty(propertyMap, textureProperty);

            // Atualizar todos os jogadores
            refreshPlayer(player);

            // Salvar skin do jogador
            saveSkinChoice(player.getUniqueId(), skinData.getOwnerName());

            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Erro ao aplicar skin em " + player.getName(), e);
            return false;
        }
    }

    /**
     * Aplica uma skin pelo nome do dono (assíncrono)
     * 
     * @param player       Jogador alvo
     * @param skinUsername Nome do dono da skin
     * @param callback     Callback executado com resultado (true = sucesso)
     */
    public void applySkinAsync(Player player, String skinUsername, Consumer<Boolean> callback) {
        // Usar CooldownManager central
        CooldownManager cooldownManager = plugin.getCooldownManager();
        if (cooldownManager != null && cooldownManager.isOnCooldown(player, CooldownManager.SKIN)) {
            int remaining = cooldownManager.getRemainingSeconds(player, CooldownManager.SKIN);
            ChatStorage.send(player, "skin.cooldown", "time", String.valueOf(remaining));
            callback.accept(false);
            return;
        }

        // Aplicar cooldown via CooldownManager
        if (cooldownManager != null) {
            cooldownManager.setCooldown(player, CooldownManager.SKIN);
        }

        // Buscar skin em thread separada
        new BukkitRunnable() {
            @Override
            public void run() {
                SkinData skinData = fetchSkin(skinUsername);

                // Voltar para main thread para aplicar
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (skinData != null && player.isOnline()) {
                            boolean success = applySkin(player, skinData);
                            callback.accept(success);
                        } else {
                            callback.accept(false);
                        }
                    }
                }.runTask(plugin);
            }
        }.runTaskAsynchronously(plugin);
    }

    /**
     * Restaura a skin original do jogador
     * 
     * @param player Jogador
     */
    public void resetSkin(Player player) {
        // Aplicar skin baseada no próprio nome (original)
        applySkinAsync(player, player.getName(), success -> {
            if (success) {
                ChatStorage.send(player, "skin.reset");
                removeSkinChoice(player.getUniqueId());
            } else {
                ChatStorage.send(player, "skin.reset-error");
            }
        });
    }

    // ==================== MÉTODOS DE REFLEXÃO ====================

    /**
     * Obtém o EntityPlayer (handle NMS) do jogador
     */
    private Object getHandle(Player player) throws Exception {
        return player.getClass().getMethod("getHandle").invoke(player);
    }

    /**
     * Obtém o GameProfile do EntityPlayer
     */
    private Object getGameProfile(Object entityPlayer) throws Exception {
        // O método getProfile() retorna o GameProfile
        Class<?> entityHumanClass = Class.forName("net.minecraft.server." + nmsVersion + ".EntityHuman");
        Method getProfile = entityHumanClass.getMethod("getProfile");
        return getProfile.invoke(entityPlayer);
    }

    /**
     * Obtém o PropertyMap do GameProfile
     */
    private Object getPropertyMap(Object gameProfile) throws Exception {
        // GameProfile.getProperties() retorna PropertyMap
        return gameProfile.getClass().getMethod("getProperties").invoke(gameProfile);
    }

    /**
     * Limpa texturas existentes do PropertyMap
     */
    private void clearTextures(Object propertyMap) throws Exception {
        // PropertyMap é um ForwardingMultimap, possui removeAll("textures")
        Method removeAll = propertyMap.getClass().getMethod("removeAll", Object.class);
        removeAll.invoke(propertyMap, "textures");
    }

    /**
     * Cria uma Property de textura
     */
    private Object createTextureProperty(String value, String signature) throws Exception {
        // Property(String name, String value, String signature)
        Class<?> propertyClass = Class.forName("com.mojang.authlib.properties.Property");
        Constructor<?> constructor = propertyClass.getConstructor(String.class, String.class, String.class);
        return constructor.newInstance("textures", value, signature);
    }

    /**
     * Adiciona uma Property ao PropertyMap
     */
    private void addTextureProperty(Object propertyMap, Object property) throws Exception {
        // PropertyMap.put(String key, Property property)
        Method put = propertyMap.getClass().getMethod("put", Object.class, Object.class);
        put.invoke(propertyMap, "textures", property);
    }

    // ==================== REFRESH DE PACOTES ====================

    /**
     * Atualiza a visualização do jogador para todos os outros
     * Envia pacotes de remoção e adição para simular reconexão
     * 
     * @param player Jogador a atualizar
     */
    public void refreshPlayer(Player player) {
        try {
            // Obter EntityPlayer
            Object entityPlayer = getHandle(player);

            // Obter PlayerConnection
            Field playerConnectionField = entityPlayer.getClass().getField("playerConnection");
            Object playerConnection = playerConnectionField.get(entityPlayer);

            // ===== PACOTES PARA OUTROS JOGADORES =====

            // PacketPlayOutPlayerInfo REMOVE_PLAYER
            Object removePacket = createPlayerInfoPacket(entityPlayer, "REMOVE_PLAYER");

            // PacketPlayOutPlayerInfo ADD_PLAYER
            Object addPacket = createPlayerInfoPacket(entityPlayer, "ADD_PLAYER");

            // PacketPlayOutEntityDestroy
            Object destroyPacket = createEntityDestroyPacket(player.getEntityId());

            // PacketPlayOutNamedEntitySpawn
            Object spawnPacket = createNamedEntitySpawnPacket(entityPlayer);

            // Enviar pacotes para todos os outros jogadores
            for (Player other : Bukkit.getOnlinePlayers()) {
                if (other.equals(player))
                    continue;
                if (!other.canSee(player))
                    continue;

                Object otherEntityPlayer = getHandle(other);
                Object otherConnection = otherEntityPlayer.getClass().getField("playerConnection")
                        .get(otherEntityPlayer);

                // Remover e adicionar na PlayerList
                sendPacket(otherConnection, removePacket);
                sendPacket(otherConnection, addPacket);

                // Destruir e spawnar entidade
                sendPacket(otherConnection, destroyPacket);
                sendPacket(otherConnection, spawnPacket);
            }

            // ===== PACOTES PARA O PRÓPRIO JOGADOR (AUTO-RESPAWN) =====

            // Salvar estado do jogador
            Location loc = player.getLocation().clone();
            boolean allowFlight = player.getAllowFlight();
            boolean flying = player.isFlying();
            float walkSpeed = player.getWalkSpeed();
            float flySpeed = player.getFlySpeed();
            int foodLevel = player.getFoodLevel();
            double health = player.getHealth();
            float exp = player.getExp();
            int level = player.getLevel();

            // Enviar pacote de remoção/adição na lista de jogadores para si mesmo
            sendPacket(playerConnection, removePacket);
            sendPacket(playerConnection, addPacket);

            // Enviar PacketPlayOutRespawn (simular mudança de dimensão)
            sendRespawnPacket(player, playerConnection);

            // Teleportar de volta e restaurar estados
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!player.isOnline())
                        return;

                    // Restaurar posição
                    player.teleport(loc);

                    // Restaurar estados
                    player.setAllowFlight(allowFlight);
                    player.setFlying(flying);
                    player.setWalkSpeed(walkSpeed);
                    player.setFlySpeed(flySpeed);
                    player.setFoodLevel(foodLevel);
                    player.setHealth(health);
                    player.setExp(exp);
                    player.setLevel(level);

                    // Atualizar inventário
                    player.updateInventory();
                }
            }.runTaskLater(plugin, 1L);

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Erro ao atualizar skin de " + player.getName(), e);
        }
    }

    /**
     * Cria um PacketPlayOutPlayerInfo
     */
    private Object createPlayerInfoPacket(Object entityPlayer, String action) throws Exception {
        Class<?> packetClass = getNMSClass("PacketPlayOutPlayerInfo");
        Class<?> enumClass = getNMSClass("PacketPlayOutPlayerInfo$EnumPlayerInfoAction");
        Class<?> entityPlayerClass = getNMSClass("EntityPlayer");

        // Obter o enum action
        Object enumAction = Enum.valueOf((Class<Enum>) enumClass, action);

        // Criar array de EntityPlayer
        Object playerArray = Array.newInstance(entityPlayerClass, 1);
        Array.set(playerArray, 0, entityPlayer);

        // Construtor: PacketPlayOutPlayerInfo(EnumPlayerInfoAction, EntityPlayer...)
        Constructor<?> constructor = packetClass.getConstructor(enumClass, playerArray.getClass());
        return constructor.newInstance(enumAction, playerArray);
    }

    /**
     * Cria um PacketPlayOutEntityDestroy
     */
    private Object createEntityDestroyPacket(int entityId) throws Exception {
        Class<?> packetClass = getNMSClass("PacketPlayOutEntityDestroy");
        Constructor<?> constructor = packetClass.getConstructor(int[].class);
        return constructor.newInstance(new int[] { entityId });
    }

    /**
     * Cria um PacketPlayOutNamedEntitySpawn
     */
    private Object createNamedEntitySpawnPacket(Object entityPlayer) throws Exception {
        Class<?> packetClass = getNMSClass("PacketPlayOutNamedEntitySpawn");
        Class<?> entityHumanClass = getNMSClass("EntityHuman");
        Constructor<?> constructor = packetClass.getConstructor(entityHumanClass);
        return constructor.newInstance(entityPlayer);
    }

    /**
     * Envia o pacote de respawn para o jogador
     */
    private void sendRespawnPacket(Player player, Object playerConnection) throws Exception {
        World world = player.getWorld();

        // Obter WorldServer
        Object craftWorld = world;
        Object worldServer = craftWorld.getClass().getMethod("getHandle").invoke(craftWorld);

        // Obter WorldData
        Object worldData = worldServer.getClass().getMethod("getWorldData").invoke(worldServer);

        // Obter dimensão do WorldProvider
        Object worldProvider = worldServer.getClass().getMethod("getWorldProvider").invoke(worldServer);
        int dimension = (int) worldProvider.getClass().getMethod("getDimension").invoke(worldProvider);

        // Obter dificuldade
        Object difficulty = worldData.getClass().getMethod("getDifficulty").invoke(worldData);

        // Obter tipo de mundo
        Object worldType = worldData.getClass().getMethod("getType").invoke(worldData);

        // Obter gamemode do jogador
        Object entityPlayer = getHandle(player);
        Object interactManager = entityPlayer.getClass().getField("playerInteractManager").get(entityPlayer);
        Object gameMode = interactManager.getClass().getMethod("getGameMode").invoke(interactManager);

        // Criar PacketPlayOutRespawn
        Class<?> respawnClass = getNMSClass("PacketPlayOutRespawn");
        Class<?> difficultyClass = getNMSClass("EnumDifficulty");
        Class<?> worldTypeClass = getNMSClass("WorldType");
        Class<?> gameModeClass = getNMSClass("WorldSettings$EnumGamemode");

        Constructor<?> constructor = respawnClass.getConstructor(int.class, difficultyClass, worldTypeClass,
                gameModeClass);
        Object respawnPacket = constructor.newInstance(dimension, difficulty, worldType, gameMode);

        sendPacket(playerConnection, respawnPacket);
    }

    /**
     * Envia um pacote para PlayerConnection
     */
    private void sendPacket(Object playerConnection, Object packet) throws Exception {
        Class<?> packetClass = getNMSClass("Packet");
        Method sendPacket = playerConnection.getClass().getMethod("sendPacket", packetClass);
        sendPacket.invoke(playerConnection, packet);
    }

    /**
     * Obtém uma classe NMS pelo nome
     */
    private Class<?> getNMSClass(String name) throws ClassNotFoundException {
        return Class.forName("net.minecraft.server." + nmsVersion + "." + name);
    }

    // ==================== COOLDOWN (DELEGADO PARA COOLDOWNMANAGER)
    // ====================

    /**
     * Verifica se o jogador está em cooldown.
     * Delega para o CooldownManager central.
     * 
     * @deprecated Use plugin.getCooldownManager().isOnCooldown(player,
     *             CooldownManager.SKIN)
     */
    @Deprecated
    public boolean isOnCooldown(Player player) {
        CooldownManager cooldownManager = plugin.getCooldownManager();
        if (cooldownManager != null) {
            return cooldownManager.isOnCooldown(player, CooldownManager.SKIN);
        }
        return false;
    }

    /**
     * Obtém o tempo restante de cooldown.
     * Delega para o CooldownManager central.
     * 
     * @deprecated Use plugin.getCooldownManager().getRemainingTime(player,
     *             CooldownManager.SKIN)
     */
    @Deprecated
    public long getCooldownRemaining(Player player) {
        CooldownManager cooldownManager = plugin.getCooldownManager();
        if (cooldownManager != null) {
            return cooldownManager.getRemainingTime(player, CooldownManager.SKIN);
        }
        return 0;
    }

    /**
     * Aplica cooldown no jogador.
     * Delega para o CooldownManager central.
     * 
     * @deprecated Use plugin.getCooldownManager().setCooldown(player,
     *             CooldownManager.SKIN)
     */
    @Deprecated
    public void setCooldown(Player player) {
        CooldownManager cooldownManager = plugin.getCooldownManager();
        if (cooldownManager != null) {
            cooldownManager.setCooldown(player, CooldownManager.SKIN);
        }
    }

    // ==================== PERSISTÊNCIA ====================

    /**
     * Salva a escolha de skin do jogador
     */
    public void saveSkinChoice(UUID uuid, String skinName) {
        skinsConfig.set("players." + uuid.toString(), skinName);
        saveSkinsFile();
    }

    /**
     * Remove a escolha de skin do jogador
     */
    public void removeSkinChoice(UUID uuid) {
        skinsConfig.set("players." + uuid.toString(), null);
        saveSkinsFile();
    }

    /**
     * Obtém a skin salva do jogador
     */
    public String getSavedSkin(UUID uuid) {
        return skinsConfig.getString("players." + uuid.toString(), null);
    }

    /**
     * Verifica se o jogador tem skin customizada
     */
    public boolean hasCustomSkin(UUID uuid) {
        return skinsConfig.contains("players." + uuid.toString());
    }

    /**
     * Restaura a skin salva do jogador ao logar
     */
    public void restoreSavedSkin(Player player) {
        String savedSkin = getSavedSkin(player.getUniqueId());
        if (savedSkin != null && !savedSkin.equalsIgnoreCase(player.getName())) {
            // Delay para garantir que o jogador está completamente carregado
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (player.isOnline()) {
                        applySkinAsync(player, savedSkin, success -> {
                            if (success) {
                                plugin.getLogger()
                                        .info("Skin de " + savedSkin + " restaurada para " + player.getName());
                            }
                        });
                    }
                }
            }.runTaskLater(plugin, 20L);
        }
    }

    // ==================== GETTERS ====================

    /**
     * Obtém as skins pré-definidas
     */
    public Map<String, String> getPresetSkins() {
        return presetSkins;
    }

    /**
     * Obtém o cache de skins
     */
    public Map<String, SkinData> getSkinCache() {
        return skinCache;
    }
}
