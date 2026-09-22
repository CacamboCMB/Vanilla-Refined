package de.ownmods.notes.client;
import de.ownmods.settings.client.UiText;

import de.ownmods.notes.core.*;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.client.gui.screens.Screen;

/** Two-step biome picker: choose dimension first, then a visually distinct biome. */
final class BiomePickerScreen extends NotebookBase {
    private String dimension;
    private final String selected;
    private final Consumer<String> apply;
    private String search="";
    private int page;

    BiomePickerScreen(Screen parent,String initialDimension,String selected,Consumer<String> apply){
        super(UiText.tr("vr.text.34f55828538d"),parent);
        this.dimension=initialDimension==null||initialDimension.isBlank()?NotesCatalog.OVERWORLD:initialDimension;
        this.selected=selected==null?"":selected;
        this.apply=apply;
    }
    @Override protected int preferredWidth(){return 650;}
    @Override protected int preferredHeight(){return 430;}
    @Override protected void build(){
        int x=left+14,w=wide-28;
        label(UiText.tr("vr.text.e68662da472b"),x,bodyY-3);
        int dimY=bodyY+11,gap=6,dw=(w-gap*2)/3;
        for(int i=0;i<NotesLabels.dimensions().size();i++){
            var d=NotesLabels.dimensions().get(i);int dx=x+i*(dw+gap);
            tabButton(dx,dimY,dw,28,d.icon(),d.label(),()->dimension.equals(d.id()),()->{dimension=d.id();page=0;rebuild();});
        }
        label(UiText.tr("vr.text.7c1a9680de5f"),x,dimY+39);
        edit(x+w-180,dimY+34,180,search,100,v->search=v);

        String q=search.strip().toLowerCase(Locale.ROOT);
        var list=NotesClient.biomes(dimension).stream().filter(e->!e.id().isBlank()).filter(e->(e.label()+" "+e.id()).toLowerCase(Locale.ROOT).contains(q)).toList();
        int gridY=dimY+58,cols=Math.max(3,Math.min(6,w/88)),cg=5,cellW=(w-cg*(cols-1))/cols,cellH=52;
        int rows=Math.max(1,(bodyBottom-gridY-24)/(cellH+cg));int per=cols*rows,pages=Math.max(1,(list.size()+per-1)/per);page=Math.max(0,Math.min(page,pages-1));
        for(int i=0;i<per;i++){
            int idx=page*per+i;if(idx>=list.size())break;var b=list.get(idx);int bx=x+(i%cols)*(cellW+cg),by=gridY+(i/cols)*(cellH+cg);
            categoryTile(bx,by,cellW,cellH,b.icon(),shortLabel(b.label()),-1,()->selected.equals(b.id()),()->{apply.accept(b.id());minecraft.gui.setScreen(parent);});
        }
        if(list.isEmpty())muted(UiText.tr("vr.text.ae914c45c8b5"),x,gridY+8);
        nav(page,pages,bodyBottom-20,p->{page=p;rebuild();});
        button(x,top+tall-30,w,UiText.tr("vr.text.548611ce58f7"),()->minecraft.gui.setScreen(parent));
    }
    private static String shortLabel(String s){return s.length()>20?s.substring(0,19)+"…":s;}
}
