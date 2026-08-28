package com.fiw.common;

import java.nio.file.Path;

public final class SweepService {
    private final JsonConfigStore<SweepConfig> configStore;
    private SweepConfig config = SweepConfig.defaults();

    public SweepService(FiwPlatform platform) {
        Path root = platform.configDirectory().resolve("fiw-admin");
        this.configStore = new JsonConfigStore<>(root.resolve("sweep.json"), SweepConfig.class, SweepConfig.defaults());
    }

    public void reload() {
        config = configStore.load();
    }

    public SweepConfig config() {
        return config;
    }

    public boolean isEnabled() {
        return config.enabled;
    }

    public void setEnabled(boolean enabled) {
        config.enabled = enabled;
        configStore.save(config);
    }
}
