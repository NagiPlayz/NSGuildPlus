package com.nagistudios.nsguildplus.util;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.Locale;

public final class Util {
    private Util() {
    }

    /** "ocean_gradient" -> "Ocean Gradient". */
    public static String pretty(String id) {
        StringBuilder sb = new StringBuilder();
        for (String part : id.replace('-', '_').split("_")) {
            if (part.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(part.substring(0, 1).toUpperCase(Locale.ROOT)).append(part.substring(1).toLowerCase(Locale.ROOT));
        }
        return sb.toString();
    }

    public static Material material(String name, Material fallback) {
        if (name == null || name.isEmpty()) {
            return fallback;
        }
        Material m = Material.matchMaterial(name);
        return m == null ? fallback : m;
    }

    /** Plays a sound by enum name. Never throws, unknown sounds are ignored. */
    public static void sound(Player p, String name) {
        sound(p, name, 1f, 1f);
    }

    public static void sound(Player p, String name, float volume, float pitch) {
        if (p == null || name == null || name.isEmpty() || name.equalsIgnoreCase("NONE")) {
            return;
        }
        try {
            Sound s = Sound.valueOf(name.toUpperCase(Locale.ROOT));
            p.playSound(p.getLocation(), s, volume, pitch);
        } catch (Throwable ignored) {
            // unknown sound on this version
        }
    }

    public static String number(double v, String mode) {
        if (mode == null) {
            mode = "COMMAS";
        }
        switch (mode.toUpperCase(Locale.ROOT)) {
            case "PLAIN":
                return v == Math.floor(v) ? String.valueOf((long) v) : String.format(Locale.ROOT, "%.2f", v);
            case "SHORT":
                double a = Math.abs(v);
                if (a >= 1_000_000_000d) {
                    return trim(v / 1_000_000_000d) + "B";
                }
                if (a >= 1_000_000d) {
                    return trim(v / 1_000_000d) + "M";
                }
                if (a >= 1_000d) {
                    return trim(v / 1_000d) + "K";
                }
                return trim(v);
            default:
                if (v == Math.floor(v)) {
                    return String.format(Locale.ROOT, "%,d", (long) v);
                }
                return String.format(Locale.ROOT, "%,.2f", v);
        }
    }

    private static String trim(double v) {
        String s = String.format(Locale.ROOT, "%.1f", v);
        return s.endsWith(".0") ? s.substring(0, s.length() - 2) : s;
    }
}
