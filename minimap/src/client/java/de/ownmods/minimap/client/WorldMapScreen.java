package de.ownmods.minimap.client;
import de.ownmods.settings.client.compat.InputCompat;
import de.ownmods.settings.client.OwnModsUi;
import de.ownmods.settings.client.UiText;
import de.ownmods.settings.layout.RefinedLayout;

import de.ownmods.minimap.core.ExploredMap;
import de.ownmods.minimap.core.MarkerMath;
import de.ownmods.notes.client.NotesBridge;
import de.ownmods.notes.core.Note;
import de.ownmods.notes.core.NotesCatalog;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;


import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Large atlas screen. The map itself is a cached DynamicTexture: opening M no longer emits one GUI
 * rectangle per map pixel. Filters are compact, interactive and never share space with the footer.
 */
public final class WorldMapScreen extends Screen {
    // 0.11.1a: Marker-Filterzustand fuer MapFilterScreen / MarkerInfoScreen.
    private static final java.util.Set<String> MAP_FILTER_CATEGORIES = java.util.Set.of(
        "waypoint", "deathpoint", "homebase", "village", "farm", "structure",
        "spawner", "portal", "biome", "other"
    );
    private String onlyMarkerId = null;
    enum MarkerMode { ALL, TARGET, NONE }

    private record MarkerHit(Note note, int x, int y) {}
    private record CategoryDef(String id, String label, String icon, int accent) {
        public String label() { return UiText.catalog(label); }
    }

    private static final List<CategoryDef> CATEGORY_DEFS = List.of(
            new CategoryDef("waypoint", "Wegpunkte", "minecraft:compass", MapUi.CYAN),
            new CategoryDef("deathpoint", "Tod", "minecraft:skeleton_skull", MapUi.RED),
            new CategoryDef("homebase", "Basen", "minecraft:red_bed", 0xFFD0634F),
            new CategoryDef("village", "Dörfer", "minecraft:bell", MapUi.GOLD),
            new CategoryDef("farm", "Farmen", "minecraft:wheat", 0xFF6FA558),
            new CategoryDef("structure", "Strukturen", "minecraft:stone_bricks", 0xFF8A8176),
            new CategoryDef("spawner", "Spawner", "minecraft:spawner", 0xFF4D86B8),
            new CategoryDef("portal", "Portale", "minecraft:obsidian", MapUi.PURPLE),
            new CategoryDef("biome", "Biome", "minecraft:grass_block", 0xFF5D8E52),
            new CategoryDef("other", "Sonstiges", "minecraft:paper", 0xFF9B8870)
    );

    private static final List<String> COLORS = List.of("red", "green", "blue", "yellow", "purple");
    private static final List<String> ICONS = List.of(
            "minecraft:map", "minecraft:compass", "minecraft:lodestone", "minecraft:target",
            "minecraft:red_bed", "minecraft:iron_pickaxe", "minecraft:obsidian", "minecraft:spawner",
            "minecraft:wheat", "minecraft:chest", "minecraft:skeleton_skull", "minecraft:diamond",
            "minecraft:nether_star", "minecraft:bell"
    );

    final MinimapSession session;
    private final Screen parent;
    private final MapRasterTexture mapTexture = new MapRasterTexture("worldmap");

    private String dim;
    private int centerX, centerZ, blocksPerPixel = 1;
    private int shellX, shellY, shellW, shellH, headerH;
    private int sideX, sideY, sideW, sideH;
    private int infoX, infoY, infoW, infoH;
    private int mapX, mapY, mapW, mapH;

    private MarkerMode markerMode = MarkerMode.ALL;
    private final Set<String> categories = new LinkedHashSet<>(List.of(
            "waypoint", "deathpoint", "homebase", "village", "farm",
            "structure", "spawner", "portal", "biome", "other"));
    private final List<MarkerHit> hits = new ArrayList<>();

    private Note selected;
    private boolean editingWaypoint;
    private String editId, editName = UiText.tr("vr.text.1533799cc9bd"), editColor = "blue", editIcon = "minecraft:map", editDimension;
    private int editX, editY, editZ;
    private EditBox nameBox, searchBox;
    private String searchQuery = "";
    private String notice = "";
    private boolean favoriteOnly;
    private boolean showPlayers;
    private boolean dockedInfo, infoOpen;
    private int categoryPage, iconPage;

    WorldMapScreen(Screen parent, MinimapSession s) {
        super(Component.literal(UiText.tr("vr.text.e730154ddc48")));
        this.parent = parent;
        session = s;
        showPlayers = MinimapClient.settings != null && MinimapClient.settings.flag("players");
        var mc = net.minecraft.client.Minecraft.getInstance();
        dim = mc.level == null ? "minecraft:overworld" : mc.level.dimension().identifier().toString();
        if (mc.player != null) {
            centerX = mc.player.blockPosition().getX();
            centerZ = mc.player.blockPosition().getZ();
        } else {
            session.map(dim).blockBounds().ifPresent(b -> {
                centerX = (b[0] + b[2]) / 2;
                centerZ = (b[1] + b[3]) / 2;
            });
        }
    }

    static WorldMapScreen forPlayerPosition(MinimapSession s) {
        WorldMapScreen screen = new WorldMapScreen(null, s);
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player != null && mc.level != null) {
            var p = mc.player.blockPosition();
            screen.beginEditState(null, mc.level.dimension().identifier().toString(), p.getX(), p.getY(), p.getZ());
        }
        return screen;
    }

    boolean mapContains(double x, double y) {
        boolean overInfo = infoW > 0 && x >= infoX && x < infoX + infoW && y >= infoY && y < infoY + infoH;
        return !overInfo && x >= mapX && y >= mapY && x < mapX + mapW && y < mapY + mapH;
    }

    void wheelZoom(double amount, double mouseX, double mouseY) {
        int old = blocksPerPixel;
        int next = amount > 0 ? Math.max(1, old / 2) : Math.min(32, old * 2);
        if (next == old) return;
        int anchorX = centerX + (int)Math.round((mouseX - (mapX + mapW / 2.0)) * old);
        int anchorZ = centerZ + (int)Math.round((mouseY - (mapY + mapH / 2.0)) * old);
        centerX = anchorX - (int)Math.round((mouseX - (mapX + mapW / 2.0)) * next);
        centerZ = anchorZ - (int)Math.round((mouseY - (mapY + mapH / 2.0)) * next);
        blocksPerPixel = next;
        mapTexture.invalidate();
    }

    @Override protected void init() {
        clearWidgets();
        nameBox = null;
        searchBox = null;
        if (width < 320 || height < 240) {
            addRenderableWidget(new OwnModsUi.Surface(4,4,width-8,height-8,g->{
                OwnModsUi.window(g,4,4,width-8,height-8);
                OwnModsUi.label(g,UiText.tr("vr.ui.resize"),12,55,width-24,OwnModsUi.BODY_TEXT);
            }));
            addRenderableWidget(new OwnModsUi.ActionButton(12,height-36,width-24,24,"minecraft:arrow",UiText.tr("vr.ui.back"),OwnModsUi.BLUE,false,this::onClose));
            return;
        }
        var a=RefinedLayout.atlas(width,height,infoOpen||selected!=null||editingWaypoint);
        shellX=a.x();shellY=a.y();shellW=a.w();shellH=a.h();headerH=72;
        sideW=a.sideW();sideX=shellX+3;sideY=a.bodyY();sideH=a.bodyH();
        mapX=a.mapX();mapY=sideY;mapW=a.mapW();mapH=sideH;
        dockedInfo=a.dockedInfo();infoX=a.infoX();infoW=a.infoW();infoY=sideY;infoH=sideH;
        addRenderableWidget(new OwnModsUi.Surface(shellX,shellY,shellW,shellH,
                g->OwnModsUi.window(g,shellX,shellY,shellW,shellH)));
        addRenderableWidget(new OwnModsUi.Surface(shellX+3,shellY+3,shellW-6,36,g->{
            OwnModsUi.icon(g,"minecraft:filled_map",shellX+12,shellY+9,24);
            OwnModsUi.label(g,UiText.tr("vr.map.title"),shellX+43,shellY+10,Math.max(40,shellW-240),OwnModsUi.TEXT);
            if(shellW>=500) OwnModsUi.smallText(g,UiText.tr("vr.map.subtitle"),shellX+43,shellY+24,OwnModsUi.MUTED,shellW-255);
        }));
        addRenderableWidget(new OwnModsUi.Surface(sideX,sideY,sideW,sideH,g->OwnModsUi.sidebar(g,sideX,sideY,sideW,sideH)));
        if (dockedInfo || infoW == 0) {
            addRenderableWidget(new MapUi.Canvas(mapX,mapY,mapW,mapH,this::mapClick,(dx,dy)->{
                centerX-=(int)Math.round(dx*blocksPerPixel);centerZ-=(int)Math.round(dy*blocksPerPixel);
            }){
                @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float d){drawMap(g);}
            });
        }
        buildHeaderButtons();
        buildSidebarButtons();
        // Add the drawer LAST. It must cover the map and receive clicks before the map canvas.
        if(infoW>0){
            addRenderableWidget(new OwnModsUi.Surface(infoX,infoY,infoW,infoH,this::drawInfoPanel));
            buildInfoButtons();
        }
    }

    private void buildHeaderButtons() {
        final int ty=shellY+46, gap=4, closeX=shellX+shellW-31;
        int searchW=Math.min(142,Math.max(80,shellW/5));
        int searchButtonX=closeX-28, searchX=searchButtonX-searchW-4;
        searchBox=addRenderableWidget(new EditBox(font,searchX,shellY+10,searchW,21,Component.literal(UiText.tr("vr.ui.search"))));
        searchBox.setMaxLength(100);searchBox.setValue(searchQuery);searchBox.setResponder(v->searchQuery=v);
        action(searchButtonX,shellY+9,24,23,"minecraft:spyglass","",this::runSearch,"vr.ui.search");
        action(closeX,shellY+9,24,23,"minecraft:barrier","",this::onClose,"vr.ui.close");
        // Dimensions have a separate toolbar. Narrow GUIs cycle a full-width selection
        // instead of squeezing three labels into unreadable, truncated tabs.
        int tx=shellX+9;
        if(shellW<580){
            String icon=dim.endsWith("overworld")?"minecraft:grass_block":dim.endsWith("the_nether")?"minecraft:netherrack":"minecraft:end_stone";
            action(tx,ty,118,22,icon,shortDimension(dim),()->{
                int i=MinimapSession.DIMS.indexOf(dim);switchDimension(MinimapSession.DIMS.get((i+1)%MinimapSession.DIMS.size()));
            },"vr.map.dimension");
        }else{
            int tabW=Math.min(104,(shellW-192)/3);
            for(String d:MinimapSession.DIMS){
                final String dimension=d;
                String icon=d.endsWith("overworld")?"minecraft:grass_block":d.endsWith("the_nether")?"minecraft:netherrack":"minecraft:end_stone";
                String label=shortDimension(d);
                addRenderableWidget(new OwnModsUi.ChoiceButton(tx,ty,tabW,22,icon,label,OwnModsUi.SELECTED,
                        ()->dim.equals(dimension),()->switchDimension(dimension),label));tx+=tabW+gap;
            }
        }
        final int buttonX=shellX+shellW-(shellW>=480?168:58);
        int addW=shellW>=480?108:24;
        action(buttonX,ty,addW,22,"minecraft:compass",shellW>=480?UiText.tr("vr.map.add_here"):"",this::newWaypointAtPlayer,"vr.map.add_here");
        action(shellX+shellW-28,ty,22,22,"minecraft:writable_book","",()->{infoOpen=!infoOpen;selected=null;editingWaypoint=false;rebuild();},"vr.map.details");
    }

    private void action(int x,int y,int w,int h,String icon,String label,Runnable click,String tooltipKey){
        var b=addRenderableWidget(new OwnModsUi.ActionButton(x,y,w,h,icon,label,OwnModsUi.BLUE,false,click));
        b.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(UiText.tr(tooltipKey))));
    }

    private void buildSidebarButtons() {
        final int x=sideX+5,w=sideW-12,headingY=sideY+6;
        final int rowH=23, rows=RefinedLayout.categoryRows(sideH);
        final int pages=Math.max(1,(CATEGORY_DEFS.size()+rows-1)/rows);
        categoryPage=Math.max(0,Math.min(categoryPage,pages-1));
        addRenderableWidget(new OwnModsUi.Surface(x,headingY,w,13,g->OwnModsUi.label(g,UiText.tr("vr.map.filters"),x,headingY,pages>1?w-44:w,OwnModsUi.MUTED)));
        for(int row=0;row<rows;row++){
            int i=categoryPage*rows+row;if(i>=CATEGORY_DEFS.size())break;
            CategoryDef def=CATEGORY_DEFS.get(i);
            addRenderableWidget(new OwnModsUi.FilterRow(x,headingY+17+row*24,w,rowH,def.icon(),def::label,
                    ()->categories.contains(def.id()),()->toggleCategoryDirect(def.id())));
        }
        final int navY=headingY-2;
        if(pages>1){
            var prev=addRenderableWidget(new MapUi.Action(x+w-40,navY,18,17,"<",MapUi.CYAN,()->{categoryPage--;rebuild();}));prev.active=categoryPage>0;
            prev.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(UiText.tr("vr.ui.previous"))));
            var next=addRenderableWidget(new MapUi.Action(x+w-19,navY,18,17,">",MapUi.CYAN,()->{categoryPage++;rebuild();}));next.active=categoryPage+1<pages;
            next.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(UiText.tr("vr.ui.next"))));
        }
        final int controlsY=sideY+sideH-99;
        String mode=UiText.tr(markerMode==MarkerMode.ALL?"vr.map.all":markerMode==MarkerMode.TARGET?"vr.map.target_only":"vr.map.none");
        action(x,controlsY,w,22,"minecraft:map",mode,()->{markerMode=switch(markerMode){case ALL->MarkerMode.TARGET;case TARGET->MarkerMode.NONE;case NONE->MarkerMode.ALL;};rebuild();},"vr.map.mode_tip");
        addRenderableWidget(new OwnModsUi.FilterRow(x,controlsY+25,w,22,"minecraft:nether_star",()->UiText.tr("vr.map.favorites"),()->favoriteOnly,()->favoriteOnly=!favoriteOnly));
        addRenderableWidget(new OwnModsUi.FilterRow(x,controlsY+49,w,22,"minecraft:player_head",()->UiText.tr("vr.map.players"),()->showPlayers,()->showPlayers=!showPlayers));
        final int bottom=sideY+sideH-25;
        var minus=addRenderableWidget(new MapUi.Action(x,bottom,23,21,"−",MapUi.CYAN,this::zoomOut));
        minus.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(UiText.tr("vr.map.zoom_out"))));
        action(x+27,bottom,w-54,21,"minecraft:compass","",this::centerPlayer,"vr.map.center");
        var plus=addRenderableWidget(new MapUi.Action(x+w-23,bottom,23,21,"+",MapUi.CYAN,this::zoomIn));
        plus.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(UiText.tr("vr.map.zoom_in"))));
    }

    /** Toggling filters is immediate and does not rebuild the whole screen. */
    private void toggleCategoryDirect(String id) {
        if (!categories.add(id)) categories.remove(id);
        if (selected != null && !categories.contains(category(selected))) {
            selected = null;
            rebuild(); // only needed because the right context-panel widget set changes
        }
    }

    private void zoomIn() {
        int next = Math.max(1, blocksPerPixel / 2);
        if (next != blocksPerPixel) { blocksPerPixel = next; mapTexture.invalidate(); }
    }

    private void zoomOut() {
        int next = Math.min(32, blocksPerPixel * 2);
        if (next != blocksPerPixel) { blocksPerPixel = next; mapTexture.invalidate(); }
    }

    private void buildInfoButtons() {
        final int x=infoX+8,w=infoW-16;
        if(!dockedInfo) action(infoX+infoW-25,infoY+5,20,20,"minecraft:barrier","",()->{infoOpen=false;selected=null;editingWaypoint=false;notice="";rebuild();},"vr.ui.close");
        if(editingWaypoint){
            nameBox=addRenderableWidget(new EditBox(font,x,infoY+29,w,20,Component.literal(UiText.tr("vr.ui.name"))));
            nameBox.setMaxLength(100);nameBox.setValue(editName);nameBox.setResponder(v->editName=v);
            int sw=Math.min(23,(w-16)/5);
            for(int i=0;i<COLORS.size();i++){
                String c=COLORS.get(i);var swatch=addRenderableWidget(new MapUi.SwatchButton(x+i*(sw+4),infoY+53,sw,MarkerMath.color(c),()->editColor.equals(c),()->editColor=c));
                swatch.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(UiText.tr("vr.color."+c))));
            }
            final int cols=Math.max(1,(w-48)/25),rows=Math.max(1,Math.min(3,(infoH-116)/25)),per=cols*rows;
            final int pages=(ICONS.size()+per-1)/per;iconPage=Math.min(iconPage,pages-1);
            for(int i=0;i<per&&iconPage*per+i<ICONS.size();i++){
                String icon=ICONS.get(iconPage*per+i);
                addRenderableWidget(new MapUi.IconChoice(x+(i%cols)*25,infoY+80+(i/cols)*25,23,icon,MapUi.CYAN,()->editIcon.equals(icon),()->editIcon=icon));
            }
            var prev=addRenderableWidget(new MapUi.Action(x+w-44,infoY+80,20,21,"<",MapUi.CYAN,()->{iconPage=Math.max(0,iconPage-1);rebuild();}));prev.active=iconPage>0;
            var next=addRenderableWidget(new MapUi.Action(x+w-21,infoY+80,20,21,">",MapUi.CYAN,()->{iconPage=Math.min(pages-1,iconPage+1);rebuild();}));next.active=iconPage+1<pages;
            int footer=infoY+infoH-27,half=(w-4)/2;
            action(x,footer,half,22,"minecraft:barrier",UiText.tr("vr.ui.cancel"),()->{editingWaypoint=false;notice="";rebuild();},"vr.ui.cancel");
            action(x+half+4,footer,w-half-4,22,"minecraft:lime_dye",UiText.tr("vr.ui.save"),this::saveWaypoint,"vr.ui.save");
            setInitialFocus(nameBox);return;
        }
        if(selected!=null){
            int y=infoY+infoH-78,half=(w-4)/2;
            action(x,y,half,22,"minecraft:target",UiText.tr("vr.map.target"),()->{session.target(selected.id);notice=UiText.tr("vr.map.target_set");},"vr.map.target");
            action(x+half+4,y,w-half-4,22,"minecraft:ender_eye",UiText.tr("vr.map.only_this"),()->{session.target(selected.id);markerMode=MarkerMode.TARGET;},"vr.map.only_this");
            action(x,y+25,half,22,"minecraft:writable_book",UiText.tr("vr.ui.notes"),this::openSelectedInNotes,"vr.ui.notes");
            action(x+half+4,y+25,w-half-4,22,"minecraft:nether_star",UiText.tr("vr.map.favorite"),this::toggleSelectedFavorite,"vr.map.favorite");
            if(selected.object.equals("waypoint")) action(x,y+50,half,22,"minecraft:feather",UiText.tr("vr.ui.edit"),()->beginEdit(selected),"vr.ui.edit");
            action(x+half+4,y+50,w-half-4,22,"minecraft:lava_bucket",UiText.tr("vr.ui.trash"),this::trashSelected,"vr.ui.trash");return;
        }
        int y=infoY+69;
        action(x,y,w,22,"minecraft:compass",UiText.tr("vr.map.add_here"),this::newWaypointAtPlayer,"vr.map.add_here");
        int maxRows=Math.max(0,Math.min(6,(infoH-104)/25));
        List<Note> quick=session.notes().stream().filter(n->!n.trashed&&n.position!=null&&n.position.dimension().equals(dim))
                .filter(n->n.object.equals("waypoint")||n.favorite).sorted(Comparator.<Note,Boolean>comparing(n->!n.favorite).thenComparing(n->n.title.toLowerCase(Locale.ROOT))).limit(maxRows).toList();
        for(int i=0;i<quick.size();i++){
            Note n=quick.get(i);var button=addRenderableWidget(new MapUi.IconAction(x,y+28+i*25,w,22,NotesCatalog.icon(n),n.title,MapUi.BLUE,false,()->{selected=n;notice="";rebuild();}));
            button.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(n.title)));
        }
    }

    private void switchDimension(String d) {
        if (d.equals(dim)) return;
        dim = d;
        selected = null;
        editingWaypoint = false;
        notice = "";
        session.map(dim).blockBounds().ifPresentOrElse(b -> {
            centerX = (b[0] + b[2]) / 2;
            centerZ = (b[1] + b[3]) / 2;
        }, () -> { centerX = 0; centerZ = 0; });
        centerPlayerIfSameDimension();
        mapTexture.invalidate();
        rebuild();
    }

    private void centerPlayerIfSameDimension() {
        if (minecraft.player != null && minecraft.level != null
                && minecraft.level.dimension().identifier().toString().equals(dim)) {
            centerX = minecraft.player.blockPosition().getX();
            centerZ = minecraft.player.blockPosition().getZ();
        }
    }

    private void centerPlayer() {
        centerPlayerIfSameDimension();
        mapTexture.invalidate();
    }

    private void drawInfoPanel(GuiGraphicsExtractor g) {
        OwnModsUi.content(g,infoX,infoY,infoW,infoH);
        int x=infoX+8,w=infoW-16;
        if(editingWaypoint){
            OwnModsUi.label(g,UiText.tr(editId==null?"vr.map.new":"vr.map.edit"),x,infoY+10,w-22,OwnModsUi.BODY_TEXT);
            if(infoH>240){
                OwnModsUi.label(g,shortDimension(editDimension),x,infoY+infoH-60,w,OwnModsUi.BODY_MUTED);
                OwnModsUi.label(g,"X "+editX+"  Y "+editY+"  Z "+editZ,x,infoY+infoH-47,w,OwnModsUi.BODY_MUTED);
            }
        }else if(selected!=null){
            OwnModsUi.icon(g,MapUi.menuItem(NotesCatalog.icon(selected)),x,infoY+8,16);
            OwnModsUi.label(g,selected.title,x+22,infoY+10,w-46,OwnModsUi.BODY_TEXT);
            OwnModsUi.smallText(g,categoryLabel(selected)+" · "+shortDimension(dim),x,infoY+29,OwnModsUi.BODY_MUTED,w);
            if(selected.position!=null){
                OwnModsUi.smallText(g,"X "+selected.position.x()+"  Y "+selected.position.y()+"  Z "+selected.position.z(),x,infoY+42,OwnModsUi.BODY_MUTED,w);
                if(infoH>215&&minecraft.player!=null&&minecraft.level!=null&&selected.position.dimension().equals(minecraft.level.dimension().identifier().toString())){
                    var p=minecraft.player.blockPosition();long dist=Math.round(Math.hypot(selected.position.x()-p.getX(),selected.position.z()-p.getZ()));
                    OwnModsUi.smallText(g,UiText.tr("vr.map.distance",dist),x,infoY+58,OwnModsUi.BODY_MUTED,w);
                }
                if(infoH>240&&MinimapClient.settings!=null&&MinimapClient.settings.flag("portal_projection"))selected.position.portalProjection().ifPresent(pp->OwnModsUi.smallText(g,UiText.tr("vr.map.portal",shortDimension(pp.dimension()),pp.x(),pp.z()),x,infoY+74,OwnModsUi.BRASS,w));
            }
        }else{
            OwnModsUi.icon(g,"minecraft:compass",x,infoY+9,16);
            OwnModsUi.label(g,UiText.tr("vr.map.waypoints"),x+24,infoY+12,w-48,OwnModsUi.BODY_TEXT);
            OwnModsUi.smallText(g,UiText.tr("vr.map.help1"),x,infoY+36,OwnModsUi.BODY_MUTED,w);
            OwnModsUi.smallText(g,UiText.tr("vr.map.help2"),x,infoY+49,OwnModsUi.BODY_MUTED,w);
        }
        if(!notice.isBlank()&&infoH>275)OwnModsUi.smallText(g,notice,x,infoY+infoH-94,OwnModsUi.BRASS,w);
    }

    private void drawMap(GuiGraphicsExtractor g) {
        MapUi.mapFrame(g, mapX, mapY, mapW, mapH);
        ExploredMap map = session.map(dim);
        mapTexture.draw(g, map, dim, centerX, centerZ, blocksPerPixel,
                mapX + 3, mapY + 3, mapW - 6, mapH - 6);

        hits.clear();
        int labelled = 0;
        for (Note n : visibleNotes()) {
            int sx = mapX + mapW / 2 + (n.position.x() - centerX) / blocksPerPixel;
            int sy = mapY + mapH / 2 + (n.position.z() - centerZ) / blocksPerPixel;
            if (sx < mapX + 7 || sy < mapY + 7 || sx >= mapX + mapW - 7 || sy >= mapY + mapH - 7) continue;
            boolean target = n.id.equals(session.targetId());
            int color = MarkerMath.color(n.color);
            MapUi.markerIcon(g, sx, sy, color, NotesCatalog.icon(n), target);
            if ((target || shouldLabel(n)) && labelled < 16) {
                String label = minecraft.font.plainSubstrByWidth(n.title, 82);
                MapUi.tag(g, label, sx + 8, sy - 4, color);
                labelled++;
            }
            hits.add(new MarkerHit(n, sx, sy));
        }

        if (minecraft.player != null && minecraft.level != null
                && minecraft.level.dimension().identifier().toString().equals(dim)) {
            int sx = mapX + mapW / 2 + (minecraft.player.blockPosition().getX() - centerX) / blocksPerPixel;
            int sy = mapY + mapH / 2 + (minecraft.player.blockPosition().getZ() - centerZ) / blocksPerPixel;
            if (sx >= mapX + 5 && sy >= mapY + 5 && sx < mapX + mapW - 5 && sy < mapY + mapH - 5) {
                MapUi.playerArrow(g, sx, sy);
            }
            if (showPlayers) drawOtherPlayers(g);
        }
        drawScale(g);
    }

    private void drawOtherPlayers(GuiGraphicsExtractor g) {
        try {
            for (var other : minecraft.level.players()) {
                if (other == minecraft.player) continue;
                var op = other.blockPosition();
                int ox = mapX + mapW / 2 + (op.getX() - centerX) / blocksPerPixel;
                int oy = mapY + mapH / 2 + (op.getZ() - centerZ) / blocksPerPixel;
                if (ox < mapX + 7 || oy < mapY + 7 || ox >= mapX + mapW - 7 || oy >= mapY + mapH - 7) continue;
                MapUi.markerIcon(g, ox, oy, MapUi.BLUE, "minecraft:player_head", false);
                MapUi.tag(g, minecraft.font.plainSubstrByWidth(other.getName().getString(), 70), ox + 8, oy - 4, MapUi.BLUE);
            }
        } catch (RuntimeException ignored) {}
    }

    private void drawScale(GuiGraphicsExtractor g) {
        int px = Math.max(45, Math.min(105, mapW / 5));
        int meters = px * blocksPerPixel;
        int x = mapX + 10, y = mapY + mapH - 15;
        g.fill(x, y, x + px, y + 2, 0xFFE9EEEE);
        g.fill(x, y - 3, x + 1, y + 4, 0xFFE9EEEE);
        g.fill(x + px, y - 3, x + px + 1, y + 4, 0xFFE9EEEE);
        MapUi.text(g, formatDistance(meters), x, y - 11, 0xFFE9EEEE);
    }

    private static String formatDistance(int meters) {
        if (meters < 1000) return meters + " m";
        int tenth = Math.max(10, Math.round(meters / 100f));
        return (tenth / 10) + "." + (tenth % 10) + " km";
    }

    private boolean shouldLabel(Note n) {
        String c = category(n);
        return c.equals("waypoint") || c.equals("homebase") || c.equals("village") || c.equals("farm") || c.equals("portal");
    }

    private List<Note> visibleNotes() {
        if (markerMode == MarkerMode.NONE) return List.of();
        var stream = session.notes().stream()
                .filter(n -> !n.trashed && n.position != null && n.position.dimension().equals(dim))
                .filter(n -> categories.contains(category(n)))
                .filter(n -> onlyMarkerId == null || onlyMarkerId.equals(n.id))
                .filter(n -> !favoriteOnly || n.favorite);
        if (markerMode == MarkerMode.TARGET) {
            String id = session.targetId();
            if (id.isBlank()) return List.of();
            stream = stream.filter(n -> n.id.equals(id));
        }
        return stream.toList();
    }

    private void mapClick(double mx, double my, int button, boolean doubleClick) {
        if (button == InputCompat.code("MOUSE_BUTTON_LEFT")) {
            for (MarkerHit h : hits) {
                if (Math.hypot(mx - h.x, my - h.y) <= 9) {
                    selected = h.note;
                    editingWaypoint = false;
                    notice = "";
                    rebuild();
                    return;
                }
            }
        }

        int wx = centerX + (int)Math.round((mx - (mapX + mapW / 2.0)) * blocksPerPixel);
        int wz = centerZ + (int)Math.round((my - (mapY + mapH / 2.0)) * blocksPerPixel);

        if (doubleClick && button == InputCompat.code("MOUSE_BUTTON_LEFT")) {
            centerX = wx;
            centerZ = wz;
            selected = null;
            editingWaypoint = false;
            notice = "";
            mapTexture.invalidate();
            rebuild();
            return;
        }

        if (button == InputCompat.code("MOUSE_BUTTON_RIGHT")) {
            if (!session.map(dim).knownAtBlock(wx, wz)) {
                selected = null;
                editingWaypoint = false;
                notice = UiText.tr("vr.text.299bf5075d5b");
                rebuild();
                return;
            }
            beginEdit(null, dim, wx, waypointY(wx, wz), wz);
            return;
        }

        if (button == InputCompat.code("MOUSE_BUTTON_LEFT") && (selected != null || editingWaypoint)) {
            selected = null;
            editingWaypoint = false;
            notice = "";
            rebuild();
        }
    }

    private int waypointY(int wx, int wz) {
        if (minecraft.player == null) return 64;
        int fallback = minecraft.player.blockPosition().getY();
        if (minecraft.level == null || !minecraft.level.dimension().identifier().toString().equals(dim)) return fallback;
        try {
            BlockPos probe = new BlockPos(wx, fallback, wz);
            if (!minecraft.level.isLoaded(probe)) return fallback;
            return minecraft.level.getHeight(Heightmap.Types.WORLD_SURFACE, wx, wz) - 1;
        } catch (RuntimeException ignored) { return fallback; }
    }

    private void newWaypointAtPlayer() {
        if (minecraft.player == null || minecraft.level == null) {
            notice = UiText.tr("vr.text.fd660f9a50d4");
            return;
        }
        String current = minecraft.level.dimension().identifier().toString();
        if (!current.equals(dim)) {
            notice = UiText.tr("vr.text.162e8e53d420");
            return;
        }
        var p = minecraft.player.blockPosition();
        beginEdit(null, current, p.getX(), p.getY(), p.getZ());
    }

    private void beginEdit(Note note) {
        if (note == null || note.position == null) return;
        editId = note.id;
        editDimension = note.position.dimension();
        editX = note.position.x();
        editY = note.position.y();
        editZ = note.position.z();
        editName = note.title;
        editColor = COLORS.contains(note.color) ? note.color : "blue";
        editIcon = ICONS.contains(note.mob) ? note.mob : "minecraft:map";
        iconPage = 0;
        editingWaypoint = true;
        selected = null;
        notice = "";
        rebuild();
    }

    private void beginEdit(String id, String dimension, int x, int y, int z) {
        beginEditState(id, dimension, x, y, z);
        selected = null;
        notice = "";
        rebuild();
    }

    private void beginEditState(String id, String dimension, int x, int y, int z) {
        editId = id;
        editDimension = dimension;
        editX = x;
        editY = y;
        editZ = z;
        editName = UiText.tr("vr.text.1533799cc9bd");
        editColor = "blue";
        editIcon = "minecraft:map";
        iconPage = 0;
        editingWaypoint = true;
    }

    private void saveWaypoint() {
        try {
            Note saved = NotesBridge.saveWaypoint(editId, editName, editColor, editIcon, editDimension, editX, editY, editZ);
            session.refreshNotes();
            selected = saved;
            editingWaypoint = false;
            notice = UiText.tr("vr.text.5e2338e21142");
            rebuild();
        } catch (Exception e) {
            notice = UiText.tr("vr.text.5df942eb6687") + safeMessage(e);
        }
    }

    private void toggleSelectedFavorite() {
        if (selected == null) return;
        try {
            Note changed = NotesBridge.setFavorite(selected.id, !selected.favorite);
            session.refreshNotes();
            selected = changed;
            notice = changed.favorite ? UiText.tr("vr.text.65570584cdf2") : UiText.tr("vr.text.d59e5c9f0eb9");
            rebuild();
        } catch (Exception e) {
            notice = UiText.tr("vr.text.2877191c49c0") + safeMessage(e);
        }
    }

    private void openSelectedInNotes() {
        if (selected == null) return;
        try { minecraft.gui.setScreen(NotesBridge.open(this, selected.id)); }
        catch (Exception e) { notice = UiText.tr("vr.text.264ed61b6239") + safeMessage(e); }
    }

    private void trashSelected() {
        if (selected == null) return;
        try {
            String id = selected.id;
            NotesBridge.trash(id);
            session.refreshNotes();
            if (session.targetId().equals(id)) session.target("");
            selected = null;
            notice = UiText.tr("vr.text.7c67caae45e7");
            rebuild();
        } catch (Exception e) {
            notice = UiText.tr("vr.text.e22e2599bbc5") + safeMessage(e);
        }
    }

    private void runSearch() {
        String q = searchQuery == null ? "" : searchQuery.strip();
        if (q.isEmpty()) { notice = UiText.tr("vr.text.e5f0a757c869"); return; }
        String lower = q.toLowerCase(Locale.ROOT);
        var result = session.notes().stream()
                .filter(n -> !n.trashed && n.position != null && n.position.dimension().equals(dim))
                .filter(n -> n.matches(q) || categoryLabel(n).toLowerCase(Locale.ROOT).contains(lower))
                .sorted(Comparator.<Note,Boolean>comparing(n -> !n.favorite)
                        .thenComparing(n -> n.title.toLowerCase(Locale.ROOT)))
                .findFirst();
        if (result.isPresent()) {
            Note n = result.get();
            centerX = n.position.x();
            centerZ = n.position.z();
            selected = n;
            editingWaypoint = false;
            notice = "";
            mapTexture.invalidate();
            rebuild();
        } else {
            notice = UiText.tr("vr.text.6de338ed647c");
        }
    }

    private void rebuild() {
        clearWidgets();
        init();
    }

    private String category(Note n) {
        if (n.kind == Note.Kind.GOAL) return "farm";
        if (n.object.equals("waypoint")) return "waypoint";
        if (n.object.equals("deathpoint")) return "deathpoint";
        if (n.object.equals("homebase")) return "homebase";
        if (n.object.equals("village")) return "village";
        if (n.object.equals("spawner")) return "spawner";
        if (n.object.contains("portal")) return "portal";
        if (n.object.equals("biome")) return "biome";
        if (Set.of("ancient_city", "mineshaft", "stronghold", "trail_ruins", "trial_chambers", "desert_pyramid",
                "igloo", "jungle_temple", "pillager_outpost", "woodland_mansion", "ocean_monument", "nether_fortress",
                "bastion_remnant", "end_city").contains(n.object)) return "structure";
        return "other";
    }

    private String categoryLabel(Note n) {
        String c = category(n);
        return CATEGORY_DEFS.stream().filter(d -> d.id().equals(c)).map(CategoryDef::label).findFirst().orElse(UiText.tr("vr.text.15b61974b270"));
    }

    private static String shortDimension(String dimension) {
        if (dimension == null) return "";
        if (dimension.endsWith("overworld")) return UiText.tr("vr.text.243fb47abea8");
        if (dimension.endsWith("the_nether")) return UiText.tr("vr.text.a5a292152531");
        if (dimension.endsWith("the_end")) return UiText.tr("vr.text.f4db1e48476f");
        return dimension;
    }

    private static String safeMessage(Exception e) {
        String s = e.getMessage();
        return s == null || s.isBlank() ? e.getClass().getSimpleName() : s;
    }

    @Override public boolean keyPressed(KeyEvent e) {
        if (searchBox != null && searchBox.isFocused()
                && (e.key() == InputCompat.code("KEY_RETURN") || e.key() == InputCompat.code("KEY_NUMPADENTER"))) {
            runSearch();
            return true;
        }
        if (editingWaypoint && nameBox != null && nameBox.isFocused()
                && (e.key() == InputCompat.code("KEY_RETURN") || e.key() == InputCompat.code("KEY_NUMPADENTER"))) {
            saveWaypoint();
            return true;
        }
        int move = Math.max(16, blocksPerPixel * 32);
        if (e.key() == InputCompat.code("KEY_LEFT")) { centerX -= move; mapTexture.invalidate(); return true; }
        if (e.key() == InputCompat.code("KEY_RIGHT")) { centerX += move; mapTexture.invalidate(); return true; }
        if (e.key() == InputCompat.code("KEY_UP")) { centerZ -= move; mapTexture.invalidate(); return true; }
        if (e.key() == InputCompat.code("KEY_DOWN")) { centerZ += move; mapTexture.invalidate(); return true; }
        if (e.key() == InputCompat.code("KEY_ESCAPE") && (editingWaypoint || selected != null)) {
            editingWaypoint = false;
            selected = null;
            notice = "";
            rebuild();
            return true;
        }
        return super.keyPressed(e);
    }

    @Override public void onClose() {
        minecraft.gui.setScreen(parent);
    }

    @Override public void removed() {
        mapTexture.close();
        super.removed();
    }

    // 0.11.1a: API fuer die neue Kartenfilter-Oberflaeche.
    boolean categoryEnabled(String category) {
        return category != null && categories.contains(category);
    }

    void toggleCategory(String category) {
        onlyMarkerId = null;
        if (category == null || !MAP_FILTER_CATEGORIES.contains(category)) return;
        toggleCategoryDirect(category);
    }

    void setAllCategories(boolean enabled) {
        onlyMarkerId = null;
        categories.clear();
        if (enabled) categories.addAll(CATEGORY_DEFS.stream().map(CategoryDef::id).toList());
    }

    void showOnly(String markerId) {
        onlyMarkerId = (markerId == null || markerId.isBlank()) ? null : markerId;
    }

    boolean markerAllowed(String markerId, String category) {
        if (onlyMarkerId != null && !onlyMarkerId.equals(markerId)) return false;
        return categoryEnabled(category);
    }

    void clearShowOnly() {
        onlyMarkerId = null;
    }
}
