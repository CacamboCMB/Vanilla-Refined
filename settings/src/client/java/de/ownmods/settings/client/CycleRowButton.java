package de.ownmods.settings.client;

import de.ownmods.settings.api.ChoiceOption;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Same dark row style as boolean options, but cycles a finite setting instead of An/Aus. */
final class CycleRowButton extends Button.Plain {
    private final ChoiceOption option;
    private final Map<String,String> draft;
    private ItemStack icon;

    CycleRowButton(int x,int y,int width,int height,ChoiceOption option,Map<String,String> draft) {
        super(x,y,width,height,Component.empty(), b -> {
            var self=(CycleRowButton)b;
            self.draft.put(self.option.key(),self.option.next(self.selected().id()));
            self.refresh();
        }, DEFAULT_NARRATION);
        this.option=option;this.draft=draft;
        refresh();
    }
    private ChoiceOption.Value selected() { return option.value(draft.getOrDefault(option.key(),option.defaultValue())); }
    private void refresh() {
        var value=selected();
        var message=Component.translatable(option.labelKey()).append(": ").append(Component.translatable(value.labelKey()));
        setMessage(message);
        setTooltip(Tooltip.create(message.copy().append("\n").append(Component.translatable(value.tooltipKey()))
            .append("\n").append(Component.translatable(option.tooltipKey()))));
        var item=BuiltInRegistries.ITEM.getValue(Identifier.parse(value.itemId()));
        icon=new ItemStack(item==null || item==Items.AIR ? Items.BARRIER : item);
    }
    @Override protected void extractContents(GuiGraphicsExtractor g,int mouseX,int mouseY,float delta) {
        int x=getX(),y=getY(),w=getWidth(),h=getHeight();
        g.fill(x,y,x+w,y+h,!active?0xB82B2B2E:isHovered()?0xF05A5A60:0xE548484D);
        g.outline(x,y,w,h,!active?0xFF55555A:0xFF707077);
        if(isFocused())g.outline(x+1,y+1,w-2,h-2,0xFFFFFFFF);
        g.fakeItem(icon,x+6,y+(h-16)/2);
        g.nextStratum();
        var font=Minecraft.getInstance().font;
        String value=Component.translatable(selected().labelKey()).getString();
        int valueWidth=Math.min(Math.max(96,font.width(value)+14),Math.max(96,w-108));
        int valueX=x+w-20-valueWidth;
        int labelX=x+28;
        String label=font.plainSubstrByWidth(Component.translatable(option.labelKey()).getString(),Math.max(1,valueX-labelX-5));
        int textColor=active?0xFFFFFFFF:0xFFAAAAAA;
        g.text(font,label,labelX,y+(h-8)/2,textColor,true);
        int valueH=Math.min(16,h-4), valueY=y+(h-valueH)/2;
        g.fill(valueX,valueY,valueX+valueWidth,valueY+valueH,active?0xFF315C73:0xFF3C3C40);
        g.outline(valueX,valueY,valueWidth,valueH,active?0xFF56869F:0xFF5A5A5F);
        value=font.plainSubstrByWidth(value,Math.max(1,valueWidth-8));
        g.text(font,value,valueX+(valueWidth-font.width(value))/2,valueY+(valueH-8)/2,textColor,true);
        g.text(font,">",x+w-11,y+(h-8)/2,textColor,true);
    }
}
