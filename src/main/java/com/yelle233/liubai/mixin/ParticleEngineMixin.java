package com.yelle233.liubai.mixin;

import com.yelle233.liubai.client.ClientHooks;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ParticleEngine.class)
public abstract class ParticleEngineMixin {
    @Unique
    private int liubai$approvedCreationDepth;

    @Inject(method = "createParticle", at = @At("HEAD"), cancellable = true)
    private void liubai$budgetBeforeProvider(ParticleOptions options, double x, double y, double z,
                                             double xSpeed, double ySpeed, double zSpeed,
                                             CallbackInfoReturnable<Particle> callback) {
        if (ClientHooks.shouldSkipParticle(options, x, y, z)) {
            callback.setReturnValue(null);
        } else {
            liubai$approvedCreationDepth++;
        }
    }

    @Inject(method = "createParticle", at = @At("RETURN"))
    private void liubai$finishCreation(ParticleOptions options, double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed,
                                       CallbackInfoReturnable<Particle> callback) {
        if (liubai$approvedCreationDepth > 0) liubai$approvedCreationDepth--;
    }

    @Inject(method = "add", at = @At("HEAD"), cancellable = true)
    private void liubai$budgetDirectAdds(Particle particle, CallbackInfo callback) {
        if (liubai$approvedCreationDepth == 0 && ClientHooks.shouldSkipParticle(particle)) callback.cancel();
    }
}
