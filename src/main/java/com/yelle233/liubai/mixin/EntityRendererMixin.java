package com.yelle233.liubai.mixin;

import com.yelle233.liubai.client.ClientHooks;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {
    // In 26.1.2 this method only prepares shadow pieces; the old dispatcher renderShadow is gone.
    @Inject(method = "finalizeRenderState", at = @At("HEAD"), cancellable = true, require = 1)
    private void liubai$reduceShadow(Entity entity, EntityRenderState state, CallbackInfo callback) {
        if (ClientHooks.shouldSkipShadow(entity)) {
            state.shadowPieces.clear();
            state.shadowRadius = 0.0F;
            callback.cancel();
        }
    }
}
