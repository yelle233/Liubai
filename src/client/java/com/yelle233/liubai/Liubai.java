package com.yelle233.liubai;

import com.yelle233.liubai.client.LiubaiClientRuntime;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Liubai implements ClientModInitializer {
    public static final String MODID = "liubai";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);
    @Override public void onInitializeClient() { LiubaiClientRuntime.initialize(); }
}
