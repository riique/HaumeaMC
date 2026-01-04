package com.haumea.kitpvp.managers;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.models.PlayerData;
import com.haumea.kitpvp.profile.PlayerProfile;
import com.haumea.kitpvp.utils.ChatStorage;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gerenciador de Bounties (Recompensas) do HaumeaMC.
 * 
 * Sistema que permite jogadores colocarem recompensas na cabeça de outros.
 * Quem matar o alvo recebe a recompensa.
 * 
 * PERSISTÊNCIA: Bounties são salvos no customData do jogador alvo.
 * - "bounty_amount" -> Valor total do bounty
 * - "bounty_contributors" -> Map de UUID -> valor contribuído
 * 
 * @author HaumeaMC
 */
public class BountyManager {

    private final HaumeaMC plugin;

    // Cache em memória para jogadores online
    // UUID do alvo -> Bounty total acumulado
    private final Map<UUID, Long> bountyCache;

    // UUID do alvo -> Lista de contribuidores (quem colocou bounty)
    private final Map<UUID, Map<UUID, Long>> contributorsCache;

    // Chaves de customData
    private static final String DATA_KEY_BOUNTY = "bounty_amount";
    private static final String DATA_KEY_CONTRIBUTORS = "bounty_contributors";

    // Taxa da casa (% que vai para o servidor)
    private static final double HOUSE_TAX = 0.05; // 5%

    // Bounty mínimo
    private static final long MIN_BOUNTY = 100;

    // Bounty máximo por jogador
    private static final long MAX_BOUNTY = 1000000;

    // ==================== BOUNTY GLOBAL (AUTOMATIZADO) ====================

    // Configurações do Bounty Global
    private static final long GLOBAL_BOUNTY_AMOUNT = 1000; // 1.000 coins do servidor
    private static final int MIN_KILLSTREAK_FOR_GLOBAL = 5; // Mínimo de killstreak para ser alvo
    private static final long SELECTION_INTERVAL_TICKS = 36000L; // 30 minutos (30 * 60 * 20)
    private static final long ANNOUNCE_INTERVAL_TICKS = 2400L; // 2 minutos (2 * 60 * 20)
    private static final long PARTICLE_INTERVAL_TICKS = 10L; // 0.5 segundos

    // Estado do Bounty Global
    private UUID globalBountyTarget; // Jogador alvo do bounty global
    private boolean hasGlobalBounty; // Se há bounty global ativo
    private BukkitTask selectionTask; // Task de seleção a cada 30min
    private BukkitTask announceTask; // Task de anúncio a cada 2min
    private BukkitTask particleTask; // Task de partículas contínuas

    public BountyManager(HaumeaMC plugin) {
        this.plugin = plugin;
        this.bountyCache = new ConcurrentHashMap<>();
        this.contributorsCache = new ConcurrentHashMap<>();
        this.hasGlobalBounty = false;
        this.globalBountyTarget = null;

        // Iniciar sistema de Bounty Global
        startGlobalBountySystem();
    }

    /**
     * Carrega bounty do PlayerData para o cache (chamado no join)
     */
    public void loadPlayerBounty(Player player) {
        PlayerProfile profile = plugin.getProfileManager().getProfile(player);
        if (profile == null)
            return;

        PlayerData data = profile.getData();
        UUID uuid = player.getUniqueId();

        // Carregar valor do bounty
        Object bountyObj = data.getCustomData(DATA_KEY_BOUNTY);
        if (bountyObj instanceof Number) {
            long bounty = ((Number) bountyObj).longValue();
            if (bounty > 0) {
                bountyCache.put(uuid, bounty);
            }
        }

        // Carregar contribuidores
        Object contribObj = data.getCustomData(DATA_KEY_CONTRIBUTORS);
        if (contribObj instanceof Map) {
            Map<UUID, Long> contributors = new HashMap<>();
            Map<?, ?> rawMap = (Map<?, ?>) contribObj;
            for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                try {
                    UUID contribUuid = UUID.fromString(entry.getKey().toString());
                    long amount = ((Number) entry.getValue()).longValue();
                    contributors.put(contribUuid, amount);
                } catch (Exception ignored) {
                }
            }
            if (!contributors.isEmpty()) {
                contributorsCache.put(uuid, contributors);
            }
        }
    }

    /**
     * Salva bounty do cache para PlayerData (chamado no quit e periodicamente)
     */
    public void savePlayerBounty(Player player) {
        PlayerProfile profile = plugin.getProfileManager().getProfile(player);
        if (profile == null)
            return;

        UUID uuid = player.getUniqueId();
        PlayerData data = profile.getData();

        Long bounty = bountyCache.get(uuid);
        if (bounty != null && bounty > 0) {
            data.setCustomData(DATA_KEY_BOUNTY, bounty);

            // Salvar contribuidores como Map<String, Long> para serialização
            Map<UUID, Long> contributors = contributorsCache.get(uuid);
            if (contributors != null && !contributors.isEmpty()) {
                Map<String, Long> serializable = new HashMap<>();
                for (Map.Entry<UUID, Long> entry : contributors.entrySet()) {
                    serializable.put(entry.getKey().toString(), entry.getValue());
                }
                data.setCustomData(DATA_KEY_CONTRIBUTORS, serializable);
            }
        } else {
            // Sem bounty, limpar dados
            data.removeCustomData(DATA_KEY_BOUNTY);
            data.removeCustomData(DATA_KEY_CONTRIBUTORS);
        }
    }

    /**
     * Limpa cache ao deslogar (após salvar)
     * Também verifica se era o alvo do Bounty Global
     */
    public void onPlayerQuit(Player player) {
        // Verificar se era o alvo do Bounty Global
        if (isGlobalBountyTarget(player)) {
            clearGlobalBounty(false);
            broadcastGlobalBountyExpired();
        }

        savePlayerBounty(player);
        // NÃO limpar cache imediatamente - outros jogadores podem precisar da info
    }

    /**
     * Adiciona bounty a um jogador
     * 
     * @param sender Quem está colocando o bounty
     * @param target Alvo do bounty
     * @param amount Quantidade de coins
     * @return true se adicionou com sucesso
     */
    public boolean addBounty(Player sender, Player target, long amount) {
        // Validações
        if (sender.equals(target)) {
            ChatStorage.send(sender, "bounty.cannot-self");
            return false;
        }

        if (amount < MIN_BOUNTY) {
            ChatStorage.send(sender, "bounty.min-amount", "min", String.valueOf(MIN_BOUNTY));
            return false;
        }

        // Verificar se sender tem coins suficientes
        PlayerProfile senderProfile = plugin.getProfileManager().getProfile(sender);
        if (senderProfile == null || senderProfile.getCoins() < amount) {
            ChatStorage.send(sender, "bounty.not-enough-coins");
            return false;
        }

        // Verificar limite máximo
        long currentBounty = getBounty(target);
        if (currentBounty + amount > MAX_BOUNTY) {
            ChatStorage.send(sender, "bounty.max-reached", "max", ChatStorage.formatNumber(MAX_BOUNTY));
            return false;
        }

        // Remover coins do sender
        senderProfile.removeCoins(amount);

        // Adicionar ao bounty
        UUID targetUuid = target.getUniqueId();
        bountyCache.merge(targetUuid, amount, Long::sum);

        // Registrar contribuição
        contributorsCache.computeIfAbsent(targetUuid, k -> new HashMap<>())
                .merge(sender.getUniqueId(), amount, Long::sum);

        // Incrementar estatística de bounty contribuído no sender
        PlayerData senderData = senderProfile.getData();
        long totalContributed = senderData.getCustomData("bounties_contributed", 0L);
        senderData.setCustomData("bounties_contributed", totalContributed + amount);

        // Notificar
        long totalBounty = getBounty(target);

        ChatStorage.send(sender, "bounty.placed",
                "amount", ChatStorage.formatNumber(amount),
                "target", target.getName());

        // Notificar o alvo
        ChatStorage.send(target, "bounty.received",
                "amount", ChatStorage.formatNumber(amount),
                "total", ChatStorage.formatNumber(totalBounty));
        target.playSound(target.getLocation(), Sound.VILLAGER_NO, 1.0f, 1.0f);

        // Broadcast para TODOS os jogadores (independente do valor)
        broadcastBountyPlaced(target.getName(), totalBounty);

        // IMPORTANTE: Salvar imediatamente no customData para persistência
        savePlayerBounty(target);

        return true;
    }

    /**
     * Processa a morte de um jogador com bounty
     * 
     * @param victim Jogador que morreu
     * @param killer Jogador que matou (pode ser null)
     */
    public void processDeath(Player victim, Player killer) {
        UUID victimUuid = victim.getUniqueId();

        // Processar bounty global se este jogador era o alvo
        processGlobalBountyDeath(victim, killer);

        if (!hasBounty(victim)) {
            return;
        }

        long bountyAmount = bountyCache.remove(victimUuid);
        contributorsCache.remove(victimUuid);

        // Limpar customData da vítima
        PlayerProfile victimProfile = plugin.getProfileManager().getProfile(victim);
        if (victimProfile != null) {
            victimProfile.getData().removeCustomData(DATA_KEY_BOUNTY);
            victimProfile.getData().removeCustomData(DATA_KEY_CONTRIBUTORS);
        }

        if (bountyAmount <= 0) {
            return;
        }

        // Calcular taxa da casa
        long houseCut = (long) (bountyAmount * HOUSE_TAX);
        long killerReward = bountyAmount - houseCut;

        if (killer != null && !killer.equals(victim)) {
            // Dar reward ao killer
            PlayerProfile killerProfile = plugin.getProfileManager().getProfile(killer);
            if (killerProfile != null) {
                killerProfile.addCoins(killerReward);
            }

            // Notificar killer
            ChatStorage.send(killer, "bounty.collected",
                    "amount", ChatStorage.formatNumber(killerReward),
                    "target", victim.getName());
            killer.playSound(killer.getLocation(), Sound.LEVEL_UP, 1.0f, 1.0f);

            // Incrementar conquista de bounty hunter e estatística
            if (plugin.getAchievementManager() != null) {
                plugin.getAchievementManager().incrementSpecialAchievement(killer, "bounty_hunter");
            }

            // Incrementar estatística de bounties coletados
            PlayerData killerData = killerProfile.getData();
            long totalCollected = killerData.getCustomData("bounties_collected", 0L);
            killerData.setCustomData("bounties_collected", totalCollected + killerReward);

            // Broadcast elaborado
            broadcastBountyCollected(killer.getName(), victim.getName(), killerReward);
        } else {
            // Jogador morreu de outra forma, bounty perdido
            broadcastBountyLost(victim.getName(), bountyAmount);
        }

        // Notificar vítima
        ChatStorage.send(victim, "bounty.your-collected",
                "amount", ChatStorage.formatNumber(bountyAmount));
    }

    /**
     * Verifica se um jogador tem bounty
     */
    public boolean hasBounty(Player player) {
        return bountyCache.containsKey(player.getUniqueId()) && bountyCache.get(player.getUniqueId()) > 0;
    }

    /**
     * Obtém o bounty de um jogador
     */
    public long getBounty(Player player) {
        return bountyCache.getOrDefault(player.getUniqueId(), 0L);
    }

    /**
     * Obtém o bounty por UUID
     */
    public long getBounty(UUID uuid) {
        return bountyCache.getOrDefault(uuid, 0L);
    }

    /**
     * Obtém top bounties
     */
    public List<Map.Entry<UUID, Long>> getTopBounties(int limit) {
        List<Map.Entry<UUID, Long>> sorted = new ArrayList<>(bountyCache.entrySet());
        sorted.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));
        return sorted.subList(0, Math.min(limit, sorted.size()));
    }

    /**
     * Obtém quantidade de jogadores com bounty ativo
     */
    public int getActiveBountiesCount() {
        return bountyCache.size();
    }

    /**
     * Limpa bounty de um jogador (admin)
     */
    public void clearBounty(UUID uuid) {
        bountyCache.remove(uuid);
        contributorsCache.remove(uuid);

        // Limpar customData do jogador se online
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            PlayerProfile profile = plugin.getProfileManager().getProfile(player);
            if (profile != null) {
                profile.getData().removeCustomData(DATA_KEY_BOUNTY);
                profile.getData().removeCustomData(DATA_KEY_CONTRIBUTORS);
            }
        }
    }

    /**
     * Limpa todos os bounties (admin)
     */
    public void clearAllBounties() {
        // Limpar customData de todos os jogadores online com bounty
        for (UUID uuid : bountyCache.keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                PlayerProfile profile = plugin.getProfileManager().getProfile(player);
                if (profile != null) {
                    profile.getData().removeCustomData(DATA_KEY_BOUNTY);
                    profile.getData().removeCustomData(DATA_KEY_CONTRIBUTORS);
                }
            }
        }

        bountyCache.clear();
        contributorsCache.clear();
    }

    /**
     * Salva todos os bounties ativos no customData dos jogadores.
     * Chamado durante o shutdown do servidor para garantir persistência.
     */
    public void saveAllBounties() {
        for (UUID uuid : bountyCache.keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                savePlayerBounty(player);
            }
        }
        plugin.getLogger().info("[BountyManager] Salvos " + bountyCache.size() + " bounties ativos.");
    }

    // ==================== MÉTODOS DE BROADCAST ====================

    /**
     * Envia broadcast quando um bounty alto é colocado
     * Formato elaborado com header/message/hint/footer
     * 
     * @param targetName Nome do alvo
     * @param amount     Valor total do bounty
     */
    private void broadcastBountyPlaced(String targetName, long amount) {
        String header = ChatStorage.getMessage("bounty.placed-broadcast.header");
        String message = ChatStorage.getMessage("bounty.placed-broadcast.message",
                "target", targetName,
                "amount", ChatStorage.formatNumber(amount));
        String hint = ChatStorage.getMessage("bounty.placed-broadcast.hint");
        String footer = ChatStorage.getMessage("bounty.placed-broadcast.footer");

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage("");
            p.sendMessage(header);
            p.sendMessage(message);
            p.sendMessage(hint);
            p.sendMessage(footer);
            p.sendMessage("");
            p.playSound(p.getLocation(), Sound.NOTE_PLING, 0.5f, 1.0f);
        }
    }

    /**
     * Envia broadcast quando um bounty é coletado
     * Formato elaborado com header/message/footer
     * 
     * @param killerName Nome de quem coletou
     * @param victimName Nome de quem tinha o bounty
     * @param amount     Valor coletado
     */
    private void broadcastBountyCollected(String killerName, String victimName, long amount) {
        String header = ChatStorage.getMessage("bounty.collected-broadcast.header");
        String message = ChatStorage.getMessage("bounty.collected-broadcast.message",
                "killer", killerName,
                "target", victimName,
                "amount", ChatStorage.formatNumber(amount));
        String footer = ChatStorage.getMessage("bounty.collected-broadcast.footer");

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage("");
            p.sendMessage(header);
            p.sendMessage(message);
            p.sendMessage(footer);
            p.sendMessage("");
            p.playSound(p.getLocation(), Sound.NOTE_PLING, 0.5f, 1.5f);
        }
    }

    /**
     * Envia broadcast quando um bounty é perdido
     * Formato elaborado com header/message/footer
     * 
     * @param victimName Nome de quem tinha o bounty
     * @param amount     Valor perdido
     */
    private void broadcastBountyLost(String victimName, long amount) {
        String header = ChatStorage.getMessage("bounty.lost-broadcast.header");
        String message = ChatStorage.getMessage("bounty.lost-broadcast.message",
                "target", victimName,
                "amount", ChatStorage.formatNumber(amount));
        String footer = ChatStorage.getMessage("bounty.lost-broadcast.footer");

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage("");
            p.sendMessage(header);
            p.sendMessage(message);
            p.sendMessage(footer);
            p.sendMessage("");
        }
    }

    // ==================== BOUNTY GLOBAL (AUTOMATIZADO) ====================

    /**
     * Inicia o sistema de Bounty Global automatizado.
     * - Task de seleção a cada 30 minutos
     * - Task de anúncio de localização a cada 2 minutos
     * - Task de partículas a cada 0.5 segundos
     */
    private void startGlobalBountySystem() {
        // Task de seleção a cada 30 minutos
        selectionTask = new BukkitRunnable() {
            @Override
            public void run() {
                selectGlobalBountyTarget();
            }
        }.runTaskTimer(plugin, SELECTION_INTERVAL_TICKS, SELECTION_INTERVAL_TICKS);

        // Task de anúncio a cada 2 minutos (só executa se há alvo)
        announceTask = new BukkitRunnable() {
            @Override
            public void run() {
                announceGlobalBountyLocation();
            }
        }.runTaskTimer(plugin, ANNOUNCE_INTERVAL_TICKS, ANNOUNCE_INTERVAL_TICKS);

        // Task de partículas a cada 0.5 segundos
        particleTask = new BukkitRunnable() {
            @Override
            public void run() {
                spawnGlobalBountyParticles();
            }
        }.runTaskTimer(plugin, PARTICLE_INTERVAL_TICKS, PARTICLE_INTERVAL_TICKS);

        plugin.getLogger().info("[BountyManager] Sistema de Bounty Global iniciado.");
    }

    /**
     * Desliga o sistema de Bounty Global.
     * Chamado no onDisable do plugin.
     */
    public void shutdownGlobalBounty() {
        if (selectionTask != null) {
            selectionTask.cancel();
            selectionTask = null;
        }
        if (announceTask != null) {
            announceTask.cancel();
            announceTask = null;
        }
        if (particleTask != null) {
            particleTask.cancel();
            particleTask = null;
        }

        // Limpar bounty global se existir
        if (hasGlobalBounty && globalBountyTarget != null) {
            clearGlobalBounty(false);
        }

        plugin.getLogger().info("[BountyManager] Sistema de Bounty Global desligado.");
    }

    /**
     * Seleciona o jogador com maior killstreak online para ser o alvo do Bounty
     * Global.
     * Requisitos:
     * - Killstreak >= MIN_KILLSTREAK_FOR_GLOBAL (5)
     * - Não estar em duelo
     * - Estar online
     */
    private void selectGlobalBountyTarget() {
        Player bestCandidate = null;
        int highestStreak = MIN_KILLSTREAK_FOR_GLOBAL - 1;

        StatsManager statsManager = plugin.getStatsManager();
        DuelManager duelManager = plugin.getDuelManager();

        for (Player player : Bukkit.getOnlinePlayers()) {
            // Ignorar jogadores em duelo
            if (duelManager != null && duelManager.isInDuel(player)) {
                continue;
            }

            int streak = statsManager.getKillstreak(player);
            if (streak > highestStreak) {
                highestStreak = streak;
                bestCandidate = player;
            }
        }

        // Se não encontrou candidato válido
        if (bestCandidate == null) {
            // Se já tinha alvo, manter (não cancelar automaticamente)
            if (!hasGlobalBounty) {
                plugin.getLogger().info("[BountyManager] Nenhum jogador elegivel para Bounty Global (streak < "
                        + MIN_KILLSTREAK_FOR_GLOBAL + ")");
            }
            return;
        }

        // Se é o mesmo alvo atual, não fazer nada
        if (hasGlobalBounty && globalBountyTarget != null && globalBountyTarget.equals(bestCandidate.getUniqueId())) {
            return;
        }

        // Se já tinha outro alvo, limpar primeiro (sem broadcast de perda)
        if (hasGlobalBounty && globalBountyTarget != null) {
            clearGlobalBounty(false);
        }

        // Definir novo alvo
        setGlobalBountyTarget(bestCandidate);
    }

    /**
     * Define um jogador como alvo do Bounty Global.
     * Adiciona 1.000 coins de bounty (do servidor) e anuncia.
     * 
     * @param player Jogador alvo
     */
    private void setGlobalBountyTarget(Player player) {
        if (player == null)
            return;

        UUID uuid = player.getUniqueId();

        // Adicionar bounty do servidor (não remove de ninguém)
        bountyCache.merge(uuid, GLOBAL_BOUNTY_AMOUNT, Long::sum);

        // Marcar contribuidor como "SERVIDOR" (UUID especial)
        contributorsCache.computeIfAbsent(uuid, k -> new HashMap<>())
                .merge(new UUID(0, 0), GLOBAL_BOUNTY_AMOUNT, Long::sum); // UUID(0,0) = servidor

        // Atualizar estado
        globalBountyTarget = uuid;
        hasGlobalBounty = true;

        // Salvar no customData
        savePlayerBounty(player);

        // Broadcast especial de Bounty Global
        broadcastGlobalBountyPlaced(player);

        plugin.getLogger().info("[BountyManager] Bounty Global colocado em " + player.getName() +
                " (Streak: " + plugin.getStatsManager().getKillstreak(player) + ")");
    }

    /**
     * Limpa o Bounty Global atual.
     * 
     * @param wasCollected true se foi coletado (alguém matou), false se apenas
     *                     removido
     */
    private void clearGlobalBounty(boolean wasCollected) {
        if (!hasGlobalBounty || globalBountyTarget == null)
            return;

        // Remover bounty global do cache (apenas a parte do servidor)
        Long currentBounty = bountyCache.get(globalBountyTarget);
        if (currentBounty != null) {
            long newBounty = currentBounty - GLOBAL_BOUNTY_AMOUNT;
            if (newBounty <= 0) {
                bountyCache.remove(globalBountyTarget);
                contributorsCache.remove(globalBountyTarget);
            } else {
                bountyCache.put(globalBountyTarget, newBounty);
                // Remover contribuição do servidor
                Map<UUID, Long> contribs = contributorsCache.get(globalBountyTarget);
                if (contribs != null) {
                    contribs.remove(new UUID(0, 0));
                }
            }
        }

        hasGlobalBounty = false;
        globalBountyTarget = null;
    }

    /**
     * Anuncia a localização do alvo do Bounty Global no chat.
     * Executado a cada 2 minutos.
     */
    private void announceGlobalBountyLocation() {
        if (!hasGlobalBounty || globalBountyTarget == null)
            return;

        Player target = Bukkit.getPlayer(globalBountyTarget);
        if (target == null || !target.isOnline()) {
            // Alvo deslogou, limpar bounty global
            clearGlobalBounty(false);
            broadcastGlobalBountyExpired();
            return;
        }

        // Verificar se entrou em duelo
        DuelManager duelManager = plugin.getDuelManager();
        if (duelManager != null && duelManager.isInDuel(target)) {
            return; // Não anunciar enquanto em duelo
        }

        Location loc = target.getLocation();
        long totalBounty = getBounty(target);

        // Broadcast de localização
        String header = ChatStorage.getMessage("bounty.global.location.header");
        String message = ChatStorage.getMessage("bounty.global.location.message",
                "target", target.getName(),
                "amount", ChatStorage.formatNumber(totalBounty),
                "x", String.valueOf(loc.getBlockX()),
                "y", String.valueOf(loc.getBlockY()),
                "z", String.valueOf(loc.getBlockZ()));
        String footer = ChatStorage.getMessage("bounty.global.location.footer");

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage("");
            p.sendMessage(header);
            p.sendMessage(message);
            p.sendMessage(footer);
            p.sendMessage("");
            p.playSound(p.getLocation(), Sound.NOTE_BASS_GUITAR, 0.5f, 1.2f);
        }
    }

    /**
     * Spawna partículas de "coroa" sobre a cabeça do alvo do Bounty Global.
     * Executado a cada 0.5 segundos para efeito visual contínuo.
     */
    private void spawnGlobalBountyParticles() {
        if (!hasGlobalBounty || globalBountyTarget == null)
            return;

        Player target = Bukkit.getPlayer(globalBountyTarget);
        if (target == null || !target.isOnline())
            return;

        Location head = target.getLocation().add(0, 2.3, 0);
        World world = head.getWorld();

        // Criar efeito de "coroa" com partículas em círculo
        // Anel de partículas mágicas (coroa)
        for (int i = 0; i < 8; i++) {
            double angle = (2 * Math.PI * i) / 8;
            double x = Math.cos(angle) * 0.5;
            double z = Math.sin(angle) * 0.5;

            Location particleLoc = head.clone().add(x, 0, z);
            world.playEffect(particleLoc, Effect.WITCH_MAGIC, 0);
        }

        // Partículas verticais (brilho da coroa)
        for (int i = 0; i < 4; i++) {
            double angle = (2 * Math.PI * i) / 4 + (System.currentTimeMillis() / 500.0);
            double x = Math.cos(angle) * 0.3;
            double z = Math.sin(angle) * 0.3;

            Location sparkLoc = head.clone().add(x, 0.2, z);
            world.playEffect(sparkLoc, Effect.MAGIC_CRIT, 0);
        }

        // Partícula central (ponto de alvo)
        world.playEffect(head, Effect.COLOURED_DUST, 0);
    }

    /**
     * Processa a morte de um jogador que é alvo do Bounty Global.
     * Chamado automaticamente pelo processDeath().
     * 
     * @param victim Jogador que morreu
     * @param killer Jogador que matou (pode ser null)
     */
    public void processGlobalBountyDeath(Player victim, Player killer) {
        if (!hasGlobalBounty || globalBountyTarget == null)
            return;
        if (!globalBountyTarget.equals(victim.getUniqueId()))
            return;

        // Limpar bounty global (foi coletado)
        clearGlobalBounty(true);

        // Broadcast de encerramento do bounty global
        if (killer != null && !killer.equals(victim)) {
            broadcastGlobalBountyCollected(killer.getName(), victim.getName());
        }

        plugin.getLogger().info("[BountyManager] Bounty Global de " + victim.getName() + " foi coletado por " +
                (killer != null ? killer.getName() : "morte natural"));
    }

    /**
     * Verifica se um jogador é o alvo do Bounty Global.
     * 
     * @param player Jogador
     * @return true se é o alvo
     */
    public boolean isGlobalBountyTarget(Player player) {
        return hasGlobalBounty && globalBountyTarget != null &&
                globalBountyTarget.equals(player.getUniqueId());
    }

    /**
     * Obtém o jogador alvo do Bounty Global atual.
     * 
     * @return Player alvo ou null se não há bounty global
     */
    public Player getGlobalBountyTargetPlayer() {
        if (!hasGlobalBounty || globalBountyTarget == null)
            return null;
        return Bukkit.getPlayer(globalBountyTarget);
    }

    /**
     * Verifica se há um Bounty Global ativo.
     * 
     * @return true se há bounty global
     */
    public boolean hasGlobalBounty() {
        return hasGlobalBounty && globalBountyTarget != null;
    }

    // ==================== BROADCASTS DO BOUNTY GLOBAL ====================

    /**
     * Broadcast quando um Bounty Global é colocado.
     */
    private void broadcastGlobalBountyPlaced(Player target) {
        String header = ChatStorage.getMessage("bounty.global.placed.header");
        String message = ChatStorage.getMessage("bounty.global.placed.message",
                "target", target.getName(),
                "amount", ChatStorage.formatNumber(GLOBAL_BOUNTY_AMOUNT),
                "streak", String.valueOf(plugin.getStatsManager().getKillstreak(target)));
        String hint = ChatStorage.getMessage("bounty.global.placed.hint");
        String footer = ChatStorage.getMessage("bounty.global.placed.footer");

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage("");
            p.sendMessage(header);
            p.sendMessage(message);
            p.sendMessage(hint);
            p.sendMessage(footer);
            p.sendMessage("");

            // Título especial
            p.playSound(p.getLocation(), Sound.WITHER_SPAWN, 0.5f, 1.5f);
        }

        // Título para o alvo
        target.sendTitle(
                ChatStorage.colorize("&c&l* VOCÊ É O ALVO *"),
                ChatStorage.colorize(
                        "&fBounty Global de &e" + ChatStorage.formatNumber(GLOBAL_BOUNTY_AMOUNT) + " coins!"));
    }

    /**
     * Broadcast quando um Bounty Global é coletado.
     */
    private void broadcastGlobalBountyCollected(String killerName, String targetName) {
        String header = ChatStorage.getMessage("bounty.global.collected.header");
        String message = ChatStorage.getMessage("bounty.global.collected.message",
                "killer", killerName,
                "target", targetName,
                "amount", ChatStorage.formatNumber(GLOBAL_BOUNTY_AMOUNT));
        String footer = ChatStorage.getMessage("bounty.global.collected.footer");

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage("");
            p.sendMessage(header);
            p.sendMessage(message);
            p.sendMessage(footer);
            p.sendMessage("");
            p.playSound(p.getLocation(), Sound.LEVEL_UP, 1.0f, 1.0f);
        }
    }

    /**
     * Broadcast quando o Bounty Global expira (alvo deslogou).
     */
    private void broadcastGlobalBountyExpired() {
        String header = ChatStorage.getMessage("bounty.global.expired.header");
        String message = ChatStorage.getMessage("bounty.global.expired.message");
        String footer = ChatStorage.getMessage("bounty.global.expired.footer");

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage("");
            p.sendMessage(header);
            p.sendMessage(message);
            p.sendMessage(footer);
            p.sendMessage("");
        }
    }
}
