package de.ownmods.gravestone;
import de.ownmods.settings.api.*;
import de.ownmods.settings.config.SimpleManagedMod;
import java.nio.file.Path;
import java.util.*;
public final class GravestoneSettings extends SimpleManagedMod {
    public GravestoneSettings(Path path) {
        super(path,"ownmods_gravestone","ownmods.gravestone","minecraft:skeleton_skull",false,List.of(
            new ToggleOption("enabled","ownmods.gravestone.enabled","ownmods.gravestone.enabled.tip",true),
            new ToggleOption("single_fallback","ownmods.gravestone.single","ownmods.gravestone.single.tip",true),
            new ToggleOption("chat_message","ownmods.gravestone.chat","ownmods.gravestone.chat.tip",true)
        ),Map.of(
            "enabled",new OptionIcon("minecraft:skeleton_skull","ownmods.gravestone.group"),
            "single_fallback",new OptionIcon("minecraft:chest","ownmods.gravestone.group"),
            "chat_message",new OptionIcon("minecraft:compass","ownmods.gravestone.group")
        ));
    }
}
