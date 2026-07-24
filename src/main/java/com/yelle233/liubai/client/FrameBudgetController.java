package com.yelle233.liubai.client;

import com.yelle233.liubai.config.ConfigSnapshot;

/** Slow, hysteretic controller. Quality drops gradually and recovers more slowly. */
public final class FrameBudgetController {
    private double averageRenderMillis = 16.67;
    private PressureLevel pressure = PressureLevel.NORMAL;
    private int overBudgetFrames;
    private int underBudgetFrames;
    private long renderStartNanos;

    public void beginFrame() {
        renderStartNanos = System.nanoTime();
    }

    public void endFrame(ConfigSnapshot config) {
        if (renderStartNanos == 0) return;
        double sample = (System.nanoTime() - renderStartNanos) / 1_000_000.0;
        averageRenderMillis += (sample - averageRenderMillis) * 0.08;
        if (!config.adaptiveMode()) {
            pressure = PressureLevel.NORMAL;
            overBudgetFrames = underBudgetFrames = 0;
            return;
        }

        double target = 1000.0 / config.targetFps();
        if (averageRenderMillis > target * 1.12) {
            overBudgetFrames++;
            underBudgetFrames = 0;
            if (overBudgetFrames >= 24) {
                pressure = switch (pressure) {
                    case NORMAL -> PressureLevel.HIGH;
                    case HIGH, CRITICAL -> PressureLevel.CRITICAL;
                };
                overBudgetFrames = 0;
            }
        } else if (averageRenderMillis < target * 0.88) {
            underBudgetFrames++;
            overBudgetFrames = 0;
            if (underBudgetFrames >= 120) {
                pressure = switch (pressure) {
                    case CRITICAL -> PressureLevel.HIGH;
                    case HIGH, NORMAL -> PressureLevel.NORMAL;
                };
                underBudgetFrames = 0;
            }
        } else {
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

    public void reset() {
        averageRenderMillis = 16.67;
        pressure = PressureLevel.NORMAL;
        overBudgetFrames = underBudgetFrames = 0;
        renderStartNanos = 0;
    }
}
