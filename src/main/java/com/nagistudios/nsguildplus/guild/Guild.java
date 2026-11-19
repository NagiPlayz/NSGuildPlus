package com.nagistudios.nsguildplus.guild;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class Guild {
    public final UUID id;
    public String name;
    public UUID captain;
    public UUID vice;
    public final Set<UUID> officers = new LinkedHashSet<>();
    public final LinkedHashMap<UUID, GuildMember> members = new LinkedHashMap<>();
    public int stars;
    public String tagStyle = "white";
    public final Set<String> ownedStyles = new LinkedHashSet<>();
    public boolean friendlyFire;
    public int nameChangesUsed;
    public long createdAt;
    public final Set<UUID> allies = new LinkedHashSet<>();
    public final LinkedHashMap<String, Hideout> hideouts = new LinkedHashMap<>();
    public final Map<Integer, ItemStack> enderchest = new HashMap<>();
    /** Shared runtime inventory, created on first use. */
    public Inventory enderInventory;

    public Guild(UUID id, String name, UUID captain) {
        this.id = id;
        this.name = name;
        this.captain = captain;
    }

    public Role roleOf(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        if (uuid.equals(captain)) {
            return Role.CAPTAIN;
        }
        if (uuid.equals(vice)) {
            return Role.VICE_CAPTAIN;
        }
        if (officers.contains(uuid)) {
            return Role.OFFICER;
        }
        return members.containsKey(uuid) ? Role.MEMBER : null;
    }

    public GuildMember member(UUID uuid) {
        return members.get(uuid);
    }

    public boolean isMember(UUID uuid) {
        return members.containsKey(uuid);
    }

    public List<Player> onlinePlayers() {
        List<Player> list = new ArrayList<>();
        for (UUID u : members.keySet()) {
            Player p = Bukkit.getPlayer(u);
            if (p != null && p.isOnline()) {
                list.add(p);
            }
        }
        return list;
    }

    public String memberName(UUID uuid) {
        if (uuid == null) {
            return "";
        }
        GuildMember m = members.get(uuid);
        if (m != null && m.name != null) {
            return m.name;
        }
        String n = Bukkit.getOfflinePlayer(uuid).getName();
        return n == null ? uuid.toString().substring(0, 8) : n;
    }
}
