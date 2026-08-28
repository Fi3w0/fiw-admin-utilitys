# Changelog

## [1.2.0] - Punishment toolkit, reports, AFK, and freeze+

### Added

- Full punishment toolkit: `/fiw kick`, `/fiw ban`/`/fiw tempban`, `/fiw unban`, `/fiw mute`/`/fiw tempmute`, `/fiw unmute`, per-player punishment history via `/fiw history`, and `/fiw punish` — an admin-editable escalation ladder (e.g. mute → tempban → tempban → ban) that auto-picks the next tier from a player's recent offense count. Bans and mutes support permanent or timed durations (now up to a year — `Durations` gained `d`/`day`/`days` and `w`/`week`/`weeks` units), are enforced on join and while online, and can broadcast to staff and Discord.
- Player-facing `/report <player> <reason>` with a per-player cooldown; staff are notified in-chat (and optionally on Discord) and can triage with `/fiw reports`, `/fiw reports claim <id>`, and `/fiw reports resolve <id>`.
- AFK detection: idle players are tagged in the tab list, can broadcast when their state changes, and can be auto-kicked after a configurable idle period (with an exempt permission). Players can self-mark with `/fiw afk`; staff can list AFK players with `/fiw afk list`.
- Freeze+: freezing now accepts an optional reason, supports auto-unfreeze after a configurable duration, can teleport the frozen player to the freezing staff member, and records an evidence snapshot (held item, position, gamemode, inventory) viewable with `/fiw freeze evidence` — plus `/fiw freeze goto` to teleport to a frozen player and Discord delivery on freeze.
- New config files: `punishment.json`, `report.json`, `afk.json`; new state files `punishments.json`, `reports.json`; `freeze.json`/`frozen.json` gained new fields (existing installs are unaffected).
- New permission nodes for all of the above (see README); `fiw.report.use` and `fiw.afk.use` are granted to everyone by default.

### Fixed

> **The NeoForge 1.21.11 build was broken and would refuse to start on a real server.** It was pinned to NeoForge `26.1.2.75`, which actually bundles Mojang's calendar-versioned "26.1.2" release, not Minecraft 1.21.11 — the mod's own version check then rejected it on every boot. It now correctly targets NeoForge `21.11.45` (Java 21, matching the other 1.21.x NeoForge builds). If you run NeoForge 1.21.11, update to this release.

### Compatibility notes

- All new commands and behavior work identically across all eight release targets.
- Existing `frozen.json`/`banned-items.json` state and all existing commands/permissions are unchanged.

## [1.1.0] - Multi-version release foundation

### Added

- Fabric and NeoForge builds for Minecraft 1.21.8 and 1.21.1.
- Fabric and Forge builds for Minecraft 1.20.1.
- GitHub Actions CI for the complete eight-target build matrix.
- Automatic tagged GitHub releases and idempotent Modrinth publishing for project `f1yH9Ggq`.
- Manual release recovery workflow, GitHub issue forms, pull request template, contribution guide, security policy, and repository metadata.

### Changed

- Reorganized the project into a Minecraft-independent `core`, exact-version `common-*` integrations, and thin loader modules.
- Ported every existing command, config, persistent state, permission, and server-side feature using official Mojang mappings for each target.
- Made Fabric permission integration optional at runtime; LuckPerms and vanilla operator fallback continue to work without Fabric Permissions API.
- Expanded the README and platform descriptions with setup, compatibility, commands, configuration, permissions, and known limitations.

### Compatibility notes

- Minecraft 1.20.1 uses Forge because NeoForge has no maintained 1.20.1 release line.
- Locator-bar hiding is only applied on Minecraft versions that expose the relevant server API.

## [1.0.0] - Initial release

Initial release for Minecraft 1.21.11 — Fabric and NeoForge, fully server-side.

### Added

- Fabric and NeoForge server-side builds.
- `/fiw` command tree with reload/status commands.
- Maintenance mode: persistent lockout state, MOTD override, allowlist, bypass permission, kick-on-enable, and join blocking. Restart countdown via `/fiw maintenance in <duration> [message...]` with broadcast warnings, `/fiw maintenance cancel`, configurable countdown text, and optional server stop.
- Sweep: timed cleanup, threshold item cleanup, chunk-local cleanup, mob caps, warnings, actionbar countdowns, dry-run counts, and configurable announcements.
- Alert: TPS checks, cooldown/escalation, top loaded chunk reports, clickable teleport/sweep actions, notification sound, Discord webhooks, and persistent alert history.
- Vanish: persistent state, self and target toggles, tab/entity hiding, join/leave suppression, ping-count filtering, admin marker, and locator-bar hiding where supported.
- Inspect and find commands for player details and inventory/ender-chest searches.
- Persistent freeze with movement, block-break, use, and attack blocking.
- Timed item bans with action blocking and optional inventory confiscation.
- New-player alerts, scheduled announcements, customizable join/leave messages, and rotating server-list MOTDs.
- Optional LuckPerms integration and vanilla operator fallback.
- Per-module JSON configuration in `config/fiw-admin/` with live `/fiw reload`.
- Shared integration tests for config creation and persistent state.

### Known limitations

- Vanish is visual/packet based: sounds and particles can still happen, mobs can still target vanished players, and sleep skipping still counts them.
- BanItem does not block crafting the item.
