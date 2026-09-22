package de.ownmods.cropreplant;
import de.ownmods.settings.api.ManagedMods;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
public final class CropReplantMod implements ModInitializer {
    static final Logger LOGGER=LoggerFactory.getLogger("ownmods_cropreplant");
    @Override public void onInitialize() {
        var settings=new CropReplantSettings(FabricLoader.getInstance().getConfigDir().resolve("ownmods/cropreplant.properties"));
        ManagedMods.register(settings);
        new CropReplantEngine(settings).register();
    }
}
