package de.ownmods.inventorysort;
import de.ownmods.settings.api.ManagedMods;
import de.ownmods.settings.policy.SettingsWire;
import de.ownmods.settings.server.ServerSettings;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
public final class InventorySortMod implements ModInitializer {
    private static InventorySortSettings settings;
    public static InventorySortSettings settings() { return settings; }
    public void onInitialize() {
        settings = new InventorySortSettings(FabricLoader.getInstance().getConfigDir().resolve("ownmods/inventorysort.properties"));
        ManagedMods.register(settings);
        PayloadTypeRegistry.serverboundPlay().register(SortRequestPayload.TYPE, SortRequestPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(SortRequestPayload.TYPE, (packet, context) -> context.server().execute(() -> {
            if (packet.protocol() != SettingsWire.PROTOCOL || packet.menuId() < 0 || packet.stateId() < 0
                    || !ServerSettings.allowAction(context.player(), packet.revision(), packet.requestId())) return;
            InventorySorter.sort(context.player(), packet.menuId(), packet.stateId(), packet.container(), settings);
        }));
    }
}
