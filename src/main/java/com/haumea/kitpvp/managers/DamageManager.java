package com.haumea.kitpvp.managers;

import com.haumea.kitpvp.HaumeaMC;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.haumea.kitpvp.models.DuelMatch;

/**
 * Gerenciador Central de Dano do HaumeaMC.
 * 
 * Responsável por:
 * - Corrigir bugs conhecidos do sistema de dano do Minecraft 1.8
 * - Normalizar hit delay e knockback para consistência
 * - Gerenciar void damage e fall damage
 * - Controlar fire ticks e regeneração durante dano
 * - Integrar com outros managers (CombatLog, PlayerState, League)
 * 
 * Correções implementadas:
 * 1. Hit Delay Inconsistente - Normalizado para valor configurável
 * 2. Knockback Bugado - Aplicação manual de velocidade
 * 3. Dano de Queda - Recálculo após teleportes
 * 4. Void Damage - Morte forçada abaixo de Y configurável
 * 5. Fire Tick Persistente - Limpeza correta em água
 * 6. Sweeping Edge Fantasma - Validação de line-of-sight
 * 7. Armor Bypass - Redução de armadura garantida
 * 8. Regeneração Durante Dano - Bloqueio de heal no mesmo tick
 * 
 * @author HaumeaMC
 */
public class DamageManager implements Listener {

    private final HaumeaMC plugin;

    // ==================== CONFIGURAÇÕES ====================

    /** Hit delay em ticks (padrão: 10 ticks = 0.5s) */
    private int hitDelayTicks = 10;

    /** Multiplicador de knockback horizontal */
    private double knockbackMultiplier = 1.0;

    /** Multiplicador de knockback vertical */
    private double knockbackVerticalMultiplier = 1.0;

    /** Altura mínima para void kill (padrão: Y < -64) */
    private int voidKillHeight = -64;

    /** Se correções estão ativas */
    private boolean fixHitDelay = true;
    private boolean fixKnockback = true;
    private boolean fixFallDamage = true;
    private boolean fixVoidDamage = true;
    private boolean fixFireTicks = true;
    private boolean fixSweepingDamage = true;
    private boolean fixArmorBypass = true;
    private boolean fixRegenDuringDamage = true;

    // ==================== CACHE DE DADOS ====================

    /** Último timestamp de dano por jogador (para hit delay) */
    private final Map<UUID, Long> lastDamageTime = new ConcurrentHashMap<>();

    /** Último atacante de cada jogador */
    private final Map<UUID, UUID> lastAttacker = new ConcurrentHashMap<>();

    /** Timestamp do último atacante */
    private final Map<UUID, Long> lastAttackerTime = new ConcurrentHashMap<>();

    /** Jogadores em invulnerabilidade temporária */
    private final Map<UUID, Long> invulnerabilityEnd = new ConcurrentHashMap<>();

    /** Jogadores que receberam dano neste tick (para bloquear regen) */
    private final Set<UUID> damagedThisTick = ConcurrentHashMap.newKeySet();

    /** Altura antes do teleporte (para correção de fall damage) */
    private final Map<UUID, Double> preTeleportHeight = new ConcurrentHashMap<>();

    /** Duração de rastreamento de último atacante em ms */
    private static final long ATTACKER_TRACK_DURATION_MS = 15000;

    // ==================== CONSTRUTOR ====================

    public DamageManager(HaumeaMC plugin) {
        this.plugin = plugin;

        // Registrar listeners
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        // Iniciar task de limpeza de dados expirados
        startCleanupTask();

        // Iniciar task de void check
        startVoidCheckTask();

        plugin.getLogger().info("DamageManager inicializado com correções de dano do 1.8");
    }

    // ==================== EVENT HANDLERS ====================

    /**
     * Intercepta todos os eventos de dano em entidades
     * Prioridade HIGHEST para processar depois de outros plugins
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player victim = (Player) event.getEntity();

        // Verificar invulnerabilidade customizada
        if (isInvulnerable(victim)) {
            event.setCancelled(true);
            return;
        }

        // Verificar se jogador está protegido (spawn, admin mode, etc)
        if (isProtectedFromDamage(victim)) {
            event.setCancelled(true);
            return;
        }

        // Correção de void damage
        if (fixVoidDamage && event.getCause() == DamageCause.VOID) {
            handleVoidDamage(victim, event);
            return;
        }

        // Correção de fall damage
        if (fixFallDamage && event.getCause() == DamageCause.FALL) {
            handleFallDamage(victim, event);
            return;
        }

        // Correção de fire damage
        if (fixFireTicks && (event.getCause() == DamageCause.FIRE_TICK ||
                event.getCause() == DamageCause.FIRE ||
                event.getCause() == DamageCause.LAVA)) {
            handleFireDamage(victim, event);
        }

        // Marcar jogador como "tomou dano neste tick" (para bloquear regen)
        if (fixRegenDuringDamage && !event.isCancelled() && event.getDamage() > 0) {
            markDamagedThisTick(victim);
        }
    }

    /**
     * Intercepta eventos de dano entre entidades
     * Prioridade HIGHEST para processar corretamente
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player victim = (Player) event.getEntity();
        Entity damagerEntity = event.getDamager();
        Player attacker = getAttackerFromEntity(damagerEntity);

        // Se não encontrou um atacante jogador, deixar o evento normal
        if (attacker == null) {
            return;
        }

        // Verificar invulnerabilidade customizada da vítima
        if (isInvulnerable(victim)) {
            event.setCancelled(true);
            return;
        }

        // Verificar se vítima está protegida
        if (isProtectedFromDamage(victim)) {
            event.setCancelled(true);
            return;
        }

        // Verificar se atacante está protegido (não pode dar dano do spawn)
        if (isProtectedFromDamage(attacker)) {
            event.setCancelled(true);
            return;
        }

        // Correção de sweeping edge / dano em área
        if (fixSweepingDamage && !hasLineOfSight(attacker, victim)) {
            event.setCancelled(true);
            return;
        }

        // Correção de hit delay
        if (fixHitDelay && !canDealDamage(attacker, victim)) {
            event.setCancelled(true);
            return;
        }

        // Registrar hit para hit delay
        registerHit(attacker, victim);

        // Registrar último atacante
        registerAttacker(victim, attacker);

        // Aplicar modificadores de dano (liga, etc)
        double modifiedDamage = applyDamageModifiers(attacker, victim, event.getDamage());
        event.setDamage(modifiedDamage);

        // Correção de armor bypass
        if (fixArmorBypass) {
            applyArmorReduction(victim, event);
        }

        // Aplicar knockback customizado (após o evento ser processado)
        if (fixKnockback) {
            scheduleKnockback(attacker, victim);
        }

        // Notificar entrada em combate
        notifyCombatEnter(victim, attacker);
        notifyCombatEnter(attacker, victim);

        // Marcar jogador como "tomou dano neste tick"
        if (fixRegenDuringDamage) {
            markDamagedThisTick(victim);
        }
    }

    /**
     * Bloqueia regeneração no mesmo tick que o jogador recebeu dano
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityRegainHealth(EntityRegainHealthEvent event) {
        if (!fixRegenDuringDamage) {
            return;
        }

        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();

        // Se tomou dano neste tick, bloquear regeneração
        if (damagedThisTick.contains(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    /**
     * Limpa fire ticks quando jogador entra em água
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!fixFireTicks) {
            return;
        }

        Player player = event.getPlayer();
        Location to = event.getTo();

        if (to == null) {
            return;
        }

        // Se está em água e pegando fogo, apagar
        if (player.getFireTicks() > 0) {
            Material blockType = to.getBlock().getType();
            if (blockType == Material.WATER || blockType == Material.STATIONARY_WATER) {
                player.setFireTicks(0);
            }
        }
    }

    /**
     * Limpa dados quando jogador sai
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        clearPlayerData(uuid);
    }

    // ==================== CORREÇÕES DE DANO ====================

    /**
     * Trata dano de void (Y < configurável)
     */
    private void handleVoidDamage(Player victim, EntityDamageEvent event) {
        // Forçar morte imediata no void
        if (victim.getLocation().getY() < voidKillHeight) {
            event.setDamage(9999);

            // Atribuir kill ao último atacante se houver
            UUID attackerUuid = lastAttacker.get(victim.getUniqueId());
            if (attackerUuid != null) {
                Long attackerTime = lastAttackerTime.get(victim.getUniqueId());
                if (attackerTime != null &&
                        System.currentTimeMillis() - attackerTime < ATTACKER_TRACK_DURATION_MS) {

                    Player attacker = plugin.getServer().getPlayer(attackerUuid);
                    if (attacker != null && attacker.isOnline()) {
                        // Aqui poderia notificar o StatsManager
                        // para atribuir a kill ao atacante
                    }
                }
            }
        }
    }

    /**
     * Trata dano de queda (recalcula se necessário)
     */
    private void handleFallDamage(Player victim, EntityDamageEvent event) {
        // Se jogador foi teleportado recentemente, ajustar dano de queda
        Double preHeight = preTeleportHeight.remove(victim.getUniqueId());
        if (preHeight != null) {
            // Recalcular dano baseado na diferença de altura
            double currentY = victim.getLocation().getY();
            double fallDistance = Math.max(0, preHeight - currentY);

            // Minecraft aplica dano = fallDistance - 3 (primeiros 3 blocos são grátis)
            double calculatedDamage = Math.max(0, fallDistance - 3);

            if (calculatedDamage != event.getDamage()) {
                event.setDamage(calculatedDamage);
            }
        }
    }

    /**
     * Trata dano de fogo
     */
    private void handleFireDamage(Player victim, EntityDamageEvent event) {
        // Se está em água, não deveria tomar dano de fogo
        Material blockType = victim.getLocation().getBlock().getType();
        if (blockType == Material.WATER || blockType == Material.STATIONARY_WATER) {
            event.setCancelled(true);
            victim.setFireTicks(0);
        }
    }

    /**
     * Aplica redução de armadura corretamente
     */
    private void applyArmorReduction(Player victim, EntityDamageByEntityEvent event) {
        // O Minecraft 1.8 já aplica redução de armadura
        // Esta função é para casos específicos onde precisa garantir a redução
        double armorPoints = getArmorPoints(victim);

        // Fórmula do Minecraft: dano_final = dano * (1 - (armor / 25))
        // Máximo de 80% de redução com 20 pontos de armadura
        double reduction = Math.min(0.80, armorPoints / 25.0);
        double reducedDamage = event.getDamage() * (1 - reduction);

        // Só ajustar se o dano calculado pelo Minecraft estiver errado
        // (tolerância de 0.5 de diferença)
        if (Math.abs(event.getFinalDamage() - reducedDamage) > 0.5) {
            event.setDamage(reducedDamage);
        }
    }

    /**
     * Calcula pontos de armadura do jogador
     */
    private double getArmorPoints(Player player) {
        double points = 0;
        ItemStack[] armor = player.getInventory().getArmorContents();

        for (ItemStack item : armor) {
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }

            String typeName = item.getType().name();

            // Capacete
            if (typeName.endsWith("_HELMET")) {
                if (typeName.startsWith("LEATHER"))
                    points += 1;
                else if (typeName.startsWith("GOLD") || typeName.startsWith("CHAINMAIL"))
                    points += 2;
                else if (typeName.startsWith("IRON"))
                    points += 2;
                else if (typeName.startsWith("DIAMOND"))
                    points += 3;
            }
            // Peitoral
            else if (typeName.endsWith("_CHESTPLATE")) {
                if (typeName.startsWith("LEATHER"))
                    points += 3;
                else if (typeName.startsWith("GOLD") || typeName.startsWith("CHAINMAIL"))
                    points += 5;
                else if (typeName.startsWith("IRON"))
                    points += 6;
                else if (typeName.startsWith("DIAMOND"))
                    points += 8;
            }
            // Calças
            else if (typeName.endsWith("_LEGGINGS")) {
                if (typeName.startsWith("LEATHER"))
                    points += 2;
                else if (typeName.startsWith("GOLD") || typeName.startsWith("CHAINMAIL"))
                    points += 3;
                else if (typeName.startsWith("IRON"))
                    points += 5;
                else if (typeName.startsWith("DIAMOND"))
                    points += 6;
            }
            // Botas
            else if (typeName.endsWith("_BOOTS")) {
                if (typeName.startsWith("LEATHER"))
                    points += 1;
                else if (typeName.startsWith("GOLD") || typeName.startsWith("CHAINMAIL"))
                    points += 1;
                else if (typeName.startsWith("IRON"))
                    points += 2;
                else if (typeName.startsWith("DIAMOND"))
                    points += 3;
            }
        }

        return points;
    }

    /**
     * Agenda aplicação de knockback customizado
     */
    private void scheduleKnockback(Player attacker, Player victim) {
        // Aplicar knockback no próximo tick para sobrescrever o do Minecraft
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!victim.isOnline() || victim.isDead()) {
                    return;
                }

                applyCustomKnockback(attacker, victim);
            }
        }.runTaskLater(plugin, 1L);
    }

    /**
     * Aplica knockback customizado com direcionamento correto
     */
    private void applyCustomKnockback(Player attacker, Player victim) {
        // Calcular direção do knockback (de atacante para vítima)
        Location attackerLoc = attacker.getLocation();
        Location victimLoc = victim.getLocation();

        double dx = victimLoc.getX() - attackerLoc.getX();
        double dz = victimLoc.getZ() - attackerLoc.getZ();

        // Normalizar vetor horizontal
        double distance = Math.sqrt(dx * dx + dz * dz);
        if (distance < 0.001) {
            // Evitar divisão por zero - usar direção que a vítima está olhando
            float yaw = victimLoc.getYaw();
            dx = -Math.sin(Math.toRadians(yaw));
            dz = Math.cos(Math.toRadians(yaw));
            distance = 1.0;
        }

        // Knockback base
        double knockbackX = (dx / distance) * 0.4 * knockbackMultiplier;
        double knockbackZ = (dz / distance) * 0.4 * knockbackMultiplier;
        double knockbackY = 0.4 * knockbackVerticalMultiplier;

        // Aplicar modificador de Knockback da espada
        ItemStack weapon = attacker.getItemInHand();
        if (weapon != null && weapon.containsEnchantment(org.bukkit.enchantments.Enchantment.KNOCKBACK)) {
            int level = weapon.getEnchantmentLevel(org.bukkit.enchantments.Enchantment.KNOCKBACK);
            knockbackX *= (1 + level * 0.5);
            knockbackZ *= (1 + level * 0.5);
        }

        // Se vítima está correndo, reduzir knockback recebido levemente
        if (victim.isSprinting()) {
            knockbackX *= 0.6;
            knockbackZ *= 0.6;
        }

        // Se atacante está correndo, aumentar knockback
        if (attacker.isSprinting()) {
            knockbackX *= 1.3;
            knockbackZ *= 1.3;
        }

        // Aplicar velocidade
        Vector velocity = new Vector(knockbackX, knockbackY, knockbackZ);
        victim.setVelocity(velocity);
    }

    // ==================== HIT DELAY ====================

    /**
     * Verifica se atacante pode dar dano (hit delay)
     */
    private boolean canDealDamage(Player attacker, Player victim) {
        String key = attacker.getUniqueId().toString() + ":" + victim.getUniqueId().toString();
        Long lastHit = lastDamageTime.get(UUID.nameUUIDFromBytes(key.getBytes()));

        if (lastHit == null) {
            return true;
        }

        // Converter ticks para millisegundos (1 tick = 50ms)
        long delayMs = hitDelayTicks * 50L;
        return System.currentTimeMillis() - lastHit >= delayMs;
    }

    /**
     * Registra um hit para controle de hit delay
     */
    private void registerHit(Player attacker, Player victim) {
        String key = attacker.getUniqueId().toString() + ":" + victim.getUniqueId().toString();
        lastDamageTime.put(UUID.nameUUIDFromBytes(key.getBytes()), System.currentTimeMillis());
    }

    // ==================== ATACANTE TRACKING ====================

    /**
     * Registra o último atacante de um jogador
     */
    private void registerAttacker(Player victim, Player attacker) {
        UUID victimUuid = victim.getUniqueId();
        lastAttacker.put(victimUuid, attacker.getUniqueId());
        lastAttackerTime.put(victimUuid, System.currentTimeMillis());
    }

    /**
     * Obtém o último atacante de um jogador
     *
     * @param player Jogador vítima
     * @return Último atacante ou null se não houver ou expirou
     */
    public Player getLastAttacker(Player player) {
        UUID attackerUuid = lastAttacker.get(player.getUniqueId());
        if (attackerUuid == null) {
            return null;
        }

        Long attackerTime = lastAttackerTime.get(player.getUniqueId());
        if (attackerTime == null ||
                System.currentTimeMillis() - attackerTime > ATTACKER_TRACK_DURATION_MS) {
            // Expirou
            lastAttacker.remove(player.getUniqueId());
            lastAttackerTime.remove(player.getUniqueId());
            return null;
        }

        return plugin.getServer().getPlayer(attackerUuid);
    }

    /**
     * Obtém o UUID do último atacante
     */
    public UUID getLastAttackerUuid(Player player) {
        return lastAttacker.get(player.getUniqueId());
    }

    // ==================== VERIFICAÇÕES ====================

    /**
     * Verifica se jogador está protegido de dano
     */
    public boolean isProtectedFromDamage(Player player) {
        if (player == null || !player.isOnline()) {
            return true;
        }

        // Em modo criativo ou espectador
        if (player.getGameMode() == GameMode.CREATIVE ||
                player.getGameMode() == GameMode.SPECTATOR) {
            return true;
        }

        // Verificar com StateManager
        PlayerStateManager stateManager = plugin.getStateManager();
        if (stateManager != null) {
            // Protegido no spawn, modo admin ou espectando
            if (stateManager.isProtected(player)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Verifica se jogador está invulnerável temporariamente
     */
    public boolean isInvulnerable(Player player) {
        Long endTime = invulnerabilityEnd.get(player.getUniqueId());
        if (endTime == null) {
            return false;
        }

        if (System.currentTimeMillis() >= endTime) {
            invulnerabilityEnd.remove(player.getUniqueId());
            return false;
        }

        return true;
    }

    /**
     * Verifica se um jogador pode receber dano de outro
     *
     * @param attacker Atacante
     * @param victim   Vítima
     * @return true se pode receber dano
     */
    public boolean canReceiveDamageFrom(Player attacker, Player victim) {
        // Não pode se dar dano
        if (attacker.equals(victim)) {
            return false;
        }

        // Vítima protegida
        if (isProtectedFromDamage(victim)) {
            return false;
        }

        // Atacante protegido (não pode atacar do spawn)
        if (isProtectedFromDamage(attacker)) {
            return false;
        }

        // Vítima invulnerável
        if (isInvulnerable(victim)) {
            return false;
        }

        // Verificar se ambos estão em duelo diferente
        DuelManager duelManager = plugin.getDuelManager();
        if (duelManager != null) {
            // Se vítima está em duelo, só pode receber dano do oponente do duelo
            if (duelManager.isInDuel(victim)) {
                DuelMatch match = duelManager.getPlayerMatch(victim);
                Player opponent = match != null ? match.getOpponentPlayer(victim) : null;
                if (opponent == null || !opponent.equals(attacker)) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Verifica line-of-sight entre atacante e vítima
     */
    private boolean hasLineOfSight(Player attacker, Player victim) {
        if (attacker.getWorld() != victim.getWorld()) {
            return false;
        }

        // Usar método nativo do Bukkit
        return attacker.hasLineOfSight(victim);
    }

    /**
     * Obtém o jogador atacante de uma entidade (pode ser projétil)
     */
    private Player getAttackerFromEntity(Entity entity) {
        if (entity instanceof Player) {
            return (Player) entity;
        }

        if (entity instanceof Projectile) {
            Projectile projectile = (Projectile) entity;
            if (projectile.getShooter() instanceof Player) {
                return (Player) projectile.getShooter();
            }
        }

        return null;
    }

    // ==================== MODIFICADORES DE DANO ====================

    /**
     * Aplica modificadores de dano baseado em Elo, kits, etc
     */
    private double applyDamageModifiers(Player attacker, Player victim, double baseDamage) {
        double damage = baseDamage;

        // Aplicar modificador de liga se disponível
        LeagueManager leagueManager = plugin.getLeagueManager();
        if (leagueManager != null) {
            // Por enquanto, sem modificador de liga
            // Pode ser implementado: jogadores de liga mais alta dão mais dano
        }

        // Aplicar modificador de força se tiver poção
        if (attacker.hasPotionEffect(PotionEffectType.INCREASE_DAMAGE)) {
            for (PotionEffect effect : attacker.getActivePotionEffects()) {
                if (effect.getType().equals(PotionEffectType.INCREASE_DAMAGE)) {
                    int level = effect.getAmplifier() + 1;
                    // Força aumenta 130% de dano base por nível no 1.8
                    damage *= (1 + (level * 1.3));
                    break;
                }
            }
        }

        // Aplicar modificador de fraqueza se vítima tiver
        if (victim.hasPotionEffect(PotionEffectType.WEAKNESS)) {
            for (PotionEffect effect : victim.getActivePotionEffects()) {
                if (effect.getType().equals(PotionEffectType.WEAKNESS)) {
                    int level = effect.getAmplifier() + 1;
                    // Fraqueza reduz 0.5 corações por nível
                    damage = Math.max(0, damage - (level * 1.0));
                    break;
                }
            }
        }

        return damage;
    }

    // ==================== INTEGRAÇÃO COM OUTROS MANAGERS ====================

    /**
     * Notifica entrada em combate
     */
    private void notifyCombatEnter(Player player, Player opponent) {
        PlayerStateManager stateManager = plugin.getStateManager();
        if (stateManager != null) {
            stateManager.enterCombat(player, opponent);
        }
    }

    /**
     * Marca jogador como tendo sido danificado neste tick
     */
    private void markDamagedThisTick(Player player) {
        UUID uuid = player.getUniqueId();
        damagedThisTick.add(uuid);

        // Limpar no próximo tick
        new BukkitRunnable() {
            @Override
            public void run() {
                damagedThisTick.remove(uuid);
            }
        }.runTaskLater(plugin, 1L);
    }

    // ==================== MÉTODOS PÚBLICOS ====================

    /**
     * Aplica dano customizado com bypass de armor
     *
     * @param player      Jogador a receber dano
     * @param damage      Quantidade de dano
     * @param bypassArmor Se deve ignorar armadura
     * @param source      Fonte do dano (pode ser null)
     */
    public void applyDamage(Player player, double damage, boolean bypassArmor, Player source) {
        if (player == null || !player.isOnline() || player.isDead()) {
            return;
        }

        if (isProtectedFromDamage(player)) {
            return;
        }

        // Aplicar redução de armadura se não for bypass
        double finalDamage = damage;
        if (!bypassArmor) {
            double armorPoints = getArmorPoints(player);
            double reduction = Math.min(0.80, armorPoints / 25.0);
            finalDamage = damage * (1 - reduction);
        }

        // Aplicar dano
        player.damage(finalDamage);

        // Registrar atacante se houver
        if (source != null) {
            registerAttacker(player, source);
            notifyCombatEnter(player, source);
        }
    }

    /**
     * Aplica knockback customizado em um jogador
     *
     * @param player    Jogador a receber knockback
     * @param direction Direção do knockback (será normalizada)
     * @param strength  Força do knockback (1.0 = normal)
     */
    public void applyKnockback(Player player, Vector direction, double strength) {
        if (player == null || !player.isOnline() || player.isDead()) {
            return;
        }

        Vector normalized = direction.normalize();
        Vector velocity = normalized.multiply(strength * knockbackMultiplier);
        velocity.setY(0.4 * knockbackVerticalMultiplier);

        player.setVelocity(velocity);
    }

    /**
     * Define invulnerabilidade temporária para um jogador
     *
     * @param player     Jogador
     * @param durationMs Duração em millisegundos
     */
    public void setInvulnerable(Player player, long durationMs) {
        invulnerabilityEnd.put(player.getUniqueId(), System.currentTimeMillis() + durationMs);
    }

    /**
     * Cancela invulnerabilidade de um jogador
     */
    public void cancelInvulnerability(Player player) {
        invulnerabilityEnd.remove(player.getUniqueId());
    }

    /**
     * Registra altura antes de teleporte (para correção de fall damage)
     */
    public void registerPreTeleportHeight(Player player) {
        preTeleportHeight.put(player.getUniqueId(), player.getLocation().getY());
    }

    /**
     * Remove todos os fire ticks de um jogador
     */
    public void extinguish(Player player) {
        player.setFireTicks(0);
    }

    // ==================== TASKS ====================

    /**
     * Inicia task de limpeza de dados expirados
     */
    private void startCleanupTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                long expireTime = 5 * 60 * 1000; // 5 minutos

                // Limpar hit delays antigos
                lastDamageTime.entrySet().removeIf(entry -> now - entry.getValue() > expireTime);

                // Limpar atacantes antigos
                lastAttacker.entrySet().removeIf(entry -> {
                    Long time = lastAttackerTime.get(entry.getKey());
                    return time == null || now - time > ATTACKER_TRACK_DURATION_MS;
                });
                lastAttackerTime.entrySet().removeIf(entry -> now - entry.getValue() > ATTACKER_TRACK_DURATION_MS);

                // Limpar invulnerabilidades expiradas
                invulnerabilityEnd.entrySet().removeIf(entry -> now >= entry.getValue());
            }
        }.runTaskTimer(plugin, 20 * 60, 20 * 60); // A cada minuto
    }

    /**
     * Inicia task de verificação de void
     */
    private void startVoidCheckTask() {
        if (!fixVoidDamage) {
            return;
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getLocation().getY() < voidKillHeight) {
                        // Jogador está no void, forçar dano
                        if (!isProtectedFromDamage(player) && !player.isDead()) {
                            player.damage(9999);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 10L, 10L); // A cada 0.5 segundos
    }

    /**
     * Limpa dados de um jogador
     */
    private void clearPlayerData(UUID uuid) {
        lastAttacker.remove(uuid);
        lastAttackerTime.remove(uuid);
        invulnerabilityEnd.remove(uuid);
        damagedThisTick.remove(uuid);
        preTeleportHeight.remove(uuid);

        // Limpar hit delays relacionados ao jogador
        String uuidStr = uuid.toString();
        lastDamageTime.entrySet().removeIf(entry -> entry.getKey().toString().contains(uuidStr));
    }

    // ==================== GETTERS/SETTERS DE CONFIGURAÇÃO ====================

    public int getHitDelayTicks() {
        return hitDelayTicks;
    }

    public void setHitDelayTicks(int ticks) {
        this.hitDelayTicks = ticks;
    }

    public double getKnockbackMultiplier() {
        return knockbackMultiplier;
    }

    public void setKnockbackMultiplier(double multiplier) {
        this.knockbackMultiplier = multiplier;
    }

    public double getKnockbackVerticalMultiplier() {
        return knockbackVerticalMultiplier;
    }

    public void setKnockbackVerticalMultiplier(double multiplier) {
        this.knockbackVerticalMultiplier = multiplier;
    }

    public int getVoidKillHeight() {
        return voidKillHeight;
    }

    public void setVoidKillHeight(int height) {
        this.voidKillHeight = height;
    }

    public void setFixHitDelay(boolean fix) {
        this.fixHitDelay = fix;
    }

    public void setFixKnockback(boolean fix) {
        this.fixKnockback = fix;
    }

    public void setFixFallDamage(boolean fix) {
        this.fixFallDamage = fix;
    }

    public void setFixVoidDamage(boolean fix) {
        this.fixVoidDamage = fix;
    }

    public void setFixFireTicks(boolean fix) {
        this.fixFireTicks = fix;
    }

    public void setFixSweepingDamage(boolean fix) {
        this.fixSweepingDamage = fix;
    }

    public void setFixArmorBypass(boolean fix) {
        this.fixArmorBypass = fix;
    }

    public void setFixRegenDuringDamage(boolean fix) {
        this.fixRegenDuringDamage = fix;
    }

    // ==================== DEBUG ====================

    /**
     * Obtém informações de debug
     */
    public String getDebugInfo() {
        return String.format(
                "DamageManager: hitDelay=%d, knockback=%.2f, voidKill=%d, " +
                        "tracking=%d atacantes, %d invulneráveis",
                hitDelayTicks, knockbackMultiplier, voidKillHeight,
                lastAttacker.size(), invulnerabilityEnd.size());
    }

    /**
     * Chamado quando o plugin é desabilitado
     */
    public void shutdown() {
        lastDamageTime.clear();
        lastAttacker.clear();
        lastAttackerTime.clear();
        invulnerabilityEnd.clear();
        damagedThisTick.clear();
        preTeleportHeight.clear();
    }
}
