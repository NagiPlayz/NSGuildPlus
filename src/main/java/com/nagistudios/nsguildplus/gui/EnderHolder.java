package com.nagistudios.nsguildplus.gui;

import com.nagistudios.nsguildplus.guild.Guild;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/** Holder of the shared guild enderchest inventory. */
public final class EnderHolder implements InventoryHolder {
    public final Guild guild;
    public Inventory inventory;

    public EnderHolder(Guild guild) {
        this.guild = guild;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
