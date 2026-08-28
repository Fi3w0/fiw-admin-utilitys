package com.fiw.common;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class InspectService {
    private final JsonConfigStore<InspectConfig> configStore;
    private final JsonConfigStore<SeenData> seenStore;
    private InspectConfig config = InspectConfig.defaults();
    private SeenData seen = new SeenData();

    public InspectService(FiwPlatform platform) {
        Path root = platform.configDirectory().resolve("fiw-admin");
        this.configStore = new JsonConfigStore<>(root.resolve("inspect.json"), InspectConfig.class, InspectConfig.defaults());
        this.seenStore = new JsonConfigStore<>(root.resolve("player-seen.json"), SeenData.class, new SeenData());
    }

    public void reload() {
        config = configStore.load();
        seen = seenStore.load();
    }

    public InspectConfig config() {
        return config;
    }

    /** Returns true when this player has never been seen before. */
    public boolean recordJoin(UUID uuid, String name) {
        long now = System.currentTimeMillis();
        boolean firstJoin = !seen.players.containsKey(uuid.toString());
        Seen entry = seen.players.computeIfAbsent(uuid.toString(), ignored -> {
            Seen created = new Seen();
            created.firstSeenMillis = now;
            return created;
        });
        entry.name = name;
        entry.lastSeenMillis = now;
        seenStore.save(seen);
        return firstJoin;
    }

    public void recordLeave(UUID uuid) {
        Seen entry = seen.players.get(uuid.toString());
        if (entry != null) {
            entry.lastSeenMillis = System.currentTimeMillis();
            seenStore.save(seen);
        }
    }

    public Seen seenInfo(UUID uuid) {
        return seen.players.get(uuid.toString());
    }

    public static final class SeenData {
        public Map<String, Seen> players = new LinkedHashMap<>();
    }

    public static final class Seen {
        public String name = "";
        public long firstSeenMillis;
        public long lastSeenMillis;
    }
}