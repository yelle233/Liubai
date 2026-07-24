package com.yelle233.liubai.config;

import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.TranslatableEnum;

public enum OcclusionMode implements TranslatableEnum {
    AUTO,
    BUILTIN,
    EXTERNAL,
    OFF;

    @Override
    public Component getTranslatedName() {
        return Component.translatable("liubai.configuration.visibility.occlusionMode." + name().toLowerCase(java.util.Locale.ROOT));
    }
}
