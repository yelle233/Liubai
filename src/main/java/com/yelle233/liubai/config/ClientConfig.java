package com.yelle233.liubai.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

/** Client configuration. Values are deliberately conservative by default. */
public final class ClientConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.BooleanValue ENABLED;
    public static final ForgeConfigSpec.IntValue TARGET_FPS;
    public static final ForgeConfigSpec.BooleanValue ADAPTIVE_MODE;

    public static final ForgeConfigSpec.BooleanValue ENTITY_CULLING;
    public static final ForgeConfigSpec.BooleanValue BLOCK_ENTITY_CULLING;
    public static final ForgeConfigSpec.BooleanValue OCCLUSION_CULLING;
    public static final ForgeConfigSpec.EnumValue<OcclusionMode> OCCLUSION_MODE;
    public static final ForgeConfigSpec.DoubleValue SAFE_DISTANCE;
    public static final ForgeConfigSpec.DoubleValue OCCLUSION_MIN_DISTANCE;
    public static final ForgeConfigSpec.IntValue OCCLUSION_CHECKS_PER_FRAME;
    public static final ForgeConfigSpec.IntValue OCCLUSION_BUDGET_MICROS;
    public static final ForgeConfigSpec.IntValue OCCLUDED_CONFIRMATIONS;
    public static final ForgeConfigSpec.IntValue CACHE_TTL_FRAMES;
    public static final ForgeConfigSpec.DoubleValue MIN_PROJECTED_RADIUS;
    public static final ForgeConfigSpec.BooleanValue SCREEN_SPACE_LOD;
    public static final ForgeConfigSpec.BooleanValue TEMPORAL_LOD;
    public static final ForgeConfigSpec.IntValue MAX_TEMPORAL_INTERVAL;
    public static final ForgeConfigSpec.BooleanValue DENSE_VANILLA_ENTITIES;
    public static final ForgeConfigSpec.DoubleValue DENSE_ENTITY_DISTANCE;
    public static final ForgeConfigSpec.IntValue DENSE_ENTITY_HIGH_LIMIT;
    public static final ForgeConfigSpec.IntValue DENSE_ENTITY_CRITICAL_LIMIT;

    public static final ForgeConfigSpec.BooleanValue REDUCE_SHADOWS;
    public static final ForgeConfigSpec.DoubleValue SHADOW_DISTANCE;
    public static final ForgeConfigSpec.BooleanValue REDUCE_NAME_TAGS;
    public static final ForgeConfigSpec.DoubleValue NAME_TAG_DISTANCE;
    public static final ForgeConfigSpec.BooleanValue REDUCE_PARTICLES;
    public static final ForgeConfigSpec.DoubleValue PARTICLE_DISTANCE;
    public static final ForgeConfigSpec.IntValue PARTICLE_SOFT_LIMIT;
    public static final ForgeConfigSpec.IntValue PARTICLE_HIGH_BUDGET;
    public static final ForgeConfigSpec.IntValue PARTICLE_CRITICAL_BUDGET;
    public static final ForgeConfigSpec.BooleanValue FLYWHEEL_ADAPTIVE_LIMITER;
    public static final ForgeConfigSpec.IntValue FLYWHEEL_HIGH_MULTIPLIER;
    public static final ForgeConfigSpec.IntValue FLYWHEEL_CRITICAL_MULTIPLIER;

    public static final ForgeConfigSpec.BooleanValue SHOW_HUD;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> ENTITY_ALLOWLIST;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> BLOCK_ENTITY_ALLOWLIST;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> DISABLED_NAMESPACES;

    public static final ForgeConfigSpec SPEC;

    static {
        BUILDER.translation("liubai.configuration.general").push("general");
        ENABLED = BUILDER.comment("Master switch. When false, render hooks take the vanilla path.")
                .translation("liubai.configuration.general.enabled")
                .define("enabled", true);
        TARGET_FPS = BUILDER.comment("Target used by the adaptive frame budget controller.")
                .translation("liubai.configuration.general.targetFps")
                .defineInRange("targetFps", 60, 30, 240);
        ADAPTIVE_MODE = BUILDER.comment("Increase optimization pressure when render time stays over budget.")
                .translation("liubai.configuration.general.adaptiveMode")
                .define("adaptiveMode", true);
        BUILDER.pop();

        BUILDER.translation("liubai.configuration.culling").push("culling");
        ENTITY_CULLING = BUILDER.translation("liubai.configuration.culling.entities").define("entities", true);
        BLOCK_ENTITY_CULLING = BUILDER.translation("liubai.configuration.culling.blockEntities").define("blockEntities", true);
        OCCLUSION_CULLING = BUILDER.comment("Cull objects only after repeated full-solid-block occlusion checks.")
                .translation("liubai.configuration.culling.occlusion")
                .define("occlusion", true);
        OCCLUSION_MODE = BUILDER.comment("AUTO delegates generic occlusion to Entity Culling when it is installed.")
                .translation("liubai.configuration.culling.occlusionMode")
                .defineEnum("occlusionMode", OcclusionMode.AUTO);
        SAFE_DISTANCE = BUILDER.translation("liubai.configuration.culling.safeDistance").defineInRange("safeDistance", 8.0, 0.0, 64.0);
        OCCLUSION_MIN_DISTANCE = BUILDER.translation("liubai.configuration.culling.occlusionMinDistance").defineInRange("occlusionMinDistance", 4.0, 4.0, 128.0);
        OCCLUSION_CHECKS_PER_FRAME = BUILDER.comment("Hard cap for world-reading visibility checks per frame.")
                .translation("liubai.configuration.culling.checksPerFrame")
                .defineInRange("checksPerFrame", 32, 0, 512);
        OCCLUSION_BUDGET_MICROS = BUILDER.comment("Client-thread time cap for built-in visibility checks per frame.")
                .translation("liubai.configuration.culling.budgetMicros")
                .defineInRange("budgetMicros", 900, 100, 10000);
        OCCLUDED_CONFIRMATIONS = BUILDER.translation("liubai.configuration.culling.occludedConfirmations").defineInRange("occludedConfirmations", 2, 1, 8);
        CACHE_TTL_FRAMES = BUILDER.translation("liubai.configuration.culling.cacheTtlFrames").defineInRange("cacheTtlFrames", 30, 2, 600);
        MIN_PROJECTED_RADIUS = BUILDER.comment("Under load, low-priority objects smaller than this many pixels may be skipped. Set 0 to disable.")
                .translation("liubai.configuration.culling.minimumProjectedRadius")
                .defineInRange("minimumProjectedRadius", 1.25, 0.0, 16.0);
        BUILDER.pop();

        BUILDER.translation("liubai.configuration.lod").push("lod");
        SCREEN_SPACE_LOD = BUILDER.translation("liubai.configuration.lod.screenSpace").define("screenSpace", true);
        TEMPORAL_LOD = BUILDER.translation("liubai.configuration.lod.temporal").define("temporal", true);
        MAX_TEMPORAL_INTERVAL = BUILDER.translation("liubai.configuration.lod.maxTemporalInterval")
                .defineInRange("maxTemporalInterval", 8, 1, 31);
        DENSE_VANILLA_ENTITIES = BUILDER.comment("Apply a stable per-chunk render cap only to distant vanilla items and experience orbs under pressure.")
                .translation("liubai.configuration.lod.denseVanillaEntities").define("denseVanillaEntities", true);
        DENSE_ENTITY_DISTANCE = BUILDER.translation("liubai.configuration.lod.denseEntityDistance")
                .defineInRange("denseEntityDistance", 32.0, 16.0, 128.0);
        DENSE_ENTITY_HIGH_LIMIT = BUILDER.translation("liubai.configuration.lod.denseEntityHighLimit")
                .defineInRange("denseEntityHighLimit", 24, 1, 256);
        DENSE_ENTITY_CRITICAL_LIMIT = BUILDER.translation("liubai.configuration.lod.denseEntityCriticalLimit")
                .defineInRange("denseEntityCriticalLimit", 12, 1, 256);
        BUILDER.pop();

        BUILDER.translation("liubai.configuration.effects").push("effects");
        REDUCE_SHADOWS = BUILDER.translation("liubai.configuration.effects.reduceEntityShadows").define("reduceEntityShadows", true);
        SHADOW_DISTANCE = BUILDER.translation("liubai.configuration.effects.shadowDistance").defineInRange("shadowDistance", 24.0, 0.0, 128.0);
        REDUCE_NAME_TAGS = BUILDER.translation("liubai.configuration.effects.reduceNameTags").define("reduceNameTags", true);
        NAME_TAG_DISTANCE = BUILDER.translation("liubai.configuration.effects.nameTagDistance").defineInRange("nameTagDistance", 48.0, 8.0, 256.0);
        REDUCE_PARTICLES = BUILDER.translation("liubai.configuration.effects.reduceParticles").define("reduceParticles", true);
        PARTICLE_DISTANCE = BUILDER.translation("liubai.configuration.effects.particleDistance").defineInRange("particleDistance", 32.0, 4.0, 256.0);
        PARTICLE_SOFT_LIMIT = BUILDER.comment("Existing particles above this count make distant admission more aggressive.")
                .translation("liubai.configuration.effects.particleSoftLimit")
                .defineInRange("particleSoftLimit", 4096, 256, 32768);
        PARTICLE_HIGH_BUDGET = BUILDER.translation("liubai.configuration.effects.particleHighBudget")
                .defineInRange("particleHighBudget", 256, 16, 4096);
        PARTICLE_CRITICAL_BUDGET = BUILDER.translation("liubai.configuration.effects.particleCriticalBudget")
                .defineInRange("particleCriticalBudget", 128, 8, 4096);
        BUILDER.pop();

        BUILDER.translation("liubai.configuration.flywheel").push("flywheel");
        FLYWHEEL_ADAPTIVE_LIMITER = BUILDER.comment("Enhance Flywheel's own distance update limiter on explicitly supported versions.")
                .translation("liubai.configuration.flywheel.adaptiveLimiter").define("adaptiveLimiter", true);
        FLYWHEEL_HIGH_MULTIPLIER = BUILDER.translation("liubai.configuration.flywheel.highMultiplier")
                .defineInRange("highMultiplier", 2, 1, 8);
        FLYWHEEL_CRITICAL_MULTIPLIER = BUILDER.translation("liubai.configuration.flywheel.criticalMultiplier")
                .defineInRange("criticalMultiplier", 3, 1, 12);
        BUILDER.pop();

        BUILDER.translation("liubai.configuration.compatibility").push("compatibility");
        ENTITY_ALLOWLIST = BUILDER.comment("Entity type ids that Liubai must never hide.")
                .translation("liubai.configuration.compatibility.entityAllowlist")
                .defineListAllowEmpty("entityAllowlist", List.of(
                        "minecraft:player", "minecraft:ender_dragon", "minecraft:wither",
                        "minecraft:tnt", "minecraft:leash_knot", "minecraft:lightning_bolt"
                ), ClientConfig::isResourceLocation);
        BLOCK_ENTITY_ALLOWLIST = BUILDER.comment("Block entity type ids with visuals outside their block.")
                .translation("liubai.configuration.compatibility.blockEntityAllowlist")
                .defineListAllowEmpty("blockEntityAllowlist", List.of(
                        "minecraft:beacon", "minecraft:end_gateway", "minecraft:end_portal",
                        "minecraft:structure_block", "minecraft:conduit"
                ), ClientConfig::isResourceLocation);
        DISABLED_NAMESPACES = BUILDER.comment("Never cull content from these mod namespaces. Create is conservative until a versioned Flywheel adapter is installed.")
                .translation("liubai.configuration.compatibility.disabledNamespaces")
                .defineListAllowEmpty("disabledNamespaces", List.of("create", "flywheel"), ClientConfig::isNamespace);
        BUILDER.pop();

        BUILDER.translation("liubai.configuration.debug").push("debug");
        SHOW_HUD = BUILDER.translation("liubai.configuration.debug.showHud").define("showHud", false);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    private ClientConfig() {
    }

    private static boolean isResourceLocation(Object value) {
        return value instanceof String string && string.matches("[a-z0-9_.-]+:[a-z0-9_./-]+");
    }

    private static boolean isNamespace(Object value) {
        return value instanceof String string && string.matches("[a-z0-9_.-]+");
    }
}
