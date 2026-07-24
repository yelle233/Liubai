package com.yelle233.liubai.client;

import net.minecraft.client.particle.Particle;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Tiny static bridge used by optional Mixins. */
public final class ClientHooks {
    private ClientHooks() {}

    public static boolean shouldSkipEntity(Entity entity) {
        return LiubaiClientSystem.INSTANCE.policies().decide(entity).skip();
    }

    public static boolean shouldSkipBlockEntity(BlockEntity blockEntity) {
        return LiubaiClientSystem.INSTANCE.policies().decide(blockEntity).skip();
    }

    public static boolean shouldSkipShadow(Entity entity) {
        return LiubaiClientSystem.INSTANCE.policies().skipShadow(entity);
    }

    public static boolean shouldSkipParticle(Particle particle) {
        return LiubaiClientSystem.INSTANCE.policies().skipParticle(particle.getPos());
    }
}
