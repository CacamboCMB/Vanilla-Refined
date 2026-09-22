package de.ownmods.settings.client;

import de.ownmods.settings.SettingsMod;
import de.ownmods.settings.api.ChoiceOption;
import de.ownmods.settings.api.ManagedMod;
import de.ownmods.settings.api.ToggleOption;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Unified Notes-style configuration screen for ordinary OwnMods modules.
 * Small modules remain one clear page; larger modules page only the option list.
 */
final class IconSettingsScreen extends Screen {
    private record Entry(boolean choice, int index) {}

    private final Screen parent;
    private final ManagedMod mod;
    private final LinkedHashMap<String, Boolean> draft;
    private final LinkedHashMap<String, String> choiceDraft;
    private int page;
    private int pageCount = 1;
    private int left, top, wide, tall, sidebarW, bodyY, bodyH, contentX, contentW;
    private String problem = "";
    private int shownRevision;

    IconSettingsScreen(Screen parent, ManagedMod mod) {
        this(parent, mod, new LinkedHashMap<>(mod.snapshot()), new LinkedHashMap<>(mod.choiceSnapshot()), 0);
    }

    private IconSettingsScreen(Screen parent, ManagedMod mod, LinkedHashMap<String, Boolean> draft,
                               LinkedHashMap<String, String> choiceDraft, int page) {
        super(Component.translatable(mod.titleKey()));
        this.parent = parent;
        this.mod = mod;
        this.draft = draft;
        this.choiceDraft = choiceDraft;
        this.page = Math.max(0, page);
    }

    @Override protected void init() {
        clearWidgets();
        shownRevision = ServerSettingsClient.revision();
        if (width < 320 || height < 240) {
            addRenderableWidget(new OwnModsUi.Surface(4, 4, Math.max(1, width-8), Math.max(1,height-8), g -> {
                OwnModsUi.window(g,4,4,Math.max(1,width-8),Math.max(1,height-8));
                OwnModsUi.label(g,UiText.tr("vr.ui.resize"),12,54,Math.max(1,width-24),OwnModsUi.BODY_TEXT);
            }));
            addRenderableWidget(new OwnModsUi.ActionButton(12,Math.max(80,height-36),Math.max(1,width-24),24,
                    "minecraft:arrow",UiText.tr("vr.ui.back"),OwnModsUi.BLUE,false,this::onClose));
            return;
        }
        wide = Math.min(720, Math.max(300, width - 20));
        tall = Math.min(430, Math.max(240, height - 20));
        left = (width - wide) / 2;
        top = (height - tall) / 2;
        sidebarW = Math.max(132, Math.min(154, wide / 4));
        bodyY = top + 40;
        bodyH = tall - 43;
        contentX = left + sidebarW + 8;
        contentW = wide - sidebarW - 16;

        addRenderableWidget(new OwnModsUi.Surface(left, top, wide, tall,
                g -> OwnModsUi.window(g, left, top, wide, tall)));
        addRenderableWidget(new OwnModsUi.Surface(left + 3, bodyY, sidebarW - 3, bodyH,
                g -> OwnModsUi.sidebar(g, left + 3, bodyY, sidebarW - 3, bodyH)));
        addRenderableWidget(new OwnModsUi.Surface(contentX, bodyY + 8, contentW, bodyH - 16,
                g -> OwnModsUi.content(g, contentX, bodyY + 8, contentW, bodyH - 16)));
        addRenderableWidget(new OwnModsUi.Surface(left + 3, top + 3, wide - 6, 36, this::drawHeader));

        int sideX = left + 6;
        int sideW = sidebarW - 9;
        addRenderableWidget(new OwnModsUi.SidebarButton(sideX, bodyY + 10, sideW, 34,
                mod.iconItemId(), UiText.tr("vr.text.f5750a5d7231"), OwnModsUi.SELECTED, () -> true, () -> {}));
        addRenderableWidget(new OwnModsUi.ActionButton(sideX, top + tall - 34, sideW, 24,
                "minecraft:barrier", UiText.tr("vr.text.548611ce58f7"), 0xFF7C7165, true, this::onClose));

        boolean editable = true;
        if (!mod.warning().isBlank()) problem = Component.translatable("ownmods.load_error").getString();

        int innerX = contentX + 12;
        int innerW = contentW - 24;
        int headingY = bodyY + 20;
        addRenderableWidget(new OwnModsUi.Surface(innerX, headingY, innerW, 34, g -> {
            OwnModsUi.smallText(g, UiText.tr("vr.text.f5750a5d7231"), innerX, headingY, OwnModsUi.BODY_TEXT, innerW);
            String hint = ServerSettingsClient.hint(mod);
            OwnModsUi.smallText(g, hint, innerX, headingY + 13, OwnModsUi.BODY_MUTED, innerW);
        }));

        List<Entry> entries = entries();
        int listTop = headingY + 38;
        int footerY = top + tall - 40;
        int rowH = 31;
        int gap = 5;
        int available = Math.max(rowH, footerY - listTop - 32);
        int rowsPerPage = Math.max(1, (available + gap) / (rowH + gap));
        pageCount = Math.max(1, (entries.size() + rowsPerPage - 1) / rowsPerPage);
        page = Math.min(page, pageCount - 1);

        int first = page * rowsPerPage;
        int last = Math.min(entries.size(), first + rowsPerPage);
        for (int i = first; i < last; i++) {
            Entry entry = entries.get(i);
            int y = listTop + (i - first) * (rowH + gap);
            if (entry.choice()) addChoiceRow(innerX, y, innerW, rowH, mod.choiceOptions().get(entry.index()), editable);
            else addToggleRow(innerX, y, innerW, rowH, mod.options().get(entry.index()), editable);
        }

        if (pageCount > 1) {
            int navY = footerY - 26;
            var prev = addRenderableWidget(new OwnModsUi.ActionButton(innerX, navY, 42, 21,
                    "minecraft:arrow", "<", OwnModsUi.BLUE, false,
                    () -> openPage(page - 1)));
            prev.active = page > 0;
            prev.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("vr.text.f8a752613ad3")));
            var next = addRenderableWidget(new OwnModsUi.ActionButton(innerX + innerW - 42, navY, 42, 21,
                    "minecraft:arrow", ">", OwnModsUi.BLUE, false,
                    () -> openPage(page + 1)));
            next.active = page + 1 < pageCount;
            next.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("vr.text.5b5def32655b")));
            addRenderableWidget(new OwnModsUi.Surface(innerX + 48, navY, Math.max(1, innerW - 96), 21, g -> {
                String p = (page + 1) + "/" + pageCount;
                int tw = OwnModsUi.smallWidth(p);
                OwnModsUi.smallText(g, p, innerX + (innerW - tw) / 2, navY + 6,
                        OwnModsUi.BODY_MUTED, Math.max(1, innerW - 96));
            }));
        }

        int saveW = Math.min(190, Math.max(120, innerW / 2));
        var save = addRenderableWidget(new OwnModsUi.ActionButton(innerX + innerW - saveW, footerY, saveW, 24,
                "minecraft:lime_dye", UiText.tr("vr.text.f6b2ff39f540"), OwnModsUi.GREEN, false, this::save));
        save.active = editable;
        if (!problem.isBlank()) {
            addRenderableWidget(new OwnModsUi.Surface(innerX, footerY + 4, Math.max(20, innerW - saveW - 8), 18,
                    g -> OwnModsUi.smallText(g, problem, innerX, footerY + 8, 0xFFC6534A,
                            Math.max(20, innerW - saveW - 8))));
        }
    }

    private void drawHeader(GuiGraphicsExtractor g) {
        OwnModsUi.icon(g, mod.iconItemId(), left + 13, top + 9, 22);
        String titleText = minecraft.font.plainSubstrByWidth(Component.translatable(mod.titleKey()).getString(), wide - 76);
        g.text(minecraft.font, titleText, left + 43, top + 11, OwnModsUi.TEXT, false);
        String description = Component.translatable(mod.descriptionKey()).getString();
        OwnModsUi.smallText(g, description, left + 43, top + 24, OwnModsUi.MUTED, wide - 76);
    }

    private List<Entry> entries() {
        var result = new ArrayList<Entry>();
        for (int i = 0; i < mod.options().size(); i++) result.add(new Entry(false, i));
        for (int i = 0; i < mod.choiceOptions().size(); i++) result.add(new Entry(true, i));
        return result;
    }

    private void addToggleRow(int x, int y, int w, int h, ToggleOption option, boolean editable) {
        var appearance = mod.optionIcons().get(option.key());
        String icon = appearance == null ? mod.iconItemId() : appearance.itemId();
        String label = Component.translatable(option.labelKey()).getString();
        String tooltip = Component.translatable(option.tooltipKey()).getString()
                + (ServerSettingsClient.locked(mod, option.key()) ? "\n" + UiText.tr("vr.server.fixed") : "");
        var row = addRenderableWidget(new OwnModsUi.ToggleRow(x, y, w, h, icon, label, OwnModsUi.GREEN,
                () -> ServerSettingsClient.flag(mod, option.key(), draft.getOrDefault(option.key(), option.defaultValue())),
                () -> draft.put(option.key(), !draft.getOrDefault(option.key(), option.defaultValue())), tooltip));
        row.active = editable && !ServerSettingsClient.locked(mod, option.key());
    }

    private void addChoiceRow(int x, int y, int w, int h, ChoiceOption option, boolean editable) {
        String label = Component.translatable(option.labelKey()).getString();
        String tooltip = Component.translatable(option.tooltipKey()).getString()
                + (ServerSettingsClient.locked(mod, option.key()) ? "\n" + UiText.tr("vr.server.fixed") : "");
        var row = addRenderableWidget(new OwnModsUi.ChoiceRow(x, y, w, h, label,
                () -> Component.translatable(option.value(ServerSettingsClient.choice(mod, option.key(), choiceDraft.get(option.key()))).labelKey()).getString(),
                () -> option.value(ServerSettingsClient.choice(mod, option.key(), choiceDraft.get(option.key()))).itemId(), OwnModsUi.SELECTED,
                () -> choiceDraft.put(option.key(), ServerSettingsClient.nextChoice(mod, option.key(),
                        option.values().stream().map(v -> v.id()).toList(), choiceDraft.get(option.key()))), tooltip));
        row.active = editable && !ServerSettingsClient.locked(mod, option.key());
    }

    private void openPage(int next) {
        if (next < 0 || next >= pageCount || next == page) return;
        minecraft.gui.setScreen(new IconSettingsScreen(parent, mod, draft, choiceDraft, next));
    }

    private void save() {
        try {
            mod.save(Map.copyOf(draft), Map.copyOf(choiceDraft));
            minecraft.gui.setScreen(parent);
        } catch (IOException | RuntimeException e) {
            problem = Component.translatable("ownmods.save_error").getString();
            SettingsMod.LOGGER.error("Could not save settings for {}", mod.id(), e);
            clearWidgets();
            init();
        }
    }

    @Override public void tick() {
        super.tick();
        if (shownRevision != ServerSettingsClient.revision()) init();
    }
    @Override public void onClose() { minecraft.gui.setScreen(parent); }
    @Override public boolean isPauseScreen() { return true; }
}
