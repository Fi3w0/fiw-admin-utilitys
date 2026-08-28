package com.fiw.common;

import java.nio.file.Path;
import java.util.List;

public final class AlertService {
    private final FiwPlatform platform;
    private final JsonConfigStore<AlertConfig> configStore;
    private final JsonConfigStore<AlertHistory> historyStore;
    private AlertConfig config = AlertConfig.defaults();
    private AlertHistory history = new AlertHistory();

    public AlertService(FiwPlatform platform) {
        this.platform = platform;
        Path root = platform.configDirectory().resolve("fiw-admin");
        this.configStore = new JsonConfigStore<>(root.resolve("alert.json"), AlertConfig.class, AlertConfig.defaults());
        this.historyStore = new JsonConfigStore<>(root.resolve("alert-history.json"), AlertHistory.class, new AlertHistory());
    }

    public void reload() {
        config = configStore.load();
        history = historyStore.load();
    }

    public void recordAlert(double tps, List<String> reportLines) {
        if (config.history.enabled) {
            history.entries.add(new AlertHistory.Entry(System.currentTimeMillis(), tps, List.copyOf(reportLines)));
            int max = Math.max(1, config.history.maxEntries);
            while (history.entries.size() > max) {
                history.entries.remove(0);
            }
            historyStore.save(history);
        }
        if (config.discord.enabled) {
            DiscordWebhook.send(config.discord.webhookUrl, String.join("\n", reportLines), platform);
        }
    }

    public List<AlertHistory.Entry> history() {
        return List.copyOf(history.entries);
    }

    public AlertConfig config() {
        return config;
    }

    public boolean isEnabled() {
        return config.enabled;
    }

    public void setEnabled(boolean enabled) {
        config.enabled = enabled;
        configStore.save(config);
    }
}
