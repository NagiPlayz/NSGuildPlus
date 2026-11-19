package com.nagistudios.nsguildplus.gui;

import com.nagistudios.nsguildplus.NSGuildPlus;
import com.nagistudios.nsguildplus.util.Util;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/** Reads the guis/*.yml files (size, slots, materials, filler, sounds). */
public final class GuiConfig {
    private static final String[] FILES = {"guild-menu", "members", "hideouts", "enderchest", "stats",
            "player-stats", "allies", "mailbox", "settings", "colour", "shop", "confirm", "leaderboard"};

    private final NSGuildPlus plugin;
    private final Map<String, YamlConfiguration> files = new HashMap<>();
    private final Map<String, YamlConfiguration> defaults = new HashMap<>();

    public GuiConfig(NSGuildPlus plugin) {
        this.plugin = plugin;
    }

    public void load() {
        files.clear();
        defaults.clear();
        for (String name : FILES) {
            String path = "guis/" + name + ".yml";
            File f = new File(plugin.getDataFolder(), path);
            if (!f.exists()) {
                plugin.saveResource(path, false);
            }
            files.put(name, YamlConfiguration.loadConfiguration(f));
            InputStream in = plugin.getResource(path);
            if (in != null) {
                defaults.put(name, YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8)));
            }
        }
    }

    public int i(String file, String path, int def) {
        YamlConfiguration y = files.get(file);
        if (y != null && y.contains(path)) {
            return y.getInt(path, def);
        }
        YamlConfiguration d = defaults.get(file);
        return d != null ? d.getInt(path, def) : def;
    }

    public String s(String file, String path, String def) {
        YamlConfiguration y = files.get(file);
        if (y != null && y.contains(path)) {
            return y.getString(path, def);
        }
        YamlConfiguration d = defaults.get(file);
        return d != null ? d.getString(path, def) : def;
    }

    public Material mat(String file, String path, Material def) {
        return Util.material(s(file, path, def.name()), def);
    }

    public int slot(String file, String item, int def) {
        return i(file, "items." + item + ".slot", def);
    }

    public Material itemMat(String file, String item, Material def) {
        return mat(file, "items." + item + ".material", def);
    }

    public void sound(Player p, String file, String key) {
        Util.sound(p, s(file, "sounds." + key, ""));
    }
}
