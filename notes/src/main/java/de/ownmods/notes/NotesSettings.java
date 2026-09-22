package de.ownmods.notes;
import de.ownmods.settings.api.*;
import de.ownmods.settings.config.SimpleManagedMod;
import java.nio.file.Path;
import java.util.*;
public final class NotesSettings extends SimpleManagedMod {
    public NotesSettings(Path path) {
        super(path,"ownmods_notes","ownmods.notes","minecraft:writable_book",true,
            List.of(new ToggleOption("enabled","ownmods.notes.enabled","ownmods.notes.enabled.tooltip",true)),
            Map.of("enabled",new OptionIcon("minecraft:writable_book","general")));
    }
    @Override public String customScreenClass() {return "de.ownmods.notes.client.NotebookScreen";}
}
