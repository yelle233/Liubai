package com.yelle233.liubai.mixin;

import com.yelle233.liubai.client.ClientHooks;
import com.yelle233.liubai.client.HookStatus;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {
    @Inject(method = "shouldRender", at = @At("RETURN"), cancellable = true, require = 1)
    private <E extends Entity> void liubai$earlyCull(E entity, Frustum frustum, double camX, double camY, double camZ,
                                                     CallbackInfoReturnable<Boolean> callback) {
        HookStatus.entityObserved();
        if (callback.getReturnValueZ() && ClientHooks.shouldSkipEntity(entity, camX, camY, camZ)) callback.setReturnValue(false);
    }
}
