package com.fiw.common;

public final class WatchdogConfig {
    public boolean enabled = true;
    public int heartbeatTimeoutSeconds = 30;
    public int alertCooldownMinutes = 5;
    public boolean crashAlertOnBoot = true;
    public String notifyPermission = "fiw.watchdog.notify";
    public Discord discord = new Discord();

    public static WatchdogConfig defaults() {
        return new WatchdogConfig();
    }

    public static final class Discord {
        public boolean enabled = false;
        public String webhookUrl = "";
    }
}
