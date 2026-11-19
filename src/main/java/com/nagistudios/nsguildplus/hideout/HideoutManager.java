package com.nagistudios.nsguildplus.hideout;

import com.nagistudios.nsguildplus.NSGuildPlus;
import com.nagistudios.nsguildplus.guild.Guild;
import com.nagistudios.nsguildplus.guild.Hideout;
import com.nagistudios.nsguildplus.guild.Role;
import com.nagistudios.nsguildplus.message.MessageManager;
import com.nagistudios.nsguildplus.util.Ph;
import com.nagistudios.nsguildplus.util.Util;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/** Guild hideouts: set, delete, rename, move and the 3 second teleport. */
public final class HideoutManager {
    private static final class Session {
        final Guild guild;
        final Hideout hideout;
        final Location startBlock;
        int left;
        BukkitTask task;
        BossBar bar;

        Session(Guild guild, Hideout hideout, Location startBlock, int left) {
            this.guild = guild;
            this.hideout = hideout;
            this.startBlock = startBlock;
            this.left = left;
        }
    }

    private final NSGuildPlus plugin;
    private final Map<UUID, Session> sessions = new HashMap<>();
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Map<UUID, Long> combat = new HashMap<>();

    public HideoutManager(NSGuildPlus plugin) {
        this.plugin = plugin;
    }

    private MessageManager msg() {
        return plugin.messages();
    }

    private ConfigurationSection cfg() {
        return plugin.getConfig().getConfigurationSection("hideout");
    }

    private boolean tp(String path, boolean def) {
        return plugin.getConfig().getBoolean("hideout.teleport." + path, def);
    }

    // ------------------------------------------------------------------ limits and names

    public int limit(Guild g) {
        int max = plugin.getConfig().getInt("hideout.max-hideouts-per-guild", 3);
        if (!plugin.getConfig().getBoolean("hideout.scale-with-guild-level.enabled", false)) {
            return max;
        }
        ConfigurationSection levels = plugin.getConfig().getConfigurationSection("hideout.scale-with-guild-level.levels");
        if (levels == null) {
            return max;
        }
        int lvl = plugin.stats().level(g);
        int bestKey = -1;
        int value = max;
        for (String key : levels.getKeys(false)) {
            try {
                int k = Integer.parseInt(key);
                if (k <= lvl && k > bestKey) {
                    bestKey = k;
                    value = levels.getInt(key);
                }
            } catch (NumberFormatException ignored) {
                // bad key
            }
        }
        return value;
    }

    public boolean validName(String name) {
        int min = plugin.getConfig().getInt("hideout.name.min-length", 3);
        int max = plugin.getConfig().getInt("hideout.name.max-length", 16);
        String regex = plugin.getConfig().getString("hideout.name.regex", "^[a-zA-Z0-9_]+$");
        if (name.length() < min || name.length() > max) {
            return false;
        }
        try {
            return Pattern.compile(regex).matcher(name).matches();
        } catch (Exception e) {
            return true;
        }
    }

    public Map<String, String> ph(Hideout h) {
        return Ph.of("hideout_name", h.name, "world", h.world, "x", String.valueOf((int) h.x),
                "y", String.valueOf((int) h.y), "z", String.valueOf((int) h.z));
    }

    private boolean isCaptain(Player p, Guild g) {
        if (g == null) {
            msg().send(p, "general.not-in-guild");
            return false;
        }
        if (!plugin.roles().can(g.roleOf(p.getUniqueId()), "set-hideout")) {
            msg().send(p, "hideout.only-captain");
            return false;
        }
        return true;
    }

    public Hideout find(Guild g, String name) {
        return name == null ? null : g.hideouts.get(name.toLowerCase(Locale.ROOT));
    }

    // ------------------------------------------------------------------ management

    public void set(Player p, String name) {
        Guild g = plugin.guilds().ofPlayer(p.getUniqueId());
        if (!isCaptain(p, g)) {
            return;
        }
        if (!p.hasPermission("nsguildplus.hideout.set")) {
            msg().send(p, "general.no-permission");
            return;
        }
        if (!validName(name)) {
            msg().send(p, "hideout.invalid-name");
            return;
        }
        if (find(g, name) != null) {
            msg().send(p, "hideout.already-exists", Ph.of("hideout_name", name));
            return;
        }
        if (g.hideouts.size() >= limit(g)) {
            msg().send(p, "hideout.limit-reached", Ph.of("amount", String.valueOf(limit(g))));
            return;
        }
        if (isDisabledWorld(p.getWorld())) {
            msg().send(p, "hideout.world-disabled");
            return;
        }
        Hideout h = Hideout.at(name, p.getLocation(), p.getUniqueId());
        g.hideouts.put(name.toLowerCase(Locale.ROOT), h);
        plugin.storage().markDirty();
        msg().send(p, "hideout.set", ph(h));
        for (Player other : g.onlinePlayers()) {
            if (!other.equals(p)) {
                msg().send(other, "hideout.broadcast-set", Ph.with(ph(h), "player", p.getName()));
            }
        }
    }

    public void delete(Player p, Guild g, String name) {
        if (!isCaptain(p, g)) {
            return;
        }
        Hideout h = find(g, name);
        if (h == null) {
            msg().send(p, "hideout.not-found", Ph.of("hideout_name", name));
            return;
        }
        g.hideouts.remove(name.toLowerCase(Locale.ROOT));
        plugin.storage().markDirty();
        msg().send(p, "hideout.deleted", ph(h));
    }

    public void rename(Player p, String oldName, String newName) {
        Guild g = plugin.guilds().ofPlayer(p.getUniqueId());
        if (!isCaptain(p, g)) {
            return;
        }
        Hideout h = find(g, oldName);
        if (h == null) {
            msg().send(p, "hideout.not-found", Ph.of("hideout_name", oldName));
            return;
        }
        if (!validName(newName)) {
            msg().send(p, "hideout.invalid-name");
            return;
        }
        Hideout clash = find(g, newName);
        if (clash != null && clash != h) {
            msg().send(p, "hideout.already-exists", Ph.of("hideout_name", newName));
            return;
        }
        g.hideouts.remove(oldName.toLowerCase(Locale.ROOT));
        h.name = newName;
        g.hideouts.put(newName.toLowerCase(Locale.ROOT), h);
        plugin.storage().markDirty();
        msg().send(p, "hideout.renamed", ph(h));
    }

    public void move(Player p, String name) {
        Guild g = plugin.guilds().ofPlayer(p.getUniqueId());
        if (!isCaptain(p, g)) {
            return;
        }
        Hideout h = find(g, name);
        if (h == null) {
            msg().send(p, "hideout.not-found", Ph.of("hideout_name", name));
            return;
        }
        if (isDisabledWorld(p.getWorld())) {
            msg().send(p, "hideout.world-disabled");
            return;
        }
        h.setLocation(p.getLocation());
        plugin.storage().markDirty();
        msg().send(p, "hideout.moved", ph(h));
    }

    private boolean isDisabledWorld(World w) {
        List<String> list = plugin.getConfig().getStringList("hideout.teleport.disabled-worlds");
        for (String s : list) {
            if (s.equalsIgnoreCase(w.getName())) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ teleport

    public boolean isTeleporting(UUID uuid) {
        return sessions.containsKey(uuid);
    }

    public int countdownLeft(UUID uuid) {
        Session s = sessions.get(uuid);
        return s == null ? 0 : s.left;
    }

    public long cooldownLeft(UUID uuid) {
        Long until = cooldowns.get(uuid);
        if (until == null) {
            return 0;
        }
        long left = (until - System.currentTimeMillis() + 999) / 1000;
        return Math.max(0, left);
    }

    public void markCombat(UUID uuid) {
        combat.put(uuid, System.currentTimeMillis());
    }

    private boolean inCombat(UUID uuid) {
        Long t = combat.get(uuid);
        if (t == null) {
            return false;
        }
        long seconds = plugin.getConfig().getLong("hideout.teleport.combat-seconds", 15);
        return System.currentTimeMillis() - t < seconds * 1000L;
    }

    public void start(Player p, Guild g, Hideout h) {
        if (!plugin.getConfig().getBoolean("hideout.enabled", true)) {
            msg().send(p, "hideout.disabled");
            return;
        }
        if (!p.hasPermission("nsguildplus.hideout.use")) {
            msg().send(p, "general.no-permission");
            return;
        }
        Role role = g.roleOf(p.getUniqueId());
        Role min = Role.parse(plugin.getConfig().getString("hideout.who-can-teleport", "MEMBER"), Role.MEMBER);
        if (role == null || !role.atLeast(min) || !plugin.roles().can(role, "use-hideout")) {
            msg().send(p, "hideout.role-not-allowed");
            return;
        }
        if (sessions.containsKey(p.getUniqueId())) {
            msg().send(p, "hideout.already-teleporting");
            return;
        }
        String bypassPerm = plugin.getConfig().getString("hideout.teleport.bypass-permission", "nsguildplus.hideout.bypass");
        boolean bypass = bypassPerm != null && !bypassPerm.isEmpty() && p.hasPermission(bypassPerm);
        Location dest = h.toLocation();
        if (dest == null || isDisabledWorld(dest.getWorld())) {
            msg().send(p, "hideout.world-disabled", ph(h));
            return;
        }
        if (!tp("allow-cross-world", true) && !p.getWorld().equals(dest.getWorld()) && !bypass) {
            msg().send(p, "hideout.cross-world-disabled");
            return;
        }
        if (!bypass) {
            long cd = cooldownLeft(p.getUniqueId());
            if (cd > 0) {
                msg().send(p, "hideout.on-cooldown", Ph.of("cooldown", String.valueOf(cd)));
                return;
            }
            if (tp("cancel-on-combat", true) && inCombat(p.getUniqueId())) {
                msg().send(p, "hideout.teleport-cancelled-combat");
                return;
            }
            double cost = plugin.getConfig().getDouble("hideout.teleport.cost", 0);
            if (cost > 0 && plugin.money() != null && !plugin.money().has(p, cost)) {
                msg().send(p, "hideout.teleport-no-money", Ph.of("cost", plugin.formatMoney(cost)));
                return;
            }
        }
        p.closeInventory();
        int seconds = Math.max(0, plugin.getConfig().getInt("hideout.teleport.delay-seconds", 3));
        final Session s = new Session(g, h, p.getLocation().getBlock().getLocation(), seconds);
        sessions.put(p.getUniqueId(), s);
        final UUID id = p.getUniqueId();
        s.task = Bukkit.getScheduler().runTaskTimer(plugin, () -> tick(id), 0L, 20L);
    }

    private void tick(UUID id) {
        Session s = sessions.get(id);
        Player p = Bukkit.getPlayer(id);
        if (s == null) {
            return;
        }
        if (p == null || !p.isOnline()) {
            cancel(id, null);
            return;
        }
        if (s.left <= 0) {
            complete(p, s);
            return;
        }
        display(p, s);
        s.left--;
    }

    private void display(Player p, Session s) {
        String mode = plugin.getConfig().getString("hideout.effects.countdown-display", "TITLE").toUpperCase(Locale.ROOT);
        Map<String, String> ph = Ph.with(ph(s.hideout), "seconds", String.valueOf(s.left));
        switch (mode) {
            case "TITLE":
                p.showTitle(Title.title(msg().component("hideout.teleport-countdown-title", ph, p),
                        msg().component("hideout.teleport-countdown-subtitle", ph, p),
                        Title.Times.times(Duration.ZERO, Duration.ofMillis(1100), Duration.ZERO)));
                break;
            case "ACTIONBAR":
                p.sendActionBar(msg().component("hideout.teleport-countdown", ph, p));
                break;
            case "CHAT":
                msg().send(p, "hideout.teleport-countdown", ph);
                break;
            case "BOSSBAR":
                int total = Math.max(1, plugin.getConfig().getInt("hideout.teleport.delay-seconds", 3));
                float progress = Math.max(0f, Math.min(1f, (float) s.left / total));
                Component title = msg().component("hideout.teleport-countdown", ph, p);
                if (s.bar == null) {
                    s.bar = BossBar.bossBar(title, progress, BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS);
                    p.showBossBar(s.bar);
                } else {
                    s.bar.name(title);
                    s.bar.progress(progress);
                }
                break;
            default:
                break;
        }
        Util.sound(p, plugin.getConfig().getString("hideout.effects.countdown-sound", ""));
    }

    private void complete(Player p, Session s) {
        cancel(p.getUniqueId(), null);
        Location dest = s.hideout.toLocation();
        if (dest == null) {
            msg().send(p, "hideout.world-disabled", ph(s.hideout));
            return;
        }
        if (plugin.getConfig().getBoolean("hideout.teleport.safe-teleport", true)) {
            dest = findSafe(dest);
            if (dest == null) {
                msg().send(p, "hideout.unsafe-location", ph(s.hideout));
                return;
            }
        }
        String bypassPerm = plugin.getConfig().getString("hideout.teleport.bypass-permission", "nsguildplus.hideout.bypass");
        boolean bypass = bypassPerm != null && !bypassPerm.isEmpty() && p.hasPermission(bypassPerm);
        double cost = plugin.getConfig().getDouble("hideout.teleport.cost", 0);
        if (!bypass && cost > 0 && plugin.money() != null) {
            if (!plugin.money().withdraw(p, cost)) {
                msg().send(p, "hideout.teleport-no-money", Ph.of("cost", plugin.formatMoney(cost)));
                return;
            }
        }
        p.teleport(dest);
        long cd = plugin.getConfig().getLong("hideout.teleport.cooldown-seconds", 60);
        if (cd > 0 && !bypass) {
            cooldowns.put(p.getUniqueId(), System.currentTimeMillis() + cd * 1000L);
        }
        msg().send(p, "hideout.teleported", ph(s.hideout));
        Util.sound(p, plugin.getConfig().getString("hideout.effects.teleport-sound", ""));
        try {
            String particle = plugin.getConfig().getString("hideout.effects.teleport-particle", "");
            if (particle != null && !particle.isEmpty() && !particle.equalsIgnoreCase("NONE")) {
                Particle pt = Particle.valueOf(particle.toUpperCase(Locale.ROOT));
                dest.getWorld().spawnParticle(pt, dest.clone().add(0, 1, 0), 40, 0.4, 0.8, 0.4, 0.1);
            }
        } catch (Throwable ignored) {
            // particle not available on this version
        }
    }

    /** Cancels a running teleport. reasonKey may be null (silent). */
    public void cancel(UUID id, String reasonKey) {
        Session s = sessions.remove(id);
        if (s == null) {
            return;
        }
        if (s.task != null) {
            s.task.cancel();
        }
        Player p = Bukkit.getPlayer(id);
        if (p != null) {
            if (s.bar != null) {
                p.hideBossBar(s.bar);
            }
            if (reasonKey != null) {
                msg().send(p, reasonKey);
            }
        }
    }

    public void cancelAll() {
        for (UUID id : new java.util.ArrayList<>(sessions.keySet())) {
            cancel(id, null);
        }
    }

    public boolean shouldCancelOn(String what) {
        return tp(what, true);
    }

    public Location sessionStart(UUID id) {
        Session s = sessions.get(id);
        return s == null ? null : s.startBlock;
    }

    // ------------------------------------------------------------------ safe teleport

    private boolean badMaterial(Material m) {
        String n = m.name();
        return n.contains("LAVA") || n.contains("FIRE") || n.contains("CACTUS") || n.contains("MAGMA")
                || n.contains("CAMPFIRE") || n.contains("SWEET_BERRY") || n.contains("POWDER_SNOW") || n.equals("WITHER_ROSE");
    }

    private boolean safeAt(Location l) {
        World w = l.getWorld();
        if (w == null) {
            return false;
        }
        int y = l.getBlockY();
        if (y <= w.getMinHeight() || y >= w.getMaxHeight() - 1) {
            return false;
        }
        Block feet = l.getBlock();
        Block head = feet.getRelative(0, 1, 0);
        Block ground = feet.getRelative(0, -1, 0);
        if (!feet.isPassable() || !head.isPassable() || feet.isLiquid() || head.isLiquid()) {
            return false;
        }
        if (badMaterial(feet.getType()) || badMaterial(head.getType()) || badMaterial(ground.getType())) {
            return false;
        }
        return ground.getType().isSolid() || ground.isLiquid() && !badMaterial(ground.getType());
    }

    /** Checks the saved spot first, then looks for the nearest safe spot around it. */
    private Location findSafe(Location base) {
        if (safeAt(base)) {
            return base;
        }
        for (int r = 1; r <= 2; r++) {
            for (int dy = -3; dy <= 3; dy++) {
                for (int dx = -r; dx <= r; dx++) {
                    for (int dz = -r; dz <= r; dz++) {
                        Location c = base.clone().add(dx, dy, dz);
                        c.setX(c.getBlockX() + 0.5);
                        c.setZ(c.getBlockZ() + 0.5);
                        if (safeAt(c)) {
                            return c;
                        }
                    }
                }
            }
        }
        return null;
    }
}
