package com.yelle233.liubai.client;

import com.yelle233.liubai.Liubai;
import com.yelle233.liubai.config.ConfigSnapshot;
import com.yelle233.liubai.compat.CompatibilityManager;
import com.yelle233.liubai.compat.sable.SableCompatibility;
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
            policies.beginFrame(FrameContext.EMPTY, null, EffectiveVisibilityBackend.DISABLED, 0);
            temporalScheduler.beginFrame(FrameContext.EMPTY, null);
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        SableCompatibility.beginFrame(minecraft.level);
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
        int liveParticles = particleCount(minecraft);
        policies.beginFrame(frame, currentConfig, visibilityBackend, liveParticles);
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
        VisibilityService.Counts counts = currentConfig.showHud() ? visibility.counts() : new VisibilityService.Counts(0, 0, 0, 0, 0, 0);
        statistics.finishFrame(counts, policies.liveParticleCount());
        budget.endFrame(currentConfig, Minecraft.getInstance().options.framerateLimit().get());
    }

    private static int particleCount(Minecraft minecraft) {
        try {
            return Integer.parseInt(minecraft.particleEngine.countParticles());
        } catch (RuntimeException ignored) {
            return 0;
        }
    }

    public RenderPolicyManager policies() { return policies; }
    public RenderStatistics.Snapshot statistics() { return statistics.snapshot(); }
    public FrameContext frame() { return frame; }
    public ConfigSnapshot config() { return config; }
    public double averageRenderMillis() { return budget.averageRenderMillis(); }
    public double p95RenderMillis() { return budget.p95RenderMillis(); }
    public double p99RenderMillis() { return budget.p99RenderMillis(); }
    public int effectiveTargetFps() {
        ConfigSnapshot current = config;
        if (current == null) return 60;
        int limit = Minecraft.getInstance().options.framerateLimit().get();
        return limit > 0 && limit < 260 ? Math.min(current.targetFps(), limit) : current.targetFps();
    }
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
        if (SableCompatibility.flywheelSafeModeActive()) {
            statistics.sableFlywheelBypassed();
            return original;
        }
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
