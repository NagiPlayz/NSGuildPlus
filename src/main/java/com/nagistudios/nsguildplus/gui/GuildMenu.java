package com.nagistudios.nsguildplus.gui;

import com.nagistudios.nsguildplus.NSGuildPlus;
import com.nagistudios.nsguildplus.guild.Guild;
import com.nagistudios.nsguildplus.guild.Role;
import com.nagistudios.nsguildplus.util.Ph;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.Map;

/** Main menu, opened by /guild. */
public final class GuildMenu extends Menu {
    public GuildMenu(NSGuildPlus plugin, Player player) {
        super(plugin, player);
    }

    @Override
    protected String file() {
        return "guild-menu";
    }

    @Override
    protected int defaultSize() {
        return 27;
    }

    @Override
    protected Component title() {
        return plugin.messages().component("gui.menu.title", null);
    }

    @Override
    protected void build() {
        Guild g = myGuild();
        if (g == null) {
            return;
        }
        Role role = g.roleOf(player.getUniqueId());
        Map<String, String> ph = Ph.with(plugin.guilds().ph(g), "player", player.getName(), "role", plugin.roleName(role));

        set(slot("members", 2), Items.lang(plugin, "gui.menu.members", mat("members", Material.LIME_BANNER), ph),
                (p, t) -> new MembersMenu(plugin, p).open());
        set(slot("hideout", 4), Items.lang(plugin, "gui.menu.hideout", mat("hideout", Material.RED_BED), ph),
                (p, t) -> new HideoutsMenu(plugin, p).open());
        set(slot("enderchest", 6), Items.lang(plugin, "gui.menu.enderchest", mat("enderchest", Material.ENDER_CHEST), ph),
                (p, t) -> Actions.openEnderchest(plugin, p));
        set(slot("shop", 11), Items.lang(plugin, "gui.menu.shop", mat("shop", Material.GOLD_INGOT), ph),
                (p, t) -> new ShopMenu(plugin, p).open());
        set(slot("stats", 13), Items.lang(plugin, "gui.menu.stats", mat("stats", Material.CLOCK), ph),
                (p, t) -> new StatsMenu(plugin, p).open());
        set(slot("allies", 15), Items.lang(plugin, "gui.menu.allies", mat("allies", Material.IRON_SWORD), ph),
                (p, t) -> new AlliesMenu(plugin, p).open());
        set(slot("invites", 20), Items.lang(plugin, "gui.menu.invites", mat("invites", Material.CHEST), ph), (p, t) -> {
            Guild now = plugin.guilds().ofPlayer(p.getUniqueId());
            if (now == null || !plugin.roles().can(now.roleOf(p.getUniqueId()), "view-mailbox")) {
                plugin.messages().send(p, "general.no-permission");
                return;
            }
            new MailboxMenu(plugin, p).open();
        });
        set(slot("settings", 22), Items.lang(plugin, "gui.menu.settings", mat("settings", Material.CRAFTING_TABLE), ph),
                (p, t) -> new SettingsMenu(plugin, p).open());
        boolean captain = role == Role.CAPTAIN;
        set(slot("leave", 24), Items.lang(plugin, captain ? "gui.menu.leave" : "gui.menu.leave-member",
                mat("leave", Material.OAK_DOOR), ph), (p, t) -> {
            if (captain && (t == ClickType.SHIFT_LEFT || t == ClickType.SHIFT_RIGHT)) {
                Actions.confirmDisband(plugin, p);
            } else {
                Actions.confirmLeave(plugin, p);
            }
        });
    }
}
