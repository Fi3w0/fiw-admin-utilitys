package com.fiw.common;

import java.util.ArrayList;
import java.util.List;

public final class AlertHistory {
    public List<Entry> entries = new ArrayList<>();

    public static final class Entry {
        public long timeMillis;
        public double tps;
        public List<String> lines = new ArrayList<>();

        public Entry() {
        }

        public Entry(long timeMillis, double tps, List<String> lines) {
            this.timeMillis = timeMillis;
            this.tps = tps;
            this.lines = lines;
        }
    }
}