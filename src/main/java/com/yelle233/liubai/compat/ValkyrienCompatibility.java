package com.yelle233.liubai.compat;

import com.yelle233.liubai.Liubai;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.Collection;

/** Reflection-isolated bridge to the public Valkyrien Skies 2.4.x API for Minecraft 1.20.1. */
public final class ValkyrienCompatibility {
    public enum BridgeState {
        NOT_INSTALLED,
        AVAILABLE,
        DETECTED_BUT_UNAVAILABLE
    }

    private static MethodHandle entityLookup;
    private static MethodHandle blockLookup;
    private static MethodHandle shipWorldLookup;
    private static MethodHandle loadedShipsLookup;
    private static volatile BridgeState state = BridgeState.NOT_INSTALLED;
    private static volatile boolean activeShips;

    private ValkyrienCompatibility() {
    }

    public static void initialize(boolean installed) {
        activeShips = false;
        state = installed ? BridgeState.DETECTED_BUT_UNAVAILABLE : BridgeState.NOT_INSTALLED;
        if (!installed) return;

        try {
            Class<?> apiClass = Class.forName("org.valkyrienskies.mod.api.ValkyrienSkies");
            Class<?> shipClass = Class.forName("org.valkyrienskies.core.api.ships.Ship");
            Class<?> shipWorldClass = Class.forName("org.valkyrienskies.core.api.world.ShipWorld");
            Class<?> shipDataClass = Class.forName("org.valkyrienskies.core.api.ships.QueryableShipData");
            MethodHandles.Lookup lookup = MethodHandles.publicLookup();

            Method entityMethod = apiClass.getMethod("getShipManagingEntity", Entity.class);
            Method blockMethod = apiClass.getMethod("getShipManagingBlock", Level.class, BlockPos.class);
            Method worldMethod = apiClass.getMethod("getShipWorld", Level.class);
            Method loadedShipsMethod = shipWorldClass.getMethod("getLoadedShips");
            if (!shipClass.isAssignableFrom(entityMethod.getReturnType())
                    || !shipClass.isAssignableFrom(blockMethod.getReturnType())
                    || !shipWorldClass.isAssignableFrom(worldMethod.getReturnType())
                    || !shipDataClass.isAssignableFrom(loadedShipsMethod.getReturnType())
                    || !Collection.class.isAssignableFrom(shipDataClass)) {
                throw new NoSuchMethodException("Unexpected Valkyrien Skies API return type");
            }

            entityLookup = lookup.unreflect(entityMethod)
                    .asType(MethodType.methodType(Object.class, Entity.class));
            blockLookup = lookup.unreflect(blockMethod)
                    .asType(MethodType.methodType(Object.class, Level.class, BlockPos.class));
            shipWorldLookup = lookup.unreflect(worldMethod)
                    .asType(MethodType.methodType(Object.class, Level.class));
            loadedShipsLookup = lookup.unreflect(loadedShipsMethod)
                    .asType(MethodType.methodType(Object.class, Object.class));

            state = BridgeState.AVAILABLE;
            Liubai.LOGGER.info("Valkyrien Skies ship safety bridge initialized.");
        } catch (ReflectiveOperationException | LinkageError | RuntimeException error) {
            state = BridgeState.DETECTED_BUT_UNAVAILABLE;
            Liubai.LOGGER.warn("Valkyrien Skies was detected but its public API was not compatible; Liubai will conservatively bypass per-object policies for this session.", error);
        }
    }

    /** Refreshes the frame-level state used where a render hook does not expose an owning ship object. */
    public static void beginFrame(ClientLevel level) {
        if (state != BridgeState.AVAILABLE || level == null) {
            activeShips = false;
            return;
        }
        try {
            Object shipWorld = (Object) shipWorldLookup.invokeExact((Level) level);
            Object loadedShips = shipWorld == null ? null : (Object) loadedShipsLookup.invokeExact(shipWorld);
            activeShips = loadedShips instanceof Collection<?> collection && !collection.isEmpty();
        } catch (Throwable error) {
            disableAfterFailure(error);
        }
    }

    public static boolean contains(BlockEntity blockEntity) {
        if (state != BridgeState.AVAILABLE || blockEntity == null || blockEntity.getLevel() == null) return false;
        try {
            return (Object) blockLookup.invokeExact((Level) blockEntity.getLevel(), blockEntity.getBlockPos()) != null;
        } catch (Throwable error) {
            disableAfterFailure(error);
            return false;
        }
    }

    public static boolean contains(Entity entity) {
        if (state != BridgeState.AVAILABLE || entity == null || entity.level() == null) return false;
        try {
            if ((Object) entityLookup.invokeExact(entity) != null) return true;
            BlockPos position = entity.blockPosition();
            return (Object) blockLookup.invokeExact((Level) entity.level(), position) != null;
        } catch (Throwable error) {
            disableAfterFailure(error);
            return false;
        }
    }

    /** Particle and Flywheel hooks expose no reliable ship owner, so active ships enable a frame-wide safe mode. */
    public static boolean ownerlessSafeModeActive() {
        return state == BridgeState.DETECTED_BUT_UNAVAILABLE
                || state == BridgeState.AVAILABLE && activeShips;
    }

    public static boolean conservativeFallbackActive() {
        return state == BridgeState.DETECTED_BUT_UNAVAILABLE;
    }

    public static BridgeState state() {
        return state;
    }

    private static synchronized void disableAfterFailure(Throwable error) {
        if (state != BridgeState.AVAILABLE) return;
        state = BridgeState.DETECTED_BUT_UNAVAILABLE;
        activeShips = false;
        Liubai.LOGGER.warn("Valkyrien Skies lookup failed at runtime; Liubai will conservatively bypass per-object policies for this session.", error);
    }
}
