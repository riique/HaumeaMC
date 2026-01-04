package com.haumea.kitpvp.managers;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.models.Group;
import com.haumea.kitpvp.profile.PlayerProfile;
import com.haumea.kitpvp.utils.ChatStorage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gerenciador do sistema de Fake Nick do HaumeaMC.
 * 
 * Permite que jogadores alterem seu nome de exibição mantendo
 * suas permissões, tags e benefícios originais.
 * 
 * Funcionalidades:
 * - Alteração de nome via Reflection no GameProfile
 * - Lista de nomes aleatórios pré-definidos
 * - Validação de nomes (padrões Minecraft, contas premium/registradas)
 * - Integração completa com sistema de Tags
 * - Persistência de permissões baseada no UUID original
 * 
 * @author HaumeaMC
 */
public class FakeNickManager {

    private final HaumeaMC plugin;

    // Mapa de jogadores com fake nick (UUID -> Nome Fake)
    private final Map<UUID, String> fakeNicks;

    // Mapa de nomes originais (UUID -> Nome Original)
    private final Map<UUID, String> originalNames;

    // Mapa de tags anteriores (UUID -> Tag ID)
    private final Map<UUID, String> previousTags;

    // Lista de nomes aleatórios disponíveis
    private final List<String> randomNames;

    // Cache de nomes já validados como Premium
    private final Set<String> premiumNamesCache;

    // Campo de Reflection para o nome no GameProfile (cacheado)
    private Field gameProfileNameField;

    // Flag de reflection inicializado
    private boolean reflectionReady;

    public FakeNickManager(HaumeaMC plugin) {
        this.plugin = plugin;
        this.fakeNicks = new ConcurrentHashMap<>();
        this.originalNames = new ConcurrentHashMap<>();
        this.previousTags = new ConcurrentHashMap<>();
        this.randomNames = new ArrayList<>();
        this.premiumNamesCache = new HashSet<>();

        loadRandomNames();
        initReflection();

        plugin.getLogger().info("[FakeNickManager] Sistema de Fake Nick inicializado!");
    }

    // ==================== INICIALIZAÇÃO ====================

    /**
     * Carrega a lista de nomes aleatórios para /fake random
     */
    private void loadRandomNames() {
        // Lista de nomes genéricos para servidores de KitPvP
        String[] defaultNames = {
                "Player", "Gamer", "xX_Pro_Xx", "Ninja", "Shadow", "Ghost", "Storm",
                "Wolf", "Dragon", "Phoenix", "Tiger", "Lion", "Eagle", "Hawk",
                "Hunter", "Warrior", "Knight", "Assassin", "Sniper", "Blade",
                "Dark", "Light", "Fire", "Ice", "Thunder", "Crystal", "Diamond",
                "Gold", "Silver", "Bronze", "Iron", "Steel", "Stone", "Wood",
                "PvPMaster", "KitPro", "SoupGod", "Legend", "Champion", "Hero",
                "Viper", "Cobra", "Python", "Scorpion", "Spider", "Mantis",
                "Raven", "Crow", "Falcon", "Vulture", "Osprey", "Sparrow",
                "Alpha", "Beta", "Gamma", "Delta", "Omega", "Sigma", "Zeta"
        };

        // Adicionar com sufixos numéricos para variedade
        for (String name : defaultNames) {
            randomNames.add(name);
            for (int i = 1; i <= 99; i++) {
                randomNames.add(name + i);
            }
        }

        Collections.shuffle(randomNames);
    }

    /**
     * Inicializa os campos de Reflection para modificar o GameProfile
     * Usa reflexão pura sem depender do import com.mojang.authlib
     */
    private void initReflection() {
        try {
            // Tentar encontrar a classe GameProfile via reflexão
            Class<?> gameProfileClass = Class.forName("com.mojang.authlib.GameProfile");

            // Campo 'name' do GameProfile
            for (Field field : gameProfileClass.getDeclaredFields()) {
                if (field.getType().equals(String.class)) {
                    // O campo 'name' é o primeiro campo String na GameProfile
                    if (gameProfileNameField == null) {
                        gameProfileNameField = field;
                        gameProfileNameField.setAccessible(true);
                        break;
                    }
                }
            }

            reflectionReady = (gameProfileNameField != null);

            if (reflectionReady) {
                plugin.getLogger().info("[FakeNickManager] Reflection inicializado com sucesso!");
            } else {
                plugin.getLogger().warning("[FakeNickManager] Campo de nome do GameProfile não encontrado.");
            }

        } catch (Exception e) {
            reflectionReady = false;
            plugin.getLogger().warning("[FakeNickManager] Erro ao inicializar reflection: " + e.getMessage());
        }
    }

    // ==================== OPERAÇÕES PRINCIPAIS ====================

    /**
     * Define um fake nick para um jogador
     * 
     * @param player   Jogador
     * @param fakeName Nome fake desejado
     * @return true se aplicado com sucesso
     */
    public boolean setFakeNick(Player player, String fakeName) {
        UUID uuid = player.getUniqueId();
        String cleanName = fakeName.trim();

        // Validar nome
        ValidationResult validation = validateNickname(cleanName, uuid);
        if (!validation.isValid()) {
            ChatStorage.sendRaw(player, validation.getMessage());
            return false;
        }

        // Salvar nome original se ainda não estiver com fake
        if (!hasFakeNick(player)) {
            originalNames.put(uuid, player.getName());

            // Salvar tag atual e definir como membro (null)
            if (plugin.getProfileManager() != null) {
                PlayerProfile profile = plugin.getProfileManager().getProfile(player);
                if (profile != null) {
                    String currentTag = profile.getData().getSelectedTag();
                    if (currentTag != null) {
                        previousTags.put(uuid, currentTag);
                    }
                    // Definir como null (Membro) para anonimato inicial
                    profile.getData().setSelectedTag(null);
                }
            }
        }

        // Aplicar fake nick
        fakeNicks.put(uuid, cleanName);

        // Notificar DisplayManager para atualização unificada
        if (plugin.getDisplayManager() != null) {
            plugin.getDisplayManager().onFakeNickChange(player);
        } else {
            // Fallback: aplicar visualmente diretamente
            applyFakeNick(player, cleanName);
        }

        // Atualizar nametag via NametagManager (prefixo + sufixo)
        // Isso garante que a nametag mostre: [prefixo neutro] [nick fake] [liga
        // original]
        if (plugin.getNametagManager() != null) {
            plugin.getNametagManager().updateNametag(player);
        }

        // Atualizar pacotes via ProtocolLib para nametag (com delay para DisplayManager
        // atualizar teams primeiro)
        if (plugin.getFakeNickPacketListener() != null) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    plugin.getFakeNickPacketListener().refreshPlayerInfo(player);
                }
            }, 5L); // 5 ticks = 250ms
        }

        // Mensagem de sucesso
        ChatStorage.sendRaw(player, ChatStorage.getMessage("fake.applied",
                "name", cleanName));

        return true;
    }

    /**
     * Define um fake nick aleatório para o jogador
     * 
     * @param player Jogador
     * @return true se aplicado com sucesso
     */
    public boolean setRandomFakeNick(Player player) {
        // Encontrar um nome disponível
        String randomName = null;

        for (int attempts = 0; attempts < 100; attempts++) {
            int index = new Random().nextInt(randomNames.size());
            String candidate = randomNames.get(index);

            ValidationResult validation = validateNickname(candidate, player.getUniqueId());
            if (validation.isValid()) {
                randomName = candidate;
                break;
            }
        }

        if (randomName == null) {
            ChatStorage.sendRaw(player, ChatStorage.getMessage("fake.no-random-available"));
            return false;
        }

        return setFakeNick(player, randomName);
    }

    /**
     * Remove o fake nick de um jogador e restaura o original
     * 
     * @param player Jogador
     * @return true se removido com sucesso
     */
    public boolean resetFakeNick(Player player) {
        UUID uuid = player.getUniqueId();

        if (!hasFakeNick(player)) {
            ChatStorage.sendRaw(player, ChatStorage.getMessage("fake.not-using"));
            return false;
        }

        // Restaurar a tag anterior
        if (plugin.getProfileManager() != null) {
            PlayerProfile profile = plugin.getProfileManager().getProfile(player);
            if (profile != null) {
                if (previousTags.containsKey(uuid)) {
                    // Restaurar tag salva
                    String oldTag = previousTags.remove(uuid);
                    profile.getData().setSelectedTag(oldTag);
                } else {
                    // Se não tinha tag salva, volta para sem tag (Membro)
                    // Isso cobre o caso onde ele mudou para uma tag durante o fake,
                    // mas originalmente estava sem tag
                    profile.getData().setSelectedTag(null);
                }
            }
        }

        // 1. Remover dos mapas de fake nick
        fakeNicks.remove(uuid);
        originalNames.remove(uuid);

        // 3. Atualizar exibição via DisplayManager (faz TUDO de forma unificada)
        if (plugin.getDisplayManager() != null) {
            plugin.getDisplayManager().onFakeNickChange(player);
        } else {
            // Fallback: atualização manual se DisplayManager não estiver disponível
            Group group = plugin.getGroupManager().getPlayerGroup(player);
            String prefix = "§7";
            if (group != null && !group.getPrefix().isEmpty()) {
                prefix = ChatStorage.colorize(group.getPrefix());
            }

            String displayName = prefix + player.getName();
            player.setDisplayName(displayName);
            player.setPlayerListName(player.getName());

            if (plugin.getTabManager() != null) {
                plugin.getTabManager().updatePlayerTeam(player);
                plugin.getTabManager().updateTabList(player);
            }

            updateScoreboardTeams(player, prefix, player.getName());
            refreshPlayerVisibility(player);
        }

        // Atualizar nametag via NametagManager (restaurar prefixo do grupo + liga)
        // Isso garante que a nametag mostre: [prefixo do grupo] [nick real] [liga
        // original]
        if (plugin.getNametagManager() != null) {
            plugin.getNametagManager().updateNametag(player);
        }

        // Atualizar pacotes via ProtocolLib para restaurar nametag original (com delay)
        if (plugin.getFakeNickPacketListener() != null) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    plugin.getFakeNickPacketListener().refreshPlayerInfo(player);
                }
            }, 5L);
        }

        // Mensagem de sucesso
        ChatStorage.sendRaw(player, ChatStorage.getMessage("fake.reset"));

        return true;
    }

    // ==================== APLICAÇÃO VISUAL ====================

    /**
     * Aplica o fake nick visualmente
     * - Atualiza o GameProfile via Reflection
     * - Atualiza o TabList para todos os jogadores
     * - Atualiza o Nametag acima da cabeça
     */
    private void applyFakeNick(Player player, String newName) {
        // 1. Tentar modificar o GameProfile via Reflection
        if (reflectionReady) {
            try {
                modifyGameProfile(player, newName);
            } catch (Exception e) {
                plugin.getLogger().warning("[FakeNickManager] Erro ao modificar GameProfile: " + e.getMessage());
            }
        }

        // 2. Atualizar DisplayName e PlayerListName
        String prefix = getCurrentPrefix(player);
        String displayName = prefix + newName;

        player.setDisplayName(displayName);
        player.setPlayerListName(newName); // Nome fake na TabList

        // 3. Atualizar TabList via TabManager
        if (plugin.getTabManager() != null) {
            plugin.getTabManager().updatePlayerTeam(player);
            plugin.getTabManager().updateTabList(player);
        }

        // 4. Forçar atualização da scoreboard para todos
        updateScoreboardTeams(player, prefix, newName);

        // 5. Esconder e mostrar jogador para atualizar nametag (no final para garantir
        // que tudo foi configurado)
        refreshPlayerVisibility(player);
    }

    /**
     * Modifica o GameProfile do jogador via Reflection pura
     */
    private void modifyGameProfile(Player player, String newName) throws Exception {
        if (gameProfileNameField == null) {
            return; // Reflection não disponível
        }

        // Obter o EntityPlayer via reflexão
        Object entityPlayer = player.getClass().getMethod("getHandle").invoke(player);

        // Encontrar o GameProfile no EntityPlayer/EntityHuman
        Object gameProfile = null;

        // Subir na hierarquia de classes até encontrar o campo GameProfile
        Class<?> clazz = entityPlayer.getClass();
        while (clazz != null && gameProfile == null) {
            for (Field field : clazz.getDeclaredFields()) {
                // Verificar se o tipo do campo é GameProfile
                if (field.getType().getName().equals("com.mojang.authlib.GameProfile")) {
                    field.setAccessible(true);
                    gameProfile = field.get(entityPlayer);
                    break;
                }
            }
            clazz = clazz.getSuperclass();
        }

        if (gameProfile != null) {
            // Modificar o nome no GameProfile
            gameProfileNameField.set(gameProfile, newName);
        }
    }

    /**
     * Atualiza a visibilidade do jogador para todos (via NMS packets)
     * Isso força a atualização do nametag enviando destroy + spawn packets
     */
    private void refreshPlayerVisibility(Player player) {
        // Executar no próximo tick para garantir sincronização
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline())
                return;

            try {
                // Obter EntityPlayer
                Object entityPlayer = player.getClass().getMethod("getHandle").invoke(player);
                int entityId = (Integer) entityPlayer.getClass().getMethod("getId").invoke(entityPlayer);

                // Obter versão NMS
                String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];

                // Criar pacote de destroy
                Class<?> destroyPacketClass = Class
                        .forName("net.minecraft.server." + version + ".PacketPlayOutEntityDestroy");
                Object destroyPacket = destroyPacketClass.getConstructor(int[].class)
                        .newInstance(new int[] { entityId });

                // Classe para obter PlayerConnection
                Class<?> craftPlayerClass = Class.forName("org.bukkit.craftbukkit." + version + ".entity.CraftPlayer");

                // Enviar pacote de destroy para todos os outros jogadores
                for (Player online : Bukkit.getOnlinePlayers()) {
                    if (online.equals(player))
                        continue;

                    // Obter PlayerConnection
                    Object onlineHandle = craftPlayerClass.getMethod("getHandle").invoke(online);
                    Object playerConnection = onlineHandle.getClass().getField("playerConnection").get(onlineHandle);

                    // Enviar pacote destroy
                    playerConnection.getClass().getMethod("sendPacket",
                            Class.forName("net.minecraft.server." + version + ".Packet"))
                            .invoke(playerConnection, destroyPacket);
                }

                // Aguardar 2 ticks e enviar pacote de spawn
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (!player.isOnline())
                        return;

                    try {
                        // Criar pacote de spawn (NamedEntitySpawn)
                        Class<?> spawnPacketClass = Class
                                .forName("net.minecraft.server." + version + ".PacketPlayOutNamedEntitySpawn");
                        Object spawnPacket = spawnPacketClass.getConstructor(
                                Class.forName("net.minecraft.server." + version + ".EntityHuman"))
                                .newInstance(entityPlayer);

                        // Criar pacote de informações do jogador (PlayerInfo - para atualizar
                        // GameProfile)
                        Class<?> playerInfoClass = Class
                                .forName("net.minecraft.server." + version + ".PacketPlayOutPlayerInfo");
                        Class<?> enumActionClass = Class.forName(
                                "net.minecraft.server." + version + ".PacketPlayOutPlayerInfo$EnumPlayerInfoAction");
                        Object addPlayerAction = enumActionClass.getField("ADD_PLAYER").get(null);
                        Object playerInfoPacket;

                        // Tentar criar o pacote com o construtor correto
                        try {
                            playerInfoPacket = playerInfoClass.getConstructor(enumActionClass, Iterable.class)
                                    .newInstance(addPlayerAction, java.util.Collections.singletonList(entityPlayer));
                        } catch (Exception e) {
                            // Fallback para outro construtor
                            playerInfoPacket = playerInfoClass
                                    .getConstructor(enumActionClass,
                                            entityPlayer.getClass().getSuperclass().getSuperclass())
                                    .newInstance(addPlayerAction, entityPlayer);
                        }

                        for (Player online : Bukkit.getOnlinePlayers()) {
                            if (online.equals(player))
                                continue;

                            Object onlineHandle = craftPlayerClass.getMethod("getHandle").invoke(online);
                            Object playerConnection = onlineHandle.getClass().getField("playerConnection")
                                    .get(onlineHandle);

                            // Enviar PlayerInfo primeiro, depois Spawn
                            playerConnection.getClass().getMethod("sendPacket",
                                    Class.forName("net.minecraft.server." + version + ".Packet"))
                                    .invoke(playerConnection, playerInfoPacket);

                            playerConnection.getClass().getMethod("sendPacket",
                                    Class.forName("net.minecraft.server." + version + ".Packet"))
                                    .invoke(playerConnection, spawnPacket);

                            // Atualizar equipamento do jogador (para mostrar armadura)
                            refreshPlayerEquipment(player, online, version, craftPlayerClass, entityPlayer);
                        }

                    } catch (Exception e) {
                        // Fallback: usar hide/show padrão
                        plugin.getLogger().warning(
                                "[FakeNickManager] Erro ao enviar spawn packet, usando fallback: " + e.getMessage());
                        for (Player online : Bukkit.getOnlinePlayers()) {
                            if (online.equals(player))
                                continue;
                            online.showPlayer(player);
                        }
                    }
                }, 2L);

            } catch (Exception e) {
                // Fallback: usar método antigo de hide/show
                plugin.getLogger().warning(
                        "[FakeNickManager] Erro ao respawnar jogador via NMS, usando fallback: " + e.getMessage());
                for (Player online : Bukkit.getOnlinePlayers()) {
                    if (online.equals(player))
                        continue;
                    online.hidePlayer(player);
                }

                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    for (Player online : Bukkit.getOnlinePlayers()) {
                        if (online.equals(player))
                            continue;
                        online.showPlayer(player);
                    }
                }, 2L);
            }
        }, 1L);
    }

    /**
     * Reenvia os pacotes de equipamento do jogador
     */
    private void refreshPlayerEquipment(Player player, Player target, String version, Class<?> craftPlayerClass,
            Object entityPlayer) {
        try {
            int entityId = (Integer) entityPlayer.getClass().getMethod("getId").invoke(entityPlayer);

            // Obter PlayerConnection do target
            Object targetHandle = craftPlayerClass.getMethod("getHandle").invoke(target);
            Object playerConnection = targetHandle.getClass().getField("playerConnection").get(targetHandle);

            // Classe do pacote de equipamento
            Class<?> equipmentPacketClass = Class
                    .forName("net.minecraft.server." + version + ".PacketPlayOutEntityEquipment");
            Class<?> itemStackClass = Class.forName("net.minecraft.server." + version + ".ItemStack");
            Class<?> craftItemStackClass = Class
                    .forName("org.bukkit.craftbukkit." + version + ".inventory.CraftItemStack");

            // Slots: 0=mão, 1=boots, 2=leggings, 3=chestplate, 4=helmet
            org.bukkit.inventory.ItemStack[] equipment = {
                    player.getItemInHand(),
                    player.getInventory().getBoots(),
                    player.getInventory().getLeggings(),
                    player.getInventory().getChestplate(),
                    player.getInventory().getHelmet()
            };

            for (int slot = 0; slot < 5; slot++) {
                Object nmsItem = craftItemStackClass.getMethod("asNMSCopy", org.bukkit.inventory.ItemStack.class)
                        .invoke(null, equipment[slot]);

                Object equipmentPacket = equipmentPacketClass.getConstructor(int.class, int.class, itemStackClass)
                        .newInstance(entityId, slot, nmsItem);

                playerConnection.getClass().getMethod("sendPacket",
                        Class.forName("net.minecraft.server." + version + ".Packet"))
                        .invoke(playerConnection, equipmentPacket);
            }
        } catch (Exception e) {
            // Ignorar erros de equipamento - não é crítico
        }
    }

    /**
     * Atualiza os teams da scoreboard para o jogador com fake nick.
     * 
     * CORREÇÃO: Usa o fake nick como entry do Team, não o nome real.
     * O cliente associa o Team ao nome visível na TabList.
     */
    private void updateScoreboardTeams(Player player, String prefix, String fakeName) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();

        // Remover de teams antigos (ambos os nomes para limpeza completa)
        String realName = player.getName();
        for (Team team : scoreboard.getTeams()) {
            if (team.hasEntry(realName)) {
                team.removeEntry(realName);
            }
            if (team.hasEntry(fakeName)) {
                team.removeEntry(fakeName);
            }
        }

        // Obter grupo para ordenação
        Group group = plugin.getGroupManager().getPlayerGroup(player);
        int sortOrder = 9999 - (group != null ? group.getPriority() : 0);
        String teamName = String.format("%04d_%s", sortOrder, group != null ? group.getName() : "membro");

        if (teamName.length() > 16) {
            teamName = teamName.substring(0, 16);
        }

        // Criar ou obter team
        Team team = scoreboard.getTeam(teamName);
        if (team == null) {
            team = scoreboard.registerNewTeam(teamName);
        }

        // Configurar prefixo (limitado a 16 chars)
        if (prefix.length() > 16) {
            prefix = prefix.substring(0, 16);
        }
        team.setPrefix(prefix);

        // CORREÇÃO: Adicionar o FAKE NICK como entry, não o nome real
        // O cliente precisa que a entry corresponda ao nome visível na TabList
        team.addEntry(fakeName);
    }

    // ==================== VALIDAÇÃO ====================

    /**
     * Valida um nickname proposto
     * 
     * @param nickname       Nome a validar
     * @param requestingUUID UUID do jogador que está solicitando
     * @return Resultado da validação
     */
    public ValidationResult validateNickname(String nickname, UUID requestingUUID) {
        // 1. Validar formato (3-16 caracteres, apenas letras, números e _)
        if (!isValidMinecraftName(nickname)) {
            return new ValidationResult(false,
                    ChatStorage.getMessage("fake.invalid-format"));
        }

        // 2. Verificar se já está sendo usado por outro jogador online
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getUniqueId().equals(requestingUUID))
                continue;

            String displayName = getDisplayName(online);
            if (displayName.equalsIgnoreCase(nickname)) {
                return new ValidationResult(false,
                        ChatStorage.getMessage("fake.name-in-use"));
            }
        }

        // 3. Verificar se é um nome original de algum jogador do servidor
        for (Map.Entry<UUID, String> entry : originalNames.entrySet()) {
            if (entry.getKey().equals(requestingUUID))
                continue;
            if (entry.getValue().equalsIgnoreCase(nickname)) {
                return new ValidationResult(false,
                        ChatStorage.getMessage("fake.name-registered"));
            }
        }

        // 4. Verificar conta Premium (API Mojang) - verifica apenas em cache
        // Para evitar lag, a verificação completa é feita assincronamente
        if (isPremiumNameCached(nickname)) {
            return new ValidationResult(false,
                    ChatStorage.getMessage("fake.name-premium"));
        }

        return new ValidationResult(true, null);
    }

    /**
     * Verifica se o nome segue o padrão do Minecraft
     * 3-16 caracteres, apenas letras, números e underscore
     */
    private boolean isValidMinecraftName(String name) {
        if (name == null || name.length() < 3 || name.length() > 16) {
            return false;
        }
        return name.matches("^[a-zA-Z0-9_]+$");
    }

    /**
     * Verifica se um nome está no cache de nomes premium
     */
    private boolean isPremiumNameCached(String name) {
        return premiumNamesCache.contains(name.toLowerCase());
    }

    /**
     * Verifica assincronamente se um nome é premium (conta Mojang)
     * O resultado é cacheado para consultas futuras
     */
    public void checkPremiumAsync(String name, java.util.function.Consumer<Boolean> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean isPremium = checkMojangApi(name);
            if (isPremium) {
                premiumNamesCache.add(name.toLowerCase());
            }

            // Callback na thread principal
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(isPremium));
        });
    }

    /**
     * Consulta a API da Mojang para verificar se um nome existe
     */
    private boolean checkMojangApi(String name) {
        try {
            URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + name);
            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
            String response = reader.readLine();
            reader.close();

            // Se retornou algo, o nome existe
            return response != null && !response.isEmpty();
        } catch (Exception e) {
            // Erro de conexão ou nome não existe (404)
            return false;
        }
    }

    // ==================== UTILITÁRIOS ====================

    /**
     * Verifica se um jogador está usando fake nick
     */
    public boolean hasFakeNick(Player player) {
        return fakeNicks.containsKey(player.getUniqueId());
    }

    /**
     * Verifica se um jogador está usando fake nick por UUID
     */
    public boolean hasFakeNick(UUID uuid) {
        return fakeNicks.containsKey(uuid);
    }

    /**
     * Obtém o nome de exibição atual do jogador
     * Retorna o fake nick se estiver usando, senão o nome real
     */
    public String getDisplayName(Player player) {
        return getDisplayName(player.getUniqueId(), player.getName());
    }

    /**
     * Obtém o nome de exibição por UUID
     */
    public String getDisplayName(UUID uuid, String defaultName) {
        return fakeNicks.getOrDefault(uuid, defaultName);
    }

    /**
     * Obtém o nome original do jogador
     */
    public String getOriginalName(Player player) {
        return originalNames.getOrDefault(player.getUniqueId(), player.getName());
    }

    /**
     * Obtém o nome original por UUID
     */
    public String getOriginalName(UUID uuid) {
        return originalNames.get(uuid);
    }

    /**
     * Obtém o fake nick atual do jogador
     * 
     * @return Fake nick ou null se não estiver usando
     */
    public String getFakeNick(Player player) {
        return fakeNicks.get(player.getUniqueId());
    }

    /**
     * Obtém o fake nick por UUID
     */
    public String getFakeNick(UUID uuid) {
        return fakeNicks.get(uuid);
    }

    /**
     * Obtém o prefixo atual do jogador (apenas tag selecionada)
     * Quando usando fake nick, NÃO usa o prefixo do grupo automaticamente
     */
    public String getCurrentPrefix(Player player) {
        // 1. Verificar tag selecionada
        if (plugin.getTagManager() != null && plugin.getProfileManager() != null) {
            PlayerProfile profile = plugin.getProfileManager().getProfile(player);
            if (profile != null) {
                String savedTag = profile.getData().getSelectedTag();
                if (savedTag != null && !savedTag.isEmpty()) {
                    com.haumea.kitpvp.models.Tag tag = plugin.getTagManager().getTag(savedTag);
                    if (tag != null && player.hasPermission(tag.getPermission())) {
                        return ChatStorage.colorize(tag.getPrefix());
                    }
                }
            }
        }

        // 2. Se está com fake nick, NÃO usar prefixo do grupo (fica sem prefixo)
        if (hasFakeNick(player)) {
            return "§7"; // Apenas cor padrão, sem tag
        }

        // 3. Se NÃO está com fake nick, usar prefixo do grupo normalmente
        Group group = plugin.getGroupManager().getPlayerGroup(player);
        if (group != null && !group.getPrefix().isEmpty()) {
            return ChatStorage.colorize(group.getPrefix());
        }

        return "§7";
    }

    /**
     * Limpa o fake nick de um jogador ao deslogar
     * Chamado pelo PlayerListener
     */
    public void onPlayerQuit(Player player) {
        // Manter os dados por um tempo para caso o jogador reconecte
        // Os dados são mantidos na memória até limpeza manual ou restart
    }

    /**
     * Restaura o fake nick de um jogador ao logar
     * Se ele estava com fake antes de deslogar
     */
    public void onPlayerJoin(Player player) {
        UUID uuid = player.getUniqueId();

        if (fakeNicks.containsKey(uuid)) {
            String fakeName = fakeNicks.get(uuid);

            // Reaplicar fake nick após um delay
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                applyFakeNick(player, fakeName);
            }, 5L);
        }
    }

    // ==================== CLASSES INTERNAS ====================

    /**
     * Resultado de validação de nickname
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String message;

        public ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public boolean isValid() {
            return valid;
        }

        public String getMessage() {
            return message;
        }
    }
}
