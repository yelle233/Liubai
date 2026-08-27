package com.yelle233.liubai.compat.flywheel;

import com.yelle233.liubai.client.LiubaiClientSystem;

/** Optional bridge used by the version-locked Create Fly Mixin. */
public final class FlywheelHooks {
    private FlywheelHooks() {
    }

    public static int adjustUpdateDivisor(double distanceSquared, int original) {
        return LiubaiClientSystem.INSTANCE.adjustFlywheelUpdateDivisor(distanceSquared, original);
    }
}
