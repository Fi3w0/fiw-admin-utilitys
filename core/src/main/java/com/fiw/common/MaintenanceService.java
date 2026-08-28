package com.fiw.common;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.UUID;

public final class MaintenanceService {
    private static final String CONFIG_DIR = "fiw-admin";
    private static final String FLAG_FILE = "maintenance.flag";

    private final FiwPlatform platform;
    private final JsonConfigStore<MaintenanceConfig> configStore;
    private final Path flagPath;

    private MaintenanceConfig config = MaintenanceConfig.defaults();
    private boolean enabled;
    private String message = config.defaultMessage;

    public MaintenanceService(FiwPlatform platform) {
        this.platform = platform;
        Path root = platform.configDirectory().resolve(CONFIG_DIR);
        this.configStore = new JsonConfigStore<>(root.resolve("maintenance.json"), MaintenanceConfig.class, MaintenanceConfig.defaults());
        this.flagPath = root.resolve(FLAG_FILE);
    }

    public void reload() {
        config = configStore.load();
        loadFlag();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public MaintenanceConfig config() {
        return config;
    }

    public String currentMessage() {
        return message;
    }

    public String currentKickMessage() {
        return TextFormat.legacyColors(message);
    }

    public String currentMotd() {
        return TextFormat.legacyColors(config.motdMessage);
    }

    public String enable(String requestedMessage) {
        if (!config.enabled) {
            return "Maintenance module is disabled in maintenance.json.";
        }

        String nextMessage = requestedMessage == null || requestedMessage.isBlank()
                ? config.defaultMessage
                : requestedMessage;
        enabled = true;
        message = nextMessage;
        writeFlag(nextMessage);
        return "Maintenance mode enabled.";
    }

    public String disable() {
        enabled = false;
        message = config.defaultMessage;
        deleteFlag();
        return "Maintenance mode disabled.";
    }

    public boolean isAllowlisted(String name, UUID uuid) {
        String normalizedName = name == null ? "" : name.toLowerCase(Locale.ROOT);
        if (config.allowlistNames.stream().map(value -> value.toLowerCase(Locale.ROOT)).anyMatch(normalizedName::equals)) {
            return true;
        }

        String normalizedUuid = uuid == null ? "" : uuid.toString().toLowerCase(Locale.ROOT);
        return config.allowlistUuids.stream()
                .map(value -> value.toLowerCase(Locale.ROOT))
                .anyMatch(normalizedUuid::equals);
    }

    public boolean shouldKick(String name, UUID uuid, boolean hasBypassPermission) {
        if (!enabled) {
            return false;
        }
        return !hasBypassPermission && !isAllowlisted(name, uuid);
    }

    private void loadFlag() {
        enabled = Files.exists(flagPath);
        message = config.defaultMessage;
        if (!enabled) {
            return;
        }

        try {
            String savedMessage = Files.readString(flagPath, StandardCharsets.UTF_8);
            if (!savedMessage.isBlank()) {
                message = savedMessage;
            }
        } catch (IOException exception) {
            platform.warn("Failed to read maintenance flag: " + exception.getMessage());
        }
    }

    private void writeFlag(String value) {
        try {
            Files.createDirectories(flagPath.getParent());
            Files.writeString(flagPath, value, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to write maintenance flag " + flagPath, exception);
        }
    }

    private void deleteFlag() {
        try {
            Files.deleteIfExists(flagPath);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to delete maintenance flag " + flagPath, exception);
        }
    }
}
