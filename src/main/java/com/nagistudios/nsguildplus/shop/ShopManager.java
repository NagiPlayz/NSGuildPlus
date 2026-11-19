package com.nagistudios.nsguildplus.shop;

import com.nagistudios.nsguildplus.NSGuildPlus;
import com.nagistudios.nsguildplus.guild.Guild;
import com.nagistudios.nsguildplus.guild.Role;
import com.nagistudios.nsguildplus.message.MessageManager;
import com.nagistudios.nsguildplus.util.Ph;
import com.nagistudios.nsguildplus.util.Util;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Tag styles and colours that guilds buy with Guild Stars. */
public final class ShopManager {
    public static final class Style {
        public final String id;
        public final String name;
        public final String category;
        public final int price;
        public final boolean enabled;
        public final String permission;
        public final String code;
        public final Material material;

        Style(String id, String name, String category, int price, boolean enabled, String permission,
              String code, Material material) {
            this.id = id;
            this.name = name;
            this.category = category;
            this.price = price;
            this.enabled = enabled;
            this.permission = permission;
            this.code = code;
            this.material = material;
        }
    }

    private final NSGuildPlus plugin;
    private final Map<String, Style> styles = new LinkedHashMap<>();

    public ShopManager(NSGuildPlus plugin) {
        this.plugin = plugin;
    }

    public void load() {
        styles.clear();
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("shop.styles");
        if (sec == null) {
            return;
        }
        for (String id : sec.getKeys(false)) {
            ConfigurationSection s = sec.getConfigurationSection(id);
            if (s == null) {
                continue;
            }
            String name = s.getString("name", Util.pretty(id));
            styles.put(id.toLowerCase(), new Style(id.toLowerCase(), name,
                    s.getString("category", "BASIC").toUpperCase(),
                    s.getInt("price", 0), s.getBoolean("enabled", true), s.getString("permission", ""),
                    s.getString("code", "&f"), Util.material(s.getString("material"), Material.WHITE_CONCRETE)));
        }
    }

    public List<Style> enabledStyles() {
        List<Style> l = new ArrayList<>();
        for (Style s : styles.values()) {
            if (s.enabled) {
                l.add(s);
            }
        }
        return l;
    }

    public Style get(String id) {
        return id == null ? null : styles.get(id.toLowerCase());
    }

    public List<String> defaultOwned() {
        List<String> l = new ArrayList<>();
        for (String s : plugin.getConfig().getStringList("shop.default-owned")) {
            l.add(s.toLowerCase());
        }
        return l;
    }

    public String defaultStyle() {
        return plugin.getConfig().getString("shop.default-style", "white").toLowerCase();
    }

    public Component styledName(Style style, String text) {
        return plugin.messages().styled(style == null ? "&f" : style.code, text);
    }

    /** Guild name in the equipped colour, as a legacy string (for PlaceholderAPI). */
    public String tagFormatted(Guild g) {
        Style s = get(g.tagStyle);
        return MessageManager.toLegacy(styledName(s, g.name));
    }

    public String styleName(String id) {
        Style s = get(id);
        return s == null ? Util.pretty(id == null ? "white" : id) : s.name;
    }

    public void buy(Player p, Guild g, Style s) {
        MessageManager m = plugin.messages();
        Role role = g.roleOf(p.getUniqueId());
        if (role != Role.CAPTAIN) {
            m.send(p, "shop.only-captain");
            return;
        }
        if (g.ownedStyles.contains(s.id)) {
            m.send(p, "shop.already-owned");
            return;
        }
        if (!s.permission.isEmpty() && !p.hasPermission(s.permission)) {
            m.send(p, "shop.no-style-permission");
            return;
        }
        if (g.stars < s.price) {
            m.send(p, "shop.not-enough-stars", Ph.of("cost", String.valueOf(s.price)));
            return;
        }
        g.stars -= s.price;
        g.ownedStyles.add(s.id);
        plugin.storage().markDirty();
        m.send(p, "shop.purchased", Ph.of("style_name", s.name, "cost", String.valueOf(s.price)));
    }

    public void equip(Player p, Guild g, Style s) {
        MessageManager m = plugin.messages();
        if (g.roleOf(p.getUniqueId()) != Role.CAPTAIN) {
            m.send(p, "shop.only-captain");
            return;
        }
        if (!g.ownedStyles.contains(s.id)) {
            return;
        }
        g.tagStyle = s.id;
        plugin.storage().markDirty();
        m.send(p, "shop.equipped", Ph.of("style_name", s.name));
    }
}
