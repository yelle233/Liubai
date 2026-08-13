package com.yelle233.liubai.client;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;
public final class LiubaiKeyMappings {
    public static final KeyMapping OPEN_CONFIG = new KeyMapping("key.liubai.open_config", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_L, "key.categories.liubai");
    private LiubaiKeyMappings() {}
}
