package de.ownmods.settings.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

abstract class PagedScreen extends Screen {
    protected final Screen parent;
    protected int page;
    protected int pageCount = 1;
    protected int rows;
    protected int left;
    protected int contentWidth;
    protected String subtitle = "";
    protected String problem = "";

    protected PagedScreen(Component title, Screen parent, int page) {
        super(title);
        this.parent = parent;
        this.page = page;
    }
    protected void layoutFor(int entries) {
        contentWidth = Math.max(100, Math.min(380, width - 24));
        left = (width - contentWidth) / 2;
        rows = Math.max(1, (height - 124) / 24);
        pageCount = Math.max(1, (entries + rows - 1) / rows);
        page = Math.max(0, Math.min(page, pageCount - 1));
    }
    protected int rowY(int row) { return 50 + row * 24; }
    protected void navigation(java.util.function.IntConsumer openPage) {
        var previous = addRenderableWidget(Button.builder(Component.literal("<"),
                b -> openPage.accept(page - 1)).bounds(left, height - 54, 35, 20).build());
        previous.active = page > 0;
        var next = addRenderableWidget(Button.builder(Component.literal(">"),
                b -> openPage.accept(page + 1)).bounds(left + contentWidth - 35, height - 54, 35, 20).build());
        next.active = page < pageCount - 1;
    }
    protected boolean remoteWorld() {
        return minecraft.level != null && minecraft.getSingleplayerServer() == null;
    }
    @Override public void onClose() { minecraft.gui.setScreen(parent); }
    @Override public boolean isPauseScreen() { return true; }
    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        renderPageChrome(graphics, mouseX, mouseY, delta);
    }
    protected void renderPageChrome(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        centered(graphics, title.getString(), 16, 0xFFFFFFFF);
        centered(graphics, subtitle, 33, 0xFFAAAAAA);
        if (pageCount > 1) centered(graphics, (page + 1) + " / " + pageCount, height - 47, 0xFFAAAAAA);
        if (!problem.isBlank()) centered(graphics, problem, height - 66, 0xFFFF7777);
    }
    private void centered(GuiGraphicsExtractor graphics, String text, int y, int color) {
        // Trim informational labels; the full description remains available in tooltips.
        String fitted = font.plainSubstrByWidth(text, Math.max(20, width - 20));
        graphics.text(font, fitted, (width - font.width(fitted)) / 2, y, color, true);
    }
}
