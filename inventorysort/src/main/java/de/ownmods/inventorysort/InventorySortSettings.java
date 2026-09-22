package de.ownmods.inventorysort;
import de.ownmods.settings.api.*;
import de.ownmods.settings.config.SimpleManagedMod;
import java.nio.file.Path;
import de.ownmods.inventorysort.core.SortMode;
import java.util.*;
public final class InventorySortSettings extends SimpleManagedMod {
    public static final ChoiceOption SORT_MODE = new ChoiceOption("sort_mode", "ownmods.inventorysort.mode",
        "ownmods.inventorysort.mode.tip", List.of(
            mode("id", "minecraft:comparator"), mode("family", "minecraft:oak_planks"),
            mode("type", "minecraft:stone_bricks"), mode("color", "minecraft:red_dye")), "family");
    private static ChoiceOption.Value mode(String id, String icon) {
        return new ChoiceOption.Value(id, "ownmods.inventorysort.mode." + id,
                "ownmods.inventorysort.mode." + id + ".tip", icon);
    }
    public record Rules(boolean enabled, boolean merge, boolean containers, SortMode mode) { }
    /** Capture one saved configuration for the entire transaction. */
    public Rules rules() {
        var state = settingsState(); var toggles = state.toggles();
        return new Rules(toggles.getOrDefault("enabled", false), toggles.getOrDefault("merge", true),
            toggles.getOrDefault("containers", true), SortMode.parse(state.choices().get("sort_mode")));
    }
    public SortMode sortMode() { return rules().mode(); }
    public InventorySortSettings(Path path) {
        super(path,"ownmods_inventorysort","ownmods.inventorysort","minecraft:chest",false,List.of(
            new ToggleOption("enabled","ownmods.inventorysort.enabled","ownmods.inventorysort.enabled.tip",true),
            new ToggleOption("merge","ownmods.inventorysort.merge","ownmods.inventorysort.merge.tip",true),
            new ToggleOption("containers","ownmods.inventorysort.containers","ownmods.inventorysort.containers.tip",true),
            new ToggleOption("buttons","ownmods.inventorysort.buttons","ownmods.inventorysort.buttons.tip",true)), Map.ofEntries(
            Map.entry("enabled",new OptionIcon("minecraft:chest","ownmods.inventorysort.group")),
            Map.entry("merge",new OptionIcon("minecraft:bundle","ownmods.inventorysort.group")),
            Map.entry("containers",new OptionIcon("minecraft:barrel","ownmods.inventorysort.group")),
            Map.entry("buttons",new OptionIcon("minecraft:comparator","ownmods.inventorysort.group"))), List.of(SORT_MODE));
    }
}
