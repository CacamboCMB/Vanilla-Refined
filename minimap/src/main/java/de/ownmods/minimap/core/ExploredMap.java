package de.ownmods.minimap.core;

import java.util.*;

/**
 * Persistent exploration grid.
 *
 * Resolution is intentionally 1:1: one world block in X/Z equals one stored map pixel.
 * Tiles stay compact (8x8 pixels) so the existing 64-bit known-mask remains efficient.
 */
public final class ExploredMap {
    public static final int BLOCKS_PER_CELL = 1;
    public static final int CELLS_PER_TILE = 8;
    public static final int CELLS = CELLS_PER_TILE * CELLS_PER_TILE;
    public static final int BLOCKS_PER_TILE = BLOCKS_PER_CELL * CELLS_PER_TILE;

    private final LinkedHashMap<Long, Tile> tiles = new LinkedHashMap<>();
    private boolean dirty;
    private long revision;

    public record Sample(int color, int height) {}

    @FunctionalInterface
    public interface PixelConsumer {
        void accept(int worldX, int worldZ, int color, int height);
    }

    public static final class Tile {
        long known;
        final int[] colors = new int[CELLS];
        final short[] heights = new short[CELLS];

        Tile copy() {
            Tile t = new Tile();
            t.known = known;
            System.arraycopy(colors, 0, t.colors, 0, CELLS);
            System.arraycopy(heights, 0, t.heights, 0, CELLS);
            return t;
        }
    }

    public static long key(int tx, int tz) { return ((long) tx << 32) ^ (tz & 0xffffffffL); }
    public static int keyX(long key) { return (int) (key >> 32); }
    public static int keyZ(long key) { return (int) key; }

    private static int cell(int block) { return Math.floorDiv(block, BLOCKS_PER_CELL); }
    private static int tile(int cell) { return Math.floorDiv(cell, CELLS_PER_TILE); }
    private static int local(int cell) { return Math.floorMod(cell, CELLS_PER_TILE); }

    /** Allocation-free known-mask lookup for live reveal and rendering hot paths. */
    public boolean knownAtBlock(int x, int z) {
        int cx = cell(x), cz = cell(z);
        Tile t = tiles.get(key(tile(cx), tile(cz)));
        int i = local(cz) * CELLS_PER_TILE + local(cx);
        return t != null && (t.known & (1L << i)) != 0;
    }

    public Optional<Sample> sampleAtBlock(int x, int z) {
        int cx = cell(x), cz = cell(z);
        int tx = tile(cx), tz = tile(cz);
        int i = local(cz) * CELLS_PER_TILE + local(cx);
        Tile t = tiles.get(key(tx, tz));
        return t == null || (t.known & (1L << i)) == 0
                ? Optional.empty()
                : Optional.of(new Sample(t.colors[i], t.heights[i]));
    }

    /** Allocation-free lookup used by the HUD texture cache. */
    public int colorAtBlockOr(int x, int z, int fallback) {
        int cx = cell(x), cz = cell(z);
        Tile t = tiles.get(key(tile(cx), tile(cz)));
        int i = local(cz) * CELLS_PER_TILE + local(cx);
        return t == null || (t.known & (1L << i)) == 0 ? fallback : t.colors[i];
    }

    public void putBlock(int x, int z, int color, int height) {
        int cx = cell(x), cz = cell(z);
        int tx = tile(cx), tz = tile(cz);
        int i = local(cz) * CELLS_PER_TILE + local(cx);
        Tile t = tiles.computeIfAbsent(key(tx, tz), k -> new Tile());
        int c = 0xFF000000 | (color & 0xFFFFFF);
        short h = (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, height));
        if ((t.known & (1L << i)) == 0 || t.colors[i] != c || t.heights[i] != h) {
            t.known |= 1L << i;
            t.colors[i] = c;
            t.heights[i] = h;
            dirty = true;
            revision++;
        }
    }

    /**
     * Iterates only known pixels whose world positions intersect the requested rectangle.
     * This is intentionally tile-based so opening the large map does not perform hundreds
     * of thousands of hash lookups or allocate Optional/Sample objects every frame.
     */
    public void forEachPixelInBounds(int minX, int minZ, int maxX, int maxZ, PixelConsumer consumer) {
        if (maxX < minX || maxZ < minZ) return;
        int minTx = Math.floorDiv(minX, BLOCKS_PER_TILE);
        int maxTx = Math.floorDiv(maxX, BLOCKS_PER_TILE);
        int minTz = Math.floorDiv(minZ, BLOCKS_PER_TILE);
        int maxTz = Math.floorDiv(maxZ, BLOCKS_PER_TILE);
        for (int tz = minTz; tz <= maxTz; tz++) {
            for (int tx = minTx; tx <= maxTx; tx++) {
                Tile t = tiles.get(key(tx, tz));
                if (t == null || t.known == 0L) continue;
                long mask = t.known;
                while (mask != 0L) {
                    int i = Long.numberOfTrailingZeros(mask);
                    mask &= mask - 1;
                    int lx = i % CELLS_PER_TILE;
                    int lz = i / CELLS_PER_TILE;
                    int wx = tx * BLOCKS_PER_TILE + lx;
                    int wz = tz * BLOCKS_PER_TILE + lz;
                    if (wx < minX || wx > maxX || wz < minZ || wz > maxZ) continue;
                    consumer.accept(wx, wz, t.colors[i], t.heights[i]);
                }
            }
        }
    }

    public int tileCount() { return tiles.size(); }
    public boolean dirty() { return dirty; }
    public void clean() { dirty = false; }
    public long revision() { return revision; }
    public Map<Long, Tile> tiles() { return Collections.unmodifiableMap(tiles); }
    public void putTile(long key, Tile tile) { tiles.put(key, tile); }

    public ExploredMap copy() {
        ExploredMap m = new ExploredMap();
        for (var e : tiles.entrySet()) m.tiles.put(e.getKey(), e.getValue().copy());
        m.dirty = dirty;
        m.revision = revision;
        return m;
    }

    public Optional<int[]> blockBounds() {
        if (tiles.isEmpty()) return Optional.empty();
        int minX = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (long k : tiles.keySet()) {
            int tx = keyX(k), tz = keyZ(k);
            minX = Math.min(minX, tx * BLOCKS_PER_TILE);
            minZ = Math.min(minZ, tz * BLOCKS_PER_TILE);
            maxX = Math.max(maxX, tx * BLOCKS_PER_TILE + BLOCKS_PER_TILE - 1);
            maxZ = Math.max(maxZ, tz * BLOCKS_PER_TILE + BLOCKS_PER_TILE - 1);
        }
        return Optional.of(new int[]{minX, minZ, maxX, maxZ});
    }
}
