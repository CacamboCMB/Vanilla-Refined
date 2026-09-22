package de.ownmods.notes.client;
import de.ownmods.settings.client.UiText;
import java.util.function.Consumer;
import net.minecraft.client.gui.screens.Screen;
final class TextInputScreen extends NotebookBase {
    String value;final Consumer<String> apply;
    TextInputScreen(Screen parent,String title,String value,Consumer<String> apply){super(title,parent);this.value=value;this.apply=apply;}
    @Override protected void build(){
        edit(left+18,bodyY,wide-36,value,240,v->value=v);
        button(left+18,top+tall-30,(wide-42)/2,UiText.tr("vr.text.f7ff1178af20"),()->minecraft.gui.setScreen(parent));
        button(left+24+(wide-42)/2,top+tall-30,(wide-42)/2,UiText.tr("vr.text.936378b371b9"),()->{if(value.isBlank())throw new IllegalArgumentException(UiText.tr("vr.text.a9cebada5b72"));apply.accept(value);minecraft.gui.setScreen(parent);});
    }
}
