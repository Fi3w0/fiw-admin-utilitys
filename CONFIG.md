# Configuration reference

Every file below lives in `config/fiw-admin/` and is created with defaults on first boot. After editing a file, run `/fiw reload` in-game or from the console — no restart needed. Colors use `&`-codes (e.g. `&c` = red, `&e` = yellow); `&r` resets formatting.

Durations (wherever a field or command takes one) accept `30s`, `5m`, `1h`, `1d`, or `1w` — seconds through weeks, up to 365 days. Anywhere a duration/threshold field is described as "0 = disabled" or "0 = permanent/manual", that's this mod's own convention throughout, not a Minecraft one.

Files not listed here (`alert-history.json`, `vanished-players.json`, `player-seen.json`, `frozen.json`, `banned-items.json`, `punishments.json`, `reports.json`, `maintenance.flag`) are **persistent state**, written by the mod as it runs. Stop the server before hand-editing them.

## maintenance.json

| Field | Default | Meaning |
|---|---|---|
| `enabled` | `true` | Master switch for the maintenance module and its commands. |
| `motdEnabled` | `true` | Replace the server-list MOTD with `motdMessage` while maintenance is on. |
| `motdMessage` | `"&cMaintenance mode"` | MOTD shown while maintenance is active (if `motdEnabled`). |
| `defaultMessage` | `"&cServer maintenance is in progress.\n&ePlease try again soon."` | Kick/disconnect message when no message is given to `/fiw maintenance on`. `\n` breaks the line. |
| `bypassPermission` | `"fiw.maintenance.bypass"` | Permission node that lets a player join during maintenance. |
| `opBypass` | `true` | If `true`, operator level 3 also bypasses maintenance (in addition to the permission node). |
| `countdownMessage` | `"&cMaintenance in &e{time}&c!"` | Broadcast during a scheduled countdown; `{time}` is substituted (e.g. `5m`). |
| `stopServerAfterCountdown` | `false` | If `true`, the server calls `stop` once a countdown-triggered maintenance enable completes. |
| `allowlistNames` | `[]` | Player names exempt from the maintenance kick, independent of permissions. |
| `allowlistUuids` | `[]` | Player UUIDs exempt from the maintenance kick. |

## sweep.json

| Field | Default | Meaning |
|---|---|---|
| `enabled` | `true` | Master switch for both item and mob sweeping. |
| `notifyPermission` | `"fiw.sweep.notify"` | Permission that receives sweep result broadcasts when `announceResults` is `"admins"`. |
| `announceResults` | `"admins"` | `"admins"` broadcasts only to `notifyPermission` holders; any other value broadcasts to everyone. |
| `items.enabled` | `true` | Enable ground-item cleanup. |
| `items.timerCleanEnabled` | `true` | Run a sweep every `items.intervalMinutes`, with warnings from `warnings`. |
| `items.intervalMinutes` | `10` | Minutes between timer sweeps. |
| `items.thresholdCleanEnabled` | `true` | Also sweep immediately once ground items reach `items.maxGroundItems` (checked every 10s). |
| `items.maxGroundItems` | `1500` | Ground item count that triggers a threshold sweep. |
| `items.minItemAgeSeconds` | `120` | Items younger than this are never swept. |
| `items.exemptNamedItems` | `true` | Skip items with a custom/anvil-renamed name. |
| `items.ignoredItems` | `["minecraft:nether_star"]` | Item IDs never swept, regardless of age. |
| `items.onlyTheseItems` | `[]` | If non-empty, **only** these item IDs are eligible for sweeping (overrides `ignoredItems`). |
| `mobs.enabled` | `true` | Enable mob-cap sweeping. |
| `mobs.checkIntervalSeconds` | `60` | Seconds between mob-cap checks. |
| `mobs.perChunkCaps.enabled` | `true` | Enforce `mobs.perChunkCaps.defaultCap` per chunk. |
| `mobs.perChunkCaps.defaultCap` | `40` | Max non-exempt mobs allowed per chunk. |
| `mobs.globalCapsEnabled` | `false` | Enforce `mobs.globalCaps` server-wide, per entity type. |
| `mobs.globalCaps` | `{}` | Map of entity ID → max count server-wide (only used when `globalCapsEnabled`). |
| `mobs.exemptNamed` | `true` | Never sweep custom-named mobs. |
| `mobs.exemptTamed` | `true` | Never sweep tamed mobs (pets). |
| `mobs.exemptLeashed` | `true` | Never sweep leashed mobs. |
| `mobs.exemptPersistent` | `true` | Never sweep mobs flagged persistent (e.g. spawned by spawn eggs, not natural spawns). |
| `mobs.neverClean` | `["minecraft:villager", "minecraft:iron_golem", "minecraft:allay"]` | Entity IDs always exempt from mob-cap sweeping. |
| `warnings.enabled` | `true` | Warn players in chat/actionbar before a timer sweep runs. |
| `warnings.chatSteps` | `[60, 30, 10]` | Seconds-remaining values that trigger a chat warning. |
| `warnings.actionbarFinalCountdown` | `5` | Show an actionbar countdown for the last N seconds. |
| `warnings.message` | `"&eGround items clearing in &c{seconds}s&e!"` | Warning text; `{seconds}` is substituted. |

## alert.json

| Field | Default | Meaning |
|---|---|---|
| `enabled` | `true` | Master switch for TPS alerting. |
| `tpsThreshold` | `15.0` | TPS below this triggers an alert (after `cooldownMinutes` since the last one). |
| `checkIntervalSeconds` | `5` | How often TPS is sampled. |
| `cooldownMinutes` | `5` | Minimum time between alerts. |
| `escalateBelowTps` | `8.0` | TPS below this is treated as a more severe alert (affects report framing). |
| `playSound` | `true` | Play a notification sound to staff on alert. |
| `notifyPermission` | `"fiw.alert.notify"` | Permission that receives alerts and sounds. |
| `report.topChunks` | `3` | Number of worst-offending chunks listed in a lag report. |
| `report.scanEntities` | `true` | Include entity counts when identifying busy chunks. |
| `report.scanBlockEntities` | `true` | Include block-entity (hoppers, etc.) counts when identifying busy chunks. |
| `report.clickableTeleport` | `true` | Add a clickable teleport link to each chunk in the report. |
| `report.clickableSweep` | `true` | Add a clickable "sweep this chunk" link to each chunk in the report. |
| `discord.enabled` | `false` | Send alerts to Discord. |
| `discord.webhookUrl` | `""` | Discord webhook URL. **Never commit or paste this in a public issue.** |
| `history.enabled` | `true` | Keep a rolling alert history (`/fiw lag history`). |
| `history.maxEntries` | `30` | Number of alerts kept in history. |

## vanish.json

| Field | Default | Meaning |
|---|---|---|
| `enabled` | `true` | Master switch for vanish. |
| `usePermission` | `"fiw.vanish.use"` | Permission to toggle vanish on self or another player. |
| `seePermission` | `"fiw.vanish.see"` | Permission to see vanished players. |
| `opUseFallback` | `true` | Operator level 3 may use vanish even without the permission node. |
| `opSeeFallback` | `true` | Operator level 3 may see vanished players even without the permission node. |
| `suppressJoinLeaveMessages` | `true` | Hide a vanished player's join/leave broadcast. |
| `hideFromTab` | `true` | Hide vanished players from the tab list for non-`seePermission` viewers. |
| `hideEntity` | `true` | Hide the vanished player's entity model from non-`seePermission` viewers. |
| `hideFromServerListCount` | `true` | Exclude vanished players from the server-list player count/sample. |
| `hideFromLocatorBar` | `true` | Hide vanished players from the locator bar/waypoint system (1.21.8+/1.21.11 only — see [Known limitations](README.md#known-limitations)). |
| `vanishedPrefix` | `"[V] "` | Prefix shown before a vanished player's own name to `seePermission` viewers (e.g. in tab list). |

## inspect.json

| Field | Default | Meaning |
|---|---|---|
| `enabled` | `true` | Master switch for `/fiw whois` and first-seen/last-seen tracking. |
| `findEnabled` | `true` | Enable `/fiw find <item>`. |
| `findIncludeEnderChests` | `true` | Include ender chest contents when `/fiw find` searches inventories. |

## freeze.json

| Field | Default | Meaning |
|---|---|---|
| `enabled` | `true` | Master switch for freeze. |
| `blockInteractions` | `true` | While frozen, block block-break/use, item-use, and attacks (movement is always anchored regardless of this flag). |
| `notifyTarget` | `true` | Send the frozen/unfrozen player a message when their state changes. |
| `frozenMessage` | `"&cYou have been frozen by an admin."` | Message sent to the target on freeze (if `notifyTarget`). |
| `unfrozenMessage` | `"&aYou have been unfrozen."` | Message sent to the target on unfreeze (if `notifyTarget`, including auto-unfreeze). |
| `reasonRequired` | `false` | If `true`, `/fiw freeze <player>` fails without a reason argument. |
| `autoUnfreezeSeconds` | `0` | Auto-unfreeze after this many seconds. `0` = manual unfreeze only. |
| `teleportToStaffOnFreeze` | `false` | Teleport the target to the freezing staff member's location at the moment of freezing. |
| `evidenceLogging` | `true` | Snapshot the target's held item, position, gamemode, and inventory at freeze time, viewable with `/fiw freeze evidence`. |
| `discord.enabled` | `false` | Send a freeze notice (and evidence, if logged) to Discord. |
| `discord.webhookUrl` | `""` | Discord webhook URL. **Never commit or paste this in a public issue.** |

## banitem.json

| Field | Default | Meaning |
|---|---|---|
| `enabled` | `true` | Master switch for item bans. |
| `blockedMessage` | `"&cThis item is disabled on this server."` | Message shown when a banned item's use/place/attack/break is blocked. |
| `bypassPermission` | `"fiw.banitem.bypass"` | Permission that lets a player use banned items anyway. |
| `confiscateFromInventory` | `true` | Also remove banned items already sitting in a non-bypass player's inventory (checked periodically). |

Crafting a banned item is not blocked yet — see [Known limitations](README.md#known-limitations).

## announce.json

| Field | Default | Meaning |
|---|---|---|
| `enabled` | `true` | Master switch for scheduled announcements. |
| `intervalMinutes` | `15` | Minutes between announcements (skipped while no players are online). |
| `randomOrder` | `false` | Pick the next message at random instead of cycling in order. |
| `prefix` | `"&7[&efiw&7] &r"` | Prepended to every announcement. |
| `messages` | `[]` | The announcement pool. Empty = nothing is announced. |

## newplayer.json

| Field | Default | Meaning |
|---|---|---|
| `enabled` | `true` | Master switch for first-join detection and alerts. |
| `notifyPermission` | `"fiw.alert.notify"` | Permission that receives `adminMessage` in chat. |
| `adminMessage` | `"&e⭐ New player joined for the first time: &b{player}"` | Sent to `notifyPermission` holders only. Blank disables the staff message. |
| `broadcastMessage` | `""` | Sent to **everyone** online. Blank (the default) disables the public broadcast. |
| `discordEnabled` | `false` | Send `discordMessage` to Discord on first join. |
| `discordWebhookUrl` | `""` | Discord webhook URL to use; if blank, falls back to `alert.json`'s `discord.webhookUrl`. **Never commit or paste this in a public issue.** |
| `discordMessage` | `"⭐ New player: {player}"` | Discord message text; `{player}` is substituted. |

## messages.json

| Field | Default | Meaning |
|---|---|---|
| `joinLeave.enabled` | `true` | Replace vanilla join/leave text with `joinMessage`/`leaveMessage`. When `false`, vanilla's own messages are used (still subject to vanish suppression). |
| `joinLeave.joinMessage` | `"&7[&a+&7] &e{player}"` | Custom join message; `{player}` is substituted. |
| `joinLeave.leaveMessage` | `"&7[&c-&7] &e{player}"` | Custom leave message; `{player}` is substituted. |
| `motd.enabled` | `true` | Rotate the server-list MOTD through `motd.motds` (maintenance MOTD always takes priority while active). |
| `motd.rotateMinutes` | `5` | Minutes between MOTD rotations. |
| `motd.randomOrder` | `false` | Pick the next MOTD at random instead of cycling in order. |
| `motd.motds` | `[]` | The MOTD pool. Empty = rotation does nothing (MOTD stays as originally set). |

## punishment.json

| Field | Default | Meaning |
|---|---|---|
| `enabled` | `true` | Master switch for kick/ban/mute commands, the ban/mute gates, and `/fiw punish`. |
| `reasonRequired` | `false` | If `true`, kick/ban/mute commands fail without a reason argument. |
| `defaultKickMessage` | `"&cYou have been kicked from the server."` | Shown to a kicked player. Supports `{reason}`. |
| `defaultBanMessage` | `"&cYou are banned from this server."` | Shown on ban and on every subsequent join attempt while banned. Supports `{reason}` and `{remaining}` (time left, only for temp bans). |
| `defaultMuteMessage` | `"&cYou are muted and cannot send chat messages."` | Shown to a muted player each time their chat message is blocked. |
| `broadcastPunishments` | `true` | Broadcast every kick/ban/mute to online `notifyPermission` holders and to Discord (if enabled). |
| `notifyPermission` | `"fiw.punish.notify"` | Permission that receives punishment broadcasts. |
| `escalationLookbackDays` | `30` | Only history entries within this many days count toward `/fiw punish`'s offense count. |
| `discord.enabled` | `false` | Send punishment broadcasts to Discord. |
| `discord.webhookUrl` | `""` | Discord webhook URL. **Never commit or paste this in a public issue.** |
| `escalationLadder` | mute 10m → tempban 1h → tempban 1d → ban | Ordered list of `{action, durationSeconds}` tiers `/fiw punish` walks through based on the target's offense count within `escalationLookbackDays` (tier index = offense count, clamped to the last tier). `action` is `MUTE`, `TEMPBAN`, or `BAN`; `durationSeconds` is ignored for `BAN` (always permanent) and `0` means permanent for `TEMPBAN`/`MUTE` too. Edit, reorder, add, or clear this list freely — an empty list disables `/fiw punish`. |

## report.json

| Field | Default | Meaning |
|---|---|---|
| `enabled` | `true` | Master switch for `/report` and `/fiw reports`. |
| `cooldownSeconds` | `60` | Minimum time between reports from the same player. |
| `notifyPermission` | `"fiw.report.notify"` | Permission that receives new-report notifications in chat. |
| `submittedMessage` | `"&aYour report has been submitted to staff."` | Shown to the reporter on successful submission. |
| `cooldownMessage` | `"&cPlease wait before submitting another report."` | Shown when a player reports again before their cooldown expires (the remaining time is appended automatically). |
| `discord.enabled` | `false` | Send new reports to Discord. |
| `discord.webhookUrl` | `""` | Discord webhook URL. **Never commit or paste this in a public issue.** |

## afk.json

AFK state itself is not persisted — it resets on server restart, so there is no `afk`-related state file.

| Field | Default | Meaning |
|---|---|---|
| `enabled` | `true` | Master switch for AFK detection, tagging, and auto-kick. |
| `idleThresholdSeconds` | `300` | Seconds of no block-break/use/attack/chat/movement before a player is marked AFK. |
| `kickAfterSeconds` | `0` | Auto-kick a player after this many idle seconds. `0` = never auto-kick. |
| `tag` | `"&7[AFK]&r"` | Prefix shown before an AFK player's name in the tab list. |
| `exemptPermission` | `"fiw.afk.exempt"` | Permission that exempts a player from auto-kick (they can still be tagged AFK). |
| `broadcastOnChange` | `true` | Broadcast `afkMessage`/`backMessage` to everyone when a player's AFK state changes. |
| `afkMessage` | `"&e{player} is now AFK."` | Broadcast when a player becomes AFK (if `broadcastOnChange`). |
| `backMessage` | `"&e{player} is no longer AFK."` | Broadcast when a player stops being AFK (if `broadcastOnChange`). |
| `kickMessage` | `"&cKicked for being AFK too long."` | Shown to a player auto-kicked for AFK. |

A player can also self-mark AFK with `/fiw afk` ("brb") — this is exempt from the idle-threshold auto-detection and only clears on another `/fiw afk` or on activity.
