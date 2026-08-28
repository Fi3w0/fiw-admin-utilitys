package com.fiw.common;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ConfigStateIntegrationTest {
    @TempDir
    Path tempDir;

    @Test
    void bootstrapCreatesDefaultConfigsAndStateFiles() {
        FiwAdminToolsCore.bootstrap(new TestPlatform(tempDir));

        Path root = tempDir.resolve("fiw-admin");
        assertTrue(Files.exists(root.resolve("maintenance.json")));
        assertTrue(Files.exists(root.resolve("sweep.json")));
        assertTrue(Files.exists(root.resolve("alert.json")));
        assertTrue(Files.exists(root.resolve("vanish.json")));
        assertTrue(Files.exists(root.resolve("vanished-players.json")));
        assertFalse(Files.exists(root.resolve("maintenance.flag")));
    }

    @Test
    void maintenanceFlagPersistsAcrossReloads() {
        MaintenanceService first = new MaintenanceService(new TestPlatform(tempDir));
        first.reload();
        first.enable("Back soon");

        MaintenanceService second = new MaintenanceService(new TestPlatform(tempDir));
        second.reload();

        assertTrue(second.isEnabled());
        assertEquals("Back soon", second.currentMessage());
    }

    @Test
    void vanishedPlayersAndNamesPersistAcrossReloads() {
        UUID uuid = UUID.fromString("00000000-0000-0000-0000-000000000123");

        VanishService first = new VanishService(new TestPlatform(tempDir));
        first.reload();
        first.setVanished(uuid, "Fiw", true);

        VanishService second = new VanishService(new TestPlatform(tempDir));
        second.reload();

        assertTrue(second.isVanished(uuid));
        assertEquals("Fiw", second.knownName(uuid));
    }

    @Test
    void punishmentBansAndHistoryPersistAcrossReloads() {
        UUID uuid = UUID.fromString("00000000-0000-0000-0000-000000000456");

        PunishmentService first = new PunishmentService(new TestPlatform(tempDir));
        first.reload();
        first.ban(uuid, "Griefer", "Fiw", "Griefing spawn", 3600);
        first.mute(uuid, "Griefer", "Fiw", "Spam", 0);

        PunishmentService second = new PunishmentService(new TestPlatform(tempDir));
        second.reload();

        assertTrue(second.activeBan(uuid) != null);
        assertEquals("Griefing spawn", second.activeBan(uuid).reason);
        assertTrue(second.activeMute(uuid) != null);
        assertEquals(2, second.history(uuid).size());

        second.unban(uuid);
        assertFalse(second.activeBan(uuid) != null);
    }

    @Test
    void freezeDetailsPersistAlongsideLegacyFrozenMap() {
        UUID uuid = UUID.fromString("00000000-0000-0000-0000-000000000789");

        FreezeService first = new FreezeService(new TestPlatform(tempDir));
        first.reload();
        first.freeze(uuid, "Suspect", "Fiw", "Investigating dupe report", 0, "held: diamond_sword");

        FreezeService second = new FreezeService(new TestPlatform(tempDir));
        second.reload();

        assertTrue(second.isFrozen(uuid));
        assertEquals("Investigating dupe report", second.detail(uuid).reason);

        assertTrue(second.unfreeze(uuid));
        assertFalse(second.isFrozen(uuid));
    }

    @Test
    void reportsPersistAcrossReloads() {
        ReportService first = new ReportService(new TestPlatform(tempDir));
        first.reload();
        ReportService.Report report = first.submit(
                UUID.fromString("00000000-0000-0000-0000-000000000abc"), "Reporter", "Target", "Being rude");

        ReportService second = new ReportService(new TestPlatform(tempDir));
        second.reload();

        assertEquals(1, second.openReports().size());
        assertTrue(second.claim(report.id, "Fiw"));
        assertTrue(second.resolve(report.id));
        assertEquals(0, second.openReports().size());
    }

    @Test
    void dupeAlertsPersistAndRateWindowExpires() {
        DupeService first = new DupeService(new TestPlatform(tempDir));
        first.reload();
        first.recordAlert("rate", "Suspicious gain by Fiw");

        DupeService second = new DupeService(new TestPlatform(tempDir));
        second.reload();
        assertEquals(1, second.recentAlerts().size());
        assertEquals("rate", second.recentAlerts().get(0).detector);

        assertEquals(0, second.rateIncrease("player-1", 10, 60));
        assertEquals(90, second.rateIncrease("player-1", 100, 60));
        second.clearHistory("player-1");
        assertEquals(0, second.rateIncrease("player-1", 5, 60));
    }

    @Test
    void dupeSignatureDetectsSimultaneousDifferentHolders() {
        DupeService service = new DupeService(new TestPlatform(tempDir));
        service.reload();

        assertNull(service.checkAndUpdateSignature("sig-1", "player-a", 5));
        String conflict = service.checkAndUpdateSignature("sig-1", "player-b", 5);
        assertEquals("player-a", conflict);
        assertNull(service.checkAndUpdateSignature("sig-1", "player-b", 5));
    }

    @Test
    void watchdogDetectsUncleanShutdownButNotCleanShutdown() {
        Path marker = tempDir.resolve("fiw-admin").resolve(".watchdog-running");

        RecordingPlatform crashedRun = new RecordingPlatform(tempDir);
        WatchdogService first = new WatchdogService(crashedRun);
        first.reload();
        first.onServerStarted();
        assertTrue(Files.exists(marker));
        // No onServerStopping() call: simulates a crash/kill leaving the marker behind.

        RecordingPlatform restartAfterCrash = new RecordingPlatform(tempDir);
        WatchdogService second = new WatchdogService(restartAfterCrash);
        second.reload();
        second.onServerStarted();
        assertTrue(restartAfterCrash.warnings.stream().anyMatch(line -> line.contains("unclean shutdown")));
        second.onServerStopping();
        assertFalse(Files.exists(marker));

        RecordingPlatform cleanRestart = new RecordingPlatform(tempDir);
        WatchdogService third = new WatchdogService(cleanRestart);
        third.reload();
        third.onServerStarted();
        assertFalse(cleanRestart.warnings.stream().anyMatch(line -> line.contains("unclean shutdown")));
        third.onServerStopping();
    }

    private record TestPlatform(Path configDirectory) implements FiwPlatform {
        @Override
        public String loaderName() {
            return "Test";
        }

        @Override
        public void info(String message) {
        }

        @Override
        public void warn(String message) {
        }
    }

    private static final class RecordingPlatform implements FiwPlatform {
        private final Path configDirectory;
        final List<String> warnings = new ArrayList<>();

        RecordingPlatform(Path configDirectory) {
            this.configDirectory = configDirectory;
        }

        @Override
        public Path configDirectory() {
            return configDirectory;
        }

        @Override
        public String loaderName() {
            return "Test";
        }

        @Override
        public void info(String message) {
        }

        @Override
        public void warn(String message) {
            warnings.add(message);
        }
    }
}
