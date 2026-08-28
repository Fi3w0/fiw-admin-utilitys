package com.fiw.sharedmc.mixin;

import com.fiw.sharedmc.FiwAdminToolsHooks;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {
    @Inject(method = "getTabListDisplayName", at = @At("RETURN"), cancellable = true)
    private void fiw$markVanishedInTab(CallbackInfoReturnable<Component> callback) {
        callback.setReturnValue(FiwAdminToolsHooks.tabListDisplayName((ServerPlayer) (Object) this, callback.getReturnValue()));
    }
}
