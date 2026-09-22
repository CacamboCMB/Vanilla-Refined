package de.ownmods.settings.client;

import de.ownmods.settings.api.ManagedMod;
import de.ownmods.settings.api.ManagedMods;
import de.ownmods.settings.layout.RefinedLayout;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.Comparator;
import java.util.List;

/** Shared Notes-style dashboard. All tile and footer positions use scaled GUI dimensions. */
public final class ModsScreen extends Screen {
    private static final List<String> ORDER=List.of("ownmods_timber","ownmods_cropreplant",
            "ownmods_shulkerpreview","ownmods_inventorysort","ownmods_mousewheel","ownmods_zoom",
            "ownmods_gravestone","ownmods_toolswap","ownmods_notes","ownmods_minimap");
    private final Screen parent;
    private int page,filter;
    private RefinedLayout.Dashboard layout;
    public ModsScreen(Screen parent,int page){super(Component.translatable("ownmods.title"));this.parent=parent;this.page=page;}
    @Override protected void init(){
        clearWidgets();
        layout=RefinedLayout.dashboard(width,height);
        if(width<320||height<240){
            addRenderableWidget(new OwnModsUi.Surface(4,4,width-8,height-8,g->{
                OwnModsUi.window(g,4,4,width-8,height-8);
                OwnModsUi.label(g,UiText.tr("vr.ui.resize"),12,54,width-24,OwnModsUi.BODY_TEXT);
            }));
            addRenderableWidget(new OwnModsUi.ActionButton(12,height-36,width-24,24,"minecraft:arrow",UiText.tr("vr.ui.back"),OwnModsUi.BLUE,false,this::onClose));
            return;
        }
        var l=layout;
        addRenderableWidget(new OwnModsUi.Surface(l.x(),l.y(),l.w(),l.h(),g->OwnModsUi.window(g,l.x(),l.y(),l.w(),l.h())));
        addRenderableWidget(new OwnModsUi.Surface(l.x()+3,l.y()+40,l.sidebar()-3,l.h()-43,
                g->OwnModsUi.sidebar(g,l.x()+3,l.y()+40,l.sidebar()-3,l.h()-43)));
        addRenderableWidget(new OwnModsUi.Surface(l.contentX()-3,l.contentY()-2,l.contentW()+6,l.contentH()+6,
                g->OwnModsUi.content(g,l.contentX()-3,l.contentY()-2,l.contentW()+6,l.contentH()+6)));
        addRenderableWidget(new OwnModsUi.Surface(l.x()+7,l.y()+7,l.w()-14,30,this::header));
        String[] keys={"vr.dashboard.all","vr.dashboard.enabled","vr.dashboard.disabled"};
        String[] icons={"minecraft:chest","minecraft:lime_dye","minecraft:gray_dye"};
        for(int i=0;i<keys.length;i++){
            final int f=i;
            addRenderableWidget(new OwnModsUi.SidebarButton(l.x()+6,l.y()+49+i*34,l.sidebar()-12,30,
                    icons[i],UiText.tr(keys[i]),OwnModsUi.SELECTED,()->filter==f,()->{filter=f;page=0;init();}));
        }
        List<ManagedMod> mods=ManagedMods.all().stream()
                .filter(m->filter==0||(filter==1?ServerSettingsClient.flag(m,"enabled",m.enabled()):!ServerSettingsClient.flag(m,"enabled",m.enabled())))
                .sorted(Comparator.comparingInt((ManagedMod m)->{int i=ORDER.indexOf(m.id());return i<0?1000:i;})
                        .thenComparing(ManagedMod::id)).toList();
        final int pages=Math.max(1,(mods.size()+l.perPage()-1)/l.perPage());
        page=Math.max(0,Math.min(page,pages-1));
        addRenderableWidget(new OwnModsUi.Surface(l.contentX()+8,l.contentY()+8,l.contentW()-16,14,
                g->OwnModsUi.label(g,UiText.tr("vr.dashboard.count",mods.size()),l.contentX()+8,l.contentY()+8,l.contentW()-16,OwnModsUi.BODY_MUTED)));
        for(int i=page*l.perPage();i<Math.min(mods.size(),(page+1)*l.perPage());i++){
            var m=mods.get(i);int offset=i-page*l.perPage();
            addRenderableWidget(new OwnModsUi.ModuleCard(l.contentX()+(offset%l.columns())*(l.tileW()+8),
                    l.contentY()+26+(offset/l.columns())*(l.tileH()+8),l.tileW(),l.tileH(),m.iconItemId(),
                    Component.translatable(m.titleKey()),Component.translatable(m.descriptionKey()),()->ServerSettingsClient.flag(m,"enabled",m.enabled()),()->openMod(m)));
        }
        addRenderableWidget(new OwnModsUi.ActionButton(l.x()+7,l.footerY(),l.sidebar()-14,24,"minecraft:arrow",UiText.tr("vr.ui.back"),OwnModsUi.BLUE,true,this::onClose));
        if(pages>1){
            var prev=addRenderableWidget(new OwnModsUi.ActionButton(l.contentX(),l.footerY(),70,24,"minecraft:arrow",UiText.tr("vr.ui.previous"),OwnModsUi.BLUE,false,()->{page--;init();}));prev.active=page>0;
            var next=addRenderableWidget(new OwnModsUi.ActionButton(l.contentX()+l.contentW()-70,l.footerY(),70,24,"minecraft:arrow",UiText.tr("vr.ui.next"),OwnModsUi.BLUE,false,()->{page++;init();}));next.active=page+1<pages;
            addRenderableWidget(new OwnModsUi.Surface(l.contentX()+72,l.footerY(),l.contentW()-144,24,
                    g->OwnModsUi.smallText(g,(page+1)+" / "+pages,l.contentX()+l.contentW()/2-15,l.footerY()+8,OwnModsUi.BODY_TEXT,l.contentW()-144)));
        }
    }
    private void header(GuiGraphicsExtractor g){
        var l=layout;OwnModsUi.icon(g,"minecraft:compass",l.x()+12,l.y()+9,24);
        OwnModsUi.label(g,title.getString(),l.x()+45,l.y()+10,l.w()-58,OwnModsUi.TEXT);
        OwnModsUi.smallText(g,Component.translatable("ownmods.dashboard.subtitle").getString(),l.x()+45,l.y()+24,OwnModsUi.MUTED,l.w()-58);
    }
    private void openMod(ManagedMod mod){
        if(!mod.customScreenClass().isBlank())try{
            Class<?> type=Class.forName(mod.customScreenClass());
            minecraft.gui.setScreen(type.asSubclass(Screen.class).getConstructor(Screen.class).newInstance(this));return;
        }catch(ReflectiveOperationException|LinkageError e){de.ownmods.settings.SettingsMod.LOGGER.error("Cannot open settings for {}",mod.id(),e);}
        minecraft.gui.setScreen(new IconSettingsScreen(this,mod));
    }
    @Override public void onClose(){minecraft.gui.setScreen(parent);}
    @Override public boolean isPauseScreen(){return true;}
}
