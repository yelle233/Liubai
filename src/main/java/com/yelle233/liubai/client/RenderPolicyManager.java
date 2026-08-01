package com.yelle233.liubai.client;

import com.yelle233.liubai.api.LiubaiApi;
import com.yelle233.liubai.config.ConfigSnapshot;
import com.yelle233.liubai.compat.ValkyrienCompatibility;
import com.yelle233.liubai.visibility.EffectiveVisibilityBackend;
import com.yelle233.liubai.visibility.RenderObjectKey;
import com.yelle233.liubai.visibility.VisibilityMath;
import com.yelle233.liubai.visibility.VisibilityService;
import com.yelle233.liubai.visibility.VisibilityState;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

public final class RenderPolicyManager {
    private static final Set<ParticleType<?>> IMPORTANT_PARTICLES = Set.of(
            ParticleTypes.DAMAGE_INDICATOR, ParticleTypes.TOTEM_OF_UNDYING, ParticleTypes.SONIC_BOOM,
            ParticleTypes.EXPLOSION_EMITTER, ParticleTypes.FLASH, ParticleTypes.SWEEP_ATTACK
    );
    private static final double MAX_GENERIC_BOUNDS = 256.0;

    private final VisibilityService visibility;
    private final RenderStatistics statistics;
    private final Map<Object, RenderDecision> frameDecisions = new IdentityHashMap<>();
    private final Map<Object, Boolean> shipManagedObjects = new IdentityHashMap<>();
    private final StableDensitySelector densitySelector = new StableDensitySelector();
    private final Map<EntityType<?>, TypePolicy> entityTypePolicies = new IdentityHashMap<>();
    private final Map<BlockEntityType<?>, TypePolicy> blockEntityTypePolicies = new IdentityHashMap<>();
    private final Map<RenderObjectKey, ScreenState> screenStates = new HashMap<>();
    private FrameContext frame = FrameContext.EMPTY;
    private ConfigSnapshot config;
    private EffectiveVisibilityBackend visibilityBackend = EffectiveVisibilityBackend.DISABLED;
    private long particleSequence;
    private int particlesAdmittedThisFrame;
    private int liveParticleCount;

    public RenderPolicyManager(VisibilityService visibility, RenderStatistics statistics) {
        this.visibility = visibility;
        this.statistics = statistics;
    }

    public void beginFrame(FrameContext frame, ConfigSnapshot config, EffectiveVisibilityBackend visibilityBackend, int liveParticleCount) {
        boolean configChanged = this.config != config;
        this.frame = frame;
        this.config = config;
        this.visibilityBackend = visibilityBackend;
        this.liveParticleCount = liveParticleCount;
        particlesAdmittedThisFrame = 0;
        frameDecisions.clear();
        shipManagedObjects.clear();
        if (configChanged) {
            entityTypePolicies.clear();
            blockEntityTypePolicies.clear();
            densitySelector.clear();
        } else if ((frame.frame() & 63L) == 0L) {
            densitySelector.prune(frame.frame());
        }
        if ((frame.frame() & 255L) == 0L) {
            long oldest = frame.frame() - 600L;
            screenStates.values().removeIf(state -> state.lastFrame < oldest);
        }
    }

    public void updateFrameContext(FrameContext frame) {
        this.frame = frame;
    }

    public RenderDecision decide(Entity entity) {
        if (config == null || !config.enabled() || !config.entityCulling() || entity.level() == null
                || (!config.screenSpaceLod() && visibilityBackend != EffectiveVisibilityBackend.BUILTIN)) {
            return RenderDecision.FULL;
        }
        RenderDecision cached = frameDecisions.get(entity);
        if (cached != null) return cached;

        if (isShipManagedEntity(entity)) {
            frameDecisions.put(entity, RenderDecision.FULL);
            statistics.valkyrienEntityBypassed();
            return RenderDecision.FULL;
        }

        RenderObjectKey key = RenderObjectKey.entity(entity.level().dimension(), entity.getUUID());
        RenderDecision decision = decideEntityUncached(entity, key);
        frameDecisions.put(entity, decision);
        statistics.entity(decision);
        return decision;
    }

    private RenderDecision decideEntityUncached(Entity entity, RenderObjectKey key) {
        TypePolicy typePolicy = entityTypePolicies.computeIfAbsent(entity.getType(), this::resolveEntityTypePolicy);
        ResourceLocation typeId = typePolicy.id;
        AABB bounds = LiubaiApi.resolveRenderBounds(entity, entity.getBoundingBoxForCulling());
        if (!safeGenericBounds(bounds)) return RenderDecision.SAFE;
        Vec3 center = bounds.getCenter();
        double distanceSqr = frame.cameraPosition().distanceToSqr(center);
        boolean withinSafeDistance = distanceSqr <= config.safeDistance() * config.safeDistance();
        if (isImportant(entity) || LiubaiApi.isForceVisible(entity)) {
            return RenderDecision.SAFE;
        }
        if (typePolicy.disabled) {
            return RenderDecision.ALLOWLISTED;
        }

        if (!withinSafeDistance && shouldApplyDensityLimit(entity, typeId, distanceSqr)) {
            return RenderDecision.DENSITY_LIMITED;
        }

        double radius = Math.max(0.35, Math.max(bounds.getXsize(), Math.max(bounds.getYsize(), bounds.getZsize())) * 0.5);
        double pixels = frame.projectedRadiusPixels(radius, center);
        if (!withinSafeDistance && shouldSkipForScreenSize(key, pixels, distanceSqr)) {
            return RenderDecision.TOO_SMALL;
        }

        if (canCheckOcclusion(distanceSqr)
                && visibilityBackend == EffectiveVisibilityBackend.BUILTIN) {
            VisibilityState state = visibility.query(key, bounds, frame, config);
            if (state == VisibilityState.OCCLUDED) {
                return RenderDecision.OCCLUDED;
            }
        }
        return withinSafeDistance ? RenderDecision.SAFE : RenderDecision.FULL;
    }

    private boolean shouldApplyDensityLimit(Entity entity, ResourceLocation typeId, double distanceSqr) {
        if (!config.denseVanillaEntities() || frame.pressure() == PressureLevel.NORMAL || typeId == null
                || !"minecraft".equals(typeId.getNamespace())
                || (!(entity instanceof ItemEntity) && !(entity instanceof ExperienceOrb))
                || distanceSqr <= config.denseEntityDistance() * config.denseEntityDistance()) return false;
        long chunk = ChunkPos.asLong(entity.getBlockX() >> 4, entity.getBlockZ() >> 4);
        int limit = frame.pressure() == PressureLevel.CRITICAL
                ? config.denseEntityCriticalLimit() : config.denseEntityHighLimit();
        return densitySelector.shouldSkip(entity.getUUID(), chunk, limit, frame.frame());
    }

    private boolean shouldSkipForScreenSize(RenderObjectKey key, double pixels, double distanceSqr) {
        if (!config.screenSpaceLod() || frame.pressure() == PressureLevel.NORMAL
                || config.minimumProjectedRadius() <= 0 || distanceSqr <= 576.0) {
            screenStates.remove(key);
            return false;
        }
        double hideThreshold = config.minimumProjectedRadius() * (frame.pressure() == PressureLevel.CRITICAL ? 1.6 : 1.0);
        ScreenState old = screenStates.get(key);
        if (pixels >= hideThreshold * 1.35) {
            screenStates.put(key, new ScreenState(0, false, frame.frame()));
            return false;
        }
        int smallFrames = pixels < hideThreshold ? (old == null ? 1 : old.smallFrames + 1) : 0;
        boolean skip = (old != null && old.skipped && pixels < hideThreshold * 1.35) || smallFrames >= 3;
        screenStates.put(key, new ScreenState(smallFrames, skip, frame.frame()));
        return skip;
    }

    public RenderDecision decide(BlockEntity blockEntity, AABB rendererBounds) {
        if (config == null || !config.enabled() || !config.blockEntityCulling() || blockEntity.getLevel() == null
                || visibilityBackend != EffectiveVisibilityBackend.BUILTIN) {
            return RenderDecision.FULL;
        }
        RenderDecision cached = frameDecisions.get(blockEntity);
        if (cached != null) return cached;

        RenderObjectKey key = RenderObjectKey.blockEntity(blockEntity.getLevel().dimension(), blockEntity.getBlockPos().asLong());
        TypePolicy typePolicy = blockEntityTypePolicies.computeIfAbsent(blockEntity.getType(), this::resolveBlockEntityTypePolicy);
        AABB bounds = LiubaiApi.resolveRenderBounds(blockEntity, rendererBounds);
        RenderDecision decision;
        if (!safeGenericBounds(bounds)) {
            decision = RenderDecision.SAFE;
        } else {
            Vec3 center = bounds.getCenter();
            double distanceSqr = frame.cameraPosition().distanceToSqr(center);
            boolean withinSafeDistance = distanceSqr <= config.safeDistance() * config.safeDistance();
            if (LiubaiApi.isForceVisible(blockEntity)) {
                decision = RenderDecision.SAFE;
            } else if (typePolicy.disabled) {
                decision = RenderDecision.ALLOWLISTED;
            } else if (canCheckOcclusion(distanceSqr)
                    && visibility.query(key, bounds, frame, config) == VisibilityState.OCCLUDED) {
                decision = RenderDecision.OCCLUDED;
            } else {
                decision = withinSafeDistance ? RenderDecision.SAFE : RenderDecision.FULL;
            }
        }
        frameDecisions.put(blockEntity, decision);
        statistics.blockEntity(decision);
        return decision;
    }

    private boolean canCheckOcclusion(double distanceSqr) {
        double start = VisibilityMath.occlusionStartDistance(config.occlusionMinDistance());
        return distanceSqr >= start * start;
    }

    public boolean bypassDynamicBlockEntity(BlockEntity blockEntity) {
        if (config == null || !config.enabled() || blockEntity == null || blockEntity.getLevel() == null) return false;
        if (ValkyrienCompatibility.conservativeFallbackActive()) {
            statistics.valkyrienBlockEntityBypassed();
            return true;
        }
        Boolean cached = shipManagedObjects.get(blockEntity);
        if (cached != null) return cached;
        boolean bypass = ValkyrienCompatibility.contains(blockEntity);
        shipManagedObjects.put(blockEntity, bypass);
        if (bypass) statistics.valkyrienBlockEntityBypassed();
        return bypass;
    }

    public boolean inspectBlockEntities() {
        return config != null && config.enabled() && config.blockEntityCulling()
                && visibilityBackend == EffectiveVisibilityBackend.BUILTIN
                && !ValkyrienCompatibility.conservativeFallbackActive();
    }

    private boolean isShipManagedEntity(Entity entity) {
        if (ValkyrienCompatibility.conservativeFallbackActive()) return true;
        Boolean cached = shipManagedObjects.get(entity);
        if (cached != null) return cached;
        boolean bypass = ValkyrienCompatibility.contains(entity);
        shipManagedObjects.put(entity, bypass);
        return bypass;
    }

    private static boolean safeGenericBounds(AABB bounds) {
        return VisibilityMath.isFinite(bounds) && bounds.getXsize() <= MAX_GENERIC_BOUNDS
                && bounds.getYsize() <= MAX_GENERIC_BOUNDS && bounds.getZsize() <= MAX_GENERIC_BOUNDS;
    }

    private boolean isImportant(Entity entity) {
        Minecraft minecraft = Minecraft.getInstance();
        if (entity instanceof Player || entity instanceof EnderDragon || entity instanceof WitherBoss
                || entity instanceof PrimedTnt || entity instanceof Projectile || entity instanceof LightningBolt) return true;
        if (entity.isPassenger() || entity.isVehicle() || entity.isCurrentlyGlowing() || entity.isOnFire()) return true;
        if (minecraft.hitResult instanceof EntityHitResult hit && hit.getEntity() == entity) return true;
        return entity instanceof Mob mob && minecraft.player != null && mob.getTarget() == minecraft.player;
    }

    private boolean isDisabled(ResourceLocation id, Set<String> allowlist) {
        return id == null || allowlist.contains(id.toString()) || config.disabledNamespaces().contains(id.getNamespace());
    }

    public boolean skipShadow(Entity entity) {
        if (config == null || !config.enabled() || !config.reduceShadows()) return false;
        TypePolicy typePolicy = entityTypePolicies.computeIfAbsent(entity.getType(), this::resolveEntityTypePolicy);
        if (isShipManagedEntity(entity) || isImportant(entity)
                || typePolicy.disabled) return false;
        double distance = config.shadowDistance();
        if (frame.pressure() == PressureLevel.HIGH) distance *= 0.75;
        if (frame.pressure() == PressureLevel.CRITICAL) distance *= 0.5;
        boolean skip = frame.cameraPosition().distanceToSqr(entity.position()) > distance * distance;
        if (skip) statistics.shadowSkipped();
        return skip;
    }

    public boolean skipNameTag(Entity entity) {
        if (config == null || !config.enabled() || !config.reduceNameTags()) return false;
        TypePolicy typePolicy = entityTypePolicies.computeIfAbsent(entity.getType(), this::resolveEntityTypePolicy);
        if (isShipManagedEntity(entity) || isImportant(entity)
                || typePolicy.disabled) return false;
        double distance = config.nameTagDistance();
        if (frame.pressure() == PressureLevel.CRITICAL) distance *= 0.75;
        boolean skip = frame.cameraPosition().distanceToSqr(entity.position()) > distance * distance;
        if (skip) statistics.nameTagSkipped();
        return skip;
    }

    public boolean skipParticle(ParticleOptions options, Vec3 position) {
        if (options != null && IMPORTANT_PARTICLES.contains(options.getType())) return false;
        return skipParticle(position);
    }

    public boolean skipParticle(Vec3 position) {
        if (config == null || !config.enabled() || !config.reduceParticles() || frame.pressure() == PressureLevel.NORMAL) return false;
        if (ValkyrienCompatibility.ownerlessSafeModeActive()) {
            statistics.valkyrienParticleBypassed();
            return false;
        }
        if (frame.cameraPosition().distanceToSqr(position) <= config.particleDistance() * config.particleDistance()) return false;
        int budget = frame.pressure() == PressureLevel.CRITICAL
                ? config.particleCriticalBudget() : config.particleHighBudget();
        boolean saturated = liveParticleCount >= config.particleSoftLimit();
        long sequence = particleSequence++;
        boolean sampledOut;
        if (frame.pressure() == PressureLevel.CRITICAL) {
            sampledOut = saturated ? (sequence & 7L) != 0 : (sequence & 3L) != 0;
        } else {
            sampledOut = saturated ? (sequence & 3L) != 0 : (sequence & 1L) != 0;
        }
        boolean skip = particlesAdmittedThisFrame >= budget || sampledOut;
        if (skip) {
            statistics.particleSkipped();
        } else {
            particlesAdmittedThisFrame++;
            statistics.particleAccepted();
        }
        return skip;
    }

    public int liveParticleCount() {
        return liveParticleCount;
    }

    private TypePolicy resolveEntityTypePolicy(EntityType<?> type) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        return new TypePolicy(id, isDisabled(id, config.entityAllowlist()));
    }

    private TypePolicy resolveBlockEntityTypePolicy(BlockEntityType<?> type) {
        ResourceLocation id = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(type);
        return new TypePolicy(id, isDisabled(id, config.blockEntityAllowlist()));
    }

    private record TypePolicy(ResourceLocation id, boolean disabled) {
    }

    private record ScreenState(int smallFrames, boolean skipped, long lastFrame) {
    }
}
