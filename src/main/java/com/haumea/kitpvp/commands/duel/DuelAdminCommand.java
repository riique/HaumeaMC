package com.haumea.kitpvp.commands.duel;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.commands.base.BaseCommand;
import com.haumea.kitpvp.commands.base.CommandInfo;
import com.haumea.kitpvp.managers.DuelManager;
import com.haumea.kitpvp.models.DuelArena;
import com.haumea.kitpvp.models.DuelMatch;
import com.haumea.kitpvp.utils.ChatStorage;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/**
 * Comando administrativo para gerenciar o sistema de duelos.
 * 
 * Uso:
 * - /dueladmin setarena <nome> <1|2> - Define spawn 1 ou 2 da arena
 * - /dueladmin delarena <nome> - Remove uma arena
 * - /dueladmin listarenas - Lista todas as arenas
 * - /dueladmin setlobby - Define o lobby de 1v1
 * - /dueladmin forceend - Encerra duelos problemáticos
 * - /dueladmin reload - Recarrega configurações
 * 
 * @author HaumeaMC
 */
@CommandInfo(name = "dueladmin", aliases = { "da",
        "dueladm" }, description = "Administração do sistema de duelos", usage = "/dueladmin <subcomando>", playerOnly = false, allowedGroups = {
                "dono", "diretor", "gerente" })
public class DuelAdminCommand extends BaseCommand {

    public DuelAdminCommand(HaumeaMC plugin) {
        super(plugin);
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        DuelManager duelManager = plugin.getDuelManager();

        if (duelManager == null) {
            ChatStorage.sendRaw(sender, ChatStorage.getPrefix() + "§cSistema de duelos não está disponível.");
            return;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "setarena":
                handleSetArena(sender, args);
                break;

            case "delarena":
            case "deletarena":
            case "removearena":
                handleDeleteArena(sender, args);
                break;

            case "listarenas":
            case "arenas":
                handleListArenas(sender);
                break;

            case "setlobby":
            case "lobby":
                handleSetLobby(sender);
                break;

            case "forceend":
            case "encerrar":
                handleForceEnd(sender, args);
                break;

            case "listaduelos":
            case "duelos":
                handleListDuels(sender);
                break;

            case "reload":
                handleReload(sender);
                break;

            case "info":
                handleInfo(sender, args);
                break;

            default:
                sendHelp(sender);
                break;
        }
    }

    /**
     * Envia mensagem de ajuda.
     */
    private void sendHelp(CommandSender sender) {
        sender.sendMessage("");
        sender.sendMessage("§8§m-----------------------------------------");
        sender.sendMessage("");
        sender.sendMessage("  §c§lDUELADMIN §8- §7Comandos Administrativos");
        sender.sendMessage("");
        sender.sendMessage("  §e/dueladmin setarena <nome> <1|2> §8- §7Define spawn da arena");
        sender.sendMessage("  §e/dueladmin delarena <nome> §8- §7Remove arena");
        sender.sendMessage("  §e/dueladmin listarenas §8- §7Lista arenas");
        sender.sendMessage("  §e/dueladmin setlobby §8- §7Define lobby de 1v1");
        sender.sendMessage("  §e/dueladmin forceend [id] §8- §7Encerra duelo");
        sender.sendMessage("  §e/dueladmin duelos §8- §7Lista duelos ativos");
        sender.sendMessage("  §e/dueladmin info <arena> §8- §7Informações da arena");
        sender.sendMessage("  §e/dueladmin reload §8- §7Recarrega configurações");
        sender.sendMessage("");
        sender.sendMessage("§8§m-----------------------------------------");
        sender.sendMessage("");
    }

    /**
     * Trata definição de arena.
     */
    private void handleSetArena(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            ChatStorage.sendRaw(sender, ChatStorage.getPrefix() + "§cApenas jogadores podem usar este comando.");
            return;
        }

        if (args.length < 3) {
            ChatStorage.sendRaw(sender, ChatStorage.getPrefix() + "§cUso: /dueladmin setarena <nome> <1|2>");
            return;
        }

        Player player = (Player) sender;
        String arenaName = args[1].toLowerCase();
        String spawnNumber = args[2];

        DuelManager duelManager = plugin.getDuelManager();
        DuelArena arena = duelManager.getArena(arenaName);

        if (arena == null) {
            arena = duelManager.createArena(arenaName);
            ChatStorage.sendCustom(player, "§aArena §e" + arenaName + " §acriada!");
        }

        Location loc = player.getLocation();

        if (spawnNumber.equals("1")) {
            arena.setSpawn1(loc);
            duelManager.saveArenas();
            ChatStorage.sendCustom(player, "§aSpawn 1 da arena §e" + arenaName + " §adefinido!");
        } else if (spawnNumber.equals("2")) {
            arena.setSpawn2(loc);
            duelManager.saveArenas();
            ChatStorage.sendCustom(player, "§aSpawn 2 da arena §e" + arenaName + " §adefinido!");
        } else {
            ChatStorage.sendCustom(player, "§cUse 1 ou 2 para definir o spawn.");
        }
    }

    /**
     * Trata remoção de arena.
     */
    private void handleDeleteArena(CommandSender sender, String[] args) {
        if (args.length < 2) {
            ChatStorage.sendRaw(sender, ChatStorage.getPrefix() + "§cUso: /dueladmin delarena <nome>");
            return;
        }

        String arenaName = args[1].toLowerCase();
        DuelManager duelManager = plugin.getDuelManager();

        if (duelManager.deleteArena(arenaName)) {
            ChatStorage.sendRaw(sender, ChatStorage.getPrefix() + "§aArena §e" + arenaName + " §aremovida!");
        } else {
            ChatStorage.sendRaw(sender, ChatStorage.getPrefix() + "§cArena não encontrada: §e" + arenaName);
        }
    }

    /**
     * Lista todas as arenas.
     */
    private void handleListArenas(CommandSender sender) {
        DuelManager duelManager = plugin.getDuelManager();
        Collection<DuelArena> arenas = duelManager.getAllArenas();

        if (arenas.isEmpty()) {
            ChatStorage.sendRaw(sender, ChatStorage.getPrefix() + "§cNenhuma arena configurada.");
            return;
        }

        sender.sendMessage("");
        sender.sendMessage("§8§m-----------------------------------------");
        sender.sendMessage("");
        sender.sendMessage("  §e§lARENAS DE DUELO (" + arenas.size() + ")");
        sender.sendMessage("");

        for (DuelArena arena : arenas) {
            String status;
            if (!arena.isReady()) {
                status = "§c[Incompleta]";
            } else if (arena.isInUse()) {
                status = "§6[Em uso]";
            } else if (!arena.isEnabled()) {
                status = "§7[Desativada]";
            } else {
                status = "§a[Disponível]";
            }

            sender.sendMessage("  §f" + arena.getName() + " " + status);
        }

        sender.sendMessage("");
        sender.sendMessage("§8§m-----------------------------------------");
        sender.sendMessage("");
    }

    /**
     * Define o lobby de 1v1.
     */
    private void handleSetLobby(CommandSender sender) {
        if (!(sender instanceof Player)) {
            ChatStorage.sendRaw(sender, ChatStorage.getPrefix() + "§cApenas jogadores podem usar este comando.");
            return;
        }

        Player player = (Player) sender;
        Location loc = player.getLocation();

        // Usar o sistema de warps existente
        plugin.getWarpsManager().setWarp("1v1", loc);
        ChatStorage.sendCustom(player, "§aLobby de 1v1 definido na sua posição atual!");
    }

    /**
     * Força o término de um duelo.
     */
    private void handleForceEnd(CommandSender sender, String[] args) {
        DuelManager duelManager = plugin.getDuelManager();

        if (args.length < 2) {
            // Encerrar todos os duelos
            int count = duelManager.getActiveMatchesCount();
            if (count == 0) {
                ChatStorage.sendRaw(sender, ChatStorage.getPrefix() + "§cNenhum duelo ativo.");
                return;
            }

            for (UUID matchId : duelManager.getActiveMatches().keySet()) {
                duelManager.forceEndDuel(matchId);
            }

            ChatStorage.sendRaw(sender, ChatStorage.getPrefix() + "§a" + count + " duelo(s) encerrado(s)!");
            return;
        }

        // Encerrar duelo específico
        try {
            UUID matchId = UUID.fromString(args[1]);
            if (duelManager.forceEndDuel(matchId)) {
                ChatStorage.sendRaw(sender, ChatStorage.getPrefix() + "§aDuelo encerrado com sucesso!");
            } else {
                ChatStorage.sendRaw(sender, ChatStorage.getPrefix() + "§cDuelo não encontrado.");
            }
        } catch (IllegalArgumentException e) {
            ChatStorage.sendRaw(sender,
                    ChatStorage.getPrefix() + "§cID inválido. Use /dueladmin duelos para ver os IDs.");
        }
    }

    /**
     * Lista duelos ativos.
     */
    private void handleListDuels(CommandSender sender) {
        DuelManager duelManager = plugin.getDuelManager();
        Map<UUID, DuelMatch> matches = duelManager.getActiveMatches();

        if (matches.isEmpty()) {
            ChatStorage.sendRaw(sender, ChatStorage.getPrefix() + "§cNenhum duelo ativo.");
            return;
        }

        sender.sendMessage("");
        sender.sendMessage("§8§m-----------------------------------------");
        sender.sendMessage("");
        sender.sendMessage("  §e§lDUELOS ATIVOS (" + matches.size() + ")");
        sender.sendMessage("");

        for (DuelMatch match : matches.values()) {
            Player p1 = match.getPlayer1();
            Player p2 = match.getPlayer2();
            String name1 = p1 != null ? p1.getName() : "???";
            String name2 = p2 != null ? p2.getName() : "???";

            sender.sendMessage("  §f" + name1 + " §7vs §f" + name2);
            sender.sendMessage("    §7Arena: §e" + match.getArena().getName() +
                    " §7| Estado: §e" + match.getState().name());
            sender.sendMessage("    §7ID: §8" + match.getMatchId().toString().substring(0, 8) + "...");
        }

        sender.sendMessage("");
        sender.sendMessage("§8§m-----------------------------------------");
        sender.sendMessage("");
    }

    /**
     * Recarrega configurações.
     */
    private void handleReload(CommandSender sender) {
        // Por enquanto, apenas salva as arenas
        plugin.getDuelManager().saveArenas();
        ChatStorage.sendRaw(sender, ChatStorage.getPrefix() + "§aConfigurações de duelo salvas!");
    }

    /**
     * Mostra informações de uma arena.
     */
    private void handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            ChatStorage.sendRaw(sender, ChatStorage.getPrefix() + "§cUso: /dueladmin info <arena>");
            return;
        }

        String arenaName = args[1].toLowerCase();
        DuelManager duelManager = plugin.getDuelManager();
        DuelArena arena = duelManager.getArena(arenaName);

        if (arena == null) {
            ChatStorage.sendRaw(sender, ChatStorage.getPrefix() + "§cArena não encontrada: §e" + arenaName);
            return;
        }

        sender.sendMessage("");
        sender.sendMessage("§8§m-----------------------------------------");
        sender.sendMessage("");
        sender.sendMessage("  §e§lINFO: " + arena.getName().toUpperCase());
        sender.sendMessage("");
        sender.sendMessage("  §7Mundo: §f" + (arena.getWorldName() != null ? arena.getWorldName() : "§cNão definido"));
        sender.sendMessage("  §7Status: " + (arena.isEnabled() ? "§aAtivada" : "§cDesativada"));
        sender.sendMessage("  §7Em uso: " + (arena.isInUse() ? "§cSim" : "§aNão"));
        sender.sendMessage("  §7Pronta: " + (arena.isReady() ? "§aSim" : "§cNão"));
        sender.sendMessage("");

        Location s1 = arena.getSpawn1();
        Location s2 = arena.getSpawn2();

        if (s1 != null) {
            sender.sendMessage("  §7Spawn 1: §f" + String.format("%.1f, %.1f, %.1f", s1.getX(), s1.getY(), s1.getZ()));
        } else {
            sender.sendMessage("  §7Spawn 1: §cNão definido");
        }

        if (s2 != null) {
            sender.sendMessage("  §7Spawn 2: §f" + String.format("%.1f, %.1f, %.1f", s2.getX(), s2.getY(), s2.getZ()));
        } else {
            sender.sendMessage("  §7Spawn 2: §cNão definido");
        }

        sender.sendMessage("");
        sender.sendMessage("§8§m-----------------------------------------");
        sender.sendMessage("");
    }
}
