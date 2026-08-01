package com.yelle233.liubai.config;

import net.minecraft.network.chat.Component;

public enum OcclusionMode {
    AUTO,
    BUILTIN,
    EXTERNAL,
    OFF;

    public Component getTranslatedName() {
        return Component.translatable("liubai.configuration.visibility.occlusionMode." + name().toLowerCase(java.util.Locale.ROOT));
    }
}
