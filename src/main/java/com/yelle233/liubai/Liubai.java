package com.yelle233.liubai;

import com.mojang.logging.LogUtils;
import com.yelle233.liubai.client.LiubaiClientRuntime;
import com.yelle233.liubai.config.ClientConfig;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/** Liubai's physical-client-only entry point. */
@Mod(Liubai.MODID)
public final class Liubai {
    public static final String MODID = "liubai";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Liubai() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        LiubaiClientRuntime.initialize(modEventBus);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
    }
}
