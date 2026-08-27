package com.yelle233.liubai.client;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;
public final class LiubaiKeyMappings {
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath("liubai", "general"));
    public static final KeyMapping OPEN_CONFIG = new KeyMapping(
            "key.liubai.open_config", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_L, CATEGORY);
    private LiubaiKeyMappings() {}
}
