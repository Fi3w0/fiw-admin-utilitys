package com.fiw.common;

import java.util.ArrayList;
import java.util.List;

public final class DupeConfig {
    public boolean enabled = true;
    public int scanIntervalSeconds = 5;
    public int gracePeriodSecondsAfterStart = 30;
    public String notifyPermission = "fiw.dupe.notify";
    public Discord discord = new Discord();
    public RateDetector rateDetector = new RateDetector();
    public SignatureDetector signatureDetector = new SignatureDetector();

    public static DupeConfig defaults() {
        return new DupeConfig();
    }

    public enum Tier {
        LOG, ALERT, DISCORD, FREEZE, KICK, TEMPBAN, BAN
    }

    public static final class Discord {
        public boolean enabled = false;
        public String webhookUrl = "";
    }

    public static final class Response {
        public Tier tier = Tier.ALERT;
        public int durationSeconds = 0;
    }

    public static final class RateDetector {
        public boolean enabled = true;
        public int windowSeconds = 10;
        public double threshold = 64;
        public boolean exemptWhileContainerOpen = true;
        public List<WatchEntry> watchList = defaultWatchList();
        public Response response = new Response();
        public ChunkScope chunkScope = new ChunkScope();

        private static List<WatchEntry> defaultWatchList() {
            List<WatchEntry> list = new ArrayList<>();
            list.add(new WatchEntry("minecraft:netherite_ingot", 8));
            list.add(new WatchEntry("minecraft:diamond", 4));
            list.add(new WatchEntry("minecraft:nether_star", 16));
            list.add(new WatchEntry("minecraft:elytra", 16));
            return list;
        }
    }

    public static final class ChunkScope {
        public boolean enabled = false;
        public double thresholdMultiplier = 4.0;
    }

    public static final class WatchEntry {
        public String itemId;
        public double weight;

        public WatchEntry() {
        }

        public WatchEntry(String itemId, double weight) {
            this.itemId = itemId;
            this.weight = weight;
        }
    }

    public static final class SignatureDetector {
        public boolean enabled = true;
        public List<String> watchList = new ArrayList<>();
        public Response response = new Response();
    }
}
