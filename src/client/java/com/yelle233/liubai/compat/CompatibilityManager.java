package com.yelle233.liubai.compat;

import com.yelle233.liubai.Liubai;
import com.yelle233.liubai.config.ConfigSnapshot;
import com.yelle233.liubai.config.OcclusionMode;
import com.yelle233.liubai.compat.iris.IrisCompatibility;
import com.yelle233.liubai.visibility.EffectiveVisibilityBackend;
import net.fabricmc.loader.api.FabricLoader;

public final class CompatibilityManager {
    public static final CompatibilityManager INSTANCE = new CompatibilityManager();

    private boolean detected;
    private boolean entityCullingLoaded;
    private boolean valkyrienskiesLoaded;
    private boolean irisLoaded;
    private boolean createLoaded;
    private boolean createFlyLoaded;
    private String entityCullingVersion = "";
    private String valkyrienskiesVersion = "";
    private String createVersion = "";
    private String createName = "";
    private boolean createFlyLimiterSignature;

    private CompatibilityManager() {
    }

    public void detect() {
        FabricLoader mods = FabricLoader.getInstance();
        entityCullingLoaded = mods.isModLoaded("entityculling");
        valkyrienskiesLoaded = mods.isModLoaded("valkyrienskies");
        irisLoaded = mods.isModLoaded("iris");
        createLoaded = mods.isModLoaded("create");
        entityCullingVersion = versionOf("entityculling");
        valkyrienskiesVersion = versionOf("valkyrienskies");
        createVersion = versionOf("create");
        createName = nameOf("create");
        createFlyLoaded = createLoaded && "Create Fly".equals(createName);
        createFlyLimiterSignature = createFlyLoaded && hasCreateFlyLimiterSignature();
        detected = true;

        ValkyrienCompatibility.initialize(valkyrienskiesLoaded);
        IrisCompatibility.initialize(irisLoaded);

        Liubai.LOGGER.info("Liubai Fabric compatibility: Entity Culling {} {}, Iris {}, Valkyrien Skies {} {}, Create {} {} ({})",
                entityCullingLoaded, entityCullingVersion, irisLoaded,
                valkyrienskiesLoaded, valkyrienskiesVersion, createLoaded, createVersion, createName);
        if (entityCullingLoaded) {
            Liubai.LOGGER.info("Entity Culling detected: AUTO mode delegates generic occlusion and disables Liubai's DDA queue.");
        }
        if (valkyrienskiesLoaded) {
            Liubai.LOGGER.info("Valkyrien Skies {} detected. Ship-managed objects will bypass Liubai's main-world per-object policies.", valkyrienskiesVersion);
        }
        if (createLoaded && !supportsAdaptiveFlywheelLimiter()) {
            Liubai.LOGGER.warn("Create implementation '{}' {} is not the verified Create Fly 6.0.9-5 limiter target; the adaptive Flywheel integration will safely remain inactive.",
                    createName, createVersion);
        } else if (supportsAdaptiveFlywheelLimiter()) {
            Liubai.LOGGER.info("Create Fly {} adaptive Flywheel limiter integration enabled.", createVersion);
        }
    }

    private String versionOf(String modId) {
        return FabricLoader.getInstance().getModContainer(modId)
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("");
    }

    private String nameOf(String modId) {
        return FabricLoader.getInstance().getModContainer(modId)
                .map(container -> container.getMetadata().getName())
                .orElse("");
    }

    private boolean hasCreateFlyLimiterSignature() {
        try {
            Class<?> limiter = Class.forName(
                    "com.zurrtum.create.client.flywheel.impl.visual.BandedPrimeLimiter", false,
                    CompatibilityManager.class.getClassLoader());
            return limiter.getDeclaredMethod("getUpdateDivisor", double.class).getReturnType() == int.class;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException error) {
            Liubai.LOGGER.warn("Create Fly was detected, but its verified Flywheel limiter signature is unavailable.", error);
            return false;
        }
    }

    public EffectiveVisibilityBackend resolveBackend(ConfigSnapshot config) {
        if (!config.enabled() || !config.occlusionCulling() || config.occlusionMode() == OcclusionMode.OFF) {
            return EffectiveVisibilityBackend.DISABLED;
        }
        return switch (config.occlusionMode()) {
            case AUTO -> entityCullingLoaded ? EffectiveVisibilityBackend.ENTITY_CULLING : EffectiveVisibilityBackend.BUILTIN;
            case BUILTIN -> EffectiveVisibilityBackend.BUILTIN;
            case EXTERNAL -> entityCullingLoaded ? EffectiveVisibilityBackend.ENTITY_CULLING : EffectiveVisibilityBackend.EXTERNAL;
            case OFF -> EffectiveVisibilityBackend.DISABLED;
        };
    }

    public boolean entityCullingLoaded() { return entityCullingLoaded; }
    public String entityCullingVersion() { return entityCullingVersion; }
    public boolean valkyrienskiesLoaded() { return valkyrienskiesLoaded; }
    public String valkyrienskiesVersion() { return valkyrienskiesVersion; }
    public boolean irisLoaded() { return irisLoaded; }
    public boolean createLoaded() { return createLoaded; }
    public boolean createFlyLoaded() { return createFlyLoaded; }
    public String createVersion() { return createVersion; }
    public boolean supportsAdaptiveFlywheelLimiter() {
        return detected && createFlyLoaded && "6.0.9-5".equals(createVersion) && createFlyLimiterSignature;
    }
}
