# NSGuildPlus 1.0

A guild plugin for Minecraft servers. Everything is GUI based: one command, `/guild`, opens every feature.

* **Version:** 1.0
* **Minecraft:** 1.20 up to 26.3 and newer versions (stable Paper API only, no NMS, no version checks)
* **Server software:** Paper and Paper forks (Purpur, Pufferfish and similar). Spigot is not supported.
* **Java:** 17 or newer (the jar is built for Java 17 and also runs on Java 21+)
* **Soft dependencies:** Vault (economy) and PlaceholderAPI. The plugin works without both.

## Features

* Roles: **Captain**, optional **Vice Captain**, **Officer**, **Member** (permissions in `roles.yml`)
* Guild Menu, Members, Hideouts, Enderchest, Statistics + Player Statistics, Allies, Mailbox, Settings, Colour menu, Confirm menu, Leaderboard and Shop
* **Guild Hideout:** the Captain sets it, every member can teleport (3 second countdown, fully configurable)
* **Guild Settings:** guild name (first change free, later changes cost Guild Stars), guild colour (only colours the guild owns), friendly fire
* **Guild Shop:** tag styles and colours bought with Guild Stars
* 5 personal status ranks (#1 is the best), Power Score, annual leaderboard
* Messages: `LEGACY_AMPERSAND` (default), `LEGACY_SECTION`, `MINIMESSAGE` and `AUTO`, everything editable in `lang.yml`
* PlaceholderAPI placeholders (`%nsguildplus_...%`)

## Installation

1. Stop the server.
2. Put `NSGuildPlus-1.0.jar` in the `plugins` folder.
3. Optional: install Vault with an economy plugin, and PlaceholderAPI.
4. Start the server. The plugin creates `config.yml`, `lang.yml`, `roles.yml` and the `guis/` folder.
5. Edit the files and run `/gadmin reload` (no restart needed).

## Commands

Players only see the commands their role allows (tab completion and `/guild help`). Typing a hidden command returns "no permission".

| Command | Who | Description |
|---|---|---|
| `/guild` | Everyone | Opens the Guild Menu (or the Mailbox if you have invites and no guild) |
| `/guild help` | Everyone | Lists the commands you can use |
| `/guild create <name>` | Players without a guild | Create a guild |
| `/guild accept [guild]` | Players without a guild | Accept an invite |
| `/guild leave` | All members | Leave (Confirm menu) |
| `/guild disband` | Captain | Disband (Confirm menu). The Vice Captain can never disband |
| `/guild invite <player>` | Captain, Vice Captain, Officer | Invite a player |
| `/guild kick <player>` | Captain, Vice Captain | Kick. The Vice Captain can only kick Officers and Members |
| `/guild promote\|demote <player>` | Captain | Member > Officer > Vice Captain |
| `/guild setvc <player>` | Captain | Set the Vice Captain |
| `/guild transfer <player>` | Captain | Transfer leadership |
| `/guild info` `stats` `leaderboard` `settings` `shop` `stars` `enderchest` | Members | Information and menus |
| `/guild mailbox` | Captain, Vice Captain, Officer | Alliance requests |
| `/guild rename <name>` | Captain | Rename (first change free, later changes cost Guild Stars) |
| `/guild colour` | Captain | Choose the guild colour |
| `/guild friendlyfire` | Captain | Toggle friendly fire |
| `/guild ally <guild>` / `unally <guild>` | Captain | Alliances |
| `/guild allychat <message>` | Members | Chat with allied guilds |
| `/guild hideout [name]` | Members | Open the Hideouts menu or teleport |
| `/guild sethideout <name>` / `delhideout <name>` / `renamehideout <old> <new>` / `movehideout <name>` | Captain | Hideout management |
| `/gadmin reload` | Admin | Reload config, lang, roles and GUI files |
| `/gadmin stars give\|take <player\|guild> <amount>` | Admin | Change Guild Stars |
| `/gadmin hideout <guild> <delete\|tp> <name>` | Admin | Hideout tools |
| `/gadmin disband <guild>` | Admin | Disband a guild |

New Captain or new Vice Captain: every online member of that guild gets a message.

## Permissions

| Permission | Default | Use |
|---|---|---|
| `nsguildplus.use` | true | Use `/guild` |
| `nsguildplus.hideout.use` | true | Teleport to hideouts |
| `nsguildplus.hideout.set` | true | Set hideouts (the Captain role is also required) |
| `nsguildplus.hideout.bypass` | op | Skip hideout cooldown, cost, combat and cross-world rules |
| `nsguildplus.enderchest` | true | Open the guild enderchest |
| `nsguildplus.settings` | true | Open guild settings |
| `nsguildplus.name.bypass-cost` | op | Rename without paying Guild Stars |
| `nsguildplus.admin` | op | `/gadmin` |
| `nsguildplus.admin.hideout` | op | `/gadmin hideout` |

Role checks (Captain, Vice Captain, Officer, Member) are always enforced by the plugin as well.

## Configuration

| File | What it controls |
|---|---|
| `config.yml` | message-format, max members, power factors, stars, ranks, hideout, settings (name rules and cost), shop styles, enderchest, allies, placeholder fallbacks |
| `lang.yml` | Every message, GUI title, item name and lore line. Uses `{placeholders}` |
| `roles.yml` | Allowed actions and visible commands per role |
| `guis/*.yml` | Size, slots, materials, filler item and sounds of each menu |
| `guilds.yml` | Guild data (created by the plugin, do not edit while the server runs) |

Colour formats, set with `message-format` in `config.yml`:

| Value | Example |
|---|---|
| `LEGACY_AMPERSAND` (default) | `&#C8FF2E&lMEMBERS` |
| `LEGACY_SECTION` | `§a§lMEMBERS` |
| `MINIMESSAGE` | `<#C8FF2E><bold>MEMBERS` |
| `AUTO` | The format is detected for each message |

The shipped `lang.yml` is written with `&` codes. If you switch the format, rewrite `lang.yml` in that format or use `AUTO`. Placeholders are replaced first, then colours are parsed. Player supplied text (names, chat input) is never parsed as colour or MiniMessage.

Missing keys in `lang.yml` fall back to the built-in English text and print a console warning. An optional extra language file `lang/<code>.yml` is used when `language: <code>` is set.

## PlaceholderAPI

Identifier: `nsguildplus`

* Player and guild: `guild_name`, `guild_tag`, `guild_level`, `guild_members`, `guild_max_members`, `captain`, `vice_captain`, `role`, `personal_rank`, `rank_number`, `power_score`, `kills`, `deaths`, `achievements`, `annual_position`, `stars`, `guild_stars`, `tag_style`, `tag_formatted`, `colour`, `friendly_fire`, `name_changes`, `name_change_free`, `name_change_cost` (Guild Stars), `ally_count`, `ally_name_<1-10>`
* Hideout: `hideout_count`, `hideout_max`, `hideout_name_<n>`, `hideout_world_<n>`, `hideout_x_<n>`, `hideout_y_<n>`, `hideout_z_<n>`, `hideout_cooldown`, `hideout_teleporting`, `hideout_countdown`
* Top guilds: `top_guild_name_<1-10>`, `top_guild_tag_<n>`, `top_guild_score_<n>`, `top_guild_members_<n>`
* Top players: `top_player_name_<n>`, `top_player_score_<n>`, `top_player_rank_<n>`
* Top members of your guild: `guild_member_top_name_<n>`, `guild_member_top_score_<n>`, `guild_member_top_rank_<n>`

Use `%nsguildplus_tag_formatted%` in chat or TAB plugins to show the guild name in its colour beside player names.

## Building from source

You need Java 17+ and Maven 3.8+ and internet access (Maven downloads Paper API and PlaceholderAPI).

```
mvn package
```

The jar is created at `target/NSGuildPlus-1.0.jar`.

No Maven on your computer? Upload this project to a GitHub repository. The file `.github/workflows/build.yml` builds the jar for free: open the Actions tab, run "Build NSGuildPlus" and download the `NSGuildPlus-1.0-jar` artifact.

## Assumptions (where the blueprint was not specific)

* Name change cost (default 500) is paid in **Guild Stars** taken from the guild. The first change is free.
* Guild Level comes from the guild's total Power Score (`guild.level-thresholds`). Level 1 is the start.
* The money part of the Power Score uses `guild.power-factors.money` (default 0.001 per unit, so large balances do not dominate).
* Buying and equipping Shop styles is **Captain only**.
* Leadership transfer: the old Captain becomes Vice Captain. A different old Vice Captain becomes Officer.
* A player without a guild who has pending invites sees the Mailbox when running `/guild`. Invites and alliance requests are kept in memory and are lost on restart.
* Alliance requests can be accepted or declined by the Captain only.
* Playtime is counted by the plugin (one minute per online minute). Experience level and balance are updated every minute.
* The annual cycle length is `leaderboard.annual-cycle-days` (default 365). At the end, the member with the most annual points becomes Guild Champion (a permanent `[MVP]` tag in the Members menu) and annual points reset.
* Guild chat is not part of this version. Only `/guild allychat` exists.

## Known limits

* Paper and Paper forks only. Folia is not supported.
* Hex colours need Minecraft 1.16+, which is always true for the supported versions.
* Storage is YAML (`guilds.yml`), saved asynchronously and on shutdown.
* This version was written without access to a Minecraft server for testing. Please test on a test server before using it on a live server and report problems.
* 
