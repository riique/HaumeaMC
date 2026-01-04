package com.haumea.kitpvp.managers;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.models.Kit;
import com.haumea.kitpvp.profile.PlayerProfile;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Gerenciador de Kits do servidor KitPvP.
 * 
 * Responsável por:
 * - Carregar e salvar kits do arquivo YAML
 * - Gerenciar seleção de kits primário e secundário
 * - Verificar permissões e incompatibilidades
 * - Sistema de aluguel de kits com coins
 * - Gerenciar permissões temporárias de kits
 * 
 * @author HaumeaMC
 */
public class KitManager {

    private final HaumeaMC plugin;
    private File kitsFile;
    private FileConfiguration kitsConfig;

    // Map de kits disponíveis (nome -> Kit)
    private final Map<String, Kit> kits;

    // Flag para lazy loading
    private volatile boolean kitsLoaded = false;

    public KitManager(HaumeaMC plugin) {
        this.plugin = plugin;
        this.kits = new LinkedHashMap<>();

        // NÃO carrega kits aqui - será lazy loaded
        // loadKitsFile();
        // loadKits();

        // Task para verificar expiração de aluguéis continua
        startExpirationTask();

        plugin.getLogger().info("[KitManager] Inicializado (lazy loading ativado)");
    }

    /**
     * Garante que os kits estão carregados (lazy loading)
     */
    private void ensureLoaded() {
        if (!kitsLoaded) {
            synchronized (kits) {
                if (!kitsLoaded) {
                    loadKitsFile();
                    loadKitsInternal();
                    kitsLoaded = true;
                }
            }
        }
    }

    // ==================== CARREGAMENTO ====================

    /**
     * Carrega o arquivo kits.yml
     */
    private void loadKitsFile() {
        kitsFile = new File(plugin.getDataFolder(), "kits.yml");

        if (!kitsFile.exists()) {
            plugin.saveResource("kits.yml", false);
        }

        kitsConfig = YamlConfiguration.loadConfiguration(kitsFile);
    }

    /**
     * Carrega todos os kits do arquivo.
     * Chamado publicamente para forçar reload.
     */
    public void loadKits() {
        synchronized (kits) {
            loadKitsFile();
            loadKitsInternal();
            kitsLoaded = true;
        }
    }

    /**
     * Carrega kits internamente
     */
    private void loadKitsInternal() {
        kits.clear();

        ConfigurationSection section = kitsConfig.getConfigurationSection("kits");
        if (section == null) {
            // Criar kits padrão se não existir
            createDefaultKits();
            return;
        }

        for (String kitName : section.getKeys(false)) {
            String path = "kits." + kitName;

            String displayName = kitsConfig.getString(path + ".display", "§a" + kitName);
            String iconStr = kitsConfig.getString(path + ".icon", "CHEST");
            short iconData = (short) kitsConfig.getInt(path + ".icon-data", 0);
            List<String> description = kitsConfig.getStringList(path + ".description");
            int price = kitsConfig.getInt(path + ".price", 0);
            String permission = kitsConfig.getString(path + ".permission", "haumea.kit." + kitName.toLowerCase());
            List<String> incompatible = kitsConfig.getStringList(path + ".incompatible");
            boolean isDefault = kitsConfig.getBoolean(path + ".default", false);

            Material icon;
            try {
                icon = Material.valueOf(iconStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                icon = Material.CHEST;
            }

            Kit kit = new Kit(kitName, displayName, icon, iconData, description,
                    price, permission, incompatible, isDefault);
            kits.put(kitName.toLowerCase(), kit);
        }

        plugin.getLogger().info("[KitManager] " + kits.size() + " kits carregados.");
    }

    /**
     * Cria kits padrão para o servidor
     */
    private void createDefaultKits() {
        // Kit PvP (padrão)
        kits.put("pvp", Kit.builder("pvp")
                .displayName("§a§lPvP")
                .icon(Material.STONE_SWORD)
                .description("§7Kit básico de combate.", "", "§eNenhuma habilidade especial.")
                .price(0)
                .isDefault(true)
                .build());

        // Kit Ninja
        kits.put("ninja", Kit.builder("ninja")
                .displayName("§a§lNinja")
                .icon(Material.FEATHER)
                .description("§7Mestre das sombras!", "", "§eHabilidade: §fLança ender pearls", "§epara se teleportar.")
                .price(500)
                .incompatibleWith("stomper", "anchor")
                .build());

        // Kit Stomper
        kits.put("stomper", Kit.builder("stomper")
                .displayName("§a§lStomper")
                .icon(Material.DIAMOND_BOOTS)
                .description("§7Queda devastadora!", "", "§eHabilidade: §fCausa dano de queda",
                        "§enos inimigos próximos.")
                .price(500)
                .incompatibleWith("ninja", "fireman")
                .build());

        // Kit Fisherman
        kits.put("fisherman", Kit.builder("fisherman")
                .displayName("§a§lFisherman")
                .icon(Material.FISHING_ROD)
                .description("§7Pescador mortal!", "", "§eHabilidade: §fPuxa inimigos", "§ecom a vara de pesca.")
                .price(400)
                .build());

        // Kit Kangaroo
        kits.put("kangaroo", Kit.builder("kangaroo")
                .displayName("§a§lKangaroo")
                .icon(Material.FIREWORK)
                .description("§7Saltos incríveis!", "", "§eHabilidade: §fPula alto", "§ecomo um canguru.")
                .price(350)
                .incompatibleWith("anchor")
                .build());

        // Kit Anchor
        kits.put("anchor", Kit.builder("anchor")
                .displayName("§a§lAnchor")
                .icon(Material.ANVIL)
                .description("§7Imóvel como uma âncora!", "", "§eHabilidade: §fNão sofre knockback",
                        "§enem dano de queda.")
                .price(400)
                .incompatibleWith("kangaroo", "ninja")
                .build());

        // Kit Fireman
        kits.put("fireman", Kit.builder("fireman")
                .displayName("§a§lFireman")
                .icon(Material.WATER_BUCKET)
                .description("§7Apaga qualquer fogo!", "", "§eHabilidade: §fImunidade a fogo", "§ee lava.")
                .price(300)
                .incompatibleWith("stomper")
                .build());

        // Kit Gladiator
        kits.put("gladiator", Kit.builder("gladiator")
                .displayName("§a§lGladiator")
                .icon(Material.IRON_FENCE)
                .description("§7Arena pessoal!", "", "§eHabilidade: §fCria uma arena", "§ecom o inimigo.")
                .price(600)
                .build());

        // Kit Thor
        kits.put("thor", Kit.builder("thor")
                .displayName("§a§lThor")
                .icon(Material.GOLD_AXE)
                .description("§7O deus do trovão!", "", "§eHabilidade: §fInvoca raios", "§enos inimigos.")
                .price(700)
                .build());

        // Kit Viking
        kits.put("viking", Kit.builder("viking")
                .displayName("§a§lViking")
                .icon(Material.IRON_AXE)
                .description("§7Guerreiro nórdico!", "", "§eHabilidade: §fMachado poderoso", "§eque causa sangramento.")
                .price(450)
                .build());

        // Kit Phantom
        kits.put("phantom", Kit.builder("phantom")
                .displayName("§a§lPhantom")
                .icon(Material.GHAST_TEAR)
                .description("§7Espírito invisível!", "", "§eHabilidade: §fFica invisível", "§epor alguns segundos.")
                .price(550)
                .build());

        // Kit Turtle
        kits.put("turtle", Kit.builder("turtle")
                .displayName("§a§lTurtle")
                .icon(Material.MOSSY_COBBLESTONE)
                .description("§7Defesa máxima!", "", "§eHabilidade: §fEntra no casco", "§ee fica imune a dano.")
                .price(400)
                .build());

        // Salvar os kits criados
        saveKits();
        plugin.getLogger().info("Criados " + kits.size() + " kits padrão.");
    }

    /**
     * Chave de customData para kits alugados
     * Formato: Map<String, Long> (kitName -> expirationTimestamp)
     */
    private static final String DATA_KEY_RENTED_KITS = "rented_kits";

    // ==================== SALVAMENTO ====================

    /**
     * Salva todos os kits no arquivo
     */
    public void saveKits() {
        for (Kit kit : kits.values()) {
            String path = "kits." + kit.getName();

            kitsConfig.set(path + ".display", kit.getDisplayName());
            kitsConfig.set(path + ".icon", kit.getIcon().name());
            kitsConfig.set(path + ".icon-data", kit.getIconData());
            kitsConfig.set(path + ".description", kit.getDescription());
            kitsConfig.set(path + ".price", kit.getPrice());
            kitsConfig.set(path + ".permission", kit.getPermission());
            kitsConfig.set(path + ".incompatible", kit.getIncompatibleKits());
            kitsConfig.set(path + ".default", kit.isDefault());
        }

        saveFile();
    }

    /**
     * Salva o arquivo no disco
     */
    private void saveFile() {
        try {
            kitsConfig.save(kitsFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ==================== GETTERS DE KITS ====================

    /**
     * Obtém um kit pelo nome
     * 
     * @param name Nome do kit
     * @return Kit ou null se não existir
     */
    public Kit getKit(String name) {
        ensureLoaded();
        if (name == null)
            return null;
        return kits.get(name.toLowerCase());
    }

    /**
     * Verifica se um kit existe
     */
    public boolean kitExists(String name) {
        ensureLoaded();
        return name != null && kits.containsKey(name.toLowerCase());
    }

    /**
     * Obtém todos os kits
     */
    public Collection<Kit> getAllKits() {
        ensureLoaded();
        return kits.values();
    }

    /**
     * Obtém kits que o jogador pode usar (tem permissão)
     */
    public List<Kit> getAvailableKits(Player player) {
        List<Kit> available = new ArrayList<>();
        for (Kit kit : kits.values()) {
            if (hasKitPermission(player, kit)) {
                available.add(kit);
            }
        }
        return available;
    }

    // ==================== PERMISSÕES ====================

    /**
     * Verifica se o jogador tem permissão para usar um kit
     */
    public boolean hasKitPermission(Player player, Kit kit) {
        if (kit.isDefault())
            return true;

        // Verificar permissão via Bukkit
        if (player.hasPermission(kit.getPermission()))
            return true;

        // Verificar se alugou o kit
        return hasRentedKit(player.getUniqueId(), kit.getName());
    }

    /**
     * Verifica se o jogador tem permissão para usar um kit pelo nome
     */
    public boolean hasKitPermission(Player player, String kitName) {
        Kit kit = getKit(kitName);
        if (kit == null)
            return false;
        return hasKitPermission(player, kit);
    }

    /**
     * Verifica se o jogador alugou um kit (usando customData persistido)
     */
    public boolean hasRentedKit(UUID uuid, String kitName) {
        Player player = plugin.getServer().getPlayer(uuid);
        if (player == null)
            return false;

        PlayerProfile profile = plugin.getProfileManager().getProfile(player);
        if (profile == null)
            return false;

        Object rentedObj = profile.getData().getCustomData(DATA_KEY_RENTED_KITS);
        if (!(rentedObj instanceof Map))
            return false;

        Map<?, ?> rentals = (Map<?, ?>) rentedObj;
        Object expirationObj = rentals.get(kitName.toLowerCase());
        if (expirationObj == null)
            return false;

        long expiration;
        if (expirationObj instanceof Number) {
            expiration = ((Number) expirationObj).longValue();
        } else {
            return false;
        }

        // 0 = permanente, ou verifica se não expirou
        return expiration == 0 || expiration > System.currentTimeMillis();
    }

    /**
     * Obtém os kits alugados de um jogador
     */
    @SuppressWarnings("unchecked")
    private Map<String, Long> getRentedKitsMap(Player player) {
        PlayerProfile profile = plugin.getProfileManager().getProfile(player);
        if (profile == null)
            return new HashMap<>();

        Object rentedObj = profile.getData().getCustomData(DATA_KEY_RENTED_KITS);
        if (rentedObj instanceof Map) {
            Map<String, Long> result = new HashMap<>();
            Map<?, ?> rawMap = (Map<?, ?>) rentedObj;
            for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                String key = entry.getKey().toString();
                long value = entry.getValue() instanceof Number ? ((Number) entry.getValue()).longValue() : 0L;
                result.put(key, value);
            }
            return result;
        }
        return new HashMap<>();
    }

    /**
     * Salva mapa de kits alugados no customData
     */
    private void saveRentedKitsMap(Player player, Map<String, Long> rentals) {
        PlayerProfile profile = plugin.getProfileManager().getProfile(player);
        if (profile == null)
            return;

        if (rentals.isEmpty()) {
            profile.getData().removeCustomData(DATA_KEY_RENTED_KITS);
        } else {
            profile.getData().setCustomData(DATA_KEY_RENTED_KITS, rentals);
        }
    }

    // ==================== SELEÇÃO DE KITS ====================

    /**
     * Obtém o kit primário selecionado pelo jogador
     */
    public String getPrimaryKit(Player player) {
        PlayerProfile profile = plugin.getProfileManager().getProfile(player);
        if (profile == null)
            return null;

        String kit = profile.getData().getCustomData("primary_kit", String.class);
        return kit;
    }

    /**
     * Obtém o kit secundário selecionado pelo jogador
     */
    public String getSecondaryKit(Player player) {
        PlayerProfile profile = plugin.getProfileManager().getProfile(player);
        if (profile == null)
            return null;

        String kit = profile.getData().getCustomData("secondary_kit", String.class);
        return kit;
    }

    /**
     * Define o kit primário do jogador
     * 
     * @return true se bem sucedido
     */
    public boolean setPrimaryKit(Player player, String kitName) {
        PlayerProfile profile = plugin.getProfileManager().getProfile(player);
        if (profile == null)
            return false;

        if (kitName == null || kitName.isEmpty()) {
            profile.getData().setCustomData("primary_kit", null);
        } else {
            profile.getData().setCustomData("primary_kit", kitName.toLowerCase());
        }
        return true;
    }

    /**
     * Define o kit secundário do jogador
     * 
     * @return true se bem sucedido
     */
    public boolean setSecondaryKit(Player player, String kitName) {
        PlayerProfile profile = plugin.getProfileManager().getProfile(player);
        if (profile == null)
            return false;

        if (kitName == null || kitName.isEmpty()) {
            profile.getData().setCustomData("secondary_kit", null);
        } else {
            profile.getData().setCustomData("secondary_kit", kitName.toLowerCase());
        }
        return true;
    }

    // ==================== COMPATIBILIDADE ====================

    /**
     * Verifica se dois kits são compatíveis entre si
     */
    public boolean areKitsCompatible(String kit1, String kit2) {
        if (kit1 == null || kit2 == null)
            return true;
        if (kit1.equalsIgnoreCase(kit2))
            return false; // Mesmo kit

        Kit k1 = getKit(kit1);
        Kit k2 = getKit(kit2);

        if (k1 == null || k2 == null)
            return true;

        // Verificar em ambas as direções
        return !k1.isIncompatibleWith(kit2) && !k2.isIncompatibleWith(kit1);
    }

    /**
     * Verifica se um kit é compatível com os kits selecionados do jogador
     * 
     * @param player    Jogador
     * @param kitName   Kit a verificar
     * @param isPrimary Se está selecionando como kit primário (verifica secundário)
     *                  ou vice-versa
     */
    public boolean isKitCompatibleWithSelection(Player player, String kitName, boolean isPrimary) {
        String otherKit = isPrimary ? getSecondaryKit(player) : getPrimaryKit(player);
        return areKitsCompatible(kitName, otherKit);
    }

    // ==================== ALUGUEL DE KITS ====================

    /**
     * Aluga um kit para o jogador
     * 
     * @param player       Jogador
     * @param kitName      Nome do kit
     * @param durationDays Duração em dias (0 = permanente)
     * @param deductCoins  Se deve descontar coins
     * @return true se bem sucedido
     */
    public boolean rentKit(Player player, String kitName, int durationDays, boolean deductCoins) {
        Kit kit = getKit(kitName);
        if (kit == null)
            return false;

        PlayerProfile profile = plugin.getProfileManager().getProfile(player);
        if (profile == null)
            return false;

        // Verificar coins se necessário
        if (deductCoins && kit.getPrice() > 0) {
            if (profile.getCoins() < kit.getPrice()) {
                return false;
            }

            // Descontar coins
            profile.removeCoins(kit.getPrice());
        }

        // Calcular expiração
        long expiration = 0;
        if (durationDays > 0) {
            expiration = System.currentTimeMillis() + (durationDays * 24L * 60L * 60L * 1000L);
        }

        // Obter mapa atual e adicionar novo aluguel
        Map<String, Long> rentals = getRentedKitsMap(player);
        rentals.put(kitName.toLowerCase(), expiration);

        // Salvar no customData
        saveRentedKitsMap(player, rentals);

        return true;
    }

    /**
     * Remove o aluguel de um kit
     */
    public void removeRentedKit(UUID uuid, String kitName) {
        Player player = plugin.getServer().getPlayer(uuid);
        if (player == null)
            return;

        Map<String, Long> rentals = getRentedKitsMap(player);
        if (rentals.containsKey(kitName.toLowerCase())) {
            rentals.remove(kitName.toLowerCase());
            saveRentedKitsMap(player, rentals);
        }
    }

    /**
     * Obtém o tempo restante de aluguel de um kit
     * 
     * @return tempo em milissegundos, 0 = permanente, -1 = não alugado/expirado
     */
    public long getRentalTimeRemaining(UUID uuid, String kitName) {
        Player player = plugin.getServer().getPlayer(uuid);
        if (player == null)
            return -1;

        Map<String, Long> rentals = getRentedKitsMap(player);
        Long expiration = rentals.get(kitName.toLowerCase());
        if (expiration == null)
            return -1;

        if (expiration == 0)
            return 0; // Permanente

        long remaining = expiration - System.currentTimeMillis();
        return remaining > 0 ? remaining : -1;
    }

    // ==================== EXPIRAÇÃO ====================

    /**
     * Inicia task para verificar expiração de aluguéis (para jogadores online)
     */
    private void startExpirationTask() {
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            long now = System.currentTimeMillis();

            for (Player player : plugin.getServer().getOnlinePlayers()) {
                Map<String, Long> rentals = getRentedKitsMap(player);
                boolean changed = false;

                Iterator<Map.Entry<String, Long>> iterator = rentals.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<String, Long> entry = iterator.next();
                    long exp = entry.getValue();
                    if (exp > 0 && exp <= now) {
                        iterator.remove();
                        changed = true;
                    }
                }

                if (changed) {
                    saveRentedKitsMap(player, rentals);
                }
            }
        }, 20L * 60L, 20L * 60L); // A cada 1 minuto
    }

    /**
     * Recarrega os kits
     */
    public void reload() {
        loadKitsFile();
        loadKits();
    }
}
