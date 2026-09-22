package de.ownmods.timber;
import de.ownmods.settings.api.ManagedMods;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
public final class TimberMod implements ModInitializer {
    static final Logger LOGGER = LoggerFactory.getLogger("ownmods_timber");
    @Override public void onInitialize() {
        var settings = new TimberSettings(FabricLoader.getInstance().getConfigDir().resolve("ownmods/timber.properties"));
        ManagedMods.register(settings);
        new TimberEngine(settings).register();
        LOGGER.info("Timber server hooks registered; execution uses per-player server-authoritative settings");
    }
}
