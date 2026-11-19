package com.nagistudios.nsguildplus.gui;

import com.nagistudios.nsguildplus.NSGuildPlus;
import com.nagistudios.nsguildplus.guild.Guild;
import com.nagistudios.nsguildplus.shop.ShopManager;
import com.nagistudios.nsguildplus.util.Ph;
import com.nagistudios.nsguildplus.util.Util;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Guild Shop: buy tag styles and colours with Guild Stars. */
public final class ShopMenu extends PagedMenu {
    public ShopMenu(NSGuildPlus plugin, Player player) {
        super(plugin, player);
    }

    @Override
    protected String file() {
        return "shop";
    }

    @Override
    protected int defaultSize() {
        return 45;
    }

    @Override
    protected String titleKey() {
        return "gui.shop-menu.title";
    }

    @Override
    protected String listName() {
        return plugin.messages().raw("gui.shop-menu.list-name");
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
        boolean captain = plugin.roles().can(g.roleOf(player.getUniqueId()), "buy-style");
        for (ShopManager.Style s : plugin.shop().enabledStyles()) {
            boolean owned = g.ownedStyles.contains(s.id);
            boolean equipped = s.id.equals(g.tagStyle);
            Map<String, String> ph = Ph.of("style_name", s.name, "style_category", Util.pretty(s.category),
                    "cost", String.valueOf(s.price));
            List<Component> lore = new ArrayList<>(plugin.messages().list("gui.shop-menu.item-lore", ph));
            String stateKey = equipped ? "gui.shop-menu.equipped"
                    : owned ? (captain ? "gui.shop-menu.owned" : "gui.shop-menu.owned-locked")
                    : "gui.shop-menu.for-sale";
            lore.addAll(plugin.messages().list(stateKey, ph));
            Component name = plugin.shop().styledName(s, s.name).decoration(TextDecoration.BOLD, true);
            ItemStack item = Items.of(s.material, name, lore);
            if (equipped) {
                Items.glow(item);
            }
            final String id = s.id;
            list.add(new Entry(item, (p, t) -> click(p, id)));
        }
        return list;
    }

    private void click(Player p, String id) {
        Guild g = plugin.guilds().ofPlayer(p.getUniqueId());
        ShopManager.Style s = plugin.shop().get(id);
        if (g == null || s == null) {
            return;
        }
        if (id.equals(g.tagStyle)) {
            return;
        }
        if (g.ownedStyles.contains(id)) {
            plugin.shop().equip(p, g, s);
        } else {
            plugin.shop().buy(p, g, s);
        }
        new ShopMenu(plugin, p).open();
    }
}
