package com.haumea.kitpvp.commands.base;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.managers.GroupManager;
import com.haumea.kitpvp.models.Group;
import com.haumea.kitpvp.utils.ChatStorage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Classe base abstrata para todos os comandos do HaumeaMC.
 * 
 * Fornece validações automáticas e estrutura padronizada.
 * Para criar um novo comando, basta estender esta classe,
 * adicionar a annotation @CommandInfo e implementar execute().
 * 
 * Exemplo:
 * {@code
 * @CommandInfo(name = "spawn", playerOnly = true)
 * public class SpawnCommand extends BaseCommand {
 * public SpawnCommand(HaumeaMC plugin) { super(plugin); }
 * 
 * @Override
 *           protected void execute(CommandSender sender, String[] args) {
 *           Player player = getPlayer(); // Já validado!
 *           player.teleport(spawnLocation);
 *           }
 *           }
 *           }
 * 
 * @author HaumeaMC
 */
public abstract class BaseCommand implements CommandExecutor {

    protected final HaumeaMC plugin;
    protected final GroupManager groupManager;

    // Contexto do comando atual (thread-safe para comandos síncronos)
    private CommandSender currentSender;
    private String[] currentArgs;
    private String currentLabel;

    /**
     * Construtor base
     * 
     * @param plugin Instância do plugin
     */
    public BaseCommand(HaumeaMC plugin) {
        this.plugin = plugin;
        this.groupManager = plugin.getGroupManager();
    }

    @Override
    public final boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Armazenar contexto
        this.currentSender = sender;
        this.currentArgs = args;
        this.currentLabel = label;

        // Obter informações da annotation
        CommandInfo info = getClass().getAnnotation(CommandInfo.class);

        if (info != null) {
            // Validação: Apenas jogadores
            if (info.playerOnly() && !(sender instanceof Player)) {
                ChatStorage.send(sender, "error.player-only");
                return true;
            }

            // Validação: Apenas console
            if (info.consoleOnly() && sender instanceof Player) {
                ChatStorage.send(sender, "error.console-only");
                return true;
            }

            // Validação: Permissão
            if (!info.permission().isEmpty() && !sender.hasPermission(info.permission())) {
                ChatStorage.send(sender, "error.no-permission");
                return true;
            }

            // Validação: Grupos permitidos
            if (info.allowedGroups().length > 0 && sender instanceof Player) {
                if (!hasAllowedGroup((Player) sender, info.allowedGroups())) {
                    ChatStorage.send(sender, "error.no-permission");
                    return true;
                }
            }
        }

        // Executar lógica do comando
        try {
            execute(sender, args);
        } catch (Exception e) {
            plugin.getLogger().severe("Erro ao executar comando /" + label + ": " + e.getMessage());
            e.printStackTrace();
            ChatStorage.sendRaw(sender, "&c&lERRO&f Ocorreu um erro ao executar o comando.");
        }

        return true;
    }

    /**
     * Método principal a ser implementado pelos comandos.
     * As validações já foram feitas automaticamente.
     * 
     * @param sender Quem executou o comando
     * @param args   Argumentos do comando
     */
    protected abstract void execute(CommandSender sender, String[] args);

    // ==================== MÉTODOS UTILITÁRIOS ====================

    /**
     * Obtém o jogador que executou o comando.
     * Só use se o comando for playerOnly!
     * 
     * @return Player ou null se for console
     */
    protected Player getPlayer() {
        return currentSender instanceof Player ? (Player) currentSender : null;
    }

    /**
     * Obtém o sender atual
     */
    protected CommandSender getSender() {
        return currentSender;
    }

    /**
     * Obtém os argumentos atuais
     */
    protected String[] getArgs() {
        return currentArgs;
    }

    /**
     * Obtém o label usado (nome ou alias)
     */
    protected String getLabel() {
        return currentLabel;
    }

    /**
     * Verifica se há argumentos suficientes
     * 
     * @param min Mínimo de argumentos
     * @return true se há argumentos suficientes
     */
    protected boolean hasArgs(int min) {
        return currentArgs.length >= min;
    }

    /**
     * Obtém um argumento específico
     * 
     * @param index Índice do argumento (0-based)
     * @return Argumento ou null se não existir
     */
    protected String getArg(int index) {
        return index < currentArgs.length ? currentArgs[index] : null;
    }

    /**
     * Obtém um argumento como inteiro
     * 
     * @param index        Índice do argumento
     * @param defaultValue Valor padrão se inválido
     * @return Valor inteiro ou padrão
     */
    protected int getArgInt(int index, int defaultValue) {
        String arg = getArg(index);
        if (arg == null)
            return defaultValue;
        try {
            return Integer.parseInt(arg);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Envia mensagem do ChatStorage
     */
    protected void sendMessage(String key, String... replacements) {
        ChatStorage.send(currentSender, key, replacements);
    }

    /**
     * Envia mensagem customizada
     */
    protected void sendRaw(String message) {
        ChatStorage.sendRaw(currentSender, message);
    }

    /**
     * Envia mensagem de uso incorreto
     */
    protected void sendUsage() {
        CommandInfo info = getClass().getAnnotation(CommandInfo.class);
        if (info != null && !info.usage().isEmpty()) {
            ChatStorage.send(currentSender, "error.invalid-usage", "usage", info.usage());
        }
    }

    /**
     * Verifica se o sender tem um dos grupos permitidos
     */
    private boolean hasAllowedGroup(Player player, String[] allowedGroups) {
        Group group = groupManager.getPlayerGroup(player);
        if (group == null)
            return false;

        String groupName = group.getName().toLowerCase();
        for (String allowed : allowedGroups) {
            if (groupName.equals(allowed.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Verifica se o jogador é membro de um grupo específico
     */
    protected boolean isInGroup(Player player, String groupName) {
        Group group = groupManager.getPlayerGroup(player);
        return group != null && group.getName().equalsIgnoreCase(groupName);
    }

    /**
     * Verifica se o jogador é staff (ajudante ou superior).
     * Delega para o GroupManager centralizado.
     */
    protected boolean isStaff(Player player) {
        return groupManager != null && groupManager.isStaff(player);
    }

    /**
     * Verifica se o jogador é staff superior (moderador ou superior).
     * Delega para o GroupManager centralizado.
     */
    protected boolean isHigherStaff(Player player) {
        return groupManager != null && groupManager.isHigherStaff(player);
    }

    /**
     * Verifica se o jogador é admin ou superior.
     * Delega para o GroupManager centralizado.
     */
    protected boolean isAdmin(Player player) {
        return groupManager != null && groupManager.isAdmin(player);
    }
}
