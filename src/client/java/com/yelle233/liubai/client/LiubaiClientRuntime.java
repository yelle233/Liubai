package com.yelle233.liubai.client;

import com.yelle233.liubai.Liubai;
import com.yelle233.liubai.compat.CompatibilityManager;
import com.yelle233.liubai.config.ClientConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import org.lwjgl.glfw.GLFW;

public final class LiubaiClientRuntime {
    private LiubaiClientRuntime() {}
    public static void initialize() {
        ClientConfig.load();
        CompatibilityManager.INSTANCE.detect();
        LiubaiClientSystem.INSTANCE.refreshConfig();
        KeyBindingHelper.registerKeyBinding(LiubaiKeyMappings.OPEN_CONFIG);
        ClientTickEvents.END_CLIENT_TICK.register(ClientEvents::onClientTick);
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> ClientEvents.onHud(drawContext));
        Liubai.LOGGER.info("Liubai Fabric adaptive render budget initialized.");
    }
}
