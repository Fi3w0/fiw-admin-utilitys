package com.fiw.common;

import java.util.ArrayList;
import java.util.List;

public final class MessagesConfig {
    public JoinLeave joinLeave = new JoinLeave();
    public Motd motd = new Motd();

    public static MessagesConfig defaults() {
        return new MessagesConfig();
    }

    public static final class JoinLeave {
        public boolean enabled = true;
        public String joinMessage = "&7[&a+&7] &e{player}";
        public String leaveMessage = "&7[&c-&7] &e{player}";
    }

    public static final class Motd {
        public boolean enabled = true;
        public int rotateMinutes = 5;
        public boolean randomOrder = false;
        public List<String> motds = new ArrayList<>();
    }
}