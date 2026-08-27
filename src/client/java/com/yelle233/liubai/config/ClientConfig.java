package com.yelle233.liubai.config;

import com.google.gson.GsonBuilder;
import com.yelle233.liubai.Liubai;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;

/** Lightweight Fabric client configuration persisted as JSON. */
public final class ClientConfig {
    public boolean enabled = true; public int targetFps = 60; public boolean adaptiveMode = true;
    public boolean entityCulling = true; public boolean blockEntityCulling = true; public boolean occlusionCulling = true;
    public OcclusionMode occlusionMode = OcclusionMode.AUTO; public double safeDistance = 8; public double occlusionMinDistance = 4;
    public int checksPerFrame = 32; public int visibilityBudgetMicros = 900; public int occludedConfirmations = 2; public int cacheTtlFrames = 30;
    public double minimumProjectedRadius = 1.25; public boolean screenSpaceLod = true; public boolean temporalLod = true; public int maxTemporalInterval = 8;
    public boolean denseVanillaEntities = true; public double denseEntityDistance = 32; public int denseEntityHighLimit = 24; public int denseEntityCriticalLimit = 12;
    public boolean reduceShadows = true; public double shadowDistance = 24; public boolean reduceNameTags = true; public double nameTagDistance = 48;
    public boolean reduceParticles = true; public double particleDistance = 4.0; public int particleSoftLimit = 4096; public int particleHighBudget = 256; public int particleCriticalBudget = 128;
    public boolean flywheelAdaptiveLimiter = true; public int flywheelHighMultiplier = 2; public int flywheelCriticalMultiplier = 3;
    public boolean showHud = false;
    public Set<String> entityAllowlist = new LinkedHashSet<>(Set.of("minecraft:player","minecraft:ender_dragon","minecraft:wither","minecraft:tnt","minecraft:leash_knot","minecraft:lightning_bolt"));
    public Set<String> blockEntityAllowlist = new LinkedHashSet<>(Set.of("minecraft:beacon","minecraft:end_gateway","minecraft:end_portal","minecraft:structure_block","minecraft:conduit"));
    public Set<String> disabledNamespaces = new LinkedHashSet<>(Set.of("create", "flywheel"));

    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("liubai-client.json");
    private static ClientConfig INSTANCE;
    public static synchronized void load() { if (INSTANCE != null) return; try (Reader r = Files.newBufferedReader(FILE)) { INSTANCE = new GsonBuilder().setPrettyPrinting().create().fromJson(r, ClientConfig.class); } catch (Exception ignored) { INSTANCE = new ClientConfig(); save(); } }
    public static synchronized void save() { if (INSTANCE == null) return; try { Files.createDirectories(FILE.getParent()); try (Writer w = Files.newBufferedWriter(FILE)) { new GsonBuilder().setPrettyPrinting().create().toJson(INSTANCE, w); } } catch (Exception e) { Liubai.LOGGER.warn("Could not save Liubai config", e); } }
    public static ClientConfig get() { load(); return INSTANCE; }
    private ClientConfig() {}
}
