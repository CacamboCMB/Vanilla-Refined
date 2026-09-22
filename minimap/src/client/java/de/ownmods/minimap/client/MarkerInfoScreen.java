package de.ownmods.minimap.client;
import de.ownmods.settings.client.UiText;
import de.ownmods.notes.client.NotesBridge;import de.ownmods.notes.core.Note;import net.minecraft.client.gui.screens.Screen;import net.minecraft.network.chat.Component;
final class MarkerInfoScreen extends Screen{
    final WorldMapScreen parent;final Note note;
    MarkerInfoScreen(WorldMapScreen p,Note n){super(Component.literal(n.title));parent=p;note=n;}
    @Override protected void init(){int x=width/2-180,y=height/2-120;
        addRenderableWidget(new MapUi.Action(x+15,y+128,100,20,UiText.tr("vr.text.3c4fcec821a8"),0xFF5EA35A,()->{parent.session.target(note.id);minecraft.gui.setScreen(parent);}));
        addRenderableWidget(new MapUi.Action(x+130,y+128,100,20,UiText.tr("vr.text.57699aa37189"),0xFF9D78C3,()->{parent.showOnly(note.id);minecraft.gui.setScreen(parent);}));
        addRenderableWidget(new MapUi.Action(x+245,y+128,100,20,UiText.tr("vr.text.e8fcfbdb1fe1"),0xFF9D78C3,()->{try{minecraft.gui.setScreen(NotesBridge.open(parent,note.id));}catch(Exception e){minecraft.gui.setScreen(new SimpleMessageScreen(parent,UiText.tr("vr.text.7800bd538c37"),e.getMessage()));}}));
        if(note.object.equals("waypoint"))addRenderableWidget(new MapUi.Action(x+15,y+158,100,20,UiText.tr("vr.text.84e45ee73411"),0xFF4B8FC1,()->minecraft.gui.setScreen(new WaypointEditScreen(parent,note.id,note.title,note.color,note.mob,note.position.dimension(),note.position.x(),note.position.y(),note.position.z()))));
        addRenderableWidget(new MapUi.Action(x+130,y+158,100,20,UiText.tr("vr.text.76cad43f9531"),0xFFB65C52,this::trash));
        addRenderableWidget(new MapUi.Action(x+245,y+158,100,20,UiText.tr("vr.text.548611ce58f7"),0xFF4B8FC1,()->minecraft.gui.setScreen(parent)));
    }
    @Override public void extractRenderState(net.minecraft.client.gui.GuiGraphicsExtractor g,int mx,int my,float d){super.extractRenderState(g,mx,my,d);int x=width/2-180,y=height/2-120;MapUi.raised(g,x,y,360,210,0xFFE3D4B9,de.ownmods.minimap.core.MarkerMath.color(note.color));MapUi.markerIcon(g,x+28,y+28,de.ownmods.minimap.core.MarkerMath.color(note.color),de.ownmods.notes.core.NotesCatalog.icon(note),true);MapUi.text(g,note.title,x+48,y+18,0xFF2C2822);String type=note.object.equals("waypoint")?UiText.tr("vr.text.1533799cc9bd"):note.kind==Note.Kind.GOAL?UiText.tr("vr.text.3c70b4d41981"):UiText.tr("vr.text.bb9b32817015")+note.object;MapUi.text(g,type,x+48,y+40,0xFF5D554A);MapUi.text(g,note.position.dimension(),x+18,y+66,0xFF5D554A);MapUi.text(g,"X "+note.position.x()+"   Y "+note.position.y()+"   Z "+note.position.z(),x+18,y+86,0xFF2C2822);if(minecraft.player!=null&&minecraft.level!=null){var p=minecraft.player.blockPosition();double dist=note.position.dimension().equals(minecraft.level.dimension().identifier().toString())?Math.hypot(note.position.x()-p.getX(),note.position.z()-p.getZ()):Double.NaN;if(Double.isFinite(dist))MapUi.text(g,UiText.tr("vr.text.2b1d9324066e")+Math.round(dist)+" m",x+18,y+106,0xFF5D554A);}}
    void trash(){try{NotesBridge.trash(note.id);parent.session.refreshNotes();if(parent.session.targetId().equals(note.id))parent.session.target("");minecraft.gui.setScreen(parent);}catch(Exception e){minecraft.gui.setScreen(new SimpleMessageScreen(parent,UiText.tr("vr.text.76cad43f9531"),e.getMessage()));}}
}
