package de.ownmods.inventorysort.core;
public final class InventorySections {
    private InventorySections() { }
    public record Range(int start,int end) { }
    public static Range select(boolean inventoryMenu,int totalSlots,boolean container) {
        if(inventoryMenu) {
            if(container || totalSlots!=46)throw new IllegalArgumentException("Invalid player menu");
            return new Range(9,36);
        }
        if(totalSlots<=36 || totalSlots>144)throw new IllegalArgumentException("Invalid storage size");
        int storage=totalSlots-36;
        return container?new Range(0,storage):new Range(storage,storage+27);
    }
}
