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
