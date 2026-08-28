package com.fiw.common;

public final class BanItemConfig {
    public boolean enabled = true;
    public String blockedMessage = "&cThis item is disabled on this server.";
    public String bypassPermission = "fiw.banitem.bypass";
    public boolean confiscateFromInventory = true;

    public static BanItemConfig defaults() {
        return new BanItemConfig();
    }
}