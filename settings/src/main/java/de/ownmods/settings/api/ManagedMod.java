package de.ownmods.settings.api;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/** Implement and register this once from an own mod's main entrypoint. No client imports. */
public interface ManagedMod {
    String id();
    /** Optional client Screen with a public (Screen parent) constructor. */
    default String customScreenClass() { return ""; }
    String titleKey();
    String descriptionKey();
    List<ToggleOption> options();
    Map<String, Boolean> snapshot();
    /** Optional finite choice settings; old modules remain binary-compatible. */
    default List<ChoiceOption> choiceOptions() { return List.of(); }
    default Map<String, String> choiceSnapshot() { return Map.of(); }
    default void save(Map<String, Boolean> values, Map<String, String> choices) throws IOException {
        if (!choices.isEmpty()) throw new IllegalArgumentException("This module has no choice settings");
        save(values);
    }
    /** Optional item icons, keyed by setting key. No client classes in this API. */
    /** Backwards compatible: legacy Timber supplies the icon of its enabled setting. */
    default String iconItemId() {
        var icon = optionIcons().get("enabled");
        return icon == null ? "minecraft:comparator" : icon.itemId();
    }
    /** Pure client options remain editable while connected to a remote server. */
    default boolean clientOnly() { return false; }
    default Map<String, OptionIcon> optionIcons() { return Map.of(); }
    /** Persist first, then atomically publish the new immutable runtime snapshot. */
    void save(Map<String, Boolean> values) throws IOException;
    default String warning() { return ""; }
    default boolean enabled() { return snapshot().getOrDefault("enabled", false); }
}
