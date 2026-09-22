package de.ownmods.notes.core;

import java.util.*;

/** Vanilla-biome dimension classification and varied representative item icons for the notebook. */
public final class BiomeVisuals {
    private BiomeVisuals() {}
    private static final Set<String> NETHER=Set.of("nether_wastes","soul_sand_valley","crimson_forest","warped_forest","basalt_deltas");
    private static final Set<String> END=Set.of("the_end","end_highlands","end_midlands","small_end_islands","end_barrens");

    public static String dimensionFor(String id) {
        String p=path(id);
        if(NETHER.contains(p))return NotesCatalog.NETHER;
        if(END.contains(p))return NotesCatalog.END;
        return NotesCatalog.OVERWORLD;
    }

    public static String iconFor(String id) {
        String p=path(id);
        if(p.equals("nether_wastes"))return "minecraft:netherrack";
        if(p.equals("soul_sand_valley"))return "minecraft:soul_sand";
        if(p.equals("crimson_forest"))return "minecraft:crimson_fungus";
        if(p.equals("warped_forest"))return "minecraft:warped_fungus";
        if(p.equals("basalt_deltas"))return "minecraft:basalt";
        if(p.equals("the_end"))return "minecraft:end_stone";
        if(p.equals("end_highlands"))return "minecraft:chorus_flower";
        if(p.equals("end_midlands"))return "minecraft:purpur_block";
        if(p.equals("small_end_islands"))return "minecraft:end_stone_bricks";
        if(p.equals("end_barrens"))return "minecraft:chorus_fruit";
        if(p.contains("deep_dark"))return "minecraft:sculk";
        if(p.contains("lush_caves"))return "minecraft:spore_blossom";
        if(p.contains("dripstone"))return "minecraft:pointed_dripstone";
        if(p.contains("cherry"))return "minecraft:cherry_sapling";
        if(p.contains("pale_garden"))return "minecraft:pale_oak_sapling";
        if(p.contains("dark_forest"))return "minecraft:dark_oak_sapling";
        if(p.contains("birch"))return "minecraft:birch_sapling";
        if(p.contains("jungle")||p.contains("bamboo"))return "minecraft:jungle_sapling";
        if(p.contains("taiga")||p.contains("old_growth_spruce")||p.contains("old_growth_pine"))return "minecraft:spruce_sapling";
        if(p.contains("mangrove"))return "minecraft:mangrove_propagule";
        if(p.contains("swamp"))return "minecraft:lily_pad";
        if(p.contains("savanna"))return "minecraft:acacia_sapling";
        if(p.contains("desert"))return "minecraft:sand";
        if(p.contains("badlands"))return "minecraft:red_sand";
        if(p.contains("snow")||p.contains("frozen")||p.contains("ice_spikes"))return "minecraft:snow_block";
        if(p.contains("mushroom"))return "minecraft:red_mushroom";
        if(p.contains("ocean"))return "minecraft:prismarine";
        if(p.contains("river"))return "minecraft:water_bucket";
        if(p.contains("beach"))return "minecraft:sand";
        if(p.contains("stony")||p.contains("windswept"))return "minecraft:stone";
        if(p.contains("meadow"))return "minecraft:dandelion";
        if(p.contains("flower_forest"))return "minecraft:poppy";
        if(p.contains("forest"))return "minecraft:oak_sapling";
        if(p.contains("plains"))return "minecraft:grass_block";
        return "minecraft:grass_block";
    }

    private static String path(String id) {
        int i=id.indexOf(':');return (i>=0?id.substring(i+1):id).toLowerCase(Locale.ROOT);
    }
}
