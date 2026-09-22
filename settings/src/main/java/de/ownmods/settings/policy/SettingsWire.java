package de.ownmods.settings.policy;

import java.util.*;

/** Bounded, deterministic protocol. No UUID, item data, commands, or disk paths come from a client. */
public final class SettingsWire {
    public static final int PROTOCOL = 1;
    public static final int MAX_CHARS = 16384;
    public static final int MAX_ROWS = 256;
    private SettingsWire() { }
    public static String encode(Map<String, PreferenceSnapshot> modules) {
        var out = new StringBuilder("VR1\n");
        new TreeMap<>(modules).forEach((mod, settings) -> {
            new TreeMap<>(settings.toggles()).forEach((key, value) -> out.append("B\t").append(mod).append('\t').append(key).append('\t').append(value).append('\n'));
            new TreeMap<>(settings.choices()).forEach((key, value) -> out.append("C\t").append(mod).append('\t').append(key).append('\t').append(value).append('\n'));
        });
        if (out.length() > MAX_CHARS) throw new IllegalArgumentException("Settings too large");
        return out.toString();
    }
    public static Map<String, PreferenceSnapshot> decode(String text, Map<String, ModuleSchema> schemas) {
        if (text == null || text.length() > MAX_CHARS || !text.startsWith("VR1\n")) throw new IllegalArgumentException("Invalid protocol document");
        String[] lines = text.split("\n", -1);
        if (lines.length > MAX_ROWS + 2) throw new IllegalArgumentException("Too many rows");
        var flags = new TreeMap<String, Map<String, Boolean>>();
        var choices = new TreeMap<String, Map<String, String>>();
        var seen = new HashSet<String>();
        for (int i = 1; i < lines.length; i++) {
            if (i == lines.length - 1 && lines[i].isEmpty()) continue;
            String[] row = lines[i].split("\t", -1);
            if (row.length != 4 || !schemas.containsKey(row[1]) || !row[2].matches("[a-z0-9_.:-]{1,96}")
                    || !seen.add(row[1] + "\t" + row[2])) throw new IllegalArgumentException("Unknown or duplicate setting");
            if (row[0].equals("B")) {
                if (!row[3].equals("true") && !row[3].equals("false")) throw new IllegalArgumentException("Invalid boolean");
                flags.computeIfAbsent(row[1], key -> new TreeMap<>()).put(row[2], Boolean.valueOf(row[3]));
            } else if (row[0].equals("C") && row[3].matches("[a-z][a-z0-9_]{0,63}")) {
                choices.computeIfAbsent(row[1], key -> new TreeMap<>()).put(row[2], row[3]);
            } else throw new IllegalArgumentException("Invalid setting type/value");
        }
        var modules = new HashSet<>(flags.keySet()); modules.addAll(choices.keySet());
        var result = new TreeMap<String, PreferenceSnapshot>();
        for (var id : modules) result.put(id, schemas.get(id).validate(new PreferenceSnapshot(
                flags.getOrDefault(id, Map.of()), choices.getOrDefault(id, Map.of()))));
        return Map.copyOf(result);
    }
}
