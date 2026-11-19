package com.nagistudios.nsguildplus.guild;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.UUID;

public final class Hideout {
    public String name;
    public String world;
    public double x;
    public double y;
    public double z;
    public float yaw;
    public float pitch;
    public UUID createdBy;
    public long createdAt;

    public Hideout() {
    }

    public static Hideout at(String name, Location l, UUID by) {
        Hideout h = new Hideout();
        h.name = name;
        h.setLocation(l);
        h.createdBy = by;
        h.createdAt = System.currentTimeMillis() / 1000L;
        return h;
    }

    public void setLocation(Location l) {
        this.world = l.getWorld() == null ? "world" : l.getWorld().getName();
        this.x = l.getX();
        this.y = l.getY();
        this.z = l.getZ();
        this.yaw = l.getYaw();
        this.pitch = l.getPitch();
    }

    /** Null if the world is not loaded. */
    public Location toLocation() {
        World w = Bukkit.getWorld(world);
        if (w == null) {
            return null;
        }
        return new Location(w, x, y, z, yaw, pitch);
    }
}
