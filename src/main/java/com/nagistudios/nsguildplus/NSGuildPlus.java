package com.nagistudios.nsguildplus;

import com.nagistudios.nsguildplus.commands.AdminCommand;
import com.nagistudios.nsguildplus.commands.GuildCommand;
import com.nagistudios.nsguildplus.economy.MoneyProvider;
import com.nagistudios.nsguildplus.economy.VaultHook;
import com.nagistudios.nsguildplus.gui.GuiConfig;
import com.nagistudios.nsguildplus.gui.MenuListener;
import com.nagistudios.nsguildplus.guild.GuildManager;
import com.nagistudios.nsguildplus.guild.Role;
import com.nagistudios.nsguildplus.guild.RoleManager;
import com.nagistudios.nsguildplus.hideout.HideoutManager;
import com.nagistudios.nsguildplus.listeners.ProtectionListener;
import com.nagistudios.nsguildplus.listeners.PromptManager;
import com.nagistudios.nsguildplus.listeners.StatListener;
import com.nagistudios.nsguildplus.message.MessageManager;
import com.nagistudios.nsguildplus.message.PapiHook;
import com.nagistudios.nsguildplus.shop.ShopManager;
import com.nagistudios.nsguildplus.stats.StatsManager;
import com.nagistudios.nsguildplus.storage.StorageManager;
import com.nagistudios.nsguildplus.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class NSGuildPlus extends JavaPlugin {
    private MessageManager messages;
    private RoleManager roles;
    private GuiConfig gui;
    private ShopManager shop;
    private StorageManager storage;
    private GuildManager guilds;
    private StatsManager stats;
    private HideoutManager hideouts;
    private PromptManager prompts;
    private MoneyProvider money;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        messages = new MessageManager(this);
        messages.load();
        roles = new RoleManager(this);
        roles.load();
        gui = new GuiConfig(this);
        gui.load();
        shop = new ShopManager(this);
        shop.load();

        money = VaultHook.create();
        if (money == null) {
            getLogger().info("Vault economy not found. Money features (name change cost, hideout cost, wealth) are limited.");
        }
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            try {
                PapiHook hook = new PapiHook();
                messages.setPapi(hook);
                hook.registerExpansion(this);
            } catch (Throwable t) {
                getLogger().warning("Could not hook into PlaceholderAPI: " + t.getMessage());
            }
        }

        stats = new StatsManager(this);
        hideouts = new HideoutManager(this);
        prompts = new PromptManager(this);
        storage = new StorageManager(this);
        guilds = new GuildManager(this);
        storage.load(guilds);

        PluginCommand guildCmd = getCommand("guild");
        if (guildCmd != null) {
            GuildCommand c = new GuildCommand(this);
            guildCmd.setExecutor(c);
            guildCmd.setTabCompleter(c);
        }
        PluginCommand adminCmd = getCommand("gadmin");
        if (adminCmd != null) {
            AdminCommand c = new AdminCommand(this);
            adminCmd.setExecutor(c);
            adminCmd.setTabCompleter(c);
        }

        Bukkit.getPluginManager().registerEvents(new MenuListener(this), this);
        Bukkit.getPluginManager().registerEvents(prompts, this);
        Bukkit.getPluginManager().registerEvents(new StatListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ProtectionListener(this), this);

        Bukkit.getScheduler().runTaskTimer(this, () -> stats.minuteTick(), 1200L, 1200L);
        Bukkit.getScheduler().runTaskTimer(this, () -> stats.checkAnnualCycle(), 100L, 72000L);
        Bukkit.getScheduler().runTaskTimer(this, () -> storage.saveAsync(), 6000L, 6000L);
        getLogger().info("NSGuildPlus 1.0 enabled. Guilds loaded: " + guilds.all().size());
    }

    @Override
    public void onDisable() {
        if (hideouts != null) {
            hideouts.cancelAll();
        }
        if (storage != null) {
            storage.saveNow();
        }
    }

    /** /gadmin reload */
    public void reloadAll() {
        reloadConfig();
        messages.load();
        roles.load();
        gui.load();
        shop.load();
    }

    public MessageManager messages() {
        return messages;
    }

    public RoleManager roles() {
        return roles;
    }

    public GuiConfig gui() {
        return gui;
    }

    public ShopManager shop() {
        return shop;
    }

    public StorageManager storage() {
        return storage;
    }

    public GuildManager guilds() {
        return guilds;
    }

    public StatsManager stats() {
        return stats;
    }

    public HideoutManager hideouts() {
        return hideouts;
    }

    public PromptManager prompts() {
        return prompts;
    }

    /** Null if Vault or an economy plugin is missing. */
    public MoneyProvider money() {
        return money;
    }

    public String formatMoney(double amount) {
        if (money != null) {
            return money.format(amount);
        }
        return Util.number(amount, "COMMAS");
    }

    public String roleName(Role role) {
        if (role == null) {
            return "";
        }
        return messages.plain("roles." + role.key(), null);
    }
}
