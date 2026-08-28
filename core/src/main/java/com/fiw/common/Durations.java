package com.fiw.common;

import java.util.Locale;

public final class Durations {
    private Durations() {
    }

    /** Parses "30s", "5m"/"5min", "1h" into seconds. Returns -1 when invalid. */
    public static int parseSeconds(String input) {
        if (input == null) {
            return -1;
        }
        String value = input.trim().toLowerCase(Locale.ROOT);
        int unitStart = 0;
        while (unitStart < value.length() && Character.isDigit(value.charAt(unitStart))) {
            unitStart++;
        }
        if (unitStart == 0) {
            return -1;
        }

        long amount;
        try {
            amount = Long.parseLong(value.substring(0, unitStart));
        } catch (NumberFormatException exception) {
            return -1;
        }

        long seconds = switch (value.substring(unitStart)) {
            case "s", "sec", "secs" -> amount;
            case "m", "min", "mins" -> amount * 60;
            case "h", "hr", "hrs" -> amount * 3600;
            default -> -1;
        };
        if (seconds <= 0 || seconds > 24 * 3600) {
            return -1;
        }
        return (int) seconds;
    }

    public static String format(int seconds) {
        if (seconds >= 3600 && seconds % 3600 == 0) {
            return (seconds / 3600) + "h";
        }
        if (seconds >= 60 && seconds % 60 == 0) {
            return (seconds / 60) + "m";
        }
        if (seconds > 60) {
            return (seconds / 60) + "m" + (seconds % 60) + "s";
        }
        return seconds + "s";
    }
}