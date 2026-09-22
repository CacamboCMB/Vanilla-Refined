package de.ownmods.settings.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Shared visual language for OwnMods configuration screens.
 *
 * The palette, spacing and interaction model deliberately mirror the Notes UI:
 * dark header/sidebar, warm parchment content, one coloured active navigation item,
 * compact rows and Minecraft item icons.  Gameplay modules can reuse this without
 * depending on the Notes module itself.
 */
public final class OwnModsUi {
    private OwnModsUi() {}

    public static final int TEXT = 0xFFF7F1E5;
    public static final int MUTED = 0xFFC8C0B1;
    public static final int BODY_TEXT = 0xFF211B16;
    public static final int BODY_MUTED = 0xFF625A50;
    public static final int HEADER = 0xFF201D1B;
    public static final int HEADER_TOP = 0xFF3C342D;
    public static final int SIDEBAR = 0xFF25282D;
    public static final int SIDEBAR_DARK = 0xFF1C1F23;
    public static final int SIDEBAR_HOVER = 0xFF343A42;
    public static final int CONTENT = 0xFFC8BFAE;
    public static final int CONTENT_INNER = 0xFFD9D0C0;
    public static final int ROW = 0xFFE2D8C7;
    public static final int ROW_HOVER = 0xFFF0E6D5;
    public static final int BORDER = 0xFF171513;
    public static final int SOFT_BORDER = 0xFF6A6258;
    public static final int SELECTED = 0xFF3B84A6;
    public static final int GREEN = 0xFF4A9B82;
    public static final int BLUE = 0xFF4D86B8;
    public static final int PURPLE = 0xFF8A68B4;
    public static final int GOLD = 0xFFE0B83D;
    public static final int RED = 0xFFC6534A;
    public static final int BRASS = 0xFFC08A3A;

    private static final float SMALL_SCALE = 0.78f;

    public static void window(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x + 4, y + 5, x + w + 4, y + h + 5, 0x70000000);
        g.fill(x, y, x + w, y + h, 0xFF0E0D0C);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF5F584F);
        g.fill(x + 2, y + 2, x + w - 2, y + h - 2, CONTENT);
        g.fill(x + 3, y + 3, x + w - 3, y + 40, HEADER);
        g.fill(x + 3, y + 3, x + w - 3, y + 5, HEADER_TOP);
        g.fill(x + 3, y + 38, x + w - 3, y + 40, 0xFF0F0E0D);
        g.fill(x + 4, y + 39, x + w - 4, y + 40, 0xFF9B7638);
        g.fill(x + 3, y + h - 5, x + w - 3, y + h - 3, 0xFF73695E);
    }

    public static void sidebar(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, SIDEBAR_DARK);
        g.fill(x + 2, y, x + w, y + h, SIDEBAR);
        for (int yy = y + 6; yy < y + h; yy += 18) g.fill(x + 3, yy, x + w - 2, yy + 1, 0x0EFFFFFF);
        g.fill(x + w - 2, y, x + w, y + h, 0xFF111315);
        g.fill(x + w - 3, y, x + w - 2, y + h, 0xFF4A5057);
    }

    public static void content(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x + 2, y + 3, x + w + 2, y + h + 3, 0x38000000);
        g.fill(x, y, x + w, y + h, CONTENT_INNER);
        for (int yy = y + 10; yy < y + h - 2; yy += 20) g.fill(x + 1, yy, x + w - 1, yy + 1, 0x10FFFFFF);
        g.fill(x, y, x + w, y + 2, 0xFFF4EADB);
        g.fill(x, y, x + 2, y + h, 0xFFF4EADB);
        g.fill(x, y + h - 2, x + w, y + h, 0xFF7D7468);
        g.fill(x + w - 2, y, x + w, y + h, 0xFF7D7468);
        g.outline(x, y, w, h, 0xFF514B44);
    }

    public static void raised(GuiGraphicsExtractor g, int x, int y, int w, int h,
                              int face, int accent, boolean selected, boolean hovered) {
        int depth = selected ? 1 : 2;
        g.fill(x + depth, y + depth, x + w + depth, y + h + depth, 0x5C000000);
        g.fill(x, y, x + w, y + h, 0xFF302B26);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, blend(face, hovered ? 0xFFFFFFFF : 0xFF000000, hovered ? 12 : 0));
        g.fill(x + 2, y + 2, x + w - 2, y + 3, blend(face, 0xFFFFFFFF, 35));
        g.fill(x + 2, y + h - 3, x + w - 2, y + h - 2, blend(face, 0xFF000000, 26));
        if (accent != 0) {
            g.fill(x + 2, y + 2, x + w - 2, y + 4, blend(accent, 0xFFFFFFFF, 8));
            g.fill(x + 2, y + h - 4, x + w - 2, y + h - 2, blend(accent, 0xFF000000, 34));
        }
        g.outline(x, y, w, h, BORDER);
        g.outline(x + 1, y + 1, w - 2, h - 2, blend(accent == 0 ? 0xFF766B5F : accent, 0xFF000000, 18));
    }

    public static int blend(int base, int overlay, int percent) {
        percent = Math.max(0, Math.min(100, percent));
        int inv = 100 - percent;
        int a = (((base >>> 24) & 255) * inv + ((overlay >>> 24) & 255) * percent) / 100;
        int r = (((base >>> 16) & 255) * inv + ((overlay >>> 16) & 255) * percent) / 100;
        int gg = (((base >>> 8) & 255) * inv + ((overlay >>> 8) & 255) * percent) / 100;
        int b = ((base & 255) * inv + (overlay & 255) * percent) / 100;
        return (a << 24) | (r << 16) | (gg << 8) | b;
    }

    public static void smallText(GuiGraphicsExtractor g, String value, int x, int y, int color, int maxWidth) {
        if (value == null || value.isBlank()) return;
        Font font = Minecraft.getInstance().font;
        int sourceWidth = Math.max(1, (int)(maxWidth / SMALL_SCALE));
        String clipped = font.plainSubstrByWidth(value, sourceWidth);
        g.pose().pushMatrix();
        try {
            g.pose().translate(x, y);
            g.pose().scale(SMALL_SCALE, SMALL_SCALE);
            g.text(font, clipped, 0, 0, color, false);
        } finally {
            g.pose().popMatrix();
        }
    }

    public static int smallWidth(String value) {
        return Math.round(Minecraft.getInstance().font.width(value) * SMALL_SCALE);
    }

    public static void icon(GuiGraphicsExtractor g, String itemId, int x, int y, int size) {
        try {
            var item = BuiltInRegistries.ITEM.getValue(Identifier.parse(itemId));
            var stack = new ItemStack(item == null || item == Items.AIR ? Items.COMPARATOR : item);
            g.pose().pushMatrix();
            try {
                g.pose().translate(x, y);
                g.pose().scale(size / 16f, size / 16f);
                g.fakeItem(stack, 0, 0);
            } finally {
                g.pose().popMatrix();
            }
            g.nextStratum();
        } catch (RuntimeException ignored) {}
    }

    /** Native-size glyphs, wrapped rather than shrunk into unreadable fractional pixels. */
    public static void label(GuiGraphicsExtractor g, String value, int x, int y, int maxWidth, int color) {
        if (maxWidth <= 0) return;
        Font font = Minecraft.getInstance().font;
        g.text(font, ellipsis(font, value, maxWidth), x, y, color, false);
    }

    public static String ellipsis(Font font, String value, int width) {
        if (value == null || width <= 0) return "";
        if (font.width(value) <= width) return value;
        String suffix = "...";
        if (font.width(suffix) > width) return font.plainSubstrByWidth(value, width);
        return font.plainSubstrByWidth(value, width - font.width(suffix)) + suffix;
    }

    /** Same neutral navigation row for modules and map categories. The check has its own column. */
    public static final class FilterRow extends Button.Plain {
        private final String iconId;
        private final java.util.function.Supplier<String> label;
        private final BooleanSupplier checked;
        public FilterRow(int x,int y,int w,int h,String iconId,java.util.function.Supplier<String> label,
                         BooleanSupplier checked,Runnable click) {
            super(x,y,w,h,Component.literal(label.get()),b->click.run(),DEFAULT_NARRATION);
            this.iconId=iconId;this.label=label;this.checked=checked;
            setTooltip(Tooltip.create(Component.literal(label.get())));
        }
        @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float d) {
            int x=getX(),y=getY(),w=getWidth(),h=getHeight();
            boolean on=checked.getAsBoolean();
            g.fill(x,y,x+w,y+h,isHovered()?SIDEBAR_HOVER:SIDEBAR);
            if(isHovered()) g.fill(x,y,x+2,y+h,SELECTED);
            icon(g,iconId,x+6,y+(h-16)/2,16);
            label(g,label.get(),x+28,y+(h-9)/2,Math.max(1,w-48),TEXT);
            int bx=x+w-15,by=y+(h-10)/2;
            g.fill(bx,by,bx+10,by+10,on?GREEN:SIDEBAR_DARK);
            g.outline(bx,by,10,10,SOFT_BORDER);
            if(on) {
                g.fill(bx+2,by+4,bx+4,by+6,TEXT);
                g.fill(bx+4,by+6,bx+6,by+8,TEXT);
                g.fill(bx+6,by+2,bx+8,by+6,TEXT);
            }
        }
    }

    /** Parchment module card, using a complete item model, not a low-resolution pictogram. */
    public static final class ModuleCard extends Button.Plain {
        private final String iconId;
        private final Component title, description;
        private final BooleanSupplier enabled;
        public ModuleCard(int x,int y,int w,int h,String iconId,Component title,Component description,
                          BooleanSupplier enabled,Runnable open) {
            super(x,y,w,h,title,b->open.run(),DEFAULT_NARRATION);
            this.iconId=iconId;this.title=title;this.description=description;this.enabled=enabled;
            setTooltip(Tooltip.create(description));
        }
        @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float d) {
            int x=getX(),y=getY(),w=getWidth(),h=getHeight();
            raised(g,x,y,w,h,isHovered()?ROW_HOVER:ROW,isHovered()?SELECTED:0xFF817565,false,isHovered());
            icon(g,iconId,x+10,y+9,32);
            Font font=Minecraft.getInstance().font;
            String name=title.getString();
            int textX=x+50,limit=w-58;
            String first=font.plainSubstrByWidth(name,limit);
            if(first.length()<name.length()) {
                int split=first.lastIndexOf(' ');
                if(split>first.length()/2) first=first.substring(0,split);
            }
            label(g,first,textX,y+12,limit,BODY_TEXT);
            if(first.length()<name.length()) label(g,name.substring(first.length()).strip(),textX,y+24,limit,BODY_TEXT);
            boolean on=enabled.getAsBoolean();
            g.fill(x+11,y+h-21,x+16,y+h-16,on?GREEN:SOFT_BORDER);
            label(g,Component.translatable(on?"ownmods.active":"ownmods.inactive").getString(),
                    x+22,y+h-23,w-30,on?0xFF286953:BODY_MUTED);
        }
    }

    public static final class Surface extends Button.Plain {
        private final Consumer<GuiGraphicsExtractor> draw;
        public Surface(int x, int y, int w, int h, Consumer<GuiGraphicsExtractor> draw) {
            super(x, y, w, h, Component.empty(), b -> {}, DEFAULT_NARRATION);
            this.draw = draw;
            active = false;
        }
        @Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float d) { draw.accept(g); }
    }

    public static final class SidebarButton extends Button.Plain {
        private final String iconId;
        private final String label;
        private final int accent;
        private final BooleanSupplier selected;
        public SidebarButton(int x, int y, int w, int h, String iconId, String label, int accent,
                             BooleanSupplier selected, Runnable click) {
            super(x, y, w, h, Component.literal(label), b -> click.run(), DEFAULT_NARRATION);
            this.iconId = iconId;
            this.label = label;
            this.accent = accent;
            this.selected = selected;
        }
        @Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float d) {
            int x = getX(), y = getY(), w = getWidth(), h = getHeight();
            boolean on = selected.getAsBoolean();
            int fill = on ? blend(SIDEBAR, accent, 42) : (isHovered() ? blend(SIDEBAR_HOVER, accent, 14) : SIDEBAR);
            g.fill(x, y, x + w, y + h, fill);
            if (on) {
                g.fill(x, y, x + 4, y + h, accent);
                g.fill(x + 4, y, x + w, y + 2, blend(accent, 0xFFFFFFFF, 28));
                g.fill(x + 4, y + h - 2, x + w, y + h, blend(accent, 0xFF000000, 38));
            } else if (isHovered()) {
                g.fill(x, y, x + 2, y + h, blend(accent, 0xFFFFFFFF, 12));
            }
            g.fill(x + w - 1, y, x + w, y + h, 0xFF15181B);
            icon(g, iconId, x + 9, y + (h - 16) / 2, 16);
            smallText(g, label, x + 30, y + (h - 7) / 2, TEXT, w - 35);
        }
    }

    public static final class ToggleRow extends Button.Plain {
        private final String iconId;
        private final String label;
        private final int accent;
        private final BooleanSupplier selected;
        public ToggleRow(int x, int y, int w, int h, String iconId, String label, int accent,
                         BooleanSupplier selected, Runnable click, String tooltip) {
            super(x, y, w, h, Component.literal(label), b -> click.run(), DEFAULT_NARRATION);
            this.iconId = iconId;
            this.label = label;
            this.accent = accent;
            this.selected = selected;
            if (tooltip != null && !tooltip.isBlank()) setTooltip(Tooltip.create(Component.literal(tooltip)));
        }
        @Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float d) {
            int x = getX(), y = getY(), w = getWidth(), h = getHeight();
            boolean on = selected.getAsBoolean();
            int face = !active ? 0xFFC2B8A7 : (isHovered() ? ROW_HOVER : ROW);
            raised(g, x, y, w, h, face, on ? accent : 0xFF817565, on, isHovered() && active);
            if (on) g.fill(x + 2, y + 2, x + 5, y + h - 3, accent);
            icon(g, iconId, x + 8, y + (h - 16) / 2, 16);
            smallText(g, label, x + 31, y + (h - 7) / 2, active ? BODY_TEXT : 0xFF777169, Math.max(20, w - 96));

            int sw = 48, sh = 16, sx = x + w - sw - 9, sy = y + (h - sh) / 2;
            int state = !active ? 0xFF817B72 : on ? blend(accent, 0xFF000000, 18) : 0xFF6E665C;
            g.fill(sx + 1, sy + 1, sx + sw + 1, sy + sh + 1, 0x42000000);
            g.fill(sx, sy, sx + sw, sy + sh, state);
            g.outline(sx, sy, sw, sh, 0xFF3C352F);
            String text = on ? UiText.tr("vr.text.5ac4d5d4c2d3") : UiText.tr("vr.text.537ae3b1762d");
            int tw = smallWidth(text);
            smallText(g, text, sx + (sw - tw) / 2, sy + 5, active ? TEXT : 0xFFD1C9BB, sw - 4);
        }
    }

    public static final class ChoiceButton extends Button.Plain {
        private final String iconId;
        private final String label;
        private final int accent;
        private final BooleanSupplier selected;
        public ChoiceButton(int x, int y, int w, int h, String iconId, String label, int accent,
                            BooleanSupplier selected, Runnable click, String tooltip) {
            super(x, y, w, h, Component.literal(label), b -> click.run(), DEFAULT_NARRATION);
            this.iconId = iconId;
            this.label = label;
            this.accent = accent;
            this.selected = selected;
            if (tooltip != null && !tooltip.isBlank()) setTooltip(Tooltip.create(Component.literal(tooltip)));
        }
        @Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float d) {
            int x = getX(), y = getY(), w = getWidth(), h = getHeight();
            boolean on = selected.getAsBoolean();
            int face = !active ? 0xFFC2B8A7 : on ? blend(ROW, accent, 26) : (isHovered() ? ROW_HOVER : ROW);
            raised(g, x, y, w, h, face, on ? accent : 0xFF817565, on, isHovered() && active);
            if (on) g.fill(x + 2, y + 2, x + w - 2, y + 5, accent);
            icon(g, iconId, x + 7, y + (h - 16) / 2, 16);
            smallText(g, label, x + 28, y + (h - 7) / 2, active ? BODY_TEXT : 0xFF777169, Math.max(1, w - 35));
        }
    }


    public static final class ChoiceRow extends Button.Plain {
        private final String optionLabel;
        private final java.util.function.Supplier<String> valueLabel;
        private final java.util.function.Supplier<String> iconId;
        private final int accent;
        public ChoiceRow(int x, int y, int w, int h, String optionLabel,
                         java.util.function.Supplier<String> valueLabel,
                         java.util.function.Supplier<String> iconId,
                         int accent, Runnable click, String tooltip) {
            super(x, y, w, h, Component.literal(optionLabel), b -> click.run(), DEFAULT_NARRATION);
            this.optionLabel = optionLabel;
            this.valueLabel = valueLabel;
            this.iconId = iconId;
            this.accent = accent;
            if (tooltip != null && !tooltip.isBlank()) setTooltip(Tooltip.create(Component.literal(tooltip)));
        }
        @Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float d) {
            int x = getX(), y = getY(), w = getWidth(), h = getHeight();
            int face = !active ? 0xFFC2B8A7 : (isHovered() ? ROW_HOVER : ROW);
            raised(g, x, y, w, h, face, active ? accent : 0xFF817565, false, isHovered() && active);
            icon(g, iconId.get(), x + 8, y + (h - 16) / 2, 16);
            int valueW = Math.min(142, Math.max(86, w / 3));
            int valueX = x + w - valueW - 9;
            smallText(g, optionLabel, x + 31, y + (h - 7) / 2, active ? BODY_TEXT : 0xFF777169,
                    Math.max(20, valueX - x - 38));
            int vy = y + 5, vh = h - 10;
            g.fill(valueX + 1, vy + 1, valueX + valueW + 1, vy + vh + 1, 0x3D000000);
            g.fill(valueX, vy, valueX + valueW, vy + vh, active ? blend(CONTENT_INNER, accent, 24) : 0xFFA9A297);
            g.outline(valueX, vy, valueW, vh, active ? blend(accent, 0xFF000000, 18) : 0xFF6E675E);
            String value = valueLabel.get();
            int tw = Math.min(valueW - 8, smallWidth(value));
            smallText(g, value, valueX + (valueW - tw) / 2, vy + 5, active ? BODY_TEXT : 0xFF777169, valueW - 8);
        }
    }

    public static final class ActionButton extends Button.Plain {
        private final String iconId;
        private final int accent;
        private final boolean dark;
        public ActionButton(int x, int y, int w, int h, String iconId, String label, int accent, boolean dark, Runnable click) {
            super(x, y, w, h, Component.literal(label), b -> click.run(), DEFAULT_NARRATION);
            this.iconId = iconId;
            this.accent = accent;
            this.dark = dark;
        }
        @Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float d) {
            int x = getX(), y = getY(), w = getWidth(), h = getHeight();
            if (dark) {
                int face = !active ? 0xFF24272A : (isHovered() ? blend(0xFF25282D, accent, 28) : 0xFF25282D);
                g.fill(x + 2, y + 2, x + w + 2, y + h + 2, 0x40000000);
                g.fill(x, y, x + w, y + h, face);
                g.outline(x, y, w, h, active ? blend(accent, 0xFFFFFFFF, 4) : 0xFF55504B);
                if (active) g.fill(x, y + h - 2, x + w, y + h, blend(accent, 0xFF000000, 18));
            } else {
                int face = !active ? 0xFFC0B8AB : blend(0xFFD7C9B5, accent, isHovered() ? 24 : 13);
                raised(g, x, y, w, h, face, active ? accent : 0xFF817565, false, isHovered() && active);
            }
            String caption = getMessage().getString();
            if (caption.isBlank()) icon(g, iconId, x + (w - 16) / 2, y + (h - 16) / 2, 16);
            else {
                icon(g, iconId, x + 7, y + (h - 16) / 2, 16);
                smallText(g, caption, x + 28, y + (h - 7) / 2, dark ? TEXT : BODY_TEXT, Math.max(1, w - 35));
            }
        }
    }
}
