package com.yelle233.liubai.client;

import com.yelle233.liubai.Liubai;
import com.yelle233.liubai.config.ConfigSnapshot;
import com.yelle233.liubai.compat.CompatibilityManager;
import com.yelle233.liubai.visibility.EffectiveVisibilityBackend;
import com.yelle233.liubai.visibility.VisibilityService;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;

public final class LiubaiClientSystem {
    public static final LiubaiClientSystem INSTANCE = new LiubaiClientSystem();

    private final FrameBudgetController budget = new FrameBudgetController();
    private final RenderStatistics statistics = new RenderStatistics();
    private final VisibilityService visibility = new VisibilityService();
    private final RenderPolicyManager policies = new RenderPolicyManager(visibility, statistics);
    private final TemporalLodScheduler temporalScheduler = new TemporalLodScheduler();
    private volatile ConfigSnapshot config;
    private ConfigSnapshot frameConfig;
    private volatile FrameContext frame = FrameContext.EMPTY;
    private volatile EffectiveVisibilityBackend visibilityBackend = EffectiveVisibilityBackend.DISABLED;
    private ClientLevel lastLevel;
    private long frameNumber;

    private LiubaiClientSystem() {
    }

    public void beginFrame() {
        ConfigSnapshot currentConfig = config;
        frameConfig = currentConfig;
        if (currentConfig == null) {
            policies.beginFrame(FrameContext.EMPTY, null, EffectiveVisibilityBackend.DISABLED);
            temporalScheduler.beginFrame(FrameContext.EMPTY, null);
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != lastLevel) {
            visibility.clear();
            budget.reset();
            lastLevel = minecraft.level;
        }
        Camera camera = minecraft.gameRenderer.getMainCamera();
        Vec3 position = camera.isInitialized() ? camera.getPosition() : Vec3.ZERO;
        frame = new FrameContext(++frameNumber, position, camera.getYRot(), camera.getXRot(),
                minecraft.options.fov().get(), Math.max(1, minecraft.getWindow().getHeight()), budget.pressure());
        EffectiveVisibilityBackend resolvedBackend = CompatibilityManager.INSTANCE.resolveBackend(currentConfig);
        if (resolvedBackend != visibilityBackend) {
            visibility.clear();
            visibilityBackend = resolvedBackend;
            Liubai.LOGGER.info("Liubai generic visibility backend switched to {}.", visibilityBackend);
        }
        policies.beginFrame(frame, currentConfig, visibilityBackend);
        temporalScheduler.beginFrame(frame, currentConfig);
        budget.beginFrame();
        if (visibilityBackend == EffectiveVisibilityBackend.BUILTIN && minecraft.level != null) {
            visibility.process(minecraft.level, frame, currentConfig);
        }
    }

    public void endFrame() {
        ConfigSnapshot currentConfig = frameConfig;
        frameConfig = null;
        if (currentConfig == null) return;
        VisibilityService.Counts counts = currentConfig.showHud() ? visibility.counts() : new VisibilityService.Counts(0, 0, 0, 0);
        statistics.finishFrame(counts.visible(), counts.occluded(), counts.unknown(), counts.queued());
        budget.endFrame(currentConfig);
    }

    public RenderPolicyManager policies() { return policies; }
    public RenderStatistics.Snapshot statistics() { return statistics.snapshot(); }
    public FrameContext frame() { return frame; }
    public ConfigSnapshot config() { return config; }
    public double averageRenderMillis() { return budget.averageRenderMillis(); }
    public EffectiveVisibilityBackend visibilityBackend() { return visibilityBackend; }
    public TemporalLodScheduler temporalScheduler() { return temporalScheduler; }
    public RenderStatistics statisticsRecorder() { return statistics; }

    public int adjustFlywheelUpdateDivisor(double distanceSquared, int original) {
        ConfigSnapshot currentConfig = config;
        FrameContext currentFrame = frame;
        if (currentConfig == null || !currentConfig.enabled() || !currentConfig.temporalLod()
                || !currentConfig.flywheelAdaptiveLimiter()
                || !CompatibilityManager.INSTANCE.supportsAdaptiveFlywheelLimiter()
                || currentFrame.pressure() == PressureLevel.NORMAL) return original;
        double safeDistance = Math.max(16.0, currentConfig.safeDistance());
        if (distanceSquared <= safeDistance * safeDistance) return original;
        int multiplier = currentFrame.pressure() == PressureLevel.CRITICAL
                ? currentConfig.flywheelCriticalMultiplier() : currentConfig.flywheelHighMultiplier();
        int adjusted = Math.max(original, Math.min(31, original * multiplier));
        if (currentFrame.pressure() == PressureLevel.CRITICAL && distanceSquared >= 1024.0) {
            adjusted = Math.max(adjusted, Math.min(31, currentConfig.maxTemporalInterval()));
        }
        if (adjusted != original) statistics.flywheelLimiterAdjusted();
        return adjusted;
    }

    public void resetWorld() {
        visibility.clear();
        lastLevel = null;
    }

    public void refreshConfig() {
        config = ConfigSnapshot.read();
    }

    public void unloadConfig() {
        config = null;
    }
}
