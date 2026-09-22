package de.ownmods.minimap.client;

import de.ownmods.minimap.core.ExploredMap;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;

/**
 * Bounded fallback that fills unknown pixels across the full HUD footprint from chunks the
 * client already has. It never requests/loads chunks. Vanilla MapItem data remains canonical:
 * this class writes only unknown cells and the normal vanilla updater can refine them later.
 *
 * The fallback uses Minecraft's own BlockState MapColor palette rather than a custom terrain
 * palette so newly revealed cells stay visually compatible with the vanilla-map-backed atlas.
 */
final class LoadedTerrainReveal {
    private static final int MAX_RADIUS = 512;
    private static final int MAP_COLOR_DESCENT = 8;

    private String dimension = "";
    private int centerX;
    private int centerZ;
    private int radius = -1;
    private int diameter;
    private int cursor;
    private int cooldown;
    private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

    void update(Minecraft mc, ExploredMap target, int requestedRadius) {
        if (mc.level == null || mc.player == null) return;
        int wanted = Math.max(8, Math.min(MAX_RADIUS, requestedRadius));
        String dim = mc.level.dimension().identifier().toString();
        int px = mc.player.blockPosition().getX();
        int pz = mc.player.blockPosition().getZ();

        int recenterDistance = Math.max(12, wanted / 4);
        boolean moved = Math.abs(px - centerX) > recenterDistance || Math.abs(pz - centerZ) > recenterDistance;
        if (!dim.equals(dimension) || wanted != radius || diameter == 0 || moved) reset(dim, px, pz, wanted);

        if (cooldown > 0) {
            cooldown--;
            return;
        }

        int budget = wanted <= 64 ? 5200 : wanted <= 192 ? 3600 : 2400;
        int total = diameter * diameter;
        int visited = 0;
        while (cursor < total && visited < budget) {
            int index = cursor++;
            visited++;
            int dx = index % diameter - radius;
            int dz = index / diameter - radius;
            if (dx * dx + dz * dz > radius * radius) continue;

            int x = centerX + dx;
            int z = centerZ + dz;
            if (target.knownAtBlock(x, z)) continue;
            if (!mc.level.hasChunk(x >> 4, z >> 4)) continue;

            try {
                int y = mc.level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
                MapColor mapColor = MapColor.NONE;
                BlockState state = null;
                for (int step = 0; step <= MAP_COLOR_DESCENT; step++) {
                    pos.set(x, y - step, z);
                    state = mc.level.getBlockState(pos);
                    mapColor = state.getMapColor(mc.level, pos);
                    if (mapColor != MapColor.NONE) {
                        y -= step;
                        break;
                    }
                }
                if (state == null || mapColor == MapColor.NONE) continue;
                int color = mapColor.calculateARGBColor(MapColor.Brightness.NORMAL);
                target.putBlock(x, z, color, y);
            } catch (RuntimeException ignored) {
                // A transient chunk/height race leaves the cell unknown so a later pass may retry it.
            }
        }

        if (cursor >= total) {
            cursor = 0;
            centerX = px;
            centerZ = pz;
            cooldown = 20;
        }
    }

    private void reset(String dim, int x, int z, int newRadius) {
        dimension = dim;
        centerX = x;
        centerZ = z;
        radius = newRadius;
        diameter = radius * 2 + 1;
        cursor = 0;
        cooldown = 0;
    }

    void reset() {
        dimension = "";
        radius = -1;
        diameter = 0;
        cursor = 0;
        cooldown = 0;
    }
}
