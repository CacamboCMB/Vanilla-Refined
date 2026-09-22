package de.ownmods.minimap.client;

import com.mojang.blaze3d.platform.NativeImage;
import de.ownmods.minimap.core.ExploredMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import de.ownmods.settings.client.compat.GuiTextureCompat;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Screen-sized cached raster for the large world map.
 *
 * The expensive atlas-to-screen conversion is never executed once per GUI frame. A DynamicTexture
 * is refreshed at a bounded cadence and the actual screen render is one textured blit.
 */
final class MapRasterTexture implements AutoCloseable {
    private static final AtomicInteger IDS = new AtomicInteger();
    private static final long VIEW_THROTTLE_NS = 60_000_000L;   // ~16 fps while panning/zooming
    private static final long DATA_THROTTLE_NS = 300_000_000L; // map data can arrive slower than GUI frames

    private final String label;
    private DynamicTexture texture;
    private Identifier id;
    private int width, height;
    private int lastCenterX = Integer.MIN_VALUE, lastCenterZ = Integer.MIN_VALUE, lastBpp = -1;
    private String lastDimension = "";
    private long lastRevision = Long.MIN_VALUE;
    private long lastBuildNs;

    MapRasterTexture(String label) { this.label = label; }

    void invalidate() {
        lastCenterX = Integer.MIN_VALUE;
        lastCenterZ = Integer.MIN_VALUE;
        lastBpp = -1;
        lastDimension = "";
        lastRevision = Long.MIN_VALUE;
    }

    void draw(GuiGraphicsExtractor g, ExploredMap map, String dimension,
              int centerX, int centerZ, int blocksPerPixel,
              int x, int y, int w, int h) {
        if (w <= 2 || h <= 2) return;
        ensure(w, h);
        long now = System.nanoTime();
        boolean viewChanged = centerX != lastCenterX || centerZ != lastCenterZ
                || blocksPerPixel != lastBpp || !dimension.equals(lastDimension);
        boolean dataChanged = map.revision() != lastRevision;
        boolean due = lastBuildNs == 0L
                || (viewChanged && now - lastBuildNs >= VIEW_THROTTLE_NS)
                || (!viewChanged && dataChanged && now - lastBuildNs >= DATA_THROTTLE_NS);
        if (due) rebuild(map, dimension, centerX, centerZ, blocksPerPixel, now);
        if (id != null) GuiTextureCompat.blit(g, id, x, y, 0, 0, w, h, w, h);
    }

    private void rebuild(ExploredMap map, String dimension, int centerX, int centerZ, int bpp, long now) {
        NativeImage pixels = texture.getPixels();
        if (pixels == null) return;
        pixels.fillRect(0, 0, width, height, 0x00000000);

        int halfW = width / 2;
        int halfH = height / 2;
        if (bpp == 1) {
            // At native 1:1 zoom iterate only known atlas pixels. This avoids hundreds of thousands
            // of hash lookups when most of the visible map is still fog-of-war.
            int minX = centerX - halfW;
            int minZ = centerZ - halfH;
            int maxX = centerX + (width - halfW - 1);
            int maxZ = centerZ + (height - halfH - 1);
            map.forEachPixelInBounds(minX, minZ, maxX, maxZ, (worldX, worldZ, color, ignoredHeight) -> {
                int px = worldX - centerX + halfW;
                int py = worldZ - centerZ + halfH;
                if (px >= 0 && py >= 0 && px < width && py < height) pixels.setPixel(px, py, color);
            });
        } else {
            // Zoomed out: a fixed amount of work per display pixel is cheaper than walking every
            // stored 1:1 block in the much larger world rectangle.
            for (int py = 0; py < height; py++) {
                int worldZ = centerZ + (py - halfH) * bpp;
                for (int px = 0; px < width; px++) {
                    int worldX = centerX + (px - halfW) * bpp;
                    int color = map.colorAtBlockOr(worldX, worldZ, 0);
                    if (color != 0) pixels.setPixel(px, py, color);
                }
            }
        }
        texture.upload();
        lastCenterX = centerX;
        lastCenterZ = centerZ;
        lastBpp = bpp;
        lastDimension = dimension;
        lastRevision = map.revision();
        lastBuildNs = now;
    }

    private void ensure(int w, int h) {
        if (texture != null && width == w && height == h) return;
        close();
        width = w;
        height = h;
        texture = new DynamicTexture("OwnMods minimap " + label, w, h, true);
        id = Identifier.fromNamespaceAndPath("ownmods_minimap", "dynamic/" + label + "_" + IDS.incrementAndGet());
        Minecraft.getInstance().getTextureManager().register(id, texture);
        invalidate();
    }

    @Override public void close() {
        if (id != null) {
            try { Minecraft.getInstance().getTextureManager().release(id); }
            catch (RuntimeException ignored) {}
        } else if (texture != null) {
            try { texture.close(); } catch (RuntimeException ignored) {}
        }
        texture = null;
        id = null;
        width = height = 0;
        lastBuildNs = 0L;
        invalidate();
    }
}
