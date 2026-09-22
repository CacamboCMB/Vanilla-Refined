package de.ownmods.minimap.client;

import de.ownmods.minimap.core.ExploredMap;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Keeps the HUD atlas live by delegating terrain sampling to Minecraft's own MapItem algorithm.
 *
 * This class deliberately contains no block, biome, heightmap or chunk colour sampler.  It owns
 * temporary scale-0 MapItemSavedData instances, lets vanilla MapItem.update(...) fill them, and
 * copies only the resulting vanilla map bytes into the persistent atlas.
 */
final class VanillaLiveMapUpdater {
    private static final int MAP_SIZE = 128;
    private static final int HALF = MAP_SIZE / 2;
    private static final int MAX_VIRTUAL_MAPS = 12;

    private record Key(String dimension, int centerX, int centerZ) {}

    private static final class VirtualMap {
        final MapItemSavedData data;
        final byte[] copied = new byte[MAP_SIZE * MAP_SIZE];
        VirtualMap(MapItemSavedData data) { this.data = data; }
    }

    private final LinkedHashMap<Key, VirtualMap> maps = new LinkedHashMap<>(16, 0.75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<Key, VirtualMap> eldest) {
            return size() > MAX_VIRTUAL_MAPS;
        }
    };
    private boolean disabled;
    private int tick;

    VanillaLiveMapUpdater() {}

    void update(Minecraft mc, ExploredMap target, int revealRadiusBlocks) {
        if (disabled || mc.level == null || mc.player == null) return;
        if (!(Items.FILLED_MAP instanceof MapItem mapItem)) return;

        try {
            tick++;
            ResourceKey<Level> dimension = mc.level.dimension();
            String dimensionId = dimension.identifier().toString();
            int playerX = mc.player.blockPosition().getX();
            int playerZ = mc.player.blockPosition().getZ();

            // Reuse the current vanilla map while the player remains inside its 128x128 scale-0
            // footprint. createFresh is only needed when crossing into a new map tile, not every tick.
            VirtualMap virtual = findContaining(dimensionId, playerX, playerZ);
            if (virtual == null) {
                MapItemSavedData probe = MapItemSavedData.createFresh(playerX, playerZ, (byte) 0, true, false, dimension);
                Key current = new Key(dimensionId, probe.centerX, probe.centerZ);
                virtual = maps.computeIfAbsent(current, k -> new VirtualMap(probe));
            }
            mapItem.update(mc.level, mc.player, virtual.data);
            copyChanged(virtual, target, playerX, playerZ,
                    Math.max(8, Math.min(63, revealRadiusBlocks)));

            // Near a vanilla-map edge, refresh one adjacent tile occasionally. Vanilla's
            // update routine still does the sampling; this only avoids a blank wedge at the HUD edge.
            if ((tick & 7) == 0) {
                int dx = playerX - virtual.data.centerX;
                int dz = playerZ - virtual.data.centerZ;
                int edge = Math.max(18, Math.min(52, revealRadiusBlocks));
                int ox = Math.abs(dx) > HALF - edge ? Integer.signum(dx) * MAP_SIZE : 0;
                int oz = Math.abs(dz) > HALF - edge ? Integer.signum(dz) * MAP_SIZE : 0;
                if (ox != 0 || oz != 0) {
                    MapItemSavedData adjacentProbe = MapItemSavedData.createFresh(
                            virtual.data.centerX + ox, virtual.data.centerZ + oz, (byte) 0, true, false, dimension);
                    Key adjacentKey = new Key(dimensionId, adjacentProbe.centerX, adjacentProbe.centerZ);
                    VirtualMap adjacent = maps.computeIfAbsent(adjacentKey, k -> new VirtualMap(adjacentProbe));
                    mapItem.update(mc.level, mc.player, adjacent.data);
                    copyChanged(adjacent, target, playerX, playerZ,
                            Math.max(8, Math.min(63, revealRadiusBlocks)));
                }
            }
        } catch (RuntimeException ex) {
            // A mapping/runtime incompatibility must never turn back into the old expensive scanner.
            // Physical vanilla maps can still be imported by VanillaMapImporter.
            disabled = true;
            maps.clear();
        }
    }

    private VirtualMap findContaining(String dimension, int playerX, int playerZ) {
        VirtualMap best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (var entry : maps.entrySet()) {
            Key key = entry.getKey();
            if (!key.dimension().equals(dimension)) continue;
            int dx = Math.abs(playerX - key.centerX());
            int dz = Math.abs(playerZ - key.centerZ());
            if (dx >= HALF || dz >= HALF) continue;
            int distance = dx + dz;
            if (distance < bestDistance) {
                best = entry.getValue();
                bestDistance = distance;
            }
        }
        return best;
    }

    private static void copyChanged(VirtualMap source, ExploredMap target, int playerX, int playerZ, int radius) {
        byte[] colors = source.data.colors;
        if (colors == null || colors.length != MAP_SIZE * MAP_SIZE) return;
        int r2 = radius * radius;
        for (int i = 0; i < colors.length; i++) {
            byte packedByte = colors[i];
            if (packedByte == source.copied[i]) continue;
            int packed = packedByte & 0xFF;
            if (packed == 0) continue;
            int px = i & 127;
            int pz = i >>> 7;
            int worldX = source.data.centerX + px - HALF;
            int worldZ = source.data.centerZ + pz - HALF;
            int dx = worldX - playerX;
            int dz = worldZ - playerZ;
            if (dx * dx + dz * dz > r2) continue;
            // Mark a source pixel as copied only after it is inside the current reveal circle.
            // Previously pixels generated by vanilla outside the circle were marked as handled and
            // therefore never appeared when the player later walked into them.
            source.copied[i] = packedByte;
            target.putBlock(worldX, worldZ, MapColor.getColorFromPackedId(packed), 0);
        }
    }

    void reset() {
        for (VirtualMap map : maps.values()) Arrays.fill(map.copied, (byte) 0);
    }
}
