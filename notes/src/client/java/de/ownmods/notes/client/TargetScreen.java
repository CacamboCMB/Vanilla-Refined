package de.ownmods.notes.client;
import de.ownmods.settings.client.UiText;
import de.ownmods.notes.core.Note;
import net.minecraft.client.gui.screens.Screen;
final class TargetScreen extends NotebookBase {
    final Note draft;final int index;final String id;String wanted,count;
    TargetScreen(Screen parent,Note draft,int index){super(UiText.tr("vr.text.9ba4b5236b33"),parent);this.draft=draft;this.index=index;var t=draft.targets.get(index);id=t.item();wanted=""+t.wanted();count=""+t.manualCount();}
    @Override protected void build(){
        int x=left+18,w=wide-36;
        label(NoteEditorScreen.itemName(id),x,top+33);
        label(UiText.tr("vr.text.2969570b7d6a"),x,bodyY+6);edit(x+w/2,bodyY,w/2,wanted,4,v->wanted=v);
        label(UiText.tr("vr.text.889649fb9274"),x,bodyY+32);edit(x+w/2,bodyY+26,w/2,count,4,v->count=v);
        label(UiText.tr("vr.text.de75c284b45c"),x,bodyY+59);
        button(x,bodyY+79,w,UiText.tr("vr.text.c4934efdefc4"),()->{draft.targets.remove(index);minecraft.gui.setScreen(parent);});
        button(x,top+tall-30,(w-6)/2,UiText.tr("vr.text.f7ff1178af20"),()->minecraft.gui.setScreen(parent));
        button(x+(w+6)/2,top+tall-30,(w-6)/2,UiText.tr("vr.text.936378b371b9"),()->{draft.targets.set(index,new Note.Target(id,Integer.parseInt(wanted.strip()),Integer.parseInt(count.strip())));minecraft.gui.setScreen(parent);});
    }
}
