package com.nagistudios.nsguildplus.message;

import com.nagistudios.nsguildplus.NSGuildPlus;
import com.nagistudios.nsguildplus.guild.Guild;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Loads lang.yml and turns text templates into Adventure components.
 * Order: replace {placeholders}, then PlaceholderAPI, then parse colours in the chosen format.
 */
public final class MessageManager {
    private static final LegacyComponentSerializer AMP = LegacyComponentSerializer.builder()
            .character('&').hexColors().build();
    private static final LegacyComponentSerializer SEC = LegacyComponentSerializer.builder()
            .character('§').hexColors().useUnusualXRepeatedCharacterHexFormat().build();
    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final Pattern MM_TAG = Pattern.compile("<(/?[a-zA-Z_]+|#[0-9a-fA-F]{6}|!?[a-zA-Z_]+)[^>]*>");

    private final NSGuildPlus plugin;
    private YamlConfiguration defaults = new YamlConfiguration();
    private YamlConfiguration lang = new YamlConfiguration();
    private YamlConfiguration override;
    private String format = "LEGACY_AMPERSAND";
    private PapiHook papi;
    private final Set<String> warned = new HashSet<>();

    public MessageManager(NSGuildPlus plugin) {
        this.plugin = plugin;
    }

    public void setPapi(PapiHook papi) {
        this.papi = papi;
    }

    public void load() {
        warned.clear();
        InputStream in = plugin.getResource("lang.yml");
        if (in != null) {
            defaults = YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
        }
        File f = new File(plugin.getDataFolder(), "lang.yml");
        if (!f.exists()) {
            plugin.saveResource("lang.yml", false);
        }
        lang = YamlConfiguration.loadConfiguration(f);
        override = null;
        String code = plugin.getConfig().getString("language", "en");
        if (code != null && !code.equalsIgnoreCase("en")) {
            File of = new File(plugin.getDataFolder(), "lang/" + code + ".yml");
            if (of.exists()) {
                override = YamlConfiguration.loadConfiguration(of);
            } else {
                plugin.getLogger().warning("Language file lang/" + code + ".yml was not found. Using lang.yml.");
            }
        }
        format = plugin.getConfig().getString("message-format", "LEGACY_AMPERSAND").toUpperCase(java.util.Locale.ROOT);
    }

    // ------------------------------------------------------------------ raw access

    public String raw(String key) {
        String v = null;
        if (override != null && override.isString(key)) {
            v = override.getString(key);
        }
        if (v == null && lang.isString(key)) {
            v = lang.getString(key);
        }
        if (v == null && defaults.isString(key)) {
            v = defaults.getString(key);
            warnMissing(key);
        }
        if (v == null) {
            warnMissing(key);
            return key;
        }
        return v;
    }

    public List<String> rawList(String key) {
        if (override != null && override.isList(key)) {
            return override.getStringList(key);
        }
        if (lang.isList(key)) {
            return lang.getStringList(key);
        }
        if (defaults.isList(key)) {
            warnMissing(key);
            return defaults.getStringList(key);
        }
        warnMissing(key);
        return new ArrayList<>();
    }

    public boolean has(String key) {
        return lang.contains(key) || defaults.contains(key) || (override != null && override.contains(key));
    }

    private void warnMissing(String key) {
        if (!lang.contains(key) && warned.add(key)) {
            plugin.getLogger().warning("Missing key in lang.yml: " + key + " (using built-in default)");
        }
    }

    // ------------------------------------------------------------------ parsing

    private String resolveFormat(String text) {
        if (!format.equals("AUTO")) {
            return format;
        }
        if (text.indexOf('§') >= 0) {
            return "LEGACY_SECTION";
        }
        if (MM_TAG.matcher(text).find()) {
            return "MINIMESSAGE";
        }
        return "LEGACY_AMPERSAND";
    }

    private String sanitize(String value, String fmt) {
        if (value == null) {
            return "";
        }
        if (fmt.equals("MINIMESSAGE")) {
            return MM.escapeTags(value.replace("§", ""));
        }
        return value.replace("&", "").replace("§", "");
    }

    /** Placeholders ending with _code are inserted as they are (trusted values). */
    public Component parse(String template, Map<String, String> ph, Player papiTarget) {
        if (template == null || template.isEmpty()) {
            return Component.empty();
        }
        String text = template;
        if (text.contains("{prefix}")) {
            text = text.replace("{prefix}", rawNoWarn("prefix"));
        }
        String fmt = resolveFormat(text);
        if (ph != null) {
            for (Map.Entry<String, String> e : ph.entrySet()) {
                String token = "{" + e.getKey() + "}";
                if (text.contains(token)) {
                    String v = e.getValue() == null ? "" : e.getValue();
                    text = text.replace(token, e.getKey().endsWith("_code") ? v : sanitize(v, fmt));
                }
            }
        }
        if (papiTarget != null && papi != null && text.indexOf('%') >= 0) {
            try {
                text = papi.apply(papiTarget, text);
            } catch (Throwable ignored) {
                // PlaceholderAPI problem, keep text
            }
        }
        switch (fmt) {
            case "LEGACY_SECTION":
                return SEC.deserialize(text);
            case "MINIMESSAGE":
                return MM.deserialize(text);
            default:
                return AMP.deserialize(text);
        }
    }

    private String rawNoWarn(String key) {
        if (lang.isString(key)) {
            return lang.getString(key);
        }
        String d = defaults.getString(key);
        return d == null ? "" : d;
    }

    public Component component(String key, Map<String, String> ph) {
        return parse(raw(key), ph, null);
    }

    public Component component(String key, Map<String, String> ph, Player papiTarget) {
        return parse(raw(key), ph, papiTarget);
    }

    public List<Component> list(String key, Map<String, String> ph) {
        return parseList(rawList(key), ph);
    }

    public List<Component> parseList(List<String> lines, Map<String, String> ph) {
        List<Component> out = new ArrayList<>();
        for (String line : lines) {
            out.add(parse(line, ph, null));
        }
        return out;
    }

    public String plain(String key, Map<String, String> ph) {
        return PlainTextComponentSerializer.plainText().serialize(component(key, ph));
    }

    public static String plain(Component c) {
        return PlainTextComponentSerializer.plainText().serialize(c);
    }

    /** Legacy section string, used for PlaceholderAPI output. */
    public static String toLegacy(Component c) {
        return SEC.serialize(c);
    }

    /** Turns a style code (legacy "&c" or MiniMessage "<gradient:..>") plus text into a component. */
    public Component styled(String code, String text) {
        if (code == null) {
            code = "";
        }
        if (code.startsWith("<")) {
            return MM.deserialize(code + MM.escapeTags(text));
        }
        return AMP.deserialize(code + text.replace("&", "").replace("§", ""));
    }

    public static Component noItalic(Component c) {
        if (c.decoration(TextDecoration.ITALIC) == TextDecoration.State.NOT_SET) {
            return c.decoration(TextDecoration.ITALIC, false);
        }
        return c;
    }

    // ------------------------------------------------------------------ sending

    public void send(CommandSender to, String key, Map<String, String> ph) {
        if (to == null) {
            return;
        }
        Player p = to instanceof Player ? (Player) to : null;
        to.sendMessage(parse(raw(key), ph, p));
    }

    public void send(CommandSender to, String key) {
        send(to, key, null);
    }

    public void sendList(CommandSender to, String key, Map<String, String> ph) {
        for (String line : rawList(key)) {
            to.sendMessage(parse(line, ph, to instanceof Player ? (Player) to : null));
        }
    }

    public void broadcastGuild(Guild g, String key, Map<String, String> ph) {
        if (g == null) {
            return;
        }
        for (Player p : g.onlinePlayers()) {
            send(p, key, ph);
        }
    }
}
