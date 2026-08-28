# Fiw Admin Tools

**Maintenance, moderation, performance, and server-presence tools in one lightweight server-side mod.** Players can join with vanilla clients.

## What it does

- **Maintenance:** persistent lockout, custom MOTD/kick screen, bypasses, kick-on-enable, and scheduled countdowns.
- **Performance:** timed and threshold sweeps, mob caps, TPS history, worst-chunk reports, clickable staff actions, and optional Discord alerts.
- **Moderation:** packet-based vanish, whois, persistent freeze, inventory/ender-chest item search, and timed item bans.
- **Presence:** custom join/leave messages, rotating MOTDs, announcements, and first-join alerts.
- **Configuration:** independent JSON settings per module with live `/fiw reload`.

All tools live under the `/fiw` command tree. LuckPerms is supported, with loader permissions and vanilla operator level 3 as fallbacks.

## Supported versions

| Minecraft | Loaders |
|---|---|
| 1.21.11 | Fabric, NeoForge |
| 1.21.8 | Fabric, NeoForge |
| 1.21.1 | Fabric, NeoForge |
| 1.20.1 | Fabric, Forge |

NeoForge does not provide a maintained 1.20.1 release line, so that Minecraft version uses Forge.

## Installation

1. Download the file for your exact Minecraft version and loader.
2. Place it in the server's `mods/` folder.
3. Fabric also requires [Fabric API](https://modrinth.com/mod/fabric-api) and [Fabric Language Kotlin](https://modrinth.com/mod/fabric-language-kotlin). Forge and NeoForge need no extra required mod dependencies.
4. Start once and edit the generated files in `config/fiw-admin/`.
5. Run `/fiw reload` after config changes.

## Main commands

`/fiw status` · `/fiw reload` · `/fiw maintenance` · `/fiw sweep` · `/fiw lag` · `/fiw alert` · `/fiw vanish` · `/fiw whois` · `/fiw freeze` · `/fiw find` · `/fiw banitem`

## Important limitations

Vanish does not suppress sounds, particles, mob targeting, or sleep counting. BanItem does not block crafting yet.

Full commands, configuration, permissions, and source documentation are available on [GitHub](https://github.com/Fi3w0/fiw-admin-utilitys).
