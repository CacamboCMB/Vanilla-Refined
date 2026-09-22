package de.ownmods.notes.client;
import de.ownmods.settings.client.UiText;

import de.ownmods.notes.core.NotesCatalog.Entry;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

abstract class NotebookBase extends Screen {
    final Screen parent;
    int left,top,wide,tall,bodyY,bodyBottom;
    String problem="";
    private boolean rebuildNext;
    final List<AbstractWidget> normal = new ArrayList<>();
    final List<Label> labels = new ArrayList<>();
    final List<AbstractWidget> overlayRows = new ArrayList<>();
    private Popup popup;
    private int px,py,pw,ph,popupRows;
    record Label(String text,int x,int y,int color) {}
    NotebookBase(String title,Screen parent) {super(Component.literal(title));this.parent=parent;}

    protected int preferredWidth(){return 640;}
    protected int preferredHeight(){return 420;}
    protected int minimumUsableWidth(){return 360;}
    protected int minimumUsableHeight(){return 260;}
    protected String headerIcon(){return "";}
    protected String displayedTitle(){return title.getString();}
    protected int titleStartX(){return left+12;}

    @Override protected final void init() {
        clearWidgets();normal.clear();labels.clear();overlayRows.clear();
        // Preserve the mock-up's proportions when GUI scale or a short window
        // constrains one axis. Older builds kept full width while shrinking only
        // height, producing the very stretched layout seen in the screenshots.
        int prefW=preferredWidth(), prefH=preferredHeight();
        double scale=Math.min(1.0,Math.min((width-12.0)/prefW,(height-12.0)/prefH));
        wide=Math.max(340,(int)Math.floor(prefW*scale));
        tall=Math.max(240,(int)Math.floor(prefH*scale));
        wide=Math.min(wide,Math.max(250,width-6));tall=Math.min(tall,Math.max(190,height-6));
        left=(width-wide)/2;top=(height-tall)/2;bodyY=top+48;bodyBottom=top+tall-40;
        addRenderableWidget(new NotesUi.Surface(left,top,wide,tall,false,()->{},g->NotesUi.window(g,left,top,wide,tall)));
        if(width<minimumUsableWidth() || height<minimumUsableHeight()) {
            label(UiText.tr("vr.text.276b7fce38f0"),left+12,top+55);
            button(left+12,top+tall-32,wide-24,UiText.tr("vr.text.548611ce58f7"),()->minecraft.gui.setScreen(parent));
        } else {build();if(popup!=null)attachPopup();}
    }
    protected abstract void build();
    final void rebuild() {rebuildNext=true;}
    @Override public void tick(){if(rebuildNext){rebuildNext=false;init();}}
    @Override public boolean isPauseScreen(){return true;}
    final boolean closePopup(){if(popup==null)return false;popup=null;rebuild();return true;}
    @Override public void onClose(){if(!closePopup())minecraft.gui.setScreen(parent);}
    final <T extends AbstractWidget>T widget(T w){normal.add(w);return addRenderableWidget(w);}
    final Button button(int x,int y,int w,String label,Runnable action){return widget(new NotesUi.ActionButton(x,y,w,20,label,()->safe(action)));}
    final NotesUi.IconButton iconButton(int x,int y,int w,int h,String icon,String label,boolean tile,Runnable action){return widget(new NotesUi.IconButton(x,y,w,h,icon,label,tile,()->safe(action)));}
    final NotesUi.TabButton tabButton(int x,int y,int w,int h,String icon,String label,java.util.function.BooleanSupplier selected,Runnable action){return widget(new NotesUi.TabButton(x,y,w,h,icon,label,selected,()->safe(action)));}
    final NotesUi.CategoryTile categoryTile(int x,int y,int w,int h,String icon,String label,int count,java.util.function.BooleanSupplier selected,Runnable action){return widget(new NotesUi.CategoryTile(x,y,w,h,icon,label,count,selected,()->safe(action)));}
    final NotesUi.NoteCardButton noteCard(int x,int y,int w,int h,String icon,String title,String subtitle,Runnable action){return widget(new NotesUi.NoteCardButton(x,y,w,h,icon,title,subtitle,()->safe(action)));}
    final NotesUi.LocationRowButton locationRow(int x,int y,int w,int h,String icon,String title,String subtitle,String distance,Runnable action){return widget(new NotesUi.LocationRowButton(x,y,w,h,icon,title,subtitle,distance,()->safe(action)));}
    final NotesUi.SidebarButton sidebarButton(int x,int y,int w,int h,String icon,String label,java.util.function.BooleanSupplier selected,Runnable action){return widget(new NotesUi.SidebarButton(x,y,w,h,icon,label,selected,()->safe(action)));}
    final NotesUi.HeaderButton headerButton(int x,int y,int w,int h,String icon,String tip,Runnable action){return widget(new NotesUi.HeaderButton(x,y,w,h,icon,tip,()->safe(action)));}
    final NotesUi.StarButton starButton(int x,int y,int w,int h,java.util.function.BooleanSupplier on,Runnable action){return widget(new NotesUi.StarButton(x,y,w,h,on,()->safe(action)));}
    final NotesUi.IconOnlyButton iconOnlyButton(int x,int y,int w,int h,String icon,String tip,int accent,Runnable action){return widget(new NotesUi.IconOnlyButton(x,y,w,h,icon,tip,accent,()->safe(action)));}
    final NotesUi.GoalRowButton goalRow(int x,int y,int w,int h,String icon,String title,String subtitle,String status,int accent,Runnable action){return widget(new NotesUi.GoalRowButton(x,y,w,h,icon,title,subtitle,status,accent,()->safe(action)));}
    final NotesUi.CollectRowButton collectRow(int x,int y,int w,int h,String icon,String title,int have,int wanted,int accent,Runnable action){return widget(new NotesUi.CollectRowButton(x,y,w,h,icon,title,have,wanted,accent,()->safe(action)));}
    final NotesUi.ItemTileButton itemTile(int x,int y,int w,int h,String icon,String label,java.util.function.IntSupplier quantity,Runnable action){return widget(new NotesUi.ItemTileButton(x,y,w,h,icon,label,quantity,()->safe(action)));}
    final EditBox edit(int x,int y,int w,String value,int limit,Consumer<String> change) {
        EditBox box=widget(new EditBox(font,x,y,w,20,Component.literal(UiText.tr("vr.text.a5ce7b5d4230"))));box.setMaxLength(limit);box.setValue(value);box.setResponder(change);return box;
    }
    final void label(String value,int x,int y){labels.add(new Label(value,x,y,NotesUi.BODY_TEXT));}
    final void muted(String value,int x,int y){labels.add(new Label(value,x,y,NotesUi.BODY_MUTED));}
    final void headerLabel(String value,int x,int y){labels.add(new Label(value,x,y,NotesUi.TEXT));}
    final void colored(String value,int x,int y,int color){labels.add(new Label(value,x,y,color));}
    final void safe(Runnable action){try{action.run();}catch(Exception e){problem=e.getMessage()==null?e.getClass().getSimpleName():e.getMessage();}}

    final void pick(String label,List<Entry> entries,int anchorX,int anchorY,int width,Consumer<Entry> result) {
        popup=new Popup(label,List.copyOf(entries),anchorX,anchorY,width,result);rebuild();
    }
    final void choice(int x,int y,int w,String label,List<Entry> entries,String id,Consumer<String> chosen) {
        Entry selected=entries.stream().filter(e->e.id().equals(id)).findFirst().orElse(new Entry(id,id,"minecraft:paper"));
        iconButton(x,y,w,20,selected.icon(),label+selected.label()+"  ▾",false,()->pick(label,entries,x,y+20,w,e->chosen.accept(e.id())));
    }
    final void nav(int page,int pages,int y,java.util.function.IntConsumer select) {
        button(left+14,y,28,"<",()->select.accept(page-1)).active=page>0;
        button(left+wide-42,y,28,">",()->select.accept(page+1)).active=page+1<pages;
        muted((page+1)+" / "+pages,left+wide/2-18,y+6);
    }

    private static final class Popup {
        final String label;final List<Entry> entries;final int x,y,w;final Consumer<Entry> choose;String search="";int page;
        Popup(String label,List<Entry> entries,int x,int y,int w,Consumer<Entry> choose){this.label=label;this.entries=entries;this.x=x;this.y=y;this.w=w;this.choose=choose;}
    }
    private void attachPopup() {
        for(AbstractWidget w:normal){w.active=false;w.setFocused(false);}
        pw=Math.min(Math.max(popup.w,250),width-16);ph=Math.min(220,height-24);
        px=Math.max(8,Math.min(popup.x,width-pw-8));py=Math.max(8,Math.min(popup.y,height-ph-8));
        popupRows=Math.max(1,(ph-68)/22);
        addRenderableWidget(new NotesUi.Surface(0,0,width,height,false,()->{},g->{g.nextStratum();g.fill(0,0,width,height,0x76000000);NotesUi.window(g,px,py,pw,ph);}));
        int[][] outside={{0,0,width,py},{0,py+ph,width,height-py-ph},{0,py,px,ph},{px+pw,py,width-px-pw,ph}};
        for(int[] r:outside)if(r[2]>0&&r[3]>0)addRenderableWidget(new NotesUi.Surface(r[0],r[1],r[2],r[3],true,this::closePopup,g->{}));
        EditBox search=addRenderableWidget(new EditBox(font,px+7,py+42,pw-14,20,Component.literal(UiText.tr("vr.text.ab6c1224fc05"))));
        search.setMaxLength(100);search.setValue(popup.search);search.setResponder(q->{if(popup!=null){popup.search=q;popup.page=0;refreshPopupRows();}});
        refreshPopupRows();setInitialFocus(search);
    }
    private void refreshPopupRows() {
        overlayRows.forEach(this::removeWidget);overlayRows.clear();if(popup==null)return;
        String query=popup.search.toLowerCase(Locale.ROOT);
        List<Entry> list=popup.entries.stream().filter(e->(e.label()+" "+e.id()).toLowerCase(Locale.ROOT).contains(query)).toList();
        int pages=Math.max(1,(list.size()+popupRows-1)/popupRows);popup.page=Math.max(0,Math.min(popup.page,pages-1));
        for(int i=0;i<popupRows;i++) {
            int index=popup.page*popupRows+i;if(index>=list.size())break;Entry entry=list.get(index);
            var row=new NotesUi.IconButton(px+7,py+66+i*22,pw-14,20,entry.icon(),entry.label(),false,()->{Popup current=popup;popup=null;safe(()->current.choose.accept(entry));rebuild();});overlayRows.add(addRenderableWidget(row));
        }
        int fy=py+ph-24;
        var previous=new NotesUi.ActionButton(px+7,fy,30,18,"<",()->{popup.page--;refreshPopupRows();});previous.active=popup.page>0;
        var next=new NotesUi.ActionButton(px+pw-37,fy,30,18,">",()->{popup.page++;refreshPopupRows();});next.active=popup.page+1<pages;
        overlayRows.add(addRenderableWidget(previous));overlayRows.add(addRenderableWidget(next));
        var close=new NotesUi.ActionButton(px+43,fy,pw-86,18,UiText.tr("vr.text.b808f6e97075"),()->{popup=null;rebuild();});overlayRows.add(addRenderableWidget(close));
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta) {
        super.extractRenderState(g,mx,my,delta);
        if(popup==null){
            int tx=titleStartX();String hi=headerIcon();if(hi!=null&&!hi.isBlank()){NotesUi.icon(g,hi,tx,top+7,24);tx+=31;}
            String name=displayedTitle();NotesUi.titleText(g,font,name,tx,top+14,NotesUi.TEXT,Math.max(20,left+wide-12-tx));
            for(Label label:labels)NotesUi.smallText(g,font,label.text(),label.x(),label.y(),label.color(),false,Math.max(10,left+wide-12-label.x()));
            if(!problem.isBlank())NotesUi.smallText(g,font,problem,left+14,top+tall-34,0xFFFF7777,true,wide-28);
        } else {
            NotesUi.titleText(g,font,popup.label,px+9,py+14,NotesUi.TEXT,pw-18);
        }
    }
}
