package com.fiw.sharedmc.mixin;

import com.fiw.sharedmc.FiwAdminToolsHooks;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerList.class)
public abstract class PlayerListMixin {
    @Shadow
    @Final
    private MinecraftServer server;

    @Inject(method = "broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Z)V", at = @At("HEAD"), cancellable = true)
    private void fiw$suppressVanishedJoinLeave(Component message, boolean overlay, CallbackInfo callback) {
        if (FiwAdminToolsHooks.shouldSuppressGameMessage(server, message, overlay)) {
            callback.cancel();
        }
    }
}
