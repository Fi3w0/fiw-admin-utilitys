package com.fiw.common;

import java.util.ArrayList;
import java.util.List;

public final class AnnounceConfig {
    public boolean enabled = true;
    public int intervalMinutes = 15;
    public boolean randomOrder = false;
    public String prefix = "&7[&efiw&7] &r";
    public List<String> messages = new ArrayList<>();

    public static AnnounceConfig defaults() {
        return new AnnounceConfig();
    }
}