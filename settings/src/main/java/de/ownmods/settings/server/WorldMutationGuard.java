package de.ownmods.settings.server;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** Native protection checks plus an opt-in veto hook for claim/region integrations. */
public final class WorldMutationGuard {
    @FunctionalInterface public interface Veto { boolean allow(ServerPlayer player, ServerLevel level, BlockPos position); }
    private static final List<Veto> VETOS = new CopyOnWriteArrayList<>();
    private WorldMutationGuard() { }
    public static void register(Veto veto) { VETOS.add(java.util.Objects.requireNonNull(veto)); }
    public static boolean allowed(ServerPlayer player, ServerLevel level, BlockPos position) {
        if (player.level() != level || !level.hasChunkAt(position) || !level.getWorldBorder().isWithinBounds(position)
                || !level.mayInteract(player, position) || position.getY() < level.getMinY() || position.getY() > level.getMaxY()) return false;
        for (var veto : VETOS) if (!veto.allow(player, level, position)) return false;
        return true;
    }
}
