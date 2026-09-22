package de.ownmods.settings.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** Non-interactive translucent panel used by the OwnMods menu theme. */
final class PanelWidget extends Button.Plain {
    PanelWidget(int x, int y, int width, int height) {
        super(x, y, width, height, Component.empty(), b -> { }, DEFAULT_NARRATION);
        active = false;
    }

    @Override protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();
        graphics.fill(x, y, x + w, y + h, 0xD81B1B1F);
        graphics.outline(x, y, w, h, 0xFF6D6D73);
        if (w > 4 && h > 4) graphics.outline(x + 2, y + 2, w - 4, h - 4, 0x5038383E);
    }
}
