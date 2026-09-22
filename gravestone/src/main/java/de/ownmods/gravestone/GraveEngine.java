package de.ownmods.gravestone;

import de.ownmods.gravestone.core.GraveResourcePlanner;
import de.ownmods.gravestone.compat.PlayerDropCompat;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.material.Fluids;

public final class GraveEngine {
    private GraveEngine() { }
    public static boolean tryCreate(ServerPlayer player, ServerLevel level, GravestoneSettings settings) {
        if(settings==null || level.getGameRules().get(GameRules.KEEP_INVENTORY)) return false;
        var effective = de.ownmods.settings.server.ServerSettings.effective(player, settings);
        if (!effective.flag("enabled")) return false;
        Inventory inv=player.getInventory();
        var stock=countResources(inv);
        var plan=GraveResourcePlanner.plan(stock,effective.flag("single_fallback"));
        if(!plan.createsGrave()) return false;
        Placement placement=plan.size()==GraveResourcePlanner.Size.DOUBLE?findDouble(player,level,player.blockPosition()):findSingle(player,level,player.blockPosition());
        if(placement==null) return false;
        BlockPos deathPos = player.blockPosition();
        if(!place(level,placement)) return false;
        Container container=container(level,placement);
        if(container==null){
            rollbackPlacement(level,placement);
            return false;
        }
        List<ItemStack> before = snapshot(inv);
        List<ItemStack> overflow = new ArrayList<>();
        try {
            consume(inv, Items.CHEST, plan.chests());
            consume(inv, ItemTags.PLANKS, plan.planks());
            consume(inv, ItemTags.LOGS, plan.logs());
            destroyVanishing(inv);
            List<ItemStack> drops=drain(inv);
            int stored=0;
            for(ItemStack stack:drops){
                if(stored<container.getContainerSize()) container.setItem(stored++,stack);
                else overflow.add(stack);
            }
            container.setChanged();
        } catch(RuntimeException ex) {
            GravestoneMod.LOGGER.error("Grave transaction failed; restoring vanilla death flow",ex);
            clearContainer(container);
            restore(inv,before);
            rollbackPlacement(level,placement);
            return false;
        }
        // Only after chest storage is fully committed are overflow stacks spawned.
        for(ItemStack stack:overflow) PlayerDropCompat.dropOverflow(player,stack);
        if(effective.flag("chat_message")) {
            try { sendMessage(player,level,deathPos,placement.first(),plan.size(),overflow.size()); }
            catch(RuntimeException ex){ GravestoneMod.LOGGER.warn("Could not send grave coordinates",ex); }
        }
        player.containerMenu.broadcastChanges();
        return true;
    }
    private static GraveResourcePlanner.Stock countResources(Inventory inv){
        int chests=0,planks=0,logs=0;
        for(int i=0;i<inv.getContainerSize();i++){
            ItemStack s=inv.getItem(i); if(s.isEmpty())continue;
            if(s.is(Items.CHEST))chests+=s.getCount();
            if(s.is(ItemTags.PLANKS))planks+=s.getCount();
            if(s.is(ItemTags.LOGS))logs+=s.getCount();
        }
        return new GraveResourcePlanner.Stock(chests,planks,logs);
    }
    private static void consume(Inventory inv, net.minecraft.world.item.Item item, int count){
        if(count<=0)return; int left=count;
        for(int i=0;i<inv.getContainerSize()&&left>0;i++){
            ItemStack s=inv.getItem(i); if(s.isEmpty()||!s.is(item))continue;
            int take=Math.min(left,s.getCount()); s.shrink(take); left-=take;
            if(s.isEmpty())inv.setItem(i,ItemStack.EMPTY);
        }
        if(left!=0)throw new IllegalStateException("grave chest resources changed during death");
    }
    private static void consume(Inventory inv, net.minecraft.tags.TagKey<net.minecraft.world.item.Item> tag, int count){
        if(count<=0)return; int left=count;
        for(int i=0;i<inv.getContainerSize()&&left>0;i++){
            ItemStack s=inv.getItem(i); if(s.isEmpty()||!s.is(tag))continue;
            int take=Math.min(left,s.getCount()); s.shrink(take); left-=take;
            if(s.isEmpty())inv.setItem(i,ItemStack.EMPTY);
        }
        if(left!=0)throw new IllegalStateException("grave wood resources changed during death");
    }
    private static List<ItemStack> snapshot(Inventory inv){
        var copy=new ArrayList<ItemStack>(inv.getContainerSize());
        for(int i=0;i<inv.getContainerSize();i++) copy.add(inv.getItem(i).copy());
        return copy;
    }
    private static void restore(Inventory inv,List<ItemStack> snapshot){
        for(int i=0;i<snapshot.size();i++) inv.setItem(i,snapshot.get(i).copy());
    }
    private static void clearContainer(Container container){
        for(int i=0;i<container.getContainerSize();i++) container.setItem(i,ItemStack.EMPTY);
        container.setChanged();
    }
    private static void destroyVanishing(Inventory inv){
        for(int i=0;i<inv.getContainerSize();i++){
            ItemStack s=inv.getItem(i);
            if(!s.isEmpty()&&EnchantmentHelper.has(s,EnchantmentEffectComponents.PREVENT_EQUIPMENT_DROP)) inv.setItem(i,ItemStack.EMPTY);
        }
    }
    private static List<ItemStack> drain(Inventory inv){
        var result=new ArrayList<ItemStack>();
        for(int i=0;i<inv.getContainerSize();i++){
            ItemStack s=inv.getItem(i);
            if(!s.isEmpty()){result.add(s.copy());inv.setItem(i,ItemStack.EMPTY);}
        }
        return result;
    }
    private record Placement(BlockPos first, BlockPos second, Direction facing, ChestType firstType, ChestType secondType, BlockState oldFirst, BlockState oldSecond){boolean doubleChest(){return second!=null;}}
    private static Placement findSingle(ServerPlayer player,ServerLevel level,BlockPos origin){
        for(BlockPos p:candidates(origin))if(replaceable(player,level,p))return new Placement(p,null,Direction.NORTH,ChestType.SINGLE,ChestType.SINGLE,level.getBlockState(p),null);
        return null;
    }
    private static Placement findDouble(ServerPlayer player,ServerLevel level,BlockPos origin){
        for(BlockPos p:candidates(origin)){
            if(!replaceable(player,level,p))continue;
            BlockPos east=p.east(); if(replaceable(player,level,east))return new Placement(p,east,Direction.NORTH,ChestType.LEFT,ChestType.RIGHT,level.getBlockState(p),level.getBlockState(east));
            BlockPos west=p.west(); if(replaceable(player,level,west))return new Placement(p,west,Direction.NORTH,ChestType.RIGHT,ChestType.LEFT,level.getBlockState(p),level.getBlockState(west));
            BlockPos south=p.south(); if(replaceable(player,level,south))return new Placement(p,south,Direction.EAST,ChestType.LEFT,ChestType.RIGHT,level.getBlockState(p),level.getBlockState(south));
            BlockPos north=p.north(); if(replaceable(player,level,north))return new Placement(p,north,Direction.EAST,ChestType.RIGHT,ChestType.LEFT,level.getBlockState(p),level.getBlockState(north));
        }
        return null;
    }
    private static List<BlockPos> candidates(BlockPos o){
        var out=new ArrayList<BlockPos>(); int[] ys={0,1,-1,2,-2,3,-3};
        for(int dy:ys)for(int r=0;r<=4;r++)for(int dx=-r;dx<=r;dx++)for(int dz=-r;dz<=r;dz++)if(Math.max(Math.abs(dx),Math.abs(dz))==r)out.add(o.offset(dx,dy,dz));
        return out;
    }
    private static boolean replaceable(ServerPlayer player,ServerLevel level,BlockPos p){
        if (!de.ownmods.settings.server.WorldMutationGuard.allowed(player, level, p)) return false;
        // Never merge a new grave into somebody else's existing chest.
        for (var direction : new Direction[]{Direction.NORTH,Direction.SOUTH,Direction.WEST,Direction.EAST}) {
            BlockPos neighbour = p.relative(direction);
            if (!level.hasChunkAt(neighbour) || level.getBlockState(neighbour).getBlock() instanceof ChestBlock) return false;
        }
        if(!level.getBlockState(p).canBeReplaced())return false;
        var fluid=level.getFluidState(p);return fluid.isEmpty()||fluid.is(Fluids.WATER);
    }
    private static boolean place(ServerLevel level,Placement p){
        boolean water1=level.getFluidState(p.first()).is(Fluids.WATER);
        BlockState s1=Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING,p.facing()).setValue(ChestBlock.TYPE,p.firstType()).setValue(ChestBlock.WATERLOGGED,water1);
        if(!level.setBlock(p.first(),s1,3))return false;
        if(p.doubleChest()){
            boolean water2=level.getFluidState(p.second()).is(Fluids.WATER);
            BlockState s2=Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING,p.facing()).setValue(ChestBlock.TYPE,p.secondType()).setValue(ChestBlock.WATERLOGGED,water2);
            if(!level.setBlock(p.second(),s2,3)){level.setBlock(p.first(),p.oldFirst(),3);return false;}
        }
        return true;
    }
    private static void rollbackPlacement(ServerLevel level,Placement p){
        level.setBlock(p.first(),p.oldFirst(),3);
        if(p.doubleChest()) level.setBlock(p.second(),p.oldSecond(),3);
    }
    private static Container container(ServerLevel level,Placement p){
        if(!(level.getBlockEntity(p.first()) instanceof ChestBlockEntity a))return null;
        if(!p.doubleChest())return a;
        if(!(level.getBlockEntity(p.second()) instanceof ChestBlockEntity b))return null;
        return new CompoundContainer(a,b);
    }
    private static void sendMessage(ServerPlayer player,ServerLevel level,BlockPos deathPos,BlockPos chestPos,GraveResourcePlanner.Size size,int overflow){
        String dimension=level.dimension().identifier().toString();
        Component base=Component.translatable(
            size==GraveResourcePlanner.Size.DOUBLE?"ownmods.gravestone.created.double":"ownmods.gravestone.created.single",
            dimension, deathPos.getX(), deathPos.getY(), deathPos.getZ(), chestPos.getX(), chestPos.getY(), chestPos.getZ());
        boolean understands = net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.canSend(player,
                de.ownmods.settings.network.ServerStatePayload.TYPE);
        if (!understands) base = Component.literal(String.format(Locale.ROOT,
                "Gravestone (%s): death %d, %d, %d; chest %d, %d, %d.", dimension,
                deathPos.getX(), deathPos.getY(), deathPos.getZ(), chestPos.getX(), chestPos.getY(), chestPos.getZ()));
        player.sendSystemMessage(base);
        if(overflow>0)player.sendSystemMessage(understands
                ? Component.translatable("ownmods.gravestone.overflow",overflow)
                : Component.literal("Gravestone overflow: " + overflow + " item stacks dropped nearby."));
    }
}
