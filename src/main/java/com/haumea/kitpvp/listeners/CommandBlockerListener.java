package com.haumea.kitpvp.listeners;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.utils.ChatStorage;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.HashSet;
import java.util.Set;

/**
 * Listener responsável por bloquear comandos do Bukkit/Spigot padrão.
 * Apenas comandos registrados pelo plugin HaumeaMC são permitidos.
 * 
 * @author HaumeaMC
 */
public class CommandBlockerListener implements Listener {

    private final HaumeaMC plugin;
    private final Set<String> allowedCommands;

    public CommandBlockerListener(HaumeaMC plugin) {
        this.plugin = plugin;
        this.allowedCommands = new HashSet<>();
        loadAllowedCommands();
    }

    /**
     * Carrega a lista de comandos permitidos do plugin.
     * Inclui comandos do plugin.yml e comandos registrados via CommandRegistry.
     */
    private void loadAllowedCommands() {
        // Comandos do plugin.yml
        addCommand("tag", "tags");
        addCommand("haumeagroups", "haumeagrupos", "haumeagrupo", "haumeagroup", "hgroups", "hgroup", "setgroup",
                "grupos");
        addCommand("tp");
        addCommand("tpall");
        addCommand("puxar");
        addCommand("inv", "invsee", "inventario");
        addCommand("kill");
        addCommand("ipcheck", "ip");
        addCommand("bc", "broadcast", "anuncio");
        addCommand("tell", "msg", "w", "whisper", "pm");
        addCommand("ping", "latency", "latencia");
        addCommand("regras", "rules");
        addCommand("conta", "stats", "estatisticas", "account");
        addCommand("score", "scoreboard", "sb");
        addCommand("haumeawarp", "hwarp", "warpadmin");
        addCommand("warp", "warps", "ir");
        addCommand("spawn", "lobby");
        addCommand("haumeaspawn", "setspawn", "hspawn");
        addCommand("medalha", "medal", "medals", "medalhas");
        addCommand("fake", "nick", "fakenick");
        addCommand("ranking", "rank", "elo", "league", "liga", "ligas");
        addCommand("duel", "duelo", "1v1", "x1");
        addCommand("dueladmin", "da", "dueladm");
        addCommand("evento", "event", "events", "eventos");
        addCommand("feast", "feasts");
        addCommand("multiplicador", "multi", "mult", "mp");
        addCommand("multadmin", "multiplicadoradmin", "mpadmin");
        addCommand("haumeastats", "hstats", "statsadmin");

        // Comandos registrados via CommandRegistry (sem plugin.yml)
        addCommand("gm", "gamemode");
        addCommand("fly", "voar");
        addCommand("admin", "adm");
        addCommand("build", "construir");
        addCommand("clearchat", "cc", "limpar");
        addCommand("staffchat", "sc", "s");
        addCommand("chat");
        addCommand("cleardrops", "cd", "limpardrops");
        addCommand("god", "imortal", "deus");
        addCommand("speed", "velocidade");
        addCommand("ban", "banir");
        addCommand("unban", "desbanir");
        addCommand("kick", "expulsar");
        addCommand("mute", "mutar", "silenciar");
        addCommand("unmute", "desmutar");
        addCommand("warn", "avisar");
        addCommand("report", "reportar", "denunciar");
        addCommand("authority", "auth");
        addCommand("skin", "skins");

        // Comandos especiais que devem ser permitidos
        addCommand("haumea", "haumeamc");
        addCommand("kit", "kits");
        addCommand("presente", "daily", "dailyreward");
    }

    /**
     * Adiciona um comando e seus aliases à lista de permitidos.
     */
    private void addCommand(String... commands) {
        for (String cmd : commands) {
            allowedCommands.add(cmd.toLowerCase());
        }
    }

    /**
     * Intercepta comandos antes de serem processados.
     * Bloqueia comandos que não são do plugin HaumeaMC.
     * Mostra mensagem customizada para comandos desconhecidos.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        if (event.isCancelled()) {
            return;
        }

        Player player = event.getPlayer();
        String message = event.getMessage();

        // Extrair o comando principal (sem /)
        String commandLine = message.substring(1).trim();
        if (commandLine.isEmpty()) {
            return;
        }

        // Pegar apenas o nome do comando (antes do primeiro espaço)
        String[] parts = commandLine.split(" ");
        String command = parts[0].toLowerCase();
        String originalCommand = command;

        // Remover prefixo do plugin se presente (ex: haumeapvp:comando)
        if (command.contains(":")) {
            String[] colonParts = command.split(":");
            String pluginPrefix = colonParts[0];
            command = colonParts[1];

            // Se for prefixo do nosso plugin, verificar se o comando existe
            if (pluginPrefix.equalsIgnoreCase("haumeapvp") ||
                    pluginPrefix.equalsIgnoreCase("haumea") ||
                    pluginPrefix.equalsIgnoreCase("haumeamc")) {
                if (isAllowedCommand(command)) {
                    return;
                }
            }
        }

        // Verificar se o comando é permitido na nossa lista
        if (isAllowedCommand(command)) {
            return;
        }

        // Verificar se é um comando registrado do nosso plugin
        PluginCommand pluginCommand = plugin.getServer().getPluginCommand(command);
        if (pluginCommand != null && pluginCommand.getPlugin().equals(plugin)) {
            return;
        }

        // Qualquer comando não permitido será bloqueado com mensagem customizada
        event.setCancelled(true);
        sendUnknownCommandMessage(player, originalCommand);
    }

    /**
     * Envia uma mensagem customizada e bonita quando o jogador tenta usar um
     * comando desconhecido.
     */
    private void sendUnknownCommandMessage(Player player, String command) {
        player.sendMessage("");
        player.sendMessage(ChatStorage.colorize("&8&m                                                    "));
        player.sendMessage("");
        player.sendMessage(ChatStorage.colorize("   &c&l✘ &c&lCOMANDO DESCONHECIDO"));
        player.sendMessage("");
        player.sendMessage(ChatStorage.colorize("   &7O comando &f&n/" + command + " &7nao foi encontrado."));
        player.sendMessage(ChatStorage.colorize("   &7Verifique a digitacao ou tente outro comando."));
        player.sendMessage("");
        player.sendMessage(ChatStorage.colorize("   &e&lSUGESTOES DE COMANDOS:"));
        player.sendMessage("");
        player.sendMessage(ChatStorage.colorize("   &6➤ &f/spawn &8- &7Voltar ao lobby"));
        player.sendMessage(ChatStorage.colorize("   &6➤ &f/kit &8- &7Escolher seu kit"));
        player.sendMessage(ChatStorage.colorize("   &6➤ &f/conta &8- &7Ver estatisticas"));
        player.sendMessage(ChatStorage.colorize("   &6➤ &f/ranking &8- &7Ver classificacao"));
        player.sendMessage("");
        player.sendMessage(ChatStorage.colorize("&8&m                                                    "));
        player.sendMessage("");
    }

    /**
     * Verifica se o comando está na lista de permitidos.
     */
    private boolean isAllowedCommand(String command) {
        return allowedCommands.contains(command.toLowerCase());
    }

    /**
     * Adiciona dinamicamente um comando à lista de permitidos.
     * Útil para comandos registrados após a inicialização.
     */
    public void addAllowedCommand(String command) {
        allowedCommands.add(command.toLowerCase());
    }

    /**
     * Remove um comando da lista de permitidos.
     */
    public void removeAllowedCommand(String command) {
        allowedCommands.remove(command.toLowerCase());
    }

    /**
     * Verifica se um comando específico está bloqueado.
     */
    public boolean isBlocked(String command) {
        return !isAllowedCommand(command);
    }

    /**
     * Retorna o número de comandos permitidos.
     */
    public int getAllowedCommandCount() {
        return allowedCommands.size();
    }
}
