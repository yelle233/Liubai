package com.yelle233.liubai.mixin;

import com.yelle233.liubai.client.LiubaiClientSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    // In 26.1.2 entity/block-entity decisions run during extraction, BEFORE RenderFrameEvent.Pre.
    // Keep the frame open until RenderFrameEvent.Post so extraction and submission share its state.
    @Inject(method = "extract", at = @At("HEAD"), require = 1)
    private void liubai$beginExtraction(DeltaTracker deltaTracker, boolean advanceGameTime, CallbackInfo callback) {
        LiubaiClientSystem.INSTANCE.beginFrame();
    }
}
