package de.ownmods.notes.client;
import de.ownmods.settings.client.UiText;
import net.minecraft.client.gui.screens.Screen;
final class ConfirmScreen extends NotebookBase {
    final Runnable confirmed;
    final Screen confirmedDestination;
    ConfirmScreen(Screen parent,String title,Runnable confirmed){this(parent,title,confirmed,parent);}
    ConfirmScreen(Screen parent,String title,Runnable confirmed,Screen confirmedDestination){super(title,parent);this.confirmed=confirmed;this.confirmedDestination=confirmedDestination;}
    @Override protected void build(){
        label(UiText.tr("vr.text.965466118bd8"),left+18,bodyY);
        button(left+18,bodyY+28,(wide-42)/2,UiText.tr("vr.text.f7ff1178af20"),()->minecraft.gui.setScreen(parent));
        button(left+24+(wide-42)/2,bodyY+28,(wide-42)/2,UiText.tr("vr.text.15b63b4de303"),()->{confirmed.run();minecraft.gui.setScreen(confirmedDestination);});
    }
}
