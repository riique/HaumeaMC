package com.haumea.kitpvp.commands.medal;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.managers.MedalManager;
import com.haumea.kitpvp.models.Medal;
import com.haumea.kitpvp.utils.ChatStorage;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Comando /medalha para gerenciar medalhas dos jogadores.
 * 
 * Uso:
 * - /medalha - Lista medalhas disponíveis (clicáveis)
 * - /medalha <nome> - Equipa uma medalha
 * - /medalha remover - Remove a medalha atual
 * 
 * @author HaumeaMC
 */
public class MedalCommand implements CommandExecutor, TabCompleter {

    private final MedalManager medalManager;

    /**
     * Prefixo para mensagens de medalha
     */
    private static final String PREFIX = "§b§lMEDALHAS §f";

    public MedalCommand(HaumeaMC plugin) {
        this.medalManager = plugin.getMedalManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Verificar se é jogador
        if (!(sender instanceof Player)) {
            ChatStorage.send(sender, "error.player-only");
            return true;
        }

        Player player = (Player) sender;

        // Sem argumentos: listar medalhas
        if (args.length == 0) {
            showMedalList(player);
            return true;
        }

        String arg = args[0].toLowerCase();

        // Comando para remover medalha
        if (arg.equals("remover") || arg.equals("remove") || arg.equals("off") || arg.equals("none")) {
            removeMedal(player);
            return true;
        }

        // Tentar equipar medalha
        equipMedal(player, args[0]);
        return true;
    }

    /**
     * Exibe a lista de medalhas disponíveis para o jogador
     * com TextComponents clicáveis
     * 
     * @param player Jogador
     */
    private void showMedalList(Player player) {
        List<Medal> availableMedals = medalManager.getAvailableMedals(player);

        // Header
        player.sendMessage("");
        player.sendMessage(ChatStorage.getSeparator());
        player.sendMessage("");
        player.sendMessage("  " + PREFIX + "§7Suas medalhas disponíveis:");
        player.sendMessage("");

        if (availableMedals.isEmpty()) {
            player.sendMessage("  §cVocê não possui nenhuma medalha!");
            player.sendMessage("  §7Adquira medalhas em nossa §bloja§7!");
        } else {
            // Medalha atualmente equipada
            Medal currentMedal = medalManager.getPlayerMedal(player);
            if (currentMedal != null) {
                player.sendMessage("  §7Medalha atual: " + currentMedal.getDisplay() + " §f" + currentMedal.getName());
                player.sendMessage("");
            }

            // Criar linha de medalhas clicáveis
            TextComponent medalLine = new TextComponent("  ");

            int count = 0;
            for (Medal medal : availableMedals) {
                if (count > 0) {
                    medalLine.addExtra(new TextComponent("  ")); // Espaçamento entre medalhas
                }

                // Criar componente clicável para cada medalha
                TextComponent medalComponent = createMedalComponent(medal, currentMedal);
                medalLine.addExtra(medalComponent);

                count++;

                // Quebra de linha a cada 7 medalhas
                if (count % 7 == 0 && count < availableMedals.size()) {
                    player.spigot().sendMessage(medalLine);
                    medalLine = new TextComponent("  ");
                }
            }

            // Enviar última linha se houver medalhas restantes
            if (count % 7 != 0) {
                player.spigot().sendMessage(medalLine);
            }

            player.sendMessage("");
            player.sendMessage("  §7Clique em uma medalha para equipá-la!");

            // Botão de remover
            if (currentMedal != null) {
                TextComponent removeComponent = new TextComponent("  §c[REMOVER MEDALHA]");
                removeComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/medalha remover"));
                removeComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        new ComponentBuilder("§cClique para remover sua medalha").create()));
                player.spigot().sendMessage(removeComponent);
            }
        }

        player.sendMessage("");
        player.sendMessage(ChatStorage.getSeparator());
        player.sendMessage("");
    }

    /**
     * Cria um TextComponent clicável para uma medalha
     * 
     * @param medal        Medalha
     * @param currentMedal Medalha atualmente equipada (pode ser null)
     * @return TextComponent configurado
     */
    private TextComponent createMedalComponent(Medal medal, Medal currentMedal) {
        boolean isEquipped = currentMedal != null && currentMedal.equals(medal);

        // Símbolo da medalha
        String displayText = medal.getDisplay();
        if (isEquipped) {
            displayText = "§8[" + medal.getDisplay() + "§8]"; // Destacar medalha equipada
        }

        TextComponent component = new TextComponent(displayText);

        // Configurar hover
        String hoverText = "§f" + medal.getName() + "\n" +
                "§7Símbolo: " + medal.getDisplay() + "\n" +
                "\n" +
                (isEquipped ? "§a✔ Equipada" : "§eClique para equipar");

        component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder(hoverText).create()));

        // Configurar click se não estiver equipada
        if (!isEquipped) {
            component.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                    "/medalha " + medal.getName()));
        }

        return component;
    }

    /**
     * Equipa uma medalha para o jogador
     * 
     * @param player    Jogador
     * @param medalName Nome da medalha
     */
    private void equipMedal(Player player, String medalName) {
        // Verificar se medalha existe
        Medal medal = medalManager.getMedal(medalName);
        if (medal == null) {
            player.sendMessage(PREFIX + ChatStorage.getMessage("medal.not-found"));
            return;
        }

        // Verificar permissão
        if (!medalManager.hasPermission(player, medal)) {
            player.sendMessage(PREFIX + ChatStorage.getMessage("medal.no-permission",
                    "medal", medal.getDisplay()));
            return;
        }

        // Verificar se já está usando
        Medal currentMedal = medalManager.getPlayerMedal(player);
        if (currentMedal != null && currentMedal.equals(medal)) {
            player.sendMessage(PREFIX + ChatStorage.getMessage("medal.already-using"));
            return;
        }

        // Equipar medalha
        if (medalManager.setPlayerMedal(player, medal)) {
            player.sendMessage(PREFIX + ChatStorage.getMessage("medal.selected",
                    "medal", medal.getDisplay() + " §f" + medal.getName()));
        }
    }

    /**
     * Remove a medalha atual do jogador
     * 
     * @param player Jogador
     */
    private void removeMedal(Player player) {
        Medal currentMedal = medalManager.getPlayerMedal(player);

        if (currentMedal == null) {
            player.sendMessage(PREFIX + ChatStorage.getMessage("medal.no-medal-equipped"));
            return;
        }

        medalManager.removePlayerMedal(player);
        player.sendMessage(PREFIX + ChatStorage.getMessage("medal.removed"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (!(sender instanceof Player)) {
            return completions;
        }

        Player player = (Player) sender;

        if (args.length == 1) {
            String input = args[0].toLowerCase();

            // Adicionar "remover" como opção
            if ("remover".startsWith(input)) {
                completions.add("remover");
            }

            // Adicionar medalhas disponíveis
            List<Medal> available = medalManager.getAvailableMedals(player);
            for (Medal medal : available) {
                if (medal.getName().toLowerCase().startsWith(input)) {
                    completions.add(medal.getName());
                }
            }
        }

        return completions;
    }
}
