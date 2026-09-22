package de.ownmods.notes.client;
import de.ownmods.settings.client.UiText;

import de.ownmods.notes.core.Note;
import de.ownmods.notes.core.NotesStore;
import java.io.IOException;
import java.util.List;
import net.minecraft.client.gui.screens.Screen;

/** Public, client-only bridge used by OwnMods modules that intentionally integrate with Notes. */
public final class NotesBridge {
    private NotesBridge() {}

    /**
     * Bridges must never keep a second long-lived NotesStore beside the notebook UI.
     * A fresh lightweight session is opened for each bridge operation, so Locations created,
     * renamed, restored or trashed in Notes become visible to Minimap immediately on refresh.
     */
    private static NotesClient.Session session() throws IOException {
        NotesClient.Session s = new NotesClient.Session();
        s.verify();
        return s;
    }

    public static String scopeKey() throws IOException { return NotesStore.scopeKey(NotesClient.identity()); }

    public static List<Note> activeLocations() throws IOException {
        return session().store.all().stream()
                .filter(n -> !n.trashed)
                .filter(n -> (n.kind == Note.Kind.LOCATION || n.kind == Note.Kind.GOAL) && n.position != null)
                .toList();
    }

    public static Note saveWaypoint(String existingId,String name,String color,String icon,String dimension,int x,int y,int z) throws IOException {
        NotesClient.Session s=session();
        Note n=existingId==null||existingId.isBlank()?new Note():s.store.find(existingId).orElseGet(Note::new);
        n.kind=Note.Kind.LOCATION;
        n.object="waypoint";
        n.title=(name==null||name.isBlank())?UiText.tr("vr.text.1533799cc9bd"):name.strip();
        n.color=Note.COLORS.contains(color)?color:"blue";
        n.mob=(icon!=null&&icon.matches("[a-z0-9_.-]+:[a-z0-9_./-]+"))?icon:"minecraft:map";
        n.biome="";
        n.position=new Note.Position(dimension,x,y,z);
        n.status=Note.Status.OPEN;
        n.trashed=false;
        s.save(n);
        return s.store.find(n.id).orElseThrow();
    }

    public static Note saveDeathPoint(String name,String dimension,int x,int y,int z) throws IOException {
        NotesClient.Session s=session();
        Note n=new Note();
        n.kind=Note.Kind.LOCATION;
        n.object="deathpoint";
        n.title=(name==null||name.isBlank())?UiText.tr("vr.text.551768716253"):name.strip();
        n.color="red";
        n.mob="minecraft:skeleton_skull";
        n.biome="";
        n.position=new Note.Position(dimension,x,y,z);
        n.status=Note.Status.OPEN;
        n.trashed=false;
        n.favorite=false;
        s.save(n);
        return s.store.find(n.id).orElseThrow();
    }

    public static Note setFavorite(String id, boolean favorite) throws IOException {
        NotesClient.Session s=session();
        Note n=s.store.find(id).orElseThrow(()->new IOException(UiText.tr("vr.text.b2f867a94710")));
        n.favorite=favorite;
        s.save(n);
        return s.store.find(n.id).orElseThrow();
    }

    /** Map deletion follows the Notes trash model: hidden everywhere but recoverable until the trash is emptied. */
    public static void trash(String id) throws IOException {
        NotesClient.Session s=session();
        Note n=s.store.find(id).orElseThrow(()->new IOException(UiText.tr("vr.text.b2f867a94710")));
        n.trashed=true;
        s.save(n);
    }

    public static Screen open(Screen parent,String id) throws IOException {
        NotesClient.Session s=session();
        Note n=s.store.find(id).orElseThrow(()->new IOException(UiText.tr("vr.text.b2f867a94710")));
        return new NoteEditorScreen(parent,s,n);
    }
}
