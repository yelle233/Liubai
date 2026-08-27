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
        int visibilityBudgetMicros,
        int occludedConfirmations,
        int cacheTtlFrames,
        double minimumProjectedRadius,
        boolean screenSpaceLod,
        boolean temporalLod,
        int maxTemporalInterval,
        boolean denseVanillaEntities,
        double denseEntityDistance,
        int denseEntityHighLimit,
        int denseEntityCriticalLimit,
        boolean reduceShadows,
        double shadowDistance,
        boolean reduceNameTags,
        double nameTagDistance,
        boolean reduceParticles,
        double particleDistance,
        int particleSoftLimit,
        int particleHighBudget,
        int particleCriticalBudget,
        boolean flywheelAdaptiveLimiter,
        int flywheelHighMultiplier,
        int flywheelCriticalMultiplier,
        boolean showHud,
        Set<String> entityAllowlist,
        Set<String> blockEntityAllowlist,
        Set<String> disabledNamespaces
) {
    public static ConfigSnapshot read() {
        ClientConfig c = ClientConfig.get();
        return new ConfigSnapshot(c.enabled,c.targetFps,c.adaptiveMode,c.entityCulling,c.blockEntityCulling,c.occlusionCulling,c.occlusionMode,
                c.safeDistance,c.occlusionMinDistance,c.checksPerFrame,c.visibilityBudgetMicros,c.occludedConfirmations,c.cacheTtlFrames,c.minimumProjectedRadius,
                c.screenSpaceLod,c.temporalLod,c.maxTemporalInterval,c.denseVanillaEntities,c.denseEntityDistance,c.denseEntityHighLimit,c.denseEntityCriticalLimit,
                c.reduceShadows,c.shadowDistance,c.reduceNameTags,c.nameTagDistance,c.reduceParticles,c.particleDistance,c.particleSoftLimit,c.particleHighBudget,c.particleCriticalBudget,
                c.flywheelAdaptiveLimiter,c.flywheelHighMultiplier,c.flywheelCriticalMultiplier,
                c.showHud,copy(c.entityAllowlist),copy(c.blockEntityAllowlist),copy(c.disabledNamespaces));
    }

    private static Set<String> copy(Iterable<? extends String> values) {
        java.util.stream.Stream.Builder<String> builder = java.util.stream.Stream.builder();
        values.forEach(builder::add);
        return builder.build().collect(Collectors.toUnmodifiableSet());
    }
}
