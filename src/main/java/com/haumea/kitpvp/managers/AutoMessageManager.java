package com.haumea.kitpvp.managers;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.utils.ChatStorage;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.Random;

/**
 * Gerenciador de mensagens automáticas (Announcer)
 * 
 * Envia mensagens configuráveis para todos os jogadores online
 * em intervalos definidos no messages.yml.
 * 
 * @author HaumeaMC
 */
public class AutoMessageManager {

    private final HaumeaMC plugin;
    private final Random random;
    private BukkitTask task;
    private int currentIndex;

    public AutoMessageManager(HaumeaMC plugin) {
        this.plugin = plugin;
        this.random = new Random();
        this.currentIndex = 0;

        start();
    }

    /**
     * Inicia o loop de mensagens automáticas
     */
    public void start() {
        // Verificar se está habilitado
        MessageManager msgManager = plugin.getMessageManager();
        if (msgManager == null || msgManager.getConfig() == null) {
            plugin.getLogger().warning("[AutoMessage] MessageManager não disponível!");
            return;
        }

        boolean enabled = msgManager.getConfig().getBoolean("announcer.enabled", true);
        if (!enabled) {
            plugin.getLogger().info("[AutoMessage] Sistema de mensagens automáticas desabilitado.");
            return;
        }

        // Obter intervalo em segundos e converter para ticks (20 ticks = 1 segundo)
        int intervalSeconds = msgManager.getConfig().getInt("announcer.interval", 300);
        long intervalTicks = intervalSeconds * 20L;

        // Obter lista de mensagens
        List<String> messages = msgManager.getMessageList("announcer.messages");
        if (messages == null || messages.isEmpty()) {
            plugin.getLogger().warning("[AutoMessage] Nenhuma mensagem configurada em announcer.messages!");
            return;
        }

        // Verificar se é aleatório
        boolean isRandom = msgManager.getConfig().getBoolean("announcer.random", true);

        plugin.getLogger().info("[AutoMessage] Iniciando sistema de mensagens automáticas:");
        plugin.getLogger().info("[AutoMessage] - Intervalo: " + intervalSeconds + " segundos");
        plugin.getLogger().info("[AutoMessage] - Mensagens: " + messages.size());
        plugin.getLogger().info("[AutoMessage] - Modo: " + (isRandom ? "Aleatório" : "Sequencial"));

        // Criar a tarefa repetitiva
        this.task = new BukkitRunnable() {
            @Override
            public void run() {
                sendNextMessage(messages, isRandom);
            }
        }.runTaskTimer(plugin, intervalTicks, intervalTicks);
    }

    /**
     * Envia a próxima mensagem para todos os jogadores online
     */
    private void sendNextMessage(List<String> messages, boolean isRandom) {
        // Verificar se há jogadores online
        if (Bukkit.getOnlinePlayers().isEmpty()) {
            return;
        }

        // Selecionar mensagem
        String message;
        if (isRandom) {
            message = messages.get(random.nextInt(messages.size()));
        } else {
            message = messages.get(currentIndex);
            currentIndex = (currentIndex + 1) % messages.size();
        }

        // Colorizar e enviar para todos os jogadores
        // Suporte a \n para quebra de linha
        String[] lines = message.split("\\n");
        for (String line : lines) {
            Bukkit.broadcastMessage(ChatStorage.colorize(line));
        }
    }

    /**
     * Para o loop de mensagens automáticas
     */
    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
            plugin.getLogger().info("[AutoMessage] Sistema de mensagens automáticas parado.");
        }
    }

    /**
     * Reinicia o sistema de mensagens automáticas
     * Útil para aplicar novas configurações após reload
     */
    public void restart() {
        stop();
        currentIndex = 0;
        start();
    }

    /**
     * Desliga o sistema (chamado no onDisable)
     */
    public void shutdown() {
        stop();
    }

    /**
     * Verifica se o sistema está rodando
     */
    public boolean isRunning() {
        return task != null;
    }
}
