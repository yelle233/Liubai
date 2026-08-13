package com.yelle233.liubai.client;

import com.yelle233.liubai.config.ClientConfig;
import com.yelle233.liubai.config.OcclusionMode;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Fabric-native, dependency-free configuration UI mirroring the NeoForge section layout. */
public final class LiubaiConfigScreen extends Screen {
    private static final int ROW_HEIGHT = 24;
    private final Screen parent;
    private final Page page;
    private final List<Option> options;
    private int firstRow;

    public LiubaiConfigScreen(Screen parent) { this(parent, null); }

    private LiubaiConfigScreen(Screen parent, Page page) {
        super(Component.translatable(page == null ? "liubai.configuration.title" : page.key));
        this.parent = parent;
        this.page = page;
        this.options = page == null ? List.of() : options(page);
    }

    @Override protected void init() {
        clearWidgets();
        if (page == null) initRoot(); else initPage();
    }

    private void initRoot() {
        int left = width / 2 - 155, y = 42;
        for (Page section : Page.values()) {
            Button button = Button.builder(Component.translatable(section.key + ".button"),
                    b -> minecraft.setScreen(new LiubaiConfigScreen(this, section)))
                    .bounds(left, y, 310, 20).tooltip(Tooltip.create(Component.translatable(section.key + ".tooltip"))).build();
            addRenderableWidget(button);
            y += 24;
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(width / 2 - 100, height - 28, 200, 20).build());
    }

    private void initPage() {
        int visibleRows = Math.max(1, (height - 80) / ROW_HEIGHT);
        firstRow = Math.max(0, Math.min(firstRow, Math.max(0, options.size() - visibleRows)));
        int end = Math.min(options.size(), firstRow + visibleRows);
        int y = 34;
        for (int i = firstRow; i < end; i++, y += ROW_HEIGHT) addOption(options.get(i), y);
        addRenderableWidget(Button.builder(Component.literal("▲"), b -> scroll(-1)).bounds(width / 2 + 162, 34, 24, 20).build()).active = firstRow > 0;
        addRenderableWidget(Button.builder(Component.literal("▼"), b -> scroll(1)).bounds(width / 2 + 162, height - 52, 24, 20).build()).active = end < options.size();
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), b -> closePage())
                .bounds(width / 2 - 100, height - 28, 200, 20).build());
    }

    private void addOption(Option option, int y) {
        int left = width / 2 - 155;
        if (option instanceof BoolOption bool) {
            Button button = Button.builder(bool.label(), b -> { bool.toggle(); b.setMessage(bool.label()); })
                    .bounds(left, y, 310, 20).tooltip(option.tooltip()).build();
            addRenderableWidget(button);
        } else if (option instanceof EnumOption enumeration) {
            Button button = Button.builder(enumeration.label(), b -> { enumeration.next(); b.setMessage(enumeration.label()); })
                    .bounds(left, y, 310, 20).tooltip(option.tooltip()).build();
            addRenderableWidget(button);
        } else if (option instanceof NumberOption number) {
            addRenderableWidget(Button.builder(option.name(), b -> {}).bounds(left, y, 205, 20).tooltip(option.tooltip()).build()).active = false;
            EditBox field = new EditBox(font, left + 209, y, 101, 20, option.name());
            field.setValue(number.text());
            field.setResponder(number::accept);
            addRenderableWidget(field);
        } else if (option instanceof ListOption list) {
            Button button = Button.builder(list.label(), b -> minecraft.setScreen(new ListEditorScreen(this, list)))
                    .bounds(left, y, 310, 20).tooltip(option.tooltip()).build();
            addRenderableWidget(button);
        }
    }

    private void scroll(int direction) { firstRow += direction; rebuildWidgets(); }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (page != null && vertical != 0) { scroll(vertical > 0 ? -1 : 1); return true; }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    private void closePage() { ClientConfig.save(); LiubaiClientSystem.INSTANCE.refreshConfig(); minecraft.setScreen(parent); }

    @Override public void onClose() {
        ClientConfig.save();
        LiubaiClientSystem.INSTANCE.refreshConfig();
        minecraft.setScreen(parent);
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 14, 0xFFFFFF);
    }

    private static List<Option> options(Page page) {
        ClientConfig c = ClientConfig.get();
        List<Option> result = new ArrayList<>();
        switch (page) {
            case GENERAL -> {
                result.add(bool("general.enabled", () -> c.enabled, v -> c.enabled = v));
                result.add(integer("general.targetFps", () -> c.targetFps, v -> c.targetFps = v, 30, 240));
                result.add(bool("general.adaptiveMode", () -> c.adaptiveMode, v -> c.adaptiveMode = v));
            }
            case CULLING -> {
                result.add(bool("culling.entities", () -> c.entityCulling, v -> c.entityCulling = v));
                result.add(bool("culling.blockEntities", () -> c.blockEntityCulling, v -> c.blockEntityCulling = v));
                result.add(bool("culling.occlusion", () -> c.occlusionCulling, v -> c.occlusionCulling = v));
                result.add(new EnumOption("culling.occlusionMode", () -> c.occlusionMode, v -> c.occlusionMode = v));
                result.add(decimal("culling.safeDistance", () -> c.safeDistance, v -> c.safeDistance = v, 0, 64));
                result.add(decimal("culling.occlusionMinDistance", () -> c.occlusionMinDistance, v -> c.occlusionMinDistance = v, 4, 128));
                result.add(integer("culling.checksPerFrame", () -> c.checksPerFrame, v -> c.checksPerFrame = v, 0, 512));
                result.add(integer("culling.budgetMicros", () -> c.visibilityBudgetMicros, v -> c.visibilityBudgetMicros = v, 100, 10000));
                result.add(integer("culling.occludedConfirmations", () -> c.occludedConfirmations, v -> c.occludedConfirmations = v, 1, 8));
                result.add(integer("culling.cacheTtlFrames", () -> c.cacheTtlFrames, v -> c.cacheTtlFrames = v, 2, 600));
                result.add(decimal("culling.minimumProjectedRadius", () -> c.minimumProjectedRadius, v -> c.minimumProjectedRadius = v, 0, 16));
            }
            case LOD -> {
                result.add(bool("lod.screenSpace", () -> c.screenSpaceLod, v -> c.screenSpaceLod = v));
                result.add(bool("lod.temporal", () -> c.temporalLod, v -> c.temporalLod = v));
                result.add(integer("lod.maxTemporalInterval", () -> c.maxTemporalInterval, v -> c.maxTemporalInterval = v, 1, 31));
                result.add(bool("lod.denseVanillaEntities", () -> c.denseVanillaEntities, v -> c.denseVanillaEntities = v));
                result.add(decimal("lod.denseEntityDistance", () -> c.denseEntityDistance, v -> c.denseEntityDistance = v, 16, 128));
                result.add(integer("lod.denseEntityHighLimit", () -> c.denseEntityHighLimit, v -> c.denseEntityHighLimit = v, 1, 256));
                result.add(integer("lod.denseEntityCriticalLimit", () -> c.denseEntityCriticalLimit, v -> c.denseEntityCriticalLimit = v, 1, 256));
            }
            case EFFECTS -> {
                result.add(bool("effects.reduceEntityShadows", () -> c.reduceShadows, v -> c.reduceShadows = v));
                result.add(decimal("effects.shadowDistance", () -> c.shadowDistance, v -> c.shadowDistance = v, 0, 128));
                result.add(bool("effects.reduceNameTags", () -> c.reduceNameTags, v -> c.reduceNameTags = v));
                result.add(decimal("effects.nameTagDistance", () -> c.nameTagDistance, v -> c.nameTagDistance = v, 8, 256));
                result.add(bool("effects.reduceParticles", () -> c.reduceParticles, v -> c.reduceParticles = v));
                result.add(decimal("effects.particleDistance", () -> c.particleDistance, v -> c.particleDistance = v, 4, 256));
                result.add(integer("effects.particleSoftLimit", () -> c.particleSoftLimit, v -> c.particleSoftLimit = v, 256, 32768));
                result.add(integer("effects.particleHighBudget", () -> c.particleHighBudget, v -> c.particleHighBudget = v, 16, 4096));
                result.add(integer("effects.particleCriticalBudget", () -> c.particleCriticalBudget, v -> c.particleCriticalBudget = v, 8, 4096));
            }
            case COMPATIBILITY -> {
                result.add(new ListOption("compatibility.entityAllowlist", c.entityAllowlist));
                result.add(new ListOption("compatibility.blockEntityAllowlist", c.blockEntityAllowlist));
                result.add(new ListOption("compatibility.disabledNamespaces", c.disabledNamespaces));
            }
            case DEBUG -> result.add(bool("debug.showHud", () -> c.showHud, v -> c.showHud = v));
        }
        return result;
    }

    private static BoolOption bool(String path, Supplier<Boolean> get, Consumer<Boolean> set) { return new BoolOption(path, get, set); }
    private static NumberOption integer(String path, Supplier<Integer> get, Consumer<Integer> set, int min, int max) {
        return new NumberOption(path, () -> Integer.toString(get.get()), text -> { try { set.accept(Math.max(min, Math.min(max, Integer.parseInt(text)))); } catch (NumberFormatException ignored) {} });
    }
    private static NumberOption decimal(String path, Supplier<Double> get, Consumer<Double> set, double min, double max) {
        return new NumberOption(path, () -> String.format(Locale.ROOT, "%s", get.get()), text -> { try { set.accept(Math.max(min, Math.min(max, Double.parseDouble(text)))); } catch (NumberFormatException ignored) {} });
    }

    private enum Page {
        GENERAL("liubai.configuration.general"), CULLING("liubai.configuration.culling"), LOD("liubai.configuration.lod"),
        EFFECTS("liubai.configuration.effects"),
        COMPATIBILITY("liubai.configuration.compatibility"), DEBUG("liubai.configuration.debug");
        private final String key; Page(String key) { this.key = key; }
    }

    private sealed interface Option permits BoolOption, NumberOption, EnumOption, ListOption {
        String path();
        default Component name() { return Component.translatable("liubai.configuration." + path()); }
        default Tooltip tooltip() { return Tooltip.create(Component.translatable("liubai.configuration." + path() + ".tooltip")); }
    }
    private record BoolOption(String path, Supplier<Boolean> get, Consumer<Boolean> set) implements Option {
        Component label() { return Component.translatable("liubai.configuration.option.boolean", name(), Component.translatable(get.get() ? "options.on" : "options.off")); }
        void toggle() { set.accept(!get.get()); }
    }
    private record NumberOption(String path, Supplier<String> get, Consumer<String> set) implements Option {
        String text() { return get.get(); } void accept(String value) { set.accept(value); }
    }
    private record EnumOption(String path, Supplier<OcclusionMode> get, Consumer<OcclusionMode> set) implements Option {
        Component label() { return Component.translatable("liubai.configuration.option.enum", name(), Component.translatable("liubai.configuration.visibility.occlusionMode." + get.get().name().toLowerCase(Locale.ROOT))); }
        void next() { OcclusionMode[] modes = OcclusionMode.values(); set.accept(modes[(get.get().ordinal() + 1) % modes.length]); }
    }
    static final class ListOption implements Option {
        private final String path; private final java.util.Set<String> values;
        ListOption(String path, java.util.Set<String> values) { this.path = path; this.values = values; }
        public String path() { return path; }
        Component label() { return Component.translatable("liubai.configuration.option.list", name(), values.size()); }
    }

    private static final class ListEditorScreen extends Screen {
        private final Screen parent; private final ListOption option; private EditBox input;
        private ListEditorScreen(Screen parent, ListOption option) { super(option.name()); this.parent = parent; this.option = option; }
        @Override protected void init() {
            input = new EditBox(font, width / 2 - 155, 46, 310, 20, title);
            input.setValue(String.join(", ", option.values)); addRenderableWidget(input);
            addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose()).bounds(width / 2 - 100, height - 28, 200, 20).build());
        }
        @Override public void onClose() {
            option.values.clear();
            for (String value : input.getValue().split("[,\\n]")) { String clean = value.trim().toLowerCase(Locale.ROOT); if (!clean.isEmpty()) option.values.add(clean); }
            ClientConfig.save(); LiubaiClientSystem.INSTANCE.refreshConfig(); minecraft.setScreen(parent);
        }
        @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            super.render(graphics, mouseX, mouseY, partialTick); graphics.drawCenteredString(font, title, width / 2, 18, 0xFFFFFF);
            graphics.drawCenteredString(font, Component.translatable("liubai.configuration.list.hint"), width / 2, 72, 0xA0A0A0);
        }
    }
}
