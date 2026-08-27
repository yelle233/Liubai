package com.yelle233.liubai.mixin;

import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/** Prevents optional integration targets from being queried unless their exact verified build is present. */
public final class LiubaiMixinPlugin implements IMixinConfigPlugin {
    private static final String CREATE_FLY_MIXIN = "com.yelle233.liubai.mixin.FlywheelBandedPrimeLimiterMixin";

    @Override public void onLoad(String mixinPackage) {
    }

    @Override public String getRefMapperConfig() {
        return null;
    }

    @Override public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (!CREATE_FLY_MIXIN.equals(mixinClassName)) return true;
        return FabricLoader.getInstance().getModContainer("create")
                .filter(container -> "Create Fly".equals(container.getMetadata().getName()))
                .filter(container -> "6.0.9-5".equals(container.getMetadata().getVersion().getFriendlyString()))
                .isPresent();
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
