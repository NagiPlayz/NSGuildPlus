package com.nagistudios.nsguildplus.placeholder;

import com.nagistudios.nsguildplus.NSGuildPlus;
import com.nagistudios.nsguildplus.guild.Guild;
import com.nagistudios.nsguildplus.guild.GuildMember;
import com.nagistudios.nsguildplus.guild.Hideout;
import com.nagistudios.nsguildplus.stats.StatsManager;
import com.nagistudios.nsguildplus.util.Util;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** %nsguildplus_...% placeholders. */
public final class NSGuildPlusExpansion extends PlaceholderExpansion {
    private static final Pattern INDEXED = Pattern.compile("^(.*)_(\\d+)$");

    private final NSGuildPlus plugin;

    public NSGuildPlusExpansion(NSGuildPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "nsguildplus";
    }

    @Override
    public String getAuthor() {
        return "NagiStudios";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    private String noGuild() {
        return plugin.getConfig().getString("placeholders.no-guild", "None");
    }

    private String empty() {
        return plugin.getConfig().getString("placeholders.empty", "-");
    }

    private String num(double v) {
        return Util.number(v, plugin.getConfig().getString("placeholders.number-format", "COMMAS"));
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        String p = params.toLowerCase(Locale.ROOT);
        Guild g = player == null ? null : plugin.guilds().ofPlayer(player.getUniqueId());
        GuildMember m = g == null ? null : g.member(player.getUniqueId());

        switch (p) {
            case "guild_name":
            case "guild_tag":
                return g == null ? noGuild() : g.name;
            case "guild_level":
                return g == null ? empty() : String.valueOf(plugin.stats().level(g));
            case "guild_members":
                return g == null ? empty() : String.valueOf(g.members.size());
            case "guild_max_members":
                return String.valueOf(plugin.guilds().maxMembers());
            case "captain":
                return g == null ? noGuild() : g.memberName(g.captain);
            case "vice_captain":
                return g == null || g.vice == null ? empty() : g.memberName(g.vice);
            case "role":
                return g == null ? noGuild() : plugin.roleName(g.roleOf(player.getUniqueId()));
            case "personal_rank":
                return m == null ? empty() : plugin.stats().rankName(plugin.stats().rankNumber(plugin.stats().power(m)));
            case "rank_number":
                return m == null ? empty() : String.valueOf(plugin.stats().rankNumber(plugin.stats().power(m)));
            case "power_score":
                return m == null ? "0" : num(plugin.stats().power(m));
            case "kills":
                return m == null ? "0" : String.valueOf(m.kills);
            case "deaths":
                return m == null ? "0" : String.valueOf(m.deaths);
            case "achievements":
                return m == null ? "0" : String.valueOf(m.achievements);
            case "annual_position":
                return m == null ? empty() : String.valueOf(plugin.stats().annualPosition(m.uuid));
            case "stars":
            case "guild_stars":
                return g == null ? "0" : num(g.stars);
            case "tag_style":
                return g == null ? empty() : g.tagStyle;
            case "tag_formatted":
                return g == null ? empty() : plugin.shop().tagFormatted(g);
            case "colour":
                return g == null ? empty() : plugin.shop().styleName(g.tagStyle);
            case "friendly_fire":
                return g == null ? empty() : (g.friendlyFire ? "ON" : "OFF");
            case "name_changes":
                return g == null ? "0" : String.valueOf(g.nameChangesUsed);
            case "name_change_free":
                return g == null ? empty() : (g.nameChangesUsed < plugin.getConfig().getInt("settings.name.free-changes", 1) ? "yes" : "no");
            case "name_change_cost":
                if (g == null) {
                    return empty();
                }
                return g.nameChangesUsed < plugin.getConfig().getInt("settings.name.free-changes", 1)
                        ? "0" : num(plugin.getConfig().getDouble("settings.name.cost", 500));
            case "ally_count":
                return g == null ? "0" : String.valueOf(g.allies.size());
            case "hideout_count":
                return g == null ? "0" : String.valueOf(g.hideouts.size());
            case "hideout_max":
                return g == null ? "0" : String.valueOf(plugin.hideouts().limit(g));
            case "hideout_cooldown":
                return player == null ? "0" : String.valueOf(plugin.hideouts().cooldownLeft(player.getUniqueId()));
            case "hideout_teleporting":
                return player != null && plugin.hideouts().isTeleporting(player.getUniqueId()) ? "yes" : "no";
            case "hideout_countdown":
                return player == null ? "0" : String.valueOf(plugin.hideouts().countdownLeft(player.getUniqueId()));
            default:
                break;
        }

        Matcher mt = INDEXED.matcher(p);
        if (!mt.matches()) {
            return null;
        }
        String base = mt.group(1);
        int n = Integer.parseInt(mt.group(2));
        if (n < 1) {
            return empty();
        }

        if (base.equals("ally_name")) {
            if (g == null) {
                return empty();
            }
            List<Guild> allies = new ArrayList<>();
            for (java.util.UUID id : g.allies) {
                Guild a = plugin.guilds().get(id);
                if (a != null) {
                    allies.add(a);
                }
            }
            return n <= allies.size() ? allies.get(n - 1).name : empty();
        }
        if (base.startsWith("hideout_")) {
            if (g == null) {
                return empty();
            }
            List<Hideout> list = new ArrayList<>(g.hideouts.values());
            if (n > list.size()) {
                return empty();
            }
            Hideout h = list.get(n - 1);
            switch (base) {
                case "hideout_name":
                    return h.name;
                case "hideout_world":
                    return h.world;
                case "hideout_x":
                    return String.valueOf((int) h.x);
                case "hideout_y":
                    return String.valueOf((int) h.y);
                case "hideout_z":
                    return String.valueOf((int) h.z);
                default:
                    return null;
            }
        }
        if (base.startsWith("top_guild_")) {
            List<Guild> top = plugin.stats().topGuilds();
            if (n > top.size()) {
                return empty();
            }
            Guild t = top.get(n - 1);
            switch (base) {
                case "top_guild_name":
                case "top_guild_tag":
                    return t.name;
                case "top_guild_score":
                    return num(plugin.stats().guildScore(t));
                case "top_guild_members":
                    return String.valueOf(t.members.size());
                default:
                    return null;
            }
        }
        if (base.startsWith("top_player_")) {
            List<StatsManager.TopPlayer> top = plugin.stats().topPlayers();
            if (n > top.size()) {
                return empty();
            }
            return memberValue(base.substring("top_player_".length()), top.get(n - 1).member);
        }
        if (base.startsWith("guild_member_top_")) {
            if (g == null) {
                return empty();
            }
            List<GuildMember> list = plugin.stats().membersByPower(g);
            if (n > list.size()) {
                return empty();
            }
            return memberValue(base.substring("guild_member_top_".length()), list.get(n - 1));
        }
        return null;
    }

    private String memberValue(String what, GuildMember m) {
        switch (what) {
            case "name":
                return m.name;
            case "score":
                return num(plugin.stats().power(m));
            case "rank":
                return plugin.stats().rankName(plugin.stats().rankNumber(plugin.stats().power(m)));
            default:
                return null;
        }
    }
}
