package de.ownmods.toolswap;
import de.ownmods.settings.api.ManagedMods;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
public final class ToolSwapMod implements ModInitializer {
    @Override public void onInitialize(){
        var settings=new ToolSwapSettings(FabricLoader.getInstance().getConfigDir().resolve("ownmods/toolswap.properties"));
        ManagedMods.register(settings);
        new ToolSwapEngine(settings).register();
    }
}
