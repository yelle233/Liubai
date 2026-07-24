package com.yelle233.liubai.client;

import java.util.concurrent.atomic.AtomicInteger;

public final class RenderStatistics {
    private int entitiesTested;
    private int entitiesSkipped;
    private int entitiesOccluded;
    private int entitiesTooSmall;
    private int blockEntitiesTested;
    private int blockEntitiesSkipped;
    private int blockEntitiesOccluded;
    private int shadowsSkipped;
    private int nameTagsSkipped;
    private int particlesSkipped;
    private int temporalUpdatesDeferred;
    private final AtomicInteger flywheelLimiterAdjusted = new AtomicInteger();
    private Snapshot previous = Snapshot.EMPTY;

    public void entity(RenderDecision decision) {
        entitiesTested++;
        if (decision.skip()) {
            entitiesSkipped++;
            if (decision.reason() == RenderDecision.Reason.OCCLUDED) entitiesOccluded++;
            if (decision.reason() == RenderDecision.Reason.TOO_SMALL) entitiesTooSmall++;
        }
    }

    public void blockEntity(RenderDecision decision) {
        blockEntitiesTested++;
        if (decision.skip()) {
            blockEntitiesSkipped++;
            if (decision.reason() == RenderDecision.Reason.OCCLUDED) blockEntitiesOccluded++;
        }
    }

    public void shadowSkipped() {
        shadowsSkipped++;
    }

    public void nameTagSkipped() {
        nameTagsSkipped++;
    }

    public void particleSkipped() {
        particlesSkipped++;
    }

    public void temporalUpdateDeferred() {
        temporalUpdatesDeferred++;
    }

    public void flywheelLimiterAdjusted() {
        flywheelLimiterAdjusted.incrementAndGet();
    }

    public void finishFrame(int visible, int occluded, int unknown, int queued) {
        previous = new Snapshot(entitiesTested, entitiesSkipped, entitiesOccluded, entitiesTooSmall,
                blockEntitiesTested, blockEntitiesSkipped, blockEntitiesOccluded,
                shadowsSkipped, nameTagsSkipped, particlesSkipped, temporalUpdatesDeferred,
                flywheelLimiterAdjusted.getAndSet(0), visible, occluded, unknown, queued);
        entitiesTested = entitiesSkipped = entitiesOccluded = entitiesTooSmall = 0;
        blockEntitiesTested = blockEntitiesSkipped = blockEntitiesOccluded = 0;
        shadowsSkipped = nameTagsSkipped = particlesSkipped = temporalUpdatesDeferred = 0;
    }

    public Snapshot snapshot() {
        return previous;
    }

    public record Snapshot(int entitiesTested, int entitiesSkipped, int entitiesOccluded, int entitiesTooSmall,
                           int blockEntitiesTested, int blockEntitiesSkipped, int blockEntitiesOccluded,
                           int shadowsSkipped, int nameTagsSkipped, int particlesSkipped,
                           int temporalUpdatesDeferred, int flywheelLimiterAdjusted,
                           int visible, int occluded, int unknown, int queued) {
        static final Snapshot EMPTY = new Snapshot(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    }
}
