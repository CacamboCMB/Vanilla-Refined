package de.ownmods.cropreplant;
import de.ownmods.settings.api.*;
import de.ownmods.settings.config.SimpleManagedMod;
import java.nio.file.Path;
import java.util.*;
public final class CropReplantSettings extends SimpleManagedMod {
    public CropReplantSettings(Path path) {
        super(path,"ownmods_cropreplant","ownmods.cropreplant","minecraft:wheat",false,List.of(
            new ToggleOption("enabled","ownmods.cropreplant.enabled","ownmods.cropreplant.enabled.tip",true),
            new ToggleOption("right_click","ownmods.cropreplant.right_click","ownmods.cropreplant.right_click.tip",true),
            new ToggleOption("sneak_bypass","ownmods.cropreplant.sneak_bypass","ownmods.cropreplant.sneak_bypass.tip",true),
            new ToggleOption("wheat","ownmods.cropreplant.wheat","ownmods.cropreplant.wheat.tip",true),
            new ToggleOption("carrots","ownmods.cropreplant.carrots","ownmods.cropreplant.carrots.tip",true),
            new ToggleOption("potatoes","ownmods.cropreplant.potatoes","ownmods.cropreplant.potatoes.tip",true),
            new ToggleOption("beetroots","ownmods.cropreplant.beetroots","ownmods.cropreplant.beetroots.tip",true),
            new ToggleOption("nether_wart","ownmods.cropreplant.nether_wart","ownmods.cropreplant.nether_wart.tip",true)), Map.ofEntries(
            Map.entry("enabled",new OptionIcon("minecraft:wheat","ownmods.cropreplant.group")),
            Map.entry("right_click",new OptionIcon("minecraft:wooden_hoe","ownmods.cropreplant.group")),
            Map.entry("sneak_bypass",new OptionIcon("minecraft:leather_boots","ownmods.cropreplant.group")),
            Map.entry("wheat",new OptionIcon("minecraft:wheat","ownmods.cropreplant.group")),
            Map.entry("carrots",new OptionIcon("minecraft:carrot","ownmods.cropreplant.group")),
            Map.entry("potatoes",new OptionIcon("minecraft:potato","ownmods.cropreplant.group")),
            Map.entry("beetroots",new OptionIcon("minecraft:beetroot","ownmods.cropreplant.group")),
            Map.entry("nether_wart",new OptionIcon("minecraft:nether_wart","ownmods.cropreplant.group"))));
    }
}
