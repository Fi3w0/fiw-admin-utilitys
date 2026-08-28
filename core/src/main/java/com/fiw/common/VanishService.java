package com.fiw.common;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class VanishService {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final JsonConfigStore<VanishConfig> configStore;
    private final Path statePath;
    private VanishConfig config = VanishConfig.defaults();
    private Set<UUID> vanished = new LinkedHashSet<>();
    private Map<UUID, String> knownNames = new LinkedHashMap<>();

    public VanishService(FiwPlatform platform) {
        Path root = platform.configDirectory().resolve("fiw-admin");
        this.configStore = new JsonConfigStore<>(root.resolve("vanish.json"), VanishConfig.class, VanishConfig.defaults());
        this.statePath = root.resolve("vanished-players.json");
    }

    public void reload() {
        config = configStore.load();
        loadState();
    }

    public VanishConfig config() {
        return config;
    }

    public boolean isVanished(UUID uuid) {
        return vanished.contains(uuid);
    }

    public boolean toggle(UUID uuid) {
        return toggle(uuid, null);
    }

    public boolean toggle(UUID uuid, String name) {
        boolean nowVanished;
        if (vanished.contains(uuid)) {
            vanished.remove(uuid);
            nowVanished = false;
        } else {
            vanished.add(uuid);
            rememberName(uuid, name);
            nowVanished = true;
        }
        saveState();
        return nowVanished;
    }

    public void setVanished(UUID uuid, boolean value) {
        setVanished(uuid, null, value);
    }

    public void setVanished(UUID uuid, String name, boolean value) {
        if (value) {
            vanished.add(uuid);
            rememberName(uuid, name);
        } else {
            vanished.remove(uuid);
        }
        saveState();
    }

    public void rememberName(UUID uuid, String name) {
        if (name != null && !name.isBlank()) {
            knownNames.put(uuid, name);
        }
    }

    public String knownName(UUID uuid) {
        return knownNames.get(uuid);
    }

    public Map<UUID, String> knownNames() {
        return Map.copyOf(knownNames);
    }

    public int vanishedCount() {
        return vanished.size();
    }

    public Set<UUID> vanishedPlayers() {
        return Set.copyOf(vanished);
    }

    private void loadState() {
        if (Files.notExists(statePath)) {
            saveState();
            return;
        }

        try (Reader reader = Files.newBufferedReader(statePath, StandardCharsets.UTF_8)) {
            VanishState state = GSON.fromJson(reader, VanishState.class);
            vanished = state == null || state.players == null ? new LinkedHashSet<>() : new LinkedHashSet<>(state.players);
            knownNames = new LinkedHashMap<>();
            if (state != null && state.names != null) {
                for (Map.Entry<String, String> entry : state.names.entrySet()) {
                    try {
                        knownNames.put(UUID.fromString(entry.getKey()), entry.getValue());
                    } catch (IllegalArgumentException ignored) {
                        // Ignore stale/corrupt name entries; the UUID set is authoritative.
                    }
                }
            }
        } catch (IOException | RuntimeException exception) {
            vanished = new LinkedHashSet<>();
            knownNames = new LinkedHashMap<>();
        }
    }

    private void saveState() {
        try {
            Files.createDirectories(statePath.getParent());
            try (Writer writer = Files.newBufferedWriter(statePath, StandardCharsets.UTF_8)) {
                VanishState state = new VanishState();
                state.players = new LinkedHashSet<>(vanished);
                state.names = new LinkedHashMap<>();
                for (Map.Entry<UUID, String> entry : knownNames.entrySet()) {
                    state.names.put(entry.getKey().toString(), entry.getValue());
                }
                GSON.toJson(state, writer);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to save vanish state " + statePath, exception);
        }
    }

    private static final class VanishState {
        Set<UUID> players = new LinkedHashSet<>();
        Map<String, String> names = new LinkedHashMap<>();
    }
}
