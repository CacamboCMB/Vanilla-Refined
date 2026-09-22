package de.ownmods.mousewheel;

import de.ownmods.settings.api.OptionIcon;
import de.ownmods.settings.api.ToggleOption;
import de.ownmods.settings.config.SimpleManagedMod;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/** Client-only inventory wheel transfer settings. */
public final class MouseWheelSettings extends SimpleManagedMod {
    public MouseWheelSettings(Path path) {
        super(path, "ownmods_mousewheel", "ownmods.mousewheel", "minecraft:hopper", true,
                List.of(
                        new ToggleOption("enabled", "ownmods.mousewheel.enabled", "ownmods.mousewheel.enabled.tip", true),
                        new ToggleOption("invert", "ownmods.mousewheel.invert", "ownmods.mousewheel.invert.tip", false)),
                Map.of(
                        "enabled", new OptionIcon("minecraft:hopper", "ownmods.mousewheel.group"),
                        "invert", new OptionIcon("minecraft:recovery_compass", "ownmods.mousewheel.group")));
    }
}
