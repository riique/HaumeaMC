package com.haumea.kitpvp.managers;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.utils.ChatStorage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Gerencia o sistema de seletor de servidores para o Lobby.
 * Usado apenas quando server-type = LOBBY no config.yml.
 * 
 * Inclui sistema de contagem de jogadores por servidor via BungeeCord.
 */
public class ServerSelectorManager implements PluginMessageListener {

    private final HaumeaMC plugin;

    // Configurações do item seletor
    private Material selectorMaterial;
    private String selectorName;
    private List<String> selectorLore;
    private int selectorSlot;

    // Lista de servidores disponíveis
    private final Map<String, ServerInfo> servers;

    // Cache de contagem de jogadores por servidor
    private final Map<String, Integer> playerCountCache;

    // Intervalo de atualização (em ticks) - 5 segundos = 100 ticks
    private static final long UPDATE_INTERVAL = 100L;

    // Task de atualização de contagem de jogadores
    private org.bukkit.scheduler.BukkitTask playerCountTask;

    public ServerSelectorManager(HaumeaMC plugin) {
        this.plugin = plugin;
        this.servers = new LinkedHashMap<>();
        this.playerCountCache = new HashMap<>();
        loadConfig();
        registerBungeeChannel();
        startPlayerCountUpdater();
    }

    /**
     * Carrega configurações do config.yml
     */
    private void loadConfig() {
        ConfigurationSection lobbySection = plugin.getConfig().getConfigurationSection("lobby");

        if (lobbySection == null) {
            // Valores padrão se não existir configuração
            this.selectorMaterial = Material.COMPASS;
            this.selectorName = "&a&lSELECIONAR SERVIDOR";
            this.selectorLore = new ArrayList<>();
            this.selectorLore.add("&7Clique para escolher um servidor!");
            this.selectorSlot = 4;

            // Servidor padrão
            servers.put("kitpvp", new ServerInfo(
                    "kitpvp",
                    "&c&lKitPvP",
                    Material.DIAMOND_SWORD,
                    0,
                    13,
                    "&7Servidor de PvP com kits!",
                    "&e&lCLIQUE PARA ENTRAR"));
            return;
        }

        // Carregar configurações do item seletor
        ConfigurationSection itemSection = lobbySection.getConfigurationSection("selector-item");
        if (itemSection != null) {
            String materialName = itemSection.getString("material", "COMPASS");
            try {
                this.selectorMaterial = Material.valueOf(materialName.toUpperCase());
            } catch (IllegalArgumentException e) {
                this.selectorMaterial = Material.COMPASS;
            }
            this.selectorName = itemSection.getString("name", "&a&lSELECIONAR SERVIDOR");
            this.selectorLore = itemSection.getStringList("lore");
            this.selectorSlot = itemSection.getInt("slot", 4);
        } else {
            this.selectorMaterial = Material.COMPASS;
            this.selectorName = "&a&lSELECIONAR SERVIDOR";
            this.selectorLore = new ArrayList<>();
            this.selectorSlot = 4;
        }

        // Carregar servidores
        ConfigurationSection serversSection = lobbySection.getConfigurationSection("servers");
        if (serversSection != null) {
            for (String serverId : serversSection.getKeys(false)) {
                ConfigurationSection serverSection = serversSection.getConfigurationSection(serverId);
                if (serverSection != null) {
                    String name = serverSection.getString("name", serverId);
                    String materialStr = serverSection.getString("material", "GRASS");
                    Material material;
                    try {
                        material = Material.valueOf(materialStr.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        material = Material.GRASS;
                    }
                    int data = serverSection.getInt("data", 0);
                    int slot = serverSection.getInt("slot", servers.size());

                    List<String> loreList = serverSection.getStringList("lore");
                    String lore = loreList.isEmpty() ? "" : String.join("\n", loreList);
                    String action = serverSection.getString("action", "&e&lCLIQUE PARA ENTRAR");

                    servers.put(serverId, new ServerInfo(serverId, name, material, data, slot, lore, action));

                    // Inicializar cache com 0
                    playerCountCache.put(serverId, 0);
                }
            }
        }

        // Se nenhum servidor configurado, adicionar padrão
        if (servers.isEmpty()) {
            servers.put("kitpvp", new ServerInfo(
                    "kitpvp",
                    "&c&lKitPvP",
                    Material.DIAMOND_SWORD,
                    0,
                    13,
                    "&7Servidor de PvP com kits!",
                    "&e&lCLIQUE PARA ENTRAR"));
            playerCountCache.put("kitpvp", 0);
        }
    }

    /**
     * Registra o canal de mensagens do BungeeCord
     */
    private void registerBungeeChannel() {
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, "BungeeCord");
        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, "BungeeCord", this);
    }

    /**
     * Inicia o atualizador de contagem de jogadores
     * Atualiza a cada 5 segundos
     */
    private void startPlayerCountUpdater() {
        playerCountTask = new BukkitRunnable() {
            @Override
            public void run() {
                requestPlayerCounts();
            }
        }.runTaskTimer(plugin, 20L, UPDATE_INTERVAL);
    }

    /**
     * Desliga o manager e cancela tasks
     * Deve ser chamado no onDisable() do plugin
     */
    public void shutdown() {
        if (playerCountTask != null) {
            playerCountTask.cancel();
            playerCountTask = null;
        }
        playerCountCache.clear();
    }

    /**
     * Solicita contagem de jogadores de todos os servidores
     */
    private void requestPlayerCounts() {
        // Precisa de pelo menos 1 jogador online para enviar plugin messages
        if (Bukkit.getOnlinePlayers().isEmpty()) {
            return;
        }

        Player player = Bukkit.getOnlinePlayers().iterator().next();

        for (String serverId : servers.keySet()) {
            requestPlayerCount(player, serverId);
        }
    }

    /**
     * Solicita contagem de jogadores de um servidor específico
     */
    private void requestPlayerCount(Player player, String serverName) {
        try {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(b);

            out.writeUTF("PlayerCount");
            out.writeUTF(serverName);

            player.sendPluginMessage(plugin, "BungeeCord", b.toByteArray());
        } catch (IOException e) {
            plugin.getLogger().warning("Erro ao solicitar contagem de jogadores: " + e.getMessage());
        }
    }

    /**
     * Recebe mensagens do BungeeCord (contagem de jogadores)
     */
    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals("BungeeCord")) {
            return;
        }

        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(message));
            String subchannel = in.readUTF();

            if (subchannel.equals("PlayerCount")) {
                String serverName = in.readUTF();
                int playerCount = in.readInt();

                // Atualizar cache
                playerCountCache.put(serverName, playerCount);
            }
        } catch (IOException e) {
            // Ignorar erros de leitura (pode ser outro tipo de mensagem)
        }
    }

    /**
     * Obtém a contagem de jogadores de um servidor (do cache)
     * 
     * @param serverName Nome do servidor
     * @return Número de jogadores online no servidor
     */
    public int getPlayerCount(String serverName) {
        return playerCountCache.getOrDefault(serverName, 0);
    }

    /**
     * Obtém o mapa completo de contagens de jogadores
     */
    public Map<String, Integer> getPlayerCounts() {
        return new HashMap<>(playerCountCache);
    }

    /**
     * Cria o item seletor de servidores
     */
    public ItemStack createSelectorItem() {
        ItemStack item = new ItemStack(selectorMaterial);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatStorage.colorize(selectorName));

        List<String> coloredLore = new ArrayList<>();
        for (String line : selectorLore) {
            coloredLore.add(ChatStorage.colorize(line));
        }
        meta.setLore(coloredLore);

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Dá o item seletor ao jogador
     */
    public void giveSelector(Player player) {
        player.getInventory().setItem(selectorSlot, createSelectorItem());
    }

    /**
     * Verifica se um item é o seletor de servidores
     */
    public boolean isSelectorItem(ItemStack item) {
        if (item == null || item.getType() != selectorMaterial) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return false;
        }

        return meta.getDisplayName().equals(ChatStorage.colorize(selectorName));
    }

    /**
     * Envia o jogador para outro servidor via BungeeCord
     */
    public void sendToServer(Player player, String serverName) {
        try {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(b);

            out.writeUTF("Connect");
            out.writeUTF(serverName);

            player.sendPluginMessage(plugin, "BungeeCord", b.toByteArray());

            ChatStorage.send(player, "lobby.server-selector.connecting", "server", serverName);
        } catch (IOException e) {
            ChatStorage.sendRaw(player, "&c&lERRO! &7Não foi possível conectar ao servidor.");
            plugin.getLogger().warning("Erro ao enviar jogador para servidor: " + e.getMessage());
        }
    }

    /**
     * Obtém a lista de servidores configurados
     */
    public Map<String, ServerInfo> getServers() {
        return servers;
    }

    /**
     * Obtém um servidor pelo ID
     */
    public ServerInfo getServer(String id) {
        return servers.get(id);
    }

    /**
     * Obtém o slot do item seletor na hotbar
     */
    public int getSelectorSlot() {
        return selectorSlot;
    }

    /**
     * Classe interna para armazenar informações de um servidor
     */
    public static class ServerInfo {
        private final String id;
        private final String displayName;
        private final Material material;
        private final int data;
        private final int slot;
        private final String description;
        private final String action;

        public ServerInfo(String id, String displayName, Material material, int data, int slot,
                String description, String action) {
            this.id = id;
            this.displayName = displayName;
            this.material = material;
            this.data = data;
            this.slot = slot;
            this.description = description;
            this.action = action;
        }

        public String getId() {
            return id;
        }

        public String getDisplayName() {
            return displayName;
        }

        public Material getMaterial() {
            return material;
        }

        public int getData() {
            return data;
        }

        public int getSlot() {
            return slot;
        }

        public String getDescription() {
            return description;
        }

        public String getAction() {
            return action;
        }
    }
}
