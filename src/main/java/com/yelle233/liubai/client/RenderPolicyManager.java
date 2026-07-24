package com.yelle233.liubai.client;

import com.yelle233.liubai.config.ConfigSnapshot;
import com.yelle233.liubai.api.LiubaiApi;
import com.yelle233.liubai.visibility.EffectiveVisibilityBackend;
import com.yelle233.liubai.visibility.RenderObjectKey;
import com.yelle233.liubai.visibility.VisibilityService;
import com.yelle233.liubai.visibility.VisibilityState;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.IdentityHashMap;
import java.util.Map;

public final class RenderPolicyManager {
    private final VisibilityService visibility;
    private final RenderStatistics statistics;
    private final Map<Object, RenderDecision> frameDecisions = new IdentityHashMap<>();
    private FrameContext frame = FrameContext.EMPTY;
    private ConfigSnapshot config;
    private EffectiveVisibilityBackend visibilityBackend = EffectiveVisibilityBackend.DISABLED;
    private long particleSequence;

    public RenderPolicyManager(VisibilityService visibility, RenderStatistics statistics) {
        this.visibility = visibility;
        this.statistics = statistics;
    }

    public void beginFrame(FrameContext frame, ConfigSnapshot config, EffectiveVisibilityBackend visibilityBackend) {
        this.frame = frame;
        this.config = config;
        this.visibilityBackend = visibilityBackend;
        frameDecisions.clear();
    }

    public RenderDecision decide(Entity entity) {
        if (config == null || !config.enabled() || !config.entityCulling() || entity.level() == null
                || (!config.screenSpaceLod() && visibilityBackend != EffectiveVisibilityBackend.BUILTIN)) {
            return RenderDecision.FULL;
        }
        RenderDecision cached = frameDecisions.get(entity);
        if (cached != null) return cached;

        RenderObjectKey key = RenderObjectKey.entity(entity.level().dimension(), entity.getUUID());
        RenderDecision decision = decideEntityUncached(entity, key);
        frameDecisions.put(entity, decision);
        statistics.entity(decision);
        return decision;
    }

    private RenderDecision decideEntityUncached(Entity entity, RenderObjectKey key) {
        ResourceLocation typeId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        Vec3 center = entity.getBoundingBox().getCenter();
        double distanceSqr = frame.cameraPosition().distanceToSqr(center);
        if (distanceSqr <= config.safeDistance() * config.safeDistance() || isImportant(entity) || LiubaiApi.isForceVisible(entity)) {
            return new RenderDecision(false, 0, RenderDecision.Reason.SAFE);
        }
        if (isDisabled(typeId, config.entityAllowlist())) {
            return new RenderDecision(false, 0, RenderDecision.Reason.ALLOWLISTED);
        }

        double radius = Math.max(0.35, Math.max(entity.getBbWidth(), entity.getBbHeight()) * 0.5);
        double pixels = frame.projectedRadiusPixels(radius, center);
        int lod = pixels < 10 ? 2 : pixels < 24 ? 1 : 0;
        if (config.screenSpaceLod() && frame.pressure() != PressureLevel.NORMAL && config.minimumProjectedRadius() > 0 && distanceSqr > 576) {
            double threshold = config.minimumProjectedRadius() * (frame.pressure() == PressureLevel.CRITICAL ? 1.6 : 1.0);
            if (pixels < threshold) return new RenderDecision(true, 4, RenderDecision.Reason.TOO_SMALL);
        }

        if (visibilityBackend == EffectiveVisibilityBackend.BUILTIN
                && distanceSqr >= config.occlusionMinDistance() * config.occlusionMinDistance()) {
            VisibilityState state = visibility.query(key, entity.getBoundingBox(), frame, config);
            if (state == VisibilityState.OCCLUDED) {
                return new RenderDecision(true, 4, RenderDecision.Reason.OCCLUDED);
            }
        }
        return new RenderDecision(false, lod, RenderDecision.Reason.VISIBLE);
    }

    public RenderDecision decide(BlockEntity blockEntity) {
        if (config == null || !config.enabled() || !config.blockEntityCulling() || blockEntity.getLevel() == null
                || visibilityBackend != EffectiveVisibilityBackend.BUILTIN) {
            return RenderDecision.FULL;
        }
        RenderDecision cached = frameDecisions.get(blockEntity);
        if (cached != null) return cached;

        RenderObjectKey key = RenderObjectKey.blockEntity(blockEntity.getLevel().dimension(), blockEntity.getBlockPos().asLong());
        ResourceLocation typeId = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(blockEntity.getType());
        BlockPos pos = blockEntity.getBlockPos();
        AABB bounds = new AABB(pos);
        Vec3 center = bounds.getCenter();
        double distanceSqr = frame.cameraPosition().distanceToSqr(center);
        RenderDecision decision;
        if (distanceSqr <= config.safeDistance() * config.safeDistance() || LiubaiApi.isForceVisible(blockEntity)) {
            decision = new RenderDecision(false, 0, RenderDecision.Reason.SAFE);
        } else if (isDisabled(typeId, config.blockEntityAllowlist())) {
            decision = new RenderDecision(false, 0, RenderDecision.Reason.ALLOWLISTED);
        } else if (distanceSqr >= config.occlusionMinDistance() * config.occlusionMinDistance()
                && visibility.query(key, bounds, frame, config) == VisibilityState.OCCLUDED) {
            decision = new RenderDecision(true, 4, RenderDecision.Reason.OCCLUDED);
        } else {
            double pixels = frame.projectedRadiusPixels(0.866, center);
            decision = new RenderDecision(false, pixels < 12 ? 2 : pixels < 28 ? 1 : 0, RenderDecision.Reason.VISIBLE);
        }
        frameDecisions.put(blockEntity, decision);
        statistics.blockEntity(decision);
        return decision;
    }

    private boolean isImportant(Entity entity) {
        Minecraft minecraft = Minecraft.getInstance();
        if (entity instanceof Player || entity instanceof EnderDragon || entity instanceof WitherBoss
                || entity instanceof PrimedTnt || entity instanceof Projectile || entity instanceof LightningBolt) return true;
        if (entity.isPassenger() || entity.isVehicle() || entity.isCurrentlyGlowing() || entity.isOnFire()) return true;
        if (minecraft.hitResult instanceof EntityHitResult hit && hit.getEntity() == entity) return true;
        return entity instanceof Mob mob && minecraft.player != null && mob.getTarget() == minecraft.player;
    }

    private boolean isDisabled(ResourceLocation id, java.util.Set<String> allowlist) {
        return id == null || allowlist.contains(id.toString()) || config.disabledNamespaces().contains(id.getNamespace());
    }

    public boolean skipShadow(Entity entity) {
        if (config == null || !config.enabled() || !config.reduceShadows() || isImportant(entity)) return false;
        double distance = config.shadowDistance();
        if (frame.pressure() == PressureLevel.HIGH) distance *= 0.75;
        if (frame.pressure() == PressureLevel.CRITICAL) distance *= 0.5;
        boolean skip = frame.cameraPosition().distanceToSqr(entity.position()) > distance * distance;
        if (skip) statistics.shadowSkipped();
        return skip;
    }

    public boolean skipNameTag(Entity entity) {
        if (config == null || !config.enabled() || !config.reduceNameTags() || isImportant(entity)) return false;
        double distance = config.nameTagDistance();
        if (frame.pressure() == PressureLevel.CRITICAL) distance *= 0.75;
        boolean skip = frame.cameraPosition().distanceToSqr(entity.position()) > distance * distance;
        if (skip) statistics.nameTagSkipped();
        return skip;
    }

    public boolean skipParticle(Vec3 position) {
        if (config == null || !config.enabled() || !config.reduceParticles() || frame.pressure() == PressureLevel.NORMAL) return false;
        if (frame.cameraPosition().distanceToSqr(position) <= config.particleDistance() * config.particleDistance()) return false;
        long sequence = particleSequence++;
        boolean skip = frame.pressure() == PressureLevel.CRITICAL ? (sequence & 3L) != 0 : (sequence & 1L) != 0;
        if (skip) statistics.particleSkipped();
        return skip;
    }
}
