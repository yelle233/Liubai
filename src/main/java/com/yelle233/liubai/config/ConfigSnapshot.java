package com.yelle233.liubai.config;

import java.util.Set;
import java.util.stream.Collectors;

/** Immutable per-frame copy, avoiding synchronized config lookups in hot hooks. */
public record ConfigSnapshot(
        boolean enabled,
        int targetFps,
        boolean adaptiveMode,
        boolean entityCulling,
        boolean blockEntityCulling,
        boolean occlusionCulling,
        OcclusionMode occlusionMode,
        double safeDistance,
        double occlusionMinDistance,
        int checksPerFrame,
        int occludedConfirmations,
        int cacheTtlFrames,
        double minimumProjectedRadius,
        boolean screenSpaceLod,
        boolean temporalLod,
        int maxTemporalInterval,
        boolean reduceShadows,
        double shadowDistance,
        boolean reduceNameTags,
        double nameTagDistance,
        boolean reduceParticles,
        double particleDistance,
        boolean flywheelAdaptiveLimiter,
        int flywheelHighMultiplier,
        int flywheelCriticalMultiplier,
        boolean showHud,
        Set<String> entityAllowlist,
        Set<String> blockEntityAllowlist,
        Set<String> disabledNamespaces
) {
    public static ConfigSnapshot read() {
        return new ConfigSnapshot(
                ClientConfig.ENABLED.get(), ClientConfig.TARGET_FPS.get(), ClientConfig.ADAPTIVE_MODE.get(),
                ClientConfig.ENTITY_CULLING.get(), ClientConfig.BLOCK_ENTITY_CULLING.get(), ClientConfig.OCCLUSION_CULLING.get(),
                ClientConfig.OCCLUSION_MODE.get(),
                ClientConfig.SAFE_DISTANCE.get(), ClientConfig.OCCLUSION_MIN_DISTANCE.get(),
                ClientConfig.OCCLUSION_CHECKS_PER_FRAME.get(), ClientConfig.OCCLUDED_CONFIRMATIONS.get(),
                ClientConfig.CACHE_TTL_FRAMES.get(), ClientConfig.MIN_PROJECTED_RADIUS.get(),
                ClientConfig.SCREEN_SPACE_LOD.get(), ClientConfig.TEMPORAL_LOD.get(), ClientConfig.MAX_TEMPORAL_INTERVAL.get(),
                ClientConfig.REDUCE_SHADOWS.get(), ClientConfig.SHADOW_DISTANCE.get(),
                ClientConfig.REDUCE_NAME_TAGS.get(), ClientConfig.NAME_TAG_DISTANCE.get(),
                ClientConfig.REDUCE_PARTICLES.get(), ClientConfig.PARTICLE_DISTANCE.get(),
                ClientConfig.FLYWHEEL_ADAPTIVE_LIMITER.get(), ClientConfig.FLYWHEEL_HIGH_MULTIPLIER.get(),
                ClientConfig.FLYWHEEL_CRITICAL_MULTIPLIER.get(), ClientConfig.SHOW_HUD.get(),
                copy(ClientConfig.ENTITY_ALLOWLIST.get()), copy(ClientConfig.BLOCK_ENTITY_ALLOWLIST.get()),
                copy(ClientConfig.DISABLED_NAMESPACES.get()));
    }

    private static Set<String> copy(Iterable<? extends String> values) {
        java.util.stream.Stream.Builder<String> builder = java.util.stream.Stream.builder();
        values.forEach(builder::add);
        return builder.build().collect(Collectors.toUnmodifiableSet());
    }
}
