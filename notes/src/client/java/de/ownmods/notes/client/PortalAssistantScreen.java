package de.ownmods.notes.client;
import de.ownmods.settings.client.UiText;

import de.ownmods.notes.core.Note;
import net.minecraft.client.gui.screens.Screen;

/** Two-column Nether portal coordinate helper, matching the notebook preview layout. */
final class PortalAssistantScreen extends NotebookBase {
    private final Note draft;
    PortalAssistantScreen(Screen parent, Note draft) { super(UiText.tr("vr.text.112750cddaca"), parent); this.draft=draft; }
    @Override protected int preferredWidth(){return 620;}
    @Override protected int preferredHeight(){return 285;}
    @Override protected int minimumUsableWidth(){return 520;}
    @Override protected int minimumUsableHeight(){return 260;}
    @Override protected String headerIcon(){return "minecraft:obsidian";}

    @Override protected void build() {
        int x=left+16,y=bodyY+2,w=wide-32,gap=10,col=(w-gap)/2;
        Note.Position base=draft.position;
        Note.Position other=draft.otherPortal;
        Note.Position overworld=null,nether=null;
        if(base!=null){
            if(base.dimension().equals("minecraft:overworld")) overworld=base;
            if(base.dimension().equals("minecraft:the_nether")) nether=base;
        }
        if(other!=null){
            if(other.dimension().equals("minecraft:overworld")) overworld=other;
            if(other.dimension().equals("minecraft:the_nether")) nether=other;
        }
        if(overworld==null && nether!=null) overworld=nether.portalProjection().orElse(null);
        if(nether==null && overworld!=null) nether=overworld.portalProjection().orElse(null);
        drawColumn(x,y,col,UiText.tr("vr.text.f541c4fc203b"),overworld,nether,true);
        drawColumn(x+col+gap,y,col,UiText.tr("vr.text.20934310915e"),nether,overworld,false);
        button(x,top+tall-30,(w-6)/2,UiText.tr("vr.text.091fb9212e64"),()->minecraft.gui.setScreen(new PositionScreen(this,draft.position,p->{draft.position=p;rebuild();})));
        button(x+(w+6)/2,top+tall-30,(w-6)/2,UiText.tr("vr.text.548611ce58f7"),()->minecraft.gui.setScreen(parent));
    }

    private void drawColumn(int x,int y,int w,String title,Note.Position source,Note.Position target,boolean toNether){
        addRenderableWidget(new NotesUi.Surface(x,y,w,145,false,()->{},g->NotesUi.section(g,x,y,w,145)));
        label(title,x+10,y+10);
        String srcName=toNether?UiText.tr("vr.text.06ce5eeb4fdd"):UiText.tr("vr.text.52aee3f792f1");
        String dstName=toNether?UiText.tr("vr.text.b55d6e3dd198"):UiText.tr("vr.text.01a6621ad1e3");
        muted(srcName,x+10,y+31);
        if(source==null){muted(UiText.tr("vr.text.00a9bd5596b7"),x+10,y+46);}else{
            label("X  "+source.x()+"     Y  "+source.y()+"     Z  "+source.z(),x+10,y+46);
        }
        muted(dstName,x+10,y+72);
        if(target==null){muted(UiText.tr("vr.text.ca32e62b4c09"),x+10,y+87);}else{
            label("X  "+target.x()+"     Y  "+target.y()+"     Z  "+target.z(),x+10,y+87);
            button(x+10,y+111,w-20,UiText.tr("vr.text.5a5a4395dada"),()->{
                if(toNether){draft.position=source;draft.otherPortal=target;}
                else {draft.position=target;draft.otherPortal=source;}
                rebuild();
            });
        }
    }
}
