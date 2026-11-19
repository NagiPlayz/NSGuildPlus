package com.nagistudios.nsguildplus.commands;

import com.nagistudios.nsguildplus.NSGuildPlus;
import com.nagistudios.nsguildplus.gui.Actions;
import com.nagistudios.nsguildplus.gui.ColourMenu;
import com.nagistudios.nsguildplus.gui.GuildMenu;
import com.nagistudios.nsguildplus.gui.HideoutsMenu;
import com.nagistudios.nsguildplus.gui.LeaderboardMenu;
import com.nagistudios.nsguildplus.gui.MailboxMenu;
import com.nagistudios.nsguildplus.gui.SettingsMenu;
import com.nagistudios.nsguildplus.gui.ShopMenu;
import com.nagistudios.nsguildplus.gui.StatsMenu;
import com.nagistudios.nsguildplus.guild.Guild;
import com.nagistudios.nsguildplus.guild.GuildManager;
import com.nagistudios.nsguildplus.guild.Hideout;
import com.nagistudios.nsguildplus.guild.Role;
import com.nagistudios.nsguildplus.message.MessageManager;
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
import java.util.UUID;

/** /guild and all its sub-commands. Players only see the commands their role allows. */
public final class GuildCommand implements CommandExecutor, TabCompleter {
    private static final List<String> ALL = Arrays.asList("help", "create", "accept", "leave", "disband", "invite", "kick",
            "promote", "demote", "setvc", "transfer", "info", "stats", "leaderboard", "settings", "rename", "colour",
            "friendlyfire", "ally", "unally", "allychat", "mailbox", "enderchest", "shop", "stars", "hideout",
            "sethideout", "delhideout", "renamehideout", "movehideout");
    private static final List<String> GUILDLESS = Arrays.asList("help", "create", "accept");

    private final NSGuildPlus plugin;

    public GuildCommand(NSGuildPlus plugin) {
        this.plugin = plugin;
    }

    private MessageManager msg() {
        return plugin.messages();
    }

    private List<String> visible(Player p) {
        Guild g = plugin.guilds().ofPlayer(p.getUniqueId());
        List<String> out = new ArrayList<>();
        for (String s : ALL) {
            if (allowed(g, p, s)) {
                out.add(s);
            }
        }
        return out;
    }

    private boolean allowed(Guild g, Player p, String sub) {
        if (g == null) {
            return GUILDLESS.contains(sub);
        }
        if (sub.equals("help")) {
            return true;
        }
        return plugin.roles().canCommand(g.roleOf(p.getUniqueId()), sub);
    }

    private void usage(Player p, String sub) {
        msg().send(p, "general.usage", Ph.of("usage", msg().raw("help." + sub + ".usage")));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            msg().send(sender, "general.player-only");
            return true;
        }
        Player p = (Player) sender;
        Guild g = plugin.guilds().ofPlayer(p.getUniqueId());
        if (args.length == 0) {
            if (g != null) {
                new GuildMenu(plugin, p).open();
            } else if (!plugin.guilds().invitesFor(p.getUniqueId()).isEmpty()) {
                new MailboxMenu(plugin, p).open();
            } else {
                msg().send(p, "guild.no-help-guild");
            }
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("color")) {
            sub = "colour";
        }
        if (!ALL.contains(sub)) {
            msg().send(p, "general.unknown-command");
            return true;
        }
        if (sub.equals("create") && g != null) {
            msg().send(p, "general.already-in-guild");
            return true;
        }
        if (sub.equals("disband") && g != null && g.roleOf(p.getUniqueId()) != Role.CAPTAIN) {
            msg().send(p, "guild.no-disband-permission");
            return true;
        }
        if (!allowed(g, p, sub)) {
            msg().send(p, g == null ? "general.not-in-guild" : "general.no-permission");
            return true;
        }
        String[] rest = Arrays.copyOfRange(args, 1, args.length);
        run(p, g, sub, rest);
        return true;
    }

    private void run(Player p, Guild g, String sub, String[] a) {
        switch (sub) {
            case "help":
                msg().send(p, "general.help-header");
                for (String s : visible(p)) {
                    msg().send(p, "general.help-line", Ph.of("usage", msg().raw("help." + s + ".usage"),
                            "description", msg().raw("help." + s + ".description")));
                }
                break;
            case "create":
                if (a.length < 1) {
                    usage(p, sub);
                } else {
                    plugin.guilds().create(p, a[0]);
                }
                break;
            case "accept":
                accept(p, a);
                break;
            case "leave":
                Actions.confirmLeave(plugin, p);
                break;
            case "disband":
                Actions.confirmDisband(plugin, p);
                break;
            case "invite":
                if (a.length < 1) {
                    usage(p, sub);
                } else {
                    plugin.guilds().invite(p, a[0]);
                }
                break;
            case "kick":
            case "promote":
            case "demote":
            case "setvc":
            case "transfer":
                memberCommand(p, g, sub, a);
                break;
            case "info":
                msg().sendList(p, "guild.info", plugin.guilds().ph(g));
                break;
            case "stats":
                new StatsMenu(plugin, p).open();
                break;
            case "leaderboard":
                new LeaderboardMenu(plugin, p).open();
                break;
            case "settings":
                new SettingsMenu(plugin, p).open();
                break;
            case "rename":
                if (a.length < 1) {
                    usage(p, sub);
                } else {
                    Actions.requestRename(plugin, p, a[0]);
                }
                break;
            case "colour":
                if (!plugin.roles().can(g.roleOf(p.getUniqueId()), "change-colour") || !Actions.canChangeSettings(plugin, g, p)) {
                    msg().send(p, "settings.only-captain");
                } else {
                    new ColourMenu(plugin, p).open();
                }
                break;
            case "friendlyfire":
                SettingsMenu.toggleFriendlyFire(plugin, p);
                break;
            case "ally":
                if (a.length < 1) {
                    usage(p, sub);
                } else {
                    plugin.guilds().requestAlliance(p, a[0]);
                }
                break;
            case "unally":
                unally(p, g, a);
                break;
            case "allychat":
                if (a.length < 1) {
                    msg().send(p, "alliance.chat-empty");
                } else {
                    plugin.guilds().allyChat(p, String.join(" ", a));
                }
                break;
            case "mailbox":
                new MailboxMenu(plugin, p).open();
                break;
            case "enderchest":
                Actions.openEnderchest(plugin, p);
                break;
            case "shop":
                new ShopMenu(plugin, p).open();
                break;
            case "stars":
                msg().send(p, "stars.balance", Ph.of("stars", String.valueOf(g.stars)));
                break;
            default:
                hideoutCommand(p, g, sub, a);
                break;
        }
    }

    private void accept(Player p, String[] a) {
        List<GuildManager.Invite> invites = plugin.guilds().invitesFor(p.getUniqueId());
        if (invites.isEmpty()) {
            msg().send(p, "guild.invite-none");
            return;
        }
        GuildManager.Invite chosen = null;
        if (a.length >= 1) {
            for (GuildManager.Invite i : invites) {
                Guild ig = plugin.guilds().get(i.guildId);
                if (ig != null && ig.name.equalsIgnoreCase(a[0])) {
                    chosen = i;
                }
            }
            if (chosen == null) {
                msg().send(p, "guild.invite-not-found");
                return;
            }
        } else {
            chosen = invites.get(invites.size() - 1);
        }
        plugin.guilds().accept(p, chosen);
    }

    private void memberCommand(Player p, Guild g, String sub, String[] a) {
        if (a.length < 1) {
            usage(p, sub);
            return;
        }
        UUID target = Actions.uuidOfName(g, a[0]);
        if (target == null) {
            msg().send(p, "guild.not-a-member", Ph.of("target", a[0]));
            return;
        }
        switch (sub) {
            case "kick":
                if (target.equals(p.getUniqueId())) {
                    msg().send(p, "guild.cannot-target-self");
                    return;
                }
                Actions.confirmKick(plugin, p, target, null);
                break;
            case "promote":
                plugin.guilds().promote(p, target);
                break;
            case "demote":
                plugin.guilds().demote(p, target);
                break;
            case "setvc":
                plugin.guilds().setVice(p, target);
                break;
            default:
                if (target.equals(p.getUniqueId())) {
                    msg().send(p, "guild.cannot-target-self");
                    return;
                }
                Actions.confirmTransfer(plugin, p, target);
                break;
        }
    }

    private void unally(Player p, Guild g, String[] a) {
        if (a.length < 1) {
            usage(p, "unally");
            return;
        }
        Guild other = plugin.guilds().byName(a[0]);
        if (other == null) {
            msg().send(p, "alliance.guild-not-found", Ph.of("guild", a[0]));
            return;
        }
        if (!g.allies.contains(other.id)) {
            msg().send(p, "alliance.not-allied", Ph.of("guild", other.name));
            return;
        }
        Actions.confirmRevoke(plugin, p, other, null);
    }

    private void hideoutCommand(Player p, Guild g, String sub, String[] a) {
        switch (sub) {
            case "hideout":
                if (a.length == 0) {
                    new HideoutsMenu(plugin, p).open();
                } else {
                    Hideout h = plugin.hideouts().find(g, a[0]);
                    if (h == null) {
                        msg().send(p, g.hideouts.isEmpty() ? "hideout.no-hideouts" : "hideout.not-found",
                                Ph.of("hideout_name", a[0]));
                    } else {
                        plugin.hideouts().start(p, g, h);
                    }
                }
                break;
            case "sethideout":
                if (a.length < 1) {
                    usage(p, sub);
                } else {
                    plugin.hideouts().set(p, a[0]);
                }
                break;
            case "delhideout":
                if (a.length < 1) {
                    usage(p, sub);
                } else if (plugin.hideouts().find(g, a[0]) == null) {
                    msg().send(p, "hideout.not-found", Ph.of("hideout_name", a[0]));
                } else {
                    Actions.confirmDeleteHideout(plugin, p, a[0], null);
                }
                break;
            case "renamehideout":
                if (a.length < 2) {
                    usage(p, sub);
                } else {
                    plugin.hideouts().rename(p, a[0], a[1]);
                }
                break;
            default:
                if (a.length < 1) {
                    usage(p, sub);
                } else {
                    plugin.hideouts().move(p, a[0]);
                }
                break;
        }
    }

    // ------------------------------------------------------------------ tab completion

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) {
            return new ArrayList<>();
        }
        Player p = (Player) sender;
        Guild g = plugin.guilds().ofPlayer(p.getUniqueId());
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            for (String s : visible(p)) {
                if (s.startsWith(args[0].toLowerCase(Locale.ROOT))) {
                    out.add(s);
                }
            }
            return out;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (!allowed(g, p, sub)) {
            return out;
        }
        String prefix = args[args.length - 1].toLowerCase(Locale.ROOT);
        List<String> pool = new ArrayList<>();
        if (args.length == 2) {
            switch (sub) {
                case "invite":
                    for (Player o : Bukkit.getOnlinePlayers()) {
                        if (plugin.guilds().ofPlayer(o.getUniqueId()) == null) {
                            pool.add(o.getName());
                        }
                    }
                    break;
                case "kick":
                case "promote":
                case "demote":
                case "setvc":
                case "transfer":
                    if (g != null) {
                        g.members.values().forEach(m -> pool.add(m.name));
                    }
                    break;
                case "ally":
                    plugin.guilds().all().forEach(o -> pool.add(o.name));
                    break;
                case "unally":
                    if (g != null) {
                        for (UUID id : g.allies) {
                            Guild o = plugin.guilds().get(id);
                            if (o != null) {
                                pool.add(o.name);
                            }
                        }
                    }
                    break;
                case "accept":
                    for (GuildManager.Invite i : plugin.guilds().invitesFor(p.getUniqueId())) {
                        Guild o = plugin.guilds().get(i.guildId);
                        if (o != null) {
                            pool.add(o.name);
                        }
                    }
                    break;
                case "hideout":
                case "delhideout":
                case "renamehideout":
                case "movehideout":
                    if (g != null) {
                        g.hideouts.values().forEach(h -> pool.add(h.name));
                    }
                    break;
                default:
                    break;
            }
        }
        for (String s : pool) {
            if (s != null && s.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                out.add(s);
            }
        }
        return out;
    }
}
