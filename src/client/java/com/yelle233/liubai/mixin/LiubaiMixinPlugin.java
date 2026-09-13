package com.yelle233.liubai.mixin;

import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/** Applies optional integrations only when their verified target versions are present. */
public final class LiubaiMixinPlugin implements IMixinConfigPlugin {
    private static final String FLYWHEEL_MIXIN = "com.yelle233.liubai.mixin.FlywheelBandedPrimeLimiterMixin";

    @Override public void onLoad(String mixinPackage) {
    }

    @Override public String getRefMapperConfig() {
        return null;
    }

    @Override public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (!FLYWHEEL_MIXIN.equals(mixinClassName)) return true;
        FabricLoader loader = FabricLoader.getInstance();
        boolean supportedCreate = loader.getModContainer("create")
                .map(container -> container.getMetadata().getVersion().getFriendlyString().startsWith("6.0.8"))
                .orElse(false);
        boolean supportedFlywheel = loader.getModContainer("flywheel")
                .map(container -> container.getMetadata().getVersion().getFriendlyString().startsWith("1.0.5"))
                .orElse(false);
        return supportedCreate && supportedFlywheel;
    }

    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override public List<String> getMixins() {
        return null;
    }

    @Override public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
