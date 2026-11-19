package com.nagistudios.nsguildplus.gui;

import com.nagistudios.nsguildplus.NSGuildPlus;
import com.nagistudios.nsguildplus.guild.Guild;
import com.nagistudios.nsguildplus.guild.GuildMember;
import com.nagistudios.nsguildplus.guild.Role;
import com.nagistudios.nsguildplus.util.Ph;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class MembersMenu extends PagedMenu {
    public MembersMenu(NSGuildPlus plugin, Player player) {
        super(plugin, player);
    }

    @Override
    protected String file() {
        return "members";
    }

    @Override
    protected int defaultSize() {
        return 36;
    }

    @Override
    protected String titleKey() {
        return "gui.members.title";
    }

    @Override
    protected String listName() {
        return plugin.messages().raw("gui.members.prev-list-name");
    }

    @Override
    protected void onBack(Player p) {
        new GuildMenu(plugin, p).open();
    }

    @Override
    protected List<Entry> entries() {
        List<Entry> list = new ArrayList<>();
        Guild g = myGuild();
        if (g == null) {
            return list;
        }
        Role viewer = g.roleOf(player.getUniqueId());
        for (GuildMember m : g.members.values()) {
            if (m.uuid.equals(g.captain)) {
                continue;
            }
            list.add(new Entry(memberItem(g, viewer, m), (p, t) -> memberClick(p, m.uuid, t)));
        }
        int free = Math.max(0, plugin.guilds().maxMembers() - g.members.size());
        for (int i = 0; i < free; i++) {
            list.add(new Entry(Items.lang(plugin, "gui.members.invite-recruit",
                    mat("invite", Material.LIME_STAINED_GLASS_PANE), Ph.of()), (p, t) -> invitePrompt(p)));
        }
        return list;
    }

    private org.bukkit.inventory.ItemStack memberItem(Guild g, Role viewer, GuildMember m) {
        Role target = g.roleOf(m.uuid);
        Role next = plugin.guilds().nextRole(target);
        Role prev = plugin.guilds().previousRole(target);
        Map<String, String> ph = Ph.of("player", m.name, "role", plugin.roleName(target),
                "next_role", next == null ? "-" : plugin.roleName(next),
                "previous_role", prev == null ? "-" : plugin.roleName(prev),
                "champion_code", m.champion ? plugin.messages().raw("gui.members.champion-tag") : "");
        List<Component> lore = new ArrayList<>(plugin.messages().list("gui.members.member.lore", ph));
        boolean promote = viewer == Role.CAPTAIN && next != null;
        boolean demote = viewer == Role.CAPTAIN && prev != null;
        boolean kick = viewer == Role.CAPTAIN
                || (viewer == Role.VICE_CAPTAIN && (target == Role.OFFICER || target == Role.MEMBER));
        if (promote || demote || kick) {
            lore.add(Component.empty());
            lore.add(plugin.messages().component("gui.members.member.controls-header", ph));
            if (promote) {
                lore.add(plugin.messages().component("gui.members.member.controls-promote", ph));
            }
            if (demote) {
                lore.add(plugin.messages().component("gui.members.member.controls-demote", ph));
            }
            if (kick) {
                if (promote || demote) {
                    lore.add(Component.empty());
                }
                lore.add(plugin.messages().component("gui.members.member.controls-kick", ph));
            }
        }
        return Items.head(m.uuid, plugin.messages().component("gui.members.member.name", ph), lore);
    }

    private void memberClick(Player p, UUID target, ClickType t) {
        Guild g = plugin.guilds().ofPlayer(p.getUniqueId());
        if (g == null) {
            return;
        }
        if (t == ClickType.LEFT) {
            plugin.guilds().promote(p, target);
            open();
        } else if (t == ClickType.RIGHT) {
            plugin.guilds().demote(p, target);
            open();
        } else if (t == ClickType.SHIFT_LEFT || t == ClickType.SHIFT_RIGHT) {
            Role r = g.roleOf(p.getUniqueId());
            if (!plugin.roles().can(r, "kick")) {
                plugin.messages().send(p, "general.no-permission");
                return;
            }
            Actions.confirmKick(plugin, p, target, () -> new MembersMenu(plugin, p).open());
        }
    }

    private void invitePrompt(Player p) {
        Guild g = plugin.guilds().ofPlayer(p.getUniqueId());
        if (g == null || !plugin.roles().can(g.roleOf(p.getUniqueId()), "invite")) {
            plugin.messages().send(p, "general.no-permission");
            return;
        }
        plugin.prompts().ask(p, "prompt.invite", null, "prompt.cancelled", text -> {
            plugin.guilds().invite(p, text);
            new MembersMenu(plugin, p).open();
        });
    }

    @Override
    protected void extras() {
        Guild g = myGuild();
        if (g == null) {
            return;
        }
        Map<String, String> ph = Ph.of("player", g.memberName(g.captain));
        set(slot("captain", 4), Items.head(g.captain, plugin.messages().component("gui.members.captain.name", ph),
                plugin.messages().list("gui.members.captain.lore", ph)), null);
    }
}
