package com.fiw.common;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class JsonConfigStore<T> {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private final Path path;
    private final Class<T> type;
    private final T defaults;

    public JsonConfigStore(Path path, Class<T> type, T defaults) {
        this.path = path;
        this.type = type;
        this.defaults = defaults;
    }

    public T load() {
        ensureParent();
        if (Files.notExists(path)) {
            save(defaults);
            return defaults;
        }

        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            T value = GSON.fromJson(reader, type);
            if (value == null) {
                save(defaults);
                return defaults;
            }
            return value;
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException("Failed to load config " + path, exception);
        }
    }

    public void save(T value) {
        ensureParent();
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(value, writer);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to save config " + path, exception);
        }
    }

    private void ensureParent() {
        try {
            Files.createDirectories(path.getParent());
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to create config directory " + path.getParent(), exception);
        }
    }
}
