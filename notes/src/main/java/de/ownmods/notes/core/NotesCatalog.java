package de.ownmods.notes.core;

import java.util.*;

/** User-selected templates, contextual filters and labels. No world structure detection. */
public final class NotesCatalog {
    private NotesCatalog() {}
    public record Entry(String id,String label,String icon) {}
    public record StatusEntry(Note.Status status,String label,String icon) {}

    public static final String OVERWORLD="minecraft:overworld";
    public static final String NETHER="minecraft:the_nether";
    public static final String END="minecraft:the_end";

    public static final List<Entry> OBJECTS = List.of(
        new Entry("homebase", "Homebase", "@home"),
        new Entry("waypoint", "Wegpunkt", "minecraft:map"),
        new Entry("ancient_city", "Ancient City / Antike Stadt", "minecraft:sculk"),
        new Entry("mineshaft", "Mineshaft / Mine", "minecraft:rail"),
        new Entry("stronghold", "Stronghold / Festung", "minecraft:ender_eye"),
        new Entry("trail_ruins", "Trail Ruins / Pfadruinen", "minecraft:brush"),
        new Entry("trial_chambers", "Trial Chambers / Prüfungskammern", "minecraft:trial_key"),
        new Entry("desert_pyramid", "Pyramid / Wüstenpyramide", "minecraft:chiseled_sandstone"),
        new Entry("igloo", "Igloo", "minecraft:snow_block"),
        new Entry("jungle_temple", "Jungle Temple / Dschungeltempel", "minecraft:mossy_cobblestone"),
        new Entry("pillager_outpost", "Pillager Outpost / Plünderer-Außenposten", "minecraft:crossbow"),
        new Entry("village", "Village / Dorf", "@village"),
        new Entry("woodland_mansion", "Woodland Mansion / Waldanwesen", "minecraft:dark_oak_planks"),
        new Entry("ocean_monument", "Ocean Monument / Ozeanmonument", "minecraft:prismarine"),
        new Entry("nether_fortress", "Nether Fortress / Netherfestung", "minecraft:nether_bricks"),
        new Entry("bastion_remnant", "Bastion Remnant / Bastionsruine", "minecraft:gilded_blackstone"),
        new Entry("end_city", "End City / Endsiedlung", "minecraft:purpur_block"),
        new Entry("biome", "Biom", "minecraft:grass_block"),
        new Entry("nether_portal", "Netherportal", "minecraft:obsidian"),
        new Entry("end_portal", "Endportal", "minecraft:end_portal_frame"),
        new Entry("spawner", "Spawner", "minecraft:spawner"),
        new Entry("other", "Anderer Ort", "minecraft:map")
    );

    public static final List<Entry> GOALS = List.of(
        new Entry("base", "Base bauen", "@home"),
        new Entry("villager_breeder", "Villager-Zucht / Breeder", "minecraft:villager_spawn_egg"),
        new Entry("trading_hall", "Villager-Handelshalle", "minecraft:emerald"),
        new Entry("mob_farm", "Mobfarm (Mob auswählen)", "minecraft:spawner"),
        new Entry("iron", "Eisenfarm", "minecraft:iron_ingot"),
        new Entry("gold", "Goldfarm", "minecraft:gold_ingot"),
        new Entry("creeper", "Creeper- / Schießpulverfarm", "minecraft:gunpowder"),
        new Entry("sugar_cane", "Zuckerrohrfarm", "minecraft:sugar_cane"),
        new Entry("bamboo", "Bambusfarm", "minecraft:bamboo"),
        new Entry("slime", "Schleimfarm", "minecraft:slime_ball"),
        new Entry("enderman", "Enderman- / XP-Farm", "minecraft:ender_pearl"),
        new Entry("blaze", "Blaze-Farm", "minecraft:blaze_rod"),
        new Entry("guardian", "Wächterfarm", "minecraft:prismarine_shard"),
        new Entry("raid", "Raidfarm", "minecraft:totem_of_undying"),
        new Entry("wither_skeleton", "Witherskelett-Farm", "minecraft:wither_skeleton_skull"),
        new Entry("shulker", "Shulkerfarm", "minecraft:shulker_shell"),
        new Entry("bartering", "Piglin-Tauschsystem", "minecraft:gold_ingot"),
        new Entry("witch", "Hexenfarm", "minecraft:redstone"),
        new Entry("drowned", "Ertrunkenenfarm", "minecraft:trident"),
        new Entry("ghast", "Ghastfarm", "minecraft:ghast_tear"),
        new Entry("hoglin", "Hoglinfarm", "minecraft:porkchop"),
        new Entry("magma_cube", "Magmawürfelfarm", "minecraft:magma_cream"),
        new Entry("zombie", "Zombiefarm", "minecraft:rotten_flesh"),
        new Entry("skeleton", "Skelettfarm", "minecraft:bone"),
        new Entry("spider", "Spinnenfarm", "minecraft:string"),
        new Entry("wheat", "Weizenfarm", "minecraft:wheat"),
        new Entry("carrot", "Karottenfarm", "minecraft:carrot"),
        new Entry("potato", "Kartoffelfarm", "minecraft:potato"),
        new Entry("beetroot", "Rote-Bete-Farm", "minecraft:beetroot"),
        new Entry("pumpkin", "Kürbisfarm", "minecraft:pumpkin"),
        new Entry("melon", "Melonenfarm", "minecraft:melon_slice"),
        new Entry("cactus", "Kaktusfarm", "minecraft:cactus"),
        new Entry("kelp", "Seetangfarm", "minecraft:kelp"),
        new Entry("cocoa", "Kakaofarm", "minecraft:cocoa_beans"),
        new Entry("sweet_berry", "Süßbeerenfarm", "minecraft:sweet_berries"),
        new Entry("glow_berry", "Leuchtbeerenfarm", "minecraft:glow_berries"),
        new Entry("tree", "Baum- / Holzfarm", "minecraft:oak_log"),
        new Entry("moss", "Moos- / Knochenmehlfarm", "minecraft:bone_meal"),
        new Entry("flower", "Blumen- / Farbstofffarm", "minecraft:poppy"),
        new Entry("honey", "Honigfarm", "minecraft:honey_bottle"),
        new Entry("wool", "Wollfarm", "minecraft:white_wool"),
        new Entry("lava", "Lavafarm", "minecraft:lava_bucket"),
        new Entry("stone", "Bruchstein- / Steinfarm", "minecraft:cobblestone"),
        new Entry("nether_wart", "Netherwarzenfarm", "minecraft:nether_wart"),
        new Entry("mushroom", "Pilzfarm", "minecraft:red_mushroom"),
        new Entry("chicken", "Hühnerfarm", "minecraft:egg"),
        new Entry("cow", "Kuh- / Lederfarm", "minecraft:leather"),
        new Entry("rabbit", "Kaninchenfarm", "minecraft:rabbit_hide"),
        new Entry("super_smelter", "Super Smelter / Großschmelze", "minecraft:furnace"),
        new Entry("storage", "Lager- / Sortiersystem", "minecraft:chest"),
        new Entry("other", "Eigenes Ziel / andere Farm", "minecraft:writable_book")
    );

    public static final List<Entry> DIMENSIONS = List.of(
        new Entry(OVERWORLD, "Overworld", "minecraft:grass_block"),
        new Entry(NETHER, "Nether", "minecraft:netherrack"),
        new Entry(END, "End", "minecraft:end_stone")
    );

    private static final Set<String> OVERWORLD_ONLY_OBJECTS=Set.of(
        "ancient_city","mineshaft","stronghold","trail_ruins","trial_chambers","desert_pyramid","igloo","jungle_temple",
        "pillager_outpost","village","woodland_mansion","ocean_monument","end_portal"
    );
    private static final Set<String> NETHER_ONLY_OBJECTS=Set.of("nether_fortress","bastion_remnant");
    private static final Set<String> END_ONLY_OBJECTS=Set.of("end_city");

    private static final Set<String> OVERWORLD_GOALS=Set.of(
        "villager_breeder","trading_hall","iron","creeper","sugar_cane","bamboo","slime","guardian","raid","witch","drowned",
        "zombie","skeleton","spider","wheat","carrot","potato","beetroot","pumpkin","melon","cactus","kelp","cocoa","sweet_berry",
        "glow_berry","tree","moss","flower","honey","wool","chicken","cow","rabbit"
    );
    private static final Set<String> NETHER_GOALS=Set.of("gold","blaze","wither_skeleton","bartering","ghast","hoglin","magma_cube","nether_wart");
    private static final Set<String> END_GOALS=Set.of("enderman","shulker");
    private static final Set<String> ANY_GOALS=Set.of("base","mob_farm","lava","stone","mushroom","super_smelter","storage","other");

    public static Entry entry(List<Entry> list,String id) {
        return list.stream().filter(e->e.id().equals(id)).findFirst().orElse(new Entry(id,id,"minecraft:paper"));
    }

    public static List<Entry> objectsForDimension(String dimension) {
        return OBJECTS.stream().filter(e->objectFitsDimension(e.id(),dimension)).toList();
    }
    public static boolean objectFitsDimension(String id,String dimension) {
        if(OVERWORLD_ONLY_OBJECTS.contains(id))return OVERWORLD.equals(dimension);
        if(NETHER_ONLY_OBJECTS.contains(id))return NETHER.equals(dimension);
        if(END_ONLY_OBJECTS.contains(id))return END.equals(dimension);
        if(id.equals("nether_portal"))return OVERWORLD.equals(dimension)||NETHER.equals(dimension);
        if(id.equals("spawner"))return OVERWORLD.equals(dimension)||NETHER.equals(dimension);
        // Homebase, generic biome and "other" can deliberately be recorded in any dimension.
        return true;
    }

    public static List<Entry> goalsForDimension(String dimension) {
        return GOALS.stream().filter(e->goalFitsDimension(e.id(),dimension)).toList();
    }
    public static boolean goalFitsDimension(String id,String dimension) {
        if(ANY_GOALS.contains(id))return true;
        if(OVERWORLD_GOALS.contains(id))return OVERWORLD.equals(dimension);
        if(NETHER_GOALS.contains(id))return NETHER.equals(dimension);
        if(END_GOALS.contains(id))return END.equals(dimension);
        return true;
    }

    /** Only statuses that make semantic sense for the selected location are returned. Empty means no status field at all. */
    public static List<StatusEntry> locationStatuses(String object) {
        return switch(object) {
            case "homebase","village","biome","waypoint","other" -> List.of();
            case "nether_portal" -> List.of(
                new StatusEntry(Note.Status.OPEN,"Geplant","minecraft:paper"),
                new StatusEntry(Note.Status.FOUND,"Aktiv","minecraft:lime_dye"),
                new StatusEntry(Note.Status.DONE,"Inaktiv","minecraft:gray_dye")
            );
            case "end_portal" -> List.of(
                new StatusEntry(Note.Status.OPEN,"Noch nicht geprüft","minecraft:paper"),
                new StatusEntry(Note.Status.FOUND,"Gefunden","minecraft:ender_eye"),
                new StatusEntry(Note.Status.DONE,"Aktiviert","minecraft:end_portal_frame")
            );
            case "spawner" -> List.of(
                new StatusEntry(Note.Status.FOUND,"Gefunden","minecraft:spawner"),
                new StatusEntry(Note.Status.EXPLORED,"Gesichert","minecraft:torch"),
                new StatusEntry(Note.Status.DONE,"Farm gebaut","minecraft:hopper")
            );
            default -> List.of(
                new StatusEntry(Note.Status.OPEN,"Unbesucht","minecraft:map"),
                new StatusEntry(Note.Status.FOUND,"Besucht","minecraft:filled_map"),
                new StatusEntry(Note.Status.EXPLORED,"Erkundet","minecraft:spyglass"),
                new StatusEntry(Note.Status.LOOTED,"Geplündert","minecraft:chest")
            );
        };
    }

    public static List<StatusEntry> goalStatuses() {
        return List.of(
            new StatusEntry(Note.Status.OPEN,"Geplant","minecraft:paper"),
            new StatusEntry(Note.Status.IN_PROGRESS,"In Arbeit","minecraft:clock"),
            new StatusEntry(Note.Status.DONE,"Erledigt","minecraft:lime_dye")
        );
    }

    public static String status(Note.Status s) {
        return switch(s){case OPEN->"Offen";case FOUND->"Gefunden";case EXPLORING->"Erkunden";case EXPLORED->"Erkundet";case LOOTED->"Geplündert";case IN_PROGRESS->"In Arbeit";case DONE->"Erledigt";};
    }
    public static String status(Note note) {
        if(note.kind==Note.Kind.GOAL) return goalStatuses().stream().filter(e->e.status()==note.status).map(StatusEntry::label).findFirst().orElse("Geplant");
        if(note.kind==Note.Kind.LOCATION) return locationStatuses(note.object).stream().filter(e->e.status()==note.status).map(StatusEntry::label).findFirst().orElse("");
        return status(note.status);
    }
    public static String kind(Note.Kind kind) {return switch(kind){case LOCATION->"Locations";case COLLECT->"Sammeln";case GOAL->"Ziele";case FREE->"Eigene Notizen";};}
    public static String icon(Note n){return switch(n.kind){case LOCATION->n.object.equals("waypoint") && n.mob!=null && n.mob.contains(":") ? n.mob : entry(OBJECTS,n.object).icon();case COLLECT->"minecraft:chest";case GOAL->entry(GOALS,n.goal).icon();case FREE->"minecraft:writable_book";};}

    /** Curated natural/common spawner types. This intentionally avoids impossible suggestions such as dragon spawn eggs. */
    public static final List<Entry> SPAWNER_MOBS=List.of(
        new Entry("minecraft:zombie","Zombie","minecraft:zombie_spawn_egg"),
        new Entry("minecraft:skeleton","Skelett","minecraft:skeleton_spawn_egg"),
        new Entry("minecraft:spider","Spinne","minecraft:spider_spawn_egg"),
        new Entry("minecraft:cave_spider","Höhlenspinne","minecraft:cave_spider_spawn_egg"),
        new Entry("minecraft:silverfish","Silberfischchen","minecraft:silverfish_spawn_egg"),
        new Entry("minecraft:blaze","Blaze","minecraft:blaze_spawn_egg"),
        new Entry("minecraft:magma_cube","Magmawürfel","minecraft:magma_cube_spawn_egg")
    );
}
