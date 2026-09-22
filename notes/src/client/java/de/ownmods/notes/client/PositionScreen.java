package de.ownmods.notes.client;
import de.ownmods.settings.client.UiText;
import de.ownmods.notes.core.*;
import java.util.function.Consumer;
import net.minecraft.client.gui.screens.Screen;

final class PositionScreen extends NotebookBase {
    String dimension,x,y,z;final Consumer<Note.Position> apply;
    PositionScreen(Screen parent,Note.Position p,Consumer<Note.Position> apply) {
        super(UiText.tr("vr.text.6d031af10da7"),parent);this.apply=apply;
        if(p==null)p=NotesClient.here();if(p==null)p=new Note.Position("minecraft:overworld",0,64,0);
        dimension=p.dimension();x=""+p.x();y=""+p.y();z=""+p.z();
    }
    static String summary(Note.Position p){return NotesCatalog.entry(NotesLabels.dimensions(),p.dimension()).label()+" · "+p.x()+" / "+p.y()+" / "+p.z();}
    @Override protected void build() {
        int l=left+18,w=wide-36;
        choice(l,bodyY,w,UiText.tr("vr.text.86cc893120c7"),NotesLabels.dimensions(),dimension,v->dimension=v);
        int third=(w-8)/3;label("X",l,bodyY+28);label("Y",l+third+4,bodyY+28);label("Z",l+(third+4)*2,bodyY+28);
        edit(l,bodyY+40,third,x,10,v->x=v);edit(l+third+4,bodyY+40,third,y,8,v->y=v);edit(l+(third+4)*2,bodyY+40,third,z,10,v->z=v);
        iconButton(l,bodyY+67,w,20,"minecraft:compass",UiText.tr("vr.text.bf7f539c395a"),false,()->{
            var pos=NotesClient.here();if(pos==null)throw new IllegalStateException(UiText.tr("vr.text.45062d344f76"));
            dimension=pos.dimension();x=""+pos.x();y=""+pos.y();z=""+pos.z();rebuild();
        });
        button(l,top+tall-30,(w-6)/2,UiText.tr("vr.text.f7ff1178af20"),()->minecraft.gui.setScreen(parent));
        button(l+(w+6)/2,top+tall-30,(w-6)/2,UiText.tr("vr.text.936378b371b9"),()->{
            var pos=new Note.Position(dimension,Integer.parseInt(x.strip()),Integer.parseInt(y.strip()),Integer.parseInt(z.strip()));apply.accept(pos);minecraft.gui.setScreen(parent);
        });
    }
}
