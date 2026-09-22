package de.ownmods.notes.client;
import de.ownmods.settings.client.UiText;

import de.ownmods.notes.core.Note;
import java.util.function.Consumer;
import net.minecraft.client.gui.screens.Screen;

/** Immediate amount editor shown directly after selecting an item in the gallery. */
final class ItemAmountScreen extends NotebookBase {
    private final String item;
    private final boolean autoInventory;
    private final Consumer<Note.Target> apply;
    private final Runnable remove;
    private String wanted;
    private String count;

    ItemAmountScreen(Screen parent,String item,Note.Target existing,boolean autoInventory,Consumer<Note.Target> apply,Runnable remove){
        super(UiText.tr("vr.text.9ba4b5236b33"),parent);this.item=item;this.autoInventory=autoInventory;this.apply=apply;this.remove=remove;
        this.wanted=Integer.toString(existing==null?64:existing.wanted());
        this.count=Integer.toString(existing==null?0:existing.manualCount());
    }
    @Override protected int preferredWidth(){return 470;}
    @Override protected int preferredHeight(){return autoInventory?285:315;}
    @Override protected void build(){
        int x=left+18,w=wide-36,y=bodyY+2;
        addRenderableWidget(new NotesUi.Surface(x,y,72,72,false,()->{},g->{NotesUi.section(g,x,y,72,72);NotesUi.icon(g,item,x+20,y+20,32);}));
        label(NoteEditorScreen.itemName(item),x+86,y+5);
        muted(UiText.tr("vr.text.be5ace8d0aba"),x+86,y+20);
        label(UiText.tr("vr.text.f487ce396018"),x+86,y+42);edit(x+126,y+35,w-126,wanted,4,v->wanted=v);

        int qy=y+82;label(UiText.tr("vr.text.ae43637bbe89"),x,qy+5);
        int[] values={1,16,32,64,128,256,512,1024};int gap=4,bw=(w-gap*7)/8;
        for(int i=0;i<values.length;i++){final int v=values[i];button(x+i*(bw+gap),qy+18,bw,Integer.toString(v),()->{wanted=Integer.toString(v);rebuild();});}
        int yy=qy+48;
        if(!autoInventory){label(UiText.tr("vr.text.17401e008bd4"),x,yy+5);edit(x+128,yy,w-128,count,4,v->count=v);yy+=30;}
        else {muted(UiText.tr("vr.text.4965969b077d"),x,yy+5);yy+=24;}
        if(remove!=null)button(x,yy,w,UiText.tr("vr.text.b16192d2d3ac"),()->{remove.run();minecraft.gui.setScreen(parent);});

        int half=(w-6)/2;button(x,top+tall-30,half,UiText.tr("vr.text.f7ff1178af20"),()->minecraft.gui.setScreen(parent));button(x+half+6,top+tall-30,w-half-6,UiText.tr("vr.text.936378b371b9"),()->{
            int goal=parse(wanted,1,9999,UiText.tr("vr.text.3b5610da7627"));int have=autoInventory?0:parse(count,0,9999,UiText.tr("vr.text.3432849cf92b"));
            apply.accept(new Note.Target(item,goal,have));minecraft.gui.setScreen(parent);
        });
    }
    private int parse(String s,int min,int max,String name){int v;try{v=Integer.parseInt(s.strip());}catch(Exception e){throw new IllegalArgumentException(name+UiText.tr("vr.text.e7e843dc6c3a"));}if(v<min||v>max)throw new IllegalArgumentException(name+UiText.tr("vr.text.c0c8abe9d464")+min+UiText.tr("vr.text.23cc156deffb")+max+UiText.tr("vr.text.baf3f34954d2"));return v;}
}
