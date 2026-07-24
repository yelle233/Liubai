package com.yelle233.liubai;

import com.mojang.logging.LogUtils;
import com.yelle233.liubai.client.LiubaiClientRuntime;
import com.yelle233.liubai.config.ClientConfig;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

/** Liubai's physical-client-only entry point. */
@Mod(value = Liubai.MODID, dist = Dist.CLIENT)
public final class Liubai {
    public static final String MODID = "liubai";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Liubai(IEventBus modEventBus, ModContainer container) {
        LiubaiClientRuntime.initialize(modEventBus, container);
        container.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
    }
}
