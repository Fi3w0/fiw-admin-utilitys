package com.fiw.common;

import java.util.ArrayList;
import java.util.List;

public final class MaintenanceConfig {
    public boolean enabled = true;
    public boolean motdEnabled = true;
    public String motdMessage = "&cMaintenance mode";
    public String defaultMessage = "&cServer maintenance is in progress.\n&ePlease try again soon.";
    public String bypassPermission = "fiw.maintenance.bypass";
    public boolean opBypass = true;
    public String countdownMessage = "&cMaintenance in &e{time}&c!";
    public boolean stopServerAfterCountdown = false;
    public List<String> allowlistNames = new ArrayList<>();
    public List<String> allowlistUuids = new ArrayList<>();

    public static MaintenanceConfig defaults() {
        return new MaintenanceConfig();
    }
}
