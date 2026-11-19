package com.nagistudios.nsguildplus.gui;

import com.nagistudios.nsguildplus.NSGuildPlus;
import com.nagistudios.nsguildplus.guild.Guild;
import com.nagistudios.nsguildplus.guild.Role;
import com.nagistudios.nsguildplus.util.Util;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

/** Base class of every menu. All clicks are cancelled, each slot can have a click action. */
public abstract class Menu implements InventoryHolder {
    public interface Click {
        void on(Player p, ClickType type);
    }

    protected final NSGuildPlus plugin;
    protected final Player player;
    protected Inventory inventory;
    private final Map<Integer, Click> actions = new HashMap<>();

    protected Menu(NSGuildPlus plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    /** Name of the file in guis/ without .yml. */
    protected abstract String file();

    protected abstract int defaultSize();

    protected abstract Component title();

    protected abstract void build();

    protected int size() {
        int s = plugin.gui().i(file(), "size", defaultSize());
        if (s < 9 || s > 54 || s % 9 != 0) {
            return defaultSize();
        }
        return s;
    }

    public void open() {
        inventory = Bukkit.createInventory(this, size(), title());
        render();
        player.openInventory(inventory);
    }

    protected void render() {
        actions.clear();
        inventory.clear();
        String filler = plugin.gui().s(file(), "filler", "GRAY_STAINED_GLASS_PANE");
        if (filler != null && !filler.equalsIgnoreCase("AIR") && !filler.equalsIgnoreCase("NONE")) {
            ItemStack pane = Items.blank(Util.material(filler, Material.GRAY_STAINED_GLASS_PANE));
            for (int i = 0; i < inventory.getSize(); i++) {
                inventory.setItem(i, pane);
            }
        }
        build();
    }

    protected void set(int slot, ItemStack item, Click click) {
        if (inventory == null || slot < 0 || slot >= inventory.getSize()) {
            return;
        }
        inventory.setItem(slot, item);
        if (click != null) {
            actions.put(slot, click);
        } else {
            actions.remove(slot);
        }
    }

    protected int slot(String item, int def) {
        return plugin.gui().slot(file(), item, def);
    }

    protected Material mat(String item, Material def) {
        return plugin.gui().itemMat(file(), item, def);
    }

    protected Guild myGuild() {
        return plugin.guilds().ofPlayer(player.getUniqueId());
    }

    protected Role myRole() {
        Guild g = myGuild();
        return g == null ? null : g.roleOf(player.getUniqueId());
    }

    protected void sound(String key) {
        plugin.gui().sound(player, file(), key);
    }

    public void handle(InventoryClickEvent e) {
        Click c = actions.get(e.getRawSlot());
        if (c == null) {
            return;
        }
        sound("click");
        try {
            c.on((Player) e.getWhoClicked(), e.getClick());
        } catch (Exception ex) {
            plugin.getLogger().warning("Menu click failed in " + file() + ": " + ex);
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
