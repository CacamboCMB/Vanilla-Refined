package de.ownmods.notes.client;
import de.ownmods.settings.client.UiText;

import de.ownmods.notes.core.Note;
import net.minecraft.client.gui.screens.Screen;

/** First step is only the intent. No form fields are shown before a type is chosen. */
final class NewNoteScreen extends NotebookBase {
    final NotesClient.Session session;
    NewNoteScreen(Screen parent,NotesClient.Session session){super(UiText.tr("vr.text.d2f3a2fec038"),parent);this.session=session;}
    @Override protected void build(){
        int x=left+20,w=wide-40,gap=8,tw=(w-gap)/2,th=72;
        muted(UiText.tr("vr.text.c3d330b4a24b"),x,bodyY-15);
        categoryTile(x,bodyY,tw,th,"minecraft:compass",UiText.tr("vr.text.15b61974b270"),-1,()->false,()->minecraft.gui.setScreen(new NoteTemplateScreen(this,parent,session,Note.Kind.LOCATION)));
        categoryTile(x+tw+gap,bodyY,tw,th,"minecraft:chest",UiText.tr("vr.text.d9bbc90a1d36"),-1,()->false,()->{
            Note n=new Note();n.kind=Note.Kind.COLLECT;n.title=UiText.tr("vr.text.ae12d4205b8e");
            minecraft.gui.setScreen(new NoteEditorScreen(parent,session,n,"material"));
        });
        categoryTile(x,bodyY+th+gap,tw,th,"minecraft:target",UiText.tr("vr.text.f487ce396018"),-1,()->false,()->minecraft.gui.setScreen(new NoteTemplateScreen(this,parent,session,Note.Kind.GOAL)));
        categoryTile(x+tw+gap,bodyY+th+gap,tw,th,"minecraft:writable_book",UiText.tr("vr.text.e61c952e0022"),-1,()->false,()->{
            Note n=new Note();n.kind=Note.Kind.FREE;n.title=UiText.tr("vr.text.d2f3a2fec038");
            minecraft.gui.setScreen(new NoteEditorScreen(parent,session,n,"text"));
        });
        muted(UiText.tr("vr.text.e697cb7c7ebe"),x,bodyY+2*(th+gap)+8);
        button(x,top+tall-30,w,UiText.tr("vr.text.f7ff1178af20"),()->minecraft.gui.setScreen(parent));
    }
}
