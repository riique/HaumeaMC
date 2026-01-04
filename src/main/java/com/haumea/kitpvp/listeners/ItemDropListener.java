package com.haumea.kitpvp.listeners;

import com.haumea.kitpvp.HaumeaMC;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Listener para gerenciar o sistema de drops de itens.
 * 
 * Regras:
 * - Apenas SOPAS e BOWLS podem ser dropados pelo jogador
 * - Itens dropados NÃO podem ser pegos por ninguém
 * - Itens dropados desaparecem após 1.5 segundos com efeito visual bonito
 * 
 * @author HaumeaMC
 */
public class ItemDropListener implements Listener {

    private final HaumeaMC plugin;

    // Materiais permitidos para drop
    private static final Material[] ALLOWED_DROP_MATERIALS = {
            Material.MUSHROOM_SOUP,
            Material.BOWL,
            // Ingredientes de recap (para poder jogar fora)
            Material.RED_MUSHROOM,
            Material.BROWN_MUSHROOM
    };

    // Tempo de despawn em ticks (1.5 segundos = 30 ticks)
    private static final int DESPAWN_TIME_TICKS = 30;

    public ItemDropListener(HaumeaMC plugin) {
        this.plugin = plugin;
    }

    /**
     * Permite drop apenas de itens permitidos
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        ItemStack droppedItem = event.getItemDrop().getItemStack();

        // Verificar se o item é permitido para drop
        if (!isAllowedToDrop(droppedItem.getType())) {
            event.setCancelled(true);
            return;
        }

        // Se o item foi dropado, agendar remoção com efeito
        Item itemEntity = event.getItemDrop();
        scheduleItemRemoval(itemEntity);
    }

    /**
     * Também processa itens que spawnam no mundo (ex: morte de jogador)
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onItemSpawn(ItemSpawnEvent event) {
        if (event.isCancelled())
            return;

        Item itemEntity = event.getEntity();
        scheduleItemRemoval(itemEntity);
    }

    /**
     * Bloqueia a coleta de TODOS os itens do chão
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        // Ninguém pode pegar itens do chão
        event.setCancelled(true);
    }

    /**
     * Verifica se o tipo de item pode ser dropado
     * 
     * @param material Tipo do material
     * @return true se pode ser dropado
     */
    private boolean isAllowedToDrop(Material material) {
        for (Material allowed : ALLOWED_DROP_MATERIALS) {
            if (material == allowed) {
                return true;
            }
        }
        return false;
    }

    /**
     * Agenda a remoção de um item do chão após 1.5 segundos
     * com efeito visual bonito
     * 
     * @param itemEntity Item no chão
     */
    private void scheduleItemRemoval(Item itemEntity) {
        if (itemEntity == null)
            return;

        new BukkitRunnable() {
            private int ticks = 0;
            private float lastY = 0;
            private boolean phaseStarted = false;

            @Override
            public void run() {
                // Verificar se o item ainda existe
                if (itemEntity == null || itemEntity.isDead() || !itemEntity.isValid()) {
                    cancel();
                    return;
                }

                ticks++;
                Location loc = itemEntity.getLocation();

                // Fase 1: Primeiros 0.5 segundos - item normal
                if (ticks <= 10) {
                    return;
                }

                // Fase 2: 0.5 a 1.0 segundos - começar a piscar
                if (ticks > 10 && ticks <= 20) {
                    if (ticks % 4 == 0) {
                        // Partículas de fumaça leves
                        loc.getWorld().playEffect(loc, Effect.SMOKE, 1);
                    }
                    return;
                }

                // Fase 3: 1.0 a 1.5 segundos - piscar mais rápido + partículas
                if (ticks > 20 && ticks < DESPAWN_TIME_TICKS) {
                    if (ticks % 2 == 0) {
                        // Partículas mais frequentes
                        loc.getWorld().playEffect(loc, Effect.SMOKE, 2);

                        // Efeito de "flutuar" - subir e descer levemente
                        if (!phaseStarted) {
                            lastY = (float) loc.getY();
                            phaseStarted = true;
                        }
                    }
                    return;
                }

                // Fase 4: Remoção com efeito final
                if (ticks >= DESPAWN_TIME_TICKS) {
                    // Efeitos finais bonitos
                    playDespawnEffect(loc);

                    // Remover o item
                    itemEntity.remove();
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L); // Executar a cada tick
    }

    /**
     * Efeito visual de despawn do item
     */
    private void playDespawnEffect(Location loc) {
        // Efeito de nuvem
        loc.getWorld().playEffect(loc, Effect.CLOUD, 0);

        // Efeito de partículas mágicas
        loc.getWorld().playEffect(loc, Effect.WITCH_MAGIC, 0);
        loc.getWorld().playEffect(loc, Effect.WITCH_MAGIC, 0);

        // Efeito de explosão pequena (visual bonito)
        loc.getWorld().playEffect(loc.clone().add(0, 0.3, 0), Effect.LARGE_SMOKE, 0);

        // Partículas em volta
        for (int i = 0; i < 8; i++) {
            double angle = Math.PI * 2 * i / 8;
            Location particleLoc = loc.clone().add(
                    Math.cos(angle) * 0.3,
                    0.2,
                    Math.sin(angle) * 0.3);
            loc.getWorld().playEffect(particleLoc, Effect.SMOKE, 4);
        }

        // Som sutil de despawn
        loc.getWorld().playSound(loc, Sound.FIZZ, 0.3f, 2.0f);
    }
}
