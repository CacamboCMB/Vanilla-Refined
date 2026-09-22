package de.ownmods.timber.core;

import de.ownmods.settings.api.ManagedMod;
import de.ownmods.settings.api.ManagedMods;
import de.ownmods.settings.api.ToggleOption;
import de.ownmods.settings.config.BooleanSettingsFile;
import de.ownmods.timber.TimberSettings;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/** No Minecraft classes, downloaded dependencies, mocking libraries or stubs. */
public final class CoreTests {
    @FunctionalInterface interface Checked { void run() throws Exception; }
    private static int passed;
    private static final GridPos ORIGIN = new GridPos(0,1,0);
    public static void main(String[] args) throws Exception {
        test("recognises a normal tree", () -> {
            var plan = plan(tree(0,0,"oak"), ORIGIN);
            equal(5, plan.logs().size()); equal(1, plan.roots().size()); check(plan.leaves().size() > 20);
        });
        test("planning does not mutate world", () -> {
            var world = tree(0,0,"oak"); var before = new HashMap<>(world);
            plan(world, ORIGIN); equal(before, world);
        });
        test("breaking a higher log finds lower trunk", () -> {
            var world = tree(0,0,"oak");
            equal(plan(world, ORIGIN).logs(), plan(world, new GridPos(0,4,0)).logs());
        });
        test("rejects ordinary blocks", () -> equal(TreePlanner.Reason.NOT_A_LOG, result(Map.of(), ORIGIN).reason()));
        test("rejects foreign or stripped log as trigger", () -> equal(TreePlanner.Reason.NOT_A_LOG,
                result(Map.of(ORIGIN, Voxel.log("")), ORIGIN).reason()));
        test("rejects a single placed log", () -> equal(TreePlanner.Reason.TOO_SMALL,
                result(Map.of(ORIGIN, Voxel.log("oak")), ORIGIN).reason()));
        test("rejects a leafless wood column", () -> {
            var world = tree(0,0,"oak"); world.entrySet().removeIf(e -> e.getValue().kind() == Voxel.Kind.LEAF);
            equal(TreePlanner.Reason.NO_NATURAL_LEAVES, result(world, ORIGIN).reason());
        });
        test("rejects crown consisting only of placed leaves", () -> {
            var world = tree(0,0,"oak");
            world.replaceAll((p,v) -> v.kind() == Voxel.Kind.LEAF ? Voxel.leaf("oak",true,7) : v);
            equal(TreePlanner.Reason.NO_NATURAL_LEAVES, result(world, ORIGIN).reason());
        });
        test("never selects placed leaves", () -> {
            var world = tree(0,0,"oak"); var placed = new GridPos(2,6,2);
            world.put(placed, Voxel.leaf("oak",true,7));
            check(!plan(world, ORIGIN).leaves().contains(placed));
        });
        test("rejects unsupported floating trunk", () -> {
            var world = tree(0,0,"oak"); world.remove(new GridPos(0,0,0));
            equal(TreePlanner.Reason.AMBIGUOUS_ROOTS, result(world, ORIGIN).reason());
        });
        test("does not select a different species of leaves", () -> {
            var world = tree(0,0,"oak"); var foreign = new GridPos(2,6,2);
            world.put(foreign, Voxel.leaf("birch",false,7));
            check(!plan(world, ORIGIN).leaves().contains(foreign));
        });
        test("leaf contact never pulls in second trunk", () -> {
            var world = tree(0,0,"oak"); merge(world, tree(5,0,"oak")); distances(world);
            var plan = plan(world, ORIGIN);
            equal(5, plan.logs().size()); check(plan.logs().stream().allMatch(p -> p.x() == 0));
            check(plan.leaves().stream().noneMatch(p -> p.x() >= 3));
        });
        test("shared equidistant leaves are retained", () -> {
            var world = tree(0,0,"oak"); merge(world, tree(6,0,"oak"));
            for (int x=2; x<=4; x++) world.put(new GridPos(x,5,0), Voxel.leaf("oak",false,7));
            distances(world);
            check(!plan(world, ORIGIN).leaves().contains(new GridPos(3,5,0)));
        });
        test("connected branches of separate rooted trees are rejected", () -> {
            var world = tree(0,0,"oak"); merge(world, tree(5,0,"oak"));
            for (int x=1;x<5;x++) world.put(new GridPos(x,4,0), Voxel.log("oak"));
            equal(TreePlanner.Reason.AMBIGUOUS_ROOTS, result(world, ORIGIN).reason());
        });
        test("adjacent two-root trunks are rejected", () -> {
            var world = tree(0,0,"oak"); merge(world, tree(1,0,"oak"));
            equal(TreePlanner.Reason.AMBIGUOUS_ROOTS, result(world, ORIGIN).reason());
        });
        test("recognises a 2x2 trunk", () -> {
            var world = new HashMap<GridPos,Voxel>();
            for (int x=0;x<2;x++) for(int z=0;z<2;z++) merge(world, tree(x,z,"spruce"));
            distances(world); var plan = plan(world, ORIGIN);
            equal(20,plan.logs().size()); equal(4,plan.roots().size());
        });
        test("rejects roots at mixed heights", () -> check(!TreePlanner.validRoots(List.of(
                new GridPos(0,1,0),new GridPos(1,1,0),new GridPos(0,1,1),new GridPos(1,2,1)))));
        test("rejects duplicate roots", () -> check(!TreePlanner.validRoots(List.of(ORIGIN,ORIGIN,ORIGIN,ORIGIN))));
        test("rejects four roots in a line", () -> check(!TreePlanner.validRoots(List.of(
                new GridPos(0,1,0),new GridPos(1,1,0),new GridPos(2,1,0),new GridPos(3,1,0)))));
        test("log count limit aborts entire plan", () -> {
            var limits = new TreePlanner.Limits(4,2048,12000,16,64,120000);
            equal(TreePlanner.Reason.SEARCH_LIMIT, new TreePlanner(limits).analyze(p -> treeCell(p), ORIGIN).reason());
        });
        test("leaf count limit aborts entire plan", () -> {
            var world = tree(0,0,"oak"); var limits = new TreePlanner.Limits(256,4,12000,16,64,120000);
            equal(TreePlanner.Reason.SEARCH_LIMIT,new TreePlanner(limits).analyze(p -> world.getOrDefault(p,Voxel.AIR),ORIGIN).reason());
        });
        test("leaf graph cap is respected", () -> {
            var world = tree(0,0,"oak"); var limits = new TreePlanner.Limits(256,4,4,16,64,120000);
            equal(TreePlanner.Reason.SEARCH_LIMIT,new TreePlanner(limits).analyze(p -> world.getOrDefault(p,Voxel.AIR),ORIGIN).reason());
        });
        test("horizontal bound aborts connected branch", () -> {
            var world = tree(0,0,"oak"); for(int x=1;x<=3;x++) world.put(new GridPos(x,4,0),Voxel.log("oak"));
            var limits = new TreePlanner.Limits(256,2048,12000,2,64,120000);
            equal(TreePlanner.Reason.SEARCH_LIMIT,new TreePlanner(limits).analyze(p -> world.getOrDefault(p,Voxel.AIR),ORIGIN).reason());
        });
        test("vertical bound is respected", () -> {
            var world = tree(0,0,"oak"); var limits = new TreePlanner.Limits(256,2048,12000,16,3,120000);
            equal(TreePlanner.Reason.SEARCH_LIMIT,new TreePlanner(limits).analyze(p -> world.getOrDefault(p,Voxel.AIR),ORIGIN).reason());
        });
        test("hard read budget is respected", () -> {
            var count = new AtomicInteger(); var world = tree(0,0,"oak");
            var limits = new TreePlanner.Limits(256,2048,12000,16,64,5);
            var r = new TreePlanner(limits).analyze(p -> {count.incrementAndGet();return world.getOrDefault(p,Voxel.AIR);},ORIGIN);
            equal(TreePlanner.Reason.SEARCH_LIMIT,r.reason()); check(count.get()<=5);
        });
        test("unloaded chunk adjacent to trunk causes refusal", () -> {
            var world = tree(0,0,"oak"); world.put(new GridPos(-1,1,0),Voxel.UNKNOWN);
            equal(TreePlanner.Reason.UNLOADED_CHUNK,result(world,ORIGIN).reason());
        });
        test("output is deterministic and immutable", () -> {
            var world=tree(0,0,"oak"); var p=plan(world,ORIGIN); equal(p,plan(world,ORIGIN));
            expect(UnsupportedOperationException.class, () -> p.logs().add(ORIGIN));
        });
        test("invalid planner limits rejected", () -> expect(IllegalArgumentException.class,
                () -> new TreePlanner.Limits(0,4,4,1,2,1)));
        test("missing configuration uses documented defaults", () -> {
            var store = store(temp("defaults")); check(store.snapshot().get("enabled")); check(!store.snapshot().get("replant"));
            check(store.warning().isEmpty());
        });
        test("save and reload roundtrip", () -> {
            var path=temp("roundtrip"); var store=store(path); var values=new LinkedHashMap<>(store.snapshot());
            values.put("enabled",false); values.put("replant",true); store.save(Map.copyOf(values));
            equal(Map.copyOf(values),store(path).snapshot());
        });
        test("snapshots are immutable", () -> expect(UnsupportedOperationException.class,
                () -> store(temp("immutable")).snapshot().put("enabled",false)));
        test("missing config keys retain defaults", () -> {
            var path=temp("partial"); Files.writeString(path,"enabled=false\n");
            var s=store(path); check(!s.snapshot().get("enabled")); check(!s.snapshot().get("replant"));
        });
        test("invalid boolean fails closed without overwriting original", () -> {
            var path=temp("invalid"); String content="enabled=definitely\nreplant=true\n"; Files.writeString(path,content);
            var s=store(path); check(!s.snapshot().get("enabled")); check(!s.warning().isBlank()); equal(content,Files.readString(path));
        });
        test("unknown schema version fails closed", () -> {
            var path=temp("schema"); Files.writeString(path,"schemaVersion=99\nenabled=true\n");
            check(!store(path).snapshot().get("enabled"));
        });
        test("unrecognised file keys survive save", () -> {
            var path=temp("unknown"); Files.writeString(path,"future_option=keep_me\nenabled=true\n");
            var s=store(path); s.save(s.snapshot()); check(Files.readString(path).contains("future_option=keep_me"));
        });
        test("failed persistence leaves active snapshot untouched", () -> {
            var path=temp("failure"); var s=store(path); var before=s.snapshot();
            Files.createDirectory(path); Files.writeString(path.resolve("keep"),"not replaceable");
            var values=new LinkedHashMap<>(before); values.put("enabled",false);
            expect(IOException.class,() -> s.save(values)); equal(before,s.snapshot());
        });
        test("save refuses missing schema keys", () -> expect(IllegalArgumentException.class,
                () -> store(temp("missingkey")).save(Map.of("enabled",true))));
        test("save refuses extra schema keys", () -> {
            var s=store(temp("extrakey")); var values=new LinkedHashMap<>(s.snapshot()); values.put("bogus",true);
            expect(IllegalArgumentException.class,() -> s.save(values));
        });
        test("save refuses null values", () -> {
            var s=store(temp("nullvalue")); var values=new LinkedHashMap<>(s.snapshot()); values.put("enabled",null);
            expect(IllegalArgumentException.class,() -> s.save(values));
        });
        test("explicit repair clears load warning", () -> {
            var path=temp("repair"); Files.writeString(path,"enabled=wrong\n"); var s=store(path);
            s.save(s.snapshot()); check(s.warning().isEmpty()); check(!store(path).snapshot().get("enabled"));
        });
        test("seven vanilla axe types including copper are offered", () -> {
            var s=new TimberSettings(temp("axes")); equal(7,TimberSettings.AXES.size()); equal(5,s.flags().allowedAxes().size());
            check(s.flags().allowedAxes().contains("minecraft:copper_axe"));
        });
        test("disabling all axe types yields empty allowlist", () -> {
            var s=new TimberSettings(temp("noaxes")); var values=new LinkedHashMap<>(s.snapshot());
            values.replaceAll((key,value) -> key.startsWith("axe.") ? false : value); s.save(values);
            check(s.flags().allowedAxes().isEmpty());
        });
        test("registry lists explicitly registered own modules", () -> {
            var s=new TimberSettings(temp("registry")); ManagedMods.register(s);
            check(ManagedMods.all().stream().anyMatch(m -> m.id().equals("ownmods_timber")));
        });
        test("registry rejects duplicate own-mod ids", () -> expect(IllegalStateException.class,
                () -> ManagedMods.register(new TimberSettings(temp("duplicate")))));
        test("registry list cannot be mutated", () -> expect(UnsupportedOperationException.class,
                () -> ManagedMods.all().clear()));
        passed += PatchTests.run();
        System.out.println("RESULT: "+passed+" passed, 0 failed. Pure Java tests only; Minecraft integration NOT tested.");
    }
    private static Map<GridPos,Voxel> tree(int x,int z,String family) {
        var world=new HashMap<GridPos,Voxel>(); world.put(new GridPos(x,0,z),Voxel.SOIL);
        for(int y=1;y<=5;y++) world.put(new GridPos(x,y,z),Voxel.log(family));
        for(int dx=-2;dx<=2;dx++) for(int dz=-2;dz<=2;dz++) for(int y=4;y<=6;y++)
            world.putIfAbsent(new GridPos(x+dx,y,z+dz),Voxel.leaf(family,false,7));
        distances(world); return world;
    }
    private static Voxel treeCell(GridPos p) { return tree(0,0,"oak").getOrDefault(p,Voxel.AIR); }
    private static void merge(Map<GridPos,Voxel> into,Map<GridPos,Voxel> source) {
        source.forEach((p,v) -> {if(v.kind()!=Voxel.Kind.LEAF || !into.containsKey(p)) into.put(p,v);});
    }
    private static void distances(Map<GridPos,Voxel> world) {
        var logs=world.entrySet().stream().filter(e->e.getValue().kind()==Voxel.Kind.LOG).map(Map.Entry::getKey).toList();
        world.replaceAll((p,v) -> v.kind()!=Voxel.Kind.LEAF ? v : Voxel.leaf(v.family(),v.persistent(),
                Math.min(7,logs.stream().mapToInt(l->Math.abs(l.x()-p.x())+Math.abs(l.y()-p.y())+Math.abs(l.z()-p.z())).min().orElse(7))));
    }
    private static TreePlanner.Result result(Map<GridPos,Voxel> world,GridPos origin) {
        return new TreePlanner().analyze(p->world.getOrDefault(p,Voxel.AIR),origin);
    }
    private static TreePlan plan(Map<GridPos,Voxel> world,GridPos origin) {
        var result=result(world,origin);
        if(result.plan().isEmpty()) throw new AssertionError("Expected tree, rejected: "+result.reason());
        return result.plan().orElseThrow();
    }
    private static Path temp(String label) throws IOException { return Files.createTempDirectory("ownmods-"+label).resolve("timber.properties"); }
    private static BooleanSettingsFile store(Path path) {
        return new BooleanSettingsFile(path,List.of(new ToggleOption("enabled","test","test.tip",true),new ToggleOption("replant","test2","test2.tip",false)));
    }
    private static void check(boolean condition) { if(!condition) throw new AssertionError("Condition was false"); }
    private static void equal(Object expected,Object actual) {
        if(!expected.equals(actual)) throw new AssertionError("Expected "+expected+"; got "+actual);
    }
    private static void expect(Class<? extends Throwable> type,Checked action) throws Exception {
        try { action.run(); } catch(Throwable e) {if(type.isInstance(e)) return; throw new AssertionError("Wrong exception: "+e,e);}
        throw new AssertionError("Expected exception "+type.getName());
    }
    private static void test(String name,Checked action) throws Exception {
        try {action.run(); passed++; System.out.println("PASS "+String.format("%02d",passed)+" "+name);}
        catch(Throwable e) {System.err.println("FAIL "+name); throw e;}
    }
}
