package com.nagistudios.nsguildplus.gui;

import com.nagistudios.nsguildplus.NSGuildPlus;
import com.nagistudios.nsguildplus.guild.Guild;
import com.nagistudios.nsguildplus.guild.GuildMember;
import com.nagistudios.nsguildplus.util.Ph;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** One head per member with the chosen stat. */
public final class PlayerStatsMenu extends PagedMenu {
    private final StatType type;

    public PlayerStatsMenu(NSGuildPlus plugin, Player player, StatType type) {
        super(plugin, player);
        this.type = type;
    }

    @Override
    protected String file() {
        return "player-stats";
    }

    @Override
    protected int defaultSize() {
        return 36;
    }

    @Override
    protected String titleKey() {
        return "gui.stats.player-stats.title";
    }

    @Override
    protected String listName() {
        return plugin.messages().raw("gui.stats.player-stats.list-name");
    }

    @Override
    protected String backLangPath() {
        return "gui.common.go-back-stats";
    }

    @Override
    protected void onBack(Player p) {
        new StatsMenu(plugin, p).open();
    }

    @Override
    protected List<Entry> entries() {
        List<Entry> list = new ArrayList<>();
        Guild g = myGuild();
        if (g == null) {
            return list;
        }
        List<GuildMember> members = new ArrayList<>(g.members.values());
        final java.util.Map<java.util.UUID, Double> values = new java.util.HashMap<>();
        for (GuildMember m : members) {
            values.put(m.uuid, type.value(plugin, m));
        }
        members.sort((a, b) -> Double.compare(values.get(b.uuid), values.get(a.uuid)));
        String label = plugin.messages().raw("gui.stat-labels." + type.labelKey());
        for (GuildMember m : members) {
            Map<String, String> ph = Ph.of("player", m.name, "stat_value", type.format(values.get(m.uuid)),
                    "stat_label", label);
            list.add(new Entry(Items.head(m.uuid, plugin.messages().component("gui.stats.player-stats.member.name", ph),
                    plugin.messages().list("gui.stats.player-stats.member.lore", ph)), null));
        }
        return list;
    }
}
