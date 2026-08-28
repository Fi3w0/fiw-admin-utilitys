package com.fiw.common;

public final class FreezeConfig {
    public boolean enabled = true;
    public boolean blockInteractions = true;
    public boolean notifyTarget = true;
    public String frozenMessage = "&cYou have been frozen by an admin.";
    public String unfrozenMessage = "&aYou have been unfrozen.";

    public static FreezeConfig defaults() {
        return new FreezeConfig();
    }
}