package com.fiw.common;

import java.nio.file.Path;
import java.util.Random;

public final class MessagesService {
    private final JsonConfigStore<MessagesConfig> configStore;
    private final Random random = new Random();
    private MessagesConfig config = MessagesConfig.defaults();
    private int motdIndex;

    public MessagesService(FiwPlatform platform) {
        Path root = platform.configDirectory().resolve("fiw-admin");
        this.configStore = new JsonConfigStore<>(root.resolve("messages.json"), MessagesConfig.class, MessagesConfig.defaults());
    }

    public void reload() {
        config = configStore.load();
        motdIndex = 0;
    }

    public MessagesConfig config() {
        return config;
    }

    public String nextMotd() {
        if (config.motd.motds.isEmpty()) {
            return null;
        }
        if (config.motd.randomOrder) {
            return config.motd.motds.get(random.nextInt(config.motd.motds.size()));
        }
        String motd = config.motd.motds.get(motdIndex % config.motd.motds.size());
        motdIndex++;
        return motd;
    }
}