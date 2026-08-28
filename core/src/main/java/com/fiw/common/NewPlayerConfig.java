package com.fiw.common;

public final class NewPlayerConfig {
    public boolean enabled = true;
    public String notifyPermission = "fiw.alert.notify";
    public String adminMessage = "&e⭐ New player joined for the first time: &b{player}";
    public String broadcastMessage = "";
    public boolean discordEnabled = false;
    public String discordWebhookUrl = "";
    public String discordMessage = "⭐ New player: {player}";

    public static NewPlayerConfig defaults() {
        return new NewPlayerConfig();
    }
}