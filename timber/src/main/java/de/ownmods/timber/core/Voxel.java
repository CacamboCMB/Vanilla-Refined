package de.ownmods.timber.core;

/** family identifies the natural tree species, not a registry namespace. */
public record Voxel(Kind kind, String family, boolean persistent, int leafDistance, boolean rootSupport) {
    public enum Kind { AIR, OTHER, SOIL, LOG, LEAF, UNKNOWN }
    public static final Voxel AIR = new Voxel(Kind.AIR, "", false, 7, false);
    public static final Voxel OTHER = new Voxel(Kind.OTHER, "", false, 7, false);
    public static final Voxel SOIL = new Voxel(Kind.SOIL, "", false, 7, true);
    public static final Voxel UNKNOWN = new Voxel(Kind.UNKNOWN, "", false, 7, false);
    public static Voxel log(String family) { return new Voxel(Kind.LOG, family, false, 7, false); }
    public static Voxel leaf(String family, boolean persistent, int distance) {
        return new Voxel(Kind.LEAF, family, persistent, distance, false);
    }
}
