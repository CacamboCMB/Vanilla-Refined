package de.ownmods.settings.client;

import java.util.function.BooleanSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Dashboard tile matching the visual language of the supplied OwnMods reference. */
final class ModTileButton extends Button.Plain {
    private final ItemStack icon;
    private final Component name;
    private final Component description;
    private final BooleanSupplier enabled;

    ModTileButton(int x, int y, int width, int height, String itemId, Component name,
                  Component description, BooleanSupplier enabled, Runnable open) {
        super(x, y, width, height, name, b -> open.run(), DEFAULT_NARRATION);
        this.icon = resolve(itemId);
        this.name = name;
        this.description = description;
        this.enabled = enabled;
        setTooltip(Tooltip.create(name.copy().append("\n").append(description)));
    }

    private static ItemStack resolve(String itemId) {
        var item = BuiltInRegistries.ITEM.getValue(Identifier.parse(itemId));
        return new ItemStack(item == null || item == Items.AIR ? Items.BARRIER : item);
    }

    @Override protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();
        int fill = !active ? 0xB82A2A2E : isHovered() ? 0xEE54545A : 0xDC3A3A40;
        graphics.fill(x, y, x + w, y + h, fill);
        graphics.outline(x, y, w, h, active ? 0xFF85858B : 0xFF505055);
        if (isFocused()) graphics.outline(x + 2, y + 2, w - 4, h - 4, 0xFFFFFFFF);

        float scale = 2.0f;
        float iconWidth = 16.0f * scale;
        graphics.pose().pushMatrix();
        try {
            graphics.pose().translate(x + (w - iconWidth) / 2.0f, y + 10.0f);
            graphics.pose().scale(scale, scale);
            graphics.fakeItem(icon, 0, 0);
        } finally { graphics.pose().popMatrix(); }
        graphics.nextStratum();

        var font = Minecraft.getInstance().font;
        String title = font.plainSubstrByWidth(name.getString(), Math.max(8, w - 8));
        graphics.text(font, title, x + (w - font.width(title)) / 2, y + h - 30, 0xFFFFFFFF, true);
        Component state = Component.translatable(enabled.getAsBoolean() ? "ownmods.active" : "ownmods.inactive");
        int color = enabled.getAsBoolean() ? 0xFF64E45D : 0xFFE58C8C;
        String stateText = state.getString();
        graphics.text(font, stateText, x + (w - font.width(stateText)) / 2, y + h - 16, color, true);
    }
}
