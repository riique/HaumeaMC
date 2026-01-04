package com.haumea.kitpvp.abilities.kits;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.abilities.Ability;
import com.haumea.kitpvp.abilities.AbilityRarity;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Kit Jumper (Saltador)
 * 
 * Raridade: MYSTIC | Preço: 2.500 | Ícone: EYE_OF_ENDER | Cooldown: 10s
 * 
 * Funcionalidade:
 * - Ao clicar com botão direito, lança uma Ender Pearl customizada
 * - O jogador "monta" na pearl e viaja junto com ela
 * - Imune a dano de queda e sufocamento durante o voo
 * 
 * @author HaumeaMC
 */
public class JumperAbility extends Ability {

    private static final String JUMPER_METADATA = "JumperPearl";
    private final Set<UUID> flyingPlayers = new HashSet<>();

    public JumperAbility(HaumeaMC plugin) {
        super(plugin, AbilityRarity.MYSTIC, Material.EYE_OF_ENDER, 10, 2500,
                new ItemStack[] { createJumperItem() });
    }

    private static ItemStack createJumperItem() {
        ItemStack item = new ItemStack(Material.EYE_OF_ENDER);
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§a§lJumper");
        java.util.List<String> lore = new java.util.ArrayList<>();
        lore.add("§7Clique direito para voar");
        lore.add("§7montado em uma pearl!");
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

        if (isInCooldown(player)) {
            sendCooldownMessage(player);
            return;
        }

        // Verificar se está em arena do Gladiator
        if (plugin.getAbilityManager() != null) {
            Ability gladiator = plugin.getAbilityManager().getAbility("Gladiator");
            if (gladiator instanceof GladiatorAbility) {
                if (((GladiatorAbility) gladiator).isInArena(player)) {
                    player.sendMessage("§c§lHAUMEAMC§f Você não pode usar isso na arena do Gladiator!");
                    return;
                }
            }
        }

        putInCooldown(player);

        // Lançar ender pearl
        EnderPearl pearl = player.launchProjectile(EnderPearl.class);
        pearl.setVelocity(player.getLocation().getDirection().multiply(1.5));
        pearl.setMetadata(JUMPER_METADATA, new FixedMetadataValue(plugin, player.getUniqueId().toString()));

        // Montar o jogador na pearl
        pearl.setPassenger(player);
        flyingPlayers.add(player.getUniqueId());

        // Efeito sonoro
        player.playSound(player.getLocation(), Sound.ENDERMAN_TELEPORT, 1.0f, 1.5f);

        // Task para partículas durante o voo
        new BukkitRunnable() {
            @Override
            public void run() {
                if (pearl.isDead() || !player.isOnline() || !flyingPlayers.contains(player.getUniqueId())) {
                    flyingPlayers.remove(player.getUniqueId());
                    cancel();
                    return;
                }

                // Partículas
                Location loc = pearl.getLocation();
                loc.getWorld().playEffect(loc, Effect.INSTANT_SPELL, 0);
            }
        }.runTaskTimer(plugin, 1L, 2L);

        // Timeout de segurança (5 segundos máximo de voo)
        new BukkitRunnable() {
            @Override
            public void run() {
                if (flyingPlayers.contains(player.getUniqueId())) {
                    flyingPlayers.remove(player.getUniqueId());
                    if (!pearl.isDead()) {
                        pearl.remove();
                    }
                    player.eject();
                    player.setFallDistance(0);
                }
            }
        }.runTaskLater(plugin, 100L); // 5 segundos
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof EnderPearl))
            return;

        EnderPearl pearl = (EnderPearl) event.getEntity();
        if (!pearl.hasMetadata(JUMPER_METADATA))
            return;

        // Remover o passageiro antes que a pearl cause dano
        if (pearl.getPassenger() instanceof Player) {
            Player player = (Player) pearl.getPassenger();
            flyingPlayers.remove(player.getUniqueId());
            player.eject();
            player.setFallDistance(0);
        }

        // Remover a pearl
        pearl.remove();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player))
            return;

        Player player = (Player) event.getEntity();

        if (!flyingPlayers.contains(player.getUniqueId()))
            return;

        DamageCause cause = event.getCause();

        // Imunidade durante o voo
        if (cause == DamageCause.FALL ||
                cause == DamageCause.SUFFOCATION ||
                cause == DamageCause.ENTITY_ATTACK) {
            event.setCancelled(true);
            player.setFallDistance(0);
        }
    }
}
