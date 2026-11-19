package com.nagistudios.nsguildplus.economy;

import org.bukkit.OfflinePlayer;

public interface MoneyProvider {
    double balance(OfflinePlayer player);

    boolean has(OfflinePlayer player, double amount);

    boolean withdraw(OfflinePlayer player, double amount);

    String format(double amount);
}
