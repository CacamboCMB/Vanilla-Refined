package de.ownmods.inventorysort;
import de.ownmods.inventorysort.core.*;
import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import org.slf4j.LoggerFactory;

/** Only ordinary storage menus. No machine inputs/outputs, armor or crafting result slots. */
public final class InventorySorter {
    private InventorySorter() { }
    public static boolean storage(AbstractContainerMenu menu) {
        var c=menu.getClass();
        return c==ChestMenu.class || c==ShulkerBoxMenu.class || c==HopperMenu.class || c==DispenserMenu.class;
    }
    public static boolean supported(AbstractContainerMenu menu) {return menu.getClass()==InventoryMenu.class || storage(menu);}
    public static List<Slot> selected(AbstractContainerMenu menu,Inventory inventory,boolean container) {
        if(!supported(menu) || (container && !storage(menu)))return List.of();
        // Storage menus put their own slots first, then 27 main inventory + 9 hotbar slots.
        // InventoryMenu layout: result[0], craft[1..4], armor[5..8], main[9..35], hotbar[36..44], offhand[45].
        InventorySections.Range range;
        try {range=InventorySections.select(menu.getClass()==InventoryMenu.class,menu.slots.size(),container);}
        catch(IllegalArgumentException e){return List.of();}
        int from=range.start(),to=range.end();
        var result=new ArrayList<Slot>();
        for(int i=from;i<to;i++) {
            Slot slot=menu.slots.get(i);
            if(container == (slot.container==inventory))return List.of();
            result.add(slot);
        }
        return List.copyOf(result);
    }
    private static final StackSorter.Ops<ItemStack> OPS=new StackSorter.Ops<>() {
        public boolean empty(ItemStack s){return s.isEmpty();}
        public int count(ItemStack s){return s.getCount();}
        public int maximum(ItemStack s){return s.getMaxStackSize();}
        public boolean sameKind(ItemStack a,ItemStack b){return ItemStack.isSameItemSameComponents(a,b);}
        public String orderKey(ItemStack s){return BuiltInRegistries.ITEM.getKey(s.getItem())+"\u0000"+s.getHoverName().getString();}
        public ItemStack withCount(ItemStack s,int count){var copy=s.copy();copy.setCount(count);return copy;}
        public ItemStack emptyValue(){return ItemStack.EMPTY;}
    };
    public static void sort(ServerPlayer p,int menuId,int stateId,boolean container,InventorySortSettings settings) {
        var state = de.ownmods.settings.server.ServerSettings.effective(p, settings);
        var rules = new InventorySortSettings.Rules(state.flag("enabled"), state.flag("merge"), state.flag("containers"),
                de.ownmods.inventorysort.core.SortMode.parse(state.choice("sort_mode", "family")));
        if(!rules.enabled() || p.isRemoved() || p.isSpectator() || !p.isAlive())return;
        var menu=p.containerMenu;
        if(menu.containerId!=menuId || menu.getStateId()!=stateId || !menu.stillValid(p) || !menu.getCarried().isEmpty() || (container && !rules.containers()))return;
        var slots=selected(menu,p.getInventory(),container);
        if(slots.isEmpty())return;
        try {
            var before=slots.stream().map(s->s.getItem().copy()).toList();
            // Snapshot belongs to the validated server menu, never to client-supplied item data.
            var after=StackSorter.sort(before,rules.merge(),OPS,new ItemSortKeys(rules.mode())::key);
            var result=SlotTransaction.commit(before,after,new SlotTransaction.Access<ItemStack>() {
                public ItemStack get(int i){return slots.get(i).getItem();}
                public boolean equal(ItemStack a,ItemStack b){return ItemStack.matches(a,b);}
                public boolean maySet(int i,ItemStack value) {
                    var slot=slots.get(i);
                    return slot.isActive() && slot.mayPickup(p) && (value.isEmpty() ||
                        (slot.mayPlace(value) && value.getCount()<=slot.getMaxStackSize(value)));
                }
                public void set(int i,ItemStack value){slots.get(i).set(value.copy());}
            });
            p.getInventory().setChanged();menu.broadcastChanges();
            p.sendOverlayMessage(Component.translatable(result==SlotTransaction.Result.APPLIED?"ownmods.inventorysort.done":"ownmods.inventorysort.stale"));
        } catch(RuntimeException e) {
            LoggerFactory.getLogger("ownmods_inventorysort").error("Sort transaction failed",e);
            menu.broadcastChanges();p.sendOverlayMessage(Component.translatable("ownmods.inventorysort.failed"));
        }
    }
}
