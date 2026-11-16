# Contributing to Fiw Admin Tools

## Getting started

1. Fork and clone the repository.
2. Run `./gradlew build` with JDK 25 as the Gradle runtime.
3. Keep loader/version adapters thin and put Minecraft-free behavior in `core`.

## Supported targets

Every behavior change must keep parity across all twelve release jars:

```text
Fabric 26.3        NeoForge 26.3
Fabric 26.1.2      NeoForge 26.1.2
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
- Do not add co-author trailers to commits.

## Automated gameplay checks

With Node 22+ installed, run `./gradlew playerTest -PsmokeTarget=<module>` to test the built
production jar in a disposable local server, then repeat after a restart. CI runs this check
for every target and retains logs on failure. Minecraft 26.3 gets the startup and console checks
only, until the test client supports it. Output is under `build/smoke/<module>`.

## Pull requests

- Run `./gradlew build` before opening the PR.
- Update `README.md`, `MODRINTH.md`, and `CHANGELOG.md` for player-visible changes.
- Explain which loader/version targets were runtime-tested.
