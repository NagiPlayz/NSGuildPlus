package com.nagistudios.nsguildplus.message;

import com.nagistudios.nsguildplus.NSGuildPlus;
import com.nagistudios.nsguildplus.placeholder.NSGuildPlusExpansion;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;

/** Only loaded when PlaceholderAPI is installed. */
public final class PapiHook {
    public String apply(Player player, String text) {
        return PlaceholderAPI.setPlaceholders(player, text);
    }

    public void registerExpansion(NSGuildPlus plugin) {
        new NSGuildPlusExpansion(plugin).register();
    }
}
