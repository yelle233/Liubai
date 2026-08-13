package com.yelle233.liubai.compat;

import com.yelle233.liubai.Liubai;
import com.yelle233.liubai.config.ConfigSnapshot;
import com.yelle233.liubai.config.OcclusionMode;
import com.yelle233.liubai.compat.iris.IrisCompatibility;
import com.yelle233.liubai.compat.sable.SableCompatibility;
import com.yelle233.liubai.visibility.EffectiveVisibilityBackend;
import net.fabricmc.loader.api.FabricLoader;

public final class CompatibilityManager {
    public static final CompatibilityManager INSTANCE = new CompatibilityManager();

    private boolean detected;
    private boolean entityCullingLoaded;
    private boolean sableLoaded;
    private boolean irisLoaded;
    private String entityCullingVersion = "";
    private String sableVersion = "";

    private CompatibilityManager() {
    }

    public void detect() {
        FabricLoader mods = FabricLoader.getInstance();
        entityCullingLoaded = mods.isModLoaded("entityculling");
        sableLoaded = mods.isModLoaded("sable");
        irisLoaded = mods.isModLoaded("iris");
        entityCullingVersion = versionOf("entityculling");
        sableVersion = versionOf("sable");
        detected = true;

        SableCompatibility.initialize(sableLoaded);
        IrisCompatibility.initialize(irisLoaded);

        Liubai.LOGGER.info("Liubai Fabric compatibility: Entity Culling {} {}, Iris {}, Sable {} {}",
                entityCullingLoaded, entityCullingVersion, irisLoaded, sableLoaded, sableVersion);
        if (entityCullingLoaded) {
            Liubai.LOGGER.info("Entity Culling detected: AUTO mode delegates generic occlusion and disables Liubai's DDA queue.");
        }
        if (sableLoaded) {
            Liubai.LOGGER.info("Sable {} detected. Dynamic sublevels will bypass Liubai's main-world per-object policies.", sableVersion);
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
    public boolean sableLoaded() { return sableLoaded; }
    public String sableVersion() { return sableVersion; }
    public boolean irisLoaded() { return irisLoaded; }
}
