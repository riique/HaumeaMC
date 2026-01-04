package com.haumea.kitpvp.models;

import java.util.ArrayList;
import java.util.List;

/**
 * Classe que representa um Grupo/Cargo do servidor
 * Cada grupo possui um conjunto de permissões
 * 
 * @author HaumeaMC
 */
public class Group {

    private final String name;
    private String prefix;
    private String displayName;
    private int priority; // Maior = mais importante
    private List<String> permissions;
    private List<String> inheritance; // Grupos que este herda

    /**
     * Construtor do Grupo
     * 
     * @param name Nome interno do grupo (ex: "dono")
     */
    public Group(String name) {
        this.name = name.toLowerCase();
        this.prefix = "";
        this.displayName = name;
        this.priority = 0;
        this.permissions = new ArrayList<>();
        this.inheritance = new ArrayList<>();
    }

    /**
     * Construtor completo do Grupo
     */
    public Group(String name, String prefix, String displayName, int priority, List<String> permissions,
            List<String> inheritance) {
        this.name = name.toLowerCase();
        this.prefix = prefix;
        this.displayName = displayName;
        this.priority = priority;
        this.permissions = permissions != null ? permissions : new ArrayList<>();
        this.inheritance = inheritance != null ? inheritance : new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }

    public void addPermission(String permission) {
        if (!permissions.contains(permission.toLowerCase())) {
            permissions.add(permission.toLowerCase());
        }
    }

    public void removePermission(String permission) {
        permissions.remove(permission.toLowerCase());
    }

    public boolean hasPermission(String permission) {
        return permissions.contains(permission.toLowerCase()) || permissions.contains("*");
    }

    public List<String> getInheritance() {
        return inheritance;
    }

    public void setInheritance(List<String> inheritance) {
        this.inheritance = inheritance;
    }

    public void addInheritance(String groupName) {
        if (!inheritance.contains(groupName.toLowerCase())) {
            inheritance.add(groupName.toLowerCase());
        }
    }
}
