package com.fiw.common;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class PunishmentService {
    private static final int MAX_HISTORY_PER_PLAYER = 200;

    private final FiwPlatform platform;
    private final JsonConfigStore<PunishmentConfig> configStore;
    private final JsonConfigStore<PunishmentState> stateStore;
    private PunishmentConfig config = PunishmentConfig.defaults();
    private PunishmentState state = new PunishmentState();

    public PunishmentService(FiwPlatform platform) {
        this.platform = platform;
        Path root = platform.configDirectory().resolve("fiw-admin");
        this.configStore = new JsonConfigStore<>(root.resolve("punishment.json"), PunishmentConfig.class, PunishmentConfig.defaults());
        this.stateStore = new JsonConfigStore<>(root.resolve("punishments.json"), PunishmentState.class, new PunishmentState());
    }

    public void reload() {
        config = configStore.load();
        state = stateStore.load();
    }

    public PunishmentConfig config() {
        return config;
    }

    public void notifyDiscord(String content) {
        if (config.discord.enabled) {
            DiscordWebhook.send(config.discord.webhookUrl, content, platform);
        }
    }

    public Sanction activeBan(UUID uuid) {
        PlayerRecord record = state.players.get(uuid.toString());
        if (record == null || record.activeBan == null) {
            return null;
        }
        if (isExpired(record.activeBan)) {
            record.activeBan = null;
            stateStore.save(state);
            return null;
        }
        return record.activeBan;
    }

    public Sanction activeMute(UUID uuid) {
        PlayerRecord record = state.players.get(uuid.toString());
        if (record == null || record.activeMute == null) {
            return null;
        }
        if (isExpired(record.activeMute)) {
            record.activeMute = null;
            stateStore.save(state);
            return null;
        }
        return record.activeMute;
    }

    /** durationSeconds 0 = permanent. */
    public void ban(UUID uuid, String playerName, String staffName, String reason, int durationSeconds) {
        PlayerRecord record = recordFor(uuid, playerName);
        record.activeBan = new Sanction(reason, staffName, System.currentTimeMillis(), expiryFor(durationSeconds));
        appendHistory(record, "BAN", reason, staffName, durationSeconds);
        stateStore.save(state);
    }

    public boolean unban(UUID uuid) {
        PlayerRecord record = state.players.get(uuid.toString());
        if (record == null || record.activeBan == null) {
            return false;
        }
        record.activeBan = null;
        stateStore.save(state);
        return true;
    }

    /** durationSeconds 0 = permanent. */
    public void mute(UUID uuid, String playerName, String staffName, String reason, int durationSeconds) {
        PlayerRecord record = recordFor(uuid, playerName);
        record.activeMute = new Sanction(reason, staffName, System.currentTimeMillis(), expiryFor(durationSeconds));
        appendHistory(record, "MUTE", reason, staffName, durationSeconds);
        stateStore.save(state);
    }

    public boolean unmute(UUID uuid) {
        PlayerRecord record = state.players.get(uuid.toString());
        if (record == null || record.activeMute == null) {
            return false;
        }
        record.activeMute = null;
        stateStore.save(state);
        return true;
    }

    public void recordKick(UUID uuid, String playerName, String staffName, String reason) {
        PlayerRecord record = recordFor(uuid, playerName);
        appendHistory(record, "KICK", reason, staffName, 0);
        stateStore.save(state);
    }

    public List<HistoryEntry> history(UUID uuid) {
        PlayerRecord record = state.players.get(uuid.toString());
        return record == null ? List.of() : List.copyOf(record.history);
    }

    /** Counts history entries within the configured lookback window, for escalation. */
    public int recentOffenseCount(UUID uuid) {
        PlayerRecord record = state.players.get(uuid.toString());
        if (record == null) {
            return 0;
        }
        long cutoff = System.currentTimeMillis() - (long) config.escalationLookbackDays * 86_400_000L;
        int count = 0;
        for (HistoryEntry entry : record.history) {
            if (entry.timestampMillis >= cutoff) {
                count++;
            }
        }
        return count;
    }

    /** Returns the escalation tier to apply next based on recent offense count, or null when the ladder is empty. */
    public PunishmentConfig.Tier nextEscalationTier(UUID uuid) {
        List<PunishmentConfig.Tier> ladder = config.escalationLadder;
        if (ladder == null || ladder.isEmpty()) {
            return null;
        }
        int index = Math.min(recentOffenseCount(uuid), ladder.size() - 1);
        return ladder.get(index);
    }

    /** Purges expired bans/mutes; persists if anything changed. */
    public void purgeExpired() {
        boolean changed = false;
        for (PlayerRecord record : state.players.values()) {
            if (record.activeBan != null && isExpired(record.activeBan)) {
                record.activeBan = null;
                changed = true;
            }
            if (record.activeMute != null && isExpired(record.activeMute)) {
                record.activeMute = null;
                changed = true;
            }
        }
        if (changed) {
            stateStore.save(state);
        }
    }

    private boolean isExpired(Sanction sanction) {
        return sanction.expiryMillis != 0 && sanction.expiryMillis <= System.currentTimeMillis();
    }

    private long expiryFor(int durationSeconds) {
        return durationSeconds <= 0 ? 0 : System.currentTimeMillis() + durationSeconds * 1000L;
    }

    private PlayerRecord recordFor(UUID uuid, String playerName) {
        PlayerRecord record = state.players.computeIfAbsent(uuid.toString(), ignored -> new PlayerRecord());
        record.name = playerName;
        return record;
    }

    private void appendHistory(PlayerRecord record, String type, String reason, String staffName, int durationSeconds) {
        record.history.add(new HistoryEntry(type, reason == null ? "" : reason, staffName, System.currentTimeMillis(), durationSeconds));
        while (record.history.size() > MAX_HISTORY_PER_PLAYER) {
            record.history.remove(0);
        }
    }

    public static final class PunishmentState {
        public Map<String, PlayerRecord> players = new LinkedHashMap<>();
    }

    public static final class PlayerRecord {
        public String name = "";
        public Sanction activeBan;
        public Sanction activeMute;
        public List<HistoryEntry> history = new ArrayList<>();
    }

    public static final class Sanction {
        public String reason = "";
        public String staffName = "";
        public long issuedAtMillis;
        public long expiryMillis;

        public Sanction() {
        }

        public Sanction(String reason, String staffName, long issuedAtMillis, long expiryMillis) {
            this.reason = reason == null ? "" : reason;
            this.staffName = staffName;
            this.issuedAtMillis = issuedAtMillis;
            this.expiryMillis = expiryMillis;
        }
    }

    public static final class HistoryEntry {
        public String type = "";
        public String reason = "";
        public String staffName = "";
        public long timestampMillis;
        public int durationSeconds;

        public HistoryEntry() {
        }

        public HistoryEntry(String type, String reason, String staffName, long timestampMillis, int durationSeconds) {
            this.type = type;
            this.reason = reason;
            this.staffName = staffName;
            this.timestampMillis = timestampMillis;
            this.durationSeconds = durationSeconds;
        }
    }
}
