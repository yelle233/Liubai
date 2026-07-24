package com.yelle233.liubai.client;

import com.yelle233.liubai.config.ConfigSnapshot;

/** Slow, hysteretic controller. Quality drops gradually and recovers more slowly. */
public final class FrameBudgetController {
    private final double[] recentSamples = new double[300];
    private double averageRenderMillis = 16.67;
    private double p95RenderMillis = 16.67;
    private double p99RenderMillis = 16.67;
    private PressureLevel pressure = PressureLevel.NORMAL;
    private int overBudgetFrames;
    private int underBudgetFrames;
    private int recoveryCooldownFrames;
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
            overBudgetFrames = underBudgetFrames = 0;
            recoveryCooldownFrames = 0;
            return;
        }

        int effectiveTargetFps = configuredFrameLimit > 0 && configuredFrameLimit < 260
                ? Math.min(config.targetFps(), configuredFrameLimit) : config.targetFps();
        double target = 1000.0 / effectiveTargetFps;
        if (averageRenderMillis > target * 1.12) {
            overBudgetFrames++;
            underBudgetFrames = 0;
            if (overBudgetFrames >= 24) {
                pressure = switch (pressure) {
                    case NORMAL -> PressureLevel.HIGH;
                    case HIGH, CRITICAL -> PressureLevel.CRITICAL;
                };
                recoveryCooldownFrames = 180;
                overBudgetFrames = 0;
            }
        } else if (averageRenderMillis < target * 0.88) {
            if (recoveryCooldownFrames > 0) {
                recoveryCooldownFrames--;
                underBudgetFrames = 0;
                return;
            }
            underBudgetFrames++;
            overBudgetFrames = 0;
            if (underBudgetFrames >= 180) {
                pressure = switch (pressure) {
                    case CRITICAL -> PressureLevel.HIGH;
                    case HIGH, NORMAL -> PressureLevel.NORMAL;
                };
                recoveryCooldownFrames = pressure == PressureLevel.NORMAL ? 0 : 120;
                underBudgetFrames = 0;
            }
        } else {
            if (recoveryCooldownFrames > 0) recoveryCooldownFrames--;
            overBudgetFrames = Math.max(0, overBudgetFrames - 1);
            underBudgetFrames = Math.max(0, underBudgetFrames - 1);
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
        overBudgetFrames = underBudgetFrames = 0;
        renderStartNanos = 0;
        recoveryCooldownFrames = 0;
        sampleCount = sampleCursor = 0;
        p95RenderMillis = p99RenderMillis = 16.67;
    }
}
