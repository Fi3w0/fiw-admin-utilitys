package com.fiw.common;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Holds config, the persisted alert log, and generic (MC-free) rolling detection state.
 * Callers supply plain values (a key, a weighted total, a signature string) computed from real
 * inventories by the loader-aware AdminRuntime; this class never touches Minecraft types.
 */
public final class DupeService {
    private static final int MAX_ALERTS = 200;

    private final FiwPlatform platform;
    private final JsonConfigStore<DupeConfig> configStore;
    private final JsonConfigStore<DupeAlertLog> alertStore;
    private DupeConfig config = DupeConfig.defaults();
    private DupeAlertLog alerts = new DupeAlertLog();

    private final Map<String, Deque<HistoryPoint>> rateHistory = new HashMap<>();
    private final Map<String, SignatureRecord> signatures = new HashMap<>();

    public DupeService(FiwPlatform platform) {
        this.platform = platform;
        Path root = platform.configDirectory().resolve("fiw-admin");
        this.configStore = new JsonConfigStore<>(root.resolve("dupe.json"), DupeConfig.class, DupeConfig.defaults());
        this.alertStore = new JsonConfigStore<>(root.resolve("dupe-alerts.json"), DupeAlertLog.class, new DupeAlertLog());
    }

    public void reload() {
        config = configStore.load();
        alerts = alertStore.load();
    }

    public DupeConfig config() {
        return config;
    }

    public void notifyDiscord(String content) {
        if (config.discord.enabled) {
            DiscordWebhook.send(config.discord.webhookUrl, content, platform);
        }
    }

    public void recordAlert(String detector, String detail) {
        alerts.entries.add(new DupeAlertLog.Entry(System.currentTimeMillis(), detector, detail));
        while (alerts.entries.size() > MAX_ALERTS) {
            alerts.entries.remove(0);
        }
        alertStore.save(alerts);
    }

    public List<DupeAlertLog.Entry> recentAlerts() {
        return List.copyOf(alerts.entries);
    }

    /** Records a weighted-value sample for {@code key} (a player UUID or a chunk key) and returns the increase over the window. */
    public double rateIncrease(String key, double currentValue, int windowSeconds) {
        long now = System.currentTimeMillis();
        Deque<HistoryPoint> history = rateHistory.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        history.addLast(new HistoryPoint(now, currentValue));
        while (!history.isEmpty() && now - history.peekFirst().timestampMillis() > windowSeconds * 1000L) {
            history.removeFirst();
        }
        return history.isEmpty() ? 0 : currentValue - history.peekFirst().value();
    }

    /**
     * Records that {@code holder} currently has {@code signature} and returns the previously
     * recorded holder if it was a *different* holder seen within the last couple of scans
     * (a likely simultaneous duplicate), or null otherwise.
     */
    public String checkAndUpdateSignature(String signature, String holder, int scanIntervalSeconds) {
        SignatureRecord previous = signatures.put(signature, new SignatureRecord(holder, System.currentTimeMillis()));
        if (previous == null || previous.holder().equals(holder)) {
            return null;
        }
        long overlapWindowMillis = Math.max(scanIntervalSeconds * 2L, 10L) * 1000L;
        if (System.currentTimeMillis() - previous.timestampMillis() <= overlapWindowMillis) {
            return previous.holder();
        }
        return null;
    }

    /** Clears rolling rate-detector history for a key (player UUID or chunk key) — the false-positive escape hatch. */
    public void clearHistory(String key) {
        rateHistory.remove(key);
    }

    private record HistoryPoint(long timestampMillis, double value) {
    }

    private record SignatureRecord(String holder, long timestampMillis) {
    }

    public static final class DupeAlertLog {
        public List<Entry> entries = new ArrayList<>();

        public static final class Entry {
            public long timestampMillis;
            public String detector = "";
            public String detail = "";

            public Entry() {
            }

            public Entry(long timestampMillis, String detector, String detail) {
                this.timestampMillis = timestampMillis;
                this.detector = detector;
                this.detail = detail;
            }
        }
    }
}
