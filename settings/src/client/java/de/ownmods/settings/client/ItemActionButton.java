package de.ownmods.settings.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.function.BooleanSupplier;

/** Vanilla keyboard/focus/narration behavior, Minecraft item model instead of text. */
public final class ItemActionButton extends Button.Plain {
    private final ItemStack icon;
    private final BooleanSupplier enabledState;
    public ItemActionButton(int x, int y, int size, String itemId, Component label,
            Component description, Runnable action, BooleanSupplier state) {
        super(x, y, size, size, label, b -> action.run(), DEFAULT_NARRATION);
        var item=BuiltInRegistries.ITEM.getValue(Identifier.parse(itemId));
        icon=new ItemStack(item==null || item==Items.AIR ? Items.BARRIER : item);
        enabledState=state;
        setTooltip(Tooltip.create(label.copy().append("\n").append(description)));
    }
    @Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float dt) {
        int x=getX(), y=getY(), size=getWidth();
        g.fill(x,y,x+size,y+size, isHovered() && active ? 0xFF484850 : 0xFF28282E);
        g.outline(x,y,size,size,active ? 0xFF9B9BA7 : 0xFF555555);
        if(isFocused()) g.outline(x+2,y+2,size-4,size-4,0xFFFFFFFF);
        float scale=size>=36 ? 1.5f : 1f;
        float pad=(size-16*scale)/2;
        g.pose().pushMatrix();
        try { g.pose().translate(x+pad,y+pad);g.pose().scale(scale,scale);g.fakeItem(icon,0,0); }
        finally { g.pose().popMatrix(); }
        if(enabledState==null) return;
        g.nextStratum();
        int sx=x+size-11, sy=y+size-11;
        g.fill(sx-1,sy-1,sx+9,sy+9,0xFF17171A);
        if(enabledState.getAsBoolean()) {
            int c=0xFF93E489;
            g.fill(sx,sy+3,sx+2,sy+5,c);g.fill(sx+2,sy+5,sx+4,sy+7,c);
            for(int i=0;i<5;i++)g.fill(sx+3+i,sy+5-i,sx+5+i,sy+7-i,c);
        } else for(int i=0;i<7;i++) {
            g.fill(sx+i,sy+i,sx+i+1,sy+i+1,0xFFEA9999);
            g.fill(sx+6-i,sy+i,sx+7-i,sy+i+1,0xFFEA9999);
        }
    }
}
