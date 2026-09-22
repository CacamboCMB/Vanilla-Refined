package de.ownmods.timber.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Minecraft-independent planting transaction. All callbacks run synchronously on the server thread.
 * In the default, free mode, item availability is NEVER consulted. The world adapter still validates
 * the original root positions and vanilla sapling survival. This is also used by the regression tests.
 */
public final class Replanting {
    private Replanting() { }
    public enum Result {
        PLANTED, INVALID_ROOTS, UNLOADED, BLOCKED, BAD_SOIL, NO_SAPLINGS,
        PLACEMENT_FAILED, RESOURCES_CHANGED, ROLLBACK_FAILED
    }
    public interface Access<S> {
        boolean loaded(GridPos pos);
        boolean empty(GridPos pos);
        boolean canSurvive(GridPos pos);
        S snapshot(GridPos pos);
        boolean place(GridPos pos);
        boolean isPlanted(GridPos pos);
        /** Must restore only our own planted state, never overwrite an unrelated replacement. */
        boolean restore(GridPos pos, S before);
        int availableSaplings();
        /** All-or-nothing: false must leave all item stacks unchanged. */
        boolean consumeSaplings(int amount);
    }
    public static <S> Result plant(List<GridPos> roots, boolean consumeItems, Access<S> access) {
        Objects.requireNonNull(roots);
        Objects.requireNonNull(access);
        roots = List.copyOf(roots);
        if (!TreePlanner.validRoots(roots)) return Result.INVALID_ROOTS;
        for (var pos : roots) {
            if (!access.loaded(pos)) return Result.UNLOADED;
            if (!access.empty(pos)) return Result.BLOCKED;
            if (!access.canSurvive(pos)) return Result.BAD_SOIL;
        }
        if (consumeItems && access.availableSaplings() < roots.size()) return Result.NO_SAPLINGS;
        var before = new ArrayList<S>();
        for (var pos : roots) before.add(access.snapshot(pos));
        int attempted = 0;
        boolean committed = false;
        try {
            for (var pos : roots) {
                // Recheck after earlier placements in case a neighbour callback modified the next root.
                if (!access.empty(pos) || !access.canSurvive(pos))
                    return failure(access, roots, before, attempted, Result.PLACEMENT_FAILED);
                attempted++;
                if (!access.place(pos) || !access.isPlanted(pos))
                    return failure(access, roots, before, attempted, Result.PLACEMENT_FAILED);
            }
            for (var pos : roots) {
                if (!access.isPlanted(pos))
                    return failure(access, roots, before, attempted, Result.PLACEMENT_FAILED);
            }
            // No items are consumed for partial placement. Resource checks and consumption must be atomic.
            if (consumeItems && !access.consumeSaplings(roots.size()))
                return failure(access, roots, before, attempted, Result.RESOURCES_CHANGED);
            committed = true;
            return Result.PLANTED;
        } catch (RuntimeException e) {
            if (!committed) {
                try {
                    if (!rollback(access, roots, before, attempted))
                        e.addSuppressed(new IllegalStateException("Replant rollback was incomplete"));
                } catch (RuntimeException rollbackError) { e.addSuppressed(rollbackError); }
            }
            throw e;
        }
    }
    private static <S> Result failure(Access<S> access, List<GridPos> roots, List<S> before,
                                      int attempted, Result reason) {
        return rollback(access, roots, before, attempted) ? reason : Result.ROLLBACK_FAILED;
    }
    private static <S> boolean rollback(Access<S> access, List<GridPos> roots, List<S> before, int attempted) {
        boolean restored = true;
        for (int i = attempted - 1; i >= 0; i--) {
            // The adapter also verifies the before state. An unrelated block is retained and reported.
            if (!access.restore(roots.get(i), before.get(i))) restored = false;
        }
        return restored;
    }
}
