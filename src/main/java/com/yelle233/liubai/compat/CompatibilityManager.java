package com.yelle233.liubai.compat;

import com.yelle233.liubai.Liubai;
import com.yelle233.liubai.config.ConfigSnapshot;
import com.yelle233.liubai.config.OcclusionMode;
import com.yelle233.liubai.compat.iris.IrisCompatibility;
import com.yelle233.liubai.visibility.EffectiveVisibilityBackend;
import net.neoforged.fml.ModList;

public final class CompatibilityManager {
    public static final CompatibilityManager INSTANCE = new CompatibilityManager();

    private boolean entityCullingLoaded;
    private boolean irisLoaded;
    private String entityCullingVersion = "";

    private CompatibilityManager() {
    }

    public void detect() {
        ModList mods = ModList.get();
        entityCullingLoaded = mods.isLoaded("entityculling");
        irisLoaded = mods.isLoaded("iris");
        entityCullingVersion = versionOf("entityculling");
        IrisCompatibility.initialize(irisLoaded);

        Liubai.LOGGER.info("Liubai compatibility: Entity Culling {} {}, Iris {}",
                entityCullingLoaded, entityCullingVersion, irisLoaded);
        if (entityCullingLoaded) {
            Liubai.LOGGER.info("Entity Culling detected: AUTO mode delegates generic occlusion and disables Liubai's DDA queue.");
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

    public boolean entityCullingLoaded() { return entityCullingLoaded; }
    public String entityCullingVersion() { return entityCullingVersion; }
    public boolean irisLoaded() { return irisLoaded; }
}
