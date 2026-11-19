package com.nagistudios.nsguildplus.gui;

import com.nagistudios.nsguildplus.NSGuildPlus;
import com.nagistudios.nsguildplus.util.Ph;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/** Confirm menu: CONFIRM at slot 12, CANCEL at slot 14. */
public final class ConfirmMenu extends Menu {
    private final String action;
    private final String cost;
    private final Runnable onConfirm;
    private final Runnable onCancel;

    /**
     * @param action    text for the title, for example "Quit your guild"
     * @param cost      formatted cost or null
     * @param onConfirm runs after the menu is closed
     * @param onCancel  runs after the menu is closed, may be null
     */
    public ConfirmMenu(NSGuildPlus plugin, Player player, String action, String cost, Runnable onConfirm, Runnable onCancel) {
        super(plugin, player);
        this.action = action;
        this.cost = cost;
        this.onConfirm = onConfirm;
        this.onCancel = onCancel;
    }

    @Override
    protected String file() {
        return "confirm";
    }

    @Override
    protected int defaultSize() {
        return 27;
    }

    @Override
    protected Component title() {
        return plugin.messages().component("gui.confirm.title", Ph.of("action", action));
    }

    @Override
    protected void build() {
        List<Component> extra = new ArrayList<>();
        if (cost != null) {
            extra.add(Component.empty());
            extra.add(plugin.messages().component("gui.cost-line", Ph.of("cost", cost)));
        }
        set(slot("confirm", 12), Items.lang(plugin, "gui.confirm.confirm", mat("confirm", Material.LIME_CONCRETE),
                Ph.of(), extra), (p, t) -> {
            p.closeInventory();
            onConfirm.run();
        });
        set(slot("cancel", 14), Items.lang(plugin, "gui.confirm.cancel", mat("cancel", Material.RED_CONCRETE), Ph.of()),
                (p, t) -> {
                    p.closeInventory();
                    if (onCancel != null) {
                        onCancel.run();
                    }
                });
    }
}
