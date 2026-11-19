package com.nagistudios.nsguildplus.listeners;

import com.nagistudios.nsguildplus.NSGuildPlus;
import com.nagistudios.nsguildplus.guild.Guild;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/** Friendly fire, ally pact and the hideout teleport cancel rules. */
public final class ProtectionListener implements Listener {
    private final NSGuildPlus plugin;

    public ProtectionListener(NSGuildPlus plugin) {
        this.plugin = plugin;
    }

    private Player shooter(Entity damager) {
        if (damager instanceof Player) {
            return (Player) damager;
        }
        if (damager instanceof Projectile) {
            Object s = ((Projectile) damager).getShooter();
            if (s instanceof Player) {
                return (Player) s;
            }
        }
        return null;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPvp(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player)) {
            return;
        }
        Player victim = (Player) e.getEntity();
        Player attacker = shooter(e.getDamager());
        if (attacker == null || attacker.equals(victim)) {
            return;
        }
        Guild vg = plugin.guilds().ofPlayer(victim.getUniqueId());
        Guild ag = plugin.guilds().ofPlayer(attacker.getUniqueId());
        if (vg != null && vg == ag && !vg.friendlyFire) {
            e.setCancelled(true);
            return;
        }
        if (plugin.guilds().areAllied(vg, ag)) {
            e.setCancelled(true);
            return;
        }
        plugin.hideouts().markCombat(victim.getUniqueId());
        plugin.hideouts().markCombat(attacker.getUniqueId());
        if (plugin.hideouts().shouldCancelOn("cancel-on-combat")) {
            plugin.hideouts().cancel(victim.getUniqueId(), "hideout.teleport-cancelled-combat");
            plugin.hideouts().cancel(attacker.getUniqueId(), "hideout.teleport-cancelled-combat");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player && plugin.hideouts().shouldCancelOn("cancel-on-damage")) {
            plugin.hideouts().cancel(e.getEntity().getUniqueId(), "hideout.teleport-cancelled-damage");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        if (e.getTo() == null || !plugin.hideouts().isTeleporting(e.getPlayer().getUniqueId())) {
            return;
        }
        if (!plugin.hideouts().shouldCancelOn("cancel-on-move")) {
            return;
        }
        if (e.getFrom().getBlockX() != e.getTo().getBlockX() || e.getFrom().getBlockY() != e.getTo().getBlockY()
                || e.getFrom().getBlockZ() != e.getTo().getBlockZ()) {
            plugin.hideouts().cancel(e.getPlayer().getUniqueId(), "hideout.teleport-cancelled-move");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent e) {
        if (e.getPlayer() instanceof Player && plugin.hideouts().isTeleporting(e.getPlayer().getUniqueId())
                && plugin.hideouts().shouldCancelOn("cancel-on-inventory-open")) {
            plugin.hideouts().cancel(e.getPlayer().getUniqueId(), "hideout.teleport-cancelled-inventory");
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        plugin.hideouts().cancel(e.getPlayer().getUniqueId(), null);
    }
}
