package com.yelle233.liubai.client;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Keeps a bounded, short-lived set of rendered objects stable across neighboring frames. */
public final class StableDensitySelector {
    private final Map<Long, Bucket> buckets = new HashMap<>();

    public boolean shouldSkip(UUID id, long region, int limit, long frame) {
        Bucket bucket = buckets.computeIfAbsent(region, ignored -> new Bucket());
        return bucket.shouldSkip(id, region, limit, frame);
    }

    public void prune(long frame) {
        buckets.values().removeIf(bucket -> bucket.lastTouchedFrame < frame - 120L);
    }

    public void clear() {
        buckets.clear();
    }

    private static long score(UUID id, long region) {
        long value = id.getMostSignificantBits() ^ Long.rotateLeft(id.getLeastSignificantBits(), 29) ^ region;
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdl;
        value ^= value >>> 33;
        value *= 0xc4ceb9fe1a85ec53l;
        return value ^ value >>> 33;
    }

    private static final class Bucket {
        private final Map<UUID, Long> selected = new HashMap<>();
        private long lastTouchedFrame;

        private boolean shouldSkip(UUID id, long region, int limit, long frame) {
            lastTouchedFrame = frame;
            selected.entrySet().removeIf(entry -> entry.getValue() < frame - 2L);
            while (selected.size() > limit) {
                UUID worst = null;
                long worstScore = 0;
                for (UUID candidate : selected.keySet()) {
                    long candidateScore = score(candidate, region);
                    if (worst == null || Long.compareUnsigned(candidateScore, worstScore) > 0) {
                        worst = candidate;
                        worstScore = candidateScore;
                    }
                }
                selected.remove(worst);
            }
            if (selected.containsKey(id)) {
                selected.put(id, frame);
                return false;
            }
            if (selected.size() >= limit) return true;
            selected.put(id, frame);
            return false;
        }
    }
}
