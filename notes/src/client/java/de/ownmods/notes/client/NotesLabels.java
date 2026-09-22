package de.ownmods.notes.client;
import de.ownmods.notes.core.Note;
import de.ownmods.notes.core.NotesCatalog;
import de.ownmods.settings.client.UiText;
import java.util.List;

/** Presentation-only localization. NotesCatalog and serialized user data stay unchanged. */
final class NotesLabels {
    private NotesLabels() {}
    private static List<NotesCatalog.Entry> localize(List<NotesCatalog.Entry> entries) {
        return entries.stream().map(e->new NotesCatalog.Entry(e.id(),UiText.catalog(e.label()),e.icon())).toList();
    }
    private static List<NotesCatalog.StatusEntry> states(List<NotesCatalog.StatusEntry> entries) {
        return entries.stream().map(e->new NotesCatalog.StatusEntry(e.status(),UiText.catalog(e.label()),e.icon())).toList();
    }
    static List<NotesCatalog.Entry> dimensions(){return localize(NotesCatalog.DIMENSIONS);}
    static List<NotesCatalog.Entry> objects(){return localize(NotesCatalog.OBJECTS);}
    static List<NotesCatalog.Entry> goals(){return localize(NotesCatalog.GOALS);}
    static List<NotesCatalog.Entry> spawnerMobs(){return localize(NotesCatalog.SPAWNER_MOBS);}
    static List<NotesCatalog.Entry> objectsForDimension(String dim){return localize(NotesCatalog.objectsForDimension(dim));}
    static List<NotesCatalog.Entry> goalsForDimension(String dim){return localize(NotesCatalog.goalsForDimension(dim));}
    static List<NotesCatalog.StatusEntry> locationStatuses(String object){return states(NotesCatalog.locationStatuses(object));}
    static List<NotesCatalog.StatusEntry> goalStatuses(){return states(NotesCatalog.goalStatuses());}
    static String status(Note note){return UiText.catalog(NotesCatalog.status(note));}
    static String status(Note.Status status){return UiText.catalog(NotesCatalog.status(status));}
    static String kind(Note.Kind kind){return UiText.catalog(NotesCatalog.kind(kind));}
}
