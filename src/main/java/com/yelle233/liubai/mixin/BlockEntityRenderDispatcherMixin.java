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
    private static <T extends BlockEntity, S extends net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState> void liubai$cullAfterVanillaChecks(BlockEntityRenderer<T, S> renderer,
                                                                              T blockEntity, float partialTick,
                                                                              PoseStack poseStack,
                                                                              MultiBufferSource bufferSource,
                                                                              CallbackInfo callback) {
        HookStatus.blockEntityObserved();
        try {
            // Avoid even asking a third-party renderer for bounds when Liubai's built-in backend is inactive.
            if (!ClientHooks.shouldInspectBlockEntities()) return;
            if (ClientHooks.shouldBypassBlockEntity(blockEntity)) return;
            if (ClientHooks.shouldSkipBlockEntity(blockEntity, renderer.getRenderBoundingBox(blockEntity))) callback.cancel();
        } catch (RuntimeException ignored) {
            // A third-party renderer with a broken bounds implementation stays visible.
        }
    }
}
