package com.fiw.common;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
}
