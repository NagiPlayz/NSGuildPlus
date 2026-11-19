package com.nagistudios.nsguildplus.gui;

import com.nagistudios.nsguildplus.NSGuildPlus;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/** Cancels every click and drag in our menus and routes clicks to the menu. */
public final class MenuListener implements Listener {
    private final NSGuildPlus plugin;

    public MenuListener(NSGuildPlus plugin) {
        this.plugin = plugin;
    }

    private InventoryHolder holder(Inventory inv) {
        if (inv == null) {
            return null;
        }
        try {
            return inv.getHolder(false);
        } catch (Throwable t) {
            return inv.getHolder();
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent e) {
        Inventory top = e.getInventory();
        InventoryHolder h = holder(top);
        if (!(h instanceof Menu)) {
            return;
        }
        e.setCancelled(true);
        if (e.getRawSlot() >= 0 && e.getRawSlot() < top.getSize()) {
            ((Menu) h).handle(e);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDrag(InventoryDragEvent e) {
        if (holder(e.getInventory()) instanceof Menu) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (holder(e.getInventory()) instanceof EnderHolder) {
            plugin.storage().markDirty();
        }
    }
}
