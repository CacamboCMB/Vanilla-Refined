package de.ownmods.toolswap;

import de.ownmods.toolswap.core.ToolPolicy;
import java.util.*;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class ToolSwapEngine {
    private enum Kind { PICKAXE, AXE, SHOVEL, HOE }
    private record Snapshot(Kind kind,int quality,int selectedSlot) { }
    private record Candidate(int slot,ItemStack stack,int quality,int durability) { }
    private final ToolSwapSettings settings;
    private final Map<UUID,Snapshot> beforeBreak=new HashMap<>();
    public ToolSwapEngine(ToolSwapSettings settings){this.settings=settings;}
    private record Pending(ServerPlayer player, Snapshot snapshot) { }
    private final List<Pending> ready = new ArrayList<>();
    private de.ownmods.settings.policy.PreferenceSnapshot rules(ServerPlayer player) {
        return de.ownmods.settings.server.ServerSettings.effective(player, settings);
    }
    public void register(){
        AttackBlockCallback.EVENT.register((player,level,hand,pos,direction)->{
            if (!(player instanceof ServerPlayer serverPlayer) || hand != InteractionHand.MAIN_HAND
                    || player.isSpectator() || !de.ownmods.settings.server.WorldMutationGuard.allowed(serverPlayer, serverPlayer.level(), pos)) return InteractionResult.PASS;
            var flags = rules(serverPlayer);
            if (flags.flag("enabled") && flags.flag("auto_tool")) autoSelect(serverPlayer, level.getBlockState(pos));
            return InteractionResult.PASS;
        });
        PlayerBlockBreakEvents.BEFORE.register((level,player,pos,state,entity)->{
            if (!(player instanceof ServerPlayer serverPlayer)) return true;
            beforeBreak.remove(player.getUUID());
            var flags = rules(serverPlayer);
            if (!flags.flag("enabled") || player.isSpectator()) return true;
            if (flags.flag("auto_tool")) autoSelect(serverPlayer, state);
            ItemStack held = player.getInventory().getSelectedItem(); Kind kind = kind(held);
            if (kind != null) beforeBreak.put(player.getUUID(), new Snapshot(kind, quality(held), player.getInventory().getSelectedSlot()));
            return true;
        });
        PlayerBlockBreakEvents.CANCELED.register((level,player,pos,state,entity)->{
            if (player instanceof ServerPlayer) beforeBreak.remove(player.getUUID());
        });
        PlayerBlockBreakEvents.AFTER.register((level,player,pos,state,entity)->{
            if (!(player instanceof ServerPlayer)) return;
            Snapshot snapshot = beforeBreak.remove(player.getUUID());
            if (snapshot != null && player instanceof ServerPlayer serverPlayer && ready.size() < 1024)
                ready.add(new Pending(serverPlayer, snapshot));
        });
        // AFTER may precede vanilla durability bookkeeping; replace only after that tick finishes.
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(server -> {
            var jobs = List.copyOf(ready); ready.clear(); beforeBreak.clear();
            for (var job : jobs) {
                var player = job.player(); var snapshot = job.snapshot(); var flags = rules(player);
                if (player.level().getServer() != server || player.isRemoved() || !player.isAlive()
                        || player.isSpectator() || !flags.flag("enabled")) continue;
                Inventory inv = player.getInventory();
                if (inv.getSelectedSlot() != snapshot.selectedSlot() || !inv.getSelectedItem().isEmpty()) continue;
                Candidate replacement = select(inv, snapshot.kind(), snapshot.quality(),
                        ToolPolicy.Mode.parse(flags.choice("policy", "best")), flags.flag("allow_enchanted"), null);
                if (replacement != null) { moveIntoSelected(inv, replacement.slot()); sync(player); }
            }
        });
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            beforeBreak.clear(); ready.clear();
        });
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            beforeBreak.remove(handler.player.getUUID());
            ready.removeIf(job -> job.player().getUUID().equals(handler.player.getUUID()));
        });
    }
    private void autoSelect(ServerPlayer player,BlockState state){
        var flags = rules(player);
        Kind wanted=kind(state);if(wanted==null)return;
        Inventory inv=player.getInventory();ItemStack current=inv.getSelectedItem();
        if(kind(current)==wanted)return;
        int reference=quality(current);
        Candidate candidate=select(inv,wanted,reference,ToolPolicy.Mode.parse(flags.choice("policy", "best")),flags.flag("allow_enchanted"),state);
        if(candidate==null)return;
        int selected=inv.getSelectedSlot();ItemStack old=inv.getItem(selected);ItemStack tool=inv.getItem(candidate.slot());
        inv.setItem(selected,tool);inv.setItem(candidate.slot(),old);sync(player);
    }
    private static Candidate select(Inventory inv,Kind wanted,int reference,ToolPolicy.Mode mode,boolean allowEnchanted,BlockState state){
        Candidate best=null;
        int size=inv.getNonEquipmentItems().size();int selected=inv.getSelectedSlot();
        for(int slot=0;slot<size;slot++){
            if(slot==selected)continue;ItemStack stack=inv.getItem(slot);if(stack.isEmpty()||kind(stack)!=wanted)continue;
            if(!allowEnchanted&&stack.isEnchanted())continue;
            int q=quality(stack);if(!ToolPolicy.qualityAllowed(q,reference,mode))continue;
            if(state!=null&&state.requiresCorrectToolForDrops()&&!stack.isCorrectToolForDrops(state))continue;
            int durability=stack.isDamageableItem()?stack.getMaxDamage()-stack.getDamageValue():Integer.MAX_VALUE/4;
            var c=new Candidate(slot,stack,q,durability);
            if(best==null||compare(c,best,mode)>0)best=c;
        }
        return best;
    }
    private static int compare(Candidate a,Candidate b,ToolPolicy.Mode mode){
        // SAME/SAME_OR_WORSE prefer the highest allowed quality, then remaining durability.
        int qa=Integer.compare(a.quality(),b.quality());if(qa!=0)return qa;
        return Integer.compare(a.durability(),b.durability());
    }
    private static void moveIntoSelected(Inventory inv,int source){
        int selected=inv.getSelectedSlot();ItemStack stack=inv.getItem(source);inv.setItem(selected,stack);inv.setItem(source,ItemStack.EMPTY);
    }
    private static void sync(Player player){if(player instanceof ServerPlayer sp)sp.containerMenu.broadcastChanges();}
    private static Kind kind(BlockState state){
        if(state.is(BlockTags.MINEABLE_WITH_PICKAXE))return Kind.PICKAXE;
        if(state.is(BlockTags.MINEABLE_WITH_AXE))return Kind.AXE;
        if(state.is(BlockTags.MINEABLE_WITH_SHOVEL))return Kind.SHOVEL;
        if(state.is(BlockTags.MINEABLE_WITH_HOE))return Kind.HOE;
        return null;
    }
    private static Kind kind(ItemStack stack){
        if(stack.isEmpty())return null;
        if(stack.is(ItemTags.PICKAXES))return Kind.PICKAXE;
        if(stack.is(ItemTags.AXES))return Kind.AXE;
        if(stack.is(ItemTags.SHOVELS))return Kind.SHOVEL;
        if(stack.is(ItemTags.HOES))return Kind.HOE;
        return null;
    }
    private static int quality(ItemStack stack){
        if(stack==null||stack.isEmpty())return -1;
        String p=BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        if(p.startsWith("netherite_"))return 6;
        if(p.startsWith("diamond_"))return 5;
        if(p.startsWith("iron_"))return 4;
        if(p.startsWith("copper_"))return 3;
        if(p.startsWith("stone_"))return 2;
        if(p.startsWith("golden_"))return 1;
        if(p.startsWith("wooden_"))return 0;
        return -1;
    }
}
