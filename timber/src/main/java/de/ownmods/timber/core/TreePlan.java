package de.ownmods.timber.core;

import java.util.List;

public record TreePlan(String family, GridPos origin, List<GridPos> logs, List<GridPos> leaves, List<GridPos> roots) {
    public TreePlan {
        logs = List.copyOf(logs);
        leaves = List.copyOf(leaves);
        roots = List.copyOf(roots);
    }
}
