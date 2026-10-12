package com.farlands.lifesteal.mixin;

import com.farlands.lifesteal.event.ModEvents;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {

    @Inject(method = "tickChildren", at = @At("TAIL"))
    private void onTickChildren(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
        ModEvents.onServerTick((MinecraftServer) (Object) this);
    }
}
