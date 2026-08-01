package com.yelle233.liubai;

import com.yelle233.liubai.client.FrameContext;
import com.yelle233.liubai.client.PressureLevel;
import com.yelle233.liubai.client.StableDensitySelector;
import com.yelle233.liubai.visibility.VisibilityMath;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/** Dependency-free assertions executed by the Gradle check lifecycle. */
public final class LiubaiSelfTest {
    public static void main(String[] args) {
        AABB original = new AABB(0, 0, 0, 4, 4, 4);
        assert !VisibilityMath.boundsChanged(original, original.move(0.1, 0, 0), 0.25)
                : "sub-tolerance movement should keep a stable visibility record";
        assert VisibilityMath.boundsChanged(original, original.move(0.3, 0, 0), 0.25)
                : "moving render bounds must invalidate the visibility record";
        assert VisibilityMath.boundsChanged(original, new AABB(0, 0, 0, 4.3, 4, 4), 0.25)
                : "size-only render-bound changes must invalidate the visibility record";
        AABB infinite = new AABB(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY,
                Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
        assert !VisibilityMath.isFinite(infinite) : "unbounded renderers must use the conservative path";
        assert VisibilityMath.segmentCrossesBlock(new Vec3(0, 0.5, 0.5), new Vec3(4, 0.5, 0.5), 2, 0, 0)
                : "a moved camera ray crossing the confirmed full block should retain its occlusion proof";
        assert !VisibilityMath.segmentCrossesBlock(new Vec3(0, 1.5, 0.5), new Vec3(4, 1.5, 0.5), 2, 0, 0)
                : "a ray moved beyond the blocker must invalidate its occlusion proof";
        assert !VisibilityMath.segmentCrossesBlock(new Vec3(2.5, 0.5, 0.5), new Vec3(4, 0.5, 0.5), 2, 0, 0)
                : "a camera inside the old blocker must take the conservative visible path";
        assert VisibilityMath.occlusionStartDistance(12.0) == 12.0
                : "the configured occlusion distance must remain authoritative";
        assert VisibilityMath.occlusionStartDistance(4.0) == 4.0
                : "an explicit shorter occlusion distance must remain effective";
        assert VisibilityMath.occlusionStartDistance(0.0) == 2.0
                : "the near-camera margin must never be removed by a small configuration";

        FrameContext frame = new FrameContext(1, Vec3.ZERO, 0, 0, 70, 1080, PressureLevel.HIGH);
        double near = frame.projectedRadiusPixels(1.0, new Vec3(0, 0, 10));
        double far = frame.projectedRadiusPixels(1.0, new Vec3(0, 0, 20));
        assert near > far && Math.abs(near / far - 2.0) < 0.0001
                : "projected radius should scale inversely with distance";

        StableDensitySelector density = new StableDensitySelector();
        UUID first = new UUID(1, 1), second = new UUID(2, 2), third = new UUID(3, 3);
        assert !density.shouldSkip(first, 42L, 2, 1L);
        assert !density.shouldSkip(second, 42L, 2, 1L);
        assert density.shouldSkip(third, 42L, 2, 1L) : "density selection must enforce its region limit";
        assert density.shouldSkip(third, 42L, 2, 2L)
                : "changing traversal order must not replace the selected set every frame";
        assert !density.shouldSkip(first, 42L, 2, 2L);
        assert !density.shouldSkip(second, 42L, 2, 2L);
        assert !density.shouldSkip(third, 42L, 2, 5L)
                : "objects absent for several frames must release their density slot";
    }
}
