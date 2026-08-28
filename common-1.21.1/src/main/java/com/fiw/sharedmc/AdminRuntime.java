package com.fiw.sharedmc;

import com.fiw.common.AfkConfig;
import com.fiw.common.AlertConfig;
import com.fiw.common.AlertHistory;
import com.fiw.common.DupeConfig;
import com.fiw.common.DupeService;
import com.fiw.common.Durations;
import com.fiw.common.FiwAdminToolsCore;
import com.fiw.common.FreezeConfig;
import com.fiw.common.AnnounceConfig;
import com.fiw.common.BanItemConfig;
import com.fiw.common.InspectService;
import com.fiw.common.MessagesConfig;
import com.fiw.common.NewPlayerConfig;
import com.fiw.common.PunishmentConfig;
import com.fiw.common.PunishmentService;
import com.fiw.common.ReportConfig;
import com.fiw.common.ReportService;
import com.fiw.common.SweepConfig;
import com.fiw.common.TextFormat;
import com.fiw.sharedmc.mixin.ChunkMapAccessor;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class AdminRuntime {
    private static final String PERMISSION_MAINTENANCE_MANAGE = "fiw.maintenance.manage";
    private static final String PERMISSION_SWEEP_MANAGE = "fiw.sweep.manage";
    private static final String PERMISSION_ALERT_MANAGE = "fiw.alert.manage";
    private static final String PERMISSION_VANISH_USE = "fiw.vanish.use";
    private static final String PERMISSION_VANISH_SEE = "fiw.vanish.see";
    private static final String PERMISSION_INSPECT_USE = "fiw.inspect.use";
    private static final String PERMISSION_FREEZE_USE = "fiw.freeze.use";
    private static final String PERMISSION_BANITEM_MANAGE = "fiw.banitem.manage";
    private static final String PERMISSION_PUNISH_KICK = "fiw.punish.kick";
    private static final String PERMISSION_PUNISH_BAN = "fiw.punish.ban";
    private static final String PERMISSION_PUNISH_MUTE = "fiw.punish.mute";
    private static final String PERMISSION_PUNISH_MANAGE = "fiw.punish.manage";
    private static final String PERMISSION_REPORT_USE = "fiw.report.use";
    private static final String PERMISSION_REPORT_MANAGE = "fiw.report.manage";
    private static final String PERMISSION_AFK_USE = "fiw.afk.use";
    private static final String PERMISSION_AFK_MANAGE = "fiw.afk.manage";
    private static final String PERMISSION_WATCHDOG_NOTIFY = "fiw.watchdog.notify";
    private static final String PERMISSION_DUPE_MANAGE = "fiw.dupe.manage";
    private static final DateTimeFormatter SEEN_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final int[] COUNTDOWN_STEPS = {3600, 1800, 1200, 600, 300, 120, 60, 30, 10, 5, 4, 3, 2, 1};

    private final FiwAdminToolsCore core;
    private final PlatformAccess access;
    private String originalMotd;
    private int tickCounter;
    private int itemCleanCountdownSeconds = -1;
    private int dupeScanCountdownSeconds = -1;
    private long dupeGraceUntilMillis;
    private long lastAlertTick = -200;
    private int maintenanceCountdownSeconds = -1;
    private String maintenanceCountdownMessage;
    private int announceCountdownSeconds = -1;
    private int motdRotateCountdownSeconds = -1;
    private final Map<UUID, FreezeAnchor> freezeAnchors = new HashMap<>();
    private final Set<String> hiddenVanishPairs = new HashSet<>();
    private final Map<UUID, String> knownVanishedNames = new LinkedHashMap<>();
    private final Map<String, Integer> recentlyVanishedNames = new LinkedHashMap<>();

    public AdminRuntime(FiwAdminToolsCore core, PlatformAccess access) {
        this.core = core;
        this.access = access;
    }

    public void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("fiw")
                .executes(context -> sendStatus(context.getSource()))
                .then(Commands.literal("status")
                        .requires(source -> access.hasPermission(source, PERMISSION_MAINTENANCE_MANAGE, 3))
                        .executes(context -> sendStatus(context.getSource())))
                .then(Commands.literal("reload")
                        .requires(source -> access.hasPermission(source, PERMISSION_MAINTENANCE_MANAGE, 3))
                        .executes(context -> reload(context.getSource())))
                .then(maintenanceCommand())
                .then(sweepCommand())
                .then(alertCommand())
                .then(Commands.literal("lag")
                        .requires(source -> access.hasPermission(source, PERMISSION_ALERT_MANAGE, 3))
                        .executes(context -> sendLagReport(context.getSource()))
                        .then(Commands.literal("history")
                                .executes(context -> sendLagHistory(context.getSource()))))
                .then(vanishCommand())
                .then(whoisCommand())
                .then(freezeCommand())
                .then(findCommand())
                .then(banItemCommand())
                .then(kickCommand())
                .then(banCommand())
                .then(tempbanCommand())
                .then(unbanCommand())
                .then(muteCommand())
                .then(tempmuteCommand())
                .then(unmuteCommand())
                .then(punishCommand())
                .then(historyCommand())
                .then(reportsCommand())
                .then(afkCommand())
                .then(watchdogCommand())
                .then(dupeCommand()));
        dispatcher.register(Commands.literal("report")
                .requires(source -> access.hasPermission(source, PERMISSION_REPORT_USE, 0))
                .then(Commands.argument("target", StringArgumentType.word())
                        .then(Commands.argument("reason", StringArgumentType.greedyString())
                                .executes(context -> submitReport(context.getSource(),
                                        StringArgumentType.getString(context, "target"),
                                        StringArgumentType.getString(context, "reason"))))));
    }

    public void onServerStarted(MinecraftServer server) {
        core.watchdog().onServerStarted();
        armDupeGracePeriod();
        applyMaintenanceMotd(server);
        resetSweepTimer();
    }

    public void onServerStopping(MinecraftServer server) {
        core.watchdog().onServerStopping();
    }

    private void armDupeGracePeriod() {
        dupeGraceUntilMillis = System.currentTimeMillis() + Math.max(0, core.dupe().config().gracePeriodSecondsAfterStart) * 1000L;
    }

    public void onPlayerJoin(MinecraftServer server, ServerPlayer player) {
        if (core.punishment().config().enabled) {
            var ban = core.punishment().activeBan(player.getGameProfile().getId());
            if (ban != null) {
                String message = formatBanKickMessage(ban);
                server.execute(() -> player.connection.disconnect(Component.literal(message)));
                return;
            }
        }
        if (core.maintenance().shouldKick(player.getGameProfile().getName(), player.getGameProfile().getId(), hasMaintenanceBypass(player))) {
            server.execute(() -> player.connection.disconnect(Component.literal(core.maintenance().currentKickMessage())));
            return;
        }
        core.afk().recordActivity(player.getGameProfile().getId());
        rememberVanishedName(player);
        syncVanishFor(player, server);
        if (core.vanish().isVanished(player.getGameProfile().getId())) {
            syncVanishForAll(server);
        }
        boolean firstJoin = core.inspect().recordJoin(player.getGameProfile().getId(), player.getGameProfile().getName());
        if (firstJoin) {
            announceNewPlayer(server, player.getGameProfile().getName());
        }
        sendCustomJoinLeave(server, player, core.messages().config().joinLeave.joinMessage, "joined");
        if (isActionFrozen(player)) {
            anchorFrozenPlayer(player);
        }
        core.watchdog().setPlayersOnline(true);
    }

    private void sendCustomJoinLeave(MinecraftServer server, ServerPlayer player, String template, String action) {
        MessagesConfig.JoinLeave config = core.messages().config().joinLeave;
        if (!config.enabled) {
            return;
        }
        boolean vanished = core.vanish().config().enabled && core.vanish().isVanished(player.getGameProfile().getId());
        // The vanilla broadcast (which also fed the console) is suppressed, so keep the server log informed here.
        access.log(player.getGameProfile().getName() + " " + action + " the game" + (vanished ? " (vanished)" : ""));
        if (vanished || template.isBlank()) {
            return;
        }
        Component message = Component.literal(TextFormat.legacyColors(template.replace("{player}", player.getGameProfile().getName())));
        for (ServerPlayer online : server.getPlayerList().getPlayers()) {
            online.sendSystemMessage(message);
        }
    }

    private void announceNewPlayer(MinecraftServer server, String name) {
        NewPlayerConfig config = core.newPlayer().config();
        if (!config.enabled) {
            return;
        }
        if (!config.adminMessage.isBlank()) {
            Component message = Component.literal(TextFormat.legacyColors(config.adminMessage.replace("{player}", name)));
            for (ServerPlayer online : server.getPlayerList().getPlayers()) {
                if (access.hasPermission(online, config.notifyPermission, 3)) {
                    online.sendSystemMessage(message);
                }
            }
        }
        if (!config.broadcastMessage.isBlank()) {
            Component message = Component.literal(TextFormat.legacyColors(config.broadcastMessage.replace("{player}", name)));
            for (ServerPlayer online : server.getPlayerList().getPlayers()) {
                online.sendSystemMessage(message);
            }
        }
        core.newPlayer().notifyDiscord(name, core.alert().config().discord.webhookUrl);
        access.log("New player joined for the first time: " + name);
    }

    public void onPlayerLeave(MinecraftServer server, ServerPlayer player) {
        if (core.vanish().isVanished(player.getGameProfile().getId())) {
            rememberVanishedName(player);
            recentlyVanishedNames.put(player.getGameProfile().getName().toLowerCase(Locale.ROOT), tickCounter + 200);
        }
        hiddenVanishPairs.removeIf(pair -> pair.startsWith(player.getGameProfile().getId() + ":") || pair.endsWith(":" + player.getGameProfile().getId()));
        syncVanishForAll(server);
        if (core.inspect().config().enabled) {
            core.inspect().recordLeave(player.getGameProfile().getId());
        }
        sendCustomJoinLeave(server, player, core.messages().config().joinLeave.leaveMessage, "left");
        freezeAnchors.remove(player.getGameProfile().getId());
        core.afk().forget(player.getGameProfile().getId());
        boolean anyoneElseOnline = server.getPlayerList().getPlayers().stream()
                .anyMatch(other -> !other.getGameProfile().getId().equals(player.getGameProfile().getId()));
        core.watchdog().setPlayersOnline(anyoneElseOnline);
    }

    public void onPlayerTrackingChanged(MinecraftServer server, ServerPlayer observer) {
        syncVanishFor(observer, server);
    }

    public boolean shouldSuppressGameMessage(MinecraftServer server, Component message, boolean overlay) {
        if (overlay) {
            return false;
        }
        if (!(message.getContents() instanceof TranslatableContents contents)) {
            return false;
        }
        String key = contents.getKey();
        if (!key.startsWith("multiplayer.player.joined") && !key.startsWith("multiplayer.player.left")) {
            return false;
        }
        // Custom join/leave messages replace the vanilla ones entirely (sent from onPlayerJoin/Leave).
        if (core.messages().config().joinLeave.enabled) {
            return true;
        }
        if (!core.vanish().config().enabled || !core.vanish().config().suppressJoinLeaveMessages) {
            return false;
        }
        String text = message.getString();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (core.vanish().isVanished(player.getGameProfile().getId()) && text.contains(player.getGameProfile().getName())) {
                return true;
            }
        }
        for (String name : knownVanishedNames.values()) {
            if (text.contains(name)) {
                return true;
            }
        }
        for (String name : recentlyVanishedNames.keySet()) {
            if (text.toLowerCase(Locale.ROOT).contains(name)) {
                return true;
            }
        }
        return false;
    }

    public void onServerTick(MinecraftServer server) {
        tickCounter++;
        core.watchdog().recordHeartbeat();
        tickFreeze(server);
        if (tickCounter % 20 != 0) {
            return;
        }

        tickMaintenanceCountdown(server);
        tickAnnouncements(server);
        tickBanItems(server);
        tickDupeScan(server);
        tickMotdRotation(server);
        tickSweep(server);
        tickAlert(server);
        tickPunishments(server);
        tickAfk(server);
        if (tickCounter % 40 == 0) {
            syncVanishForAll(server);
        }
        recentlyVanishedNames.entrySet().removeIf(entry -> entry.getValue() <= tickCounter);
    }

    private com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> maintenanceCommand() {
        return Commands.literal("maintenance")
                .requires(source -> access.hasPermission(source, PERMISSION_MAINTENANCE_MANAGE, 3))
                .executes(context -> sendMaintenanceStatus(context.getSource()))
                .then(Commands.literal("status").executes(context -> sendMaintenanceStatus(context.getSource())))
                .then(Commands.literal("on")
                        .executes(context -> enableMaintenance(context.getSource(), null))
                        .then(Commands.argument("message", StringArgumentType.greedyString())
                                .executes(context -> enableMaintenance(context.getSource(), StringArgumentType.getString(context, "message")))))
                .then(Commands.literal("in")
                        .then(Commands.argument("duration", StringArgumentType.word())
                                .executes(context -> startMaintenanceCountdown(context.getSource(), StringArgumentType.getString(context, "duration"), null))
                                .then(Commands.argument("message", StringArgumentType.greedyString())
                                        .executes(context -> startMaintenanceCountdown(context.getSource(),
                                                StringArgumentType.getString(context, "duration"),
                                                StringArgumentType.getString(context, "message"))))))
                .then(Commands.literal("cancel").executes(context -> cancelMaintenanceCountdown(context.getSource())))
                .then(Commands.literal("off").executes(context -> disableMaintenance(context.getSource())));
    }

    private com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> whoisCommand() {
        return Commands.literal("whois")
                .requires(source -> access.hasPermission(source, PERMISSION_INSPECT_USE, 3))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> sendWhois(context.getSource(), EntityArgument.getPlayer(context, "player"))));
    }

    private com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> freezeCommand() {
        return Commands.literal("freeze")
                .requires(source -> access.hasPermission(source, PERMISSION_FREEZE_USE, 3))
                .then(Commands.literal("list").executes(context -> listFrozen(context.getSource())))
                .then(Commands.literal("goto")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> teleportToFrozen(context.getSource(), EntityArgument.getPlayer(context, "player")))))
                .then(Commands.literal("evidence")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> sendFreezeEvidence(context.getSource(), EntityArgument.getPlayer(context, "player")))))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> toggleFreeze(context.getSource(), EntityArgument.getPlayer(context, "player"), null))
                        .then(Commands.argument("reason", StringArgumentType.greedyString())
                                .executes(context -> toggleFreeze(context.getSource(), EntityArgument.getPlayer(context, "player"),
                                        StringArgumentType.getString(context, "reason")))));
    }

    private com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> findCommand() {
        return Commands.literal("find")
                .requires(source -> access.hasPermission(source, PERMISSION_INSPECT_USE, 3))
                .then(Commands.argument("item", StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(BuiltInRegistries.ITEM.keySet(), builder))
                        .executes(context -> findItem(context.getSource(), StringArgumentType.getString(context, "item"))));
    }

    private com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> banItemCommand() {
        return Commands.literal("banitem")
                .requires(source -> access.hasPermission(source, PERMISSION_BANITEM_MANAGE, 3))
                .then(Commands.literal("list").executes(context -> listBannedItems(context.getSource())))
                .then(Commands.argument("item", StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(BuiltInRegistries.ITEM.keySet(), builder))
                        .executes(context -> toggleBanItem(context.getSource(), StringArgumentType.getString(context, "item"), null))
                        .then(Commands.argument("duration", StringArgumentType.word())
                                .executes(context -> toggleBanItem(context.getSource(),
                                        StringArgumentType.getString(context, "item"),
                                        StringArgumentType.getString(context, "duration")))));
    }

    private com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> kickCommand() {
        return Commands.literal("kick")
                .requires(source -> access.hasPermission(source, PERMISSION_PUNISH_KICK, 3))
                .then(Commands.argument("target", GameProfileArgument.gameProfile())
                        .executes(context -> kickPlayers(context.getSource(), GameProfileArgument.getGameProfiles(context, "target"), null))
                        .then(Commands.argument("reason", StringArgumentType.greedyString())
                                .executes(context -> kickPlayers(context.getSource(), GameProfileArgument.getGameProfiles(context, "target"),
                                        StringArgumentType.getString(context, "reason")))));
    }

    private com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> banCommand() {
        return Commands.literal("ban")
                .requires(source -> access.hasPermission(source, PERMISSION_PUNISH_BAN, 3))
                .then(Commands.argument("target", GameProfileArgument.gameProfile())
                        .executes(context -> banPlayers(context.getSource(), GameProfileArgument.getGameProfiles(context, "target"), null, 0))
                        .then(Commands.argument("reason", StringArgumentType.greedyString())
                                .executes(context -> banPlayers(context.getSource(), GameProfileArgument.getGameProfiles(context, "target"),
                                        StringArgumentType.getString(context, "reason"), 0))));
    }

    private com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> tempbanCommand() {
        return Commands.literal("tempban")
                .requires(source -> access.hasPermission(source, PERMISSION_PUNISH_BAN, 3))
                .then(Commands.argument("target", GameProfileArgument.gameProfile())
                        .then(Commands.argument("duration", StringArgumentType.word())
                                .executes(context -> tempbanPlayers(context.getSource(), GameProfileArgument.getGameProfiles(context, "target"),
                                        StringArgumentType.getString(context, "duration"), null))
                                .then(Commands.argument("reason", StringArgumentType.greedyString())
                                        .executes(context -> tempbanPlayers(context.getSource(), GameProfileArgument.getGameProfiles(context, "target"),
                                                StringArgumentType.getString(context, "duration"),
                                                StringArgumentType.getString(context, "reason"))))));
    }

    private com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> unbanCommand() {
        return Commands.literal("unban")
                .requires(source -> access.hasPermission(source, PERMISSION_PUNISH_MANAGE, 3))
                .then(Commands.argument("target", GameProfileArgument.gameProfile())
                        .executes(context -> unbanPlayers(context.getSource(), GameProfileArgument.getGameProfiles(context, "target"))));
    }

    private com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> muteCommand() {
        return Commands.literal("mute")
                .requires(source -> access.hasPermission(source, PERMISSION_PUNISH_MUTE, 3))
                .then(Commands.argument("target", GameProfileArgument.gameProfile())
                        .executes(context -> mutePlayers(context.getSource(), GameProfileArgument.getGameProfiles(context, "target"), null, 0))
                        .then(Commands.argument("reason", StringArgumentType.greedyString())
                                .executes(context -> mutePlayers(context.getSource(), GameProfileArgument.getGameProfiles(context, "target"),
                                        StringArgumentType.getString(context, "reason"), 0))));
    }

    private com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> tempmuteCommand() {
        return Commands.literal("tempmute")
                .requires(source -> access.hasPermission(source, PERMISSION_PUNISH_MUTE, 3))
                .then(Commands.argument("target", GameProfileArgument.gameProfile())
                        .then(Commands.argument("duration", StringArgumentType.word())
                                .executes(context -> tempmutePlayers(context.getSource(), GameProfileArgument.getGameProfiles(context, "target"),
                                        StringArgumentType.getString(context, "duration"), null))
                                .then(Commands.argument("reason", StringArgumentType.greedyString())
                                        .executes(context -> tempmutePlayers(context.getSource(), GameProfileArgument.getGameProfiles(context, "target"),
                                                StringArgumentType.getString(context, "duration"),
                                                StringArgumentType.getString(context, "reason"))))));
    }

    private com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> unmuteCommand() {
        return Commands.literal("unmute")
                .requires(source -> access.hasPermission(source, PERMISSION_PUNISH_MANAGE, 3))
                .then(Commands.argument("target", GameProfileArgument.gameProfile())
                        .executes(context -> unmutePlayers(context.getSource(), GameProfileArgument.getGameProfiles(context, "target"))));
    }

    private com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> punishCommand() {
        return Commands.literal("punish")
                .requires(source -> access.hasPermission(source, PERMISSION_PUNISH_MANAGE, 3))
                .then(Commands.argument("target", EntityArgument.player())
                        .executes(context -> escalatePunish(context.getSource(), EntityArgument.getPlayer(context, "target"), null))
                        .then(Commands.argument("reason", StringArgumentType.greedyString())
                                .executes(context -> escalatePunish(context.getSource(), EntityArgument.getPlayer(context, "target"),
                                        StringArgumentType.getString(context, "reason")))));
    }

    private com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> historyCommand() {
        return Commands.literal("history")
                .requires(source -> access.hasPermission(source, PERMISSION_PUNISH_MANAGE, 3))
                .then(Commands.argument("target", GameProfileArgument.gameProfile())
                        .executes(context -> sendHistory(context.getSource(), GameProfileArgument.getGameProfiles(context, "target"))));
    }

    private com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> reportsCommand() {
        return Commands.literal("reports")
                .requires(source -> access.hasPermission(source, PERMISSION_REPORT_MANAGE, 2))
                .executes(context -> listReports(context.getSource()))
                .then(Commands.literal("claim")
                        .then(Commands.argument("id", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1))
                                .executes(context -> claimReport(context.getSource(),
                                        com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "id")))))
                .then(Commands.literal("resolve")
                        .then(Commands.argument("id", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1))
                                .executes(context -> resolveReport(context.getSource(),
                                        com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "id")))));
    }

    private com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> afkCommand() {
        return Commands.literal("afk")
                .requires(source -> access.hasPermission(source, PERMISSION_AFK_USE, 0))
                .executes(context -> toggleSelfAfk(context.getSource()))
                .then(Commands.literal("list")
                        .requires(source -> access.hasPermission(source, PERMISSION_AFK_MANAGE, 2))
                        .executes(context -> listAfk(context.getSource())));
    }

    private com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> watchdogCommand() {
        return Commands.literal("watchdog")
                .requires(source -> access.hasPermission(source, PERMISSION_WATCHDOG_NOTIFY, 3))
                .executes(context -> sendWatchdogStatus(context.getSource()));
    }

    private com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> dupeCommand() {
        return Commands.literal("dupe")
                .requires(source -> access.hasPermission(source, PERMISSION_DUPE_MANAGE, 3))
                .executes(context -> sendDupeStatus(context.getSource()))
                .then(Commands.literal("status").executes(context -> sendDupeStatus(context.getSource())))
                .then(Commands.literal("alerts").executes(context -> sendDupeAlerts(context.getSource())))
                .then(Commands.literal("clear")
                        .then(Commands.argument("player", GameProfileArgument.gameProfile())
                                .executes(context -> clearDupeHistory(context.getSource(), GameProfileArgument.getGameProfiles(context, "player")))));
    }

    private com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> sweepCommand() {
        return Commands.literal("sweep")
                .requires(source -> access.hasPermission(source, PERMISSION_SWEEP_MANAGE, 3))
                .executes(context -> sendSweepStatus(context.getSource()))
                .then(Commands.literal("count").executes(context -> sweepCount(context.getSource())))
                .then(Commands.literal("now").executes(context -> sweepNow(context.getSource())))
                .then(Commands.literal("here").executes(context -> sweepHere(context.getSource())))
                .then(Commands.literal("on").executes(context -> setSweepEnabled(context.getSource(), true)))
                .then(Commands.literal("off").executes(context -> setSweepEnabled(context.getSource(), false)));
    }

    private com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> alertCommand() {
        return Commands.literal("alert")
                .requires(source -> access.hasPermission(source, PERMISSION_ALERT_MANAGE, 3))
                .executes(context -> sendAlertStatus(context.getSource()))
                .then(Commands.literal("on").executes(context -> setAlertEnabled(context.getSource(), true)))
                .then(Commands.literal("off").executes(context -> setAlertEnabled(context.getSource(), false)));
    }

    private com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> vanishCommand() {
        return Commands.literal("vanish")
                .requires(source -> access.hasPermission(source, PERMISSION_VANISH_USE, 3))
                .executes(context -> toggleVanish(context.getSource()))
                .then(Commands.literal("list")
                        .executes(context -> listVanished(context.getSource())))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> toggleVanish(context.getSource(), EntityArgument.getPlayer(context, "player"))));
    }

    private int sendStatus(CommandSourceStack source) {
        for (String line : core.statusLines()) {
            source.sendSuccess(() -> Component.literal(line), false);
        }
        return 1;
    }

    private int reload(CommandSourceStack source) {
        String message = core.reload();
        resetSweepTimer();
        announceCountdownSeconds = -1;
        motdRotateCountdownSeconds = -1;
        armDupeGracePeriod();
        applyMaintenanceMotd(source.getServer());
        syncVanishForAll(source.getServer());
        source.sendSuccess(() -> Component.literal(message), false);
        return 1;
    }

    private int sendMaintenanceStatus(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("Maintenance: " + (core.maintenance().isEnabled() ? "on" : "off")), false);
        if (core.maintenance().isEnabled()) {
            source.sendSuccess(() -> Component.literal("Message: " + core.maintenance().currentMessage()), false);
        }
        if (maintenanceCountdownSeconds > 0) {
            source.sendSuccess(() -> Component.literal("Countdown: maintenance in " + Durations.format(maintenanceCountdownSeconds)), false);
        }
        return 1;
    }

    private int enableMaintenance(CommandSourceStack source, String message) {
        maintenanceCountdownSeconds = -1;
        String result = core.maintenance().enable(message);
        applyMaintenanceMotd(source.getServer());
        kickNonExemptPlayers(source.getServer());
        source.sendSuccess(() -> Component.literal(result), true);
        return 1;
    }

    private int startMaintenanceCountdown(CommandSourceStack source, String duration, String message) {
        if (!core.maintenance().config().enabled) {
            source.sendFailure(Component.literal("Maintenance module is disabled in maintenance.json."));
            return 0;
        }
        if (core.maintenance().isEnabled()) {
            source.sendFailure(Component.literal("Maintenance mode is already on."));
            return 0;
        }
        int seconds = Durations.parseSeconds(duration);
        if (seconds < 0) {
            source.sendFailure(Component.literal("Invalid duration '" + duration + "'. Use e.g. 30s, 5m, 1h."));
            return 0;
        }
        maintenanceCountdownSeconds = seconds;
        maintenanceCountdownMessage = message;
        access.log("Maintenance countdown started: " + Durations.format(seconds));
        broadcastCountdown(source.getServer(), seconds, true);
        source.sendSuccess(() -> Component.literal("Maintenance countdown started: " + Durations.format(seconds)
                + (core.maintenance().config().stopServerAfterCountdown ? " (server will stop)" : "")), true);
        return 1;
    }

    private int cancelMaintenanceCountdown(CommandSourceStack source) {
        if (maintenanceCountdownSeconds <= 0) {
            source.sendFailure(Component.literal("No maintenance countdown is running."));
            return 0;
        }
        maintenanceCountdownSeconds = -1;
        maintenanceCountdownMessage = null;
        for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
            player.sendSystemMessage(Component.literal(TextFormat.legacyColors("&aMaintenance countdown cancelled.")));
        }
        source.sendSuccess(() -> Component.literal("Maintenance countdown cancelled."), true);
        return 1;
    }

    private void tickMaintenanceCountdown(MinecraftServer server) {
        if (maintenanceCountdownSeconds < 0) {
            return;
        }
        maintenanceCountdownSeconds--;
        if (maintenanceCountdownSeconds <= 0) {
            String message = maintenanceCountdownMessage;
            maintenanceCountdownSeconds = -1;
            maintenanceCountdownMessage = null;
            core.maintenance().enable(message);
            applyMaintenanceMotd(server);
            kickNonExemptPlayers(server);
            access.log("Maintenance countdown finished; maintenance mode enabled.");
            if (core.maintenance().config().stopServerAfterCountdown) {
                access.log("Stopping server (stopServerAfterCountdown).");
                server.halt(false);
            }
            return;
        }
        broadcastCountdown(server, maintenanceCountdownSeconds, false);
    }

    private void tickAnnouncements(MinecraftServer server) {
        AnnounceConfig config = core.announce().config();
        if (!config.enabled || config.messages.isEmpty()) {
            announceCountdownSeconds = -1;
            return;
        }
        if (announceCountdownSeconds < 0) {
            announceCountdownSeconds = Math.max(60, config.intervalMinutes * 60);
        }
        announceCountdownSeconds--;
        if (announceCountdownSeconds > 0) {
            return;
        }
        announceCountdownSeconds = Math.max(60, config.intervalMinutes * 60);
        if (server.getPlayerList().getPlayers().isEmpty()) {
            return;
        }
        String message = core.announce().nextMessage();
        if (message == null) {
            return;
        }
        Component component = Component.literal(TextFormat.legacyColors(config.prefix + message));
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.sendSystemMessage(component);
        }
    }

    private void tickMotdRotation(MinecraftServer server) {
        MessagesConfig.Motd config = core.messages().config().motd;
        if (!config.enabled || config.motds.isEmpty()) {
            motdRotateCountdownSeconds = -1;
            return;
        }
        if (core.maintenance().isEnabled()) {
            return; // the maintenance MOTD takes priority
        }
        if (motdRotateCountdownSeconds < 0) {
            motdRotateCountdownSeconds = 1;
        }
        motdRotateCountdownSeconds--;
        if (motdRotateCountdownSeconds > 0) {
            return;
        }
        motdRotateCountdownSeconds = Math.max(10, config.rotateMinutes * 60);
        String next = core.messages().nextMotd();
        if (next == null) {
            return;
        }
        if (originalMotd == null) {
            originalMotd = server.getMotd();
        }
        server.setMotd(TextFormat.legacyColors(next));
        server.invalidateStatus();
    }

    private void broadcastCountdown(MinecraftServer server, int seconds, boolean force) {
        boolean isStep = false;
        for (int step : COUNTDOWN_STEPS) {
            if (step == seconds) {
                isStep = true;
                break;
            }
        }
        if (!isStep && !force) {
            return;
        }
        Component message = Component.literal(TextFormat.legacyColors(
                core.maintenance().config().countdownMessage.replace("{time}", Durations.format(seconds))));
        boolean actionbar = seconds <= 10 && !force;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.sendSystemMessage(message, actionbar);
        }
    }

    private int disableMaintenance(CommandSourceStack source) {
        String result = core.maintenance().disable();
        applyMaintenanceMotd(source.getServer());
        source.sendSuccess(() -> Component.literal(result), true);
        return 1;
    }

    private void kickNonExemptPlayers(MinecraftServer server) {
        for (ServerPlayer player : List.copyOf(server.getPlayerList().getPlayers())) {
            if (core.maintenance().shouldKick(player.getGameProfile().getName(), player.getGameProfile().getId(), hasMaintenanceBypass(player))) {
                player.connection.disconnect(Component.literal(core.maintenance().currentKickMessage()));
            }
        }
    }

    private boolean hasMaintenanceBypass(ServerPlayer player) {
        return core.maintenance().isAllowlisted(player.getGameProfile().getName(), player.getGameProfile().getId())
                || access.hasPermission(player, core.maintenance().config().bypassPermission, core.maintenance().config().opBypass ? 3 : -1);
    }

    private void applyMaintenanceMotd(MinecraftServer server) {
        if (originalMotd == null) {
            originalMotd = server.getMotd();
        }
        if (core.maintenance().isEnabled() && core.maintenance().config().motdEnabled) {
            server.setMotd(core.maintenance().currentMotd());
        } else {
            server.setMotd(originalMotd);
        }
        server.invalidateStatus();
    }

    private int sendSweepStatus(CommandSourceStack source) {
        SweepConfig config = core.sweep().config();
        source.sendSuccess(() -> Component.literal("Sweep: " + (config.enabled ? "on" : "off")), false);
        source.sendSuccess(() -> Component.literal("Items: " + (config.items.enabled ? "on" : "off")
                + ", interval " + config.items.intervalMinutes + "m, threshold " + config.items.maxGroundItems), false);
        source.sendSuccess(() -> Component.literal("Mobs: " + (config.mobs.enabled ? "on" : "off")
                + ", per-chunk cap " + config.mobs.perChunkCaps.defaultCap), false);
        return 1;
    }

    private int setSweepEnabled(CommandSourceStack source, boolean enabled) {
        core.sweep().setEnabled(enabled);
        resetSweepTimer();
        source.sendSuccess(() -> Component.literal("Sweep " + (enabled ? "enabled." : "disabled.")), true);
        return 1;
    }

    private int sweepCount(CommandSourceStack source) {
        SweepResult result = scanSweep(source.getServer(), false, null);
        source.sendSuccess(() -> Component.literal("Sweep dry-run: " + result.items + " items, " + result.mobs + " mobs."), false);
        return Math.max(1, result.items + result.mobs);
    }

    private int sweepNow(CommandSourceStack source) {
        SweepResult result = runSweep(source.getServer(), "manual");
        source.sendSuccess(() -> Component.literal("Sweep removed " + result.items + " items and " + result.mobs + " mobs."), true);
        return Math.max(1, result.items + result.mobs);
    }

    private int sweepHere(CommandSourceStack source) {
        Entity entity = source.getEntity();
        if (entity == null) {
            source.sendFailure(Component.literal("Only entities can use /fiw sweep here."));
            return 0;
        }
        SweepResult result = runSweep(source.getServer(), "chunk", entity.level().dimension(), entity.chunkPosition());
        source.sendSuccess(() -> Component.literal("Chunk sweep removed " + result.items + " items and " + result.mobs + " mobs."), true);
        return Math.max(1, result.items + result.mobs);
    }

    private void resetSweepTimer() {
        itemCleanCountdownSeconds = Math.max(1, core.sweep().config().items.intervalMinutes * 60);
    }

    private void tickSweep(MinecraftServer server) {
        SweepConfig config = core.sweep().config();
        if (!config.enabled) {
            return;
        }

        if (config.items.enabled && config.items.timerCleanEnabled) {
            if (itemCleanCountdownSeconds < 0) {
                resetSweepTimer();
            }
            maybeWarnSweep(server, itemCleanCountdownSeconds);
            itemCleanCountdownSeconds--;
            if (itemCleanCountdownSeconds <= 0) {
                SweepResult result = runSweep(server, "timer");
                announceSweep(server, "Sweep removed " + result.items + " items and " + result.mobs + " mobs.");
                resetSweepTimer();
            }
        }

        if (tickCounter % 200 == 0 && config.items.enabled && config.items.thresholdCleanEnabled) {
            int groundItems = countGroundItems(server);
            if (groundItems >= config.items.maxGroundItems) {
                SweepResult result = runSweep(server, "threshold");
                announceSweep(server, "Threshold sweep removed " + result.items + " items and " + result.mobs + " mobs.");
                resetSweepTimer();
            }
        }

        int mobInterval = Math.max(1, config.mobs.checkIntervalSeconds) * 20;
        if (config.mobs.enabled && tickCounter % mobInterval == 0) {
            SweepResult result = runMobSweep(server);
            if (result.mobs > 0) {
                announceSweep(server, "Mob cap sweep removed " + result.mobs + " mobs.");
            }
        }
    }

    private void maybeWarnSweep(MinecraftServer server, int seconds) {
        SweepConfig.Warnings warnings = core.sweep().config().warnings;
        if (!warnings.enabled) {
            return;
        }
        if (warnings.chatSteps.contains(seconds)) {
            String message = TextFormat.legacyColors(warnings.message.replace("{seconds}", Integer.toString(seconds)));
            broadcastSweep(server, Component.literal(message), false);
        }
        if (seconds <= warnings.actionbarFinalCountdown && seconds > 0) {
            Component component = Component.literal(TextFormat.legacyColors(warnings.message.replace("{seconds}", Integer.toString(seconds))));
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                player.sendSystemMessage(component, true);
            }
        }
    }

    private SweepResult runSweep(MinecraftServer server, String reason) {
        return runSweep(server, reason, null, null);
    }

    private SweepResult runSweep(MinecraftServer server, String reason, net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> onlyDimension, ChunkPos onlyChunk) {
        SweepResult result = scanSweep(server, true, onlyDimension == null ? null : new ChunkFilter(onlyDimension, onlyChunk));
        access.log("Sweep " + reason + " removed " + result.items + " items and " + result.mobs + " mobs");
        return result;
    }

    private SweepResult runMobSweep(MinecraftServer server) {
        SweepResult result = scanSweep(server, true, null);
        result.items = 0;
        return result;
    }

    private SweepResult scanSweep(MinecraftServer server, boolean remove, ChunkFilter filter) {
        SweepResult result = new SweepResult();
        List<Entity> mobsToRemove = collectMobsToRemove(server, filter);
        for (ServerLevel level : server.getAllLevels()) {
            if (filter != null && !filter.matchesDimension(level)) {
                continue;
            }
            for (Entity entity : level.getAllEntities()) {
                if (filter != null && !filter.matchesChunk(entity.chunkPosition())) {
                    continue;
                }
                if (entity instanceof ItemEntity item && canSweepItem(item)) {
                    result.items++;
                    if (remove) {
                        item.remove(Entity.RemovalReason.DISCARDED);
                    }
                }
            }
        }
        result.mobs = mobsToRemove.size();
        if (remove) {
            for (Entity entity : mobsToRemove) {
                entity.remove(Entity.RemovalReason.DISCARDED);
            }
        }
        return result;
    }

    private int countGroundItems(MinecraftServer server) {
        int count = 0;
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof ItemEntity) {
                    count++;
                }
            }
        }
        return count;
    }

    private boolean canSweepItem(ItemEntity item) {
        SweepConfig.Items config = core.sweep().config().items;
        if (item.tickCount < config.minItemAgeSeconds * 20) {
            return false;
        }
        if (config.exemptNamedItems && item.getItem().has(DataComponents.CUSTOM_NAME)) {
            return false;
        }
        String id = BuiltInRegistries.ITEM.getKey(item.getItem().getItem()).toString();
        if (!config.onlyTheseItems.isEmpty() && !containsIgnoreCase(config.onlyTheseItems, id)) {
            return false;
        }
        return !containsIgnoreCase(config.ignoredItems, id);
    }

    private List<Entity> collectMobsToRemove(MinecraftServer server, ChunkFilter filter) {
        SweepConfig.Mobs config = core.sweep().config().mobs;
        List<Mob> candidates = new ArrayList<>();
        for (ServerLevel level : server.getAllLevels()) {
            if (filter != null && !filter.matchesDimension(level)) {
                continue;
            }
            for (Entity entity : level.getAllEntities()) {
                if (filter != null && !filter.matchesChunk(entity.chunkPosition())) {
                    continue;
                }
                if (entity instanceof Mob mob && canSweepMob(mob)) {
                    candidates.add(mob);
                }
            }
        }

        Set<Entity> remove = new HashSet<>();
        if (config.perChunkCaps.enabled) {
            Map<String, Integer> counts = new HashMap<>();
            for (Mob mob : candidates) {
                String key = mob.level().dimension().location() + ":" + chunkX(mob.chunkPosition()) + "," + chunkZ(mob.chunkPosition());
                int count = counts.merge(key, 1, Integer::sum);
                if (count > config.perChunkCaps.defaultCap) {
                    remove.add(mob);
                }
            }
        }
        if (config.globalCapsEnabled) {
            Map<String, Integer> counts = new HashMap<>();
            for (Mob mob : candidates) {
                String key = EntityType.getKey(mob.getType()).toString();
                int count = counts.merge(key, 1, Integer::sum);
                int cap = config.globalCaps.getOrDefault(key, Integer.MAX_VALUE);
                if (count > cap) {
                    remove.add(mob);
                }
            }
        }
        return new ArrayList<>(remove);
    }

    private boolean canSweepMob(Mob mob) {
        SweepConfig.Mobs config = core.sweep().config().mobs;
        String id = EntityType.getKey(mob.getType()).toString();
        if (containsIgnoreCase(config.neverClean, id)) {
            return false;
        }
        if (config.exemptNamed && mob.hasCustomName()) {
            return false;
        }
        if (config.exemptPersistent && mob.isPersistenceRequired()) {
            return false;
        }
        if (config.exemptTamed && mob instanceof TamableAnimal tameable && tameable.isTame()) {
            return false;
        }
        return !(config.exemptLeashed && mob instanceof Leashable leashable && leashable.isLeashed());
    }

    private void announceSweep(MinecraftServer server, String message) {
        broadcastSweep(server, Component.literal(message), true);
    }

    private void broadcastSweep(MinecraftServer server, Component component, boolean respectMode) {
        String mode = core.sweep().config().announceResults.toLowerCase(Locale.ROOT);
        if (respectMode && mode.equals("off")) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!respectMode || mode.equals("all") || access.hasPermission(player, core.sweep().config().notifyPermission, 3)) {
                player.sendSystemMessage(component);
            }
        }
    }

    private int sendAlertStatus(CommandSourceStack source) {
        AlertConfig config = core.alert().config();
        source.sendSuccess(() -> Component.literal("Alert: " + (config.enabled ? "on" : "off")
                + ", threshold " + config.tpsThreshold + " TPS, cooldown " + config.cooldownMinutes + "m"), false);
        return 1;
    }

    private int setAlertEnabled(CommandSourceStack source, boolean enabled) {
        core.alert().setEnabled(enabled);
        source.sendSuccess(() -> Component.literal("Alert " + (enabled ? "enabled." : "disabled.")), true);
        return 1;
    }

    private void tickAlert(MinecraftServer server) {
        AlertConfig config = core.alert().config();
        if (!config.enabled) {
            return;
        }
        int interval = Math.max(1, config.checkIntervalSeconds) * 20;
        if (tickCounter % interval != 0) {
            return;
        }
        double tps = currentTps(server);
        if (tps >= config.tpsThreshold) {
            return;
        }
        long cooldownTicks = (long) config.cooldownMinutes * 60L * 20L;
        if (tps > config.escalateBelowTps && lastAlertTick >= 0 && tickCounter - lastAlertTick < cooldownTicks) {
            return;
        }
        lastAlertTick = tickCounter;
        List<Component> report = buildLagReport(server, tps);
        List<String> plainLines = new ArrayList<>();
        for (Component line : report) {
            plainLines.add(line.getString());
        }
        core.alert().recordAlert(tps, plainLines);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (access.hasPermission(player, config.notifyPermission, 3)) {
                for (Component line : report) {
                    player.sendSystemMessage(line);
                }
                if (config.playSound) {
                    player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.7f, 0.55f);
                }
            }
        }
    }

    private int sendLagReport(CommandSourceStack source) {
        for (Component line : buildLagReport(source.getServer(), currentTps(source.getServer()))) {
            source.sendSuccess(() -> line, false);
        }
        return 1;
    }

    private int sendLagHistory(CommandSourceStack source) {
        List<AlertHistory.Entry> entries = core.alert().history();
        if (entries.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No TPS alerts recorded."), false);
            return 1;
        }
        source.sendSuccess(() -> Component.literal("Last " + entries.size() + " TPS alerts (newest first):"), false);
        for (int i = entries.size() - 1; i >= 0; i--) {
            AlertHistory.Entry entry = entries.get(i);
            String when = SEEN_FORMAT.format(Instant.ofEpochMilli(entry.timeMillis).atZone(ZoneId.systemDefault()));
            String top = entry.lines.size() > 1 ? " — " + entry.lines.get(1) : "";
            Component line = Component.literal(String.format(Locale.ROOT, "%s — %.2f TPS%s", when, entry.tps, top));
            source.sendSuccess(() -> line, false);
        }
        return entries.size();
    }

    private double currentTps(MinecraftServer server) {
        long nanos = Math.max(1L, server.getAverageTickTimeNanos());
        return Math.min(20.0, 1_000_000_000.0 / nanos);
    }

    private List<Component> buildLagReport(MinecraftServer server, double tps) {
        List<ChunkReport> reports = scanChunkReports(server);
        int limit = Math.max(1, core.alert().config().report.topChunks);
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal(String.format(Locale.ROOT, "TPS alert: %.2f TPS", tps)).withStyle(ChatFormatting.RED));
        for (int i = 0; i < Math.min(limit, reports.size()); i++) {
            ChunkReport report = reports.get(i);
            Component line = Component.literal("#" + (i + 1) + " " + report.dimension + " [" + report.chunkX + ", " + report.chunkZ + "] "
                    + report.entities + " entities, " + report.blockEntities + " block entities, top " + report.dominantType);
            if (core.alert().config().report.clickableTeleport) {
                int x = report.chunkX * 16 + 8;
                int z = report.chunkZ * 16 + 8;
                line = Component.literal("").append(line).append(" ").append(Component.literal("[Teleport]")
                        .withStyle(style -> style.withColor(ChatFormatting.AQUA)
                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                        "/execute in " + report.dimension + " run tp @s " + x + " ~ " + z))));
            }
            if (core.alert().config().report.clickableSweep && core.sweep().isEnabled()) {
                line = Component.literal("").append(line).append(" ").append(Component.literal("[Sweep here]")
                        .withStyle(style -> style.withColor(ChatFormatting.YELLOW)
                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                        "/execute in " + report.dimension + " positioned " + (report.chunkX * 16 + 8)
                                                + " ~ " + (report.chunkZ * 16 + 8) + " run fiw sweep here"))));
            }
            lines.add(line);
        }
        if (reports.isEmpty()) {
            lines.add(Component.literal("No loaded entity chunks found."));
        }
        return lines;
    }

    private List<ChunkReport> scanChunkReports(MinecraftServer server) {
        Map<String, ChunkReport> reports = new LinkedHashMap<>();
        for (ServerLevel level : server.getAllLevels()) {
            String dimension = level.dimension().location().toString();
            if (core.alert().config().report.scanEntities) {
                for (Entity entity : level.getAllEntities()) {
                    ChunkPos pos = entity.chunkPosition();
                    int chunkX = chunkX(pos);
                    int chunkZ = chunkZ(pos);
                    ChunkReport report = reports.computeIfAbsent(dimension + ":" + chunkX + ":" + chunkZ,
                            ignored -> new ChunkReport(dimension, chunkX, chunkZ));
                    report.entities++;
                    report.addType(EntityType.getKey(entity.getType()).toString());
                }
            }
            if (core.alert().config().report.scanBlockEntities) {
                for (ChunkHolder holder : ((ChunkMapAccessor) level.getChunkSource().chunkMap).fiw$getChunks()) {
                    LevelChunk chunk = holder.getChunkToSend();
                    if (chunk == null) {
                        continue;
                    }
                    int chunkX = chunkX(chunk.getPos());
                    int chunkZ = chunkZ(chunk.getPos());
                    ChunkReport report = reports.computeIfAbsent(dimension + ":" + chunkX + ":" + chunkZ,
                            ignored -> new ChunkReport(dimension, chunkX, chunkZ));
                    report.blockEntities += chunk.getBlockEntities().size();
                    for (var blockEntity : chunk.getBlockEntities().values()) {
                        report.addType(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(blockEntity.getType()).toString());
                    }
                }
            }
        }
        List<ChunkReport> sorted = new ArrayList<>(reports.values());
        sorted.sort(Comparator.comparingInt(ChunkReport::weight).reversed());
        for (ChunkReport report : sorted) {
            report.finish();
        }
        return sorted;
    }

    private int sendWhois(CommandSourceStack source, ServerPlayer target) {
        if (!core.inspect().config().enabled) {
            source.sendFailure(Component.literal("Inspect is disabled in inspect.json."));
            return 0;
        }
        String name = target.getGameProfile().getName();
        String dimension = target.level().dimension().location().toString();
        String position = String.format(Locale.ROOT, "%.0f %.0f %.0f", target.getX(), target.getY(), target.getZ());
        String gamemode = target.gameMode.getGameModeForPlayer().getName();
        String health = String.format(Locale.ROOT, "%.1f/%.1f", target.getHealth(), target.getMaxHealth());
        int food = target.getFoodData().getFoodLevel();
        int ping = target.connection.latency();

        InspectService.Seen seen = core.inspect().seenInfo(target.getGameProfile().getId());
        String firstSeen = seen == null ? "unknown"
                : SEEN_FORMAT.format(Instant.ofEpochMilli(seen.firstSeenMillis).atZone(ZoneId.systemDefault()));

        List<String> lines = List.of(
                "=== " + name + " ===",
                "Position: " + position + " (" + dimension + ")",
                "Gamemode: " + gamemode,
                "Health: " + health + "  Food: " + food + "/20",
                "Ping: " + ping + " ms",
                "First seen: " + firstSeen,
                "Last seen: online now"
        );
        for (String line : lines) {
            source.sendSuccess(() -> Component.literal(line), false);
        }
        return 1;
    }

    private static String normalizeItemId(String input) {
        String id = input.trim().toLowerCase(Locale.ROOT);
        return id.contains(":") ? id : "minecraft:" + id;
    }

    private static String itemId(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    private int findItem(CommandSourceStack source, String rawId) {
        if (!core.inspect().config().enabled || !core.inspect().config().findEnabled) {
            source.sendFailure(Component.literal("Find is disabled in inspect.json."));
            return 0;
        }
        String id = normalizeItemId(rawId);
        boolean includeEnder = core.inspect().config().findIncludeEnderChests;
        source.sendSuccess(() -> Component.literal("Searching " + id + (includeEnder ? " (inventories + ender chests):" : " (inventories):")), false);
        int total = 0;
        int holders = 0;
        for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
            int inventory = countItems(player.getInventory(), id);
            int ender = includeEnder ? countItems(player.getEnderChestInventory(), id) : 0;
            if (inventory + ender == 0) {
                continue;
            }
            holders++;
            total += inventory + ender;
            String line = "- " + player.getGameProfile().getName() + ": " + inventory + " (inv)"
                    + (includeEnder && ender > 0 ? " + " + ender + " (ender)" : "");
            source.sendSuccess(() -> Component.literal(line), false);
        }
        if (holders == 0) {
            source.sendSuccess(() -> Component.literal("No online players have " + id + "."), false);
            return 1;
        }
        int totalFinal = total;
        int holdersFinal = holders;
        source.sendSuccess(() -> Component.literal("Total: " + totalFinal + " across " + holdersFinal + " player(s)."), false);
        return total;
    }

    private int countItems(Container container, String id) {
        int count = 0;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (!stack.isEmpty() && itemId(stack).equals(id)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private int toggleBanItem(CommandSourceStack source, String rawId, String duration) {
        if (!core.banItem().config().enabled) {
            source.sendFailure(Component.literal("BanItem is disabled in banitem.json."));
            return 0;
        }
        String id = normalizeItemId(rawId);
        if (core.banItem().isBanned(id) && duration == null) {
            core.banItem().unban(id);
            access.log("Item unbanned: " + id);
            source.sendSuccess(() -> Component.literal("Unbanned " + id + "."), true);
            return 1;
        }
        int seconds = 0;
        if (duration != null) {
            seconds = Durations.parseSeconds(duration);
            if (seconds < 0) {
                source.sendFailure(Component.literal("Invalid duration '" + duration + "'. Use e.g. 30s, 5m, 1h."));
                return 0;
            }
        }
        core.banItem().ban(id, seconds);
        String until = seconds > 0 ? " for " + Durations.format(seconds) : " until unbanned";
        access.log("Item banned: " + id + until);
        source.sendSuccess(() -> Component.literal("Banned " + id + until + "."), true);
        return 1;
    }

    private int listBannedItems(CommandSourceStack source) {
        core.banItem().purgeExpired();
        Map<String, Long> bans = core.banItem().bans();
        if (bans.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No banned items."), false);
            return 1;
        }
        source.sendSuccess(() -> Component.literal("Banned items (" + bans.size() + "):"), false);
        long now = System.currentTimeMillis();
        for (Map.Entry<String, Long> entry : bans.entrySet()) {
            String remaining = entry.getValue() == 0 ? "until unbanned"
                    : Durations.format((int) Math.max(1, (entry.getValue() - now) / 1000)) + " left";
            String line = "- " + entry.getKey() + " (" + remaining + ")";
            source.sendSuccess(() -> Component.literal(line), false);
        }
        return bans.size();
    }

    /** Called by the loaders before item/block use and attacks; sends the configured notice when blocked. */
    public boolean shouldBlockItemUse(ServerPlayer player, ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        BanItemConfig config = core.banItem().config();
        if (!config.enabled || !core.banItem().isBanned(itemId(stack))) {
            return false;
        }
        if (access.hasPermission(player, config.bypassPermission, -1)) {
            return false;
        }
        player.sendSystemMessage(Component.literal(TextFormat.legacyColors(config.blockedMessage)), true);
        return true;
    }

    private void tickBanItems(MinecraftServer server) {
        BanItemConfig config = core.banItem().config();
        if (!config.enabled) {
            return;
        }
        for (String expired : core.banItem().purgeExpired()) {
            access.log("Item ban expired: " + expired);
        }
        if (!config.confiscateFromInventory || core.banItem().activeBanCount() == 0) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (access.hasPermission(player, config.bypassPermission, -1)) {
                continue;
            }
            Inventory inventory = player.getInventory();
            for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
                ItemStack stack = inventory.getItem(slot);
                if (stack.isEmpty() || !core.banItem().isBanned(itemId(stack))) {
                    continue;
                }
                inventory.setItem(slot, ItemStack.EMPTY);
                player.drop(stack, false);
                player.sendSystemMessage(Component.literal(TextFormat.legacyColors(config.blockedMessage)), true);
            }
        }
    }

    public boolean isActionFrozen(ServerPlayer player) {
        return core.freeze().config().enabled && core.freeze().isFrozen(player.getGameProfile().getId());
    }

    public boolean shouldBlockInteraction(ServerPlayer player) {
        return isActionFrozen(player) && core.freeze().config().blockInteractions;
    }

    /** Called by the loaders before an outgoing chat message is delivered; sends the mute notice when blocked. */
    public boolean shouldBlockChat(ServerPlayer player) {
        recordActivity(player);
        if (!core.punishment().config().enabled) {
            return false;
        }
        var mute = core.punishment().activeMute(player.getGameProfile().getId());
        if (mute == null) {
            return false;
        }
        player.sendSystemMessage(Component.literal(TextFormat.legacyColors(core.punishment().config().defaultMuteMessage)));
        return true;
    }

    /** Called by the loaders on player actions (block/item/attack/chat/movement) to feed AFK tracking. */
    public void recordActivity(ServerPlayer player) {
        core.afk().recordActivity(player.getGameProfile().getId());
    }

    private String formatBanKickMessage(PunishmentService.Sanction ban) {
        String template = ban.expiryMillis == 0 ? core.punishment().config().defaultBanMessage
                : core.punishment().config().defaultBanMessage + " ({remaining} remaining)";
        String remaining = ban.expiryMillis == 0 ? "" : Durations.format((int) Math.max(1, (ban.expiryMillis - System.currentTimeMillis()) / 1000));
        String reason = ban.reason == null || ban.reason.isBlank() ? "No reason given." : ban.reason;
        return TextFormat.legacyColors(template.replace("{reason}", reason).replace("{remaining}", remaining));
    }

    private int toggleFreeze(CommandSourceStack source, ServerPlayer target, String reason) {
        FreezeConfig config = core.freeze().config();
        if (!config.enabled) {
            source.sendFailure(Component.literal("Freeze is disabled in freeze.json."));
            return 0;
        }
        if (core.freeze().isFrozen(target.getGameProfile().getId())) {
            core.freeze().unfreeze(target.getGameProfile().getId());
            freezeAnchors.remove(target.getGameProfile().getId());
            if (config.notifyTarget) {
                target.sendSystemMessage(Component.literal(TextFormat.legacyColors(config.unfrozenMessage)));
            }
            access.log("Unfroze " + target.getGameProfile().getName());
            source.sendSuccess(() -> Component.literal(target.getGameProfile().getName() + " is no longer frozen."), true);
            return 1;
        }
        if (config.reasonRequired && (reason == null || reason.isBlank())) {
            source.sendFailure(Component.literal("A reason is required to freeze a player."));
            return 0;
        }
        String staffName = source.getTextName();
        String evidence = config.evidenceLogging ? buildFreezeEvidence(target) : "";
        core.freeze().freeze(target.getGameProfile().getId(), target.getGameProfile().getName(), staffName, reason, config.autoUnfreezeSeconds, evidence);
        anchorFrozenPlayer(target);
        if (config.notifyTarget) {
            target.sendSystemMessage(Component.literal(TextFormat.legacyColors(config.frozenMessage)));
        }
        if (config.teleportToStaffOnFreeze && source.getEntity() instanceof ServerPlayer staffPlayer) {
            target.teleportTo((ServerLevel) staffPlayer.level(), staffPlayer.getX(), staffPlayer.getY(), staffPlayer.getZ(),
                    Set.<net.minecraft.world.entity.RelativeMovement>of(), staffPlayer.getYRot(), staffPlayer.getXRot());
            anchorFrozenPlayer(target);
        }
        String logLine = staffName + " froze " + target.getGameProfile().getName()
                + (reason != null && !reason.isBlank() ? ": " + reason : "");
        access.log(logLine);
        core.freeze().notifyDiscord(logLine + (evidence.isBlank() ? "" : "\n" + evidence));
        source.sendSuccess(() -> Component.literal(target.getGameProfile().getName() + " is now frozen."), true);
        return 1;
    }

    private String buildFreezeEvidence(ServerPlayer player) {
        StringBuilder builder = new StringBuilder();
        builder.append("held: ").append(itemId(player.getMainHandItem()));
        builder.append(", pos: ").append(player.blockPosition().toShortString());
        builder.append(", gamemode: ").append(player.gameMode.getGameModeForPlayer().getName());
        List<String> items = new ArrayList<>();
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty()) {
                items.add(stack.getCount() + "x " + itemId(stack));
            }
        }
        if (!items.isEmpty()) {
            builder.append(", inventory: ").append(String.join(", ", items));
        }
        return builder.toString();
    }

    private int teleportToFrozen(CommandSourceStack source, ServerPlayer target) {
        if (!core.freeze().isFrozen(target.getGameProfile().getId())) {
            source.sendFailure(Component.literal(target.getGameProfile().getName() + " is not frozen."));
            return 0;
        }
        if (!(source.getEntity() instanceof ServerPlayer staffPlayer)) {
            source.sendFailure(Component.literal("Only players can use /fiw freeze goto."));
            return 0;
        }
        staffPlayer.teleportTo((ServerLevel) target.level(), target.getX(), target.getY(), target.getZ(),
                Set.<net.minecraft.world.entity.RelativeMovement>of(), target.getYRot(), target.getXRot());
        source.sendSuccess(() -> Component.literal("Teleported to " + target.getGameProfile().getName() + "."), false);
        return 1;
    }

    private int sendFreezeEvidence(CommandSourceStack source, ServerPlayer target) {
        var detail = core.freeze().detail(target.getGameProfile().getId());
        if (detail == null || detail.evidence.isBlank()) {
            source.sendSuccess(() -> Component.literal("No evidence recorded for " + target.getGameProfile().getName() + "."), false);
            return 1;
        }
        source.sendSuccess(() -> Component.literal(target.getGameProfile().getName() + " evidence: " + detail.evidence), false);
        return 1;
    }

    private int listFrozen(CommandSourceStack source) {
        if (core.freeze().frozenCount() == 0) {
            source.sendSuccess(() -> Component.literal("No frozen players."), false);
            return 1;
        }
        source.sendSuccess(() -> Component.literal("Frozen players (" + core.freeze().frozenCount() + "):"), false);
        for (Map.Entry<String, String> entry : core.freeze().frozenEntries()) {
            String name = entry.getValue() == null || entry.getValue().isBlank() ? "unknown" : entry.getValue();
            UUID uuid = parseUuidOrNull(entry.getKey());
            var detail = uuid == null ? null : core.freeze().detail(uuid);
            String reasonSuffix = detail != null && !detail.reason.isBlank() ? " - " + detail.reason : "";
            source.sendSuccess(() -> Component.literal("- " + name + " (" + entry.getKey() + ")" + reasonSuffix), false);
        }
        return core.freeze().frozenCount();
    }

    private UUID parseUuidOrNull(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private void anchorFrozenPlayer(ServerPlayer player) {
        freezeAnchors.put(player.getGameProfile().getId(), new FreezeAnchor(
                player.level().dimension(), player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot()));
    }

    private void tickFreeze(MinecraftServer server) {
        if (!core.freeze().config().enabled || core.freeze().frozenCount() == 0) {
            return;
        }
        if (tickCounter % 20 == 0) {
            for (UUID uuid : core.freeze().expiredUuids()) {
                core.freeze().unfreeze(uuid);
                freezeAnchors.remove(uuid);
                ServerPlayer expiredPlayer = server.getPlayerList().getPlayer(uuid);
                if (expiredPlayer != null && core.freeze().config().notifyTarget) {
                    expiredPlayer.sendSystemMessage(Component.literal(TextFormat.legacyColors(core.freeze().config().unfrozenMessage)));
                }
                access.log("Auto-unfroze " + (expiredPlayer != null ? expiredPlayer.getGameProfile().getName() : uuid));
            }
        }
        for (UUID uuid : core.freeze().frozenUuids()) {
            ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            if (player == null) {
                continue;
            }
            FreezeAnchor anchor = freezeAnchors.get(uuid);
            if (anchor == null || !player.level().dimension().equals(anchor.dimension)) {
                anchorFrozenPlayer(player);
                continue;
            }
            double dx = player.getX() - anchor.x;
            double dy = player.getY() - anchor.y;
            double dz = player.getZ() - anchor.z;
            if (dx * dx + dy * dy + dz * dz > 0.0225) {
                player.connection.teleport(anchor.x, anchor.y, anchor.z, anchor.yRot, anchor.xRot);
            }
        }
    }

    private record FreezeAnchor(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension,
                                double x, double y, double z, float yRot, float xRot) {
    }

    private void tickPunishments(MinecraftServer server) {
        if (!core.punishment().config().enabled) {
            return;
        }
        core.punishment().purgeExpired();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (core.punishment().activeBan(player.getGameProfile().getId()) != null) {
                String message = formatBanKickMessage(core.punishment().activeBan(player.getGameProfile().getId()));
                server.execute(() -> player.connection.disconnect(Component.literal(message)));
            }
        }
    }

    private void tickAfk(MinecraftServer server) {
        AfkConfig config = core.afk().config();
        if (!config.enabled) {
            return;
        }
        for (UUID uuid : core.afk().refresh()) {
            ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            String name = player != null ? player.getGameProfile().getName() : "A player";
            boolean nowAfk = core.afk().isAfk(uuid);
            if (config.broadcastOnChange) {
                String template = nowAfk ? config.afkMessage : config.backMessage;
                Component message = Component.literal(TextFormat.legacyColors(template.replace("{player}", name)));
                for (ServerPlayer online : server.getPlayerList().getPlayers()) {
                    online.sendSystemMessage(message);
                }
            }
        }
        if (config.kickAfterSeconds > 0) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                UUID uuid = player.getGameProfile().getId();
                if (!core.afk().isAfk(uuid) || access.hasPermission(player, config.exemptPermission, -1)) {
                    continue;
                }
                if (core.afk().idleSeconds(uuid) >= config.kickAfterSeconds) {
                    String message = TextFormat.legacyColors(config.kickMessage);
                    server.execute(() -> player.connection.disconnect(Component.literal(message)));
                }
            }
        }
    }

    private enum PunishKind { KICK, BAN, MUTE }

    private int applyToTargets(CommandSourceStack source, Collection<GameProfile> targets, PunishKind kind, String reason, int durationSeconds) {
        PunishmentConfig config = core.punishment().config();
        if (!config.enabled) {
            source.sendFailure(Component.literal("Punishments are disabled in punishment.json."));
            return 0;
        }
        if (targets.isEmpty()) {
            source.sendFailure(Component.literal("No matching player."));
            return 0;
        }
        if (config.reasonRequired && (reason == null || reason.isBlank())) {
            source.sendFailure(Component.literal("A reason is required."));
            return 0;
        }
        String staffName = source.getTextName();
        int count = 0;
        for (GameProfile target : targets) {
            applyPunishment(source.getServer(), target.getId(), target.getName(), staffName, kind, reason, durationSeconds);
            count++;
        }
        int total = count;
        source.sendSuccess(() -> Component.literal(kind.name() + " applied to " + total + " player(s)."), true);
        return count;
    }

    private void applyPunishment(MinecraftServer server, UUID uuid, String name, String staffName, PunishKind kind, String reason, int durationSeconds) {
        PunishmentConfig config = core.punishment().config();
        switch (kind) {
            case KICK -> {
                core.punishment().recordKick(uuid, name, staffName, reason);
                disconnectIfOnline(server, uuid, formatPunishMessage(config.defaultKickMessage, reason, 0));
            }
            case BAN -> {
                core.punishment().ban(uuid, name, staffName, reason, durationSeconds);
                disconnectIfOnline(server, uuid, formatPunishMessage(config.defaultBanMessage, reason, durationSeconds));
            }
            case MUTE -> core.punishment().mute(uuid, name, staffName, reason, durationSeconds);
        }
        String verb = switch (kind) {
            case KICK -> "kicked";
            case BAN -> durationSeconds > 0 ? "temp-banned" : "banned";
            case MUTE -> durationSeconds > 0 ? "temp-muted" : "muted";
        };
        String durationSuffix = durationSeconds > 0 ? " for " + Durations.format(durationSeconds) : "";
        String reasonSuffix = reason != null && !reason.isBlank() ? ": " + reason : "";
        String line = staffName + " " + verb + " " + name + durationSuffix + reasonSuffix;
        access.log(line);
        if (config.broadcastPunishments) {
            Component component = Component.literal(TextFormat.legacyColors("&7[Staff] &f" + line));
            for (ServerPlayer online : server.getPlayerList().getPlayers()) {
                if (access.hasPermission(online, config.notifyPermission, 3)) {
                    online.sendSystemMessage(component);
                }
            }
        }
        core.punishment().notifyDiscord(line);
    }

    private String formatPunishMessage(String template, String reason, int durationSeconds) {
        String reasonText = reason == null || reason.isBlank() ? "No reason given." : reason;
        String remaining = durationSeconds > 0 ? Durations.format(durationSeconds) : "permanent";
        return TextFormat.legacyColors(template.replace("{reason}", reasonText).replace("{remaining}", remaining));
    }

    private void disconnectIfOnline(MinecraftServer server, UUID uuid, String message) {
        ServerPlayer online = server.getPlayerList().getPlayer(uuid);
        if (online != null) {
            server.execute(() -> online.connection.disconnect(Component.literal(message)));
        }
    }

    private int kickPlayers(CommandSourceStack source, Collection<GameProfile> targets, String reason) {
        return applyToTargets(source, targets, PunishKind.KICK, reason, 0);
    }

    private int banPlayers(CommandSourceStack source, Collection<GameProfile> targets, String reason, int durationSeconds) {
        return applyToTargets(source, targets, PunishKind.BAN, reason, durationSeconds);
    }

    private int tempbanPlayers(CommandSourceStack source, Collection<GameProfile> targets, String duration, String reason) {
        int seconds = Durations.parseSeconds(duration);
        if (seconds < 0) {
            source.sendFailure(Component.literal("Invalid duration '" + duration + "'. Use e.g. 30s, 5m, 1h, 1d."));
            return 0;
        }
        return applyToTargets(source, targets, PunishKind.BAN, reason, seconds);
    }

    private int mutePlayers(CommandSourceStack source, Collection<GameProfile> targets, String reason, int durationSeconds) {
        return applyToTargets(source, targets, PunishKind.MUTE, reason, durationSeconds);
    }

    private int tempmutePlayers(CommandSourceStack source, Collection<GameProfile> targets, String duration, String reason) {
        int seconds = Durations.parseSeconds(duration);
        if (seconds < 0) {
            source.sendFailure(Component.literal("Invalid duration '" + duration + "'. Use e.g. 30s, 5m, 1h, 1d."));
            return 0;
        }
        return applyToTargets(source, targets, PunishKind.MUTE, reason, seconds);
    }

    private int unbanPlayers(CommandSourceStack source, Collection<GameProfile> targets) {
        int count = 0;
        for (GameProfile target : targets) {
            if (core.punishment().unban(target.getId())) {
                access.log(source.getTextName() + " unbanned " + target.getName());
                count++;
            }
        }
        int total = count;
        source.sendSuccess(() -> Component.literal("Unbanned " + total + " player(s)."), true);
        return count;
    }

    private int unmutePlayers(CommandSourceStack source, Collection<GameProfile> targets) {
        int count = 0;
        for (GameProfile target : targets) {
            if (core.punishment().unmute(target.getId())) {
                access.log(source.getTextName() + " unmuted " + target.getName());
                count++;
            }
        }
        int total = count;
        source.sendSuccess(() -> Component.literal("Unmuted " + total + " player(s)."), true);
        return count;
    }

    private int escalatePunish(CommandSourceStack source, ServerPlayer target, String reason) {
        if (!core.punishment().config().enabled) {
            source.sendFailure(Component.literal("Punishments are disabled in punishment.json."));
            return 0;
        }
        PunishmentConfig.Tier tier = core.punishment().nextEscalationTier(target.getGameProfile().getId());
        if (tier == null) {
            source.sendFailure(Component.literal("No escalation ladder configured in punishment.json."));
            return 0;
        }
        PunishKind kind = tier.action == PunishmentConfig.Action.MUTE ? PunishKind.MUTE : PunishKind.BAN;
        int durationSeconds = tier.action == PunishmentConfig.Action.BAN ? 0 : tier.durationSeconds;
        applyPunishment(source.getServer(), target.getGameProfile().getId(), target.getGameProfile().getName(), source.getTextName(), kind, reason, durationSeconds);
        String tierLabel = tier.action.name().toLowerCase(Locale.ROOT) + (durationSeconds > 0 ? " " + Durations.format(durationSeconds) : "");
        source.sendSuccess(() -> Component.literal("Escalation applied to " + target.getGameProfile().getName() + " (" + tierLabel + ")."), true);
        return 1;
    }

    private int sendHistory(CommandSourceStack source, Collection<GameProfile> targets) {
        if (targets.isEmpty()) {
            source.sendFailure(Component.literal("No matching player."));
            return 0;
        }
        int total = 0;
        for (GameProfile target : targets) {
            List<PunishmentService.HistoryEntry> history = core.punishment().history(target.getId());
            source.sendSuccess(() -> Component.literal(target.getName() + " history (" + history.size() + "):"), false);
            for (PunishmentService.HistoryEntry entry : history) {
                String line = "- " + entry.type + " by " + entry.staffName
                        + (entry.durationSeconds > 0 ? " (" + Durations.format(entry.durationSeconds) + ")" : "")
                        + (entry.reason != null && !entry.reason.isBlank() ? ": " + entry.reason : "");
                source.sendSuccess(() -> Component.literal(line), false);
            }
            total += history.size();
        }
        return Math.max(1, total);
    }

    private int submitReport(CommandSourceStack source, String targetName, String reason) {
        if (!core.report().config().enabled) {
            source.sendFailure(Component.literal("Reports are disabled in report.json."));
            return 0;
        }
        ServerPlayer reporter = source.getPlayer();
        if (reporter == null) {
            source.sendFailure(Component.literal("Only players can use /report."));
            return 0;
        }
        UUID reporterUuid = reporter.getGameProfile().getId();
        int cooldown = core.report().cooldownRemaining(reporterUuid);
        if (cooldown > 0) {
            String cooldownText = TextFormat.legacyColors(core.report().config().cooldownMessage) + " (" + Durations.format(cooldown) + ")";
            source.sendFailure(Component.literal(cooldownText));
            return 0;
        }
        ReportService.Report report = core.report().submit(reporterUuid, reporter.getGameProfile().getName(), targetName, reason);
        source.sendSuccess(() -> Component.literal(TextFormat.legacyColors(core.report().config().submittedMessage)), false);
        String line = "Report #" + report.id + ": " + reporter.getGameProfile().getName() + " reported " + targetName + ": " + reason;
        access.log(line);
        Component staffMessage = Component.literal(TextFormat.legacyColors("&7[Report] &f" + line));
        for (ServerPlayer online : source.getServer().getPlayerList().getPlayers()) {
            if (access.hasPermission(online, core.report().config().notifyPermission, 2)) {
                online.sendSystemMessage(staffMessage);
            }
        }
        core.report().notifyDiscord(line);
        return 1;
    }

    private int listReports(CommandSourceStack source) {
        List<ReportService.Report> open = core.report().openReports();
        if (open.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No open reports."), false);
            return 1;
        }
        source.sendSuccess(() -> Component.literal("Open reports (" + open.size() + "):"), false);
        for (ReportService.Report report : open) {
            String line = "- #" + report.id + " " + report.reporterName + " -> " + report.targetName + ": " + report.reason
                    + " (" + report.status + (report.claimedBy.isBlank() ? "" : " by " + report.claimedBy) + ")";
            source.sendSuccess(() -> Component.literal(line), false);
        }
        return open.size();
    }

    private int claimReport(CommandSourceStack source, int id) {
        if (!core.report().claim(id, source.getTextName())) {
            source.sendFailure(Component.literal("Report #" + id + " not found or already resolved."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Claimed report #" + id + "."), true);
        return 1;
    }

    private int resolveReport(CommandSourceStack source, int id) {
        if (!core.report().resolve(id)) {
            source.sendFailure(Component.literal("Report #" + id + " not found."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Resolved report #" + id + "."), true);
        return 1;
    }

    private int toggleSelfAfk(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("Only players can use /fiw afk."));
            return 0;
        }
        if (!core.afk().config().enabled) {
            source.sendFailure(Component.literal("AFK detection is disabled in afk.json."));
            return 0;
        }
        boolean nowAfk = core.afk().toggleManual(player.getGameProfile().getId());
        source.sendSuccess(() -> Component.literal(nowAfk ? "You are now marked AFK." : "You are no longer AFK."), false);
        return 1;
    }

    private int listAfk(CommandSourceStack source) {
        Set<UUID> afk = core.afk().afkPlayers();
        if (afk.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No AFK players."), false);
            return 1;
        }
        source.sendSuccess(() -> Component.literal("AFK players (" + afk.size() + "):"), false);
        for (UUID uuid : afk) {
            ServerPlayer player = source.getServer().getPlayerList().getPlayer(uuid);
            String name = player != null ? player.getGameProfile().getName() : uuid.toString();
            long idleSeconds = core.afk().idleSeconds(uuid);
            String idleText = idleSeconds < 0 ? "?" : Durations.format((int) idleSeconds);
            source.sendSuccess(() -> Component.literal("- " + name + " (idle " + idleText + ")"), false);
        }
        return afk.size();
    }

    private int sendWatchdogStatus(CommandSourceStack source) {
        var config = core.watchdog().config();
        source.sendSuccess(() -> Component.literal("Watchdog: " + (config.enabled ? "on" : "off")), false);
        source.sendSuccess(() -> Component.literal("Heartbeat age: " + core.watchdog().heartbeatAgeSeconds()
                + "s (hang alert at " + config.heartbeatTimeoutSeconds + "s)"), false);
        source.sendSuccess(() -> Component.literal("Crash-on-boot alert: " + (config.crashAlertOnBoot ? "on" : "off")), false);
        return 1;
    }

    private int sendDupeStatus(CommandSourceStack source) {
        DupeConfig config = core.dupe().config();
        source.sendSuccess(() -> Component.literal("Dupe detection: " + (config.enabled ? "on" : "off")), false);
        source.sendSuccess(() -> Component.literal("Rate detector: " + (config.rateDetector.enabled ? "on" : "off")
                + " (threshold " + config.rateDetector.threshold + " / " + config.rateDetector.windowSeconds
                + "s, response " + config.rateDetector.response.tier + ")"), false);
        source.sendSuccess(() -> Component.literal("Chunk scope: " + (config.rateDetector.chunkScope.enabled ? "on" : "off")), false);
        source.sendSuccess(() -> Component.literal("Signature detector: " + (config.signatureDetector.enabled ? "on" : "off")
                + " (" + config.signatureDetector.watchList.size() + " watched item(s), response "
                + config.signatureDetector.response.tier + ")"), false);
        return 1;
    }

    private int sendDupeAlerts(CommandSourceStack source) {
        List<DupeService.DupeAlertLog.Entry> recent = core.dupe().recentAlerts();
        if (recent.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No dupe alerts logged."), false);
            return 1;
        }
        int start = Math.max(0, recent.size() - 20);
        int shown = recent.size() - start;
        source.sendSuccess(() -> Component.literal("Recent dupe alerts (" + shown + " of " + recent.size() + "):"), false);
        for (DupeService.DupeAlertLog.Entry entry : recent.subList(start, recent.size())) {
            String line = "- [" + entry.detector + "] " + entry.detail;
            source.sendSuccess(() -> Component.literal(line), false);
        }
        return recent.size();
    }

    private int clearDupeHistory(CommandSourceStack source, Collection<GameProfile> targets) {
        int count = 0;
        for (GameProfile target : targets) {
            core.dupe().clearHistory(target.getId().toString());
            count++;
        }
        int total = count;
        source.sendSuccess(() -> Component.literal("Cleared dupe rate history for " + total + " player(s)."), true);
        return count;
    }

    private void tickDupeScan(MinecraftServer server) {
        int intervalSeconds = Math.max(1, core.dupe().config().scanIntervalSeconds);
        if (dupeScanCountdownSeconds > 0) {
            dupeScanCountdownSeconds--;
            return;
        }
        dupeScanCountdownSeconds = intervalSeconds;
        tickDupeDetection(server);
    }

    private void tickDupeDetection(MinecraftServer server) {
        DupeConfig config = core.dupe().config();
        if (!config.enabled || System.currentTimeMillis() < dupeGraceUntilMillis) {
            return;
        }
        if (config.rateDetector.enabled) {
            scanRateDetector(server, config);
        }
        if (config.signatureDetector.enabled && !config.signatureDetector.watchList.isEmpty()) {
            scanSignatureDetector(server, config);
        }
    }

    private void scanRateDetector(MinecraftServer server, DupeConfig config) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            double value = weighFromWatchList(player.getInventory().items, config.rateDetector.watchList);
            String key = player.getGameProfile().getId().toString();
            double increase = core.dupe().rateIncrease(key, value, config.rateDetector.windowSeconds);
            if (increase < config.rateDetector.threshold) {
                continue;
            }
            if (config.rateDetector.exemptWhileContainerOpen && player.containerMenu != player.inventoryMenu) {
                continue;
            }
            if (!core.dupe().shouldAlert(key, config.rateDetector.windowSeconds)) {
                continue;
            }
            String detail = player.getGameProfile().getName() + " gained " + String.format(Locale.ROOT, "%.1f", increase)
                    + " weighted watch-list value in " + config.rateDetector.windowSeconds
                    + "s (threshold " + config.rateDetector.threshold + ").";
            applyDupeResponse(server, player.getGameProfile().getId(), player.getGameProfile().getName(), "rate", detail, config.rateDetector.response);
        }
        if (config.rateDetector.chunkScope.enabled) {
            scanRateDetectorChunks(server, config);
        }
    }

    private double weighFromWatchList(Iterable<ItemStack> items, List<DupeConfig.WatchEntry> watchList) {
        double total = 0;
        for (ItemStack stack : items) {
            if (stack.isEmpty()) {
                continue;
            }
            String id = itemId(stack);
            for (DupeConfig.WatchEntry entry : watchList) {
                if (id.equals(entry.itemId)) {
                    total += entry.weight * stack.getCount();
                    break;
                }
            }
        }
        return total;
    }

    private void scanRateDetectorChunks(MinecraftServer server, DupeConfig config) {
        Map<String, Double> chunkValues = new HashMap<>();
        for (ServerLevel level : server.getAllLevels()) {
            String dimension = level.dimension().location().toString();
            for (Entity entity : level.getAllEntities()) {
                if (!(entity instanceof ItemEntity itemEntity)) {
                    continue;
                }
                ItemStack stack = itemEntity.getItem();
                if (stack.isEmpty()) {
                    continue;
                }
                String id = itemId(stack);
                for (DupeConfig.WatchEntry entry : config.rateDetector.watchList) {
                    if (!id.equals(entry.itemId)) {
                        continue;
                    }
                    ChunkPos pos = itemEntity.chunkPosition();
                    String key = dimension + ":" + chunkX(pos) + ":" + chunkZ(pos);
                    chunkValues.merge(key, entry.weight * stack.getCount(), Double::sum);
                    break;
                }
            }
        }
        double chunkThreshold = config.rateDetector.threshold * config.rateDetector.chunkScope.thresholdMultiplier;
        for (Map.Entry<String, Double> entry : chunkValues.entrySet()) {
            double increase = core.dupe().rateIncrease("chunk:" + entry.getKey(), entry.getValue(), config.rateDetector.windowSeconds);
            if (increase < chunkThreshold) {
                continue;
            }
            if (!core.dupe().shouldAlert("chunk:" + entry.getKey(), config.rateDetector.windowSeconds)) {
                continue;
            }
            String detail = "Chunk " + entry.getKey() + " gained " + String.format(Locale.ROOT, "%.1f", increase)
                    + " weighted dropped-item value in " + config.rateDetector.windowSeconds
                    + "s (threshold " + chunkThreshold + ").";
            applyDupeResponse(server, null, entry.getKey(), "rate-chunk", detail, config.rateDetector.response);
        }
    }

    private void scanSignatureDetector(MinecraftServer server, DupeConfig config) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            for (ItemStack stack : player.getInventory().items) {
                if (stack.isEmpty()) {
                    continue;
                }
                String id = itemId(stack);
                if (!config.signatureDetector.watchList.contains(id)) {
                    continue;
                }
                String signature = id + "#" + stack.getComponentsPatch();
                String conflictHolder = core.dupe().checkAndUpdateSignature(
                        signature, player.getGameProfile().getId().toString(), config.scanIntervalSeconds);
                if (conflictHolder != null) {
                    String detail = "Item " + id + " appears to be held by both " + conflictHolder + " and "
                            + player.getGameProfile().getName() + " (" + player.getGameProfile().getId() + ") within the same scan window.";
                    applyDupeResponse(server, player.getGameProfile().getId(), player.getGameProfile().getName(),
                            "signature", detail, config.signatureDetector.response);
                }
            }
        }
    }

    private void applyDupeResponse(MinecraftServer server, UUID target, String targetLabel, String detector, String detail, DupeConfig.Response response) {
        core.dupe().recordAlert(detector, detail);
        access.log("[dupe] " + detail);
        DupeConfig.Tier tier = response.tier;
        boolean punitive = tier == DupeConfig.Tier.FREEZE || tier == DupeConfig.Tier.KICK
                || tier == DupeConfig.Tier.TEMPBAN || tier == DupeConfig.Tier.BAN;
        if (punitive && target == null) {
            tier = DupeConfig.Tier.ALERT;
        }
        if (tier == DupeConfig.Tier.DISCORD || punitive) {
            core.dupe().notifyDiscord(detail);
        }
        if (tier != DupeConfig.Tier.LOG) {
            Component component = Component.literal(TextFormat.legacyColors("&7[Dupe] &f" + detail));
            for (ServerPlayer online : server.getPlayerList().getPlayers()) {
                if (access.hasPermission(online, core.dupe().config().notifyPermission, 3)) {
                    online.sendSystemMessage(component);
                }
            }
        }
        String staffName = "Dupe-" + detector;
        String reason = "Automatic dupe detection: " + detail;
        switch (tier) {
            case FREEZE -> {
                ServerPlayer targetPlayer = server.getPlayerList().getPlayer(target);
                if (targetPlayer != null && core.freeze().config().enabled && !core.freeze().isFrozen(target)) {
                    String evidence = buildFreezeEvidence(targetPlayer);
                    core.freeze().freeze(target, targetLabel, staffName, reason, 0, evidence);
                    anchorFrozenPlayer(targetPlayer);
                }
            }
            case KICK -> applyPunishment(server, target, targetLabel, staffName, PunishKind.KICK, reason, 0);
            case TEMPBAN -> applyPunishment(server, target, targetLabel, staffName, PunishKind.BAN, reason, response.durationSeconds);
            case BAN -> applyPunishment(server, target, targetLabel, staffName, PunishKind.BAN, reason, 0);
            default -> {
            }
        }
    }

    private int toggleVanish(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("Only players can use /fiw vanish."));
            return 0;
        }
        if (!core.vanish().config().enabled) {
            source.sendFailure(Component.literal("Vanish is disabled in vanish.json."));
            return 0;
        }
        boolean vanished = core.vanish().toggle(player.getGameProfile().getId(), player.getGameProfile().getName());
        if (vanished) {
            rememberVanishedName(player);
            source.sendSuccess(() -> Component.literal("You are now vanished."), false);
        } else {
            knownVanishedNames.remove(player.getGameProfile().getId());
            recentlyVanishedNames.remove(player.getGameProfile().getName().toLowerCase(Locale.ROOT));
            source.sendSuccess(() -> Component.literal("You are no longer vanished."), false);
        }
        syncVanishForAll(source.getServer());
        return 1;
    }

    private int toggleVanish(CommandSourceStack source, ServerPlayer target) {
        if (!core.vanish().config().enabled) {
            source.sendFailure(Component.literal("Vanish is disabled in vanish.json."));
            return 0;
        }
        boolean vanished = core.vanish().toggle(target.getGameProfile().getId(), target.getGameProfile().getName());
        if (vanished) {
            rememberVanishedName(target);
            target.sendSystemMessage(Component.literal("You were vanished by " + source.getTextName() + "."));
        } else {
            knownVanishedNames.remove(target.getGameProfile().getId());
            recentlyVanishedNames.remove(target.getGameProfile().getName().toLowerCase(Locale.ROOT));
            target.sendSystemMessage(Component.literal("You were unvanished by " + source.getTextName() + "."));
        }
        source.sendSuccess(() -> Component.literal(target.getGameProfile().getName() + " is now " + (vanished ? "vanished." : "visible.")), true);
        syncVanishForAll(source.getServer());
        return 1;
    }

    private int listVanished(CommandSourceStack source) {
        Set<UUID> vanishedPlayers = core.vanish().vanishedPlayers();
        if (vanishedPlayers.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No vanished players."), false);
            return 1;
        }

        source.sendSuccess(() -> Component.literal("Vanished players (" + vanishedPlayers.size() + "):"), false);
        for (UUID uuid : vanishedPlayers) {
            String name = vanishedDisplayName(source.getServer(), uuid);
            source.sendSuccess(() -> Component.literal("- " + name + " (" + uuid + ")"), false);
        }
        return vanishedPlayers.size();
    }

    private String vanishedDisplayName(MinecraftServer server, UUID uuid) {
        ServerPlayer online = server.getPlayerList().getPlayer(uuid);
        if (online != null) {
            return online.getGameProfile().getName();
        }
        String known = core.vanish().knownName(uuid);
        return known == null ? "unknown" : known;
    }

    private void syncVanishForAll(MinecraftServer server) {
        for (ServerPlayer target : server.getPlayerList().getPlayers()) {
            applyLocatorBarState(target);
        }
        for (ServerPlayer observer : server.getPlayerList().getPlayers()) {
            syncVanishFor(observer, server);
        }
    }

    // The locator bar runs on the waypoint system, not entity tracking; vanished players
    // stop transmitting (range 0) so they drop off everyone's bar, admins included.
    private void applyLocatorBarState(ServerPlayer target) {
        // The locator bar and waypoint transmit attribute were introduced after 1.21.1.
    }

    private void syncVanishFor(ServerPlayer observer, MinecraftServer server) {
        for (ServerPlayer target : server.getPlayerList().getPlayers()) {
            if (target == observer) {
                continue;
            }
            boolean targetVanished = core.vanish().isVanished(target.getGameProfile().getId());
            boolean canSee = access.hasPermission(observer, core.vanish().config().seePermission, core.vanish().config().opSeeFallback ? 3 : -1);
            String pairKey = vanishPairKey(observer, target);
            if (targetVanished && !canSee) {
                rememberVanishedName(target);
                if (core.vanish().config().hideFromTab) {
                    observer.connection.send(new ClientboundPlayerInfoRemovePacket(List.of(target.getGameProfile().getId())));
                }
                if (core.vanish().config().hideEntity) {
                    observer.connection.send(new ClientboundRemoveEntitiesPacket(target.getId()));
                }
                hiddenVanishPairs.add(pairKey);
            } else if (hiddenVanishPairs.remove(pairKey)) {
                observer.connection.send(ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(List.of(target)));
                if (observer.level().dimension().equals(target.level().dimension())) {
                    resendPlayerEntity(observer, target);
                }
            }
        }
    }

    private void resendPlayerEntity(ServerPlayer observer, ServerPlayer target) {
        observer.connection.send(new ClientboundAddEntityPacket(
                target.getId(),
                target.getGameProfile().getId(),
                target.getX(),
                target.getY(),
                target.getZ(),
                target.getXRot(),
                target.getYRot(),
                EntityType.PLAYER,
                0,
                target.getDeltaMovement(),
                target.getYHeadRot()
        ));
        List<net.minecraft.network.syncher.SynchedEntityData.DataValue<?>> entityData = target.getEntityData().getNonDefaultValues();
        if (entityData != null && !entityData.isEmpty()) {
            observer.connection.send(new ClientboundSetEntityDataPacket(target.getId(), entityData));
        }
    }

    private void rememberVanishedName(ServerPlayer player) {
        if (core.vanish().isVanished(player.getGameProfile().getId())) {
            core.vanish().rememberName(player.getGameProfile().getId(), player.getGameProfile().getName());
            knownVanishedNames.put(player.getGameProfile().getId(), player.getGameProfile().getName());
        }
    }

    private String vanishPairKey(ServerPlayer observer, ServerPlayer target) {
        return observer.getGameProfile().getId() + ":" + target.getGameProfile().getId();
    }

    public boolean isVanished(ServerPlayer player) {
        return core.vanish().config().enabled && core.vanish().isVanished(player.getGameProfile().getId());
    }

    public Component tabListDisplayName(ServerPlayer player, Component original) {
        Component base = original == null ? player.getName() : original;
        if (isVanished(player)) {
            base = Component.literal(core.vanish().config().vanishedPrefix).withStyle(ChatFormatting.GRAY).append(base);
        }
        if (core.afk().isAfk(player.getGameProfile().getId())) {
            base = Component.literal(TextFormat.legacyColors(core.afk().config().tag + " ")).append(base);
        }
        return base;
    }

    public Optional<ServerStatus.Players> serverStatusPlayers(MinecraftServer server, ServerStatus.Players original) {
        if (!core.vanish().config().enabled || !core.vanish().config().hideFromServerListCount || original == null) {
            return Optional.ofNullable(original);
        }

        Set<UUID> vanished = core.vanish().vanishedPlayers();
        if (vanished.isEmpty()) {
            return Optional.of(original);
        }

        List<GameProfile> sample = new ArrayList<>();
        for (GameProfile entry : original.sample()) {
            if (!vanished.contains(entry.getId())) {
                sample.add(entry);
            }
        }

        int visibleOnline = Math.max(0, original.online() - onlineVanishedCount(server));
        return Optional.of(new ServerStatus.Players(original.max(), visibleOnline, sample));
    }

    private int onlineVanishedCount(MinecraftServer server) {
        int count = 0;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (core.vanish().isVanished(player.getGameProfile().getId())) {
                count++;
            }
        }
        return count;
    }

    private boolean containsIgnoreCase(List<String> values, String value) {
        for (String candidate : values) {
            if (candidate.equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }

    private static final class SweepResult {
        int items;
        int mobs;
    }

    private static final class ChunkFilter {
        private final net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension;
        private final ChunkPos chunk;

        private ChunkFilter(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension, ChunkPos chunk) {
            this.dimension = dimension;
            this.chunk = chunk;
        }

        boolean matchesDimension(ServerLevel level) {
            return level.dimension().equals(dimension);
        }

        boolean matchesChunk(ChunkPos other) {
            return chunk == null || (chunkX(chunk) == chunkX(other) && chunkZ(chunk) == chunkZ(other));
        }
    }

    private static int chunkX(ChunkPos pos) {
        return pos.getMinBlockX() >> 4;
    }

    private static int chunkZ(ChunkPos pos) {
        return pos.getMinBlockZ() >> 4;
    }

    private static final class ChunkReport {
        final String dimension;
        final int chunkX;
        final int chunkZ;
        int entities;
        int blockEntities;
        String dominantType = "none";
        final Map<String, Integer> typeCounts = new HashMap<>();

        ChunkReport(String dimension, int chunkX, int chunkZ) {
            this.dimension = dimension;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
        }

        void addType(String type) {
            typeCounts.merge(type, 1, Integer::sum);
        }

        void finish() {
            dominantType = typeCounts.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse("none");
        }

        int weight() {
            return entities + blockEntities;
        }
    }

    public interface PlatformAccess {
        boolean hasPermission(CommandSourceStack source, String permission, int fallbackOpLevel);

        boolean hasPermission(ServerPlayer player, String permission, int fallbackOpLevel);

        void log(String message);
    }
}
