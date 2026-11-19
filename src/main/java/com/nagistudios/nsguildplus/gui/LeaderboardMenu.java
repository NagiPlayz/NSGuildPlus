package com.nagistudios.nsguildplus.gui;

import com.nagistudios.nsguildplus.NSGuildPlus;
import com.nagistudios.nsguildplus.guild.Guild;
import com.nagistudios.nsguildplus.guild.GuildMember;
import com.nagistudios.nsguildplus.util.Ph;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Guilds ranked by total score. Click a guild to see its members. */
public final class LeaderboardMenu extends PagedMenu {
    public LeaderboardMenu(NSGuildPlus plugin, Player player) {
        super(plugin, player);
    }

    @Override
    protected String file() {
        return "leaderboard";
    }

    @Override
    protected int defaultSize() {
        return 36;
    }

    @Override
    protected String titleKey() {
        return "gui.leaderboard.title";
    }

    @Override
    protected String listName() {
        return plugin.messages().raw("gui.leaderboard.list-name");
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
        int rank = 1;
        for (Guild g : plugin.stats().topGuilds()) {
            Map<String, String> ph = Ph.with(plugin.guilds().ph(g), "rank_number", String.valueOf(rank));
            rank++;
            final Guild target = g;
            list.add(new Entry(Items.lang(plugin, "gui.leaderboard.guild", mat("guild", Material.WHITE_BANNER), ph),
                    (p, t) -> new Board(plugin, p, target).open()));
        }
        return list;
    }

    /** Members of one guild sorted by power score. */
    static final class Board extends PagedMenu {
        private final Guild guild;

        Board(NSGuildPlus plugin, Player player, Guild guild) {
            super(plugin, player);
            this.guild = guild;
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
            return "gui.leaderboard.board-title";
        }

        @Override
        protected String listName() {
            return plugin.messages().raw("gui.stats.player-stats.list-name");
        }

        @Override
        protected void onBack(Player p) {
            new LeaderboardMenu(plugin, p).open();
        }

        @Override
        protected List<Entry> entries() {
            List<Entry> list = new ArrayList<>();
            for (GuildMember m : plugin.stats().membersByPower(guild)) {
                int rank = plugin.stats().rankNumber(plugin.stats().power(m));
                Map<String, String> ph = Ph.of("player", m.name, "rank_number", String.valueOf(rank),
                        "personal_rank", plugin.stats().rankName(rank),
                        "power_score", String.valueOf((long) plugin.stats().power(m)));
                list.add(new Entry(Items.head(m.uuid, plugin.messages().component("gui.leaderboard.board-member.name", ph),
                        plugin.messages().list("gui.leaderboard.board-member.lore", ph)), null));
            }
            return list;
        }
    }
}
