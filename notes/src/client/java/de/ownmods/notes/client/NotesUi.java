package de.ownmods.notes.client;
import de.ownmods.settings.client.UiText;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import de.ownmods.settings.client.compat.GuiTextureCompat;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Visual language for the Notes screens.
 *
 * 0.8.1 keeps the 3x3 reference layout, but replaces the sterile grey-only
 * surfaces with a warmer Minecraft-inspired palette, category colours,
 * raised pixel buttons, inset fields and layered shadows/highlights.
 */
final class NotesUi {
    static final int TEXT=0xFFF7F1E5;
    static final int MUTED=0xFFC8C0B1;
    static final int BODY_TEXT=0xFF211B16;
    static final int BODY_MUTED=0xFF625A50;

    static final int HEADER=0xFF201D1B;
    static final int HEADER_TOP=0xFF3C342D;
    static final int SIDEBAR=0xFF25282D;
    static final int SIDEBAR_DARK=0xFF1C1F23;
    static final int SIDEBAR_HOVER=0xFF343A42;
    static final int CONTENT=0xFFC8BFAE;
    static final int CONTENT_INNER=0xFFD9D0C0;
    static final int CONTENT_STRIPE=0xFFD2C8B6;
    static final int ROW=0xFFE2D8C7;
    static final int ROW_ALT=0xFFD9CEBB;
    static final int ROW_HOVER=0xFFF0E6D5;
    static final int FIELD=0xFFB7AE9F;
    static final int BORDER=0xFF171513;
    static final int SOFT_BORDER=0xFF6A6258;
    static final int SELECTED=0xFF3B84A6;
    static final int SELECTED_LIGHT=0xFF82CBE8;
    static final int SELECTED_BG=0xFFC8E5E9;
    static final int ACCENT=0xFF6FA558;
    static final int INK=0xFF3A2B1E;
    static final int BOOK=0xFFF2E1B8;

    static final int LOCATION=0xFF4A9B82;
    static final int COLLECT=0xFFC08A3A;
    static final int GOAL=0xFFD0634F;
    static final int NOTES=0xFF8A68B4;
    static final int FAVORITE=0xFFE0B83D;
    static final int DANGER=0xFFC6534A;
    static final int BLUE=0xFF4D86B8;

    static final float BODY_SCALE=0.74f;
    static final float TITLE_SCALE=0.88f;

    static void window(GuiGraphicsExtractor g,int x,int y,int w,int h) {
        // Deep outer shadow -> dark frame -> warm carved interior.
        g.fill(x+3,y+4,x+w+3,y+h+4,0x78000000);
        g.fill(x,y,x+w,y+h,0xFF0E0D0C);
        g.fill(x+1,y+1,x+w-1,y+h-1,0xFF5F584F);
        g.fill(x+2,y+2,x+w-2,y+h-2,CONTENT);

        // Header: layered dark wood / deepslate feel with a warm brass trim.
        g.fill(x+3,y+3,x+w-3,y+40,HEADER);
        g.fill(x+3,y+3,x+w-3,y+5,HEADER_TOP);
        g.fill(x+3,y+5,x+w-3,y+7,0xFF2D2925);
        g.fill(x+3,y+38,x+w-3,y+40,0xFF0F0E0D);
        g.fill(x+4,y+39,x+w-4,y+40,0xFF9B7638);

        // Body receives a warm stone/parchment tint rather than flat grey.
        g.fill(x+3,y+40,x+w-3,y+h-3,CONTENT);
        for(int yy=y+42;yy<y+h-4;yy+=16)g.fill(x+4,yy,x+w-4,yy+1,0x18FFFFFF);
        g.fill(x+3,y+h-5,x+w-3,y+h-3,0xFF73695E);
    }

    static void sidebar(GuiGraphicsExtractor g,int x,int y,int w,int h) {
        g.fill(x,y,x+w,y+h,SIDEBAR_DARK);
        g.fill(x+2,y,x+w,y+h,SIDEBAR);
        for(int yy=y+6;yy<y+h;yy+=18)g.fill(x+3,yy,x+w-2,yy+1,0x0EFFFFFF);
        g.fill(x+w-2,y,x+w,y+h,0xFF111315);
        g.fill(x+w-3,y,x+w-2,y+h,0xFF4A5057);
    }

    static void content(GuiGraphicsExtractor g,int x,int y,int w,int h) {
        g.fill(x+2,y+3,x+w+2,y+h+3,0x38000000);
        g.fill(x,y,x+w,y+h,CONTENT_INNER);
        for(int yy=y+10;yy<y+h-2;yy+=20)g.fill(x+1,yy,x+w-1,yy+1,0x10FFFFFF);
        g.fill(x,y,x+w,y+2,0xFFF4EADB);
        g.fill(x,y,x+2,y+h,0xFFF4EADB);
        g.fill(x,y+h-2,x+w,y+h,0xFF7D7468);
        g.fill(x+w-2,y,x+w,y+h,0xFF7D7468);
        g.outline(x,y,w,h,0xFF514B44);
    }

    static void panel(GuiGraphicsExtractor g,int x,int y,int w,int h) { window(g,x,y,w,h); }
    static void section(GuiGraphicsExtractor g,int x,int y,int w,int h) { raised(g,x,y,w,h,0xFFE0D3BD,0xFF8B6F46,false,false); }

    static void slot(GuiGraphicsExtractor g,int x,int y,int w,int h,boolean selected,boolean hover) {
        int accent=selected?SELECTED:0xFF8A765D;
        int fill=selected?SELECTED_BG:(hover?ROW_HOVER:ROW);
        raised(g,x,y,w,h,fill,accent,selected,hover);
        if(selected){
            g.fill(x+3,y+3,x+6,y+h-5,SELECTED);
            g.fill(x+6,y+3,x+w-4,y+5,0x339DE9F2);
            g.outline(x+2,y+2,w-4,h-4,0xFFB9F0F4);
        }
    }

    static void bevel(GuiGraphicsExtractor g,int x,int y,int w,int h,int fill,boolean strong) {
        raised(g,x,y,w,h,fill,strong?SELECTED:0xFF817463,strong,false);
    }

    /** Raised Minecraft-style panel/button: shadow, rim, face, top shine, bottom shade. */
    static void raised(GuiGraphicsExtractor g,int x,int y,int w,int h,int face,int accent,boolean pressed,boolean hover){
        int depth=pressed?1:2;
        g.fill(x+depth,y+depth,x+w+depth,y+h+depth,0x5C000000);
        g.fill(x,y,x+w,y+h,0xFF302B26);
        g.fill(x+1,y+1,x+w-1,y+h-1,blend(face,hover?0xFFFFFFFF:0xFF000000,hover?12:0));
        g.fill(x+2,y+2,x+w-2,y+3,blend(face,0xFFFFFFFF,35));
        g.fill(x+2,y+3,x+3,y+h-2,blend(face,0xFFFFFFFF,22));
        g.fill(x+2,y+h-3,x+w-2,y+h-2,blend(face,0xFF000000,26));
        g.fill(x+w-3,y+3,x+w-2,y+h-2,blend(face,0xFF000000,23));
        if(accent!=0){
            g.fill(x+2,y+2,x+w-2,y+4,blend(accent,0xFFFFFFFF,8));
            g.fill(x+2,y+h-4,x+w-2,y+h-2,blend(accent,0xFF000000,34));
        }
        g.outline(x,y,w,h,0xFF171411);
        g.outline(x+1,y+1,w-2,h-2,blend(accent==0?0xFF766B5F:accent,0xFF000000,18));
    }

    static void searchFrame(GuiGraphicsExtractor g,int x,int y,int w,int h){
        g.fill(x+2,y+2,x+w+2,y+h+2,0x55000000);
        g.fill(x,y,x+w,y+h,0xFF111416);
        g.outline(x,y,w,h,0xFF8E7242);
        g.fill(x+1,y+1,x+w-1,y+3,0xFF2A3034);
    }

    static void thumbnail(GuiGraphicsExtractor g,int x,int y,int w,int h){
        g.fill(x+3,y+4,x+w+3,y+h+4,0x44000000);
        g.fill(x,y,x+w,y+h,0xFF6C5840);
        g.fill(x+2,y+2,x+w-2,y+h-2,0xFFE4D5B8);
        g.fill(x+4,y+4,x+w-4,y+h-4,0xFFD5C6A9);
        g.fill(x+4,y+4,x+w-4,y+6,0xFFFFF0D0);
        g.outline(x,y,w,h,0xFF342B21);
    }

    static void textArea(GuiGraphicsExtractor g,int x,int y,int w,int h){
        g.fill(x+2,y+3,x+w+2,y+h+3,0x44000000);
        g.fill(x,y,x+w,y+h,0xFF1C2428);
        g.outline(x,y,w,h,0xFF6E805E);
        g.fill(x+1,y+1,x+w-1,y+3,0xFF344248);
        g.fill(x+3,y+3,x+w-3,y+h-3,0xFF242D31);
    }

    static void book(GuiGraphicsExtractor g,int x,int y,int w,int h) {
        g.fill(x+4,y+5,x+w+4,y+h+5,0x66000000);
        g.fill(x,y,x+w,y+h,0xFF2C1C11);
        g.fill(x+2,y+2,x+w-2,y+h-2,0xFF704025);
        g.fill(x+4,y+3,x+w-4,y+h-4,0xFFB07842);
        g.fill(x+7,y+5,x+w-7,y+h-7,0xFFE7D3A4);
        g.fill(x+9,y+7,x+w-9,y+h-9,BOOK);
        g.fill(x+10,y+8,x+w-10,y+10,0xFFFFF1CE);
        for(int yy=y+16;yy<y+h-11;yy+=11)g.fill(x+14,yy,x+w-14,yy+1,0xFFD9C89F);
        for(int yy=y+10;yy<y+h-9;yy+=8){g.fill(x+9,yy,x+11,yy+4,0xFFB7533A);g.fill(x+11,yy+3,x+12,yy+6,0xFF9D452F);}
        g.fill(x+w-9,y+10,x+w-7,y+h-10,0x55A16A38);
    }

    static void progress(GuiGraphicsExtractor g,int x,int y,int w,int h,int percent) {
        percent=Math.max(0,Math.min(100,percent));
        g.fill(x+1,y+2,x+w+1,y+h+2,0x55000000);
        g.fill(x,y,x+w,y+h,0xFF4D453B);g.outline(x,y,w,h,0xFF2E2923);
        g.fill(x+1,y+1,x+w-1,y+h-1,0xFF8D7D67);
        int fw=(w-2)*percent/100;
        if(fw>0){
            int c=percent>=100?0xFF63B657:percent>=60?0xFF78AC4E:percent>=30?0xFFD0A23F:0xFFC7743F;
            g.fill(x+1,y+1,x+1+fw,y+h-1,c);
            g.fill(x+2,y+2,x+fw,y+3,blend(c,0xFFFFFFFF,35));
            g.fill(x+2,y+h-2,x+fw,y+h-1,blend(c,0xFF000000,28));
        }
    }

    static void statusDot(GuiGraphicsExtractor g,int x,int y,int color) {
        g.fill(x+1,y,x+4,y+5,0x66000000);g.fill(x,y+1,x+5,y+4,0x66000000);
        g.fill(x+1,y,x+4,y+5,color);g.fill(x,y+1,x+5,y+4,color);
        g.fill(x+1,y+1,x+2,y+2,0xAAFFFFFF);
    }

    static void smallText(GuiGraphicsExtractor g,Font f,String text,int x,int y,int color,boolean shadow,int maxWidth) {scaledText(g,f,text,x,y,color,shadow,maxWidth,BODY_SCALE);}
    static void titleText(GuiGraphicsExtractor g,Font f,String text,int x,int y,int color,int maxWidth) {scaledText(g,f,text,x,y,color,false,maxWidth,TITLE_SCALE);}
    private static void scaledText(GuiGraphicsExtractor g,Font f,String text,int x,int y,int color,boolean shadow,int maxWidth,float scale){
        if(text==null||text.isEmpty())return;int sourceWidth=Math.max(1,(int)(maxWidth/scale));String clipped=f.plainSubstrByWidth(text,sourceWidth);
        g.pose().pushMatrix();try {g.pose().translate(x,y);g.pose().scale(scale,scale);g.text(f,clipped,0,0,color,shadow);}finally {g.pose().popMatrix();}
    }
    static int smallWidth(Font f,String text){return Math.round(f.width(text)*BODY_SCALE);}

    static void icon(GuiGraphicsExtractor g,String id,int x,int y,int size) {
        if(id==null||id.isBlank())return;
        if(id.equals("@home")) {home(g,x,y,size);return;}
        if(id.equals("@village")) {int small=Math.max(8,size*2/3);home(g,x,y+size-small,small);home(g,x+size-small,y,small);icon(g,"minecraft:bell",x+size/2-3,y+size/2-1,Math.max(7,size/2));return;}
        var item=BuiltInRegistries.ITEM.getValue(Identifier.parse(id));var stack=new ItemStack(item==null||item==Items.AIR?Items.PAPER:item);
        g.pose().pushMatrix();try {g.pose().translate(x,y);g.pose().scale(size/16f,size/16f);g.fakeItem(stack,0,0);}finally {g.pose().popMatrix();}g.nextStratum();
    }
    private static void home(GuiGraphicsExtractor g,int x,int y,int size) {GuiTextureCompat.blit(g,Identifier.fromNamespaceAndPath("ownmods_notes","textures/gui/home.png"),x,y,0,0,size,size,32,32,32,32);}

    static int color(String name) {return switch(name){case "red"->0xFFD76558;case "green"->0xFF72A85B;case "blue"->0xFF5597C7;case "yellow"->0xFFD6AE3C;case "purple"->0xFF916DB4;default->0xFF9A8973;};}
    static int statusColor(String status) {
        if(java.util.List.of("DONE",UiText.tr("vr.text.467e01ba533e"),UiText.tr("vr.text.10639ced8abf"),UiText.tr("vr.text.166d0a8edb9b")).contains(status))return 0xFF62A650;
        if(java.util.List.of("IN_PROGRESS","EXPLORING",UiText.tr("vr.text.e94e8adc17be"),UiText.tr("vr.text.cc486ecd9892"),UiText.tr("vr.text.dbbae30b567c")).contains(status))return 0xFFE2A52E;
        if(java.util.List.of("FOUND","EXPLORED","LOOTED",UiText.tr("vr.text.f671b115fac5"),UiText.tr("vr.text.b5cc4330456e"),UiText.tr("vr.text.c5023376bdc7"),UiText.tr("vr.text.4fe36ac14ff0"),UiText.tr("vr.text.8163454f378f"),UiText.tr("vr.text.2b106cffafe6")).contains(status))return 0xFF4D9AC1;
        return 0xFF8F8577;
    }

    static int categoryAccent(String icon,String label){
        String k=((icon==null?"":icon)+" "+(label==null?"":label)).toLowerCase();
        if(k.contains("village")||k.contains("compass")||k.contains("location")||k.contains("biom")||k.contains("homebase"))return LOCATION;
        if(k.contains("chest")||k.contains("item_frame")||k.contains("sammel")||k.contains("item")||k.contains("material"))return COLLECT;
        if(k.contains("target")||k.contains("ziel")||k.contains("farm")||k.contains("mob"))return GOAL;
        if(k.contains("book")||k.contains("notiz")||k.contains("paper"))return NOTES;
        if(k.contains("nether_star")||k.contains("favorit"))return FAVORITE;
        if(k.contains("lava_bucket")||k.contains("papierkorb")||k.contains("löschen")||k.contains("entfernen")||k.contains("verwerfen"))return DANGER;
        if(k.contains("spyglass")||k.contains("chain")||k.contains("recovery_compass"))return BLUE;
        return 0xFF8A765D;
    }

    static int actionAccent(String label){
        String k=label==null?"":label.toLowerCase();
        if(k.contains("speichern")||k.contains("übernehmen")||k.contains("hinzufügen")||k.startsWith("+")||k.contains("aktivieren"))return 0xFF5E9B53;
        if(k.contains("abbrechen")||k.contains("löschen")||k.contains("entfernen")||k.contains("papierkorb")||k.contains("verwerfen")||k.startsWith("–"))return DANGER;
        if(k.contains("zurück")||k.contains("schließen")||k.equals("<")||k.equals(">"))return BLUE;
        return 0xFF9A7440;
    }

    static int blend(int base,int overlay,int percent){
        percent=Math.max(0,Math.min(100,percent));int inv=100-percent;
        int a=(((base>>>24)&255)*inv+((overlay>>>24)&255)*percent)/100;
        int r=(((base>>>16)&255)*inv+((overlay>>>16)&255)*percent)/100;
        int gg=(((base>>>8)&255)*inv+((overlay>>>8)&255)*percent)/100;
        int b=((base&255)*inv+(overlay&255)*percent)/100;
        return (a<<24)|(r<<16)|(gg<<8)|b;
    }

    static final class Surface extends Button.Plain {
        private final java.util.function.Consumer<GuiGraphicsExtractor> draw;
        Surface(int x,int y,int w,int h,boolean interactive,Runnable click,java.util.function.Consumer<GuiGraphicsExtractor> draw) {super(x,y,w,h,Component.empty(),b->click.run(),DEFAULT_NARRATION);this.draw=draw;active=interactive;}
        @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float d){draw.accept(g);}
    }

    static final class ActionButton extends Button.Plain {
        final String label;
        ActionButton(int x,int y,int w,int h,String label,Runnable click){super(x,y,w,h,Component.literal(label),b->click.run(),DEFAULT_NARRATION);this.label=label;}
        @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float d){
            int x=getX(),y=getY(),w=getWidth(),h=getHeight();int accent=active?actionAccent(label):0xFF66615B;int face=active?blend(0xFFD5C6AE,accent,18):0xFFAAA49A;
            raised(g,x,y,w,h,face,accent,isFocused(),isHovered()&&active);var f=Minecraft.getInstance().font;int tw=Math.min(w-10,smallWidth(f,label));smallText(g,f,label,x+(w-tw)/2,y+(h-7)/2,active?BODY_TEXT:0xFF706B64,false,w-10);
        }
    }

    static class IconButton extends Button.Plain {
        int accent;final String icon;final boolean tile;private final java.util.function.BooleanSupplier selected;
        IconButton(int x,int y,int w,int h,String icon,String label,boolean tile,Runnable click){this(x,y,w,h,icon,label,tile,()->false,click);}
        IconButton(int x,int y,int w,int h,String icon,String label,boolean tile,java.util.function.BooleanSupplier selected,Runnable click){super(x,y,w,h,Component.literal(label),b->click.run(),DEFAULT_NARRATION);this.icon=icon;this.tile=tile;this.selected=selected;setTooltip(Tooltip.create(Component.literal(label.replace('\n',' '))));}
        @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float d){
            int x=getX(),y=getY(),w=getWidth(),h=getHeight();boolean on=selected.getAsBoolean();int ac=accent!=0?accent:categoryAccent(icon,getMessage().getString());int face=blend(0xFFE2D6C3,ac,on?22:11);raised(g,x,y,w,h,face,ac,on,isHovered());
            if(on){g.fill(x+3,y+3,x+6,y+h-5,ac);g.outline(x+2,y+2,w-4,h-4,blend(ac,0xFFFFFFFF,42));}
            var f=Minecraft.getInstance().font;
            if(tile){
                int iconSize=Math.min(30,Math.max(18,h-31));g.fill(x+(w-iconSize)/2-3,y+5,x+(w+iconSize)/2+3,y+9+iconSize,0x23000000);icon(g,icon,x+(w-iconSize)/2,y+7,iconSize);
                String raw=getMessage().getString();String[] lines=raw.split("\\n",2);int yy=lines.length==1?y+h-15:y+h-22;int lw=Math.min(w-10,smallWidth(f,lines[0]));smallText(g,f,lines[0],x+(w-lw)/2,yy,BODY_TEXT,false,w-10);
                if(lines.length>1){int l2w=Math.min(w-10,smallWidth(f,lines[1]));smallText(g,f,lines[1],x+(w-l2w)/2,yy+9,BODY_TEXT,false,w-10);}
                if(on){g.fill(x+w-17,y+4,x+w-3,y+18,0xFF397A3F);g.outline(x+w-17,y+4,14,14,0xFF183E20);g.fill(x+w-16,y+5,x+w-4,y+7,0xFF70B978);smallText(g,f,"✓",x+w-14,y+7,0xFFFFFFFF,true,10);}
            }else{
                if(!icon.isBlank()){g.fill(x+4,y+3,x+21,y+h-3,blend(ac,0xFF000000,12));icon(g,icon,x+6,y+(h-14)/2,14);}smallText(g,f,getMessage().getString(),x+(icon.isBlank()?7:25),y+(h-7)/2,active?BODY_TEXT:0xFF777169,false,w-(icon.isBlank()?12:30));
            }
        }
    }

    static final class ItemTileButton extends Button.Plain {
        final String icon,label;final java.util.function.IntSupplier quantity;
        ItemTileButton(int x,int y,int w,int h,String icon,String label,java.util.function.IntSupplier quantity,Runnable click){super(x,y,w,h,Component.literal(label),b->click.run(),DEFAULT_NARRATION);this.icon=icon;this.label=label;this.quantity=quantity;setTooltip(Tooltip.create(Component.literal(label)));}
        @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float d){
            int q=quantity.getAsInt();boolean on=q>0;int x=getX(),y=getY(),w=getWidth(),h=getHeight();int ac=on?0xFF49A36A:categoryAccent(icon,label);int face=on?0xFFCDE8C8:blend(0xFFE3D7C4,ac,8);raised(g,x,y,w,h,face,ac,on,isHovered());
            if(on){g.fill(x+2,y+2,x+6,y+h-4,0xFF3C9861);g.outline(x+1,y+1,w-2,h-2,0xFFA7E0B2);}
            int size=Math.min(23,h-19);g.fill(x+(w-size)/2-2,y+4,x+(w+size)/2+2,y+8+size,0x22000000);icon(g,icon,x+(w-size)/2,y+5,size);var f=Minecraft.getInstance().font;
            int lw=Math.min(w-8,smallWidth(f,label));smallText(g,f,label,x+(w-lw)/2,y+h-13,BODY_TEXT,false,w-8);
            if(on){String badge="×"+q;int bw=Math.min(w-6,smallWidth(f,badge)+8);g.fill(x+w-bw-4,y+4,x+w-3,y+15,0xFF2F7448);g.fill(x+w-bw-3,y+5,x+w-4,y+7,0xFF66B77A);g.outline(x+w-bw-4,y+4,bw+1,11,0xFF173D27);smallText(g,f,badge,x+w-bw,y+6,0xFFFFFFFF,true,bw-4);g.fill(x+4,y+4,x+16,y+16,0xFF2F7448);smallText(g,f,"✓",x+6,y+6,0xFFFFFFFF,true,9);}
        }
    }

    static final class CategoryTile extends Button.Plain {
        final String icon,label;final int count;final java.util.function.BooleanSupplier selected;
        CategoryTile(int x,int y,int w,int h,String icon,String label,int count,java.util.function.BooleanSupplier selected,Runnable click){super(x,y,w,h,Component.literal(label),b->click.run(),DEFAULT_NARRATION);this.icon=icon;this.label=label;this.count=count;this.selected=selected;setTooltip(Tooltip.create(Component.literal(count<0?label:label+" · "+count)));}
        @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float d){
            int x=getX(),y=getY(),w=getWidth(),h=getHeight();boolean on=selected.getAsBoolean();int ac=categoryAccent(icon,label);int face=blend(0xFFE0D3BE,ac,on?23:12);raised(g,x,y,w,h,face,ac,on,isHovered());if(on)g.fill(x+3,y+3,x+6,y+h-4,ac);
            int iconSize=Math.min(28,h-25);icon(g,icon,x+(w-iconSize)/2,y+6,iconSize);var f=Minecraft.getInstance().font;int lw=Math.min(w-10,smallWidth(f,label));smallText(g,f,label,x+(w-lw)/2,y+h-(count>=0?19:12),BODY_TEXT,false,w-10);if(count>=0){String c=Integer.toString(count);int cw=Math.min(w-10,smallWidth(f,c));smallText(g,f,c,x+(w-cw)/2,y+h-9,blend(BODY_MUTED,ac,25),false,w-10);}
        }
    }

    static final class NoteCardButton extends Button.Plain {
        final String icon,title,subtitle;int accent;
        NoteCardButton(int x,int y,int w,int h,String icon,String title,String subtitle,Runnable click){super(x,y,w,h,Component.literal(title),b->click.run(),DEFAULT_NARRATION);this.icon=icon;this.title=title;this.subtitle=subtitle;setTooltip(Tooltip.create(Component.literal(title+(subtitle.isBlank()?"":"\n"+subtitle))));}
        @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float d){int x=getX(),y=getY(),w=getWidth(),h=getHeight();int ac=accent!=0?accent:categoryAccent(icon,subtitle);raised(g,x,y,w,h,blend(0xFFE2D8C8,ac,9),ac,false,isHovered());g.fill(x+2,y+2,x+6,y+h-4,ac);icon(g,icon,x+10,y+(h-21)/2,21);var f=Minecraft.getInstance().font;smallText(g,f,title,x+38,y+5,BODY_TEXT,false,w-45);smallText(g,f,subtitle,x+38,y+16,blend(BODY_MUTED,ac,18),false,w-45);}
    }

    static final class LocationRowButton extends Button.Plain {
        final String icon,title,subtitle,distance;int accent;
        LocationRowButton(int x,int y,int w,int h,String icon,String title,String subtitle,String distance,Runnable click){super(x,y,w,h,Component.literal(title),b->click.run(),DEFAULT_NARRATION);this.icon=icon;this.title=title;this.subtitle=subtitle;this.distance=distance==null?"":distance;setTooltip(Tooltip.create(Component.literal(title+(subtitle.isBlank()?"":"\n"+subtitle)+(this.distance.isBlank()?"":"\n"+this.distance))));}
        @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float d){int x=getX(),y=getY(),w=getWidth(),h=getHeight();int ac=accent!=0?accent:LOCATION;raised(g,x,y,w,h,blend(0xFFE0D8C8,ac,9),ac,false,isHovered());g.fill(x+2,y+2,x+6,y+h-4,ac);icon(g,icon,x+10,y+(h-21)/2,21);var f=Minecraft.getInstance().font;int right=distance.isBlank()?8:Math.max(48,smallWidth(f,distance)+10);smallText(g,f,title,x+38,y+5,BODY_TEXT,false,w-46-right);smallText(g,f,subtitle,x+38,y+16,blend(BODY_MUTED,ac,18),false,w-46-right);if(!distance.isBlank()){g.fill(x+w-right+2,y+8,x+w-5,y+h-8,blend(ac,0xFFFFFFFF,60));g.outline(x+w-right+2,y+8,right-7,h-16,blend(ac,0xFF000000,18));smallText(g,f,distance,x+w-right+7,y+11,BODY_TEXT,false,right-13);}}
    }

    static final class TabButton extends Button.Plain {
        int accent;final String icon,label;final java.util.function.BooleanSupplier selected;
        TabButton(int x,int y,int w,int h,String icon,String label,java.util.function.BooleanSupplier selected,Runnable click){super(x,y,w,h,Component.literal(label),b->click.run(),DEFAULT_NARRATION);this.icon=icon;this.label=label;this.selected=selected;setTooltip(Tooltip.create(Component.literal(label)));}
        @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float d){
            int x=getX(),y=getY(),w=getWidth(),h=getHeight();boolean on=selected.getAsBoolean();int ac=accent!=0?accent:categoryAccent(icon,label);
            // Inactive tabs are deliberately neutral. Only the selected filter carries
            // the category colour, so an "Items" or "Farms" label can never look active by accident.
            int face=on?blend(0xFFDCCFBA,ac,30):(isHovered()?0xFFE5DDCF:0xFFD8CDBB);
            int rim=on?ac:0xFF817565;
            raised(g,x,y,w,h,face,rim,on,isHovered());
            if(on){g.fill(x+2,y+2,x+w-2,y+5,ac);g.fill(x+3,y+5,x+w-3,y+6,blend(ac,0xFFFFFFFF,35));}
            if(!icon.isBlank())icon(g,icon,x+5,y+(h-13)/2,13);var f=Minecraft.getInstance().font;if(w>=34)smallText(g,f,label,x+(icon.isBlank()?6:21),y+(h-7)/2,BODY_TEXT,false,w-(icon.isBlank()?11:25));
        }
    }

    static final class SidebarButton extends Button.Plain {
        final String icon,label;final java.util.function.BooleanSupplier selected;
        SidebarButton(int x,int y,int w,int h,String icon,String label,java.util.function.BooleanSupplier selected,Runnable click){super(x,y,w,h,Component.literal(label),b->click.run(),DEFAULT_NARRATION);this.icon=icon;this.label=label;this.selected=selected;}
        @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float d){int x=getX(),y=getY(),w=getWidth(),h=getHeight();boolean on=selected.getAsBoolean();int ac=categoryAccent(icon,label);int fill=on?blend(SIDEBAR,ac,42):(isHovered()?blend(SIDEBAR_HOVER,ac,14):SIDEBAR);g.fill(x,y,x+w,y+h,fill);if(on){g.fill(x,y,x+4,y+h,ac);g.fill(x+4,y,x+w,y+2,blend(ac,0xFFFFFFFF,28));g.fill(x+4,y+h-2,x+w,y+h,blend(ac,0xFF000000,38));}else if(isHovered())g.fill(x,y,x+2,y+h,blend(ac,0xFFFFFFFF,12));g.fill(x+w-1,y,x+w,y+h,0xFF15181B);icon(g,icon,x+9,y+(h-16)/2,16);smallText(g,Minecraft.getInstance().font,label,x+30,y+(h-7)/2,TEXT,false,w-35);}
    }

    static final class StarButton extends Button.Plain {
        final java.util.function.BooleanSupplier on;
        StarButton(int x,int y,int w,int h,java.util.function.BooleanSupplier on,Runnable click){super(x,y,w,h,Component.literal(UiText.tr("vr.text.e2325be783c5")),b->click.run(),DEFAULT_NARRATION);this.on=on;setTooltip(Tooltip.create(Component.literal(UiText.tr("vr.text.e2325be783c5"))));}
        @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float d){
            int x=getX(),y=getY(),w=getWidth(),h=getHeight();boolean selected=on.getAsBoolean();
            int face=selected?0xFFF0D47A:(isHovered()?0xFFE8D7A5:0xFFD7CCB9);
            raised(g,x,y,w,h,face,selected?FAVORITE:0xFF8B7C65,selected,isHovered());
            int size=Math.min(18,Math.max(14,Math.min(w,h)-8));
            icon(g,"minecraft:nether_star",x+(w-size)/2,y+(h-size)/2,size);
            if(selected){g.fill(x+2,y+2,x+w-2,y+4,FAVORITE);g.fill(x+w-8,y+4,x+w-4,y+8,0xFFFFF1A6);}
        }
    }

    static final class IconOnlyButton extends Button.Plain {
        final String icon,tip;final int accent;
        IconOnlyButton(int x,int y,int w,int h,String icon,String tip,int accent,Runnable click){super(x,y,w,h,Component.literal(tip),b->click.run(),DEFAULT_NARRATION);this.icon=icon;this.tip=tip;this.accent=accent;setTooltip(Tooltip.create(Component.literal(tip)));}
        @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float d){
            int x=getX(),y=getY(),w=getWidth(),h=getHeight();int face=blend(0xFFD7C9B5,accent,isHovered()?25:14);raised(g,x,y,w,h,face,accent,isFocused(),isHovered());int size=Math.min(17,Math.max(13,Math.min(w,h)-9));icon(g,icon,x+(w-size)/2,y+(h-size)/2,size);
        }
    }

    static final class GoalRowButton extends Button.Plain {
        final String icon,title,subtitle,status;final int accent;
        GoalRowButton(int x,int y,int w,int h,String icon,String title,String subtitle,String status,int accent,Runnable click){super(x,y,w,h,Component.literal(title),b->click.run(),DEFAULT_NARRATION);this.icon=icon;this.title=title;this.subtitle=subtitle;this.status=status;this.accent=accent;}
        @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float d){int x=getX(),y=getY(),w=getWidth(),h=getHeight();int ac=accent!=0?accent:GOAL;raised(g,x,y,w,h,blend(0xFFE2D6C5,ac,8),ac,false,isHovered());g.fill(x+2,y+2,x+6,y+h-4,ac);icon(g,icon,x+10,y+(h-21)/2,21);var f=Minecraft.getInstance().font;smallText(g,f,title,x+38,y+5,BODY_TEXT,false,w-139);smallText(g,f,subtitle,x+38,y+16,blend(BODY_MUTED,ac,16),false,w-139);if(!status.isBlank()){int sc=statusColor(status);g.fill(x+w-97,y+7,x+w-5,y+h-7,blend(sc,0xFFFFFFFF,62));g.outline(x+w-97,y+7,92,h-14,blend(sc,0xFF000000,20));statusDot(g,x+w-91,y+13,sc);smallText(g,f,status,x+w-82,y+11,BODY_TEXT,false,76);}}
    }

    static final class CollectRowButton extends Button.Plain {
        final String icon,title;final int have,wanted,accent;
        CollectRowButton(int x,int y,int w,int h,String icon,String title,int have,int wanted,int accent,Runnable click){super(x,y,w,h,Component.literal(title),b->click.run(),DEFAULT_NARRATION);this.icon=icon;this.title=title;this.have=have;this.wanted=Math.max(1,wanted);this.accent=accent;}
        @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float d){int x=getX(),y=getY(),w=getWidth(),h=getHeight();int ac=accent!=0?accent:COLLECT;raised(g,x,y,w,h,blend(0xFFE2D6C5,ac,8),ac,false,isHovered());g.fill(x+2,y+2,x+6,y+h-4,ac);int p=Math.min(100,(int)(100L*Math.min(Math.max(0,have),wanted)/wanted));g.fill(x+8,y+13,x+17,y+22,0xFFFFF3D4);g.outline(x+8,y+13,9,9,0xFF67533A);if(p>=100){g.fill(x+10,y+15,x+15,y+20,0xFF4D8E43);smallText(g,Minecraft.getInstance().font,"✓",x+9,y+13,0xFFFFFFFF,true,8);}icon(g,icon,x+22,y+(h-21)/2,21);var f=Minecraft.getInstance().font;smallText(g,f,title,x+50,y+5,BODY_TEXT,false,w-153);String c=have+" / "+wanted;smallText(g,f,c,x+w-96,y+5,BODY_TEXT,false,90);progress(g,x+50,y+20,Math.max(30,w-98),6,p);smallText(g,f,p+" %",x+w-37,y+17,blend(BODY_MUTED,ac,20),false,34);}
    }

    static final class HeaderButton extends Button.Plain {
        final String icon;final String tip;
        HeaderButton(int x,int y,int w,int h,String icon,String tip,Runnable click){super(x,y,w,h,Component.literal(tip),b->click.run(),DEFAULT_NARRATION);this.icon=icon;this.tip=tip;setTooltip(Tooltip.create(Component.literal(tip)));}
        @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float d){int x=getX(),y=getY(),w=getWidth(),h=getHeight();int ac=categoryAccent(icon,tip);raised(g,x,y,w,h,blend(0xFF554B40,ac,20),ac,isFocused(),isHovered());icon(g,icon,x+(w-15)/2,y+(h-15)/2,15);}
    }
}
