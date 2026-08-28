package com.fiw.common;

import java.util.ArrayList;
import java.util.List;

public final class PunishmentConfig {
    public boolean enabled = true;
    public boolean reasonRequired = false;
    public String defaultKickMessage = "&cYou have been kicked from the server.";
    public String defaultBanMessage = "&cYou are banned from this server.";
    public String defaultMuteMessage = "&cYou are muted and cannot send chat messages.";
    public boolean broadcastPunishments = true;
    public String notifyPermission = "fiw.punish.notify";
    public int escalationLookbackDays = 30;
    public Discord discord = new Discord();
    public List<Tier> escalationLadder = defaultLadder();

    public static PunishmentConfig defaults() {
        return new PunishmentConfig();
    }

    private static List<Tier> defaultLadder() {
        List<Tier> ladder = new ArrayList<>();
        ladder.add(new Tier(Action.MUTE, 600));
        ladder.add(new Tier(Action.TEMPBAN, 3600));
        ladder.add(new Tier(Action.TEMPBAN, 86400));
        ladder.add(new Tier(Action.BAN, 0));
        return ladder;
    }

    public enum Action {
        MUTE, TEMPBAN, BAN
    }

    public static final class Tier {
        public Action action;
        public int durationSeconds;

        public Tier() {
        }

        public Tier(Action action, int durationSeconds) {
            this.action = action;
            this.durationSeconds = durationSeconds;
        }
    }

    public static final class Discord {
        public boolean enabled = false;
        public String webhookUrl = "";
    }
}
