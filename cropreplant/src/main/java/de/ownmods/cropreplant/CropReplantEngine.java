package de.ownmods.cropreplant;

import de.ownmods.cropreplant.core.ReplantTransaction;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** Normal vanilla breaking/loot first. Replant only when AFTER confirms the break. */
final class CropReplantEngine {
    private final CropReplantSettings settings;
    private final Map<UUID,Job> pending=new HashMap<>();
    private final List<Job> ready=new ArrayList<>();
    private static final int MAX_JOBS=128;
    CropReplantEngine(CropReplantSettings settings){this.settings=settings;}
    private record Crop(Block block,Item seed,String option) {
        boolean mature(BlockState state) {
            if(!state.is(block))return false;
            if(block instanceof CropBlock crop)return crop.isMaxAge(state);
            return block==Blocks.NETHER_WART && state.getValue(NetherWartBlock.AGE)==3;
        }
        BlockState young() {
            return block instanceof CropBlock crop?crop.getStateForAge(0):block.defaultBlockState();
        }
    }
    private static final List<Crop> CROPS=List.of(
        new Crop(Blocks.WHEAT,Items.WHEAT_SEEDS,"wheat"),
        new Crop(Blocks.CARROTS,Items.CARROT,"carrots"),
        new Crop(Blocks.POTATOES,Items.POTATO,"potatoes"),
        new Crop(Blocks.BEETROOTS,Items.BEETROOT_SEEDS,"beetroots"),
        new Crop(Blocks.NETHER_WART,Items.NETHER_WART,"nether_wart"));
    private record Job(ServerLevel level,ServerPlayer player,BlockPos pos,Crop crop,Set<UUID> oldDrops) { }
    private Crop crop(BlockState state, ServerPlayer player) {
        for(var c:CROPS)if(de.ownmods.settings.server.ServerSettings.effective(player, settings).flag(c.option()) && c.mature(state))return c;
        return null;
    }
    private boolean allowed(ServerLevel level,ServerPlayer p) {
        if(!de.ownmods.settings.server.ServerSettings.effective(p, settings).flag("enabled") || p.isRemoved() || !p.isAlive() || p.level()!=level)return false;
        var mode=p.gameMode.getGameModeForPlayer();
        return (mode==GameType.SURVIVAL || mode==GameType.CREATIVE)
            && !(de.ownmods.settings.server.ServerSettings.effective(p, settings).flag("sneak_bypass") && p.isShiftKeyDown());
    }
    void register() {
        PlayerBlockBreakEvents.BEFORE.register((world,player,pos,state,entity)->{
            if (!(world instanceof ServerLevel) || !(player instanceof ServerPlayer)) return true;
            pending.remove(player.getUUID());
            if(world instanceof ServerLevel level && player instanceof ServerPlayer p && allowed(level,p)) {
                Crop crop=crop(state,p);
                if(crop!=null && ready.size()<MAX_JOBS) {
                    Set<UUID> previous=new HashSet<>();
                    for(var item:drops(level,pos,crop.seed()))previous.add(item.getUUID());
                    pending.put(p.getUUID(),new Job(level,p,pos.immutable(),crop,Set.copyOf(previous)));
                }
            }
            return true;
        });
        PlayerBlockBreakEvents.CANCELED.register((world,player,pos,state,entity)->{
            if (world instanceof ServerLevel && player instanceof ServerPlayer) pending.remove(player.getUUID());
        });
        PlayerBlockBreakEvents.AFTER.register((world,player,pos,state,entity)->{
            if (!(world instanceof ServerLevel) || !(player instanceof ServerPlayer)) return;
            Job job=pending.remove(player.getUUID());
            if(job!=null && job.level()==world && job.pos().equals(pos) && ready.size()<MAX_JOBS)ready.add(job);
        });
        UseBlockCallback.EVENT.register((player,world,hand,hit)->{
            if(hand!=InteractionHand.MAIN_HAND)return InteractionResult.PASS;
            if(!(world instanceof ServerLevel level) || !(player instanceof ServerPlayer p) || !allowed(level,p))return InteractionResult.PASS;
            if(!de.ownmods.settings.server.ServerSettings.effective(p, settings).flag("right_click")
                    || !p.canInteractWithBlock(hit.getBlockPos(), 0.0)
                    || !de.ownmods.settings.server.WorldMutationGuard.allowed(p, level, hit.getBlockPos())
                    || crop(level.getBlockState(hit.getBlockPos()),p)==null)return InteractionResult.PASS;
            // Server game-mode path keeps vanilla loot, durability, permissions and break callbacks.
            return p.gameMode.destroyBlock(hit.getBlockPos())?InteractionResult.SUCCESS:InteractionResult.PASS;
        });
        ServerTickEvents.END_SERVER_TICK.register(server->{
            var jobs=List.copyOf(ready);ready.clear();pending.clear();
            for(var job:jobs)if(job.level().getServer()==server) {
                try { plant(job); }
                catch(RuntimeException e) { CropReplantMod.LOGGER.error("Replant failed at {}",job.pos(),e); }
            }
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server->{ready.clear();pending.clear();});
    }
    private static List<ItemEntity> drops(ServerLevel level,BlockPos pos,Item seed) {
        return level.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(0.8),
            entity->!entity.isRemoved() && entity.getItem().is(seed));
    }
    private void plant(Job job) {
        var level=job.level();var p=job.player();var pos=job.pos();var young=job.crop().young();
        if(!allowed(level,p) || !de.ownmods.settings.server.ServerSettings.effective(p, settings).flag(job.crop().option()))return;
        var fresh=drops(level,pos,job.crop().seed()).stream().filter(e->!job.oldDrops().contains(e.getUUID())).toList();
        boolean creative=p.gameMode.getGameModeForPlayer()==GameType.CREATIVE;
        var result=ReplantTransaction.run(creative,new ReplantTransaction.Access() {
            @Override public boolean emptyAndLoaded(){return de.ownmods.settings.server.WorldMutationGuard.allowed(p, level, pos) && level.hasChunkAt(pos.below()) && level.getBlockState(pos).isAir();}
            @Override public boolean suitableSoil(){return young.canSurvive(level,pos);}
            @Override public boolean hasSeed(){return findSeed(p,fresh,job.crop().seed())!=null;}
            @Override public boolean plant(){return level.setBlock(pos,young,Block.UPDATE_ALL) && level.getBlockState(pos).equals(young);}
            @Override public boolean consumeSeed(){
                Runnable consume=findSeed(p,fresh,job.crop().seed());
                if(consume==null)return false;consume.run();p.getInventory().setChanged();return true;
            }
            @Override public boolean rollback(){
                if(level.getBlockState(pos).equals(young))return level.setBlock(pos,Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);
                return level.getBlockState(pos).isAir();
            }
        });
        if(result!=ReplantTransaction.Result.PLANTED) {
            p.sendOverlayMessage(Component.translatable("ownmods.cropreplant.result."+result.name().toLowerCase(Locale.ROOT)));
            if(result==ReplantTransaction.Result.FAILED)CropReplantMod.LOGGER.warn("Replant transaction failed at {}",pos);
        }
    }
    /** Build a fresh one-item debit, no world callbacks between lookup and consumption. */
    private static Runnable findSeed(ServerPlayer p,List<ItemEntity> drops,Item seed) {
        for(var entity:drops)if(!entity.isRemoved() && entity.getItem().is(seed) && !entity.getItem().isEmpty()) {
            return ()->{var stack=entity.getItem().copy();stack.shrink(1);if(stack.isEmpty())entity.discard();else entity.setItem(stack);};
        }
        var inventory=p.getInventory();
        for(int i=0;i<inventory.getContainerSize();i++) {
            var stack=inventory.getItem(i);
            if(!stack.isEmpty() && stack.is(seed))return ()->stack.shrink(1);
        }
        return null;
    }
}
