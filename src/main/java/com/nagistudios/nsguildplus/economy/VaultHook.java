package com.nagistudios.nsguildplus.economy;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.lang.reflect.Method;

/** Talks to Vault through reflection, so the plugin compiles and runs without the Vault jar. */
public final class VaultHook implements MoneyProvider {
    private final Object economy;
    private final Method getBalance;
    private final Method has;
    private final Method withdraw;
    private final Method format;
    private final Method success;

    private VaultHook(Object economy) throws Exception {
        this.economy = economy;
        Class<?> c = Class.forName("net.milkbowl.vault.economy.Economy");
        this.getBalance = c.getMethod("getBalance", OfflinePlayer.class);
        this.has = c.getMethod("has", OfflinePlayer.class, double.class);
        this.withdraw = c.getMethod("withdrawPlayer", OfflinePlayer.class, double.class);
        this.format = c.getMethod("format", double.class);
        Class<?> resp = Class.forName("net.milkbowl.vault.economy.EconomyResponse");
        this.success = resp.getMethod("transactionSuccess");
    }

    /** Returns null if Vault or an economy plugin is not available. */
    public static VaultHook create() {
        try {
            if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
                return null;
            }
            Class<?> c = Class.forName("net.milkbowl.vault.economy.Economy");
            @SuppressWarnings({"unchecked", "rawtypes"})
            org.bukkit.plugin.RegisteredServiceProvider<?> rsp =
                    Bukkit.getServicesManager().getRegistration((Class) c);
            if (rsp == null || rsp.getProvider() == null) {
                return null;
            }
            return new VaultHook(rsp.getProvider());
        } catch (Throwable t) {
            return null;
        }
    }

    @Override
    public double balance(OfflinePlayer player) {
        try {
            return ((Number) getBalance.invoke(economy, player)).doubleValue();
        } catch (Throwable t) {
            return 0;
        }
    }

    @Override
    public boolean has(OfflinePlayer player, double amount) {
        try {
            return (Boolean) has.invoke(economy, player, amount);
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public boolean withdraw(OfflinePlayer player, double amount) {
        try {
            Object r = withdraw.invoke(economy, player, amount);
            return (Boolean) success.invoke(r);
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public String format(double amount) {
        try {
            return String.valueOf(format.invoke(economy, amount));
        } catch (Throwable t) {
            return String.valueOf(amount);
        }
    }
}
