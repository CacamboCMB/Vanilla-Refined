package de.ownmods.settings.policy;

import java.util.*;

/** Enforcement is independent of UI state. FORCE_TRUE also supports resource requirements. */
public record ModulePolicy(Mode mode, PreferenceSnapshot serverValues,
                           Map<String, BooleanRule> booleanRules, Map<String, List<String>> allowedChoices) {
    public enum Mode { PERSONAL, SERVER_LOCKED, DISABLED }
    public enum BooleanRule { PERSONAL, FORCE_TRUE, FORCE_FALSE }
    public ModulePolicy {
        Objects.requireNonNull(mode);
        Objects.requireNonNull(serverValues);
        booleanRules = Map.copyOf(booleanRules);
        var copy = new TreeMap<String, List<String>>();
        allowedChoices.forEach((key, values) -> copy.put(key, List.copyOf(values)));
        allowedChoices = Collections.unmodifiableMap(copy);
    }
    public static ModulePolicy defaults(ModuleSchema schema, boolean dedicated) {
        var rules = new TreeMap<String, BooleanRule>();
        schema.defaults().toggles().keySet().forEach(key -> rules.put(key, BooleanRule.PERSONAL));
        // On a dedicated server, paying for saplings is a server requirement by default.
        if (dedicated && schema.id().equals("ownmods_timber")) rules.put("consume_saplings", BooleanRule.FORCE_TRUE);
        var mode = dedicated && schema.id().equals("ownmods_gravestone") ? Mode.SERVER_LOCKED : Mode.PERSONAL;
        return new ModulePolicy(mode, schema.defaults(), rules, schema.choices());
    }
    public ModulePolicy validate(ModuleSchema schema) {
        schema.validate(serverValues);
        if (!booleanRules.keySet().equals(schema.defaults().toggles().keySet())
                || !allowedChoices.keySet().equals(schema.choices().keySet())) throw new IllegalArgumentException("Policy key mismatch");
        for (var entry : allowedChoices.entrySet()) {
            if (entry.getValue().isEmpty() || new HashSet<>(entry.getValue()).size() != entry.getValue().size()
                    || !schema.choices().get(entry.getKey()).containsAll(entry.getValue())
                    || !entry.getValue().contains(serverValues.choices().get(entry.getKey())))
                throw new IllegalArgumentException("Invalid allowed choice set: " + entry.getKey());
        }
        return this;
    }
    public PreferenceSnapshot effective(ModuleSchema schema, PreferenceSnapshot personal) {
        validate(schema);
        if (mode == Mode.SERVER_LOCKED) return serverValues;
        if (mode == Mode.DISABLED) return serverValues.disabled();
        var source = personal == null ? schema.defaults().disabled() : schema.validate(personal);
        var flags = new TreeMap<>(source.toggles());
        booleanRules.forEach((key, rule) -> {
            if (rule != BooleanRule.PERSONAL) flags.put(key, rule == BooleanRule.FORCE_TRUE);
        });
        // No unsolicited Timber/Farmer/Tool Swap for players who never opted in.
        if (personal == null) flags.put("enabled", false);
        var choices = new TreeMap<>(source.choices());
        allowedChoices.forEach((key, allowed) -> {
            if (!allowed.contains(choices.get(key))) choices.put(key, serverValues.choices().get(key));
        });
        return new PreferenceSnapshot(flags, choices);
    }
    public boolean locked(String key) {
        return mode != Mode.PERSONAL
                || (booleanRules.containsKey(key) && booleanRules.get(key) != BooleanRule.PERSONAL)
                || (allowedChoices.containsKey(key) && allowedChoices.get(key).size() == 1);
    }
}
