package com.nagistudios.nsguildplus.storage;

import com.nagistudios.nsguildplus.NSGuildPlus;
import com.nagistudios.nsguildplus.guild.Guild;
import com.nagistudios.nsguildplus.guild.GuildManager;
import com.nagistudios.nsguildplus.guild.GuildMember;
import com.nagistudios.nsguildplus.guild.Hideout;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** YAML storage (guilds.yml). Saving never blocks the main thread. */
public final class StorageManager {
    private final NSGuildPlus plugin;
    private final File file;
    private final ExecutorService io = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "NSGuildPlus-IO");
        t.setDaemon(true);
        return t;
    });
    private boolean scheduled;
    private long cycleStart;

    public StorageManager(NSGuildPlus plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "guilds.yml");
    }

    public long cycleStart() {
        return cycleStart;
    }

    public void setCycleStart(long v) {
        this.cycleStart = v;
        markDirty();
    }

    // ------------------------------------------------------------------ load

    public void load(GuildManager guilds) {
        if (!file.exists()) {
            return;
        }
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        cycleStart = y.getLong("meta.cycle-start", 0);
        ConfigurationSection root = y.getConfigurationSection("guilds");
        if (root == null) {
            return;
        }
        for (String key : root.getKeys(false)) {
            try {
                ConfigurationSection s = root.getConfigurationSection(key);
                if (s == null) {
                    continue;
                }
                UUID id = UUID.fromString(key);
                UUID captain = UUID.fromString(s.getString("captain", ""));
                Guild g = new Guild(id, s.getString("name", "Guild"), captain);
                String vice = s.getString("vice-captain", "none");
                if (vice != null && !vice.equalsIgnoreCase("none")) {
                    g.vice = UUID.fromString(vice);
                }
                for (String o : s.getStringList("officers")) {
                    g.officers.add(UUID.fromString(o));
                }
                g.stars = s.getInt("stars", 0);
                g.tagStyle = s.getString("tag-style", "white");
                g.ownedStyles.addAll(s.getStringList("owned-styles"));
                g.friendlyFire = s.getBoolean("friendly-fire", false);
                g.nameChangesUsed = s.getInt("name-changes-used", 0);
                g.createdAt = s.getLong("created-at", 0);
                for (String a : s.getStringList("allies")) {
                    g.allies.add(UUID.fromString(a));
                }
                ConfigurationSection hs = s.getConfigurationSection("hideouts");
                if (hs != null) {
                    for (String hk : hs.getKeys(false)) {
                        ConfigurationSection h = hs.getConfigurationSection(hk);
                        if (h == null) {
                            continue;
                        }
                        Hideout ho = new Hideout();
                        ho.name = h.getString("name", hk);
                        ho.world = h.getString("world", "world");
                        ho.x = h.getDouble("x");
                        ho.y = h.getDouble("y");
                        ho.z = h.getDouble("z");
                        ho.yaw = (float) h.getDouble("yaw");
                        ho.pitch = (float) h.getDouble("pitch");
                        String by = h.getString("created-by", "");
                        ho.createdBy = by.isEmpty() ? null : UUID.fromString(by);
                        ho.createdAt = h.getLong("created-at", 0);
                        g.hideouts.put(hk.toLowerCase(), ho);
                    }
                }
                ConfigurationSection es = s.getConfigurationSection("enderchest");
                if (es != null) {
                    for (String slot : es.getKeys(false)) {
                        ItemStack it = es.getItemStack(slot);
                        if (it != null) {
                            g.enderchest.put(Integer.parseInt(slot), it);
                        }
                    }
                }
                ConfigurationSection ms = s.getConfigurationSection("members");
                if (ms != null) {
                    for (String mk : ms.getKeys(false)) {
                        ConfigurationSection m = ms.getConfigurationSection(mk);
                        if (m == null) {
                            continue;
                        }
                        GuildMember gm = new GuildMember(UUID.fromString(mk), m.getString("name", "?"),
                                m.getLong("joined-at", 0));
                        gm.kills = m.getInt("kills");
                        gm.deaths = m.getInt("deaths");
                        gm.mobsKilled = m.getInt("mobs-killed");
                        gm.blocksPlaced = m.getInt("blocks-placed");
                        gm.blocksMined = m.getInt("blocks-mined");
                        gm.playtimeMinutes = m.getInt("playtime-minutes");
                        gm.achievements = m.getInt("achievements");
                        gm.xpLevel = m.getInt("xp-level");
                        gm.balance = m.getDouble("balance");
                        gm.annualPoints = m.getInt("annual-points");
                        gm.champion = m.getBoolean("champion");
                        gm.rankNumber = m.getInt("rank", 5);
                        g.members.put(gm.uuid, gm);
                    }
                }
                if (!g.members.containsKey(g.captain)) {
                    g.members.put(g.captain, new GuildMember(g.captain, g.memberName(g.captain), g.createdAt));
                }
                for (String def : plugin.shop().defaultOwned()) {
                    g.ownedStyles.add(def);
                }
                guilds.register(g);
            } catch (Exception e) {
                plugin.getLogger().warning("Could not load guild " + key + ": " + e.getMessage());
            }
        }
    }

    // ------------------------------------------------------------------ save

    public void markDirty() {
        if (scheduled || !plugin.isEnabled()) {
            return;
        }
        scheduled = true;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            scheduled = false;
            saveAsync();
        }, 100L);
    }

    private String snapshot() {
        YamlConfiguration y = new YamlConfiguration();
        y.set("meta.cycle-start", cycleStart);
        for (Guild g : plugin.guilds().all()) {
            syncEnderchest(g);
            String b = "guilds." + g.id;
            y.set(b + ".name", g.name);
            y.set(b + ".captain", g.captain.toString());
            y.set(b + ".vice-captain", g.vice == null ? "none" : g.vice.toString());
            List<String> officers = new ArrayList<>();
            for (UUID u : g.officers) {
                officers.add(u.toString());
            }
            y.set(b + ".officers", officers);
            y.set(b + ".stars", g.stars);
            y.set(b + ".tag-style", g.tagStyle);
            y.set(b + ".owned-styles", new ArrayList<>(g.ownedStyles));
            y.set(b + ".friendly-fire", g.friendlyFire);
            y.set(b + ".name-changes-used", g.nameChangesUsed);
            y.set(b + ".created-at", g.createdAt);
            List<String> allies = new ArrayList<>();
            for (UUID u : g.allies) {
                allies.add(u.toString());
            }
            y.set(b + ".allies", allies);
            for (java.util.Map.Entry<String, Hideout> e : g.hideouts.entrySet()) {
                Hideout h = e.getValue();
                String hb = b + ".hideouts." + e.getKey();
                y.set(hb + ".name", h.name);
                y.set(hb + ".world", h.world);
                y.set(hb + ".x", h.x);
                y.set(hb + ".y", h.y);
                y.set(hb + ".z", h.z);
                y.set(hb + ".yaw", (double) h.yaw);
                y.set(hb + ".pitch", (double) h.pitch);
                y.set(hb + ".created-by", h.createdBy == null ? "" : h.createdBy.toString());
                y.set(hb + ".created-at", h.createdAt);
            }
            for (java.util.Map.Entry<Integer, ItemStack> e : g.enderchest.entrySet()) {
                if (e.getValue() != null) {
                    y.set(b + ".enderchest." + e.getKey(), e.getValue());
                }
            }
            for (GuildMember m : g.members.values()) {
                String mb = b + ".members." + m.uuid;
                y.set(mb + ".name", m.name);
                y.set(mb + ".joined-at", m.joinedAt);
                y.set(mb + ".kills", m.kills);
                y.set(mb + ".deaths", m.deaths);
                y.set(mb + ".mobs-killed", m.mobsKilled);
                y.set(mb + ".blocks-placed", m.blocksPlaced);
                y.set(mb + ".blocks-mined", m.blocksMined);
                y.set(mb + ".playtime-minutes", m.playtimeMinutes);
                y.set(mb + ".achievements", m.achievements);
                y.set(mb + ".xp-level", m.xpLevel);
                y.set(mb + ".balance", m.balance);
                y.set(mb + ".annual-points", m.annualPoints);
                y.set(mb + ".champion", m.champion);
                y.set(mb + ".rank", m.rankNumber);
            }
        }
        return y.saveToString();
    }

    /** Copies the live shared inventory into the guild data. */
    private void syncEnderchest(Guild g) {
        Inventory inv = g.enderInventory;
        if (inv == null) {
            return;
        }
        g.enderchest.clear();
        ItemStack[] contents = inv.getContents();
        for (int i = 0; i < contents.length; i++) {
            if (contents[i] != null && !contents[i].getType().isAir()) {
                g.enderchest.put(i, contents[i].clone());
            }
        }
    }

    public void saveAsync() {
        final String data;
        try {
            data = snapshot();
        } catch (Exception e) {
            plugin.getLogger().severe("Could not save guilds: " + e.getMessage());
            return;
        }
        io.execute(() -> write(data));
    }

    /** Used on shutdown: waits until the data is on disk. */
    public void saveNow() {
        try {
            write(snapshot());
        } catch (Exception e) {
            plugin.getLogger().severe("Could not save guilds: " + e.getMessage());
        }
        io.shutdown();
    }

    private synchronized void write(String data) {
        try {
            plugin.getDataFolder().mkdirs();
            File tmp = new File(plugin.getDataFolder(), "guilds.yml.tmp");
            Files.write(tmp.toPath(), data.getBytes(StandardCharsets.UTF_8));
            Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            plugin.getLogger().severe("Could not write guilds.yml: " + e.getMessage());
        }
    }
}
