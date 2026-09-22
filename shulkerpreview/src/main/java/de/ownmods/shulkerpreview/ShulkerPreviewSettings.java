package de.ownmods.shulkerpreview;
import de.ownmods.settings.api.*;
import de.ownmods.settings.config.SimpleManagedMod;
import java.nio.file.Path;
import java.util.*;
public final class ShulkerPreviewSettings extends SimpleManagedMod {
    public ShulkerPreviewSettings(Path path) {
        super(path,"ownmods_shulkerpreview","ownmods.shulkerpreview","minecraft:purple_shulker_box",true,List.of(
            new ToggleOption("enabled","ownmods.shulkerpreview.enabled","ownmods.shulkerpreview.enabled.tip",true),
            new ToggleOption("shift_only","ownmods.shulkerpreview.shift_only","ownmods.shulkerpreview.shift_only.tip",false),
            new ToggleOption("counts","ownmods.shulkerpreview.counts","ownmods.shulkerpreview.counts.tip",true),
            new ToggleOption("show_empty","ownmods.shulkerpreview.show_empty","ownmods.shulkerpreview.show_empty.tip",true)), Map.ofEntries(
            Map.entry("enabled",new OptionIcon("minecraft:shulker_box","ownmods.shulkerpreview.group")),
            Map.entry("shift_only",new OptionIcon("minecraft:leather_boots","ownmods.shulkerpreview.group")),
            Map.entry("counts",new OptionIcon("minecraft:paper","ownmods.shulkerpreview.group")),
            Map.entry("show_empty",new OptionIcon("minecraft:glass","ownmods.shulkerpreview.group"))));
    }
}
