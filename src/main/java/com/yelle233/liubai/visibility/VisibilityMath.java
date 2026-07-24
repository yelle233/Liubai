package com.yelle233.liubai.visibility;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Pure visibility helpers kept separate so safety thresholds can be regression-tested. */
public final class VisibilityMath {
    private VisibilityMath() {
    }

    public static boolean boundsChanged(AABB previous, AABB current, double tolerance) {
        if (!isFinite(previous) || !isFinite(current)) return true;
        return Math.abs(previous.minX - current.minX) > tolerance
                || Math.abs(previous.minY - current.minY) > tolerance
                || Math.abs(previous.minZ - current.minZ) > tolerance
                || Math.abs(previous.maxX - current.maxX) > tolerance
                || Math.abs(previous.maxY - current.maxY) > tolerance
                || Math.abs(previous.maxZ - current.maxZ) > tolerance;
    }

    public static boolean isFinite(AABB box) {
        return box != null && Double.isFinite(box.minX) && Double.isFinite(box.minY) && Double.isFinite(box.minZ)
                && Double.isFinite(box.maxX) && Double.isFinite(box.maxY) && Double.isFinite(box.maxZ)
                && box.maxX >= box.minX && box.maxY >= box.minY && box.maxZ >= box.minZ;
    }

    /** True only when the segment crosses the interior of a full block, not merely an edge or corner. */
    public static boolean segmentCrossesBlock(Vec3 start, Vec3 end, int blockX, int blockY, int blockZ) {
        double epsilon = 1.0e-4;
        double minX = blockX + epsilon, maxX = blockX + 1.0 - epsilon;
        double minY = blockY + epsilon, maxY = blockY + 1.0 - epsilon;
        double minZ = blockZ + epsilon, maxZ = blockZ + 1.0 - epsilon;
        if (start.x >= minX && start.x <= maxX && start.y >= minY && start.y <= maxY
                && start.z >= minZ && start.z <= maxZ) return false;

        double tNear = 0.0;
        double tFar = 1.0;
        double dx = end.x - start.x;
        if (Math.abs(dx) < 1.0e-12) {
            if (start.x < minX || start.x > maxX) return false;
        } else {
            double first = (minX - start.x) / dx;
            double second = (maxX - start.x) / dx;
            if (first > second) { double swap = first; first = second; second = swap; }
            tNear = Math.max(tNear, first);
            tFar = Math.min(tFar, second);
            if (tNear >= tFar) return false;
        }

        double dy = end.y - start.y;
        if (Math.abs(dy) < 1.0e-12) {
            if (start.y < minY || start.y > maxY) return false;
        } else {
            double first = (minY - start.y) / dy;
            double second = (maxY - start.y) / dy;
            if (first > second) { double swap = first; first = second; second = swap; }
            tNear = Math.max(tNear, first);
            tFar = Math.min(tFar, second);
            if (tNear >= tFar) return false;
        }

        double dz = end.z - start.z;
        if (Math.abs(dz) < 1.0e-12) {
            if (start.z < minZ || start.z > maxZ) return false;
        } else {
            double first = (minZ - start.z) / dz;
            double second = (maxZ - start.z) / dz;
            if (first > second) { double swap = first; first = second; second = swap; }
            tNear = Math.max(tNear, first);
            tFar = Math.min(tFar, second);
            if (tNear >= tFar) return false;
        }
        return tFar > 0.0 && tNear < 1.0;
    }
}
