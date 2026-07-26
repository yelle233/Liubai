package com.yelle233.liubai.compat.sable;

import com.yelle233.liubai.Liubai;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Position;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Optional, reflection-isolated bridge to Sable's public sublevel lookup API.
 * All reflective discovery happens once; render hot paths use cached method handles.
 */
public final class SableCompatibility {
    public enum BridgeState {
        NOT_INSTALLED,
        AVAILABLE,
        DETECTED_BUT_UNAVAILABLE
    }

    private static MethodHandle blockEntityLookup;
    private static MethodHandle entityLookup;
    private static MethodHandle trackingEntityLookup;
    private static MethodHandle positionLookup;
    private static MethodHandle clientContainerLookup;
    private static MethodHandle loadedCountLookup;
    private static volatile BridgeState state = BridgeState.NOT_INSTALLED;
    private static volatile boolean aeronauticsLoaded;
    private static volatile boolean activeSubLevels;

    private SableCompatibility() {
    }

    public static void initialize(boolean sableLoaded, boolean hasAeronautics) {
        aeronauticsLoaded = hasAeronautics;
        activeSubLevels = false;
        state = sableLoaded ? BridgeState.DETECTED_BUT_UNAVAILABLE : BridgeState.NOT_INSTALLED;
        if (!sableLoaded) return;

        try {
            Class<?> sableClass = Class.forName("dev.ryanhcode.sable.Sable");
            Class<?> helperClass = Class.forName("dev.ryanhcode.sable.ActiveSableCompanion");
            Class<?> subLevelClass = Class.forName("dev.ryanhcode.sable.sublevel.SubLevel");
            Class<?> containerClass = Class.forName("dev.ryanhcode.sable.api.sublevel.SubLevelContainer");
            Class<?> clientContainerClass = Class.forName("dev.ryanhcode.sable.api.sublevel.ClientSubLevelContainer");

            Field helperField = sableClass.getField("HELPER");
            Object helper = helperField.get(null);
            MethodHandles.Lookup lookup = MethodHandles.publicLookup();

            blockEntityLookup = bind(lookup, helper,
                    helperClass.getMethod("getContaining", BlockEntity.class),
                    MethodType.methodType(Object.class, BlockEntity.class));
            entityLookup = bind(lookup, helper,
                    helperClass.getMethod("getContaining", Entity.class),
                    MethodType.methodType(Object.class, Entity.class));
            trackingEntityLookup = bind(lookup, helper,
                    helperClass.getMethod("getTrackingOrVehicleSubLevel", Entity.class),
                    MethodType.methodType(Object.class, Entity.class));
            positionLookup = bind(lookup, helper,
                    helperClass.getMethod("getContaining", Level.class, Position.class),
                    MethodType.methodType(Object.class, Level.class, Position.class));

            Method getContainer = containerClass.getMethod("getContainer", ClientLevel.class);
            clientContainerLookup = lookup.unreflect(getContainer)
                    .asType(MethodType.methodType(Object.class, ClientLevel.class));
            loadedCountLookup = lookup.findVirtual(containerClass, "getLoadedCount", MethodType.methodType(int.class))
                    .asType(MethodType.methodType(int.class, Object.class));

            // Force return-type resolution during initialization so incompatible Sable versions fail closed here.
            if (!subLevelClass.isAssignableFrom(blockEntityLookup.type().returnType())
                    && blockEntityLookup.type().returnType() != Object.class) {
                throw new NoSuchMethodException("Unexpected Sable sublevel return type");
            }
            if (!containerClass.isAssignableFrom(clientContainerClass)) {
                throw new NoSuchMethodException("Unexpected Sable client container type");
            }
            state = BridgeState.AVAILABLE;
            Liubai.LOGGER.info("Sable dynamic-sublevel compatibility bridge initialized.");
        } catch (ReflectiveOperationException | RuntimeException error) {
            state = BridgeState.DETECTED_BUT_UNAVAILABLE;
            Liubai.LOGGER.warn("Sable was detected but its sublevel API was not compatible; Liubai will conservatively bypass per-object policies for this session.", error);
        }
    }

    private static MethodHandle bind(MethodHandles.Lookup lookup, Object receiver, Method method, MethodType exposedType)
            throws IllegalAccessException {
        return lookup.unreflect(method).bindTo(receiver).asType(exposedType);
    }

    /** Refreshes the only frame-level query used by the Flywheel safety fallback. */
    public static void beginFrame(ClientLevel level) {
        if (state != BridgeState.AVAILABLE || level == null) {
            activeSubLevels = false;
            return;
        }
        try {
            Object container = (Object) clientContainerLookup.invokeExact(level);
            activeSubLevels = container != null && (int) loadedCountLookup.invokeExact(container) > 0;
        } catch (Throwable error) {
            disableAfterFailure(error);
        }
    }

    public static boolean contains(BlockEntity blockEntity) {
        if (state != BridgeState.AVAILABLE || blockEntity == null || blockEntity.getLevel() == null) return false;
        try {
            return (Object) blockEntityLookup.invokeExact(blockEntity) != null;
        } catch (Throwable error) {
            disableAfterFailure(error);
            return false;
        }
    }

    public static boolean contains(Entity entity) {
        if (state != BridgeState.AVAILABLE || entity == null || entity.level() == null) return false;
        try {
            Object tracked = (Object) trackingEntityLookup.invokeExact(entity);
            return tracked != null || (Object) entityLookup.invokeExact(entity) != null;
        } catch (Throwable error) {
            disableAfterFailure(error);
            return false;
        }
    }

    public static boolean contains(Level level, Position position) {
        if (state != BridgeState.AVAILABLE || level == null || position == null) return false;
        try {
            return (Object) positionLookup.invokeExact(level, position) != null;
        } catch (Throwable error) {
            disableAfterFailure(error);
            return false;
        }
    }

    /**
     * BandedPrimeLimiter exposes only a distance, not its Visual owner. While an Aeronautics sublevel exists,
     * the safe choice is to leave Flywheel's original divisor untouched for every Visual.
     */
    public static boolean flywheelSafeModeActive() {
        return aeronauticsLoaded && (state == BridgeState.DETECTED_BUT_UNAVAILABLE
                || state == BridgeState.AVAILABLE && activeSubLevels);
    }

    public static boolean available() {
        return state == BridgeState.AVAILABLE;
    }

    /** A detected but unusable bridge cannot safely distinguish main-world objects from Plot-local objects. */
    public static boolean conservativeFallbackActive() {
        return state == BridgeState.DETECTED_BUT_UNAVAILABLE;
    }

    public static BridgeState state() {
        return state;
    }

    private static synchronized void disableAfterFailure(Throwable error) {
        if (state != BridgeState.AVAILABLE) return;
        state = BridgeState.DETECTED_BUT_UNAVAILABLE;
        activeSubLevels = false;
        Liubai.LOGGER.warn("Sable sublevel lookup failed at runtime; Liubai will conservatively bypass per-object policies for this session.", error);
    }
}
