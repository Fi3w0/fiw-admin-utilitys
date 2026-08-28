package com.fiw.forge;

import com.fiw.common.FiwAdminToolsCore;
import com.fiw.common.FiwPlatform;
import com.fiw.common.LuckPermsBridge;
import com.fiw.sharedmc.AdminRuntime;
import com.fiw.sharedmc.FiwAdminToolsHooks;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.server.permission.PermissionAPI;
import net.minecraftforge.server.permission.events.PermissionGatherEvent;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import net.minecraftforge.server.permission.nodes.PermissionTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

@Mod(FiwAdminToolsCore.NEOFORGE_MOD_ID)
public final class FiwAdminToolsForge {
    private static final Logger LOGGER = LoggerFactory.getLogger(FiwAdminToolsCore.NEOFORGE_MOD_ID);
    private static final Map<String, PermissionNode<Boolean>> PERMISSIONS = new LinkedHashMap<>();

    private final AdminRuntime runtime;

    public FiwAdminToolsForge() {
        registerNode("fiw.maintenance.manage", "Maintenance manage", "Allows managing maintenance mode.");
        registerNode("fiw.maintenance.bypass", "Maintenance bypass", "Allows joining while maintenance mode is enabled.");
        registerNode("fiw.sweep.manage", "Sweep manage", "Allows using sweep commands.");
        registerNode("fiw.sweep.notify", "Sweep notify", "Receives sweep announcements.");
        registerNode("fiw.alert.manage", "Alert manage", "Allows using alert commands.");
        registerNode("fiw.alert.notify", "Alert notify", "Receives TPS alerts.");
        registerNode("fiw.vanish.use", "Vanish use", "Allows toggling vanish.");
        registerNode("fiw.vanish.see", "Vanish see", "Allows seeing vanished players.");
        registerNode("fiw.inspect.use", "Inspect use", "Allows using /fiw whois and /fiw find.");
        registerNode("fiw.freeze.use", "Freeze use", "Allows freezing players.");
        registerNode("fiw.banitem.manage", "BanItem manage", "Allows banning/unbanning items.");
        registerNode("fiw.banitem.bypass", "BanItem bypass", "Allows using banned items.");
        registerNode("fiw.punish.kick", "Punish kick", "Allows kicking players.");
        registerNode("fiw.punish.ban", "Punish ban", "Allows banning/tempbanning players.");
        registerNode("fiw.punish.mute", "Punish mute", "Allows muting/tempmuting players.");
        registerNode("fiw.punish.manage", "Punish manage", "Allows unban/unmute/history/punish.");
        registerNode("fiw.punish.notify", "Punish notify", "Receives punishment broadcasts.");
        registerNode("fiw.report.use", "Report use", "Allows submitting /report.", true);
        registerNode("fiw.report.manage", "Report manage", "Allows managing reports.");
        registerNode("fiw.report.notify", "Report notify", "Receives report notifications.");
        registerNode("fiw.afk.use", "AFK use", "Allows self-marking AFK.", true);
        registerNode("fiw.afk.manage", "AFK manage", "Allows listing AFK players.");
        registerNode("fiw.afk.exempt", "AFK exempt", "Exempts from AFK auto-kick.");

        FiwAdminToolsCore core = FiwAdminToolsCore.bootstrap(new ForgePlatform());
        runtime = new AdminRuntime(core, new ForgeAccess());
        FiwAdminToolsHooks.setRuntime(runtime);

        MinecraftForge.EVENT_BUS.register(this);
    }

    private static void registerNode(String node, String name, String description) {
        registerNode(node, name, description, false);
    }

    private static void registerNode(String node, String name, String description, boolean defaultValue) {
        String withoutPrefix = node.startsWith("fiw.") ? node.substring("fiw.".length()) : node;
        PERMISSIONS.put(node, new PermissionNode<>(
                "fiw",
                withoutPrefix,
                PermissionTypes.BOOLEAN,
                (player, uuid, context) -> defaultValue
        ).setInformation(Component.literal(name), Component.literal(description)));
    }

    @SubscribeEvent
    public void registerCommands(RegisterCommandsEvent event) {
        runtime.registerCommands(event.getDispatcher());
    }

    @SubscribeEvent
    public void registerPermissionNodes(PermissionGatherEvent.Nodes event) {
        event.addNodes(new ArrayList<PermissionNode<?>>(PERMISSIONS.values()));
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        runtime.onServerStarted(event.getServer());
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            runtime.onServerTick(event.getServer());
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            runtime.onPlayerJoin(player.getServer(), player);
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            runtime.onPlayerLeave(player.getServer(), player);
        }
    }

    @SubscribeEvent
    public void onPlayerStartTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getTarget() instanceof ServerPlayer) {
            runtime.onPlayerTrackingChanged(player.getServer(), player);
        }
    }

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            runtime.recordActivity(player);
            if (runtime.shouldBlockInteraction(player) || runtime.shouldBlockItemUse(player, player.getMainHandItem())) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            runtime.recordActivity(player);
            if (runtime.shouldBlockInteraction(player) || runtime.shouldBlockItemUse(player, event.getItemStack())) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            runtime.recordActivity(player);
            if (runtime.shouldBlockInteraction(player) || runtime.shouldBlockItemUse(player, event.getItemStack())) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public void onAttackEntity(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            runtime.recordActivity(player);
            if (runtime.shouldBlockInteraction(player) || runtime.shouldBlockItemUse(player, player.getMainHandItem())) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public void onServerChat(ServerChatEvent event) {
        if (runtime.shouldBlockChat(event.getPlayer())) {
            event.setCanceled(true);
        }
    }

    private static final class ForgeAccess implements AdminRuntime.PlatformAccess {
        @Override
        public boolean hasPermission(CommandSourceStack source, String permission, int fallbackOpLevel) {
            ServerPlayer player = source.getPlayer();
            if (player == null) {
                return fallbackOpLevel >= 0;
            }
            return hasPermission(player, permission, fallbackOpLevel);
        }

        @Override
        public boolean hasPermission(ServerPlayer player, String permission, int fallbackOpLevel) {
            Boolean luckPerms = LuckPermsBridge.check(player.getGameProfile().getId(), permission);
            if (luckPerms != null) {
                return luckPerms;
            }
            PermissionNode<Boolean> node = PERMISSIONS.get(permission);
            if (node != null && PermissionAPI.getRegisteredNodes().contains(node) && PermissionAPI.getPermission(player, node)) {
                return true;
            }
            return fallbackOpLevel >= 3 && player.createCommandSourceStack().hasPermission(3);
        }

        @Override
        public void log(String message) {
            LOGGER.info(message);
        }
    }

    private static final class ForgePlatform implements FiwPlatform {
        @Override
        public String loaderName() {
            return "Forge";
        }

        @Override
        public Path configDirectory() {
            return FMLPaths.CONFIGDIR.get();
        }

        @Override
        public void info(String message) {
            LOGGER.info(message);
        }

        @Override
        public void warn(String message) {
            LOGGER.warn(message);
        }
    }
}
