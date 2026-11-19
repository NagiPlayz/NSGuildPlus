package com.nagistudios.nsguildplus.commands;

import com.nagistudios.nsguildplus.NSGuildPlus;
import com.nagistudios.nsguildplus.guild.Guild;
import com.nagistudios.nsguildplus.guild.Hideout;
import com.nagistudios.nsguildplus.util.Ph;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** /gadmin reload | stars give|take | hideout | disband */
public final class AdminCommand implements CommandExecutor, TabCompleter {
    private final NSGuildPlus plugin;

    public AdminCommand(NSGuildPlus plugin) {
        this.plugin = plugin;
    }

    private void usage(CommandSender s, String usage) {
        plugin.messages().send(s, "general.usage", Ph.of("usage", usage));
    }

    /** Finds a guild by guild name, or by the name of one of its members. */
    private Guild findGuild(String name) {
        Guild g = plugin.guilds().byName(name);
        if (g != null) {
            return g;
        }
        Player p = Bukkit.getPlayerExact(name);
        if (p != null) {
            return plugin.guilds().ofPlayer(p.getUniqueId());
        }
        for (Guild other : plugin.guilds().all()) {
            for (var m : other.members.values()) {
                if (m.name != null && m.name.equalsIgnoreCase(name)) {
                    return other;
                }
            }
        }
        return null;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("nsguildplus.admin")) {
            plugin.messages().send(sender, "general.no-permission");
            return true;
        }
        if (args.length == 0) {
            usage(sender, "/gadmin <reload|stars|hideout|disband>");
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload":
                plugin.reloadAll();
                plugin.messages().send(sender, "general.reloaded");
                break;
            case "stars":
                stars(sender, args);
                break;
            case "hideout":
                hideout(sender, args);
                break;
            case "disband":
                if (args.length < 2) {
                    usage(sender, "/gadmin disband <guild>");
                    break;
                }
                Guild g = findGuild(args[1]);
                if (g == null) {
                    plugin.messages().send(sender, "general.guild-not-found", Ph.of("guild", args[1]));
                    break;
                }
                plugin.guilds().disband(g, "guild.disbanded");
                break;
            default:
                usage(sender, "/gadmin <reload|stars|hideout|disband>");
                break;
        }
        return true;
    }

    private void stars(CommandSender sender, String[] a) {
        if (a.length < 4 || !(a[1].equalsIgnoreCase("give") || a[1].equalsIgnoreCase("take"))) {
            usage(sender, "/gadmin stars <give|take> <player|guild> <amount>");
            return;
        }
        Guild g = findGuild(a[2]);
        if (g == null) {
            plugin.messages().send(sender, "general.guild-not-found", Ph.of("guild", a[2]));
            return;
        }
        int amount;
        try {
            amount = Integer.parseInt(a[3]);
        } catch (NumberFormatException e) {
            plugin.messages().send(sender, "general.invalid-number");
            return;
        }
        if (amount < 0) {
            plugin.messages().send(sender, "general.invalid-number");
            return;
        }
        g.stars = a[1].equalsIgnoreCase("give") ? g.stars + amount : Math.max(0, g.stars - amount);
        plugin.storage().markDirty();
        plugin.messages().send(sender, "general.admin-stars", Ph.of("guild", g.name, "stars", String.valueOf(g.stars)));
    }

    private void hideout(CommandSender sender, String[] a) {
        if (!sender.hasPermission("nsguildplus.admin.hideout")) {
            plugin.messages().send(sender, "general.no-permission");
            return;
        }
        if (a.length < 4 || !(a[2].equalsIgnoreCase("delete") || a[2].equalsIgnoreCase("tp"))) {
            usage(sender, "/gadmin hideout <guild> <delete|tp> <name>");
            return;
        }
        Guild g = findGuild(a[1]);
        if (g == null) {
            plugin.messages().send(sender, "general.guild-not-found", Ph.of("guild", a[1]));
            return;
        }
        Hideout h = plugin.hideouts().find(g, a[3]);
        if (h == null) {
            plugin.messages().send(sender, "hideout.not-found", Ph.of("hideout_name", a[3]));
            return;
        }
        Map<String, String> ph = Ph.with(plugin.hideouts().ph(h), "guild", g.name);
        if (a[2].equalsIgnoreCase("delete")) {
            g.hideouts.remove(a[3].toLowerCase(Locale.ROOT));
            plugin.storage().markDirty();
            plugin.messages().send(sender, "hideout.deleted", ph);
            return;
        }
        if (!(sender instanceof Player)) {
            plugin.messages().send(sender, "general.player-only");
            return;
        }
        if (h.toLocation() == null) {
            plugin.messages().send(sender, "hideout.world-disabled", ph);
            return;
        }
        ((Player) sender).teleport(h.toLocation());
        plugin.messages().send(sender, "general.admin-hideout-tp", ph);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (!sender.hasPermission("nsguildplus.admin")) {
            return out;
        }
        List<String> pool = new ArrayList<>();
        if (args.length == 1) {
            pool.addAll(Arrays.asList("reload", "stars", "hideout", "disband"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("stars")) {
            pool.addAll(Arrays.asList("give", "take"));
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("hideout") || args[0].equalsIgnoreCase("disband"))
                || args.length == 3 && args[0].equalsIgnoreCase("stars")) {
            plugin.guilds().all().forEach(g -> pool.add(g.name));
        } else if (args.length == 3 && args[0].equalsIgnoreCase("hideout")) {
            pool.addAll(Arrays.asList("delete", "tp"));
        } else if (args.length == 4 && args[0].equalsIgnoreCase("hideout")) {
            Guild g = findGuild(args[1]);
            if (g != null) {
                g.hideouts.values().forEach(h -> pool.add(h.name));
            }
        }
        String prefix = args[args.length - 1].toLowerCase(Locale.ROOT);
        for (String s : pool) {
            if (s.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                out.add(s);
            }
        }
        return out;
    }
}
