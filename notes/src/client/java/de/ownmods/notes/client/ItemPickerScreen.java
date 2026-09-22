package de.ownmods.notes.client;
import de.ownmods.settings.client.UiText;

import de.ownmods.notes.core.*;
import java.util.*;
import net.minecraft.client.gui.screens.Screen;

/** Item gallery with immediate quantity editing. No ItemStack is moved; only notebook data changes. */
final class ItemPickerScreen extends NotebookBase {
    private final Note draft;
    private final List<NotesCatalog.Entry> catalog=NotesClient.items();
    private String search="";private int page;
    ItemPickerScreen(Screen parent,Note draft) {super(UiText.tr("vr.text.5ca0a7544abf"),parent);this.draft=draft;}
    @Override protected int preferredWidth(){return 680;}
    @Override protected int preferredHeight(){return 440;}
    @Override protected void build() {
        int x=left+14,w=wide-28;
        label(UiText.tr("vr.text.6f835b201915"),x,bodyY-3);
        edit(x+83,bodyY-8,Math.max(160,w-270),search,100,s->search=s);
        button(x+w-98,bodyY-8,98,UiText.tr("vr.text.dd097e77dd73"),()->{page=0;rebuild();});
        muted(UiText.tr("vr.text.9c2df8cbbe26")+draft.targets.size()+UiText.tr("vr.text.9882f328121a"),x,bodyY+18);

        String q=search.toLowerCase(Locale.ROOT);
        var items=catalog.stream().filter(e->(e.label()+" "+e.id()).toLowerCase(Locale.ROOT).contains(q)).toList();
        int gridY=bodyY+36,columns=Math.max(4,Math.min(8,w/72)),gap=5,cellW=(w-gap*(columns-1))/columns,cellH=57;
        int rows=Math.max(1,(bodyBottom-gridY-24)/(cellH+gap)),perPage=columns*rows;
        int pages=Math.max(1,(items.size()+perPage-1)/perPage);page=Math.max(0,Math.min(page,pages-1));
        for(int i=0;i<perPage;i++) {
            int index=page*perPage+i;if(index>=items.size())break;var e=items.get(index);int tx=x+(i%columns)*(cellW+gap),ty=gridY+(i/columns)*(cellH+gap);
            itemTile(tx,ty,cellW,cellH,e.icon(),shortLabel(e.label()),()->quantity(e.id()),()->openAmount(e));
        }
        nav(page,pages,bodyBottom-20,p->{page=p;rebuild();});
        button(x,top+tall-30,w,UiText.tr("vr.text.25d52d7e8665"),()->minecraft.gui.setScreen(parent));
    }
    private int quantity(String id){return draft.targets.stream().filter(t->t.item().equals(id)).findFirst().map(Note.Target::wanted).orElse(0);}
    private void openAmount(NotesCatalog.Entry e){
        Note.Target old=draft.targets.stream().filter(t->t.item().equals(e.id())).findFirst().orElse(null);
        minecraft.gui.setScreen(new ItemAmountScreen(this,e.id(),old,draft.autoInventory,t->{
            int idx=indexOf(e.id());if(idx>=0)draft.targets.set(idx,t);else {if(draft.targets.size()>=256)throw new IllegalStateException(UiText.tr("vr.text.b60755328356"));draft.targets.add(t);}
        },old==null?null:()->{int idx=indexOf(e.id());if(idx>=0)draft.targets.remove(idx);}));
    }
    private int indexOf(String id){for(int i=0;i<draft.targets.size();i++)if(draft.targets.get(i).item().equals(id))return i;return -1;}
    private static String shortLabel(String s){return s.length()>16?s.substring(0,15)+"…":s;}
}
