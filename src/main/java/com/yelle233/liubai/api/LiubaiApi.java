package com.yelle233.liubai.api;

import com.yelle233.liubai.client.LiubaiClientSystem;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import java.util.function.BiFunction;

/** Public, client-only opt-in API for third-party visual safety rules and temporal update slots. */
public final class LiubaiApi {
    private static final List<Predicate<Entity>> ENTITY_SAFETY_RULES = new CopyOnWriteArrayList<>();
    private static final List<Predicate<BlockEntity>> BLOCK_ENTITY_SAFETY_RULES = new CopyOnWriteArrayList<>();
    private static final List<BiFunction<Entity, AABB, AABB>> ENTITY_BOUNDS_RULES = new CopyOnWriteArrayList<>();
    private static final List<BiFunction<BlockEntity, AABB, AABB>> BLOCK_ENTITY_BOUNDS_RULES = new CopyOnWriteArrayList<>();

    private LiubaiApi() {
    }

    public static void registerForceVisibleEntityRule(Predicate<Entity> rule) {
        ENTITY_SAFETY_RULES.add(java.util.Objects.requireNonNull(rule));
    }

    public static void registerForceVisibleBlockEntityRule(Predicate<BlockEntity> rule) {
        BLOCK_ENTITY_SAFETY_RULES.add(java.util.Objects.requireNonNull(rule));
    }

    /** Registers an opt-in render-bounds override. Return the supplied bounds when the rule does not apply. */
    public static void registerEntityRenderBoundsRule(BiFunction<Entity, AABB, AABB> rule) {
        ENTITY_BOUNDS_RULES.add(java.util.Objects.requireNonNull(rule));
    }

    /** Registers an opt-in block-entity render-bounds override. Return the supplied bounds when the rule does not apply. */
    public static void registerBlockEntityRenderBoundsRule(BiFunction<BlockEntity, AABB, AABB> rule) {
        BLOCK_ENTITY_BOUNDS_RULES.add(java.util.Objects.requireNonNull(rule));
    }

    public static boolean isForceVisible(Entity entity) {
        for (Predicate<Entity> rule : ENTITY_SAFETY_RULES) if (safeTest(rule, entity)) return true;
        return false;
    }

    public static boolean isForceVisible(BlockEntity blockEntity) {
        for (Predicate<BlockEntity> rule : BLOCK_ENTITY_SAFETY_RULES) if (safeTest(rule, blockEntity)) return true;
        return false;
    }

    private static <T> boolean safeTest(Predicate<T> rule, T value) {
        try {
            return rule.test(value);
        } catch (RuntimeException ignored) {
            return true;
        }
    }

    public static AABB resolveRenderBounds(Entity entity, AABB fallback) {
        AABB result = fallback;
        for (BiFunction<Entity, AABB, AABB> rule : ENTITY_BOUNDS_RULES) {
            try {
                AABB candidate = rule.apply(entity, result);
                if (candidate != null) result = candidate;
            } catch (RuntimeException ignored) {
                return AABB.INFINITE;
            }
        }
        return result;
    }

    public static AABB resolveRenderBounds(BlockEntity blockEntity, AABB fallback) {
        AABB result = fallback;
        for (BiFunction<BlockEntity, AABB, AABB> rule : BLOCK_ENTITY_BOUNDS_RULES) {
            try {
                AABB candidate = rule.apply(blockEntity, result);
                if (candidate != null) result = candidate;
            } catch (RuntimeException ignored) {
                return AABB.INFINITE;
            }
        }
        return result;
    }

    public static int recommendedRenderUpdateInterval(Vec3 center, double worldRadius, boolean important) {
        return LiubaiClientSystem.INSTANCE.temporalScheduler().recommendedInterval(center, worldRadius, important);
    }

    public static RenderQuality recommendedRenderQuality(Vec3 center, double worldRadius, boolean important) {
        return LiubaiClientSystem.INSTANCE.temporalScheduler().recommendedQuality(center, worldRadius, important);
    }

    public static boolean shouldRunRenderUpdate(long stableKey, int interval) {
        boolean update = LiubaiClientSystem.INSTANCE.temporalScheduler().shouldUpdate(stableKey, interval);
        if (!update) LiubaiClientSystem.INSTANCE.statisticsRecorder().temporalUpdateDeferred();
        return update;
    }
}
