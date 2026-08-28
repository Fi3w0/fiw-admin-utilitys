package com.fiw.common;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class FreezeService {
    private final FiwPlatform platform;
    private final JsonConfigStore<FreezeConfig> configStore;
    private final JsonConfigStore<FreezeState> stateStore;
    private FreezeConfig config = FreezeConfig.defaults();
    private FreezeState state = new FreezeState();

    public FreezeService(FiwPlatform platform) {
        this.platform = platform;
        Path root = platform.configDirectory().resolve("fiw-admin");
        this.configStore = new JsonConfigStore<>(root.resolve("freeze.json"), FreezeConfig.class, FreezeConfig.defaults());
        this.stateStore = new JsonConfigStore<>(root.resolve("frozen.json"), FreezeState.class, new FreezeState());
    }

    public void reload() {
        config = configStore.load();
        state = stateStore.load();
    }

    public FreezeConfig config() {
        return config;
    }

    public void notifyDiscord(String content) {
        if (config.discord.enabled) {
            DiscordWebhook.send(config.discord.webhookUrl, content, platform);
        }
    }

    public boolean isFrozen(UUID uuid) {
        return state.frozen.containsKey(uuid.toString());
    }

    /** durationSeconds 0 = manual unfreeze only. */
    public void freeze(UUID uuid, String name, String staffName, String reason, int durationSeconds, String evidence) {
        String key = uuid.toString();
        state.frozen.put(key, name);
        long expiry = durationSeconds <= 0 ? 0 : System.currentTimeMillis() + durationSeconds * 1000L;
        state.details.put(key, new FreezeDetail(reason == null ? "" : reason, staffName,
                System.currentTimeMillis(), expiry, evidence == null ? "" : evidence));
        stateStore.save(state);
    }

    /** Returns true when the player was frozen and is now unfrozen. */
    public boolean unfreeze(UUID uuid) {
        String key = uuid.toString();
        boolean wasFrozen = state.frozen.remove(key) != null;
        state.details.remove(key);
        if (wasFrozen) {
            stateStore.save(state);
        }
        return wasFrozen;
    }

    public FreezeDetail detail(UUID uuid) {
        return state.details.get(uuid.toString());
    }

    /** Frozen uuids whose auto-unfreeze duration has elapsed. */
    public List<UUID> expiredUuids() {
        long now = System.currentTimeMillis();
        List<UUID> expired = new ArrayList<>();
        for (Map.Entry<String, FreezeDetail> entry : state.details.entrySet()) {
            if (entry.getValue().expiryMillis != 0 && entry.getValue().expiryMillis <= now) {
                try {
                    expired.add(UUID.fromString(entry.getKey()));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        return expired;
    }

    public Set<Map.Entry<String, String>> frozenEntries() {
        return state.frozen.entrySet();
    }

    public int frozenCount() {
        return state.frozen.size();
    }

    public List<UUID> frozenUuids() {
        List<UUID> uuids = new ArrayList<>();
        for (String key : state.frozen.keySet()) {
            try {
                uuids.add(UUID.fromString(key));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return uuids;
    }

    public static final class FreezeState {
        public Map<String, String> frozen = new LinkedHashMap<>();
        public Map<String, FreezeDetail> details = new LinkedHashMap<>();
    }

    public static final class FreezeDetail {
        public String reason = "";
        public String staffName = "";
        public long frozenAtMillis;
        public long expiryMillis;
        public String evidence = "";

        public FreezeDetail() {
        }

        public FreezeDetail(String reason, String staffName, long frozenAtMillis, long expiryMillis, String evidence) {
            this.reason = reason;
            this.staffName = staffName;
            this.frozenAtMillis = frozenAtMillis;
            this.expiryMillis = expiryMillis;
            this.evidence = evidence;
        }
    }
}