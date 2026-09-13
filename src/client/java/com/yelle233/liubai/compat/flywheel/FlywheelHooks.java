package com.yelle233.liubai.compat.flywheel;

import com.yelle233.liubai.client.LiubaiClientSystem;

public final class FlywheelHooks {
    private FlywheelHooks() {
    }

    public static int adjustUpdateDivisor(double distanceSquared, int original) {
        return LiubaiClientSystem.INSTANCE.adjustFlywheelUpdateDivisor(distanceSquared, original);
    }
}
