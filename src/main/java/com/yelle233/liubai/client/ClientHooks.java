package com.yelle233.liubai.client;

import com.yelle233.liubai.compat.iris.IrisCompatibility;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Tiny static bridge used by optional Mixins. */
public final class ClientHooks {
    private ClientHooks() {}

    public static boolean shouldSkipEntity(Entity entity, double cameraX, double cameraY, double cameraZ) {
        if (IrisCompatibility.shouldBypassPerObjectPolicies()) return false;
        // RenderFrameEvent.Pre may still expose the previous interpolated camera while the player is moving.
        // Compare against GameRenderer's camera at the actual shouldRender call instead of that frame snapshot.
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        if (!camera.isInitialized() || camera.getPosition().distanceToSqr(cameraX, cameraY, cameraZ) > 0.01) return false;
        LiubaiClientSystem.INSTANCE.prepareMainRender(camera);
        return LiubaiClientSystem.INSTANCE.policies().decide(entity).skip();
    }

    public static boolean shouldSkipBlockEntity(BlockEntity blockEntity, AABB rendererBounds) {
        if (IrisCompatibility.shouldBypassPerObjectPolicies()) return false;
        return LiubaiClientSystem.INSTANCE.policies().decide(blockEntity, rendererBounds).skip();
    }

    public static boolean shouldBypassBlockEntity(BlockEntity blockEntity) {
        return LiubaiClientSystem.INSTANCE.policies().bypassDynamicBlockEntity(blockEntity);
    }

    public static boolean shouldInspectBlockEntities() {
        if (IrisCompatibility.shouldBypassPerObjectPolicies()) return false;
        prepareMainRender();
        return LiubaiClientSystem.INSTANCE.policies().inspectBlockEntities();
    }

    public static boolean shouldSkipShadow(Entity entity) {
        if (IrisCompatibility.shouldBypassPerObjectPolicies()) return false;
        prepareMainRender();
        return LiubaiClientSystem.INSTANCE.policies().skipShadow(entity);
    }

    public static boolean shouldSkipParticle(Particle particle) {
        return LiubaiClientSystem.INSTANCE.policies().skipParticle(particle.getPos());
    }

    public static boolean shouldSkipParticle(ParticleOptions options, double x, double y, double z) {
        return LiubaiClientSystem.INSTANCE.policies().skipParticle(options, new Vec3(x, y, z));
    }

    private static void prepareMainRender() {
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        if (camera.isInitialized()) LiubaiClientSystem.INSTANCE.prepareMainRender(camera);
    }
}
