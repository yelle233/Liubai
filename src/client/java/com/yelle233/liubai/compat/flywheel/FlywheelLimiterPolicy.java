package com.yelle233.liubai.compat.flywheel;

import com.yelle233.liubai.client.PressureLevel;

/** Pure policy for adapting Create Fly's client-side visual update divisor. */
public final class FlywheelLimiterPolicy {
    private FlywheelLimiterPolicy() {
    }

    public static int adjust(double distanceSquared, int original, double configuredSafeDistance,
                             PressureLevel pressure, int highMultiplier, int criticalMultiplier,
                             int maxTemporalInterval) {
        if (pressure == PressureLevel.NORMAL) return original;
        double safeDistance = Math.max(16.0, configuredSafeDistance);
        if (distanceSquared <= safeDistance * safeDistance) return original;

        int multiplier = pressure == PressureLevel.CRITICAL ? criticalMultiplier : highMultiplier;
        int adjusted = Math.max(original, (int) Math.min(31L, (long) original * multiplier));
        if (pressure == PressureLevel.CRITICAL && distanceSquared >= 32.0 * 32.0) {
            adjusted = Math.max(adjusted, Math.min(31, maxTemporalInterval));
        }
        return adjusted;
    }
}
