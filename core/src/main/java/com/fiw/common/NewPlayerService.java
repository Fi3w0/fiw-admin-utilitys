package com.fiw.common;

import java.nio.file.Path;

public final class NewPlayerService {
    private final FiwPlatform platform;
    private final JsonConfigStore<NewPlayerConfig> configStore;
    private NewPlayerConfig config = NewPlayerConfig.defaults();

    public NewPlayerService(FiwPlatform platform) {
        this.platform = platform;
        Path root = platform.configDirectory().resolve("fiw-admin");
        this.configStore = new JsonConfigStore<>(root.resolve("newplayer.json"), NewPlayerConfig.class, NewPlayerConfig.defaults());
    }

    public void reload() {
        config = configStore.load();
    }

    public NewPlayerConfig config() {
        return config;
    }

    /** Sends the Discord notification; falls back to the given webhook URL when this module has none configured. */
    public void notifyDiscord(String playerName, String fallbackWebhookUrl) {
        if (!config.discordEnabled) {
            return;
        }
        String url = config.discordWebhookUrl.isBlank() ? fallbackWebhookUrl : config.discordWebhookUrl;
        DiscordWebhook.send(url, config.discordMessage.replace("{player}", playerName), platform);
    }
}