package com.fiw.common;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ReportService {
    private final FiwPlatform platform;
    private final JsonConfigStore<ReportConfig> configStore;
    private final JsonConfigStore<ReportState> stateStore;
    private ReportConfig config = ReportConfig.defaults();
    private ReportState state = new ReportState();
    private final Map<UUID, Long> lastReportMillis = new HashMap<>();

    public ReportService(FiwPlatform platform) {
        this.platform = platform;
        Path root = platform.configDirectory().resolve("fiw-admin");
        this.configStore = new JsonConfigStore<>(root.resolve("report.json"), ReportConfig.class, ReportConfig.defaults());
        this.stateStore = new JsonConfigStore<>(root.resolve("reports.json"), ReportState.class, new ReportState());
    }

    public void reload() {
        config = configStore.load();
        state = stateStore.load();
    }

    public ReportConfig config() {
        return config;
    }

    public void notifyDiscord(String content) {
        if (config.discord.enabled) {
            DiscordWebhook.send(config.discord.webhookUrl, content, platform);
        }
    }

    /** Returns the remaining cooldown in seconds, or 0 when the player may report now. */
    public int cooldownRemaining(UUID reporter) {
        Long last = lastReportMillis.get(reporter);
        if (last == null) {
            return 0;
        }
        long elapsedSeconds = (System.currentTimeMillis() - last) / 1000L;
        long remaining = config.cooldownSeconds - elapsedSeconds;
        return remaining > 0 ? (int) remaining : 0;
    }

    public Report submit(UUID reporterUuid, String reporterName, String targetName, String reason) {
        lastReportMillis.put(reporterUuid, System.currentTimeMillis());
        int id = state.nextId++;
        Report report = new Report(id, reporterUuid.toString(), reporterName, targetName, reason, System.currentTimeMillis(), "OPEN", "");
        state.reports.add(report);
        stateStore.save(state);
        return report;
    }

    public List<Report> openReports() {
        List<Report> open = new ArrayList<>();
        for (Report report : state.reports) {
            if (!"RESOLVED".equals(report.status)) {
                open.add(report);
            }
        }
        return open;
    }

    public Report findById(int id) {
        for (Report report : state.reports) {
            if (report.id == id) {
                return report;
            }
        }
        return null;
    }

    public boolean claim(int id, String staffName) {
        Report report = findById(id);
        if (report == null || "RESOLVED".equals(report.status)) {
            return false;
        }
        report.status = "CLAIMED";
        report.claimedBy = staffName;
        stateStore.save(state);
        return true;
    }

    public boolean resolve(int id) {
        Report report = findById(id);
        if (report == null) {
            return false;
        }
        report.status = "RESOLVED";
        stateStore.save(state);
        return true;
    }

    public static final class ReportState {
        public int nextId = 1;
        public List<Report> reports = new ArrayList<>();
    }

    public static final class Report {
        public int id;
        public String reporterUuid = "";
        public String reporterName = "";
        public String targetName = "";
        public String reason = "";
        public long timestampMillis;
        public String status = "OPEN";
        public String claimedBy = "";

        public Report() {
        }

        public Report(int id, String reporterUuid, String reporterName, String targetName, String reason,
                       long timestampMillis, String status, String claimedBy) {
            this.id = id;
            this.reporterUuid = reporterUuid;
            this.reporterName = reporterName;
            this.targetName = targetName;
            this.reason = reason == null ? "" : reason;
            this.timestampMillis = timestampMillis;
            this.status = status;
            this.claimedBy = claimedBy;
        }
    }
}
