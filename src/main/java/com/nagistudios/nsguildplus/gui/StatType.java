package com.nagistudios.nsguildplus.gui;

import com.nagistudios.nsguildplus.NSGuildPlus;
import com.nagistudios.nsguildplus.guild.GuildMember;
import com.nagistudios.nsguildplus.util.Util;
import org.bukkit.Bukkit;

import java.util.Locale;

public enum StatType {
    LEVEL("level", "level"),
    WEALTH("wealth", "wealth"),
    PLAYTIME("playtime", "playtime"),
    KILLS("kills", "kills"),
    DEATHS("deaths", "deaths"),
    MOBS("mobs", "mobs"),
    PLACED("placed", "blocks-placed"),
    MINED("mined", "blocks-mined");

    private final String labelKey;
    private final String langKey;

    StatType(String labelKey, String langKey) {
        this.labelKey = labelKey;
        this.langKey = langKey;
    }

    /** Key below gui.stat-labels. */
    public String labelKey() {
        return labelKey;
    }

    /** Key below gui.stats. */
    public String langKey() {
        return langKey;
    }

    public double value(NSGuildPlus plugin, GuildMember m) {
        switch (this) {
            case LEVEL:
                return m.xpLevel;
            case WEALTH:
                if (plugin.money() != null) {
                    return plugin.money().balance(Bukkit.getOfflinePlayer(m.uuid));
                }
                return m.balance;
            case PLAYTIME:
                return m.playtimeMinutes / 60.0;
            case KILLS:
                return m.kills;
            case DEATHS:
                return m.deaths;
            case MOBS:
                return m.mobsKilled;
            case PLACED:
                return m.blocksPlaced;
            default:
                return m.blocksMined;
        }
    }

    public String format(double v) {
        if (this == PLAYTIME) {
            return String.format(Locale.ROOT, "%.1f", v);
        }
        return Util.number(v, "COMMAS");
    }
}
