package de.ownmods.notes.client;
import de.ownmods.settings.client.UiText;

import de.ownmods.notes.core.*;
import java.util.*;
import net.minecraft.client.gui.screens.Screen;

/** Dimension-first icon gallery. Impossible/illogical structures and farms are hidden from the chosen dimension. */
final class NoteTemplateScreen extends NotebookBase {
    final NotesClient.Session session;final Note.Kind kind;final Screen destination;
    String search="";int page;String dimension;
    NoteTemplateScreen(Screen parent,Screen destination,NotesClient.Session session,Note.Kind kind){
        super(kind==Note.Kind.LOCATION?UiText.tr("vr.text.1f9d7a854a58"):UiText.tr("vr.text.6e8fb0fe5cb1"),parent);this.destination=destination;this.session=session;this.kind=kind;
        var here=NotesClient.here();dimension=here==null?NotesCatalog.OVERWORLD:here.dimension();
        if(!Set.of(NotesCatalog.OVERWORLD,NotesCatalog.NETHER,NotesCatalog.END).contains(dimension))dimension=NotesCatalog.OVERWORLD;
    }
    @Override protected int preferredWidth(){return 680;}
    @Override protected int preferredHeight(){return 440;}
    @Override protected void build(){
        int x=left+14,w=wide-28;
        label(UiText.tr("vr.text.e68662da472b"),x,bodyY-3);
        int dimY=bodyY+11,gap=6,dw=(w-gap*2)/3;
        for(int i=0;i<NotesLabels.dimensions().size();i++){
            var d=NotesLabels.dimensions().get(i);int dx=x+i*(dw+gap);
            tabButton(dx,dimY,dw,27,d.icon(),d.label(),()->dimension.equals(d.id()),()->{dimension=d.id();page=0;rebuild();});
        }
        label(kind==Note.Kind.LOCATION?UiText.tr("vr.text.7e4a3f57c158"):UiText.tr("vr.text.768e90bb54a6"),x,dimY+39);
        edit(x+w-180,dimY+34,180,search,100,v->search=v);
        muted(contextHint(),x,dimY+52);

        var source=kind==Note.Kind.LOCATION?NotesLabels.objectsForDimension(dimension):NotesLabels.goalsForDimension(dimension);
        String q=search.strip().toLowerCase(Locale.ROOT);
        var entries=source.stream().filter(e->(e.label()+" "+e.id()).toLowerCase(Locale.ROOT).contains(q)).toList();
        int gridY=dimY+68,cols=Math.max(3,Math.min(6,w/88)),cg=5,cellW=(w-cg*(cols-1))/cols,cellH=54;
        int rows=Math.max(1,(bodyBottom-gridY-23)/(cellH+cg)),per=cols*rows,pages=Math.max(1,(entries.size()+per-1)/per);page=Math.max(0,Math.min(page,pages-1));
        for(int i=0;i<per;i++){
            int index=page*per+i;if(index>=entries.size())break;var e=entries.get(index);int cx=x+(i%cols)*(cellW+cg),cy=gridY+(i/cols)*(cellH+cg);
            categoryTile(cx,cy,cellW,cellH,e.icon(),shortLabel(e.label()),-1,()->false,()->safe(()->choose(e)));
        }
        nav(page,pages,bodyBottom-20,p->{page=p;rebuild();});
        button(x,top+tall-30,w,UiText.tr("vr.text.548611ce58f7"),()->minecraft.gui.setScreen(parent));
    }
    private String contextHint(){
        String d=NotesCatalog.entry(NotesLabels.dimensions(),dimension).label();
        return kind==Note.Kind.LOCATION?UiText.tr("vr.text.616c7a33004e")+d+UiText.tr("vr.text.5fdc71d53a76"):UiText.tr("vr.text.506a6ce70669")+d+UiText.tr("vr.text.bcff62ea7826");
    }
    private void choose(NotesCatalog.Entry e){
        Note n=new Note();n.kind=kind;n.title=cleanTitle(e.label());
        var here=NotesClient.here();Note.Position pos=(here!=null&&here.dimension().equals(dimension))?here:new Note.Position(dimension,0,64,0);
        if(kind==Note.Kind.LOCATION){
            n.object=e.id();n.position=pos;n.biome=(here!=null&&here.dimension().equals(dimension))?NotesClient.biomeHere():"";
            // Start structures as unvisited, portals as planned, spawners as found only after the user decides.
            n.status=Note.Status.OPEN;
            minecraft.gui.setScreen(new NoteEditorScreen(destination,session,n,"details"));
        } else {
            n.goal=e.id();n.status=Note.Status.OPEN;
            if(here!=null&&here.dimension().equals(dimension))n.position=here;
            minecraft.gui.setScreen(new NoteEditorScreen(destination,session,n,"details"));
        }
    }
    private static String shortLabel(String s){int p=s.indexOf('/');String t=p>0?s.substring(0,p).strip():s;return t.length()>20?t.substring(0,19)+"…":t;}
    private static String cleanTitle(String s){int p=s.indexOf('/');return p>0?s.substring(0,p).strip():s;}
}
