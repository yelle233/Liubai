package com.yelle233.liubai.client;

import com.yelle233.liubai.config.ConfigSnapshot;

/** Slow, hysteretic controller. Quality drops gradually and recovers more slowly. */
public final class FrameBudgetController {
    private static final long DEGRADE_AFTER_NANOS = 400_000_000L;
    private static final long RECOVER_AFTER_NANOS = 3_000_000_000L;
    private static final long RECOVERY_COOLDOWN_NANOS = 2_000_000_000L;
    private final double[] recentSamples = new double[300];
    private double averageRenderMillis = 16.67;
    private double p95RenderMillis = 16.67;
    private double p99RenderMillis = 16.67;
    private PressureLevel pressure = PressureLevel.NORMAL;
    private long overBudgetSinceNanos;
    private long underBudgetSinceNanos;
    private long recoveryAllowedNanos;
    private long renderStartNanos;
    private int sampleCount;
    private int sampleCursor;

    public void beginFrame() {
        renderStartNanos = System.nanoTime();
    }

    public void endFrame(ConfigSnapshot config, int configuredFrameLimit) {
        if (renderStartNanos == 0) return;
        double sample = (System.nanoTime() - renderStartNanos) / 1_000_000.0;
        recentSamples[sampleCursor] = sample;
        sampleCursor = (sampleCursor + 1) % recentSamples.length;
        sampleCount = Math.min(recentSamples.length, sampleCount + 1);
        if ((sampleCursor % 60) == 0 && sampleCount >= 30) updatePercentiles();
        averageRenderMillis += (sample - averageRenderMillis) * 0.08;
        if (!config.adaptiveMode()) {
            pressure = PressureLevel.NORMAL;
            overBudgetSinceNanos = underBudgetSinceNanos = recoveryAllowedNanos = 0;
            return;
        }

        long now = System.nanoTime();
        int effectiveTargetFps = configuredFrameLimit > 0 && configuredFrameLimit < 260
                ? Math.min(config.targetFps(), configuredFrameLimit) : config.targetFps();
        double target = 1000.0 / effectiveTargetFps;
        if (averageRenderMillis > target * 1.12) {
            if (overBudgetSinceNanos == 0) overBudgetSinceNanos = now;
            underBudgetSinceNanos = 0;
            if (now - overBudgetSinceNanos >= DEGRADE_AFTER_NANOS) {
                pressure = switch (pressure) {
                    case NORMAL -> PressureLevel.HIGH;
                    case HIGH, CRITICAL -> PressureLevel.CRITICAL;
                };
                recoveryAllowedNanos = now + RECOVERY_COOLDOWN_NANOS;
                overBudgetSinceNanos = now;
            }
        } else if (averageRenderMillis < target * 0.88) {
            overBudgetSinceNanos = 0;
            if (now < recoveryAllowedNanos) {
                underBudgetSinceNanos = 0;
                return;
            }
            if (underBudgetSinceNanos == 0) underBudgetSinceNanos = now;
            if (now - underBudgetSinceNanos >= RECOVER_AFTER_NANOS) {
                pressure = switch (pressure) {
                    case CRITICAL -> PressureLevel.HIGH;
                    case HIGH, NORMAL -> PressureLevel.NORMAL;
                };
                recoveryAllowedNanos = pressure == PressureLevel.NORMAL ? 0 : now + RECOVERY_COOLDOWN_NANOS;
                underBudgetSinceNanos = 0;
            }
        } else {
            overBudgetSinceNanos = 0;
            underBudgetSinceNanos = 0;
        }
    }

    public PressureLevel pressure() {
        return pressure;
    }

    public double averageRenderMillis() {
        return averageRenderMillis;
    }

    public double p95RenderMillis() { return p95RenderMillis; }
    public double p99RenderMillis() { return p99RenderMillis; }

    private void updatePercentiles() {
        double[] sorted = java.util.Arrays.copyOf(recentSamples, sampleCount);
        java.util.Arrays.sort(sorted);
        p95RenderMillis = sorted[Math.min(sorted.length - 1, (int)Math.ceil(sorted.length * 0.95) - 1)];
        p99RenderMillis = sorted[Math.min(sorted.length - 1, (int)Math.ceil(sorted.length * 0.99) - 1)];
    }

    public void reset() {
        averageRenderMillis = 16.67;
        pressure = PressureLevel.NORMAL;
        overBudgetSinceNanos = underBudgetSinceNanos = 0;
        renderStartNanos = 0;
        recoveryAllowedNanos = 0;
        sampleCount = sampleCursor = 0;
        p95RenderMillis = p99RenderMillis = 16.67;
    }
}
