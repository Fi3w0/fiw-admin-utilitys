# Contributing to Fiw Admin Tools

## Getting started

1. Fork and clone the repository.
2. Run `./gradlew build` with JDK 21 as the Gradle runtime.
3. Keep loader/version adapters thin and put Minecraft-free behavior in `core`.

## Supported targets

Every behavior change must keep parity across all eight release jars:

```text
Fabric 1.21.11     NeoForge 1.21.11
Fabric 1.21.8      NeoForge 1.21.8
Fabric 1.21.1      NeoForge 1.21.1
Fabric 1.20.1      Forge 1.20.1
```

Implement and verify version work newest to oldest. Use official Mojang mappings for the exact target; do not guess APIs.

## Code conventions

- Java uses 4 spaces; Kotlin follows the existing tab-indented adapter style.
- Keep config/state behavior in `core` and loader events in `fabric-*`, `neoforge-*`, or `forge-*`.
- Preserve the server-only design and public command/config compatibility.
- Do not add AI co-author trailers. Commits must be authored by a human.

## Pull requests

- Run `./gradlew build` before opening the PR.
- Update `README.md`, `MODRINTH.md`, and `CHANGELOG.md` for player-visible changes.
- Explain which loader/version targets were runtime-tested.
