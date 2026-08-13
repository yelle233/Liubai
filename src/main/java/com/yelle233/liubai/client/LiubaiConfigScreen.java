package com.yelle233.liubai.client;

import com.yelle233.liubai.config.ClientConfig;
import com.yelle233.liubai.config.OcclusionMode;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

/** Forge 1.20.1 counterpart of NeoForge's hierarchical generic configuration screen. */
public final class LiubaiConfigScreen extends Screen {
    private static final int ROW_HEIGHT = 24;
    private static final int CONTENT_WIDTH = 310;
    private static final List<Page> CATEGORIES = List.of(
            Page.GENERAL, Page.CULLING, Page.LOD, Page.EFFECTS, Page.FLYWHEEL,
            Page.COMPATIBILITY, Page.DEBUG
    );

    private final Screen parent;
    private final Page page;
    private final List<Row> rows = new ArrayList<>();
    private int scroll;
    private int maxScroll;

    public LiubaiConfigScreen(Screen parent) {
        this(parent, Page.CATEGORIES);
    }

    private LiubaiConfigScreen(Screen parent, Page page) {
        super(Component.translatable(page.titleKey()));
        this.parent = parent;
        this.page = page;
    }

    @Override
    protected void init() {
        rows.clear();
        scroll = 0;
        if (page == Page.CATEGORIES) {
            int y = 32;
            for (Page category : CATEGORIES) {
                String key = category.translationKey();
                addRenderableWidget(Button.builder(Component.translatable(key + ".button"),
                                button -> minecraft.setScreen(new LiubaiConfigScreen(this, category)))
                        .bounds(width / 2 - CONTENT_WIDTH / 2, y, CONTENT_WIDTH, 20)
                        .tooltip(Tooltip.create(Component.translatable(key + ".tooltip")))
                        .build());
                y += ROW_HEIGHT;
            }
            addDoneButton();
            return;
        }

        buildCategory();
        rows.stream().map(Row::widget).forEach(this::addRenderableWidget);
        maxScroll = Math.max(0, rows.size() * ROW_HEIGHT - Math.max(1, height - 78));
        layoutRows();
        addRenderableWidget(Button.builder(Component.translatable("controls.reset"), button -> resetAll())
                .bounds(width / 2 - 154, height - 28, 150, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> leavePage())
                .bounds(width / 2 + 4, height - 28, 150, 20).build());
    }

    private void addDoneButton() {
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> leavePage())
                .bounds(width / 2 - 100, height - 28, 200, 20).build());
    }

    private void buildCategory() {
        switch (page) {
            case GENERAL -> {
                addBoolean("liubai.configuration.general.enabled", ClientConfig.ENABLED);
                addInteger("liubai.configuration.general.targetFps", ClientConfig.TARGET_FPS, 60, 30, 240);
                addBoolean("liubai.configuration.general.adaptiveMode", ClientConfig.ADAPTIVE_MODE);
            }
            case CULLING -> {
                addBoolean("liubai.configuration.culling.entities", ClientConfig.ENTITY_CULLING);
                addBoolean("liubai.configuration.culling.blockEntities", ClientConfig.BLOCK_ENTITY_CULLING);
                addBoolean("liubai.configuration.culling.occlusion", ClientConfig.OCCLUSION_CULLING);
                addEnum("liubai.configuration.culling.occlusionMode", ClientConfig.OCCLUSION_MODE);
                addDouble("liubai.configuration.culling.safeDistance", ClientConfig.SAFE_DISTANCE, 8.0, 0.0, 64.0);
                addDouble("liubai.configuration.culling.occlusionMinDistance", ClientConfig.OCCLUSION_MIN_DISTANCE, 4.0, 4.0, 128.0);
                addInteger("liubai.configuration.culling.checksPerFrame", ClientConfig.OCCLUSION_CHECKS_PER_FRAME, 32, 0, 512);
                addInteger("liubai.configuration.culling.budgetMicros", ClientConfig.OCCLUSION_BUDGET_MICROS, 900, 100, 10000);
                addInteger("liubai.configuration.culling.occludedConfirmations", ClientConfig.OCCLUDED_CONFIRMATIONS, 2, 1, 8);
                addInteger("liubai.configuration.culling.cacheTtlFrames", ClientConfig.CACHE_TTL_FRAMES, 30, 2, 600);
                addDouble("liubai.configuration.culling.minimumProjectedRadius", ClientConfig.MIN_PROJECTED_RADIUS, 1.25, 0.0, 16.0);
            }
            case LOD -> {
                addBoolean("liubai.configuration.lod.screenSpace", ClientConfig.SCREEN_SPACE_LOD);
                addBoolean("liubai.configuration.lod.temporal", ClientConfig.TEMPORAL_LOD);
                addInteger("liubai.configuration.lod.maxTemporalInterval", ClientConfig.MAX_TEMPORAL_INTERVAL, 8, 1, 31);
                addBoolean("liubai.configuration.lod.denseVanillaEntities", ClientConfig.DENSE_VANILLA_ENTITIES);
                addDouble("liubai.configuration.lod.denseEntityDistance", ClientConfig.DENSE_ENTITY_DISTANCE, 32.0, 16.0, 128.0);
                addInteger("liubai.configuration.lod.denseEntityHighLimit", ClientConfig.DENSE_ENTITY_HIGH_LIMIT, 24, 1, 256);
                addInteger("liubai.configuration.lod.denseEntityCriticalLimit", ClientConfig.DENSE_ENTITY_CRITICAL_LIMIT, 12, 1, 256);
            }
            case EFFECTS -> {
                addBoolean("liubai.configuration.effects.reduceEntityShadows", ClientConfig.REDUCE_SHADOWS);
                addDouble("liubai.configuration.effects.shadowDistance", ClientConfig.SHADOW_DISTANCE, 24.0, 0.0, 128.0);
                addBoolean("liubai.configuration.effects.reduceNameTags", ClientConfig.REDUCE_NAME_TAGS);
                addDouble("liubai.configuration.effects.nameTagDistance", ClientConfig.NAME_TAG_DISTANCE, 48.0, 8.0, 256.0);
                addBoolean("liubai.configuration.effects.reduceParticles", ClientConfig.REDUCE_PARTICLES);
                addDouble("liubai.configuration.effects.particleDistance", ClientConfig.PARTICLE_DISTANCE, 32.0, 4.0, 256.0);
                addInteger("liubai.configuration.effects.particleSoftLimit", ClientConfig.PARTICLE_SOFT_LIMIT, 4096, 256, 32768);
                addInteger("liubai.configuration.effects.particleHighBudget", ClientConfig.PARTICLE_HIGH_BUDGET, 256, 16, 4096);
                addInteger("liubai.configuration.effects.particleCriticalBudget", ClientConfig.PARTICLE_CRITICAL_BUDGET, 128, 8, 4096);
            }
            case FLYWHEEL -> {
                addBoolean("liubai.configuration.flywheel.adaptiveLimiter", ClientConfig.FLYWHEEL_ADAPTIVE_LIMITER);
                addInteger("liubai.configuration.flywheel.highMultiplier", ClientConfig.FLYWHEEL_HIGH_MULTIPLIER, 2, 1, 8);
                addInteger("liubai.configuration.flywheel.criticalMultiplier", ClientConfig.FLYWHEEL_CRITICAL_MULTIPLIER, 3, 1, 12);
            }
            case COMPATIBILITY -> {
                addList("liubai.configuration.compatibility.entityAllowlist", ClientConfig.ENTITY_ALLOWLIST,
                        List.of("minecraft:player", "minecraft:ender_dragon", "minecraft:wither", "minecraft:tnt", "minecraft:leash_knot", "minecraft:lightning_bolt"),
                        value -> value.matches("[a-z0-9_.-]+:[a-z0-9_./-]+"));
                addList("liubai.configuration.compatibility.blockEntityAllowlist", ClientConfig.BLOCK_ENTITY_ALLOWLIST,
                        List.of("minecraft:beacon", "minecraft:end_gateway", "minecraft:end_portal", "minecraft:structure_block", "minecraft:conduit"),
                        value -> value.matches("[a-z0-9_.-]+:[a-z0-9_./-]+"));
                addList("liubai.configuration.compatibility.disabledNamespaces", ClientConfig.DISABLED_NAMESPACES,
                        List.of("create", "flywheel"), value -> value.matches("[a-z0-9_.-]+"));
            }
            case DEBUG -> addBoolean("liubai.configuration.debug.showHud", ClientConfig.SHOW_HUD);
            default -> throw new IllegalStateException("Not a category: " + page);
        }
    }

    private void addBoolean(String key, ForgeConfigSpec.BooleanValue value) {
        Button button = Button.builder(booleanValue(value.get()), pressed -> {
            value.set(!value.get());
            pressed.setMessage(booleanValue(value.get()));
            refreshRuntimeConfig();
        }).bounds(0, 0, 155, 20).tooltip(tooltip(key)).build();
        rows.add(new Row(Component.translatable(key), button, () -> { }, () -> {
            value.set((Boolean) value.getDefault());
            button.setMessage(booleanValue(value.get()));
        }));
    }

    private void addEnum(String key, ForgeConfigSpec.EnumValue<OcclusionMode> value) {
        Button button = Button.builder(value.get().getTranslatedName(), pressed -> {
            OcclusionMode[] modes = OcclusionMode.values();
            value.set(modes[(value.get().ordinal() + 1) % modes.length]);
            pressed.setMessage(value.get().getTranslatedName());
            refreshRuntimeConfig();
        }).bounds(0, 0, 155, 20).tooltip(tooltip(key)).build();
        rows.add(new Row(Component.translatable(key), button, () -> { }, () -> {
            value.set(OcclusionMode.AUTO);
            button.setMessage(value.get().getTranslatedName());
        }));
    }

    private void addInteger(String key, ForgeConfigSpec.IntValue value, int defaultValue, int min, int max) {
        addText(key, () -> Integer.toString(value.get()), text -> {
            int parsed = Integer.parseInt(text.trim());
            if (parsed < min || parsed > max) throw new IllegalArgumentException();
            value.set(parsed);
        }, Integer.toString(defaultValue));
    }

    private void addDouble(String key, ForgeConfigSpec.DoubleValue value, double defaultValue, double min, double max) {
        addText(key, () -> formatDouble(value.get()), text -> {
            double parsed = Double.parseDouble(text.trim());
            if (!Double.isFinite(parsed) || parsed < min || parsed > max) throw new IllegalArgumentException();
            value.set(parsed);
        }, formatDouble(defaultValue));
    }

    private void addList(String key, ForgeConfigSpec.ConfigValue<List<? extends String>> value,
                         List<String> defaults, Predicate<String> validator) {
        Button button = Button.builder(Component.translatable(key + ".button"), pressed -> {
            commitRows();
            minecraft.setScreen(new ListEditorScreen(this, key, value, defaults, validator));
        }).bounds(0, 0, 155, 20).tooltip(tooltip(key)).build();
        rows.add(new Row(Component.translatable(key), button, () -> { }, () -> value.set(new ArrayList<>(defaults))));
    }

    private void addText(String key, Supplier<String> current, Consumer<String> setter, String defaultValue) {
        EditBox field = new EditBox(font, 0, 0, 155, 20, Component.translatable(key));
        field.setValue(current.get());
        field.setTooltip(tooltip(key));
        Runnable commit = () -> {
            try {
                setter.accept(field.getValue());
                field.setTextColor(0xE0E0E0);
            } catch (IllegalArgumentException ignored) {
                field.setValue(current.get());
                field.setTextColor(0xFF5555);
            }
        };
        rows.add(new Row(Component.translatable(key), field, commit, () -> field.setValue(defaultValue)));
    }

    private static Tooltip tooltip(String key) {
        return Tooltip.create(Component.translatable(key + ".tooltip"));
    }

    private static Component booleanValue(boolean enabled) {
        return Component.translatable(enabled ? "options.on" : "options.off");
    }

    private static String formatDouble(double value) {
        return String.format(Locale.ROOT, "%s", value);
    }

    private void layoutRows() {
        int baseY = 34 - scroll;
        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i);
            int y = baseY + i * ROW_HEIGHT;
            row.widget.setX(width / 2);
            row.widget.setY(y);
            row.widget.visible = y >= 30 && y <= height - 50;
            if (row.widget instanceof EditBox editBox) editBox.setWidth(155);
        }
    }

    private void resetAll() {
        rows.forEach(row -> row.reset.run());
        commitRows();
        refreshRuntimeConfig();
    }

    private void commitRows() {
        rows.forEach(row -> row.commit.run());
    }

    private void leavePage() {
        commitRows();
        refreshRuntimeConfig();
        if (page == Page.CATEGORIES) {
            ClientConfig.SPEC.save();
        }
        if (minecraft != null) minecraft.setScreen(parent);
    }

    private static void refreshRuntimeConfig() {
        LiubaiClientSystem.INSTANCE.refreshConfig();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (page == Page.CATEGORIES) return super.mouseScrolled(mouseX, mouseY, delta);
        scroll = Math.max(0, Math.min(maxScroll, scroll - (int) Math.signum(delta) * ROW_HEIGHT * 3));
        layoutRows();
        return true;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 10, 0xFFFFFF);
        if (page == Page.CATEGORIES) return;
        for (int i = 0; i < rows.size(); i++) {
            int y = 40 - scroll + i * ROW_HEIGHT;
            if (y < 30 || y > height - 44) continue;
            graphics.drawString(font, rows.get(i).label, width / 2 - 155, y, 0xFFFFFF);
        }
    }

    @Override
    public void onClose() {
        leavePage();
    }

    private enum Page {
        CATEGORIES("liubai.configuration.section.liubai.client.toml.title"),
        GENERAL("liubai.configuration.general"),
        CULLING("liubai.configuration.culling"),
        LOD("liubai.configuration.lod"),
        EFFECTS("liubai.configuration.effects"),
        FLYWHEEL("liubai.configuration.flywheel"),
        COMPATIBILITY("liubai.configuration.compatibility"),
        DEBUG("liubai.configuration.debug");

        private final String translationKey;

        Page(String translationKey) {
            this.translationKey = translationKey;
        }

        String translationKey() {
            return translationKey;
        }

        String titleKey() {
            return translationKey;
        }
    }

    private record Row(Component label, AbstractWidget widget, Runnable commit, Runnable reset) {
    }

    private static final class ListEditorScreen extends Screen {
        private final Screen parent;
        private final String key;
        private final ForgeConfigSpec.ConfigValue<List<? extends String>> value;
        private final List<String> defaults;
        private final Predicate<String> validator;
        private final List<String> entries;
        private final List<EditBox> fields = new ArrayList<>();
        private final List<Button> removeButtons = new ArrayList<>();
        private int scroll;
        private int maxScroll;
        private boolean invalid;

        private ListEditorScreen(Screen parent, String key,
                                 ForgeConfigSpec.ConfigValue<List<? extends String>> value,
                                 List<String> defaults, Predicate<String> validator) {
            this(parent, key, value, defaults, validator, value.get().stream().map(String::valueOf).toList());
        }

        private ListEditorScreen(Screen parent, String key,
                                 ForgeConfigSpec.ConfigValue<List<? extends String>> value,
                                 List<String> defaults, Predicate<String> validator, List<String> entries) {
            super(Component.translatable(key));
            this.parent = parent;
            this.key = key;
            this.value = value;
            this.defaults = List.copyOf(defaults);
            this.validator = validator;
            this.entries = new ArrayList<>(entries);
        }

        @Override
        protected void init() {
            fields.clear();
            removeButtons.clear();
            for (int i = 0; i < entries.size(); i++) {
                int index = i;
                EditBox field = new EditBox(font, 0, 0, 270, 20, Component.translatable(key));
                field.setValue(entries.get(i));
                field.setMaxLength(256);
                fields.add(field);
                addRenderableWidget(field);
                Button remove = Button.builder(Component.literal("-"), button -> reopenWithout(index))
                        .bounds(0, 0, 34, 20)
                        .tooltip(Tooltip.create(Component.translatable("liubai.configuration.list.remove")))
                        .build();
                removeButtons.add(remove);
                addRenderableWidget(remove);
            }
            maxScroll = Math.max(0, entries.size() * ROW_HEIGHT - Math.max(1, height - 78));
            layoutEntries();
            addRenderableWidget(Button.builder(Component.literal("+"), button -> reopenWithAdded())
                    .bounds(width / 2 - 154, height - 28, 44, 20)
                    .tooltip(Tooltip.create(Component.translatable("liubai.configuration.list.add")))
                    .build());
            addRenderableWidget(Button.builder(Component.translatable("controls.reset"), button -> reopen(defaults))
                    .bounds(width / 2 - 104, height - 28, 100, 20).build());
            addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> saveAndClose())
                    .bounds(width / 2 + 4, height - 28, 150, 20).build());
        }

        private void layoutEntries() {
            int baseY = 34 - scroll;
            for (int i = 0; i < fields.size(); i++) {
                int y = baseY + i * ROW_HEIGHT;
                EditBox field = fields.get(i);
                field.setX(width / 2 - 155);
                field.setY(y);
                field.visible = y >= 30 && y <= height - 50;
                Button remove = removeButtons.get(i);
                remove.setX(width / 2 + 121);
                remove.setY(y);
                remove.visible = field.visible;
            }
        }

        private List<String> currentEntries() {
            return fields.stream().map(EditBox::getValue).map(String::trim).toList();
        }

        private void reopenWithout(int index) {
            List<String> next = new ArrayList<>(currentEntries());
            next.remove(index);
            reopen(next);
        }

        private void reopenWithAdded() {
            List<String> next = new ArrayList<>(currentEntries());
            next.add("");
            reopen(next);
        }

        private void reopen(List<String> next) {
            minecraft.setScreen(new ListEditorScreen(parent, key, value, defaults, validator, next));
        }

        private void saveAndClose() {
            List<String> next = currentEntries();
            invalid = next.stream().anyMatch(entry -> entry.isEmpty() || !validator.test(entry));
            for (int i = 0; i < fields.size(); i++) {
                String entry = next.get(i);
                fields.get(i).setTextColor(entry.isEmpty() || !validator.test(entry) ? 0xFF5555 : 0xE0E0E0);
            }
            if (invalid) return;
            value.set(new ArrayList<>(next));
            LiubaiClientSystem.INSTANCE.refreshConfig();
            minecraft.setScreen(parent);
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
            scroll = Math.max(0, Math.min(maxScroll, scroll - (int) Math.signum(delta) * ROW_HEIGHT * 3));
            layoutEntries();
            return true;
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            renderBackground(graphics);
            super.render(graphics, mouseX, mouseY, partialTick);
            graphics.drawCenteredString(font, title, width / 2, 10, 0xFFFFFF);
            if (invalid) {
                graphics.drawCenteredString(font, Component.translatable("liubai.configuration.list.invalid"),
                        width / 2, height - 40, 0xFF5555);
            }
        }

        @Override
        public void onClose() {
            minecraft.setScreen(parent);
        }
    }
}
