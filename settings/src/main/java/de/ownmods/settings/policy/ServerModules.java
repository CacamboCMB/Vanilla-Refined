package de.ownmods.settings.policy;

import de.ownmods.settings.api.ManagedMods;
import java.util.*;

public final class ServerModules {
    public static final Set<String> IDS = Set.of("ownmods_timber", "ownmods_cropreplant", "ownmods_inventorysort", "ownmods_toolswap", "ownmods_gravestone");
    private ServerModules() { }
    public static Map<String, ModuleSchema> schemas() {
        var result = new TreeMap<String, ModuleSchema>();
        for (var mod : ManagedMods.all()) if (IDS.contains(mod.id())) result.put(mod.id(), ModuleSchema.from(mod));
        return Map.copyOf(result);
    }
    public static Map<String, PreferenceSnapshot> personal(Map<String, ModuleSchema> schemas) {
        var result = new TreeMap<String, PreferenceSnapshot>();
        for (var mod : ManagedMods.all()) if (schemas.containsKey(mod.id())) result.put(mod.id(),
                schemas.get(mod.id()).validate(new PreferenceSnapshot(mod.snapshot(), mod.choiceSnapshot())));
        return Map.copyOf(result);
    }
}
