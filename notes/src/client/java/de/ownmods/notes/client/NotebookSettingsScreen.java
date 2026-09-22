package de.ownmods.notes.client;
import de.ownmods.settings.client.UiText;

import java.util.Map;
import net.minecraft.client.gui.screens.Screen;

/** Small notebook-only settings page opened by the gear in the screenshot-style header. */
final class NotebookSettingsScreen extends NotebookBase {
    NotebookSettingsScreen(Screen parent){super(UiText.tr("vr.text.bf90768a5a9f"),parent);}
    @Override protected String headerIcon(){return "minecraft:comparator";}
    @Override protected int preferredWidth(){return 470;}
    @Override protected int preferredHeight(){return 260;}
    @Override protected void build(){
        int x=left+18,w=wide-36;
        tabButton(x,bodyY,w,26,"minecraft:writable_book",NotesClient.settings.enabled()?UiText.tr("vr.text.52fcfe6e20be"):UiText.tr("vr.text.3c23bc0d0c84"),NotesClient.settings::enabled,()->safe(()->{
            try{NotesClient.settings.save(Map.of("enabled",!NotesClient.settings.enabled()));rebuild();}catch(Exception e){throw new IllegalStateException(e.getMessage(),e);}
        }));
        muted(UiText.tr("vr.text.4b7bf72d1ddb"),x,bodyY+36);
        button(x,top+tall-30,w,UiText.tr("vr.text.548611ce58f7"),()->minecraft.gui.setScreen(parent));
    }
}
