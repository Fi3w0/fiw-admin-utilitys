package com.fiw.admin.api;

import com.fiw.admin.common.AfkService;
import java.util.UUID;

/** Optional server-thread bridge for mods that need the same AFK state as /fiw afk. */
public final class AdminAfkApi {
    private static AfkService authority;

    private AdminAfkApi() {}

    public static void start(AfkService service) {
        authority = service;
    }

    public static void stop(AfkService service) {
        if (authority == service) authority = null;
    }

    /** Null before startup/after shutdown or before this player's join event. */
    public static Boolean isAfk(UUID uuid) {
        AfkService service = authority;
        return service != null && service.tracks(uuid) ? service.isAfk(uuid) : null;
    }
}
