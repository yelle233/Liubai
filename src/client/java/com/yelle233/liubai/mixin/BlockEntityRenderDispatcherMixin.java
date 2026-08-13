package com.yelle233.liubai.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.yelle233.liubai.client.ClientHooks;
import com.yelle233.liubai.client.HookStatus;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntityRenderDispatcher.class)
public abstract class BlockEntityRenderDispatcherMixin {
    @Inject(method = "setupAndRender", at = @At("HEAD"), cancellable = true)
    private static <T extends BlockEntity> void liubai$cullAfterVanillaChecks(BlockEntityRenderer<T> renderer,
                                                                              T blockEntity, float partialTick,
                                                                              PoseStack poseStack,
                                                                              MultiBufferSource bufferSource,
                                                                              CallbackInfo callback) {
        HookStatus.blockEntityObserved();
        try {
            // Avoid even asking a third-party renderer for bounds when Liubai's built-in backend is inactive.
            if (!ClientHooks.shouldInspectBlockEntities()) return;
            if (ClientHooks.shouldBypassBlockEntity(blockEntity)) return;
            if (ClientHooks.shouldSkipBlockEntity(blockEntity, new net.minecraft.world.phys.AABB(blockEntity.getBlockPos()))) callback.cancel();
        } catch (RuntimeException ignored) {
            // A third-party renderer with a broken bounds implementation stays visible.
        }
    }
}
