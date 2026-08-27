package com.yelle233.liubai.mixin;

import com.yelle233.liubai.client.ClientHooks;
import com.yelle233.liubai.client.HookStatus;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity, S extends EntityRenderState> {
    @Shadow
    protected abstract AABB getBoundingBoxForCulling(T entity);

    /** Runs after vanilla and third-party frustum checks, before render-state extraction. */
    @Inject(method = "shouldRender", at = @At("RETURN"), cancellable = true)
    private void liubai$earlyCull(T entity, Frustum frustum, double camX, double camY, double camZ,
                                  CallbackInfoReturnable<Boolean> callback) {
        HookStatus.entityObserved();
        if (!callback.getReturnValueZ()) return;
        try {
            if (ClientHooks.shouldSkipEntity(entity, getBoundingBoxForCulling(entity), camX, camY, camZ)) {
                callback.setReturnValue(false);
            }
        } catch (RuntimeException ignored) {
            // A renderer with a broken culling-bounds implementation stays visible.
        }
    }

    /** 1.21.11 extracts name tags and shadows into a render state before submission. */
    @Inject(method = "createRenderState(Lnet/minecraft/world/entity/Entity;F)Lnet/minecraft/client/renderer/entity/state/EntityRenderState;",
            at = @At("RETURN"))
    private void liubai$reduceSecondaryEffects(T entity, float partialTick,
                                                CallbackInfoReturnable<S> callback) {
        S state = callback.getReturnValue();
        if (state == null) return;
        if (state.nameTag != null && ClientHooks.shouldSkipNameTag(entity)) {
            state.nameTag = null;
        }
        if (!state.shadowPieces.isEmpty() && ClientHooks.shouldSkipShadow(entity)) {
            state.shadowPieces.clear();
            state.shadowRadius = 0.0F;
        }
    }
}
