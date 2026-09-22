package de.ownmods.notes.core;

import java.util.*;

/** Editable draft. The repository always publishes deep copies, never these mutable lists. */
public final class Note {
    public enum Kind { LOCATION, COLLECT, GOAL, FREE }
    public enum Status { OPEN, FOUND, EXPLORING, EXPLORED, LOOTED, IN_PROGRESS, DONE }
    public static final List<String> COLORS = List.of("none", "red", "green", "blue", "yellow", "purple");
    public String id = UUID.randomUUID().toString();
    public Kind kind = Kind.FREE;
    public String title = "Neue Notiz", object = "homebase", biome = "", mob = "", goal = "base";
    public Status status = Status.OPEN;
    public boolean favorite, farmBuilt, autoInventory, trashed;
    public String color = "none";
    public Position position, otherPortal;
    public long created = System.currentTimeMillis(), updated = created;
    public final List<Target> targets = new ArrayList<>();
    public final List<String> pages = new ArrayList<>(List.of(""));
    public final List<Check> checks = new ArrayList<>();
    public final List<String> links = new ArrayList<>();
    public record Position(String dimension, int x, int y, int z) {
        public Position {
            identifier(dimension);
            if (Math.abs((long)x) > 30_000_000 || Math.abs((long)z) > 30_000_000
                    || y < -2048 || y > 2048) throw new IllegalArgumentException("Koordinaten ausserhalb des erlaubten Bereichs");
        }
        /** Horizontal distance only; there is no cross-dimensional Euclidean distance. */
        public double distance(Position to) {
            return to == null || !dimension.equals(to.dimension) ? Double.POSITIVE_INFINITY
                    : Math.hypot((double)x - to.x, (double)z - to.z);
        }
        public Optional<Position> portalProjection() {
            if (dimension.equals("minecraft:overworld"))
                return Optional.of(new Position("minecraft:the_nether", Math.floorDiv(x,8), y, Math.floorDiv(z,8)));
            if (dimension.equals("minecraft:the_nether")) {
                long nx = (long)x*8, nz = (long)z*8;
                if (Math.abs(nx)>30_000_000 || Math.abs(nz)>30_000_000) return Optional.empty();
                return Optional.of(new Position("minecraft:overworld", (int)nx, y, (int)nz));
            }
            return Optional.empty();
        }
    }
    public record Target(String item, int wanted, int manualCount) {
        public Target {
            identifier(item);
            if (wanted < 1 || wanted > 9999 || manualCount < 0 || manualCount > 9999)
                throw new IllegalArgumentException("Ziel: 1–9999, Fortschritt: 0–9999");
        }
    }
    public record Check(String text, boolean done) {
        public Check { length(text, 240, "Aufgabe"); if(text.isBlank()) throw new IllegalArgumentException("Aufgabe ist leer"); }
    }
    public static void identifier(String value) {
        if (value == null || value.length()>200 || !value.matches("[a-z0-9_.-]+:[a-z0-9_./-]+"))
            throw new IllegalArgumentException("Ungueltige Kennung: " + value);
    }
    public static void length(String value, int max, String field) {
        if (value == null || value.length()>max || value.indexOf('\0')>=0)
            throw new IllegalArgumentException(field + " ist ungueltig/zu lang");
    }
    public void validate() {
        UUID.fromString(id); Objects.requireNonNull(kind); Objects.requireNonNull(status);
        length(title, 100, "Titel"); if(title.isBlank()) throw new IllegalArgumentException("Titel fehlt");
        length(object, 200, "Objekt"); length(biome, 200, "Biom"); length(mob, 200, "Mob"); length(goal,200,"Ziel");
        if(!COLORS.contains(color)) throw new IllegalArgumentException("Unbekannte Farbe");
        if(kind==Kind.LOCATION && position==null) throw new IllegalArgumentException("Location benoetigt Koordinaten");
        if(pages.isEmpty() || pages.size()>24) throw new IllegalArgumentException("1–24 Buchseiten erlaubt");
        pages.forEach(p -> length(p,4000,"Buchseite"));
        if(targets.size()>256 || checks.size()>200 || links.size()>100) throw new IllegalArgumentException("Zu viele Eintraege");
        if(targets.stream().map(Target::item).distinct().count()!=targets.size()) throw new IllegalArgumentException("Doppeltes Sammelitem");
        if(links.stream().distinct().count()!=links.size()) throw new IllegalArgumentException("Doppelter Verweis");
        for(String link:links) { UUID.fromString(link); if(id.equals(link)) throw new IllegalArgumentException("Kein Selbstverweis"); }
        if(created < 0 || updated < 0) throw new IllegalArgumentException("Ungueltiger Zeitstempel");
    }
    public Note copy() {
        Note n = new Note(); n.id=id; n.kind=kind; n.title=title; n.object=object; n.biome=biome; n.mob=mob; n.goal=goal;
        n.status=status; n.favorite=favorite; n.farmBuilt=farmBuilt; n.autoInventory=autoInventory; n.trashed=trashed; n.color=color;
        n.position=position; n.otherPortal=otherPortal; n.created=created; n.updated=updated;
        n.targets.addAll(targets); n.pages.clear(); n.pages.addAll(pages); n.checks.addAll(checks); n.links.addAll(links); return n;
    }
    public int progressPercent(Map<String,Integer> inventory) {
        long wanted=0, have=0;
        for(Target t:targets) { wanted+=t.wanted; have+=Math.min(t.wanted, Math.max(0,autoInventory?inventory.getOrDefault(t.item,0):t.manualCount)); }
        return wanted==0?0:(int)(100L*have/wanted);
    }
    public boolean matches(String query) {
        String hay = title+" "+object+" "+biome+" "+mob+" "+goal+" "+String.join(" ",pages)+" "+checks+" "+targets;
        return hay.toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT).strip());
    }
}
