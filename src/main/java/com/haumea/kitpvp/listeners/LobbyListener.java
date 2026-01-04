package com.haumea.kitpvp.listeners;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.managers.NPCManager;
import com.haumea.kitpvp.managers.ServerSelectorManager;
import com.haumea.kitpvp.menu.ServerSelectorMenu;
import com.haumea.kitpvp.utils.ChatStorage;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Listener específico para o servidor de Lobby.
 * Gerencia:
 * - Dar item seletor de servidores ao entrar
 * - Abrir menu ao clicar no seletor
 * - Proteções do lobby (sem dano, sem fome, etc)
 * - Double Jump para diversão
 * - Mensagens de boas-vindas personalizadas
 */
public class LobbyListener implements Listener {

    private final HaumeaMC plugin;

    // Cooldown de Double Jump (em milissegundos)
    private static final long DOUBLE_JUMP_COOLDOWN = 2000L; // 2 segundos
    private final Map<UUID, Long> doubleJumpCooldowns;

    public LobbyListener(HaumeaMC plugin) {
        this.plugin = plugin;
        this.doubleJumpCooldowns = new ConcurrentHashMap<>();
    }

    /**
     * Ao entrar no lobby:
     * - Limpa inventário
     * - Dá item seletor de servidores
     * - Define gamemode Adventure
     * - Habilita Double Jump
     * - Envia mensagem de boas-vindas personalizada
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();

        // Delay de 1 tick para garantir que o jogador está totalmente carregado
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline())
                    return;

                // Limpar inventário
                player.getInventory().clear();
                player.getInventory().setArmorContents(null);

                // Curar
                player.setHealth(player.getMaxHealth());
                player.setFoodLevel(20);
                player.setSaturation(20.0f);

                // Limpar efeitos
                player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));

                // GameMode Adventure (não pode quebrar/colocar blocos)
                player.setGameMode(GameMode.ADVENTURE);

                // Habilitar Double Jump
                player.setAllowFlight(true);
                player.setFlying(false);

                // Dar item seletor
                ServerSelectorManager selectorManager = plugin.getServerSelectorManager();
                if (selectorManager != null) {
                    selectorManager.giveSelector(player);
                }

                // Enviar mensagem de boas-vindas do Lobby
                sendWelcomeMessage(player);

                // Tocar som de boas-vindas
                player.playSound(player.getLocation(), Sound.LEVEL_UP, 1.0f, 1.0f);

                // Enviar Title de boas-vindas
                sendWelcomeTitle(player);

                // Spawnar NPCs para este jogador (se disponivel)
                NPCManager npcManager = plugin.getNPCManager();
                if (npcManager != null) {
                    npcManager.spawnNPCsForPlayer(player);
                }
            }
        }.runTaskLater(plugin, 1L);
    }

    /**
     * Envia a mensagem de boas-vindas do Lobby personalizada
     */
    private void sendWelcomeMessage(Player player) {
        ChatStorage.sendRaw(player, ChatStorage.getMessage("lobby.welcome.header"));
        ChatStorage.sendRaw(player, ChatStorage.getMessage("lobby.welcome.title"));
        ChatStorage.sendRaw(player, ChatStorage.getMessage("lobby.welcome.header"));
        ChatStorage.sendRaw(player, ChatStorage.getMessage("lobby.welcome.selector-hint"));
        ChatStorage.sendRaw(player, ChatStorage.getMessage("lobby.welcome.help-hint"));
        ChatStorage.sendRaw(player, ChatStorage.getMessage("lobby.welcome.footer"));
    }

    /**
     * Envia o Title de boas-vindas na tela
     */
    private void sendWelcomeTitle(Player player) {
        String title = ChatStorage.getMessage("lobby.title.main");
        String subtitle = ChatStorage.getMessage("lobby.title.subtitle");

        // Enviar title (1.8 style via NMS ou método genérico)
        player.sendTitle(ChatStorage.colorize(title), ChatStorage.colorize(subtitle));
    }

    /**
     * Ao clicar com o item seletor -> abrir menu
     */
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null) {
            return;
        }

        ServerSelectorManager selectorManager = plugin.getServerSelectorManager();
        if (selectorManager == null) {
            return;
        }

        // Verificar se é o item seletor
        if (selectorManager.isSelectorItem(item)) {
            event.setCancelled(true);
            new ServerSelectorMenu(plugin, player).open();
        }
    }

    /**
     * Sistema de Double Jump
     * Quando o jogador tenta voar, aplica um boost para cima
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onToggleFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();

        // Ignorar se está em modo criativo ou espectador (pode voar normalmente)
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }

        // Verificar se o jogador está no chão ou no ar
        if (!player.isOnGround()) {
            event.setCancelled(true);

            // Verificar cooldown
            UUID uuid = player.getUniqueId();
            long now = System.currentTimeMillis();
            Long lastJump = doubleJumpCooldowns.get(uuid);

            if (lastJump != null && now - lastJump < DOUBLE_JUMP_COOLDOWN) {
                // Ainda em cooldown
                long remaining = (DOUBLE_JUMP_COOLDOWN - (now - lastJump)) / 1000;
                if (remaining > 0) {
                    ChatStorage.send(player, "lobby.double-jump.cooldown", "time", String.valueOf(remaining));
                }
                return;
            }

            // Aplicar Double Jump
            applyDoubleJump(player);

            // Registrar cooldown
            doubleJumpCooldowns.put(uuid, now);

            // Desabilitar voo temporariamente (será reabilitado ao tocar no chão)
            player.setAllowFlight(false);
        }
    }

    /**
     * Aplica o efeito de Double Jump
     */
    private void applyDoubleJump(Player player) {
        // Calcular direção do jogador com boost para cima
        Vector velocity = player.getLocation().getDirection().multiply(1.5);
        velocity.setY(1.0);

        // Aplicar velocity
        player.setVelocity(velocity);

        // Efeitos visuais
        Location loc = player.getLocation();

        // Spawnar partículas de nuvem
        try {
            player.getWorld().playEffect(loc, Effect.CLOUD, 0);
            player.getWorld().playEffect(loc.add(0, 0.5, 0), Effect.CLOUD, 0);
        } catch (Exception ignored) {
            // Ignorar se efeito não existir
        }

        // Tocar som de firework
        player.playSound(loc, Sound.FIREWORK_LAUNCH, 1.0f, 1.0f);
    }

    /**
     * Reabilitar Double Jump ao tocar no chão
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        // Ignorar se está em modo criativo ou espectador
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }

        // Se o jogador está no chão e não pode voar, reabilitar
        if (player.isOnGround() && !player.getAllowFlight()) {
            player.setAllowFlight(true);
        }
    }

    /**
     * Impedir dano no lobby
     */
    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            event.setCancelled(true);
        }
    }

    /**
     * Impedir fome no lobby
     */
    @EventHandler
    public void onHunger(FoodLevelChangeEvent event) {
        event.setCancelled(true);
        event.setFoodLevel(20);
    }

    /**
     * Impedir drop de itens
     */
    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        // Apenas staff pode dropar
        if (!event.getPlayer().hasPermission("haumea.lobby.drop")) {
            event.setCancelled(true);
        }
    }

    /**
     * Impedir pegar itens
     */
    @EventHandler
    public void onPickup(PlayerPickupItemEvent event) {
        // Apenas staff pode pegar
        if (!event.getPlayer().hasPermission("haumea.lobby.pickup")) {
            event.setCancelled(true);
        }
    }

    /**
     * Impedir quebrar blocos (backup, já que GameMode está em Adventure)
     */
    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        if (!event.getPlayer().hasPermission("haumea.lobby.build")) {
            event.setCancelled(true);
        }
    }

    /**
     * Impedir colocar blocos (backup)
     */
    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        if (!event.getPlayer().hasPermission("haumea.lobby.build")) {
            event.setCancelled(true);
        }
    }

    /**
     * Limpar cooldowns quando jogador sair (prevenir memory leak)
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        doubleJumpCooldowns.remove(event.getPlayer().getUniqueId());
    }
}
