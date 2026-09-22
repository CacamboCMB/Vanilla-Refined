package de.ownmods.minimap.client;

import de.ownmods.minimap.core.ExploredMap;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.zip.CRC32;

/** Imports pixels produced by vanilla filled maps. It never reads world blocks. */
final class VanillaMapImporter {
    private final Map<Integer, Long> signatures = new HashMap<>();
    private int cursor;

    private Method allMapData;
    private Field colorsField, centerXField, centerZField, scaleField, dimensionField;

    @SuppressWarnings("unchecked")
    void importOne(Minecraft mc, String currentDimension, ExploredMap target) {
        if (mc.level == null) return;
        try {
            if (allMapData == null) {
                allMapData = findMethod(mc.level.getClass(), "getAllMapData");
                allMapData.setAccessible(true);
            }
            Object raw = allMapData.invoke(mc.level);
            if (!(raw instanceof Map<?, ?> maps) || maps.isEmpty()) return;

            List<Map.Entry<?, ?>> entries = new ArrayList<>(maps.entrySet());
            for (int attempt = 0; attempt < entries.size(); attempt++) {
                Map.Entry<?, ?> e = entries.get(Math.floorMod(cursor++, entries.size()));
                if (!(e.getKey() instanceof MapId id) || !(e.getValue() instanceof MapItemSavedData data)) continue;
                ensureFields(data.getClass());
                String dim = dimensionField.get(data).toString();
                // ResourceKey#toString is typically ResourceKey[minecraft:dimension / minecraft:overworld].
                // Compare by suffix to stay independent from that formatting.
                String wantedPath = currentDimension.substring(currentDimension.indexOf(':') + 1);
                if (!dim.contains(wantedPath)) continue;
                byte scale = ((Number) scaleField.get(data)).byteValue();
                if (scale != 0) continue; // 1 block = 1 pixel only.

                byte[] colors = (byte[]) colorsField.get(data);
                if (colors == null || colors.length != 128 * 128) continue;
                long sig = signature(colors);
                if (Objects.equals(signatures.get(id.id()), sig)) return;
                signatures.put(id.id(), sig);

                int centerX = ((Number) centerXField.get(data)).intValue();
                int centerZ = ((Number) centerZField.get(data)).intValue();
                for (int pz = 0; pz < 128; pz++) {
                    int wz = centerZ + pz - 64;
                    int row = pz * 128;
                    for (int px = 0; px < 128; px++) {
                        int packed = colors[row + px] & 0xFF;
                        if (packed == 0) continue; // vanilla unexplored pixel
                        int wx = centerX + px - 64;
                        target.putBlock(wx, wz, MapColor.getColorFromPackedId(packed), 0);
                    }
                }
                return; // at most one changed vanilla map per import pass
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // Fail closed: missing vanilla map internals should never re-enable the expensive old scanner.
        }
    }

    private void ensureFields(Class<?> type) throws ReflectiveOperationException {
        if (colorsField != null) return;
        colorsField = findField(type, "colors");
        centerXField = findField(type, "centerX");
        centerZField = findField(type, "centerZ");
        scaleField = findField(type, "scale");
        dimensionField = findField(type, "dimension");
        for (Field f : List.of(colorsField, centerXField, centerZField, scaleField, dimensionField)) f.setAccessible(true);
    }

    private static Method findMethod(Class<?> type, String name) throws NoSuchMethodException {
        Class<?> c = type;
        while (c != null) {
            try { return c.getDeclaredMethod(name); }
            catch (NoSuchMethodException ignored) { c = c.getSuperclass(); }
        }
        throw new NoSuchMethodException(name);
    }

    private static Field findField(Class<?> type, String name) throws NoSuchFieldException {
        Class<?> c = type;
        while (c != null) {
            try { return c.getDeclaredField(name); }
            catch (NoSuchFieldException ignored) { c = c.getSuperclass(); }
        }
        throw new NoSuchFieldException(name);
    }

    private static long signature(byte[] colors) {
        CRC32 crc = new CRC32();
        crc.update(colors, 0, colors.length);
        return crc.getValue();
    }
}
