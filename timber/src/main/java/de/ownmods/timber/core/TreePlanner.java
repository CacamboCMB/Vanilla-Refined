package de.ownmods.timber.core;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * Read-only, bounded tree recognition. Never edits the world and never follows a leaf
 * into another trunk. Ambiguous touching trunks are rejected instead of partially cut.
 * This is a heuristic, not a proof that a structure was naturally generated.
 */
public final class TreePlanner {
    private static final int[][] FACES = {{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}};
    private static final int LEAF_REACH = 6;
    public record Limits(int maxLogs, int maxLeaves, int maxLeafGraph, int horizontalRadius,
                         int verticalRadius, int maxReads) {
        public Limits {
            if (maxLogs < 1 || maxLeaves < 4 || maxLeafGraph < maxLeaves || horizontalRadius < 1
                    || verticalRadius < 2 || maxReads < 1) throw new IllegalArgumentException("Invalid limits");
        }
        public static Limits defaults() { return new Limits(256, 2048, 12000, 16, 64, 120000); }
    }
    public enum Reason { OK, NOT_A_LOG, TOO_SMALL, SEARCH_LIMIT, UNLOADED_CHUNK, AMBIGUOUS_ROOTS, NO_NATURAL_LEAVES }
    public record Result(Optional<TreePlan> plan, Reason reason) {
        public static Result reject(Reason reason) { return new Result(Optional.empty(), reason); }
    }
    private static final class ReadLimit extends RuntimeException { }
    private final Limits limits;
    public TreePlanner() { this(Limits.defaults()); }
    public TreePlanner(Limits limits) { this.limits = limits; }

    public Result analyze(Function<GridPos, Voxel> source, GridPos origin) {
        var cache = new HashMap<GridPos, Voxel>();
        Function<GridPos, Voxel> world = p -> {
            var old = cache.get(p);
            if (old != null) return old;
            if (cache.size() >= limits.maxReads()) throw new ReadLimit();
            Voxel value = source.apply(p);
            if (value == null) value = Voxel.UNKNOWN;
            cache.put(p, value);
            return value;
        };
        try { return analyzeCached(world, origin); }
        catch (ReadLimit e) { return Result.reject(Reason.SEARCH_LIMIT); }
    }
    private Result analyzeCached(Function<GridPos, Voxel> world, GridPos origin) {
        Voxel first = world.apply(origin);
        if (first.kind() != Voxel.Kind.LOG || first.family().isBlank()) return Result.reject(Reason.NOT_A_LOG);
        String family = first.family();
        var logs = new LinkedHashSet<GridPos>();
        var queue = new ArrayDeque<GridPos>();
        logs.add(origin);
        queue.add(origin);
        while (!queue.isEmpty()) {
            GridPos p = queue.removeFirst();
            for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dy == 0 && dz == 0) continue;
                GridPos n = p.offset(dx, dy, dz);
                Voxel cell = world.apply(n);
                if (cell.kind() == Voxel.Kind.UNKNOWN) return Result.reject(Reason.UNLOADED_CHUNK);
                if (cell.kind() != Voxel.Kind.LOG || !cell.family().equals(family) || logs.contains(n)) continue;
                if (Math.abs((long)n.x() - origin.x()) > limits.horizontalRadius()
                        || Math.abs((long)n.z() - origin.z()) > limits.horizontalRadius()
                        || Math.abs((long)n.y() - origin.y()) > limits.verticalRadius()
                        || logs.size() >= limits.maxLogs()) return Result.reject(Reason.SEARCH_LIMIT);
                logs.add(n);
                queue.addLast(n);
            }
        }
        if (logs.size() < 3) return Result.reject(Reason.TOO_SMALL);
        List<GridPos> roots = logs.stream().filter(p -> world.apply(p.offset(0,-1,0)).rootSupport()).sorted().toList();
        if (!validRoots(roots)) return Result.reject(Reason.AMBIGUOUS_ROOTS);
        int baseY = roots.getFirst().y();
        if (logs.stream().mapToInt(GridPos::y).max().orElse(baseY) - baseY < 2) return Result.reject(Reason.TOO_SMALL);

        // A 12-step halo contains every competing support path of <=6 steps for
        // a leaf whose own support is <=6. The halo itself is never removed.
        var ownDistance = new LinkedHashMap<GridPos, Integer>();
        for (var log : logs) for (var delta : FACES) {
            var n = log.offset(delta[0], delta[1], delta[2]);
            if (world.apply(n).kind() == Voxel.Kind.LEAF && ownDistance.putIfAbsent(n, 1) == null) queue.addLast(n);
        }
        while (!queue.isEmpty()) {
            var p = queue.removeFirst();
            if (ownDistance.size() > limits.maxLeafGraph()) return Result.reject(Reason.SEARCH_LIMIT);
            int distance = ownDistance.get(p);
            if (distance >= LEAF_REACH * 2) continue;
            for (var delta : FACES) {
                var n = p.offset(delta[0], delta[1], delta[2]);
                if (world.apply(n).kind() != Voxel.Kind.LEAF || ownDistance.containsKey(n)) continue;
                ownDistance.put(n, distance + 1);
                queue.addLast(n);
            }
        }
        var foreignDistance = new HashMap<GridPos, Integer>();
        for (var p : ownDistance.keySet()) {
            int foreign = world.apply(p).persistent() ? 0 : Integer.MAX_VALUE;
            for (var delta : FACES) {
                var n = p.offset(delta[0], delta[1], delta[2]);
                var cell = world.apply(n);
                if (cell.kind() == Voxel.Kind.UNKNOWN) foreign = 0;
                else if (cell.kind() == Voxel.Kind.LOG && !logs.contains(n)) foreign = Math.min(foreign, 1);
            }
            if (foreign != Integer.MAX_VALUE) {
                foreignDistance.put(p, foreign);
                queue.addLast(p);
            }
        }
        while (!queue.isEmpty()) {
            var p = queue.removeFirst();
            int distance = foreignDistance.get(p);
            if (distance >= LEAF_REACH) continue;
            for (var delta : FACES) {
                var n = p.offset(delta[0], delta[1], delta[2]);
                if (ownDistance.containsKey(n) && foreignDistance.getOrDefault(n, Integer.MAX_VALUE) > distance + 1) {
                    foreignDistance.put(n, distance + 1);
                    queue.addLast(n);
                }
            }
        }
        var leaves = new ArrayList<GridPos>();
        for (var entry : ownDistance.entrySet()) {
            var p = entry.getKey();
            int distance = entry.getValue();
            var cell = world.apply(p);
            if (cell.persistent() || !cell.family().equals(family) || distance > LEAF_REACH) continue;
            // The game's stored distance also guards support paths outside our family graph.
            if (distance > cell.leafDistance() || distance >= foreignDistance.getOrDefault(p, Integer.MAX_VALUE)) continue;
            leaves.add(p);
            if (leaves.size() > limits.maxLeaves()) return Result.reject(Reason.SEARCH_LIMIT);
        }
        if (leaves.size() < 4) return Result.reject(Reason.NO_NATURAL_LEAVES);
        leaves.sort(Comparator.naturalOrder());
        return new Result(Optional.of(new TreePlan(family, origin, logs.stream().sorted().toList(), leaves, roots)), Reason.OK);
    }
    public static boolean validRoots(List<GridPos> roots) {
        if (roots.size() == 1) return true;
        if (roots.size() != 4 || Set.copyOf(roots).size() != 4) return false;
        int y = roots.getFirst().y();
        int x = roots.stream().mapToInt(GridPos::x).min().orElseThrow();
        int z = roots.stream().mapToInt(GridPos::z).min().orElseThrow();
        return Set.copyOf(roots).equals(Set.of(new GridPos(x,y,z), new GridPos(x+1,y,z),
                new GridPos(x,y,z+1), new GridPos(x+1,y,z+1)));
    }
}
