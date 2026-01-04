package com.haumea.kitpvp.managers;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.models.PlayerData;
import com.haumea.kitpvp.models.cosmetic.*;
import com.haumea.kitpvp.profile.PlayerProfile;
import com.haumea.kitpvp.utils.ChatStorage;
import com.haumea.kitpvp.utils.VisualManager;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Gerenciador Central de Cosméticos
 * 
 * Responsável por:
 * - Registrar e gerenciar todos os cosméticos disponíveis
 * - Processar compras e desbloqueios
 * - Ativar/desativar cosméticos de jogadores
 * - Integrar com o sistema de combate para aplicar efeitos
 * - Persistir dados no PlayerData via customData
 * 
 * Chaves no customData:
 * - cosmetics_unlocked: List<String> (IDs dos cosméticos desbloqueados)
 * - cosmetic_kill_effect: String (ID do efeito de kill selecionado)
 * - cosmetic_kill_sound: String (ID do som de kill selecionado)
 * - cosmetic_kill_message: String (ID da mensagem de kill selecionada)
 * - cosmetics_enabled: Boolean (toggle geral)
 * 
 * @author HaumeaMC
 */
public class CosmeticManager {

    private final HaumeaMC plugin;

    // Registro de todos os cosméticos disponíveis
    private final Map<String, Cosmetic> allCosmetics;
    private final Map<CosmeticType, List<Cosmetic>> cosmeticsByType;

    // Cache de cosméticos ativos por jogador (para performance)
    private final Map<UUID, PlayerCosmeticData> playerCache;

    // Chaves do customData
    private static final String KEY_UNLOCKED = "cosmetics_unlocked";
    private static final String KEY_KILL_EFFECT = "cosmetic_kill_effect";
    private static final String KEY_KILL_SOUND = "cosmetic_kill_sound";
    private static final String KEY_KILL_MESSAGE = "cosmetic_kill_message";
    private static final String KEY_ENABLED = "cosmetics_enabled";

    public CosmeticManager(HaumeaMC plugin) {
        this.plugin = plugin;
        this.allCosmetics = new LinkedHashMap<>();
        this.cosmeticsByType = new EnumMap<>(CosmeticType.class);
        this.playerCache = new HashMap<>();

        // Inicializar listas por tipo
        for (CosmeticType type : CosmeticType.values()) {
            cosmeticsByType.put(type, new ArrayList<>());
        }

        // Registrar todos os cosméticos
        registerAllCosmetics();

        plugin.getLogger().info("[Cosmetics] Sistema de cosmeticos inicializado com " +
                allCosmetics.size() + " itens!");
    }

    // ==================== REGISTRO DE COSMÉTICOS ====================

    /**
     * Registra todos os cosméticos disponíveis no servidor.
     * Chamado durante a inicialização.
     */
    private void registerAllCosmetics() {
        // ========== EFEITOS DE KILL (Partículas) ==========

        // Comum
        registerCosmetic(new KillEffect(
                "kill_smoke",
                "Fumaca Sombria",
                new String[] { "&7Uma nuvem de fumaca", "&7aparece no local da morte." },
                CosmeticRarity.COMMON, Material.INK_SACK, 1000,
                "SMOKE", 25, 1.5f));

        registerCosmetic(new KillEffect(
                "kill_hearts",
                "Coracoes Partidos",
                new String[] { "&cCoracoes flutuam", "&cda vitima derrotada." },
                CosmeticRarity.COMMON, Material.APPLE, 1500,
                "HEART", 15, 1.0f));

        // Incomum
        registerCosmetic(new KillEffect(
                "kill_flames",
                "Chamas Infernais",
                new String[] { "&6Chamas envolvem", "&6a vitima ao morrer." },
                CosmeticRarity.UNCOMMON, Material.BLAZE_POWDER, 2500,
                "FLAME", 40, 1.5f));

        registerCosmetic(new KillEffect(
                "kill_portal",
                "Portal Dimensional",
                new String[] { "&5Particulas de portal", "&5transportam a alma." },
                CosmeticRarity.UNCOMMON, Material.ENDER_PEARL, 3000,
                "PORTAL", 50, 2.0f));

        // Raro
        registerCosmetic(new KillEffect(
                "kill_enchant",
                "Encantamento Arcano",
                new String[] { "&bParticulas magicas", "&bde encantamento surgem." },
                CosmeticRarity.RARE, Material.ENCHANTED_BOOK, 5000,
                "ENCHANT", 60, 2.0f));

        registerCosmetic(new KillEffect(
                "kill_explosion",
                "Explosao Epica",
                new String[] { "&4Uma explosao massiva", "&4marca sua vitoria!" },
                CosmeticRarity.RARE, Material.TNT, 6000,
                "EXPLOSION", 1, 0f));

        registerCosmetic(new KillEffect(
                "kill_witch",
                "Maldicao da Bruxa",
                new String[] { "&dParticulas de magia negra", "&denvolve a vitima." },
                CosmeticRarity.RARE, Material.FERMENTED_SPIDER_EYE, 5500,
                "WITCH", 40, 1.5f));

        // Épico
        registerCosmetic(new KillEffect(
                "kill_crit",
                "Dano Critico",
                new String[] { "&eEstrelas de dano critico", "&eexplodem no local!" },
                CosmeticRarity.EPIC, Material.NETHER_STAR, 10000,
                "CRIT", 50, 2.0f));

        registerCosmetic(new KillEffect(
                "kill_magic_crit",
                "Critico Magico",
                new String[] { "&dCriticos encantados", "&dcobrem a area!" },
                CosmeticRarity.EPIC, Material.DIAMOND_SWORD, 12000,
                "MAGIC_CRIT", 50, 2.0f));

        registerCosmetic(new KillEffect(
                "kill_lava",
                "Erupcao Vulcanica",
                new String[] { "&6Lava irrompe do chao", "&6onde a vitima caiu!" },
                CosmeticRarity.EPIC, Material.LAVA_BUCKET, 11000,
                "LAVA", 30, 1.5f));

        // Lendário
        registerCosmetic(new KillEffect(
                "kill_happy",
                "Celebracao Verde",
                new String[] { "&aParticulas de alegria", "&acelebram sua vitoria!" },
                CosmeticRarity.LEGENDARY, Material.EMERALD, 25000,
                "VILLAGER_HAPPY", 80, 3.0f));

        registerCosmetic(new KillEffect(
                "kill_water",
                "Tempestade Aquatica",
                new String[] { "&9Gotas de agua formam", "&9uma tempestade ao redor!" },
                CosmeticRarity.LEGENDARY, Material.WATER_BUCKET, 30000,
                "WATER", 60, 2.5f));

        // Mítico
        registerCosmetic(new KillEffect(
                "kill_combo",
                "Combo Supremo",
                new String[] { "&c&lTODOS os efeitos", "&c&lcombinados em um!" },
                CosmeticRarity.MYTHIC, Material.BEACON, 75000,
                "FLAME", 100, 3.0f // Base, o apply é customizado
        ) {
            @Override
            public void apply(Player killer, Player victim) {
                super.apply(killer, victim);
                // Adicionar mais efeitos para o combo
                org.bukkit.Location loc = victim.getLocation().add(0, 1, 0);
                org.bukkit.World world = loc.getWorld();

                // Explosão central
                world.playEffect(loc, Effect.EXPLOSION_LARGE, 0);

                // Anel de partículas
                for (int i = 0; i < 20; i++) {
                    double angle = Math.PI * 2 * i / 20;
                    double x = Math.cos(angle) * 2.0;
                    double z = Math.sin(angle) * 2.0;
                    world.playEffect(loc.clone().add(x, 0, z), Effect.MAGIC_CRIT, 0);
                    world.playEffect(loc.clone().add(x, 0.5, z), Effect.WITCH_MAGIC, 0);
                }

                // Som épico
                killer.playSound(victim.getLocation(), Sound.WITHER_DEATH, 0.5f, 1.5f);
            }
        });

        // ========== SONS DE KILL ==========

        // Comum
        registerCosmetic(new KillSound(
                "sound_pling",
                "Pling!",
                new String[] { "&eSom classico de", "&enota musical." },
                CosmeticRarity.COMMON, Material.NOTE_BLOCK, 800,
                Sound.NOTE_PLING, 1.0f, 1.5f));

        registerCosmetic(new KillSound(
                "sound_orb",
                "Experiencia",
                new String[] { "&aSom de coletar", "&aexperiencia." },
                CosmeticRarity.COMMON, Material.EXP_BOTTLE, 800,
                Sound.ORB_PICKUP, 1.0f, 1.0f));

        // Incomum
        registerCosmetic(new KillSound(
                "sound_levelup",
                "Level Up!",
                new String[] { "&bSom de subir de nivel!", "&bCelebre sua vitoria!" },
                CosmeticRarity.UNCOMMON, Material.BOOK, 2000,
                Sound.LEVEL_UP, 1.0f, 1.2f));

        registerCosmetic(new KillSound(
                "sound_anvil",
                "Bigorna",
                new String[] { "&8Som pesado de", "&8bigorna caindo." },
                CosmeticRarity.UNCOMMON, Material.ANVIL, 2500,
                Sound.ANVIL_LAND, 0.8f, 0.8f));

        // Raro
        registerCosmetic(new KillSound(
                "sound_thunder",
                "Trovao",
                new String[] { "&9Um trovao poderoso", "&9ecoa pelo servidor!", "", "&7Outros jogadores proximos",
                        "&7tambem ouvem!" },
                CosmeticRarity.RARE, Material.GOLD_SWORD, 6000,
                Sound.AMBIENCE_THUNDER, 2.0f, 1.0f, true, 50));

        registerCosmetic(new KillSound(
                "sound_blaze",
                "Chama do Blaze",
                new String[] { "&6O grito de um Blaze", "&6marca sua kill!" },
                CosmeticRarity.RARE, Material.BLAZE_ROD, 5000,
                Sound.BLAZE_DEATH, 1.0f, 0.7f));

        // Épico
        registerCosmetic(new KillSound(
                "sound_wither",
                "Spawn do Wither",
                new String[] { "&5O som aterrorizante", "&5do Wither surgindo!" },
                CosmeticRarity.EPIC, Material.SKULL_ITEM, 12000,
                Sound.WITHER_SPAWN, 0.6f, 1.0f, true, 30));

        registerCosmetic(new KillSound(
                "sound_enderdragon",
                "Rugido do Dragao",
                new String[] { "&dO Ender Dragon", "&druge em sua honra!" },
                CosmeticRarity.EPIC, Material.DRAGON_EGG, 15000,
                Sound.ENDERDRAGON_GROWL, 0.8f, 0.8f, true, 40));

        // Lendário
        registerCosmetic(new KillSound(
                "sound_firework",
                "Fogos de Artificio",
                new String[] { "&e&lFogos de artificio", "&e&lcelebram sua kill!", "", "&7Som ouvido por todos!" },
                CosmeticRarity.LEGENDARY, Material.FIREWORK, 25000,
                Sound.FIREWORK_BLAST, 1.0f, 1.0f, true, 60));

        // ========== MENSAGENS DE KILL ==========

        // Comum
        registerCosmetic(new KillMessage(
                "msg_classic",
                "Classica",
                new String[] { "&7Mensagem simples", "&7e elegante." },
                CosmeticRarity.COMMON, Material.PAPER, 500,
                "&8[&c&lKILL&8] &f{killer} &7eliminou &f{victim}",
                false));

        registerCosmetic(new KillMessage(
                "msg_cool",
                "Descolada",
                new String[] { "&bMensagem moderna", "&bcom estilo." },
                CosmeticRarity.COMMON, Material.BOOK_AND_QUILL, 700,
                "&b>> &f{killer} &edetonou &f{victim} &b<<",
                false));

        // Incomum
        registerCosmetic(new KillMessage(
                "msg_brutal",
                "Brutal",
                new String[] { "&4Mensagem agressiva", "&4para intimidar!" },
                CosmeticRarity.UNCOMMON, Material.IRON_SWORD, 2000,
                "&4&l[BRUTAL] &c{killer} &4DESTRUIU &c{victim}!",
                true));

        registerCosmetic(new KillMessage(
                "msg_humiliate",
                "Humilhacao",
                new String[] { "&eMensagem para", "&ezombar da vitima!" },
                CosmeticRarity.UNCOMMON, Material.SLIME_BALL, 2500,
                "&e{killer} &6acabou com o &e{victim} &6facilmente!",
                true));

        // Raro
        registerCosmetic(new KillMessage(
                "msg_epic",
                "Epica",
                new String[] { "&5Mensagem epica", "&5com simbolos especiais!" },
                CosmeticRarity.RARE, Material.DIAMOND_SWORD, 5000,
                "&8&m---&r &6&l* &e{killer} &6&lELIMINOU &e{victim} &6&l* &8&m---",
                true, Sound.NOTE_PLING));

        registerCosmetic(new KillMessage(
                "msg_assassin",
                "Assassino",
                new String[] { "&8Mensagem furtiva", "&8de assassino." },
                CosmeticRarity.RARE, Material.IRON_SWORD, 6000,
                "&8[&4&lX&8] &7{killer} &8assassinou silenciosamente &7{victim}",
                true));

        // Épico
        registerCosmetic(new KillMessage(
                "msg_legendary",
                "Lendaria",
                new String[] { "&6Mensagem de verdadeiro", "&6lendario do PvP!" },
                CosmeticRarity.EPIC, Material.GOLD_SWORD, 10000,
                "&6&m----&r &e&l[LENDA] &6{killer} &e&lCACAOU &6{victim}! &6&m----",
                true, Sound.LEVEL_UP));

        registerCosmetic(new KillMessage(
                "msg_god",
                "Deus do PvP",
                new String[] { "&dMensagem para", "&dOS DEUSES DO PVP!" },
                CosmeticRarity.EPIC, Material.DIAMOND_CHESTPLATE, 15000,
                "&d&l[GOD] &f{killer} &d&lSMITEW &f{victim} &d&lFROM EXISTENCE!",
                true, Sound.ENDERDRAGON_GROWL));

        // Lendário
        registerCosmetic(new KillMessage(
                "msg_mythic",
                "Mitica",
                new String[] { "&c&lA mensagem mais", "&c&lepica do servidor!" },
                CosmeticRarity.LEGENDARY, Material.NETHER_STAR, 30000,
                "\n&8&m=========&r &c&l* ELIMINACAO &c&lMITICA * &8&m=========\n&f  {killer} &c&lEXTINGUIU &f{victim} &c&lDO SERVIDOR!\n&8&m==========================================\n",
                true, Sound.WITHER_DEATH));

        plugin.getLogger().info("[Cosmetics] Registrados: " +
                getCosmeticsByType(CosmeticType.KILL_EFFECT).size() + " efeitos, " +
                getCosmeticsByType(CosmeticType.KILL_SOUND).size() + " sons, " +
                getCosmeticsByType(CosmeticType.KILL_MESSAGE).size() + " mensagens");
    }

    /**
     * Registra um cosmético no sistema
     */
    private void registerCosmetic(Cosmetic cosmetic) {
        allCosmetics.put(cosmetic.getId(), cosmetic);
        cosmeticsByType.get(cosmetic.getType()).add(cosmetic);
    }

    // ==================== OBTER COSMÉTICOS ====================

    /**
     * Obtém um cosmético pelo ID
     */
    public Cosmetic getCosmetic(String id) {
        return allCosmetics.get(id);
    }

    /**
     * Obtém todos os cosméticos
     */
    public Collection<Cosmetic> getAllCosmetics() {
        return Collections.unmodifiableCollection(allCosmetics.values());
    }

    /**
     * Obtém cosméticos por tipo
     */
    public List<Cosmetic> getCosmeticsByType(CosmeticType type) {
        return Collections.unmodifiableList(cosmeticsByType.get(type));
    }

    /**
     * Obtém cosméticos por raridade
     */
    public List<Cosmetic> getCosmeticsByRarity(CosmeticRarity rarity) {
        List<Cosmetic> result = new ArrayList<>();
        for (Cosmetic cosmetic : allCosmetics.values()) {
            if (cosmetic.getRarity() == rarity) {
                result.add(cosmetic);
            }
        }
        return result;
    }

    // ==================== JOGADOR - DESBLOQUEAR/COMPRAR ====================

    /**
     * Verifica se o jogador possui um cosmético desbloqueado
     */
    public boolean hasUnlocked(Player player, String cosmeticId) {
        PlayerData data = getPlayerData(player);
        if (data == null)
            return false;

        List<String> unlocked = getUnlockedList(data);
        return unlocked.contains(cosmeticId);
    }

    /**
     * Desbloqueia um cosmético para o jogador (sem cobrar)
     */
    public void unlockCosmetic(Player player, String cosmeticId) {
        PlayerData data = getPlayerData(player);
        if (data == null)
            return;

        List<String> unlocked = getUnlockedList(data);
        if (!unlocked.contains(cosmeticId)) {
            unlocked.add(cosmeticId);
            data.setCustomData(KEY_UNLOCKED, unlocked);
        }

        // Atualizar cache
        updatePlayerCache(player);
    }

    /**
     * Tenta comprar um cosmético com coins
     * 
     * @return true se a compra foi bem sucedida
     */
    public boolean purchaseCosmetic(Player player, String cosmeticId) {
        Cosmetic cosmetic = getCosmetic(cosmeticId);
        if (cosmetic == null) {
            ChatStorage.sendRaw(player, "&c&lERRO &fCosmetico nao encontrado!");
            return false;
        }

        // Verificar se já possui
        if (hasUnlocked(player, cosmeticId)) {
            ChatStorage.sendRaw(player, "&c&lERRO &fVoce ja possui este cosmetico!");
            return false;
        }

        // Verificar permissão especial (se houver)
        if (cosmetic.hasPermission() && !cosmetic.canUse(player)) {
            ChatStorage.sendRaw(player, "&c&lERRO &fVoce nao tem permissao para comprar este cosmetico!");
            return false;
        }

        // Verificar coins
        ProfileManager profileManager = plugin.getProfileManager();
        if (profileManager == null)
            return false;

        PlayerProfile profile = profileManager.getProfile(player);
        if (profile == null)
            return false;

        if (profile.getCoins() < cosmetic.getPrice()) {
            ChatStorage.sendRaw(player, "&c&lERRO &fVoce nao tem coins suficientes! " +
                    "&7(Necessario: &e" + ChatStorage.formatNumber(cosmetic.getPrice()) + "&7)");
            return false;
        }

        // Cobrar coins
        profile.removeCoins(cosmetic.getPrice());

        // Desbloquear
        unlockCosmetic(player, cosmeticId);

        // Feedback visual
        applyPurchaseEffects(player, cosmetic);

        plugin.getLogger().info("[Cosmetics] " + player.getName() + " comprou " +
                cosmetic.getId() + " por " + cosmetic.getPrice() + " coins");

        return true;
    }

    /**
     * Aplica efeitos visuais quando um cosmético é comprado
     */
    private void applyPurchaseEffects(Player player, Cosmetic cosmetic) {
        String rarityColor = cosmetic.getRarity().getFormattedColor();
        String rarityName = cosmetic.getRarity().getDisplayName();

        // Título
        VisualManager.sendTitle(player,
                "&a&l* COSMETICO DESBLOQUEADO *",
                rarityColor + cosmetic.getDisplayName(),
                10, 60, 20);

        // Som
        player.playSound(player.getLocation(), Sound.LEVEL_UP, 1.0f, 1.2f);

        // Mensagem bonita no chat
        ChatStorage.sendRaw(player, "");
        ChatStorage.sendRaw(player, "&8&m----------&r  " + rarityColor + "&l* COSMETICO * &8&m----------");
        ChatStorage.sendRaw(player, "");
        ChatStorage.sendRaw(player, "  &a&lDESBLOQUEADO COM SUCESSO!");
        ChatStorage.sendRaw(player, "");
        ChatStorage.sendRaw(player, "  &fCosmetico: " + rarityColor + cosmetic.getDisplayName());
        ChatStorage.sendRaw(player, "  &fRaridade: " + rarityColor + rarityName);
        ChatStorage.sendRaw(player, "  &fTipo: &7" + cosmetic.getType().getDisplayName());
        ChatStorage.sendRaw(player, "");
        ChatStorage.sendRaw(player, "  &7Use &e/cosmeticos &7para equipar!");
        ChatStorage.sendRaw(player, "");
        ChatStorage.sendRaw(player, "&8&m--------------------------------------------");
        ChatStorage.sendRaw(player, "");

        // Prévia do efeito
        cosmetic.preview(player);
    }

    // ==================== JOGADOR - SELEÇÃO ====================

    /**
     * Seleciona um cosmético para o jogador
     */
    public boolean selectCosmetic(Player player, String cosmeticId) {
        Cosmetic cosmetic = getCosmetic(cosmeticId);
        if (cosmetic == null)
            return false;

        // Verificar se possui
        if (!hasUnlocked(player, cosmeticId)) {
            ChatStorage.sendRaw(player, "&c&lERRO &fVoce nao possui este cosmetico!");
            return false;
        }

        PlayerData data = getPlayerData(player);
        if (data == null)
            return false;

        // Definir o cosmético selecionado baseado no tipo
        String key = getKeyForType(cosmetic.getType());
        if (key != null) {
            data.setCustomData(key, cosmeticId);
        }

        // Atualizar cache
        updatePlayerCache(player);

        // Feedback
        ChatStorage.sendRaw(player, "&6&lCOSMETICOS &fVoce selecionou: " +
                cosmetic.getRarity().getFormattedColor() + cosmetic.getDisplayName());
        player.playSound(player.getLocation(), Sound.ORB_PICKUP, 1.0f, 1.5f);

        return true;
    }

    /**
     * Remove a seleção de um tipo de cosmético
     */
    public void deselectCosmetic(Player player, CosmeticType type) {
        PlayerData data = getPlayerData(player);
        if (data == null)
            return;

        String key = getKeyForType(type);
        if (key != null) {
            data.setCustomData(key, null);
        }

        // Atualizar cache
        updatePlayerCache(player);

        ChatStorage.sendRaw(player, "&6&lCOSMETICOS &7" + type.getDisplayName() + " desativado.");
    }

    /**
     * Obtém o ID do cosmético selecionado de um tipo
     */
    public String getSelectedCosmetic(Player player, CosmeticType type) {
        PlayerData data = getPlayerData(player);
        if (data == null)
            return null;

        String key = getKeyForType(type);
        if (key == null)
            return null;

        return data.getCustomData(key, String.class);
    }

    // ==================== TOGGLE GERAL ====================

    /**
     * Verifica se os cosméticos estão habilitados para o jogador
     */
    public boolean areCosmeticsEnabled(Player player) {
        PlayerData data = getPlayerData(player);
        if (data == null)
            return true; // Padrão: habilitado

        Boolean enabled = data.getCustomData(KEY_ENABLED, Boolean.class);
        return enabled == null || enabled;
    }

    /**
     * Toggle geral de cosméticos
     */
    public void toggleCosmetics(Player player) {
        PlayerData data = getPlayerData(player);
        if (data == null)
            return;

        boolean current = areCosmeticsEnabled(player);
        data.setCustomData(KEY_ENABLED, !current);

        // Atualizar cache
        updatePlayerCache(player);

        if (!current) {
            ChatStorage.sendRaw(player, "&6&lCOSMETICOS &aCosmeticos ativados!");
        } else {
            ChatStorage.sendRaw(player, "&6&lCOSMETICOS &cCosmeticos desativados!");
        }
    }

    // ==================== APLICAR EFEITOS (COMBATE) ====================

    /**
     * Aplica os efeitos de kill do jogador.
     * Chamado pelo CombatListener quando o jogador mata alguém.
     * 
     * @param killer Jogador que matou
     * @param victim Jogador que morreu
     */
    public void applyKillEffects(Player killer, Player victim) {
        if (!areCosmeticsEnabled(killer))
            return;

        PlayerCosmeticData cache = getOrCreateCache(killer);

        // Aplicar efeito visual
        if (cache.selectedKillEffect != null) {
            cache.selectedKillEffect.apply(killer, victim);
        }

        // Aplicar som
        if (cache.selectedKillSound != null) {
            cache.selectedKillSound.apply(killer, victim);
        }

        // Aplicar mensagem
        if (cache.selectedKillMessage != null) {
            cache.selectedKillMessage.apply(killer, victim);
        }
    }

    // ==================== CACHE ====================

    /**
     * Obtém ou cria o cache de cosméticos do jogador
     */
    private PlayerCosmeticData getOrCreateCache(Player player) {
        UUID uuid = player.getUniqueId();
        PlayerCosmeticData cache = playerCache.get(uuid);

        if (cache == null) {
            cache = new PlayerCosmeticData();
            updatePlayerCacheInternal(player, cache);
            playerCache.put(uuid, cache);
        }

        return cache;
    }

    /**
     * Atualiza o cache do jogador
     */
    public void updatePlayerCache(Player player) {
        UUID uuid = player.getUniqueId();
        PlayerCosmeticData cache = playerCache.get(uuid);

        if (cache == null) {
            cache = new PlayerCosmeticData();
            playerCache.put(uuid, cache);
        }

        updatePlayerCacheInternal(player, cache);
    }

    private void updatePlayerCacheInternal(Player player, PlayerCosmeticData cache) {
        // Carregar cosméticos selecionados
        String effectId = getSelectedCosmetic(player, CosmeticType.KILL_EFFECT);
        String soundId = getSelectedCosmetic(player, CosmeticType.KILL_SOUND);
        String messageId = getSelectedCosmetic(player, CosmeticType.KILL_MESSAGE);

        cache.selectedKillEffect = effectId != null ? (KillEffect) getCosmetic(effectId) : null;
        cache.selectedKillSound = soundId != null ? (KillSound) getCosmetic(soundId) : null;
        cache.selectedKillMessage = messageId != null ? (KillMessage) getCosmetic(messageId) : null;
        cache.enabled = areCosmeticsEnabled(player);
    }

    /**
     * Limpa o cache do jogador (chamado no logout)
     */
    public void clearPlayerCache(Player player) {
        playerCache.remove(player.getUniqueId());
    }

    // ==================== UTILITÁRIOS ====================

    private PlayerData getPlayerData(Player player) {
        ProfileManager profileManager = plugin.getProfileManager();
        if (profileManager == null)
            return null;

        PlayerProfile profile = profileManager.getProfile(player);
        if (profile == null)
            return null;

        return profile.getData();
    }

    @SuppressWarnings("unchecked")
    private List<String> getUnlockedList(PlayerData data) {
        Object obj = data.getCustomData(KEY_UNLOCKED);
        if (obj instanceof List) {
            return (List<String>) obj;
        }

        List<String> newList = new ArrayList<>();
        data.setCustomData(KEY_UNLOCKED, newList);
        return newList;
    }

    private String getKeyForType(CosmeticType type) {
        switch (type) {
            case KILL_EFFECT:
                return KEY_KILL_EFFECT;
            case KILL_SOUND:
                return KEY_KILL_SOUND;
            case KILL_MESSAGE:
                return KEY_KILL_MESSAGE;
            default:
                return null;
        }
    }

    /**
     * Conta quantos cosméticos o jogador possui
     */
    public int getUnlockedCount(Player player) {
        PlayerData data = getPlayerData(player);
        if (data == null)
            return 0;
        return getUnlockedList(data).size();
    }

    /**
     * Conta quantos cosméticos de um tipo o jogador possui
     */
    public int getUnlockedCount(Player player, CosmeticType type) {
        PlayerData data = getPlayerData(player);
        if (data == null)
            return 0;

        List<String> unlocked = getUnlockedList(data);
        int count = 0;

        for (String id : unlocked) {
            Cosmetic cosmetic = getCosmetic(id);
            if (cosmetic != null && cosmetic.getType() == type) {
                count++;
            }
        }

        return count;
    }

    // ==================== CLASSE INTERNA PARA CACHE ====================

    private static class PlayerCosmeticData {
        KillEffect selectedKillEffect;
        KillSound selectedKillSound;
        KillMessage selectedKillMessage;
        boolean enabled = true;
    }
}
