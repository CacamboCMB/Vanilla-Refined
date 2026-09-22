package de.ownmods.timber;

import de.ownmods.settings.api.ManagedMod;
import de.ownmods.settings.api.OptionIcon;
import java.util.LinkedHashMap;
import de.ownmods.settings.api.ToggleOption;
import de.ownmods.settings.config.BooleanSettingsFile;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class TimberSettings implements ManagedMod {
    public static final List<String> AXES = List.of("wooden_axe", "stone_axe", "copper_axe", "iron_axe", "golden_axe", "diamond_axe", "netherite_axe");
    public static final List<ToggleOption> SCHEMA = createSchema();
    private final BooleanSettingsFile file;
    public TimberSettings(Path path) { file = new BooleanSettingsFile(path, SCHEMA); }
    private static List<ToggleOption> createSchema() {
        var options = new ArrayList<ToggleOption>();
        options.add(new ToggleOption("enabled", "ownmods.timber.enabled", "ownmods.timber.enabled.tip", true));
        options.add(new ToggleOption("leaves", "ownmods.timber.leaves", "ownmods.timber.leaves.tip", true));
        options.add(new ToggleOption("replant", "ownmods.timber.replant", "ownmods.timber.replant.tip", true));
        options.add(new ToggleOption("consume_saplings", "ownmods.timber.consume", "ownmods.timber.consume.tip", true));
        options.add(new ToggleOption("sneak_bypass", "ownmods.timber.sneak", "ownmods.timber.sneak.tip", true));
        for (var axe : AXES) {
            boolean enabledByDefault = !axe.equals("diamond_axe") && !axe.equals("netherite_axe");
            options.add(new ToggleOption("axe.minecraft:" + axe, "item.minecraft." + axe, "ownmods.timber.axe.tip", enabledByDefault));
        }
        return List.copyOf(options);
    }
    @Override public String id() { return "ownmods_timber"; }
    @Override public String titleKey() { return "ownmods.timber.title"; }
    @Override public String descriptionKey() { return "ownmods.timber.description"; }
    @Override public String iconItemId() { return "minecraft:oak_sapling"; }
    @Override public List<ToggleOption> options() { return SCHEMA; }
    @Override public Map<String, OptionIcon> optionIcons() { return ICONS; }
    private static final Map<String, OptionIcon> ICONS = createIcons();
    private static Map<String, OptionIcon> createIcons() {
        var icons = new LinkedHashMap<String, OptionIcon>();
        String general = "ownmods.timber.group.features";
        icons.put("enabled", new OptionIcon("minecraft:oak_log", general));
        icons.put("leaves", new OptionIcon("minecraft:oak_leaves", general));
        icons.put("replant", new OptionIcon("minecraft:oak_sapling", general));
        icons.put("consume_saplings", new OptionIcon("minecraft:chest", general));
        icons.put("sneak_bypass", new OptionIcon("minecraft:leather_boots", general));
        for (var axe : AXES) icons.put("axe.minecraft:" + axe,
                new OptionIcon("minecraft:" + axe, "ownmods.timber.group.axes"));
        return Map.copyOf(icons);
    }
    @Override public Map<String, Boolean> snapshot() { return file.snapshot(); }
    @Override public void save(Map<String, Boolean> values) throws IOException { file.save(values); }
    @Override public String warning() { return file.warning(); }
    public Flags flags() {
        return flags(snapshot());
    }
    public static Flags flags(Map<String, Boolean> values) {
        var axes = values.entrySet().stream().filter(e -> e.getKey().startsWith("axe.") && e.getValue())
                .map(e -> e.getKey().substring(4)).collect(Collectors.toUnmodifiableSet());
        return new Flags(values.get("enabled"), values.get("leaves"), values.get("replant"), values.get("consume_saplings"), values.get("sneak_bypass"), axes);
    }
    public record Flags(boolean enabled, boolean leaves, boolean replant, boolean consumeSaplings, boolean sneakBypass, Set<String> allowedAxes) {
        public Flags { allowedAxes = Set.copyOf(allowedAxes); }
    }
}
