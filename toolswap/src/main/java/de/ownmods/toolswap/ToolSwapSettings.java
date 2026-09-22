package de.ownmods.toolswap;
import de.ownmods.settings.api.*;
import de.ownmods.settings.config.SimpleManagedMod;
import java.nio.file.Path;
import java.util.*;
public final class ToolSwapSettings extends SimpleManagedMod {
    public static final ChoiceOption POLICY=new ChoiceOption("policy","ownmods.toolswap.policy","ownmods.toolswap.policy.tip",List.of(
        new ChoiceOption.Value("same","ownmods.toolswap.policy.same","ownmods.toolswap.policy.same.tip","minecraft:iron_pickaxe"),
        new ChoiceOption.Value("same_or_worse","ownmods.toolswap.policy.worse","ownmods.toolswap.policy.worse.tip","minecraft:stone_pickaxe"),
        new ChoiceOption.Value("best","ownmods.toolswap.policy.best","ownmods.toolswap.policy.best.tip","minecraft:netherite_pickaxe")
    ),"best");
    public ToolSwapSettings(Path path){
        super(path,"ownmods_toolswap","ownmods.toolswap","minecraft:diamond_pickaxe",false,List.of(
            new ToggleOption("enabled","ownmods.toolswap.enabled","ownmods.toolswap.enabled.tip",true),
            new ToggleOption("auto_tool","ownmods.toolswap.auto","ownmods.toolswap.auto.tip",false),
            new ToggleOption("allow_enchanted","ownmods.toolswap.enchanted","ownmods.toolswap.enchanted.tip",true)
        ),Map.of(
            "enabled",new OptionIcon("minecraft:diamond_pickaxe","ownmods.toolswap.group"),
            "auto_tool",new OptionIcon("minecraft:iron_shovel","ownmods.toolswap.group"),
            "allow_enchanted",new OptionIcon("minecraft:enchanted_book","ownmods.toolswap.group")
        ),List.of(POLICY));
    }
    public String policy(){return choiceSnapshot().getOrDefault("policy","best");}
}
