package com.haumea.kitpvp.abilities.kits;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.abilities.Ability;
import com.haumea.kitpvp.abilities.AbilityRarity;
import com.haumea.kitpvp.abilities.Ejectable;
import com.haumea.kitpvp.utils.ChatStorage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Kit Ninja
 * 
 * Raridade: RARE | Preço: 1.500 | Ícone: NETHER_STAR | Cooldown: 5s
 * 
 * Funcionalidade:
 * - Rastreia o último jogador que você atacou
 * - Ao agachar (Shift), teleporta para as costas desse jogador
 * - Não funciona se o alvo estiver na área protegida (spawn)
 * - Respeita as regras do Gladiator
 * 
 * @author HaumeaMC
 */
public class NinjaAbility extends Ability implements Ejectable {

    // Último alvo atacado por cada jogador
    private final Map<UUID, UUID> targetId = new HashMap<>();

    public NinjaAbility(HaumeaMC plugin) {
        super(plugin, AbilityRarity.RARE, Material.NETHER_STAR, 5, 1500);
    }

    @EventHandler(ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player))
            return;
        if (!(event.getEntity() instanceof Player))
            return;

        Player attacker = (Player) event.getDamager();
        Player victim = (Player) event.getEntity();

        if (!hasAbility(attacker))
            return;

        // Registrar último alvo atacado
        targetId.put(attacker.getUniqueId(), victim.getUniqueId());
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        if (!event.isSneaking())
            return;

        Player player = event.getPlayer();
        if (!hasAbility(player))
            return;

        UUID targetUUID = targetId.get(player.getUniqueId());
        if (targetUUID == null) {
            ChatStorage.sendRaw(player, "&c&lHAUMEAMC&f Voce precisa atacar alguem primeiro!");
            return;
        }

        Player target = Bukkit.getPlayer(targetUUID);
        if (target == null || !target.isOnline() || target.equals(player)) {
            targetId.remove(player.getUniqueId());
            ChatStorage.sendRaw(player, "&c&lHAUMEAMC&f O alvo nao esta mais online!");
            return;
        }

        // Verificar cooldown
        if (isInCooldown(player)) {
            sendCooldownMessage(player);
            return;
        }

        // Verificar se o alvo está protegido
        if (isProtected(target)) {
            targetId.remove(player.getUniqueId());
            ChatStorage.sendRaw(player, "&c&lHAUMEAMC&f O jogador esta no spawn!");
            return;
        }

        // Verificar regras do Gladiator
        if (plugin.getAbilityManager() != null) {
            Ability gladiator = plugin.getAbilityManager().getAbility("Gladiator");
            if (gladiator instanceof GladiatorAbility) {
                GladiatorAbility gladAbility = (GladiatorAbility) gladiator;
                boolean playerInArena = gladAbility.isInArena(player);
                boolean targetInArena = gladAbility.isInArena(target);

                if (playerInArena != targetInArena) {
                    ChatStorage.sendRaw(player, "&c&lHAUMEAMC&f Voces precisam estar na mesma arena!");
                    return;
                }

                // Se ambos em arena, verificar se é a mesma
                if (playerInArena) {
                    UUID playerArena = gladAbility.getArenaOwner(player);
                    UUID targetArena = gladAbility.getArenaOwner(target);

                    if (!playerArena.equals(targetArena)) {
                        ChatStorage.sendRaw(player, "&c&lHAUMEAMC&f O alvo esta em outra arena!");
                        return;
                    }
                }
            }
        }

        putInCooldown(player);
        targetId.remove(player.getUniqueId());

        // Teleportar para as costas do alvo
        Location targetLoc = target.getLocation().clone();
        // Calcular posição atrás do alvo
        float yaw = targetLoc.getYaw();
        double radians = Math.toRadians(yaw);

        Location behindTarget = targetLoc.clone();
        behindTarget.add(Math.sin(radians) * 1.5, 0, -Math.cos(radians) * 1.5);
        behindTarget.setYaw(yaw); // Mesma direção que o alvo

        player.teleport(behindTarget);
        player.setFallDistance(0);

        // Efeitos
        player.playSound(player.getLocation(), Sound.ENDERMAN_TELEPORT, 1.0f, 1.5f);
        ChatStorage.sendRaw(player, "&6&lHAUMEAMC&f Voce teleportou para as costas de &e" + target.getName() + "&f!");

        // Registrar combate
        registerCombat(target, player);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        eject(event.getPlayer());
    }

    @Override
    public void eject(Player player) {
        targetId.remove(player.getUniqueId());

        // Remover também como alvo de outros
        targetId.entrySet().removeIf(entry -> entry.getValue().equals(player.getUniqueId()));
    }
}
