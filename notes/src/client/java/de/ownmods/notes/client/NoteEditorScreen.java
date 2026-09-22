package de.ownmods.notes.client;
import de.ownmods.settings.client.UiText;

import de.ownmods.notes.core.*;
import de.ownmods.notes.core.NotesCatalog.Entry;
import java.util.*;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Screenshot-matched editors: location form, collection tracker, goal form and Minecraft book note. */
final class NoteEditorScreen extends NotebookBase {
    final NotesClient.Session session;final Note draft;
    private String mode="details";private int page,bookPage;

    NoteEditorScreen(Screen parent,NotesClient.Session session,Note n){this(parent,session,n,"details");}
    NoteEditorScreen(Screen parent,NotesClient.Session session,Note n,String initialMode){
        super(titleFor(n),parent);this.session=session;draft=n.copy();mode=initialMode;
    }
    private static String titleFor(Note n){return switch(n.kind){case LOCATION->UiText.tr("vr.text.d7a47085ce61");case COLLECT->UiText.tr("vr.text.d9bbc90a1d36");case GOAL->UiText.tr("vr.text.9e8ff0949275");case FREE->UiText.tr("vr.text.e61c952e0022");};}
    @Override protected int preferredWidth(){return 650;}
    @Override protected int preferredHeight(){return 430;}
    @Override protected int minimumUsableWidth(){return 560;}
    @Override protected int minimumUsableHeight(){return 330;}

    @Override protected void build(){
        headerButton(left+wide-34,top+9,24,20,"minecraft:comparator",UiText.tr("vr.text.d0451118a858"),()->minecraft.gui.setScreen(new EditorActionsScreen(this,session,draft)));
        if(mode.equals("checks")){checks();footer();return;}
        if(mode.equals("links")){links();footer();return;}
        if(mode.equals("portal")){minecraft.gui.setScreen(new PortalAssistantScreen(this,draft));mode="details";return;}
        switch(draft.kind){case LOCATION->locationEditor();case COLLECT->collectEditor();case GOAL->goalEditor();case FREE->freeEditor();}
        footer();
    }
    private void footer(){
        int x=left+14,w=wide-28,y=top+tall-30;
        boolean existing=session.store.find(draft.id).isPresent();
        if(existing){
            int gap=6,trashW=Math.max(92,w/5),remain=w-trashW-gap,half=(remain-gap)/2;
            button(x,y,half,UiText.tr("vr.text.f7ff1178af20"),this::onClose);
            button(x+half+gap,y,trashW,UiText.tr("vr.text.76cad43f9531"),()->minecraft.gui.setScreen(new ConfirmScreen(this,trashPrompt(),this::trashDraft,parent)));
            button(x+half+gap+trashW+gap,y,remain-half-gap,UiText.tr("vr.text.f6b2ff39f540"),this::saveAndExit);
        }else{
            int b=(w-6)/2;button(x,y,b,UiText.tr("vr.text.f7ff1178af20"),this::onClose);button(x+b+6,y,w-b-6,UiText.tr("vr.text.f6b2ff39f540"),this::saveAndExit);
        }
    }
    private void saveAndExit(){safe(()->{try{session.save(draft);minecraft.gui.setScreen(parent);}catch(Exception e){throw new IllegalStateException(e.getMessage(),e);}});}
    private String trashPrompt(){String what=switch(draft.kind){case LOCATION->UiText.tr("vr.text.15b61974b270");case COLLECT->UiText.tr("vr.text.a923cd44c3a7");case GOAL->UiText.tr("vr.text.f487ce396018");case FREE->UiText.tr("vr.text.74b487d9d223");};String title=draft.title==null||draft.title.isBlank()?"":(" \""+draft.title+"\"");return what+title+UiText.tr("vr.text.877e7d625b8e");}
    private void trashDraft(){safe(()->{try{draft.trashed=true;session.save(draft);}catch(Exception e){throw new IllegalStateException(e.getMessage(),e);}});}
    @Override public void onClose(){if(closePopup())return;minecraft.gui.setScreen(new DiscardScreen(this,parent));}

    private void locationEditor(){
        normalizeLocationStatus();
        int x=left+14,y=bodyY+4,w=wide-28;int preview=112,formX=x+preview+12,formW=w-preview-12;
        addRenderableWidget(new NotesUi.Surface(x,y,preview,126,false,()->{},g->{NotesUi.thumbnail(g,x,y,preview,126);NotesUi.icon(g,NotesCatalog.icon(draft),x+28,y+14,56);}));
        muted(cleanLabel(NotesCatalog.entry(NotesLabels.objects(),draft.object).label()),x+8,y+84);
        if(draft.position!=null)muted(shortDim(draft.position.dimension())+" · "+draft.position.x()+" / "+draft.position.y()+" / "+draft.position.z(),x+8,y+99);

        int row=23,yy=y;
        label(UiText.tr("vr.text.dcd1d5223f73"),formX,yy+6);edit(formX+72,yy,formW-72,draft.title,100,v->draft.title=v);yy+=row;
        label(UiText.tr("vr.text.0bc70684cf30"),formX,yy+6);choice(formX+72,yy,formW-72,"",NotesLabels.dimensions(),locationDimension(),v->{
            Note.Position p=draft.position==null?new Note.Position(v,0,64,0):new Note.Position(v,draft.position.x(),draft.position.y(),draft.position.z());draft.position=p;
            if(!NotesCatalog.objectFitsDimension(draft.object,v))draft.object="biome";
            if(!draft.biome.isBlank()&&!BiomeVisuals.dimensionFor(draft.biome).equals(v))draft.biome="";
            normalizeLocationStatus();rebuild();
        });yy+=row;
        label(UiText.tr("vr.text.666105122f20"),formX,yy+6);int coordW=(formW-72-78-8)/3;String sx=draft.position==null?"0":""+draft.position.x(),sy=draft.position==null?"64":""+draft.position.y(),sz=draft.position==null?"0":""+draft.position.z();
        edit(formX+72,yy,coordW,sx,10,v->setCoordinate(0,v));edit(formX+76+coordW,yy,coordW,sy,8,v->setCoordinate(1,v));edit(formX+80+2*coordW,yy,coordW,sz,10,v->setCoordinate(2,v));button(formX+formW-74,yy,74,UiText.tr("vr.text.d7763968462d"),()->{var p=NotesClient.here();if(p==null)throw new IllegalStateException(UiText.tr("vr.text.45062d344f76"));draft.position=p;draft.biome=NotesClient.biomeHere();if(!NotesCatalog.objectFitsDimension(draft.object,p.dimension()))draft.object="biome";normalizeLocationStatus();rebuild();});yy+=row;
        label(UiText.tr("vr.text.fe41f9bb1834"),formX,yy+6);choice(formX+72,yy,formW-72,"",NotesLabels.objectsForDimension(locationDimension()),draft.object,v->{draft.object=v;normalizeLocationStatus();rebuild();});yy+=row;
        label(UiText.tr("vr.text.d0d55f76c380"),formX,yy+6);String bio=draft.biome.isBlank()?UiText.tr("vr.text.83fe3439a399"):biomeLabel(draft.biome);String bioIcon=draft.biome.isBlank()?NotesCatalog.entry(NotesLabels.dimensions(),locationDimension()).icon():BiomeVisuals.iconFor(draft.biome);iconButton(formX+72,yy,formW-72,20,bioIcon,bio+"  ›",false,()->minecraft.gui.setScreen(new BiomePickerScreen(this,locationDimension(),draft.biome,v->{String d=BiomeVisuals.dimensionFor(v);draft.biome=v;if(draft.position!=null&&!draft.position.dimension().equals(d))draft.position=new Note.Position(d,draft.position.x(),draft.position.y(),draft.position.z());if(!NotesCatalog.objectFitsDimension(draft.object,d))draft.object="biome";normalizeLocationStatus();})));yy+=row;
        if(draft.object.equals("spawner")){label(UiText.tr("vr.text.15e5c91a8167"),formX,yy+6);choice(formX+72,yy,formW-72,"",NotesClient.spawnerMobs(),draft.mob,v->draft.mob=v);yy+=row;}
        List<NotesCatalog.StatusEntry> statusOptions=NotesLabels.locationStatuses(draft.object);
        if(!statusOptions.isEmpty()){
            var states=statusOptions.stream().map(e->new Entry(e.status().name(),e.label(),e.icon())).toList();label(UiText.tr("vr.text.920e413c7d41"),formX,yy+6);choice(formX+72,yy,formW-72,"",states,draft.status.name(),v->draft.status=Note.Status.valueOf(v));yy+=row;
        }
        var colors=colorEntries();label(UiText.tr("vr.text.a952c8f76f58"),formX,yy+6);choice(formX+72,yy,Math.max(120,formW-176),"",colors,draft.color,v->draft.color=v);tabButton(formX+formW-96,yy,96,20,"minecraft:nether_star",UiText.tr("vr.text.e2325be783c5"),()->draft.favorite,()->{draft.favorite=!draft.favorite;rebuild();});yy+=row+2;

        int suggestionY=y+132;
        if(draft.object.equals("nether_portal"))iconButton(x,suggestionY,preview,22,"minecraft:obsidian",UiText.tr("vr.text.dd24e2388761"),false,()->minecraft.gui.setScreen(new PortalAssistantScreen(this,draft)));

        int notesY=Math.max(y+132,yy);label(UiText.tr("vr.text.7800bd538c37"),formX,notesY+3);int notesX=formX+72,notesH=Math.max(42,Math.min(58,bodyBottom-notesY-4));
        addRenderableWidget(new NotesUi.Surface(notesX,notesY,formW-72,notesH,false,()->{},g->NotesUi.textArea(g,notesX,notesY,formW-72,notesH)));
        int idx=0;var box=MultiLineEditBox.builder().setX(notesX+5).setY(notesY+5).setTextColor(NotesUi.TEXT).setTextShadow(false).setCursorColor(NotesUi.TEXT).setShowBackground(false).setShowDecorations(false).build(font,formW-82,notesH-10,Component.literal(UiText.tr("vr.text.7800bd538c37")));box.setCharacterLimit(4000);box.setValue(draft.pages.get(idx));box.setValueListener(v->draft.pages.set(idx,v));widget(box);
    }

    private String locationDimension(){return draft.position==null?NotesCatalog.OVERWORLD:draft.position.dimension();}
    private void normalizeLocationStatus(){
        var options=NotesLabels.locationStatuses(draft.object);if(options.isEmpty())return;
        if(options.stream().noneMatch(e->e.status()==draft.status))draft.status=options.getFirst().status();
    }
    private String biomeLabel(String id){return NotesClient.biomes(BiomeVisuals.dimensionFor(id)).stream().filter(e->e.id().equals(id)).map(Entry::label).findFirst().orElse(id);}
    private static String cleanLabel(String s){int p=s.indexOf('/');return p>0?s.substring(0,p).strip():s;}

    private void setCoordinate(int axis,String value){
        try{int v=Integer.parseInt(value.strip());Note.Position p=draft.position==null?new Note.Position("minecraft:overworld",0,64,0):draft.position;draft.position=switch(axis){case 0->new Note.Position(p.dimension(),v,p.y(),p.z());case 1->new Note.Position(p.dimension(),p.x(),v,p.z());default->new Note.Position(p.dimension(),p.x(),p.y(),v);};}catch(Exception ignored){}
    }

    private void goalEditor(){
        if(NotesLabels.goalStatuses().stream().noneMatch(e->e.status()==draft.status))draft.status=Note.Status.OPEN;
        int x=left+18,w=wide-36,y=bodyY+6;
        addRenderableWidget(new NotesUi.Surface(x,y,104,118,false,()->{},g->{NotesUi.section(g,x,y,104,118);NotesUi.icon(g,NotesCatalog.icon(draft),x+27,y+18,50);}));
        muted(cleanLabel(NotesCatalog.entry(NotesLabels.goals(),draft.goal).label()),x+7,y+82);
        muted(shortDim(goalDimension()),x+7,y+97);

        int fx=x+118,fw=w-118,yy=y;
        label(UiText.tr("vr.text.dcd1d5223f73"),fx,yy+6);edit(fx+72,yy,fw-72,draft.title,100,v->draft.title=v);yy+=26;
        label(UiText.tr("vr.text.f487ce396018"),fx,yy+6);choice(fx+72,yy,fw-72,"",NotesLabels.goalsForDimension(goalDimension()),draft.goal,v->{draft.goal=v;draft.status=Note.Status.OPEN;rebuild();});yy+=26;
        if(draft.goal.equals("mob_farm")){label(UiText.tr("vr.text.15e5c91a8167"),fx,yy+6);choice(fx+72,yy,fw-72,"",NotesClient.mobs(),draft.mob,v->draft.mob=v);yy+=26;}
        var states=NotesLabels.goalStatuses().stream().map(e->new Entry(e.status().name(),e.label(),e.icon())).toList();
        label(UiText.tr("vr.text.920e413c7d41"),fx,yy+6);choice(fx+72,yy,fw-72,"",states,draft.status.name(),v->draft.status=Note.Status.valueOf(v));yy+=26;
        label(UiText.tr("vr.text.ecf34cc8921a"),fx,yy+6);iconButton(fx+72,yy,fw-72,20,"minecraft:compass",draft.position==null?UiText.tr("vr.text.327148fd002a"):PositionScreen.summary(draft.position),false,()->minecraft.gui.setScreen(new PositionScreen(this,draft.position,p->{draft.position=p;if(!NotesCatalog.goalFitsDimension(draft.goal,p.dimension()))draft.goal="base";})));yy+=28;
        tabButton(fx,yy,fw/2-3,22,"minecraft:nether_star",UiText.tr("vr.text.e2325be783c5"),()->draft.favorite,()->{draft.favorite=!draft.favorite;rebuild();});choice(fx+fw/2+3,yy,fw/2-3,"",colorEntries(),draft.color,v->draft.color=v);

        int ny=Math.max(y+132,yy+4);label(UiText.tr("vr.text.7800bd538c37"),x,ny);int nh=Math.max(48,Math.min(66,bodyBottom-ny-4));
        addRenderableWidget(new NotesUi.Surface(x,ny+14,w,nh-14,false,()->{},g->NotesUi.textArea(g,x,ny+14,w,nh-14)));
        var box=MultiLineEditBox.builder().setX(x+6).setY(ny+20).setTextColor(NotesUi.TEXT).setTextShadow(false).setCursorColor(NotesUi.TEXT).setShowBackground(false).setShowDecorations(false).build(font,w-12,nh-26,Component.literal(UiText.tr("vr.text.a2d13855f508")));box.setCharacterLimit(4000);box.setValue(draft.pages.getFirst());box.setValueListener(v->draft.pages.set(0,v));widget(box);
    }
    private String goalDimension(){
        if(draft.position!=null)return draft.position.dimension();
        var p=NotesClient.here();return p==null?NotesCatalog.OVERWORLD:p.dimension();
    }

    private void collectEditor(){
        int x=left+14,w=wide-28,y=bodyY+4;
        label(draft.title,x,y+3);edit(x+90,y,Math.max(150,w-310),draft.title,100,v->draft.title=v);tabButton(x+w-210,y,102,20,"minecraft:bundle",draft.autoInventory?UiText.tr("vr.text.d602900fb259"):UiText.tr("vr.text.961cacaa6e41"),()->draft.autoInventory,()->{draft.autoInventory=!draft.autoInventory;rebuild();});iconButton(x+w-104,y,104,20,"minecraft:item_frame",UiText.tr("vr.text.e9d2dd0e6912"),false,()->minecraft.gui.setScreen(new ItemPickerScreen(this,draft)));
        Map<String,Integer> inventory=NotesClient.inventory();int listY=y+30,rowH=39,bottom=bodyBottom-3,rows=Math.max(1,(bottom-listY)/rowH),pages=Math.max(1,(draft.targets.size()+rows-1)/rows);page=Math.max(0,Math.min(page,pages-1));
        for(int i=0;i<rows;i++){int index=page*rows+i;if(index>=draft.targets.size())break;var t=draft.targets.get(index);int have=draft.autoInventory?inventory.getOrDefault(t.item(),0):t.manualCount();collectRow(x,listY+i*rowH,w,36,t.item(),itemName(t.item()),have,t.wanted(),have>=t.wanted()?0xFF70A85B:0,()->openTargetAmount(index));}
        if(draft.targets.isEmpty())muted(UiText.tr("vr.text.2ce9861a6d74"),x,listY+9);
        if(pages>1){button(x,bottom-20,28,"<",()->{page--;rebuild();}).active=page>0;button(x+31,bottom-20,28,">",()->{page++;rebuild();}).active=page+1<pages;}
    }

    private void openTargetAmount(int index){
        Note.Target old=draft.targets.get(index);minecraft.gui.setScreen(new ItemAmountScreen(this,old.item(),old,draft.autoInventory,t->draft.targets.set(index,t),()->draft.targets.remove(index)));
    }

    private void freeEditor(){
        int x=left+14,w=wide-28,y=bodyY+3,rightW=146,gap=10,bookW=w-rightW-gap,bh=bodyBottom-y-2;bookPage=Math.max(0,Math.min(bookPage,draft.pages.size()-1));int idx=bookPage;
        addRenderableWidget(new NotesUi.Surface(x,y,bookW,bh,false,()->{},g->NotesUi.book(g,x,y,bookW,bh)));
        edit(x+20,y+16,bookW-40,draft.title,100,v->draft.title=v);
        int textH=Math.max(55,bh/3);var box=MultiLineEditBox.builder().setX(x+20).setY(y+42).setTextColor(NotesUi.INK).setTextShadow(false).setCursorColor(NotesUi.INK).setShowBackground(false).setShowDecorations(false).build(font,bookW-40,textH,Component.literal(UiText.tr("vr.text.f6f1c0143b27")));box.setCharacterLimit(4000);box.setValue(draft.pages.get(idx));box.setValueListener(v->draft.pages.set(idx,v));widget(box);
        int cy=y+48+textH;int visible=Math.max(2,(bh-(cy-y)-32)/23);for(int i=0;i<visible&&i<draft.checks.size();i++){int ci=i;var c=draft.checks.get(i);tabButton(x+20,cy+i*23,22,20,c.done()?"minecraft:lime_dye":"minecraft:gray_dye","",()->c.done(),()->{draft.checks.set(ci,new Note.Check(c.text(),!c.done()));rebuild();});button(x+46,cy+i*23,bookW-74,c.text(),()->minecraft.gui.setScreen(new TextInputScreen(this,UiText.tr("vr.text.5b915cda8054"),c.text(),v->draft.checks.set(ci,new Note.Check(v,c.done())))));}
        iconButton(x+20,y+bh-26,bookW-40,20,"minecraft:paper",UiText.tr("vr.text.86284c3d8c5b"),false,()->minecraft.gui.setScreen(new TextInputScreen(this,UiText.tr("vr.text.947c8a65c89b"),"",v->{if(draft.checks.size()>=200)throw new IllegalStateException(UiText.tr("vr.text.a1f990645337"));draft.checks.add(new Note.Check(v,false));})));

        int rx=x+bookW+gap;label(UiText.tr("vr.text.e00b871f85fd")+(bookPage+1)+"/"+draft.pages.size(),rx,y+4);button(rx,y+24,36,"<",()->{bookPage--;rebuild();}).active=bookPage>0;button(rx+40,y+24,36,">",()->{bookPage++;rebuild();}).active=bookPage+1<draft.pages.size();button(rx+82,y+24,rightW-82,UiText.tr("vr.text.9565dd363c90"),()->{if(draft.pages.size()>=24)throw new IllegalStateException(UiText.tr("vr.text.969226291a36"));draft.pages.add("");bookPage=draft.pages.size()-1;rebuild();});button(rx,y+48,rightW,UiText.tr("vr.text.ef7d06e78455"),()->minecraft.gui.setScreen(new ConfirmScreen(this,UiText.tr("vr.text.278d951b41c2"),()->{if(draft.pages.size()==1)draft.pages.set(0,"");else draft.pages.remove(bookPage);bookPage=Math.min(bookPage,draft.pages.size()-1);}))); 
        label(UiText.tr("vr.text.04471e2770cf"),rx,y+82);int ly=y+98,shown=Math.min(4,draft.links.size());for(int i=0;i<shown;i++){String id=draft.links.get(i);var n=session.store.find(id);noteCard(rx,ly+i*38,rightW,35,n.map(NotesCatalog::icon).orElse("minecraft:barrier"),n.map(v->v.title).orElse(UiText.tr("vr.text.d0124fb1d921")),n.map(v->NotesLabels.kind(v.kind)).orElse(UiText.tr("vr.text.380f98c64bde")),()->{}).active=false;}
        iconButton(rx,y+bh-54,rightW,22,"minecraft:chain",UiText.tr("vr.text.bf4b997be4d2"),false,()->openLinkPicker());tabButton(rx,y+bh-28,rightW,22,"minecraft:nether_star",UiText.tr("vr.text.e2325be783c5"),()->draft.favorite,()->{draft.favorite=!draft.favorite;rebuild();});
    }

    private void checks(){
        int x=left+18,w=wide-36;label(UiText.tr("vr.text.ab242651b325"),x,bodyY+2);iconButton(x+w-120,bodyY,120,22,"minecraft:paper",UiText.tr("vr.text.86284c3d8c5b"),false,()->minecraft.gui.setScreen(new TextInputScreen(this,UiText.tr("vr.text.947c8a65c89b"),"",v->{if(draft.checks.size()>=200)throw new IllegalStateException(UiText.tr("vr.text.a1f990645337"));draft.checks.add(new Note.Check(v,false));})));
        long done=draft.checks.stream().filter(Note.Check::done).count();muted(done+" / "+draft.checks.size()+UiText.tr("vr.text.49fe37e18af2"),x,bodyY+24);int listY=bodyY+39,rows=Math.max(1,(bodyBottom-listY-4)/28),pages=Math.max(1,(draft.checks.size()+rows-1)/rows);page=Math.max(0,Math.min(page,pages-1));for(int i=0;i<rows;i++){int index=page*rows+i;if(index>=draft.checks.size())break;var c=draft.checks.get(index);int yy=listY+i*28;tabButton(x,yy,28,24,c.done()?"minecraft:lime_dye":"minecraft:gray_dye","",()->c.done(),()->{draft.checks.set(index,new Note.Check(c.text(),!c.done()));rebuild();});button(x+32,yy,w-62,c.text(),()->minecraft.gui.setScreen(new TextInputScreen(this,UiText.tr("vr.text.5b915cda8054"),c.text(),v->draft.checks.set(index,new Note.Check(v,c.done())))));button(x+w-26,yy,26,"–",()->{draft.checks.remove(index);rebuild();});}if(pages>1)nav(page,pages,bodyBottom-20,p->{page=p;rebuild();});
    }
    private void links(){int x=left+18,w=wide-36;label(UiText.tr("vr.text.b7d4e83b83a8"),x,bodyY+2);iconButton(x+w-160,bodyY,160,22,"minecraft:chain",UiText.tr("vr.text.331c34965d84"),false,this::openLinkPicker);int listY=bodyY+34,rows=Math.max(1,(bodyBottom-listY-4)/38),pages=Math.max(1,(draft.links.size()+rows-1)/rows);page=Math.max(0,Math.min(page,pages-1));for(int i=0;i<rows;i++){int index=page*rows+i;if(index>=draft.links.size())break;String id=draft.links.get(index);var n=session.store.find(id);int yy=listY+i*38;noteCard(x,yy,w-30,35,n.map(NotesCatalog::icon).orElse("minecraft:barrier"),n.map(v->v.title).orElse(UiText.tr("vr.text.a08f478ce25d")),n.map(v->NotesLabels.kind(v.kind)).orElse(UiText.tr("vr.text.380f98c64bde")),()->{var target=session.store.find(id).orElseThrow(()->new IllegalArgumentException(UiText.tr("vr.text.1d922ebb3078")));minecraft.gui.setScreen(new NoteEditorScreen(this,session,target));});button(x+w-26,yy,26,"–",()->{draft.links.remove(id);rebuild();});}if(pages>1)nav(page,pages,bodyBottom-20,p->{page=p;rebuild();});}
    private void openLinkPicker(){var entries=session.store.all().stream().filter(n->!n.trashed&&!n.id.equals(draft.id)&&!draft.links.contains(n.id)).map(n->new Entry(n.id,n.title,NotesCatalog.icon(n))).toList();pick(UiText.tr("vr.text.8ca3f93cc45e"),entries,left+wide-210,bodyY+40,190,e->{if(draft.links.size()>=100)throw new IllegalStateException(UiText.tr("vr.text.92c8419a0b64"));draft.links.add(e.id());});}


    private static List<Entry> colorEntries(){return Note.COLORS.stream().map(c->new Entry(c,switch(c){case "red"->UiText.tr("vr.text.2207deb65b86");case "green"->UiText.tr("vr.text.995223df54c6");case "blue"->UiText.tr("vr.text.8d55f705cf2b");case "yellow"->UiText.tr("vr.text.5fa3188cc950");case "purple"->UiText.tr("vr.text.dcb7f576709b");default->UiText.tr("vr.text.8a572015c49b");},"minecraft:"+(c.equals("none")?"paper":c+"_dye"))).toList();}
    private static String statusIcon(Note.Status s){return switch(s){case DONE->"minecraft:lime_dye";case FOUND,EXPLORED,LOOTED->"minecraft:map";case IN_PROGRESS,EXPLORING->"minecraft:clock";default->"minecraft:paper";};}
    static String itemName(String id){var item=net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.parse(id));return item==null||item==net.minecraft.world.item.Items.AIR?id:new net.minecraft.world.item.ItemStack(item).getHoverName().getString();}
    private static String shortDim(String id){return id.endsWith("overworld")?UiText.tr("vr.text.243fb47abea8"):id.endsWith("the_nether")?UiText.tr("vr.text.a5a292152531"):id.endsWith("the_end")?UiText.tr("vr.text.f4db1e48476f"):id;}

    private static final class EditorActionsScreen extends NotebookBase {
        final NotesClient.Session session;final Note draft;
        EditorActionsScreen(Screen parent,NotesClient.Session session,Note draft){super(UiText.tr("vr.text.d0451118a858"),parent);this.session=session;this.draft=draft;}
        @Override protected int preferredWidth(){return 460;}@Override protected int preferredHeight(){return 300;}
        @Override protected void build(){int x=left+18,w=wide-36,y=bodyY+4;tabButton(x,y,w,24,"minecraft:nether_star",draft.favorite?UiText.tr("vr.text.634fa3a6ce05"):UiText.tr("vr.text.247d1676c4cd"),()->draft.favorite,()->{draft.favorite=!draft.favorite;rebuild();});y+=30;iconButton(x,y,w,24,"minecraft:paper",UiText.tr("vr.text.bdc348a05042"),false,()->{var editor=(NoteEditorScreen)parent;editor.mode="checks";editor.page=0;minecraft.gui.setScreen(editor);editor.rebuild();});y+=30;iconButton(x,y,w,24,"minecraft:chain",UiText.tr("vr.text.23bcee566b47"),false,()->{var editor=(NoteEditorScreen)parent;editor.mode="links";editor.page=0;minecraft.gui.setScreen(editor);editor.rebuild();});y+=30;if(draft.kind==Note.Kind.LOCATION&&draft.object.equals("nether_portal")){iconButton(x,y,w,24,"minecraft:obsidian",UiText.tr("vr.text.2c3bc6ea27dc"),false,()->minecraft.gui.setScreen(new PortalAssistantScreen(parent,draft)));y+=30;}button(x,y,w,UiText.tr("vr.text.34ab2e8cba1e"),()->safe(()->{try{Note c=draft.copy();c.trashed=true;session.save(c);minecraft.gui.setScreen(((NoteEditorScreen)parent).parent);}catch(Exception e){throw new IllegalStateException(e.getMessage(),e);}}));button(x,top+tall-30,w,UiText.tr("vr.text.548611ce58f7"),()->minecraft.gui.setScreen(parent));}
    }
    private static final class DiscardScreen extends NotebookBase {final Screen destination;DiscardScreen(Screen editor,Screen destination){super(UiText.tr("vr.text.8821e2d25752"),editor);this.destination=destination;}@Override protected int preferredWidth(){return 460;}@Override protected int preferredHeight(){return 250;}@Override protected void build(){label(UiText.tr("vr.text.9383ed726191"),left+16,bodyY);button(left+16,bodyY+28,(wide-38)/2,UiText.tr("vr.text.4ec78c785dd3"),()->minecraft.gui.setScreen(parent));button(left+22+(wide-38)/2,bodyY+28,(wide-38)/2,UiText.tr("vr.text.594ca889c442"),()->minecraft.gui.setScreen(destination));}}
}
