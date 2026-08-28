<div align="center">

# Fiw Admin Tools

### One lightweight, server-side toolkit for maintenance, moderation, performance, and server presence.

[![Build](https://github.com/Fi3w0/fiw-admin-utilitys/actions/workflows/build.yml/badge.svg)](https://github.com/Fi3w0/fiw-admin-utilitys/actions/workflows/build.yml)
[![Modrinth](https://img.shields.io/modrinth/dt/fiw-admin-utilitys?logo=modrinth&label=Modrinth)](https://modrinth.com/mod/fiw-admin-utilitys)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1%20%7C%201.21.1%20%7C%201.21.8%20%7C%201.21.11-62b47a)](#supported-versions)
[![Loaders](https://img.shields.io/badge/Loaders-Fabric%20%7C%20NeoForge%20%7C%20Forge-8a63d2)](#supported-versions)
[![License](https://img.shields.io/badge/License-All%20Rights%20Reserved-red)](LICENSE)

**[Download](https://modrinth.com/mod/fiw-admin-utilitys/versions)** · **[Quick setup](#quick-setup)** · **[Commands](#commands)** · **[Configuration](#configuration)** · **[Report an issue](https://github.com/Fi3w0/fiw-admin-utilitys/issues/new/choose)**

</div>

Fiw Admin Tools gives server owners a focused `/fiw` command suite without requiring players to install anything. Lock the server for maintenance, diagnose lag, safely clear entities, vanish staff, inspect and freeze players, control troublesome items, and customize the server's public presence from one mod.

## Highlights

- **Maintenance without surprises** — persistent lockout, custom MOTD and kick message, bypass list, kick-on-enable, and scheduled countdowns with an optional server stop.
- **Actionable lag alerts** — TPS history, worst-chunk reports, clickable teleport/sweep actions, notification sounds, and optional Discord webhooks.
- **Safe cleanup** — timed or threshold-based item sweeps, dry runs, chunk-local cleanup, mob caps, warnings, and protection for named, tamed, leashed, or persistent entities.
- **Practical moderation** — vanish, whois, freeze, inventory/ender-chest item search, and temporary or permanent item bans.
- **Better server presence** — custom join/leave messages, rotating MOTDs, announcements, and first-join alerts in game or through Discord.
- **Live JSON configuration** — each module can be enabled independently and reloaded with `/fiw reload`.

## Quick setup

1. Download the jar matching your exact Minecraft version and loader from [Modrinth](https://modrinth.com/mod/fiw-admin-utilitys/versions).
2. Put it in the server's `mods/` directory.
3. For Fabric, also install [Fabric API](https://modrinth.com/mod/fabric-api) and [Fabric Language Kotlin](https://modrinth.com/mod/fabric-language-kotlin). Forge and NeoForge builds have no extra required mod dependencies.
4. Start the server once. Defaults are generated in `config/fiw-admin/`.
5. Give staff the relevant permission nodes with LuckPerms, or use vanilla operator level 3 as the fallback.
6. Run `/fiw status` to confirm the modules loaded.

This mod is **server-side only**. Vanilla clients can join normally.

## Supported versions

| Minecraft | Loader | Minimum loader/API | Java | Release jar |
|---|---|---|---:|---|
| 1.21.11 | Fabric | Loader 0.19.2, Fabric API 0.141.4 | 21 | `fiw-admin-tools-fabric-1.21.11-<version>.jar` |
| 1.21.11 | NeoForge | 26.1.2.75 | 25 | `fiw-admin-tools-neoforge-1.21.11-<version>.jar` |
| 1.21.8 | Fabric | Loader 0.19.2, Fabric API 0.130.0 | 21 | `fiw-admin-tools-fabric-1.21.8-<version>.jar` |
| 1.21.8 | NeoForge | 21.8.53 | 21 | `fiw-admin-tools-neoforge-1.21.8-<version>.jar` |
| 1.21.1 | Fabric | Loader 0.19.2, Fabric API 0.115.0 | 21 | `fiw-admin-tools-fabric-1.21.1-<version>.jar` |
| 1.21.1 | NeoForge | 21.1.227 | 21 | `fiw-admin-tools-neoforge-1.21.1-<version>.jar` |
| 1.20.1 | Fabric | Loader 0.16.14, Fabric API 0.92.2 | 17 | `fiw-admin-tools-fabric-1.20.1-<version>.jar` |
| 1.20.1 | Forge | 47.3.0 | 17 | `fiw-admin-tools-forge-1.20.1-<version>.jar` |

NeoForge does not have a maintained 1.20.1 release line, so Minecraft 1.20.1 uses Forge. Every target uses the official Mojang mappings for its exact Minecraft version.

## Features

### Maintenance

Enable a restart-safe server lockout immediately or schedule it with a live countdown. Non-exempt players are kicked, new connections are blocked, and the server-list MOTD changes until maintenance is disabled. The countdown can optionally stop the server when it completes.

### Sweep and TPS alerts

Sweep can clean ground items on a timer, when a threshold is crossed, or only in the current chunk. Mob caps protect server performance while exemptions keep important entities safe. TPS alerts record history and identify the busiest loaded chunks by entities and block entities, with clickable staff actions and optional Discord delivery.

### Vanish and moderation

Vanish hides staff from the player list, world tracking, join/leave messages, server-list counts, and—where Minecraft provides it—the locator bar. `/fiw whois` shows useful live player details, freeze persists through relogs, find searches inventories and optional ender chests, and BanItem blocks and optionally confiscates configured items.

### Messages and first joins

Replace vanilla join/leave text, rotate MOTDs, schedule announcements, and notify staff when a player joins for the first time. Message colors, prefixes, intervals, randomization, and Discord delivery are configurable.

## Commands

| Area | Commands |
|---|---|
| General | `/fiw status`, `/fiw reload` |
| Maintenance | `/fiw maintenance on [message]`, `/fiw maintenance in <duration> [message]`, `/fiw maintenance cancel`, `/fiw maintenance off`, `/fiw maintenance status` |
| Sweep | `/fiw sweep now`, `/fiw sweep count`, `/fiw sweep here`, `/fiw sweep on`, `/fiw sweep off` |
| Performance | `/fiw lag`, `/fiw lag history`, `/fiw alert on`, `/fiw alert off` |
| Vanish | `/fiw vanish`, `/fiw vanish <player>`, `/fiw vanish list` |
| Inspect | `/fiw whois <player>`, `/fiw find <item>` |
| Freeze | `/fiw freeze <player>`, `/fiw freeze list` |
| BanItem | `/fiw banitem <item> [duration]`, `/fiw banitem list` |

Durations accept values such as `30s`, `5m`, or `1h`.

## Configuration

Files are generated in `config/fiw-admin/`. Run `/fiw reload` after editing them.

| File | Purpose |
|---|---|
| `maintenance.json` | Lockout, MOTD, bypasses, countdown, and stop behavior |
| `sweep.json` | Item cleanup, mob caps, exemptions, warnings, and announcements |
| `alert.json` | TPS thresholds, cooldowns, reports, sounds, and Discord webhook |
| `vanish.json` | Vanish display and visibility behavior |
| `inspect.json` | Whois and item-search options |
| `freeze.json` | Freeze behavior |
| `banitem.json` | Item-ban behavior and confiscation |
| `announce.json` | Scheduled rotating or random announcements |
| `newplayer.json` | First-join staff, broadcast, and Discord messages |
| `messages.json` | Join/leave messages and rotating server-list MOTD |

The mod also owns `alert-history.json`, `vanished-players.json`, `player-seen.json`, `frozen.json`, `banned-items.json`, and `maintenance.flag`. These are persistent state files; stop the server before editing them manually. Never publish a Discord webhook from `alert.json` or `newplayer.json` in an issue or log.

## Permissions

LuckPerms is supported when installed. Its explicit allow/deny result is checked first; otherwise the mod uses the loader permission API where available and finally vanilla operator level 3.

| Node | Grants |
|---|---|
| `fiw.maintenance.manage` | Manage maintenance and its countdown |
| `fiw.maintenance.bypass` | Join during maintenance |
| `fiw.sweep.manage` | Run and toggle sweeps |
| `fiw.sweep.notify` | Receive sweep notifications |
| `fiw.alert.manage` | Run lag reports and toggle alerts |
| `fiw.alert.notify` | Receive TPS alerts |
| `fiw.vanish.use` | Vanish self or another player |
| `fiw.vanish.see` | See vanished players |
| `fiw.inspect.use` | Use whois and find |
| `fiw.freeze.use` | Freeze players and list frozen players |
| `fiw.banitem.manage` | Add, remove, and list item bans |
| `fiw.banitem.bypass` | Use banned items |

## Known limitations

- Vanish is packet/visibility based. Sounds and particles can still reveal activity, mobs can still target vanished players, and sleep skipping still counts them.
- Minecraft 1.20.1 and 1.21.1 do not have the newer locator-bar API, so there is no locator-bar state to hide on those versions.
- BanItem blocks using, placing, attacking, and breaking with an item, and can confiscate it from inventories; crafting the item itself is not blocked yet.

## Building from source

The Gradle runtime uses JDK 21 and resolves the Java 17, 21, and 25 toolchains required by individual targets.

```bash
./gradlew build
```

That command runs the shared tests and creates all eight release jars. Individual targets can be built with tasks such as `./gradlew :fabric-1.21.1:build` or `./gradlew :forge-1.20.1:build`.

The project keeps Minecraft-independent configuration and state logic in `core`, exact-version Minecraft integrations in `common-*`, and loader entrypoints in `fabric-*`, `neoforge-*`, and `forge-*`.

## Support and contributing

- Found a bug? Use the [bug report form](https://github.com/Fi3w0/fiw-admin-utilitys/issues/new?template=bug_report.yml).
- Have an idea? Use the [feature request form](https://github.com/Fi3w0/fiw-admin-utilitys/issues/new?template=feature_request.yml).
- Want to contribute? Read [CONTRIBUTING.md](CONTRIBUTING.md).
- Please report security-sensitive problems according to [SECURITY.md](SECURITY.md).

## License

Copyright © Fi3w0. All rights reserved. See [LICENSE](LICENSE).
