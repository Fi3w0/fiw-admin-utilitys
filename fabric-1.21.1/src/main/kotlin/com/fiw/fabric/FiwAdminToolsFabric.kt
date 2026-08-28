package com.fiw.fabric

import com.fiw.common.FiwAdminToolsCore
import com.fiw.common.FiwPlatform
import com.fiw.common.LuckPermsBridge
import com.fiw.sharedmc.AdminRuntime
import com.fiw.sharedmc.FiwAdminToolsHooks
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.event.player.AttackEntityCallback
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents
import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.fabricmc.fabric.api.event.player.UseItemCallback
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionResultHolder
import org.slf4j.LoggerFactory
import java.nio.file.Path

object FiwAdminToolsFabric : ModInitializer {
	private val logger = LoggerFactory.getLogger(FiwAdminToolsCore.FABRIC_MOD_ID)
	private lateinit var runtime: AdminRuntime

	override fun onInitialize() {
		val core = FiwAdminToolsCore.bootstrap(FabricPlatform)
		runtime = AdminRuntime(core, FabricAccess)
		FiwAdminToolsHooks.setRuntime(runtime)

		CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
			runtime.registerCommands(dispatcher)
		}
		ServerLifecycleEvents.SERVER_STARTED.register { server ->
			runtime.onServerStarted(server)
		}
		ServerTickEvents.END_SERVER_TICK.register { server ->
			runtime.onServerTick(server)
		}
		ServerPlayConnectionEvents.JOIN.register { handler, _, server ->
			runtime.onPlayerJoin(server, handler.player)
		}
		ServerPlayConnectionEvents.DISCONNECT.register { handler, server ->
			runtime.onPlayerLeave(server, handler.player)
		}
		EntityTrackingEvents.START_TRACKING.register { _, player ->
			runtime.onPlayerTrackingChanged(player.level().server, player)
		}
		ServerMessageEvents.ALLOW_GAME_MESSAGE.register { server, message, overlay ->
			!runtime.shouldSuppressGameMessage(server, message, overlay)
		}
		PlayerBlockBreakEvents.BEFORE.register { _, player, _, _, _ ->
			!(player is ServerPlayer && (runtime.shouldBlockInteraction(player)
					|| runtime.shouldBlockItemUse(player, player.mainHandItem)))
		}
		UseBlockCallback.EVENT.register { player, _, hand, _ ->
			if (player is ServerPlayer && (runtime.shouldBlockInteraction(player)
							|| runtime.shouldBlockItemUse(player, player.getItemInHand(hand)))) {
				InteractionResult.FAIL
			} else {
				InteractionResult.PASS
			}
		}
		UseItemCallback.EVENT.register { player, _, hand ->
			val stack = player.getItemInHand(hand)
			if (player is ServerPlayer && (runtime.shouldBlockInteraction(player)
							|| runtime.shouldBlockItemUse(player, stack))) {
				InteractionResultHolder.fail(stack)
			} else {
				InteractionResultHolder.pass(stack)
			}
		}
		AttackEntityCallback.EVENT.register { player, _, _, _, _ ->
			if (player is ServerPlayer && (runtime.shouldBlockInteraction(player)
							|| runtime.shouldBlockItemUse(player, player.mainHandItem))) {
				InteractionResult.FAIL
			} else {
				InteractionResult.PASS
			}
		}
	}

	private object FabricAccess : AdminRuntime.PlatformAccess {
		// Resolve the optional permissions API reflectively because its Minecraft
		// linkage varies by target version. LuckPerms and vanilla ops remain fallbacks.
		private var permissionsApiUsable = true

		override fun hasPermission(source: CommandSourceStack, permission: String, fallbackOpLevel: Int): Boolean {
			source.player?.let { player ->
				LuckPermsBridge.check(player.gameProfile.id, permission)?.let { return it }
			}
			if (permissionsApiUsable) fabricPermission(source, permission, fallbackOpLevel)?.let { return it }
			return vanillaOpCheck(source, fallbackOpLevel)
		}

		override fun hasPermission(player: ServerPlayer, permission: String, fallbackOpLevel: Int): Boolean {
			LuckPermsBridge.check(player.gameProfile.id, permission)?.let { return it }
			if (permissionsApiUsable) fabricPermission(player, permission, fallbackOpLevel)?.let { return it }
			return vanillaOpCheck(player.createCommandSourceStack(), fallbackOpLevel)
		}

		private fun fabricPermission(subject: Any, permission: String, fallbackOpLevel: Int): Boolean? {
			try {
				val permissions = Class.forName("me.lucko.fabric.api.permissions.v0.Permissions")
				val defaultValue: Any = if (fallbackOpLevel >= 0) fallbackOpLevel else false
				val defaultType = if (fallbackOpLevel >= 0) Int::class.javaPrimitiveType else Boolean::class.javaPrimitiveType
				val method = permissions.methods.firstOrNull { candidate ->
					candidate.name == "check"
							&& candidate.parameterCount == 3
							&& candidate.parameterTypes[0].isAssignableFrom(subject.javaClass)
							&& candidate.parameterTypes[1] == String::class.java
							&& candidate.parameterTypes[2] == defaultType
				} ?: return null
				return method.invoke(null, subject, permission, defaultValue) as Boolean
			} catch (error: ReflectiveOperationException) {
				permissionsApiUsable = false
				logger.info("fabric-permissions-api not installed; using LuckPerms/op fallback")
			} catch (error: LinkageError) {
				permissionsApiUsable = false
				logger.warn("fabric-permissions-api incompatible ({}), using LuckPerms/op fallback", error.toString())
			}
			return null
		}

		private fun vanillaOpCheck(source: CommandSourceStack, fallbackOpLevel: Int): Boolean {
			if (fallbackOpLevel < 0) return false
			if (fallbackOpLevel == 0) return true
			return source.hasPermission(fallbackOpLevel)
		}

		override fun log(message: String) {
			logger.info(message)
		}
	}

	private object FabricPlatform : FiwPlatform {
		override fun loaderName(): String = "Fabric"

		override fun configDirectory(): Path = FabricLoader.getInstance().configDir

		override fun info(message: String) {
			logger.info(message)
		}

		override fun warn(message: String) {
			logger.warn(message)
		}
	}
}
