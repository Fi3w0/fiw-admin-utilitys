package com.fiw.admin.common;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.LongSupplier;

/** Purely runtime (in-memory) — AFK state resets on restart, so no state file is persisted. */
public final class AfkService {
    private final JsonConfigStore<AfkConfig> configStore;
    private final LongSupplier clock;
    private AfkConfig config = AfkConfig.defaults();
    private final Map<UUID, Long> lastActivityMillis = new HashMap<>();
    private final Set<UUID> afk = new HashSet<>();
    private final Set<UUID> manuallyMarked = new HashSet<>();
    private final Set<UUID> pendingChanges = new HashSet<>();
    private final Map<UUID, Rotation> rotations = new HashMap<>();

    public AfkService(FiwPlatform platform) {
        this(platform, System::currentTimeMillis);
    }

    AfkService(FiwPlatform platform, LongSupplier clock) {
        this.clock = clock;
        Path root = platform.configDirectory().resolve("fiw-admin");
        this.configStore = new JsonConfigStore<>(root.resolve("afk.json"), AfkConfig.class, AfkConfig.defaults());
    }

    public void reload() {
        config = configStore.load();
    }

    public AfkConfig config() {
        return config;
    }

    public void recordActivity(UUID uuid) {
        lastActivityMillis.put(uuid, clock.getAsLong());
        manuallyMarked.remove(uuid);
        if (afk.remove(uuid)) pendingChanges.add(uuid);
    }

    /** Camera motion counts as input; water, pistons and teleporting do not. */
    public void observeRotation(UUID uuid, float yaw, float pitch) {
        Rotation previous = rotations.put(uuid, new Rotation(yaw, pitch));
        if (previous != null && (previous.yaw() != yaw || previous.pitch() != pitch)) {
            recordActivity(uuid);
        }
    }

    public boolean tracks(UUID uuid) {
        return lastActivityMillis.containsKey(uuid);
    }

    public void forget(UUID uuid) {
        lastActivityMillis.remove(uuid);
        afk.remove(uuid);
        manuallyMarked.remove(uuid);
        pendingChanges.remove(uuid);
        rotations.remove(uuid);
    }

    public boolean isAfk(UUID uuid) {
        return config.enabled && afk.contains(uuid);
    }

    /** Toggles a manual "brb" AFK mark; returns the new state. */
    public boolean toggleManual(UUID uuid) {
        if (!config.enabled) return false;
        pendingChanges.add(uuid);
        if (manuallyMarked.contains(uuid)) {
            manuallyMarked.remove(uuid);
            afk.remove(uuid);
            lastActivityMillis.put(uuid, clock.getAsLong());
            return false;
        }
        manuallyMarked.add(uuid);
        afk.add(uuid);
        return true;
    }

    public Set<UUID> afkPlayers() {
        return config.enabled ? Set.copyOf(afk) : Set.of();
    }

    /** Seconds since the player's last recorded activity, or -1 when never recorded. */
    public long idleSeconds(UUID uuid) {
        Long last = lastActivityMillis.get(uuid);
        if (last == null) {
            return -1;
        }
        return (clock.getAsLong() - last) / 1000L;
    }

    /** Re-evaluates idle players against the threshold; returns uuids whose AFK state just changed (for broadcast/tag refresh). */
    public Set<UUID> refresh() {
        Set<UUID> changed = new HashSet<>(pendingChanges);
        pendingChanges.clear();
        if (!config.enabled) {
            changed.addAll(afk);
            afk.clear();
            manuallyMarked.clear();
            lastActivityMillis.replaceAll((uuid, last) -> clock.getAsLong());
            return changed;
        }
        for (Map.Entry<UUID, Long> entry : lastActivityMillis.entrySet()) {
            UUID uuid = entry.getKey();
            if (manuallyMarked.contains(uuid)) {
                continue;
            }
            long idleSeconds = (clock.getAsLong() - entry.getValue()) / 1000L;
            boolean shouldBeAfk = idleSeconds >= config.idleThresholdSeconds;
            boolean currentlyAfk = afk.contains(uuid);
            if (shouldBeAfk && !currentlyAfk) {
                afk.add(uuid);
                changed.add(uuid);
            } else if (!shouldBeAfk && currentlyAfk) {
                afk.remove(uuid);
                changed.add(uuid);
            }
        }
        return changed;
    }

    private record Rotation(float yaw, float pitch) {}
}
