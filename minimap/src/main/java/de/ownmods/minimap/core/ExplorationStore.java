package de.ownmods.minimap.core;

import java.io.*;
import java.nio.file.*;
import java.util.zip.*;

/**
 * One compressed file per dimension.
 * Format 2 stores one X/Z world block per map pixel.
 * Format 1 (old 2x2 cells) is migrated on load by expanding every old pixel to four 1x1 pixels.
 */
public final class ExplorationStore {
    private static final int MAGIC = 0x4F4D4D50;
    private static final int FORMAT = 2;
    private static final int LEGACY_FORMAT = 1;
    private static final int MAX_TILES = 8_000_000;

    private ExplorationStore() {}

    public static ExploredMap load(Path file, String dimension) throws IOException {
        ExploredMap map = new ExploredMap();
        if (!Files.exists(file)) return map;
        if (Files.size(file) > 1024L * 1024 * 1024) throw new IOException("Minimap-Datei zu groß");

        try (var in = new DataInputStream(new BufferedInputStream(new GZIPInputStream(Files.newInputStream(file))))) {
            if (in.readInt() != MAGIC) throw new IOException("Unbekanntes Minimap-Format");
            int format = in.readInt();
            if (format != FORMAT && format != LEGACY_FORMAT) throw new IOException("Unbekanntes Minimap-Format");
            if (!dimension.equals(in.readUTF())) throw new IOException("Falsche Dimension in Kartendatei");
            int n = in.readInt();
            if (n < 0 || n > MAX_TILES) throw new IOException("Ungültige Tile-Anzahl");

            if (format == FORMAT) loadCurrent(in, map, n);
            else loadLegacy2x2(in, map, n);

            if (in.read() != -1) throw new IOException("Unerwartete Zusatzdaten");
        } catch (EOFException e) {
            throw new IOException("Minimap-Datei unvollständig", e);
        }
        map.clean();
        return map;
    }

    private static void loadCurrent(DataInputStream in, ExploredMap map, int n) throws IOException {
        for (int k = 0; k < n; k++) {
            int tx = in.readInt(), tz = in.readInt();
            long mask = in.readLong();
            ExploredMap.Tile t = new ExploredMap.Tile();
            t.known = mask;
            for (int i = 0; i < ExploredMap.CELLS; i++) {
                t.colors[i] = in.readInt();
                t.heights[i] = in.readShort();
            }
            map.putTile(ExploredMap.key(tx, tz), t);
        }
    }

    /** Legacy format: 8x8 cells per tile, but every cell represented a 2x2 world-block square. */
    private static void loadLegacy2x2(DataInputStream in, ExploredMap map, int n) throws IOException {
        final int legacyCellsPerTile = 8;
        final int legacyCells = 64;
        for (int k = 0; k < n; k++) {
            int tx = in.readInt(), tz = in.readInt();
            long mask = in.readLong();
            int[] colors = new int[legacyCells];
            short[] heights = new short[legacyCells];
            for (int i = 0; i < legacyCells; i++) {
                colors[i] = in.readInt();
                heights[i] = in.readShort();
            }
            for (int i = 0; i < legacyCells; i++) {
                if ((mask & (1L << i)) == 0) continue;
                int lx = i % legacyCellsPerTile;
                int lz = i / legacyCellsPerTile;
                int oldCellX = tx * legacyCellsPerTile + lx;
                int oldCellZ = tz * legacyCellsPerTile + lz;
                int bx = oldCellX * 2;
                int bz = oldCellZ * 2;
                for (int dz = 0; dz < 2; dz++) for (int dx = 0; dx < 2; dx++) {
                    map.putBlock(bx + dx, bz + dz, colors[i], heights[i]);
                }
            }
        }
    }

    public static void save(Path file, String dimension, ExploredMap map) throws IOException {
        Files.createDirectories(file.toAbsolutePath().getParent());
        Path tmp = Files.createTempFile(file.toAbsolutePath().getParent(), "minimap-", ".tmp");
        try {
            try (var out = new DataOutputStream(new BufferedOutputStream(new GZIPOutputStream(Files.newOutputStream(tmp, StandardOpenOption.TRUNCATE_EXISTING))))) {
                out.writeInt(MAGIC);
                out.writeInt(FORMAT);
                out.writeUTF(dimension);
                out.writeInt(map.tileCount());
                for (var e : map.tiles().entrySet()) {
                    out.writeInt(ExploredMap.keyX(e.getKey()));
                    out.writeInt(ExploredMap.keyZ(e.getKey()));
                    var t = e.getValue();
                    out.writeLong(t.known);
                    for (int i = 0; i < ExploredMap.CELLS; i++) {
                        out.writeInt(t.colors[i]);
                        out.writeShort(t.heights[i]);
                    }
                }
            }
            if (Files.exists(file)) Files.copy(file, file.resolveSibling(file.getFileName() + ".bak"), StandardCopyOption.REPLACE_EXISTING);
            try {
                Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(tmp);
        }
    }
}
