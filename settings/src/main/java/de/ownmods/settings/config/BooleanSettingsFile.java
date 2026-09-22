package de.ownmods.settings.config;

import de.ownmods.settings.api.ToggleOption;
import de.ownmods.settings.api.ChoiceOption;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/** Small dependency-free, thread-safe, fail-closed config store. */
public final class BooleanSettingsFile {
    private final Path path;
    private final Map<String, Boolean> defaults;
    private final Properties preserved = new Properties();
    private final Map<String, ChoiceOption> choiceSchema;
    private final Map<String, String> choiceDefaults;
    /** One publication for toggles and choices; readers never observe half a save. */
    public record State(Map<String, Boolean> toggles, Map<String, String> choices) {
        public State { toggles = Map.copyOf(toggles); choices = Map.copyOf(choices); }
    }
    private volatile State current;
    private volatile String warning = "";

    public BooleanSettingsFile(Path path, List<ToggleOption> schema) {
        this(path, schema, List.of());
    }
    public BooleanSettingsFile(Path path, List<ToggleOption> schema, List<ChoiceOption> choices) {
        this.path = path.toAbsolutePath();
        var map = new LinkedHashMap<String, Boolean>();
        for (var option : schema) {
            if (map.putIfAbsent(option.key(), option.defaultValue()) != null) {
                throw new IllegalArgumentException("Duplicate key: " + option.key());
            }
        }
        if (!map.containsKey("enabled")) throw new IllegalArgumentException("Missing enabled key");
        defaults = Map.copyOf(map);
        var choiceMap = new LinkedHashMap<String, ChoiceOption>();
        var choiceValues = new LinkedHashMap<String, String>();
        for (var option : choices) {
            if (defaults.containsKey(option.key()) || option.key().equals("schemaVersion") ||
                    choiceMap.putIfAbsent(option.key(), option) != null)
                throw new IllegalArgumentException("Duplicate/reserved choice key: " + option.key());
            choiceValues.put(option.key(), option.defaultValue());
        }
        choiceSchema = Map.copyOf(choiceMap);
        choiceDefaults = Map.copyOf(choiceValues);
        current = new State(defaults, choiceDefaults);
        load();
    }
    private void load() {
        if (!Files.exists(path)) return;
        try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            preserved.load(reader);
            if (!preserved.getProperty("schemaVersion", "1").equals("1")) {
                throw new IOException("Unsupported configuration schema");
            }
            var loaded = new LinkedHashMap<>(defaults);
            for (var key : defaults.keySet()) {
                var value = preserved.getProperty(key);
                if (value == null) continue;
                if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
                    throw new IOException("Invalid boolean for " + key);
                }
                loaded.put(key, Boolean.parseBoolean(value));
            }
            var loadedChoices = new LinkedHashMap<>(choiceDefaults);
            for (var entry : choiceSchema.entrySet()) {
                var value = preserved.getProperty(entry.getKey(), entry.getValue().defaultValue());
                if (!entry.getValue().accepts(value)) throw new IOException("Invalid choice for " + entry.getKey());
                loadedChoices.put(entry.getKey(), value);
            }
            current = new State(loaded, loadedChoices);
        } catch (IOException | IllegalArgumentException e) {
            var disabled = new LinkedHashMap<>(defaults);
            disabled.put("enabled", false);
            current = new State(disabled, choiceDefaults);
            warning = "Configuration could not be loaded; feature disabled: " + e.getMessage();
            // Leave the original file untouched. Only an explicit Save can replace it.
        }
    }
    public Map<String, Boolean> snapshot() { return current.toggles(); }
    public Map<String, String> choiceSnapshot() { return current.choices(); }
    public State state() { return current; }
    public String warning() { return warning; }
    public synchronized void save(Map<String, Boolean> values) throws IOException {
        save(values, current.choices());
    }
    public synchronized void save(Map<String, Boolean> values, Map<String, String> choices) throws IOException {
        if (!values.keySet().equals(defaults.keySet()) || values.values().stream().anyMatch(java.util.Objects::isNull)) {
            throw new IllegalArgumentException("Settings do not match the schema");
        }
        if (!choices.keySet().equals(choiceSchema.keySet())) throw new IllegalArgumentException("Choice schema mismatch");
        for (var entry : choices.entrySet()) if (!choiceSchema.get(entry.getKey()).accepts(entry.getValue()))
            throw new IllegalArgumentException("Invalid choice: " + entry.getKey());
        var next = new State(values, choices);
        var properties = new Properties();
        properties.putAll(preserved); // preserve unrecognised keys for forwards compatibility
        properties.setProperty("schemaVersion", "1");
        next.toggles().forEach((key, value) -> properties.setProperty(key, value.toString()));
        next.choices().forEach(properties::setProperty);
        Files.createDirectories(path.getParent());
        Path temp = Files.createTempFile(path.getParent(), path.getFileName().toString(), ".tmp");
        try {
            try (var writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
                properties.store(writer, "OwnMods - changes apply after saving; local singleplayer configuration");
            }
            try {
                Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
            }
            preserved.clear();
            preserved.putAll(properties);
            current = next;
            warning = "";
        } finally {
            Files.deleteIfExists(temp);
        }
    }
}
