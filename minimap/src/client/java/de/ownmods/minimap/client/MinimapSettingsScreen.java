package de.ownmods.minimap.client;
import de.ownmods.settings.client.UiText;

import de.ownmods.minimap.MinimapSettings;
import de.ownmods.settings.client.OwnModsUi;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/** Notes-style Minimap configuration with a persistent left navigation and uncluttered pages. */
public final class MinimapSettingsScreen extends Screen {
    private enum Tab { DISPLAY, MAP, INFO, MARKER }

    private final Screen parent;
    private final MinimapSettings settings;
    private final LinkedHashMap<String, Boolean> flags;
    private final LinkedHashMap<String, String> choices;
    private Tab tab = Tab.DISPLAY;
    private int left, top, wide, tall, sidebarW, bodyY, bodyH, contentX, contentY, contentW, contentH;
    private String problem = "";

    public MinimapSettingsScreen(Screen parent) {
        super(Component.literal(UiText.tr("vr.text.4bad6cff07d2")));
        this.parent = parent;
        this.settings = MinimapClient.settings;
        this.flags = new LinkedHashMap<>(settings.snapshot());
        this.choices = new LinkedHashMap<>(settings.choiceSnapshot());
    }

    @Override protected void init() {
        wide = Math.min(760, Math.max(320, width - 20));
        tall = Math.min(440, Math.max(260, height - 20));
        left = (width - wide) / 2;
        top = (height - tall) / 2;
        sidebarW = Math.max(142, Math.min(158, wide / 4));
        bodyY = top + 40;
        bodyH = tall - 43;
        contentX = left + sidebarW + 8;
        contentY = bodyY + 8;
        contentW = wide - sidebarW - 16;
        contentH = bodyH - 16;

        addRenderableWidget(new OwnModsUi.Surface(left, top, wide, tall,
                g -> OwnModsUi.window(g, left, top, wide, tall)));
        addRenderableWidget(new OwnModsUi.Surface(left + 3, bodyY, sidebarW - 3, bodyH,
                g -> OwnModsUi.sidebar(g, left + 3, bodyY, sidebarW - 3, bodyH)));
        addRenderableWidget(new OwnModsUi.Surface(contentX, contentY, contentW, contentH,
                g -> OwnModsUi.content(g, contentX, contentY, contentW, contentH)));
        addRenderableWidget(new OwnModsUi.Surface(left + 3, top + 3, wide - 6, 36, this::drawHeader));

        buildSidebar();
        switch (tab) {
            case DISPLAY -> buildDisplay();
            case MAP -> buildMap();
            case INFO -> buildInfo();
            case MARKER -> buildMarker();
        }
        buildFooter();
    }

    private void drawHeader(GuiGraphicsExtractor g) {
        OwnModsUi.icon(g, "minecraft:filled_map", left + 13, top + 9, 22);
        g.text(minecraft.font, UiText.tr("vr.text.4bad6cff07d2"), left + 43, top + 11, OwnModsUi.TEXT, false);
        OwnModsUi.smallText(g, UiText.tr("vr.text.03bc644b1594"), left + 43, top + 24,
                OwnModsUi.MUTED, wide - 76);
    }

    private void buildSidebar() {
        int x = left + 6;
        int y = bodyY + 10;
        int w = sidebarW - 9;
        int h = 34;
        int gap = 3;
        sidebarTab(x, y, w, h, "minecraft:filled_map", UiText.tr("vr.text.2e2f6d648fdd"), OwnModsUi.SELECTED, Tab.DISPLAY); y += h + gap;
        sidebarTab(x, y, w, h, "minecraft:compass", UiText.tr("vr.text.48906bf1c741"), OwnModsUi.BLUE, Tab.MAP); y += h + gap;
        sidebarTab(x, y, w, h, "minecraft:spyglass", UiText.tr("vr.text.b6b30fdd5998"), OwnModsUi.BRASS, Tab.INFO); y += h + gap;
        sidebarTab(x, y, w, h, "minecraft:lodestone", UiText.tr("vr.text.44b949fa4efe"), OwnModsUi.PURPLE, Tab.MARKER);

        addRenderableWidget(new OwnModsUi.ActionButton(x, top + tall - 34, w, 24,
                "minecraft:barrier", UiText.tr("vr.text.548611ce58f7"), 0xFF7C7165, true, this::onClose));
    }

    private void sidebarTab(int x, int y, int w, int h, String icon, String label, int accent, Tab value) {
        addRenderableWidget(new OwnModsUi.SidebarButton(x, y, w, h, icon, label, accent,
                () -> tab == value, () -> switchTab(value)));
    }

    private void buildDisplay() {
        int x = contentX + 12;
        int w = contentW - 24;
        int y = contentY + 14;
        heading(UiText.tr("vr.text.2e2f6d648fdd"), UiText.tr("vr.text.1f2c3ccddc16"), x, y, w);
        y += 34;

        addToggle(x, y, w, "minecraft:filled_map", UiText.tr("vr.text.7ec06c9cb270"), OwnModsUi.GREEN, "enabled");
        y += 38;

        section(UiText.tr("vr.text.6d031af10da7"), x, y); y += 16;
        int gap = 5;
        int half = (w - gap) / 2;
        addChoice(x, y, half, "minecraft:map", UiText.tr("vr.text.6971c90fc33b"), OwnModsUi.SELECTED, "position", "left");
        addChoice(x + half + gap, y, w - half - gap, "minecraft:filled_map", UiText.tr("vr.text.68a7c031f040"), OwnModsUi.SELECTED, "position", "right");
        y += 29;
        addChoice(x, y, half, "minecraft:compass", UiText.tr("vr.text.dcb7e2efb17b"), OwnModsUi.SELECTED, "position", "bottom_left");
        addChoice(x + half + gap, y, w - half - gap, "minecraft:recovery_compass", UiText.tr("vr.text.980ffaff8063"), OwnModsUi.SELECTED, "position", "bottom_right");
        y += 38;

        section(UiText.tr("vr.text.aedc3f80989a"), x, y); y += 16;
        int third = (w - gap * 2) / 3;
        addChoice(x, y, third, "minecraft:paper", UiText.tr("vr.text.d3e963a0a44e"), OwnModsUi.BRASS, "size", "small");
        addChoice(x + third + gap, y, third, "minecraft:map", UiText.tr("vr.text.a7248eeb45eb"), OwnModsUi.BRASS, "size", "medium");
        addChoice(x + (third + gap) * 2, y, w - third * 2 - gap * 2,
                "minecraft:filled_map", UiText.tr("vr.text.251097f33d8d"), OwnModsUi.BRASS, "size", "large");
        y += 38;

        section(UiText.tr("vr.text.2e0e960ab320"), x, y); y += 16;
        addChoice(x, y, half, "minecraft:clock", UiText.tr("vr.text.5fd1b9f20c91"), OwnModsUi.SELECTED, "shape", "circle");
        addChoice(x + half + gap, y, w - half - gap, "minecraft:item_frame", UiText.tr("vr.text.26d97352d605"), OwnModsUi.SELECTED, "shape", "square");
    }

    private void buildMap() {
        int x = contentX + 12;
        int w = contentW - 24;
        int y = contentY + 14;
        heading(UiText.tr("vr.text.48906bf1c741"), UiText.tr("vr.text.41376a911f0c"), x, y, w);
        y += 38;

        section(UiText.tr("vr.text.6ac5c56f9fc4"), x, y); y += 16;
        int gap = 5;
        int half = (w - gap) / 2;
        addChoice(x, y, half, "minecraft:compass", UiText.tr("vr.text.2ec466a3fd59"), OwnModsUi.GREEN, "rotation", "north_up");
        addChoice(x + half + gap, y, w - half - gap, "minecraft:recovery_compass", UiText.tr("vr.text.fab5df21fbdb"), OwnModsUi.GREEN, "rotation", "player_up");
        y += 42;

        section(UiText.tr("vr.text.c32c4866dcab"), x, y); y += 16;
        int quarter = (w - gap * 3) / 4;
        addChoice(x, y, quarter, "minecraft:map", "1×", OwnModsUi.BLUE, "zoom", "one");
        addChoice(x + quarter + gap, y, quarter, "minecraft:paper", "2×", OwnModsUi.BLUE, "zoom", "two",
                UiText.tr("vr.text.aefd15c3c019"));
        addChoice(x + (quarter + gap) * 2, y, quarter, "minecraft:spyglass", "4×", OwnModsUi.BLUE, "zoom", "four",
                UiText.tr("vr.text.3ca23796d52a"));
        addChoice(x + (quarter + gap) * 3, y, w - quarter * 3 - gap * 3,
                "minecraft:filled_map", "8×", OwnModsUi.BLUE, "zoom", "eight",
                UiText.tr("vr.text.a8af7ebb2966"));
        y += 32;

        final int warningY = y;
        addRenderableWidget(new OwnModsUi.Surface(x, warningY, w, 28, g -> {
            boolean high = !"one".equals(choices.get("zoom"));
            int accent = high ? OwnModsUi.GOLD : OwnModsUi.BODY_MUTED;
            OwnModsUi.smallText(g, high ? UiText.tr("vr.text.7d85583057f1") :
                    UiText.tr("vr.text.d4eae7861d2d"), x, warningY + 2, accent, w);
            OwnModsUi.smallText(g, UiText.tr("vr.text.9fc99694153b"),
                    x, warningY + 14, OwnModsUi.BODY_MUTED, w);
        }));
        y += 34;

        addToggle(x, y, w, "minecraft:elytra", UiText.tr("vr.text.6ec39307f62f"), OwnModsUi.BLUE, "auto_zoom");
        y += 44;
        final int revealY = y;
        addRenderableWidget(new OwnModsUi.Surface(x, revealY, w, 44, g -> {
            OwnModsUi.smallText(g, UiText.tr("vr.text.b0bab0525146"), x, revealY, OwnModsUi.BODY_TEXT, w);
            OwnModsUi.smallText(g, UiText.tr("vr.text.cc0c248c2fcf"),
                    x, revealY + 14, OwnModsUi.BODY_MUTED, w);
            OwnModsUi.smallText(g, UiText.tr("vr.text.27c409849526"),
                    x, revealY + 26, OwnModsUi.BODY_MUTED, w);
        }));
    }

    private void buildInfo() {
        int x = contentX + 12;
        int w = contentW - 24;
        int y = contentY + 14;
        heading(UiText.tr("vr.text.b6b30fdd5998"), UiText.tr("vr.text.0c99871f3ea2"), x, y, w);
        y += 38;
        addToggle(x, y, w, "minecraft:compass", UiText.tr("vr.text.949725ff2e0c"), OwnModsUi.SELECTED, "coordinates"); y += 36;
        addToggle(x, y, w, "minecraft:stone", UiText.tr("vr.text.cbfac5705c05"), 0xFF8A8176, "height"); y += 36;
        addToggle(x, y, w, "minecraft:grass_block", UiText.tr("vr.text.d0d55f76c380"), 0xFF6FA558, "biome"); y += 36;
        addToggle(x, y, w, "minecraft:ender_eye", UiText.tr("vr.text.0bc70684cf30"), OwnModsUi.PURPLE, "dimension"); y += 36;
        addToggle(x, y, w, "minecraft:recovery_compass", UiText.tr("vr.text.70012e502bda"), OwnModsUi.GOLD, "direction");
    }

    private void buildMarker() {
        int x = contentX + 12;
        int w = contentW - 24;
        int y = contentY + 14;
        heading(UiText.tr("vr.text.44b949fa4efe"), UiText.tr("vr.text.3f23e43fe036"), x, y, w);
        y += 38;

        int gap = 8;
        int half = (w - gap) / 2;
        int rightX = x + half + gap;
        int rightW = w - half - gap;

        section(UiText.tr("vr.text.c7860048458f"), x, y);
        section(UiText.tr("vr.text.1da7a3b4eea2"), rightX, y);
        y += 17;

        int leftY = y;
        addToggle(x, leftY, half, "minecraft:lodestone", UiText.tr("vr.text.2cdbb170d53e"), OwnModsUi.SELECTED, "waypoints"); leftY += 36;
        addToggle(x, leftY, half, "minecraft:writable_book", UiText.tr("vr.text.9041f5f764ba"), OwnModsUi.PURPLE, "note_locations"); leftY += 36;
        addToggle(x, leftY, half, "minecraft:skeleton_skull", UiText.tr("vr.text.d7ae7194a77f"), OwnModsUi.RED, "deathpoints"); leftY += 36;
        addToggle(x, leftY, half, "minecraft:player_head", UiText.tr("vr.text.e312a9c1e332"), OwnModsUi.BLUE, "players");

        int rightY = y;
        addToggle(rightX, rightY, rightW, "minecraft:name_tag", UiText.tr("vr.text.b9a636b7f1b4"), OwnModsUi.BRASS, "names"); rightY += 36;
        addToggle(rightX, rightY, rightW, "minecraft:spyglass", UiText.tr("vr.text.8a0c349fc7c6"), OwnModsUi.GOLD, "distance"); rightY += 36;
        addToggle(rightX, rightY, rightW, "minecraft:obsidian", UiText.tr("vr.text.b353b4fe639d"), OwnModsUi.PURPLE, "portal_projection");
    }

    private void heading(String title, String subtitle, int x, int y, int w) {
        addRenderableWidget(new OwnModsUi.Surface(x, y, w, 30, g -> {
            OwnModsUi.smallText(g, title, x, y, OwnModsUi.BODY_TEXT, w);
            OwnModsUi.smallText(g, subtitle, x, y + 13, OwnModsUi.BODY_MUTED, w);
        }));
    }

    private void section(String title, int x, int y) {
        addRenderableWidget(new OwnModsUi.Surface(x, y, 180, 14,
                g -> OwnModsUi.smallText(g, title, x, y + 1, OwnModsUi.BODY_MUTED, 180)));
    }

    private void addToggle(int x, int y, int w, String icon, String label, int accent, String key) {
        addRenderableWidget(new OwnModsUi.ToggleRow(x, y, w, 30, icon, label, accent,
                () -> flag(key), () -> toggle(key), label));
    }

    private void addChoice(int x, int y, int w, String icon, String label, int accent, String key, String value) {
        addChoice(x, y, w, icon, label, accent, key, value, label);
    }

    private void addChoice(int x, int y, int w, String icon, String label, int accent,
                           String key, String value, String tooltip) {
        addRenderableWidget(new OwnModsUi.ChoiceButton(x, y, w, 24, icon, label, accent,
                () -> value.equals(choices.get(key)), () -> { choices.put(key, value); rebuildCurrentTab(); }, tooltip));
    }

    private void rebuildCurrentTab() {
        clearWidgets();
        init();
    }

    private void buildFooter() {
        int y = contentY + contentH - 32;
        int w = Math.min(190, Math.max(130, (contentW - 24) / 2));
        int x = contentX + contentW - 12 - w;
        addRenderableWidget(new OwnModsUi.ActionButton(x, y, w, 24,
                "minecraft:lime_dye", UiText.tr("vr.text.f6b2ff39f540"), OwnModsUi.GREEN, false, this::save));
        if (!problem.isBlank()) {
            addRenderableWidget(new OwnModsUi.Surface(contentX + 12, y + 4, Math.max(20, x - contentX - 20), 18,
                    g -> OwnModsUi.smallText(g, problem, contentX + 12, y + 7, OwnModsUi.RED,
                            Math.max(20, x - contentX - 20))));
        }
    }

    private boolean flag(String key) { return flags.getOrDefault(key, false); }
    private void toggle(String key) { flags.put(key, !flag(key)); }

    private void switchTab(Tab next) {
        if (tab == next) return;
        tab = next;
        clearWidgets();
        init();
    }

    private void save() {
        try {
            settings.save(Map.copyOf(flags), Map.copyOf(choices));
            minecraft.gui.setScreen(parent);
        } catch (IOException | RuntimeException e) {
            problem = UiText.tr("vr.text.6ad2ee31d2ba");
            clearWidgets();
            init();
        }
    }

    @Override public void onClose() { minecraft.gui.setScreen(parent); }
    @Override public boolean isPauseScreen() { return true; }
}
