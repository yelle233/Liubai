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
    private boolean createLoaded;
    private boolean flywheelLoaded;
    private boolean valkyrienskiesLoaded;
    private boolean irisLoaded;
    private String entityCullingVersion = "";
    private String createVersion = "";
    private String flywheelVersion = "";
    private String valkyrienskiesVersion = "";

    private CompatibilityManager() {
    }

    public void detect() {
        FabricLoader mods = FabricLoader.getInstance();
        entityCullingLoaded = mods.isModLoaded("entityculling");
        createLoaded = mods.isModLoaded("create");
        flywheelLoaded = mods.isModLoaded("flywheel");
        valkyrienskiesLoaded = mods.isModLoaded("valkyrienskies");
        irisLoaded = mods.isModLoaded("iris");
        entityCullingVersion = versionOf("entityculling");
        createVersion = versionOf("create");
        flywheelVersion = versionOf("flywheel");
        valkyrienskiesVersion = versionOf("valkyrienskies");
        detected = true;

        ValkyrienCompatibility.initialize(valkyrienskiesLoaded);
        IrisCompatibility.initialize(irisLoaded);

        Liubai.LOGGER.info("Liubai Fabric compatibility: Entity Culling {} {}, Iris {}, Create {} {}, Flywheel {} {}, Valkyrien Skies {} {}",
                entityCullingLoaded, entityCullingVersion, irisLoaded, createLoaded, createVersion,
                flywheelLoaded, flywheelVersion, valkyrienskiesLoaded, valkyrienskiesVersion);
        if (entityCullingLoaded) {
            Liubai.LOGGER.info("Entity Culling detected: AUTO mode delegates generic occlusion and disables Liubai's DDA queue.");
        }
        if (createLoaded && !supportsAdaptiveFlywheelLimiter()) {
            Liubai.LOGGER.warn("Create {} / Flywheel {} is outside the verified Create Fabric 6.0.8 / Flywheel 1.0.5 line; adaptive Flywheel limiting remains inactive.",
                    createVersion, flywheelVersion);
        } else if (supportsAdaptiveFlywheelLimiter()) {
            Liubai.LOGGER.info("Adaptive Flywheel limiter enabled for Create Fabric {} / Flywheel {}.", createVersion, flywheelVersion);
        }
        if (valkyrienskiesLoaded) {
            Liubai.LOGGER.info("Valkyrien Skies {} detected. Ship-managed objects will bypass Liubai's main-world per-object policies.", valkyrienskiesVersion);
        }
    }

    private String versionOf(String modId) {
        return FabricLoader.getInstance().getModContainer(modId)
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("");
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
    public boolean supportsAdaptiveFlywheelLimiter() {
        return detected && createLoaded && flywheelLoaded
                && createVersion.startsWith("6.0.8") && flywheelVersion.startsWith("1.0.5");
    }
    public boolean createLoaded() { return createLoaded; }
    public String createVersion() { return createVersion; }
    public boolean flywheelLoaded() { return flywheelLoaded; }
    public String flywheelVersion() { return flywheelVersion; }
    public boolean valkyrienskiesLoaded() { return valkyrienskiesLoaded; }
    public String valkyrienskiesVersion() { return valkyrienskiesVersion; }
    public boolean irisLoaded() { return irisLoaded; }
}
