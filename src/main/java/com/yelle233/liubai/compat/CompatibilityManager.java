package com.yelle233.liubai.compat;

import com.yelle233.liubai.Liubai;
import com.yelle233.liubai.config.ConfigSnapshot;
import com.yelle233.liubai.config.OcclusionMode;
import com.yelle233.liubai.visibility.EffectiveVisibilityBackend;
import net.neoforged.fml.ModList;

public final class CompatibilityManager {
    public static final CompatibilityManager INSTANCE = new CompatibilityManager();

    private boolean detected;
    private boolean entityCullingLoaded;
    private boolean createLoaded;
    private boolean flywheelLoaded;
    private String entityCullingVersion = "";
    private String createVersion = "";
    private String flywheelVersion = "";

    private CompatibilityManager() {
    }

    public void detect() {
        ModList mods = ModList.get();
        entityCullingLoaded = mods.isLoaded("entityculling");
        createLoaded = mods.isLoaded("create");
        flywheelLoaded = mods.isLoaded("flywheel");
        entityCullingVersion = versionOf("entityculling");
        createVersion = versionOf("create");
        flywheelVersion = versionOf("flywheel");
        detected = true;

        Liubai.LOGGER.info("Liubai compatibility: Entity Culling {} {}, Create {} {}, Flywheel {} {}",
                entityCullingLoaded, entityCullingVersion, createLoaded, createVersion, flywheelLoaded, flywheelVersion);
        if (entityCullingLoaded) {
            Liubai.LOGGER.info("Entity Culling detected: AUTO mode delegates generic occlusion and disables Liubai's DDA queue.");
        }
        if (flywheelLoaded && !supportsAdaptiveFlywheelLimiter()) {
            Liubai.LOGGER.warn("Flywheel {} is not supported by Liubai's adaptive limiter; the integration will safely remain inactive.", flywheelVersion);
        } else if (supportsAdaptiveFlywheelLimiter()) {
            Liubai.LOGGER.info("Flywheel adaptive limiter enabled for the supported Create 6.0.10 / Flywheel 1.0.6 line.");
        }
    }

    private String versionOf(String modId) {
        return ModList.get().getModContainerById(modId)
                .map(container -> container.getModInfo().getVersion().toString())
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

    public boolean supportsAdaptiveFlywheelLimiter() {
        return detected && createLoaded && flywheelLoaded
                && createVersion.startsWith("6.0.10") && flywheelVersion.startsWith("1.0.6");
    }

    public boolean entityCullingLoaded() { return entityCullingLoaded; }
    public boolean createLoaded() { return createLoaded; }
    public boolean flywheelLoaded() { return flywheelLoaded; }
    public String entityCullingVersion() { return entityCullingVersion; }
    public String createVersion() { return createVersion; }
    public String flywheelVersion() { return flywheelVersion; }
}
