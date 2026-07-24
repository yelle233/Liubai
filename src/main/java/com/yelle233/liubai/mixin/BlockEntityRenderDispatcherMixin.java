package com.yelle233.liubai.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.yelle233.liubai.client.ClientHooks;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntityRenderDispatcher.class)
public abstract class BlockEntityRenderDispatcherMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private <E extends BlockEntity> void liubai$earlyCull(E blockEntity, float partialTick, PoseStack poseStack,
                                                          MultiBufferSource bufferSource, CallbackInfo callback) {
        if (ClientHooks.shouldSkipBlockEntity(blockEntity)) callback.cancel();
    }
}
