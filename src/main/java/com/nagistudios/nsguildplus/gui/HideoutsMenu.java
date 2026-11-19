package com.nagistudios.nsguildplus.gui;

import com.nagistudios.nsguildplus.NSGuildPlus;
import com.nagistudios.nsguildplus.guild.Guild;
import com.nagistudios.nsguildplus.guild.Hideout;
import com.nagistudios.nsguildplus.guild.Role;
import com.nagistudios.nsguildplus.util.Util;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class HideoutsMenu extends PagedMenu {
    public HideoutsMenu(NSGuildPlus plugin, Player player) {
        super(plugin, player);
    }

    @Override
    protected String file() {
        return "hideouts";
    }

    @Override
    protected int defaultSize() {
        return plugin.getConfig().getInt("hideout.gui.size", 36);
    }

    @Override
    protected String titleKey() {
        return "gui.hideouts.title";
    }

    @Override
    protected String listName() {
        return plugin.messages().raw("gui.hideouts.list-name");
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
        boolean captain = plugin.roles().can(g.roleOf(player.getUniqueId()), "delete-hideout");
        Material mat = Util.material(plugin.getConfig().getString("hideout.gui.item-material", "RED_BED"), Material.RED_BED);
        for (Hideout h : g.hideouts.values()) {
            Map<String, String> ph = plugin.hideouts().ph(h);
            List<String> raw = new ArrayList<>(plugin.messages().rawList("gui.hideouts.hideout.lore"));
            if (!captain && !raw.isEmpty()) {
                raw.remove(raw.size() - 1);
            }
            List<Component> lore = plugin.messages().parseList(raw, ph);
            Component name = plugin.messages().component("gui.hideouts.hideout.name", ph);
            final String hideoutName = h.name;
            list.add(new Entry(Items.of(mat, name, lore), (p, t) -> click(p, hideoutName, t)));
        }
        return list;
    }

    private void click(Player p, String name, ClickType t) {
        Guild g = plugin.guilds().ofPlayer(p.getUniqueId());
        if (g == null) {
            return;
        }
        Hideout h = plugin.hideouts().find(g, name);
        if (h == null) {
            return;
        }
        if (t == ClickType.SHIFT_LEFT || t == ClickType.SHIFT_RIGHT) {
            if (!plugin.roles().can(g.roleOf(p.getUniqueId()), "delete-hideout")) {
                plugin.messages().send(p, "hideout.only-captain");
                return;
            }
            Actions.confirmDeleteHideout(plugin, p, name, () -> new HideoutsMenu(plugin, p).open());
        } else if (t == ClickType.LEFT) {
            plugin.hideouts().start(p, g, h);
        }
    }

    @Override
    protected void extras() {
        Guild g = myGuild();
        if (g == null || g.roleOf(player.getUniqueId()) != Role.CAPTAIN) {
            return;
        }
        Material mat = Util.material(plugin.getConfig().getString("hideout.gui.set-button-material", "NETHER_STAR"),
                Material.NETHER_STAR);
        set(slot("set", 31), Items.lang(plugin, "gui.hideouts.set-hideout", mat, Map.of()), (p, t) ->
                plugin.prompts().ask(p, "prompt.hideout", null, "prompt.cancelled", text -> {
                    plugin.hideouts().set(p, text);
                    new HideoutsMenu(plugin, p).open();
                }));
    }
}
