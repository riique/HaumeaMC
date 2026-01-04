package com.haumea.kitpvp;

import com.haumea.kitpvp.server.ServerType;
import com.haumea.kitpvp.managers.ServerSelectorManager;
import com.haumea.kitpvp.listeners.LobbyListener;

import com.haumea.kitpvp.commands.base.CommandRegistry;
import com.haumea.kitpvp.commands.essential.*;
import com.haumea.kitpvp.commands.group.GroupCommand;
import com.haumea.kitpvp.commands.punishment.*;
import com.haumea.kitpvp.commands.report.ReportCommand;
import com.haumea.kitpvp.commands.report.ReportMenuGUI;
import com.haumea.kitpvp.commands.report.ReportsCommand;
import com.haumea.kitpvp.commands.staff.*;
import com.haumea.kitpvp.commands.tag.TagCommand;
import com.haumea.kitpvp.listeners.AdminListener;
import com.haumea.kitpvp.listeners.ChatListener;
import com.haumea.kitpvp.listeners.PlayerListener;
import com.haumea.kitpvp.listeners.PunishmentListener;
import com.haumea.kitpvp.listeners.WorldListener;
import com.haumea.kitpvp.listeners.base.ListenerRegistry;
import com.haumea.kitpvp.listeners.TabListener;
import com.haumea.kitpvp.listeners.CombatListener;
import com.haumea.kitpvp.listeners.ScoreboardListener;
import com.haumea.kitpvp.listeners.SoupListener;
import com.haumea.kitpvp.listeners.SpawnTransitionListener;
import com.haumea.kitpvp.scoreboard.ScoreboardManager;
import com.haumea.kitpvp.tablist.TabManager;
import com.haumea.kitpvp.managers.ChatManager;
import com.haumea.kitpvp.managers.ConfigManager;
import com.haumea.kitpvp.managers.GroupManager;
import com.haumea.kitpvp.managers.PermissionManager;
import com.haumea.kitpvp.managers.ProfileManager;
import com.haumea.kitpvp.managers.PunishmentManager;
import com.haumea.kitpvp.managers.ReportManager;
import com.haumea.kitpvp.managers.StatsManager;
import com.haumea.kitpvp.managers.TagManager;
import com.haumea.kitpvp.managers.WarpsManager;
import com.haumea.kitpvp.managers.ArenaItemsHandler;
import com.haumea.kitpvp.managers.FakeNickManager;
import com.haumea.kitpvp.listeners.FakeNickPacketListener;
import com.haumea.kitpvp.commands.fake.FakeCommand;
import com.haumea.kitpvp.managers.MedalManager;
import com.haumea.kitpvp.managers.KitManager;
import com.haumea.kitpvp.managers.LeagueManager;
import com.haumea.kitpvp.managers.SkinManager;
import com.haumea.kitpvp.managers.DisplayManager;
import com.haumea.kitpvp.managers.CooldownManager;
import com.haumea.kitpvp.managers.PlayerStateManager;
import com.haumea.kitpvp.managers.TeleportManager;
import com.haumea.kitpvp.managers.DuelManager;
import com.haumea.kitpvp.managers.EventManager;
import com.haumea.kitpvp.managers.FeastManager;

import com.haumea.kitpvp.managers.InteractionManager;
import com.haumea.kitpvp.managers.DamageManager;
import com.haumea.kitpvp.managers.WorldEditManager;
import com.haumea.kitpvp.managers.MultiplierManager;
import com.haumea.kitpvp.managers.VipKeyManager;
import com.haumea.kitpvp.managers.AchievementManager;
import com.haumea.kitpvp.managers.BountyManager;
import com.haumea.kitpvp.managers.DailyChallengeManager;
import com.haumea.kitpvp.managers.TradeManager;
import com.haumea.kitpvp.abilities.AbilityManager;
import com.haumea.kitpvp.managers.RankingManager;
import com.haumea.kitpvp.managers.IgnoreManager;
import com.haumea.kitpvp.managers.AdminLogManager;
import com.haumea.kitpvp.managers.AntiFloodManager;
import com.haumea.kitpvp.managers.AutoMessageManager;
import com.haumea.kitpvp.managers.ChatEventManager;
import com.haumea.kitpvp.managers.VipShopManager;
import com.haumea.kitpvp.managers.CasinoManager;
import com.haumea.kitpvp.managers.CosmeticManager;
import com.haumea.kitpvp.managers.NametagManager;
import com.haumea.kitpvp.managers.MessageManager;
import com.haumea.kitpvp.managers.FPSWarpManager;
import com.haumea.kitpvp.managers.NPCManager;
import com.haumea.kitpvp.managers.BossBarManager;
import com.haumea.kitpvp.database.MongoVipKeyRepository;
import com.haumea.kitpvp.database.MongoManager;
import com.haumea.kitpvp.database.MongoAccountRepository;
import com.haumea.kitpvp.commands.event.EventCommand;
import com.haumea.kitpvp.commands.feast.FeastCommand;
import com.haumea.kitpvp.listeners.EventListener;
import com.haumea.kitpvp.listeners.CasinoListener;
import com.haumea.kitpvp.commands.skin.SkinCommand;
import com.haumea.kitpvp.commands.duel.DuelCommand;
import com.haumea.kitpvp.commands.duel.DuelAdminCommand;
import com.haumea.kitpvp.listeners.SkinListener;
import com.haumea.kitpvp.listeners.DuelListener;
import com.haumea.kitpvp.listeners.LauncherListener;
import com.haumea.kitpvp.listeners.CommandBlockerListener;
import com.haumea.kitpvp.commands.KitCommand;

import com.haumea.kitpvp.permissions.AuthorityManager;
import com.haumea.kitpvp.listeners.AuthorityListener;
import com.haumea.kitpvp.menu.MenuListener;
import com.haumea.kitpvp.commands.medal.MedalCommand;
import com.haumea.kitpvp.listeners.ArenaItemsListener;
import com.haumea.kitpvp.commands.warp.HaumeaWarpCommand;
import com.haumea.kitpvp.commands.warp.WarpCommand;
import com.haumea.kitpvp.commands.warp.SpawnCommand;
import com.haumea.kitpvp.commands.warp.HaumeaSpawnCommand;
import com.haumea.kitpvp.commands.league.RankingCommand;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import com.haumea.kitpvp.utils.ChatStorage;
import com.haumea.kitpvp.utils.VisualManager;

/**
 * Classe principal do plugin HaumeaMC
 * Servidor HaumeaMC - KitPvP 1.7x/1.8x
 * 
 * @author HaumeaMC
 * @version 1.0.0
 */
public class HaumeaMC extends JavaPlugin {

    private static HaumeaMC instance;

    // Managers
    private ConfigManager configManager;
    private GroupManager groupManager;
    private PermissionManager permissionManager;
    private ProfileManager profileManager;
    private TagManager tagManager;
    private ChatManager chatManager;
    private TabManager tabManager;
    private StatsManager statsManager;
    private ScoreboardManager scoreboardManager;
    private PunishmentManager punishmentManager;
    private ReportManager reportManager;
    private WarpsManager warpsManager;
    private ArenaItemsHandler arenaItemsHandler;
    private MedalManager medalManager;
    private KitManager kitManager;
    private AuthorityManager authorityManager;
    private FakeNickManager fakeNickManager;
    private FakeNickPacketListener fakeNickPacketListener;
    private LeagueManager leagueManager;
    private SkinManager skinManager;
    private DisplayManager displayManager;
    private CooldownManager cooldownManager;
    private PlayerStateManager stateManager;
    private TeleportManager teleportManager;
    private DuelManager duelManager;
    private EventManager eventManager;
    private FeastManager feastManager;

    private InteractionManager interactionManager;
    private MultiplierManager multiplierManager;
    private DamageManager damageManager;
    private MongoManager mongoManager;
    private MongoAccountRepository mongoAccountRepository;
    private WorldEditManager worldEditManager;
    private VipKeyManager vipKeyManager;
    private MongoVipKeyRepository vipKeyRepository;

    // Novos sistemas
    private AchievementManager achievementManager;
    private BountyManager bountyManager;
    private DailyChallengeManager dailyChallengeManager;
    private TradeManager tradeManager;
    private RankingManager rankingManager;
    private AbilityManager abilityManager;
    private IgnoreManager ignoreManager;
    private AdminLogManager adminLogManager;
    private AntiFloodManager antiFloodManager;
    private ChatEventManager chatEventManager;
    private MessageManager messageManager;
    private AutoMessageManager autoMessageManager;
    private VipShopManager vipShopManager;
    private CasinoManager casinoManager;
    private CosmeticManager cosmeticManager;
    private NametagManager nametagManager;
    private FPSWarpManager fpsWarpManager;

    // Multi-Server Support
    private ServerType serverType;
    private ServerSelectorManager serverSelectorManager;
    private BossBarManager bossBarManager;
    private NPCManager npcManager;

    // Registries
    private CommandRegistry commandRegistry;
    private ListenerRegistry listenerRegistry;

    @Override
    public void onEnable() {
        instance = this;

        // Criar pasta de dados
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        // Salvar configurações padrão
        saveDefaultConfig();
        saveResource("groups.yml", false);
        saveResource("kits.yml", false);

        // Carregar tipo de servidor (LOBBY, KITPVP, etc)
        String serverTypeStr = getConfig().getString("server-type", "KITPVP");
        this.serverType = ServerType.fromString(serverTypeStr);
        getLogger().info("[ServerType] Modo de servidor: " + serverType.getDisplayName());

        // Inicializar managers (ordem importa!)
        initManagers();

        // Inicializar sistema de feedback visual
        VisualManager.initialize(this);

        // Inicializar registries
        initRegistries();

        // Registrar comandos (legados via plugin.yml + novos via reflexão)
        registerCommands();

        // Inicializar e iniciar TabManager
        initTabManager();

        // Registrar listeners por módulos
        registerListeners();

        // Registrar canal BungeeCord para comunicação inter-servidor
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        getLogger().info("[BungeeCord] Canal de mensagens registrado.");

        // Injetar permissões em jogadores que já estão online (caso de /reload)
        injectOnlinePlayers();

        // Mensagem de inicialização
        logStartup();
    }

    @Override
    public void onDisable() {
        // IMPORTANTE: Desligar sistema de Bounty Global ANTES de salvar
        // para cancelar tasks e evitar erros
        if (bountyManager != null) {
            bountyManager.shutdownGlobalBounty();
            bountyManager.saveAllBounties();
        }

        // Kickar todos os jogadores com mensagem de reinicialização
        String kickMessage = ChatStorage.getMessage("server.restart-kick");
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.kickPlayer(kickMessage);
        }

        // Parar ScoreboardManager
        if (scoreboardManager != null) {
            scoreboardManager.stop();
        }

        // Parar TabManager
        if (tabManager != null) {
            tabManager.stop();
        }

        // Parar BossBarManager (limpar entidades Wither)
        if (bossBarManager != null) {
            bossBarManager.stop();
        }

        // Desligar ServerSelectorManager (Lobby - cancelar task de player count)
        if (serverSelectorManager != null) {
            serverSelectorManager.shutdown();
        }

        // Desligar NPCManager (Lobby - remover NPCs e hologramas)
        if (npcManager != null) {
            npcManager.shutdown();
        }

        // Salvar perfis de jogadores
        if (profileManager != null) {
            profileManager.unloadAllProfiles();
        }

        // Salvar definições de grupos (groups.yml)
        // NOTA: Grupos de JOGADORES são salvos automaticamente no MongoDB
        if (groupManager != null) {
            groupManager.saveGroups();
        }

        // NOTA: HaumeaAccountStorage foi removido - agora usamos MongoDB
        // O mongoAccountRepository.saveAllToDatabase() já é chamado antes

        // NOTA: Kits alugados agora são salvos no customData do PlayerData (MongoDB)
        // Não precisamos mais chamar saveRentedKits() aqui

        // Desligar AuthorityManager (restaurar permissões originais)
        if (authorityManager != null) {
            authorityManager.shutdown();
        }

        // Desligar DuelManager (encerrar duelos e salvar arenas)
        if (duelManager != null) {
            duelManager.shutdown();
        }

        // Desligar EventManager (cancelar evento ativo)
        if (eventManager != null) {
            eventManager.shutdown();
        }

        // Desligar FeastManager (cancelar feast e salvar config)
        if (feastManager != null) {
            feastManager.shutdown();
        }

        // Desligar VisualManager (limpar tasks e caches)
        VisualManager.shutdown();

        // Desligar DamageManager (limpar caches de dano)
        if (damageManager != null) {
            damageManager.shutdown();
        }

        // Desligar WorldEditManager (cancelar operações e limpar filas)
        if (worldEditManager != null) {
            worldEditManager.shutdown();
        }

        // Desligar AbilityManager (limpar dados temporários)
        if (abilityManager != null) {
            abilityManager.shutdown();
        }

        // Desligar ChatEventManager (cancelar eventos de chat)
        if (chatEventManager != null) {
            chatEventManager.shutdown();
        }

        // Desligar AutoMessageManager (parar anúncios automáticos)
        if (autoMessageManager != null) {
            autoMessageManager.shutdown();
        }

        // Desligar CasinoManager (devolver apostas pendentes)
        if (casinoManager != null) {
            casinoManager.shutdown();
        }

        // Salvar contas no MongoDB e desconectar
        if (mongoAccountRepository != null) {
            mongoAccountRepository.saveAllToDatabase();
        }
        if (mongoManager != null) {
            mongoManager.disconnect();
        }

        Bukkit.getConsoleSender().sendMessage("§8[§6HaumeaMC§8] §cPlugin desativado com sucesso!");
    }

    /**
     * Inicializa todos os managers do plugin
     * A ordem é importante: GroupManager antes do PermissionManager
     * 
     * MULTI-SERVER: Carrega managers condicionalmente baseado no serverType
     * - LOBBY: Apenas sistemas essenciais + seletor de servidores
     * - KITPVP: Todos os sistemas
     */
    private void initManagers() {
        // ==================== MANAGERS ESSENCIAIS (TODOS OS SERVIDORES)
        // ====================

        // MessageManager PRIMEIRO - carrega messages.yml antes de qualquer outro
        // manager
        this.messageManager = new MessageManager(this);

        // Inicializar ChatStorage com as mensagens do YAML
        ChatStorage.init(this);

        this.configManager = new ConfigManager(this);
        this.groupManager = new GroupManager(this);

        // AuthorityManager ANTES do PermissionManager
        this.authorityManager = new AuthorityManager(this);
        this.permissionManager = new PermissionManager(this);

        // ==================== MONGODB (TODOS OS SERVIDORES) ====================
        this.mongoManager = new MongoManager(this);
        if (!mongoManager.connect()) {
            getLogger().severe("[MongoDB] ATENÇÃO: MongoDB não conectado! Dados NÃO serão salvos!");
            getLogger().severe("[MongoDB] Verifique se o MongoDB está rodando em localhost:27017");
        }

        // ProfileManager agora usa MongoDB
        this.profileManager = new ProfileManager(this, mongoManager);

        // MongoAccountRepository para contas
        this.mongoAccountRepository = new MongoAccountRepository(this, mongoManager);

        // ==================== SISTEMAS COMUNS (TODOS OS SERVIDORES)
        // ====================
        this.tagManager = new TagManager(this);
        this.chatManager = new ChatManager(this);
        this.punishmentManager = new PunishmentManager(this);
        this.reportManager = new ReportManager(this);
        this.warpsManager = new WarpsManager(this);
        this.fakeNickManager = new FakeNickManager(this);
        this.ignoreManager = new IgnoreManager(this);
        this.adminLogManager = new AdminLogManager(this, mongoManager);
        this.antiFloodManager = new AntiFloodManager(this);

        // Inicializar listener de pacotes para fake nick (requer ProtocolLib)
        try {
            Class.forName("com.comphenix.protocol.ProtocolLibrary");
            this.fakeNickPacketListener = new FakeNickPacketListener(this);
        } catch (ClassNotFoundException e) {
            getLogger().warning(
                    "[FakeNick] ProtocolLib não encontrado! Fake nick pode não funcionar corretamente na nametag.");
        }

        this.skinManager = new SkinManager(this);
        this.cooldownManager = new CooldownManager(this);
        this.stateManager = new PlayerStateManager(this);
        this.teleportManager = new TeleportManager(this);
        this.displayManager = new DisplayManager(this);
        this.nametagManager = new NametagManager(this);

        // ==================== LOBBY ESPECÍFICO ====================
        if (serverType.isLobby()) {
            getLogger().info("[ServerType] Carregando sistemas de Lobby...");
            this.serverSelectorManager = new ServerSelectorManager(this);

            // NPCManager para spawnar NPCs clicáveis do lobby
            try {
                Class.forName("com.comphenix.protocol.ProtocolLibrary");
                this.npcManager = new NPCManager(this);
            } catch (ClassNotFoundException e) {
                getLogger().warning("[NPCManager] ProtocolLib não encontrado! NPCs não serão spawnados.");
            }

            // BossBarManager para todos os servidores (mostra nome do servidor)
            this.bossBarManager = new BossBarManager(this);
            this.bossBarManager.start();

            // Não carregar sistemas de combate no lobby
            getLogger().info("[ServerType] Sistemas de combate DESABILITADOS (modo Lobby)");
            return;
        }

        // ==================== KITPVP ESPECÍFICO (SISTEMAS DE COMBATE)
        // ====================
        getLogger().info("[ServerType] Carregando sistemas de combate KitPvP...");

        this.statsManager = new StatsManager(this);
        this.arenaItemsHandler = new ArenaItemsHandler(this);
        this.medalManager = new MedalManager(this);
        this.kitManager = new KitManager(this);
        this.leagueManager = new LeagueManager(this);

        // DuelManager (sistema de duelos 1v1)
        this.duelManager = new DuelManager(this);

        // EventManager (sistema de eventos especiais)
        this.eventManager = new EventManager(this);

        // FeastManager (sistema de Feast)
        this.feastManager = new FeastManager(this);

        // InteractionManager (sistema de placas e interações especiais)
        this.interactionManager = new InteractionManager(this);

        // DamageManager (sistema de correção de dano do 1.8)
        this.damageManager = new DamageManager(this);

        // WorldEditManager (sistema de WorldEdit assíncrono)
        this.worldEditManager = new WorldEditManager(this);

        // MultiplierManager (sistema de multiplicadores de coins)
        this.multiplierManager = new MultiplierManager(this);

        // VipKeyManager (sistema de chaves VIP)
        this.vipKeyRepository = new MongoVipKeyRepository(this, mongoManager);
        this.vipKeyManager = new VipKeyManager(this, vipKeyRepository);

        // Novos sistemas de funcionalidades
        this.achievementManager = new AchievementManager(this);
        this.bountyManager = new BountyManager(this);
        this.dailyChallengeManager = new DailyChallengeManager(this);
        this.tradeManager = new TradeManager(this);
        this.rankingManager = new RankingManager(this);

        // AbilityManager (sistema de habilidades de kits)
        this.abilityManager = new AbilityManager(this);

        // ChatEventManager (sistema de eventos de chat)
        this.chatEventManager = new ChatEventManager(this);

        // AutoMessageManager (sistema de mensagens automáticas)
        this.autoMessageManager = new AutoMessageManager(this);

        // VipShopManager (sistema de loja de VIPs por coins)
        this.vipShopManager = new VipShopManager(this);

        // CasinoManager (sistema de cassino e apostas)
        this.casinoManager = new CasinoManager(this);

        // CosmeticManager (sistema de cosmeticos)
        this.cosmeticManager = new CosmeticManager(this);

        // FPSWarpManager (sistema de warps com proteção de spawn)
        this.fpsWarpManager = new FPSWarpManager(this);

        // BossBarManager para todos os servidores (mostra nome do servidor)
        this.bossBarManager = new BossBarManager(this);
        this.bossBarManager.start();
    }

    /**
     * Inicializa os sistemas de registro automático
     */
    private void initRegistries() {
        this.commandRegistry = new CommandRegistry(this);
        this.listenerRegistry = new ListenerRegistry(this);
    }

    /**
     * Registra todos os comandos do plugin
     * 
     * MULTI-SERVER: Registra comandos condicionalmente
     * - LOBBY: Apenas comandos essenciais (tags, grupos, staff, punições)
     * - KITPVP: Todos os comandos (kits, duelos, eventos, etc)
     */
    private void registerCommands() {
        // ==================== COMANDOS ESSENCIAIS (TODOS OS SERVIDORES)
        // ====================

        // Comandos legados (declarados no plugin.yml)
        getCommand("tag").setExecutor(new TagCommand(this));
        getCommand("haumeagroups").setExecutor(new GroupCommand(this));

        // Comandos essenciais de staff
        getCommand("tp").setExecutor(new TpCommand(this));
        getCommand("tpall").setExecutor(new TpAllCommand(this));
        getCommand("puxar").setExecutor(new PuxarCommand(this));
        getCommand("inv").setExecutor(new InvCommand(this));
        getCommand("kill").setExecutor(new KillCommand(this));
        getCommand("ipcheck").setExecutor(new IpCheckCommand(this));
        getCommand("bc").setExecutor(new BcCommand(this));
        getCommand("tell").setExecutor(new TellCommand(this));
        getCommand("ping").setExecutor(new PingCommand(this));
        getCommand("regras").setExecutor(new RegrasCommand(this));
        getCommand("conta").setExecutor(new ContaCommand(this));

        // Comando de warps (essencial para todos os servidores)
        HaumeaWarpCommand warpAdminCmd = new HaumeaWarpCommand(this);
        getCommand("haumeawarp").setExecutor(warpAdminCmd);
        getCommand("haumeawarp").setTabCompleter(warpAdminCmd);

        WarpCommand warpCmd = new WarpCommand(this);
        getCommand("warp").setExecutor(warpCmd);
        getCommand("warp").setTabCompleter(warpCmd);

        getCommand("spawn").setExecutor(new SpawnCommand(this));
        getCommand("haumeaspawn").setExecutor(new HaumeaSpawnCommand(this));

        // Comando /fake - sistema de Fake Nick
        FakeCommand fakeCmd = new FakeCommand(this);
        getCommand("fake").setExecutor(fakeCmd);
        getCommand("fake").setTabCompleter(fakeCmd);

        // Comandos de staff via registro automático (essenciais)
        commandRegistry.registerAll(
                GameModeCommand.class,
                ReiniciarCommand.class,
                FlyCommand.class,
                AdminCommand.class,
                GotoWatchCommand.class,
                BuildCommand.class,
                ClearChatCommand.class,
                StaffChatCommand.class,
                ChatCommand.class,
                ClearDropsCommand.class,
                GodCommand.class,
                SpeedCommand.class,
                // Comandos de punição
                BanCommand.class,
                UnbanCommand.class,
                KickCommand.class,
                MuteCommand.class,
                UnmuteCommand.class,
                WarnCommand.class,
                // Comando de verificação de jogadores
                com.haumea.kitpvp.commands.staff.CheckCommand.class,
                // Comando de report
                ReportCommand.class,
                ReportsCommand.class,
                // Comando de autoridade (debug de permissões)
                AuthorityCommand.class,
                // Comando de skin
                SkinCommand.class,
                // Comando de ignorar jogadores
                com.haumea.kitpvp.commands.social.IgnoreCommand.class,
                // Comando de lista de staffs online
                com.haumea.kitpvp.commands.essential.StaffCommand.class,
                // Comando de tempo de jogo
                com.haumea.kitpvp.commands.essential.TempoCommand.class);

        // Se for LOBBY, registrar comando de NPC e retornar
        if (serverType.isLobby()) {
            // Comando /npc para gerenciar NPCs do Lobby
            com.haumea.kitpvp.commands.npc.NPCCommand npcCmd = new com.haumea.kitpvp.commands.npc.NPCCommand(this);
            getCommand("npc").setExecutor(npcCmd);

            getLogger().info("[ServerType] Comandos de combate não registrados (modo Lobby)");
            return;
        }

        // ==================== COMANDOS KITPVP (APENAS SERVIDORES DE COMBATE)
        // ====================
        getLogger().info("[ServerType] Registrando comandos de combate KitPvP...");

        getCommand("score").setExecutor(new ScoreCommand(this));

        // Comando /medalha - sistema de medalhas
        MedalCommand medalCmd = new MedalCommand(this);
        getCommand("medalha").setExecutor(medalCmd);
        getCommand("medalha").setTabCompleter(medalCmd);

        // Comando /ranking - sistema de ligas
        getCommand("ranking").setExecutor(new RankingCommand(this));

        // Comando /duel - sistema de duelos 1v1
        DuelCommand duelCmd = new DuelCommand(this);
        getCommand("duel").setExecutor(duelCmd);

        // Comando /dueladmin - administração de duelos
        DuelAdminCommand duelAdminCmd = new DuelAdminCommand(this);
        getCommand("dueladmin").setExecutor(duelAdminCmd);

        // Comando /evento - sistema de eventos
        EventCommand eventCmd = new EventCommand(this);
        getCommand("evento").setExecutor(eventCmd);
        getCommand("evento").setTabCompleter(eventCmd);

        // Comando /feast - sistema de Feast
        FeastCommand feastCmd = new FeastCommand(this);
        getCommand("feast").setExecutor(feastCmd);
        getCommand("feast").setTabCompleter(feastCmd);

        // Comandos específicos de KitPvP
        commandRegistry.registerAll(
                // Comando de VIP Keys
                com.haumea.kitpvp.commands.vip.HaumeaVipCommand.class,
                // Comandos de economia e funcionalidades
                com.haumea.kitpvp.commands.essential.PayCommand.class,
                com.haumea.kitpvp.commands.essential.BountyCommand.class,
                com.haumea.kitpvp.commands.essential.TopCommand.class,
                // Comando /lobby - volta para o lobby
                com.haumea.kitpvp.commands.essential.LobbyCommand.class,
                // Comando de kits/abilities
                KitCommand.class,
                // Comando da loja
                ShopCommand.class,
                // Comando de eventos de chat
                com.haumea.kitpvp.commands.chatevent.ChatEventCommand.class,
                // Comando de cosmeticos
                com.haumea.kitpvp.commands.cosmetic.CosmeticCommand.class);

        // Comandos de multiplicadores
        com.haumea.kitpvp.commands.multiplier.MultiplierCommand multiplierCmd = new com.haumea.kitpvp.commands.multiplier.MultiplierCommand(
                this);
        getCommand("multiplicador").setExecutor(multiplierCmd);

        com.haumea.kitpvp.commands.multiplier.MultiplierAdminCommand multAdminCmd = new com.haumea.kitpvp.commands.multiplier.MultiplierAdminCommand(
                this);
        getCommand("multadmin").setExecutor(multAdminCmd);

        // Comando de administração de estatísticas
        com.haumea.kitpvp.commands.admin.HaumeaStatsCommand statsCmd = new com.haumea.kitpvp.commands.admin.HaumeaStatsCommand(
                this);
        getCommand("haumeastats").setExecutor(statsCmd);
        getCommand("haumeastats").setTabCompleter(statsCmd);

        // Comando de VIP Keys
        com.haumea.kitpvp.commands.vip.HaumeaVipCommand vipCmd = new com.haumea.kitpvp.commands.vip.HaumeaVipCommand(
                this);
        getCommand("haumeavip").setExecutor(vipCmd);
    }

    /**
     * Registra todos os listeners organizados por módulos
     * 
     * MULTI-SERVER: Registra listeners condicionalmente
     * - LOBBY: Apenas listeners essenciais + LobbyListener
     * - KITPVP: Todos os listeners de combate
     * 
     * IMPORTANTE: A ordem de registro afeta a ordem de processamento!
     */
    private void registerListeners() {

        // ==================== LISTENERS ESSENCIAIS (TODOS OS SERVIDORES)
        // ====================

        // Módulo: Bloqueio de Comandos (PRIMEIRO - bloqueia comandos Bukkit)
        listenerRegistry.registerModule("command-blocker",
                new CommandBlockerListener(this));

        // Módulo: Autoridade (injeção de permissões)
        listenerRegistry.registerModule("autoridade",
                new AuthorityListener(this));

        // Módulo: Jogadores (login, logout, permissões)
        listenerRegistry.registerModule("jogadores",
                new PlayerListener(this));

        // Módulo: Mundo (proteção, fome, tempo)
        listenerRegistry.registerModule("mundo",
                new WorldListener(this));

        // Módulo: Chat (staffchat, bloqueio de chat)
        listenerRegistry.registerModule("chat",
                new ChatListener(this));

        // Módulo: Admin (itens de inspeção do modo admin)
        listenerRegistry.registerModule("admin",
                new AdminListener(this));

        // Módulo: TabList (atualização ao entrar)
        listenerRegistry.registerModule("tablist",
                new TabListener(this));

        // Módulo: Scoreboard (scoreboard lateral)
        listenerRegistry.registerModule("scoreboard",
                new ScoreboardListener(this));

        // Módulo: Punições (ban, mute, warn)
        listenerRegistry.registerModule("punicoes",
                new PunishmentListener(this));

        // Módulo: Reports (menu GUI)
        listenerRegistry.registerModule("reports",
                new ReportMenuGUI(this));

        // Módulo: Menus GUI
        listenerRegistry.registerModule("menus",
                new MenuListener(this));

        // Módulo: Fake Nick (restaurar fake nick ao logar)
        listenerRegistry.registerModule("fake-nick",
                new com.haumea.kitpvp.listeners.FakeNickListener(this));

        // Módulo: Skins (restauração de skins salvas ao logar)
        listenerRegistry.registerModule("skins",
                new SkinListener(this));

        // Módulo: God Mode (proteção de dano para staff)
        listenerRegistry.registerModule("godmode",
                new com.haumea.kitpvp.listeners.GodModeListener(this));

        // Módulo: Boss Bar (nome do servidor no topo da tela)
        if (bossBarManager != null && bossBarManager.isAvailable()) {
            listenerRegistry.registerModule("bossbar",
                    new com.haumea.kitpvp.listeners.BossBarListener(this));
        }

        // ==================== LOBBY ESPECÍFICO ====================
        if (serverType.isLobby()) {
            getLogger().info("[ServerType] Registrando listeners de Lobby...");

            // Listener específico do Lobby (seletor de servidores, proteções)
            listenerRegistry.registerModule("lobby",
                    new LobbyListener(this));

            getLogger().info("[ServerType] Listeners de combate não registrados (modo Lobby)");
            return;
        }

        // ==================== KITPVP ESPECÍFICO (SISTEMAS DE COMBATE)
        // ====================
        getLogger().info("[ServerType] Registrando listeners de combate KitPvP...");

        // Módulo: Combate (kills, deaths, killstreaks)
        listenerRegistry.registerModule("combate",
                new CombatListener(this));

        // Módulo: Sopa (regeneração KitPvP style)
        listenerRegistry.registerModule("sopa",
                new SoupListener(this));

        // Módulo: Itens de Arena (lobby/combate)
        listenerRegistry.registerModule("arena-items",
                new ArenaItemsListener(this));

        // Módulo: Transição Spawn/Combate (dá itens do kit)
        listenerRegistry.registerModule("spawn-transition",
                new SpawnTransitionListener(this));

        // Módulo: Item Drops (apenas sopas, itens desaparecem em 2s)
        listenerRegistry.registerModule("item-drops",
                new com.haumea.kitpvp.listeners.ItemDropListener(this));

        // Módulo: Duelos (sistema de 1v1)
        listenerRegistry.registerModule("duelos",
                new DuelListener(this));

        // Módulo: Eventos (sistema de eventos especiais)
        listenerRegistry.registerModule("eventos",
                new EventListener(this));

        // Interacoes (placas especiais, encantamento automatico)
        if (interactionManager != null) {
            listenerRegistry.registerModule("interacoes", interactionManager);
        }

        // Módulo: Lançadores (Jump Pads e Dashes)
        listenerRegistry.registerModule("lancadores",
                new LauncherListener(this));

        // Módulo: Eventos de Chat (respostas de eventos de quiz/math/etc)
        listenerRegistry.registerModule("chat-events",
                new com.haumea.kitpvp.listeners.ChatEventListener(this));

        // Módulo: Cassino (limpeza de sessões)
        listenerRegistry.registerModule("casino",
                new CasinoListener(this));

        // Módulo: Cosméticos (efeitos de kill)
        listenerRegistry.registerModule("cosmetics",
                new com.haumea.kitpvp.listeners.CosmeticListener(this));

        // Módulo: FPS Warp (proteção de spawn em warps)
        listenerRegistry.registerModule("fps-warp",
                new com.haumea.kitpvp.listeners.FPSWarpListener(this));
    }

    /**
     * Inicializa e inicia o sistema de TabList
     */
    private void initTabManager() {
        this.tabManager = new TabManager(this);
        this.tabManager.start();

        // Inicializar ScoreboardManager
        this.scoreboardManager = new ScoreboardManager(this);
        this.scoreboardManager.start();
    }

    /**
     * Injeta o sistema de autoridade em jogadores que já estão online.
     * 
     * Isso é necessário para casos de:
     * - /reload do servidor
     * - Plugin carregado via PlugMan ou similar
     */
    private void injectOnlinePlayers() {
        if (authorityManager == null || !authorityManager.isReflectionWorking()) {
            return;
        }

        int injected = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!authorityManager.hasInjectedPermissible(player)) {
                if (authorityManager.injectPermissible(player)) {
                    injected++;
                }
            }
        }

        if (injected > 0) {
            getLogger().info("§aAuthorityManager: Injetado em " + injected + " jogador(es) já online.");
        }
    }

    /**
     * Exibe mensagem de inicialização no console
     */
    private void logStartup() {
        Bukkit.getConsoleSender().sendMessage("");
        Bukkit.getConsoleSender().sendMessage("§8§m----------------------------------------");
        Bukkit.getConsoleSender().sendMessage("");
        Bukkit.getConsoleSender().sendMessage("  §6§lHAUMEA§e§lMC");
        Bukkit.getConsoleSender().sendMessage("");
        Bukkit.getConsoleSender().sendMessage("  §fVersão: §e" + getDescription().getVersion());
        Bukkit.getConsoleSender().sendMessage("  §fAutor: §e" + getDescription().getAuthors().get(0));
        Bukkit.getConsoleSender().sendMessage("  §fGrupos: §e" + groupManager.getAllGroups().size() + " carregados");
        Bukkit.getConsoleSender().sendMessage("  §fListeners: §e" + listenerRegistry.getTotalListeners() + " em " +
                listenerRegistry.getModules().size() + " módulos");
        Bukkit.getConsoleSender().sendMessage("  §fStatus: §aAtivado com sucesso!");
        Bukkit.getConsoleSender().sendMessage("");
        Bukkit.getConsoleSender().sendMessage("§8§m----------------------------------------");
        Bukkit.getConsoleSender().sendMessage("");
    }

    // ==================== GETTERS ====================

    /**
     * Obtém a instância do plugin
     */
    public static HaumeaMC getInstance() {
        return instance;
    }

    /**
     * Obtém o ConfigManager
     */
    public ConfigManager getConfigManager() {
        return configManager;
    }

    /**
     * Obtém o GroupManager
     */
    public GroupManager getGroupManager() {
        return groupManager;
    }

    /**
     * Obtém o PermissionManager
     */
    public PermissionManager getPermissionManager() {
        return permissionManager;
    }

    /**
     * Obtém o ProfileManager
     */
    public ProfileManager getProfileManager() {
        return profileManager;
    }

    /**
     * Obtém o TagManager
     */
    public TagManager getTagManager() {
        return tagManager;
    }

    /**
     * Obtém o ChatManager
     */
    public ChatManager getChatManager() {
        return chatManager;
    }

    /**
     * Obtém o TabManager
     */
    public TabManager getTabManager() {
        return tabManager;
    }

    /**
     * Obtém o StatsManager
     */
    public StatsManager getStatsManager() {
        return statsManager;
    }

    /**
     * Obtém o ScoreboardManager
     */
    public ScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }

    /**
     * Obtém o CommandRegistry para registro de comandos
     */
    public CommandRegistry getCommandRegistry() {
        return commandRegistry;
    }

    /**
     * Obtém o ListenerRegistry para registro de listeners
     */
    public ListenerRegistry getListenerRegistry() {
        return listenerRegistry;
    }

    // NOTA: getAccountStorage() foi removido - use getMongoAccountRepository()

    /**
     * Obtém o PunishmentManager para gerenciamento de punições
     */
    public PunishmentManager getPunishmentManager() {
        return punishmentManager;
    }

    /**
     * Obtém o ReportManager para gerenciamento de denúncias
     */
    public ReportManager getReportManager() {
        return reportManager;
    }

    /**
     * Obtém o WarpsManager para gerenciamento de warps
     */
    public WarpsManager getWarpsManager() {
        return warpsManager;
    }

    /**
     * Obtém o ArenaItemsHandler para gerenciamento de itens de arena
     */
    public ArenaItemsHandler getArenaItemsHandler() {
        return arenaItemsHandler;
    }

    /**
     * Obtém o MedalManager para gerenciamento de medalhas
     */
    public MedalManager getMedalManager() {
        return medalManager;
    }

    /**
     * Obtém o KitManager para gerenciamento de kits
     */
    public KitManager getKitManager() {
        return kitManager;
    }

    /**
     * Obtém o AuthorityManager para gerenciamento de autoridade e permissões
     * avançadas
     */
    public AuthorityManager getAuthorityManager() {
        return authorityManager;
    }

    /**
     * Obtém o FakeNickManager para gerenciamento de nomes falsos
     */
    public FakeNickManager getFakeNickManager() {
        return fakeNickManager;
    }

    /**
     * Obtém o FakeNickPacketListener para atualização de pacotes
     */
    public FakeNickPacketListener getFakeNickPacketListener() {
        return fakeNickPacketListener;
    }

    /**
     * Obtém o LeagueManager para gerenciamento de ligas/elo
     */
    public LeagueManager getLeagueManager() {
        return leagueManager;
    }

    /**
     * Obtém o SkinManager para gerenciamento de skins
     */
    public SkinManager getSkinManager() {
        return skinManager;
    }

    /**
     * Obtém o DisplayManager para gerenciamento unificado de exibição
     */
    public DisplayManager getDisplayManager() {
        return displayManager;
    }

    /**
     * Obtém o CooldownManager para gerenciamento unificado de cooldowns
     */
    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }

    /**
     * Obtém o PlayerStateManager para gerenciamento unificado de estados
     */
    public PlayerStateManager getStateManager() {
        return stateManager;
    }

    /**
     * Obtém o TeleportManager para gerenciamento unificado de teleportes
     */
    public TeleportManager getTeleportManager() {
        return teleportManager;
    }

    /**
     * Obtém o DuelManager para gerenciamento de duelos 1v1
     */
    public DuelManager getDuelManager() {
        return duelManager;
    }

    /**
     * Obtém o EventManager para gerenciamento de eventos especiais
     */
    public EventManager getEventManager() {
        return eventManager;
    }

    /**
     * Obtém o FeastManager para gerenciamento do sistema de Feast
     */
    public FeastManager getFeastManager() {
        return feastManager;
    }

    /**
     * Obtém o InteractionManager para gerenciamento de interações especiais
     */
    public InteractionManager getInteractionManager() {
        return interactionManager;
    }

    /**
     * Obtém o DamageManager para gerenciamento de dano e correções do 1.8
     */
    public DamageManager getDamageManager() {
        return damageManager;
    }

    /**
     * Obtém o WorldEditManager para operações de WorldEdit assíncronas
     */
    public WorldEditManager getWorldEditManager() {
        return worldEditManager;
    }

    /**
     * Obtém o MultiplierManager para gerenciamento de multiplicadores de coins
     */
    public MultiplierManager getMultiplierManager() {
        return multiplierManager;
    }

    /**
     * Obtém o MongoManager para acesso ao MongoDB
     */
    public MongoManager getMongoManager() {
        return mongoManager;
    }

    /**
     * Obtém o MongoAccountRepository para operações de contas
     */
    public MongoAccountRepository getMongoAccountRepository() {
        return mongoAccountRepository;
    }

    /**
     * Obtém o VipKeyManager para gerenciamento de chaves VIP
     */
    public VipKeyManager getVipKeyManager() {
        return vipKeyManager;
    }

    /**
     * Obtém o VipKeyRepository para acesso às chaves VIP
     */
    public MongoVipKeyRepository getVipKeyRepository() {
        return vipKeyRepository;
    }

    /**
     * Obtém o AchievementManager para gerenciamento de conquistas
     */
    public AchievementManager getAchievementManager() {
        return achievementManager;
    }

    /**
     * Obtém o BountyManager para gerenciamento de recompensas
     */
    public BountyManager getBountyManager() {
        return bountyManager;
    }

    /**
     * Obtém o DailyChallengeManager para gerenciamento de desafios diários
     */
    public DailyChallengeManager getDailyChallengeManager() {
        return dailyChallengeManager;
    }

    /**
     * Obtém o TradeManager para gerenciamento de trocas de coins
     */
    public TradeManager getTradeManager() {
        return tradeManager;
    }

    /**
     * Obtém o RankingManager para gerenciamento de rankings globais
     */
    public RankingManager getRankingManager() {
        return rankingManager;
    }

    /**
     * Obtém o AbilityManager para gerenciamento de habilidades de kits
     */
    public AbilityManager getAbilityManager() {
        return abilityManager;
    }

    /**
     * Obtém o IgnoreManager para gerenciamento de jogadores ignorados
     */
    public IgnoreManager getIgnoreManager() {
        return ignoreManager;
    }

    /**
     * Obtém o AdminLogManager para logs de ações administrativas
     */
    public AdminLogManager getAdminLogManager() {
        return adminLogManager;
    }

    /**
     * Obtém o ChatEventManager para gerenciamento de eventos de chat
     */
    public ChatEventManager getChatEventManager() {
        return chatEventManager;
    }

    /**
     * Obtém o MessageManager para gerenciamento de mensagens do messages.yml
     */
    public MessageManager getMessageManager() {
        return messageManager;
    }

    /**
     * Obtém o AntiFloodManager para controle de antiflood no chat
     */
    public AntiFloodManager getAntiFloodManager() {
        return antiFloodManager;
    }

    /**
     * Obtém o MongoAccountRepository para acesso a dados de contas
     */
    public MongoAccountRepository getAccountRepository() {
        return mongoAccountRepository;
    }

    /**
     * Obtém o AutoMessageManager para gerenciamento de mensagens automáticas
     */
    public AutoMessageManager getAutoMessageManager() {
        return autoMessageManager;
    }

    /**
     * Obtém o VipShopManager para gerenciamento da loja de VIPs por coins
     */
    public VipShopManager getVipShopManager() {
        return vipShopManager;
    }

    /**
     * Obtém o CasinoManager para gerenciamento do cassino e apostas
     */
    public CasinoManager getCasinoManager() {
        return casinoManager;
    }

    /**
     * Obtém o CosmeticManager para gerenciamento de cosmeticos
     */
    public CosmeticManager getCosmeticManager() {
        return cosmeticManager;
    }

    /**
     * Obtém o NametagManager para gerenciamento centralizado de nametags
     */
    public NametagManager getNametagManager() {
        return nametagManager;
    }

    /**
     * Obtém o FPSWarpManager para gerenciamento de warps com proteção de spawn
     */
    public FPSWarpManager getFPSWarpManager() {
        return fpsWarpManager;
    }

    // ==================== MULTI-SERVER GETTERS ====================

    /**
     * Obtém o tipo de servidor atual (LOBBY, KITPVP, etc)
     */
    public ServerType getServerType() {
        return serverType;
    }

    /**
     * Verifica se este servidor é um Lobby
     */
    public boolean isLobby() {
        return serverType != null && serverType.isLobby();
    }

    /**
     * Verifica se este servidor requer sistemas de combate
     */
    public boolean requiresCombat() {
        return serverType != null && serverType.requiresCombat();
    }

    /**
     * Obtém o ServerSelectorManager para gerenciamento do seletor de servidores.
     * Retorna null se não for um servidor de Lobby.
     */
    public ServerSelectorManager getServerSelectorManager() {
        return serverSelectorManager;
    }

    /**
     * Obtém o BossBarManager para gerenciamento de Boss Bars.
     */
    public BossBarManager getBossBarManager() {
        return bossBarManager;
    }

    /**
     * Obtém o NPCManager para gerenciamento de NPCs do Lobby.
     * Retorna null se não for um servidor de Lobby ou ProtocolLib não estiver
     * instalado.
     */
    public NPCManager getNPCManager() {
        return npcManager;
    }
}
