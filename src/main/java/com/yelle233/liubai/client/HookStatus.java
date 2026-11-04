package com.yelle233.liubai.client;

/** Records whether optional Mixin hooks have actually been reached in this client session. */
public final class HookStatus {
    private static boolean entityObserved;
    private static boolean blockEntityObserved;
    private static boolean particleObserved;

    private HookStatus() {
    }

    public static void entityObserved() { entityObserved = true; }
    public static void blockEntityObserved() { blockEntityObserved = true; }
    public static void particleObserved() { particleObserved = true; }

    public static Snapshot snapshot() {
        return new Snapshot(entityObserved, blockEntityObserved, particleObserved);
    }

    public record Snapshot(boolean entity, boolean blockEntity, boolean particle) {
    }
}
