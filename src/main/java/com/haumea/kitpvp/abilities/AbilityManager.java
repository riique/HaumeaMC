package com.haumea.kitpvp.abilities;

import com.haumea.kitpvp.HaumeaMC;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

import java.util.*;

/**
 * Gerenciador de Abilities (Habilidades de Kits).
 * 
 * Responsável por:
 * - Carregar e registrar todas as abilities
 * - Gerenciar o ciclo de vida das abilities
 * - Fornecer acesso às abilities por nome
 * - Limpar dados temporários quando jogadores saem
 * 
 * @author HaumeaMC
 */
public class AbilityManager {

    private final HaumeaMC plugin;
    private final Map<String, Ability> abilities;
    private final List<Ability> abilityList;

    public AbilityManager(HaumeaMC plugin) {
        this.plugin = plugin;
        this.abilities = new LinkedHashMap<>();
        this.abilityList = new ArrayList<>();

        loadAbilities();
    }

    /**
     * Carrega e registra todas as abilities.
     */
    private void loadAbilities() {
        // Registrar todas as abilities do sistema

        // EPIC
        registerAbility(new com.haumea.kitpvp.abilities.kits.AnchorAbility(plugin));
        registerAbility(new com.haumea.kitpvp.abilities.kits.NeoAbility(plugin));
        registerAbility(new com.haumea.kitpvp.abilities.kits.QuickdropAbility(plugin));
        registerAbility(new com.haumea.kitpvp.abilities.kits.SwitcherAbility(plugin));

        // RARE
        registerAbility(new com.haumea.kitpvp.abilities.kits.FiremanAbility(plugin));
        registerAbility(new com.haumea.kitpvp.abilities.kits.FishermanAbility(plugin));
        registerAbility(new com.haumea.kitpvp.abilities.kits.FlashAbility(plugin));
        registerAbility(new com.haumea.kitpvp.abilities.kits.MagmaAbility(plugin));
        registerAbility(new com.haumea.kitpvp.abilities.kits.NinjaAbility(plugin));
        registerAbility(new com.haumea.kitpvp.abilities.kits.SnailAbility(plugin));
        registerAbility(new com.haumea.kitpvp.abilities.kits.SpiderAbility(plugin));
        registerAbility(new com.haumea.kitpvp.abilities.kits.SteelheadAbility(plugin));
        registerAbility(new com.haumea.kitpvp.abilities.kits.ThorAbility(plugin));
        registerAbility(new com.haumea.kitpvp.abilities.kits.ViperAbility(plugin));

        // LEGENDARY
        registerAbility(new com.haumea.kitpvp.abilities.kits.GrapplerAbility(plugin));

        // MYSTIC
        registerAbility(new com.haumea.kitpvp.abilities.kits.GladiatorAbility(plugin));
        registerAbility(new com.haumea.kitpvp.abilities.kits.JumperAbility(plugin));
        registerAbility(new com.haumea.kitpvp.abilities.kits.KangarooAbility(plugin));
        registerAbility(new com.haumea.kitpvp.abilities.kits.StomperAbility(plugin));

        plugin.getLogger().info("[AbilityManager] " + abilities.size() + " abilities registradas.");
    }

    /**
     * Registra uma nova ability no sistema.
     * 
     * @param ability Ability a registrar
     */
    public void registerAbility(Ability ability) {
        String name = ability.getName().toLowerCase();

        if (abilities.containsKey(name)) {
            plugin.getLogger().warning("[AbilityManager] Ability '" + name + "' já está registrada!");
            return;
        }

        abilities.put(name, ability);
        abilityList.add(ability);

        // Registrar como listener
        plugin.getServer().getPluginManager().registerEvents(ability, plugin);

        plugin.getLogger().info("[AbilityManager] Registrada ability: " + ability.getName() +
                " (" + ability.getRarity().getDisplayName() + ")");
    }

    /**
     * Remove uma ability do sistema.
     * 
     * @param name Nome da ability
     */
    public void unregisterAbility(String name) {
        Ability ability = abilities.remove(name.toLowerCase());
        if (ability != null) {
            abilityList.remove(ability);
            HandlerList.unregisterAll(ability);
        }
    }

    /**
     * Obtém uma ability pelo nome.
     * 
     * @param name Nome da ability
     * @return Ability ou null se não existir
     */
    public Ability getAbility(String name) {
        if (name == null)
            return null;
        return abilities.get(name.toLowerCase());
    }

    /**
     * Verifica se uma ability existe.
     * 
     * @param name Nome da ability
     * @return true se existe
     */
    public boolean hasAbility(String name) {
        return name != null && abilities.containsKey(name.toLowerCase());
    }

    /**
     * Obtém todas as abilities registradas.
     * 
     * @return Lista de abilities
     */
    public List<Ability> getAllAbilities() {
        return new ArrayList<>(abilityList);
    }

    /**
     * Obtém abilities por raridade.
     * 
     * @param rarity Raridade desejada
     * @return Lista de abilities com essa raridade
     */
    public List<Ability> getAbilitiesByRarity(AbilityRarity rarity) {
        List<Ability> result = new ArrayList<>();
        for (Ability ability : abilityList) {
            if (ability.getRarity() == rarity) {
                result.add(ability);
            }
        }
        return result;
    }

    /**
     * Obtém a quantidade de abilities registradas.
     * 
     * @return Número de abilities
     */
    public int getAbilityCount() {
        return abilities.size();
    }

    /**
     * Limpa dados temporários de um jogador em todas as abilities.
     * Chamado quando o jogador sai ou morre.
     * 
     * @param player Jogador
     */
    public void cleanupPlayer(Player player) {
        for (Ability ability : abilityList) {
            // Limpar cooldown
            ability.removeCooldown(player);

            // Se implementa Ejectable, chamar eject
            if (ability instanceof Ejectable) {
                ((Ejectable) ability).eject(player);
            }
        }
    }

    /**
     * Verifica se um jogador tem uma ability específica equipada.
     * 
     * @param player      Jogador
     * @param abilityName Nome da ability
     * @return true se tem a ability equipada
     */
    public boolean playerHasAbility(Player player, String abilityName) {
        Ability ability = getAbility(abilityName);
        return ability != null && ability.hasAbility(player);
    }

    /**
     * Obtém as abilities equipadas por um jogador.
     * 
     * @param player Jogador
     * @return Lista de abilities equipadas (primária e secundária)
     */
    public List<Ability> getPlayerAbilities(Player player) {
        List<Ability> result = new ArrayList<>();

        String primaryKit = plugin.getKitManager().getPrimaryKit(player);
        String secondaryKit = plugin.getKitManager().getSecondaryKit(player);

        if (primaryKit != null) {
            Ability primary = getAbility(primaryKit);
            if (primary != null) {
                result.add(primary);
            }
        }

        if (secondaryKit != null) {
            Ability secondary = getAbility(secondaryKit);
            if (secondary != null) {
                result.add(secondary);
            }
        }

        return result;
    }

    /**
     * Dá os itens de todas as abilities equipadas ao jogador.
     * 
     * @param player Jogador
     */
    public void giveAbilityItems(Player player) {
        for (Ability ability : getPlayerAbilities(player)) {
            ability.giveItems(player);
        }
    }

    /**
     * Obtém o preço ajustado de uma ability para a economia do servidor.
     * Os preços foram ajustados para a economia interna.
     * 
     * @param name Nome da ability
     * @return Preço ajustado
     */
    public int getAdjustedPrice(String name) {
        Ability ability = getAbility(name);
        if (ability == null)
            return 0;
        return ability.getPrice();
    }

    /**
     * Desliga o sistema de abilities.
     * Chamado quando o plugin é desabilitado.
     */
    public void shutdown() {
        // Limpar todos os jogadores online
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            cleanupPlayer(player);
        }

        // Desregistrar listeners
        for (Ability ability : abilityList) {
            HandlerList.unregisterAll(ability);
        }

        abilities.clear();
        abilityList.clear();
    }

    /**
     * Obtém informações de debug.
     */
    public String getDebugInfo() {
        return String.format("[AbilityManager] %d abilities registradas", abilities.size());
    }
}
