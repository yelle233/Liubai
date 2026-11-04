package com.yelle233.liubai.mixin;

import com.yelle233.liubai.client.ClientHooks;
import com.yelle233.liubai.client.HookStatus;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockEntityRenderDispatcher.class)
public abstract class BlockEntityRenderDispatcherMixin {
    @Shadow
    public abstract <E extends BlockEntity, S extends BlockEntityRenderState> BlockEntityRenderer<E, S> getRenderer(E blockEntity);

    // The four-argument NeoForge overload has already checked validity, frustum and render distance here.
    @Inject(method = "tryExtractRenderState(Lnet/minecraft/world/level/block/entity/BlockEntity;FLnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;Lnet/minecraft/client/renderer/culling/Frustum;)Lnet/minecraft/client/renderer/blockentity/state/BlockEntityRenderState;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/blockentity/BlockEntityRenderer;createRenderState()Lnet/minecraft/client/renderer/blockentity/state/BlockEntityRenderState;"),
            cancellable = true, require = 1)
    private <E extends BlockEntity> void liubai$cullAfterVanillaChecks(E blockEntity, float partialTick,
            ModelFeatureRenderer.CrumblingOverlay breakProgress, Frustum frustum,
            CallbackInfoReturnable<BlockEntityRenderState> callback) {
        HookStatus.blockEntityObserved();
        try {
            // Avoid even asking a third-party renderer for bounds when Liubai's built-in backend is inactive.
            if (!ClientHooks.shouldInspectBlockEntities()) return;
            BlockEntityRenderer<E, ?> renderer = this.getRenderer(blockEntity);
            if (renderer != null && ClientHooks.shouldSkipBlockEntity(blockEntity, renderer.getRenderBoundingBox(blockEntity))) {
                callback.setReturnValue(null);
            }
        } catch (RuntimeException ignored) {
            // A third-party renderer with a broken bounds implementation stays visible.
        }
    }
}
