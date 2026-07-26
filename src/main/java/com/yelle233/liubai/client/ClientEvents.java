package com.yelle233.liubai.client;

import com.yelle233.liubai.compat.sable.SableCompatibility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.common.util.TriState;

import java.util.Locale;

public final class ClientEvents {
    private final ModContainer container;

    public ClientEvents(ModContainer container) {
        this.container = container;
    }

    @SubscribeEvent
    public void onScreenInitialized(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof PauseScreen pauseScreen) || !pauseScreen.showsPauseMenu()) return;
        Component label = Component.translatable("liubai.menu.config");
        Button button = Button.builder(label, pressed -> Minecraft.getInstance().setScreen(new ConfigurationScreen(container, pauseScreen)))
                .bounds(pauseScreen.width - 84, 8, 76, 20)
                .tooltip(Tooltip.create(Component.translatable("liubai.menu.config.tooltip")))
                .build();
        event.addListener(button);
    }

    @SubscribeEvent
    public void onFramePre(RenderFrameEvent.Pre event) {
        LiubaiClientSystem.INSTANCE.beginFrame();
    }

    @SubscribeEvent
    public void onFramePost(RenderFrameEvent.Post event) {
        LiubaiClientSystem.INSTANCE.endFrame();
    }

    @SubscribeEvent
    public void onNameTag(RenderNameTagEvent event) {
        if (LiubaiClientSystem.INSTANCE.policies().skipNameTag(event.getEntity())) {
            event.setCanRender(TriState.FALSE);
        }
    }

    @SubscribeEvent
    public void onHud(RenderGuiEvent.Post event) {
        LiubaiClientSystem system = LiubaiClientSystem.INSTANCE;
        if (system.config() == null || !system.config().showHud() || Minecraft.getInstance().options.hideGui) return;
        drawHud(event.getGuiGraphics(), system);
    }

    private static void drawHud(GuiGraphics graphics, LiubaiClientSystem system) {
        RenderStatistics.Snapshot stats = system.statistics();
        double frameMillis = system.averageRenderMillis();
        double targetMillis = 1000.0 / system.effectiveTargetFps();
        double estimatedFps = frameMillis <= 0.001 ? 0.0 : 1000.0 / frameMillis;
        Component state = Component.translatable(system.config().enabled() ? "liubai.hud.state.enabled" : "liubai.hud.state.disabled");
        Component pressure = Component.translatable(switch (system.frame().pressure()) {
            case NORMAL -> "liubai.hud.pressure.normal";
            case HIGH -> "liubai.hud.pressure.high";
            case CRITICAL -> "liubai.hud.pressure.critical";
        });
        Component backend = Component.translatable(system.visibilityBackend().translationKey());
        int pressureColor = switch (system.frame().pressure()) {
            case NORMAL -> 0x80FF80;
            case HIGH -> 0xFFE080;
            case CRITICAL -> 0xFF7070;
        };

        int x = 6, y = 6, line = 10;
        boolean sableSafeMode = SableCompatibility.flywheelSafeModeActive();
        boolean sableFallback = SableCompatibility.conservativeFallbackActive();
        HookStatus.Snapshot hooks = HookStatus.snapshot();
        graphics.fill(2, 2, 430, sableSafeMode || sableFallback ? 168 : 158, 0xA0000000);
        graphics.drawString(Minecraft.getInstance().font, Component.translatable("liubai.hud.title", state), x, y, 0xFFFFFF);
        graphics.drawString(Minecraft.getInstance().font, Component.translatable("liubai.hud.frame",
                decimal(frameMillis), decimal(targetMillis), integer(estimatedFps)), x, y += line, 0xD8F3FF);
        graphics.drawString(Minecraft.getInstance().font, Component.translatable("liubai.hud.percentiles",
                decimal(system.p95RenderMillis()), decimal(system.p99RenderMillis())), x, y += line, 0xA8C7D3);
        graphics.drawString(Minecraft.getInstance().font, Component.translatable("liubai.hud.pressure", pressure), x, y += line, pressureColor);
        graphics.drawString(Minecraft.getInstance().font, Component.translatable("liubai.hud.backend", backend), x, y += line, 0xB8E0FF);
        if (sableFallback) {
            graphics.drawString(Minecraft.getInstance().font, Component.translatable("liubai.hud.sableFallback"),
                    x, y += line, 0xFFB070);
        } else if (sableSafeMode) {
            graphics.drawString(Minecraft.getInstance().font, Component.translatable("liubai.hud.sableSafeMode",
                    stats.sableEntitiesBypassed(), stats.sableBlockEntitiesBypassed(),
                    stats.sableParticlesBypassed(), stats.sableFlywheelBypassed()), x, y += line, 0x80FFB0);
        }
        graphics.drawString(Minecraft.getInstance().font, Component.translatable("liubai.hud.entities",
                stats.entitiesTested(), stats.entitiesTested() - stats.entitiesSkipped(), stats.entitiesSkipped()), x, y += line, 0xD8F3FF);
        graphics.drawString(Minecraft.getInstance().font, Component.translatable("liubai.hud.entityReasons",
                stats.entitiesOccluded(), stats.entitiesTooSmall(), stats.entitiesDensityLimited()), x, y += line, 0xA8C7D3);
        graphics.drawString(Minecraft.getInstance().font, Component.translatable("liubai.hud.blockEntities",
                stats.blockEntitiesTested(), stats.blockEntitiesTested() - stats.blockEntitiesSkipped(),
                stats.blockEntitiesSkipped(), stats.blockEntitiesOccluded()), x, y += line, 0xD8F3FF);
        graphics.drawString(Minecraft.getInstance().font, Component.translatable("liubai.hud.visibility",
                stats.visible(), stats.occluded(), stats.unknown(), stats.queued()), x, y += line, 0xD8F3FF);
        graphics.drawString(Minecraft.getInstance().font, Component.translatable("liubai.hud.visibilityWork",
                stats.visibilityChecks(), stats.visibilityTimeouts(), stats.visibilityMicros()), x, y += line, 0xA8C7D3);
        graphics.drawString(Minecraft.getInstance().font, Component.translatable("liubai.hud.effects",
                stats.shadowsSkipped(), stats.nameTagsSkipped(), stats.particlesSkipped()), x, y += line, 0xD8F3FF);
        graphics.drawString(Minecraft.getInstance().font, Component.translatable("liubai.hud.particles",
                stats.liveParticles(), stats.particlesAccepted()), x, y += line, 0xA8C7D3);
        graphics.drawString(Minecraft.getInstance().font, Component.translatable("liubai.hud.temporal",
                stats.temporalUpdatesDeferred(), stats.flywheelLimiterAdjusted()), x, y += line, 0xD8F3FF);
        graphics.drawString(Minecraft.getInstance().font, Component.translatable("liubai.hud.hooks",
                mark(hooks.entity()), mark(hooks.blockEntity()), mark(hooks.particle()), mark(hooks.flywheel())),
                x, y + line, 0xA8C7D3);
    }

    private static String decimal(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static String integer(double value) {
        return String.format(Locale.ROOT, "%.0f", value);
    }

    private static String mark(boolean observed) {
        return observed ? "✓" : "-";
    }
}
