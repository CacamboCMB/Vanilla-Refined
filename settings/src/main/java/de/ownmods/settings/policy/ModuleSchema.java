package de.ownmods.settings.policy;

import de.ownmods.settings.api.ManagedMod;
import java.util.*;

/** The server defines the permitted keys and tokens; a client cannot extend this schema. */
public record ModuleSchema(String id, PreferenceSnapshot defaults, Map<String, List<String>> choices) {
    public ModuleSchema {
        if (!id.matches("[a-z][a-z0-9_]{1,63}")) throw new IllegalArgumentException("Invalid module ID");
        var copy = new TreeMap<String, List<String>>();
        choices.forEach((key, values) -> copy.put(key, List.copyOf(values)));
        choices = Collections.unmodifiableMap(copy);
        if (!defaults.toggles().containsKey("enabled") || !choices.keySet().equals(defaults.choices().keySet()))
            throw new IllegalArgumentException("Invalid schema");
        for (var key : defaults.toggles().keySet())
            if (!key.matches("[a-z0-9_.:-]{1,96}") || choices.containsKey(key)) throw new IllegalArgumentException("Invalid key");
        for (var entry : choices.entrySet()) {
            if (!entry.getKey().matches("[a-z0-9_.:-]{1,96}") || entry.getValue().isEmpty()
                    || entry.getValue().size() != new HashSet<>(entry.getValue()).size()
                    || !entry.getValue().contains(defaults.choices().get(entry.getKey())))
                throw new IllegalArgumentException("Invalid choice schema");
            for (var token : entry.getValue())
                if (!token.matches("[a-z][a-z0-9_]{0,63}")) throw new IllegalArgumentException("Invalid token");
        }
    }
    public static ModuleSchema from(ManagedMod mod) {
        var bools = new TreeMap<String, Boolean>();
        mod.options().forEach(option -> bools.put(option.key(), option.defaultValue()));
        var defaults = new TreeMap<String, String>();
        var allowed = new TreeMap<String, List<String>>();
        mod.choiceOptions().forEach(option -> {
            defaults.put(option.key(), option.defaultValue());
            allowed.put(option.key(), option.values().stream().map(v -> v.id()).toList());
        });
        return new ModuleSchema(mod.id(), new PreferenceSnapshot(bools, defaults), allowed);
    }
    public PreferenceSnapshot validate(PreferenceSnapshot value) {
        if (!value.toggles().keySet().equals(defaults.toggles().keySet()) || !value.choices().keySet().equals(choices.keySet()))
            throw new IllegalArgumentException("Settings schema mismatch for " + id);
        for (var entry : value.choices().entrySet())
            if (!choices.get(entry.getKey()).contains(entry.getValue())) throw new IllegalArgumentException("Invalid choice for " + id);
        return value;
    }
}
