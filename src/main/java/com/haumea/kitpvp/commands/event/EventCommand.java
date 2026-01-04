package com.haumea.kitpvp.commands.event;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.managers.EventManager;
import com.haumea.kitpvp.models.EventState;
import com.haumea.kitpvp.utils.ChatStorage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Comando principal do sistema de eventos.
 * 
 * Subcomandos para jogadores:
 * - /evento - Teleporta para a warp do evento (se aberto)
 * - /evento sair - Sai do evento atual
 * 
 * Subcomandos para staff (com permissão):
 * - /evento criar [nome] - Cria um novo evento
 * - /evento abrir - Abre inscrições
 * - /evento fechar - Fecha inscrições
 * - /evento iniciar - Inicia o evento
 * - /evento pausar - Pausa o evento
 * - /evento finalizar - Finaliza o evento
 * - /evento cancelar - Cancela e expulsa todos
 * - /evento pvp - Toggle de PvP
 * - /evento dano - Toggle de dano
 * - /evento build - Toggle de build
 * - /evento kick <jogador> - Expulsa jogador do evento
 * - /evento tpall - Teleporta todos para a arena
 * - /evento lista - Lista jogadores no evento
 * - /evento setwarp - Define a warp do evento
 * - /evento info - Mostra informações do evento atual
 * 
 * @author HaumeaMC
 */
public class EventCommand implements CommandExecutor, TabCompleter {

    private final HaumeaMC plugin;
    private final EventManager eventManager;

    // Subcomandos de jogadores
    private static final List<String> PLAYER_SUBCOMMANDS = Arrays.asList("sair");

    // Subcomandos de staff
    private static final List<String> STAFF_SUBCOMMANDS = Arrays.asList(
            "criar", "abrir", "fechar", "iniciar", "pausar", "finalizar",
            "cancelar", "pvp", "dano", "build", "kick", "tpall", "lista",
            "setwarp", "info");

    // Grupos que podem administrar eventos
    private static final List<String> ADMIN_GROUPS = Arrays.asList(
            "dono", "diretor", "gerente", "admin", "moderador");

    public EventCommand(HaumeaMC plugin) {
        this.plugin = plugin;
        this.eventManager = plugin.getEventManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Sem argumentos - jogador quer entrar no evento
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                ChatStorage.send(sender, "error.player-only");
                return true;
            }

            Player player = (Player) sender;
            handleJoinEvent(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        // Subcomandos de jogadores
        switch (subCommand) {
            case "sair":
            case "leave":
            case "exit":
                if (!(sender instanceof Player)) {
                    ChatStorage.send(sender, "error.player-only");
                    return true;
                }
                handleLeaveEvent((Player) sender);
                return true;
        }

        // Verificar permissão para comandos de staff
        if (!isStaff(sender)) {
            ChatStorage.send(sender, "error.no-permission");
            return true;
        }

        Player staff = (sender instanceof Player) ? (Player) sender : null;

        // Subcomandos de staff
        switch (subCommand) {
            case "criar":
            case "create":
                if (staff == null) {
                    ChatStorage.send(sender, "error.player-only");
                    return true;
                }
                String eventName = args.length > 1 ? joinArgs(args, 1) : null;
                handleCreateEvent(staff, eventName);
                break;

            case "abrir":
            case "open":
                if (staff == null) {
                    ChatStorage.send(sender, "error.player-only");
                    return true;
                }
                eventManager.openEvent(staff);
                break;

            case "fechar":
            case "close":
                if (staff == null) {
                    ChatStorage.send(sender, "error.player-only");
                    return true;
                }
                eventManager.closeEvent(staff);
                break;

            case "iniciar":
            case "start":
                if (staff == null) {
                    ChatStorage.send(sender, "error.player-only");
                    return true;
                }
                eventManager.startEvent(staff);
                break;

            case "pausar":
            case "pause":
                if (staff == null) {
                    ChatStorage.send(sender, "error.player-only");
                    return true;
                }
                eventManager.pauseEvent(staff);
                break;

            case "finalizar":
            case "finish":
            case "end":
                if (staff == null) {
                    ChatStorage.send(sender, "error.player-only");
                    return true;
                }
                handleFinishEvent(staff, args);
                break;

            case "cancelar":
            case "cancel":
                if (staff == null) {
                    ChatStorage.send(sender, "error.player-only");
                    return true;
                }
                eventManager.cancelEvent(staff);
                break;

            case "pvp":
                if (staff == null) {
                    ChatStorage.send(sender, "error.player-only");
                    return true;
                }
                boolean pvp = eventManager.togglePvP(staff);
                ChatStorage.send(staff, "event.toggle-result",
                        "setting", "PvP",
                        "status", pvp ? "§aATIVADO" : "§cDESATIVADO");
                break;

            case "dano":
            case "damage":
                if (staff == null) {
                    ChatStorage.send(sender, "error.player-only");
                    return true;
                }
                boolean damage = eventManager.toggleDamage(staff);
                ChatStorage.send(staff, "event.toggle-result",
                        "setting", "Dano",
                        "status", damage ? "§aATIVADO" : "§cDESATIVADO");
                break;

            case "build":
            case "construir":
                if (staff == null) {
                    ChatStorage.send(sender, "error.player-only");
                    return true;
                }
                boolean build = eventManager.toggleBuild(staff);
                ChatStorage.send(staff, "event.toggle-result",
                        "setting", "Build",
                        "status", build ? "§aATIVADO" : "§cDESATIVADO");
                break;

            case "kick":
            case "expulsar":
                if (staff == null) {
                    ChatStorage.send(sender, "error.player-only");
                    return true;
                }
                handleKickPlayer(staff, args);
                break;

            case "tpall":
            case "teleportall":
                if (staff == null) {
                    ChatStorage.send(sender, "error.player-only");
                    return true;
                }
                eventManager.teleportAllToEvent(staff);
                break;

            case "lista":
            case "list":
                handleListPlayers(sender);
                break;

            case "setwarp":
            case "definirwarp":
                if (staff == null) {
                    ChatStorage.send(sender, "error.player-only");
                    return true;
                }
                handleSetWarp(staff);
                break;

            case "info":
            case "informacoes":
                handleInfo(sender);
                break;

            default:
                sendHelpMessage(sender);
                break;
        }

        return true;
    }

    // ==================== HANDLERS ====================

    /**
     * Handler para entrar no evento
     */
    private void handleJoinEvent(Player player) {
        if (!eventManager.hasActiveEvent()) {
            ChatStorage.send(player, "event.no-active-event");
            return;
        }

        if (eventManager.isParticipant(player)) {
            ChatStorage.send(player, "event.already-in");
            return;
        }

        eventManager.addPlayerToEvent(player);
    }

    /**
     * Handler para sair do evento
     */
    private void handleLeaveEvent(Player player) {
        if (!eventManager.isParticipant(player)) {
            ChatStorage.send(player, "event.not-in-event");
            return;
        }

        // Bloquear saída durante evento iniciado (opcional, comentado)
        // if (eventManager.getState().isRunning()) {
        // ChatStorage.send(player, "event.cannot-leave-running");
        // return;
        // }

        eventManager.removePlayerFromEvent(player, true);
    }

    /**
     * Handler para criar evento
     */
    private void handleCreateEvent(Player staff, String name) {
        eventManager.createEvent(staff, name);
    }

    /**
     * Handler para finalizar evento
     */
    private void handleFinishEvent(Player staff, String[] args) {
        Player winner = null;
        if (args.length > 1) {
            winner = Bukkit.getPlayer(args[1]);
            if (winner == null) {
                ChatStorage.send(staff, "event.player-not-found", "player", args[1]);
                return;
            }
        }
        eventManager.finishEvent(staff, winner);
    }

    /**
     * Handler para expulsar jogador
     */
    private void handleKickPlayer(Player staff, String[] args) {
        if (args.length < 2) {
            ChatStorage.send(staff, "event.usage-kick");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            ChatStorage.send(staff, "event.player-not-found", "player", args[1]);
            return;
        }

        eventManager.kickPlayerFromEvent(target, staff);
    }

    /**
     * Handler para listar jogadores
     */
    private void handleListPlayers(CommandSender sender) {
        if (!eventManager.hasActiveEvent()) {
            ChatStorage.send(sender, "event.no-active-event");
            return;
        }

        List<String> names = eventManager.getParticipantNames();
        int count = names.size();

        ChatStorage.sendRaw(sender, "§8§m----------------------------------------");
        ChatStorage.sendRaw(sender, " §6§lPARTICIPANTES DO EVENTO §7(" + count + ")");
        ChatStorage.sendRaw(sender, "§8§m----------------------------------------");

        if (names.isEmpty()) {
            ChatStorage.sendRaw(sender, " §7Nenhum jogador no evento.");
        } else {
            String playerList = String.join("§7, §f", names);
            ChatStorage.sendRaw(sender, " §f" + playerList);
        }

        ChatStorage.sendRaw(sender, "§8§m----------------------------------------");
    }

    /**
     * Handler para definir warp do evento
     */
    private void handleSetWarp(Player staff) {
        eventManager.setEventWarp(staff.getLocation());
        ChatStorage.send(staff, "event.warp-set");
    }

    /**
     * Handler para exibir informações
     */
    private void handleInfo(CommandSender sender) {
        EventState state = eventManager.getState();

        ChatStorage.sendRaw(sender, "§8§m----------------------------------------");
        ChatStorage.sendRaw(sender, " §6§l⚔ INFORMAÇÕES DO EVENTO ⚔");
        ChatStorage.sendRaw(sender, "§8§m----------------------------------------");

        if (!eventManager.hasActiveEvent()) {
            ChatStorage.sendRaw(sender, " §7Nenhum evento ativo no momento.");
        } else {
            ChatStorage.sendRaw(sender, " §fNome: §e" + eventManager.getEventName());
            ChatStorage.sendRaw(sender, " §fCriador: §e" + eventManager.getCreatorName());
            ChatStorage.sendRaw(sender, " §fEstado: " + state.getDisplayName());
            ChatStorage.sendRaw(sender, " §fJogadores: §b" + eventManager.getParticipantCount());
            ChatStorage.sendRaw(sender, " ");
            ChatStorage.sendRaw(sender, " §fPvP: " + (eventManager.isPvPEnabled() ? "§a✔" : "§c✘"));
            ChatStorage.sendRaw(sender, " §fDano: " + (eventManager.isDamageEnabled() ? "§a✔" : "§c✘"));
            ChatStorage.sendRaw(sender, " §fBuild: " + (eventManager.isBuildEnabled() ? "§a✔" : "§c✘"));

            if (state.isRunning()) {
                ChatStorage.sendRaw(sender, " §fTempo: §e" + eventManager.getFormattedTime());
            }

            if (eventManager.getWinnerName() != null) {
                ChatStorage.sendRaw(sender, " §fVencedor: §a" + eventManager.getWinnerName());
            }
        }

        ChatStorage.sendRaw(sender, "§8§m----------------------------------------");
    }

    /**
     * Envia mensagem de ajuda
     */
    private void sendHelpMessage(CommandSender sender) {
        ChatStorage.sendRaw(sender, "§8§m----------------------------------------");
        ChatStorage.sendRaw(sender, " §6§l⚔ COMANDOS DE EVENTO ⚔");
        ChatStorage.sendRaw(sender, "§8§m----------------------------------------");
        ChatStorage.sendRaw(sender, " §e/evento §7- Entrar no evento");
        ChatStorage.sendRaw(sender, " §e/evento sair §7- Sair do evento");

        if (isStaff(sender)) {
            ChatStorage.sendRaw(sender, " ");
            ChatStorage.sendRaw(sender, " §c§lSTAFF:");
            ChatStorage.sendRaw(sender, " §e/evento criar [nome] §7- Cria um evento");
            ChatStorage.sendRaw(sender, " §e/evento abrir §7- Abre inscrições");
            ChatStorage.sendRaw(sender, " §e/evento fechar §7- Fecha inscrições");
            ChatStorage.sendRaw(sender, " §e/evento iniciar §7- Inicia o evento");
            ChatStorage.sendRaw(sender, " §e/evento pausar §7- Pausa o evento");
            ChatStorage.sendRaw(sender, " §e/evento finalizar §7- Finaliza o evento");
            ChatStorage.sendRaw(sender, " §e/evento cancelar §7- Cancela tudo");
            ChatStorage.sendRaw(sender, " §e/evento pvp/dano/build §7- Toggle configurações");
            ChatStorage.sendRaw(sender, " §e/evento kick <jogador> §7- Expulsa do evento");
            ChatStorage.sendRaw(sender, " §e/evento tpall §7- Teleporta todos");
            ChatStorage.sendRaw(sender, " §e/evento lista §7- Lista participantes");
            ChatStorage.sendRaw(sender, " §e/evento setwarp §7- Define warp do evento");
            ChatStorage.sendRaw(sender, " §e/evento info §7- Informações do evento");
        }

        ChatStorage.sendRaw(sender, "§8§m----------------------------------------");
    }

    // ==================== UTILITÁRIOS ====================

    /**
     * Verifica se o sender é staff.
     * Delega para o GroupManager centralizado.
     */
    private boolean isStaff(CommandSender sender) {
        if (sender.hasPermission("haumea.event.admin") || sender.isOp()) {
            return true;
        }

        if (sender instanceof Player) {
            return plugin.getGroupManager() != null && plugin.getGroupManager().isStaff((Player) sender);
        }

        return false;
    }

    /**
     * Junta argumentos a partir de um índice
     */
    private String joinArgs(String[] args, int startIndex) {
        StringBuilder sb = new StringBuilder();
        for (int i = startIndex; i < args.length; i++) {
            if (sb.length() > 0)
                sb.append(" ");
            sb.append(args[i]);
        }
        return sb.toString();
    }

    // ==================== TAB COMPLETER ====================

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // Primeiro argumento - subcomandos
            completions.addAll(PLAYER_SUBCOMMANDS);

            if (isStaff(sender)) {
                completions.addAll(STAFF_SUBCOMMANDS);
            }

            // Filtrar por prefixo
            String prefix = args[0].toLowerCase();
            return completions.stream()
                    .filter(s -> s.toLowerCase().startsWith(prefix))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            String subCommand = args[0].toLowerCase();

            // Comandos que precisam de jogador
            if (subCommand.equals("kick") || subCommand.equals("expulsar")) {
                // Retornar jogadores no evento
                return eventManager.getParticipantNames().stream()
                        .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }

            if (subCommand.equals("finalizar") || subCommand.equals("finish")) {
                // Retornar jogadores no evento para selecionar vencedor
                return eventManager.getParticipantNames().stream()
                        .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        return completions;
    }
}
