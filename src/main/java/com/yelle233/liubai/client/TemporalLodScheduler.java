package com.yelle233.liubai.client;

import com.yelle233.liubai.config.ConfigSnapshot;
import net.minecraft.world.phys.Vec3;

/** Stable update-slot scheduling for opt-in render adapters. It never skips game logic ticks. */
public final class TemporalLodScheduler {
    private FrameContext frame = FrameContext.EMPTY;
    private ConfigSnapshot config;

    public void beginFrame(FrameContext frame, ConfigSnapshot config) {
        this.frame = frame;
        this.config = config;
    }

    public int recommendedInterval(Vec3 center, double worldRadius, boolean important) {
        if (config == null || !config.enabled() || !config.temporalLod() || important) return 1;
        double pixels = frame.projectedRadiusPixels(Math.max(0.1, worldRadius), center);
        int interval;
        if (pixels >= 24.0) interval = 1;
        else if (pixels >= 8.0) interval = frame.pressure() == PressureLevel.NORMAL ? 1 : 2;
        else interval = switch (frame.pressure()) {
            case NORMAL -> 2;
            case HIGH -> 4;
            case CRITICAL -> config.maxTemporalInterval();
        };
        return Math.max(1, Math.min(config.maxTemporalInterval(), interval));
    }

    public boolean shouldUpdate(long stableKey, int interval) {
        if (interval <= 1) return true;
        return Math.floorMod(frame.frame() + stableKey, interval) == 0;
    }
}
