package de.ownmods.minimap;

import de.ownmods.settings.api.*;
import de.ownmods.settings.config.SimpleManagedMod;
import java.nio.file.Path;
import java.util.*;

/** User-facing Minimap settings. Choice ids are stable config tokens. */
public final class MinimapSettings extends SimpleManagedMod {
    private static ChoiceOption.Value value(String id,String label,String icon){
        return new ChoiceOption.Value(id,"ownmods.minimap."+label,"ownmods.minimap."+label+".tip",icon);
    }

    public static final ChoiceOption POSITION=new ChoiceOption(
        "position","ownmods.minimap.position","ownmods.minimap.position.tip",List.of(
            value("left","position.left","minecraft:map"),
            value("right","position.right","minecraft:filled_map"),
            value("bottom_left","position.bottom_left","minecraft:compass"),
            value("bottom_right","position.bottom_right","minecraft:recovery_compass")
        ),"left");

    public static final ChoiceOption SIZE=new ChoiceOption(
        "size","ownmods.minimap.size","ownmods.minimap.size.tip",List.of(
            value("small","size.small","minecraft:paper"),
            value("medium","size.medium","minecraft:map"),
            value("large","size.large","minecraft:filled_map")
        ),"medium");

    public static final ChoiceOption SHAPE=new ChoiceOption(
        "shape","ownmods.minimap.shape","ownmods.minimap.shape.tip",List.of(
            value("circle","shape.circle","minecraft:clock"),
            value("square","shape.square","minecraft:item_frame")
        ),"circle");

    public static final ChoiceOption ROTATION=new ChoiceOption(
        "rotation","ownmods.minimap.rotation","ownmods.minimap.rotation.tip",List.of(
            value("north_up","rotation.north_up","minecraft:compass"),
            value("player_up","rotation.player_up","minecraft:recovery_compass")
        ),"north_up");

    public static final ChoiceOption ZOOM=new ChoiceOption(
        "zoom","ownmods.minimap.zoom","ownmods.minimap.zoom.tip",List.of(
            value("one","zoom.one","minecraft:map"),
            value("two","zoom.two","minecraft:paper"),
            value("four","zoom.four","minecraft:spyglass"),
            value("eight","zoom.eight","minecraft:filled_map")
        ),"one");

    public MinimapSettings(Path path){
        super(path,"ownmods_minimap","ownmods.minimap","minecraft:map",true,List.of(
            new ToggleOption("enabled","ownmods.minimap.enabled","ownmods.minimap.enabled.tip",true),
            new ToggleOption("waypoints","ownmods.minimap.waypoints","ownmods.minimap.waypoints.tip",true),
            new ToggleOption("note_locations","ownmods.minimap.note_locations","ownmods.minimap.note_locations.tip",true),
            new ToggleOption("deathpoints","ownmods.minimap.deathpoints","ownmods.minimap.deathpoints.tip",true),
            new ToggleOption("players","ownmods.minimap.players","ownmods.minimap.players.tip",false),
            new ToggleOption("coordinates","ownmods.minimap.coordinates","ownmods.minimap.coordinates.tip",true),
            new ToggleOption("biome","ownmods.minimap.biome","ownmods.minimap.biome.tip",true),
            new ToggleOption("height","ownmods.minimap.height","ownmods.minimap.height.tip",true),
            new ToggleOption("dimension","ownmods.minimap.dimension","ownmods.minimap.dimension.tip",false),
            new ToggleOption("direction","ownmods.minimap.direction","ownmods.minimap.direction.tip",true),
            new ToggleOption("names","ownmods.minimap.names","ownmods.minimap.names.tip",true),
            new ToggleOption("distance","ownmods.minimap.distance","ownmods.minimap.distance.tip",true),
            new ToggleOption("auto_zoom","ownmods.minimap.auto_zoom","ownmods.minimap.auto_zoom.tip",false),
            new ToggleOption("portal_projection","ownmods.minimap.portal_projection","ownmods.minimap.portal_projection.tip",true)
        ),Map.ofEntries(
            Map.entry("enabled",new OptionIcon("minecraft:map","ownmods.minimap.group")),
            Map.entry("waypoints",new OptionIcon("minecraft:lodestone","ownmods.minimap.group")),
            Map.entry("note_locations",new OptionIcon("minecraft:writable_book","ownmods.minimap.group")),
            Map.entry("deathpoints",new OptionIcon("minecraft:skeleton_skull","ownmods.minimap.group")),
            Map.entry("players",new OptionIcon("minecraft:player_head","ownmods.minimap.group")),
            Map.entry("coordinates",new OptionIcon("minecraft:compass","ownmods.minimap.group")),
            Map.entry("biome",new OptionIcon("minecraft:grass_block","ownmods.minimap.group")),
            Map.entry("height",new OptionIcon("minecraft:stone","ownmods.minimap.group")),
            Map.entry("dimension",new OptionIcon("minecraft:ender_eye","ownmods.minimap.group")),
            Map.entry("direction",new OptionIcon("minecraft:recovery_compass","ownmods.minimap.group")),
            Map.entry("names",new OptionIcon("minecraft:name_tag","ownmods.minimap.group")),
            Map.entry("distance",new OptionIcon("minecraft:spyglass","ownmods.minimap.group")),
            Map.entry("auto_zoom",new OptionIcon("minecraft:elytra","ownmods.minimap.group")),
            Map.entry("portal_projection",new OptionIcon("minecraft:obsidian","ownmods.minimap.group"))
        ),List.of(POSITION,SIZE,SHAPE,ROTATION,ZOOM));
    }

    @Override public String customScreenClass(){ return "de.ownmods.minimap.client.MinimapSettingsScreen"; }

    public String choice(String key){return choiceSnapshot().get(key);}
    public int blocksPerPixel(){
        return switch(choice("zoom")){
            case "two" -> 2;
            case "four" -> 4;
            case "eight" -> 8;
            default -> 1;
        };
    }
}
