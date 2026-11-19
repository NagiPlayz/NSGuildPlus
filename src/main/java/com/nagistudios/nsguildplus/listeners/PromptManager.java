package com.nagistudios.nsguildplus.listeners;

import com.nagistudios.nsguildplus.NSGuildPlus;
import com.nagistudios.nsguildplus.util.Ph;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/** Chat input prompts (guild name, invite, alliance, hideout name). */
public final class PromptManager implements Listener {
    private static final class Prompt {
        final Consumer<String> handler;
        final String cancelKey;
        BukkitTask timeout;

        Prompt(Consumer<String> handler, String cancelKey) {
            this.handler = handler;
            this.cancelKey = cancelKey;
        }
    }

    private final NSGuildPlus plugin;
    private final Map<UUID, Prompt> prompts = new HashMap<>();

    public PromptManager(NSGuildPlus plugin) {
        this.plugin = plugin;
    }

    public boolean has(UUID uuid) {
        return prompts.containsKey(uuid);
    }

    public void ask(Player p, String promptKey, Map<String, String> ph, String cancelKey, Consumer<String> handler) {
        cancel(p.getUniqueId());
        p.closeInventory();
        Map<String, String> all = Ph.with(ph, "cancel_word", cancelWord(),
                "max_length", String.valueOf(plugin.getConfig().getInt("settings.name.max-length", 12)));
        plugin.messages().send(p, promptKey, all);
        Prompt pr = new Prompt(handler, cancelKey);
        int seconds = plugin.getConfig().getInt("settings.name.input-timeout-seconds", 30);
        final UUID id = p.getUniqueId();
        pr.timeout = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Prompt cur = prompts.get(id);
            if (cur == pr) {
                prompts.remove(id);
                Player online = Bukkit.getPlayer(id);
                if (online != null) {
                    plugin.messages().send(online, "prompt.timeout");
                }
            }
        }, Math.max(1, seconds) * 20L);
        prompts.put(id, pr);
    }

    private String cancelWord() {
        return plugin.getConfig().getString("settings.name.cancel-word", "cancel");
    }

    public void cancel(UUID uuid) {
        Prompt pr = prompts.remove(uuid);
        if (pr != null && pr.timeout != null) {
            pr.timeout.cancel();
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent e) {
        final UUID id = e.getPlayer().getUniqueId();
        if (!prompts.containsKey(id)) {
            return;
        }
        e.setCancelled(true);
        final String text = PlainTextComponentSerializer.plainText().serialize(e.message()).trim();
        Bukkit.getScheduler().runTask(plugin, () -> {
            Prompt pr = prompts.remove(id);
            if (pr == null) {
                return;
            }
            if (pr.timeout != null) {
                pr.timeout.cancel();
            }
            Player p = Bukkit.getPlayer(id);
            if (p == null) {
                return;
            }
            if (text.equalsIgnoreCase(cancelWord())) {
                plugin.messages().send(p, pr.cancelKey);
                return;
            }
            pr.handler.accept(text);
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        cancel(e.getPlayer().getUniqueId());
    }
}
