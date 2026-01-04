package com.haumea.kitpvp.managers;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.models.chatevents.ChatEvent;
import com.haumea.kitpvp.models.chatevents.ChatEventType;
import com.haumea.kitpvp.models.chatevents.impl.*;
import com.haumea.kitpvp.profile.PlayerProfile;
import com.haumea.kitpvp.utils.ChatStorage;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gerenciador de Eventos de Chat do HaumeaMC.
 * 
 * Sistema de mini-eventos rápidos no chat onde jogadores
 * respondem perguntas/desafios para ganhar prêmios.
 * 
 * Características:
 * - Top 3 ganhadores por evento
 * - Prêmios em moedas diferenciados
 * - Eventos automáticos periódicos
 * - 10 tipos diferentes de eventos
 * 
 * @author HaumeaMC
 */
public class ChatEventManager {

    private final HaumeaMC plugin;

    // Estado do evento atual
    private ChatEvent currentEvent;
    private boolean eventActive;
    private long eventStartTime;

    // Ganhadores do evento atual (Top 3)
    private final List<WinnerEntry> winners;
    private static final int MAX_WINNERS = 3;

    // Players que já responderam (evitar múltiplas tentativas)
    private final Set<UUID> alreadyAnswered;

    // Tasks
    private BukkitTask autoEventTask;
    private BukkitTask eventTimeoutTask;
    private BukkitTask reminderTask;

    // Configurações padrão
    private static final int DEFAULT_EVENT_DURATION_SECONDS = 180; // Duração padrão do evento (3 min)
    private static final int DEFAULT_REMINDER_INTERVAL_SECONDS = 30; // Intervalo padrão de lembrete
    private static final int AUTO_EVENT_INTERVAL_MINUTES = 20; // Intervalo entre eventos automáticos

    // Duração atual do evento (pode ser customizada)
    private int currentEventDuration = DEFAULT_EVENT_DURATION_SECONDS;
    private int currentReminderInterval = DEFAULT_REMINDER_INTERVAL_SECONDS;

    // Prêmios por posição (moedas)
    private static final int REWARD_FIRST = 500;
    private static final int REWARD_SECOND = 300;
    private static final int REWARD_THIRD = 150;

    // Bônus por dificuldade
    private static final double DIFFICULTY_MULTIPLIER_EASY = 1.0;
    private static final double DIFFICULTY_MULTIPLIER_MEDIUM = 1.5;
    private static final double DIFFICULTY_MULTIPLIER_HARD = 2.0;

    // Símbolos e cores
    private static final String SEPARATOR = "§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━";
    private static final String GOLD = "§6";
    private static final String YELLOW = "§e";
    private static final String GREEN = "§a";
    private static final String WHITE = "§f";
    private static final String GRAY = "§7";

    // Cache de tipos para evitar overhead
    private final ChatEventType[] enabledTypes;

    public ChatEventManager(HaumeaMC plugin) {
        this.plugin = plugin;
        this.winners = Collections.synchronizedList(new ArrayList<>());
        this.alreadyAnswered = ConcurrentHashMap.newKeySet();
        this.eventActive = false;

        // Todos os tipos habilitados (menos os removidos pelo usuário)
        this.enabledTypes = new ChatEventType[] {
                ChatEventType.MATH,
                ChatEventType.DESCRAMBLE,
                ChatEventType.TRIVIA,
                ChatEventType.TYPE_RACE,
                ChatEventType.SEQUENCE,
                ChatEventType.MUSIC_GUESS,
                ChatEventType.MOB_GUESS,
                ChatEventType.ITEM_GUESS,
                ChatEventType.RIDDLE,
                ChatEventType.COMPLETE_WORD
        };

        // Iniciar eventos automáticos
        startAutoEvents();
    }

    // ==================== CONTROLE DE EVENTOS ====================

    /**
     * Inicia um evento de chat de um tipo específico com duração padrão
     */
    public boolean startEvent(ChatEventType type) {
        return startEvent(type, DEFAULT_EVENT_DURATION_SECONDS);
    }

    /**
     * Inicia um evento de chat de um tipo específico com duração customizada
     * 
     * @param type            Tipo do evento
     * @param durationSeconds Duração em segundos (0 = padrão)
     */
    public boolean startEvent(ChatEventType type, int durationSeconds) {
        if (eventActive) {
            return false;
        }

        currentEvent = createEventByType(type);
        if (currentEvent == null) {
            return false;
        }

        currentEvent.generate();
        eventActive = true;
        eventStartTime = System.currentTimeMillis();
        currentEventDuration = durationSeconds > 0 ? durationSeconds : DEFAULT_EVENT_DURATION_SECONDS;
        winners.clear();
        alreadyAnswered.clear();

        // Broadcast do início do evento
        broadcastEventStart();

        // Iniciar timeout e reminder do evento
        startEventTimeout();
        startReminderTask();

        return true;
    }

    /**
     * Inicia um evento de chat aleatório
     */
    public boolean startRandomEvent() {
        if (eventActive) {
            return false;
        }

        ChatEventType randomType = enabledTypes[new Random().nextInt(enabledTypes.length)];
        return startEvent(randomType);
    }

    /**
     * Inicia um evento de chat customizado pelo staffer.
     * O staffer define a pergunta/desafio e a resposta esperada.
     * 
     * @param type           Tipo do evento
     * @param customQuestion Pergunta/desafio customizado
     * @param customAnswer   Resposta esperada
     * @return true se iniciado com sucesso
     */
    public boolean startCustomEvent(ChatEventType type, String customQuestion, String customAnswer) {
        return startCustomEvent(type, customQuestion, customAnswer, DEFAULT_EVENT_DURATION_SECONDS);
    }

    /**
     * Inicia um evento de chat customizado com duração customizada.
     * 
     * @param type            Tipo do evento
     * @param customQuestion  Pergunta/desafio customizado
     * @param customAnswer    Resposta esperada
     * @param durationSeconds Duração em segundos (0 = padrão)
     * @return true se iniciado com sucesso
     */
    public boolean startCustomEvent(ChatEventType type, String customQuestion, String customAnswer,
            int durationSeconds) {
        if (eventActive) {
            return false;
        }

        if (customQuestion == null || customQuestion.trim().isEmpty()) {
            return false;
        }

        if (customAnswer == null || customAnswer.trim().isEmpty()) {
            return false;
        }

        currentEvent = createEventByType(type);
        if (currentEvent == null) {
            return false;
        }

        // Definir dados customizados
        currentEvent.setCustomData(customQuestion, customAnswer);

        eventActive = true;
        eventStartTime = System.currentTimeMillis();
        currentEventDuration = durationSeconds > 0 ? durationSeconds : DEFAULT_EVENT_DURATION_SECONDS;
        winners.clear();
        alreadyAnswered.clear();

        // Broadcast do início do evento
        broadcastEventStart();

        // Iniciar timeout e reminder do evento
        startEventTimeout();
        startReminderTask();

        return true;
    }

    /**
     * Processa uma resposta de um jogador
     */
    public boolean processAnswer(Player player, String answer) {
        if (!eventActive || currentEvent == null) {
            return false;
        }

        // Verificar se já respondeu corretamente
        if (alreadyAnswered.contains(player.getUniqueId())) {
            return false;
        }

        // Verificar se já temos 3 ganhadores
        if (winners.size() >= MAX_WINNERS) {
            return false;
        }

        // Verificar se a resposta está correta
        if (!currentEvent.isCorrect(answer)) {
            return false;
        }

        // Marcar que o jogador já respondeu corretamente
        alreadyAnswered.add(player.getUniqueId());

        // Calcular tempo de resposta
        long responseTime = System.currentTimeMillis() - eventStartTime;

        // Adicionar aos ganhadores
        int position = winners.size() + 1;
        int reward = calculateReward(position);

        WinnerEntry entry = new WinnerEntry(player.getUniqueId(), player.getName(), position, reward, responseTime);
        winners.add(entry);

        // Dar prêmio
        giveReward(player, reward);

        // Anunciar posição
        announcePosition(player, position, reward);

        // Incrementar contador de eventos ganhos para conquistas
        PlayerProfile profile = plugin.getProfileManager().getProfile(player);
        if (profile != null) {
            int eventsWon = profile.getData().getCustomData("chat_events_won", 0);
            profile.getData().setCustomData("chat_events_won", eventsWon + 1);

            // Tracking de vitórias consecutivas em 1º lugar (para conquista "lucky")
            if (position == 1) {
                int firstPlaceStreak = profile.getData().getCustomData("first_place_streak", 0);
                firstPlaceStreak++;
                profile.getData().setCustomData("first_place_streak", firstPlaceStreak);

                // Verificar conquista "lucky" (3 primeiros lugares seguidos)
                if (firstPlaceStreak == 3 && plugin.getAchievementManager() != null) {
                    plugin.getAchievementManager().incrementSpecialAchievement(player, "lucky");
                    profile.getData().setCustomData("first_place_streak", 0); // Reset após conquista
                }
            } else {
                // Reset streak se não for 1º lugar
                profile.getData().setCustomData("first_place_streak", 0);
            }

            // Verificar conquistas de eventos
            if (plugin.getAchievementManager() != null) {
                plugin.getAchievementManager().checkAchievements(player);
            }
        }

        // Verificar se completamos o Top 3
        if (winners.size() >= MAX_WINNERS) {
            finishEvent();
        }

        return true;
    }

    /**
     * Finaliza o evento atual (com ou sem 3 ganhadores)
     */
    public void finishEvent() {
        if (!eventActive) {
            return;
        }

        // Cancelar timeout e reminder
        if (eventTimeoutTask != null) {
            eventTimeoutTask.cancel();
            eventTimeoutTask = null;
        }
        stopReminderTask();

        // Broadcast dos resultados
        broadcastEventEnd();

        // Limpar estado
        eventActive = false;
        currentEvent = null;
        winners.clear();
        alreadyAnswered.clear();
    }

    /**
     * Cancela o evento atual sem anunciar resultados
     */
    public void cancelEvent() {
        if (!eventActive) {
            return;
        }

        // Cancelar timeout e reminder
        if (eventTimeoutTask != null) {
            eventTimeoutTask.cancel();
            eventTimeoutTask = null;
        }
        stopReminderTask();

        // Broadcast de cancelamento
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(SEPARATOR);
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("   " + GOLD + "§lEVENTO CANCELADO!");
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(SEPARATOR);
        Bukkit.broadcastMessage("");

        // Limpar estado
        eventActive = false;
        currentEvent = null;
        winners.clear();
        alreadyAnswered.clear();
    }

    /**
     * Pula para o próximo evento (cancela atual e inicia novo)
     */
    public void skipToNextEvent() {
        cancelEvent();
        startRandomEvent();
    }

    // ==================== BROADCASTS ====================

    /**
     * Anuncia o início do evento
     */
    private void broadcastEventStart() {
        ChatEventType type = currentEvent.getType();

        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(SEPARATOR);
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("   " + type.getFullName());
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("   " + WHITE + currentEvent.getQuestion());
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("   " + GRAY + "Digite a resposta no chat!");
        Bukkit.broadcastMessage("   " + GRAY + "Tempo: " + YELLOW + currentEventDuration + "s" +
                GRAY + " | Prêmio: " + GOLD + "§l" + ChatStorage.formatNumber(REWARD_FIRST) + " moedas");
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(SEPARATOR);
        Bukkit.broadcastMessage("");

        // Som para todos
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(player.getLocation(), Sound.NOTE_PLING, 1.0f, 1.5f);
        }
    }

    /**
     * Anuncia o fim do evento com resultados
     */
    private void broadcastEventEnd() {
        ChatEventType type = currentEvent.getType();

        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(SEPARATOR);
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("   " + GOLD + "§l🏆 EVENTO FINALIZADO! 🏆");
        Bukkit.broadcastMessage("");

        // Mostrar resposta correta
        Bukkit.broadcastMessage("   " + GRAY + "Resposta: " + GREEN + "§l" + currentEvent.getAnswer().toUpperCase());
        Bukkit.broadcastMessage("");

        // Mostrar ganhadores
        if (winners.isEmpty()) {
            Bukkit.broadcastMessage("   " + GRAY + "Ninguém acertou a tempo!");
        } else {
            Bukkit.broadcastMessage("   " + YELLOW + "§lGANHADORES:");
            Bukkit.broadcastMessage("");

            for (WinnerEntry winner : winners) {
                String medal;
                String color;
                switch (winner.position) {
                    case 1:
                        medal = "§6§l🥇";
                        color = "§6";
                        break;
                    case 2:
                        medal = "§f§l🥈";
                        color = "§f";
                        break;
                    case 3:
                        medal = "§c§l🥉";
                        color = "§c";
                        break;
                    default:
                        medal = "§7•";
                        color = "§7";
                }

                // Obter prefixo/tag do jogador
                String playerTag = "";
                Player winnerPlayer = Bukkit.getPlayer(winner.playerId);
                if (winnerPlayer != null && winnerPlayer.isOnline()) {
                    DisplayManager displayManager = plugin.getDisplayManager();
                    if (displayManager != null) {
                        DisplayManager.DisplayData displayData = displayManager.getDisplayData(winnerPlayer);
                        if (displayData != null) {
                            playerTag = displayData.getPrefix();
                        }
                    }
                }

                String time = String.format("%.1f", winner.responseTime / 1000.0);
                String playerDisplay = playerTag + winner.playerName;
                Bukkit.broadcastMessage("   " + medal + " " + playerDisplay +
                        GRAY + " §8» " + YELLOW + ChatStorage.formatNumber(winner.reward) + " moedas" +
                        GRAY + " §7(" + time + "s)");
            }
        }

        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(SEPARATOR);
        Bukkit.broadcastMessage("");

        // Som de vitória para ganhadores
        for (WinnerEntry winner : winners) {
            Player player = Bukkit.getPlayer(winner.playerId);
            if (player != null && player.isOnline()) {
                player.playSound(player.getLocation(), Sound.LEVEL_UP, 1.0f, 1.0f);
            }
        }
    }

    /**
     * Anuncia quando um jogador acerta
     */
    private void announcePosition(Player player, int position, int reward) {
        String medal;
        String color;
        String ordinal;
        String effect;

        switch (position) {
            case 1:
                medal = "§6§l🥇";
                color = "§6";
                ordinal = "1º";
                effect = "§e§l✦ ";
                break;
            case 2:
                medal = "§f§l🥈";
                color = "§f";
                ordinal = "2º";
                effect = "§7§l✦ ";
                break;
            case 3:
                medal = "§c§l🥉";
                color = "§c";
                ordinal = "3º";
                effect = "§8§l✦ ";
                break;
            default:
                medal = "§7•";
                color = "§7";
                ordinal = position + "º";
                effect = "";
        }

        // Obter prefixo/tag do jogador
        String playerTag = "";
        DisplayManager displayManager = plugin.getDisplayManager();
        if (displayManager != null) {
            DisplayManager.DisplayData displayData = displayManager.getDisplayData(player);
            if (displayData != null) {
                playerTag = displayData.getPrefix();
            }
        }

        // Nome com tag
        String playerDisplay = playerTag + player.getName();

        // Mensagem bonita e elaborada
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(effect + medal + " " + playerDisplay + " §8» " + color + "§l" + ordinal + " LUGAR!");
        Bukkit.broadcastMessage("    §8├ §fPrêmio: " + GOLD + "+" + ChatStorage.formatNumber(reward) + " moedas");
        Bukkit.broadcastMessage("");

        // Som específico para o jogador
        player.playSound(player.getLocation(), Sound.ORB_PICKUP, 1.0f, 1.2f);
    }

    // ==================== RECOMPENSAS ====================

    /**
     * Calcula a recompensa baseada na posição e dificuldade
     */
    private int calculateReward(int position) {
        int baseReward;
        switch (position) {
            case 1:
                baseReward = REWARD_FIRST;
                break;
            case 2:
                baseReward = REWARD_SECOND;
                break;
            case 3:
                baseReward = REWARD_THIRD;
                break;
            default:
                baseReward = 50;
        }

        // Multiplicador de dificuldade
        double multiplier;
        if (currentEvent != null) {
            switch (currentEvent.getDifficulty()) {
                case 2:
                    multiplier = DIFFICULTY_MULTIPLIER_MEDIUM;
                    break;
                case 3:
                    multiplier = DIFFICULTY_MULTIPLIER_HARD;
                    break;
                default:
                    multiplier = DIFFICULTY_MULTIPLIER_EASY;
            }
        } else {
            multiplier = DIFFICULTY_MULTIPLIER_EASY;
        }

        return (int) (baseReward * multiplier);
    }

    /**
     * Dá a recompensa ao jogador
     */
    private void giveReward(Player player, int amount) {
        plugin.getStatsManager().addMoney(player, (long) amount);
    }

    // ==================== TIMER E AUTO-EVENTOS ====================

    /**
     * Inicia o timeout do evento
     */
    private void startEventTimeout() {
        if (eventTimeoutTask != null) {
            eventTimeoutTask.cancel();
        }

        eventTimeoutTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (eventActive) {
                    // Evento expirou sem 3 ganhadores
                    finishEvent();
                }
            }
        }.runTaskLater(plugin, currentEventDuration * 20L);
    }

    /**
     * Inicia a task de lembrete que repete a pergunta periodicamente
     */
    private void startReminderTask() {
        if (reminderTask != null) {
            reminderTask.cancel();
        }

        // Intervalo em ticks (20 ticks = 1 segundo)
        long intervalTicks = currentReminderInterval * 20L;

        reminderTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!eventActive || currentEvent == null) {
                    cancel();
                    return;
                }

                // Broadcast de lembrete (mais simples que o inicial)
                broadcastReminder();
            }
        }.runTaskTimer(plugin, intervalTicks, intervalTicks);
    }

    /**
     * Para a task de lembrete
     */
    private void stopReminderTask() {
        if (reminderTask != null) {
            reminderTask.cancel();
            reminderTask = null;
        }
    }

    /**
     * Broadcast de lembrete simplificado
     */
    private void broadcastReminder() {
        if (currentEvent == null || !eventActive)
            return;

        ChatEventType type = currentEvent.getType();
        int remaining = getRemainingSeconds();
        int winnersNeeded = MAX_WINNERS - winners.size();

        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("§8§m━━━━━━━━━━━━━━§r " + type.getFullName() + " §8§m━━━━━━━━━━━━━━");
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("   " + WHITE + currentEvent.getQuestion());
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("   " + GRAY + "⏱ Tempo: " + YELLOW + remaining + "s" +
                GRAY + " | Faltam: " + GREEN + winnersNeeded + " ganhador(es)");
        Bukkit.broadcastMessage("");

        // Som sutil de lembrete
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(player.getLocation(), Sound.NOTE_STICKS, 0.5f, 1.0f);
        }
    }

    /**
     * Inicia o sistema de eventos automáticos
     */
    public void startAutoEvents() {
        if (autoEventTask != null) {
            autoEventTask.cancel();
        }

        // Intervalo em ticks (20 ticks = 1 segundo)
        long intervalTicks = AUTO_EVENT_INTERVAL_MINUTES * 60L * 20L;

        autoEventTask = new BukkitRunnable() {
            @Override
            public void run() {
                // Só iniciar se houver jogadores suficientes e não tiver evento ativo
                if (Bukkit.getOnlinePlayers().size() >= 2 && !eventActive) {
                    startRandomEvent();
                }
            }
        }.runTaskTimer(plugin, intervalTicks, intervalTicks);

        plugin.getLogger().info("[ChatEventManager] Eventos automáticos iniciados (intervalo: "
                + AUTO_EVENT_INTERVAL_MINUTES + " minutos)");
    }

    /**
     * Para o sistema de eventos automáticos
     */
    public void stopAutoEvents() {
        if (autoEventTask != null) {
            autoEventTask.cancel();
            autoEventTask = null;
        }
    }

    /**
     * Desliga o manager
     */
    public void shutdown() {
        stopAutoEvents();
        stopReminderTask();
        if (eventActive) {
            cancelEvent();
        }
    }

    // ==================== FACTORY ====================

    /**
     * Cria um evento baseado no tipo
     */
    private ChatEvent createEventByType(ChatEventType type) {
        switch (type) {
            case MATH:
                return new MathEvent();
            case DESCRAMBLE:
                return new DescrambleEvent();
            case TRIVIA:
                return new TriviaEvent();
            case TYPE_RACE:
                return new TypeRaceEvent();
            case SEQUENCE:
                return new SequenceEvent();
            case MUSIC_GUESS:
                return new MusicGuessEvent();
            case MOB_GUESS:
                return new MobGuessEvent();
            case ITEM_GUESS:
                return new ItemGuessEvent();
            case RIDDLE:
                return new RiddleEvent();
            case COMPLETE_WORD:
                return new CompleteWordEvent();
            default:
                return null;
        }
    }

    // ==================== GETTERS ====================

    /**
     * Verifica se há um evento ativo
     */
    public boolean isEventActive() {
        return eventActive;
    }

    /**
     * Obtém o evento atual
     */
    public ChatEvent getCurrentEvent() {
        return currentEvent;
    }

    /**
     * Obtém a lista de ganhadores do evento atual
     */
    public List<WinnerEntry> getWinners() {
        return Collections.unmodifiableList(winners);
    }

    /**
     * Obtém os tipos de eventos habilitados
     */
    public ChatEventType[] getEnabledTypes() {
        return enabledTypes.clone();
    }

    /**
     * Obtém o tempo restante do evento em segundos
     */
    public int getRemainingSeconds() {
        if (!eventActive) {
            return 0;
        }
        long elapsed = (System.currentTimeMillis() - eventStartTime) / 1000;
        return (int) Math.max(0, currentEventDuration - elapsed);
    }

    /**
     * Define o intervalo de lembrete para os próximos eventos
     * 
     * @param seconds Intervalo em segundos
     */
    public void setReminderInterval(int seconds) {
        this.currentReminderInterval = Math.max(5, Math.min(60, seconds)); // Entre 5 e 60 segundos
    }

    /**
     * Obtém o intervalo de lembrete atual
     * 
     * @return Intervalo em segundos
     */
    public int getReminderInterval() {
        return currentReminderInterval;
    }

    /**
     * Obtém a duração atual do evento
     * 
     * @return Duração em segundos
     */
    public int getCurrentEventDuration() {
        return currentEventDuration;
    }

    // ==================== INNER CLASS ====================

    /**
     * Representa um ganhador do evento
     */
    public static class WinnerEntry {
        public final UUID playerId;
        public final String playerName;
        public final int position;
        public final int reward;
        public final long responseTime;

        public WinnerEntry(UUID playerId, String playerName, int position, int reward, long responseTime) {
            this.playerId = playerId;
            this.playerName = playerName;
            this.position = position;
            this.reward = reward;
            this.responseTime = responseTime;
        }
    }
}
