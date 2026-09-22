package de.ownmods.gravestone.core;

/**
 * Pure resource/capacity decision for the grave.
 *
 * One chest is valued as eight planks and one log as four planks. Plans may
 * therefore combine carried chests, planks and logs, e.g. one chest + eight
 * planks is enough for a double chest.
 */
public final class GraveResourcePlanner {
    public enum Size { NONE, SINGLE, DOUBLE }

    public record Stock(int chests, int planks, int logs) {
        public Stock {
            if (chests < 0 || planks < 0 || logs < 0) throw new IllegalArgumentException("negative stock");
        }
        public int plankUnits() { return chests * 8 + planks + logs * 4; }
    }

    public record Plan(Size size, int chests, int planks, int logs, int capacity) {
        public boolean createsGrave() { return size != Size.NONE; }
        public int plankUnits() { return chests * 8 + planks + logs * 4; }
        public int resourcesConsumed() { return chests + planks + logs; }
    }

    private GraveResourcePlanner() { }

    public static Plan plan(Stock stock, boolean singleFallback) {
        Plan doublePlan = bestPlan(stock, Size.DOUBLE, 16, 54);
        if (doublePlan != null) return doublePlan;
        if (singleFallback) {
            Plan singlePlan = bestPlan(stock, Size.SINGLE, 8, 27);
            if (singlePlan != null) return singlePlan;
        }
        return new Plan(Size.NONE, 0, 0, 0, 0);
    }

    /**
     * Find the least-waste plan. With equal waste we prefer already-crafted
     * chests, then planks, preserving logs where possible.
     */
    private static Plan bestPlan(Stock s, Size size, int requiredUnits, int capacity) {
        if (s.plankUnits() < requiredUnits) return null;
        Plan best = null;
        int maxChests = Math.min(s.chests(), (requiredUnits + 7) / 8 + 1);
        int maxLogs = Math.min(s.logs(), (requiredUnits + 3) / 4 + 1);
        int maxPlanks = Math.min(s.planks(), requiredUnits + 3);

        for (int chests = 0; chests <= maxChests; chests++) {
            for (int logs = 0; logs <= maxLogs; logs++) {
                for (int planks = 0; planks <= maxPlanks; planks++) {
                    int units = chests * 8 + planks + logs * 4;
                    if (units < requiredUnits) continue;
                    Plan candidate = new Plan(size, chests, planks, logs, capacity);
                    if (better(candidate, best, requiredUnits)) best = candidate;
                }
            }
        }
        return best;
    }

    private static boolean better(Plan a, Plan b, int requiredUnits) {
        if (b == null) return true;
        int wasteA = a.plankUnits() - requiredUnits;
        int wasteB = b.plankUnits() - requiredUnits;
        if (wasteA != wasteB) return wasteA < wasteB;
        if (a.chests() != b.chests()) return a.chests() > b.chests();
        if (a.planks() != b.planks()) return a.planks() > b.planks();
        if (a.logs() != b.logs()) return a.logs() < b.logs();
        return a.resourcesConsumed() < b.resourcesConsumed();
    }
}
