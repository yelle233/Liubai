package com.yelle233.liubai.client;

import com.yelle233.liubai.Liubai;
import com.yelle233.liubai.compat.CompatibilityManager;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public final class LiubaiClientRuntime {
    private LiubaiClientRuntime() {
    }

    public static void initialize(IEventBus modEventBus) {
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, parent) -> new LiubaiConfigScreen(parent)));
        MinecraftForge.EVENT_BUS.register(new ClientEvents());
        modEventBus.addListener(LiubaiClientRuntime::clientSetup);
        modEventBus.addListener(LiubaiClientRuntime::configChanged);
    }

    private static void clientSetup(FMLClientSetupEvent event) {
        CompatibilityManager.INSTANCE.detect();
        Liubai.LOGGER.info("Liubai adaptive render budget initialized.");
    }

    private static void configChanged(ModConfigEvent event) {
        if (!event.getConfig().getModId().equals(Liubai.MODID)) return;
        if (event instanceof ModConfigEvent.Loading || event instanceof ModConfigEvent.Reloading) {
            LiubaiClientSystem.INSTANCE.refreshConfig();
        } else if (event instanceof ModConfigEvent.Unloading) {
            LiubaiClientSystem.INSTANCE.unloadConfig();
        }
    }
}
