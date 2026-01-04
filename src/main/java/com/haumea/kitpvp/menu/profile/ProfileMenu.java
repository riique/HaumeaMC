package com.haumea.kitpvp.menu.profile;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.managers.GroupManager;
import com.haumea.kitpvp.managers.MultiplierManager;
import com.haumea.kitpvp.menu.BaseMenu;
import com.haumea.kitpvp.menu.MultiplierMenu;
import com.haumea.kitpvp.models.ActiveMultiplier;
import com.haumea.kitpvp.models.Group;
import com.haumea.kitpvp.profile.PlayerProfile;
import com.haumea.kitpvp.utils.ChatStorage;
import com.haumea.kitpvp.utils.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Menu de Perfil do Jogador (45 slots - 5 linhas).
 * 
 * Hub central para o jogador acessar informações sobre sua conta.
 * 
 * Layout:
 * - Slot 13: Cabeça do jogador com informações
 * - Slot 29: Ver estatísticas
 * - Slot 30: Medalhas
 * - Slot 31: Idioma
 * - Slot 32: Preferências
 * - Slot 33: Sua skin
 * 
 * @author HaumeaMC
 */
public class ProfileMenu extends BaseMenu {

    private static final int SLOT_HEAD = 13;
    private static final int SLOT_ACHIEVEMENTS = 20; // Conquistas
    private static final int SLOT_CHALLENGES = 21; // Desafios Diários
    private static final int SLOT_MULTIPLIERS = 23; // Multiplicadores
    private static final int SLOT_DAILY = 24; // Presença diária
    private static final int SLOT_STATS = 29;
    private static final int SLOT_MEDALS = 30;
    private static final int SLOT_LANGUAGE = 31;
    private static final int SLOT_PREFERENCES = 32;
    private static final int SLOT_SKIN = 33;

    // Jogador alvo (pode visualizar perfil de outros)
    private final Player targetPlayer;
    private final boolean isOwnProfile;

    /**
     * Construtor para visualizar próprio perfil
     */
    public ProfileMenu(HaumeaMC plugin, Player player) {
        this(plugin, player, player);
    }

    /**
     * Construtor para visualizar perfil de outro jogador
     */
    public ProfileMenu(HaumeaMC plugin, Player viewer, Player target) {
        super(plugin, viewer, "&6&lPerfil", 45);
        this.targetPlayer = target;
        this.isOwnProfile = viewer.equals(target);
    }

    @Override
    protected void setupItems() {
        // Preencher bordas decorativas
        fillBorders(15); // Preto

        // Decorar com vidros laranjas nos cantos internos
        ItemStack orangePane = createGlassPane(1, " "); // Laranja
        setItem(10, orangePane);
        setItem(16, orangePane);
        setItem(28, orangePane);
        setItem(34, orangePane);

        // === CABEÇA DO JOGADOR (Slot 13) ===
        setupPlayerHead();

        // === CONQUISTAS (Slot 20) ===
        setupAchievementsButton();

        // === DESAFIOS DIÁRIOS (Slot 21) ===
        setupChallengesButton();

        // === MULTIPLICADORES (Slot 23) ===
        setupMultipliersButton();

        // === PRESENÇA DIÁRIA (Slot 24) ===
        setupDailyRewardButton();

        // === VER ESTATÍSTICAS (Slot 29) ===
        ItemStack statsItem = new ItemBuilder(Material.PAPER)
                .name("§a§lVer estatísticas")
                .lore(
                        "§7Veja suas estatísticas",
                        "§7de todos os jogos!",
                        "",
                        "§8▪ §fKills, Deaths, K/D",
                        "§8▪ §fKillstreak",
                        "§8▪ §fModos de jogo",
                        "",
                        "§eClique para abrir!")
                .build();

        setItem(SLOT_STATS, statsItem, (p, click) -> {
            new StatsMenu(plugin, player, targetPlayer).open();
            playClickSound();
        });

        // === MEDALHAS (Slot 30) ===
        ItemStack medalsItem = new ItemBuilder(Material.NAME_TAG)
                .name("§e§lMedalhas")
                .lore(
                        "§7Veja e selecione medalhas",
                        "§7para exibir no chat!",
                        "",
                        "§eClique para ver!")
                .build();

        setItem(SLOT_MEDALS, medalsItem, (p, click) -> {
            if (!isOwnProfile) {
                ChatStorage.sendCustom(player, "§cVocê só pode gerenciar suas próprias medalhas!");
                playErrorSound();
                return;
            }
            close();
            Bukkit.dispatchCommand(player, "medalha");
            playClickSound();
        });

        // === IDIOMA (Slot 31) ===
        ItemStack languageItem = new ItemBuilder(Material.ENCHANTED_BOOK)
                .name("§b§lIdioma")
                .lore(
                        "§7Alterar seu idioma",
                        "§7de preferência.",
                        "",
                        "§eClique para alterar!")
                .build();

        setItem(SLOT_LANGUAGE, languageItem, (p, click) -> {
            if (!isOwnProfile) {
                ChatStorage.sendCustom(player, "§cVocê só pode alterar seu próprio idioma!");
                playErrorSound();
                return;
            }
            close();
            Bukkit.dispatchCommand(player, "language");
            playClickSound();
        });

        // === PREFERÊNCIAS (Slot 32) ===
        ItemStack prefsItem = new ItemBuilder(Material.REDSTONE_COMPARATOR)
                .name("§d§lPreferências")
                .lore(
                        "§7Altere suas preferências",
                        "§7de notificações e privacidade.",
                        "",
                        "§eClique para abrir!")
                .build();

        setItem(SLOT_PREFERENCES, prefsItem, (p, click) -> {
            if (!isOwnProfile) {
                ChatStorage.sendCustom(player, "§cVocê só pode alterar suas próprias preferências!");
                playErrorSound();
                return;
            }
            new PreferencesMenu(plugin, player).open();
            playClickSound();
        });

        // === SUA SKIN (Slot 33) ===
        ItemStack skinItem = new ItemBuilder(Material.ITEM_FRAME)
                .name("§6§lSua Skin")
                .lore(
                        "§7Visualize e altere",
                        "§7sua skin atual.",
                        "",
                        "§eClique para abrir!")
                .build();

        setItem(SLOT_SKIN, skinItem, (p, click) -> {
            if (!isOwnProfile) {
                ChatStorage.sendCustom(player, "§cVocê só pode gerenciar sua própria skin!");
                playErrorSound();
                return;
            }
            // TODO: Menu de skins
            ChatStorage.sendCustom(player, "§eEm breve! Sistema de skins em desenvolvimento.");
            playClickSound();
        });
    }

    /**
     * Configura a cabeça do jogador com informações completas
     */
    private void setupPlayerHead() {
        PlayerProfile profile = plugin.getProfileManager().getProfile(targetPlayer);
        GroupManager groupManager = plugin.getGroupManager();

        List<String> lore = new ArrayList<>();
        lore.add("§7" + targetPlayer.getName());
        lore.add("");

        // Listar grupos/cargos do jogador
        lore.add("§6Grupos:");

        if (groupManager != null) {
            List<String> groupNames = groupManager.getPlayerGroupNames(targetPlayer.getUniqueId());

            // Converter nomes em objetos Group para poder ordenar
            List<Group> playerGroups = new ArrayList<>();
            for (String name : groupNames) {
                Group g = groupManager.getGroup(name);
                if (g != null)
                    playerGroups.add(g);
            }

            // Ordenar por prioridade (maior primeiro)
            playerGroups.sort((g1, g2) -> Integer.compare(g2.getPriority(), g1.getPriority()));

            int groupId = 0;
            for (Group group : playerGroups) {
                // Calcular tempo restante
                long timeRemaining = groupManager.getTimeRemaining(targetPlayer.getUniqueId(), group.getName());
                String timeStr;

                if (timeRemaining == 0) {
                    timeStr = "§aPermanente";
                } else if (timeRemaining > 0) {
                    timeStr = "§e" + formatTimeRemaining(timeRemaining);
                } else {
                    continue; // Expirado
                }

                // Usar ChatStorage.colorize para converter & no prefixo e adicionar &l
                String groupDisplay = group.getPrefix().trim();

                // Fallback 1: Display Name
                if (groupDisplay.isEmpty()) {
                    groupDisplay = group.getDisplayName().trim();
                }

                // Fallback 2: Nome do grupo capitalizado
                if (groupDisplay.isEmpty()) {
                    groupDisplay = group.getName().substring(0, 1).toUpperCase()
                            + group.getName().substring(1).toLowerCase();
                }

                // Aplicar &l (negrito) mantendo a cor original
                // Se não tiver cor, adicionar &7 (cinza) padrão
                String formattedDisplay = groupDisplay;
                if (!formattedDisplay.contains("&")) {
                    formattedDisplay = "&7" + formattedDisplay;
                }
                formattedDisplay = formatWithBold(formattedDisplay);

                lore.add("  §8▪ " + ChatStorage.colorize(formattedDisplay) + " §7- ID: (" + groupId + ") - " + timeStr);
                groupId++;
            }

            if (groupId == 0) {
                lore.add("  §8▪ §7§lMembro §7- Permanente");
            }
        } else {
            lore.add("  §8▪ §7§lMembro §7- Permanente");
        }

        lore.add("");

        // Estatísticas resumidas
        if (profile != null) {
            lore.add("§6Estatísticas rápidas:");
            lore.add("  §8▪ §fKills: §e" + profile.getKills());
            lore.add("  §8▪ §fDeaths: §e" + profile.getDeaths());
            lore.add("  §8▪ §fK/D: §e" + String.format("%.2f", profile.getKDR()));
            lore.add("  §8▪ §fCoins: §e" + ChatStorage.formatNumber(profile.getCoins()));
        }

        ItemStack headItem = ItemBuilder.playerHead(targetPlayer.getName())
                .name("§a§lInformações do Jogador")
                .lore(lore)
                .build();

        setItem(SLOT_HEAD, headItem);
    }

    /**
     * Formata tempo restante em formato legível
     */
    private String formatTimeRemaining(long millis) {
        if (millis <= 0)
            return "Expirado";

        long days = millis / (24 * 60 * 60 * 1000);
        long hours = (millis % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000);
        long minutes = (millis % (60 * 60 * 1000)) / (60 * 1000);

        StringBuilder sb = new StringBuilder();
        if (days > 0)
            sb.append(days).append("d ");
        if (hours > 0)
            sb.append(hours).append("h ");
        if (minutes > 0)
            sb.append(minutes).append("m");

        return sb.toString().trim();
    }

    /**
     * Adiciona &l (negrito) após cada código de cor &
     * 
     * @param input Texto original
     * @return Texto com negrito aplicado
     */
    private String formatWithBold(String input) {
        if (input == null)
            return "";
        // Substituir &6 por &6&l, etc.
        // Regex busca & seguido de um caractere de cor (0-9, a-f) e insere &l
        return input.replaceAll("&([0-9a-fA-F])", "&$1&l");
    }

    /**
     * Configura o botão de Multiplicadores
     */
    private void setupMultipliersButton() {
        if (!isOwnProfile) {
            // Não mostrar para outros perfis
            return;
        }

        MultiplierManager multiplierManager = plugin.getMultiplierManager();
        if (multiplierManager == null) {
            return;
        }

        // Verificar se tem multiplicador ativo
        ActiveMultiplier active = multiplierManager.getActiveMultiplier(player);
        int totalMultipliers = multiplierManager.getTotalInventoryCount(player);

        ItemStack multiplierItem;
        if (active != null && !active.isExpired()) {
            // Tem multiplicador ativo - mostrar informações
            long timeRemaining = active.getRemainingTime();
            String timeFormatted = formatDailyTimeRemaining(timeRemaining);

            multiplierItem = new ItemBuilder(Material.MAGMA_CREAM)
                    .name("§6§l★ Multiplicadores ★")
                    .lore(
                            "§7Gerencie seus multiplicadores",
                            "§7de coins!",
                            "",
                            "§aMultiplicador Ativo:",
                            "§8▪ §f" + ChatStorage.colorize(active.getType().getDisplayMultiplier()),
                            "§8▪ §eTempo restante: §f" + timeFormatted,
                            "",
                            "§6Seu inventário: §e" + totalMultipliers + " multiplicador(es)",
                            "",
                            "§eClique para gerenciar!")
                    .glow()
                    .build();
        } else {
            // Sem multiplicador ativo
            multiplierItem = new ItemBuilder(Material.SLIME_BALL)
                    .name("§e§lMultiplicadores")
                    .lore(
                            "§7Gerencie seus multiplicadores",
                            "§7de coins!",
                            "",
                            "§cNenhum multiplicador ativo",
                            "",
                            "§6Seu inventário: §e" + totalMultipliers + " multiplicador(es)",
                            "",
                            "§eClique para gerenciar!")
                    .build();
        }

        setItem(SLOT_MULTIPLIERS, multiplierItem, (p, click) -> {
            MultiplierMenu.open(plugin, player);
            playClickSound();
        });
    }

    /**
     * Configura o botão de Presença Diária
     */
    private void setupDailyRewardButton() {
        if (!isOwnProfile) {
            // Não mostrar para outros perfis
            return;
        }

        PlayerProfile profile = plugin.getProfileManager().getProfile(player);
        if (profile == null) {
            return;
        }

        // Verificar se pode resgatar
        long lastClaim = getLastDailyClaim(profile.getData());
        boolean canClaim = System.currentTimeMillis() - lastClaim >= 86400000L; // 24h em ms

        ItemStack dailyItem;
        if (canClaim) {
            // Disponível para resgate - mostrar item brilhando
            dailyItem = new ItemBuilder(Material.CHEST)
                    .name("§a§l★ Presenca Diaria ★")
                    .lore(
                            "§7Resgate sua recompensa",
                            "§7diaria de moedas gratis!",
                            "",
                            "§a§lDISPONIVEL!",
                            "",
                            "§eClique para resgatar!")
                    .glow()
                    .build();
        } else {
            // Aguardando cooldown
            long timeRemaining = 86400000L - (System.currentTimeMillis() - lastClaim);
            String timeFormatted = formatDailyTimeRemaining(timeRemaining);

            dailyItem = new ItemBuilder(Material.ENDER_CHEST)
                    .name("§6§lPresenca Diaria")
                    .lore(
                            "§7Resgate sua recompensa",
                            "§7diaria de moedas gratis!",
                            "",
                            "§cAguardando: §e" + timeFormatted,
                            "",
                            "§eClique para ver detalhes!")
                    .build();
        }

        setItem(SLOT_DAILY, dailyItem, (p, click) -> {
            new DailyRewardMenu(plugin, player).open();
            playClickSound();
        });
    }

    /**
     * Obtém o último tempo de resgate diário
     */
    private long getLastDailyClaim(com.haumea.kitpvp.models.PlayerData data) {
        Object value = data.getCustomData("daily_last_claim");
        if (value instanceof Long) {
            return (Long) value;
        } else if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return 0L;
    }

    /**
     * Formata tempo restante para presença diária
     */
    private String formatDailyTimeRemaining(long millis) {
        if (millis <= 0) {
            return "Disponivel!";
        }
        long hours = millis / 3600000L;
        long minutes = (millis % 3600000L) / 60000L;
        long seconds = (millis % 60000L) / 1000L;

        if (hours > 0) {
            return String.format("%02dh %02dm", hours, minutes);
        } else if (minutes > 0) {
            return String.format("%02dm %02ds", minutes, seconds);
        } else {
            return String.format("%02ds", seconds);
        }
    }

    /**
     * Configura o botão de Conquistas
     */
    private void setupAchievementsButton() {
        if (!isOwnProfile) {
            return;
        }

        int unlocked = 0;
        int total = 0;

        if (plugin.getAchievementManager() != null) {
            unlocked = plugin.getAchievementManager().getUnlockedCount(player);
            total = plugin.getAchievementManager().getTotalCount();
        }

        ItemStack achievementsItem = new ItemBuilder(Material.DIAMOND)
                .name("§6§l★ Conquistas")
                .lore(
                        "§7Veja suas conquistas e",
                        "§7recompensas desbloqueadas!",
                        "",
                        "§7Progresso: §e" + unlocked + "§7/§e" + total,
                        "",
                        "§eClique para ver!")
                .glow()
                .build();

        setItem(SLOT_ACHIEVEMENTS, achievementsItem, (p, click) -> {
            new AchievementsMenu(plugin, player).open();
            playClickSound();
        });
    }

    /**
     * Configura o botão de Desafios Diários
     */
    private void setupChallengesButton() {
        if (!isOwnProfile) {
            return;
        }

        int completed = 0;
        int total = 3; // Desafios por dia

        if (plugin.getDailyChallengeManager() != null) {
            completed = plugin.getDailyChallengeManager().getCompletedCount(player);
            total = plugin.getDailyChallengeManager().getDailyChallenges().size();
        }

        boolean allCompleted = completed >= total;

        ItemStack challengesItem = new ItemBuilder(allCompleted ? Material.EXP_BOTTLE : Material.BOOK)
                .name("§e§l⚔ Desafios Diarios")
                .lore(
                        "§7Complete desafios para",
                        "§7ganhar recompensas!",
                        "",
                        "§7Hoje: §" + (allCompleted ? "a" : "e") + completed + "§7/§e" + total
                                + (allCompleted ? " §a✓" : ""),
                        "",
                        "§eClique para ver!")
                .build();

        if (allCompleted) {
            challengesItem = new ItemBuilder(challengesItem).glow().build();
        }

        setItem(SLOT_CHALLENGES, challengesItem, (p, click) -> {
            new DailyChallengesMenu(plugin, player).open();
            playClickSound();
        });
    }
}
