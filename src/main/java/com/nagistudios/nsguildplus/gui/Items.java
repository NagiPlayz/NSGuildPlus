package com.nagistudios.nsguildplus.gui;

import com.nagistudios.nsguildplus.NSGuildPlus;
import com.nagistudios.nsguildplus.message.MessageManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class Items {
    private Items() {
    }

    public static ItemStack of(Material mat, Component name, List<Component> lore) {
        ItemStack item = new ItemStack(mat == null ? Material.STONE : mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (name != null) {
                meta.displayName(MessageManager.noItalic(name));
            }
            if (lore != null && !lore.isEmpty()) {
                List<Component> l = new ArrayList<>();
                for (Component c : lore) {
                    l.add(MessageManager.noItalic(c));
                }
                meta.lore(l);
            }
            hideFlags(meta);
            item.setItemMeta(meta);
        }
        return item;
    }

    /** Builds an item from lang.yml: path.name and path.lore. */
    public static ItemStack lang(NSGuildPlus plugin, String path, Material mat, Map<String, String> ph) {
        return lang(plugin, path, mat, ph, null);
    }

    public static ItemStack lang(NSGuildPlus plugin, String path, Material mat, Map<String, String> ph,
                                 List<Component> extraLore) {
        MessageManager m = plugin.messages();
        Component name = m.component(path + ".name", ph);
        List<Component> lore = new ArrayList<>(m.list(path + ".lore", ph));
        if (extraLore != null) {
            lore.addAll(extraLore);
        }
        return of(mat, name, lore);
    }

    public static ItemStack blank(Material mat) {
        return of(mat, Component.space(), null);
    }

    public static ItemStack head(UUID owner, Component name, List<Component> lore) {
        ItemStack item = of(Material.PLAYER_HEAD, name, lore);
        try {
            ItemMeta meta = item.getItemMeta();
            if (meta instanceof SkullMeta) {
                ((SkullMeta) meta).setOwningPlayer(Bukkit.getOfflinePlayer(owner));
                item.setItemMeta(meta);
            }
        } catch (Throwable ignored) {
            // skin could not be applied
        }
        return item;
    }

    public static List<Component> append(List<Component> base, List<Component> more) {
        List<Component> l = new ArrayList<>(base);
        l.addAll(more);
        return l;
    }

    private static void hideFlags(ItemMeta meta) {
        try {
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
            try {
                meta.addItemFlags(ItemFlag.valueOf("HIDE_ADDITIONAL_TOOLTIP"));
            } catch (IllegalArgumentException ignored) {
                // older versions do not have this flag
            }
        } catch (Throwable ignored) {
            // flags not supported
        }
    }

    /** Enchant glint without changing the item. Works on old and new versions. */
    public static ItemStack glow(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        try {
            Method m = ItemMeta.class.getMethod("setEnchantmentGlintOverride", Boolean.class);
            m.invoke(meta, Boolean.TRUE);
            item.setItemMeta(meta);
            return item;
        } catch (Throwable ignored) {
            // 1.20.4 or older: use a hidden enchantment instead
        }
        try {
            @SuppressWarnings("deprecation")
            Enchantment e = Enchantment.getByKey(NamespacedKey.minecraft("unbreaking"));
            if (e != null) {
                meta.addEnchant(e, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                item.setItemMeta(meta);
            }
        } catch (Throwable ignored) {
            // no glow
        }
        return item;
    }
}
