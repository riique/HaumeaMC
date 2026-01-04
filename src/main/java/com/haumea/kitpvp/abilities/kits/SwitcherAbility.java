package com.haumea.kitpvp.abilities.kits;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.abilities.Ability;
import com.haumea.kitpvp.abilities.AbilityRarity;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;

/**
 * Kit Switcher (Trocador)
 * 
 * Raridade: EPIC | Preço: 2.000 | Ícone: SNOW_BALL | Cooldown: 2s
 * 
 * Funcionalidade:
 * - Lança uma bola de neve especial
 * - Ao acertar um jogador, troca de posição instantaneamente
 * - Não funciona em jogadores com kit Neo
 * - Não funciona em jogadores na área protegida
 * 
 * @author HaumeaMC
 */
public class SwitcherAbility extends Ability {

    private static final String SWITCHER_METADATA = "SwitcherSnowball";

    public SwitcherAbility(HaumeaMC plugin) {
        super(plugin, AbilityRarity.EPIC, Material.SNOW_BALL, 2, 2000,
                new ItemStack[] { createSwitcherItem() });
    }

    private static ItemStack createSwitcherItem() {
        ItemStack item = new ItemStack(Material.SNOW_BALL, 16);
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§a§lSwitcher");
        java.util.List<String> lore = new java.util.ArrayList<>();
        lore.add("§7Clique direito para lançar");
        lore.add("§7e trocar de lugar com o alvo!");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (!hasAbility(player))
            return;
        if (!isMainItem(player.getItemInHand()))
            return;

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK)
            return;

        event.setCancelled(true);
        player.updateInventory();

        if (isInCooldown(player)) {
            sendCooldownMessage(player);
            return;
        }

        putInCooldown(player);

        // Lançar snowball
        Snowball snowball = player.launchProjectile(Snowball.class);
        snowball.setVelocity(snowball.getVelocity().multiply(2)); // Velocidade dobrada
        snowball.setMetadata(SWITCHER_METADATA, new FixedMetadataValue(plugin, player.getUniqueId().toString()));

        // Som
        player.playSound(player.getLocation(), Sound.SHOOT_ARROW, 1.0f, 1.5f);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onProjectileHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Snowball))
            return;

        Snowball snowball = (Snowball) event.getDamager();
        if (!snowball.hasMetadata(SWITCHER_METADATA))
            return;

        event.setCancelled(true);

        if (!(snowball.getShooter() instanceof Player))
            return;
        if (!(event.getEntity() instanceof Player))
            return;

        Player shooter = (Player) snowball.getShooter();
        Player victim = (Player) event.getEntity();

        // Verificar se a vítima está protegida
        if (isProtected(victim)) {
            shooter.sendMessage("§c§lHAUMEAMC§f O jogador está em área protegida!");
            return;
        }

        // Verificar se a vítima tem Neo
        if (Ability.hasAbility(victim, "Neo", plugin)) {
            shooter.sendMessage("§c§lHAUMEAMC§f O jogador está utilizando o kit Neo!");
            return;
        }

        // Trocar posições
        Location shooterLoc = shooter.getLocation().clone();
        Location victimLoc = victim.getLocation().clone();

        shooter.teleport(victimLoc);
        victim.teleport(shooterLoc);

        // Sons
        shooter.playSound(shooter.getLocation(), Sound.ENDERMAN_TELEPORT, 1.0f, 1.0f);
        victim.playSound(victim.getLocation(), Sound.ENDERMAN_TELEPORT, 1.0f, 1.0f);

        // Mensagens
        shooter.sendMessage("§6§lHAUMEAMC§f Você trocou de lugar com §e" + victim.getName() + "§f!");
        victim.sendMessage("§c§lHAUMEAMC§f Você foi teletransportado por §e" + shooter.getName() + "§f!");

        // Registrar combate
        registerCombat(victim, shooter);
    }
}
