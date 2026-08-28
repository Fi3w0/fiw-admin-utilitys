package com.fiw.common;

public final class AfkConfig {
    public boolean enabled = true;
    public int idleThresholdSeconds = 300;
    public int kickAfterSeconds = 0;
    public String tag = "&7[AFK]&r";
    public String exemptPermission = "fiw.afk.exempt";
    public boolean broadcastOnChange = true;
    public String afkMessage = "&e{player} is now AFK.";
    public String backMessage = "&e{player} is no longer AFK.";
    public String kickMessage = "&cKicked for being AFK too long.";

    public static AfkConfig defaults() {
        return new AfkConfig();
    }
}
