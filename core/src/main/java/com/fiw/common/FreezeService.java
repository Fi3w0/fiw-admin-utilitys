package com.fiw.common;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class FreezeService {
    private final JsonConfigStore<FreezeConfig> configStore;
    private final JsonConfigStore<FreezeState> stateStore;
    private FreezeConfig config = FreezeConfig.defaults();
    private FreezeState state = new FreezeState();

    public FreezeService(FiwPlatform platform) {
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

    public boolean isFrozen(UUID uuid) {
        return state.frozen.containsKey(uuid.toString());
    }

    /** Returns the new frozen state. */
    public boolean toggle(UUID uuid, String name) {
        String key = uuid.toString();
        boolean nowFrozen;
        if (state.frozen.containsKey(key)) {
            state.frozen.remove(key);
            nowFrozen = false;
        } else {
            state.frozen.put(key, name);
            nowFrozen = true;
        }
        stateStore.save(state);
        return nowFrozen;
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
    }
}