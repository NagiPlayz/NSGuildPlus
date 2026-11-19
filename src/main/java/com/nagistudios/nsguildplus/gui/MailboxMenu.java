package com.nagistudios.nsguildplus.gui;

import com.nagistudios.nsguildplus.NSGuildPlus;
import com.nagistudios.nsguildplus.guild.Guild;
import com.nagistudios.nsguildplus.guild.GuildManager;
import com.nagistudios.nsguildplus.util.Ph;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Guild invites (for players without a guild) and alliance requests (for the guild). */
public final class MailboxMenu extends PagedMenu {
    public MailboxMenu(NSGuildPlus plugin, Player player) {
        super(plugin, player);
    }

    @Override
    protected String file() {
        return "mailbox";
    }

    @Override
    protected int defaultSize() {
        return 36;
    }

    @Override
    protected String titleKey() {
        return "gui.mailbox.title";
    }

    @Override
    protected String listName() {
        return plugin.messages().raw("gui.mailbox.list-name");
    }

    @Override
    protected void onBack(Player p) {
        if (plugin.guilds().ofPlayer(p.getUniqueId()) != null) {
            new GuildMenu(plugin, p).open();
        } else {
            p.closeInventory();
        }
    }

    @Override
    protected List<Entry> entries() {
        List<Entry> list = new ArrayList<>();
        Guild mine = myGuild();
        if (mine == null) {
            for (GuildManager.Invite inv : plugin.guilds().invitesFor(player.getUniqueId())) {
                Guild g = plugin.guilds().get(inv.guildId);
                if (g == null) {
                    continue;
                }
                Map<String, String> ph = Ph.of("guild", g.name, "sender", inv.senderName);
                list.add(new Entry(Items.lang(plugin, "gui.mailbox.invite", mat("invite", Material.PAPER), ph), (p, t) -> {
                    if (t == ClickType.LEFT) {
                        plugin.guilds().accept(p, inv);
                        if (plugin.guilds().ofPlayer(p.getUniqueId()) != null) {
                            new GuildMenu(plugin, p).open();
                            return;
                        }
                        new MailboxMenu(plugin, p).open();
                    } else if (t == ClickType.RIGHT) {
                        plugin.guilds().decline(p, inv);
                        new MailboxMenu(plugin, p).open();
                    }
                }));
            }
        } else {
            for (GuildManager.AllyRequest req : plugin.guilds().requestsFor(mine.id)) {
                Guild from = plugin.guilds().get(req.fromGuild);
                if (from == null) {
                    continue;
                }
                Map<String, String> ph = Ph.of("guild", from.name, "sender", req.senderName);
                list.add(new Entry(Items.lang(plugin, "gui.mailbox.alliance-request", mat("alliance", Material.WHITE_BANNER), ph),
                        (p, t) -> {
                            if (t == ClickType.LEFT) {
                                plugin.guilds().acceptAlliance(p, req);
                                new MailboxMenu(plugin, p).open();
                            } else if (t == ClickType.RIGHT) {
                                plugin.guilds().declineAlliance(p, req);
                                new MailboxMenu(plugin, p).open();
                            }
                        }));
            }
        }
        return list;
    }
}
