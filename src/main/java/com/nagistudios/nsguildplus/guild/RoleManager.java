package com.nagistudios.nsguildplus.guild;

import com.nagistudios.nsguildplus.NSGuildPlus;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Reads roles.yml: allowed actions and visible commands per role. */
public final class RoleManager {
    private final NSGuildPlus plugin;
    private final Map<Role, Set<String>> permissions = new EnumMap<>(Role.class);
    private final Map<Role, List<String>> commands = new EnumMap<>(Role.class);

    public RoleManager(NSGuildPlus plugin) {
        this.plugin = plugin;
    }

    public void load() {
        permissions.clear();
        commands.clear();
        File f = new File(plugin.getDataFolder(), "roles.yml");
        if (!f.exists()) {
            plugin.saveResource("roles.yml", false);
        }
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(f);
        YamlConfiguration def = new YamlConfiguration();
        if (plugin.getResource("roles.yml") != null) {
            def = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(plugin.getResource("roles.yml"), StandardCharsets.UTF_8));
        }
        for (Role r : Role.values()) {
            String base = "roles." + r.name();
            List<String> perms = yml.contains(base + ".permissions")
                    ? yml.getStringList(base + ".permissions") : def.getStringList(base + ".permissions");
            List<String> cmds = yml.contains(base + ".commands")
                    ? yml.getStringList(base + ".commands") : def.getStringList(base + ".commands");
            Set<String> ps = new HashSet<>();
            for (String p : perms) {
                ps.add(p.toLowerCase());
            }
            List<String> cs = new ArrayList<>();
            for (String c : cmds) {
                cs.add(c.toLowerCase());
            }
            permissions.put(r, ps);
            commands.put(r, cs);
        }
    }

    public boolean can(Role role, String action) {
        if (role == null) {
            return false;
        }
        Set<String> s = permissions.get(role);
        return s != null && s.contains(action.toLowerCase());
    }

    public boolean canCommand(Role role, String sub) {
        if (role == null) {
            return false;
        }
        List<String> c = commands.get(role);
        return c != null && c.contains(sub.toLowerCase());
    }

    public List<String> commands(Role role) {
        List<String> c = role == null ? null : commands.get(role);
        return c == null ? Collections.emptyList() : c;
    }
}
