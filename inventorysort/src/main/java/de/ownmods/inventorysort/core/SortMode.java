package de.ownmods.inventorysort.core;

/** Persisted tokens do not depend on game language or an enum ordinal. */
public enum SortMode {
    ID("id"), FAMILY("family"), TYPE("type"), COLOR("color");
    private final String token;
    SortMode(String token) { this.token = token; }
    public String token() { return token; }
    public SortMode next() { return values()[(ordinal() + 1) % values().length]; }
    public static SortMode parse(String token) {
        for (var mode : values()) if (mode.token.equals(token)) return mode;
        return ID;
    }
}
