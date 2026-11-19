package com.nagistudios.nsguildplus.gui;

import com.nagistudios.nsguildplus.NSGuildPlus;
import com.nagistudios.nsguildplus.util.Ph;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Menu with pages. Previous and Next buttons are hidden when they do not apply. */
public abstract class PagedMenu extends Menu {
    public static final class Entry {
        public final ItemStack item;
        public final Click click;

        public Entry(ItemStack item, Click click) {
            this.item = item;
            this.click = click;
        }
    }

    protected int page = 1;
    protected int maxPage = 1;
    private List<Entry> cache = new ArrayList<>();

    protected PagedMenu(NSGuildPlus plugin, Player player) {
        super(plugin, player);
    }

    protected abstract List<Entry> entries();

    /** lang.yml path of the title, for example gui.members.title. */
    protected abstract String titleKey();

    /** Text for {list_name} in the page buttons. */
    protected abstract String listName();

    protected abstract void onBack(Player p);

    protected String backLangPath() {
        return "gui.common.go-back";
    }

    protected void extras() {
    }

    protected int perPage() {
        return Math.max(1, plugin.gui().i(file(), "per-page", 18));
    }

    protected int start() {
        return plugin.gui().i(file(), "content-start", 9);
    }

    private void reload() {
        cache = entries();
        int per = perPage();
        maxPage = Math.max(1, (cache.size() + per - 1) / per);
        if (page > maxPage) {
            page = maxPage;
        }
        if (page < 1) {
            page = 1;
        }
    }

    @Override
    public void open() {
        reload();
        super.open();
    }

    @Override
    protected Component title() {
        return plugin.messages().component(titleKey(), Ph.of("page", String.valueOf(page), "max_page", String.valueOf(maxPage)));
    }

    @Override
    protected void build() {
        int per = perPage();
        int from = (page - 1) * per;
        for (int i = 0; i < per && from + i < cache.size(); i++) {
            Entry e = cache.get(from + i);
            set(start() + i, e.item, e.click);
        }
        if (page > 1) {
            set(slot("prev", 27), Items.lang(plugin, "gui.common.previous-page", mat("prev", Material.ARROW),
                    Ph.of("list_name", listName())), (p, t) -> {
                page--;
                sound("page");
                open();
            });
        }
        if (page < maxPage) {
            set(slot("next", 35), Items.lang(plugin, "gui.common.next-page", mat("next", Material.ARROW),
                    Ph.of("list_name", listName())), (p, t) -> {
                page++;
                sound("page");
                open();
            });
        }
        set(slot("back", 31), Items.lang(plugin, backLangPath(), mat("back", Material.CREEPER_BANNER_PATTERN), Ph.of()),
                (p, t) -> onBack(p));
        extras();
    }
}
