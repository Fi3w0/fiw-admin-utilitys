package com.fiw.common;

public final class AlertConfig {
    public boolean enabled = true;
    public double tpsThreshold = 15.0;
    public int checkIntervalSeconds = 5;
    public int cooldownMinutes = 5;
    public double escalateBelowTps = 8.0;
    public boolean playSound = true;
    public Report report = new Report();
    public Discord discord = new Discord();
    public History history = new History();
    public String notifyPermission = "fiw.alert.notify";

    public static AlertConfig defaults() {
        return new AlertConfig();
    }

    public static final class Report {
        public int topChunks = 3;
        public boolean scanEntities = true;
        public boolean scanBlockEntities = true;
        public boolean clickableTeleport = true;
        public boolean clickableSweep = true;
    }

    public static final class Discord {
        public boolean enabled = false;
        public String webhookUrl = "";
    }

    public static final class History {
        public boolean enabled = true;
        public int maxEntries = 30;
    }
}
