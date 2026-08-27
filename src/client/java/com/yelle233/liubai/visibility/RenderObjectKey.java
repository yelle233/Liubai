package com.yelle233.liubai.visibility;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.UUID;

public record RenderObjectKey(ResourceKey<Level> dimension, Kind kind, long primary, long secondary) {
    public enum Kind { ENTITY, BLOCK_ENTITY }

    public static RenderObjectKey entity(ResourceKey<Level> dimension, UUID uuid) {
        return new RenderObjectKey(dimension, Kind.ENTITY, uuid.getMostSignificantBits(), uuid.getLeastSignificantBits());
    }

    public static RenderObjectKey blockEntity(ResourceKey<Level> dimension, long packedPosition) {
        return new RenderObjectKey(dimension, Kind.BLOCK_ENTITY, packedPosition, 0);
    }
}
