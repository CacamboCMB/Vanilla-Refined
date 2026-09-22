package de.ownmods.zoom;
import de.ownmods.settings.api.*;
import de.ownmods.settings.config.SimpleManagedMod;
import java.nio.file.Path;
import java.util.*;
public final class ZoomSettings extends SimpleManagedMod {
    public ZoomSettings(Path path) {
        super(path,"ownmods_zoom","ownmods.zoom","minecraft:spyglass",true,List.of(
            new ToggleOption("enabled","ownmods.zoom.enabled","ownmods.zoom.enabled.tip",true),
            new ToggleOption("toggle","ownmods.zoom.toggle","ownmods.zoom.toggle.tip",false),
            new ToggleOption("strong","ownmods.zoom.strong","ownmods.zoom.strong.tip",false),
            new ToggleOption("smooth","ownmods.zoom.smooth","ownmods.zoom.smooth.tip",true)), Map.ofEntries(
            Map.entry("enabled",new OptionIcon("minecraft:spyglass","ownmods.zoom.group")),
            Map.entry("toggle",new OptionIcon("minecraft:lever","ownmods.zoom.group")),
            Map.entry("strong",new OptionIcon("minecraft:amethyst_shard","ownmods.zoom.group")),
            Map.entry("smooth",new OptionIcon("minecraft:feather","ownmods.zoom.group"))));
    }
}
