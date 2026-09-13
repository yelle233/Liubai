package com.yelle233.liubai.mixin;

import com.yelle233.liubai.client.HookStatus;
import com.yelle233.liubai.compat.flywheel.FlywheelHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Version-gated integration for Create Fabric 6.0.8 / Flywheel 1.0.5. */
@Pseudo
@Mixin(targets = "dev.engine_room.flywheel.impl.visual.BandedPrimeLimiter", remap = false)
public abstract class FlywheelBandedPrimeLimiterMixin {
    @Inject(method = "getUpdateDivisor(D)I", at = @At("RETURN"), cancellable = true, remap = false)
    private void liubai$adaptiveUpdateDivisor(double distanceSquared, CallbackInfoReturnable<Integer> callback) {
        HookStatus.flywheelObserved();
        callback.setReturnValue(FlywheelHooks.adjustUpdateDivisor(distanceSquared, callback.getReturnValue()));
    }
}
