package de.ownmods.tests;

import de.ownmods.settings.api.*;
import de.ownmods.settings.policy.*;
import de.ownmods.timber.TimberSettings;
import de.ownmods.cropreplant.CropReplantSettings;
import de.ownmods.inventorysort.InventorySortSettings;
import de.ownmods.toolswap.ToolSwapSettings;
import de.ownmods.gravestone.GravestoneSettings;
import de.ownmods.shulkerpreview.ShulkerPreviewSettings;
import de.ownmods.minimap.MinimapSettings;
import de.ownmods.inventorysort.core.*;
import java.nio.file.*;
import java.util.*;

/** Runs the actual pure policy/codec/config/transaction code, not Minecraft API stand-ins. */
public final class FoundationTests {
    private static int assertions;
    @FunctionalInterface interface Checked { void run() throws Exception; }
    private static void ok(String name, boolean value) {
        if (!value) throw new AssertionError(name); assertions++;
    }
    private static void rejects(String name, Checked action) throws Exception {
        try { action.run(); } catch (IllegalArgumentException | java.io.IOException expected) { assertions++; return; }
        throw new AssertionError("Expected rejection: " + name);
    }
    private static PreferenceSnapshot changed(PreferenceSnapshot s, String key, boolean value) {
        var flags = new TreeMap<>(s.toggles()); flags.put(key, value); return new PreferenceSnapshot(flags, s.choices());
    }
    public static void main(String[] args) throws Exception {
        Path tmp = Files.createTempDirectory("vr-foundation-");
        var timber = new TimberSettings(tmp.resolve("timber"));
        var farmer = new CropReplantSettings(tmp.resolve("farmer"));
        var sort = new InventorySortSettings(tmp.resolve("sort"));
        var tool = new ToolSwapSettings(tmp.resolve("tool"));
        var grave = new GravestoneSettings(tmp.resolve("grave"));
        List<ManagedMod> modules = List.of(timber, farmer, sort, tool, grave);
        var schemas = new TreeMap<String, ModuleSchema>();
        for (var mod : modules) { schemas.put(mod.id(), ModuleSchema.from(mod)); ManagedMods.register(mod); }
        var defaults = PolicyCodec.defaults(schemas, true);
        var personal = ServerModules.personal(schemas);
        ok("five server modules", ServerModules.schemas().size() == 5);
        ok("no client-only modules sent", !personal.containsKey("ownmods_minimap"));
        ok("Timber personal", defaults.get(timber.id()).mode() == ModulePolicy.Mode.PERSONAL);
        ok("dedicated grave locked", defaults.get(grave.id()).mode() == ModulePolicy.Mode.SERVER_LOCKED);
        ok("integrated grave personal", PolicyCodec.defaults(schemas, false).get(grave.id()).mode() == ModulePolicy.Mode.PERSONAL);
        ok("five default axes", timber.flags().allowedAxes().size() == 5);
        ok("diamond default off", !timber.flags().allowedAxes().contains("minecraft:diamond_axe"));
        ok("netherite default off", !timber.flags().allowedAxes().contains("minecraft:netherite_axe"));
        ok("paid replant default", timber.flags().consumeSaplings() && timber.flags().replant());
        ok("Farmer all enabled", farmer.snapshot().values().stream().allMatch(Boolean::booleanValue));
        ok("family sort default", sort.choiceSnapshot().get("sort_mode").equals("family"));
        ok("best tool default", tool.policy().equals("best"));
        ok("auto tool off", !tool.flag("auto_tool"));
        ok("enchanted tools enabled", tool.flag("allow_enchanted"));
        var preview = new ShulkerPreviewSettings(tmp.resolve("preview"));
        ok("Shulker shift off", !preview.flag("shift_only"));
        var minimap = new MinimapSettings(tmp.resolve("minimap"));
        ok("minimap 1x default", minimap.choiceSnapshot().get("zoom").equals("one"));
        ok("minimap other players off", !minimap.flag("players"));
        ok("minimap dimension off", !minimap.flag("dimension"));
        ok("minimap auto zoom off", !minimap.flag("auto_zoom"));
        for (var mod : modules) {
            var schema = schemas.get(mod.id()); var policy = defaults.get(mod.id());
            var value = schema.defaults();
            ok("schema validates " + mod.id(), schema.validate(value).equals(value));
            ok("no mutation " + mod.id(), value.equals(schema.defaults()));
            if (policy.mode() == ModulePolicy.Mode.PERSONAL)
                ok("no unsolicited automation " + mod.id(), !policy.effective(schema, null).flag("enabled"));
        }
        var ts = schemas.get(timber.id()); var tp = defaults.get(timber.id());
        var alice = changed(ts.defaults(), "axe.minecraft:iron_axe", false);
        var bob = changed(ts.defaults(), "axe.minecraft:diamond_axe", true);
        ok("Alice iron disabled", !tp.effective(ts, alice).flag("axe.minecraft:iron_axe"));
        ok("Bob iron enabled independently", tp.effective(ts, bob).flag("axe.minecraft:iron_axe"));
        ok("Bob enables allowed diamond", tp.effective(ts, bob).flag("axe.minecraft:diamond_axe"));
        var rules = new TreeMap<>(tp.booleanRules()); rules.put("axe.minecraft:diamond_axe", ModulePolicy.BooleanRule.FORCE_FALSE);
        var capped = new ModulePolicy(tp.mode(), tp.serverValues(), rules, tp.allowedChoices());
        ok("server deny wins", !capped.effective(ts, bob).flag("axe.minecraft:diamond_axe"));
        ok("deny lock reported", capped.locked("axe.minecraft:diamond_axe"));
        ok("allowed iron stays editable", !capped.locked("axe.minecraft:iron_axe"));
        ok("sapling requirement cannot be bypassed", capped.effective(ts, changed(bob,"consume_saplings",false)).flag("consume_saplings"));
        ok("player can turn off Timber", !capped.effective(ts, bob.disabled()).flag("enabled"));
        var global = new ModulePolicy(ModulePolicy.Mode.SERVER_LOCKED, ts.defaults(), tp.booleanRules(), tp.allowedChoices());
        ok("server locked ignores personal disable", global.effective(ts,bob.disabled()).flag("enabled"));
        ok("server locked applies without mod", global.effective(ts,null).flag("enabled"));
        ok("server mode locks every option", global.locked("enabled") && global.locked("axe.minecraft:iron_axe"));
        var denied = new ModulePolicy(ModulePolicy.Mode.DISABLED, ts.defaults(), tp.booleanRules(), tp.allowedChoices());
        ok("DISABLED overrides personal true", !denied.effective(ts,bob).flag("enabled"));
        var ss = schemas.get(sort.id()); var sp = defaults.get(sort.id());
        var permitted = new TreeMap<>(sp.allowedChoices()); permitted.put("sort_mode", List.of("family"));
        var familyOnly = new ModulePolicy(sp.mode(),sp.serverValues(),sp.booleanRules(),permitted);
        var sortId = new PreferenceSnapshot(ss.defaults().toggles(),Map.of("sort_mode","id"));
        ok("choice whitelist enforced", familyOnly.effective(ss,sortId).choice("sort_mode","").equals("family"));
        ok("one allowed choice locked", familyOnly.locked("sort_mode"));
        String policy = PolicyCodec.encode(defaults);
        ok("policy roundtrip", PolicyCodec.decode(policy,schemas,true).equals(defaults));
        ok("policy deterministic", PolicyCodec.encode(PolicyCodec.decode(policy,schemas,true)).equals(policy));
        rejects("unknown policy key", () -> PolicyCodec.decode(policy+"owner=true\n",schemas,true));
        rejects("duplicate policy key", () -> PolicyCodec.decode(policy+"schemaVersion=1\n",schemas,true));
        rejects("unknown policy schema", () -> PolicyCodec.decode(policy.replace("schemaVersion=1","schemaVersion=99"),schemas,true));
        rejects("oversized policy", () -> PolicyCodec.decode("x".repeat(PolicyCodec.MAX_CHARS+1),schemas,true));
        rejects("invalid global mode", () -> PolicyCodec.decode(policy.replace("=PERSONAL","=ADMIN_FROM_CLIENT"),schemas,true));
        var badAllowed = new TreeMap<>(sp.allowedChoices()); badAllowed.put("sort_mode",List.of("evil"));
        rejects("unknown allowed choice", () -> new ModulePolicy(sp.mode(),sp.serverValues(),sp.booleanRules(),badAllowed).validate(ss));
        badAllowed.put("sort_mode",List.of());
        rejects("empty allowed choices", () -> new ModulePolicy(sp.mode(),sp.serverValues(),sp.booleanRules(),badAllowed).validate(ss));
        String wire = SettingsWire.encode(personal);
        ok("preferences roundtrip", SettingsWire.decode(wire,schemas).equals(personal));
        ok("wire deterministic", SettingsWire.encode(SettingsWire.decode(wire,schemas)).equals(wire));
        rejects("wrong wire version", () -> SettingsWire.decode(wire.replace("VR1","VR9"),schemas));
        rejects("malformed record", () -> SettingsWire.decode(wire+"B\tincomplete\n",schemas));
        rejects("extra boolean key", () -> SettingsWire.decode(wire+"B\townmods_timber\toperator\ttrue\n",schemas));
        rejects("duplicate key", () -> SettingsWire.decode(wire+"B\townmods_timber\tenabled\ttrue\n",schemas));
        rejects("boolean case coercion rejected", () -> SettingsWire.decode(wire.replace("\ttrue\n","\tTRUE\n"),schemas));
        rejects("unknown choice", () -> SettingsWire.decode(wire.replace("\tfamily\n","\tinvented\n"),schemas));
        rejects("unknown module", () -> SettingsWire.decode(wire.replace("ownmods_timber","ownmods_admin"),schemas));
        rejects("client UUID field", () -> SettingsWire.decode(wire+"U\tUUID\tplayer\t1234\n",schemas));
        rejects("client path field", () -> SettingsWire.decode(wire+"B\townmods_timber\t../server-policy\ttrue\n",schemas));
        rejects("too many rows", () -> SettingsWire.decode("VR1\n"+"\n".repeat(SettingsWire.MAX_ROWS+2),schemas));
        rejects("too many chars", () -> SettingsWire.decode("VR1\n"+"x".repeat(SettingsWire.MAX_CHARS),schemas));
        var gate = new RequestGate(100);
        ok("first request", gate.allow(0,1000));
        ok("replay rejected", !gate.allow(0,1200));
        ok("negative request rejected", !gate.allow(-1,1200));
        ok("too early rejected", !gate.allow(1,1099));
        ok("rate boundary accepted", gate.allow(1,1100));
        ok("older id rejected", !gate.allow(0,2000));
        var second = new RequestGate(100);
        ok("independent player request gate", second.allow(0,1000));
        rejects("negative rate interval", () -> new RequestGate(-1));
        Path file = tmp.resolve("server/policy.properties"); PolicyCodec.atomicWrite(file,policy);
        ok("durable policy contents", PolicyCodec.readBounded(file,PolicyCodec.MAX_CHARS).equals(policy));
        PolicyCodec.atomicWrite(file,policy+"# owner edit\n");
        ok("atomic replacement", Files.readString(file).endsWith("# owner edit\n"));
        rejects("bounded file read", () -> PolicyCodec.readBounded(file,4));
        Path impossible=tmp.resolve("directory");Files.createDirectories(impossible);Files.writeString(impossible.resolve("retain"),"keep");
        rejects("failed persistence", () -> PolicyCodec.atomicWrite(impossible,wire));
        ok("failed write preserves directory", Files.readString(impossible.resolve("retain")).equals("keep"));
        // Actual transaction: stale menu and permission veto must never write.
        var before=List.of("b","a");var after=List.of("a","b");
        var cells=new ArrayList<>(before);int[] writes={0};boolean[] deniedSlot={false};boolean[] failOnce={false};
        var access=new SlotTransaction.Access<String>() {
            public String get(int i){return cells.get(i);}public boolean equal(String a,String b){return a.equals(b);}
            public boolean maySet(int i,String v){return !(deniedSlot[0]&&i==1);}
            public void set(int i,String v){writes[0]++;if(failOnce[0]&&i==1){failOnce[0]=false;throw new IllegalStateException("write");}cells.set(i,v);}
        };
        deniedSlot[0]=true;
        ok("sort permission veto",SlotTransaction.commit(before,after,access)==SlotTransaction.Result.REJECTED);
        ok("no writes before full permission check",writes[0]==0);
        deniedSlot[0]=false;cells.set(0,"changed");
        ok("stale transaction rejected",SlotTransaction.commit(before,after,access)==SlotTransaction.Result.STALE);
        ok("stale transaction no writes",writes[0]==0);
        cells.set(0,"b");failOnce[0]=true;
        ok("failed transaction rolled back",SlotTransaction.commit(before,after,access)==SlotTransaction.Result.ROLLED_BACK);
        ok("rollback preserves inventory",cells.equals(before));
        ok("sort applied",SlotTransaction.commit(before,after,access)==SlotTransaction.Result.APPLIED);
        ok("sort final contents",cells.equals(after));
        randomizedPolicies(ts,tp);
        System.out.println("FOUNDATION RESULT: "+assertions+" assertions passed (includes 2000 randomized policy samples). No Minecraft runtime test.");
    }
    private static void randomizedPolicies(ModuleSchema schema,ModulePolicy base) {
        var random=new Random(123731);
        for(int n=0;n<2000;n++) {
            var a=new TreeMap<String,Boolean>();var b=new TreeMap<String,ModulePolicy.BooleanRule>();
            for(var key:schema.defaults().toggles().keySet()){
                a.put(key,random.nextBoolean());b.put(key,ModulePolicy.BooleanRule.values()[random.nextInt(3)]);
            }
            var personal=new PreferenceSnapshot(a,Map.of());
            var policy=new ModulePolicy(ModulePolicy.Mode.PERSONAL,schema.defaults(),b,Map.of());
            var result=policy.effective(schema,personal);
            for(var entry:b.entrySet()) {
                boolean expected=switch(entry.getValue()){case PERSONAL->a.get(entry.getKey());case FORCE_TRUE->true;case FORCE_FALSE->false;};
                if(result.flag(entry.getKey())!=expected)throw new AssertionError("policy invariant");
            }
            ok("random snapshot unmodified",personal.toggles().equals(a));
        }
    }
}
