package de.ownmods.timber.core;

/** Immutable coordinate, independent of Minecraft so the planner can be tested offline. */
public record GridPos(int x, int y, int z) implements Comparable<GridPos> {
    public GridPos offset(int dx, int dy, int dz) { return new GridPos(x + dx, y + dy, z + dz); }
    @Override public int compareTo(GridPos other) {
        int c = Integer.compare(y, other.y);
        if (c == 0) c = Integer.compare(x, other.x);
        if (c == 0) c = Integer.compare(z, other.z);
        return c;
    }
}
