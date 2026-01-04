package com.haumea.kitpvp.commands.tag;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.managers.TagManager;
import com.haumea.kitpvp.models.Tag;
import com.haumea.kitpvp.utils.ChatStorage;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Comando /tag - Gerencia as tags dos jogadores
 * 
 * Uso: /tag - Mostra todas as tags disponíveis organizadas por categoria
 * /tag <nome> - Seleciona uma tag específica
 * 
 * @author HaumeaMC
 */
public class TagCommand implements CommandExecutor {

    private final HaumeaMC plugin;
    private final TagManager tagManager;

    // Configuração das categorias com ícones e cores
    private static final Map<String, CategoryInfo> CATEGORIES = new LinkedHashMap<>();

    static {
        CATEGORIES.put("staff", new CategoryInfo("§c§l✦ STAFF", "§c", "§4"));
        CATEGORIES.put("vip", new CategoryInfo("§6§l★ VIP", "§6", "§e"));
        CATEGORIES.put("festiva", new CategoryInfo("§d§l❄ FESTIVAS", "§d", "§5"));
        CATEGORIES.put("comum", new CategoryInfo("§7§l● COMUM", "§7", "§8"));
    }

    public TagCommand(HaumeaMC plugin) {
        this.plugin = plugin;
        this.tagManager = plugin.getTagManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // Verificar se é um jogador
        if (!(sender instanceof Player)) {
            ChatStorage.send(sender, "error.player-only");
            return true;
        }

        Player player = (Player) sender;

        // Se não houver argumentos, mostrar todas as tags disponíveis
        if (args.length == 0) {
            showAvailableTags(player);
            return true;
        }

        // Se houver argumento, tentar selecionar a tag
        String tagName = args[0].toLowerCase();

        // Normalizar aliases comuns (ex: yt+ -> ytplus)
        tagName = normalizeTagName(tagName);

        selectTag(player, tagName);

        return true;
    }

    /**
     * Normaliza o nome da tag para suportar aliases
     * Ex: yt+ -> ytplus, mod+ -> modplus
     * 
     * @param tagName Nome original
     * @return Nome normalizado
     */
    private String normalizeTagName(String tagName) {
        // Substituir + por plus no final
        if (tagName.endsWith("+")) {
            return tagName.substring(0, tagName.length() - 1) + "plus";
        }
        return tagName;
    }

    /**
     * Mostra todas as tags disponíveis para o jogador de forma elegante
     * 
     * @param player Jogador
     */
    private void showAvailableTags(Player player) {
        List<Tag> availableTags = tagManager.getAvailableTags(player);

        // Organizar tags por categoria
        Map<String, List<Tag>> tagsByCategory = new LinkedHashMap<>();
        for (String category : CATEGORIES.keySet()) {
            tagsByCategory.put(category, new ArrayList<Tag>());
        }

        for (Tag tag : availableTags) {
            String category = tag.getCategory();
            if (!tagsByCategory.containsKey(category)) {
                tagsByCategory.put(category, new ArrayList<Tag>());
            }
            tagsByCategory.get(category).add(tag);
        }

        // Tag atual do jogador
        Tag currentTag = tagManager.getPlayerTag(player);
        String currentTagName = currentTag != null ? currentTag.getFormattedDisplay() : "§7Nenhuma";

        // Exibir cabeçalho bonito
        player.sendMessage("");
        player.sendMessage("§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("");
        player.sendMessage("       §6§l✦ §e§lSELECIONE SUA TAG §6§l✦");
        player.sendMessage("");
        player.sendMessage("   §7Tag atual: " + currentTagName);
        player.sendMessage("   §7Uso: §f/tag <nome> §8ou §fclique na tag");
        player.sendMessage("");
        player.sendMessage("§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        // Exibir cada categoria que tem tags disponíveis
        boolean hasAnyTag = false;
        for (Map.Entry<String, List<Tag>> entry : tagsByCategory.entrySet()) {
            List<Tag> tags = entry.getValue();
            if (!tags.isEmpty()) {
                hasAnyTag = true;
                CategoryInfo info = CATEGORIES.get(entry.getKey());

                // Linha vazia antes da categoria
                player.sendMessage("");
                // Título da categoria
                player.sendMessage("  " + info.title);
                // Espaço entre título e tags
                player.sendMessage("");

                // Construir linha de tags clicáveis
                sendClickableTags(player, tags);
            }
        }

        // Se não tiver nenhuma tag
        if (!hasAnyTag) {
            player.sendMessage("");
            player.sendMessage("  §cVocê não possui nenhuma tag disponível!");
            player.sendMessage("  §7Adquira um §6VIP §7para desbloquear tags exclusivas.");
        }

        // Rodapé
        player.sendMessage("");
        player.sendMessage("§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("  §a➤ §7Clique em uma tag para selecioná-la!");
        player.sendMessage("§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("");
    }

    /**
     * Envia tags clicáveis para o jogador
     * 
     * @param player Jogador
     * @param tags   Lista de tags
     */
    private void sendClickableTags(Player player, List<Tag> tags) {
        // Criar componente base com espaçamento
        TextComponent message = new TextComponent("    ");

        for (int i = 0; i < tags.size(); i++) {
            Tag tag = tags.get(i);

            // Criar componente clicável para a tag
            TextComponent tagComponent = new TextComponent(tag.getFormattedDisplay());

            // Adicionar evento de clique - executa /tag <nome>
            tagComponent.setClickEvent(new ClickEvent(
                    ClickEvent.Action.RUN_COMMAND,
                    "/tag " + tag.getName()));

            // Adicionar hover text
            tagComponent.setHoverEvent(new HoverEvent(
                    HoverEvent.Action.SHOW_TEXT,
                    new ComponentBuilder("§aClique para selecionar a tag " + tag.getFormattedDisplay())
                            .append("\n§7Comando: §f/tag " + tag.getName())
                            .create()));

            message.addExtra(tagComponent);

            // Adicionar separador elegante entre as tags
            if (i < tags.size() - 1) {
                message.addExtra(new TextComponent(" §8│ "));
            }
        }

        // Enviar mensagem clicável
        player.spigot().sendMessage(message);
    }

    /**
     * Tenta selecionar uma tag para o jogador
     * 
     * @param player  Jogador
     * @param tagName Nome da tag
     */
    private void selectTag(Player player, String tagName) {
        // Verificar se a tag existe
        if (!tagManager.tagExists(tagName)) {
            ChatStorage.send(player, "tag.not-found");
            return;
        }

        Tag tag = tagManager.getTag(tagName);

        // Verificar se o jogador tem permissão
        if (!player.hasPermission(tag.getPermission())) {
            ChatStorage.send(player, "tag.no-permission", "tag", tag.getFormattedDisplay());
            return;
        }

        // Verificar se já está usando a tag
        Tag currentTag = tagManager.getPlayerTag(player);
        if (currentTag != null && currentTag.getName().equalsIgnoreCase(tagName)) {
            ChatStorage.send(player, "tag.already-using");
            return;
        }

        // Selecionar a tag
        if (tagManager.setPlayerTag(player, tagName)) {
            ChatStorage.send(player, "tag.selected", "tag", tag.getFormattedDisplay());
        }
    }

    /**
     * Classe interna para armazenar informações de categoria
     */
    private static class CategoryInfo {
        final String title;
        final String primaryColor;
        final String secondaryColor;

        CategoryInfo(String title, String primaryColor, String secondaryColor) {
            this.title = title;
            this.primaryColor = primaryColor;
            this.secondaryColor = secondaryColor;
        }
    }
}
