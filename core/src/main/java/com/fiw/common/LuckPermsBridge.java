package com.fiw.common;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.util.Tristate;

import java.util.UUID;

/** Optional LuckPerms hook: consulted first when the LuckPerms mod is installed, silently ignored otherwise. */
public final class LuckPermsBridge {
    private static volatile boolean unavailable;

    private LuckPermsBridge() {
    }

    /** TRUE/FALSE when LuckPerms has an explicit verdict for this node, null when LuckPerms is absent or undefined. */
    public static Boolean check(UUID playerId, String permission) {
        if (unavailable) {
            return null;
        }
        try {
            LuckPerms luckPerms = LuckPermsProvider.get();
            User user = luckPerms.getUserManager().getUser(playerId);
            if (user == null) {
                return null;
            }
            Tristate result = user.getCachedData().getPermissionData().checkPermission(permission);
            return result == Tristate.UNDEFINED ? null : result.asBoolean();
        } catch (IllegalStateException notLoadedYet) {
            // LuckPerms may still be initializing; try again on the next check.
            return null;
        } catch (NoClassDefFoundError apiAbsent) {
            unavailable = true;
            return null;
        }
    }
}