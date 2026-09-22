package de.ownmods.settings.client;

import de.ownmods.settings.SettingsMod;
import de.ownmods.settings.api.OptionIcon;
import de.ownmods.settings.api.ToggleOption;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** One screenshot-style setting row: item icon, label, An/Aus state and chevron. */
final class OptionRowButton extends Button.Plain {
    private final ToggleOption option;
    private final Map<String, Boolean> draft;
    private final ItemStack icon;

    OptionRowButton(int x, int y, int width, int height, ToggleOption option,
                    OptionIcon appearance, Map<String, Boolean> draft) {
        super(x, y, width, height, Component.empty(), b -> {
            var self = (OptionRowButton) b;
            self.draft.put(self.option.key(), !self.selected());
            self.refreshAccessibleText();
        }, DEFAULT_NARRATION);
        this.option = option;
        this.draft = draft;
        this.icon = resolve(appearance.itemId());
        refreshAccessibleText();
    }

    private static ItemStack resolve(String itemId) {
        var item = BuiltInRegistries.ITEM.getValue(Identifier.parse(itemId));
        if (item == null || item == Items.AIR) {
            SettingsMod.LOGGER.warn("Unknown settings icon: {}", itemId);
            return new ItemStack(Items.BARRIER);
        }
        return new ItemStack(item);
    }

    private boolean selected() { return draft.getOrDefault(option.key(), option.defaultValue()); }

    private void refreshAccessibleText() {
        var label = Component.translatable(option.labelKey()).append(": ")
                .append(Component.translatable(selected() ? "ownmods.on" : "ownmods.off"));
        setMessage(label);
        setTooltip(Tooltip.create(label.copy().append("\n").append(Component.translatable(option.tooltipKey()))));
    }

    @Override protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();
        int fill = !active ? 0xB82B2B2E : isHovered() ? 0xF05A5A60 : 0xE548484D;
        graphics.fill(x, y, x + w, y + h, fill);
        graphics.outline(x, y, w, h, !active ? 0xFF55555A : 0xFF707077);
        if (isFocused()) graphics.outline(x + 1, y + 1, w - 2, h - 2, 0xFFFFFFFF);

        float scale = h >= 22 ? 1.0f : 0.875f;
        graphics.pose().pushMatrix();
        try {
            graphics.pose().translate(x + 6.0f, y + Math.max(2.0f, (h - 16.0f * scale) / 2.0f));
            graphics.pose().scale(scale, scale);
            graphics.fakeItem(icon, 0, 0);
        } finally { graphics.pose().popMatrix(); }
        graphics.nextStratum();

        var font = Minecraft.getInstance().font;
        int toggleWidth = 42;
        int toggleHeight = Math.min(16, h - 4);
        int toggleX = x + w - toggleWidth - 20;
        int toggleY = y + (h - toggleHeight) / 2;
        int labelX = x + 28;
        int labelMax = Math.max(20, toggleX - labelX - 6);
        String label = font.plainSubstrByWidth(Component.translatable(option.labelKey()).getString(), labelMax);
        graphics.text(font, label, labelX, y + (h - 8) / 2, active ? 0xFFFFFFFF : 0xFF9A9A9A, true);

        boolean on = selected();
        int stateFill = !active ? 0xFF3C3C40 : on ? 0xFF15751B : 0xFF444449;
        int stateBorder = !active ? 0xFF5A5A5F : on ? 0xFF2CA634 : 0xFF67676C;
        graphics.fill(toggleX, toggleY, toggleX + toggleWidth, toggleY + toggleHeight, stateFill);
        graphics.outline(toggleX, toggleY, toggleWidth, toggleHeight, stateBorder);
        String state = Component.translatable(on ? "ownmods.on" : "ownmods.off").getString();
        graphics.text(font, state, toggleX + (toggleWidth - font.width(state)) / 2,
                toggleY + (toggleHeight - 8) / 2, active ? 0xFFFFFFFF : 0xFFAAAAAA, true);

        String arrow = ">";
        graphics.text(font, arrow, x + w - 11, y + (h - 8) / 2, 0xFFE5E5E5, true);
    }
}
