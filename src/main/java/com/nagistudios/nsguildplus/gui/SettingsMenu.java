package com.nagistudios.nsguildplus.gui;

import com.nagistudios.nsguildplus.NSGuildPlus;
import com.nagistudios.nsguildplus.guild.Guild;
import com.nagistudios.nsguildplus.util.Ph;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Map;

/** Guild Settings: name, colour and friendly fire. */
public final class SettingsMenu extends Menu {
    public SettingsMenu(NSGuildPlus plugin, Player player) {
        super(plugin, player);
    }

    @Override
    protected String file() {
        return "settings";
    }

    @Override
    protected int defaultSize() {
        return 9;
    }

    @Override
    protected Component title() {
        return plugin.messages().component("gui.settings.title", null);
    }

    @Override
    protected void build() {
        Guild g = myGuild();
        if (g == null) {
            return;
        }
        int free = plugin.getConfig().getInt("settings.name.free-changes", 1);
        String next = g.nameChangesUsed < free
                ? plugin.messages().raw("settings.next-change-free")
                : plugin.messages().plain("settings.next-change-paid",
                Ph.of("cost", String.valueOf((int) Math.round(plugin.getConfig().getDouble("settings.name.cost", 500)))));
        Map<String, String> ph = Ph.with(plugin.guilds().ph(g), "next_change", next);

        set(slot("back", 0), Items.lang(plugin, "gui.common.go-back", mat("back", Material.CREEPER_BANNER_PATTERN), Ph.of()),
                (p, t) -> new GuildMenu(plugin, p).open());
        set(slot("name", 3), Items.lang(plugin, "gui.settings.name", mat("name", Material.NAME_TAG), ph), (p, t) -> {
            Guild now = plugin.guilds().ofPlayer(p.getUniqueId());
            if (now == null || !plugin.roles().can(now.roleOf(p.getUniqueId()), "change-name")
                    || !Actions.canChangeSettings(plugin, now, p)) {
                plugin.messages().send(p, "settings.only-captain");
                return;
            }
            plugin.prompts().ask(p, "settings.name-prompt", plugin.guilds().namePh(""), "settings.name-cancelled",
                    text -> Actions.requestRename(plugin, p, text));
        });
        set(slot("colour", 4), Items.lang(plugin, "gui.settings.colour", mat("colour", Material.LIGHT_BLUE_DYE), ph),
                (p, t) -> {
                    Guild now = plugin.guilds().ofPlayer(p.getUniqueId());
                    if (now == null || !plugin.roles().can(now.roleOf(p.getUniqueId()), "change-colour")
                            || !Actions.canChangeSettings(plugin, now, p)) {
                        plugin.messages().send(p, "settings.only-captain");
                        return;
                    }
                    new ColourMenu(plugin, p).open();
                });
        boolean on = g.friendlyFire;
        Material ffMat = on
                ? plugin.gui().itemMat(file(), "friendly-fire", Material.RED_CONCRETE)
                : plugin.gui().mat(file(), "items.friendly-fire.material-off", Material.LIME_CONCRETE);
        set(slot("friendly-fire", 5), Items.of(ffMat, plugin.messages().component("gui.settings.friendly-fire.name", ph),
                plugin.messages().list(on ? "gui.settings.friendly-fire.lore-enabled" : "gui.settings.friendly-fire.lore-disabled", ph)),
                (p, t) -> {
                    toggleFriendlyFire(plugin, p);
                    refresh();
                });
    }

    public void refresh() {
        if (inventory != null) {
            render();
        }
    }

    /** Toggles friendly fire if the player is allowed to. Shared with /guild friendlyfire. */
    public static void toggleFriendlyFire(NSGuildPlus plugin, Player p) {
        Guild g = plugin.guilds().ofPlayer(p.getUniqueId());
        if (g == null) {
            plugin.messages().send(p, "general.not-in-guild");
            return;
        }
        com.nagistudios.nsguildplus.guild.Role min = com.nagistudios.nsguildplus.guild.Role.parse(
                plugin.getConfig().getString("settings.friendly-fire.who-can-toggle", "CAPTAIN"),
                com.nagistudios.nsguildplus.guild.Role.CAPTAIN);
        com.nagistudios.nsguildplus.guild.Role r = g.roleOf(p.getUniqueId());
        if (r == null || !r.atLeast(min) || !plugin.roles().can(r, "toggle-friendlyfire")) {
            plugin.messages().send(p, "settings.only-captain");
            return;
        }
        g.friendlyFire = !g.friendlyFire;
        plugin.storage().markDirty();
        plugin.messages().broadcastGuild(g, g.friendlyFire ? "settings.friendly-fire-on" : "settings.friendly-fire-off", null);
    }
}
