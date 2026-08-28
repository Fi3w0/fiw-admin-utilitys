package com.fiw.sharedmc.mixin;

import com.fiw.sharedmc.FiwAdminToolsHooks;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {
    @Inject(method = "buildPlayerStatus", at = @At("RETURN"), cancellable = true)
    private void fiw$hideVanishedFromServerStatus(CallbackInfoReturnable<ServerStatus.Players> callback) {
        callback.setReturnValue(FiwAdminToolsHooks.serverStatusPlayers((MinecraftServer) (Object) this, callback.getReturnValue()).orElse(null));
    }
}
