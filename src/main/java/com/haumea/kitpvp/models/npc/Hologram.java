package com.haumea.kitpvp.models.npc;

import com.haumea.kitpvp.utils.ChatStorage;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;

import java.util.ArrayList;
import java.util.List;

/**
 * Representa um holograma usando ArmorStands invisíveis.
 * Cada linha do holograma é um ArmorStand separado.
 * Posicionamento de baixo para cima (linha 0 é a mais alta).
 */
public class Hologram {

    // Distância entre linhas do holograma
    private static final double LINE_HEIGHT = 0.25;

    private final Location baseLocation;
    private final List<ArmorStand> armorStands;
    private List<String> lines;

    public Hologram(Location baseLocation, List<String> lines) {
        this.baseLocation = baseLocation.clone();
        this.armorStands = new ArrayList<>();
        this.lines = new ArrayList<>(lines);
        spawn();
    }

    /**
     * Spawna o holograma criando ArmorStands invisíveis
     */
    private void spawn() {
        // Limpar existentes
        despawn();

        // Spawnar de baixo para cima (últimas linhas em baixo)
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);

            // Se linha vazia, pular (não criar ArmorStand)
            if (line == null || line.trim().isEmpty()) {
                // Criar ArmorStand invisível para manter espaçamento
                double yOffset = (lines.size() - 1 - i) * LINE_HEIGHT;
                Location loc = baseLocation.clone().add(0, yOffset, 0);

                ArmorStand armorStand = (ArmorStand) loc.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
                armorStand.setVisible(false);
                armorStand.setGravity(false);
                armorStand.setCanPickupItems(false);
                armorStand.setCustomNameVisible(false); // NÃO MOSTRAR NOME em linhas vazias
                armorStand.setCustomName("");
                armorStand.setMarker(true);
                armorStand.setSmall(true);
                armorStands.add(armorStand);
                continue;
            }

            // Calcular altura (linhas de cima para baixo)
            double yOffset = (lines.size() - 1 - i) * LINE_HEIGHT;
            Location loc = baseLocation.clone().add(0, yOffset, 0);

            ArmorStand armorStand = (ArmorStand) loc.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
            armorStand.setVisible(false);
            armorStand.setGravity(false);
            armorStand.setCanPickupItems(false);
            armorStand.setMarker(true); // Sem hitbox
            armorStand.setSmall(true);

            // IMPORTANTE: Setar nome ANTES de setar visibilidade
            String coloredLine = ChatStorage.colorize(line);
            armorStand.setCustomName(coloredLine);
            armorStand.setCustomNameVisible(true);

            armorStands.add(armorStand);
        }
    }

    /**
     * Atualiza uma linha específica do holograma
     */
    public void updateLine(int index, String text) {
        if (index >= 0 && index < armorStands.size()) {
            armorStands.get(index).setCustomName(ChatStorage.colorize(text));
            lines.set(index, text);
        }
    }

    /**
     * Atualiza todas as linhas do holograma
     */
    public void updateLines(List<String> newLines) {
        // Se quantidade de linhas mudou, refazer tudo
        if (newLines.size() != lines.size()) {
            this.lines = new ArrayList<>(newLines);
            spawn();
            return;
        }

        // Atualizar texto existente
        for (int i = 0; i < newLines.size(); i++) {
            updateLine(i, newLines.get(i));
        }
    }

    /**
     * Remove todos os ArmorStands do holograma
     */
    public void despawn() {
        for (ArmorStand armorStand : armorStands) {
            if (armorStand != null && !armorStand.isDead()) {
                armorStand.remove();
            }
        }
        armorStands.clear();
    }

    public Location getBaseLocation() {
        return baseLocation.clone();
    }

    public List<String> getLines() {
        return new ArrayList<>(lines);
    }
}
