package com.yelle233.liubai.mixin;

import com.yelle233.liubai.client.ClientHooks;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ParticleEngine.class)
public abstract class ParticleEngineMixin {
    @Inject(method = "add", at = @At("HEAD"), cancellable = true)
    private void liubai$applyParticleBudget(Particle particle, CallbackInfo callback) {
        if (ClientHooks.shouldSkipParticle(particle)) callback.cancel();
    }
}
