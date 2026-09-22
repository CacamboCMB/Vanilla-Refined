package de.ownmods.minimap.client;
import de.ownmods.settings.client.UiText;

import de.ownmods.minimap.core.ExploredMap;
import de.ownmods.minimap.core.ExplorationStore;
import de.ownmods.notes.client.NotesBridge;
import de.ownmods.notes.core.Note;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Persistent atlas. Vanilla MapItem data remains the preferred/canonical source.
 *
 * Live exploration is produced by VanillaLiveMapUpdater, which delegates sampling to
 * MapItem.update(...). A bounded loaded-chunk fallback fills otherwise-black parts of the currently
 * visible HUD footprint without forcing chunk loads; vanilla data may refine those pixels later.
 */
final class MinimapSession implements AutoCloseable {
    static final List<String> DIMS = List.of("minecraft:overworld", "minecraft:the_nether", "minecraft:the_end");

    final String scope;
    final Path root;
    private final Map<String, ExploredMap> maps = new HashMap<>();
    private final Set<String> loaded = new HashSet<>();
    private final VanillaMapImporter importer = new VanillaMapImporter();
    private final VanillaLiveMapUpdater live = new VanillaLiveMapUpdater();
    private final LoadedTerrainReveal loadedReveal = new LoadedTerrainReveal();
    private int ticks;
    private String targetId = "";
    private List<Note> notes = List.of();

    MinimapSession() throws IOException {
        scope = NotesBridge.scopeKey();
        root = FabricLoader.getInstance().getConfigDir().resolve("ownmods/minimap/vanilla-atlas/" + scope);
        Files.createDirectories(root);
        refreshNotes();
    }

    String targetId() { return targetId; }
    void target(String id) { targetId = id == null ? "" : id; }

    private Path file(String dim) {
        return root.resolve(dim.substring(dim.indexOf(':') + 1) + ".vanilla.omm.gz");
    }

    ExploredMap map(String dim) {
        try {
            if (!loaded.contains(dim)) {
                maps.put(dim, ExplorationStore.load(file(dim), dim));
                loaded.add(dim);
            }
            return maps.computeIfAbsent(dim, d -> new ExploredMap());
        } catch (IOException e) {
            throw new IllegalStateException(UiText.tr("vr.text.a4d3a291c9e3") + e.getMessage(), e);
        }
    }

    List<Note> notes() { return notes; }

    void refreshNotes() {
        try { notes = NotesBridge.activeLocations(); }
        catch (Exception e) { notes = List.of(); }
    }

    /**
     * One vanilla map update per client tick plus a bounded loaded-chunk reveal pass.
     * The radius is expressed in world blocks and follows the complete visible HUD footprint,
     * including the configured HUD zoom.
     */
    void tick(Minecraft mc, int revealRadiusBlocks) {
        ticks++;
        if (ticks % 20 == 0) refreshNotes();
        if (mc.player == null || mc.level == null) return;

        String currentDim = mc.level.dimension().identifier().toString();
        ExploredMap current = map(currentDim);
        live.update(mc, current, revealRadiusBlocks);
        loadedReveal.update(mc, current, revealRadiusBlocks);

        // Import held/received physical map data as a secondary source, but at a much lower cadence.
        if (ticks % 80 == 0) importer.importOne(mc, currentDim, current);
        if (ticks % 600 == 0) flushDirty();
    }

    boolean hasVanillaPixels(String dim) { return map(dim).tileCount() > 0; }

    void flushDirty() {
        for (String dim : new ArrayList<>(loaded)) {
            ExploredMap m = maps.get(dim);
            if (m != null && m.dirty()) {
                try {
                    ExplorationStore.save(file(dim), dim, m.copy());
                    m.clean();
                } catch (IOException ignored) {}
            }
        }
    }

    @Override public void close() {
        flushDirty();
        live.reset();
        loadedReveal.reset();
    }
}
