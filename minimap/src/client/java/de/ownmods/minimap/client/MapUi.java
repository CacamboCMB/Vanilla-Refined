package de.ownmods.minimap.client;
import de.ownmods.settings.client.UiText;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.MouseButtonEvent;
import de.ownmods.settings.client.compat.GuiTextureCompat;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.function.Consumer;
import de.ownmods.settings.client.OwnModsUi;

/** Compact visual language used by the Minimap HUD and the full world map. */
final class MapUi {
    private MapUi() {}

    // Warm Notes-derived palette: dark carved header/sidebar, parchment-stone controls and
    // category colour accents. The actual map canvas stays dark so vanilla map colours dominate.
    static final int TEXT = 0xFFF7F1E5;
    static final int MUTED = 0xFFC8C0B1;
    static final int BODY_TEXT = 0xFF211B16;
    static final int BODY_MUTED = 0xFF625A50;
    static final int HEADER = 0xFF201D1B;
    static final int HEADER_EDGE = 0xFF3C342D;
    static final int SIDEBAR = 0xFF25282D;
    static final int INFO = 0xFF292A2D;
    static final int CONTENT = 0xFFC8BFAE;
    static final int ROW = 0xFFE2D8C7;
    static final int ROW_HOVER = 0xFFF0E6D5;
    static final int MAP_BG = 0xFF071012;
    static final int MAP_GRID = 0x262C6972;
    static final int BORDER = 0xFF171513;
    static final int BLUE = 0xFF4D86B8;
    static final int GREEN = 0xFF4A9B82;
    static final int RED = 0xFFC6534A;
    static final int GOLD = 0xFFE0B83D;
    static final int BRASS = 0xFFC08A3A;
    static final int CYAN = 0xFF3B84A6;
    static final int PURPLE = 0xFF8A68B4;

    static void shell(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x + 5, y + 6, x + w + 5, y + h + 6, 0x72000000);
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF0E0D0C);
        g.fill(x, y, x + w, y + h, 0xFF5F584F);
        g.fill(x + 2, y + 2, x + w - 2, y + h - 2, CONTENT);
        g.outline(x, y, w, h, 0xFF171513);
        g.outline(x + 2, y + 2, w - 4, h - 4, 0xFF7D7468);
        g.fill(x + 3, y + 3, x + w - 3, y + 4, 0xFFA79B89);
        g.fill(x + 3, y + h - 4, x + w - 3, y + h - 3, dark(BRASS, 30));
    }

    static void header(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, HEADER);
        g.fill(x, y, x + w, y + 2, HEADER_EDGE);
        g.fill(x + 1, y + 2, x + w - 1, y + 4, 0xFF2D2925);
        g.fill(x, y + h - 3, x + w, y + h - 1, 0xFF0F0E0D);
        g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, BRASS);
    }

    static void sidebar(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, 0xFF1C1F23);
        g.fill(x + 2, y, x + w, y + h, SIDEBAR);
        for (int yy = y + 8; yy < y + h; yy += 20) g.fill(x + 3, yy, x + w - 2, yy + 1, 0x0EFFFFFF);
        g.fill(x + w - 2, y, x + w, y + h, 0xFF111315);
        g.fill(x + w - 3, y, x + w - 2, y + h, 0xFF4A5057);
    }

    static void infoPanel(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, INFO);
        g.fill(x, y, x + 2, y + h, 0xFF151719);
        g.fill(x + 2, y, x + 3, y + h, 0xFF56504A);
    }

    static void mapFrame(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        // Same warm raised frame as the other Vanilla Refined menus; only the actual map canvas stays dark.
        g.fill(x + 3, y + 4, x + w + 3, y + h + 4, 0x4F000000);
        g.fill(x, y, x + w, y + h, 0xFF302B26);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, CONTENT);
        g.fill(x + 3, y + 3, x + w - 3, y + h - 3, MAP_BG);
        g.fill(x + 3, y + 3, x + w - 3, y + 5, 0xFF41545A);
        g.fill(x + 3, y + h - 5, x + w - 3, y + h - 3, 0xFF142A2F);
        g.outline(x, y, w, h, BORDER);
        g.outline(x + 2, y + 2, w - 4, h - 4, 0xFF6A6258);
        for (int gx = x + 18; gx < x + w - 3; gx += 24) g.fill(gx, y + 5, gx + 1, y + h - 5, MAP_GRID);
        for (int gy = y + 18; gy < y + h - 3; gy += 24) g.fill(x + 5, gy, x + w - 5, gy + 1, MAP_GRID);
    }

    static void raised(GuiGraphicsExtractor g, int x, int y, int w, int h, int face, int accent) {
        g.fill(x + 2, y + 2, x + w + 2, y + h + 2, 0x55000000);
        g.fill(x, y, x + w, y + h, 0xFF111315);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, face);
        g.fill(x + 2, y + 2, x + w - 2, y + 3, light(accent, 34));
        g.fill(x + 2, y + h - 3, x + w - 2, y + h - 2, dark(accent, 38));
    }

    static void compactButton(GuiGraphicsExtractor g, int x, int y, int w, int h, int accent, boolean active, boolean hovered) {
        int face = active ? blend(0xFF252A2E, accent, hovered ? 30 : 19) : 0xFF202428;
        g.fill(x + 1, y + 1, x + w + 1, y + h + 1, 0x4A000000);
        g.fill(x, y, x + w, y + h, active ? accent : 0xFF4D5358);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, face);
        if (active) g.fill(x + 2, y + h - 2, x + w - 2, y + h - 1, dark(accent, 22));
    }

    static int light(int c, int p) { return blend(c, 0xFFFFFFFF, p); }
    static int dark(int c, int p) { return blend(c, 0xFF000000, p); }
    static int blend(int a, int b, int p) {
        int q = 100 - p;
        return 0xFF000000
                | ((((a >> 16) & 255) * q + ((b >> 16) & 255) * p) / 100 << 16)
                | ((((a >> 8) & 255) * q + ((b >> 8) & 255) * p) / 100 << 8)
                | (((a & 255) * q + (b & 255) * p) / 100);
    }

    static void text(GuiGraphicsExtractor g, String s, int x, int y, int color) {
        g.text(Minecraft.getInstance().font, s, x, y, color, false);
    }

    static void centeredText(GuiGraphicsExtractor g, String s, int x, int y, int w, int color) {
        int tw = Minecraft.getInstance().font.width(s);
        text(g, s, x + Math.max(0, (w - tw) / 2), y, color);
    }

    private static final float SMALL_SCALE = 0.72f;

    static int smallWidth(String s) {
        return Math.round(Minecraft.getInstance().font.width(s) * SMALL_SCALE);
    }

    static String smallSubstr(String s, int maxWidth) {
        int source = Math.max(1, (int)(maxWidth / SMALL_SCALE));
        return Minecraft.getInstance().font.plainSubstrByWidth(s, source);
    }

    static void smallText(GuiGraphicsExtractor g, String s, int x, int y, int color) {
        if (s == null || s.isBlank()) return;
        g.pose().pushMatrix();
        try {
            g.pose().translate(x, y);
            g.pose().scale(SMALL_SCALE, SMALL_SCALE);
            g.text(Minecraft.getInstance().font, s, 0, 0, color, false);
        } finally {
            g.pose().popMatrix();
        }
    }

    static void fillCircle(GuiGraphicsExtractor g, int cx, int cy, int r, int color) {
        for (int dy = -r; dy <= r; dy++) {
            int dx = (int) Math.floor(Math.sqrt(Math.max(0, r * r - dy * dy)));
            g.fill(cx - dx, cy + dy, cx + dx + 1, cy + dy + 1, color);
        }
    }

    static void circle(GuiGraphicsExtractor g, int cx, int cy, int r, int color) {
        for (int dx = -r; dx <= r; dx++) {
            int yy = (int) Math.round(Math.sqrt(Math.max(0, r * r - dx * dx)));
            g.fill(cx + dx, cy - yy, cx + dx + 1, cy - yy + 1, color);
            g.fill(cx + dx, cy + yy, cx + dx + 1, cy + yy + 1, color);
        }
    }

    /** Procedural HUD frame: no texture asset, with a compact three-layer Minecraft-style depth ring. */
    static void hudFrame(GuiGraphicsExtractor g, int x, int y, int diameter, boolean circleShape) {
        int r = diameter / 2, cx = x + r, cy = y + r;
        if (circleShape) {
            fillCircle(g, cx + 2, cy + 3, r + 4, 0x52000000);
            fillCircle(g, cx, cy, r + 2, 0xED121416);
            fillCircle(g, cx, cy, r, 0xD90A0E10);
            circle(g, cx, cy, r + 1, 0xFF0D0E0F);
            circle(g, cx, cy, r, 0xFFE7DEC9);
            circle(g, cx, cy, r - 1, 0xFF3B555D);
            // Small generated atlas ticks; cyan/gold make the HUD less sterile without blocking map pixels.
            g.fill(cx - 2, y - 1, cx + 2, y + 3, GOLD);
            g.fill(cx - 2, y + diameter - 3, cx + 2, y + diameter + 1, BRASS);
            g.fill(x - 1, cy - 2, x + 3, cy + 2, CYAN);
            g.fill(x + diameter - 3, cy - 2, x + diameter + 1, cy + 2, CYAN);
        } else {
            g.fill(x + 3, y + 4, x + diameter + 5, y + diameter + 6, 0x52000000);
            g.fill(x - 2, y - 2, x + diameter + 2, y + diameter + 2, 0xED121416);
            g.fill(x, y, x + diameter, y + diameter, 0xD90A0E10);
            g.outline(x - 1, y - 1, diameter + 2, diameter + 2, 0xFFE7DEC9);
            g.outline(x, y, diameter, diameter, 0xFF3B555D);
            g.fill(x, y, x + 18, y + 2, GOLD);
            g.fill(x, y, x + 2, y + 18, CYAN);
        }
    }

    static void playerArrow(GuiGraphicsExtractor g, int x, int y) {
        // North-up map: compact white pointer with a dark outline.
        g.fill(x - 1, y - 6, x + 2, y + 5, 0xD9000000);
        g.fill(x - 5, y + 2, x + 6, y + 5, 0xD9000000);
        g.fill(x, y - 5, x + 1, y + 3, 0xFFFFFFFF);
        g.fill(x - 1, y - 3, x + 2, y + 2, 0xFFFFFFFF);
        g.fill(x - 2, y - 1, x + 3, y + 3, 0xFFFFFFFF);
    }

    static void marker(GuiGraphicsExtractor g, int x, int y, int color, boolean target) {
        int r = target ? 4 : 3;
        g.fill(x - r, y - 1, x + r + 1, y + 2, 0xCC000000);
        g.fill(x - 1, y - r, x + 2, y + r + 1, 0xCC000000);
        g.fill(x - r + 1, y, x + r, y + 1, color);
        g.fill(x, y - r + 1, x + 1, y + r, color);
        if (target) g.outline(x - 5, y - 5, 11, 11, 0xFFFFFFFF);
    }

    static void markerIcon(GuiGraphicsExtractor g, int x, int y, int color, String itemId, boolean target) {
        // Compact marker footprint: item identity stays readable without hiding terrain.
        int box = target ? 9 : 7;
        int half = box / 2;
        g.fill(x - half - 2, y - half - 1, x + half + 3, y + half + 3, 0x82000000);
        g.fill(x - half - 1, y - half - 1, x + half + 2, y + half + 2, color);
        g.fill(x - half, y - half, x + half + 1, y + half + 1, 0xD5151719);
        icon(g, itemId, x - half + 1, y - half + 1, Math.max(5, box - 2));
        if (target) g.outline(x - half - 2, y - half - 2, box + 4, box + 4, 0xFFFFFFFF);
    }

    static void tag(GuiGraphicsExtractor g, String label, int x, int y, int color) {
        int tw = Minecraft.getInstance().font.width(label);
        g.fill(x - 3, y - 2, x + tw + 4, y + 10, 0xC9141719);
        g.fill(x - 3, y - 2, x - 2, y + 10, color);
        text(g, label, x, y, TEXT);
    }

    /** Semantic aliases use Minecraft item models at menu sizes; HUD glyphs stay compact. */
    static String menuItem(String id) {
        return switch(id) {
            case "@home" -> "minecraft:red_bed";
            case "@village" -> "minecraft:bell";
            case "@waypoint", "@pin", "@center" -> "minecraft:compass";
            case "@map", "@hud", "@layers" -> "minecraft:filled_map";
            case "@filter" -> "minecraft:hopper";
            case "@target" -> "minecraft:target";
            case "@eye" -> "minecraft:ender_eye";
            case "@rotate" -> "minecraft:recovery_compass";
            case "@zoom", "@search" -> "minecraft:spyglass";
            case "@info" -> "minecraft:book";
            case "@star" -> "minecraft:nether_star";
            case "@trash" -> "minecraft:lava_bucket";
            case "@player" -> "minecraft:player_head";
            case "@save" -> "minecraft:lime_dye";
            case "@close", "@cancel" -> "minecraft:barrier";
            case "@settings" -> "minecraft:comparator";
            default -> id;
        };
    }

    static void icon(GuiGraphicsExtractor g, String id, int x, int y, int size) {
        if (size >= 13 && id.startsWith("@") && !id.equals("@plus") && !id.equals("@minus")) {
            item(g, menuItem(id), x, y, Math.max(16, size)); return;
        }
        if (id == null || id.isBlank()) return;
        if (id.equals("@home")) { home(g, x, y, size); return; }
        if (id.equals("@village")) {
            int small = Math.max(7, size * 2 / 3);
            home(g, x, y + size - small, small);
            home(g, x + size - small, y, small);
            item(g, "minecraft:bell", x + size / 2 - 3, y + size / 2 - 1, Math.max(6, size / 2));
            return;
        }
        if (id.startsWith("@")) { pixelIcon(g, id, x, y, size); return; }
        item(g, id, x, y, size);
    }

    static void item(GuiGraphicsExtractor g, String id, int x, int y, int size) {
        try {
            var item = BuiltInRegistries.ITEM.getValue(Identifier.parse(id));
            var stack = new ItemStack(item == null || item == Items.AIR ? Items.MAP : item);
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

    private static void home(GuiGraphicsExtractor g, int x, int y, int size) {
        GuiTextureCompat.blit(g,
                Identifier.fromNamespaceAndPath("ownmods_notes", "textures/gui/home.png"),
                x, y, 0, 0, size, size, 32, 32, 32, 32);
    }

    static void arrow(GuiGraphicsExtractor g, int x, int y, double dx, double dz, int color) {
        double len = Math.max(0.001, Math.hypot(dx, dz));
        double nx = dx / len, ny = dz / len;
        int tipX = x + (int) Math.round(nx * 5), tipY = y + (int) Math.round(ny * 5);
        int tailX = x - (int) Math.round(nx * 3), tailY = y - (int) Math.round(ny * 3);
        int sideX = (int) Math.round(-ny * 2), sideY = (int) Math.round(nx * 2);
        line(g, tailX, tailY, tipX, tipY, 0xDD000000, 2);
        line(g, tailX, tailY, tipX, tipY, color, 1);
        line(g, tailX + sideX, tailY + sideY, tipX, tipY, color, 1);
        line(g, tailX - sideX, tailY - sideY, tipX, tipY, color, 1);
    }

    private static void line(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, int color, int thickness) {
        int dx = Math.abs(x1 - x0), sx = x0 < x1 ? 1 : -1;
        int dy = -Math.abs(y1 - y0), sy = y0 < y1 ? 1 : -1;
        int err = dx + dy;
        while (true) {
            int r = Math.max(0, thickness - 1);
            g.fill(x0 - r, y0 - r, x0 + r + 1, y0 + r + 1, color);
            if (x0 == x1 && y0 == y1) break;
            int e2 = 2 * err;
            if (e2 >= dy) { err += dy; x0 += sx; }
            if (e2 <= dx) { err += dx; y0 += sy; }
        }
    }


    /** Small procedural pixel icons: no external HUD texture file and no emoji dependency. */
    private static void pixelIcon(GuiGraphicsExtractor g, String id, int x, int y, int size) {
        int s = Math.max(8, size), u = Math.max(1, s / 8), cx = x + s / 2, cy = y + s / 2;
        int white = 0xFFF5E9D2, ink = 0xFF1A1816;
        switch (id) {
            case "@map" -> {
                g.fill(x+u,y+2*u,x+s-u,y+s-u,0xFFF0DCA5); g.outline(x+u,y+2*u,s-2*u,s-3*u,0xFF6B5235);
                g.fill(cx-u,y+3*u,cx+u,y+s-2*u,0xFF5EA66B); g.fill(x+2*u,cy,x+4*u,cy+u,0xFF4D86B8);
            }
            case "@pin" -> {
                fillCircle(g,cx,cy-2*u,3*u,RED); fillCircle(g,cx,cy-2*u,u,white);
                g.fill(cx-u,cy+u,cx+u,y+s-u,RED); g.fill(cx,cy+2*u,cx+u,y+s, dark(RED,22));
            }
            case "@hud" -> {
                circle(g,cx,cy,3*u,white); circle(g,cx,cy,2*u,CYAN); arrow(g,cx,cy,0,-1,white);
            }
            case "@filter" -> {
                g.fill(x+u,y+u,x+s-u,y+2*u,white); g.fill(x+2*u,y+3*u,x+s-2*u,y+4*u,white);
                g.fill(x+3*u,y+5*u,x+s-3*u,y+6*u,white); g.fill(cx-u,y+6*u,cx+u,y+s-u,GOLD);
            }
            case "@target" -> {
                fillCircle(g,cx,cy,3*u,RED); fillCircle(g,cx,cy,2*u,white); fillCircle(g,cx,cy,u,RED);
            }
            case "@eye" -> {
                g.fill(x+u,cy-u,x+s-u,cy+u,white); g.fill(x+2*u,cy-2*u,x+s-2*u,cy+2*u,white);
                fillCircle(g,cx,cy,2*u,CYAN); fillCircle(g,cx,cy,u,ink);
            }
            case "@rotate" -> {
                circle(g,cx,cy,3*u,BLUE); arrow(g,x+s-2*u,cy-2*u,1,1,white); arrow(g,x+2*u,cy+2*u,-1,-1,white);
            }
            case "@zoom" -> {
                circle(g,cx-u,cy-u,2*u,white); g.fill(cx+u,cy+u,x+s-u,y+s-u,white);
                g.fill(cx-2*u,cy-u,cx,cy,BLUE); g.fill(cx-u,cy-2*u,cx,cy,BLUE);
            }
            case "@info" -> {
                circle(g,cx,cy,3*u,BLUE); g.fill(cx-u,y+2*u,cx+u,y+3*u,white); g.fill(cx-u,y+4*u,cx+u,y+7*u,white);
            }
            case "@star" -> item(g,"minecraft:nether_star",x,y,s);
            case "@trash" -> {
                g.fill(x+2*u,y+3*u,x+s-2*u,y+s-u,DANGER); g.fill(x+u,y+2*u,x+s-u,y+3*u,light(DANGER,15));
                g.fill(x+3*u,y+u,x+s-3*u,y+2*u,white);
            }
            case "@player" -> {
                fillCircle(g,cx,y+3*u,2*u,0xFFE7C39D); g.fill(x+2*u,y+5*u,x+s-2*u,y+s-u,GREEN);
            }
            case "@layers" -> {
                g.fill(x+u,y+2*u,cx,y+4*u,GREEN); g.fill(cx,y+2*u,x+s-u,y+4*u,BLUE);
                g.fill(x+2*u,y+4*u,x+s-2*u,y+6*u,GOLD); g.fill(x+3*u,y+6*u,x+s-3*u,y+s-u,PURPLE);
            }
            case "@search" -> {
                circle(g,cx-u,cy-u,2*u,white); g.fill(cx+u,cy+u,x+s-u,y+s-u,white);
            }
            case "@close" -> {
                line(g,x+2*u,y+2*u,x+s-2*u,y+s-2*u,white,1); line(g,x+s-2*u,y+2*u,x+2*u,y+s-2*u,white,1);
            }
            case "@plus" -> {
                g.fill(cx-u,y+u,cx+u,y+s-u,white); g.fill(x+u,cy-u,x+s-u,cy+u,white);
            }
            case "@minus" -> g.fill(x+u,cy-u,x+s-u,cy+u,white);
            case "@center" -> {
                circle(g,cx,cy,3*u,white); g.fill(cx-u,cy-u,cx+u,cy+u,RED);
                g.fill(cx-u,y,cx+u,y+2*u,white); g.fill(cx-u,y+s-2*u,cx+u,y+s,white);
            }
            case "@save" -> {
                g.fill(x+u,y+u,x+s-u,y+s-u,GREEN); g.fill(x+2*u,y+2*u,x+s-2*u,y+4*u,white);
                g.fill(x+3*u,y+5*u,x+s-3*u,y+s-2*u,dark(GREEN,24));
            }
            case "@cancel" -> {
                circle(g,cx,cy,3*u,RED); line(g,x+2*u,y+2*u,x+s-2*u,y+s-2*u,white,1); line(g,x+s-2*u,y+2*u,x+2*u,y+s-2*u,white,1);
            }
            case "@waypoint" -> {
                fillCircle(g,cx,cy-2*u,3*u,BLUE); fillCircle(g,cx,cy-2*u,u,white);
                g.fill(cx-u,cy+u,cx+u,y+s-u,BLUE);
            }
            case "@settings" -> {
                circle(g,cx,cy,3*u,0xFF9A8E7D); fillCircle(g,cx,cy,u,ink);
                g.fill(cx-u,y,cx+u,y+2*u,white); g.fill(cx-u,y+s-2*u,cx+u,y+s,white);
                g.fill(x,cy-u,x+2*u,cy+u,white); g.fill(x+s-2*u,cy-u,x+s,cy+u,white);
            }
            default -> item(g,"minecraft:map",x,y,s);
        }
    }

    static final int DANGER = RED;

    /** Compact two-column map filter tile. Active state is intentionally very obvious. */
    static final class FilterTile extends Button.Plain {
        final String icon,label; final int accent; final java.util.function.BooleanSupplier selected;
        FilterTile(int x,int y,int w,int h,String icon,String label,int accent,java.util.function.BooleanSupplier selected,Runnable click){
            super(x,y,w,h,Component.literal(label),b->click.run(),DEFAULT_NARRATION);
            this.icon=icon;this.label=label;this.accent=accent;this.selected=selected;
        }
        @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float d){
            boolean on=selected.getAsBoolean();int x=getX(),y=getY(),w=getWidth(),h=getHeight();
            int face=on?blend(SIDEBAR,accent,38):(isHovered()?blend(SIDEBAR,accent,12):SIDEBAR);
            g.fill(x,y,x+w,y+h,face);
            if(on){
                g.fill(x,y,x+4,y+h,accent);
                g.fill(x+4,y,x+w,y+2,light(accent,18));
                g.fill(x+4,y+h-2,x+w,y+h,dark(accent,32));
            } else if(isHovered()) {
                g.fill(x,y,x+2,y+h,light(accent,8));
            }
            g.fill(x+w-1,y,x+w,y+h,0xFF15181B);
            icon(g,icon,x+7,y+(h-14)/2,14);
            String fitted=smallSubstr(label,Math.max(8,w-31));
            smallText(g,fitted,x+25,y+(h-7)/2,TEXT);
            if(on) smallText(g,"✓",x+w-10,y+(h-7)/2,TEXT);
        }
    }

    static final class FilterChip extends Button.Plain {
        final String icon, label; final int accent; final java.util.function.BooleanSupplier selected;
        FilterChip(int x,int y,int w,int h,String icon,String label,int accent,java.util.function.BooleanSupplier selected,Runnable click){
            super(x,y,w,h,Component.literal(label),b->click.run(),DEFAULT_NARRATION);this.icon=icon;this.label=label;this.accent=accent;this.selected=selected;
        }
        @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float d){
            boolean on=selected.getAsBoolean();int x=getX(),y=getY(),w=getWidth(),h=getHeight();
            int face=on?blend(ROW,accent,24):(isHovered()?0xFFE9DFCE:0xFFD7CCBA);raised(g,x,y,w,h,face,on?accent:0xFF817565);
            if(on)g.fill(x+2,y+2,x+5,y+h-3,accent);icon(g,icon,x+6,y+(h-13)/2,13);
            text(g,Minecraft.getInstance().font.plainSubstrByWidth(label,Math.max(10,w-25)),x+22,y+(h-9)/2,BODY_TEXT);
        }
    }

    static final class ToggleRow extends Button.Plain {
        final String icon,label;final int accent;final java.util.function.BooleanSupplier on;
        ToggleRow(int x,int y,int w,int h,String icon,String label,int accent,java.util.function.BooleanSupplier on,Runnable click){
            super(x,y,w,h,Component.literal(label),b->click.run(),DEFAULT_NARRATION);this.icon=icon;this.label=label;this.accent=accent;this.on=on;
        }
        @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float d){
            int x=getX(),y=getY(),w=getWidth(),h=getHeight();boolean v=on.getAsBoolean();raised(g,x,y,w,h,isHovered()?ROW_HOVER:ROW,v?accent:0xFF817565);
            icon(g,icon,x+7,y+(h-15)/2,15);text(g,label,x+28,y+(h-9)/2,BODY_TEXT);
            int sw=42,sx=x+w-sw-7,sy=y+5,sh=h-10;g.fill(sx+1,sy+1,sx+sw+1,sy+sh+1,0x45000000);
            g.fill(sx,sy,sx+sw,sy+sh,v?dark(accent,12):0xFF6E665C);g.outline(sx,sy,sw,sh,0xFF3C352F);
            int knob=Math.max(8,sh-4),kx=v?sx+sw-knob-2:sx+2;g.fill(kx,sy+2,kx+knob,sy+sh-2,v?0xFFF2E6CE:0xFFC9C0B0);
            centeredText(g,v?UiText.tr("vr.text.5ac4d5d4c2d3"):UiText.tr("vr.text.537ae3b1762d"),sx,sy+Math.max(1,(sh-9)/2),sw,v?TEXT:0xFFE4D9C7);
        }
    }

    static final class ChoiceChip extends Button.Plain {
        final String icon,label;final int accent;final java.util.function.BooleanSupplier selected;
        ChoiceChip(int x,int y,int w,int h,String icon,String label,int accent,java.util.function.BooleanSupplier selected,Runnable click){
            super(x,y,w,h,Component.literal(label),b->click.run(),DEFAULT_NARRATION);this.icon=icon;this.label=label;this.accent=accent;this.selected=selected;
        }
        @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float d){
            int x=getX(),y=getY(),w=getWidth(),h=getHeight();boolean on=selected.getAsBoolean();int face=on?blend(ROW,accent,28):(isHovered()?ROW_HOVER:ROW);
            raised(g,x,y,w,h,face,on?accent:0xFF817565);if(on)g.fill(x+2,y+2,x+w-2,y+4,accent);
            if(icon!=null&&!icon.isBlank())icon(g,icon,x+5,y+(h-13)/2,13);int tx=(icon==null||icon.isBlank())?x+5:x+21;
            text(g,Minecraft.getInstance().font.plainSubstrByWidth(label,Math.max(8,w-(tx-x)-5)),tx,y+(h-9)/2,BODY_TEXT);
        }
    }

    static final class SwatchButton extends Button.Plain {
        final int color;final java.util.function.BooleanSupplier selected;
        SwatchButton(int x,int y,int size,int color,java.util.function.BooleanSupplier selected,Runnable click){
            super(x,y,size,size,Component.literal(UiText.tr("vr.text.a952c8f76f58")),b->click.run(),DEFAULT_NARRATION);this.color=color;this.selected=selected;
        }
        @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float d){int x=getX(),y=getY(),w=getWidth();g.fill(x+2,y+2,x+w+2,y+w+2,0x45000000);g.fill(x,y,x+w,y+w,selected.getAsBoolean()?0xFFF5E6C4:0xFF2D2925);g.fill(x+3,y+3,x+w-3,y+w-3,color);if(selected.getAsBoolean())g.outline(x,y,w,w,0xFFFFFFFF);else g.outline(x,y,w,w,0xFF6D6255);}
    }

    static final class IconChoice extends Button.Plain {
        final String icon;final int accent;final java.util.function.BooleanSupplier selected;
        IconChoice(int x,int y,int size,String icon,int accent,java.util.function.BooleanSupplier selected,Runnable click){super(x,y,size,size,Component.literal(icon),b->click.run(),DEFAULT_NARRATION);this.icon=icon;this.accent=accent;this.selected=selected;
            var item=BuiltInRegistries.ITEM.getValue(Identifier.parse(menuItem(icon)));
            if(item!=null)setTooltip(net.minecraft.client.gui.components.Tooltip.create(new ItemStack(item).getHoverName()));
        }
        @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float d){int x=getX(),y=getY(),s=getWidth();boolean on=selected.getAsBoolean();raised(g,x,y,s,s,on?blend(ROW,accent,25):(isHovered()?ROW_HOVER:ROW),on?accent:0xFF817565);icon(g,icon,x+(s-16)/2,y+(s-16)/2,16);}
    }

    static final class Surface extends Button.Plain {
        private final Consumer<GuiGraphicsExtractor> draw;
        Surface(int x, int y, int w, int h, Consumer<GuiGraphicsExtractor> draw) {
            super(x, y, w, h, Component.empty(), b -> {}, DEFAULT_NARRATION);
            this.draw = draw;
            active = false;
        }
        @Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float d) { draw.accept(g); }
    }

    static class Action extends Button.Plain {
        final int accent;
        final boolean dark;
        Action(int x, int y, int w, int h, String label, int accent, Runnable run) { this(x, y, w, h, label, accent, false, run); }
        Action(int x, int y, int w, int h, String label, int accent, boolean dark, Runnable run) {
            super(x, y, w, h, Component.literal(label), b -> run.run(), DEFAULT_NARRATION);
            this.accent = accent;
            this.dark = dark;
        }
        @Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float d) {
            if (dark) compactButton(g, getX(), getY(), getWidth(), getHeight(), accent, active, isHovered());
            else OwnModsUi.raised(g, getX(), getY(), getWidth(), getHeight(), isHovered() ? ROW_HOVER : ROW, accent, false, isHovered());
            String s = getMessage().getString();
            centeredText(g, s, getX(), getY() + Math.max(1, (getHeight() - 9) / 2), getWidth(), dark ? TEXT : 0xFF252525);
        }
    }

    static final class IconAction extends Button.Plain {
        final int accent;
        final String icon;
        final boolean selected;
        IconAction(int x, int y, int w, int h, String icon, String label, int accent, boolean selected, Runnable run) {
            super(x, y, w, h, Component.literal(label), b -> run.run(), DEFAULT_NARRATION);
            this.icon = icon;
            this.accent = accent;
            this.selected = selected;
        }
        @Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float d) {
            int x=getX(),y=getY(),w=getWidth(),h=getHeight();
            int face=selected?blend(ROW,accent,24):(isHovered()?ROW_HOVER:ROW);
            raised(g,x,y,w,h,face,selected?accent:0xFF817565);
            if(selected) g.fill(x+2,y+2,x+5,y+h-3,accent);
            icon(g,icon,x+7,y+(h-16)/2,16);
            String label=getMessage().getString();
            if(!label.isBlank()) smallText(g,smallSubstr(label,Math.max(1,w-35)),x+28,y+Math.max(2,(h-7)/2),BODY_TEXT);
        }
    }

    interface MapClick { void click(double x, double y, int button, boolean doubleClick); }
    interface MapDrag { void drag(double dx, double dy); }

    static abstract class Canvas extends Button.Plain {
        final MapClick click;
        final MapDrag drag;
        Canvas(int x, int y, int w, int h, MapClick click, MapDrag drag) {
            super(x, y, w, h, Component.empty(), b -> {}, DEFAULT_NARRATION);
            this.click = click;
            this.drag = drag;
        }
        @Override public void onClick(MouseButtonEvent e, boolean dbl) { click.click(e.x(), e.y(), e.button(), dbl); }
        @Override protected void onDrag(MouseButtonEvent e, double dx, double dy) { drag.drag(dx, dy); }
    }
}
