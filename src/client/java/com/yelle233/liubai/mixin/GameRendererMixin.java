package com.yelle233.liubai.mixin;

import com.yelle233.liubai.client.LiubaiClientSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Fabric equivalent of NeoForge's RenderFrameEvent.Pre/Post, covering the complete rendered frame. */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void liubai$beginFrame(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo callback) {
        LiubaiClientSystem.INSTANCE.beginFrame();
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void liubai$endFrame(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo callback) {
        LiubaiClientSystem.INSTANCE.endFrame();
    }
}
