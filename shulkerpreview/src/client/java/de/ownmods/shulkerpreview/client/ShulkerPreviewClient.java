package de.ownmods.shulkerpreview.client;
import de.ownmods.settings.client.compat.InputCompat;
import de.ownmods.settings.client.compat.ContainerContentsCompat;

import de.ownmods.shulkerpreview.ShulkerPreviewSettings;
import de.ownmods.shulkerpreview.core.PreviewLayout;
import de.ownmods.settings.api.ManagedMods;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import java.util.List;


public final class ShulkerPreviewClient implements ClientModInitializer {
    private static ShulkerPreviewSettings settings;
    @Override public void onInitializeClient() {
        settings=new ShulkerPreviewSettings(FabricLoader.getInstance().getConfigDir().resolve("ownmods/shulkerpreview.properties"));
        ManagedMods.register(settings);
    }
    /** Returns true only if a complete read-only preview was drawn. */
    public static boolean draw(Screen screen,GuiGraphicsExtractor g,ItemStack box,int mouseX,int mouseY) {
        if(settings==null || !settings.enabled() || box.isEmpty())return false;
        if(!(box.getItem() instanceof BlockItem item) || !(item.getBlock() instanceof ShulkerBoxBlock))return false;
        Minecraft mc=Minecraft.getInstance();
        if(settings.flag("shift_only") && !InputCompat.isKeyDown(mc.getWindow(), "KEY_LSHIFT")
                && !InputCompat.isKeyDown(mc.getWindow(), "KEY_RSHIFT"))return false;
        var contents=box.get(DataComponents.CONTAINER);
        List<ItemStack> slots=ContainerContentsCompat.copyFirst(contents,27);
        if(!settings.flag("show_empty") && slots.stream().allMatch(ItemStack::isEmpty))return false;
        var layout=PreviewLayout.place(screen.width,screen.height,mouseX,mouseY);
        if(layout.scale()<=0)return false;
        g.nextStratum();g.pose().pushMatrix();
        try {
            g.pose().translate(layout.x(),layout.y());g.pose().scale(layout.scale(),layout.scale());
            g.fill(0,0,174,80,0xF520182A);g.outline(0,0,174,80,0xFFB998D6);
            String name=mc.font.plainSubstrByWidth(box.getHoverName().getString(),164);
            g.text(mc.font,name,5,5,0xFFFFFFFF,true);
            for(int i=0;i<27;i++) {
                int x=6+(i%9)*18,y=20+(i/9)*18;
                g.fill(x-1,y-1,x+17,y+17,0xFF403748);g.outline(x-1,y-1,18,18,0xFF66566F);
            }
            for(int i=0;i<slots.size();i++) {
                var stack=slots.get(i);if(stack.isEmpty())continue;
                g.fakeItem(stack,6+(i%9)*18,20+(i/9)*18);
            }
            g.nextStratum();
            if(settings.flag("counts"))for(int i=0;i<slots.size();i++) {
                var stack=slots.get(i);if(!stack.isEmpty())g.itemDecorations(mc.font,stack,6+(i%9)*18,20+(i/9)*18,null);
            }
        } finally {g.pose().popMatrix();}
        return true;
    }
}
