package com.fiw.common;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Purely runtime (in-memory) — AFK state resets on restart, so no state file is persisted. */
public final class AfkService {
    private final JsonConfigStore<AfkConfig> configStore;
    private AfkConfig config = AfkConfig.defaults();
    private final Map<UUID, Long> lastActivityMillis = new HashMap<>();
    private final Set<UUID> afk = new HashSet<>();
    private final Set<UUID> manuallyMarked = new HashSet<>();

    public AfkService(FiwPlatform platform) {
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
        lastActivityMillis.put(uuid, System.currentTimeMillis());
        if (manuallyMarked.remove(uuid)) {
            afk.remove(uuid);
        }
    }

    public void forget(UUID uuid) {
        lastActivityMillis.remove(uuid);
        afk.remove(uuid);
        manuallyMarked.remove(uuid);
    }

    public boolean isAfk(UUID uuid) {
        return afk.contains(uuid);
    }

    /** Toggles a manual "brb" AFK mark; returns the new state. */
    public boolean toggleManual(UUID uuid) {
        if (manuallyMarked.contains(uuid)) {
            manuallyMarked.remove(uuid);
            afk.remove(uuid);
            lastActivityMillis.put(uuid, System.currentTimeMillis());
            return false;
        }
        manuallyMarked.add(uuid);
        afk.add(uuid);
        return true;
    }

    public Set<UUID> afkPlayers() {
        return Set.copyOf(afk);
    }

    /** Seconds since the player's last recorded activity, or -1 when never recorded. */
    public long idleSeconds(UUID uuid) {
        Long last = lastActivityMillis.get(uuid);
        if (last == null) {
            return -1;
        }
        return (System.currentTimeMillis() - last) / 1000L;
    }

    /** Re-evaluates idle players against the threshold; returns uuids whose AFK state just changed (for broadcast/tag refresh). */
    public Set<UUID> refresh() {
        Set<UUID> changed = new HashSet<>();
        if (!config.enabled) {
            return changed;
        }
        for (Map.Entry<UUID, Long> entry : lastActivityMillis.entrySet()) {
            UUID uuid = entry.getKey();
            if (manuallyMarked.contains(uuid)) {
                continue;
            }
            long idleSeconds = (System.currentTimeMillis() - entry.getValue()) / 1000L;
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
}
