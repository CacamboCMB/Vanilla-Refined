package de.ownmods.settings;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SettingsMod implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("ownmods_settings");
    @Override public void onInitialize() {
        de.ownmods.settings.server.ServerSettings.register();
        var loader = FabricLoader.getInstance();
        String minecraft = loader.getModContainer("minecraft")
                .map(m -> m.getMetadata().getVersion().getFriendlyString()).orElse("unknown");
        String fabric = loader.getModContainer("fabricloader")
                .map(m -> m.getMetadata().getVersion().getFriendlyString()).orElse("unknown");
        LOGGER.info("Vanilla Refined foundation candidate: Minecraft {}, Fabric Loader {}", minecraft, fabric);
        LOGGER.warn("This is a compatibility test build, not a runtime-validated release. An accepted Minecraft version is not a compatibility guarantee.");
    }
}
