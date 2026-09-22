package de.ownmods.settings.config;

import de.ownmods.settings.api.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/** Shared config mechanics only. No gameplay of the four add-ons lives in the manager. */
public abstract class SimpleManagedMod implements ManagedMod {
    private final String id, title, icon;
    private final boolean clientOnly;
    private final List<ToggleOption> options;
    private final Map<String, OptionIcon> icons;
    private final BooleanSettingsFile file;
    private final List<ChoiceOption> choices;
    protected SimpleManagedMod(Path path, String id, String title, String icon,
            boolean clientOnly, List<ToggleOption> options, Map<String, OptionIcon> icons) {
        this(path, id, title, icon, clientOnly, options, icons, List.of());
    }
    protected SimpleManagedMod(Path path, String id, String title, String icon,
            boolean clientOnly, List<ToggleOption> options, Map<String, OptionIcon> icons, List<ChoiceOption> choices) {
        this.choices = List.copyOf(choices);
        this.id=id; this.title=title; this.icon=icon; this.clientOnly=clientOnly;
        this.options=List.copyOf(options); this.icons=Map.copyOf(icons);
        if (!this.icons.keySet().equals(new HashSet<>(options.stream().map(ToggleOption::key).toList())))
            throw new IllegalArgumentException("Missing/extra option icon");
        this.file=new BooleanSettingsFile(path, options, choices);
    }
    @Override public final String id() { return id; }
    @Override public final String titleKey() { return title + ".title"; }
    @Override public final String descriptionKey() { return title + ".description"; }
    @Override public final String iconItemId() { return icon; }
    @Override public final boolean clientOnly() { return clientOnly; }
    @Override public final List<ToggleOption> options() { return options; }
    @Override public final Map<String, OptionIcon> optionIcons() { return icons; }
    @Override public final Map<String, Boolean> snapshot() { return file.snapshot(); }
    @Override public final void save(Map<String, Boolean> values) throws IOException { file.save(values); }
    @Override public final List<ChoiceOption> choiceOptions() { return choices; }
    @Override public final Map<String, String> choiceSnapshot() { return file.choiceSnapshot(); }
    @Override public final void save(Map<String, Boolean> values, Map<String, String> selected) throws IOException { file.save(values, selected); }
    protected final BooleanSettingsFile.State settingsState() { return file.state(); }
    @Override public final String warning() { return file.warning(); }
    public final boolean flag(String key) { return snapshot().getOrDefault(key, false); }
}
