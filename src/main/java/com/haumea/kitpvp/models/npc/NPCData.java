package com.haumea.kitpvp.models.npc;

import org.bukkit.Location;

import java.util.List;

/**
 * Classe de dados para armazenar informações de um NPC configurado.
 * Usado pelo NPCManager para spawnar NPCs no lobby.
 */
public class NPCData {

    private final String id;
    private final String displayName;
    private final String skinName;
    private final String targetServer;
    private final Location location;
    private final List<String> hologramLines;

    public NPCData(String id, String displayName, String skinName, String targetServer,
            Location location, List<String> hologramLines) {
        this.id = id;
        this.displayName = displayName;
        this.skinName = skinName;
        this.targetServer = targetServer;
        this.location = location;
        this.hologramLines = hologramLines;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getSkinName() {
        return skinName;
    }

    public String getTargetServer() {
        return targetServer;
    }

    public Location getLocation() {
        return location;
    }

    public List<String> getHologramLines() {
        return hologramLines;
    }
}
