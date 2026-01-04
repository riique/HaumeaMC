package com.haumea.kitpvp.commands.chatevent;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.commands.base.BaseCommand;
import com.haumea.kitpvp.commands.base.CommandInfo;
import com.haumea.kitpvp.managers.ChatEventManager;
import com.haumea.kitpvp.models.chatevents.ChatEventType;
import com.haumea.kitpvp.utils.ChatStorage;
import org.bukkit.command.CommandSender;

/**
 * Comando para gerenciar eventos de chat.
 * 
 * Uso:
 * - /chatevent start [tipo] [tempo] - Inicia um evento (tempo em segundos
 * opcional)
 * - /chatevent custom <tipo> <pergunta> | <resposta> [tempo] - Evento
 * customizado
 * - /chatevent stop - Para o evento atual
 * - /chatevent skip - Pula para o próximo evento
 * - /chatevent tipos - Lista todos os tipos de eventos
 * - /chatevent info - Mostra informações do evento atual
 * - /chatevent auto [on/off] - Liga/desliga eventos automáticos
 * - /chatevent reminder <segundos> - Define intervalo de lembrete
 * 
 * @author HaumeaMC
 */
@CommandInfo(name = "chatevent", aliases = { "ce", "eventochat" }, permission = "haumea.chatevent", playerOnly = false)
public class ChatEventCommand extends BaseCommand {

    private final ChatEventManager chatEventManager;

    public ChatEventCommand(HaumeaMC plugin) {
        super(plugin);
        this.chatEventManager = plugin.getChatEventManager();
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sendUsageMessage(sender);
            return;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "start":
            case "iniciar":
                handleStart(sender, args);
                break;
            case "custom":
            case "customizado":
            case "personalizado":
                handleCustom(sender, args);
                break;
            case "stop":
            case "parar":
                handleStop(sender);
                break;
            case "skip":
            case "pular":
                handleSkip(sender);
                break;
            case "tipos":
            case "types":
            case "lista":
                handleTypes(sender);
                break;
            case "info":
            case "status":
                handleInfo(sender);
                break;
            case "auto":
                handleAuto(sender, args);
                break;
            case "reminder":
            case "lembrete":
                handleReminder(sender, args);
                break;
            default:
                sendUsageMessage(sender);
        }
    }

    /**
     * Inicia um evento (aleatório ou tipo específico com presets)
     * Formato: /chatevent start [tipo] [tempo]
     */
    private void handleStart(CommandSender sender, String[] args) {
        if (chatEventManager.isEventActive()) {
            ChatStorage.send(sender, "chatevent.error.already-active");
            return;
        }

        boolean success;
        int duration = 0; // 0 = usar padrão

        if (args.length >= 2) {
            // Verificar se o segundo argumento é um número (tempo) ou um tipo
            ChatEventType type = parseEventType(args[1]);

            if (type == null) {
                // Talvez seja um tempo?
                try {
                    duration = Integer.parseInt(args[1]);
                    success = chatEventManager.startEvent(
                            chatEventManager.getEnabledTypes()[new java.util.Random()
                                    .nextInt(chatEventManager.getEnabledTypes().length)],
                            duration);
                } catch (NumberFormatException e) {
                    ChatStorage.send(sender, "chatevent.error.invalid-type", "type", args[1]);
                    return;
                }
            } else {
                // Tem tipo, verificar se tem tempo também
                if (args.length >= 3) {
                    try {
                        duration = Integer.parseInt(args[2]);
                    } catch (NumberFormatException e) {
                        // Ignorar, usar padrão
                    }
                }
                success = chatEventManager.startEvent(type, duration);
            }
        } else {
            // Evento aleatório
            success = chatEventManager.startRandomEvent();
        }

        if (success) {
            int finalDuration = chatEventManager.getCurrentEventDuration();
            ChatStorage.send(sender, "chatevent.success.started");
            ChatStorage.sendRaw(sender, "&7Duração: &e" + finalDuration + "s &7| Lembrete: &e"
                    + chatEventManager.getReminderInterval() + "s");
        } else {
            ChatStorage.send(sender, "chatevent.error.start-failed");
        }
    }

    /**
     * Inicia um evento customizado com pergunta e resposta definidas pelo staffer
     * Formato: /chatevent custom <tipo> <pergunta> | <resposta> [tempo]
     */
    private void handleCustom(CommandSender sender, String[] args) {
        if (chatEventManager.isEventActive()) {
            ChatStorage.send(sender, "chatevent.error.already-active");
            return;
        }

        if (args.length < 3) {
            sendCustomUsage(sender);
            return;
        }

        // Primeiro argumento após "custom" é o tipo
        ChatEventType type = parseEventType(args[1]);
        if (type == null) {
            ChatStorage.send(sender, "chatevent.error.invalid-type", "type", args[1]);
            return;
        }

        // Juntar os argumentos restantes e separar por |
        StringBuilder fullText = new StringBuilder();
        for (int i = 2; i < args.length; i++) {
            if (fullText.length() > 0) {
                fullText.append(" ");
            }
            fullText.append(args[i]);
        }

        String text = fullText.toString();

        // Verificar se o último "argumento" é um número (tempo)
        int duration = 0;
        String[] parts = text.split("\\s+");
        if (parts.length > 0) {
            try {
                duration = Integer.parseInt(parts[parts.length - 1]);
                // Remover o tempo do texto
                text = text.substring(0, text.lastIndexOf(parts[parts.length - 1])).trim();
            } catch (NumberFormatException e) {
                // Não é tempo, continuar normalmente
            }
        }

        // Separar pergunta e resposta por |
        int separatorIndex = text.indexOf("|");
        if (separatorIndex == -1) {
            ChatStorage.send(sender, "chatevent.error.missing-separator");
            sendCustomUsage(sender);
            return;
        }

        String question = text.substring(0, separatorIndex).trim();
        String answer = text.substring(separatorIndex + 1).trim();

        if (question.isEmpty()) {
            ChatStorage.send(sender, "chatevent.error.empty-question");
            return;
        }

        if (answer.isEmpty()) {
            ChatStorage.send(sender, "chatevent.error.empty-answer");
            return;
        }

        // Iniciar evento customizado
        boolean success = chatEventManager.startCustomEvent(type, question, answer, duration);

        if (success) {
            int finalDuration = chatEventManager.getCurrentEventDuration();
            ChatStorage.send(sender, "chatevent.success.custom-started");
            ChatStorage.sendRaw(sender, "&7Pergunta: &e" + question);
            ChatStorage.sendRaw(sender, "&7Resposta: &a" + answer);
            ChatStorage.sendRaw(sender, "&7Duração: &e" + finalDuration + "s &7| Lembrete: &e"
                    + chatEventManager.getReminderInterval() + "s");
        } else {
            ChatStorage.send(sender, "chatevent.error.custom-failed");
        }
    }

    /**
     * Para o evento atual
     */
    private void handleStop(CommandSender sender) {
        if (!chatEventManager.isEventActive()) {
            ChatStorage.send(sender, "chatevent.error.no-active");
            return;
        }

        chatEventManager.cancelEvent();
        ChatStorage.send(sender, "chatevent.success.stopped");
    }

    /**
     * Pula para o próximo evento
     */
    private void handleSkip(CommandSender sender) {
        chatEventManager.skipToNextEvent();
        ChatStorage.send(sender, "chatevent.success.skipped");
    }

    /**
     * Lista todos os tipos de eventos
     */
    private void handleTypes(CommandSender sender) {
        ChatStorage.sendRaw(sender, "");
        ChatStorage.sendRaw(sender, "&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        ChatStorage.sendRaw(sender, "");
        sender.sendMessage(ChatStorage.getMessage("chatevent.types.header"));
        ChatStorage.sendRaw(sender, "");

        for (ChatEventType type : chatEventManager.getEnabledTypes()) {
            ChatStorage.sendRaw(sender,
                    "   &e" + type.getIcon() + " &f" + type.name() + " &8- " + type.getDescription());
        }

        ChatStorage.sendRaw(sender, "");
        ChatStorage.sendRaw(sender, "   &7Preset: &e/chatevent start <tipo> [tempo]");
        ChatStorage.sendRaw(sender, "   &7Custom: &e/chatevent custom <tipo> <pergunta> | <resposta> [tempo]");
        ChatStorage.sendRaw(sender, "");
        ChatStorage.sendRaw(sender, "&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        ChatStorage.sendRaw(sender, "");
    }

    /**
     * Mostra informações do evento atual
     */
    private void handleInfo(CommandSender sender) {
        ChatStorage.sendRaw(sender, "");
        ChatStorage.sendRaw(sender, "&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        ChatStorage.sendRaw(sender, "");
        sender.sendMessage(ChatStorage.getMessage("chatevent.info.header"));
        ChatStorage.sendRaw(sender, "");

        if (!chatEventManager.isEventActive()) {
            ChatStorage.sendRaw(sender, "   &7Nenhum evento ativo no momento.");
            ChatStorage.sendRaw(sender, "");
            ChatStorage.sendRaw(sender,
                    "   &fIntervalo de lembrete: &e" + chatEventManager.getReminderInterval() + "s");
        } else {
            ChatEventType type = chatEventManager.getCurrentEvent().getType();
            int remaining = chatEventManager.getRemainingSeconds();
            int winners = chatEventManager.getWinners().size();
            boolean isCustom = chatEventManager.getCurrentEvent().isCustom();

            ChatStorage.sendRaw(sender,
                    "   &fTipo: " + type.getFullName() + (isCustom ? " &e(Custom)" : " &7(Preset)"));
            ChatStorage.sendRaw(sender, "   &fPergunta: &e" + chatEventManager.getCurrentEvent().getQuestion());
            ChatStorage.sendRaw(sender, "   &fTempo Restante: &e" + remaining + "s");
            ChatStorage.sendRaw(sender, "   &fDuração Total: &e" + chatEventManager.getCurrentEventDuration() + "s");
            ChatStorage.sendRaw(sender, "   &fLembrete a cada: &e" + chatEventManager.getReminderInterval() + "s");
            ChatStorage.sendRaw(sender, "   &fGanhadores: &e" + winners + "/3");
            ChatStorage.sendRaw(sender, "   &fResposta: &a" + chatEventManager.getCurrentEvent().getAnswer());
        }

        ChatStorage.sendRaw(sender, "");
        ChatStorage.sendRaw(sender, "&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        ChatStorage.sendRaw(sender, "");
    }

    /**
     * Liga/desliga eventos automáticos
     */
    private void handleAuto(CommandSender sender, String[] args) {
        if (args.length < 2) {
            ChatStorage.sendRaw(sender, "&7Use: &e/chatevent auto <on/off>");
            return;
        }

        String option = args[1].toLowerCase();

        if (option.equals("on") || option.equals("ligado") || option.equals("true")) {
            chatEventManager.startAutoEvents();
            ChatStorage.send(sender, "chatevent.success.auto-on");
        } else if (option.equals("off") || option.equals("desligado") || option.equals("false")) {
            chatEventManager.stopAutoEvents();
            ChatStorage.send(sender, "chatevent.success.auto-off");
        } else {
            ChatStorage.sendRaw(sender, "&7Use: &e/chatevent auto <on/off>");
        }
    }

    /**
     * Define o intervalo de lembrete
     */
    private void handleReminder(CommandSender sender, String[] args) {
        if (args.length < 2) {
            ChatStorage.sendRaw(sender, "&7Intervalo atual: &e" + chatEventManager.getReminderInterval() + "s");
            ChatStorage.sendRaw(sender, "&7Use: &e/chatevent reminder <segundos>");
            return;
        }

        try {
            int seconds = Integer.parseInt(args[1]);
            if (seconds < 5 || seconds > 60) {
                ChatStorage.send(sender, "chatevent.error.reminder-range");
                return;
            }

            chatEventManager.setReminderInterval(seconds);
            ChatStorage.send(sender, "chatevent.success.reminder-set", "seconds", String.valueOf(seconds));
        } catch (NumberFormatException e) {
            ChatStorage.send(sender, "chatevent.error.reminder-invalid");
        }
    }

    /**
     * Envia mensagem de uso do comando custom
     */
    private void sendCustomUsage(CommandSender sender) {
        ChatStorage.sendRaw(sender, "");
        sender.sendMessage(ChatStorage.getMessage("chatevent.help.custom-header"));
        ChatStorage.sendRaw(sender, "");
        ChatStorage.sendRaw(sender, "&e  Uso: &f/chatevent custom <tipo> <pergunta> | <resposta> [tempo]");
        ChatStorage.sendRaw(sender, "");
        ChatStorage.sendRaw(sender, "&6  Exemplos:");
        ChatStorage.sendRaw(sender, "    &a• &f/chatevent custom MATH 25 + 17 = ? | 42");
        ChatStorage.sendRaw(sender, "    &e• &f/chatevent custom TYPE_RACE haumea eh top | haumea eh top 60");
        ChatStorage.sendRaw(sender, "    &d• &f/chatevent custom MUSIC_GUESS Ai se eu te pego | ai se eu te pego");
        ChatStorage.sendRaw(sender, "    &a• &f/chatevent custom TRIVIA Qual mob explode? | creeper 30");
        ChatStorage.sendRaw(sender, "");
        ChatStorage.sendRaw(sender, "&7  Nota: Use &e|&7 para separar pergunta e resposta.");
        ChatStorage.sendRaw(sender, "&7  O tempo (em segundos) no final é opcional.");
        ChatStorage.sendRaw(sender, "");
    }

    /**
     * Envia mensagem de uso do comando
     */
    private void sendUsageMessage(CommandSender sender) {
        ChatStorage.sendRaw(sender, "");
        sender.sendMessage(ChatStorage.getMessage("chatevent.help.header"));
        ChatStorage.sendRaw(sender, "");
        ChatStorage.sendRaw(sender, "&e  Uso: &f/chatevent <subcomando> [argumentos]");
        ChatStorage.sendRaw(sender, "");
        ChatStorage.sendRaw(sender, "&6  Subcomandos:");
        ChatStorage.sendRaw(sender, "    &a• start [tipo] [tempo] &8- &7Inicia evento (tempo em segundos)");
        ChatStorage.sendRaw(sender, "    &e• custom <tipo> <pergunta> | <resposta> [tempo] &8- &7Evento personalizado");
        ChatStorage.sendRaw(sender, "    &c• stop &8- &7Para o evento atual");
        ChatStorage.sendRaw(sender, "    &c• skip &8- &7Pula para o próximo evento");
        ChatStorage.sendRaw(sender, "    &b• tipos &8- &7Lista todos os tipos de eventos");
        ChatStorage.sendRaw(sender, "    &b• info &8- &7Mostra informações do evento atual");
        ChatStorage.sendRaw(sender, "    &d• auto <on/off> &8- &7Liga/desliga eventos automáticos");
        ChatStorage.sendRaw(sender, "    &d• reminder <segundos> &8- &7Define intervalo de lembrete (5-60s)");
        ChatStorage.sendRaw(sender, "");
        ChatStorage.sendRaw(sender, "&6  Exemplos:");
        ChatStorage.sendRaw(sender, "    &f/chatevent start &7(evento aleatório, tempo padrão 45s)");
        ChatStorage.sendRaw(sender, "    &f/chatevent start MATH 60 &7(matemática por 60 segundos)");
        ChatStorage.sendRaw(sender, "    &f/chatevent custom MATH 100 + 50 = ? | 150 &7(custom, tempo padrão)");
        ChatStorage.sendRaw(sender, "    &f/chatevent custom TRIVIA Quantas vidas tem um gato? | 7 90 &7(custom, 90s)");
        ChatStorage.sendRaw(sender, "    &f/chatevent reminder 15 &7(lembrete a cada 15s)");
        ChatStorage.sendRaw(sender, "");
    }

    /**
     * Tenta parsear um tipo de evento pelo nome
     */
    private ChatEventType parseEventType(String input) {
        String typeName = input.toUpperCase().replace("-", "_");

        try {
            return ChatEventType.valueOf(typeName);
        } catch (IllegalArgumentException e) {
            // Tentar encontrar por nome parcial
            for (ChatEventType t : ChatEventType.values()) {
                if (t.name().contains(typeName) || typeName.contains(t.name())) {
                    return t;
                }
            }
        }

        return null;
    }
}
