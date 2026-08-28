package com.fiw.common;

import java.nio.file.Path;
import java.util.Random;

public final class AnnounceService {
    private final JsonConfigStore<AnnounceConfig> configStore;
    private final Random random = new Random();
    private AnnounceConfig config = AnnounceConfig.defaults();
    private int rotationIndex;

    public AnnounceService(FiwPlatform platform) {
        Path root = platform.configDirectory().resolve("fiw-admin");
        this.configStore = new JsonConfigStore<>(root.resolve("announce.json"), AnnounceConfig.class, AnnounceConfig.defaults());
    }

    public void reload() {
        config = configStore.load();
        rotationIndex = 0;
    }

    public AnnounceConfig config() {
        return config;
    }

    public String nextMessage() {
        if (config.messages.isEmpty()) {
            return null;
        }
        if (config.randomOrder) {
            return config.messages.get(random.nextInt(config.messages.size()));
        }
        String message = config.messages.get(rotationIndex % config.messages.size());
        rotationIndex++;
        return message;
    }
}