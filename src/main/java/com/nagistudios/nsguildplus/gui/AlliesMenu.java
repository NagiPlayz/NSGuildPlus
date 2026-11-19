package com.nagistudios.nsguildplus.gui;

import com.nagistudios.nsguildplus.NSGuildPlus;
import com.nagistudios.nsguildplus.guild.Guild;
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

public final class AlliesMenu extends PagedMenu {
    public AlliesMenu(NSGuildPlus plugin, Player player) {
        super(plugin, player);
    }

    @Override
    protected String file() {
        return "allies";
    }

    @Override
    protected int defaultSize() {
        return 36;
    }

    @Override
    protected String titleKey() {
        return "gui.allies.title";
    }

    @Override
    protected String listName() {
        return plugin.messages().raw("gui.allies.list-name");
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
        boolean captain = g.roleOf(player.getUniqueId()) == Role.CAPTAIN;
        for (UUID id : g.allies) {
            Guild ally = plugin.guilds().get(id);
            if (ally == null) {
                continue;
            }
            Map<String, String> ph = Ph.of("guild", ally.name);
            List<String> raw = new ArrayList<>(plugin.messages().rawList("gui.allies.ally.lore"));
            if (!captain && !raw.isEmpty()) {
                raw.remove(raw.size() - 1);
            }
            Component name = plugin.messages().component("gui.allies.ally.name", ph);
            list.add(new Entry(Items.of(mat("ally", Material.WHITE_BANNER), name, plugin.messages().parseList(raw, ph)),
                    (p, t) -> {
                        if (t == ClickType.RIGHT) {
                            Guild mine = plugin.guilds().ofPlayer(p.getUniqueId());
                            if (mine == null || mine.roleOf(p.getUniqueId()) != Role.CAPTAIN) {
                                plugin.messages().send(p, "alliance.only-captain");
                                return;
                            }
                            Actions.confirmRevoke(plugin, p, ally, () -> new AlliesMenu(plugin, p).open());
                        }
                    }));
        }
        int free = Math.max(0, plugin.getConfig().getInt("allies.max", 5) - g.allies.size());
        for (int i = 0; i < free; i++) {
            list.add(new Entry(Items.lang(plugin, "gui.allies.form-alliance",
                    mat("form", Material.LIME_STAINED_GLASS_PANE), Ph.of()), (p, t) -> formPrompt(p)));
        }
        return list;
    }

    private void formPrompt(Player p) {
        Guild g = plugin.guilds().ofPlayer(p.getUniqueId());
        if (g == null || g.roleOf(p.getUniqueId()) != Role.CAPTAIN) {
            plugin.messages().send(p, "alliance.only-captain");
            return;
        }
        plugin.prompts().ask(p, "prompt.ally", null, "prompt.cancelled", text -> {
            plugin.guilds().requestAlliance(p, text);
            new AlliesMenu(plugin, p).open();
        });
    }

    @Override
    protected void extras() {
        Guild g = myGuild();
        if (g == null) {
            return;
        }
        set(slot("own", 4), Items.lang(plugin, "gui.allies.own-guild", mat("own", Material.NETHER_STAR),
                Ph.of("guild", g.name)), null);
    }
}
