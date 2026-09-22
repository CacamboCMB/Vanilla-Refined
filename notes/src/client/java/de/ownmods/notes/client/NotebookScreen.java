package de.ownmods.notes.client;
import de.ownmods.settings.client.UiText;
import de.ownmods.settings.client.compat.InputCompat;

import de.ownmods.notes.core.*;
import de.ownmods.notes.core.NotesCatalog.Entry;
import java.util.*;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;


/** Screenshot-matched notebook dashboard with a persistent left navigation and compact content lists. */
public final class NotebookScreen extends NotebookBase {
    private NotesClient.Session session;
    private boolean attempted;
    private String query="";
    private String view="home";
    private String dimension="all";
    private boolean distanceOrder=true;
    private String collectCategory="all";
    private String goalCategory="all";
    private int page;
    private EditBox searchBox;

    public NotebookScreen(Screen parent){super(UiText.tr("vr.text.7800bd538c37"),parent);}
    @Override protected String headerIcon(){return switch(view){case "LOCATION"->"@village";case "COLLECT"->"minecraft:chest";case "GOAL"->"minecraft:target";case "FREE"->"minecraft:writable_book";case "favorites"->"minecraft:nether_star";case "trash"->"minecraft:lava_bucket";default->"minecraft:writable_book";};}
    @Override protected String displayedTitle(){return switch(view){case "LOCATION"->UiText.tr("vr.text.95c61170318a");case "COLLECT"->UiText.tr("vr.text.d9bbc90a1d36");case "GOAL"->UiText.tr("vr.text.ddbe8fd7d4ee");case "FREE"->UiText.tr("vr.text.63b63723f10a");case "favorites"->UiText.tr("vr.text.b4f83e2e6b86");case "trash"->UiText.tr("vr.text.76cad43f9531");default->UiText.tr("vr.text.7800bd538c37");};}
    @Override protected int preferredWidth(){return 650;}
    @Override protected int preferredHeight(){return 430;}
    @Override protected int minimumUsableWidth(){return 560;}
    @Override protected int minimumUsableHeight(){return 330;}

    @Override protected void build() {
        searchBox=null;
        if(!attempted){attempted=true;safe(()->{try{session=new NotesClient.Session();}catch(Exception e){throw new IllegalStateException(e.getMessage(),e);}});}
        if(session==null) {
            label(UiText.tr("vr.text.4a127667939c"),left+16,bodyY);
            muted(UiText.tr("vr.text.e41af5a29dc0"),left+16,bodyY+18);
            button(left+15,top+tall-30,wide-30,UiText.tr("vr.text.548611ce58f7"),()->minecraft.gui.setScreen(parent));
            return;
        }
        if(!NotesClient.settings.enabled()) {
            label(UiText.tr("vr.text.2887750aeb27"),left+16,bodyY);
            button(left+15,bodyY+25,wide-30,UiText.tr("vr.text.3f2fdef83cb2"),()->safe(()->{try{NotesClient.settings.save(Map.of("enabled",true));rebuild();}catch(Exception e){throw new IllegalStateException(e);}}));
            button(left+15,top+tall-30,wide-30,UiText.tr("vr.text.548611ce58f7"),()->minecraft.gui.setScreen(parent));
            return;
        }

        final int side=128;
        final int sx=left+3, sy=top+40, sh=tall-43;
        final int mx=sx+side, my=sy, mw=wide-side-6, mh=sh;
        addRenderableWidget(new NotesUi.Surface(sx,sy,side,sh,false,()->{},g->NotesUi.sidebar(g,sx,sy,side,sh)));
        addRenderableWidget(new NotesUi.Surface(mx,my,mw,mh,false,()->{},g->NotesUi.content(g,mx,my,mw,mh)));

        int searchW=166;int searchX=left+wide-searchW-39;
        headerButton(searchX,top+9,20,20,"minecraft:spyglass",UiText.tr("vr.text.95e827d1f133"),()->{page=0;rebuild();});
        searchBox=edit(searchX+20,top+9,searchW-20,query,100,v->query=v);
        headerButton(left+wide-34,top+9,24,20,"minecraft:comparator",UiText.tr("vr.text.f5750a5d7231"),()->minecraft.gui.setScreen(new NotebookSettingsScreen(this)));

        int ny=sy+4,nh=29;
        navButton(sx,ny,side,nh,"@home",UiText.tr("vr.text.4d2bb368f919"),"home");ny+=nh;
        navButton(sx,ny,side,nh,"@village",UiText.tr("vr.text.95c61170318a"),"LOCATION");ny+=nh;
        navButton(sx,ny,side,nh,"minecraft:chest",UiText.tr("vr.text.d9bbc90a1d36"),"COLLECT");ny+=nh;
        navButton(sx,ny,side,nh,"minecraft:target",UiText.tr("vr.text.ddbe8fd7d4ee"),"GOAL");ny+=nh;
        navButton(sx,ny,side,nh,"minecraft:writable_book",UiText.tr("vr.text.63b63723f10a"),"FREE");
        sidebarButton(sx,sy+sh-60,side,28,"minecraft:nether_star",UiText.tr("vr.text.b4f83e2e6b86"),()->view.equals("favorites"),()->{view="favorites";page=0;rebuild();});
        sidebarButton(sx,sy+sh-31,side,28,"minecraft:lava_bucket",UiText.tr("vr.text.76cad43f9531"),()->view.equals("trash"),()->{view="trash";page=0;rebuild();});

        switch(view){
            case "home"->dashboard(mx+12,my+10,mw-24,mh-20);
            case "LOCATION"->locations(mx+12,my+10,mw-24,mh-20,false);
            case "COLLECT"->collect(mx+12,my+10,mw-24,mh-20,false);
            case "GOAL"->goals(mx+12,my+10,mw-24,mh-20,false);
            case "FREE"->freeNotes(mx+12,my+10,mw-24,mh-20,false);
            case "favorites"->favorites(mx+12,my+10,mw-24,mh-20);
            case "trash"->trash(mx+12,my+10,mw-24,mh-20);
            default->{view="home";dashboard(mx+12,my+10,mw-24,mh-20);}
        }
    }

    private void navButton(int x,int y,int w,int h,String icon,String label,String id){sidebarButton(x,y,w,h,icon,label,()->view.equals(id),()->{view=id;page=0;rebuild();});}

    @Override public boolean keyPressed(KeyEvent event) {
        if(searchBox!=null && searchBox.isFocused() && (event.key()==InputCompat.code("KEY_RETURN") || event.key()==InputCompat.code("KEY_NUMPADENTER"))) {
            page=0;
            rebuild();
            return true;
        }
        return super.keyPressed(event);
    }

    private void dashboard(int x,int y,int w,int h) {
        label(UiText.tr("vr.text.5c79c2d61628"),x,y+2);
        int gap=8,tw=(w-gap*3)/4,th=94,ty=y+18;
        iconButton(x,ty,tw,th,"minecraft:compass",UiText.tr("vr.text.82bdf537621d"),true,()->minecraft.gui.setScreen(new NoteTemplateScreen(this,this,session,Note.Kind.LOCATION)));
        iconButton(x+(tw+gap),ty,tw,th,"minecraft:chest",UiText.tr("vr.text.f85e8a55b86e"),true,()->{Note n=new Note();n.kind=Note.Kind.COLLECT;n.title=UiText.tr("vr.text.ae12d4205b8e");minecraft.gui.setScreen(new NoteEditorScreen(this,session,n,"material"));});
        iconButton(x+2*(tw+gap),ty,tw,th,"minecraft:target",UiText.tr("vr.text.d11b99220105"),true,()->minecraft.gui.setScreen(new NoteTemplateScreen(this,this,session,Note.Kind.GOAL)));
        iconButton(x+3*(tw+gap),ty,tw,th,"minecraft:writable_book",UiText.tr("vr.text.7a5136e6a263"),true,()->{Note n=new Note();n.kind=Note.Kind.FREE;n.title=UiText.tr("vr.text.d2f3a2fec038");minecraft.gui.setScreen(new NoteEditorScreen(this,session,n,"text"));});

        int ry=ty+th+18;label(UiText.tr("vr.text.f15f5b8b7e73"),x,ry);ry+=15;
        int rowH=36;
        int maxRows=Math.max(1,(y+h-ry-2)/(rowH+2));
        List<Note> recent=session.store.all().stream().filter(n->!n.trashed).filter(this::matchesSearch)
                .sorted(Comparator.comparing((Note n)->!n.favorite).thenComparing(Comparator.comparingLong((Note n)->n.updated).reversed())).limit(maxRows).toList();
        for(Note n:recent){var card=noteCard(x,ry,w-29,rowH,NotesCatalog.icon(n),n.title,dashboardSubtitle(n),()->minecraft.gui.setScreen(new NoteEditorScreen(this,session,n)));card.accent=NotesUi.color(n.color);starButton(x+w-27,ry,27,rowH,()->n.favorite,()->toggleFavorite(n));ry+=rowH+2;}
        if(recent.isEmpty())muted(UiText.tr("vr.text.1bcc640ec9c9"),x,ry+5);
    }

    private void locations(int x,int y,int w,int h,boolean favOnly) {
        label(favOnly?UiText.tr("vr.text.09c661a0172f"):UiText.tr("vr.text.95c61170318a"),x,y+2);
        List<Entry> dims=new ArrayList<>();dims.add(new Entry("all",UiText.tr("vr.text.8bbb9dddfd55"),"minecraft:map"));dims.addAll(NotesLabels.dimensions());
        choice(x+w-300,y,w>450?150:135,"",dims,dimension,v->{dimension=v;page=0;rebuild();});
        iconButton(x+w-145,y,145,20,"minecraft:recovery_compass",distanceOrder?UiText.tr("vr.text.986f2838cfc8"):UiText.tr("vr.text.cf7f9dcc4a16"),false,()->{distanceOrder=!distanceOrder;page=0;rebuild();});
        var here=NotesClient.here();Comparator<Note> order=distanceOrder?Comparator.comparingDouble((Note n)->n.position==null?Double.POSITIVE_INFINITY:n.position.distance(here)).thenComparing(n->n.title):Comparator.comparingLong((Note n)->n.updated).reversed();
        List<Note> notes=session.store.all().stream().filter(n->!n.trashed&&n.kind==Note.Kind.LOCATION).filter(n->!favOnly||n.favorite)
                .filter(n->dimension.equals("all")||n.position!=null&&n.position.dimension().equals(dimension)).filter(this::matchesSearch).sorted(order).toList();
        int listY=y+30,rowH=43,actionY=y+h-20,navY=actionY-23,listBottom=navY-3;
        int rows=Math.max(1,(listBottom-listY)/rowH),pages=Math.max(1,(notes.size()+rows-1)/rows);page=Math.max(0,Math.min(page,pages-1));
        for(int i=0;i<rows;i++){
            int index=page*rows+i;if(index>=notes.size())break;Note n=notes.get(index);int yy=listY+i*rowH;String dist=distanceText(n,here);
            var card=locationRow(x,yy,w-60,40,NotesCatalog.icon(n),n.title,locationSubtitle(n),dist,()->minecraft.gui.setScreen(new NoteEditorScreen(this,session,n)));card.accent=NotesUi.color(n.color);
            starButton(x+w-58,yy,28,40,()->n.favorite,()->toggleFavorite(n));
            iconOnlyButton(x+w-28,yy,28,40,"minecraft:lava_bucket",UiText.tr("vr.text.a1a6cd67d113"),NotesUi.DANGER,()->trashLocation(n));
        }
        if(notes.isEmpty())muted(UiText.tr("vr.text.47d1fc71203c"),x,listY+9);
        if(pages>1){button(x,navY,28,"<",()->{page--;rebuild();}).active=page>0;button(x+31,navY,28,">",()->{page++;rebuild();}).active=page+1<pages;muted((page+1)+" / "+pages,x+66,navY+6);}
        button(x,actionY,w,UiText.tr("vr.text.a351726aab5e"),()->minecraft.gui.setScreen(new NoteTemplateScreen(this,this,session,Note.Kind.LOCATION)));
    }

    private record CollectEntry(Note note,int targetIndex,Note.Target target){}
    private void collect(int x,int y,int w,int h,boolean favOnly) {
        label(favOnly?UiText.tr("vr.text.e434fedd30ed"):UiText.tr("vr.text.d9bbc90a1d36"),x,y+2);
        String[] ids={"all","blocks","items","raw","food","redstone"};String[] names={UiText.tr("vr.text.3801a35c5182"),UiText.tr("vr.text.c49522c2de81"),UiText.tr("vr.text.fb8e7a1a3cb7"),UiText.tr("vr.text.7cf68da9ae50"),UiText.tr("vr.text.7fb6dbef1dad"),UiText.tr("vr.text.2175a59f2ac5")};
        int tabY=y+22,gap=3,tw=(w-gap*(ids.length-1))/ids.length;
        for(int i=0;i<ids.length;i++){final String id=ids[i];var tab=tabButton(x+i*(tw+gap),tabY,tw,21,"",names[i],()->collectCategory.equals(id),()->{collectCategory=id;page=0;rebuild();});tab.accent=NotesUi.COLLECT;}
        List<CollectEntry> entries=new ArrayList<>();for(Note n:session.store.all())if(!n.trashed&&n.kind==Note.Kind.COLLECT&&(!favOnly||n.favorite)&&matchesSearch(n))for(int i=0;i<n.targets.size();i++){var t=n.targets.get(i);if(collectCategory.equals("all")||collectCategory(t.item()).equals(collectCategory))entries.add(new CollectEntry(n,i,t));}
        int listY=tabY+27,rowH=43,actionY=y+h-20,navY=actionY-23,listBottom=navY-3;
        int rows=Math.max(1,(listBottom-listY)/rowH),pages=Math.max(1,(entries.size()+rows-1)/rows);page=Math.max(0,Math.min(page,pages-1));Map<String,Integer> inv=NotesClient.inventory();
        for(int i=0;i<rows;i++){int index=page*rows+i;if(index>=entries.size())break;CollectEntry e=entries.get(index);int yy=listY+i*rowH;int have=e.note.autoInventory?inv.getOrDefault(e.target.item(),0):e.target.manualCount();collectRow(x,yy,w-60,40,e.target.item(),NoteEditorScreen.itemName(e.target.item()),have,e.target.wanted(),NotesUi.color(e.note.color),()->minecraft.gui.setScreen(new NoteEditorScreen(this,session,e.note,"material")));starButton(x+w-58,yy,28,40,()->e.note.favorite,()->toggleFavorite(e.note));iconOnlyButton(x+w-28,yy,28,40,"minecraft:lava_bucket",UiText.tr("vr.text.1632dad9b59d"),NotesUi.DANGER,()->trashNote(e.note));}
        if(entries.isEmpty())muted(UiText.tr("vr.text.12942f4f579d"),x,listY+9);
        if(pages>1){button(x,navY,28,"<",()->{page--;rebuild();}).active=page>0;button(x+31,navY,28,">",()->{page++;rebuild();}).active=page+1<pages;muted((page+1)+" / "+pages,x+66,navY+6);}
        int half=(w-6)*3/5;button(x,actionY,half,UiText.tr("vr.text.efeb7dd20288"),this::openCollectEditor);button(x+half+6,actionY,w-half-6,UiText.tr("vr.text.49a66ad2f4c4"),this::toggleCollectCounters);
    }

    private void goals(int x,int y,int w,int h,boolean favOnly) {
        label(favOnly?UiText.tr("vr.text.06149b337b66"):UiText.tr("vr.text.ddbe8fd7d4ee"),x,y+2);
        String[] ids={"all","farm","base","tech","other"};String[] names={UiText.tr("vr.text.3801a35c5182"),UiText.tr("vr.text.a92021748145"),UiText.tr("vr.text.7b47361aad19"),UiText.tr("vr.text.fc7c52801403"),UiText.tr("vr.text.9f3d5f8d94cf")};int tabY=y+22,gap=3,tw=(w-gap*4)/5;
        for(int i=0;i<ids.length;i++){final String id=ids[i];var tab=tabButton(x+i*(tw+gap),tabY,tw,21,"",names[i],()->goalCategory.equals(id),()->{goalCategory=id;page=0;rebuild();});tab.accent=NotesUi.GOAL;}
        List<Note> notes=session.store.all().stream().filter(n->!n.trashed&&n.kind==Note.Kind.GOAL).filter(n->!favOnly||n.favorite).filter(this::matchesSearch).filter(n->goalCategory.equals("all")||goalCategory(n.goal).equals(goalCategory)).sorted(Comparator.comparingLong((Note n)->n.updated).reversed()).toList();
        int listY=tabY+27,rowH=43,actionY=y+h-20,navY=actionY-23,listBottom=navY-3;
        int rows=Math.max(1,(listBottom-listY)/rowH),pages=Math.max(1,(notes.size()+rows-1)/rows);page=Math.max(0,Math.min(page,pages-1));
        for(int i=0;i<rows;i++){int index=page*rows+i;if(index>=notes.size())break;Note n=notes.get(index);int yy=listY+i*rowH;goalRow(x,yy,w-60,40,NotesCatalog.icon(n),n.title,goalCategoryLabel(n.goal),NotesLabels.status(n),NotesUi.color(n.color),()->minecraft.gui.setScreen(new NoteEditorScreen(this,session,n)));starButton(x+w-58,yy,28,40,()->n.favorite,()->toggleFavorite(n));iconOnlyButton(x+w-28,yy,28,40,"minecraft:lava_bucket",UiText.tr("vr.text.e6d9fa63931b"),NotesUi.DANGER,()->trashNote(n));}
        if(notes.isEmpty())muted(UiText.tr("vr.text.ae07859639a2"),x,listY+9);
        if(pages>1){button(x,navY,28,"<",()->{page--;rebuild();}).active=page>0;button(x+31,navY,28,">",()->{page++;rebuild();}).active=page+1<pages;muted((page+1)+" / "+pages,x+66,navY+6);}
        button(x,actionY,w,UiText.tr("vr.text.b35ee86b6e8a"),()->minecraft.gui.setScreen(new NoteTemplateScreen(this,this,session,Note.Kind.GOAL)));
    }

    private void freeNotes(int x,int y,int w,int h,boolean favOnly) {
        label(favOnly?UiText.tr("vr.text.583b341193a2"):UiText.tr("vr.text.63b63723f10a"),x,y+2);
        List<Note> notes=session.store.all().stream().filter(n->!n.trashed&&n.kind==Note.Kind.FREE).filter(n->!favOnly||n.favorite).filter(this::matchesSearch).sorted(Comparator.comparingLong((Note n)->n.updated).reversed()).toList();
        int listY=y+23,rowH=43,actionY=y+h-20,navY=actionY-23,listBottom=navY-3;int rows=Math.max(1,(listBottom-listY)/rowH),pages=Math.max(1,(notes.size()+rows-1)/rows);page=Math.max(0,Math.min(page,pages-1));
        for(int i=0;i<rows;i++){int index=page*rows+i;if(index>=notes.size())break;Note n=notes.get(index);int yy=listY+i*rowH;var card=noteCard(x,yy,w-60,40,"minecraft:writable_book",n.title,freeSubtitle(n),()->minecraft.gui.setScreen(new NoteEditorScreen(this,session,n,"text")));card.accent=NotesUi.color(n.color);starButton(x+w-58,yy,28,40,()->n.favorite,()->toggleFavorite(n));iconOnlyButton(x+w-28,yy,28,40,"minecraft:lava_bucket",UiText.tr("vr.text.63b53909cd4b"),NotesUi.DANGER,()->trashNote(n));}
        if(notes.isEmpty())muted(UiText.tr("vr.text.d7877cdf4802"),x,listY+9);
        if(pages>1){button(x,navY,28,"<",()->{page--;rebuild();}).active=page>0;button(x+31,navY,28,">",()->{page++;rebuild();}).active=page+1<pages;muted((page+1)+" / "+pages,x+66,navY+6);}
        button(x,actionY,w,UiText.tr("vr.text.73359f19189d"),()->{Note n=new Note();n.kind=Note.Kind.FREE;n.title=UiText.tr("vr.text.d2f3a2fec038");minecraft.gui.setScreen(new NoteEditorScreen(this,session,n,"text"));});
    }

    private void favorites(int x,int y,int w,int h) {
        label(UiText.tr("vr.text.b4f83e2e6b86"),x,y+2);
        List<Note> notes=session.store.all().stream().filter(n->!n.trashed&&n.favorite).filter(this::matchesSearch).sorted(Comparator.comparingLong((Note n)->n.updated).reversed()).toList();
        int listY=y+23,rowH=43,navY=y+h-20,listBottom=navY-3;int rows=Math.max(1,(listBottom-listY)/rowH),pages=Math.max(1,(notes.size()+rows-1)/rows);page=Math.max(0,Math.min(page,pages-1));
        for(int i=0;i<rows;i++){int index=page*rows+i;if(index>=notes.size())break;Note n=notes.get(index);int yy=listY+i*rowH;var card=noteCard(x,yy,w-60,40,NotesCatalog.icon(n),n.title,dashboardSubtitle(n),()->minecraft.gui.setScreen(new NoteEditorScreen(this,session,n)));card.accent=NotesUi.color(n.color);starButton(x+w-58,yy,28,40,()->true,()->toggleFavorite(n));iconOnlyButton(x+w-28,yy,28,40,"minecraft:lava_bucket",UiText.tr("vr.text.a1a6cd67d113"),NotesUi.DANGER,()->trashNote(n));}
        if(notes.isEmpty())muted(UiText.tr("vr.text.ea60809c6220"),x,listY+9);
        if(pages>1){button(x,navY,28,"<",()->{page--;rebuild();}).active=page>0;button(x+31,navY,28,">",()->{page++;rebuild();}).active=page+1<pages;muted((page+1)+" / "+pages,x+66,navY+6);}
    }

    private void trash(int x,int y,int w,int h) {
        label(UiText.tr("vr.text.76cad43f9531"),x,y+2);muted(UiText.tr("vr.text.c6450eae4449"),x+72,y+2);
        List<Note> notes=session.store.all().stream().filter(n->n.trashed).filter(this::matchesSearch).sorted(Comparator.comparingLong((Note n)->n.updated).reversed()).toList();
        int listY=y+23,rowH=43,navY=y+h-20,listBottom=navY-3;int rows=Math.max(1,(listBottom-listY)/rowH),pages=Math.max(1,(notes.size()+rows-1)/rows);page=Math.max(0,Math.min(page,pages-1));
        for(int i=0;i<rows;i++){int index=page*rows+i;if(index>=notes.size())break;Note n=notes.get(index);int yy=listY+i*rowH;noteCard(x,yy,w-126,40,NotesCatalog.icon(n),n.title,NotesLabels.kind(n.kind),()->{}).active=false;button(x+w-122,yy+2,60,UiText.tr("vr.text.548611ce58f7"),()->restore(n));button(x+w-59,yy+2,59,UiText.tr("vr.text.6c2d352161df"),()->permanentDelete(n));}
        if(notes.isEmpty())muted(UiText.tr("vr.text.325395d7cc6a"),x,listY+9);
        if(pages>1){button(x,navY,28,"<",()->{page--;rebuild();}).active=page>0;button(x+31,navY,28,">",()->{page++;rebuild();}).active=page+1<pages;muted((page+1)+" / "+pages,x+66,navY+6);}
    }

    private boolean matchesSearch(Note n){if(query==null||query.isBlank())return true;String extra=NotesCatalog.entry(NotesLabels.objects(),n.object).label()+" "+NotesCatalog.entry(NotesLabels.goals(),n.goal).label();return n.matches(query)||extra.toLowerCase(Locale.ROOT).contains(query.strip().toLowerCase(Locale.ROOT));}
    private String dashboardSubtitle(Note n){return switch(n.kind){case LOCATION->locationSubtitle(n);case COLLECT->UiText.tr("vr.text.56e79f801796")+n.progressPercent(NotesClient.inventory())+" %";case GOAL->UiText.tr("vr.text.9b94724f2be3")+NotesLabels.status(n);case FREE->UiText.tr("vr.text.e61c952e0022")+(n.checks.isEmpty()?"":" · "+n.checks.stream().filter(Note.Check::done).count()+"/"+n.checks.size()+" ✓");};}
    private String locationSubtitle(Note n){return n.position==null?NotesLabels.status(n):shortDim(n.position.dimension())+" · "+n.position.x()+"  "+n.position.y()+"  "+n.position.z();}
    private String freeSubtitle(Note n){return UiText.tr(n.pages.size()==1?"vr.note.page_one":"vr.note.page_many",n.pages.size())+(n.checks.isEmpty()?"":" · "+n.checks.stream().filter(Note.Check::done).count()+"/"+n.checks.size()+UiText.tr("vr.text.49fe37e18af2"));}
    private static String shortDim(String id){return id.endsWith("overworld")?UiText.tr("vr.text.243fb47abea8"):id.endsWith("the_nether")?UiText.tr("vr.text.a5a292152531"):id.endsWith("the_end")?UiText.tr("vr.text.f4db1e48476f"):id;}
    private String distanceText(Note n,Note.Position here){if(n.position==null||here==null)return "";double d=n.position.distance(here);if(!Double.isFinite(d))return "";if(d<1000)return Math.round(d)+" m";return String.format(Locale.ROOT,"%.1f km",d/1000.0).replace('.',',');}
    private static String collectCategory(String id){String s=id.toLowerCase(Locale.ROOT);if(s.contains("redstone")||s.contains("repeater")||s.contains("comparator")||s.contains("observer")||s.contains("piston")||s.contains("dispenser")||s.contains("dropper")||s.contains("lever"))return "redstone";if(s.contains("wheat")||s.contains("carrot")||s.contains("potato")||s.contains("beetroot")||s.contains("apple")||s.contains("bread")||s.contains("berry")||s.contains("pork")||s.contains("beef")||s.contains("chicken")||s.contains("mutton")||s.contains("fish"))return "food";if(s.contains("ingot")||s.contains("nugget")||s.contains("diamond")||s.contains("emerald")||s.contains("coal")||s.contains("quartz")||s.contains("lapis")||s.contains("raw_"))return "raw";if(s.contains("_block")||s.contains("stone")||s.contains("brick")||s.contains("planks")||s.contains("log")||s.contains("glass")||s.contains("wool")||s.contains("concrete"))return "blocks";return "items";}
    private static String goalCategory(String id){if(id.equals("base"))return "base";if(Set.of("super_smelter","storage","bartering","trading_hall").contains(id))return "tech";if(id.equals("other"))return "other";return "farm";}
    private static String goalCategoryLabel(String id){return switch(goalCategory(id)){case "base"->UiText.tr("vr.text.7b47361aad19");case "tech"->UiText.tr("vr.text.fc7c52801403");case "other"->UiText.tr("vr.text.9f3d5f8d94cf");default->UiText.tr("vr.text.3457b1617fe5");};}

    private void toggleFavorite(Note n){safe(()->{try{Note c=n.copy();c.favorite=!c.favorite;session.save(c);rebuild();}catch(Exception e){throw new IllegalStateException(e.getMessage(),e);}});}
    private void trashLocation(Note n){trashNote(n);}
    private void trashNote(Note n){
        String what=switch(n.kind){case LOCATION->UiText.tr("vr.text.15b61974b270");case COLLECT->UiText.tr("vr.text.a923cd44c3a7");case GOAL->UiText.tr("vr.text.f487ce396018");case FREE->UiText.tr("vr.text.74b487d9d223");};
        String title=n.title==null||n.title.isBlank()?"":(" \""+n.title+"\"");
        minecraft.gui.setScreen(new ConfirmScreen(this,what+title+UiText.tr("vr.text.877e7d625b8e"),()->safe(()->{try{Note c=n.copy();c.trashed=true;session.save(c);page=0;rebuild();}catch(Exception e){throw new IllegalStateException(e.getMessage(),e);}})));
    }
    private void restore(Note n){safe(()->{try{Note c=n.copy();c.trashed=false;session.save(c);rebuild();}catch(Exception e){throw new IllegalStateException(e.getMessage(),e);}});}
    private void permanentDelete(Note n){minecraft.gui.setScreen(new ConfirmScreen(this,UiText.tr("vr.text.444f4af13e10"),()->safe(()->{try{session.delete(n.id);rebuild();}catch(Exception e){throw new IllegalStateException(e.getMessage(),e);}})));}
    private void openCollectEditor(){List<Note> c=session.store.all().stream().filter(n->!n.trashed&&n.kind==Note.Kind.COLLECT).sorted(Comparator.comparingLong((Note n)->n.updated).reversed()).toList();if(c.isEmpty()){Note n=new Note();n.kind=Note.Kind.COLLECT;n.title=UiText.tr("vr.text.d06c845fd212");minecraft.gui.setScreen(new NoteEditorScreen(this,session,n,"material"));}else minecraft.gui.setScreen(new NoteEditorScreen(this,session,c.getFirst(),"material"));}
    private void toggleCollectCounters(){safe(()->{try{for(Note n:session.store.all())if(!n.trashed&&n.kind==Note.Kind.COLLECT){Note c=n.copy();c.autoInventory=!c.autoInventory;session.save(c);}rebuild();}catch(Exception e){throw new IllegalStateException(e.getMessage(),e);}});}
}
