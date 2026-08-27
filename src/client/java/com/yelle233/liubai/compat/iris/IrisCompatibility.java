package com.yelle233.liubai.compat.iris;

import com.yelle233.liubai.Liubai;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/** Optional bridge to Iris' public API. Unknown or failed APIs choose visual correctness over culling. */
public final class IrisCompatibility {
    private static MethodHandle instanceLookup;
    private static MethodHandle shadowPassLookup;
    private static volatile boolean installed;
    private static volatile boolean available;

    private IrisCompatibility() {
    }

    public static void initialize(boolean irisLoaded) {
        installed = irisLoaded;
        available = false;
        if (!irisLoaded) return;
        try {
            Class<?> apiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            MethodHandles.Lookup lookup = MethodHandles.publicLookup();
            instanceLookup = lookup.findStatic(apiClass, "getInstance", MethodType.methodType(apiClass))
                    .asType(MethodType.methodType(Object.class));
            shadowPassLookup = lookup.findVirtual(apiClass, "isRenderingShadowPass", MethodType.methodType(boolean.class))
                    .asType(MethodType.methodType(boolean.class, Object.class));
            available = true;
            Liubai.LOGGER.info("Iris render-pass compatibility bridge initialized.");
        } catch (ReflectiveOperationException | RuntimeException error) {
            Liubai.LOGGER.warn("Iris was detected but its render-pass API was unavailable; Liubai will conservatively bypass per-object policies while Iris is present.", error);
        }
    }

    public static boolean shouldBypassPerObjectPolicies() {
        if (!installed) return false;
        if (!available) return true;
        try {
            Object api = (Object) instanceLookup.invokeExact();
            return api != null && (boolean) shadowPassLookup.invokeExact(api);
        } catch (Throwable error) {
            disableAfterFailure(error);
            return true;
        }
    }

    public static boolean available() {
        return available;
    }

    private static synchronized void disableAfterFailure(Throwable error) {
        if (!available) return;
        available = false;
        Liubai.LOGGER.warn("Iris render-pass lookup failed at runtime; Liubai will conservatively bypass per-object policies for this session.", error);
    }
}
