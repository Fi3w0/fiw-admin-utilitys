package com.fiw.common;

public final class FreezeConfig {
    public boolean enabled = true;
    public boolean blockInteractions = true;
    public boolean notifyTarget = true;
    public String frozenMessage = "&cYou have been frozen by an admin.";
    public String unfrozenMessage = "&aYou have been unfrozen.";
    public boolean reasonRequired = false;
    public int autoUnfreezeSeconds = 0;
    public boolean teleportToStaffOnFreeze = false;
    public boolean evidenceLogging = true;
    public Discord discord = new Discord();

    public static FreezeConfig defaults() {
        return new FreezeConfig();
    }

    public static final class Discord {
        public boolean enabled = false;
        public String webhookUrl = "";
    }
}
