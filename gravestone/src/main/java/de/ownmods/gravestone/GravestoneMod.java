package de.ownmods.gravestone;
import de.ownmods.settings.api.ManagedMods;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
public final class GravestoneMod implements ModInitializer {
    public static final Logger LOGGER=LoggerFactory.getLogger("ownmods_gravestone");
    private static volatile GravestoneSettings SETTINGS;
    @Override public void onInitialize() {
        SETTINGS=new GravestoneSettings(FabricLoader.getInstance().getConfigDir().resolve("ownmods/gravestone.properties"));
        ManagedMods.register(SETTINGS);
    }
    public static GravestoneSettings settings(){return SETTINGS;}
}
