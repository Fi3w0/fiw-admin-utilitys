package com.fiw.common;

import java.util.List;

public final class FiwAdminToolsCore {
    public static final String FABRIC_MOD_ID = "fiw-admin-tools";
    public static final String NEOFORGE_MOD_ID = "fiw_admin_tools";
    public static final String DISPLAY_NAME = "fiw admin tools";

    private final FiwPlatform platform;
    private final MaintenanceService maintenanceService;
    private final SweepService sweepService;
    private final AlertService alertService;
    private final VanishService vanishService;
    private final InspectService inspectService;
    private final FreezeService freezeService;
    private final AnnounceService announceService;
    private final NewPlayerService newPlayerService;
    private final BanItemService banItemService;
    private final MessagesService messagesService;
    private final PunishmentService punishmentService;
    private final ReportService reportService;
    private final AfkService afkService;
    private final WatchdogService watchdogService;
    private final DupeService dupeService;

    private FiwAdminToolsCore(FiwPlatform platform) {
        this.platform = platform;
        this.maintenanceService = new MaintenanceService(platform);
        this.sweepService = new SweepService(platform);
        this.alertService = new AlertService(platform);
        this.vanishService = new VanishService(platform);
        this.inspectService = new InspectService(platform);
        this.freezeService = new FreezeService(platform);
        this.announceService = new AnnounceService(platform);
        this.newPlayerService = new NewPlayerService(platform);
        this.banItemService = new BanItemService(platform);
        this.messagesService = new MessagesService(platform);
        this.punishmentService = new PunishmentService(platform);
        this.reportService = new ReportService(platform);
        this.afkService = new AfkService(platform);
        this.watchdogService = new WatchdogService(platform);
        this.dupeService = new DupeService(platform);
    }

    public static FiwAdminToolsCore bootstrap(FiwPlatform platform) {
        platform.info(DISPLAY_NAME + " booting on " + platform.loaderName());
        FiwAdminToolsCore core = new FiwAdminToolsCore(platform);
        core.reload();
        return core;
    }

    public List<String> statusLines() {
        return List.of(
                DISPLAY_NAME + " status",
                "Loader: " + platform.loaderName(),
                "Maintenance: " + (maintenanceService.isEnabled() ? "on" : "off"),
                "Sweep: " + (sweepService.isEnabled() ? "on" : "off"),
                "Alert: " + (alertService.isEnabled() ? "on" : "off"),
                "Vanish: " + (vanishService.config().enabled ? "on" : "off") + " (" + vanishService.vanishedCount() + " vanished)",
                "Freeze: " + (freezeService.config().enabled ? "on" : "off") + " (" + freezeService.frozenCount() + " frozen)",
                "Announce: " + (announceService.config().enabled ? "on" : "off")
                        + " (" + announceService.config().messages.size() + " messages, every " + announceService.config().intervalMinutes + "m)",
                "BanItem: " + (banItemService.config().enabled ? "on" : "off") + " (" + banItemService.activeBanCount() + " banned)",
                "Messages: join/leave " + (messagesService.config().joinLeave.enabled ? "on" : "off")
                        + ", motd rotation " + (messagesService.config().motd.enabled ? "on" : "off")
                        + " (" + messagesService.config().motd.motds.size() + " motds)",
                "Punishment: " + (punishmentService.config().enabled ? "on" : "off"),
                "Report: " + (reportService.config().enabled ? "on" : "off")
                        + " (" + reportService.openReports().size() + " open)",
                "AFK: " + (afkService.config().enabled ? "on" : "off") + " (" + afkService.afkPlayers().size() + " afk)",
                "Watchdog: " + (watchdogService.config().enabled ? "on" : "off"),
                "Dupe: " + (dupeService.config().enabled ? "on" : "off")
                        + " (" + dupeService.recentAlerts().size() + " logged alerts)"
        );
    }

    public String reload() {
        maintenanceService.reload();
        sweepService.reload();
        alertService.reload();
        vanishService.reload();
        inspectService.reload();
        freezeService.reload();
        announceService.reload();
        newPlayerService.reload();
        banItemService.reload();
        messagesService.reload();
        punishmentService.reload();
        reportService.reload();
        afkService.reload();
        watchdogService.reload();
        dupeService.reload();
        return "Reloaded fiw admin tools config.";
    }

    public MaintenanceService maintenance() {
        return maintenanceService;
    }

    public SweepService sweep() {
        return sweepService;
    }

    public AlertService alert() {
        return alertService;
    }

    public VanishService vanish() {
        return vanishService;
    }

    public InspectService inspect() {
        return inspectService;
    }

    public FreezeService freeze() {
        return freezeService;
    }

    public AnnounceService announce() {
        return announceService;
    }

    public NewPlayerService newPlayer() {
        return newPlayerService;
    }

    public BanItemService banItem() {
        return banItemService;
    }

    public MessagesService messages() {
        return messagesService;
    }

    public PunishmentService punishment() {
        return punishmentService;
    }

    public ReportService report() {
        return reportService;
    }

    public AfkService afk() {
        return afkService;
    }

    public WatchdogService watchdog() {
        return watchdogService;
    }

    public DupeService dupe() {
        return dupeService;
    }
}
