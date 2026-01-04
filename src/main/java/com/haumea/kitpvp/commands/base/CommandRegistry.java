package com.haumea.kitpvp.commands.base;

import com.haumea.kitpvp.HaumeaMC;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.SimplePluginManager;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

/**
 * Sistema de registro automático de comandos via reflexão.
 * 
 * Registra comandos diretamente no CommandMap do Bukkit,
 * sem necessidade de declarar no plugin.yml.
 * 
 * Uso:
 * 1. Crie seu comando estendendo BaseCommand
 * 2. Adicione @CommandInfo com os metadados
 * 3. Chame CommandRegistry.register(SeuComando.class)
 * 
 * Ou use registerAll() para registrar múltiplos comandos de uma vez.
 * 
 * @author HaumeaMC
 */
public class CommandRegistry {

    private final HaumeaMC plugin;
    private CommandMap commandMap;
    private final List<String> registeredCommands;

    /**
     * Construtor do CommandRegistry
     * 
     * @param plugin Instância do plugin
     */
    public CommandRegistry(HaumeaMC plugin) {
        this.plugin = plugin;
        this.registeredCommands = new ArrayList<>();
        this.commandMap = getCommandMap();
    }

    /**
     * Obtém o CommandMap do servidor via reflexão
     */
    private CommandMap getCommandMap() {
        try {
            if (Bukkit.getPluginManager() instanceof SimplePluginManager) {
                Field field = SimplePluginManager.class.getDeclaredField("commandMap");
                field.setAccessible(true);
                return (CommandMap) field.get(Bukkit.getPluginManager());
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Não foi possível obter o CommandMap!", e);
        }
        return null;
    }

    /**
     * Registra um comando automaticamente usando a annotation @CommandInfo
     * 
     * @param commandClass Classe do comando (deve estender BaseCommand)
     * @return true se registrou com sucesso
     */
    public boolean register(Class<? extends BaseCommand> commandClass) {
        // Verificar annotation
        CommandInfo info = commandClass.getAnnotation(CommandInfo.class);
        if (info == null) {
            plugin.getLogger().warning("Comando " + commandClass.getSimpleName() + " não possui @CommandInfo!");
            return false;
        }

        if (commandMap == null) {
            plugin.getLogger().severe("CommandMap não disponível!");
            return false;
        }

        try {
            // Instanciar o comando
            Constructor<? extends BaseCommand> constructor = commandClass.getConstructor(HaumeaMC.class);
            BaseCommand commandInstance = constructor.newInstance(plugin);

            // Criar o PluginCommand via reflexão
            Constructor<PluginCommand> pluginCommandConstructor = PluginCommand.class
                    .getDeclaredConstructor(String.class, Plugin.class);
            pluginCommandConstructor.setAccessible(true);
            PluginCommand pluginCommand = pluginCommandConstructor.newInstance(info.name(), plugin);

            // Configurar metadados
            pluginCommand.setExecutor(commandInstance);
            pluginCommand.setDescription(info.description());
            pluginCommand.setUsage(info.usage());

            if (info.aliases().length > 0) {
                pluginCommand.setAliases(Arrays.asList(info.aliases()));
            }

            if (!info.permission().isEmpty()) {
                pluginCommand.setPermission(info.permission());
            }

            // Registrar no CommandMap
            commandMap.register(plugin.getName().toLowerCase(), pluginCommand);
            registeredCommands.add(info.name());

            plugin.getLogger().info("Comando registrado: /" + info.name() +
                    (info.aliases().length > 0 ? " (aliases: " + String.join(", ", info.aliases()) + ")" : ""));

            return true;

        } catch (NoSuchMethodException e) {
            plugin.getLogger().severe("Comando " + commandClass.getSimpleName() +
                    " deve ter um construtor que recebe HaumeaMC!");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Erro ao registrar comando " + commandClass.getSimpleName(), e);
        }

        return false;
    }

    /**
     * Registra múltiplos comandos de uma vez
     * 
     * @param commandClasses Classes dos comandos
     */
    @SafeVarargs
    public final void registerAll(Class<? extends BaseCommand>... commandClasses) {
        int success = 0;
        for (Class<? extends BaseCommand> commandClass : commandClasses) {
            if (register(commandClass)) {
                success++;
            }
        }
        plugin.getLogger().info("Comandos registrados: " + success + "/" + commandClasses.length);
    }

    /**
     * Obtém a lista de comandos registrados
     * 
     * @return Lista de nomes de comandos
     */
    public List<String> getRegisteredCommands() {
        return new ArrayList<>(registeredCommands);
    }

    /**
     * Obtém a quantidade de comandos registrados
     * 
     * @return Quantidade de comandos
     */
    public int getCommandCount() {
        return registeredCommands.size();
    }
}
