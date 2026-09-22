package de.ownmods.minimap.client;
import de.ownmods.settings.client.UiText;
import de.ownmods.settings.client.compat.InputCompat;

import com.mojang.blaze3d.platform.InputConstants;
import de.ownmods.minimap.MinimapSettings;
import de.ownmods.minimap.core.MarkerMath;
import de.ownmods.notes.client.NotesBridge;
import de.ownmods.notes.core.Note;
import de.ownmods.notes.core.NotesCatalog;
import de.ownmods.settings.api.ManagedMods;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.List;

/** Client entry point. Vanilla map data stays canonical; a bounded loaded-chunk pass prevents black HUD wedges. */
public final class MinimapClient implements ClientModInitializer {
    static MinimapSettings settings;
    static MinimapSession session;
    private static KeyMapping mapKey;
    private static KeyMapping waypointKey;
    private static String scope = "";
    private static boolean wasAlive = true;
    private static int lastX, lastY, lastZ;
    private static String lastDimension = "minecraft:overworld";
    private static int deathSerial;
    private static HudMapTexture hudTexture = new HudMapTexture();

    @Override public void onInitializeClient() {
        settings = new MinimapSettings(FabricLoader.getInstance().getConfigDir().resolve("ownmods/minimap.properties"));
        ManagedMods.register(settings);
        var cat = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("ownmods_minimap", "keys"));
        mapKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.ownmods_minimap.worldmap", InputCompat.keyboardType(InputConstants.Type.class), InputCompat.code("KEY_M"), cat));
        waypointKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.ownmods_minimap.waypoint", InputCompat.keyboardType(InputConstants.Type.class), InputCompat.code("KEY_B"), cat));
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, Identifier.fromNamespaceAndPath("ownmods_minimap", "map"), MinimapClient::hud);
        ClientTickEvents.END_CLIENT_TICK.register(MinimapClient::tick);

        // World-map wheel zoom is handled only while the cursor is over the actual map canvas.
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (!(screen instanceof WorldMapScreen world)) return;
            ScreenMouseEvents.allowMouseScroll(screen).register((s, mouseX, mouseY, horizontalAmount, verticalAmount) -> {
                if (!world.mapContains(mouseX, mouseY) || verticalAmount == 0.0) return true;
                world.wheelZoom(verticalAmount, mouseX, mouseY);
                return false;
            });
        });
    }

    private static void tick(Minecraft mc) {
        if (mc.level == null || mc.player == null) {
            if (session != null) { session.close(); session = null; scope = ""; }
            hudTexture.close();
            while (mapKey.consumeClick()) {}
            while (waypointKey.consumeClick()) {}
            wasAlive = true;
            return;
        }
        try {
            String s = NotesBridge.scopeKey();
            if (session == null || !s.equals(scope)) {
                if (session != null) session.close();
                hudTexture.close();
                session = new MinimapSession();
                scope = s;
                wasAlive = true;
            }
        } catch (Exception e) { return; }

        var pos = mc.player.blockPosition();
        boolean alive = mc.player.getHealth() > 0.0F;
        if (alive) {
            lastX = pos.getX(); lastY = pos.getY(); lastZ = pos.getZ();
            lastDimension = mc.level.dimension().identifier().toString();
        } else if (wasAlive && settings.flag("deathpoints")) {
            try {
                deathSerial++;
                NotesBridge.saveDeathPoint(UiText.tr("vr.text.34a5c01274ad") + deathSerial, lastDimension, lastX, lastY, lastZ);
                session.refreshNotes();
            } catch (Exception ignored) {}
        }
        wasAlive = alive;

        // Reveal the complete world-space footprint represented by the visible HUD circle.
        // The session never forces chunk loads; areas the client does not have remain unknown.
        if (settings.enabled()) session.tick(mc, revealRadiusBlocks(mc));

        while (mapKey.consumeClick()) {
            if (mc.gui.screen() == null) mc.gui.setScreen(new WorldMapScreen(null, session));
        }
        while (waypointKey.consumeClick()) {
            if (mc.gui.screen() == null) mc.gui.setScreen(WorldMapScreen.forPlayerPosition(session));
        }
    }

    private static int radius() {
        return switch (settings.choice("size")) {
            case "small" -> 30;
            case "large" -> 48;
            default -> 39;
        };
    }

    private static int hudBlocksPerPixel(Minecraft mc) {
        int bpp = settings.blocksPerPixel();
        if (settings.flag("auto_zoom") && mc.player != null) {
            try {
                if (mc.player.getDeltaMovement().horizontalDistanceSqr() > 0.08D) bpp = Math.min(8, bpp * 2);
            } catch (RuntimeException ignored) {}
        }
        return bpp;
    }

    private static int revealRadiusBlocks(Minecraft mc) {
        // HudMapTexture renders terrain to roughly r-3 logical pixels from the centre.
        // Multiply by blocks/pixel so 2x/4x/8x zoom no longer keeps the old small reveal circle.
        int pixelRadius = radius() - 3;
        if ("square".equals(settings.choice("shape"))) {
            pixelRadius = (int)Math.ceil(pixelRadius * Math.sqrt(2.0));
        }
        return Math.max(8, pixelRadius * hudBlocksPerPixel(mc));
    }

    private static void hud(GuiGraphicsExtractor g, DeltaTracker ignored) {
        Minecraft mc = Minecraft.getInstance();
        if (session == null || !settings.enabled() || mc.level == null || mc.player == null || mc.gui.screen() != null) return;

        int r = radius();
        int d = r * 2;
        String position = settings.choice("position");
        boolean right = position.equals("right") || position.equals("bottom_right");
        boolean bottom = position.equals("bottom_left") || position.equals("bottom_right");
        int x = right ? g.guiWidth() - d - 10 : 10;
        int y = bottom ? Math.max(10, g.guiHeight() - d - 52) : 10;
        int cx = x + r, cy = y + r;
        int playerX = mc.player.blockPosition().getX();
        int playerY = mc.player.blockPosition().getY();
        int playerZ = mc.player.blockPosition().getZ();
        String currentDim = mc.level.dimension().identifier().toString();
        var map = session.map(currentDim);
        int bpp = hudBlocksPerPixel(mc);
        boolean circle = !"square".equals(settings.choice("shape"));
        boolean playerUp = "player_up".equals(settings.choice("rotation"));
        double yaw = Math.toRadians(mc.player.getYRot());

        // Procedural HUD chrome plus one cached texture blit for terrain.
        MapUi.hudFrame(g, x, y, d, circle);
        hudTexture.draw(g, map, playerX, playerZ, bpp, circle, playerUp, yaw, x, y, d);

        if (settings.flag("direction")) drawCompass(g, cx, cy, r, playerUp, yaw);

        if (playerUp) MapUi.arrow(g, cx, cy, 0, -1, 0xFFFFFFFF);
        else MapUi.arrow(g, cx, cy, -Math.sin(yaw), Math.cos(yaw), 0xFFFFFFFF);

        Note target = null;
        List<Note> notes = session.notes().stream()
                .filter(n -> !n.trashed && n.position != null && n.position.dimension().equals(currentDim))
                .filter(MinimapClient::hudMarkerVisible)
                .sorted(Comparator.<Note, Boolean>comparing(n -> !n.id.equals(session.targetId()))
                        .thenComparingDouble(n -> n.position.distance(new Note.Position(n.position.dimension(), playerX, playerY, playerZ))))
                .limit(32).toList();
        int arrows = 0;
        for (Note n : notes) {
            double dx = n.position.x() - playerX;
            double dz = n.position.z() - playerZ;
            double[] screen = worldToScreen(dx, dz, playerUp, yaw);
            screen[0] /= bpp; screen[1] /= bpp;
            int col = MarkerMath.color(n.color);
            boolean isTarget = n.id.equals(session.targetId());
            if (isTarget) target = n;
            if (MarkerMath.inside(screen[0], screen[1], r - 9)) {
                int mx = cx + (int)Math.round(screen[0]), my = cy + (int)Math.round(screen[1]);
                MapUi.markerIcon(g, mx, my, col, NotesCatalog.icon(n), isTarget);
            } else if (arrows < 7 || isTarget) {
                double[] e = MarkerMath.edge(screen[0], screen[1], r - 6);
                MapUi.arrow(g, cx + (int)e[0], cy + (int)e[1], screen[0], screen[1], col);
                arrows++;
            }
        }

        if (settings.flag("players")) {
            try {
                for (var other : mc.level.players()) {
                    if (other == mc.player) continue;
                    var op = other.blockPosition();
                    double[] screen = worldToScreen(op.getX() - playerX, op.getZ() - playerZ, playerUp, yaw);
                    screen[0] /= bpp; screen[1] /= bpp;
                    if (MarkerMath.inside(screen[0], screen[1], r - 9)) {
                        int mx = cx + (int)Math.round(screen[0]), my = cy + (int)Math.round(screen[1]);
                        MapUi.markerIcon(g, mx, my, 0xFF5AA7E8, "minecraft:player_head", false);
                    }
                }
            } catch (RuntimeException ignored2) {}
        }

        int infoHeight = drawInfoBox(g, mc, cx, y + d + 4, d, playerX, playerY, playerZ, currentDim);

        if (target != null && (settings.flag("names") || settings.flag("distance"))) {
            String label = settings.flag("names") ? target.title : UiText.tr("vr.text.f487ce396018");
            if (settings.flag("distance")) {
                long meters = Math.round(Math.hypot(target.position.x() - playerX, target.position.z() - playerZ));
                label += "  " + meters + " m";
            }
            int tw = mc.font.width(label);
            int bw = Math.min(Math.max(72, tw + 24), 156);
            int bx = right ? x + d - bw : x;
            int by = bottom ? y - 24 : y + d + 8 + infoHeight;
            g.fill(bx + 2, by + 2, bx + bw + 2, by + 18, 0x4D000000);
            g.fill(bx, by, bx + bw, by + 16, 0xC9141719);
            int col = MarkerMath.color(target.color);
            g.fill(bx, by, bx + 3, by + 16, col);
            MapUi.icon(g, NotesCatalog.icon(target), bx + 5, by + 2, 12);
            MapUi.text(g, mc.font.plainSubstrByWidth(label, bw - 23), bx + 20, by + 4, 0xFFF4F4F4);
        }
    }


    private static int drawInfoBox(GuiGraphicsExtractor g, Minecraft mc, int cx, int top, int mapDiameter,
                                   int playerX, int playerY, int playerZ, String currentDim) {
        StringBuilder primary = new StringBuilder();
        if (settings.flag("coordinates")) primary.append(playerX).append(" · ").append(playerZ);
        if (settings.flag("height")) appendPart(primary, "Y " + playerY);

        StringBuilder secondary = new StringBuilder();
        if (settings.flag("biome")) appendPart(secondary, biomeName(mc));
        if (settings.flag("dimension")) appendPart(secondary, shortDimension(currentDim));
        if (primary.isEmpty() && secondary.isEmpty()) return 0;

        int maxBox = Math.min(Math.max(82, mapDiameter + 22), Math.max(82, g.guiWidth() - 8));
        String p = primary.isEmpty() ? "" : MapUi.smallSubstr(primary.toString(), maxBox - 14);
        String q = secondary.isEmpty() ? "" : MapUi.smallSubstr(secondary.toString(), maxBox - 14);
        int textW = Math.max(MapUi.smallWidth(p), MapUi.smallWidth(q));
        int boxW = Math.max(58, Math.min(maxBox, textW + 14));
        int boxH = p.isEmpty() || q.isEmpty() ? 14 : 23;
        int bx = Math.max(4, Math.min(g.guiWidth() - boxW - 4, cx - boxW / 2));
        int by = Math.min(Math.max(4, top), Math.max(4, g.guiHeight() - boxH - 4));

        g.fill(bx + 2, by + 2, bx + boxW + 2, by + boxH + 2, 0x42000000);
        g.fill(bx, by, bx + boxW, by + boxH, 0xB9141719);
        g.outline(bx, by, boxW, boxH, 0x99545E61);
        if (!p.isEmpty()) {
            int py = q.isEmpty() ? by + 4 : by + 3;
            MapUi.smallText(g, p, bx + (boxW - MapUi.smallWidth(p)) / 2, py, 0xFFE8E5DF);
        }
        if (!q.isEmpty()) {
            int qy = p.isEmpty() ? by + 4 : by + 12;
            MapUi.smallText(g, q, bx + (boxW - MapUi.smallWidth(q)) / 2, qy, 0xFFBFC5C4);
        }
        return boxH;
    }

    private static boolean hudMarkerVisible(Note n) {
        if (n.object.equals("waypoint")) return settings.flag("waypoints");
        if (n.object.equals("deathpoint")) return settings.flag("deathpoints");
        return settings.flag("note_locations");
    }

    private static void appendPart(StringBuilder b, String value) {
        if (value == null || value.isBlank()) return;
        if (!b.isEmpty()) b.append(" · ");
        b.append(value);
    }

    private static double[] screenToWorld(double sx, double sy, boolean playerUp, double yaw) {
        if (!playerUp) return new double[]{sx, sy};
        double sin = Math.sin(yaw), cos = Math.cos(yaw);
        return new double[]{-cos * sx + sin * sy, -sin * sx - cos * sy};
    }

    private static double[] worldToScreen(double dx, double dz, boolean playerUp, double yaw) {
        if (!playerUp) return new double[]{dx, dz};
        double sin = Math.sin(yaw), cos = Math.cos(yaw);
        return new double[]{-(dx * cos + dz * sin), dx * sin - dz * cos};
    }

    private static void drawCompass(GuiGraphicsExtractor g, int cx, int cy, int r, boolean playerUp, double yaw) {
        compassLabel(g,"N",0,-1,cx,cy,r,playerUp,yaw);
        compassLabel(g,"E",1,0,cx,cy,r,playerUp,yaw);
        compassLabel(g,"S",0,1,cx,cy,r,playerUp,yaw);
        compassLabel(g,"W",-1,0,cx,cy,r,playerUp,yaw);
    }

    private static void compassLabel(GuiGraphicsExtractor g,String label,double dx,double dz,int cx,int cy,int r,boolean playerUp,double yaw) {
        double[] p=worldToScreen(dx,dz,playerUp,yaw);
        double len=Math.max(0.001,Math.hypot(p[0],p[1]));
        int x=cx+(int)Math.round(p[0]/len*(r-6));
        int y=cy+(int)Math.round(p[1]/len*(r-6));
        int tw=Minecraft.getInstance().font.width(label);
        MapUi.text(g,label,x-tw/2,y-4,0xFFF5F5F5);
    }

    private static String shortDimension(String d) {
        if (d.endsWith("overworld")) return UiText.tr("vr.text.243fb47abea8");
        if (d.endsWith("the_nether")) return UiText.tr("vr.text.a5a292152531");
        if (d.endsWith("the_end")) return UiText.tr("vr.text.f4db1e48476f");
        return d;
    }

    /** Reflection keeps biome label cosmetic and non-fatal across minor mapping changes. */
    private static String biomeName(Minecraft mc) {
        try {
            Object holder=mc.level.getBiome(mc.player.blockPosition());
            Method unwrapKey=holder.getClass().getMethod("unwrapKey");
            Object opt=unwrapKey.invoke(holder);
            if (opt instanceof java.util.Optional<?> o && o.isPresent()) {
                Object key=o.get();
                Method identifier=key.getClass().getMethod("identifier");
                Object id=identifier.invoke(key);
                String s=String.valueOf(id);
                int colon=s.indexOf(':');
                if(colon>=0)s=s.substring(colon+1);
                return titleCase(s.replace('_',' '));
            }
        } catch (Exception ignored) {}
        return "";
    }

    private static String titleCase(String s){
        if(s.isBlank())return s;
        StringBuilder out=new StringBuilder(); boolean upper=true;
        for(char c:s.toCharArray()){
            if(c==' '){out.append(c);upper=true;}
            else {out.append(upper?Character.toUpperCase(c):c);upper=false;}
        }
        return out.toString();
    }
}
