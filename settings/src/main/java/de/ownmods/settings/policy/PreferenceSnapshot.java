package de.ownmods.settings.policy;

import java.util.LinkedHashMap;
import java.util.Map;

/** Immutable personal/effective settings. This type has no Minecraft or Fabric dependency. */
public record PreferenceSnapshot(Map<String, Boolean> toggles, Map<String, String> choices) {
    public PreferenceSnapshot {
        toggles = Map.copyOf(toggles);
        choices = Map.copyOf(choices);
    }
    public boolean flag(String key) { return toggles.getOrDefault(key, false); }
    public String choice(String key, String fallback) { return choices.getOrDefault(key, fallback); }
    public PreferenceSnapshot disabled() {
        var values = new LinkedHashMap<>(toggles);
        values.put("enabled", false);
        return new PreferenceSnapshot(values, choices);
    }
}
