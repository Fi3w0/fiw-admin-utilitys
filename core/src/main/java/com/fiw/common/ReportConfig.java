package com.fiw.common;

public final class ReportConfig {
    public boolean enabled = true;
    public int cooldownSeconds = 60;
    public String notifyPermission = "fiw.report.notify";
    public String submittedMessage = "&aYour report has been submitted to staff.";
    public String cooldownMessage = "&cPlease wait before submitting another report.";
    public Discord discord = new Discord();

    public static ReportConfig defaults() {
        return new ReportConfig();
    }

    public static final class Discord {
        public boolean enabled = false;
        public String webhookUrl = "";
    }
}
