package com.nagistudios.nsguildplus.gui;

import com.nagistudios.nsguildplus.NSGuildPlus;
import com.nagistudios.nsguildplus.guild.Guild;
import com.nagistudios.nsguildplus.shop.ShopManager;
import com.nagistudios.nsguildplus.util.Ph;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Shows only the colours (tag styles) the guild owns. */
public final class ColourMenu extends PagedMenu {
    public ColourMenu(NSGuildPlus plugin, Player player) {
        super(plugin, player);
    }

    @Override
    protected String file() {
        return "colour";
    }

    @Override
    protected int defaultSize() {
        return 45;
    }

    @Override
    protected int perPage() {
        int cfg = plugin.getConfig().getInt("settings.colour.slots-per-page", 27);
        return Math.max(1, Math.min(cfg, super.perPage()));
    }

    @Override
    protected String titleKey() {
        return "gui.settings.colour-menu.title";
    }

    @Override
    protected String listName() {
        return plugin.messages().raw("gui.settings.colour-menu.list-name");
    }

    @Override
    protected String backLangPath() {
        return "gui.common.go-back-settings";
    }

    @Override
    protected void onBack(Player p) {
        new SettingsMenu(plugin, p).open();
    }

    @Override
    protected List<Entry> entries() {
        List<Entry> list = new ArrayList<>();
        Guild g = myGuild();
        if (g == null) {
            return list;
        }
        for (ShopManager.Style s : plugin.shop().enabledStyles()) {
            if (!g.ownedStyles.contains(s.id)) {
                continue;
            }
            boolean equipped = s.id.equals(g.tagStyle);
            Map<String, String> ph = Ph.of("colour_name", s.name, "style_category", com.nagistudios.nsguildplus.util.Util.pretty(s.category));
            List<Component> lore = new ArrayList<>(plugin.messages().list("gui.settings.colour-menu.colour-item-lore", ph));
            if (equipped) {
                lore.add(Component.empty());
                lore.add(plugin.messages().component("gui.settings.colour-menu.equipped-line", ph));
            }
            Component name = plugin.shop().styledName(s, s.name).decoration(TextDecoration.BOLD, true);
            ItemStack item = Items.of(s.material, name, lore);
            if (equipped) {
                Items.glow(item);
            }
            final String id = s.id;
            list.add(new Entry(item, (p, t) -> select(p, id)));
        }
        return list;
    }

    private void select(Player p, String id) {
        Guild g = plugin.guilds().ofPlayer(p.getUniqueId());
        if (g == null) {
            return;
        }
        if (!plugin.roles().can(g.roleOf(p.getUniqueId()), "change-colour") || !Actions.canChangeSettings(plugin, g, p)) {
            plugin.messages().send(p, "settings.only-captain");
            return;
        }
        if (!g.ownedStyles.contains(id)) {
            return;
        }
        g.tagStyle = id;
        plugin.storage().markDirty();
        plugin.messages().send(p, "settings.colour-selected", Ph.of("colour_name", plugin.shop().styleName(id)));
        new ColourMenu(plugin, p).open();
    }
}
