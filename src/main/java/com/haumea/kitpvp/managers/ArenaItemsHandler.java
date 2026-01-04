package com.haumea.kitpvp.managers;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.menu.shop.ShopMainMenu;
import com.haumea.kitpvp.menu.WarpsMenu;
import com.haumea.kitpvp.menu.kit.PrimaryKitMenu;
import com.haumea.kitpvp.menu.kit.SecondaryKitMenu;
import com.haumea.kitpvp.menu.profile.ProfileMenu;
import com.haumea.kitpvp.menu.casino.CasinoMainMenu;
import com.haumea.kitpvp.utils.ChatStorage;
import com.haumea.kitpvp.utils.ItemActionHandler;
import com.haumea.kitpvp.utils.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import com.haumea.kitpvp.models.Warp;
import com.haumea.kitpvp.profile.PlayerProfile;

/**
 * Gerenciador de itens de inventário para os estados Lobby e Combate.
 * 
 * Este handler controla quais itens o jogador recebe em diferentes situações:
 * - Lobby (Spawn): Itens de seleção de kits, perfil, warps, loja, evento
 * - Combate: Kit completo com espada, bússola rastreadora e sopas
 * 
 * Todos os itens interativos usam o sistema de {@link ItemActionHandler}
 * para processar cliques de forma eficiente.
 * 
 * @author HaumeaMC
 */
public class ArenaItemsHandler {

    private final HaumeaMC plugin;

    /**
     * Construtor do ArenaItemsHandler.
     * Registra todos os handlers de ação para os itens.
     * 
     * @param plugin Instância do plugin
     */
    public ArenaItemsHandler(HaumeaMC plugin) {
        this.plugin = plugin;
        registerItemActions();
    }

    /**
     * Registra todas as ações de itens no ItemActionHandler.
     */
    private void registerItemActions() {
        // Slot 0: Selecionar Kit 1
        ItemActionHandler.registerAction(ItemActionHandler.ACTION_SELECT_KIT_1, player -> {
            new PrimaryKitMenu(plugin, player).open();
        });

        // Slot 1: Selecionar Kit 2
        ItemActionHandler.registerAction(ItemActionHandler.ACTION_SELECT_KIT_2, player -> {
            new SecondaryKitMenu(plugin, player).open();
        });

        // Slot 3: Perfil
        ItemActionHandler.registerAction(ItemActionHandler.ACTION_PROFILE, player -> {
            new ProfileMenu(plugin, player).open();
        });

        // Slot 6: Warps
        ItemActionHandler.registerAction(ItemActionHandler.ACTION_WARPS, player -> {
            new WarpsMenu(plugin, player).open();
        });

        // Slot 7: Loja
        ItemActionHandler.registerAction(ItemActionHandler.ACTION_SHOP, player -> {
            ShopMainMenu.open(plugin, player);
        });

        // Slot 8: Evento
        ItemActionHandler.registerAction(ItemActionHandler.ACTION_EVENT, player -> {
            // Teleportar para o evento
            Warp eventWarp = plugin.getWarpsManager().getWarp("evento");
            if (eventWarp != null && eventWarp.isValid()) {
                player.teleport(eventWarp.toLocation());
                ChatStorage.sendCustom(player, "§aTeleportado para o §6Evento§a!");
            } else {
                ChatStorage.sendCustom(player, "§cNenhum evento ativo no momento.");
            }
        });

        // Bússola rastreadora (combate)
        ItemActionHandler.registerAction(ItemActionHandler.ACTION_TRACKER, this::handleTrackerClick);

        // Slot 2: Cassino
        ItemActionHandler.registerAction(ItemActionHandler.ACTION_CASINO, player -> {
            if (plugin.getCasinoManager() != null && plugin.getCasinoManager().isEnabled()) {
                new CasinoMainMenu(plugin, player).open();
            } else {
                ChatStorage.sendCustom(player, "§cCassino está desativado no momento.");
            }
        });
    }

    // ==================== ESTADO DE LOBBY ====================

    /**
     * Dá os itens de lobby para o jogador.
     * Limpa o inventário e equipa os itens interativos da hotbar.
     * 
     * @param player Jogador que receberá os itens
     */
    public void giveLobbyItems(Player player) {
        PlayerInventory inv = player.getInventory();

        // Limpar inventário completamente
        inv.clear();
        inv.setArmorContents(null);

        // Slot 0: Selecionar Kit 1
        ItemStack kit1 = new ItemBuilder(Material.CHEST)
                .name("§aSelecionar kit 1")
                .lore("§7Clique para escolher", "§7seu kit primário!")
                .actionId(ItemActionHandler.ACTION_SELECT_KIT_1)
                .build();
        inv.setItem(0, kit1);

        // Slot 1: Selecionar Kit 2
        ItemStack kit2 = new ItemBuilder(Material.CHEST)
                .name("§aSelecionar kit 2")
                .lore("§7Clique para escolher", "§7seu kit secundário!")
                .actionId(ItemActionHandler.ACTION_SELECT_KIT_2)
                .build();
        inv.setItem(1, kit2);

        // Slot 2: Cassino
        String coinsDisplay = "§eSaldo: §6Carregando...";
        if (plugin.getStatsManager() != null) {
            long coins = plugin.getStatsManager().getMoney(player);
            coinsDisplay = "§eSaldo: §6" + String.format("%,d", coins) + " coins";
        }
        ItemStack casino = new ItemBuilder(Material.GOLD_NUGGET)
                .name("§6§l✦ CASSINO ✦")
                .lore(
                        "",
                        "§7Teste sua sorte nos jogos!",
                        "§7Slots, Roleta, Blackjack...",
                        "",
                        coinsDisplay,
                        "",
                        "§aClique para abrir!")
                .glow()
                .actionId(ItemActionHandler.ACTION_CASINO)
                .build();
        inv.setItem(2, casino);

        // Slot 3: Perfil (Cabeça do jogador)
        ItemStack profile = ItemBuilder.playerHead(player.getName())
                .name("§aPerfil")
                .lore(
                        "§7Veja suas estatísticas:",
                        "",
                        "§8• §fKills, Deaths, K/D",
                        "§8• §fKillstreak atual",
                        "§8• §fCoins e rank",
                        "",
                        "§eClique para ver!")
                .actionId(ItemActionHandler.ACTION_PROFILE)
                .build();
        inv.setItem(3, profile);

        // Slot 6: Warps
        ItemStack warps = new ItemBuilder(Material.PAPER)
                .name("§aWarps")
                .lore(
                        "§7Selecione uma arena:",
                        "",
                        "§8• §eFPS",
                        "§8• §eDuels",
                        "§8• §eLava",
                        "§8• §eEvento",
                        "",
                        "§eClique para abrir!")
                .actionId(ItemActionHandler.ACTION_WARPS)
                .build();
        inv.setItem(6, warps);

        // Slot 7: Loja
        ItemStack shop = new ItemBuilder(Material.GOLD_INGOT)
                .name("§aLoja")
                .lore(
                        "§7Compre itens exclusivos!",
                        "",
                        "§8• §eKits especiais",
                        "§8• §eTags únicas",
                        "§8• §eCosmetics",
                        "",
                        "§eClique para abrir!")
                .glow()
                .actionId(ItemActionHandler.ACTION_SHOP)
                .build();
        inv.setItem(7, shop);

        // Slot 8: Evento (Baiacu/Pufferfish)
        ItemStack event = new ItemBuilder(Material.RAW_FISH, 1, (short) 3) // Pufferfish
                .name("§aEvento")
                .lore(
                        "§7Participe do evento atual!",
                        "",
                        "§cClique para teleportar!")
                .actionId(ItemActionHandler.ACTION_EVENT)
                .build();
        inv.setItem(8, event);

        // Atualizar inventário
        player.updateInventory();
    }

    // ==================== ESTADO DE COMBATE ====================

    /**
     * Dá os itens de combate para o jogador.
     * Limpa o inventário e equipa o kit de PvP completo.
     * 
     * @param player Jogador que receberá os itens
     */
    public void giveCombatItems(Player player) {
        giveCombatItems(player, false);
    }

    /**
     * Dá os itens de combate para o jogador.
     * 
     * @param player         Jogador que receberá os itens
     * @param hasKitSelected Se true, a espada não terá Sharpness I (o kit
     *                       sobrescreve)
     */
    public void giveCombatItems(Player player, boolean hasKitSelected) {
        giveCombatItems(player, hasKitSelected, 0);
    }

    /**
     * Dá os itens de combate para o jogador.
     * 
     * @param player         Jogador que receberá os itens
     * @param hasKitSelected Se true, a espada não terá Sharpness I
     * @param reserveSlots   Quantidade de slots a reservar para itens de kit
     */
    public void giveCombatItems(Player player, boolean hasKitSelected, int reserveSlots) {
        PlayerInventory inv = player.getInventory();

        // Limpar inventário completamente
        inv.clear();
        inv.setArmorContents(null);

        // Slot 0: Espada de Pedra Inquebrável
        ItemBuilder swordBuilder = ItemBuilder.stoneSword()
                .name("§fEspada de Pedra")
                .unbreakable();

        // Se não tem kit selecionado, adiciona Sharpness I
        if (!hasKitSelected) {
            swordBuilder.enchant(Enchantment.DAMAGE_ALL, 1);
        }

        inv.setItem(0, swordBuilder.build());

        // Slot 8: Bússola Rastreadora
        ItemStack tracker = ItemBuilder.compass()
                .name("§eRastreadora")
                .lore(
                        "§7Aponta para o inimigo",
                        "§7mais próximo!",
                        "",
                        "§eClique para rastrear!")
                .actionId(ItemActionHandler.ACTION_TRACKER)
                .build();
        inv.setItem(8, tracker);

        // Refill (Recap) - Slots 13, 14, 15 com 64 de cada
        inv.setItem(13, new ItemStack(Material.BOWL, 64));
        inv.setItem(14, new ItemStack(Material.RED_MUSHROOM, 64));
        inv.setItem(15, new ItemStack(Material.BROWN_MUSHROOM, 64));

        // Calcular slots reservados para itens de kit (slots 1-7 são usados para kits)
        // Slots a pular: 0 (espada), 8 (bússola), 13-15 (recap), e 1 até reserveSlots
        // para kits
        java.util.Set<Integer> reservedSlots = new java.util.HashSet<>();
        reservedSlots.add(0);
        reservedSlots.add(8);
        reservedSlots.add(13);
        reservedSlots.add(14);
        reservedSlots.add(15);

        // Reservar slots para kits (1, 2, 3...)
        for (int i = 1; i <= Math.min(reserveSlots, 7); i++) {
            reservedSlots.add(i);
        }

        // Preencher slots vazios com Sopa de Cogumelo
        ItemStack soup = ItemBuilder.mushroomSoup().build();

        for (int slot = 0; slot < 36; slot++) {
            // Pular slots reservados
            if (reservedSlots.contains(slot)) {
                continue;
            }
            // Pular slots que já têm itens
            if (inv.getItem(slot) != null) {
                continue;
            }
            inv.setItem(slot, soup.clone());
        }

        // Atualizar inventário
        player.updateInventory();
    }

    // ==================== BÚSSOLA RASTREADORA ====================

    /**
     * Processa o clique na bússola rastreadora.
     * Encontra o jogador válido mais próximo e aponta a bússola para ele.
     * 
     * @param player Jogador que clicou na bússola
     */
    private void handleTrackerClick(Player player) {
        Player target = findNearestTarget(player);

        if (target == null) {
            ChatStorage.sendCustom(player, "§cNenhum jogador encontrado para rastrear!");
            return;
        }

        // Atualizar o compass target do jogador
        player.setCompassTarget(target.getLocation());

        // Calcular distância
        int distance = (int) player.getLocation().distance(target.getLocation());

        // Enviar mensagem
        ChatStorage.sendCustom(player, "§eApontando para §6" + target.getName() + " §7(" + distance + " blocos)");
    }

    /**
     * Encontra o jogador válido mais próximo para rastrear.
     * Ignora jogadores que estão no spawn ou protegidos.
     * 
     * @param player Jogador que está rastreando
     * @return Jogador alvo ou null se nenhum encontrado
     */
    private Player findNearestTarget(Player player) {
        Player nearestTarget = null;
        double nearestDistance = Double.MAX_VALUE;

        Location playerLoc = player.getLocation();

        for (Player online : Bukkit.getOnlinePlayers()) {
            // Ignorar o próprio jogador
            if (online.equals(player)) {
                continue;
            }

            // Verificar se está no mesmo mundo
            if (!online.getWorld().equals(player.getWorld())) {
                continue;
            }

            // Verificar se o jogador está protegido (no spawn)
            if (isPlayerProtected(online)) {
                continue;
            }

            // Verificar se é staff em modo admin (invisível)
            if (isStaffInAdminMode(online)) {
                continue;
            }

            // Calcular distância
            double distance = playerLoc.distance(online.getLocation());

            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestTarget = online;
            }
        }

        return nearestTarget;
    }

    /**
     * Verifica se um jogador está protegido (no spawn).
     * 
     * @param player Jogador a verificar
     * @return true se está protegido
     */
    private boolean isPlayerProtected(Player player) {
        // Verificar se está na região de spawn
        Warp spawnWarp = plugin.getWarpsManager().getWarp("spawn");
        if (spawnWarp != null && spawnWarp.isValid()) {
            Location spawnLocation = spawnWarp.toLocation();
            // Considera protegido se estiver a menos de 30 blocos do spawn
            double distance = player.getLocation().distance(spawnLocation);
            if (distance < 30) {
                return true;
            }
        }

        // Verificar se o jogador está em combate (se não estiver, pode estar protegido)
        // TODO: Integrar com CombatTagManager quando implementado
        return false;
    }

    /**
     * Verifica se um jogador é staff em modo admin (invisível).
     * 
     * @param player Jogador a verificar
     * @return true se está em modo admin
     */
    private boolean isStaffInAdminMode(Player player) {
        // Verificar via ProfileManager se o jogador está em modo admin (vanish)
        if (plugin.getProfileManager() != null) {
            PlayerProfile profile = plugin.getProfileManager().getProfile(player);
            if (profile != null && profile.isVanish()) {
                return true;
            }
        }
        return false;
    }

    // ==================== UTILITÁRIOS ====================

    /**
     * Verifica se o jogador está no estado de lobby (spawn).
     * 
     * @param player Jogador a verificar
     * @return true se está no lobby
     */
    public boolean isInLobby(Player player) {
        return isPlayerProtected(player);
    }

    /**
     * Atualiza os itens do jogador baseado em sua localização atual.
     * Se estiver no spawn, dá itens de lobby. Caso contrário, mantém.
     * 
     * @param player Jogador a atualizar
     */
    public void updatePlayerItems(Player player) {
        if (isInLobby(player)) {
            giveLobbyItems(player);
        }
        // Se não está no lobby, não altera os itens
        // (os itens de combate são dados manualmente ao sair do spawn)
    }
}
