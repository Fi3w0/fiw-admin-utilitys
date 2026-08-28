package com.fiw.common;

public final class VanishConfig {
    public boolean enabled = true;
    public String usePermission = "fiw.vanish.use";
    public String seePermission = "fiw.vanish.see";
    public boolean opUseFallback = true;
    public boolean opSeeFallback = true;
    public boolean suppressJoinLeaveMessages = true;
    public boolean hideFromTab = true;
    public boolean hideEntity = true;
    public boolean hideFromServerListCount = true;
    public boolean hideFromLocatorBar = true;
    public String vanishedPrefix = "[V] ";

    public static VanishConfig defaults() {
        return new VanishConfig();
    }
}
