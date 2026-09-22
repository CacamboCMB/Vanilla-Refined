package de.ownmods.minimap.client;

import com.mojang.blaze3d.platform.NativeImage;
import de.ownmods.minimap.core.ExploredMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import de.ownmods.settings.client.compat.GuiTextureCompat;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.util.concurrent.atomic.AtomicInteger;

/** Tiny cached texture for the HUD minimap; removes per-frame per-pixel GUI fill calls. */
final class HudMapTexture implements AutoCloseable {
    private static final AtomicInteger IDS = new AtomicInteger();
    private static final long THROTTLE_NS = 95_000_000L;

    private DynamicTexture texture;
    private Identifier id;
    private int size;
    private int lastX = Integer.MIN_VALUE, lastZ = Integer.MIN_VALUE, lastBpp = -1, lastYawBucket = Integer.MIN_VALUE;
    private boolean lastCircle, lastPlayerUp;
    private long lastRevision = Long.MIN_VALUE, lastBuildNs;

    void draw(GuiGraphicsExtractor g, ExploredMap map, int playerX, int playerZ, int bpp,
              boolean circle, boolean playerUp, double yaw, int x, int y, int diameter) {
        ensure(diameter);
        int yawBucket = playerUp ? (int)Math.floor(Math.toDegrees(yaw) / 4.0) : 0;
        boolean changed = playerX != lastX || playerZ != lastZ || bpp != lastBpp || circle != lastCircle
                || playerUp != lastPlayerUp || yawBucket != lastYawBucket || map.revision() != lastRevision;
        long now = System.nanoTime();
        if (lastBuildNs == 0L || (changed && now - lastBuildNs >= THROTTLE_NS)) {
            rebuild(map, playerX, playerZ, bpp, circle, playerUp, yaw, yawBucket, now);
        }
        if (id != null) GuiTextureCompat.blit(g, id, x, y, 0, 0, diameter, diameter, diameter, diameter);
    }

    private void rebuild(ExploredMap map, int playerX, int playerZ, int bpp,
                         boolean circle, boolean playerUp, double yaw, int yawBucket, long now) {
        NativeImage pixels = texture.getPixels();
        if (pixels == null) return;
        pixels.fillRect(0, 0, size, size, 0x00000000);
        int c = size / 2;
        int r = Math.max(1, c - 3);
        int r2 = r * r;
        double sin = Math.sin(yaw), cos = Math.cos(yaw);
        for (int py = 0; py < size; py++) {
            int sy = py - c;
            for (int px = 0; px < size; px++) {
                int sx = px - c;
                if (circle && sx * sx + sy * sy > r2) continue;
                double wxOff;
                double wzOff;
                if (playerUp) {
                    wxOff = -cos * (sx * bpp) + sin * (sy * bpp);
                    wzOff = -sin * (sx * bpp) - cos * (sy * bpp);
                } else {
                    wxOff = sx * bpp;
                    wzOff = sy * bpp;
                }
                int color = map.colorAtBlockOr(playerX + (int)Math.round(wxOff), playerZ + (int)Math.round(wzOff), 0);
                if (color != 0) pixels.setPixel(px, py, color);
            }
        }
        texture.upload();
        lastX = playerX;
        lastZ = playerZ;
        lastBpp = bpp;
        lastCircle = circle;
        lastPlayerUp = playerUp;
        lastYawBucket = yawBucket;
        lastRevision = map.revision();
        lastBuildNs = now;
    }

    private void ensure(int diameter) {
        if (texture != null && size == diameter) return;
        close();
        size = diameter;
        texture = new DynamicTexture("OwnMods HUD minimap", diameter, diameter, true);
        id = Identifier.fromNamespaceAndPath("ownmods_minimap", "dynamic/hud_" + IDS.incrementAndGet());
        Minecraft.getInstance().getTextureManager().register(id, texture);
        lastBuildNs = 0L;
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
        size = 0;
        lastBuildNs = 0L;
        lastRevision = Long.MIN_VALUE;
        lastX = lastZ = Integer.MIN_VALUE;
        lastBpp = -1;
    }
}
