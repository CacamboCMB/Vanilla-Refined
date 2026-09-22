package de.ownmods.inventorysort.client;
import de.ownmods.settings.client.compat.InputCompat;
import com.mojang.blaze3d.platform.InputConstants;
import de.ownmods.inventorysort.*;
import de.ownmods.inventorysort.core.SortButtonLayout;
import de.ownmods.inventorysort.client.mixin.ContainerScreenAccess;
import de.ownmods.settings.api.ManagedMods;
import java.util.concurrent.atomic.AtomicBoolean;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.screen.v1.*;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class InventorySortClient implements ClientModInitializer {
    private InventorySortSettings settings;
    private KeyMapping sortKey;
    @Override public void onInitializeClient() {
        settings=InventorySortMod.settings();
        var category=KeyMapping.Category.register(Identifier.fromNamespaceAndPath("ownmods_inventorysort","keys"));
        sortKey=KeyMappingHelper.registerKeyMapping(new KeyMapping("key.ownmods_inventorysort.sort",InputCompat.keyboardType(InputConstants.Type.class),InputCompat.code("KEY_R"),category));
        ScreenEvents.AFTER_INIT.register((client,screen,width,height)->{
            if(!(screen instanceof AbstractContainerScreen<?> container) || !InventorySorter.supported(container.getMenu()))return;
            ScreenKeyboardEvents.allowKeyPress(screen).register((s,key)->{
                if(!settings.enabled() || !de.ownmods.settings.client.ServerSettingsClient.ready(settings.id()) || !sortKey.matches(key))return true;
                // Do not hijack typing into a recipe-book search field.
                if(typingIntoTextField(screen))return true;
                request(client,container,InventorySorter.storage(container.getMenu()) && !key.hasShiftDown());
                return false;
            });
            if(!de.ownmods.settings.client.ServerSettingsClient.ready(settings.id()) || !settings.enabled() || !settings.flag("buttons"))return;
            var a=(ContainerScreenAccess)container;
            var place=SortButtonLayout.place(width,height,a.ownmodsSort$left(),a.ownmodsSort$top(),a.ownmodsSort$width());
            if(place.isEmpty())return;
            var p=place.get();
            var widgets=Screens.getWidgets(screen);
            widgets.removeIf(w->w instanceof SortButton);
            widgets.add(new SortButton(p.x(),p.y(),false,()->request(client,container,false)));
            if(InventorySorter.storage(container.getMenu()) && settings.flag("containers"))
                widgets.add(new SortButton(p.x(),p.y()+24,true,()->request(client,container,true)));
        });
    }
    private static boolean typingIntoTextField(net.minecraft.client.gui.screens.Screen screen) {
        net.minecraft.client.gui.components.events.GuiEventListener focused=screen.getFocused();
        for(int depth=0;focused!=null && depth<16;depth++) {
            if(focused instanceof net.minecraft.client.gui.components.EditBox)return true;
            if(focused instanceof net.minecraft.client.gui.components.events.ContainerEventHandler parent)focused=parent.getFocused();
            else break;
        }
        return false;
    }
    private void request(Minecraft client,AbstractContainerScreen<?> screen,boolean container) {
        if (!settings.enabled() || client.player == null || !screen.getMenu().getCarried().isEmpty()
                || (container && !settings.flag("containers"))) return;
        if (!de.ownmods.settings.client.ServerSettingsClient.ready(settings.id())
                || !net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.canSend(SortRequestPayload.TYPE)) {
            client.player.sendOverlayMessage(Component.translatable("vr.server.unavailable")); return;
        }
        var menu = screen.getMenu();
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(new SortRequestPayload(
                de.ownmods.settings.policy.SettingsWire.PROTOCOL,
                de.ownmods.settings.client.ServerSettingsClient.revision(),
                de.ownmods.settings.client.ServerSettingsClient.nextActionId(), menu.containerId, menu.getStateId(), container));
    }
    private static final class SortButton extends net.minecraft.client.gui.components.Button.Plain {
        private final net.minecraft.world.item.ItemStack icon;
        SortButton(int x,int y,boolean storage,Runnable action) {
            super(x,y,20,20,Component.translatable(storage?"ownmods.inventorysort.storage_button":"ownmods.inventorysort.inventory_button"),b->action.run(),DEFAULT_NARRATION);
            icon=new net.minecraft.world.item.ItemStack(storage?net.minecraft.world.item.Items.CHEST:net.minecraft.world.item.Items.BUNDLE);
            setTooltip(net.minecraft.client.gui.components.Tooltip.create(getMessage()));
        }
        @Override protected void extractContents(net.minecraft.client.gui.GuiGraphicsExtractor g,int mx,int my,float dt) {
            g.fill(getX(),getY(),getX()+20,getY()+20,isHovered()?0xFF55555D:0xFF2A2A30);
            g.outline(getX(),getY(),20,20,isFocused()?0xFFFFFFFF:0xFF999999);
            g.fakeItem(icon,getX()+2,getY()+2);
        }
    }
}
