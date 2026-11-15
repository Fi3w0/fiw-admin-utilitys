package com.fiw.admin.common;

import com.fiw.admin.api.AdminAfkApi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

final class AfkServiceTest {
    @TempDir Path directory;
    private final AtomicLong now = new AtomicLong(1_000);
    private final UUID player = UUID.randomUUID();

    private AfkService service() {
        FiwPlatform platform = new FiwPlatform() {
            public String loaderName() { return "test"; }
            public Path configDirectory() { return directory; }
            public void info(String message) {}
            public void warn(String message) {}
        };
        AfkService service = new AfkService(platform, now::get);
        service.recordActivity(player);
        return service;
    }

    @Test
    void realActivityImmediatelyClearsAutomaticAfkAndReportsTheTransition() {
        AfkService service = service();
        now.addAndGet(300_000);
        assertTrue(service.refresh().contains(player));
        assertTrue(service.isAfk(player));
        service.recordActivity(player);
        assertFalse(service.isAfk(player));
        assertTrue(service.refresh().contains(player));
        assertTrue(service.refresh().isEmpty());
    }

    @Test
    void manualReturnIsReportedOnceForBroadcastAndTagRefresh() {
        AfkService service = service();
        assertTrue(service.toggleManual(player));
        service.refresh();
        service.recordActivity(player);
        assertFalse(service.isAfk(player));
        assertTrue(service.refresh().contains(player));
        assertTrue(service.refresh().isEmpty());
    }

    @Test
    void disablingAfkClearsTheSharedState() {
        AfkService service = service();
        service.toggleManual(player);
        service.config().enabled = false;
        service.refresh();
        assertFalse(service.isAfk(player));
        assertTrue(service.afkPlayers().isEmpty());
    }

    @Test
    void cameraActivityClearsAfkWithoutLettingAnUnchangedCameraResetIdleTime() {
        AfkService service = service();
        service.observeRotation(player, 0, 0);
        now.addAndGet(300_000);
        service.observeRotation(player, 0, 0);
        service.refresh();
        assertTrue(service.isAfk(player));
        service.observeRotation(player, 1, 0);
        assertFalse(service.isAfk(player));
        assertEquals(0, service.idleSeconds(player));
    }

    @Test
    void sharedApiFollowsManualMarksDisabledConfigLogoutAndServerLifecycle() {
        AfkService service = service();
        assertNull(AdminAfkApi.isAfk(player));
        AdminAfkApi.start(service);
        try {
            assertEquals(Boolean.FALSE, AdminAfkApi.isAfk(player));
            service.toggleManual(player);
            assertEquals(Boolean.TRUE, AdminAfkApi.isAfk(player));
            service.config().enabled = false;
            assertEquals(Boolean.FALSE, AdminAfkApi.isAfk(player));
            service.forget(player);
            assertNull(AdminAfkApi.isAfk(player));
        } finally {
            AdminAfkApi.stop(service);
        }
        assertNull(AdminAfkApi.isAfk(player));
    }
}
