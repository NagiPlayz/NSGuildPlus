package com.nagistudios.nsguildplus.gui;

import com.nagistudios.nsguildplus.NSGuildPlus;
import com.nagistudios.nsguildplus.guild.Guild;
import com.nagistudios.nsguildplus.guild.Role;
import com.nagistudios.nsguildplus.message.MessageManager;
import com.nagistudios.nsguildplus.util.Ph;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

/** Flows shared by menus and commands (they all use the Confirm menu). */
public final class Actions {
    private Actions() {
    }

    private static MessageManager msg(NSGuildPlus plugin) {
        return plugin.messages();
    }

    private static String actionText(NSGuildPlus plugin, String key, Map<String, String> ph) {
        return msg(plugin).plain("gui.confirm-actions." + key, ph);
    }

    public static void confirmLeave(NSGuildPlus plugin, Player p) {
        Guild g = plugin.guilds().ofPlayer(p.getUniqueId());
        if (g == null) {
            msg(plugin).send(p, "general.not-in-guild");
            return;
        }
        new ConfirmMenu(plugin, p, actionText(plugin, "leave", null), null,
                () -> plugin.guilds().leave(p), () -> new GuildMenu(plugin, p).open()).open();
    }

    public static void confirmDisband(NSGuildPlus plugin, Player p) {
        Guild g = plugin.guilds().ofPlayer(p.getUniqueId());
        if (g == null) {
            msg(plugin).send(p, "general.not-in-guild");
            return;
        }
        if (g.roleOf(p.getUniqueId()) != Role.CAPTAIN) {
            msg(plugin).send(p, "guild.no-disband-permission");
            return;
        }
        new ConfirmMenu(plugin, p, actionText(plugin, "disband", null), null, () -> {
            Guild now = plugin.guilds().ofPlayer(p.getUniqueId());
            if (now != null && now.roleOf(p.getUniqueId()) == Role.CAPTAIN) {
                plugin.guilds().disband(now, "guild.disbanded");
            }
        }, () -> new GuildMenu(plugin, p).open()).open();
    }

    public static void confirmKick(NSGuildPlus plugin, Player p, UUID target, Runnable back) {
        Guild g = plugin.guilds().ofPlayer(p.getUniqueId());
        if (g == null) {
            return;
        }
        String name = g.memberName(target);
        new ConfirmMenu(plugin, p, actionText(plugin, "kick", Ph.of("target", name)), null,
                () -> {
                    plugin.guilds().kick(p, target);
                    if (back != null) {
                        back.run();
                    }
                }, back).open();
    }

    public static void confirmTransfer(NSGuildPlus plugin, Player p, UUID target) {
        new ConfirmMenu(plugin, p, actionText(plugin, "transfer", null), null,
                () -> plugin.guilds().transfer(p, target), null).open();
    }

    public static void confirmDeleteHideout(NSGuildPlus plugin, Player p, String name, Runnable back) {
        Guild g = plugin.guilds().ofPlayer(p.getUniqueId());
        if (g == null) {
            return;
        }
        new ConfirmMenu(plugin, p, actionText(plugin, "delete-hideout", null), null, () -> {
            Guild now = plugin.guilds().ofPlayer(p.getUniqueId());
            plugin.hideouts().delete(p, now, name);
            if (back != null) {
                back.run();
            }
        }, back).open();
    }

    public static void confirmRevoke(NSGuildPlus plugin, Player p, Guild other, Runnable back) {
        new ConfirmMenu(plugin, p, actionText(plugin, "revoke", null), null, () -> {
            plugin.guilds().revokeAlliance(p, other);
            if (back != null) {
                back.run();
            }
        }, back).open();
    }

    public static void openEnderchest(NSGuildPlus plugin, Player p) {
        Guild g = plugin.guilds().ofPlayer(p.getUniqueId());
        if (g == null) {
            msg(plugin).send(p, "general.not-in-guild");
            return;
        }
        if (!plugin.getConfig().getBoolean("enderchest.enabled", true)
                || !plugin.roles().can(g.roleOf(p.getUniqueId()), "use-enderchest")
                || !p.hasPermission("nsguildplus.enderchest")) {
            msg(plugin).send(p, "enderchest.no-access");
            return;
        }
        p.openInventory(plugin.guilds().enderInventory(g));
        msg(plugin).send(p, "enderchest.opened");
    }

    /** True if the player's role may change guild settings (settings.who-can-change). */
    public static boolean canChangeSettings(NSGuildPlus plugin, Guild g, Player p) {
        Role r = g.roleOf(p.getUniqueId());
        if (r == null) {
            return false;
        }
        Role min = Role.parse(plugin.getConfig().getString("settings.who-can-change", "CAPTAIN"), Role.CAPTAIN);
        return r.atLeast(min);
    }

    /** Guild rename: first change free, later changes cost Guild Stars. */
    public static void requestRename(NSGuildPlus plugin, Player p, String name) {
        MessageManager m = msg(plugin);
        Guild g = plugin.guilds().ofPlayer(p.getUniqueId());
        if (g == null) {
            m.send(p, "general.not-in-guild");
            return;
        }
        if (!plugin.roles().can(g.roleOf(p.getUniqueId()), "change-name") || !canChangeSettings(plugin, g, p)) {
            m.send(p, "settings.only-captain");
            return;
        }
        String err = plugin.guilds().validateName(name, g);
        if (err != null) {
            m.send(p, err, plugin.guilds().namePh(name));
            return;
        }
        int free = plugin.getConfig().getInt("settings.name.free-changes", 1);
        int cost = g.nameChangesUsed < free ? 0 : (int) Math.round(plugin.getConfig().getDouble("settings.name.cost", 500));
        boolean pays = cost > 0 && !p.hasPermission("nsguildplus.name.bypass-cost");
        if (!pays) {
            applyRename(plugin, p, g, name, 0);
            return;
        }
        if (g.stars < cost) {
            m.send(p, "settings.name-no-balance", Ph.of("cost", String.valueOf(cost), "stars", String.valueOf(g.stars)));
            return;
        }
        final int price = cost;
        new ConfirmMenu(plugin, p, actionText(plugin, "rename", null), price + " \u2B50", () -> {
            Guild now = plugin.guilds().ofPlayer(p.getUniqueId());
            if (now == null || now.roleOf(p.getUniqueId()) == null) {
                return;
            }
            String err2 = plugin.guilds().validateName(name, now);
            if (err2 != null) {
                m.send(p, err2, plugin.guilds().namePh(name));
                return;
            }
            if (now.stars < price) {
                m.send(p, "settings.name-no-balance", Ph.of("cost", String.valueOf(price), "stars", String.valueOf(now.stars)));
                return;
            }
            now.stars -= price;
            applyRename(plugin, p, now, name, price);
        }, () -> new SettingsMenu(plugin, p).open()).open();
    }

    private static void applyRename(NSGuildPlus plugin, Player p, Guild g, String name, int paid) {
        String old = g.name;
        plugin.guilds().applyRename(g, name);
        Map<String, String> ph = Ph.of("new_name", name, "old_name", old, "player", p.getName(),
                "cost", String.valueOf(paid));
        msg(plugin).send(p, paid > 0 ? "settings.name-changed-paid" : "settings.name-changed-free", ph);
        for (Player other : g.onlinePlayers()) {
            if (!other.equals(p)) {
                msg(plugin).send(other, "settings.name-changed-broadcast", ph);
            }
        }
    }

    public static UUID uuidOfName(Guild g, String name) {
        for (var m : g.members.values()) {
            if (m.name != null && m.name.equalsIgnoreCase(name)) {
                return m.uuid;
            }
        }
        Player online = Bukkit.getPlayerExact(name);
        return online != null && g.isMember(online.getUniqueId()) ? online.getUniqueId() : null;
    }
}
