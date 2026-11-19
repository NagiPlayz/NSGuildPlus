package com.nagistudios.nsguildplus.guild;

import com.nagistudios.nsguildplus.NSGuildPlus;
import com.nagistudios.nsguildplus.message.MessageManager;
import com.nagistudios.nsguildplus.util.Ph;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/** All guild rules: create, invite, kick, promote, leave, disband, alliances. */
public final class GuildManager {
    public static final class Invite {
        public final UUID guildId;
        public final UUID sender;
        public final String senderName;

        Invite(UUID guildId, UUID sender, String senderName) {
            this.guildId = guildId;
            this.sender = sender;
            this.senderName = senderName;
        }
    }

    public static final class AllyRequest {
        public final UUID fromGuild;
        public final String senderName;

        AllyRequest(UUID fromGuild, String senderName) {
            this.fromGuild = fromGuild;
            this.senderName = senderName;
        }
    }

    private final NSGuildPlus plugin;
    private final Map<UUID, Guild> guilds = new LinkedHashMap<>();
    private final Map<UUID, UUID> playerGuild = new HashMap<>();
    private final Map<UUID, List<Invite>> invites = new HashMap<>();
    private final Map<UUID, List<AllyRequest>> allyRequests = new HashMap<>();

    public GuildManager(NSGuildPlus plugin) {
        this.plugin = plugin;
    }

    private MessageManager msg() {
        return plugin.messages();
    }

    // ------------------------------------------------------------------ lookups

    public Collection<Guild> all() {
        return Collections.unmodifiableCollection(new ArrayList<>(guilds.values()));
    }

    public Guild get(UUID id) {
        return id == null ? null : guilds.get(id);
    }

    public Guild ofPlayer(UUID player) {
        UUID id = playerGuild.get(player);
        return id == null ? null : guilds.get(id);
    }

    public Guild byName(String name) {
        if (name == null) {
            return null;
        }
        for (Guild g : guilds.values()) {
            if (g.name.equalsIgnoreCase(name)) {
                return g;
            }
        }
        return null;
    }

    public Role roleOf(Player p) {
        Guild g = ofPlayer(p.getUniqueId());
        return g == null ? null : g.roleOf(p.getUniqueId());
    }

    public void register(Guild g) {
        guilds.put(g.id, g);
        for (UUID u : g.members.keySet()) {
            playerGuild.put(u, g.id);
        }
    }

    public int maxMembers() {
        return plugin.getConfig().getInt("guild.max-members", 50);
    }

    // ------------------------------------------------------------------ placeholders

    public Map<String, String> ph(Guild g) {
        Map<String, String> m = new HashMap<>();
        if (g == null) {
            return m;
        }
        m.put("guild", g.name);
        m.put("guild_tag", g.name);
        m.put("members", String.valueOf(g.members.size()));
        m.put("max_members", String.valueOf(maxMembers()));
        m.put("level", String.valueOf(plugin.stats().level(g)));
        m.put("stars", String.valueOf(g.stars));
        m.put("power_score", String.valueOf(plugin.stats().guildScore(g)));
        m.put("captain", g.memberName(g.captain));
        m.put("vice_captain", g.vice == null ? plugin.getConfig().getString("placeholders.empty", "-") : g.memberName(g.vice));
        m.put("ally_count", String.valueOf(g.allies.size()));
        return m;
    }

    // ------------------------------------------------------------------ names

    /** Returns the lang key of the error, or null if the name is fine. */
    public String validateName(String name, Guild except) {
        int min = plugin.getConfig().getInt("settings.name.min-length", 3);
        int max = plugin.getConfig().getInt("settings.name.max-length", 12);
        String regex = plugin.getConfig().getString("settings.name.regex", "^[a-zA-Z0-9_]+$");
        if (name.length() < min) {
            return "settings.name-too-short";
        }
        if (name.length() > max) {
            return "settings.name-too-long";
        }
        boolean allowColours = plugin.getConfig().getBoolean("settings.name.allow-colours", false);
        if (!allowColours && (name.indexOf('&') >= 0 || name.indexOf('§') >= 0)) {
            return "settings.name-invalid";
        }
        try {
            if (!Pattern.compile(regex).matcher(name).matches()) {
                return "settings.name-invalid";
            }
        } catch (Exception e) {
            // bad regex in config: ignore the regex check
        }
        Guild other = byName(name);
        if (other != null && other != except) {
            return "settings.name-taken";
        }
        return null;
    }

    public Map<String, String> namePh(String name) {
        return Ph.of("new_name", name,
                "min_length", String.valueOf(plugin.getConfig().getInt("settings.name.min-length", 3)),
                "max_length", String.valueOf(plugin.getConfig().getInt("settings.name.max-length", 12)),
                "cancel_word", plugin.getConfig().getString("settings.name.cancel-word", "cancel"));
    }

    // ------------------------------------------------------------------ create

    public Guild create(Player p, String name) {
        if (ofPlayer(p.getUniqueId()) != null) {
            msg().send(p, "general.already-in-guild");
            return null;
        }
        String err = validateName(name, null);
        if (err != null) {
            msg().send(p, err, namePh(name));
            return null;
        }
        long now = System.currentTimeMillis() / 1000L;
        Guild g = new Guild(UUID.randomUUID(), name, p.getUniqueId());
        g.createdAt = now;
        g.friendlyFire = plugin.getConfig().getBoolean("settings.friendly-fire.default", false);
        g.ownedStyles.addAll(plugin.shop().defaultOwned());
        g.tagStyle = plugin.shop().defaultStyle();
        g.ownedStyles.add(g.tagStyle);
        GuildMember m = new GuildMember(p.getUniqueId(), p.getName(), now);
        if (plugin.money() != null) {
            m.balance = plugin.money().balance(p);
        }
        g.members.put(p.getUniqueId(), m);
        register(g);
        plugin.stats().refresh(g, m);
        plugin.storage().markDirty();
        msg().send(p, "guild.created", Ph.of("guild", g.name));
        return g;
    }

    // ------------------------------------------------------------------ invites

    public List<Invite> invitesFor(UUID player) {
        List<Invite> list = invites.get(player);
        if (list == null) {
            return new ArrayList<>();
        }
        Iterator<Invite> it = list.iterator();
        while (it.hasNext()) {
            if (!guilds.containsKey(it.next().guildId)) {
                it.remove();
            }
        }
        return new ArrayList<>(list);
    }

    public void invite(Player sender, String targetName) {
        Guild g = ofPlayer(sender.getUniqueId());
        if (g == null) {
            msg().send(sender, "general.not-in-guild");
            return;
        }
        if (!plugin.roles().can(g.roleOf(sender.getUniqueId()), "invite")) {
            msg().send(sender, "general.no-permission");
            return;
        }
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            msg().send(sender, "general.player-not-found", Ph.of("target", targetName));
            return;
        }
        if (ofPlayer(target.getUniqueId()) != null) {
            msg().send(sender, "guild.target-in-guild", Ph.of("target", target.getName()));
            return;
        }
        if (g.members.size() >= maxMembers()) {
            msg().send(sender, "guild.full", ph(g));
            return;
        }
        List<Invite> list = invites.computeIfAbsent(target.getUniqueId(), k -> new ArrayList<>());
        list.removeIf(i -> i.guildId.equals(g.id));
        list.add(new Invite(g.id, sender.getUniqueId(), sender.getName()));
        msg().broadcastGuild(g, "guild.invited", Ph.of("player", sender.getName(), "target", target.getName()));
        msg().send(target, "guild.invite-received", Ph.of("guild", g.name, "sender", sender.getName()));
    }

    public void accept(Player p, Invite inv) {
        Guild g = get(inv.guildId);
        if (g == null) {
            msg().send(p, "guild.invite-invalid");
            return;
        }
        if (ofPlayer(p.getUniqueId()) != null) {
            msg().send(p, "general.already-in-guild");
            return;
        }
        if (g.members.size() >= maxMembers()) {
            msg().send(p, "guild.full", ph(g));
            return;
        }
        GuildMember m = new GuildMember(p.getUniqueId(), p.getName(), System.currentTimeMillis() / 1000L);
        if (plugin.money() != null) {
            m.balance = plugin.money().balance(p);
        }
        g.members.put(p.getUniqueId(), m);
        playerGuild.put(p.getUniqueId(), g.id);
        invites.remove(p.getUniqueId());
        plugin.stats().refresh(g, m);
        plugin.storage().markDirty();
        msg().send(p, "guild.you-joined", Ph.of("guild", g.name));
        for (Player other : g.onlinePlayers()) {
            if (!other.getUniqueId().equals(p.getUniqueId())) {
                msg().send(other, "guild.joined", Ph.of("player", p.getName()));
            }
        }
    }

    public void decline(Player p, Invite inv) {
        List<Invite> list = invites.get(p.getUniqueId());
        if (list != null) {
            list.remove(inv);
        }
        Guild g = get(inv.guildId);
        msg().send(p, "guild.invite-declined", Ph.of("guild", g == null ? "?" : g.name));
    }

    // ------------------------------------------------------------------ roles

    private void clearPlayer(UUID uuid) {
        plugin.hideouts().cancel(uuid, null);
        plugin.prompts().cancel(uuid);
        Player p = Bukkit.getPlayer(uuid);
        if (p != null) {
            p.closeInventory();
        }
    }

    private void removeMember(Guild g, UUID uuid) {
        g.members.remove(uuid);
        g.officers.remove(uuid);
        if (uuid.equals(g.vice)) {
            g.vice = null;
        }
        playerGuild.remove(uuid);
        clearPlayer(uuid);
    }

    public void kick(Player actor, UUID target) {
        Guild g = ofPlayer(actor.getUniqueId());
        if (g == null) {
            msg().send(actor, "general.not-in-guild");
            return;
        }
        Role ar = g.roleOf(actor.getUniqueId());
        if (!plugin.roles().can(ar, "kick")) {
            msg().send(actor, "general.no-permission");
            return;
        }
        if (!g.isMember(target)) {
            msg().send(actor, "guild.not-a-member", Ph.of("target", Bukkit.getOfflinePlayer(target).getName()));
            return;
        }
        if (target.equals(actor.getUniqueId())) {
            msg().send(actor, "guild.cannot-target-self");
            return;
        }
        Role tr = g.roleOf(target);
        if (tr == Role.CAPTAIN || (ar == Role.VICE_CAPTAIN && (tr == Role.VICE_CAPTAIN))) {
            msg().send(actor, "guild.cannot-kick");
            return;
        }
        String targetName = g.memberName(target);
        msg().broadcastGuild(g, "guild.kicked", Ph.of("target", targetName, "player", actor.getName()));
        Player tp = Bukkit.getPlayer(target);
        removeMember(g, target);
        if (tp != null) {
            msg().send(tp, "guild.you-kicked", Ph.of("guild", g.name));
        }
        plugin.storage().markDirty();
    }

    private Role next(Role r) {
        switch (r) {
            case MEMBER:
                return Role.OFFICER;
            case OFFICER:
                return Role.VICE_CAPTAIN;
            default:
                return null;
        }
    }

    private Role previous(Role r) {
        switch (r) {
            case VICE_CAPTAIN:
                return Role.OFFICER;
            case OFFICER:
                return Role.MEMBER;
            default:
                return null;
        }
    }

    public Role nextRole(Role r) {
        return r == null ? null : next(r);
    }

    public Role previousRole(Role r) {
        return r == null ? null : previous(r);
    }

    private void setRole(Guild g, UUID u, Role r) {
        g.officers.remove(u);
        if (u.equals(g.vice)) {
            g.vice = null;
        }
        if (r == Role.OFFICER) {
            g.officers.add(u);
        } else if (r == Role.VICE_CAPTAIN) {
            g.vice = u;
        }
    }

    private boolean checkLeaderAction(Player actor, Guild g, UUID target, String action) {
        if (g == null) {
            msg().send(actor, "general.not-in-guild");
            return false;
        }
        if (!plugin.roles().can(g.roleOf(actor.getUniqueId()), action)) {
            msg().send(actor, "general.no-permission");
            return false;
        }
        if (!g.isMember(target)) {
            msg().send(actor, "guild.not-a-member", Ph.of("target", Bukkit.getOfflinePlayer(target).getName()));
            return false;
        }
        if (target.equals(actor.getUniqueId())) {
            msg().send(actor, "guild.cannot-target-self");
            return false;
        }
        return true;
    }

    public void promote(Player actor, UUID target) {
        Guild g = ofPlayer(actor.getUniqueId());
        if (!checkLeaderAction(actor, g, target, "promote")) {
            return;
        }
        Role cur = g.roleOf(target);
        Role nxt = cur == null ? null : next(cur);
        String name = g.memberName(target);
        if (nxt == null) {
            msg().send(actor, "guild.cannot-promote", Ph.of("target", name));
            return;
        }
        if (nxt == Role.VICE_CAPTAIN && g.vice != null) {
            msg().send(actor, "guild.vice-slot-taken");
            return;
        }
        setRole(g, target, nxt);
        announceRole(g, name, nxt, true, actor.getName());
        plugin.storage().markDirty();
    }

    public void demote(Player actor, UUID target) {
        Guild g = ofPlayer(actor.getUniqueId());
        if (!checkLeaderAction(actor, g, target, "demote")) {
            return;
        }
        Role cur = g.roleOf(target);
        Role prev = cur == null ? null : previous(cur);
        String name = g.memberName(target);
        if (prev == null) {
            msg().send(actor, "guild.cannot-demote", Ph.of("target", name));
            return;
        }
        setRole(g, target, prev);
        announceRole(g, name, prev, false, actor.getName());
        plugin.storage().markDirty();
    }

    public void setVice(Player actor, UUID target) {
        Guild g = ofPlayer(actor.getUniqueId());
        if (!checkLeaderAction(actor, g, target, "setvc")) {
            return;
        }
        if (target.equals(g.vice)) {
            msg().send(actor, "guild.already-vice", Ph.of("target", g.memberName(target)));
            return;
        }
        if (g.vice != null) {
            UUID old = g.vice;
            setRole(g, old, Role.OFFICER);
        }
        setRole(g, target, Role.VICE_CAPTAIN);
        announceRole(g, g.memberName(target), Role.VICE_CAPTAIN, true, actor.getName());
        plugin.storage().markDirty();
    }

    /** New Vice Captain gets the special broadcast, other changes get promoted/demoted. */
    private void announceRole(Guild g, String targetName, Role role, boolean up, String actorName) {
        Map<String, String> ph = Ph.of("target", targetName, "guild", g.name, "player", actorName,
                "role", plugin.roleName(role));
        if (role == Role.VICE_CAPTAIN) {
            msg().broadcastGuild(g, "guild.new-vice-captain-broadcast", ph);
        } else {
            msg().broadcastGuild(g, up ? "guild.promoted" : "guild.demoted", ph);
        }
    }

    public void transfer(Player actor, UUID target) {
        Guild g = ofPlayer(actor.getUniqueId());
        if (!checkLeaderAction(actor, g, target, "transfer")) {
            return;
        }
        UUID old = g.captain;
        // the new Captain leaves their old rank, the old Captain becomes Vice Captain
        boolean wasVice = target.equals(g.vice);
        g.officers.remove(target);
        if (wasVice) {
            g.vice = null;
        }
        if (g.vice != null && !g.vice.equals(old)) {
            g.officers.add(g.vice);
        }
        g.captain = target;
        g.vice = old;
        Map<String, String> ph = Ph.of("target", g.memberName(target), "guild", g.name);
        msg().send(actor, "guild.transferred", ph);
        msg().broadcastGuild(g, "guild.new-captain-broadcast", ph);
        plugin.storage().markDirty();
    }

    // ------------------------------------------------------------------ leave and disband

    public void leave(Player p) {
        Guild g = ofPlayer(p.getUniqueId());
        if (g == null) {
            msg().send(p, "general.not-in-guild");
            return;
        }
        UUID me = p.getUniqueId();
        if (!me.equals(g.captain)) {
            removeMember(g, me);
            msg().send(p, "guild.you-left", Ph.of("guild", g.name));
            msg().broadcastGuild(g, "guild.left", Ph.of("player", p.getName()));
            plugin.storage().markDirty();
            return;
        }
        // Captain leaves
        if (g.members.size() <= 1) {
            disband(g, "guild.disbanded-auto");
            return;
        }
        UUID successor = null;
        String key;
        if (g.vice != null) {
            successor = g.vice;
            key = "guild.captain-left-vice-promoted";
        } else if (plugin.getConfig().getBoolean("guild.auto-promote-oldest-member", true)) {
            long best = Long.MAX_VALUE;
            for (GuildMember m : g.members.values()) {
                if (m.uuid.equals(me)) {
                    continue;
                }
                if (m.joinedAt < best) {
                    best = m.joinedAt;
                    successor = m.uuid;
                }
            }
            key = "guild.captain-left-oldest-promoted";
        } else {
            msg().send(p, "guild.captain-leave-blocked");
            return;
        }
        if (successor == null) {
            msg().send(p, "guild.captain-leave-blocked");
            return;
        }
        String newName = g.memberName(successor);
        g.officers.remove(successor);
        if (successor.equals(g.vice)) {
            g.vice = null;
        }
        g.captain = successor;
        removeMember(g, me);
        msg().send(p, "guild.you-left", Ph.of("guild", g.name));
        msg().broadcastGuild(g, key, Ph.of("old_captain", p.getName(), "new_captain", newName, "guild", g.name));
        plugin.storage().markDirty();
    }

    /** Disbands a guild. Only Captain commands and admins should call this. */
    public void disband(Guild g, String broadcastKey) {
        Map<String, String> ph = Ph.of("guild", g.name);
        msg().broadcastGuild(g, broadcastKey, ph);
        // close the shared enderchest for everyone looking at it
        if (g.enderInventory != null) {
            for (org.bukkit.entity.HumanEntity viewer : new ArrayList<>(g.enderInventory.getViewers())) {
                viewer.closeInventory();
            }
        }
        for (UUID u : new ArrayList<>(g.members.keySet())) {
            playerGuild.remove(u);
            clearPlayer(u);
        }
        guilds.remove(g.id);
        for (Guild other : guilds.values()) {
            other.allies.remove(g.id);
        }
        allyRequests.remove(g.id);
        for (List<AllyRequest> list : allyRequests.values()) {
            list.removeIf(r -> r.fromGuild.equals(g.id));
        }
        for (List<Invite> list : invites.values()) {
            list.removeIf(i -> i.guildId.equals(g.id));
        }
        plugin.storage().markDirty();
    }

    // ------------------------------------------------------------------ alliances

    public List<AllyRequest> requestsFor(UUID guildId) {
        List<AllyRequest> list = allyRequests.get(guildId);
        if (list == null) {
            return new ArrayList<>();
        }
        list.removeIf(r -> !guilds.containsKey(r.fromGuild));
        return new ArrayList<>(list);
    }

    private int maxAllies() {
        return plugin.getConfig().getInt("allies.max", 5);
    }

    public void requestAlliance(Player p, String guildName) {
        Guild g = ofPlayer(p.getUniqueId());
        if (g == null) {
            msg().send(p, "general.not-in-guild");
            return;
        }
        if (!plugin.roles().can(g.roleOf(p.getUniqueId()), "form-alliance")) {
            msg().send(p, "alliance.only-captain");
            return;
        }
        Guild t = byName(guildName);
        if (t == null) {
            msg().send(p, "alliance.guild-not-found", Ph.of("guild", guildName));
            return;
        }
        if (t == g) {
            msg().send(p, "alliance.own-guild");
            return;
        }
        if (g.allies.contains(t.id)) {
            msg().send(p, "alliance.already-allied", Ph.of("guild", t.name));
            return;
        }
        if (g.allies.size() >= maxAllies()) {
            msg().send(p, "alliance.limit-reached", Ph.of("amount", String.valueOf(maxAllies())));
            return;
        }
        List<AllyRequest> list = allyRequests.computeIfAbsent(t.id, k -> new ArrayList<>());
        for (AllyRequest r : list) {
            if (r.fromGuild.equals(g.id)) {
                msg().send(p, "alliance.already-requested", Ph.of("guild", t.name));
                return;
            }
        }
        list.add(new AllyRequest(g.id, p.getName()));
        msg().send(p, "alliance.requested", Ph.of("guild", t.name));
        msg().broadcastGuild(t, "alliance.received", Ph.of("guild", g.name));
    }

    public void acceptAlliance(Player p, AllyRequest r) {
        Guild g = ofPlayer(p.getUniqueId());
        Guild from = get(r.fromGuild);
        if (g == null || from == null) {
            msg().send(p, "alliance.request-invalid");
            return;
        }
        if (!plugin.roles().can(g.roleOf(p.getUniqueId()), "form-alliance")) {
            msg().send(p, "alliance.only-captain");
            return;
        }
        if (g.allies.size() >= maxAllies()) {
            msg().send(p, "alliance.limit-reached", Ph.of("amount", String.valueOf(maxAllies())));
            return;
        }
        if (from.allies.size() >= maxAllies()) {
            msg().send(p, "alliance.target-limit-reached");
            return;
        }
        g.allies.add(from.id);
        from.allies.add(g.id);
        List<AllyRequest> list = allyRequests.get(g.id);
        if (list != null) {
            list.removeIf(x -> x.fromGuild.equals(from.id));
        }
        msg().broadcastGuild(g, "alliance.formed", Ph.of("guild", from.name));
        msg().broadcastGuild(from, "alliance.formed", Ph.of("guild", g.name));
        plugin.storage().markDirty();
    }

    public void declineAlliance(Player p, AllyRequest r) {
        Guild g = ofPlayer(p.getUniqueId());
        if (g == null) {
            return;
        }
        if (!plugin.roles().can(g.roleOf(p.getUniqueId()), "form-alliance")) {
            msg().send(p, "alliance.only-captain");
            return;
        }
        List<AllyRequest> list = allyRequests.get(g.id);
        if (list != null) {
            list.remove(r);
        }
        Guild from = get(r.fromGuild);
        msg().send(p, "alliance.declined", Ph.of("guild", from == null ? "?" : from.name));
    }

    public void revokeAlliance(Player p, Guild other) {
        Guild g = ofPlayer(p.getUniqueId());
        if (g == null) {
            msg().send(p, "general.not-in-guild");
            return;
        }
        if (!plugin.roles().can(g.roleOf(p.getUniqueId()), "revoke-alliance")) {
            msg().send(p, "alliance.only-captain");
            return;
        }
        if (!g.allies.contains(other.id)) {
            msg().send(p, "alliance.not-allied", Ph.of("guild", other.name));
            return;
        }
        g.allies.remove(other.id);
        other.allies.remove(g.id);
        msg().broadcastGuild(g, "alliance.revoked", Ph.of("guild", other.name));
        msg().broadcastGuild(other, "alliance.revoked", Ph.of("guild", g.name));
        plugin.storage().markDirty();
    }

    public boolean areAllied(Guild a, Guild b) {
        return a != null && b != null && a != b && a.allies.contains(b.id);
    }

    /** Allied chat: own guild and all allied guilds. */
    public void allyChat(Player p, String message) {
        Guild g = ofPlayer(p.getUniqueId());
        if (g == null) {
            msg().send(p, "general.not-in-guild");
            return;
        }
        Map<String, String> ph = Ph.of("guild", g.name, "player", p.getName(), "message", message);
        msg().broadcastGuild(g, "alliance.chat-format", ph);
        for (UUID id : g.allies) {
            Guild ally = get(id);
            if (ally != null) {
                msg().broadcastGuild(ally, "alliance.chat-format", ph);
            }
        }
    }

    // ------------------------------------------------------------------ enderchest

    /** The shared guild enderchest. Created on first use from the saved items. */
    public org.bukkit.inventory.Inventory enderInventory(Guild g) {
        if (g.enderInventory == null) {
            int rows = Math.max(1, Math.min(6, plugin.getConfig().getInt("enderchest.rows", 4)));
            com.nagistudios.nsguildplus.gui.EnderHolder holder = new com.nagistudios.nsguildplus.gui.EnderHolder(g);
            org.bukkit.inventory.Inventory inv = Bukkit.createInventory(holder, rows * 9,
                    msg().component("gui.enderchest.title", null));
            holder.inventory = inv;
            for (Map.Entry<Integer, org.bukkit.inventory.ItemStack> e : g.enderchest.entrySet()) {
                if (e.getKey() >= 0 && e.getKey() < inv.getSize()) {
                    inv.setItem(e.getKey(), e.getValue());
                }
            }
            g.enderInventory = inv;
        }
        return g.enderInventory;
    }

    // ------------------------------------------------------------------ renaming

    public void applyRename(Guild g, String newName) {
        g.name = newName;
        g.nameChangesUsed++;
        plugin.storage().markDirty();
    }
}
