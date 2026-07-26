package com.yelle233.liubai.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.yelle233.liubai.client.ClientHooks;
import com.yelle233.liubai.client.HookStatus;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelReader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {
    @Inject(method = "shouldRender", at = @At("RETURN"), cancellable = true)
    private <E extends Entity> void liubai$earlyCull(E entity, Frustum frustum, double camX, double camY, double camZ,
                                                     CallbackInfoReturnable<Boolean> callback) {
        HookStatus.entityObserved();
        if (callback.getReturnValueZ() && ClientHooks.shouldSkipEntity(entity, camX, camY, camZ)) callback.setReturnValue(false);
    }

    @Inject(method = "renderShadow", at = @At("HEAD"), cancellable = true)
    private static void liubai$reduceShadow(PoseStack poseStack, MultiBufferSource buffer, Entity entity,
                                            float weight, float partialTick, LevelReader level, float size,
                                            CallbackInfo callback) {
        HookStatus.entityObserved();
        if (ClientHooks.shouldSkipShadow(entity)) callback.cancel();
    }
}
