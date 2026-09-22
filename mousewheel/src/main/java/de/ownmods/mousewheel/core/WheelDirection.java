package de.ownmods.mousewheel.core;

/** Pure direction policy for OwnMods Mouse Wheel. */
public final class WheelDirection {
    private WheelDirection() {}

    public enum Destination { STORAGE, INVENTORY }

    /** Minecraft scroll events use positive vertical amount for wheel-up. */
    public static Destination destination(double verticalAmount, boolean invert) {
        if (verticalAmount == 0.0 || Double.isNaN(verticalAmount)) {
            throw new IllegalArgumentException("verticalAmount must be non-zero");
        }
        Destination normal = verticalAmount > 0.0 ? Destination.STORAGE : Destination.INVENTORY;
        if (!invert) return normal;
        return normal == Destination.STORAGE ? Destination.INVENTORY : Destination.STORAGE;
    }

    public static boolean hoveredIsDestination(boolean hoveredIsPlayerInventory, Destination destination) {
        return hoveredIsPlayerInventory == (destination == Destination.INVENTORY);
    }
}
