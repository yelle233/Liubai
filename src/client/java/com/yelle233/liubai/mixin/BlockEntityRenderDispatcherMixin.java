package com.yelle233.liubai.mixin;

import com.yelle233.liubai.client.ClientHooks;
import com.yelle233.liubai.client.HookStatus;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockEntityRenderDispatcher.class)
public abstract class BlockEntityRenderDispatcherMixin {
    @Inject(method = "tryExtractRenderState",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/blockentity/BlockEntityRenderer;createRenderState()Lnet/minecraft/client/renderer/blockentity/state/BlockEntityRenderState;"),
            cancellable = true)
    private <E extends BlockEntity, S extends BlockEntityRenderState> void liubai$cullAfterVanillaChecks(
            E blockEntity, float partialTick, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay,
            CallbackInfoReturnable<S> callback) {
        HookStatus.blockEntityObserved();
        try {
            if (!ClientHooks.shouldInspectBlockEntities()) return;
            if (ClientHooks.shouldBypassBlockEntity(blockEntity)) return;
            if (ClientHooks.shouldSkipBlockEntity(blockEntity,
                    new net.minecraft.world.phys.AABB(blockEntity.getBlockPos()))) {
                callback.setReturnValue(null);
            }
        } catch (RuntimeException ignored) {
            // A third-party renderer with broken state stays visible.
        }
    }
}
