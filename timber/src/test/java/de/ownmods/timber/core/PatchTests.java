package de.ownmods.timber.core;

import de.ownmods.settings.api.OptionIcon;
import de.ownmods.settings.layout.IconGridLayout;
import de.ownmods.timber.TimberSettings;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Tests the real planting transaction and layout math with an in-memory world, not Minecraft stubs. */
public final class PatchTests {
    @FunctionalInterface interface Checked { void run() throws Exception; }
    private static int passed;
    private static final GridPos ROOT = new GridPos(10, 64, 20);
    private static final List<GridPos> SINGLE = List.of(ROOT);
    private static final List<GridPos> QUAD = List.of(ROOT, ROOT.offset(1,0,0), ROOT.offset(0,0,1), ROOT.offset(1,0,1));
    private PatchTests() { }
    static int run() throws Exception {
        test("free replant works without any sapling drops or inventory", () -> {
            var w = new World();
            equal(Replanting.Result.PLANTED, Replanting.plant(SINGLE, false, w));
            check(w.isPlanted(ROOT)); equal(0, w.availableCalls); equal(0, w.consumeCalls);
        });
        test("free replant restores all four positions of a 2x2 tree", () -> {
            var w = new World();
            equal(Replanting.Result.PLANTED, Replanting.plant(QUAD, false, w));
            check(QUAD.stream().allMatch(w::isPlanted)); equal(4, w.attempts); equal(0, w.consumed);
        });
        test("free replant leaves existing item counts untouched", () -> {
            var w = new World(); w.items = 20;
            Replanting.plant(SINGLE, false, w); equal(20, w.items); equal(0, w.consumeCalls);
        });
        test("paid replant reports missing items without placing anything", () -> {
            var w = new World();
            equal(Replanting.Result.NO_SAPLINGS, Replanting.plant(SINGLE, true, w));
            equal(0, w.attempts); equal(0, w.consumeCalls);
        });
        test("paid 2x2 replant needs four items, not one", () -> {
            var w = new World(); w.items = 3;
            equal(Replanting.Result.NO_SAPLINGS, Replanting.plant(QUAD, true, w));
            equal(0, w.attempts); equal(3, w.items);
        });
        test("paid single-tree replant consumes exactly one item", () -> {
            var w = new World(); w.items = 8;
            equal(Replanting.Result.PLANTED, Replanting.plant(SINGLE, true, w));
            equal(1, w.consumed); equal(7, w.items);
        });
        test("paid 2x2 replant consumes exactly four items", () -> {
            var w = new World(); w.items = 8;
            equal(Replanting.Result.PLANTED, Replanting.plant(QUAD, true, w));
            equal(4, w.consumed); equal(4, w.items);
        });
        test("no planting in an unloaded chunk", () -> {
            var w = new World(); w.unloaded.add(ROOT);
            equal(Replanting.Result.UNLOADED, Replanting.plant(SINGLE, false, w)); equal(0, w.attempts);
        });
        test("no planting over an occupied root", () -> {
            var w = new World(); w.blocks.put(ROOT, "stone");
            equal(Replanting.Result.BLOCKED, Replanting.plant(SINGLE, false, w)); equal("stone", w.snapshot(ROOT));
        });
        test("unsuitable soil is reported before any placement", () -> {
            var w = new World(); w.badSoil.add(ROOT);
            equal(Replanting.Result.BAD_SOIL, Replanting.plant(SINGLE, false, w)); equal(0, w.attempts);
        });
        test("all four roots are validated before placing the first", () -> {
            var w = new World(); w.badSoil.add(QUAD.getLast());
            equal(Replanting.Result.BAD_SOIL, Replanting.plant(QUAD, false, w)); equal(0, w.attempts);
        });
        test("an empty root list is rejected", () -> {
            var w = new World();
            equal(Replanting.Result.INVALID_ROOTS, Replanting.plant(List.of(), false, w)); equal(0, w.attempts);
        });
        test("two roots are not a supported planting shape", () -> {
            var w = new World();
            equal(Replanting.Result.INVALID_ROOTS, Replanting.plant(QUAD.subList(0, 2), false, w));
        });
        test("duplicate root positions are rejected", () -> {
            var w = new World();
            equal(Replanting.Result.INVALID_ROOTS, Replanting.plant(List.of(ROOT,ROOT,ROOT,ROOT), false, w));
        });
        test("mixed-height roots are rejected", () -> {
            var roots = new ArrayList<>(QUAD); roots.set(3, roots.get(3).offset(0,1,0));
            equal(Replanting.Result.INVALID_ROOTS, Replanting.plant(roots, false, new World()));
        });
        test("failed single placement consumes no items", () -> {
            var w = new World(); w.items = 4; w.failAt = 1;
            equal(Replanting.Result.PLACEMENT_FAILED, Replanting.plant(SINGLE, true, w));
            equal(4, w.items); equal(0, w.consumed); check(w.empty(ROOT));
        });
        test("failed third placement rolls back the earlier 2x2 saplings", () -> {
            var w = new World(); w.items = 8; w.failAt = 3;
            equal(Replanting.Result.PLACEMENT_FAILED, Replanting.plant(QUAD, true, w));
            check(QUAD.stream().allMatch(w::empty)); equal(8, w.items); equal(0, w.consumeCalls);
        });
        test("false placement result after a write also rolls back the failed root", () -> {
            var w = new World(); w.failAt = 2; w.writeBeforeFailure = true;
            equal(Replanting.Result.PLACEMENT_FAILED, Replanting.plant(QUAD, false, w));
            check(QUAD.stream().allMatch(w::empty));
        });
        test("failed item commit rolls back all placed saplings", () -> {
            var w = new World(); w.items = 8; w.rejectConsumption = true;
            equal(Replanting.Result.RESOURCES_CHANGED, Replanting.plant(QUAD, true, w));
            check(QUAD.stream().allMatch(w::empty)); equal(8, w.items); equal(0, w.consumed);
        });
        test("placement exceptions roll back without hiding the exception", () -> {
            var w = new World(); w.throwAt = 2;
            expect(IllegalStateException.class, () -> Replanting.plant(QUAD, false, w));
            check(QUAD.stream().allMatch(w::empty));
        });
        test("rollback does not overwrite another block placed by a callback", () -> {
            var w = new World(); w.foreignAfterWrite = true;
            equal(Replanting.Result.ROLLBACK_FAILED, Replanting.plant(SINGLE, false, w));
            equal("stone", w.snapshot(ROOT));
        });
        test("no replanting on the higher log position", () -> {
            var higherOrigin = ROOT.offset(0,3,0);
            var plan = new TreePlan("oak", higherOrigin, List.of(ROOT, higherOrigin), List.of(), SINGLE);
            var w = new World();
            equal(Replanting.Result.PLANTED, Replanting.plant(plan.roots(), false, w));
            check(w.isPlanted(ROOT)); check(w.empty(higherOrigin));
        });
        test("fresh configurations use the approved paid-replant defaults", () -> {
            var s = settings("fresh"); check(s.flags().consumeSaplings()); check(s.flags().replant());
        });
        test("old config preserves enabled replant and disabled axes", () -> {
            var path = temp("migration");
            Files.writeString(path, "schemaVersion=1\nenabled=true\nreplant=true\nconsume_saplings=false\naxe.minecraft\\:iron_axe=false\n");
            var s = new TimberSettings(path);
            check(s.flags().replant()); check(!s.flags().consumeSaplings());
            check(!s.flags().allowedAxes().contains("minecraft:iron_axe")); check(s.warning().isEmpty());
        });
        test("old disabled replant remains disabled during migration", () -> {
            var path = temp("migration-off"); Files.writeString(path,"replant=false\nenabled=true\n");
            check(!new TimberSettings(path).flags().replant());
        });
        test("paid replant option persists independently", () -> {
            var path = temp("paid-save"); var s = new TimberSettings(path);
            var values = new LinkedHashMap<>(s.snapshot()); values.put("replant", true); values.put("consume_saplings", true);
            s.save(values); var reopened = new TimberSettings(path);
            check(reopened.flags().replant()); check(reopened.flags().consumeSaplings());
        });
        test("every Timber option has a real-item icon descriptor", () -> {
            var s = settings("icons"); equal(s.snapshot().keySet(), s.optionIcons().keySet()); equal(12, s.optionIcons().size());
        });
        test("each axe uses its own corresponding item icon", () -> {
            var s = settings("axe-icons");
            for (var axe : TimberSettings.AXES) equal("minecraft:" + axe, s.optionIcons().get("axe.minecraft:" + axe).itemId());
        });
        test("replant uses a sapling and leaves use a leaf-block icon", () -> {
            var s = settings("feature-icons");
            equal("minecraft:oak_sapling", s.optionIcons().get("replant").itemId());
            equal("minecraft:oak_leaves", s.optionIcons().get("leaves").itemId());
        });
        test("icon map cannot be modified", () -> expect(UnsupportedOperationException.class,
                () -> settings("immutable-icons").optionIcons().clear()));
        test("invalid icon IDs are rejected", () -> expect(IllegalArgumentException.class,
                () -> new OptionIcon("Minecraft:Bad Item", "group")));
        test("blank section headings are rejected", () -> expect(IllegalArgumentException.class,
                () -> new OptionIcon("minecraft:oak_log", " ")));
        test("all 12 settings fit one normal 380x240 content layout", () -> {
            var s = settings("normal-layout"); var g = IconGridLayout.create(s.options(), s.optionIcons(), 380, 240, 0);
            equal(7, g.columns()); equal(2, g.rowsPerPage()); equal(1, g.pageCount());
            equal(5, g.visibleRows().get(0).options().size()); equal(7, g.visibleRows().get(1).options().size());
        });
        test("small layout retains every option across pages without duplicates", () -> {
            var s = settings("small-layout"); var seen = new HashSet<String>();
            var first = IconGridLayout.create(s.options(), s.optionIcons(), 148, 240, 0);
            equal(3, first.columns()); equal(3, first.pageCount());
            for (int p = 0; p < first.pageCount(); p++) {
                var g = IconGridLayout.create(s.options(), s.optionIcons(), 148, 240, p);
                for (var row : g.visibleRows()) for (var o : row.options()) check(seen.add(o.key()));
            }
            equal(s.snapshot().keySet(), seen);
        });
        test("page index is clamped after resize", () -> {
            var s = settings("resize");
            equal(0, IconGridLayout.create(s.options(),s.optionIcons(),380,500,99).page());
            equal(0, IconGridLayout.create(s.options(),s.optionIcons(),148,240,-5).page());
        });
        test("missing icons cause a clear layout error", () -> {
            var s = settings("missing-icon");
            expect(IllegalArgumentException.class, () -> IconGridLayout.create(s.options(),Map.of(),380,240,0));
        });
        test("empty generic icon schema has one empty page", () -> {
            var g = IconGridLayout.create(List.of(),Map.of(),380,240,0);
            equal(1, g.pageCount()); check(g.visibleRows().isEmpty());
        });
        test("layout rows are immutable", () -> {
            var s = settings("immutable-layout"); var g = IconGridLayout.create(s.options(),s.optionIcons(),380,240,0);
            expect(UnsupportedOperationException.class, () -> g.rows().clear());
            expect(UnsupportedOperationException.class, () -> g.rows().getFirst().options().clear());
        });
        test("all icon rows fit above footer at supported GUI dimensions", () -> {
            var s = settings("bounds");
            for (int width : List.of(296, 380, 254, 148)) for (int height : List.of(240, 270, 360, 540)) {
                var first = IconGridLayout.create(s.options(),s.optionIcons(),width,height,0);
                for (int p = 0; p < first.pageCount(); p++) {
                    var g = IconGridLayout.create(s.options(),s.optionIcons(),width,height,p);
                    for (int r = 0; r < g.visibleRows().size(); r++) {
                        var row = g.visibleRows().get(r);
                        check(row.options().size() * (IconGridLayout.SIZE + IconGridLayout.GAP) - IconGridLayout.GAP <= width);
                        check(IconGridLayout.TOP + r * IconGridLayout.ROW_HEIGHT + 14 + IconGridLayout.SIZE <= height - 72);
                    }
                }
            }
        });
        System.out.println("PATCH RESULT: " + passed + " passed. In-memory logic/layout tests; NOT an in-game test.");
        return passed;
    }
    private static final class World implements Replanting.Access<String> {
        final Map<GridPos, String> blocks = new HashMap<>();
        final Set<GridPos> unloaded = new HashSet<>(), badSoil = new HashSet<>();
        int items, consumed, attempts, availableCalls, consumeCalls, failAt = -1, throwAt = -1;
        boolean rejectConsumption, writeBeforeFailure, foreignAfterWrite;
        @Override public boolean loaded(GridPos p) { return !unloaded.contains(p); }
        @Override public boolean empty(GridPos p) { return snapshot(p).equals("air"); }
        @Override public boolean canSurvive(GridPos p) { return !badSoil.contains(p); }
        @Override public String snapshot(GridPos p) { return blocks.getOrDefault(p, "air"); }
        @Override public boolean place(GridPos p) {
            attempts++;
            if (attempts == failAt && !writeBeforeFailure) return false;
            blocks.put(p, "sapling");
            if (attempts == throwAt) throw new IllegalStateException("test placement exception");
            if (foreignAfterWrite) blocks.put(p, "stone");
            return attempts != failAt;
        }
        @Override public boolean isPlanted(GridPos p) { return snapshot(p).equals("sapling"); }
        @Override public boolean restore(GridPos p, String before) {
            if (snapshot(p).equals(before)) return true;
            if (!isPlanted(p)) return false;
            blocks.put(p, before); return true;
        }
        @Override public int availableSaplings() { availableCalls++; return items; }
        @Override public boolean consumeSaplings(int amount) {
            consumeCalls++;
            if (rejectConsumption || items < amount) return false;
            items -= amount; consumed += amount; return true;
        }
    }
    private static Path temp(String label) throws Exception { return Files.createTempDirectory("ownmods-patch-"+label).resolve("timber.properties"); }
    private static TimberSettings settings(String label) throws Exception { return new TimberSettings(temp(label)); }
    private static void check(boolean condition) { if (!condition) throw new AssertionError("Condition was false"); }
    private static void equal(Object expected, Object actual) {
        if (!expected.equals(actual)) throw new AssertionError("Expected " + expected + "; got " + actual);
    }
    private static void expect(Class<? extends Throwable> type, Checked action) throws Exception {
        try { action.run(); } catch (Throwable e) { if (type.isInstance(e)) return; throw new AssertionError("Wrong exception",e); }
        throw new AssertionError("Expected exception " + type.getName());
    }
    private static void test(String name, Checked action) throws Exception {
        try { action.run(); passed++; System.out.printf("PATCH PASS %02d %s%n", passed, name); }
        catch (Throwable e) { System.err.println("PATCH FAIL " + name); throw e; }
    }
}
