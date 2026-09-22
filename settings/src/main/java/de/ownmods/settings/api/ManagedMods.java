package de.ownmods.settings.api;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Explicit opt-in registry: unrelated Fabric mods and datapacks are not listed. */
public final class ManagedMods {
    private static final Map<String, ManagedMod> MODS = new LinkedHashMap<>();
    private ManagedMods() { }

    public static synchronized void register(ManagedMod mod) {
        Objects.requireNonNull(mod);
        if (!mod.id().matches("[a-z][a-z0-9_-]{1,63}")) throw new IllegalArgumentException("Invalid mod id");
        var keys = new HashSet<String>();
        for (var option : mod.options()) {
            if (!keys.add(option.key())) throw new IllegalArgumentException("Duplicate option: " + option.key());
        }
        if (!keys.contains("enabled")) throw new IllegalArgumentException("Missing enabled setting");
        if (!keys.equals(mod.snapshot().keySet())) throw new IllegalArgumentException("Schema/snapshot mismatch");
        var choiceKeys = new HashSet<String>();
        for (var option : mod.choiceOptions()) {
            if (keys.contains(option.key()) || !choiceKeys.add(option.key()))
                throw new IllegalArgumentException("Duplicate choice option: " + option.key());
            if (!option.accepts(mod.choiceSnapshot().get(option.key())))
                throw new IllegalArgumentException("Invalid choice snapshot: " + option.key());
        }
        if (!choiceKeys.equals(mod.choiceSnapshot().keySet())) throw new IllegalArgumentException("Choice snapshot mismatch");
        if (MODS.putIfAbsent(mod.id(), mod) != null) throw new IllegalStateException("Duplicate own mod: " + mod.id());
    }
    public static synchronized List<ManagedMod> all() {
        return List.copyOf(new ArrayList<>(MODS.values()));
    }
}
