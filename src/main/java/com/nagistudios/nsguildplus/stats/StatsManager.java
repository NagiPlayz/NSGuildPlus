package com.nagistudios.nsguildplus.stats;

import com.nagistudios.nsguildplus.NSGuildPlus;
import com.nagistudios.nsguildplus.guild.Guild;
import com.nagistudios.nsguildplus.guild.GuildMember;
import com.nagistudios.nsguildplus.util.Ph;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Power score, personal ranks, guild level, Guild Stars and the annual leaderboard. */
public final class StatsManager {
    public static final class TopPlayer {
        public final Guild guild;
        public final GuildMember member;

        TopPlayer(Guild guild, GuildMember member) {
            this.guild = guild;
            this.member = member;
        }
    }

    private final NSGuildPlus plugin;

    public StatsManager(NSGuildPlus plugin) {
        this.plugin = plugin;
    }

    // ------------------------------------------------------------------ power and ranks

    public double power(GuildMember m) {
        ConfigurationSection f = plugin.getConfig().getConfigurationSection("guild.power-factors");
        double kills = f == null ? 10 : f.getDouble("kills", 10);
        double deaths = f == null ? -5 : f.getDouble("deaths", -5);
        double money = f == null ? 0.001 : f.getDouble("money", 0.001);
        double ach = f == null ? 25 : f.getDouble("achievements", 25);
        double v = m.kills * kills + m.deaths * deaths + m.balance * money + m.achievements * ach;
        return Math.max(0, Math.floor(v));
    }

    public int rankNumber(double power) {
        ConfigurationSection r = plugin.getConfig().getConfigurationSection("ranks");
        if (r == null) {
            return 5;
        }
        int best = 5;
        for (String key : r.getKeys(false)) {
            int n;
            try {
                n = Integer.parseInt(key);
            } catch (NumberFormatException e) {
                continue;
            }
            if (power >= r.getDouble(key + ".score", 0) && n < best) {
                best = n;
            }
        }
        return best;
    }

    public String rankName(int n) {
        return plugin.getConfig().getString("ranks." + n + ".name", "Rank " + n);
    }

    /** Recalculates the rank of one member and tells them if it changed. */
    public void refresh(Guild g, GuildMember m) {
        int old = m.rankNumber;
        int now = rankNumber(power(m));
        m.rankNumber = now;
        if (now != old) {
            Player p = Bukkit.getPlayer(m.uuid);
            if (p != null) {
                String key = now < old ? "rank.rank-up" : "rank.rank-down";
                plugin.messages().send(p, key, Ph.of("player", m.name, "rank_number", String.valueOf(now),
                        "personal_rank", rankName(now)));
            }
        }
    }

    public int guildScore(Guild g) {
        double total = 0;
        for (GuildMember m : g.members.values()) {
            total += power(m);
        }
        return (int) total;
    }

    public int level(Guild g) {
        int score = guildScore(g);
        int level = 1;
        ConfigurationSection t = plugin.getConfig().getConfigurationSection("guild.level-thresholds");
        if (t != null) {
            for (String key : t.getKeys(false)) {
                try {
                    int l = Integer.parseInt(key);
                    if (score >= t.getInt(key) && l > level) {
                        level = l;
                    }
                } catch (NumberFormatException ignored) {
                    // bad key
                }
            }
        }
        return level;
    }

    // ------------------------------------------------------------------ events

    public void addKill(Guild g, GuildMember m) {
        m.kills++;
        m.annualPoints += plugin.getConfig().getInt("guild.power-factors.kills", 10);
        changeStars(g, plugin.getConfig().getInt("guild.stars.kills", 5), "reason-kill");
        refresh(g, m);
    }

    public void addDeath(Guild g, GuildMember m) {
        m.deaths++;
        m.annualPoints += plugin.getConfig().getInt("guild.power-factors.deaths", -5);
        changeStars(g, plugin.getConfig().getInt("guild.stars.deaths", -2), "reason-death");
        refresh(g, m);
    }

    public void addAchievement(Guild g, GuildMember m) {
        m.achievements++;
        m.annualPoints += plugin.getConfig().getInt("guild.power-factors.achievements", 25);
        changeStars(g, plugin.getConfig().getInt("guild.stars.achievements", 10), "reason-achievement");
        refresh(g, m);
    }

    /** Stars never go below zero. */
    public void changeStars(Guild g, int delta, String reasonKey) {
        if (delta == 0) {
            return;
        }
        int before = g.stars;
        g.stars = Math.max(0, g.stars + delta);
        int real = g.stars - before;
        if (real == 0 || !plugin.getConfig().getBoolean("guild.stars.notify", true)) {
            return;
        }
        String reason = plugin.messages().raw("stars." + reasonKey);
        String key = real > 0 ? "stars.earned" : "stars.lost";
        plugin.messages().broadcastGuild(g, key, Ph.of("stars", String.valueOf(Math.abs(real)), "reason", reason));
    }

    // ------------------------------------------------------------------ lists

    public List<Guild> topGuilds() {
        List<Guild> list = new ArrayList<>(plugin.guilds().all());
        list.sort(Comparator.comparingInt(this::guildScore).reversed());
        return list;
    }

    public List<GuildMember> membersByPower(Guild g) {
        List<GuildMember> list = new ArrayList<>(g.members.values());
        list.sort(Comparator.comparingDouble(this::power).reversed());
        return list;
    }

    public List<TopPlayer> topPlayers() {
        List<TopPlayer> list = new ArrayList<>();
        for (Guild g : plugin.guilds().all()) {
            for (GuildMember m : g.members.values()) {
                list.add(new TopPlayer(g, m));
            }
        }
        list.sort((a, b) -> Double.compare(power(b.member), power(a.member)));
        return list;
    }

    public int annualPosition(java.util.UUID uuid) {
        List<TopPlayer> list = new ArrayList<>();
        for (Guild g : plugin.guilds().all()) {
            for (GuildMember m : g.members.values()) {
                list.add(new TopPlayer(g, m));
            }
        }
        list.sort((a, b) -> Integer.compare(b.member.annualPoints, a.member.annualPoints));
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).member.uuid.equals(uuid)) {
                return i + 1;
            }
        }
        return 0;
    }

    // ------------------------------------------------------------------ periodic work

    /** Runs every minute: playtime, xp level, balance, rank. */
    public void minuteTick() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            Guild g = plugin.guilds().ofPlayer(p.getUniqueId());
            if (g == null) {
                continue;
            }
            GuildMember m = g.member(p.getUniqueId());
            if (m == null) {
                continue;
            }
            m.playtimeMinutes++;
            m.xpLevel = p.getLevel();
            if (plugin.money() != null) {
                m.balance = plugin.money().balance(p);
            }
            refresh(g, m);
        }
        plugin.storage().markDirty();
    }

    /** Runs once per hour: ends the annual cycle when it is over. */
    public void checkAnnualCycle() {
        long days = plugin.getConfig().getLong("leaderboard.annual-cycle-days", 365);
        if (days <= 0) {
            return;
        }
        long now = System.currentTimeMillis() / 1000L;
        long start = plugin.storage().cycleStart();
        if (start <= 0) {
            plugin.storage().setCycleStart(now);
            return;
        }
        if (now - start < days * 86400L) {
            return;
        }
        TopPlayer best = null;
        for (Guild g : plugin.guilds().all()) {
            for (GuildMember m : g.members.values()) {
                if (m.annualPoints > 0 && (best == null || m.annualPoints > best.member.annualPoints)) {
                    best = new TopPlayer(g, m);
                }
            }
        }
        if (best != null) {
            best.member.champion = true;
            Map<String, String> ph = Ph.of("player", best.member.name);
            for (Player p : Bukkit.getOnlinePlayers()) {
                plugin.messages().send(p, "rank.champion-announce", ph);
            }
        }
        for (Guild g : plugin.guilds().all()) {
            for (GuildMember m : g.members.values()) {
                m.annualPoints = 0;
            }
        }
        plugin.storage().setCycleStart(now);
        plugin.storage().markDirty();
    }
}
