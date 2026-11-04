package com.yelle233.liubai;

import com.yelle233.liubai.client.FrameContext;
import com.yelle233.liubai.client.HookStatus;
import com.yelle233.liubai.client.PressureLevel;
import com.yelle233.liubai.client.RenderDecision;
import com.yelle233.liubai.client.RenderStatistics;
import com.yelle233.liubai.client.StableDensitySelector;
import com.yelle233.liubai.visibility.VisibilityMath;
import com.yelle233.liubai.visibility.VisibilityService;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

/** Dependency-free assertions executed by the Gradle check lifecycle. */
public final class LiubaiSelfTest {
    public static void main(String[] args) throws Exception {
        RenderHookSelfTest.run();
        // Source-level guard: the 26.1.2 GUI API silently discards RGB-only (zero-alpha) text.
        // Inspect the real HUD call sites without bootstrapping a graphical Minecraft client.
        String hudSource = Files.readString(Path.of("src/main/java/com/yelle233/liubai/client/ClientEvents.java"));
        var colors = Pattern.compile("0x([0-9A-Fa-f]+)\\b").matcher(hudSource);
        int checkedColors = 0;
        while (colors.find()) {
            String color = colors.group(1);
            assert color.length() == 8 && Integer.parseInt(color.substring(0, 2), 16) > 0
                    : "HUD colors must use explicit nonzero ARGB alpha: " + colors.group();
            checkedColors++;
        }
        assert checkedColors > 0 : "HUD color regression check must inspect actual colors";

        AABB original = new AABB(0, 0, 0, 4, 4, 4);
        assert !VisibilityMath.boundsChanged(original, original.move(0.1, 0, 0), 0.25)
                : "sub-tolerance movement should keep a stable visibility record";
        assert VisibilityMath.boundsChanged(original, original.move(0.3, 0, 0), 0.25)
                : "moving render bounds must invalidate the visibility record";
        assert VisibilityMath.boundsChanged(original, new AABB(0, 0, 0, 4.3, 4, 4), 0.25)
                : "size-only render-bound changes must invalidate the visibility record";
        assert !VisibilityMath.isFinite(AABB.INFINITE) : "unbounded renderers must use the conservative path";
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

        // Exercise the retained HUD counters after removing unsupported mod-specific buckets.
        RenderStatistics statistics = new RenderStatistics();
        VisibilityService.Counts counts = new VisibilityService.Counts(1, 2, 3, 4, 5, 6, 7);
        for (int i = 0; i < 64; i++) {
            statistics.entity(RenderDecision.OCCLUDED);
            statistics.entity(RenderDecision.TOO_SMALL);
            statistics.entity(RenderDecision.DENSITY_LIMITED);
            statistics.blockEntity(RenderDecision.OCCLUDED);
            statistics.shadowSkipped();
            statistics.nameTagSkipped();
            statistics.particleAccepted();
            statistics.particleSkipped();
            statistics.temporalUpdateDeferred();
            statistics.finishFrame(counts, 42);
        }
        // Flush the real one-second statistics window rather than checking only its gauges.
        Thread.sleep(1050);
        statistics.finishFrame(counts, 42);
        RenderStatistics.Snapshot snapshot = statistics.snapshot();
        assert snapshot.entitiesTested() > 0 && snapshot.entitiesSkipped() > 0;
        assert snapshot.entitiesOccluded() > 0 && snapshot.entitiesTooSmall() > 0
                && snapshot.entitiesDensityLimited() > 0 : "entity skip reasons must survive the rate window";
        assert snapshot.blockEntitiesTested() > 0 && snapshot.blockEntitiesSkipped() > 0
                && snapshot.blockEntitiesOccluded() > 0 : "block entity skip counts must reach the HUD";
        assert snapshot.shadowsSkipped() > 0 && snapshot.nameTagsSkipped() > 0
                && snapshot.particlesSkipped() > 0 && snapshot.temporalUpdatesDeferred() > 0;
        assert snapshot.visible() == 1 && snapshot.occluded() == 2 && snapshot.unknown() == 3;
        assert snapshot.queued() == 4 && snapshot.visibilityChecks() == 5;
        assert snapshot.visibilityTimeouts() == 6 && snapshot.visibilityMicros() == 7;
        assert snapshot.liveParticles() == 42 : "HUD gauges must retain their original mapping";

        assert !HookStatus.snapshot().entity() && !HookStatus.snapshot().blockEntity()
                && !HookStatus.snapshot().particle();
        HookStatus.entityObserved();
        HookStatus.blockEntityObserved();
        HookStatus.particleObserved();
        assert HookStatus.snapshot().entity() && HookStatus.snapshot().blockEntity()
                && HookStatus.snapshot().particle() : "all retained HUD hooks must be observable";
    }
}
