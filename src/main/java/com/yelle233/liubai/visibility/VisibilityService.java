package com.yelle233.liubai.visibility;

import com.yelle233.liubai.client.FrameContext;
import com.yelle233.liubai.config.ConfigSnapshot;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Conservative, budgeted visibility cache. Render hooks only enqueue/read records;
 * all world access happens once at frame start on the client thread.
 */
public final class VisibilityService {
    private static final int MAX_QUEUE = 4096;
    private final Map<RenderObjectKey, Record> records = new HashMap<>();
    private final ArrayDeque<Request> queue = new ArrayDeque<>();
    private final Set<RenderObjectKey> queuedKeys = new HashSet<>();

    public VisibilityState query(RenderObjectKey key, AABB bounds, FrameContext frame, ConfigSnapshot config) {
        Record record = records.get(key);
        boolean moved = record != null && record.camera.distanceToSqr(frame.cameraPosition()) > 0.25;
        boolean boundsChanged = record != null && bounds.getCenter().distanceToSqr(record.bounds.getCenter()) > 1.0;
        boolean expired = record == null || frame.frame() >= record.nextCheckFrame || moved || boundsChanged;
        if (expired) enqueue(key, bounds);
        if (record == null || moved || boundsChanged) return VisibilityState.UNKNOWN;
        return record.state;
    }

    private void enqueue(RenderObjectKey key, AABB bounds) {
        if (queue.size() >= MAX_QUEUE || !queuedKeys.add(key)) return;
        queue.addLast(new Request(key, bounds));
    }

    public void process(ClientLevel level, FrameContext frame, ConfigSnapshot config) {
        int limit = config.checksPerFrame();
        for (int checked = 0; checked < limit; checked++) {
            Request request = queue.pollFirst();
            if (request == null) break;
            queuedKeys.remove(request.key);
            TestResult result = testBounds(level, frame.cameraPosition(), request.bounds);
            apply(request, result, frame, config);
        }
        if ((frame.frame() & 255L) == 0L) {
            long oldest = frame.frame() - Math.max(600L, config.cacheTtlFrames() * 20L);
            records.values().removeIf(record -> record.lastCheckedFrame < oldest);
        }
    }

    private void apply(Request request, TestResult result, FrameContext frame, ConfigSnapshot config) {
        Record old = records.get(request.key);
        int occludedCount = old == null ? 0 : old.consecutiveOccluded;
        VisibilityState state = old == null ? VisibilityState.UNKNOWN : old.state;
        if (result == TestResult.VISIBLE) {
            state = VisibilityState.VISIBLE;
            occludedCount = 0;
        } else if (result == TestResult.OCCLUDED) {
            occludedCount++;
            state = occludedCount >= config.occludedConfirmations() ? VisibilityState.OCCLUDED : VisibilityState.UNKNOWN;
        } else {
            state = VisibilityState.UNKNOWN;
            occludedCount = 0;
        }

        int interval = switch (state) {
            case VISIBLE -> Math.min(6, config.cacheTtlFrames());
            case OCCLUDED -> config.cacheTtlFrames();
            case UNKNOWN -> 2;
        };
        records.put(request.key, new Record(state, occludedCount, frame.frame(), frame.frame() + interval,
                frame.cameraPosition(), request.bounds));
    }

    private static TestResult testBounds(ClientLevel level, Vec3 camera, AABB box) {
        if (box.contains(camera)) return TestResult.VISIBLE;
        Vec3 center = box.getCenter();
        double insetX = Math.min(0.05, box.getXsize() * 0.1);
        double insetY = Math.min(0.05, box.getYsize() * 0.1);
        double insetZ = Math.min(0.05, box.getZsize() * 0.1);
        double minX = box.minX + insetX, maxX = box.maxX - insetX;
        double minY = box.minY + insetY, maxY = box.maxY - insetY;
        double minZ = box.minZ + insetZ, maxZ = box.maxZ - insetZ;
        Vec3[] samples = {
                center,
                new Vec3(minX, minY, minZ), new Vec3(minX, minY, maxZ),
                new Vec3(minX, maxY, minZ), new Vec3(minX, maxY, maxZ),
                new Vec3(maxX, minY, minZ), new Vec3(maxX, minY, maxZ),
                new Vec3(maxX, maxY, minZ), new Vec3(maxX, maxY, maxZ)
        };
        boolean unknown = false;
        for (Vec3 sample : samples) {
            RayResult ray = traceStrongOccluders(level, camera, sample);
            if (ray == RayResult.CLEAR) return TestResult.VISIBLE;
            if (ray == RayResult.UNKNOWN) unknown = true;
        }
        return unknown ? TestResult.UNKNOWN : TestResult.OCCLUDED;
    }

    /** Amanatides-Woo voxel traversal; only full visual cubes are accepted as blockers. */
    private static RayResult traceStrongOccluders(ClientLevel level, Vec3 start, Vec3 end) {
        double dx = end.x - start.x, dy = end.y - start.y, dz = end.z - start.z;
        int x = floor(start.x), y = floor(start.y), z = floor(start.z);
        int endX = floor(end.x), endY = floor(end.y), endZ = floor(end.z);
        int stepX = Integer.compare(endX, x), stepY = Integer.compare(endY, y), stepZ = Integer.compare(endZ, z);
        double tDeltaX = dx == 0 ? Double.POSITIVE_INFINITY : Math.abs(1.0 / dx);
        double tDeltaY = dy == 0 ? Double.POSITIVE_INFINITY : Math.abs(1.0 / dy);
        double tDeltaZ = dz == 0 ? Double.POSITIVE_INFINITY : Math.abs(1.0 / dz);
        double tMaxX = initialTMax(start.x, dx, stepX);
        double tMaxY = initialTMax(start.y, dy, stepY);
        double tMaxZ = initialTMax(start.z, dz, stepZ);
        int maxSteps = Math.abs(endX - x) + Math.abs(endY - y) + Math.abs(endZ - z) + 3;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int i = 0; i < maxSteps; i++) {
            if (tMaxX <= tMaxY && tMaxX <= tMaxZ) {
                x += stepX;
                tMaxX += tDeltaX;
            } else if (tMaxY <= tMaxZ) {
                y += stepY;
                tMaxY += tDeltaY;
            } else {
                z += stepZ;
                tMaxZ += tDeltaZ;
            }
            if (x == endX && y == endY && z == endZ) return RayResult.CLEAR;
            pos.set(x, y, z);
            if (!level.hasChunkAt(pos)) return RayResult.UNKNOWN;
            BlockState state = level.getBlockState(pos);
            if (state.canOcclude() && state.isSolidRender(level, pos)) return RayResult.BLOCKED;
        }
        return RayResult.CLEAR;
    }

    private static int floor(double value) {
        int integer = (int) value;
        return value < integer ? integer - 1 : integer;
    }

    private static double initialTMax(double start, double delta, int step) {
        if (step == 0 || delta == 0) return Double.POSITIVE_INFINITY;
        double boundary = step > 0 ? Math.floor(start) + 1.0 : Math.floor(start);
        return (boundary - start) / delta;
    }

    public Counts counts() {
        int visible = 0, occluded = 0, unknown = 0;
        for (Record record : records.values()) {
            switch (record.state) {
                case VISIBLE -> visible++;
                case OCCLUDED -> occluded++;
                case UNKNOWN -> unknown++;
            }
        }
        return new Counts(visible, occluded, unknown, queue.size());
    }

    public void clear() {
        records.clear();
        queue.clear();
        queuedKeys.clear();
    }

    public record Counts(int visible, int occluded, int unknown, int queued) {}
    private record Request(RenderObjectKey key, AABB bounds) {}
    private record Record(VisibilityState state, int consecutiveOccluded, long lastCheckedFrame,
                          long nextCheckFrame, Vec3 camera, AABB bounds) {}
    private enum TestResult { VISIBLE, OCCLUDED, UNKNOWN }
    private enum RayResult { CLEAR, BLOCKED, UNKNOWN }
}
