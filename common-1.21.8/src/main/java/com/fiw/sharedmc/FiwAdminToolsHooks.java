package com.fiw.sharedmc;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

public final class FiwAdminToolsHooks {
    private static AdminRuntime runtime;

    private FiwAdminToolsHooks() {
    }

    public static void setRuntime(AdminRuntime runtime) {
        FiwAdminToolsHooks.runtime = runtime;
    }

    public static boolean shouldSuppressGameMessage(MinecraftServer server, Component message, boolean overlay) {
        return runtime != null && runtime.shouldSuppressGameMessage(server, message, overlay);
    }

    public static Optional<ServerStatus.Players> serverStatusPlayers(MinecraftServer server, ServerStatus.Players players) {
        if (runtime == null) {
            return Optional.ofNullable(players);
        }
        return runtime.serverStatusPlayers(server, players);
    }

    public static Component tabListDisplayName(ServerPlayer player, Component original) {
        if (runtime == null) {
            return original;
        }
        return runtime.tabListDisplayName(player, original);
    }
}
