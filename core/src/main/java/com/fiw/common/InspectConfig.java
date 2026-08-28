package com.fiw.common;

public final class InspectConfig {
    public boolean enabled = true;
    public boolean findEnabled = true;
    public boolean findIncludeEnderChests = true;

    public static InspectConfig defaults() {
        return new InspectConfig();
    }
}