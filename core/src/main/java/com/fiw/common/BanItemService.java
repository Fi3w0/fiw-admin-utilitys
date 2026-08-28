package com.fiw.common;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BanItemService {
    private final JsonConfigStore<BanItemConfig> configStore;
    private final JsonConfigStore<BanState> stateStore;
    private BanItemConfig config = BanItemConfig.defaults();
    private BanState state = new BanState();

    public BanItemService(FiwPlatform platform) {
        Path root = platform.configDirectory().resolve("fiw-admin");
        this.configStore = new JsonConfigStore<>(root.resolve("banitem.json"), BanItemConfig.class, BanItemConfig.defaults());
        this.stateStore = new JsonConfigStore<>(root.resolve("banned-items.json"), BanState.class, new BanState());
    }

    public void reload() {
        config = configStore.load();
        state = stateStore.load();
    }

    public BanItemConfig config() {
        return config;
    }

    public boolean isBanned(String itemId) {
        Long expiry = state.bans.get(itemId);
        if (expiry == null) {
            return false;
        }
        if (expiry != 0 && expiry <= System.currentTimeMillis()) {
            state.bans.remove(itemId);
            stateStore.save(state);
            return false;
        }
        return true;
    }

    /** durationSeconds 0 = until unbanned. */
    public void ban(String itemId, int durationSeconds) {
        long expiry = durationSeconds <= 0 ? 0 : System.currentTimeMillis() + durationSeconds * 1000L;
        state.bans.put(itemId, expiry);
        stateStore.save(state);
    }

    public void unban(String itemId) {
        if (state.bans.remove(itemId) != null) {
            stateStore.save(state);
        }
    }

    /** Removes expired bans and returns their ids. */
    public List<String> purgeExpired() {
        long now = System.currentTimeMillis();
        List<String> expired = new ArrayList<>();
        state.bans.entrySet().removeIf(entry -> {
            if (entry.getValue() != 0 && entry.getValue() <= now) {
                expired.add(entry.getKey());
                return true;
            }
            return false;
        });
        if (!expired.isEmpty()) {
            stateStore.save(state);
        }
        return expired;
    }

    public Map<String, Long> bans() {
        return state.bans;
    }

    public int activeBanCount() {
        return state.bans.size();
    }

    public static final class BanState {
        public Map<String, Long> bans = new LinkedHashMap<>();
    }
}