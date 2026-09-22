package de.ownmods.settings.api;

import java.util.*;

/** A finite, ordered setting. Values are stable config tokens, never translated labels. */
public record ChoiceOption(String key, String labelKey, String tooltipKey,
                           List<Value> values, String defaultValue) {
    public record Value(String id, String labelKey, String tooltipKey, String itemId) {
        public Value {
            Objects.requireNonNull(id); Objects.requireNonNull(labelKey);
            Objects.requireNonNull(tooltipKey); Objects.requireNonNull(itemId);
            if (!id.matches("[a-z][a-z0-9_]*")) throw new IllegalArgumentException("Invalid choice token");
        }
    }
    public ChoiceOption {
        Objects.requireNonNull(key); Objects.requireNonNull(labelKey); Objects.requireNonNull(tooltipKey);
        Objects.requireNonNull(defaultValue);
        values = List.copyOf(values);
        if (key.isBlank() || values.size() < 2) throw new IllegalArgumentException("Invalid choice schema");
        var ids = new HashSet<String>();
        for (var value : values) if (!ids.add(value.id())) throw new IllegalArgumentException("Duplicate choice value");
        if (!ids.contains(defaultValue)) throw new IllegalArgumentException("Missing default choice");
    }
    public boolean accepts(String id) { return values.stream().anyMatch(v -> v.id().equals(id)); }
    public Value value(String id) {
        return values.stream().filter(v -> v.id().equals(id)).findFirst()
                .orElseGet(() -> values.stream().filter(v -> v.id().equals(defaultValue)).findFirst().orElseThrow());
    }
    public String next(String current) {
        int index = values.indexOf(value(current));
        return values.get((index + 1) % values.size()).id();
    }
}
