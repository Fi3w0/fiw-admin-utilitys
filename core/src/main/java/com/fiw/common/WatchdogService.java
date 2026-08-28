package com.fiw.common;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** MC-free: heartbeat-hang and dirty-shutdown (crash) alerting via log + Discord only, never in-game chat. */
public final class WatchdogService {
    private static final int CHECK_INTERVAL_SECONDS = 5;

    private final FiwPlatform platform;
    private final JsonConfigStore<WatchdogConfig> configStore;
    private final Path markerFile;
    private WatchdogConfig config = WatchdogConfig.defaults();
    private volatile long lastHeartbeatMillis = System.currentTimeMillis();
    private volatile long lastAlertMillis;
    private ScheduledExecutorService executor;

    public WatchdogService(FiwPlatform platform) {
        this.platform = platform;
        Path root = platform.configDirectory().resolve("fiw-admin");
        this.configStore = new JsonConfigStore<>(root.resolve("watchdog.json"), WatchdogConfig.class, WatchdogConfig.defaults());
        this.markerFile = root.resolve(".watchdog-running");
    }

    public void reload() {
        config = configStore.load();
    }

    public WatchdogConfig config() {
        return config;
    }

    public void recordHeartbeat() {
        lastHeartbeatMillis = System.currentTimeMillis();
    }

    public long heartbeatAgeSeconds() {
        return (System.currentTimeMillis() - lastHeartbeatMillis) / 1000L;
    }

    /** Call once from the server-started hook: reports an unclean previous shutdown, then arms the heartbeat monitor. */
    public void onServerStarted() {
        if (config.crashAlertOnBoot && Files.exists(markerFile)) {
            String message = "Server restarted after an unclean shutdown (crash, kill, or power loss) last session.";
            platform.warn(message);
            notifyDiscord(message);
        }
        writeMarker();
        recordHeartbeat();
        if (executor == null) {
            executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
                Thread thread = new Thread(runnable, "fiw-watchdog");
                thread.setDaemon(true);
                return thread;
            });
            executor.scheduleAtFixedRate(this::checkHeartbeat, CHECK_INTERVAL_SECONDS, CHECK_INTERVAL_SECONDS, TimeUnit.SECONDS);
        }
    }

    /** Call once from the server-stopping hook: marks a clean shutdown and stops the heartbeat monitor. */
    public void onServerStopping() {
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
        try {
            Files.deleteIfExists(markerFile);
        } catch (IOException exception) {
            platform.warn("Failed to remove watchdog marker file: " + exception.getMessage());
        }
    }

    private void checkHeartbeat() {
        if (!config.enabled) {
            return;
        }
        long ageSeconds = heartbeatAgeSeconds();
        if (ageSeconds < config.heartbeatTimeoutSeconds) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastAlertMillis < config.alertCooldownMinutes * 60_000L) {
            return;
        }
        lastAlertMillis = now;
        String message = "Server watchdog: no tick heartbeat for " + ageSeconds + "s (threshold "
                + config.heartbeatTimeoutSeconds + "s). The server may be hanging.";
        platform.warn(message);
        notifyDiscord(message);
    }

    private void writeMarker() {
        try {
            Files.createDirectories(markerFile.getParent());
            Files.writeString(markerFile, Long.toString(System.currentTimeMillis()));
        } catch (IOException exception) {
            platform.warn("Failed to write watchdog marker file: " + exception.getMessage());
        }
    }

    private void notifyDiscord(String content) {
        if (config.discord.enabled) {
            DiscordWebhook.send(config.discord.webhookUrl, content, platform);
        }
    }
}
