package com.nagistudios.nsguildplus.gui;

import com.nagistudios.nsguildplus.NSGuildPlus;
import com.nagistudios.nsguildplus.guild.Guild;
import com.nagistudios.nsguildplus.util.Ph;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/** Guild Statistics menu: every button opens the Player Statistics list of that stat. */
public final class StatsMenu extends Menu {
    public StatsMenu(NSGuildPlus plugin, Player player) {
        super(plugin, player);
    }

    @Override
    protected String file() {
        return "stats";
    }

    @Override
    protected int defaultSize() {
        return 27;
    }

    @Override
    protected Component title() {
        return plugin.messages().component("gui.stats.title", null);
    }

    private void button(StatType type, String key, int defSlot, Material defMat) {
        set(slot(key, defSlot), Items.lang(plugin, "gui.stats." + type.langKey(), mat(key, defMat), Ph.of()),
                (p, t) -> new PlayerStatsMenu(plugin, p, type).open());
    }

    @Override
    protected void build() {
        Guild g = myGuild();
        if (g == null) {
            return;
        }
        button(StatType.LEVEL, "level", 3, Material.LECTERN);
        button(StatType.WEALTH, "wealth", 4, Material.GOLD_INGOT);
        button(StatType.PLAYTIME, "playtime", 5, Material.CLOCK);
        button(StatType.KILLS, "kills", 11, Material.DIAMOND_SWORD);
        button(StatType.DEATHS, "deaths", 12, Material.DIAMOND_HELMET);
        button(StatType.MOBS, "mobs", 13, Material.IRON_SWORD);
        button(StatType.PLACED, "placed", 14, Material.COARSE_DIRT);
        button(StatType.MINED, "mined", 15, Material.DIAMOND_PICKAXE);
        set(slot("back", 22), Items.lang(plugin, "gui.common.go-back", mat("back", Material.CREEPER_BANNER_PATTERN), Ph.of()),
                (p, t) -> new GuildMenu(plugin, p).open());
    }
}
