package de.ownmods.timber;

import de.ownmods.timber.core.GridPos;
import de.ownmods.timber.core.Replanting;
import net.minecraft.network.chat.Component;
import de.ownmods.timber.core.TreePlan;
import de.ownmods.timber.core.TreePlanner;
import de.ownmods.timber.core.Voxel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** All world access runs on the logical server thread; each player has an independent effective snapshot. */
final class TimberEngine {
    private static final int MAX_TREES_PER_TICK = 4;
    private final TimberSettings settings;
    private final TreePlanner planner = new TreePlanner();
    private final Map<UUID, Pending> pending = new HashMap<>();
    private final List<Pending> ready = new ArrayList<>();
    private final Set<UUID> busy = new HashSet<>();
    private record Pending(ServerLevel level, ServerPlayer player, TreeSpecies species, TreePlan plan, String axeId, TimberSettings.Flags flags) { }

    TimberEngine(TimberSettings settings) { this.settings = settings; }
    void register() {
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, entity) -> {
            if (!(world instanceof ServerLevel level) || !(player instanceof ServerPlayer serverPlayer)) return true;
            if (busy.contains(player.getUUID())) return true;
            pending.remove(player.getUUID());
            var flags = TimberSettings.flags(de.ownmods.settings.server.ServerSettings.effective(serverPlayer, settings).toggles());
            if (!mayTrigger(serverPlayer, flags) || ready.size() >= MAX_TREES_PER_TICK) return true;
            var species = TreeSpecies.forLog(state.getBlock());
            if (species == null) return true;
            var result = planner.analyze(p -> read(level, p), grid(pos));
            result.plan().ifPresent(plan -> pending.put(player.getUUID(),
                    new Pending(level, serverPlayer, species, plan, itemId(serverPlayer.getMainHandItem()), flags)));
            return true; // BEFORE is strictly read-only; vanilla is allowed to finish the original break.
        });
        PlayerBlockBreakEvents.CANCELED.register((world, player, pos, state, entity) -> {
            if (!(world instanceof ServerLevel) || !(player instanceof ServerPlayer)) return;
            if (!busy.contains(player.getUUID())) pending.remove(player.getUUID());
        });
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, entity) -> {
            if (!(world instanceof ServerLevel) || !(player instanceof ServerPlayer)) return;
            if (busy.contains(player.getUUID())) return;
            var job = pending.remove(player.getUUID());
            if (job != null && job.level() == world && job.plan().origin().equals(grid(pos)) && ready.size() < MAX_TREES_PER_TICK) ready.add(job);
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            // Fabric's AFTER hook occurs before all of vanilla's drop/tool bookkeeping is done.
            // The same-tick flush avoids re-entering destroyBlock on the original tool stack.
            var jobs = List.copyOf(ready);
            ready.clear();
            pending.clear();
            for (var job : jobs) {
                if (job.level().getServer() != server) continue;
                try { execute(job); }
                catch (RuntimeException e) { TimberMod.LOGGER.error("Timber operation aborted; the tree may be partially felled", e); }
            }
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            pending.clear(); ready.clear(); busy.clear();
        });
    }
    private static String itemId(ItemStack stack) { return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(); }
    private static boolean permittedAxe(ItemStack stack, TimberSettings.Flags flags) {
        return !stack.isEmpty() && stack.is(ItemTags.AXES) && flags.allowedAxes().contains(itemId(stack));
    }
    private static boolean mayTrigger(ServerPlayer player, TimberSettings.Flags flags) {
        if (!flags.enabled() || player.isRemoved()) return false;
        var mode = player.gameMode.getGameModeForPlayer();
        if (mode != GameType.SURVIVAL && mode != GameType.CREATIVE) return false;
        if (flags.sneakBypass() && player.isShiftKeyDown()) return false;
        return permittedAxe(player.getMainHandItem(), flags);
    }
    private void execute(Pending job) {
        var level = job.level();
        var player = job.player();
        var flags = TimberSettings.flags(de.ownmods.settings.server.ServerSettings.effective(player, settings).toggles());
        if (!flags.equals(job.flags())) return;
        var plan = job.plan();
        if (player.isRemoved() || player.level() != level) return;
        if (!mayTrigger(player, flags) || !itemId(player.getMainHandItem()).equals(job.axeId())) return;
        if (!level.hasChunkAt(block(plan.origin())) || !level.getBlockState(block(plan.origin())).isAir()) return;
        // Abort on changed/unloaded logs instead of cutting a different structure.
        for (var p : plan.logs()) {
            if (p.equals(plan.origin())) continue;
            if (!de.ownmods.settings.server.WorldMutationGuard.allowed(player, level, block(p))
                    || !level.getBlockState(block(p)).is(job.species().log())) return;
        }
        boolean creative = player.gameMode.getGameModeForPlayer() == GameType.CREATIVE;
        AABB area = area(plan);
        Set<UUID> previousSaplings = new HashSet<>();
        if (flags.replant() && flags.consumeSaplings() && !creative) {
            for (var entity : saplings(level, area, job.species().sapling().asItem())) previousSaplings.add(entity.getUUID());
        }
        busy.add(player.getUUID());
        try {
            for (var p : plan.logs()) {
                if (p.equals(plan.origin())) continue;
                // Vanilla applies normal per-log durability, enchantments and other break hooks.
                // Never continue with a broken, replaced or now-forbidden axe.
                if (!permittedAxe(player.getMainHandItem(), flags) || !itemId(player.getMainHandItem()).equals(job.axeId())) return;
                if (!de.ownmods.settings.server.WorldMutationGuard.allowed(player, level, block(p))
                    || !level.getBlockState(block(p)).is(job.species().log())) return;
                if (!player.gameMode.destroyBlock(block(p))) return;
            }
            if (flags.leaves()) {
                for (var p : plan.leaves()) {
                    BlockPos pos = block(p);
                    if (!de.ownmods.settings.server.WorldMutationGuard.allowed(player, level, pos)) continue;
                    BlockState state = level.getBlockState(pos);
                    if (!state.is(job.species().leaves()) || persistent(state)) continue;
                    if (!PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(level, player, pos, state, null)) {
                        PlayerBlockBreakEvents.CANCELED.invoker().onBlockBreakCanceled(level, player, pos, state, null);
                        continue;
                    }
                    // Natural leaf drops: game rules and fluid replacement stay vanilla.
                    // Leaves do not charge axe durability; no Fortune/Silk Touch leaf bonus.
                    if (level.destroyBlock(pos, !creative, player)) {
                        PlayerBlockBreakEvents.AFTER.invoker().afterBlockBreak(level, player, pos, state, null);
                    }
                }
            }
            if (flags.replant()) replant(job, area, previousSaplings, creative);
        } finally {
            busy.remove(player.getUUID());
        }
    }
    private static void replant(Pending job, AABB area, Set<UUID> previousSaplings, boolean creative) {
        var level = job.level();
        var player = job.player();
        var sapling = job.species().sapling();
        var state = sapling.defaultBlockState();
        boolean consume = job.flags().consumeSaplings() && !creative;
        // No dependency on random loot, leaf removal, nearby item entities or inventory in free mode.
        List<ItemEntity> dropped = consume ? saplings(level, area, sapling.asItem()).stream()
                .filter(e -> !previousSaplings.contains(e.getUUID())).toList() : List.of();
        var access = new Replanting.Access<BlockState>() {
            @Override public boolean loaded(GridPos p) {
                return de.ownmods.settings.server.WorldMutationGuard.allowed(player, level, block(p)) && level.hasChunkAt(block(p).below());
            }
            @Override public boolean empty(GridPos p) { return level.getBlockState(block(p)).isAir(); }
            @Override public boolean canSurvive(GridPos p) { return state.canSurvive(level, block(p)); }
            @Override public BlockState snapshot(GridPos p) { return level.getBlockState(block(p)); }
            @Override public boolean place(GridPos p) { return level.setBlock(block(p), state, Block.UPDATE_ALL); }
            @Override public boolean isPlanted(GridPos p) { return level.getBlockState(block(p)).is(sapling); }
            @Override public boolean restore(GridPos p, BlockState before) {
                var pos = block(p);
                var current = level.getBlockState(pos);
                if (current.equals(before)) return true;
                if (!current.is(sapling)) return false; // Do not overwrite another mod's block.
                return level.setBlock(pos, before, Block.UPDATE_ALL) && level.getBlockState(pos).equals(before);
            }
            @Override public int availableSaplings() { return available(player, dropped, sapling.asItem()); }
            @Override public boolean consumeSaplings(int amount) {
                return consume(player, dropped, sapling.asItem(), amount);
            }
        };
        var result = Replanting.plant(job.plan().roots(), consume, access);
        if (result == Replanting.Result.PLANTED) return;
        String suffix = switch (result) {
            case NO_SAPLINGS -> "no_saplings";
            case BLOCKED -> "blocked";
            case BAD_SOIL -> "bad_soil";
            case UNLOADED -> "unloaded";
            default -> "failed";
        };
        // Explain skipped planting in the action bar instead of silently returning.
        player.sendOverlayMessage(Component.translatable("ownmods.timber.replant." + suffix,
                job.plan().roots().size()));
        TimberMod.LOGGER.debug("Replant skipped at {}: {}", job.plan().roots(), result);
        if (suffix.equals("failed"))
            TimberMod.LOGGER.warn("Replant failed at {}: {}", job.plan().roots(), result);
    }
    private static int available(ServerPlayer player, List<ItemEntity> dropped, Item item) {
        int count = 0;
        for (var entity : dropped) {
            if (!entity.isRemoved() && entity.getItem().is(item)) count += entity.getItem().getCount();
        }
        var inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            var stack = inventory.getItem(slot);
            if (stack.is(item)) count += stack.getCount();
        }
        return count;
    }
    /** No event dispatch/world changes between the availability check and item consumption. */
    private static boolean consume(ServerPlayer player, List<ItemEntity> dropped, Item item, int required) {
        if (available(player, dropped, item) < required) return false;
        int remaining = required;
        for (var entity : dropped) {
            if (remaining == 0) break;
            if (entity.isRemoved() || !entity.getItem().is(item)) continue;
            var stack = entity.getItem().copy();
            int used = Math.min(remaining, stack.getCount());
            stack.shrink(used);
            remaining -= used;
            if (stack.isEmpty()) entity.discard(); else entity.setItem(stack);
        }
        var inventory = player.getInventory();
        for (int slot = 0; remaining > 0 && slot < inventory.getContainerSize(); slot++) {
            var stack = inventory.getItem(slot);
            if (!stack.is(item)) continue;
            int used = Math.min(remaining, stack.getCount());
            stack.shrink(used);
            remaining -= used;
        }
        inventory.setChanged();
        return true;
    }
    private static List<ItemEntity> saplings(ServerLevel level, AABB area, Item item) {
        return level.getEntitiesOfClass(ItemEntity.class, area, entity -> !entity.isRemoved() && entity.getItem().is(item));
    }
    private static AABB area(TreePlan plan) {
        var positions = new ArrayList<>(plan.logs());
        positions.addAll(plan.leaves());
        int minX = positions.stream().mapToInt(GridPos::x).min().orElseThrow();
        int maxX = positions.stream().mapToInt(GridPos::x).max().orElseThrow();
        int minY = positions.stream().mapToInt(GridPos::y).min().orElseThrow();
        int maxY = positions.stream().mapToInt(GridPos::y).max().orElseThrow();
        int minZ = positions.stream().mapToInt(GridPos::z).min().orElseThrow();
        int maxZ = positions.stream().mapToInt(GridPos::z).max().orElseThrow();
        return new AABB(minX-1, minY-1, minZ-1, maxX+2, maxY+2, maxZ+2);
    }
    private static boolean persistent(BlockState state) {
        return state.hasProperty(LeavesBlock.PERSISTENT) && state.getValue(LeavesBlock.PERSISTENT);
    }
    private static Voxel read(ServerLevel level, GridPos p) {
        BlockPos pos = block(p);
        if (!level.hasChunkAt(pos)) return Voxel.UNKNOWN;
        var state = level.getBlockState(pos);
        var species = TreeSpecies.forLog(state.getBlock());
        if (species != null) return Voxel.log(species.name());
        if (state.is(BlockTags.LOGS)) return Voxel.log(""); // foreign/stripped log can support a neighbouring crown
        if (state.is(BlockTags.LEAVES)) {
            int distance = state.hasProperty(LeavesBlock.DISTANCE) ? state.getValue(LeavesBlock.DISTANCE) : 7;
            return Voxel.leaf(TreeSpecies.leafFamily(state.getBlock()), persistent(state), distance);
        }
        if (state.is(BlockTags.DIRT) || state.is(Blocks.MOSS_BLOCK) || state.is(Blocks.PALE_MOSS_BLOCK)
                || state.is(Blocks.MANGROVE_ROOTS) || state.is(Blocks.MUDDY_MANGROVE_ROOTS)) return Voxel.SOIL;
        return state.isAir() ? Voxel.AIR : Voxel.OTHER;
    }
    private static GridPos grid(BlockPos pos) { return new GridPos(pos.getX(), pos.getY(), pos.getZ()); }
    private static BlockPos block(GridPos pos) { return new BlockPos(pos.x(), pos.y(), pos.z()); }
}
