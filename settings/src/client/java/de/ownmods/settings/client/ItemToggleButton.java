package de.ownmods.settings.client;

import de.ownmods.settings.SettingsMod;
import de.ownmods.settings.api.OptionIcon;
import de.ownmods.settings.api.ToggleOption;
import de.ownmods.settings.layout.IconGridLayout;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Real Minecraft item rendering; full message is retained for narration, not painted as text. */
final class ItemToggleButton extends Button.Plain {
    private final ToggleOption option;
    private final Map<String, Boolean> draft;
    private final ItemStack icon;

    ItemToggleButton(int x, int y, ToggleOption option, OptionIcon appearance, Map<String, Boolean> draft) {
        // Vanilla Button keeps mouse/keyboard activation, click sounds, focus, tooltips and narration.
        super(x, y, IconGridLayout.SIZE, IconGridLayout.SIZE, Component.empty(), b -> {
            var self = (ItemToggleButton) b;
            self.draft.put(self.option.key(), !self.selected());
            self.refreshDescription();
        }, DEFAULT_NARRATION);
        this.option = option;
        this.draft = draft;
        this.icon = resolve(appearance.itemId());
        refreshDescription();
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
    private void refreshDescription() {
        var label = Component.translatable(option.labelKey()).append(": ")
                .append(Component.translatable(selected() ? "ownmods.on" : "ownmods.off"));
        setMessage(label); // Accessible name includes the current state even though no label is drawn.
        setTooltip(Tooltip.create(label.copy().append("\n").append(Component.translatable(option.tooltipKey()))));
    }
    @Override protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();
        boolean on = selected();
        int border = !active ? 0xFF555555 : on ? 0xFF78C875 : 0xFF888888;
        int fill = !active ? 0xFF252525 : on ? 0xFF203C2B : 0xFF303034;
        if (active && isHovered()) fill = on ? 0xFF31513B : 0xFF45454B;
        graphics.fill(x, y, x + w, y + h, fill);
        graphics.outline(x, y, w, h, border);
        if (isFocused()) graphics.outline(x + 2, y + 2, w - 4, h - 4, 0xFFFFFFFF);
        // Render a 16px game model at 24px. Resource packs are respected; no texture copies shipped.
        graphics.pose().pushMatrix();
        try {
            graphics.pose().translate(x + 6.0f, y + 5.0f);
            graphics.pose().scale(1.5f, 1.5f);
            graphics.fakeItem(icon, 0, 0);
        } finally { graphics.pose().popMatrix(); }
        graphics.nextStratum(); // Keep state badges above the extracted item model.
        // Shape + colour, not colour alone: checkmark = on, cross = off.
        int sx = x + w - 12, sy = y + h - 12;
        graphics.fill(sx - 1, sy - 1, sx + 10, sy + 10, 0xEE151515);
        int mark = !active ? 0xFF888888 : on ? 0xFF91E78D : 0xFFDD8C8C;
        if (on) {
            graphics.fill(sx + 1, sy + 4, sx + 3, sy + 6, mark);
            graphics.fill(sx + 3, sy + 6, sx + 5, sy + 8, mark);
            for (int i = 0; i < 5; i++) graphics.fill(sx + 4 + i, sy + 6 - i, sx + 6 + i, sy + 8 - i, mark);
        } else {
            for (int i = 0; i < 7; i++) {
                graphics.fill(sx + 1 + i, sy + 1 + i, sx + 2 + i, sy + 2 + i, mark);
                graphics.fill(sx + 7 - i, sy + 1 + i, sx + 8 - i, sy + 2 + i, mark);
            }
        }
    }
}
