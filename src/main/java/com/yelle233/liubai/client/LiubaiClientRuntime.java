package com.yelle233.liubai.client;

import com.yelle233.liubai.Liubai;
import com.yelle233.liubai.compat.CompatibilityManager;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

public final class LiubaiClientRuntime {
    private LiubaiClientRuntime() {
    }

    public static void initialize(IEventBus modEventBus, ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        NeoForge.EVENT_BUS.register(new ClientEvents(container));
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
