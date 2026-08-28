# Fiw Admin Tools roadmap

## Current foundation

Version 1.1.0 keeps the complete existing feature set across eight server-side builds:

| Minecraft | Fabric | NeoForge | Forge |
|---|:---:|:---:|:---:|
| 1.21.11 | Yes | Yes | — |
| 1.21.8 | Yes | Yes | — |
| 1.21.1 | Yes | Yes | — |
| 1.20.1 | Yes | — | Yes |

The project uses one Minecraft-independent `core`, an exact-version `common-*` integration for official Mojang-mapped APIs, and thin loader adapters. A single root version produces all eight jars. GitHub Actions builds the matrix, creates version tags and GitHub releases, publishes each loader/version file to Modrinth project `f1yH9Ggq`, and synchronizes `MODRINTH.md`.

## Existing modules

- Maintenance mode and restart countdown
- Safe item/mob sweeps
- TPS alerts, reports, history, and Discord delivery
- Packet-based staff vanish
- Player inspection and item search
- Persistent player freeze
- Timed item bans and confiscation
- First-join alerts, announcements, join/leave messages, and rotating MOTDs
- LuckPerms and vanilla operator permission handling

## Next discussion

New gameplay/admin functionality will be selected separately after the multi-version release foundation is live. Feature work should preserve command/config compatibility and ship across all eight targets unless a Minecraft API does not exist on an older version.

Potential requests belong in the GitHub feature-request form so scope, permissions, configuration, loader parity, and acceptance criteria can be agreed before implementation.
