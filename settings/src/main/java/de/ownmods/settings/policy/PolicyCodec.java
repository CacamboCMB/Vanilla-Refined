package de.ownmods.settings.policy;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Human-editable properties on disk; the same strictly validated format is sent server -> client. */
public final class PolicyCodec {
    public static final int MAX_CHARS = 32768;
    private PolicyCodec() { }
    public static Map<String, ModulePolicy> defaults(Map<String, ModuleSchema> schemas, boolean dedicated) {
        var result = new TreeMap<String, ModulePolicy>();
        schemas.forEach((id, schema) -> result.put(id, ModulePolicy.defaults(schema, dedicated)));
        return Map.copyOf(result);
    }
    public static String encode(Map<String, ModulePolicy> policies) {
        var props = new Properties(); props.setProperty("schemaVersion", "1");
        new TreeMap<>(policies).forEach((id, p) -> {
            props.setProperty(id + ".mode", p.mode().name());
            p.serverValues().toggles().forEach((key, value) -> props.setProperty(id + ".server.bool." + key, value.toString()));
            p.serverValues().choices().forEach((key, value) -> props.setProperty(id + ".server.choice." + key, value));
            p.booleanRules().forEach((key, value) -> props.setProperty(id + ".rule." + key, value.name()));
            p.allowedChoices().forEach((key, value) -> props.setProperty(id + ".allowed." + key, String.join(",", value)));
        });
        // Properties.store escapes ':' in keys correctly; remove its timestamp for stable fingerprints.
        var writer = new StringWriter();
        try { props.store(writer, null); } catch (IOException impossible) { throw new UncheckedIOException(impossible); }
        String result = writer.toString().lines().filter(line -> !line.startsWith("#")).sorted().reduce("", (a, b) -> a + b + "\n");
        if (result.length() > MAX_CHARS) throw new IllegalArgumentException("Policy too large");
        return result;
    }
    public static Map<String, ModulePolicy> decode(String text, Map<String, ModuleSchema> schemas, boolean dedicated) {
        if (text == null || text.length() > MAX_CHARS) throw new IllegalArgumentException("Policy too large");
        var props = new Properties() {
            @Override public synchronized Object put(Object key, Object value) {
                if (containsKey(key)) throw new IllegalArgumentException("Duplicate policy key: " + key);
                return super.put(key, value);
            }
        };
        try { props.load(new StringReader(text)); } catch (IOException e) { throw new UncheckedIOException(e); }
        if (!"1".equals(props.remove("schemaVersion"))) throw new IllegalArgumentException("Unsupported policy schema");
        var result = new TreeMap<String, ModulePolicy>();
        for (var entry : new TreeMap<>(schemas).entrySet()) {
            String id = entry.getKey(); var schema = entry.getValue(); var fallback = ModulePolicy.defaults(schema, dedicated);
            var flags = new TreeMap<>(fallback.serverValues().toggles());
            var choices = new TreeMap<>(fallback.serverValues().choices());
            var rules = new TreeMap<>(fallback.booleanRules());
            var allowed = new TreeMap<>(fallback.allowedChoices());
            var mode = ModulePolicy.Mode.valueOf(take(props, id + ".mode", fallback.mode().name()));
            for (var key : flags.keySet()) {
                String flag = take(props, id + ".server.bool." + key, flags.get(key).toString());
                if (!flag.equals("true") && !flag.equals("false")) throw new IllegalArgumentException("Invalid policy boolean");
                flags.put(key, Boolean.valueOf(flag));
                rules.put(key, ModulePolicy.BooleanRule.valueOf(take(props, id + ".rule." + key, rules.get(key).name())));
            }
            for (var key : choices.keySet()) {
                choices.put(key, take(props, id + ".server.choice." + key, choices.get(key)));
                allowed.put(key, List.of(take(props, id + ".allowed." + key, String.join(",", allowed.get(key))).split(",", -1)));
            }
            result.put(id, new ModulePolicy(mode, new PreferenceSnapshot(flags, choices), rules, allowed).validate(schema));
        }
        if (!props.isEmpty()) throw new IllegalArgumentException("Unknown policy keys: " + props.keySet());
        return Map.copyOf(result);
    }
    private static String take(Properties props, String key, String fallback) {
        Object value = props.remove(key); return value == null ? fallback : value.toString().trim();
    }
    public static void atomicWrite(Path path, String text) throws IOException {
        Files.createDirectories(path.toAbsolutePath().getParent());
        var temporary = Files.createTempFile(path.toAbsolutePath().getParent(), "vr-", ".tmp");
        try {
            Files.writeString(temporary, text, StandardCharsets.UTF_8);
            try { Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException e) { Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temporary); }
    }
    public static String readBounded(Path path, int maximum) throws IOException {
        if (Files.size(path) > maximum * 4L) throw new IOException("File exceeds size limit: " + path.getFileName());
        String text = Files.readString(path, StandardCharsets.UTF_8);
        if (text.length() > maximum) throw new IOException("File exceeds character limit");
        return text;
    }
}
