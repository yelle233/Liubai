package com.yelle233.liubai.client;

import net.minecraft.world.phys.Vec3;

public record FrameContext(
        long frame,
        Vec3 cameraPosition,
        float cameraYaw,
        float cameraPitch,
        double fovDegrees,
        int screenHeight,
        PressureLevel pressure
) {
    public static final FrameContext EMPTY = new FrameContext(0, Vec3.ZERO, 0, 0, 70, 1, PressureLevel.NORMAL);

    public double projectedRadiusPixels(double worldRadius, Vec3 center) {
        double distance = Math.sqrt(Math.max(0.0001, cameraPosition.distanceToSqr(center)));
        double focalLength = screenHeight / (2.0 * Math.tan(Math.toRadians(fovDegrees) * 0.5));
        return worldRadius / distance * focalLength;
    }
}
