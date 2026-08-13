package com.yelle233.liubai.mixin;

import com.yelle233.liubai.client.ClientHooks;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Fabric equivalent of NeoForge's RenderNameTagEvent. */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity> {
    @Inject(method = "shouldShowName", at = @At("RETURN"), cancellable = true)
    private void liubai$reduceNameTags(T entity, CallbackInfoReturnable<Boolean> callback) {
        if (callback.getReturnValueZ() && ClientHooks.shouldSkipNameTag(entity)) callback.setReturnValue(false);
    }
}
