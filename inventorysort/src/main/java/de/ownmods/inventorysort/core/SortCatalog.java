package de.ownmods.inventorysort.core;

import java.util.*;

/**
 * Deterministic semantic categories, independent of Minecraft GUI/rendering and locale.
 * This is NOT texture recognition. Explicit colour families, live item tags and curated
 * natural material colours are used; unidentified items retain a stable fallback order.
 * Function wins over material for redstone parts and equipment in FAMILY mode.
 */
public final class SortCatalog {
    private SortCatalog() { }
    public enum Family {
        REDSTONE, WOOD, STONE, MINERALS, EARTH, COLOURED_BUILDING, FARMING,
        EQUIPMENT, FOOD, STORAGE_TRANSPORT, WORKSTATIONS, MAGIC, OTHER_BLOCKS, OTHER
    }
    /** Adjacent groups also form larger useful sections: construction, equipment, farming. */
    public enum Kind {
        FULL_BLOCKS, STAIRS, SLABS, WALLS, FENCES, FENCE_GATES, BARS_GRATES,
        DOORS, TRAPDOORS, GLASS_PANES, CARPETS, BEDS, BANNERS, LIGHTS,
        CONTAINERS, REDSTONE_PARTS, TRANSPORT, WORKSTATIONS,
        SWORDS, PICKAXES, AXES, HOES, SHOVELS, SPEARS, RANGED_WEAPONS,
        HELMETS, CHESTPLATES, LEGGINGS, BOOTS, TOOLS_OTHER,
        SEEDS_CROPS, SAPLINGS, LEAVES, PLANTS, FOOD, RAW_MATERIALS, DYES,
        POTIONS, BOOKS, OTHER_BLOCKS, OTHER
    }
    public enum Tone {
        WHITE, LIGHT_GRAY, GRAY, BLACK, BROWN, RED, ORANGE, YELLOW,
        LIME, GREEN, CYAN, LIGHT_BLUE, BLUE, PURPLE, MAGENTA, PINK, UNKNOWN;
        public String token() { return name().toLowerCase(Locale.ROOT); }
    }
    public record Facts(String id, boolean block, Set<String> tags) {
        public Facts {
            Objects.requireNonNull(id);
            if (!id.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) throw new IllegalArgumentException("Invalid item ID: " + id);
            tags = Set.copyOf(tags);
        }
        public String path() { return id.substring(id.indexOf(':') + 1); }
        public boolean has(String tag) { return tags.contains(tag); }
    }
    public record Traits(Family family, String material, Kind kind, Tone tone) { }

    private static final Set<String> REDSTONE = words("redstone redstone_block redstone_ore deepslate_redstone_ore repeater comparator " +
        "piston sticky_piston observer dispenser dropper hopper lever target tnt tripwire_hook daylight_detector " +
        "redstone_lamp redstone_torch trapped_chest crafter sculk_sensor calibrated_sculk_sensor lightning_rod " +
        "note_block powered_rail detector_rail activator_rail rail");
    private static final Set<String> CROP_ITEMS = words("wheat_seeds beetroot_seeds pumpkin_seeds melon_seeds torchflower_seeds " +
        "pitcher_pod cocoa_beans sweet_berries glow_berries carrot potato beetroot wheat nether_wart");
    private static final Set<String> FOODS = words("apple golden_apple enchanted_golden_apple bread cookie cake pumpkin_pie " +
        "melon_slice glistering_melon_slice dried_kelp honey_bottle mushroom_stew rabbit_stew beetroot_soup suspicious_stew " +
        "beef porkchop chicken mutton rabbit cod salmon tropical_fish pufferfish rotten_flesh spider_eye egg turtle_egg sniffer_egg " +
        "baked_potato poisonous_potato golden_carrot");
    private static final Set<String> WORKSTATIONS = words("crafting_table furnace blast_furnace smoker stonecutter smithing_table " +
        "fletching_table cartography_table loom grindstone anvil chipped_anvil damaged_anvil enchanting_table " +
        "brewing_stand cauldron composter lectern");
    private static final Set<String> TOOLS = words("shears flint_and_steel brush fishing_rod carrot_on_a_stick warped_fungus_on_a_stick " +
        "compass recovery_compass clock spyglass shield elytra lead saddle name_tag fire_charge wind_charge");
    private static final Set<String> PLANTS = words("dandelion poppy blue_orchid allium azure_bluet oxeye_daisy cornflower lily_of_the_valley " +
        "wither_rose torchflower pitcher_plant sunflower lilac rose_bush peony pink_petals wildflowers cactus_flower " +
        "cactus bamboo sugar_cane kelp seagrass sea_pickle lily_pad vine glow_lichen hanging_roots dead_bush fern large_fern " +
        "short_grass tall_grass bush firefly_bush leaf_litter spore_blossom crimson_fungus warped_fungus crimson_roots " +
        "warped_roots nether_sprouts weeping_vines twisting_vines brown_mushroom red_mushroom");
    private static final Set<String> SOILS = words("dirt coarse_dirt rooted_dirt grass_block podzol mycelium mud packed_mud " +
        "sand red_sand gravel clay snow snow_block powder_snow ice packed_ice blue_ice soul_sand soul_soil moss_block pale_moss_block");
    private static final Set<String> RAW = words("stick string feather leather flint paper bone bone_meal gunpowder slime_ball " +
        "honeycomb ink_sac glow_ink_sac clay_ball brick nether_brick resin_brick resin_clump coal charcoal quartz " +
        "amethyst_shard prismarine_shard prismarine_crystals echo_shard diamond emerald lapis_lazuli redstone " +
        "blaze_rod blaze_powder breeze_rod nether_star ender_pearl ender_eye ghast_tear rabbit_hide rabbit_foot phantom_membrane " +
        "scute turtle_scute armadillo_scute shulker_shell nautilus_shell heart_of_the_sea magma_cream");
    // Long/specific names first: dark_oak must never become oak, red_sandstone never generic stone.
    private static final List<String> WOODS = List.of("dark_oak", "pale_oak", "mangrove", "cherry", "spruce", "birch", "jungle", "acacia", "bamboo", "crimson", "warped", "oak");
    private static final Set<String> WOOD_PARTS = words("log wood planks stairs slab fence fence_gate door trapdoor button pressure_plate " +
        "sign hanging_sign boat chest_boat raft chest_raft stem hyphae block mosaic mosaic_stairs mosaic_slab");
    private static final List<String> STONES = List.of("red_sandstone", "sandstone", "deepslate", "blackstone", "prismarine", "end_stone",
        "nether_brick", "resin_brick", "mud_brick", "cobblestone", "stone_brick", "smooth_stone", "granite", "diorite", "andesite",
        "dripstone", "netherrack", "basalt", "calcite", "quartz", "purpur", "tuff", "brick", "stone");
    private static final List<String> MINERALS = List.of("netherite", "diamond", "emerald", "lapis", "amethyst", "copper", "iron", "gold", "coal", "quartz");
    private static Set<String> words(String values) { return Set.of(values.split(" +")); }
    private static boolean token(String path, String token) {
        return path.equals(token) || path.startsWith(token + "_") || path.endsWith("_" + token) || path.contains("_" + token + "_");
    }
    private static boolean tag(Facts f, String path) { return f.has("minecraft:" + path) || f.has("c:" + path); }
    private static boolean suffix(String p, String s) { return p.endsWith("_" + s); }
    private static boolean equipment(Kind k) { return k.ordinal() >= Kind.SWORDS.ordinal() && k.ordinal() <= Kind.TOOLS_OTHER.ordinal(); }
    private static boolean redstone(Facts f) {
        String p = f.path();
        return REDSTONE.contains(p) || suffix(p,"button") || suffix(p,"pressure_plate") ||
            p.equals("redstone_wall_torch") || f.has("c:redstone_dusts") || f.has("c:redstone_components");
    }
    private static boolean crop(Facts f) {
        String p=f.path();
        return CROP_ITEMS.contains(p) || suffix(p,"seeds") || suffix(p,"berries") || suffix(p,"beans") ||
            tag(f,"seeds") || tag(f,"crops");
    }
    private static boolean plant(Facts f) {
        String p=f.path();
        return PLANTS.contains(p) || suffix(p,"tulip") || suffix(p,"coral") || suffix(p,"coral_fan") ||
            suffix(p,"mushroom") || tag(f,"flowers") || tag(f,"small_flowers") || tag(f,"tall_flowers");
    }
    private static String wood(Facts f) {
        String p=f.path(); if(p.startsWith("stripped_"))p=p.substring(9);
        for(String material:WOODS) if(p.startsWith(material+"_") && WOOD_PARTS.contains(p.substring(material.length()+1))) return material;
        if(p.equals("stick") || p.equals("ladder") || p.equals("bowl") || p.equals("crafting_table") || p.equals("bookshelf") || p.equals("chiseled_bookshelf"))return "wood";
        if(tag(f,"logs") || tag(f,"logs_that_burn") || tag(f,"planks") || tag(f,"wooden_slabs") || tag(f,"wooden_stairs") ||
            tag(f,"wooden_fences") || tag(f,"wooden_doors") || tag(f,"wooden_trapdoors") || f.has("c:wooden_blocks"))return "wood";
        return "";
    }
    private static String stone(Facts f) {
        String p=f.path();
        if(!f.block() || suffix(p,"ore") || suffix(p,"block") && (p.equals("redstone_block") || p.equals("coal_block")))return "";
        for(String material:STONES) if(token(p,material) || token(p,material+"s") || token(p,material+"_block")) {
            if(material.equals("cobblestone") || material.equals("stone_brick") || material.equals("smooth_stone")) return "stone";
            return material;
        }
        if(f.has("c:stones") || f.has("c:cobblestones") || tag(f,"stone_bricks") || tag(f,"stone_crafting_materials"))return "stone";
        return "";
    }
    private static String mineral(Facts f) {
        String p=f.path();
        for(String name:MINERALS)if(token(p,name) || name.equals("gold") && token(p,"golden"))return name;
        if(f.has("c:ores") || f.has("c:ingots") || f.has("c:nuggets") || f.has("c:gems") || f.has("c:raw_materials"))return "minerals";
        return "";
    }
    private static boolean colouredMaterial(String p) {
        return p.equals("glass") || p.equals("tinted_glass") || token(p,"wool") || token(p,"concrete") || token(p,"terracotta") ||
            token(p,"glass") || suffix(p,"carpet") || suffix(p,"bed") || suffix(p,"banner") || suffix(p,"candle") || suffix(p,"dye");
    }
    public static Kind kind(Facts f) {
        String p=f.path();
        // Check specialised forms before the generic block classification.
        if(suffix(p,"stairs") || tag(f,"stairs"))return Kind.STAIRS;
        if(suffix(p,"slab") || tag(f,"slabs"))return Kind.SLABS;
        if(suffix(p,"wall") || tag(f,"walls"))return Kind.WALLS;
        if(suffix(p,"fence_gate") || tag(f,"fence_gates"))return Kind.FENCE_GATES;
        if(suffix(p,"fence") || tag(f,"fences"))return Kind.FENCES;
        if(p.equals("iron_bars") || suffix(p,"bars") || suffix(p,"grate") || p.equals("chain") || suffix(p,"chain"))return Kind.BARS_GRATES;
        if(suffix(p,"trapdoor") || tag(f,"trapdoors"))return Kind.TRAPDOORS;
        if(suffix(p,"door") || tag(f,"doors"))return Kind.DOORS;
        if(p.equals("glass_pane") || suffix(p,"glass_pane"))return Kind.GLASS_PANES;
        if(suffix(p,"carpet") || tag(f,"wool_carpets"))return Kind.CARPETS;
        if(suffix(p,"bed") || tag(f,"beds"))return Kind.BEDS;
        if(suffix(p,"banner") || tag(f,"banners"))return Kind.BANNERS;
        if(crop(f))return Kind.SEEDS_CROPS;
        if(suffix(p,"sapling") || p.equals("mangrove_propagule") || tag(f,"saplings"))return Kind.SAPLINGS;
        if(suffix(p,"leaves") || tag(f,"leaves"))return Kind.LEAVES;
        if(plant(f))return Kind.PLANTS;
        if(suffix(p,"sword") || tag(f,"swords") || f.has("c:tools/sword"))return Kind.SWORDS;
        if(suffix(p,"pickaxe") || tag(f,"pickaxes") || f.has("c:tools/pickaxe"))return Kind.PICKAXES;
        if(suffix(p,"axe") || tag(f,"axes") || f.has("c:tools/axe"))return Kind.AXES;
        if(suffix(p,"hoe") || tag(f,"hoes") || f.has("c:tools/hoe"))return Kind.HOES;
        if(suffix(p,"shovel") || tag(f,"shovels") || f.has("c:tools/shovel"))return Kind.SHOVELS;
        if(suffix(p,"spear") || p.equals("trident") || p.equals("mace"))return Kind.SPEARS;
        if(p.equals("bow") || p.equals("crossbow") || p.equals("arrow") || suffix(p,"arrow"))return Kind.RANGED_WEAPONS;
        if(suffix(p,"helmet") || tag(f,"head_armor"))return Kind.HELMETS;
        if(suffix(p,"chestplate") || tag(f,"chest_armor"))return Kind.CHESTPLATES;
        if(suffix(p,"leggings") || tag(f,"leg_armor"))return Kind.LEGGINGS;
        if(suffix(p,"boots") || tag(f,"foot_armor"))return Kind.BOOTS;
        if(TOOLS.contains(p) || suffix(p,"bucket") || p.equals("bucket") || suffix(p,"horse_armor") || p.equals("wolf_armor"))return Kind.TOOLS_OTHER;
        if(p.equals("chest") || p.equals("trapped_chest") || p.equals("ender_chest") || p.equals("barrel") ||
            p.equals("shulker_box") || suffix(p,"shulker_box") || p.equals("bundle") || suffix(p,"bundle"))return Kind.CONTAINERS;
        if(p.equals("rail") || suffix(p,"rail") || p.equals("minecart") || suffix(p,"minecart") || suffix(p,"boat") || suffix(p,"raft"))return Kind.TRANSPORT;
        if(WORKSTATIONS.contains(p))return Kind.WORKSTATIONS;
        // Redstone block and ore are full cubes, unlike small components such as repeaters.
        if(redstone(f) && !p.equals("redstone_block") && !suffix(p,"ore") &&
            !Set.of("piston","sticky_piston","observer","dispenser","dropper","target","tnt","redstone_lamp","note_block","crafter").contains(p))return Kind.REDSTONE_PARTS;
        if(p.equals("torch") || suffix(p,"torch") || p.equals("lantern") || suffix(p,"lantern") || p.equals("candle") || suffix(p,"candle") ||
            p.equals("campfire") || suffix(p,"campfire") || p.equals("end_rod"))return Kind.LIGHTS;
        if(suffix(p,"dye") || tag(f,"dyes"))return Kind.DYES;
        if(token(p,"potion") || p.equals("dragon_breath") || p.equals("experience_bottle"))return Kind.POTIONS;
        if(p.equals("book") || suffix(p,"book") || p.equals("paper") || p.equals("map") || p.equals("filled_map"))return Kind.BOOKS;
        if(FOODS.contains(p) || p.startsWith("cooked_") || f.has("c:foods"))return Kind.FOOD;
        if(RAW.contains(p) || suffix(p,"ingot") || suffix(p,"nugget") || suffix(p,"shard") || suffix(p,"scrap") || p.startsWith("raw_") && !f.block() ||
            f.has("c:ingots") || f.has("c:gems") || f.has("c:nuggets"))return Kind.RAW_MATERIALS;
        if(f.block()) {
            // Known partial decorations must not be advertised as solid cubes.
            if(suffix(p,"sign") || p.equals("ladder") || p.equals("scaffolding") || p.equals("flower_pot") ||
                p.equals("pointed_dripstone") || token(p,"amethyst_bud") || p.equals("amethyst_cluster") ||
                suffix(p,"head") || suffix(p,"skull") || p.equals("cobweb") || p.equals("snow") || p.equals("bell"))return Kind.OTHER_BLOCKS;
            if(!wood(f).isEmpty() || !stone(f).isEmpty() || !mineral(f).isEmpty() || SOILS.contains(p) ||
                colouredMaterial(p) || redstone(f) || suffix(p,"block") || suffix(p,"bricks") ||
                Set.of("obsidian","crying_obsidian","bedrock","sponge","wet_sponge","glowstone","sea_lantern","shroomlight","honeycomb_block").contains(p))return Kind.FULL_BLOCKS;
            return Kind.OTHER_BLOCKS;
        }
        return Kind.OTHER;
    }
    public static Family family(Facts f, Kind k) {
        if(equipment(k))return Family.EQUIPMENT;
        if(redstone(f))return Family.REDSTONE;
        if(!wood(f).isEmpty())return Family.WOOD;
        if(!stone(f).isEmpty())return Family.STONE;
        if(!mineral(f).isEmpty())return Family.MINERALS;
        if(SOILS.contains(f.path()))return Family.EARTH;
        if(colouredMaterial(f.path()))return Family.COLOURED_BUILDING;
        if(k==Kind.SEEDS_CROPS || k==Kind.SAPLINGS || k==Kind.LEAVES || k==Kind.PLANTS || f.path().equals("bone_meal"))return Family.FARMING;
        if(k==Kind.FOOD)return Family.FOOD;
        if(k==Kind.CONTAINERS || k==Kind.TRANSPORT)return Family.STORAGE_TRANSPORT;
        if(k==Kind.WORKSTATIONS)return Family.WORKSTATIONS;
        if(k==Kind.POTIONS || k==Kind.BOOKS)return Family.MAGIC;
        return f.block()?Family.OTHER_BLOCKS:Family.OTHER;
    }
    public static String material(Facts f, Family family) {
        return switch(family) {
            case WOOD -> wood(f);
            case STONE -> stone(f);
            case MINERALS -> mineral(f);
            case COLOURED_BUILDING -> {
                String p=f.path();
                yield token(p,"wool") || suffix(p,"carpet") ? "wool" : token(p,"glass") ? "glass" :
                    token(p,"terracotta") ? "terracotta" : token(p,"concrete") ? "concrete" : "decoration";
            }
            default -> "";
        };
    }
    public static Tone tone(Facts f) {
        String p=f.path();
        if(p.equals("red_sand") || token(p,"red_sandstone"))return Tone.ORANGE;
        // Longest colour prefix first. The tag fallback also recognises conventionally tagged mod items.
        var tones=Arrays.stream(Tone.values()).filter(t->t!=Tone.UNKNOWN)
            .sorted(Comparator.comparingInt((Tone t)->t.token().length()).reversed()).toList();
        for(Tone t:tones) if(p.equals(t.token()+"_dye") || p.startsWith(t.token()+"_") || f.has("c:dyed/"+t.token()) || f.has("c:dyes/"+t.token()))return t;
        if(token(p,"wooden") || p.equals("note_block") || p.equals("chest_minecart") || p.equals("lectern") || p.equals("loom") ||
            p.equals("composter") || p.equals("smithing_table") || p.equals("fletching_table") || p.equals("cartography_table") ||
            p.equals("cookie") || p.equals("cake") || p.startsWith("cooked_") || p.equals("rabbit_hide") || p.equals("saddle"))return Tone.BROWN;
        if((token(p,"stone") && !f.block()) || p.equals("furnace") || p.equals("blast_furnace") || p.equals("smoker") ||
            p.equals("dispenser") || p.equals("dropper") || p.equals("observer") || p.equals("piston") || p.equals("cauldron") || p.equals("stonecutter") || p.equals("grindstone"))return Tone.GRAY;
        if(p.equals("lightning_rod") || p.equals("resin_clump") || p.equals("resin_block"))return Tone.ORANGE;
        if(p.equals("beef") || p.equals("salmon") || p.equals("mutton") || token(p,"nether_wart"))return Tone.RED;
        if(p.equals("porkchop") || p.equals("rabbit") || p.equals("pink_petals") || p.equals("cherry_sapling"))return Tone.PINK;
        if(p.equals("chicken") || p.equals("egg") || p.equals("lily_of_the_valley"))return Tone.WHITE;
        if(p.equals("sticky_piston") || p.equals("melon"))return Tone.GREEN;
        if(token(p,"copper") && (token(p,"oxidized") || token(p,"weathered")))return Tone.CYAN;
        if(token(p,"copper") && token(p,"exposed"))return Tone.BROWN;
        if(token(p,"copper") || p.equals("carrot") || token(p,"pumpkin") || p.equals("lava_bucket") || p.equals("blaze_powder") || p.equals("blaze_rod"))return Tone.ORANGE;
        if(token(p,"gold") || token(p,"golden") || p.equals("wheat") || token(p,"hay") || p.equals("glowstone") || p.equals("glowstone_dust") ||
            p.equals("honey_bottle") || p.equals("honeycomb") || p.equals("sunflower") || p.equals("dandelion") || p.equals("end_stone") || p.startsWith("end_stone_") || token(p,"sponge"))return Tone.YELLOW;
        if(p.equals("redstone") || token(p,"redstone") || p.equals("repeater") || p.equals("comparator") || p.equals("apple") || p.equals("sweet_berries") ||
            p.equals("beetroot") || p.equals("poppy") || p.equals("nether_wart") || p.equals("netherrack") || token(p,"nether_brick"))return Tone.RED;
        if(token(p,"diamond") || token(p,"prismarine") || p.equals("ender_pearl") || p.equals("ender_eye"))return Tone.CYAN;
        if(token(p,"lapis") || p.equals("water_bucket") || p.equals("cornflower"))return Tone.BLUE;
        if(token(p,"amethyst") || token(p,"purpur") || p.equals("chorus_fruit") || p.equals("shulker_box") || p.equals("shulker_shell"))return Tone.PURPLE;
        if(token(p,"netherite") || token(p,"coal") || p.equals("charcoal") || token(p,"blackstone") || p.equals("obsidian") || p.equals("crying_obsidian") ||
            p.equals("ink_sac") || p.equals("wither_rose") || token(p,"sculk"))return Tone.BLACK;
        if(token(p,"iron") || p.equals("clay") || p.equals("clay_ball"))return Tone.LIGHT_GRAY;
        if(p.equals("glass") || p.equals("glass_pane") || p.equals("bone") || p.equals("bone_meal") || p.equals("sugar") || p.equals("paper") ||
            token(p,"quartz") || p.equals("snow") || p.equals("snow_block") || p.equals("calcite") || p.equals("diorite") || token(p,"diorite") ||
            p.equals("string") || p.equals("feather") || p.equals("white_tulip") || p.equals("lily_of_the_valley"))return Tone.WHITE;
        if(p.equals("ice") || p.equals("packed_ice") || p.equals("blue_ice") || p.equals("blue_orchid"))return Tone.LIGHT_BLUE;
        if(p.equals("slime_ball") || p.equals("slime_block") || p.equals("wheat_seeds") || p.equals("glow_berries"))return Tone.LIME;
        if(token(p,"emerald") || suffix(p,"leaves") || suffix(p,"sapling") || tag(f,"leaves") || tag(f,"saplings") || p.equals("grass_block") ||
            token(p,"moss") || p.equals("cactus") || p.equals("bamboo") || p.equals("sugar_cane") || p.equals("kelp") || p.equals("vine"))return Tone.GREEN;
        String wood=wood(f);
        if(!wood.isEmpty())return switch(wood) {
            case "cherry" -> Tone.PINK; case "mangrove" -> Tone.RED; case "crimson" -> Tone.MAGENTA;
            case "warped" -> Tone.CYAN; case "bamboo" -> Tone.YELLOW; case "birch", "pale_oak" -> Tone.WHITE;
            case "acacia" -> Tone.ORANGE; default -> Tone.BROWN;
        };
        if(token(p,"sandstone") || p.equals("sand"))return Tone.YELLOW;
        if(p.equals("granite") || token(p,"granite") || p.equals("brick") || p.equals("bricks") || p.startsWith("brick_"))return Tone.RED;
        if(!stone(f).isEmpty() || p.equals("gravel") || p.equals("flint") || p.equals("gunpowder") || p.equals("hopper") || p.equals("anvil"))return Tone.GRAY;
        if(SOILS.contains(p) || p.equals("cocoa_beans") || p.equals("leather") || token(p,"leather") || p.equals("bread") || p.equals("potato") ||
            p.equals("baked_potato") || p.equals("chest") || p.equals("barrel") || p.equals("bowl") || p.equals("fishing_rod") || p.equals("bow") || p.equals("crossbow"))return Tone.BROWN;
        return Tone.UNKNOWN;
    }
    public static Traits classify(Facts f) {
        Kind k=kind(f); Family family=family(f,k);
        return new Traits(family,material(f,family),k,tone(f));
    }
    private static String rank(int n) { return n<10?"0"+n:Integer.toString(n); }
    public static String prefix(Facts f, SortMode mode) {
        if(mode==SortMode.ID)return "";
        var t=classify(f);
        return switch(mode) {
            case ID -> "";
            case FAMILY -> rank(t.family().ordinal())+"/"+t.material()+"/"+rank(t.kind().ordinal())+"/";
            case TYPE -> rank(t.kind().ordinal())+"/"+rank(t.family().ordinal())+"/"+t.material()+"/";
            case COLOR -> rank(t.tone().ordinal())+"/"+rank(t.kind().ordinal())+"/";
        };
    }
    public static String orderKey(Facts f, SortMode mode, String name) {
        return prefix(f,mode)+f.id()+"\u0000"+Objects.requireNonNull(name);
    }
    /** Live probes, not hard-coded ItemTags fields that may change between game versions. */
    public static Set<String> relevantTags() {
        var result=new LinkedHashSet<String>();
        for(String name:List.of("logs","logs_that_burn","planks","wooden_slabs","wooden_stairs","wooden_fences","wooden_doors","wooden_trapdoors",
            "stone_bricks","stone_crafting_materials","stairs","slabs","walls","fences","fence_gates","trapdoors","doors","wool_carpets","beds","banners",
            "saplings","leaves","flowers","small_flowers","tall_flowers","swords","pickaxes","axes","hoes","shovels","head_armor","chest_armor","leg_armor","foot_armor"))result.add("minecraft:"+name);
        for(String name:List.of("wooden_blocks","stones","cobblestones","ores","ingots","nuggets","gems","raw_materials","foods","seeds","crops","dyes",
            "redstone_dusts","redstone_components","tools/sword","tools/pickaxe","tools/axe","tools/hoe","tools/shovel"))result.add("c:"+name);
        for(Tone tone:Tone.values())if(tone!=Tone.UNKNOWN){result.add("c:dyed/"+tone.token());result.add("c:dyes/"+tone.token());}
        return Set.copyOf(result);
    }
}
