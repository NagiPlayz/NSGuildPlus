package com.nagistudios.nsguildplus.listeners;

import com.nagistudios.nsguildplus.NSGuildPlus;
import com.nagistudios.nsguildplus.guild.Guild;
import com.nagistudios.nsguildplus.guild.GuildMember;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerJoinEvent;

/** Collects kills, deaths, mobs, blocks and advancements for Power Score and Guild Stars. */
public final class StatListener implements Listener {
    private final NSGuildPlus plugin;

    public StatListener(NSGuildPlus plugin) {
        this.plugin = plugin;
    }

    private Guild guildOf(Player p) {
        return p == null ? null : plugin.guilds().ofPlayer(p.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        Guild g = guildOf(p);
        if (g == null) {
            return;
        }
        GuildMember m = g.member(p.getUniqueId());
        if (m == null) {
            return;
        }
        m.name = p.getName();
        m.xpLevel = p.getLevel();
        if (plugin.money() != null) {
            m.balance = plugin.money().balance(p);
        }
        plugin.stats().refresh(g, m);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent e) {
        Player victim = e.getEntity();
        Guild vg = guildOf(victim);
        if (vg != null) {
            GuildMember m = vg.member(victim.getUniqueId());
            if (m != null) {
                plugin.stats().addDeath(vg, m);
            }
        }
        Player killer = victim.getKiller();
        if (killer != null && !killer.equals(victim)) {
            Guild kg = guildOf(killer);
            if (kg != null) {
                GuildMember km = kg.member(killer.getUniqueId());
                if (km != null) {
                    plugin.stats().addKill(kg, km);
                }
            }
        }
        plugin.storage().markDirty();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent e) {
        LivingEntity dead = e.getEntity();
        if (dead instanceof Player) {
            return;
        }
        Player killer = dead.getKiller();
        Guild g = guildOf(killer);
        if (g == null) {
            return;
        }
        GuildMember m = g.member(killer.getUniqueId());
        if (m != null) {
            m.mobsKilled++;
            plugin.storage().markDirty();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        Guild g = guildOf(e.getPlayer());
        if (g == null) {
            return;
        }
        GuildMember m = g.member(e.getPlayer().getUniqueId());
        if (m != null) {
            m.blocksPlaced++;
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        Guild g = guildOf(e.getPlayer());
        if (g == null) {
            return;
        }
        GuildMember m = g.member(e.getPlayer().getUniqueId());
        if (m != null) {
            m.blocksMined++;
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onAdvancement(PlayerAdvancementDoneEvent e) {
        String key = e.getAdvancement().getKey().getKey();
        if (key.startsWith("recipes/")) {
            return;
        }
        Player p = e.getPlayer();
        Guild g = guildOf(p);
        if (g == null) {
            return;
        }
        GuildMember m = g.member(p.getUniqueId());
        if (m != null) {
            plugin.stats().addAchievement(g, m);
            plugin.storage().markDirty();
        }
    }
}
