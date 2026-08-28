package com.fiw.common;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SweepConfig {
    public boolean enabled = true;
    public Items items = new Items();
    public Mobs mobs = new Mobs();
    public Warnings warnings = new Warnings();
    public String announceResults = "admins";
    public String notifyPermission = "fiw.sweep.notify";

    public static SweepConfig defaults() {
        return new SweepConfig();
    }

    public static final class Items {
        public boolean enabled = true;
        public boolean timerCleanEnabled = true;
        public int intervalMinutes = 10;
        public boolean thresholdCleanEnabled = true;
        public int maxGroundItems = 1500;
        public int minItemAgeSeconds = 120;
        public boolean exemptNamedItems = true;
        public List<String> ignoredItems = new ArrayList<>(List.of("minecraft:nether_star"));
        public List<String> onlyTheseItems = new ArrayList<>();
    }

    public static final class Mobs {
        public boolean enabled = true;
        public int checkIntervalSeconds = 60;
        public PerChunkCaps perChunkCaps = new PerChunkCaps();
        public Map<String, Integer> globalCaps = new LinkedHashMap<>();
        public boolean globalCapsEnabled = false;
        public boolean exemptNamed = true;
        public boolean exemptTamed = true;
        public boolean exemptLeashed = true;
        public boolean exemptPersistent = true;
        public List<String> neverClean = new ArrayList<>(List.of(
                "minecraft:villager",
                "minecraft:iron_golem",
                "minecraft:allay"
        ));
    }

    public static final class PerChunkCaps {
        public boolean enabled = true;
        public int defaultCap = 40;
    }

    public static final class Warnings {
        public boolean enabled = true;
        public List<Integer> chatSteps = new ArrayList<>(List.of(60, 30, 10));
        public int actionbarFinalCountdown = 5;
        public String message = "&eGround items clearing in &c{seconds}s&e!";
    }
}
