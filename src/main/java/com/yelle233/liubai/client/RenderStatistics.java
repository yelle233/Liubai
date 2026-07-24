package com.yelle233.liubai.client;

import com.yelle233.liubai.visibility.VisibilityService;

import java.util.concurrent.atomic.AtomicInteger;

/** One-second rates plus current gauges; avoids the old render-frame/tick aliasing in the HUD. */
public final class RenderStatistics {
    private int entitiesTested, entitiesSkipped, entitiesOccluded, entitiesTooSmall, entitiesDensityLimited;
    private int blockEntitiesTested, blockEntitiesSkipped, blockEntitiesOccluded;
    private int shadowsSkipped, nameTagsSkipped, particlesAccepted, particlesSkipped, temporalUpdatesDeferred;
    private final AtomicInteger flywheelLimiterAdjusted = new AtomicInteger();
    private int sableEntitiesBypassed, sableBlockEntitiesBypassed, sableParticlesBypassed;
    private final AtomicInteger sableFlywheelBypassed = new AtomicInteger();
    private long[] window = new long[18];
    private long windowStartNanos = System.nanoTime();
    private Rate rate = Rate.EMPTY;
    private VisibilityService.Counts visibility = new VisibilityService.Counts(0, 0, 0, 0, 0, 0);
    private int liveParticles;

    public void entity(RenderDecision decision) {
        entitiesTested++;
        if (decision.skip()) {
            entitiesSkipped++;
            if (decision.reason() == RenderDecision.Reason.OCCLUDED) entitiesOccluded++;
            if (decision.reason() == RenderDecision.Reason.TOO_SMALL) entitiesTooSmall++;
            if (decision.reason() == RenderDecision.Reason.DENSITY_LIMIT) entitiesDensityLimited++;
        }
    }

    public void blockEntity(RenderDecision decision) {
        blockEntitiesTested++;
        if (decision.skip()) {
            blockEntitiesSkipped++;
            if (decision.reason() == RenderDecision.Reason.OCCLUDED) blockEntitiesOccluded++;
        }
    }

    public void shadowSkipped() { shadowsSkipped++; }
    public void nameTagSkipped() { nameTagsSkipped++; }
    public void particleAccepted() { particlesAccepted++; }
    public void particleSkipped() { particlesSkipped++; }
    public void temporalUpdateDeferred() { temporalUpdatesDeferred++; }
    public void flywheelLimiterAdjusted() { flywheelLimiterAdjusted.incrementAndGet(); }
    public void sableEntityBypassed() { sableEntitiesBypassed++; }
    public void sableBlockEntityBypassed() { sableBlockEntitiesBypassed++; }
    public void sableParticleBypassed() { sableParticlesBypassed++; }
    public void sableFlywheelBypassed() { sableFlywheelBypassed.incrementAndGet(); }

    public void finishFrame(VisibilityService.Counts visibility, int liveParticles) {
        this.visibility = visibility;
        this.liveParticles = liveParticles;
        int flywheel = flywheelLimiterAdjusted.getAndSet(0);
        int safeFlywheel = sableFlywheelBypassed.getAndSet(0);
        int[] values = {
                entitiesTested, entitiesSkipped, entitiesOccluded, entitiesTooSmall, entitiesDensityLimited,
                blockEntitiesTested, blockEntitiesSkipped, blockEntitiesOccluded,
                shadowsSkipped, nameTagsSkipped, particlesAccepted, particlesSkipped, temporalUpdatesDeferred, flywheel,
                sableEntitiesBypassed, sableBlockEntitiesBypassed, sableParticlesBypassed, safeFlywheel
        };
        for (int i = 0; i < values.length; i++) window[i] += values[i];
        long now = System.nanoTime();
        long elapsed = now - windowStartNanos;
        if (elapsed >= 1_000_000_000L) {
            double seconds = elapsed / 1_000_000_000.0;
            int[] perSecond = new int[window.length];
            for (int i = 0; i < window.length; i++) perSecond[i] = (int)Math.round(window[i] / seconds);
            rate = new Rate(perSecond);
            window = new long[18];
            windowStartNanos = now;
        }
        entitiesTested = entitiesSkipped = entitiesOccluded = entitiesTooSmall = entitiesDensityLimited = 0;
        blockEntitiesTested = blockEntitiesSkipped = blockEntitiesOccluded = 0;
        shadowsSkipped = nameTagsSkipped = particlesAccepted = particlesSkipped = temporalUpdatesDeferred = 0;
        sableEntitiesBypassed = sableBlockEntitiesBypassed = sableParticlesBypassed = 0;
    }

    public Snapshot snapshot() {
        return new Snapshot(rate.entitiesTested, rate.entitiesSkipped, rate.entitiesOccluded, rate.entitiesTooSmall,
                rate.entitiesDensityLimited, rate.blockEntitiesTested, rate.blockEntitiesSkipped, rate.blockEntitiesOccluded,
                rate.shadowsSkipped, rate.nameTagsSkipped, rate.particlesAccepted, rate.particlesSkipped,
                rate.temporalUpdatesDeferred, rate.flywheelLimiterAdjusted,
                rate.sableEntitiesBypassed, rate.sableBlockEntitiesBypassed, rate.sableParticlesBypassed,
                rate.sableFlywheelBypassed,
                visibility.visible(), visibility.occluded(), visibility.unknown(), visibility.queued(),
                visibility.checked(), visibility.micros(), liveParticles);
    }

    private record Rate(int entitiesTested, int entitiesSkipped, int entitiesOccluded, int entitiesTooSmall,
                        int entitiesDensityLimited, int blockEntitiesTested, int blockEntitiesSkipped,
                        int blockEntitiesOccluded, int shadowsSkipped, int nameTagsSkipped, int particlesAccepted,
                        int particlesSkipped, int temporalUpdatesDeferred, int flywheelLimiterAdjusted,
                        int sableEntitiesBypassed, int sableBlockEntitiesBypassed, int sableParticlesBypassed,
                        int sableFlywheelBypassed) {
        private static final Rate EMPTY = new Rate(new int[18]);
        private Rate(int[] values) {
            this(values[0], values[1], values[2], values[3], values[4], values[5], values[6], values[7],
                    values[8], values[9], values[10], values[11], values[12], values[13], values[14], values[15],
                    values[16], values[17]);
        }
    }

    public record Snapshot(int entitiesTested, int entitiesSkipped, int entitiesOccluded, int entitiesTooSmall,
                           int entitiesDensityLimited, int blockEntitiesTested, int blockEntitiesSkipped,
                           int blockEntitiesOccluded, int shadowsSkipped, int nameTagsSkipped, int particlesAccepted,
                           int particlesSkipped, int temporalUpdatesDeferred, int flywheelLimiterAdjusted,
                           int sableEntitiesBypassed, int sableBlockEntitiesBypassed, int sableParticlesBypassed,
                           int sableFlywheelBypassed,
                           int visible, int occluded, int unknown, int queued, int visibilityChecks,
                           long visibilityMicros, int liveParticles) {
    }
}
