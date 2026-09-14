package com.yelle233.liubai.visibility;

import com.yelle233.liubai.client.FrameContext;
import com.yelle233.liubai.config.ConfigSnapshot;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

/** Conservative, prioritized visibility cache. All world reads remain on the client thread. */
public final class VisibilityService {
    private static final int MAX_PENDING = 4096;
    private static final int MAX_HEAP_ENTRIES = MAX_PENDING * 4;
    private static final double BOUNDS_TOLERANCE = 0.25;

    private final Map<RenderObjectKey, Record> records = new HashMap<>();
    private final Map<RenderObjectKey, Request> pending = new HashMap<>();
    private final PriorityQueue<Request> queue = new PriorityQueue<>(Comparator
            .comparingDouble(Request::priority).thenComparingLong(Request::sequence));
    private final Map<Long, Boolean> frameSolidBlocks = new HashMap<>();
    private long requestSequence;
    private Vec3 lastProcessCamera;
    private int lastProcessed;
    private int lastTimedOut;
    private long lastProcessNanos;

    public VisibilityState query(RenderObjectKey key, AABB bounds, FrameContext frame, ConfigSnapshot config) {
        if (!VisibilityMath.isFinite(bounds)) return VisibilityState.VISIBLE;
        Record record = records.get(key);
        boolean cameraMoved = record != null && record.camera.distanceToSqr(frame.cameraPosition()) > 0.25;
        boolean boundsChanged = record != null && VisibilityMath.boundsChanged(record.bounds, bounds, BOUNDS_TOLERANCE);
        boolean expired = record == null || frame.frame() >= record.nextCheckFrame || cameraMoved || boundsChanged;
        if (expired) enqueue(key, bounds, frame, record, cameraMoved || boundsChanged);
        if (record == null || boundsChanged) return VisibilityState.UNKNOWN;
        if (cameraMoved && (record.state != VisibilityState.OCCLUDED || record.proof == null
                || !record.proof.stillBlocks(frame.cameraPosition()))) return VisibilityState.UNKNOWN;
        return record.state;
    }

    private void enqueue(RenderObjectKey key, AABB bounds, FrameContext frame, Record record, boolean urgent) {
        Request existing = pending.get(key);
        if (existing != null && !VisibilityMath.boundsChanged(existing.bounds, bounds, BOUNDS_TOLERANCE)
                && (!urgent || existing.urgent)) return;
        if (existing == null && pending.size() >= MAX_PENDING) return;
        double distanceSqr = frame.cameraPosition().distanceToSqr(bounds.getCenter());
        double priority = distanceSqr;
        if (record != null && record.state == VisibilityState.OCCLUDED) priority -= 1_000_000_000.0;
        if (urgent) priority -= 2_000_000_000.0;
        Request request = new Request(key, bounds, priority, ++requestSequence, urgent);
        pending.put(key, request);
        queue.add(request);
        if (queue.size() > MAX_HEAP_ENTRIES) rebuildQueue();
    }

    private void rebuildQueue() {
        queue.clear();
        queue.addAll(pending.values());
    }

    public void process(ClientLevel level, FrameContext frame, ConfigSnapshot config) {
        frameSolidBlocks.clear();
        boolean moving = lastProcessCamera != null && lastProcessCamera.distanceToSqr(frame.cameraPosition()) > 0.04;
        int limit = Math.min(512, config.checksPerFrame() * (moving ? 2 : 1));
        long start = System.nanoTime();
        long deadline = start + config.visibilityBudgetMicros() * 1_000L;
        int checked = 0;
        lastTimedOut = 0;
        while (checked < limit) {
            if (System.nanoTime() >= deadline) break;
            Request request = pollLatest();
            if (request == null) break;
            TestOutcome result = testBounds(level, frame.cameraPosition(), request.bounds, deadline);
            if (result.result == TestResult.TIMED_OUT) {
                defer(request);
                lastTimedOut++;
                checked++;
                break;
            }
            apply(request, result, frame, config);
            checked++;
            if (System.nanoTime() >= deadline) break;
        }
        lastProcessed = checked;
        lastProcessNanos = System.nanoTime() - start;
        lastProcessCamera = frame.cameraPosition();

        if ((frame.frame() & 255L) == 0L) {
            long oldest = frame.frame() - Math.max(600L, config.cacheTtlFrames() * 20L);
            records.values().removeIf(record -> record.lastCheckedFrame < oldest);
        }
    }

    private Request pollLatest() {
        Request request;
        while ((request = queue.poll()) != null) {
            if (pending.get(request.key) == request) {
                pending.remove(request.key);
                return request;
            }
        }
        return null;
    }

    private void defer(Request request) {
        Request deferred = new Request(request.key, request.bounds, request.priority, ++requestSequence, true);
        pending.put(deferred.key, deferred);
        queue.add(deferred);
    }

    private void apply(Request request, TestOutcome outcome, FrameContext frame, ConfigSnapshot config) {
        Record old = records.get(request.key);
        int occludedCount = old == null ? 0 : old.consecutiveOccluded;
        VisibilityState state = old == null ? VisibilityState.UNKNOWN : old.state;
        OcclusionProof proof = null;
        if (outcome.result == TestResult.VISIBLE) {
            state = VisibilityState.VISIBLE;
            occludedCount = 0;
        } else if (outcome.result == TestResult.OCCLUDED) {
            occludedCount++;
            state = occludedCount >= config.occludedConfirmations() ? VisibilityState.OCCLUDED : VisibilityState.UNKNOWN;
            proof = outcome.proof;
        } else {
            state = VisibilityState.UNKNOWN;
            occludedCount = 0;
        }

        int interval = switch (state) {
            case VISIBLE -> Math.min(6, config.cacheTtlFrames());
            case OCCLUDED -> frame.cameraPosition().distanceToSqr(request.bounds.getCenter()) <= 1024.0
                    ? Math.min(10, config.cacheTtlFrames()) : config.cacheTtlFrames();
            case UNKNOWN -> 2;
        };
        records.put(request.key, new Record(state, occludedCount, frame.frame(), frame.frame() + interval,
                frame.cameraPosition(), request.bounds, proof));
    }

    private TestOutcome testBounds(ClientLevel level, Vec3 camera, AABB box, long deadlineNanos) {
        if (System.nanoTime() >= deadlineNanos) return TestOutcome.TIMED_OUT;
        if (!VisibilityMath.isFinite(box) || box.contains(camera)) return TestOutcome.VISIBLE;
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
        BlockPos[] blockers = new BlockPos[samples.length];
        boolean unknown = false;
        for (int i = 0; i < samples.length; i++) {
            RayTrace ray = traceStrongOccluders(level, camera, samples[i], deadlineNanos);
            if (ray.result == RayResult.TIMED_OUT) return TestOutcome.TIMED_OUT;
            if (ray.result == RayResult.CLEAR) return TestOutcome.VISIBLE;
            if (ray.result == RayResult.UNKNOWN) unknown = true;
            blockers[i] = ray.blocker;
        }
        return unknown ? TestOutcome.UNKNOWN : new TestOutcome(TestResult.OCCLUDED, new OcclusionProof(samples, blockers));
    }

    private RayTrace traceStrongOccluders(ClientLevel level, Vec3 start, Vec3 end, long deadlineNanos) {
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
            if ((i & 7) == 0 && System.nanoTime() >= deadlineNanos) return RayTrace.TIMED_OUT;
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
            if (x == endX && y == endY && z == endZ) return RayTrace.CLEAR;
            pos.set(x, y, z);
            if (!level.hasChunkAt(pos)) return RayTrace.UNKNOWN;
            long packed = pos.asLong();
            Boolean solid = frameSolidBlocks.get(packed);
            if (solid == null) {
                BlockState state = level.getBlockState(pos);
                solid = state.canOcclude() && state.isSolidRender();
                frameSolidBlocks.put(packed, solid);
            }
            if (solid) return new RayTrace(RayResult.BLOCKED, pos.immutable());
        }
        return RayTrace.CLEAR;
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
        return new Counts(visible, occluded, unknown, pending.size(), lastProcessed, lastTimedOut,
                lastProcessNanos / 1_000L);
    }

    public void clear() {
        records.clear();
        pending.clear();
        queue.clear();
        frameSolidBlocks.clear();
        lastProcessCamera = null;
        lastProcessed = 0;
        lastTimedOut = 0;
        lastProcessNanos = 0;
    }

    public record Counts(int visible, int occluded, int unknown, int queued, int checked, int timedOut, long micros) {
    }

    private record Request(RenderObjectKey key, AABB bounds, double priority, long sequence, boolean urgent) {
    }

    private record Record(VisibilityState state, int consecutiveOccluded, long lastCheckedFrame,
                          long nextCheckFrame, Vec3 camera, AABB bounds, OcclusionProof proof) {
    }

    private record OcclusionProof(Vec3[] samples, BlockPos[] blockers) {
        private boolean stillBlocks(Vec3 camera) {
            for (int i = 0; i < samples.length; i++) {
                BlockPos blocker = blockers[i];
                if (blocker == null || !VisibilityMath.segmentCrossesBlock(camera, samples[i],
                        blocker.getX(), blocker.getY(), blocker.getZ())) return false;
            }
            return true;
        }
    }

    private record TestOutcome(TestResult result, OcclusionProof proof) {
        private static final TestOutcome VISIBLE = new TestOutcome(TestResult.VISIBLE, null);
        private static final TestOutcome UNKNOWN = new TestOutcome(TestResult.UNKNOWN, null);
        private static final TestOutcome TIMED_OUT = new TestOutcome(TestResult.TIMED_OUT, null);
    }

    private record RayTrace(RayResult result, BlockPos blocker) {
        private static final RayTrace CLEAR = new RayTrace(RayResult.CLEAR, null);
        private static final RayTrace UNKNOWN = new RayTrace(RayResult.UNKNOWN, null);
        private static final RayTrace TIMED_OUT = new RayTrace(RayResult.TIMED_OUT, null);
    }

    private enum TestResult { VISIBLE, OCCLUDED, UNKNOWN, TIMED_OUT }

    private enum RayResult { CLEAR, BLOCKED, UNKNOWN, TIMED_OUT }
}
