package de.ownmods.settings.layout;

import java.util.*;

/** Shared ordering/pagination for toggle rows and cyclic choice rows. No GUI imports. */
public final class SettingsRows {
    private SettingsRows() { }
    public record Entry(boolean choice, int index) { }
    public static List<Entry> entries(int toggles, int choices) {
        if(toggles<0 || choices<0)throw new IllegalArgumentException("Negative row count");
        var result=new ArrayList<Entry>();
        if(toggles>0)result.add(new Entry(false,0)); // overall enable toggle first
        for(int i=0;i<choices;i++)result.add(new Entry(true,i));
        for(int i=1;i<toggles;i++)result.add(new Entry(false,i));
        return List.copyOf(result);
    }
    public record Page(int rows, int count, int index) { }
    public static Page page(int panelHeight, int options, int requestedPage) {
        if(panelHeight<160 || options<0)throw new IllegalArgumentException("Invalid panel");
        int rows=Math.max(1,(panelHeight-110)/24);
        // Reserve the navigation row BEFORE distributing widgets, so it never overlaps them.
        if(options>rows)rows=Math.max(1,(panelHeight-134)/24);
        int count=Math.max(1,(options+rows-1)/rows);
        return new Page(rows,count,Math.max(0,Math.min(requestedPage,count-1)));
    }
}
